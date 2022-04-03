package org.ucombinator.jade.maven

import kotlinx.coroutines.*
import org.ucombinator.jade.util.Log
import java.io.File

object DownloadMetadata {
  private val log = Log {}
  fun main(indexFile: File, destDir: File, authFile: File? = null) {
    log.info("Reading index $indexFile")
    val files = indexFile.useLines { lines ->
      val STR = "/maven-metadata.xml"
      val STR_LEN = STR.length
      lines
        .mapNotNull {
          val end = it.lastIndexOf('\t')
          if (it.regionMatches(end - STR_LEN, STR, 0, STR_LEN)) File(it.substring(0, end)) else null
        }
        // .take(100)
        .toList()
    }

    MavenRepo.download(files, destDir, authFile)
  }
}
