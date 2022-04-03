package org.ucombinator.jade.util

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.storage.Bucket
import com.google.cloud.storage.StorageOptions
import org.ucombinator.jade.util.Log
import java.io.File
import java.io.FileInputStream
import kotlinx.coroutines.*

import org.xml.sax.SAXException
import org.w3c.dom.Element
import java.io.IOException
import javax.xml.parsers.DocumentBuilderFactory

object Xml {
  private val log = Log {}
  fun readXml(file: File, rootTag: String): Element? {
    val document = try {
      DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
    } catch (e: IOException) {
      // log.error(e) { "Failed to read $file" }
      return null
    } catch (e: SAXException) {
      // log.error(e) { "Failed to parse $file" }
      return null
    }

    val root = document.documentElement
    if (root.tagName != rootTag) {
      // log.error("Wrong root tag '${root.tagName} in $file")
      return null
    }

    return root
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

  fun Element.getElementByTagName(tag: String): Element? {
    val children = this.getElementsByTagName(tag)
    return if (children.length == 1) children.item(0) as? Element else null
  }
}
