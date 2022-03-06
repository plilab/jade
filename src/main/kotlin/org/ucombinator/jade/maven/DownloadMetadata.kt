package org.ucombinator.jade.maven

import java.io.File

object DownloadMetadata {
  fun invoke(indexFile: File, destDir: File, authFile: File? = null) {
    val regex = """\t\d+$""".toRegex()
    val files = indexFile.useLines { lines ->
      lines
        .map { regex.replace(it, "") }
        .filter { it.endsWith("/maven-metadata.xml") }
    }.toList()

    val bucket = MavenRepo.open(authFile)
    val blobs = bucket.get(files)
    for (blob in blobs) {
      println(blob.name)
      blob.downloadTo(destDir.toPath().resolve(blob.name))
    }
  }
}
