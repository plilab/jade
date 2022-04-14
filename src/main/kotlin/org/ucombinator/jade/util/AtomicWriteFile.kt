package org.ucombinator.jade.util

import java.io.File

object AtomicWriteFile {
  fun write(file: File, string: String, mkdirs: Boolean = false) {
    if (mkdirs) { file.parentFile.mkdirs() }
    val tmpFile = File.createTempFile(file.name + ".part-", "", file.parentFile)
    try {
      tmpFile.writeText(string)
      tmpFile.renameTo(file)
    } finally {
      if (tmpFile.exists()) tmpFile.delete()
    }
  }
}
