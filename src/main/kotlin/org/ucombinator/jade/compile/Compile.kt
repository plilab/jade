package org.ucombinator.jade.compile

import org.ucombinator.jade.util.Log
import java.io.File
import javax.tools.ToolProvider

// TODO:
// - https://docs.oracle.com/en/java/javase/21/docs/api/java.compiler/javax/tools/JavaCompiler.html
// - https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/file/FileSystem.html
// - https://docs.oracle.com/en/java/javase/21/docs/api/java.compiler/javax/tools/JavaFileManager.html
// - https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/jar/JarOutputStream.html
// - https://docs.oracle.com/en/java/javase/21/docs/api/java.compiler/javax/tools/StandardLocation.html
// - https://docs.oracle.com/en/java/javase/21/docs/api/java.compiler/javax/tools/JavaCompiler.CompilationTask.html
// - JavaFileManager memory
// - https://www.tabnine.com/code/java/classes/javax.tools.JavaFileManager
// - https://stackoverflow.com/questions/1168931/how-to-create-an-object-from-a-string-in-java-how-to-eval-a-string/1169771#1169771
// - https://www.tabnine.com/code/java/classes/net.java.btrace.compiler.MemoryJavaFileManager
// - MemoryJavaFileManager
// - https://github.com/michaelliao/compiler/blob/master/src/main/java/com/itranswarp/compiler/MemoryJavaFileManager.java

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
