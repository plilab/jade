package org.ucombinator.jade.maven

import org.ucombinator.jade.util.Log
import org.ucombinator.jade.util.Xml
import org.xml.sax.SAXException
import org.w3c.dom.Element
import java.io.File
import java.io.IOException
import javax.xml.parsers.DocumentBuilderFactory
import org.ucombinator.jade.util.Xml.getChildByTagName
import org.ucombinator.jade.util.Xml.getChildrenByTagName

object DownloadParents {
  private val log = Log {}
  var count = 0
  fun main(srcDir: File, dstDir: File, authFile: File? = null) {
    var noLatest = 0
    var noRelease = 0
    var wrongFileName = 0
    var withVersions = 0
    val dir = srcDir.resolve("maven2")
    var dataDir = dir.resolve("data")
    // TODO: loop until done
    val versions = dir.walk()
      .onEnter { it != dataDir }
      .flatMap { parse(dir, it) }
      .toSet()
      .map { (group, artifact, version) -> MavenRepo.pomFile(dir, group, artifact, version) }
      .toList()

    // println(wrongFileName)
    // println(withVersions)
    // for (v in versions) {
    //   println(v)
    // }

    MavenRepo.download(versions, dstDir, authFile)
  }

  fun parse(dir: File, file: File): Set<Triple<String, String, String>> {
    var results = setOf<Triple<String, String, String>>()

    if (!file.name.endsWith(".pom")) return results
    if (!file.isFile) return results

    val root = Xml.readXml(file, "project") ?: return results

    // if (root.getChildrenByTagName("artifactId").size != 1) log.error("No artifactId in $file")

    val parent = root.getChildByTagName("parent")
    // if (root.getChildrenByTagName("version").size != 1
    // && (parents.size != 1
    // || parents.get(0).getChildrenByTagName("version").size != 1
    // )) log.error("No version in $file")

    // parent/groupId
    // artifactId
    // version

    if (parent !== null) {
      val (groupId, artifactId, version) = MavenRepo.getCoord(parent)
      if (groupId === null) { log.error("No groupId in $file"); return results } // TODO: break
      if (artifactId === null) { log.error("No artifactId in $file"); return results }
      if (version === null) { log.error("No version in $file"); return results }

      results += Triple(groupId, artifactId, version)
    }

    val dependencyManagement = root.getChildByTagName("dependencyManagement")
    if (dependencyManagement !== null) {
      val dependencies = dependencyManagement.getChildByTagName("dependencies")
      if (dependencies !== null) {
        for (dependency in dependencies.getChildrenByTagName("dependency")) {
          val scope = dependency.getChildByTagName("scope")?.textContent?.trim()
          if (scope != "import") { continue }

          val (groupId, artifactId, version) = MavenRepo.getCoord(dependency)
          if (groupId === null) { log.error("No groupId in $file"); continue }
          if (artifactId === null) { log.error("No artifactId in $file"); continue }
          if (version === null) { continue }

          results += Triple(groupId, artifactId, version)
        }
      }
    }

    return results
  }

  fun Element.getChildrenByTagName(tag: String): List<Element> {
    val children = this.childNodes
    return (0 until children.length)
      .mapNotNull { children.item(it) as? Element }
      .filter { it.tagName == tag }
  }

  fun Element.getChildByTagName(tag: String): Element? {
    val children = this.getChildrenByTagName(tag)
    return if (children.size == 1) children[0] else null
  }
}
