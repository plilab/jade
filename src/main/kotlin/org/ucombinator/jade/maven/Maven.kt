package org.ucombinator.jade.maven

import org.eclipse.aether.RepositorySystemSession
import org.eclipse.aether.impl.ArtifactResolver
import org.eclipse.aether.impl.MetadataResolver
import org.eclipse.aether.impl.OfflineController
import org.eclipse.aether.impl.RemoteRepositoryFilterManager
import org.eclipse.aether.impl.RemoteRepositoryManager
import org.eclipse.aether.impl.RepositoryConnectorProvider
import org.eclipse.aether.impl.RepositoryEventDispatcher
import org.eclipse.aether.impl.UpdateCheckManager
import org.eclipse.aether.impl.VersionResolver
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.repository.RepositoryPolicy
import org.eclipse.aether.repository.LocalRepository
import org.eclipse.aether.resolution.ArtifactRequest
import org.eclipse.aether.resolution.ArtifactResolutionException
import org.eclipse.aether.resolution.ArtifactResult
import org.eclipse.aether.spi.io.FileProcessor
import org.eclipse.aether.spi.resolution.ArtifactResolverPostProcessor
import org.eclipse.aether.spi.synccontext.SyncContextFactory
import org.eclipse.aether.supplier.RepositorySystemSupplier
import org.eclipse.aether.artifact.Artifact
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.metadata.Metadata
import org.eclipse.aether.DefaultRepositorySystemSession
import org.ucombinator.jade.util.Exceptions
import org.ucombinator.jade.util.Log
import org.ucombinator.jade.util.Parallel
import org.ucombinator.jade.util.Tuples.Fiveple
import org.eclipse.aether.resolution.MetadataRequest
import org.eclipse.aether.resolution.MetadataResult
import org.eclipse.aether.transfer.MetadataNotFoundException

import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger
import java.io.File
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.toJavaDuration

/** TODO:doc.
 *
 * @property artifact TODO:doc
 */
data class CaretInVersionException(val artifact: Artifact) :
  Exception("Caret in version for artifact $artifact")

/** TODO:doc.
 *
 * @property artifact TODO:doc
 */
data class DollarInCoordinateException(val artifact: Artifact) :
  Exception("Dollar in coordinate $artifact")

/** TODO:doc.
 *
 * @property groupIdPath TODO:doc
 * @property artifactId TODO:doc
 */
data class DotInGroupIdException(val groupIdPath: String, val artifactId: String) :
  Exception("File path for groupId contains a dot: $groupIdPath (artifactId = $artifactId)")

/** TODO:doc.
 *
 * @property file TODO:doc
 */
data class ModelParsingException(val file: File) :
  Exception("Could not parse POM file $file")

/** TODO:doc.
 *
 * @property groupId TODO:doc
 * @property artifactId TODO:doc
 */
data class NoVersioningTagException(val groupId: String, val artifactId: String) :
  Exception("No <versioning> tag in POM for $groupId:$artifactId")

/** TODO:doc.
 *
 * @property groupId TODO:doc
 * @property artifactId TODO:doc
 */
data class NoVersionsInVersioningTagException(val groupId: String, val artifactId: String) :
  Exception("No versions in POM for for $groupId:$artifactId")

/** TODO:doc.
 *
 * @property groupId TODO:doc
 * @property artifactId TODO:doc
 */
data class UnsolvableArtifactException(val groupId: String, val artifactId: String) :
  Exception("Skipped artifact with unsolvable dependencies: $groupId:$artifactId")
// data class VersionDoesNotExistException(val artifact: Artifact, val e: ArtifactResolutionException) :
//   Exception("Artifact version does not exist: $artifact", e)

/** TODO:doc.
 *
 * @property artifact TODO:doc
 * @property e TODO:doc
 */
data class ArtifactWriteLockException(val artifact: Artifact, val e: IllegalStateException) :
  Exception("Could not aquire write lock for $artifact", e)

/** TODO:doc.
 *
 * @property metadata TODO:doc
 * @property e TODO:doc
 */
data class MetadataWriteLockException(val metadata: Metadata, val e: IllegalStateException) :
  Exception("Could not aquire write lock for $metadata", e)

/** TODO:doc.
 *
 * @property root TODO:doc
 * @property dependency TODO:doc
 */
data class SystemDependencyException(val root: Artifact, val dependency: Artifact) :
  Exception("Dependency on system provided artifact $dependency by $root")

/** TODO:doc.
 *
 * @property name TODO:doc
 * @property stackTrace TODO:doc
 */
data class CachedException(val name: String, val stackTrace: String) :
  Exception("CachedException: $name\n$stackTrace")

// TODO: add command to clear locks
// $ find ~/a/local/jade2/maven '(' -name \*.part -o -name \*.lock -o -size 0 ')' -type f -print0 | xargs -0 rm -v
// $ find ~/a/local/jade2/jar-lists/ -size 0 -type f -print0 | xargs -0 rm -v

// TODO: add sizes (from index) to starting and ending outputs


/** TODO:doc. */
private typealias CacheKey<T> = Fiveple<String, String, String, String, T>

/** TODO:doc.
 *
 * @param T TODO:doc
 */
class Cache<T> : LinkedHashMap<CacheKey<T>, Pair<String, String>>(16, 0.75F, true) {
  override fun removeEldestEntry(eldest: Map.Entry<CacheKey<T>, Pair<String, String>>): Boolean = this.size > 100_000
}

// // TODO: NearestVersionSelectorWrapper (DependencyNodeWrapper) https://kotlinlang.org/docs/delegation.html

// /** TODO:doc. */
class CachingMetadataResolver : MetadataResolver { // TODO: rename to wrapper
  private val log = Log {}

  /** TODO:doc. */
  val cache = Collections.synchronizedMap(Cache<Metadata.Nature>())

  override fun resolveMetadata(
    session: RepositorySystemSession,
    requests: MutableCollection<out MetadataRequest>,
  ): List<MetadataResult> = requests.map { resolveMetadata(session, it) }

  /** TODO:doc.
   *
   * @param session TODO:doc
   * @param request TODO:doc
   * @return TODO:doc
   * @throws CachedException TODO:doc
   * @throws IllegalStateException TODO:doc
   * @throws MetadataNotFoundException TODO:doc
   */
  fun resolveMetadata(session: RepositorySystemSession, request: MetadataRequest): MetadataResult {
    // log.error { "META: $request" }

    val key = Fiveple(
      request.metadata.groupId,
      request.metadata.artifactId,
      request.metadata.version,
      request.metadata.type,
      request.metadata.nature,
    )

    val maxTries = 3
    val maxSilentTries = 2
    var i = 1
    while (true) { // TODO: change to 'for' loop
      val ex = cache.get(key)
      if (ex != null) throw CachedException(ex.first, ex.second)

      // TODO: check that maven-metadata.xml location matches groupId/artifactId (e.g., https://repo1.maven.org/maven2/org/apache/continuum/redback-legacy/1.3.4/maven-metadata.xml)
      try {
        val results = defaultMetadataResolver!!.resolveMetadata(session, mutableListOf(request))
        assert(results.size == 1)
        return results[0]
      } catch (e: IllegalStateException) { // TODO: java.lang.RuntimeException:java.lang.InterruptedException and java.io.UncheckedIOException:java.nio.channels.FileLockInterruptionException
        // if (!(e.message ?: "").startsWith("Could not acquire write lock for ")) throw e
        if (i == maxTries) {
          log.error("Failed after $i tries: metadata ${request.metadata}")
          throw MetadataWriteLockException(request.metadata, e)
        } else {
          if (i >= maxSilentTries) log.warn("Retrying ($i of $maxTries) metadata ${request.metadata}")
          Thread.sleep(i * Random.nextLong(500, 1_000))
          i++ // TODO: put in 'for' loop
          continue // TODO: remove
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
    // TODO()
  }

  companion object {
    /** TODO:doc. */
    var defaultMetadataResolver: MetadataResolver? = null
  }
}

/** TODO:doc. */
object Maven {
  private val log = Log {}

  // Dummy values just so we can access the DefaultLocalPathComposer
  private val artifactFileLocalRepositoryManager =
    RepositorySystemSupplier().get().newLocalRepositoryManager(DefaultRepositorySystemSession(), LocalRepository(""))
  // TODO: local(baseVersion) vs remote(version)
  // art: org/wso2/carbon/registry/org.wso2.carbon.registry.contentsearch.ui.feature/4.8.35/org.wso2.carbon.registry.contentsearch.ui.feature-4.8.35.jar
  fun artifactFileX(artifact: Artifact): String =
    artifactFileLocalRepositoryManager.getPathForLocalArtifact(artifact)
  fun artifactFileX(groupId: String, artifactId: String): String =
    artifactFileX(DefaultArtifact(groupId, artifactId, "", "")).substringBefore("//").also {
      assert(File(it) == File(groupId.replace('.', '/'), artifactId))
    }
  // art: org.opennms.features.provisioning:org.opennms.features.provisioning.api:jar:33.0.5 org/opennms/features/provisioning/org.opennms.features.provisioning.api//org.opennms.features.provisioning.api-
  // meta: org/webjars/bowergithub/web-animations/web-animations-js/type-local
  fun metadataFileX(metadata: Metadata): String =
    artifactFileLocalRepositoryManager.getPathForLocalMetadata(metadata)
  // fun foo() =
  //   system.newLocalRepositoryManager(TODO(), TODO()).getPathForLocalArtifact(artifact)
    // getPathForLocalMetadata
    // system.getLocalRespositoryProvider()
    // system.getLocalPathComposer()
  // DefaultLocalPathComposer implements LocalPathComposer {
  //   @Override
  //   public String getPathForArtifact(
  //     getPathForMetadata

  fun artifactFile(artifact: Artifact): File =
    // TODO: replace unsafe names
    // TODO: filename should account for version, classifier and extension (artifact.toString())
    // artifact.toString().removeBefore(':').replace(':', '_')
    // classifier only if not "jar"
    // Use Maven code for file
    // with(artifact) { "${artifactId}-${version}-${classifier}_.${extension}" }
    File(artifact.groupId.replace('.', '/'), artifact.artifactId)

  fun artifactFile(baseDir: File, artifact: Artifact, passSuffix: String, failSuffix: String): Pair<File, File> =
    Parallel.outputFiles(baseDir.resolve(artifactFile(artifact)), passSuffix, failSuffix)

  fun mavenCentral(): Pair<RemoteRepository, List<RemoteRepository>> {
    // TODO: DEFAULT_REMOTE_REPO_URL
    fun builder(id: String, url: String) =
      RemoteRepository.Builder(id, "default" /* TODO: layout:"maven2" */, url)
        // TODO: policy: release
        .setPolicy(RepositoryPolicy(true, RepositoryPolicy.UPDATE_POLICY_NEVER, RepositoryPolicy.CHECKSUM_POLICY_FAIL))

    // TODO: make configurable (including things like proxy)
    // TODO: see setContentType for why "default"
    // TODO: "null" goes into maven-metadata-${id}.xml string
    // TODO: https://repo.maven.apache.org/maven2 (repo1 vs repo)
    val central = builder("central", "https://repo1.maven.org/maven2/").build()
    // https://repo1.maven.org/maven2/</url>
    // TODO: NOTE: maven only uses first mirror
    return central to listOf(
      "google-maven-central" to "https://maven-central.storage-download.googleapis.com/maven2/",
      "google-maven-central-eu" to "https://maven-central-eu.storage-download.googleapis.com/maven2/",
      "google-maven-central-ap" to "https://maven-central-asia.storage-download.googleapis.com/maven2/",
    ).map { (id, url) -> builder(id, url).addMirroredRepository(central).build() }
  }

  /** TODO:doc. */
  private val artifactErrorCache = Collections.synchronizedMap(
    // TODO: probably could use mutableMapOf<String, Pair<AtomicInteger, Throwable>>()
    object : LinkedHashMap<Artifact, Pair<AtomicInteger, Throwable>>(16, 0.75F, true) {
      override fun removeEldestEntry(eldest: Map.Entry<Artifact, Pair<AtomicInteger, Throwable>>): Boolean =
        this.size > 100_000
    }
  )
  // Runtime.getRuntime().addShutdownHook(
  //   Thread {
  //     for ((k, v) in cache.toList().sortedByDescending { it.second.first.get() }) {
  //       println("----- ${v.first} ${k} ${v.second}")
  //     }
  //   }
  // )

  // TODO: nearest version selector
  // CachingArtifactResolver.defaultArtifactResolver = getService(ArtifactResolver::class.java)
  // setService<ArtifactResolver>(ArtifactResolver::class.java, CachingArtifactResolver::class.java)

  fun <T, K, R> withErrorCache(getKey: (T) -> K, cache: (Throwable) -> Boolean, action: (T) -> R): (T) -> R {
    val errorCache = Collections.synchronizedMap(
      // TODO: probably could use mutableMapOf<String, Pair<AtomicInteger, Throwable>>()
      object : LinkedHashMap<K, Pair<AtomicInteger, Throwable>>(16, 0.75F, true) {
        override fun removeEldestEntry(eldest: Map.Entry<K, Pair<AtomicInteger, Throwable>>): Boolean =
          this.size > 100_000
      }
    )
    // Runtime.getRuntime().addShutdownHook(
    //   Thread {
    //     for ((k, v) in cache.toList().sortedByDescending { it.second.first.get() }) {
    //       println("----- ${v.first} ${k} ${v.second}")
    //     }
    //   }
    // )
    return {
      val key = getKey(it)
      errorCache[key]?.let { (count, exception) -> count.incrementAndGet(); throw exception }
      try {
        action(it)
      } catch (e: Throwable) {
        if (cache(e)) errorCache[key] = Pair(AtomicInteger(0), e)
        throw e
      }
    }
  }
  // withErrorCache({ it.artifact }, { isError(it) }) { action }
  // TODO: randomly shuffle mirror repositories before each request

  fun <T, R> withRetry(maxTries: Int, maxSilentTries: Int, delay: Duration, retry: (Throwable) -> Boolean, action: (T) -> R): (T) -> R {
    require(maxTries >= 1) { "maxTries must be at least 1 but is ${maxTries}" }
    return block@{
      throw (1..maxTries).fold<Int, Throwable?>(null) { _, attempt ->
        if (attempt > maxSilentTries) log.warn("Retrying ${it} (${attempt} of ${maxTries})")
        try {
          return@block action(it)
        } catch (e: Throwable) {
          if (!retry(e)) throw e
          Thread.sleep((delay * attempt * Random.nextDouble(1.0, 2.0)).toJavaDuration())
          e
        }
      }!!
    }
  }
  // withRetry(3, 4, 0.5.seconds, { isError(it) }) (withErrorCache(...) { action })

  val system = object : RepositorySystemSupplier() {
    protected override fun getMetadataResolver(
      repositoryEventDispatcher: RepositoryEventDispatcher,
      updateCheckManager: UpdateCheckManager,
      repositoryConnectorProvider: RepositoryConnectorProvider,
      remoteRepositoryManager: RemoteRepositoryManager,
      syncContextFactory: SyncContextFactory,
      offlineController: OfflineController,
      remoteRepositoryFilterManager: RemoteRepositoryFilterManager): MetadataResolver {
      CachingMetadataResolver.defaultMetadataResolver = super.getMetadataResolver(
        repositoryEventDispatcher,
        updateCheckManager,
        repositoryConnectorProvider,
        remoteRepositoryManager,
        syncContextFactory,
        offlineController,
        remoteRepositoryFilterManager,
      )
      return CachingMetadataResolver()
    }
    protected override fun getArtifactResolver(
      fileProcessor: FileProcessor,
      repositoryEventDispatcher: RepositoryEventDispatcher,
      versionResolver: VersionResolver,
      updateCheckManager: UpdateCheckManager,
      repositoryConnectorProvider: RepositoryConnectorProvider,
      remoteRepositoryManager: RemoteRepositoryManager,
      syncContextFactory: SyncContextFactory,
      offlineController: OfflineController,
      artifactResolverPostProcessors: Map<String, ArtifactResolverPostProcessor>,
      remoteRepositoryFilterManager: RemoteRepositoryFilterManager): ArtifactResolver =
      super.getArtifactResolver(
        fileProcessor,
        repositoryEventDispatcher,
        versionResolver,
        updateCheckManager,
        repositoryConnectorProvider,
        remoteRepositoryManager,
        syncContextFactory,
        offlineController,
        artifactResolverPostProcessors,
        remoteRepositoryFilterManager,
      ).let { artifactResolver ->
        object : ArtifactResolver {
          // TODO: throws ArtifactResolutionException
          // NOTE: we have to be careful not to cause a loop with the super of resolveArtifacts and resolveArtifact
          override fun resolveArtifacts(
            session: RepositorySystemSession,
            requests: MutableCollection<out ArtifactRequest>,
          ): List<ArtifactResult> = requests.map { resolveArtifact(session, it) }

          public override fun resolveArtifact(
            session: RepositorySystemSession,
            request: ArtifactRequest,
          ): ArtifactResult {
            // log.info("resolveArtifact(${request})")
            // Exception("resolveArtifact(${request})").printStackTrace()
            if ("\${" in with(request.artifact) { "${groupId}${artifactId}${version}${classifier}${extension}" }) {
              throw DollarInCoordinateException(request.artifact)
            }
            if (request.artifact.version.startsWith('^')) throw CaretInVersionException(request.artifact)

            request.repositories = request.repositories.mapNotNull {
              // ERROR maven.Dependencies: URL: typesafe (http://repo.typesafe.com/typesafe/releases/, default, releases+snapshots) repo.typesafe.com true http://repo.typesafe.com/typesafe/releases/
              // if (it.url.contains("typesafe")) { log.error("URL: ${it} ${it.host} ${it.host == "repo.typesafe.com"} ${it.url}") }
              when {
                it.host == "repo.typesafe.com" -> null // TODO: new typesafe repo // TODO: still broken for some reason
                else -> it
              }
            }
            // NearestVersionSelector
            // WARN  .org.apache.http.client.protocol.ResponseProcessCookies: Invalid cookie header: "Set-Cookie: AWSALB=4jNoB45fsH/C/StkXBt4UzDLCyUxTlyIgs0GVEtdWjU+Cd2pwTeAP2ww0qJMksiMWvBpSNjNnrMklmdIN1jXWZN5FqYR7c/cPbEhRZ9zV8iYurqQ7PR5kK16Eh4S; Expires=Sat, 29 Jun 2024 10:52:53 GMT; Path=/". Invalid 'expires' attribute: Sat, 29 Jun 2024 10:52:53 GMT
            // WARN  .org.apache.http.client.protocol.ResponseProcessCookies: Invalid cookie header: "Set-Cookie: AWSALBCORS=4jNoB45fsH/C/StkXBt4UzDLCyUxTlyIgs0GVEtdWjU+Cd2pwTeAP2ww0qJMksiMWvBpSNjNnrMklmdIN1jXWZN5FqYR7c/cPbEhRZ9zV8iYurqQ7PR5kK16Eh4S; Expires=Sat, 29 Jun 2024 10:52:53 GMT; Path=/; SameSite=None". Invalid 'expires' attribute: Sat, 29 Jun 2024 10:52:53 GMT
            // WARN  .org.apache.http.client.protocol.ResponseProcessCookies: Invalid cookie header: "Set-Cookie: AWSALB=SmJE2Th6xgQTRRFjJrUoRJ4vRI8laij1v51rT8AB1Momw5flXy9/eKVXWbO4Gq23OEkX8d3R1LRNYMXEpVLXCasft6zMsE5lGNKZIuerLJSCZ582tqUrTV8r2zzs; Expires=Sat, 29 Jun 2024 10:52:53 GMT; Path=/". Invalid 'expires' attribute: Sat, 29 Jun 2024 10:52:53 GMT
            // WARN  .org.apache.http.client.protocol.ResponseProcessCookies: Invalid cookie header: "Set-Cookie: AWSALBCORS=SmJE2Th6xgQTRRFjJrUoRJ4vRI8laij1v51rT8AB1Momw5flXy9/eKVXWbO4Gq23OEkX8d3R1LRNYMXEpVLXCasft6zMsE5lGNKZIuerLJSCZ582tqUrTV8r2zzs; Expires=Sat, 29 Jun 2024 10:52:53 GMT; Path=/; SameSite=None". Invalid 'expires' attribute: Sat, 29 Jun 2024 10:52:53 GMT
            // INFO  .org.apache.http.impl.execchain.RetryExec: I/O exception (java.net.SocketException) caught when processing request to {}->http://repo.typesafe.com:80: Network is unreachable
            // INFO  .org.apache.http.impl.execchain.RetryExec: Retrying request to {}->http://repo.typesafe.com:80

            // ERROR maven.Dependencies: java.lang.IllegalStateException
            // java.lang.IllegalStateException: Could not acquire lock(s)
            //   at org.eclipse.aether.internal.impl.synccontext.named.NamedLockFactoryAdapter$AdaptedLockSyncContext.acquire(NamedLockFactoryAdapter.java:219)
            //   at org.eclipse.aether.internal.impl.DefaultMetadataResolver.resolve(DefaultMetadataResolver.java:194)
            //   at org.eclipse.aether.internal.impl.DefaultMetadataResolver.resolveMetadata(DefaultMetadataResolver.java:180)

            val maxTries = 3
            val maxSilentTries = 2
            for (i in 1..maxTries) { // TODO: rename i to attempt
              artifactErrorCache[request.artifact]?.let { (count, exception) -> count.incrementAndGet(); throw exception }
              try {
                return artifactResolver.resolveArtifact(session, request)
              } catch (e: IllegalStateException) {
                // java.lang.IllegalStateException: Could not acquire lock(s)
                // at org.eclipse.aether.internal.impl.synccontext.named.NamedLockFactoryAdapter$AdaptedLockSyncContext.acquire(NamedLockFactoryAdapter.java:219)
                // TODO: figure out why we sometimes get null pointer exception
                // TODO: figure out why we sometimes get "lock" exception that is not write-lock
                // if (!(e.message ?: "").startsWith("Could not acquire write lock for ")) throw e
                if (i == maxTries) {
                  log.error("Failed to get write lock after $i tries: artifact ${request.artifact}")
                  throw ArtifactWriteLockException(request.artifact, e)
                } else {
                  if (i >= maxSilentTries) log.warn("Retrying ($i of $maxTries) artifact ${request.artifact}")
                  Thread.sleep(i * Random.nextLong(500, 1_000))
                }
              } catch (e: ArtifactResolutionException) {
                // log.error("${cache.size} ${request} ${e}")
                artifactErrorCache[request.artifact] = Pair(AtomicInteger(0), e)
                throw e
              }
            }
            TODO() // TODO: mark as unreachable code
          }
        }
      }
    }.get()
}

// object Mirrors {
  // List<RemoteRepository> mirrors = repositorySystem.newResolutionContext(session).getRepositorySelector().getEffectiveRepositories(remoteRepository);
  // Existing constant for maven central

  // Maybe by setting maven-metadata.xml?

  // val remote = RemoteRepository
  //   // TODO: make configurable (including things like proxy)
  //   // TODO: see setContentType for why "default"
  //   .Builder("google-maven-central-ap", "default", "https://maven-central-asia.storage-download.googleapis.com/maven2").apply {
  //     mirroredRepositories(central)
  //     policy(RepositoryPolicy(true, RepositoryPolicy.UPDATE_POLICY_NEVER, RepositoryPolicy.CHECKSUM_POLICY_FAIL))
  //   .build()

  // RepositoryMetadata metadata = system.readRepositoryMetadata(session, new RepositoryMetadataRequest(remoteRepo));
// }
