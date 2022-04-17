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

data class DollarInCoordinateException(val artifact: Artifact) : Exception("Dollar in coordinate ${artifact}")
data class CaretInVersionException(val artifact: Artifact) : Exception("Caret in version for artifact ${artifact}")
data class NoSuchVersionException(val artifact: Artifact) : Exception("No such version for artifact ${artifact}")
data class DotInGroupIdException(val groupIdPath: String, val artifactId: String) : Exception("File path for groupId contains a dot: $groupIdPath (artifactId = $artifactId)")
data class NoVersioningException(val groupId: String, val artifactId: String) : Exception("No <versioning> tag in POM for $groupId:$artifactId")
data class NoVersionsException(val groupId: String, val artifactId: String) : Exception("No versions in POM for for $groupId:$artifactId")
data class UnsolvableArtifactException(val groupId: String, val artifactId: String) : Exception("Skipped artifact with unsolvable dependencies: $groupId:$artifactId")
data class WriteLockException(val artifact: Artifact, val e: IllegalStateException) : Exception("Could not aquire write lock for ${artifact}", e)
data class ModelParsingException(val file: File) : Exception("Could not parse POM file $file")
data class SystemDependencyException(
  val rootGroupId: String, val rootArtifactId: String, val rootVersion: String,
  val dependencyGroupId: String, val dependencyArtifactId: String, val dependencyVersion: String) :
  Exception("Dependency on system provided artifact $dependencyGroupId:$dependencyArtifactId:$dependencyVersion by $rootGroupId:$rootArtifactId:$rootVersion")

class CachingArtifactResolver() : ArtifactResolver {
  private val log = Log {}

  val cache = Collections.synchronizedMap(mutableMapOf<Artifact, ArtifactResolutionException>())
  override fun resolveArtifact(session: RepositorySystemSession, request: ArtifactRequest): ArtifactResult {
    if (
      request.artifact.groupId.contains("\${") ||
      request.artifact.artifactId.contains("\${") ||
      request.artifact.version.contains("\${") ||
      request.artifact.classifier.contains("\${") ||
      request.artifact.extension.contains("\${"))
      throw DollarInCoordinateException(request.artifact)

    if (request.artifact.version.startsWith('^')) throw CaretInVersionException(request.artifact)

    if (setOf(
      "org.fusesource.leveldbjni:leveldbjni-all:pom:1.8-odl", // no such version
      "com.github.romix:java-concurrent-hash-trie-map:pom:0.2.23-ODL", // no such version
      "equinoxSDK381:org.eclipse.osgi:pom:3.8.1.v20120830-144521", // no such artifact
      "equinoxSDK381:javax.servlet:pom:3.0.0.v201112011016", // no such artifact

      "cn.com.antcloud.api:antcloud-prod-api-provider-sdk:pom:2.3.0.20180705", // no such artifact
      "com.baggonius.gson:guava-gson-serializers:pom:1.0.1", // no such artifact
      "com.github.crykn:kryonet:pom:2.22.7", // to such artifact
      "com.github.liaomengge:base-platform-bom:pom:1.0.3-SNAPSHOT", // no such version
      "com.kenai.nbpwr:com-eaio-uuid:pom:2.1.5-201007271250", // http error 404
      "com.palantir.atlasdb:atlasdb:pom:0.586.0", // no such artifact
      "com.walterjwhite:parent:pom:0.0.15-SNAPSHOT", // no such version
      "com.wudgaby.platform:basis-parent:pom:1.0.0", // no such artifact
      "commons-jelly:commons-jelly:pom:SNAPSHOT", // no such version
      "de.citec.csra:rta-lib:pom:1.4.0", // no such version
      "io.flutter:flutter_embedding_release:pom:1.0.0-bdc9708d235e582483d299642ad8682826ebb90d", // no such version
      "javax.transaction:javax.transaction-api:pom:1.2.1", // no such version
      "javax.xml.bind:jaxb-api:pom:2.4.0", // no such version
      "jline:jline:pom:0.9.95.20100209", // no such version
      "org.activiti:activiti-dependencies:pom:7.0.0-SNAPSHOT", // no such version
      "org.apache.kylin:kylin-shaded-guava:pom:3.1.0", // no such artifact
      "org.apache.stratos:stratos-parent:pom:3.0.0-incubating", // no such version
      "org.bithon.shaded:shaded-slf4j:pom:1.0-SNAPSHOT", // no such artifact
      "org.eclipse:draw2d:pom:3.6.1", // no such version
      "org.eclipse.app4mc.migration:plugins:pom:2.0.0", // no such artifact
      "org.eclipse.core:org.eclipse.core.runtime:pom:3.11.1", // no such version
      "org.eclipse.jdt:core:pom:3.3.0.771", // no such version
      "org.eclipse.osgi:org.eclipse.osgi:pom:3.7.1.R37x_v20110808_1106", // no such version
      "org.geotools:gt-api:pom:10.0", // http error 404
      "org.glassfish.jersey:jersey-bom:pom:3.1.0-SNAPSHOT", // no such version
      "org.glassfish.tyrus:tyrus-bom:pom:2.1.0-M2", // no such version
      "org.oasis-open.sca.j:sca-caa-apis:pom:1.1-CD04", // no such artifact
      "org.ogema.tools:ogema-tools:pom:2.2.1-SNAPSHOT", // no such version
      "org.openidentityplatform.external.com.iplanet.jato:jato:pom:14.6.4", // no such artifact
      "org.openjfx:javafx.base:jar:11.0.0-SNAPSHOT", // no such version
      "org.restcomm.media:media-parent:pom:8.0.0-SNAPSHOT", // no such version
      "org.semweb4j:rdf2go.api:pom:4.7.2", // no such version
      "org.tinygroup:tiny:pom:3.4.9_1", // no such version
      "vigna.dsi.unimi.it:jal:pom:20031117", // no such artifact
    ).contains(
      "${request.artifact.groupId}:${request.artifact.artifactId}:${request.artifact.extension}:${request.artifact.version}"
    ))
      throw NoSuchVersionException(request.artifact)

    var i = 1
    val maxTries = 30
    while (true) {
      val ex = cache.get(request.artifact)
      if (ex !== null) throw ex

      try {
        return defaultArtifactResolver!!.resolveArtifact(session, request)
      } catch (e: ArtifactResolutionException) {
        cache.put(request.artifact, e)
        throw e
      } catch (e: IllegalStateException) {
        if (!(e.message ?: "").startsWith("Could not acquire write lock for ")) throw e
        if (i == maxTries) throw WriteLockException(request.artifact, e)
        else {
          if (i > 5) log.warn("Retrying ($i of $maxTries) artifact ${request.artifact}: ${request.trace}")
          Thread.sleep(Random.nextLong(1_000, 3_000))
          i += 1
          continue
        }
      }
      // TODO: mark as unreachable
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

  val cachedFails = Collections.synchronizedMap(mutableMapOf<String, Int>())
  val fails = Collections.synchronizedMap(mutableMapOf<String, Int>())
  val aborts = Collections.synchronizedMap(mutableMapOf<String, Int>())
  val running = Collections.synchronizedMap(mutableMapOf<Pair<String, String>, Long>())

  val cachedPass = AtomicInteger()
  val cachedFail = AtomicInteger()
  val pass = AtomicInteger()
  val fail = AtomicInteger()
  val abort = AtomicInteger()

  val locator = MavenRepositorySystemUtils.newServiceLocator()

  init {
    locator.addService(RepositoryConnectorFactory::class.java, BasicRepositoryConnectorFactory::class.java)
    locator.addService(TransporterFactory::class.java, FileTransporterFactory::class.java)
    locator.addService(TransporterFactory::class.java, HttpTransporterFactory::class.java)
    // locator.addService(TransporterFactory::class.java, GcsTransporterFactory::class.java)
    CachingArtifactResolver.defaultArtifactResolver = locator.getService(ArtifactResolver::class.java)
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
  // 41k -> 34k (google) -> 31k (wso2) -> 28.2k (jboss) -> 26.0k (spring) -> 27.7k (no spring) -> 27.0k (ebi-public)
  // 27.5k (reset) -> 26.9k (redhat-gc)
  // (reset) -> (apache)

//  133 org.eclipse.stp.sca.osoa.java:osoa-java-api:pom:2.0.1.2 (ow2-public: error 404)
//  143 com.kenai.nbpwr:com-eaio-uuid:pom:2.1.5-201007271250 (tidalwave: error 404)

//  101 org.tinygroup:tiny:pom:3.4.9_1 (no such version)
//  101 org.wso2.carbon:org.wso2.carbon.core:pom:4.5.3 (wso2-releases)
//  127 org.jboss.ws:jbossws-parent:pom:1.0.2.GA (jboss-releases/mulesoft)
//  149 com.walterjwhite:parent:pom:0.0.15-SNAPSHOT (no such version)
//  177 org.openjfx:javafx.base:jar:11.0.0-SNAPSHOT (no such version)
//  188 com.entwinemedia.common:functional:pom:1.4.2 (no such server)
//  206 org.glassfish.tyrus:tyrus-bom:pom:2.1.0-M2 (no such version)
//  211 org.webjars.bower:github-com-webcomponents-shadycss:pom:^v1.1.0 (malformed version)
//  321 org.eclipse.core:org.eclipse.core.runtime:pom:3.11.1 (too new)


  val remotes =
    listOf(
      "google-maven-central" to "https://maven-central-asia.storage-download.googleapis.com/maven2/", // 8.7M jars

      "google" to "https://maven.google.com/", // 32k jars
      //   // android/
      //   // androidx/
      //   // com/android/
      //   // com/crashlytics/sdk/android/
      //   // com/google/
      //   // com/googlecode/
      //   // io/fabric/sdk/android/
      //   // org/chromium/

      "wso2" to "http://dist.wso2.org/maven2/", // 30k jars
      "wso2-releases" to "https://maven.wso2.org/nexus/content/repositories/releases/", // 270k jars
      "wso2-public" to "https://maven.wso2.org/nexus/content/repositories/public/", // 227k jars
      "wso2-thirdparty" to "https://maven.wso2.org/nexus/content/repositories/thirdparty/", // 19 jars

      "jboss-releases" to "https://repository.jboss.org/nexus/content/repositories/releases/", // 273k jars
      "jboss-thirdparty-releases" to "https://repository.jboss.org/nexus/content/repositories/thirdparty-releases/", // 6.4k jars

      // See https://spring.io/blog/2020/10/29/notice-of-permissions-changes-to-repo-spring-io-fall-and-winter-2020
      // // "springio-plugins-release" to "https://repo.spring.io/plugins-release/", // 1.4M jars
      // // "spring-libs-milestone" to "https://repo.spring.io/libs-milestone/", // 1.3M jars

      // // "ebi-public" to "https://www.ebi.ac.uk/intact/maven/nexus/content/repositories/public/", // 22k jars

      // // "redhat-ga" to "https://maven.repository.redhat.com/ga/", // 145k jars

      // // "apache-releases" to "https://repository.apache.org/content/repositories/releases/", // 92k jars
      // // "apache-public" to "https://repository.apache.org/content/repositories/public/", // 17k jars

      "osgeo-release" to "https://repo.osgeo.org/repository/release/", // 13k jars
      "osgeo" to "https://download.osgeo.org/webdav/geotools/", // 22k jars

      // // "seasar" to "https://www.seasar.org/maven/maven2/", // 3.7k jars

      // // "netbeans" to "http://netbeans.apidesign.org/maven2/", // 21k jars; Note that old netbeans repo was decommissioned (see https://netbeans.apache.org/about/oracle-transition.html)

      // // "sonatype-releases" to "https://oss.sonatype.org/content/repositories/releases/", // 2.1M jars

      "geomajas" to "http://maven.geomajas.org/", // 120k jars

      "eclipse-releases" to "https://repo.eclipse.org/content/groups/releases/", // 21k jars

      // // "icm" to "http://maven.icm.edu.pl/artifactory/repo/", // 94k jars

      // // "ow2-public" to "https://repository.ow2.org/nexus/content/repositories/public/", // 3.9k jars

      // // "tidalwave-releases" to "http://services.tidalwave.it/nexus/content/repositories/releases/", // 96 jars
      // // "typesafe-maven-releases" to "https://repo.typesafe.com/typesafe/maven-releases/", // 8.2k jars

      "mulesoft-public" to "https://repository.mulesoft.org/nexus/content/repositories/public/", // 72k jars
      "liferay-public" to "https://repository.liferay.com/nexus/content/repositories/public/", // 59k jars
      // // "fusesource-releases" to "https://repo.fusesource.com/nexus/content/repositories/releases/", // 41k jars
      // // "conjars" to "https://conjars.org/repo/", // 10k jars
      // // (no server) "metova-public" to "http://repo.metova.com/nexus/content/groups/public/", // 3.9k jars
      // // "geo-solutions" to "http://maven.geo-solutions.it/", // 3.6k jars
      // // (no server) "entwine-releases" to "http://maven.entwinemedia.com/content/repositories/releases/", // 1.3k jars
      "eclipse-paho" to "https://repo.eclipse.org/content/repositories/paho-releases/", // 67 jars
      // "scalaz-releases" to "https://dl.bintray.com/scalaz/releases/", // 28 jars
      // "tweetyproject" to "https://tweetyproject.org/mvn/", // 4 jars
      "pentaho-omni" to "https://nexus.pentaho.org/content/groups/omni/", // 280k jars
      "pustefix-framework" to "http://pustefix-framework.org/repository/maven/", // 1.2k jars
      "twitter" to "https://maven.twttr.com/", // 9.0k jars
      "edinburgh-ph" to "https://www2.ph.ed.ac.uk/maven2/", // 112 jars
      "confluent-packages" to "https://packages.confluent.io/maven/", // 13k jars
      // "atricore-m2-release-repository" to "http://repository.atricore.org/m2-release-repository/", // 1.9k jars
      "nuxeo-public-releases" to "https://maven-eu.nuxeo.org/nexus/content/repositories/public-releases/", // 142k jars
      // "jspresso" to "http://repository.jspresso.org/maven2/", // 10k jars
      "unidata-ucar-releases" to "https://artifacts.unidata.ucar.edu/content/repositories/unidata-releases/", // 1.4k jars
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
          println("cached pass $cachedPass")
          println("cached fail $cachedFail")
          println("pass  $pass")
          println("fail  $fail")
          println("abort $abort")

          println()
          println("Cached fails: ${cachedFails.toList().map(Pair<String, Int>::second).sum()}\n")
          for ((key, value) in cachedFails.toList().sortedBy(Pair<String, Int>::first).sortedBy(Pair<String, Int>::second)) {
            println("$value\t$key")
          }

          println()
          println("Fails: ${fails.toList().map(Pair<String, Int>::second).sum()}\n")
          for ((key, value) in fails.toList().sortedBy(Pair<String, Int>::first).sortedBy(Pair<String, Int>::second)) {
            println("$value\t$key")
          }

          println()
          println("Aborts: ${aborts.toList().map(Pair<String, Int>::second).sum()}\n")
          for ((key, value) in aborts.toList().sortedBy(Pair<String, Int>::first).sortedBy(Pair<String, Int>::second)) {
            if (key.contains("org.eclipse.aether.resolution.ArtifactResolutionException")) log.error { "$value\t$key" }
            else println("$value\t$key")
          }

          println()
          println("Running: ${running.size}\n")
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
          // if (groupIdPath[0] != 'o') continue
          // if (!groupIdPath.startsWith("com/")) continue
          // if (groupIdPath.startsWith("com/github/")) continue
          // if (groupIdPath.startsWith("io/")) continue
          // if (groupIdPath.startsWith("net/")) continue
          // if (groupIdPath.startsWith("org/")) continue
          if (groupIdPath == "org/integratedmodelling") continue
          if (groupIdPath.startsWith("org/opendaylight/")) continue
          if (groupIdPath == "me/phoboslabs/illuminati" && artifactId == "illuminati-processor") continue

          val jarListFile = jarListsDir.resolve(groupIdPath).resolve("$artifactId.jar-list")
          if (jarListFile.exists()) {
            jarListFile.bufferedReader().use {
              val line = it.readLine()
              when {
                line === null -> log.error("!!!! Empty cache file: $jarListFile")
                line.startsWith('!') -> {
                  cachedFail.incrementAndGet()
                  cachedFails.merge(line.substring(1), 1, Int::plus)
                }
                else -> cachedPass.incrementAndGet()
              }
            }
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
    try {
      running.put(Pair(groupIdPath, artifactId), startTime)
      println("running +${running.size}")
      if (groupIdPath.contains('.')) throw DotInGroupIdException(groupIdPath, artifactId) // Maven will incorrectly translate the '.' to a '/'
      println("start   $name")
      // println("start  $i $name")
      val version = getVersion(groupId, artifactId)
      val artifactDescriptorResult = getArtifactDescriptor(groupId, artifactId, version)
      val pomArtifactResult = getArtifact(groupId, artifactId, null, "pom", version)
      val model = getModel(pomArtifactResult.artifact.file)
      val artifactResult = getArtifact(groupId, artifactId, artifactDescriptorResult.artifact.classifier, model.packaging, version)
      artifactDescriptorResult.setArtifact(artifactResult.artifact)
      artifactDescriptorResult.request.setArtifact(artifactResult.artifact)
      val dependencyTree = getDependencyTree(artifactDescriptorResult)
      val dependencyList = getDependencyList(dependencyTree)
      val artifactResults = downloadDependencies(dependencyList)
      writeJarList(startTime, artifactResults, jarListFile)
      pass.incrementAndGet()
      println("pass  ${time(startTime)} $name")
    } catch (e: Exception) {
      val t = time(startTime)
      if (isfailure(e)) {
        println("fail  $t $name")
        fail.incrementAndGet()
        fails.merge(exceptionName(e), 1, Int::plus)
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
        println("abort $t $name")
        abort.incrementAndGet()
        aborts.merge(exceptionName(e), 1, Int::plus)
        log.error(e) { "!!!!!!! $groupId:$artifactId" }
      }
    } finally {
      running.remove(Pair(groupIdPath, artifactId))
      println("running -${running.size}")
    }
  }

  fun isfailure(e: Throwable) =
    // isA(
    //   e,
    //   org.eclipse.aether.transfer.MetadataTransferException::class,
    //   org.eclipse.aether.transfer.ChecksumFailureException::class,
    // ) ||
    // isA(
    //   e,
    //   org.eclipse.aether.transfer.ArtifactTransferException::class,
    //   org.eclipse.aether.transfer.ChecksumFailureException::class,
    // ) ||
    // isA(
    //   e,
    //   org.eclipse.aether.transfer.ChecksumFailureException::class,
    // ) ||
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
      org.eclipse.aether.transfer.NoRepositoryConnectorException::class,
      org.eclipse.aether.transfer.NoRepositoryConnectorException::class,
      org.eclipse.aether.transfer.NoRepositoryLayoutException::class,
      org.eclipse.aether.transfer.NoRepositoryLayoutException::class,
    ) ||
    e is DotInGroupIdException ||
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
      extension == "eclipse-target-definition" -> Pair(classifier, listOf("target"))
      classifier == "config" -> Pair(classifier, listOf("cfg", "xml"))
      classifier == "plugin2asciidoc" -> Pair(classifier, listOf("xsl"))
      classifier == "idmtool" -> Pair(classifier, listOf("py"))

      classifier == "configuration" -> Pair(classifier, listOf("cfg"))
      classifier == "countersconf" -> Pair(classifier, listOf("cfg"))
      classifier == "datastore" -> Pair(classifier, listOf("cfg"))
      classifier == "restconf" -> Pair(classifier, listOf("cfg"))

      listOf(
        "aaa-app-config",
        "aaa-datastore-config",
        "aaa-password-service-config",
        "akkaconf",
        "bgp-initial-config",
        "factoryakkaconf",
        "features-core",
        "features",
        "legacyConfig",
        "moduleconf",
        "moduleshardconf",
        "network-topology-bgp-initial-config",
        "network-topology-pcep-initial-config",
        "odl-bmp-monitors-config",
        "routing-policy-default-config",
        "network-topology-initial-config",
        "configstats",
      ).contains(classifier)
        -> Pair(classifier, listOf("xml"))
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
      val pomArtifactResult = getArtifact(node.artifact.groupId, node.artifact.artifactId, null, "pom", node.artifact.version)
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
}
