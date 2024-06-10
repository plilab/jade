package org.ucombinator.jade.util

import org.w3c.dom.Element
import org.xml.sax.SAXException
import java.io.File
import java.io.IOException
import javax.xml.parsers.DocumentBuilderFactory

object Xml {
  fun readXml(file: File, rootTag: String): Element? {
    val document = @Suppress("SwallowedException") try {
      DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
    } catch (e: IOException) {
      return null
    } catch (e: SAXException) {
      return null
    }

    val root = document.documentElement
    if (root.tagName != rootTag) {
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
