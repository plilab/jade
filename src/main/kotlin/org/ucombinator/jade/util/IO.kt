package org.ucombinator.jade.util

import java.io.ByteArrayOutputStream

/** TODO:doc. */
object IO {
  fun buildByteArray(block: ByteArrayOutputStream.() -> Unit): ByteArray =
    ByteArrayOutputStream().let { byteArrayOutputStream ->
      byteArrayOutputStream.block()
      byteArrayOutputStream.toByteArray()
    }
}
