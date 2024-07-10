package org.ucombinator.jade.maven

import org.eclipse.aether.artifact.Artifact
import org.eclipse.aether.repository.LocalRepository
import org.eclipse.aether.resolution.ArtifactRequest
import org.ucombinator.jade.util.Json
import org.ucombinator.jade.util.Log
import org.ucombinator.jade.util.Parallel

import java.io.File
import kotlin.time.Duration

/** TODO:doc. */
object Download {
  private val log = Log {}

  /** TODO:doc. */
  fun main( // TODO: rename
    // TODO: remoterepos(default=central)
    // remote: URI,
    // local: File,
    // TODO: overwrite
    // TODO: mkdir
    // TODO: proxy
    timeout: Duration,
    localRepoDir: File,
    artifactsDir: File, // TODO: or to stdout
    artifacts: List<Artifact>,
  ) {
    // TODO: error messages for exceptions
    val session = Maven.session(LocalRepository(localRepoDir))
    // TODO: support multiple remotes
    // val compressorStreamFactory = CompressorStreamFactory()

    Parallel.run(
      log,
      timeout,
      artifacts,
      { Maven.artifactFile(artifactsDir, it, ".artifact.json", ".artifact.err") },
      { false },
    ) { artifact ->
      // file.lines.map(::words)
      // TODO: check result.exceptions
      // .artifact
      // .repositories
      // .requestContext
      // TODO: take remotes from DependencyNode.remotes: group:artifact:ext:ver name,contentType,URL,mirror... ...
      val remotes = listOf(Maven.remote)
      Json.of(Maven.system.resolveArtifact(session, ArtifactRequest(artifact, remotes, null))).toString().toByteArray()
    }
  }

  // properties: {language=none, constitutesBuildPath=false, type=jar, includesDependencies=false}
  // includesDependencies=false
  // require(node.version === node.artifact.version)
  // data: Map<Object, Object>
  //   ConflictResolver.NODE_DATA_WINNER
}
