package org.ucombinator.jade.util

import java.io.File

// TODO: Move to IO.kt

/** TODO:doc. */
object AtomicWriteFile {
  /** TODO:doc.
   *
   * @param file TODO:doc
   * @param text TODO:doc
   * @param mkdirs TODO:doc
   */
  @Deprecated("Use write(File, ByteArray, Boolean = ...): Unit", ReplaceWith("write(file, text.toByteArray(), mkdirs)"))
  fun write(file: File, text: String, mkdirs: Boolean = false) { write(file, text.toByteArray(), mkdirs) }

  /** TODO:doc.
   *
   * @param file TODO:doc
   * @param bytes TODO:doc
   * @param mkdirs TODO:doc
   */
  fun write(file: File, bytes: ByteArray, mkdirs: Boolean = false) {
    val dir = file.parentFile ?: File(".")
    if (mkdirs && !dir.exists()) dir.mkdirs() || Errors.fatal("could not create dir for ${file}")
    val tmpFile = File.createTempFile(file.name + ".part-", "", dir)
    try {
      tmpFile.writeBytes(bytes)
      tmpFile.renameTo(file) || Errors.fatal("could not rename temporary file ${tmpFile} to ${file}")
    } finally {
      if (tmpFile.exists()) tmpFile.delete()
    }
  }
}
