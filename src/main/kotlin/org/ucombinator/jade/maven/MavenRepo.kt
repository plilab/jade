package org.ucombinator.jade.maven

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.storage.Bucket
import com.google.cloud.storage.StorageOptions
import org.ucombinator.jade.util.Xml.getChildrenByTagName
import org.ucombinator.jade.util.Xml.getElementByTagName
import org.ucombinator.jade.util.Log
import java.io.File
import java.io.FileInputStream
import kotlinx.coroutines.*
import org.ucombinator.jade.util.Xml.getChildByTagName
import org.ucombinator.jade.util.Xml
import org.ucombinator.jade.util.Tuples.Fiveple

import org.xml.sax.SAXException
import org.w3c.dom.Element
import java.io.IOException
import javax.xml.parsers.DocumentBuilderFactory

// See https://googleapis.dev/java/google-cloud-storage/latest/overview-summary.html
object MavenRepo {
  private val log = Log {}
  const val MAVEN_BUCKET = "maven-central"
  const val ENV_VAR = "GOOGLE_APPLICATION_CREDENTIALS"

  fun open(authFile: File? = null): Bucket {
    val storage =
      if (authFile !== null) {
        val credentials = GoogleCredentials.fromStream(FileInputStream(authFile))
          .createScoped(listOf("https://www.googleapis.com/auth/cloud-platform"))
        StorageOptions.newBuilder().setCredentials(credentials).build().getService()
      } else {
        StorageOptions.getDefaultInstance().getService()
      }

    return storage.get(MAVEN_BUCKET)
  }

  fun download(files: List<File>, destDir: File, authFile: File? = null) {
    log.info("Connecting to Maven repository")
    val bucket = MavenRepo.open(authFile)

    log.info("Downloading blobs")
    runBlocking {
      for (file in files) {
        val destFile = destDir.resolve(file)
        if (destFile.exists()) {
          // log.debug("Skipping existing blob for $destFile")
        } else {
          async(Dispatchers.IO) {
            // log.info("Downloading blob to $destFile")
            val blob = bucket.get(file.toString())
            if (blob === null) {
              log.error("Skipping missing blob $destFile")
            } else {
              log.info("Downloading blob to $destFile")
              destFile.parentFile.mkdirs()
              blob.downloadTo(destFile.toPath())
            }
          }
        }
      }
    }
  }

  fun getCoord(element: Element): Triple<String?, String?, String?> {
    val groupId = element.getChildByTagName("groupId")?.textContent?.trim()
    val artifactId = element.getChildByTagName("artifactId")?.textContent?.trim()
    val version = element.getChildByTagName("version")?.textContent?.trim()
    return Triple(groupId, artifactId, version)
  }

  fun pomFile(dir: File, groupId: String, artifactId: String, version: String): File =
    File("$dir/${groupId.replace(".", "/")}/$artifactId/$version/$artifactId-$version.pom")

  fun readPom(dir: File, groupId: String, artifactId: String, version: String): Element? {
    val file = MavenRepo.pomFile(dir, groupId, artifactId, version)
    return Xml.readXml(file, "project")
  }

  val MAVEN_METADATA_XML = "maven-metadata.xml"
  fun parseMavenMetadataXml(file: File): Fiveple<String?, String?, String?, String?, List<String>?>? {
    // log.trace("Parsing $file")

    if (file.name != MAVEN_METADATA_XML) {
      log.error("Wrong filename: expected '$MAVEN_METADATA_XML', actual '$file'")
      return null
    }

    val document = try {
      DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
    } catch (e: IOException) {
      log.error(e) { "Failed to read $file" }
      return null
    } catch (e: SAXException) {
      log.error(e) { "Failed to parse $file" }
      return null
    }

    val root = document.documentElement
    if (root.tagName != "metadata") {
      log.error("Wrong root tag in $file: expected 'metadata', actual '${root.tagName}'")
      return null
    }

    if (root.getElementsByTagName("plugins").length != 0) return null

    val groupId = root.getElementByTagName("groupId")?.textContent
    val artifactId = root.getElementByTagName("artifactId")?.textContent
    val latest = root.getElementByTagName("latest")?.textContent
    val release = root.getElementByTagName("release")?.textContent
    val versions = root.getElementByTagName("versions")?.getChildrenByTagName("version")?.map { it.textContent }

    return Fiveple(groupId, artifactId, latest, release, versions)
  }

  // fun readIndex(indexFile: File, predicate: (String, Int) -> Boolean) {
  //   log.info("Reading index $indexFile")
  //   val files = indexFile.useLines { lines ->
  //     val STR = "/maven-metadata.xml"
  //     val STR_LEN = STR.length
  //     lines
  //       .mapNotNull {
  //         val end = it.lastIndexOf('\t')
  //         val name = it.substring(0, end)
  //         val size = it.substring(end + 1).toInt()
  //         if (predicate(name, size)) Pair(name, end) else null
  //       }
  //       .toList()
  //   }
  // }
}
