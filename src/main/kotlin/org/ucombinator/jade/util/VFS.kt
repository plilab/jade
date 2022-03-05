package org.ucombinator.jade.util
// initial implementation: dir or file only (no "inner" paths)

// url
// dir(traversedPath, remainingPath)

// zip(scan entire file)

// file(traversedPath, remainingPath, bytes)

// // TODO: caching
// // TODO: http, https, ftp, sftp, scp, git
// // TODO: gzip, tar, bzip2

// zip
//   .apk
//   .war
//   .ear
//   jar
//   jmod
// .tar
// .gz
// .bz2
// .xz
// .Z (compress)

// .iso
// .cab
// .dmg
// .rar

// https://stackoverflow.com/questions/315618/how-do-i-extract-a-tar-file-in-java
//   https://commons.apache.org/proper/commons-compress/examples.html
// https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/zip/package-summary.html
// https://commons.apache.org/proper/commons-vfs/
//   https://commons.apache.org/proper/commons-vfs/filesystems.html
// https://en.wikipedia.org/wiki/List_of_archive_formats

import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveException
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.compressors.CompressorException
import org.apache.commons.compress.compressors.CompressorStreamFactory
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

fun <A, B, C> List<Pair<A, B>>.map(f: (A, B) -> C): List<C> = this.map { p -> f(p.first, p.second) }

class VFS {
  companion object {
    private val CLASS_SIGNATURE = listOf(0xca, 0xfe, 0xba, 0xbe).map { it.toUByte().toByte() }
    private val JMOD_SIGNATURE = listOf(0x4a, 0x4d, 0x01, 0x00, 0x50, 0x4b, 0x03, 0x04).map { it.toUByte().toByte() }
    private val JMOD_OFFSET = 4
  }

  // TODO: keep list of already traversed paths

  val result = mutableMapOf<List<File>, ByteArray>()
  val seen = mutableSetOf<File>()

  fun dir(file: File) {
    file
      .walk()
      .onEnter {
        if (seen.contains(it)) {
          false
        } else {
          seen.add(it)
          // match_file_branch
          true
        }
      }
      .filter { it.isFile() }
      // .filter { match_file_leaf }
      .forEach { name -> FileInputStream(name).use { read(listOf(name), it) } }
  }

  fun headerMatches(inputStream: InputStream, header: List<Byte>): Boolean {
    assert(inputStream.markSupported())
    inputStream.mark(header.size)
    // `ByteArray`s always compare as false (I don't know why), so we use List<Byte> instead
    val h = inputStream.readNBytes(header.size).toList()
    inputStream.reset()
    return h == header
  }

  // TODO: better handling of closing input streams
  fun read(name: List<File>, inputStream: InputStream) {
    // We wrap in a BufferedInputStream to ensure we have support for `mark()` and `reset()`
    var input = BufferedInputStream(inputStream)
    while (true) {
      try {
        input = BufferedInputStream(CompressorStreamFactory().createCompressorInputStream(input))
      } catch (e: CompressorException) { break }
    }

    if (headerMatches(input, CLASS_SIGNATURE)) {
      // if (!match_entry(entryName.joinToString("\0"))) { continue; }
      result.put(name, input.readBytes())
      return
    }

    if (headerMatches(input, JMOD_SIGNATURE)) {
      input.readNBytes(JMOD_OFFSET)
    }
    val archive = try {
      ArchiveStreamFactory().createArchiveInputStream(input)
    } catch (e: ArchiveException) {
      // TODO: log
      return
    }

    val entries = mutableMapOf<List<File>, ByteArrayInputStream>()
    while (true) {
      val entry: ArchiveEntry? = archive.nextEntry
      if (entry === null) { break }
      if (!archive.canReadEntryData(entry)) { continue }
      if (entry.isDirectory) { continue }
      val entryName = name + File(entry.name)
      // if (!match_entry(entryName.joinToString("\0"))) { continue; }
      entries.set(entryName, ByteArrayInputStream(archive.readBytes()))
    }

    // Note that we do this only after we have read the entire archive because
    // in some formats (e.g., `.zip`) the same filename could appear in multiple
    // entries.  We want to use the last entry with each filename.
    entries.toList().map(::read)
  }
}
