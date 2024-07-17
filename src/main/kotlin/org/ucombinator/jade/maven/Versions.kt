package org.ucombinator.jade.maven

import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader
import org.eclipse.aether.metadata.DefaultMetadata
import org.eclipse.aether.metadata.Metadata
import org.eclipse.aether.repository.LocalRepository
import org.eclipse.aether.resolution.MetadataRequest
import org.eclipse.aether.util.version.GenericVersionScheme
import org.ucombinator.jade.util.Exceptions
import org.ucombinator.jade.util.Log
import org.ucombinator.jade.util.Parallel

import java.io.File
import java.io.FileInputStream
import kotlin.time.Duration

// TODO: remove non-aether maven imports

/** TODO:doc. */
object Versions { // TODO: rename to versions
  private val log = Log {}

  /** TODO:doc. */
  fun main( // TODO: rename
    timeout: Duration,
    // TODO: remoterepos(default=central)
    // TODO: overwrite
    // TODO: mkdir
    // TODO: proxy
    localRepoDir: File,
    versionsDir: File, // TODO: or to stdout
    artifacts: List<Pair<String, String>>,
  ) {
    // TODO: error messages for exceptions

    // TODO: factor into utility class?
    val session = Maven.session(LocalRepository(localRepoDir))
    // TODO: use https://repo1.maven.org/maven2/ as backup
    val versionScheme = GenericVersionScheme() // TODO: locator.getService(VersionScheme::class.java)

    Parallel.run(
      log,
      timeout,
      artifacts,
      { Maven.artifactFile(versionsDir, it, ".version", ".version.err") },
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
    ) { (groupId, artifactId) ->
      // TODO: use VersionResolver?
      // TODO: RELEASE_OR_SNAPSHOT?
      val metadata = DefaultMetadata(groupId, artifactId, "maven-metadata.xml", Metadata.Nature.RELEASE)
      val metadataRequest = MetadataRequest(metadata, Maven.remote, null)
      val metadataResult = Maven.system.resolveMetadata(session, listOf(metadataRequest)).single()
      if (metadataResult.metadata == null) throw metadataResult.exception
      val versioning =
        FileInputStream(metadataResult.metadata.file).use { MetadataXpp3Reader().read(it, false) }.versioning
          ?: throw NoVersioningTagException(groupId, artifactId)
      // TODO: link spec
      val version =
        versioning.release?.ifEmpty { null }
          ?: versioning.latest?.ifEmpty { null }
          ?: versioning.versions.filter(String::isNotEmpty).maxByOrNull(versionScheme::parseVersion)
          ?: throw NoVersionsInVersioningTagException(groupId, artifactId)
      "${groupId}:${artifactId}:${version}\n".toByteArray()
    }
  }
}

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
