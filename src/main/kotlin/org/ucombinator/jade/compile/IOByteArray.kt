package org.ucombinator.jade.compile

import java.io.InputStream
import java.io.OutputStream

class IOByteArray(bytes: ByteArray) {
  private var buf = bytes.copyOf()
  private var count = buf.size

  open class ByteArrayInputStream(val buffer: IOByteArray) : InputStream() {
    private var pos = 0
    // TODO: mark
    // TODO: other functions
    override fun read(): Int =
      synchronized(buffer) {
        if (pos >= buffer.count) -1 else buffer.buf[pos++].toInt()
      }
  }

  open class ByteArrayOutputStream(val buffer: IOByteArray) : OutputStream() {
    private var pos = 0
    override fun write(b: Int): Unit {
      synchronized(buffer) {
        // TODO: do in an overflow aware way
        if (pos >= buffer.buf.size) buffer.buf = buffer.buf.copyOf(2 * buffer.buf.size.coerceAtLeast(16))
        // println("pos: $pos size: ${buffer.buf.size}")
        buffer.buf[pos++] = b.toByte() // TODO: keep only lower 8 bits
        if (pos > buffer.count) buffer.count = pos
      }
    }
  }
}
