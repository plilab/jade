package org.ucombinator.jade.maven

import org.apache.maven.index.reader.Utils.INDEX_FILE_PREFIX
import org.apache.maven.index.reader.resource.UriResourceHandler
import org.ucombinator.jade.util.Log

import java.io.File
import java.net.URI
import java.nio.channels.Channels
import kotlin.io.resolve

/** TODO:doc. */
object Index {
  private val log = Log {}

  /** TODO:doc. */
  fun main( // TODO: rename
    remote: URI,
    local: File,
    // TODO: overwrite
    // TODO: mkdir
    // TODO: proxy
  ) {
    // We do not use IndexUpdateRequest because the interface for that is complicated.
    // Also, we manually download instead of IndexReader because we would need to manually download the .properties file anyway.
    // Also, opening a ChunkReader does not give us the URL.
    // Also, we do not use IndexWriter because that stores only the .properties file and lists only the updated chunks.
    var remoteDir = remote.resolve(".index/")
    UriResourceHandler(remoteDir).use { resourceHandler ->
      for (name in listOf(".properties", ".gz").map(INDEX_FILE_PREFIX::plus)) {
        log.info("downloading $name from $remoteDir to $local")
        resourceHandler.locate(name).use { resource ->
          resource.read().use { inputStream ->
            // TODO: inputStream == null
            Channels.newChannel(inputStream).use { readableByteChannel ->
              // TODO: outputStream throws java.io.FileNotFoundException
              local.resolve(name).outputStream().use { fileOutputStream ->
                val transferred = fileOutputStream.channel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE)
                log.info("downloaded ${name} from $remoteDir to $local containing ${transferred} bytes")
              }
            }
          }
        }
      }
    }
  }
}
