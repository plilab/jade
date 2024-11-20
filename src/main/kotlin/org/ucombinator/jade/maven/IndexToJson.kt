package org.ucombinator.jade.maven

import org.apache.maven.index.reader.IndexReader
import org.apache.maven.index.reader.RecordExpander
import org.apache.maven.index.reader.resource.PathWritableResourceHandler
import org.ucombinator.jade.util.Errors
import org.ucombinator.jade.util.Log

import java.io.File
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

// TODO: can this be parallelized?
// TODO: repos/central/data is old version of maven2?
// $ gsutil -o Credentials:gs_service_key_file=../smooth-splicer-342907-2a129f6f3cd4.json \
//     ls -l 'gs://maven-central/maven2/**'
// TODO: trailing commas
// TODO: search to "TODO" typos

// TODO: Format is JsonLines/NDJSON

/** TODO:doc. */
object IndexToJson { // TODO: rename "export index" or just "index" if we can combine with download or "index-export"
  private val log = Log {}

  enum class Kind { INDEX, CHUNK, RECORD, EXPANDED_RECORD }

  interface Key {
    fun key() = toString().replaceFirstChar(Char::lowercase)

    companion object {
      val kind = Kind::class.simpleName!!.replaceFirstChar(Char::lowercase)
    }

    // TODO: NOTE: capitalization and spelling of these matter
    enum class Index : Key { IndexId, PublishedTimestamp, Incremental, ChunkNames }
    enum class Chunk : Key { Name, Timestamp, Version }
    enum class Record : Key { /* No keys */ }
    enum class ExpandedRecord : Key { Type }
  }

  // https://github.com/ndjson/ndjson-spec
  // https://github.com/wardi/jsonlines
  // https://jsonlines.org/

  // https://maven.apache.org/maven-indexer/indexer-reader/apidocs/org/apache/maven/index/reader/Record.html
  // https://maven.apache.org/maven-indexer/indexer-reader/apidocs/org/apache/maven/index/reader/Record.Type.html

  private fun printJson(kind: Kind, vararg entries: Pair<Key, Any>) {
    printJson(kind, entries.toMap().mapKeys { it.key.key() })
  }

  // TODO: use Json.kt
  // TODO: "null" in {"kind":"EXPANDED_RECORD","type":"ARTIFACT_ADD","hasJavadoc":false,"version":"1.2.2","recordModified":1318434019089,"groupId":"xstream","fileSize":-1,"fileExtension":"pom","artifactId":"xstream","packaging":"null","hasSources":false,"hasSignature":false,"fileModified":1182839150000}
  private fun printJson(kind: Kind, entries: Map<String, Any>) {
    println(
      JsonObject(
        mapOf(Key.kind to JsonPrimitive(kind.toString())) +
          entries.mapValues { (_, value) ->
            when (value) {
              is Boolean -> JsonPrimitive(value)
              is Int -> JsonPrimitive(value)
              is Long -> JsonPrimitive(value)
              is String -> JsonPrimitive(value)
              is Array<*> -> when {
                value.isArrayOf<String>() -> JsonArray(value.map { JsonPrimitive(it as String) })
                else -> Errors.unmatchedType(value)
              }
              else -> Errors.unmatchedType(value)
            }
          }
      )
    )
  }

  // TODO: take file://relative URL for remote

  /** TODO:doc. */
  fun main( // TODO: rename to "list" or "toJson" or "export"? TODO: take outputstream as argument
    local: File,
    index: Boolean,
    chunk: Boolean,
    record: Boolean,
    expandedRecord: Boolean,
  ) {
    val expander = RecordExpander()

    // TODO: see if BufferedResourceHandler brings any speedup
    PathWritableResourceHandler(local.toPath()).use { resourceHandler ->
      IndexReader(null, resourceHandler).use { indexReader ->
        if (index) {
          printJson(
            Kind.INDEX,
            Key.Index.IndexId to indexReader.indexId,
            Key.Index.PublishedTimestamp to indexReader.publishedTimestamp.time,
            Key.Index.Incremental to indexReader.isIncremental,
            Key.Index.ChunkNames to indexReader.chunkNames.toTypedArray(),
          )
        }

        // TODO: combine "for" with "use"
        for (chunkReader in indexReader) {
          chunkReader.use { _ ->
            if (chunk) {
              printJson(
                Kind.CHUNK,
                Key.Chunk.Name to chunkReader.name,
                Key.Chunk.Timestamp to chunkReader.timestamp.time,
                Key.Chunk.Version to chunkReader.version,
              )
            }

            for (recordMap in chunkReader) {
              if (record) printJson(Kind.RECORD, recordMap)

              if (expandedRecord) {
                expander.apply(recordMap).let { expanded ->
                  printJson(
                    Kind.EXPANDED_RECORD,
                    mapOf(Key.ExpandedRecord.Type.key() to expanded.type.toString()) +
                      expanded.expanded.mapKeys { it.key.name },
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}
