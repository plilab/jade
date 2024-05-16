package org.ucombinator.jade.maven

import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader
import org.apache.maven.model.Model
import org.apache.maven.model.building.DefaultModelBuilderFactory
import org.apache.maven.model.building.ModelBuildingRequest
import org.apache.maven.repository.internal.MavenRepositorySystemUtils
import org.eclipse.aether.RepositorySystem
import org.eclipse.aether.RepositorySystemSession
import org.eclipse.aether.artifact.Artifact
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.collection.CollectRequest
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory
import org.eclipse.aether.graph.DependencyNode
import org.eclipse.aether.graph.Exclusion
import org.eclipse.aether.impl.ArtifactResolver
import org.eclipse.aether.impl.MetadataResolver
import org.eclipse.aether.metadata.DefaultMetadata
import org.eclipse.aether.metadata.Metadata
import org.eclipse.aether.metadata.Metadata.Nature
import org.eclipse.aether.repository.LocalRepository
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.repository.RepositoryPolicy
import org.eclipse.aether.resolution.ArtifactDescriptorRequest
import org.eclipse.aether.resolution.ArtifactDescriptorResult
import org.eclipse.aether.resolution.ArtifactRequest
import org.eclipse.aether.resolution.ArtifactResolutionException
import org.eclipse.aether.resolution.ArtifactResult
import org.eclipse.aether.resolution.MetadataRequest
import org.eclipse.aether.resolution.MetadataResult
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory
import org.eclipse.aether.spi.connector.transport.TransporterFactory
import org.eclipse.aether.transfer.MetadataNotFoundException
import org.eclipse.aether.transport.file.FileTransporterFactory
import org.eclipse.aether.transport.http.HttpTransporterFactory
import org.eclipse.aether.util.artifact.JavaScopes
import org.eclipse.aether.util.repository.AuthenticationBuilder
import org.eclipse.aether.util.version.GenericVersionScheme
import org.ucombinator.jade.util.AtomicWriteFile
import org.ucombinator.jade.util.Log
import org.ucombinator.jade.util.Tuples.Fiveple
import org.eclipse.aether.util.graph.transformer.ConflictResolver
import org.eclipse.aether.util.graph.transformer.JavaScopeSelector
import org.eclipse.aether.util.graph.transformer.SimpleOptionalitySelector
import org.eclipse.aether.util.graph.transformer.JavaScopeDeriver
import org.eclipse.aether.util.graph.transformer.ChainedDependencyGraphTransformer
import org.eclipse.aether.util.graph.transformer.JavaDependencyContextRefiner

import org.eclipse.aether.util.graph.transformer.NearestVersionSelector

import java.io.File
import java.io.FileInputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random
import kotlin.text.RegexOption
import kotlinx.coroutines.*

data class CaretInVersionException(val artifact: Artifact) : Exception("Caret in version for artifact $artifact")
data class DollarInCoordinateException(val artifact: Artifact) : Exception("Dollar in coordinate $artifact")
data class DotInGroupIdException(val groupIdPath: String, val artifactId: String) : Exception("File path for groupId contains a dot: $groupIdPath (artifactId = $artifactId)")
data class ModelParsingException(val file: File) : Exception("Could not parse POM file $file")
data class NoVersioningTagException(val groupId: String, val artifactId: String) : Exception("No <versioning> tag in POM for $groupId:$artifactId")
data class NoVersionsInVersioningTagException(val groupId: String, val artifactId: String) : Exception("No versions in POM for for $groupId:$artifactId")
data class UnsolvableArtifactException(val groupId: String, val artifactId: String) : Exception("Skipped artifact with unsolvable dependencies: $groupId:$artifactId")
// data class VersionDoesNotExistException(val artifact: Artifact, val e: ArtifactResolutionException) : Exception("Artifact version does not exist: $artifact", e)
data class ArtifactWriteLockException(val artifact: Artifact, val e: IllegalStateException) : Exception("Could not aquire write lock for $artifact", e)
data class MetadataWriteLockException(val metadata: Metadata, val e: IllegalStateException) : Exception("Could not aquire write lock for $metadata", e)
data class SystemDependencyException(val root: Artifact, val dependency: Artifact) : Exception("Dependency on system provided artifact $dependency by $root")

data class CachedException(val name: String, val stackTrace: String) : Exception("CachedException: $name\n$stackTrace")

// $ find ~/a/local/jade2/maven '(' -name \*.part -o -name \*.lock -o -size 0 ')' -type f -print0 | xargs -0 rm -v
// $ find ~/a/local/jade2/jar-lists/ -size 0 -type f -print0 | xargs -0 rm -v

object Exceptions {
  fun name(exception: Throwable): String {
    var e: Throwable? = exception
    var l = listOf<String>()

    while (e !== null) {
      l += e::class.qualifiedName ?: "<anonymous>"
      e = e.cause
    }

    return l.joinToString(":")
  }

  fun stackTrace(exception: Throwable): String {
    val stringWriter = StringWriter()
    val printWriter = PrintWriter(stringWriter)
    exception.printStackTrace(printWriter)
    printWriter.flush()
    return stringWriter.toString()
  }
}

// TODO: NearestVersionSelectorWrapper (DependencyNodeWrapper) https://kotlinlang.org/docs/delegation.html
class CachingMetadataResolver : MetadataResolver { // TODO: rename to wrapper
  private val log = Log {}

  val cache = Collections.synchronizedMap(object : LinkedHashMap<Fiveple<String, String, String, String, Nature>, Pair<String, String>>(16, 0.75F, true) {
    override fun removeEldestEntry(eldest: Map.Entry<Fiveple<String, String, String, String, Nature>, Pair<String, String>>): Boolean =
      this.size > 100_000
  })

  override fun resolveMetadata(session: RepositorySystemSession, requests: MutableCollection<out MetadataRequest>): List<MetadataResult> =
    requests.map { resolveMetadata(session, it) }

  fun resolveMetadata(session: RepositorySystemSession, request: MetadataRequest): MetadataResult {
    // log.error { "META: $request" }

    val key = Fiveple(
      request.metadata.groupId,
      request.metadata.artifactId,
      request.metadata.version,
      request.metadata.type,
      request.metadata.nature,
    )

    var i = 1
    val maxTries = 10
    while (true) {
      val ex = cache.get(key)
      if (ex !== null) throw CachedException(ex.first, ex.second)

      try {
        val results = defaultMetadataResolver!!.resolveMetadata(session, mutableListOf(request))
        assert(results.size == 1)
        return results[0]
      } catch (e: IllegalStateException) {
        if (!(e.message ?: "").startsWith("Could not acquire write lock for ")) throw e
        if (i == maxTries) {
          log.error("Failed after $i tries: metadata ${request.metadata}")
          throw MetadataWriteLockException(request.metadata, e)
        } else {
          if (i >= 10) log.warn("Retrying ($i of $maxTries) metadata ${request.metadata}")
          Thread.sleep(i * Random.nextLong(500, 1_000))
          i += 1
          continue
        }
      } catch (e: MetadataNotFoundException) {
        // val stringWriter = StringWriter()
        // val printWriter = PrintWriter(stringWriter)
        // e.printStackTrace(printWriter)
        // printWriter.flush()
        // val content =
        //   "!" + exceptionName(e) + "\n" +
        //     stringWriter.toString()
        cache.put(key, Pair(Exceptions.name(e), Exceptions.stackTrace(e)))
        throw e
      }
      // TODO: mark as unreachable
    }
  }

  companion object {
    var defaultMetadataResolver: MetadataResolver? = null
  }
}

class CachingArtifactResolver : ArtifactResolver {
  private val log = Log {}

  // val exists = Collections.synchronizedMap(mutableMapOf<Artifact, Boolean>())
  val cache = Collections.synchronizedMap(object : LinkedHashMap<Fiveple<String, String, String, String, String>, Pair<String, String>>(16, 0.75F, true) {
    override fun removeEldestEntry(eldest: Map.Entry<Fiveple<String, String, String, String, String>, Pair<String, String>>): Boolean =
      this.size > 100_000
  })
  override fun resolveArtifact(session: RepositorySystemSession, request: ArtifactRequest): ArtifactResult {
    if (
      request.artifact.groupId.contains("\${") ||
      request.artifact.artifactId.contains("\${") ||
      request.artifact.version.contains("\${") ||
      request.artifact.classifier.contains("\${") ||
      request.artifact.extension.contains("\${")
    ) throw DollarInCoordinateException(request.artifact)

    if (request.artifact.version.startsWith('^')) throw CaretInVersionException(request.artifact)

    val repositories = request.repositories.mapNotNull {
      when {
        // These repositories have shutdown or do not allow anonymous access
        it.host == "dl.bintray.com" ||
          it.host == "repo.spring.io" ||
          it.host.endsWith(".codehaus.org")
        -> null

        it.url == "http://repo1.maven.org/maven2" ||
          it.url == "http://repo1.maven.org/maven2/"
        -> RemoteRepository.Builder(it).setUrl("https://repo1.maven.org/maven2/").build()

        it.url == "http://repo2.maven.org/maven2" ||
          it.url == "http://repo2.maven.org/maven2/"
        -> RemoteRepository.Builder(it).setUrl("https://repo2.maven.org/maven2/").build()

        it.url == "http://repo.maven.apache.org/maven2" ||
          it.url == "http://repo.maven.apache.org/maven2/"
        -> RemoteRepository.Builder(it).setUrl("https://repo.maven.apache.org/maven2/").build()

        it.url == "http://svn.apache.org/repos/asf/servicemix/m2-repo" ||
          it.url == "http://svn.apache.org/repos/asf/servicemix/m2-repo/"
        -> RemoteRepository.Builder(it).setUrl("https://svn.apache.org/repos/asf/servicemix/m2-repo/").build()

        // See https://netbeans.apache.org/about/oracle-transition.html
        it.url == "http://bits.netbeans.org/maven2" ||
          it.url == "http://bits.netbeans.org/maven2/"
        -> RemoteRepository.Builder(it).setUrl("http://netbeans.apidesign.org/maven2/").build()

        else -> it
      }
    }
    request.repositories = repositories

    val key = Fiveple(
      request.artifact.groupId,
      request.artifact.artifactId,
      request.artifact.baseVersion,
      request.artifact.classifier,
      request.artifact.extension,
    )

    var i = 1
    val maxTries = 10
    while (true) {
      val ex = cache.get(key)
      if (ex !== null) throw CachedException(ex.first, ex.second)

      try {
        return defaultArtifactResolver!!.resolveArtifact(session, request)
      } catch (e: IllegalStateException) {
        if (!(e.message ?: "").startsWith("Could not acquire write lock for ")) throw e
        if (i == maxTries) {
          log.error("Failed to get write lock after $i tries: artifact ${request.artifact}")
          throw ArtifactWriteLockException(request.artifact, e)
        } else {
          if (i >= 10) log.warn("Retrying ($i of $maxTries) artifact ${request.artifact}")
          Thread.sleep(i * Random.nextLong(500, 1_000))
          i += 1
          continue
        }
      } catch (e: ArtifactResolutionException) {
        // if (!exists.containsKey(request.artifact)) {
        //   val url = URL("https://mvnrepository.com/artifact/${request.artifact.groupId}/${request.artifact.artifactId}/${request.artifact.version}")
        //   val connection = url.openConnection() as HttpURLConnection
        //   connection.setRequestMethod("HEAD")
        //   connection.setRequestProperty("User-Agent", "Mozilla/5.0")
        //   val responseCode = connection.responseCode
        //   if (responseCode == HttpURLConnection.HTTP_FORBIDDEN) log.error { "MVNREPOSITORY FORBIDDEN: ${request.artifact} $url" }
        //   else exists.put(request.artifact, responseCode != HttpURLConnection.HTTP_NOT_FOUND)
        // }
        val ee = e // if (exists.getValue(request.artifact)) e else VersionDoesNotExistException(request.artifact, e)
        cache.put(key, Pair(Exceptions.name(e), Exceptions.stackTrace(e)))
        // <tr><th style="width: 12em;">Repositories</th><td><a class="b lic" href="/repos/icm">ICM</a></td></tr>
        throw ee
      }
      // TODO: mark as unreachable
    }
  }

  override fun resolveArtifacts(session: RepositorySystemSession, requests: MutableCollection<out ArtifactRequest>): List<ArtifactResult> =
    requests.map { resolveArtifact(session, it) }
  companion object {
    var defaultArtifactResolver: ArtifactResolver? = null
  }
}

class DownloadMaven(val indexFile: File, val localRepoDir: File, val jarListsDir: File, val reverse: Boolean, val shuffle: Boolean) {
  private val log = Log {}

  val cachedFails = Collections.synchronizedMap(mutableMapOf<String, Int>())
  val fails = Collections.synchronizedMap(mutableMapOf<String, Int>())
  val aborts = Collections.synchronizedMap(mutableMapOf<String, Int>())
  val running = Collections.synchronizedMap(mutableMapOf<Pair<String, String>, Long>())
  val remaining = AtomicInteger()

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
    CachingArtifactResolver.defaultArtifactResolver = locator.getService(ArtifactResolver::class.java)
    locator.setService<ArtifactResolver>(ArtifactResolver::class.java, CachingArtifactResolver::class.java)
    CachingMetadataResolver.defaultMetadataResolver = locator.getService(MetadataResolver::class.java)
    locator.setService<MetadataResolver>(MetadataResolver::class.java, CachingMetadataResolver::class.java)
    locator.setService<MetadataResolver>(MetadataResolver::class.java, CachingMetadataResolver::class.java)
  }

  val system = locator.getService(RepositorySystem::class.java)
  val versionScheme = GenericVersionScheme() // locator.getService(VersionScheme::class.java)
  val modelBuilder = DefaultModelBuilderFactory().newInstance() // TODO: locator
  val session = MavenRepositorySystemUtils.newSession()
  val localRepo = LocalRepository(localRepoDir)
  init {
    session.localRepositoryManager = system.newLocalRepositoryManager(session, localRepo)
    val transformer = ConflictResolver(NearestVersionSelector(), JavaScopeSelector(),
                            SimpleOptionalitySelector(), JavaScopeDeriver())
    val t = ChainedDependencyGraphTransformer(transformer, JavaDependencyContextRefiner())
    session.dependencyGraphTransformer = t
  }

  val remote = RemoteRepository
    .Builder("google-maven-central", "default", "https://maven-central-asia.storage-download.googleapis.com/maven2")
    .setPolicy(RepositoryPolicy(true, RepositoryPolicy.UPDATE_POLICY_NEVER, RepositoryPolicy.CHECKSUM_POLICY_FAIL))
    .build()

  val authentication =
    AuthenticationBuilder()
      .addHostnameVerifier(NoopHostnameVerifier())
      .build()

  val remotes =
    listOf(
      // We do not use springio-* or spring-* repositories as they no longer allow anonymous access.
      // See https://spring.io/blog/2020/10/29/notice-of-permissions-changes-to-repo-spring-io-fall-and-winter-2020

      /* 9.6M uses / 8.7M jars */ "google-maven-central" to "https://maven-central-asia.storage-download.googleapis.com/maven2/",
      /* 137k uses /  32k jars */ "google" to "https://maven.google.com/",
      /*  78k uses / 270k jars */ "wso2-releases" to "https://maven.wso2.org/nexus/content/repositories/releases/",
      /*  68k uses /  30k jars */ "wso2" to "http://dist.wso2.org/maven2/",
      /*  41k uses /  59k jars */ "liferay-public" to "https://repository.liferay.com/nexus/content/repositories/public/",
      /*  33k uses /  13k jars */ "osgeo-release" to "https://repo.osgeo.org/repository/release/",
      /*  12k uses / 280k jars */ "pentaho-omni" to "https://nexus.pentaho.org/content/groups/omni/",
      // /* 5.1k uses /  22k jars */ "ebi-public" to "https://www.ebi.ac.uk/intact/maven/nexus/content/repositories/public/",
      /* 4.0k uses / 145k jars */ "redhat-ga" to "https://maven.repository.redhat.com/ga/",
      /* 3.4k uses / 120k jars */ "geomajas" to "http://maven.geomajas.org/",
      /* 1.8k uses / 8.2k jars */ "typesafe-maven-releases" to "https://repo.typesafe.com/typesafe/maven-releases/",
      /* 1.5k uses / 9.0k jars */ "twitter" to "https://maven.twttr.com/",
      /* 1.4k uses /  41k jars */ "fusesource-releases" to "https://repo.fusesource.com/nexus/content/repositories/releases/",
      /* 1.1k uses /  13k jars */ "confluent-packages" to "https://packages.confluent.io/maven/",
      /* 791  uses /  19  jars */ "wso2-thirdparty" to "https://maven.wso2.org/nexus/content/repositories/thirdparty/",
      /* 638  uses /  21k jars */ "netbeans" to "http://netbeans.apidesign.org/maven2/", // Note: https://netbeans.apache.org/about/oracle-transition.html
      /* 268  uses / 3.9k jars */ "ow2-public" to "https://repository.ow2.org/nexus/content/repositories/public/",
      /* 130  uses / 227k jars */ "wso2-public" to "https://maven.wso2.org/nexus/content/repositories/public/",

      // TODO: maven.java.net
      // TODO: Certificate for .. doesn't match any of the subject ...

      //////////////// broken but possibly fixable

      // "icm" to "https://maven.icm.edu.pl/artifactory/repo/", // 94k jars / null pointer exception / too long to reach?
      // "apache-releases" to "https://repository.apache.org/content/repositories/releases/", // 92k jars / Network is unreachable (connect failed)
      //    commons-*
      //    javax/jdo:*
      //    javax/portlet:portlet-api
      //    log4j:apache-log4j-extras
      //    log4j:log4j
      //    net/jini:jsk-*
      //    net/jini/lookup:serviceui
      //    org/apache*
      //    org/freemarker*
      //    org/netbeans*
      //    org/openoffice*
      //    org/qi4j*
      // "apache-public" to "https://repository.apache.org/content/repositories/public/", // 17k jars / Network is unreachable (connect failed)
      //    commons-*
      //    io/re-digital:*
      //    javax/jdo:*
      //    javax/portlet*
      //    log4j:apache-chainsaw
      //    log4j:apache-log4j-extras
      //    log4j:log4j
      //    net/jini:jsk-*
      //    net/jini/lookup:serviceui
      //    org/apache*
      //    org/cocooon*
      //    org/deri*
      //    org/foo*
      //    org/freemarker*
      //    org/netbeans*
      //    org/openoffice*
      //    org/qi4j*
      //    org/swssf*

      //////////////// less than 35 uses

      // /*  32 uses / 3.6k jars */ "geo-solutions" to "http://maven.geo-solutions.it/",
      // /*  26 uses /  21k jars */ "eclipse-releases" to "https://repo.eclipse.org/content/groups/releases/",
      // /*  20 uses / 280  jars */ "cit-ec-releases" to "https://mvn.cit-ec.de/nexus/content/repositories/releases/",
      // /*  19 uses /  10k jars */ "conjars" to "https://conjars.org/repo/",
      // /*  18 uses / 142k jars */ "nuxeo-public-releases" to "https://maven-eu.nuxeo.org/nexus/content/repositories/public-releases/",
      // /*   7 uses /  72k jars */ "mulesoft-public" to "https://repository.mulesoft.org/nexus/content/repositories/public/",
      // /*   2 uses / 112  jars */ "edinburgh-ph" to "https://www2.ph.ed.ac.uk/maven2/",

      //////////////// 0 uses

      // /*   0 uses / 2.1M jars */ "sonatype-releases" to "https://oss.sonatype.org/content/repositories/releases/",
      // /*   0 uses / 273k jars */ "jboss-releases" to "https://repository.jboss.org/nexus/content/repositories/releases/",
      // /*   0 uses /  22k jars */ "osgeo" to "https://download.osgeo.org/webdav/geotools/",
      // /*   0 uses / 6.4k jars */ "jboss-thirdparty-releases" to "https://repository.jboss.org/nexus/content/repositories/thirdparty-releases/",
      // /*   0 uses / 3.7k jars */ "seasar" to "https://maven.seasar.org/maven2/",
      // /*   0 uses / 1.4k jars */ "unidata-ucar-releases" to "https://artifacts.unidata.ucar.edu/content/repositories/unidata-releases/",
      // /*   0 uses / 1.2k jars */ "pustefix-framework" to "http://pustefix-framework.org/repository/maven/",
      // /*   0 uses /  96  jars */ "tidalwave-releases" to "https://services.tidalwave.it/nexus/content/repositories/releases/",
      // /*   0 uses /  67  jars */ "eclipse-paho" to "https://repo.eclipse.org/content/repositories/paho-releases/",
      // /*   0 uses /   4  jars */ "tweetyproject" to "https://tweetyproject.org/mvn/",

      //////////////// unreachable servers

      // /*   ? uses/  10k jars */ "jspresso" to "http://repository.jspresso.org/maven2/",
      // /*   ? uses/ 3.9k jars */ "metova-public" to "http://repo.metova.com/nexus/content/groups/public/",
      // /*   ? uses/ 1.9k jars */ "atricore-m2-release-repository" to "http://repository.atricore.org/m2-release-repository/",
      // /*   ? uses/ 1.3k jars */ "entwine-releases" to "http://maven.entwinemedia.com/content/repositories/releases/",

    ).map {
      RemoteRepository.Builder(it.first, "default", it.second)
        .setPolicy(RepositoryPolicy(true, RepositoryPolicy.UPDATE_POLICY_NEVER, RepositoryPolicy.CHECKSUM_POLICY_FAIL))
        .setAuthentication(authentication)
        .build()
    }

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
            println("$value\t$key")
          }

          println()
          println("Running: ${running.size} / $remaining\n")
          for ((name, startTime) in running.toList().sortedBy(Pair<Pair<String, String>, Long>::second).reversed()) {
            println("- ${time(startTime)}: ${name.first}:${name.second}")
          }
        }
      }
    )

    MavenRepo.listArtifacts(indexFile) { artifacts ->
      runBlocking {
        val r = if (shuffle) artifacts.shuffled() else artifacts
        val reorderedArtifacts = if (reverse) r.toList().reversed().asSequence() else r
        for ((groupIdPath, artifactId) in reorderedArtifacts) {
          // if (groupIdPath[0] != 'o') continue
          // if (groupIdPath >= "com") continue
          // if (!groupIdPath.startsWith("com/")) continue
          // if (groupIdPath.startsWith("com/github/")) continue
          // if (groupIdPath.startsWith("io/")) continue
          // if (groupIdPath.startsWith("net/")) continue
          // if (!groupIdPath.startsWith("org/")) continue
          // if (groupIdPath == "org/integratedmodelling") continue
          // if (groupIdPath >= "org/z") continue
          // if (groupIdPath.startsWith("org/open")) continue // openidentity
          // if (groupIdPath < "org/oj") continue
          // if (!groupIdPath.startsWith("org/open")) continue
          // if (groupIdPath.startsWith("org/open") && groupIdPath != "org/opencadc") continue
          // if (artifactId == "cadc-test-uws") continue
          // if (artifactId == "caom2-datalink-server") continue
          // if (artifactId == "caom2-meta-server") continue
          // if (artifactId == "caom2-pkg-server") continue
          // if (artifactId == "caom2-search-server") continue
          // if (artifactId == "caom2-soda-server") continue

          // if (artifactId == "caom2harvester") continue

          // if (groupIdPath.startsWith("org/open") && groupIdPath >= "org/opend") continue
          // if (groupIdPath.startsWith("org/open") && groupIdPath >= "org/opencb") continue 
          // if (groupIdPath.startsWith("org/o")) continue
          // if (!groupIdPath.startsWith("org/p")) continue
          // if (!groupIdPath.startsWith("org/q")) continue
          // if (!groupIdPath.startsWith("org/r")) continue
          // if (!groupIdPath.startsWith("org/s")) continue
          // if (groupIdPath >= "org/o" && groupIdPath < "org/t") continue
          // if (groupIdPath.startsWith("org/ow2/")) continue
          // if (groupIdPath.startsWith("org/wso2/")) continue
          // if (groupIdPath.startsWith("org/webjars/")) continue
          // if (groupIdPath.startsWith("org/opendaylight/")) continue
          // if (groupIdPath == "me/phoboslabs/illuminati" && artifactId == "illuminati-processor") continue

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

          remaining.incrementAndGet()
          async(Dispatchers.IO) { collectDependencies(groupIdPath, artifactId, jarListFile) }
        }
      }
    }
  }
  // TODO: clean repo .lock and .part files before starting

  fun time(startTime: Long): String = "%.2f".format((System.nanoTime() - startTime).toDouble() / 1e9)

  fun collectDependencies(groupIdPath: String, artifactId: String, jarListFile: File) {
    val groupId = groupIdPath.replace('/', '.')
    val name = "$groupId:$artifactId"
    val startTime = System.nanoTime()
    try {
      running.put(Pair(groupIdPath, artifactId), startTime)
      println("running +${running.size} / $remaining")
      if (groupIdPath.contains('.')) throw DotInGroupIdException(groupIdPath, artifactId) // Maven will incorrectly translate the '.' to a '/'
      println("start   $name")
      val version = getVersion(groupId, artifactId)
      val artifactDescriptorResult = getArtifactDescriptor(groupId, artifactId, version)
      val pomArtifactResult = getArtifact(groupId, artifactId, null, "pom", version)
      val model = getModel(pomArtifactResult.artifact.file)
      val artifactResult = getArtifact(groupId, artifactId, artifactDescriptorResult.artifact.classifier, model.packaging, version)
      artifactDescriptorResult.artifact = artifactResult.artifact
      artifactDescriptorResult.request.artifact = artifactResult.artifact
      val dependencyTree = getDependencyTree(artifactDescriptorResult)
      val dependencyList = getDependencyList(dependencyTree)
      val artifactResults = downloadDependencies(dependencyList)
      writeJarList(artifactResults, jarListFile)
      pass.incrementAndGet()
      println("pass  ${time(startTime)} $name")
    } catch (e: Exception) {
      val t = time(startTime)
      if (isFailure(e)) {
        println("fail  $t $name")
        fail.incrementAndGet()
        fails.merge(exceptionName(e), 1, Int::plus)
        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)
        e.printStackTrace(printWriter)
        printWriter.flush()
        val content =
          "!" + exceptionName(e) + "\n" +
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
      remaining.decrementAndGet()
      println("running -${running.size} / $remaining")
    }
  }

  fun isChecksumFailure(e: Throwable?): Boolean =
    when (e) {
      null -> false
      is org.eclipse.aether.transfer.ChecksumFailureException ->
        e.message != "Checksum validation failed, no checksums available"
      else -> isChecksumFailure(e.cause)
    }

  fun isFailure(e: Throwable): Boolean =
    isChecksumFailure(e) ||
      isA(
        e,
        org.eclipse.aether.transfer.ArtifactTransferException::class,
        org.eclipse.aether.transfer.NoRepositoryConnectorException::class,
        org.eclipse.aether.transfer.NoRepositoryConnectorException::class,
        org.eclipse.aether.transfer.NoTransporterException::class
      ) ||
      // isA(
      //   e,
      //   org.eclipse.aether.collection.UnsolvableVersionConflictException::class,
      // ) ||
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
      e is CaretInVersionException ||
      e is DollarInCoordinateException || // TODO
      e is DotInGroupIdException ||
      e is ModelParsingException ||
      e is NoVersioningTagException ||
      e is NoVersionsInVersioningTagException ||
      e is SystemDependencyException
      // e is VersionDoesNotExistException // TODO

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
    if (m.versioning === null) throw NoVersioningTagException(groupId, artifactId)
    if (m.versioning.release !== null) return m.versioning.release
    if (m.versioning.latest !== null) return m.versioning.latest
    val versions = m.versioning.versions
    return versions.maxByOrNull(versionScheme::parseVersion) ?: throw NoVersionsInVersioningTagException(groupId, artifactId)
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

      listOf(
        "configuration",
        "countersconf",
        "datastore",
        "restconf",
      ).contains(classifier)
      -> Pair(classifier, listOf("cfg"))

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
        "config-He",
      ).contains(classifier)
      -> Pair(classifier, listOf("xml"))

      else -> Pair(classifier, listOf(extension, "jar", "zip"))
    }

    var firstException: Throwable? = null
    for (ext in extensions) {
      val artifact = DefaultArtifact(groupId, artifactId, cls, ext, version)
      val artifactRequest = ArtifactRequest(artifact, remotes, null)
      try {
        return system.resolveArtifact(session, artifactRequest)
      } catch (e: org.eclipse.aether.resolution.ArtifactResolutionException) {
        firstException = firstException ?: e
      } catch (e: CachedException) {
        firstException = firstException ?: e
      }
    }
    throw firstException!!
  }

  fun getDependencyTree(descriptorResult: ArtifactDescriptorResult): DependencyNode {
    if (unsolvable(descriptorResult.artifact.groupId, descriptorResult.artifact.artifactId)) {
      throw UnsolvableArtifactException(descriptorResult.artifact.groupId, descriptorResult.artifact.artifactId)
    }

    val collectRequest = CollectRequest()
    // collectRequest.setRoot(Dependency(descriptorResult.artifact, JavaScopes.COMPILE))
    collectRequest.rootArtifact = descriptorResult.artifact
    collectRequest.dependencies = descriptorResult.dependencies
    collectRequest.managedDependencies = descriptorResult.managedDependencies
    collectRequest.repositories = remotes
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
        JavaScopes.SYSTEM -> throw SystemDependencyException(root.artifact, node.artifact)
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
    return artifactRequests.map { getArtifact(it._1, it._2, it._3, it._4, it._5) }
  }

  fun writeJarList(artifactResults: List<ArtifactResult>, jarListFile: File) {
    val builder = StringBuilder()
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
