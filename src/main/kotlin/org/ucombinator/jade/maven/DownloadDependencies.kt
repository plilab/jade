package org.ucombinator.jade.maven

import org.ucombinator.jade.util.Log
import org.ucombinator.jade.util.Xml.getChildByTagName
import org.ucombinator.jade.util.Xml.getChildrenByTagName
import org.ucombinator.jade.util.Xml
import org.xml.sax.SAXException
import org.w3c.dom.Element
import java.io.File
import java.io.IOException
import javax.xml.parsers.DocumentBuilderFactory

object DownloadDependencies {
  private val log = Log {}
  var count = 0
  fun main(srcDir: File, dstDir: File, authFile: File? = null, threads: Int = 0) {
    val dir = srcDir.resolve("maven2")
    var dataDir = dir.resolve("data")
    // TODO: loop until done
    val versions = dir.walk()
      .onEnter { it != dataDir }
      .mapNotNull { parse(dir, it) }
      .toList()

    // println(wrongFileName)
    // println(withVersions)
    // for (v in versions) {
    //   println(v)
    // }
    println("count $count")

    // MavenRepo.download(versions, dstDir, authFile, threads)
  }

  fun parse(dir: File, file: File): Triple<String, String, String>? {
    if (!file.name.endsWith(".pom")) return null
    if (!file.isFile) return null
    // TODO: skip sha1 check

    val root = Xml.readXml(file, "project") ?: return null
    val parentMap = mutableMapOf<String, MutableMap<String, MutableSet<String>>>()
    getDependencyManagement(dir, listOf(/*file*/), root, parentMap)
    // if (parentMap === null) { log.error("Failed to load parentMap in $file"); return null }

    // if (root.getChildrenByTagName("artifactId").size != 1) log.error("No artifactId in $file")

    val dependencies = root.getChildByTagName("dependencies")
    // parent/groupId
    // artifactId
    // version

    if (dependencies === null) { return null }
    else {
      for (dependency in dependencies.getChildrenByTagName("dependency")) {
        // println("dep")
        val scope = dependency.getChildByTagName("scope")?.textContent?.trim()
        if (scope === null || scope == "compile") {
          val (groupId, artifactId, version) = MavenRepo.getCoord(dependency)
          if (groupId === null) { log.error("No groupId in $file"); continue }
          if (artifactId === null) { log.error("No artifactId in $file"); continue }
          if (version == null && parentMap.getOrPut(groupId, { mutableMapOf() }).getOrPut(artifactId, { mutableSetOf() }).isEmpty()) {
            count++;
            // log.warn("No version in $groupId:$artifactId $file $count");
            // println(parentMap)
            return null
          }
          // if (groupId.startsWith("$")) { log.error(groupId) }
          // if (artifactId.startsWith("$")) {
        }
      }

      // val file = "$dir/${parent.getChildByTagName("groupId")!!.textContent.trim().replace(".", "/")}/${parent.getChildByTagName("artifactId")!!.textContent.trim()}/${parent.getChildByTagName("version")!!.textContent.trim()}/${parent.getChildByTagName("artifactId")!!.textContent.trim()}-${parent.getChildByTagName("version")!!.textContent.trim()}.pom"
      return null // Triple(parent.getChildByTagName("groupId")!!.textContent.trim(), parent.getChildByTagName("artifactId")!!.textContent.trim(), parent.getChildByTagName("version")!!.textContent.trim())

    }
  }

  fun getParent(dir: File, files: List<Triple<String, String, String>>, parent: Element?, parentMap: MutableMap<String, MutableMap<String, MutableSet<String>>>) {
    if (parent === null) { return }
    else {
      val (groupId, artifactId, version) = MavenRepo.getCoord(parent)
      // println("files: $files")
      if (groupId === null) { log.error("No groupId in $files"); return }
      if (artifactId === null) { log.error("No artifactId in $files"); return }
      if (version === null) { log.error("No version in $files"); return }
      val coord = Triple(groupId, artifactId, version)

      if (coord in files) {
        log.error("Cycle in $coord and $files")
        return
      }

      val parent = MavenRepo.readPom(dir, groupId, artifactId, version) ?: return
      getDependencyManagement(dir, files + coord, parent, parentMap)
    }
  }

  // TODO: mutable files list
  fun getDependencyManagement(dir: File, files: List<Triple<String,String,String>>, root: Element, parentMap: MutableMap<String, MutableMap<String, MutableSet<String>>>) {
    getParent(dir, files, root.getChildByTagName("parent"), parentMap)

    val dependencyManagement = root.getChildByTagName("dependencyManagement")
    if (dependencyManagement !== null) {
      val dependencies = dependencyManagement.getChildByTagName("dependencies")
      if (dependencies !== null) {
        for (dependency in dependencies.getChildrenByTagName("dependency")) {
          val (groupId, artifactId, version) = MavenRepo.getCoord(dependency)
          if (groupId === null) { log.error("No groupId in $files"); continue }
          if (artifactId === null) { log.error("No artifactId in $files"); continue }
          if (version === null) { continue }

          parentMap.getOrPut(groupId, { mutableMapOf() }).getOrPut(artifactId, { mutableSetOf() }).add(version)

          val scope = dependency.getChildByTagName("scope")?.textContent?.trim()
          if (scope == "import") {
            getParent(dir, files, dependency, parentMap)
          }
        }
      }
    }
  }
}
