package org.ucombinator.jade.maven

import com.google.cloud.storage.Storage
import org.ucombinator.jade.maven.googlecloudstorage.GcsBucket
import org.ucombinator.jade.util.Log
import java.io.File
import java.io.FileWriter
import java.io.RandomAccessFile

// TODO: can this be parallelized?
// TODO: repos/central/data is old version of maven2?
// $ gsutil -o Credentials:gs_service_key_file=../smooth-splicer-342907-2a129f6f3cd4.json ls -l 'gs://maven-central/maven2/**'
// TDOO: trailing commas
object DownloadIndex {
  private val log = Log {}

  fun main(
    indexFile: File,
    authFile: File? = null,
    resume: Boolean = false,
    maxResults: Long = 0L,
    pageSize: Long = 0L,
    prefix: String? = null,
    startOffset: String? = null,
    flushFrequency: Long = 1L shl 14,
  ) {
    // TODO: add auto-removal of indexFile
    val trueStartOffset =
      if (resume) {
        if (startOffset !== null) TODO() // TODO: use `require`
        if (!indexFile.exists()) TODO()
        val offset = lastFullLine(indexFile).substringBeforeLast('\t', missingDelimiterValue = "")
        if (offset == "") TODO()
        offset
      } else {
        if (indexFile.exists()) TODO()
        startOffset
      }

    FileWriter(indexFile, true).buffered().use { writer ->
      val bucket = GcsBucket.open(authFile)

      var options = listOf(Storage.BlobListOption.fields(Storage.BlobField.NAME, Storage.BlobField.SIZE))
      if (pageSize != 0L) options += Storage.BlobListOption.pageSize(pageSize)
      if (prefix !== null) options += Storage.BlobListOption.prefix(prefix)
      if (trueStartOffset !== null) options += Storage.BlobListOption.startOffset(trueStartOffset)

      val blobs = bucket.list(*options.toTypedArray()).iterateAll()

      var checkStartOffset = resume
      for ((index, blob) in blobs.withIndex()) {
        if (maxResults != 0L && index >= maxResults) break
        if (checkStartOffset && blob.name == trueStartOffset) {
          checkStartOffset = false
          continue
        }
        val line = "${blob.name}\t${blob.size}"
        log.trace { "writing line ${index}: ${line}" }
        writer.write(line + "\n")
        if (index % flushFrequency == 0L) {
          writer.flush()
          log.debug { "flushing line ${index}: ${line}" }
        }
      }
    }
  }

  fun lastFullLine(file: File): String {
    RandomAccessFile(file, "rw").use { input ->
      fun newlineBefore(end: Long): Long? =
        (end - 1 downTo 0).find { input.seek(it); input.readByte().toInt().toChar() == '\n' }

      // The last newline is probably the last byte but we check in case an incomplete line was written
      val lastNewline = newlineBefore(input.length()) ?: return ""
      val secondToLastNewline = newlineBefore(lastNewline) ?: return ""

      input.seek(secondToLastNewline + 1)
      return input.readLine()
    }
  }
}
