package org.ucombinator.jade.maven

import org.ucombinator.jade.util.Log
import org.xml.sax.SAXException
import org.w3c.dom.Element
import java.io.File
import java.io.IOException
import javax.xml.parsers.DocumentBuilderFactory

object DownloadPoms {
  private val log = Log {}
  fun main(srcDir: File, dstDir: File, authFile: File? = null) {
    var noLatest = 0
    var noRelease = 0
    var wrongFileName = 0
    var withVersions = 0
    val dir = srcDir.resolve("maven2")
    var dataDir = dir.resolve("data")
    val versions = dir.walk()
      .onEnter { it != dataDir }
      .filter { it.name == "maven-metadata.xml" }
      .mapNotNull { MavenRepo.parseMavenMetadataXml(it) }
      .mapNotNull { (group, artifact, _, release, _) ->
        if (group === null || artifact === null || release === null) {
          null
        } else {
          MavenRepo.pomFile(dir, group, artifact, release)
        }
      }
      .toList()

    // println(wrongFileName)
    // println(withVersions)
    // for (v in versions) {
    //   println(v)
    // }

    MavenRepo.download(versions, dstDir, authFile)
  }

  // val x = dir.resolve("${groupId.replace(".", "/")}/${artifactId}/maven-metadata.xml")
  // if (file != x) {
  //   // wrongFileName++
  //   // log.error("File name doesn't match: $groupId $artifactId ${file.toString()} $x")
  //   return null
  // }

}
