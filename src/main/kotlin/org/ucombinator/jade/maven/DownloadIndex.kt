package org.ucombinator.jade.maven

import com.google.cloud.storage.Storage
import java.io.EOFException
import java.io.File
import java.io.FileWriter
import java.io.RandomAccessFile

object DownloadIndex {
  const val FLUSH_FREQUENCY = 1000
  fun download(
    indexFile: File,
    authFile: File? = null,
    resume: Boolean = false,
    maxResults: Long = 0L,
    pageSize: Long = 0L,
    prefix: String? = null,
    startOffset: String? = null
  ) {
    val trueStartOffset =
      if (resume) {
        if (startOffset !== null) throw Exception("TODO")
        if (!indexFile.exists()) throw Exception("TODO")
        val offset = lastFullLine(indexFile).substringBeforeLast('\t', missingDelimiterValue = "")
        if (offset == "") throw Exception("TODO")
        offset
      } else {
        if (indexFile.exists()) throw Exception("TODO")
        startOffset
      }

    FileWriter(indexFile, true).buffered().use { writer ->
      var count = 0L

      val bucket = MavenRepo.open(authFile)

      var options = listOf(Storage.BlobListOption.fields(Storage.BlobField.NAME, Storage.BlobField.SIZE))
      if (pageSize != 0L) options += Storage.BlobListOption.pageSize(pageSize)
      if (prefix !== null) options += Storage.BlobListOption.prefix(prefix)
      if (trueStartOffset !== null) options += Storage.BlobListOption.startOffset(trueStartOffset)

      val blobs = bucket.list(*options.toTypedArray()).iterateAll()

      // val blobs = open(authFile, pageSize, prefix, trueStartOffset)
      var checkStartOffset = resume
      for (blob in blobs) {
        if (checkStartOffset && blob.name == trueStartOffset) {
          checkStartOffset = false
          continue
        }
        writer.write("${blob.name}\t${blob.size}\n")
        count++
        if (count >= maxResults) break
        if (count % FLUSH_FREQUENCY == 0L) writer.flush()
      }
    }
  }

  const val BUFSIZ = 8192
  fun lastFullLine(file: File): String {
    RandomAccessFile(file, "rw").use { input ->
      var bytes = ByteArray(BUFSIZ)
      fun findNewlineBefore(pos: Long): Long {
        for (end in pos downTo 0 step bytes.size.toLong()) {
          val start = end - bytes.size
          input.seek(if (start < 0) 0 else start)
          bytes.fill(0.toByte())
          try {
            input.readFully(bytes) // TODO: check count read, check if downTo goes to zero
          } catch (_: EOFException) {}
          val i = bytes.indexOf('\n'.code.toByte())
          if (i >= 0) { // TODO: and not last byte
            // read line starting from that '\n'
            return start + i
          }
        }
        return -1
      }

      // Find the last '\n' (this is probably the last byte but we check in case an incomplete line was written)
      val lastNewline = findNewlineBefore(input.length())
      if (lastNewline < 0) return ""
      // Find the second to last '\n'
      val nextToLastNewline = findNewlineBefore(lastNewline)
      if (nextToLastNewline < 0) return ""

      input.seek(nextToLastNewline + 1L)
      val lastLine = ByteArray((lastNewline - nextToLastNewline - 1L).toInt())
      input.readFully(lastLine)

      input.setLength(lastNewline + 1L)

      return String(lastLine)
    }
  }
}
