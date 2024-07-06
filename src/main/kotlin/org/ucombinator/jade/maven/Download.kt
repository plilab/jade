package org.ucombinator.jade.maven

// TODO: remove non-aether maven imports
import org.apache.commons.compress.compressors.CompressorStreamFactory
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader
import org.apache.maven.index.reader.IndexReader
import org.apache.maven.index.reader.Record
import org.apache.maven.index.reader.RecordExpander
import org.apache.maven.index.reader.Utils.INDEX_FILE_PREFIX
import org.apache.maven.index.reader.resource.PathWritableResourceHandler
import org.apache.maven.index.reader.resource.UriResourceHandler
import org.apache.maven.repository.internal.MavenRepositorySystemUtils
import org.eclipse.aether.DefaultRepositorySystemSession
import org.eclipse.aether.RepositorySystem
import org.eclipse.aether.RepositorySystemSession
import org.eclipse.aether.artifact.Artifact
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.artifact.AbstractArtifact
import org.eclipse.aether.collection.CollectRequest
import org.eclipse.aether.collection.DependencyCollectionContext
import org.eclipse.aether.collection.DependencyTraverser
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory
import org.eclipse.aether.graph.Dependency
import org.eclipse.aether.graph.DependencyNode
import org.eclipse.aether.graph.Exclusion
import org.eclipse.aether.impl.ArtifactResolver
import org.eclipse.aether.impl.MetadataResolver
import org.eclipse.aether.impl.OfflineController
import org.eclipse.aether.impl.RemoteRepositoryFilterManager
import org.eclipse.aether.impl.RemoteRepositoryManager
import org.eclipse.aether.impl.RepositoryConnectorProvider
import org.eclipse.aether.impl.RepositoryEventDispatcher
import org.eclipse.aether.impl.UpdateCheckManager
import org.eclipse.aether.impl.VersionResolver
import org.eclipse.aether.internal.impl.DefaultArtifactResolver
import org.eclipse.aether.internal.impl.DefaultMetadataResolver
import org.eclipse.aether.metadata.DefaultMetadata
import org.eclipse.aether.metadata.Metadata
import org.eclipse.aether.repository.LocalRepository
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.repository.RepositoryPolicy
import org.eclipse.aether.resolution.ArtifactDescriptorRequest
import org.eclipse.aether.resolution.ArtifactDescriptorResult
import org.eclipse.aether.resolution.ArtifactRequest
import org.eclipse.aether.resolution.ArtifactResolutionException
import org.eclipse.aether.resolution.ArtifactResult
import org.eclipse.aether.resolution.MetadataRequest
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory
import org.eclipse.aether.spi.connector.transport.TransporterFactory
import org.eclipse.aether.spi.io.FileProcessor
import org.eclipse.aether.spi.resolution.ArtifactResolverPostProcessor
import org.eclipse.aether.spi.synccontext.SyncContextFactory
import org.eclipse.aether.supplier.RepositorySystemSupplier
import org.eclipse.aether.transport.file.FileTransporterFactory
import org.eclipse.aether.transport.http.HttpTransporterFactory
import org.eclipse.aether.util.artifact.JavaScopes
import org.eclipse.aether.util.graph.selector.ScopeDependencySelector
import org.eclipse.aether.util.graph.transformer.ChainedDependencyGraphTransformer
import org.eclipse.aether.util.graph.transformer.ConflictResolver
import org.eclipse.aether.util.graph.transformer.JavaDependencyContextRefiner
import org.eclipse.aether.util.graph.transformer.JavaScopeDeriver
import org.eclipse.aether.util.graph.transformer.JavaScopeSelector
import org.eclipse.aether.util.graph.transformer.NearestVersionSelector
import org.eclipse.aether.util.graph.transformer.SimpleOptionalitySelector
import org.eclipse.aether.util.version.GenericVersionScheme
import org.ucombinator.jade.util.Errors
import org.ucombinator.jade.util.Json
import org.ucombinator.jade.util.Log
import org.ucombinator.jade.util.Parallel

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.net.URI
import java.nio.channels.Channels
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.resolve
import kotlin.random.Random
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** TODO:doc. */
object Download {
  private val log = Log {}

  /** TODO:doc. */
  fun main( // TODO: rename
    // TODO: class Dependencies { remoterepos(default=central) localIndex localRepo artifacts() -> stdout or outputDir (.err) }
    // remote: URI,
    // local: File,
    // TODO: overwrite
    // TODO: mkdir
    // TODO: proxy
    localRepoDir: File,
    artifacts: List<Artifact>,
  ) {
    // TODO: error messages for exceptions
    val cache = Collections.synchronizedMap(mutableMapOf<String, Pair<AtomicInteger, Throwable>>())
    // Runtime.getRuntime().addShutdownHook(
    //   Thread {
    //     for ((k, v) in cache.toList().sortedByDescending { it.second.first.get() }) {
    //       println("----- ${v.first} ${k} ${v.second}")
    //     }
    //   }
    // )
    // TODO: factor into utility class?
    // TODO: nearest version selector
    // CachingArtifactResolver.defaultArtifactResolver = getService(ArtifactResolver::class.java)
    // setService<ArtifactResolver>(ArtifactResolver::class.java, CachingArtifactResolver::class.java)

    val system = Maven.system
    val localRepo = LocalRepository(localRepoDir)
    val session = DefaultRepositorySystemSession().apply {
      dependencySelector = ScopeDependencySelector(null, listOf(JavaScopes.RUNTIME, JavaScopes.TEST))
      localRepositoryManager = system.newLocalRepositoryManager(this, localRepo)
      // TODO: what if we omit this? we get cycles
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
      setSystemProperties(System.getProperties()) // TODO: why doesn't assignment syntax work?
    }

    val remote = RemoteRepository
      // TODO: make configurable (including things like proxy)
      // TODO: see setContentType for why "default"
      // TODO: "null" goes into maven-metadata-${id}.xml string
      .Builder("google-maven-central-ap", "default", "https://maven-central-asia.storage-download.googleapis.com/maven2")
      .setPolicy(RepositoryPolicy(true, RepositoryPolicy.UPDATE_POLICY_NEVER, RepositoryPolicy.CHECKSUM_POLICY_FAIL))
      .build()
    // TODO: support multiple remotes
    val compressorStreamFactory = CompressorStreamFactory()

    Parallel.run(
      log,
      artifacts,
      {
        val groupPath = TODO() // dependenciesDir.resolve(artifact.groupId.replace('.', '/')) // TODO: common function for artifact to path
        Pair(groupPath.resolve("${it.artifactId}.json.zst"), groupPath.resolve("${it.artifactId}.err"))
        // TODO: (use extension as extension) Maven.artifactFile(dependenciesDir, artifact, ".json.zst", ".err")
      },
      { false },
    ) { artifact ->

      // file.lines.map(::words)
      TODO()

      // val (cls, extensions) = when {
      //   extension == "pom" -> Pair(null, listOf(extension)) // TODO: null isn't technically correct (should be classifier)
      //   extension == "feature" -> Pair("features", listOf("xml"))
      //   extension == "tile" -> Pair(classifier, listOf("xml"))
      //   extension == "eclipse-target-definition" -> Pair(classifier, listOf("target"))
      //   classifier == "config" -> Pair(classifier, listOf("cfg", "xml"))
      //   classifier == "plugin2asciidoc" -> Pair(classifier, listOf("xsl"))
      //   classifier == "idmtool" -> Pair(classifier, listOf("py"))

      //   listOf(
      //     "configuration",
      //     "countersconf",
      //     "datastore",
      //     "restconf",
      //   ).contains(classifier)
      //   -> Pair(classifier, listOf("cfg"))

      //   listOf(
      //     "aaa-app-config",
      //     "aaa-datastore-config",
      //     "aaa-password-service-config",
      //     "akkaconf",
      //     "bgp-initial-config",
      //     "factoryakkaconf",
      //     "features-core",
      //     "features",
      //     "legacyConfig",
      //     "moduleconf",
      //     "moduleshardconf",
      //     "network-topology-bgp-initial-config",
      //     "network-topology-pcep-initial-config",
      //     "odl-bmp-monitors-config",
      //     "routing-policy-default-config",
      //     "network-topology-initial-config",
      //     "configstats",
      //     "config-He",
      //   ).contains(classifier)
      //   -> Pair(classifier, listOf("xml"))

      //   else -> Pair(classifier, listOf(extension, "jar", "zip"))
      //   // TODO: .war .nar local-repo/org/apache/any23/apache-any23-service/2.3/apache-any23-service-2.3.war local-repo/org/apache/nifi/nifi-accumulo-services-api-nar/2.0.0-M2/nifi-accumulo-services-api-nar-2.0.0-M2.nar
      //   // ./local-repo/com/tencent/edu/TYICSDK/2.0.1.97/TYICSDK-2.0.1.97.aar
      //   // ./local-repo/se/skltp/components/mule-probe/4.1/mule-probe-4.1.zip
      // }

      // var firstException: Throwable? = null
      // for (ext in extensions) {
      //   val artifact = DefaultArtifact(groupId, artifactId, cls, ext, version)
      //   val artifactRequest = ArtifactRequest(artifact, remotes, null)
      //   try {
      //     return system.resolveArtifact(session, artifactRequest)
      //   } catch (e: org.eclipse.aether.resolution.ArtifactResolutionException) {
      //     firstException = firstException ?: e
      //   } catch (e: CachedException) {
      //     firstException = firstException ?: e
      //   }
      // }
      // throw firstException!!

      // TODO: check result.exceptions
      // .artifact
      // .repositories
      // .requestContext
      val remotes = TODO()
      Json.of(system.resolveArtifact(session, ArtifactRequest(artifact, remotes, null))).toString().toByteArray()
    }
  }

  // TODO: root.accept(ConsoleDependencyGraphDumper())
  // TODO: record optionality (maybe look at ConsoleDependencyGraphDuper for code)
  // TODO: fix cookie error
  // int MANAGED_VERSION = 0x01;
  // int MANAGED_SCOPE = 0x02;
  // int MANAGED_OPTIONAL = 0x04;
  // int MANAGED_PROPERTIES = 0x08;
  // int MANAGED_EXCLUSIONS = 0x10;

  // properties: {language=none, constitutesBuildPath=false, type=jar, includesDependencies=false}
  // includesDependencies=false
  // require(node.version === node.artifact.version)
  // data: Map<Object, Object>
  //   ConflictResolver.NODE_DATA_WINNER

  // transitive dependencies
  // https://maven.apache.org/guides/introduction/introduction-to-optional-and-excludes-dependencies.html
  // https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html
  // https://maven.apache.org/repositories/remote.html
  // https://cwiki.apache.org/confluence/display/MAVEN/Maven+3.x+Compatibility+Notes#Maven3.xCompatibilityNotes-PluginMetaversionResolution
  // https://maven.apache.org/maven-ci-friendly.html
  // https://maven.apache.org/resolver/maven-resolver-supplier/dependency-info.html
  // https://maven.apache.org/maven-indexer/indexer-core/index.html
  // https://github.com/apache/maven-indexer/blob/master/indexer-reader/src/main/java/org/apache/maven/index/reader/Record.java
  // https://maven.apache.org/maven-indexer/indexer-reader/index.html
  // https://maven.apache.org/maven-indexer/index.html
}
