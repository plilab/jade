package org.ucombinator.jade.maven

// import com.google.auth.oauth2.GoogleCredentials
// import com.google.cloud.storage.Bucket
// import com.google.cloud.storage.StorageOptions
// import org.ucombinator.jade.util.Xml.getChildrenByTagName
// import org.ucombinator.jade.util.Xml.getElementByTagName
import org.ucombinator.jade.util.Log
import java.io.File
// import java.io.FileInputStream
import kotlinx.coroutines.*
import org.ucombinator.jade.maven.googlecloudstorage.GcsBucket
import org.ucombinator.jade.util.Xml.getChildByTagName
// import org.ucombinator.jade.util.Xml
// import org.ucombinator.jade.util.Tuples.Fiveple

// import org.xml.sax.SAXException
import org.w3c.dom.Element
// import java.io.IOException
// import javax.xml.parsers.DocumentBuilderFactory

// See https://googleapis.dev/java/google-cloud-storage/latest/overview-summary.html
object MavenRepo {
  private val log = Log {}

  fun download(files: List<File>, destDir: File, authFile: File? = null) {
    log.info("Connecting to Maven repository")
    val bucket = GcsBucket.open(authFile)

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

  // fun readPom(dir: File, groupId: String, artifactId: String, version: String): Element? {
  //   val file = MavenRepo.pomFile(dir, groupId, artifactId, version)
  //   return Xml.readXml(file, "project")
  // }

  // val MAVEN_METADATA_XML = "maven-metadata.xml"
  // fun parseMavenMetadataXml(file: File): Fiveple<String?, String?, String?, String?, List<String>?>? {
  //   // log.trace("Parsing $file")

  //   if (file.name != MAVEN_METADATA_XML) {
  //     log.error("Wrong filename: expected '$MAVEN_METADATA_XML', actual '$file'")
  //     return null
  //   }

  //   val document = try {
  //     DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
  //   } catch (e: IOException) {
  //     log.error(e) { "Failed to read $file" }
  //     return null
  //   } catch (e: SAXException) {
  //     log.error(e) { "Failed to parse $file" }
  //     return null
  //   }

  //   val root = document.documentElement
  //   if (root.tagName != "metadata") {
  //     log.error("Wrong root tag in $file: expected 'metadata', actual '${root.tagName}'")
  //     return null
  //   }

  //   if (root.getElementsByTagName("plugins").length != 0) return null

  //   val groupId = root.getElementByTagName("groupId")?.textContent
  //   val artifactId = root.getElementByTagName("artifactId")?.textContent
  //   val latest = root.getElementByTagName("latest")?.textContent
  //   val release = root.getElementByTagName("release")?.textContent
  //   val versions = root.getElementByTagName("versions")?.getChildrenByTagName("version")?.map { it.textContent }

  //   return Fiveple(groupId, artifactId, latest, release, versions)
  // }

  const val METADATA_SUFFIX = "/maven-metadata.xml"
  const val METADATA_SUFFIX_LEN = METADATA_SUFFIX.length
  fun <T> listArtifacts(indexFile: File, block: (Sequence<Pair<String, String>>) -> T): T {
    fun f(line: String): Pair<String, String>? {
      val coor = line.substring(0, line.lastIndexOf('\t'))
      if (!coor.endsWith(METADATA_SUFFIX)) return null
      else {
        val x = coor.substring(0, coor.length - METADATA_SUFFIX_LEN)
        val i = x.lastIndexOf('/')
        if (i == -1) return null
        else return Pair(x.substring(0, i), x.substring(i + 1))
      }
    }
    return readIndex(indexFile, ::f, block)
  }

  const val INDEX_PREFIX = "maven2/"
  const val INDEX_PREFIX_LEN = INDEX_PREFIX.length
  fun <S, T> readIndex(indexFile: File, f: (String) -> S?, block: (Sequence<S>) -> T): T =
    indexFile.useLines { lines ->
      val data = lines
        .mapNotNull {
          if (!it.startsWith(INDEX_PREFIX)) null
          else if (it.startsWith("maven2/data/")) null // TODO: this could ignore groupIds that start with "data."
          else f(it.substring(INDEX_PREFIX_LEN))
        }
      return block(data)
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
