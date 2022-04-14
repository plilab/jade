package org.ucombinator.jade.maven

import org.eclipse.aether.resolution.ArtifactResolutionException
import org.eclipse.aether.RepositorySystemSession
import org.eclipse.aether.repository.LocalMetadataRequest
import org.apache.maven.repository.internal.MavenRepositorySystemUtils
import org.eclipse.aether.artifact.Artifact
import org.eclipse.aether.metadata.Metadata
import org.eclipse.aether.metadata.DefaultMetadata
import org.eclipse.aether.resolution.ArtifactRequest
import org.eclipse.aether.resolution.ArtifactResult
import org.eclipse.aether.resolution.MetadataRequest
import org.eclipse.aether.resolution.MetadataResult
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.collection.CollectRequest
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory
import org.eclipse.aether.DefaultRepositorySystemSession
import org.eclipse.aether.graph.DependencyNode
import org.eclipse.aether.graph.Exclusion
import org.eclipse.aether.repository.LocalRepository
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.repository.RepositoryPolicy
import org.eclipse.aether.repository.LocalMetadataRegistration
import org.eclipse.aether.RepositorySystem
import org.eclipse.aether.resolution.ArtifactDescriptorRequest
import org.eclipse.aether.resolution.VersionRequest
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory
import org.eclipse.aether.spi.connector.transport.TransporterFactory
import org.eclipse.aether.transport.file.FileTransporterFactory
import org.eclipse.aether.transport.http.HttpTransporterFactory
import org.eclipse.aether.util.artifact.JavaScopes
import org.ucombinator.jade.maven.googlecloudstorage.GcsTransporterFactory
import org.ucombinator.jade.maven.googlecloudstorage.HttpRepository
import org.ucombinator.jade.util.AtomicWriteFile
import org.ucombinator.jade.util.Log
import org.eclipse.aether.resolution.ArtifactDescriptorResult
import org.eclipse.aether.resolution.VersionRangeRequest
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader
import org.eclipse.aether.version.VersionScheme
import org.eclipse.aether.util.version.GenericVersionScheme
import org.apache.maven.model.building.DefaultModelBuilderFactory
import org.apache.maven.model.building.DefaultModelBuilder
import org.apache.maven.model.building.DefaultModelBuildingRequest
import org.apache.maven.model.building.ModelBuildingRequest
import org.apache.maven.model.building.FileModelSource
import org.apache.maven.model.resolution.ModelResolver
import org.apache.maven.model.Model
import org.apache.maven.resolver.examples.util.ConsoleDependencyGraphDumper
import org.ucombinator.jade.util.Tuples.Fiveple
import org.eclipse.aether.util.repository.AuthenticationBuilder
import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.eclipse.aether.util.ConfigUtils
import org.eclipse.aether.impl.ArtifactResolver

import java.io.File
import java.io.FileInputStream
import java.io.StringWriter
import java.io.PrintWriter
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.*
import kotlin.text.RegexOption
import kotlin.random.Random

// find dependencies/ -name \*.dependencies | xargs cat |perl -ne 'chomp; my ($group, $art, $ver) = split(":"); $group =~ s[\.][/]g; $jars{"maven2/$group/$art/$ver/$art-$ver.jar"} = 1; END { open my $f, "index.tsv"; while (my $line = <$f>) { chomp $line; my ($file, $size) = split("\t", $line); if ($jars{$file}) { $total += $size; print("$total\t$file\n") } } print("$total\n"); }' >tmp.txt &

// WARN  .org.apache.http.client.protocol.ResponseProcessCookies Invalid cookie header: ...
// WARN  .org.eclipse.aether.internal.impl.DefaultUpdatePolicyAnalyzer: Non-parseable repository update policy 'interval:1m', assuming 'interval:1440'

// WARN  .org.eclipse.aether.internal.impl.WarnChecksumPolicy: Could not validate integrity of download from http://maven.aliyun.com/mvn/repository/org/glassfish/javax.el/maven-metadata.xml
// org.eclipse.aether.transfer.ChecksumFailureException: Checksum validation failed, expected <!doctype but is 2321baca4e611c8a1f8aebd0123a1fb15ceae0c8
//         at org.eclipse.aether.connector.basic.ChecksumValidator.validateExternalChecksums(ChecksumValidator.java:174)
//         at org.eclipse.aether.connector.basic.ChecksumValidator.validate(ChecksumValidator.java:103)
//         at org.eclipse.aether.connector.basic.BasicRepositoryConnector$GetTaskRunner.runTask(BasicRepositoryConnector.java:460)
//         at org.eclipse.aether.connector.basic.BasicRepositoryConnector$TaskRunner.run(BasicRepositoryConnector.java:364)
//         at org.eclipse.aether.util.concurrency.RunnableErrorForwarder.lambda$wrap$0(RunnableErrorForwarder.java:73)
//         at org.eclipse.aether.connector.basic.BasicRepositoryConnector$DirectExecutor.execute(BasicRepositoryConnector.java:627)
//         at org.eclipse.aether.connector.basic.BasicRepositoryConnector.get(BasicRepositoryConnector.java:235)
//         at org.eclipse.aether.internal.impl.DefaultMetadataResolver$ResolveTask.run(DefaultMetadataResolver.java:582)
//         at org.eclipse.aether.util.concurrency.RunnableErrorForwarder.lambda$wrap$0(RunnableErrorForwarder.java:73)
//         at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1128)
//         at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:628)
//         at java.base/java.lang.Thread.run(Thread.java:829)
// INFO  .org.apache.http.impl.execchain.RetryExec: I/O exception (java.net.SocketException) caught when processing request to {s}->https://maven-central-asia.storage-download.googleapis.com:443: Broken pipe (Write failed)
// INFO  .org.apache.http.impl.execchain.RetryExec: Retrying request to {s}->https://maven-central-asia.storage-download.googleapis.com:443

data class DotInGroupIdException(val groupIdPath: String, val artifactId: String) : Exception("File path for groupId contains a dot: $groupIdPath (artifactId = $artifactId)")
data class NoVersioningException(val groupId: String, val artifactId: String) : Exception("No <versioning> tag in POM for $groupId:$artifactId")
data class NoVersionsException(val groupId: String, val artifactId: String) : Exception("No versions in POM for for $groupId:$artifactId")
data class UnsolvableArtifactException(val groupId: String, val artifactId: String) : Exception("Skipped artifact with unsolvable dependencies: $groupId:$artifactId")
data class WriteLockException(val groupId: String, val artifactId: String, val e: IllegalStateException) : Exception("Could not aquire write lock for $groupId:$artifactId", e)
data class ModelParsingException(val file: File) : Exception("Could not parse POM file $file")
data class SystemDependencyException(
  val rootGroupId: String, val rootArtifactId: String, val rootVersion: String,
  val dependencyGroupId: String, val dependencyArtifactId: String, val dependencyVersion: String) :
  Exception("Dependency on system provided artifact $dependencyGroupId:$dependencyArtifactId:$dependencyVersion by $rootGroupId:$rootArtifactId:$rootVersion")
data class NoPomException(
  val rootGroupId: String, val rootArtifactId: String, val rootVersion: String,
  val dependencyGroupId: String, val dependencyArtifactId: String, val dependencyVersion: String,
  val e: Throwable) : Exception(e)
data class NoRootPomException(
  val groupId: String, val artifactId: String, val version: String,
  val e: Throwable) : Exception("No POM for $groupId:$artifactId:$version", e)

// }

class CachingArtifactResolver() : ArtifactResolver {
  val cache = Collections.synchronizedMap(mutableMapOf<Artifact, ArtifactResolutionException>())
  override fun resolveArtifact(session: RepositorySystemSession, request: ArtifactRequest): ArtifactResult {
    val ex = cache.get(request.artifact)
    if (ex !== null) {
      // println("**** $request $ex")
      // ex.printStackTrace()
      // println("----- $request")
      throw ex
    }

    try {
      val r = defaultArtifactResolver!!.resolveArtifact(session, request)
      // if (r.exceptions.isNotEmpty()) {
      //   println("%%%% $request ${r.exceptions}")
      // }
      return r
    } catch (e: ArtifactResolutionException) {
      // e.printStackTrace()
      // println("-----------")
      cache.put(request.artifact, e)
      // if (!(e is IllegalStateException)) { println("??? $request $e"); cache.put(request.artifact, e) }
      throw e
    }
  }

  override fun resolveArtifacts(session: RepositorySystemSession, requests: MutableCollection<out ArtifactRequest>): List<ArtifactResult> =
    requests.map { resolveArtifact(session, it) }
              
      // throws ArtifactResolutionException;
  
  companion object {
    var defaultArtifactResolver: ArtifactResolver? = null
  }
}

class DownloadMaven(val indexFile: File, val localRepoDir: File, val jarListsDir: File) {
  private val log = Log {}

  val temporaryFails = Collections.synchronizedMap(mutableMapOf<String, Int>())
  val running = Collections.synchronizedMap(mutableMapOf<Pair<String, String>, Long>())

  val cached = AtomicInteger()
  val pass = AtomicInteger()
  val permanentFail = AtomicInteger()
  val temporaryFail = AtomicInteger()

  val locator = MavenRepositorySystemUtils.newServiceLocator()

  init {
    locator.addService(RepositoryConnectorFactory::class.java, BasicRepositoryConnectorFactory::class.java)
    locator.addService(TransporterFactory::class.java, FileTransporterFactory::class.java)
    locator.addService(TransporterFactory::class.java, HttpTransporterFactory::class.java)
    // locator.addService(TransporterFactory::class.java, GcsTransporterFactory::class.java)
    CachingArtifactResolver.defaultArtifactResolver = locator.getService(ArtifactResolver::class.java)
    // println("defaultArtifactResolver $defaultArtifactResolver")
    

    locator.setService<ArtifactResolver>(ArtifactResolver::class.java, CachingArtifactResolver::class.java)
  }

  // locator.setErrorHandler( object : DefaultServiceLocator.ErrorHandler() {
  //   override fun serviceCreationFailed(type: Class<*>, impl: Class<*>, exception: Throwable) {
  //     TODO()
  //   }
  // })

  val system = locator.getService(RepositorySystem::class.java)
  val versionScheme = GenericVersionScheme() // locator.getService(VersionScheme::class.java)
  val modelBuilder = DefaultModelBuilderFactory().newInstance() // TODO: locator
  val session = MavenRepositorySystemUtils.newSession()
  val localRepo = LocalRepository(localRepoDir)
  init {
    session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo))
  }

  val remote = HttpRepository
    .getBuilder()
    .setPolicy(RepositoryPolicy(true, RepositoryPolicy.UPDATE_POLICY_NEVER, RepositoryPolicy.CHECKSUM_POLICY_FAIL))
    .build()

  val authentication =
    AuthenticationBuilder()
      .addHostnameVerifier(NoopHostnameVerifier())
      .build()

  val remotes =
    listOf(
      "google-maven-central" to "https://maven-central-asia.storage-download.googleapis.com/maven2", // 8.7M jars
      "sonatype-releases" to "https://oss.sonatype.org/content/repositories/releases", // 2.1M jars
      // "springio-plugins-release" to "https://repo.spring.io/plugins-release", // 1.4M jars; non-functional", redirects to jfrog
      "springio-plugins-release" to "https://repo.spring.io/ui/native/plugins-release", // 1.4M jars
      "jboss-releases" to "https://repository.jboss.org/nexus/content/repositories/releases", // 273k jars
      "wso2-public" to "https://maven.wso2.org/nexus/content/repositories/public", // 227k jars
      "redhat-ga" to "https://maven.repository.redhat.com/ga/org/", // 145k jars
      "geomajas" to "http://maven.geomajas.org", // 120k jars
      "icm" to "https://maven.icm.edu.pl/artifactory/repo", // 94k jars
      "apache-releases" to "https://repository.apache.org/content/repositories/releases", // 92k jars
      "mulesoft-public" to "https://repository.mulesoft.org/nexus/content/repositories/public", // 72k jars
      "google" to "https://maven.google.com", // 32k jars
      "netbeans" to "http://netbeans.apidesign.org/maven2", // 21k jars; Note that old netbeans repo was decommissioned
      "typesafe-maven-releases" to "https://repo.typesafe.com/typesafe/maven-releases", // 8.2k jars
      // "ow2-public" to "https://repository.ow2.org/nexus/content/repositories/public", // 3.8k jars
      "ow2-public" to "https://repository.ow2.org/nexus/content/groups/public", // 3.8k jars
      "titalwave-releases" to "http://services.tidalwave.it/nexus/content/repositories/releases", // 96 jars
    ).map {
      RemoteRepository.Builder(it.first, "default", it.second)
        .setPolicy(RepositoryPolicy(true, RepositoryPolicy.UPDATE_POLICY_NEVER, RepositoryPolicy.CHECKSUM_POLICY_FAIL))
        .setAuthentication(authentication)
        .build()
    }

  // TODO: use transport listener to log when downloads start
  // TODO: use RepositoryListener (not transportlistener) (possibly use request traces to correlate things)
  fun run() {
    Runtime.getRuntime().addShutdownHook(
      object : Thread() {
        override fun run() {
          println()
          println("cache $cached")
          println("pass  $pass")
          println("fail  $permanentFail")
          println("abort $temporaryFail")

          println()
          for ((key, value) in temporaryFails.toList().sortedBy(Pair<String, Int>::first).sortedBy(Pair<String, Int>::second)) {
            println("$value\t$key")
          }

          println()
          println("Running:\n")
          for ((name, startTime) in running.toList().sortedBy(Pair<Pair<String, String>, Long>::second).reversed()) {
            println("- ${time(startTime)}: ${name.first}:${name.second}")
          }
        }
      }
    )

    MavenRepo.listArtifacts(indexFile) { artifacts ->
      runBlocking {
        val reorderedArtifacts = if (false) artifacts.shuffled() else artifacts
        for ((groupIdPath, artifactId) in reorderedArtifacts) {
          // if (groupIdPath[0] != 'o') continue // e i m s t
          if (groupIdPath.startsWith("com/")) continue
          // if (!groupIdPath.startsWith("io/")) continue
          // if (!groupIdPath.startsWith("net/")) continue
          // if (!groupIdPath.startsWith("org/")) continue
          // if (groupId.startsWith("org.webjars.")) continue

          val jarListFile = jarListsDir.resolve(groupIdPath).resolve("$artifactId.jar-list")
          if (jarListFile.exists()) {
            cached.incrementAndGet()
            // println("cached $groupIdPath:$artifactId")
            continue
          }

          async(Dispatchers.IO) { collectDependencies(groupIdPath, artifactId, jarListFile) }
        }
      }
    }
  }
  // TODO: clean repo before starting

  fun time(startTime: Long): String = "%.2f".format((System.nanoTime() - startTime).toDouble() / 1e9)

  fun collectDependencies(groupIdPath: String, artifactId: String, jarListFile: File) {
    val groupId = groupIdPath.replace('/', '.')
    val name = "$groupId:$artifactId"
    val startTime = System.nanoTime()
    var i = 1
    try {
      running.put(Pair(groupIdPath, artifactId), startTime)
      println("running +${running.size}")
      if (groupIdPath.contains('.')) throw DotInGroupIdException(groupIdPath, artifactId) // Maven will incorrectly translate the '.' to a '/'
      val maxTries = 30
      println("start   $name")
      while (true) {
        try {
          // println("start  $i $name")
          val version = getVersion(groupId, artifactId)
          val artifactDescriptorResult = getArtifactDescriptor(groupId, artifactId, version)
          val pomArtifactResult = //try {
            getArtifact(groupId, artifactId, null, "pom", version)
          // } catch (e: Throwable) {
          //   throw NoRootPomException(groupId, artifactId, version, e)
          // }
          val model = getModel(pomArtifactResult.artifact.file)
          val artifactResult = getArtifact(groupId, artifactId, artifactDescriptorResult.artifact.classifier, model.packaging, version)
          artifactDescriptorResult.setArtifact(artifactResult.artifact)
          artifactDescriptorResult.request.setArtifact(artifactResult.artifact)
          val dependencyTree = getDependencyTree(artifactDescriptorResult)
          val dependencyList = getDependencyList(dependencyTree)
          val artifactResults = downloadDependencies(dependencyList)
          writeJarList(startTime, artifactResults, jarListFile)
          pass.incrementAndGet()
          println("pass  $i ${time(startTime)} $name")
          break
        } catch (e: Throwable) {
          when {
            isA(
              e,
              java.lang.IllegalStateException::class,
            ) && e.message!!.startsWith("Could not acquire write lock for ")
            -> {
              if (i == maxTries) throw WriteLockException(groupId, artifactId, e as IllegalStateException)
              else { Thread.sleep(Random.nextLong(1_000, 3_000)); i += 1; continue }
            }

            else -> throw e
          }
        }
        TODO("impossible case")
      }
    } catch (e: Exception) {
      val t = time(startTime)
      if (isPermanentFailure(e)) {
        println("fail  $i $t $name")
        permanentFail.incrementAndGet()
        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)
        e.printStackTrace(printWriter)
        printWriter.flush()
        val content =
          "!" + exceptionName(e) + "\n" +
          "time: $t\n" +
          stringWriter.toString().replace("^".toRegex(RegexOption.MULTILINE), "# ")
        try {
          AtomicWriteFile.write(jarListFile, content, true)
        } catch (e: Throwable) {
          log.error(e) { "Failed to write permanent errors to file $jarListFile" }
        }
      } else {
        println("abort $i $t $name")
        temporaryFail.incrementAndGet()
        temporaryFails.merge(exceptionName(e), 1, Int::plus)
        // log.error(e) { "Failed to get dependencies of $groupId:$artifactId" }
        if (e is WriteLockException) {
          println("WRITE LOCK maven2/${groupId.replace(".", "/")}/$artifactId/maven-metadata.xml\t0")
        }
        if (
          isA(
            e,
            // org.eclipse.aether.resolution.ArtifactResolutionException::class,
            org.eclipse.aether.transfer.ArtifactNotFoundException::class,
          )
        ) {
          var ex: Throwable? = e
          while (
            ex is org.apache.maven.model.resolution.UnresolvableModelException ||
            ex is org.eclipse.aether.collection.DependencyCollectionException ||
            ex is org.eclipse.aether.resolution.ArtifactDescriptorException ||
            ex is org.eclipse.aether.resolution.ArtifactResolutionException
          ) {
            ex = ex.cause
          }
          val x = ex as org.eclipse.aether.transfer.ArtifactNotFoundException
          println("ARTIFACT NOT FOUND ${x.artifact}")
        }
        log.error(e) { "!!!!!!! $groupId:$artifactId" }
      }
    } finally {
      running.remove(Pair(groupIdPath, artifactId))
      println("running -${running.size}")
    }
  }

  fun isPermanentFailure(e: Throwable) =
    isA(
      e,
      org.eclipse.aether.transfer.MetadataTransferException::class,
      org.eclipse.aether.transfer.ChecksumFailureException::class,
    ) ||
    isA(
      e,
      org.eclipse.aether.transfer.ArtifactTransferException::class,
      org.eclipse.aether.transfer.NoRepositoryConnectorException::class,
      org.eclipse.aether.transfer.NoRepositoryConnectorException::class,
      org.eclipse.aether.transfer.NoTransporterException::class
    ) ||
    isA(
      e,
      org.eclipse.aether.collection.UnsolvableVersionConflictException::class,
    ) ||
    isA(
      e,
      org.eclipse.aether.resolution.VersionRangeResolutionException::class,
    ) ||
    isA(
      e,
      org.eclipse.aether.resolution.VersionRangeResolutionException::class,
      org.eclipse.aether.version.InvalidVersionSpecificationException::class,
    ) ||
    isA(
      e,
      org.eclipse.aether.transfer.ArtifactTransferException::class,
      org.eclipse.aether.transfer.ChecksumFailureException::class,
    ) ||
    isA(
      e,
      org.eclipse.aether.transfer.ChecksumFailureException::class,
    ) ||
    isA(
      e,
      org.eclipse.aether.transfer.ArtifactTransferException::class,
      org.eclipse.aether.transfer.NoRepositoryConnectorException::class,
      org.eclipse.aether.transfer.NoRepositoryConnectorException::class,
      org.eclipse.aether.transfer.NoRepositoryLayoutException::class,
      org.eclipse.aether.transfer.NoRepositoryLayoutException::class,
    ) ||
    e is DotInGroupIdException ||
    // e is NoPomException ||
    e is NoRootPomException ||
    e is ModelParsingException ||
    e is NoVersioningException ||
    e is NoVersionsException ||
    e is SystemDependencyException

  fun getVersion(groupId: String, artifactId: String): String {
    val metadata = DefaultMetadata(groupId, artifactId, null, "maven-metadata.xml", Metadata.Nature.RELEASE)
    val metadataRequest = MetadataRequest(metadata, remote, null)
    val metadataResults = system.resolveMetadata(session, listOf(metadataRequest))
    assert(metadataResults.size == 1)
    val metadataResult = metadataResults[0]
    if (metadataResult.metadata === null) {
      assert(metadataResult.exception !== null)
      throw metadataResult.exception
    }
    val m = FileInputStream(metadataResult.metadata.file).use { MetadataXpp3Reader().read(it, false) }
    if (m.versioning === null) throw NoVersioningException(groupId, artifactId)
    if (m.versioning.release !== null) return m.versioning.release
    if (m.versioning.latest !== null) return m.versioning.latest
    val versions = m.versioning.versions
    return versions.maxByOrNull(versionScheme::parseVersion) ?: throw NoVersionsException(groupId, artifactId)
  }

  // TODO: classifier?
  fun getArtifactDescriptor(groupId: String, artifactId: String, version: String): ArtifactDescriptorResult {
    val artifact = DefaultArtifact(groupId, artifactId, "pom", version)
    // val artifactRequest = ArtifactRequest(artifact, listOf(remote), null)
    // val artifactResult = system.resolveArtifact(session, artifactRequest)

    val descriptorRequest = ArtifactDescriptorRequest(artifact, remotes, null)
    // val artifactRequest = ArtifactRequest(artifact, listOf(remote), null)
    return system.readArtifactDescriptor(session, descriptorRequest)
    // return system.resolveArtifact(session, artifactRequest)
    // TODO: try local first then log if having to hit remote
  }

  fun getModel(file: File): Model {
    return modelBuilder.buildRawModel(file, ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL, false).get()
      ?: throw ModelParsingException(file)
  }

  // TODO: rename extension to packaging
  fun getArtifact(groupId: String, artifactId: String, classifier: String?, extension: String, version: String): ArtifactResult {
    val (cls, extensions) = when {
      extension == "pom" -> Pair(null, listOf(extension)) // TODO: null isn't technically correct (should be classifier)
      extension == "feature" -> Pair("features", listOf("xml"))
      extension == "tile" -> Pair(classifier, listOf("xml"))
      classifier == "config" -> Pair("config", listOf("cfg"))
      extension == "eclipse-target-definition" -> Pair(classifier, listOf("target"))
      else -> Pair(classifier, listOf(extension, "jar", "zip"))
    }
    // println("$groupId:$artifactId -> extension $extension classifier $classifier cls $cls extensions $extensions")
    var firstException: Throwable? = null
    for (ext in extensions) {
      // println("trying $ext")
      val artifact = DefaultArtifact(groupId, artifactId, cls, ext, version)
      val artifactRequest = ArtifactRequest(artifact, remotes, null)
      // return system.readArtifactDescriptor(session, descriptorRequest)
      try {
        return system.resolveArtifact(session, artifactRequest)
      } catch (e: org.eclipse.aether.resolution.ArtifactResolutionException) {
        // e.printStackTrace()
        // println("iseq ${e.cause is org.eclipse.aether.transfer.ArtifactNotFoundException}")
        firstException = firstException ?: e
        // if (e.cause is org.eclipse.aether.transfer.ArtifactNotFoundException) { /* Do nothing */ }
        // else throw e
      }
      // TODO: try local first then log if having to hit remote
    }
    throw firstException!!
  }

  fun getDependencyTree(descriptorResult: ArtifactDescriptorResult): DependencyNode {
    if (unsolvable(descriptorResult.artifact.groupId, descriptorResult.artifact.artifactId)) {
      throw UnsolvableArtifactException(descriptorResult.artifact.groupId, descriptorResult.artifact.artifactId)
    }

    val collectRequest = CollectRequest()
    // collectRequest.setRoot(Dependency(descriptorResult.artifact, JavaScopes.COMPILE))
    collectRequest.setRootArtifact(descriptorResult.artifact)
    // println("desc ${descriptorResult} ${descriptorResult.dependencies}")
    collectRequest.setDependencies(descriptorResult.dependencies)
    collectRequest.setManagedDependencies(descriptorResult.managedDependencies)
    collectRequest.setRepositories(remotes) //listOf(remote))
    val collectResult = system.collectDependencies(session, collectRequest)
    // collectResult.getRoot().accept(ConsoleDependencyGraphDumper())
    return collectResult.getRoot()
  }

  fun getDependencyList(root: DependencyNode): List<Fiveple<String, String, String, String, String>> {
    // root.accept(ConsoleDependencyGraphDumper())
    var artifactRequests = listOf<Fiveple<String, String, String, String, String>>()

    fun match(string: String, pattern: String): Boolean = pattern == "*" || pattern == string

    fun match(a: Artifact, e: Exclusion): Boolean =
      match(a.groupId, e.groupId) &&
      match(a.artifactId, e.artifactId) &&
      match(a.classifier, e.classifier) &&
      match(a.extension, e.extension)

    fun go(node: DependencyNode, exclusions: List<Exclusion>) {
      // TODO: use dependency.exclusions to cut off search
      when (node.dependency?.scope) {
        // null ->
        // compile
        // provided
        JavaScopes.RUNTIME -> return
        JavaScopes.TEST -> return
        JavaScopes.SYSTEM -> throw SystemDependencyException(root.artifact.groupId, root.artifact.artifactId, root.artifact.version, node.artifact.groupId, node.artifact.artifactId, node.artifact.version)
        // system
        // import
      }

      if (exclusions.any { match(node.artifact, it) }) {
        log.error { "Found exclusion: ${root.artifact} -> ... -> ${node.artifact} (exclusions: $exclusions)" }
        return
      }

      // if (node.artifact !== node.dependency?.artifact) {
      //   log.error("dependency.artifact === artifact: ${node.artifact === node.dependency?.artifact} for ${node.artifact} ${node.dependency} ${groupId}:${artifactId}")
      // }

      if (false) {
        // println(node.aliases)
        println("------------")
        println("artifact: ${node.artifact}")
        println("artifact.artifactId: ${node.artifact.artifactId}")
        println("artifact.baseVersion: ${node.artifact.baseVersion}")
        println("artifact.classifier: ${node.artifact.classifier}")
        println("artifact.extension: ${node.artifact.extension}")
        println("artifact.file: ${node.artifact.file}") // TODO: use this in dependencies file
        println("artifact.groupId: ${node.artifact.groupId}")
        println("artifact.properties: ${node.artifact.properties}")
        println("artifact.version: ${node.artifact.version}")
        println("data: ${node.data}")
        println("dependency: ${node.dependency}")
        println("dependency.artifact == artifact: ${node.artifact === node.dependency?.artifact}")
        println("dependency.exclusions: ${node.dependency?.exclusions}")
        println("dependency.optional: ${node.dependency?.optional}")
        println("dependency.scope: ${node.dependency?.scope}")
        println("managedBits: ${node.managedBits}")
        println("managedBigs.MANAGED_EXCLUSIONS: ${node.managedBits and DependencyNode.MANAGED_EXCLUSIONS != 0}")
        println("managedBigs.MANAGED_OPTIONAL: ${node.managedBits and DependencyNode.MANAGED_OPTIONAL != 0}")
        println("managedBigs.MANAGED_PROPERTIES: ${node.managedBits and DependencyNode.MANAGED_PROPERTIES != 0}")
        println("managedBigs.MANAGED_SCOPE: ${node.managedBits and DependencyNode.MANAGED_SCOPE != 0}")
        println("managedBigs.MANAGED_VERSION: ${node.managedBits and DependencyNode.MANAGED_VERSION != 0}")
        // println(node.relocations)
        // println(node.repositories)
        println("requestContext: ${node.requestContext}")
        println("version: ${node.version}")
        println("versionConstraint: ${node.versionConstraint}")

        // String premanaged = DependencyManagerUtils.getPremanagedVersion( node );
        // premanaged = DependencyManagerUtils.getPremanagedScope( node );
        // DependencyNode winner = (DependencyNode) node.getData().get( ConflictResolver.NODE_DATA_WINNER );
        // if ( winner != null && !ArtifactIdUtils.equalsId( a, winner.getArtifact() ) )
      }

      // println("packaging ${node.artifact} ${node.artifact.extension}")
      val pomArtifactResult = //try {
        getArtifact(node.artifact.groupId, node.artifact.artifactId, null, "pom", node.artifact.version)
      // } catch (e: Throwable) {
      //   throw NoPomException(root.artifact.groupId, root.artifact.artifactId, root.artifact.version, node.artifact.groupId, node.artifact.artifactId, node.artifact.version, e)
      // }
      val model = getModel(pomArtifactResult.artifact.file)
      val art = Fiveple(node.artifact.groupId, node.artifact.artifactId, node.artifact.classifier, model.packaging, node.artifact.version)
      artifactRequests += art

      for (child in node.children) {
        go(child, exclusions + (node.dependency?.exclusions ?: listOf()))
      }
    }

    go(root, listOf())

    return artifactRequests
  }

  fun downloadDependencies(artifactRequests: List<Fiveple<String, String, String, String, String>>): List<ArtifactResult> {
    // TODO: parallel map
    // return system.resolveArtifacts(session, artifactRequests)
    suspend fun <A, B> Iterable<A>.pmap(f: suspend (A) -> B): List<B> = coroutineScope {
      map { async { f(it) } }.awaitAll()
    }
    // return runBlocking (Dispatchers.IO) {
    //   artifactRequests.pmap { getArtifact(it._1, it._2, it._3, it._4, it._5) }
    // }
    return artifactRequests.map { getArtifact(it._1, it._2, it._3, it._4, it._5) }
    // TODO: check that all results succeeded?
  }

  fun writeJarList(startTime: Long, artifactResults: List<ArtifactResult>, jarListFile: File) {
    val builder = StringBuilder()
    builder.append("time: ${time(startTime)}\n")
    for (result in artifactResults) {
      builder.append(result.artifact.file.relativeTo(localRepoDir).toString() + "\t${result.repository}\n")
    }
    try {
      AtomicWriteFile.write(jarListFile, builder.toString(), true)
    } catch (e: Throwable) {
      log.error(e) { "Failed to write dependencies file $jarListFile" }
    }
  }

  fun exceptionName(exception: Throwable): String {
    var e: Throwable? = exception
    var l = listOf<String>()

    while (e !== null) {
      l += e::class.qualifiedName ?: "<anonymous>"
      e = e.cause
    }

    return l.joinToString(":")
  }

  fun isA(exception: Throwable?, vararg classes: kotlin.reflect.KClass<*>): Boolean {
    var e: Throwable? = exception
    while (
      e is org.apache.maven.model.resolution.UnresolvableModelException ||
      e is org.eclipse.aether.collection.DependencyCollectionException ||
      e is org.eclipse.aether.resolution.ArtifactDescriptorException ||
      e is org.eclipse.aether.resolution.ArtifactResolutionException
    ) {
      e = e.cause
    }

    for (c in classes) {
      if (e === null) { return false }
      if (!c.isInstance(e)) { return false }
      e = e.cause
    }
    return e === null
  }

  private val UNSOLVABLE_ORG_WEBJARS_NPM_ARTIFACTS = listOf(
    "3dmol",
    "admin-lte",
    "adminlte-reactjs",
    "aframe",
    "alfresco-js-api",
    "formio",
    "github-com-MyEtherWallet-MEWconnect-web-client",
    "github-com-palantir-blueprint",
    "jest",
    "jest__reporters",
    "jest-runtime",
    "nuxt",
    "angular-data-grid",
    "angular-devkit__build-angular",
    "angular-patternfly",
    "apollo-language-server",
    "asyncapi__web-component",
    "atlassian__aui",
    "aurelia-cli",
    "aurelia-webpack-plugin",
    "ava",
    "babel-cli",
    "babel-plugin-add-module-exports",
    "babel-plugin-transform-amd-system-wrapper",
    "babel-plugin-transform-cjs-system-wrapper",
    "babel-plugin-transform-decorators-legacy",
    "babel-plugin-transform-global-system-wrapper",
    "babel-preset-es2015-loose",
    "babel-preset-es2015-webpack",
    "bourbon-bitters",
    "brfs",
    "canvas2pdf",
    "cli-color",
    "cssnano-preset-default",
    "cssnano",
    "cucumber",
    "d3-geomap",
    "debug-fabulous",
    "derequire",
    "documentation",
    "dr-svg-sprites",
    "duration",
    "elastic__eui",
    "email-builder-core",
    "ember-cli-babel",
    "ember-data",
    "ember-source",
    "emissary",
    "enzyme",
    "es6-iterator",
    "escope",
    "esniff",
    "excaliburjs",
    "fbjs-scripts",
    "first-mate",
    "fmin",
    "foliojs-fork__fontkit",
    "foliojs-fork__pdfkit",
    "fomantic-ui",
    "fontkit",
    "formio-workers",
    "frisby",
    "fs2",
    "github-com-angular-flex-layout",
    "github-com-ant-design-ant-design",
    "github-com-arnellebalane-hermes",
    "github-com-aurelia-webpack-plugin",
    "github-com-bpampuch-pdfmake",
    "github-com-brightcove-videojs-overlay",
    "github-com-brightcove-videojs-playlist",
    "github-com-ColorlibHQ-AdminLTE",
    "github-com-d2-projects-d2-admin",
    "github-com-DesignRevision-shards-dashboard",
    "github-com-EmergingTechnologyAdvisors-node-serialport",
    "github-com-facebook-regenerator",
    "github-com-haraldrudell-react-swagger-ui",
    "github-com-JetBrains-ring-ui",
    "github-com-jitsi-jitsi-meet",
    "github-com-jmurphyau-ember-truth-helpers",
    "github-com-knsv-mermaid",
    "github-com-krystalcampioni-vue-hotel-datepicker",
    "github-com-marmelab-admin-on-rest",
    "github-com-medikoo-memoizee",
    "github-com-modularcode-modular-admin-html",
    "github-com-naomiaro-waveform-playlist",
    "github-com-polymer-polymer-cli",
    "github-com-Polymer-polymer-cli",
    "github-com-swagger-api-swagger-ui",
    "github-com-uber-react-map-gl",
    "github-com-yegor256-tacit",
    "gitlab-com-meno-dropzone",
    "graphql-import",
    "graphql-playground-react",
    "graphql-toolkit__apollo-engine-loader",
    "graphql-voyager",
    "gulp-cli",
    "gulp-copy",
    "gulp-diff",
    "gulp-eslint",
    "gulp-sass",
    "gulp-useref",
    "gulp",
    "highlights",
    "htmlhint",
    "htmlnano",
    "is-hundred",
    "jest__test-sequencer",
    "jest-cli",
    "jest-environment-jsdom",
    "jest-jasmine2",
    "jest-resolve-dependencies",
    "jest-snapshot",
    "jscs",
    "json-schema-to-typescript",
    "kafka-node",
    "last-run",
    "leaflet-realtime",
    "lerna",
    "linebreak",
    "marathon-ui",
    "material-ui__utils",
    "material-ui",
    "memoizee",
    "metro-babel-register",
    "metro-react-native-babel-transformer",
    "minimize",
    "mqtt",
    "ncjsm",
    "node-sass",
    "oas-raml-converter",
    "ol3-layerswitcher",
    "pdfkit",
    "pdfmake",
    "plotly.js",
    "polyfill-service",
    "postcss-svgo",
    "property-accessors",
    "ra-core",
    "ra-ui-materialui",
    "rails__webpacker",
    "raml2html",
    "react-admin",
    "react-bootstrap-select",
    "react-bootstrap-typeahead",
    "react-confirm-bootstrap",
    "react-map-gl",
    "react-native",
    "react-scripts",
    "react-styleguidist",
    "react-web",
    "regl-line2d",
    "rollup-plugin-postcss",
    "sass-lint",
    "scope-analyzer",
    "scssify",
    "serialport",
    "static-module",
    "supermap__iclient-ol",
    "svg-to-pdfkit",
    "svgo",
    "swagger-editor",
    "swagger-ui",
    "systemjs-builder",
    "tableexport.jquery.plugin",
    "ts-jest",
    "turf__turf",
    "types__protractor",
    "types__react-loadable",
    "unassertify",
    "undertaker",
    "venn.js",
    "vinyl-fs",
    "vue-element-utils",
    "vue-hotel-datepicker",
    "vueify",
    "waveform-playlist",
    "web3-bzz",
    "web3-core-helpers",
    "web3-core-method",
    "web3-core-requestmanager",
    "web3-core-subscriptions",
    "web3-core",
    "web3-eth-abi",
    "web3-eth-accounts",
    "web3-eth-contract",
    "web3-eth-ens",
    "web3-eth-iban",
    "web3-eth-personal",
    "web3-eth",
    "web3-net",
    "web3-providers-http",
    "web3-providers-ws",
    "web3-utils",
    "web3",
    "webpack-dev-server",
    "wrtc",
  )

  fun unsolvable(groupId: String, artifactId: String): Boolean =
    groupId == "org.webjars.npm" && UNSOLVABLE_ORG_WEBJARS_NPM_ARTIFACTS.contains(artifactId)

  fun skipArtifact(groupId: String, artifactId: String): Boolean {
    // if (groupId.startsWith("com.")) return true
    // if (!groupId.startsWith("org.")) return true
    // if (!groupId.startsWith("io.")) return true
    // if (groupId[0] != 'a') return true

    // if (groupId != "org.apache.camel.quarkus") return true
    // if (!groupId.startsWith("org.kie")) return true
    // if (!groupId.startsWith("org.opendaylight.")) return true

    // if (groupId != "org.integratedmodelling") return true
      // listOf(
      //   "klab-server",
      //   "klab-node",
      // ).contains(artifactId)

    // if (groupId != "org.webjars.npm") return true
    // if (groupId == "org.webjars.npm" && BAD_ORG_WEBJARS_NPM_ARTIFACTS.contains(artifactId)) return true

    return false
  }
}
