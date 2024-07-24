package org.ucombinator.jade.compile

import org.ucombinator.jade.util.Log

import java.io.File
import java.io.Writer
import java.util.Locale
import java.nio.charset.Charset
import javax.tools.ToolProvider
import javax.tools.DiagnosticListener
import javax.tools.JavaFileObject

// TODO:
// - https://docs.oracle.com/en/java/javase/21/docs/api/java.compiler/module-summary.html
// - https://docs.oracle.com/en/java/javase/21/docs/api/java.compiler/javax/tools/package-summary.html

// - https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/file/FileSystem.html
// - https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/jar/JarOutputStream.html

// - JavaFileManager memory
// - https://www.tabnine.com/code/java/classes/javax.tools.JavaFileManager
// - https://stackoverflow.com/questions/1168931/how-to-create-an-object-from-a-string-in-java-how-to-eval-a-string/1169771#1169771
// - https://www.tabnine.com/code/java/classes/net.java.btrace.compiler.MemoryJavaFileManager
// - MemoryJavaFileManager
// - https://github.com/michaelliao/compiler/blob/master/src/main/java/com/itranswarp/compiler/MemoryJavaFileManager.java

// TODO: Use a JavaAgent of a nested compiler to test whether the code compiles
// TODO: Test whether it compiles under different Java versions
// TODO: Back-off if compilation fails

/** TODO:doc. */
object Compile {
  private val log = Log {}

  /** TODO:doc.
   *
   * @param files TODO:doc
   */
  fun main(
    out: Writer?,
    locale: Locale?,
    charset: Charset?,
    options: Iterable<String>,
    classes: Iterable<String>,
    files: Iterable<File>,
  ) {
    val javaCompiler = ToolProvider.getSystemJavaCompiler()

    val diagnosticListener: DiagnosticListener<JavaFileObject>? = null // TODO
    val standardFileManager = javaCompiler.getStandardFileManager(diagnosticListener, locale, charset)
    val fileManager = standardFileManager // TODO: in-memory files
    // files: Map<File, ByteArray>
    // output: Map<File, ByteArray>
    // must support StandardLocation
    val compilationUnits = fileManager.getJavaFileObjectsFromFiles(files)

    val task = javaCompiler.getTask(out, fileManager, diagnosticListener, options, classes, compilationUnits)
    // TODO: task.addModules
    // TODO: task.setProcessors
    // TODO: task.setLocale
    task.call() // TODO: exit code from Boolean return value
  }
}
