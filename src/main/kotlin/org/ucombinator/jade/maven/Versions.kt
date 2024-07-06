package org.ucombinator.jade.maven

import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader
import org.eclipse.aether.DefaultRepositorySystemSession
import org.eclipse.aether.metadata.DefaultMetadata
import org.eclipse.aether.metadata.Metadata
import org.eclipse.aether.repository.LocalRepository
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.repository.RepositoryPolicy
import org.eclipse.aether.resolution.MetadataRequest
import org.eclipse.aether.supplier.RepositorySystemSupplier
import org.eclipse.aether.util.version.GenericVersionScheme
import org.ucombinator.jade.util.Errors
import org.ucombinator.jade.util.Exceptions
import org.ucombinator.jade.util.Log
import org.ucombinator.jade.util.Parallel
import java.io.File
import java.io.FileInputStream
import kotlin.io.resolve

// TODO: remove non-aether maven imports

/** TODO:doc. */
object Versions { // TODO: rename to versions
  private val log = Log {}

  // TODO: split maven code into main/ folder and /util folder (or some other names)

  /** TODO:doc. */
  fun coordinate(coordinate: String): Pair<String, String> =
    // TODO: support classifier/extension or others
    coordinate.split(':').let {
      if (it.size < 2) Errors.fatal("Missing \":\" in ${coordinate}")
      if (it.size > 2) Errors.fatal("Multiple \":\" in ${coordinate}")
      Pair(it[0], it[1])
    }

  /** TODO:doc. */
  fun main( // TODO: rename
    // TODO: class Dependencies { remoterepos(default=central) localIndex localRepo artifacts() -> stdout or outputDir (.err) }
    // remote: URI,
    // local: File,
    // TODO: overwrite
    // TODO: mkdir
    // TODO: proxy
    localRepoDir: File,
    versionsDir: File,
    coordinates: List<Pair<String, String>>,
  ) {
    // TODO: error messages for exceptions
    // IndexSearcher

    // // We do not use IndexUpdateRequest because the interface for that is complicated
    // UriResourceHandler(remote).use { resourceHandler ->
    //   // TODO: URLs to source for IndexUpdateRequest and IndexReader examples
    //   // We used direct URI instead of IndexReader because we would need to manually download the .properties file anyway
    //   // Also, opening a ChunkReader does not give us the URL
    //   // Also, we do not use IndexWriter because that stores only the .properties file and lists only the updated chunks
    //   for (name in listOf(".properties", ".gz").map(INDEX_FILE_PREFIX::plus)) {
    //     log.info("downloading $name from $remote to $local")
    //     resourceHandler.locate(name).use { resource ->
    //       // TODO: if read() returns null
    //       resource.read().use { inputStream ->
    //         Channels.newChannel(inputStream).use { readableByteChannel ->
    //           local.resolve(name).outputStream().use { fileOutputStream ->
    //             val transferred = fileOutputStream.channel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE)
    //             log.info("downloaded ${name} from $remote to $local containing ${transferred} bytes")
    //           }
    //         }
    //       }
    //     }
    //   }
    // }
    // DefaultArtifact()

    // TODO: factor into utility class?
    val system = RepositorySystemSupplier().get()
    val localRepo = LocalRepository(localRepoDir)
    val session = DefaultRepositorySystemSession().apply {
      localRepositoryManager = system.newLocalRepositoryManager(this, localRepo)
    }
    // val session = MavenRepositorySystemUtils.newSession().apply {
    //   localRepositoryManager = system.newLocalRepositoryManager(this, localRepo)
    //   val transformer =
    //     ConflictResolver(NearestVersionSelector(), JavaScopeSelector(), SimpleOptionalitySelector(), JavaScopeDeriver())
    //   dependencyGraphTransformer = ChainedDependencyGraphTransformer(transformer, JavaDependencyContextRefiner())
    // }
    val remote = RemoteRepository
      // TODO: make configurable (including things like proxy)
      // TODO: see setContentType for why "default"
      .Builder("google-maven-central-ap", "default", "https://maven-central-asia.storage-download.googleapis.com/maven2")
      .setPolicy(RepositoryPolicy(true, RepositoryPolicy.UPDATE_POLICY_NEVER, RepositoryPolicy.CHECKSUM_POLICY_FAIL))
      .build()
    // TODO: use https://repo1.maven.org/maven2/ as backup
    val versionScheme = GenericVersionScheme() // TODO: locator.getService(VersionScheme::class.java)

    Parallel.run(
      log,
      coordinates,
      { (groupId, artifactId) -> // TODO: replace with `it`
        val groupPath = versionsDir.resolve(groupId.replace('.', '/'))
        Pair(groupPath.resolve("${artifactId}.version"), groupPath.resolve("${artifactId}.version.err"))
        // TODO: artifactFileX(groupId, artifactId)
      },
      {
        Exceptions.isClasses(
          it,
          org.eclipse.aether.transfer.MetadataTransferException::class,
          org.eclipse.aether.transfer.ChecksumFailureException::class,
        ) ||
          Exceptions.isClasses(
            it,
            org.ucombinator.jade.maven.NoVersioningTagException::class,
          )
      },
      // retry: true
      //   permanent: true
      //   permanent: false
      // retry: false
      //   permanent: true
      //   permanent: false
      ) { (groupId, artifactId) ->
      // TODO: use VersionResolver?
      // TODO: RELEASE_OR_SNAPSHOT?
      val metadata = DefaultMetadata(groupId, artifactId, "maven-metadata.xml", Metadata.Nature.RELEASE)
      val metadataRequest = MetadataRequest(metadata, remote, null)
      val metadataResults = system.resolveMetadata(session, listOf(metadataRequest))
      assert(metadataResults.size == 1)
      val metadataResult = metadataResults.first()
      if (metadataResult.metadata == null) {
        assert(metadataResult.exception != null) // TODO: Would the exception be thrown anyway?
        throw metadataResult.exception
      }
      val versioning =
        FileInputStream(metadataResult.metadata.file).use {MetadataXpp3Reader().read(it, false) }.versioning
        ?: throw NoVersioningTagException(groupId, artifactId)
      // TODO: link spec
      // TODO: check spec for other tags
      val version =
        versioning.release?.ifEmpty { null }
          ?: versioning.latest?.ifEmpty { null }
          ?: versioning.versions.filter(String::isNotEmpty).maxByOrNull(versionScheme::parseVersion)
          ?: throw NoVersionsInVersioningTagException(groupId, artifactId)
      "$groupId:$artifactId:$version\n".toByteArray()
    }
  }

  // 2	org.ucombinator.jade.maven.NoVersioningTagException
  // 2593	org.eclipse.aether.transfer.MetadataTransferException:org.eclipse.aether.transfer.ChecksumFailureException
  // 7639	org.eclipse.aether.transfer.MetadataNotFoundException (retry)
}
