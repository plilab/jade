package org.ucombinator.jade.compile

import org.ucombinator.jade.util.Log
import java.io.File
import javax.tools.ToolProvider

/** TODO:doc. */
object Compile {
  private val log = Log {}

  /** TODO:doc.
   *
   * @param files TODO:doc
   */
  fun main(files: List<File>) {
    log.debug { "${ToolProvider.getSystemJavaCompiler()}" }
    TODO()
  }
}
