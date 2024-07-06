package org.ucombinator.jade.maven

import org.apache.maven.index.reader.Utils.INDEX_FILE_PREFIX
import org.apache.maven.index.reader.resource.UriResourceHandler
import org.ucombinator.jade.util.Log
import java.io.File
import java.net.URI
import java.nio.channels.Channels
import kotlin.io.resolve

// TODO: can this be parallelized?
// TODO: repos/central/data is old version of maven2?
// $ gsutil -o Credentials:gs_service_key_file=../smooth-splicer-342907-2a129f6f3cd4.json \
//     ls -l 'gs://maven-central/maven2/**'
// TODO: trailing commas
// TODO: search to "TODO" typos

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
    // TODO: error messages for exceptions

    // We do not use IndexUpdateRequest because the interface for that is complicated
    UriResourceHandler(remote).use { resourceHandler ->
      // TODO: URLs to source for IndexUpdateRequest and IndexReader examples
      // We used direct URI instead of IndexReader because we would need to manually download the .properties file anyway
      // Also, opening a ChunkReader does not give us the URL
      // Also, we do not use IndexWriter because that stores only the .properties file and lists only the updated chunks
      for (name in listOf(".properties", ".gz").map(INDEX_FILE_PREFIX::plus)) {
        log.info("downloading $name from $remote to $local")
        resourceHandler.locate(name).use { resource ->
          // TODO: if read() returns null
          resource.read().use { inputStream ->
            Channels.newChannel(inputStream).use { readableByteChannel ->
              local.resolve(name).outputStream().use { fileOutputStream ->
                val transferred = fileOutputStream.channel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE)
                log.info("downloaded ${name} from $remote to $local containing ${transferred} bytes")
              }
            }
          }
        }
      }
    }
  }
}
