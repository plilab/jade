package org.ucombinator.jade.util

import java.io.File

/** TODO:doc. */
object AtomicWriteFile {
  /** TODO:doc.
   *
   * @param file TODO:doc
   * @param string TODO:doc
   * @param mkdirs TODO:doc
   */
  fun write(file: File, string: String, mkdirs: Boolean = false) {
    if (mkdirs) file.parentFile.mkdirs()
    val tmpFile = File.createTempFile(file.name + ".part-", "", file.parentFile)
    try {
      tmpFile.writeText(string)
      tmpFile.renameTo(file)
    } finally {
      if (tmpFile.exists()) tmpFile.delete()
    }
  }
}
