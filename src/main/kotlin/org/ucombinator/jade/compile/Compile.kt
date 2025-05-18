package org.ucombinator.jade.compile

import org.ucombinator.jade.util.Log

import java.io.File
import java.io.Writer
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.Reader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.Locale
import java.util.ServiceLoader
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.nio.charset.CharsetDecoder
import java.nio.file.Path
import java.net.URI
import javax.tools.ToolProvider
import javax.tools.DiagnosticListener
import javax.tools.JavaFileObject
import javax.tools.DiagnosticCollector
import javax.tools.StandardJavaFileManager
import javax.tools.ForwardingJavaFileManager
import javax.tools.StandardLocation
import javax.tools.JavaFileManager
import javax.tools.FileObject
import javax.lang.model.element.Modifier
import javax.lang.model.element.NestingKind

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
    // javaCompiler.isSupportedOption(string)
    // javaCompiler.getSourceVersions()
    // javaCompiler.name()
    // javaCompiler.run(InputStream in, OutputStream out, OutputStream err, String... arguments)

    val diagnosticListener = DiagnosticCollector<JavaFileObject>()
    val standardFileManager = javaCompiler.getStandardFileManager(diagnosticListener, locale, charset)
    val fileManager = MemoryJavaFileManager(standardFileManager)
    val compilationUnits = fileManager.getJavaFileObjectsFromFiles(files)
    // fileManager.getJavaFileForInput(StandardLocation.SOURCE_PATH, className, JavaFileObject.Kind.SOURCE)
    for (i in listOf(
      StandardLocation.ANNOTATION_PROCESSOR_MODULE_PATH, // Location to search for modules containing annotation processors.
      StandardLocation.ANNOTATION_PROCESSOR_PATH, // Location to search for annotation processors.
      StandardLocation.CLASS_OUTPUT, // Location of new class files.
      StandardLocation.CLASS_PATH, // Location to search for user class files.
      StandardLocation.MODULE_PATH, // Location to search for precompiled user modules.
      StandardLocation.MODULE_SOURCE_PATH, // Location to search for the source code of modules.
      StandardLocation.NATIVE_HEADER_OUTPUT, // Location of new native header files.
      StandardLocation.PATCH_MODULE_PATH, // Location to search for module patches.
      StandardLocation.PLATFORM_CLASS_PATH, // Location to search for platform classes.
      StandardLocation.SOURCE_OUTPUT, // Location of new source files.
      StandardLocation.SOURCE_PATH, // Location to search for existing source files.
      StandardLocation.SYSTEM_MODULES, // Location to search for system modules.
      StandardLocation.UPGRADE_MODULE_PATH, // Location to search for upgradeable system modules.
      // FileObject
      // JavaFileObject
      // StandardJavaFileManager
      // SimpleJavaFileObject
      // ForwardingJavaFileManager
      // ForwardingFileObject
      // ForwardingJavaFileObject
    )) {
      if (fileManager.hasLocation(i)) println(fileManager.getLocation(i)!!.toList())
    }

    // fileManager.getLocation(StandardLocation.SOURCE_PATH)
    // fileManager.getJavaFileForInput(StandardLocation.SOURCE_PATH, "foo.bar", JavaFileObject.Kind.SOURCE)
    // val compilationUnits = fileManager.getJavaFileForInput(StandardLocation.SOURCE_PATH, 
    // fileManager.getJavaFileForInput(StandardLocation.SOURCE_PATH, className, JavaFileObject.Kind.SOURCE)
    // ANNOTATION_PROCESSOR_MODULE_PATH    Location to search for modules containing annotation processors.
    // ANNOTATION_PROCESSOR_PATH    Location to search for annotation processors.
    // CLASS_OUTPUT    Location of new class files.
    // CLASS_PATH    Location to search for user class files.
    // MODULE_PATH    Location to search for precompiled user modules.
    // MODULE_SOURCE_PATH    Location to search for the source code of modules.
    // NATIVE_HEADER_OUTPUT    Location of new native header files.
    // PATCH_MODULE_PATH    Location to search for module patches.
    // PLATFORM_CLASS_PATH    Location to search for platform classes.
    // SOURCE_OUTPUT    Location of new source files.
    // SOURCE_PATH    Location to search for existing source files.
    // SYSTEM_MODULES    Location to search for system modules.
    // UPGRADE_MODULE_PATH    Location to search for upgradeable system modules.

    // FileObject
    // JavaFileObject

    // StandardJavaFileManager
    // SimpleJavaFileObject

    // ForwardingJavaFileManager
    // ForwardingFileObject
    // ForwardingJavaFileObject

    val task = javaCompiler.getTask(out, fileManager, diagnosticListener, options, classes, compilationUnits)
    // TODO: task.addModules
    // TODO: task.setProcessors
    // TODO: task.setLocale
    task.call() // TODO: exit code from Boolean return value
    // diagnosticListener.diagnostics
  }
}
