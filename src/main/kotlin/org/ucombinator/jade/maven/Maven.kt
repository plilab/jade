package org.ucombinator.jade.maven

import org.eclipse.aether.DefaultRepositorySystemSession
import org.eclipse.aether.RepositorySystemSession
import org.eclipse.aether.artifact.Artifact
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.impl.ArtifactResolver
import org.eclipse.aether.impl.MetadataResolver
import org.eclipse.aether.impl.OfflineController
import org.eclipse.aether.impl.RemoteRepositoryFilterManager
import org.eclipse.aether.impl.RemoteRepositoryManager
import org.eclipse.aether.impl.RepositoryConnectorProvider
import org.eclipse.aether.impl.RepositoryEventDispatcher
import org.eclipse.aether.impl.UpdateCheckManager
import org.eclipse.aether.impl.VersionResolver
import org.eclipse.aether.metadata.Metadata
import org.eclipse.aether.repository.LocalRepository
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.repository.RepositoryPolicy
import org.eclipse.aether.resolution.ArtifactRequest
import org.eclipse.aether.resolution.ArtifactResolutionException
import org.eclipse.aether.resolution.ArtifactResult
import org.eclipse.aether.resolution.MetadataRequest
import org.eclipse.aether.resolution.MetadataResult
import org.eclipse.aether.spi.io.FileProcessor
import org.eclipse.aether.spi.resolution.ArtifactResolverPostProcessor
import org.eclipse.aether.spi.synccontext.SyncContextFactory
import org.eclipse.aether.supplier.RepositorySystemSupplier
import org.eclipse.aether.transfer.MetadataNotFoundException
import org.eclipse.aether.util.artifact.JavaScopes
import org.eclipse.aether.util.graph.selector.ScopeDependencySelector
import org.eclipse.aether.util.graph.transformer.ConflictResolver
import org.eclipse.aether.util.graph.transformer.JavaScopeDeriver
import org.eclipse.aether.util.graph.transformer.JavaScopeSelector
import org.eclipse.aether.util.graph.transformer.NearestVersionSelector
import org.eclipse.aether.util.graph.transformer.SimpleOptionalitySelector
import org.ucombinator.jade.util.Errors
import org.ucombinator.jade.util.Log
import org.ucombinator.jade.util.Parallel
import org.ucombinator.jade.util.repeat

import java.io.File
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
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

// TODO: add sizes (from index) to starting and ending outputs

// TODO: NearestVersionSelectorWrapper (DependencyNodeWrapper) https://kotlinlang.org/docs/delegation.html

// TODO: split maven code into main/ folder and /util folder (or some other names)

/** TODO:doc. */
object Maven {
  private val log = Log {}

  // Dummy values just so we can access the DefaultLocalPathComposer
  private val artifactFileLocalRepositoryManager =
    RepositorySystemSupplier().get().newLocalRepositoryManager(DefaultRepositorySystemSession(), LocalRepository(""))

  // TODO: local(baseVersion) vs remote(version)
  // art: org/wso2/carbon/registry/org.wso2.carbon.registry.contentsearch.ui.feature/4.8.35/org.wso2.carbon.registry.contentsearch.ui.feature-4.8.35.jar
  fun artifactFile(artifact: Artifact): String =
    artifactFileLocalRepositoryManager.getPathForLocalArtifact(artifact)

  fun artifactFile(groupId: String, artifactId: String): String =
    artifactFile(DefaultArtifact(groupId, artifactId, "", "")).substringBefore("//").also {
      assert(File(it) == File(groupId.replace('.', '/'), artifactId))
    }

  fun artifactFile(baseDir: File, artifact: Artifact, passSuffix: String, failSuffix: String): Pair<File, File> =
    Parallel.outputFiles(baseDir.resolve(artifactFile(artifact)), passSuffix, failSuffix)

  fun artifactFile(baseDir: File, artifact: Pair<String, String>, passSuffix: String, failSuffix: String): Pair<File, File> =
    Parallel.outputFiles(baseDir.resolve(artifactFile(artifact.first, artifact.second)), passSuffix, failSuffix)

  // art: org.opennms.features.provisioning:org.opennms.features.provisioning.api:jar:33.0.5 org/opennms/features/provisioning/org.opennms.features.provisioning.api//org.opennms.features.provisioning.api-
  // meta: org/webjars/bowergithub/web-animations/web-animations-js/type-local
  // fun metadataFile(metadata: Metadata): String =
  //   artifactFileLocalRepositoryManager.getPathForLocalMetadata(metadata)

  /** TODO:doc. */
  fun coordinate(coordinate: String): Pair<String, String> =
    // TODO: support classifier/extension or others
    coordinate.split(':').let {
      if (it.size < 2) Errors.fatal("Missing \":\" in coordinate ${coordinate}")
      if (it.size > 2) Errors.fatal("Multiple \":\" in coordinate ${coordinate}")
      Pair(it[0], it[1])
    }

  val remote = RemoteRepository
    // TODO: make configurable (including things like proxy)
    // TODO: see setContentType for why "default"
    // TODO: "null" goes into maven-metadata-${id}.xml string
    // TODO: make configurable (including things like proxy)
    // TODO: see setContentType for why "default"
    .Builder("google-maven-central-ap", "default", "https://maven-central-asia.storage-download.googleapis.com/maven2")
    .build()

  fun mavenCentral(): Pair<RemoteRepository, List<RemoteRepository>> {
    // List<RemoteRepository> mirrors = repositorySystem.newResolutionContext(session).getRepositorySelector().getEffectiveRepositories(remoteRepository);
    // Existing constant for maven central

    // Maybe by setting maven-metadata.xml?

    // TODO: DEFAULT_REMOTE_REPO_URL
    fun builder(id: String, url: String) =
      // TODO: policy: release
      RemoteRepository.Builder(id, "default" /* TODO: layout:"maven2" */, url)

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

  // TODO: nearest version selector
  // CachingArtifactResolver.defaultArtifactResolver = getService(ArtifactResolver::class.java)
  // setService<ArtifactResolver>(ArtifactResolver::class.java, CachingArtifactResolver::class.java)

  // TODO: move to Parallel
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

  // TODO: randomly shuffle mirror repositories before each request

  fun <T, R> withRetry(maxTries: Int, maxSilentTries: Int, delay: Duration, retry: (Throwable) -> Boolean, action: (T) -> R): (T) -> R {
    require(maxTries >= 1) { "maxTries must be at least 1 but is ${maxTries}" }
    return block@{
      var attempt = 1
      repeat {
        if (attempt > maxSilentTries) log.warn("Retrying (${attempt} of ${maxTries}): ${it}")
        try {
          return@block action(it)
        } catch (e: Throwable) {
          if (attempt >= maxTries || !retry(e)) throw e
          Thread.sleep((delay * attempt * Random.nextDouble(1.0, 2.0)).toJavaDuration())
          attempt++
        }
      }
    }
  }

  val system = object : RepositorySystemSupplier() {
    protected override fun getMetadataResolver(
      repositoryEventDispatcher: RepositoryEventDispatcher,
      updateCheckManager: UpdateCheckManager,
      repositoryConnectorProvider: RepositoryConnectorProvider,
      remoteRepositoryManager: RemoteRepositoryManager,
      syncContextFactory: SyncContextFactory,
      offlineController: OfflineController,
      remoteRepositoryFilterManager: RemoteRepositoryFilterManager): MetadataResolver =
      JadeMetadataResolver(
        super.getMetadataResolver(
          repositoryEventDispatcher,
          updateCheckManager,
          repositoryConnectorProvider,
          remoteRepositoryManager,
          syncContextFactory,
          offlineController,
          remoteRepositoryFilterManager,
        )
      )

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
      JadeArtifactResolver(
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
        )
      )
  }.get()

  fun session(localRepository: LocalRepository): DefaultRepositorySystemSession =
    DefaultRepositorySystemSession().apply {
      checksumPolicy = RepositoryPolicy.CHECKSUM_POLICY_FAIL
      updatePolicy = RepositoryPolicy.UPDATE_POLICY_NEVER // TODO: no longer need on repos
      dependencySelector = ScopeDependencySelector(null, listOf(JavaScopes.RUNTIME, JavaScopes.TEST))
      localRepositoryManager = system.newLocalRepositoryManager(this, localRepository)
      // NOTE: If we omit this, we get cycles
      // TODO: explain this
      dependencyGraphTransformer =
        ConflictResolver(NearestVersionSelector(), JavaScopeSelector(), SimpleOptionalitySelector(), JavaScopeDeriver())
      // TODO: handle optional: https://mvnrepository.com/artifact/ant/ant/1.6.5
      // TODO: handle fat: https://github.com/apache/maven-resolver/blob/maven-resolver-1.9.20/maven-resolver-util/src/main/java/org/eclipse/aether/util/graph/traverser/FatArtifactTraverser.java
      // TODO: handle exclusions
      // TODO: look at other settings
      // TODO: transformer that limits repos
      // val oldDependencyTraverser = dependencyTraverser
      // dependencyTraverser = object : DependencyTraverser {
      //   override public fun traverseDependency(dependency: Dependency): Boolean {
      //   override public fun deriveChildTraverser(context: DependencyCollectionContext): DependencyTraverser {
      // }
      // TODO: NOTE: needed for "java.version" (is there another way to ensure it is loaded)
      // TODO: https://github.com/apache/maven/blob/c3f1cd6f76bd296a4e7c552990eff27afa1c4825/maven-model-builder/src/main/java/org/apache/maven/model/profile/activation/JdkVersionProfileActivator.java#L68
      setSystemProperties(System.getProperties()) // TODO: why doesn't assignment syntax work?
    }
}

// TODO: private?

/** TODO:doc. */
class JadeMetadataResolver(var metadataResolver: MetadataResolver) : MetadataResolver { // TODO: rename to wrapper
  private val log = Log {}

  private val resolveMetadata =
    // TODO: java.lang.RuntimeException:java.lang.InterruptedException and java.io.UncheckedIOException:java.nio.channels.FileLockInterruptionException
    // if (!(e.message ?: "").startsWith("Could not acquire write lock for ")) throw e
    Maven.withRetry(3, 2, 0.5.seconds, { it is IllegalStateException },
      Maven.withErrorCache({ it.second.metadata }, { it is MetadataNotFoundException}) {
        (session, request): Pair<RepositorySystemSession, MetadataRequest> ->
        metadataResolver.resolveMetadata(session, listOf(request)).single()
      }
    )

  override fun resolveMetadata(
    session: RepositorySystemSession,
    requests: MutableCollection<out MetadataRequest>,
  ): List<MetadataResult> = requests.map {
    // TODO: check that maven-metadata.xml location matches groupId/artifactId (e.g., https://repo1.maven.org/maven2/org/apache/continuum/redback-legacy/1.3.4/maven-metadata.xml)
    resolveMetadata(Pair(session, it))
  }
}

class JadeArtifactResolver(var artifactResolver: ArtifactResolver) : ArtifactResolver {
  private val resolveArtifact =
    // java.lang.IllegalStateException: Could not acquire lock(s)
    // at org.eclipse.aether.internal.impl.synccontext.named.NamedLockFactoryAdapter$AdaptedLockSyncContext.acquire(NamedLockFactoryAdapter.java:219)
    // TODO: figure out why we sometimes get null pointer exception
    // TODO: figure out why we sometimes get "lock" exception that is not write-lock
    // if (!(e.message ?: "").startsWith("Could not acquire write lock for ")) throw e
    // TODO: maybe this retry isn't needed anymore?
    Maven.withRetry(3, 2, 0.5.seconds, { it is IllegalStateException },
      Maven.withErrorCache({ it.second.artifact }, { it is ArtifactResolutionException}) {
        (session, request): Pair<RepositorySystemSession, ArtifactRequest> ->
        artifactResolver.resolveArtifact(session, request)
      }
    )

  // TODO: throws ArtifactResolutionException
  // NOTE: we have to be careful not to cause a loop with the super of resolveArtifacts and resolveArtifact
  override fun resolveArtifacts(
    session: RepositorySystemSession,
    requests: MutableCollection<out ArtifactRequest>, // TODO: could we run these in parallel? (use async and a context as param)
  ): List<ArtifactResult> = requests.map { resolveArtifact(session, it) }
  // TODO: make http requests cooperate with coroutines

  override fun resolveArtifact(
    session: RepositorySystemSession,
    request: ArtifactRequest,
  ): ArtifactResult {
    if ("\${" in with(request.artifact) { "${groupId}${artifactId}${version}${classifier}${extension}" }) {
      throw DollarInCoordinateException(request.artifact)
    }
    if (request.artifact.version.startsWith('^')) throw CaretInVersionException(request.artifact)

    // TODO: override repository policies
    // TODO: data-driven list of replacements
    // TODO: data-driven list of removals
    // request.repositories = request.repositories.mapNotNull {
    //   // ERROR maven.Dependencies: URL: typesafe (http://repo.typesafe.com/typesafe/releases/, default, releases+snapshots) repo.typesafe.com true http://repo.typesafe.com/typesafe/releases/
    //   // if (it.url.contains("typesafe")) { log.error("URL: ${it} ${it.host} ${it.host == "repo.typesafe.com"} ${it.url}") }
    //   when {
    //     it.host == "repo.typesafe.com" -> null // TODO: new typesafe repo // TODO: still broken for some reason
    //     else -> it
    //   }
    // }
    // NearestVersionSelector
    // WARN  .org.apache.http.client.protocol.ResponseProcessCookies: Invalid cookie header: "Set-Cookie: AWSALB=4jNoB45fsH/C/StkXBt4UzDLCyUxTlyIgs0GVEtdWjU+Cd2pwTeAP2ww0qJMksiMWvBpSNjNnrMklmdIN1jXWZN5FqYR7c/cPbEhRZ9zV8iYurqQ7PR5kK16Eh4S; Expires=Sat, 29 Jun 2024 10:52:53 GMT; Path=/". Invalid 'expires' attribute: Sat, 29 Jun 2024 10:52:53 GMT
    // WARN  .org.apache.http.client.protocol.ResponseProcessCookies: Invalid cookie header: "Set-Cookie: AWSALBCORS=4jNoB45fsH/C/StkXBt4UzDLCyUxTlyIgs0GVEtdWjU+Cd2pwTeAP2ww0qJMksiMWvBpSNjNnrMklmdIN1jXWZN5FqYR7c/cPbEhRZ9zV8iYurqQ7PR5kK16Eh4S; Expires=Sat, 29 Jun 2024 10:52:53 GMT; Path=/; SameSite=None". Invalid 'expires' attribute: Sat, 29 Jun 2024 10:52:53 GMT
    // WARN  .org.apache.http.client.protocol.ResponseProcessCookies: Invalid cookie header: "Set-Cookie: AWSALB=SmJE2Th6xgQTRRFjJrUoRJ4vRI8laij1v51rT8AB1Momw5flXy9/eKVXWbO4Gq23OEkX8d3R1LRNYMXEpVLXCasft6zMsE5lGNKZIuerLJSCZ582tqUrTV8r2zzs; Expires=Sat, 29 Jun 2024 10:52:53 GMT; Path=/". Invalid 'expires' attribute: Sat, 29 Jun 2024 10:52:53 GMT
    // WARN  .org.apache.http.client.protocol.ResponseProcessCookies: Invalid cookie header: "Set-Cookie: AWSALBCORS=SmJE2Th6xgQTRRFjJrUoRJ4vRI8laij1v51rT8AB1Momw5flXy9/eKVXWbO4Gq23OEkX8d3R1LRNYMXEpVLXCasft6zMsE5lGNKZIuerLJSCZ582tqUrTV8r2zzs; Expires=Sat, 29 Jun 2024 10:52:53 GMT; Path=/; SameSite=None". Invalid 'expires' attribute: Sat, 29 Jun 2024 10:52:53 GMT
    // INFO  .org.apache.http.impl.execchain.RetryExec: I/O exception (java.net.SocketException) caught when processing request to {}->http://repo.typesafe.com:80: Network is unreachable
    // INFO  .org.apache.http.impl.execchain.RetryExec: Retrying request to {}->http://repo.typesafe.com:80
    // http://jcenter.bintray.com:80
    // "dl.bintray.com" ||
    // "repo.spring.io" ||
    // ".codehaus.org"

    ////////////////
    // 501 HTTPS Required. 
    // Use https://repo1.maven.org/maven2/
    // More information at https://links.sonatype.com/central/501-https-required
    // "HTTP/1.1 501 HTTPS Required"

    //     it.url == "http://repo1.maven.org/maven2" ||
    //     it.url == "http://repo1.maven.org/maven2/"
    //   -> RemoteRepository.Builder(it).setUrl("https://repo1.maven.org/maven2/").build()

    //   it.url == "http://repo2.maven.org/maven2" ||
    //     it.url == "http://repo2.maven.org/maven2/"
    //   -> RemoteRepository.Builder(it).setUrl("https://repo2.maven.org/maven2/").build()

    //   it.url == "http://repo.maven.apache.org/maven2" ||
    //     it.url == "http://repo.maven.apache.org/maven2/"
    //   -> RemoteRepository.Builder(it).setUrl("https://repo.maven.apache.org/maven2/").build()

    // Seems Okay: it.url == "http://svn.apache.org/repos/asf/servicemix/m2-repo" ||

    //   // See https://netbeans.apache.org/about/oracle-transition.html (dead link)
    //   it.url == "http://bits.netbeans.org/maven2" ||
    //   it.url == "http://bits.netbeans.org/maven2/"
    // -> RemoteRepository.Builder(it).setUrl("http://netbeans.apidesign.org/maven2/").build()


    // ERROR maven.Dependencies: java.lang.IllegalStateException
    // java.lang.IllegalStateException: Could not acquire lock(s)
    //   at org.eclipse.aether.internal.impl.synccontext.named.NamedLockFactoryAdapter$AdaptedLockSyncContext.acquire(NamedLockFactoryAdapter.java:219)
    //   at org.eclipse.aether.internal.impl.DefaultMetadataResolver.resolve(DefaultMetadataResolver.java:194)
    //   at org.eclipse.aether.internal.impl.DefaultMetadataResolver.resolveMetadata(DefaultMetadataResolver.java:180)

    // Caused by: org.apache.maven.model.resolution.UnresolvableModelException: The following artifacts could not be resolved: org.springframework.cloud:spring-cloud-dependencies:pom:Greenwich.RC2 (present, but unavailable): Could not transfer artifact org.springframework.cloud:spring-cloud-dependencies:pom:Greenwich.RC2 from/to central-2 (http://central.maven.org/maven2/): central.maven.org: Name or service not known

    // Fails (Cache):
    //   119690 	org.eclipse.aether.collection.DependencyCollectionException:org.eclipse.aether.resolution.ArtifactDescriptorException:org.eclipse.aether.resolution.ArtifactResolutionException:org.eclipse.aether.transfer.ArtifactNotFoundException
    //   7753 	org.eclipse.aether.collection.DependencyCollectionException:org.eclipse.aether.resolution.ArtifactDescriptorException:org.apache.maven.model.building.ModelBuildingException
    //   6466 	org.eclipse.aether.collection.DependencyCollectionException:org.eclipse.aether.resolution.ArtifactDescriptorException:org.eclipse.aether.resolution.ArtifactResolutionException:org.eclipse.aether.transfer.ArtifactTransferException:java.net.UnknownHostException
    //   5887 	org.eclipse.aether.collection.DependencyCollectionException:org.eclipse.aether.resolution.ArtifactDescriptorException:org.apache.maven.model.resolution.UnresolvableModelException:org.eclipse.aether.resolution.ArtifactResolutionException:org.eclipse.aether.transfer.ArtifactNotFoundException
    //   4024 	org.eclipse.aether.collection.DependencyCollectionException:org.eclipse.aether.resolution.ArtifactDescriptorException:org.eclipse.aether.resolution.ArtifactResolutionException:org.eclipse.aether.transfer.ArtifactTransferException:org.apache.http.client.HttpResponseException
    //   2356 	org.eclipse.aether.collection.DependencyCollectionException:org.eclipse.aether.resolution.VersionRangeResolutionException
    //   977 	org.ucombinator.jade.maven.DollarInCoordinateException
    //   452 	org.eclipse.aether.collection.DependencyCollectionException:org.eclipse.aether.collection.UnsolvableVersionConflictException
    //   363 	org.ucombinator.jade.maven.CaretInVersionException

    // Fails (New):
    //   16671 	org.eclipse.aether.collection.DependencyCollectionException:org.eclipse.aether.resolution.ArtifactDescriptorException:org.eclipse.aether.resolution.ArtifactResolutionException:org.eclipse.aether.transfer.ArtifactNotFoundException
    //   1867 	org.eclipse.aether.collection.DependencyCollectionException:org.eclipse.aether.resolution.ArtifactDescriptorException:org.apache.maven.model.building.ModelBuildingException
    //   301 	org.eclipse.aether.collection.DependencyCollectionException:org.eclipse.aether.resolution.ArtifactDescriptorException:org.eclipse.aether.resolution.ArtifactResolutionException:org.eclipse.aether.transfer.ArtifactTransferException:java.net.UnknownHostException
    //   243 	org.eclipse.aether.collection.DependencyCollectionException:org.eclipse.aether.resolution.ArtifactDescriptorException:org.eclipse.aether.resolution.ArtifactResolutionException:org.eclipse.aether.transfer.ArtifactTransferException:org.apache.http.client.HttpResponseException
    //   10 	org.eclipse.aether.collection.DependencyCollectionException:org.eclipse.aether.resolution.VersionRangeResolutionException
    //   10 	org.ucombinator.jade.maven.DollarInCoordinateException
    //   5 	org.eclipse.aether.collection.DependencyCollectionException:org.eclipse.aether.resolution.ArtifactDescriptorException:org.apache.maven.model.resolution.UnresolvableModelException:org.eclipse.aether.resolution.ArtifactResolutionException:org.eclipse.aether.transfer.ArtifactNotFoundException
    //   2 	org.eclipse.aether.collection.DependencyCollectionException:org.eclipse.aether.collection.UnsolvableVersionConflictException

    // Fails (Glitch):
    //   816 	java.lang.RuntimeException:java.lang.RuntimeException:java.lang.InterruptedException
    //   444 	java.io.UncheckedIOException:java.nio.channels.FileLockInterruptionException
    //   425 	java.lang.IllegalStateException:java.lang.IllegalStateException
    //   131 	org.eclipse.aether.collection.DependencyCollectionException:org.eclipse.aether.resolution.ArtifactDescriptorException:org.apache.maven.model.resolution.UnresolvableModelException:org.eclipse.aether.resolution.ArtifactResolutionException:org.eclipse.aether.transfer.ArtifactTransferException:org.apache.http.client.HttpResponseException
    //   123 	org.eclipse.aether.collection.DependencyCollectionException:org.eclipse.aether.resolution.ArtifactDescriptorException:org.eclipse.aether.resolution.ArtifactResolutionException:org.eclipse.aether.transfer.ArtifactTransferException:javax.net.ssl.SSLHandshakeException:sun.security.validator.ValidatorException:sun.security.provider.certpath.SunCertPathBuilderException
    //   109 	org.eclipse.aether.collection.DependencyCollectionException:org.eclipse.aether.resolution.ArtifactDescriptorException:org.eclipse.aether.resolution.ArtifactResolutionException:org.eclipse.aether.transfer.ArtifactTransferException:org.apache.http.conn.ConnectTimeoutException:java.net.SocketTimeoutException
    //   96 	org.eclipse.aether.collection.DependencyCollectionException:org.eclipse.aether.resolution.ArtifactDescriptorException:org.eclipse.aether.resolution.ArtifactResolutionException:org.eclipse.aether.transfer.ArtifactTransferException:org.eclipse.aether.transfer.NoRepositoryConnectorException:org.eclipse.aether.transfer.NoRepositoryConnectorException:org.eclipse.aether.transfer.NoRepositoryLayoutException:org.eclipse.aether.transfer.NoRepositoryLayoutException
    //   61 	org.eclipse.aether.collection.DependencyCollectionException:org.eclipse.aether.resolution.ArtifactDescriptorException:org.eclipse.aether.resolution.ArtifactResolutionException:org.eclipse.aether.transfer.ArtifactTransferException:org.eclipse.aether.transfer.NoRepositoryConnectorException:org.eclipse.aether.transfer.NoRepositoryConnectorException:org.eclipse.aether.transfer.NoTransporterException
    //   45 	org.eclipse.aether.collection.DependencyCollectionException:org.eclipse.aether.resolution.ArtifactDescriptorException:org.eclipse.aether.resolution.ArtifactResolutionException:org.eclipse.aether.transfer.ArtifactTransferException:org.eclipse.aether.transfer.ChecksumFailureException
    //   23 	org.eclipse.aether.collection.DependencyCollectionException:org.eclipse.aether.resolution.ArtifactDescriptorException:org.apache.maven.model.resolution.UnresolvableModelException:org.eclipse.aether.resolution.ArtifactResolutionException:org.eclipse.aether.transfer.ArtifactTransferException:java.net.UnknownHostException
    //   18 	org.eclipse.aether.collection.DependencyCollectionException:org.eclipse.aether.resolution.ArtifactDescriptorException:org.apache.maven.model.resolution.UnresolvableModelException:org.eclipse.aether.resolution.ArtifactResolutionException:org.eclipse.aether.transfer.ArtifactTransferException:org.apache.http.conn.ConnectTimeoutException:java.net.SocketTimeoutException
    //   9 	org.eclipse.aether.collection.DependencyCollectionException:org.eclipse.aether.resolution.ArtifactDescriptorException:org.eclipse.aether.resolution.ArtifactResolutionException:org.eclipse.aether.transfer.ArtifactTransferException:org.apache.http.NoHttpResponseException
    //   8 	org.eclipse.aether.collection.DependencyCollectionException:org.eclipse.aether.resolution.ArtifactDescriptorException:org.apache.maven.model.resolution.UnresolvableModelException:org.eclipse.aether.resolution.ArtifactResolutionException:org.eclipse.aether.transfer.ArtifactTransferException:org.eclipse.aether.transfer.ChecksumFailureException
    //   8 	org.eclipse.aether.collection.DependencyCollectionException:org.eclipse.aether.resolution.ArtifactDescriptorException:org.eclipse.aether.resolution.ArtifactResolutionException:org.eclipse.aether.transfer.ArtifactTransferException:java.net.SocketException
    //   8 	org.eclipse.aether.collection.DependencyCollectionException:org.eclipse.aether.resolution.ArtifactDescriptorException:org.eclipse.aether.resolution.VersionResolutionException:org.eclipse.aether.transfer.MetadataNotFoundException
    //   7 	org.eclipse.aether.collection.DependencyCollectionException:org.eclipse.aether.resolution.ArtifactDescriptorException:org.apache.maven.model.resolution.UnresolvableModelException:org.eclipse.aether.resolution.ArtifactResolutionException:org.eclipse.aether.transfer.ArtifactTransferException:java.net.SocketException
    //   6 	kotlinx.coroutines.TimeoutCancellationException:kotlinx.coroutines.TimeoutCancellationException
    //   5 	org.eclipse.aether.collection.DependencyCollectionException:org.eclipse.aether.resolution.VersionRangeResolutionException:org.eclipse.aether.version.InvalidVersionSpecificationException
    //   4 	org.eclipse.aether.collection.DependencyCollectionException:org.eclipse.aether.resolution.ArtifactDescriptorException:org.eclipse.aether.resolution.ArtifactResolutionException:org.eclipse.aether.transfer.ArtifactTransferException:javax.net.ssl.SSLPeerUnverifiedException
    //   3 	org.eclipse.aether.collection.DependencyCollectionException:org.eclipse.aether.resolution.ArtifactDescriptorException:org.eclipse.aether.resolution.ArtifactResolutionException:org.eclipse.aether.transfer.ArtifactTransferException:org.apache.http.conn.HttpHostConnectException:java.net.ConnectException
    //   2 	org.eclipse.aether.collection.DependencyCollectionException:org.eclipse.aether.resolution.ArtifactDescriptorException:org.apache.maven.model.resolution.UnresolvableModelException
    //   1 	org.eclipse.aether.collection.DependencyCollectionException:org.eclipse.aether.resolution.ArtifactDescriptorException:org.apache.maven.model.resolution.UnresolvableModelException:org.eclipse.aether.resolution.ArtifactResolutionException:org.eclipse.aether.transfer.ArtifactTransferException:javax.net.ssl.SSLHandshakeException:sun.security.validator.ValidatorException:java.security.cert.CertPathValidatorException:java.security.cert.CertificateExpiredException
    //   1 	org.eclipse.aether.collection.DependencyCollectionException:org.eclipse.aether.resolution.ArtifactDescriptorException:org.apache.maven.model.resolution.UnresolvableModelException:org.eclipse.aether.resolution.ArtifactResolutionException:org.eclipse.aether.transfer.ArtifactTransferException:org.apache.http.NoHttpResponseException
    //   1 	org.eclipse.aether.collection.DependencyCollectionException:org.eclipse.aether.resolution.ArtifactDescriptorException:org.apache.maven.model.resolution.UnresolvableModelException:org.eclipse.aether.resolution.ArtifactResolutionException:org.eclipse.aether.transfer.ArtifactTransferException:org.apache.http.conn.HttpHostConnectException:java.net.ConnectException
    //   1 	org.eclipse.aether.collection.DependencyCollectionException:org.eclipse.aether.resolution.ArtifactDescriptorException:org.apache.maven.model.resolution.UnresolvableModelException:org.eclipse.aether.resolution.ArtifactResolutionException:org.eclipse.aether.transfer.ArtifactTransferException:org.eclipse.aether.transfer.NoRepositoryConnectorException:org.eclipse.aether.transfer.NoRepositoryConnectorException:org.eclipse.aether.transfer.NoTransporterException
    //   1 	org.eclipse.aether.collection.DependencyCollectionException:org.eclipse.aether.resolution.ArtifactDescriptorException:org.eclipse.aether.RepositoryException
    //   1 	org.eclipse.aether.collection.DependencyCollectionException:org.eclipse.aether.resolution.ArtifactDescriptorException:org.eclipse.aether.resolution.ArtifactResolutionException:org.eclipse.aether.transfer.ArtifactTransferException:java.net.NoRouteToHostException
    //   1 	org.eclipse.aether.collection.DependencyCollectionException:org.eclipse.aether.resolution.ArtifactDescriptorException:org.eclipse.aether.resolution.ArtifactResolutionException:org.eclipse.aether.transfer.ArtifactTransferException:javax.net.ssl.SSLHandshakeException:sun.security.validator.ValidatorException:java.security.cert.CertPathValidatorException:java.security.cert.CertificateExpiredException
    //   1 	org.eclipse.aether.collection.DependencyCollectionException:org.eclipse.aether.resolution.ArtifactDescriptorException:org.eclipse.aether.resolution.ArtifactResolutionException:org.eclipse.aether.transfer.ArtifactTransferException:org.apache.http.client.ClientProtocolException:org.apache.http.client.CircularRedirectException

    return resolveArtifact(Pair(session, request))
  }
}
