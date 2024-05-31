package org.ucombinator.jade.compile

import org.ucombinator.jade.util.Log
import java.io.File
import javax.tools.ToolProvider

object Compile {
  private val log = Log {}

  fun main(files: List<File>) {
    log.debug { "${ToolProvider.getSystemJavaCompiler()}" }
    TODO()
  }
}
