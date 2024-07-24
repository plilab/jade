package org.ucombinator.jade.maven

import org.apache.maven.index.reader.Utils.INDEX_FILE_PREFIX
import org.apache.maven.index.reader.resource.UriResourceHandler
import org.apache.maven.index.reader.ResourceHandler.Resource
import org.ucombinator.jade.util.Log

import java.io.File
import java.net.URI
import java.nio.channels.Channels
import kotlin.io.resolve

/** TODO:doc. */
object Mirrors {
  private val log = Log {}

  /** TODO:doc. */
  fun main( // TODO: rename
    remote: URI,
    // TODO: overwrite
    // TODO: mkdir
    // TODO: proxy
  ) {
    // There does not seem to be a Maven API for this so we download manually
    val bytes = UriResourceHandler(remote).use { resourceHandler ->
      resourceHandler.locate(".meta/repository-metadata.xml").use { resource ->
        resource.read().use { inputStream ->
          // TODO: inputStream == null
          inputStream.readAllBytes()
        }
      }
    }
    println(bytes)
  }
}
