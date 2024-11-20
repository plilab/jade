package org.ucombinator.jade.compile

import org.ucombinator.jade.util.Log
import org.ucombinator.jade.compile.MemoryDirectory

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.Reader
import java.io.Writer
import java.net.URI
import java.nio.charset.Charset
import java.nio.charset.CharsetDecoder
import java.nio.charset.CodingErrorAction
import java.nio.file.Path
import java.util.ServiceLoader
import javax.lang.model.element.Modifier
import javax.lang.model.element.NestingKind
import javax.tools.FileObject
import javax.tools.JavaFileManager
import javax.tools.JavaFileObject
import javax.tools.StandardJavaFileManager

// // TODO: use forwarding?
class MemoryJavaFileManager(val fileManager: StandardJavaFileManager) : StandardJavaFileManager {
  val log = Log {}

  val files = mutableMapOf<JavaFileManager.Location, MemoryEntry>()

  operator fun get(location: JavaFileManager.Location, path: List<String>): MemoryFileTree? =
    files.getOrPut(location) { MemoryEntry() }[path]
  operator fun set(location: JavaFileManager.Location, path: List<String>, value: MemoryFileTree?) {
    files.getOrPut(location) { MemoryEntry() }[path] = value
  }
  // must support StandardLocation

  private val defaultEncodingName = OutputStreamWriter(ByteArrayOutputStream()).getEncoding();
  private val encodingName = defaultEncodingName // TODO: options.get(Option.ENCODING);
  private val charset by lazy { Charset.forName(encodingName) }
  fun getDecoder(): CharsetDecoder = charset.newDecoder()

  ////////////////////////////////

  override fun isSupportedOption(option: String): Int = fileManager.isSupportedOption(option).also {
    log.debug("isSupportedOption(${option}) = ${it}")
  }

  ////////////////////////////////

  override fun getClassLoader(location: JavaFileManager.Location): ClassLoader = fileManager.getClassLoader(location).also {
    log.debug("getClassLoader(${location}) = ${it}")
  }
  override fun list(location: JavaFileManager.Location, packageName: String, kinds: Set<JavaFileObject.Kind>, recurse: Boolean): Iterable<JavaFileObject> = fileManager.list(location, packageName, kinds, recurse).also {
    log.debug("list($location, $packageName, $kinds, $recurse) = ${it}")
  }
  override fun inferBinaryName(location: JavaFileManager.Location, file: JavaFileObject): String = fileManager.inferBinaryName(location, file).also {
    // log.debug("inferBinaryName($location, $file) = ${it}")
  }
  override fun isSameFile(a: FileObject, b: FileObject): Boolean = fileManager.isSameFile(a, b).also {
    log.debug("isSameFile($a, $b) = ${it}")
  }
  override fun handleOption(current: String, remaining: Iterator<String>): Boolean = fileManager.handleOption(current, remaining).also {
    log.debug("handleOption(${current}, ${remaining}) = ${it}")
  }
  override fun hasLocation(location: JavaFileManager.Location): Boolean = fileManager.hasLocation(location).also {
    log.debug("hasLocation(${location}) = ${it}")
  }
  override fun getJavaFileForInput(location: JavaFileManager.Location, className: String, kind: JavaFileObject.Kind): JavaFileObject = fileManager.getJavaFileForInput(location, className, kind).also {
    log.debug("getJavaFileForInput($location, $className, $kind) = ${it}")
  }
  override fun getJavaFileForOutput(location: JavaFileManager.Location, className: String, kind: JavaFileObject.Kind, sibling: FileObject): JavaFileObject {
    log.debug("getJavaFileForOutput($location, $className, $kind, $sibling)")
    log.error("uri: ${sibling.toUri().path}")
    log.error("name: ${sibling.getName()}")
    val path = pathOf(className, kind)
    // TODO: include kind after path
    //return this[location, path] ?:
    TODO()
    // return MemoryJavaFileObject(this, location, path, ByteArray(0), kind).also { this[location, path] = it }
  }

  private fun pathOf(className: String, kind: JavaFileObject.Kind): List<String> =
    className.split('.').let { it.subList(0, it.size - 1) + listOf(it.last() + kind.extension) }

  // override fun getJavaFileForOutput(location: JavaFileManager.Location, className: String, kind: JavaFileObject.Kind, sibling: FileObject): JavaFileObject = fileManager.getJavaFileForOutput(location, className, kind, sibling).also {
  //     log.debug("getJavaFileForOutput($location, $className, $kind, $sibling) = ${it}")
  // }
  override fun getFileForInput(location: JavaFileManager.Location, packageName: String, relativeName: String): FileObject = fileManager.getFileForInput(location, packageName, relativeName).also {
    log.debug("getFileForInput($location, $packageName, $relativeName) = ${it}")
    // this[location, name]!!
    // get memory (not exist vs could not exist) then get from super
  }
  override fun getFileForOutput(location: JavaFileManager.Location, packageName: String, relativeName: String, sibling: FileObject): FileObject = fileManager.getFileForOutput(location, packageName, relativeName, sibling).also {
    // get then put then get
    log.debug("getFileForOutput($location, $packageName, $relativeName, $sibling) = ${it}")
    // this[location, name] ?: MemoryFileObject(this, TODO(), ByteArray(0)).also { this[location, name] = it }
  }
  override fun flush(): Unit = fileManager.flush().also {
    log.debug("flush() = ${it}")
  }
  override fun close(): Unit = fileManager.close().also {
    log.debug("close() = ${it}")
  }
  override fun getLocationForModule(location: JavaFileManager.Location, moduleName: String): /* default */ JavaFileManager.Location = fileManager.getLocationForModule(location, moduleName).also {
    log.debug("getLocationForModule($location, $moduleName) = ${it}")
  }
  override fun getLocationForModule(location: JavaFileManager.Location, fo: JavaFileObject): /* default */ JavaFileManager.Location = fileManager.getLocationForModule(location, fo).also {
    log.debug("getLocationForModule($location, $fo) = ${it}")
  }
  override fun <S> getServiceLoader(location: JavaFileManager.Location, service: Class<S>): /* default */ ServiceLoader<S> = fileManager.getServiceLoader(location, service).also {
    log.debug("getServiceLoader($location, $service) = ${it}")
  }
  override fun inferModuleName(location: JavaFileManager.Location): /* default */ String = fileManager.inferModuleName(location).also {
    log.debug("inferModuleName($location) = ${it}")
  }
  override fun listLocationsForModules(location: JavaFileManager.Location): /* default */ Iterable<Set<JavaFileManager.Location>> = fileManager.listLocationsForModules(location).also {
    log.debug("listLocationsForModules($location) = ${it}")
  }
  override fun contains(location: JavaFileManager.Location, fo: FileObject): /* default */ Boolean = fileManager.contains(location, fo).also {
    log.debug("contains(${location}, ${fo}) = ${it}")
  }

  ////////////////////////////////

  override fun getJavaFileObjectsFromFiles(files: Iterable<File>): Iterable<JavaFileObject> = fileManager.getJavaFileObjectsFromFiles(files).also {
    log.debug("getJavaFileObjectsFromFiles($files) = ${it}")
  }
  override fun getJavaFileObjectsFromPaths(paths: Iterable<Path>): /* default */ Iterable<JavaFileObject> = fileManager.getJavaFileObjectsFromPaths(paths).also {
    log.debug("getJavaFileObjectsFromPaths($paths) = ${it}")
  }
  override fun getJavaFileObjects(vararg files: File): Iterable<JavaFileObject> = fileManager.getJavaFileObjects(*files).also {
    log.debug("getJavaFileObjects($files) = ${it}")
  }
  override fun getJavaFileObjects(vararg paths: Path): /* default */ Iterable<JavaFileObject> = fileManager.getJavaFileObjects(*paths).also {
    log.debug("getJavaFileObjects($paths) = ${it}")
  }
  override fun getJavaFileObjectsFromStrings(names: Iterable<String>): Iterable<JavaFileObject> = fileManager.getJavaFileObjectsFromStrings(names).also {
    log.debug("getJavaFileObjectsFromStrings($names) = ${it}")
  }
  override fun getJavaFileObjects(vararg names: String): Iterable<JavaFileObject> = fileManager.getJavaFileObjects(*names).also {
    log.debug("getJavaFileObjects($names) = ${it}")
  }
  override fun setLocation(location: JavaFileManager.Location, files: Iterable<File>): Unit = fileManager.setLocation(location, files).also {
    log.debug("setLocation($location, $files) = ${it}")
  }
  override fun setLocationFromPaths(location: JavaFileManager.Location, paths: Collection<Path>): /* default */ Unit = fileManager.setLocationFromPaths(location, paths).also {
    log.debug("setLocationFromPaths($location, $paths) = ${it}")
  }
  override fun setLocationForModule(location: JavaFileManager.Location, moduleName: String, paths: Collection<Path>): /* default */ Unit = fileManager.setLocationForModule(location, moduleName, paths).also {
    log.debug("setLocationForModule($location, $moduleName, $paths) = ${it}")
  }
  override fun getLocation(location: JavaFileManager.Location): Iterable<File>? = fileManager.getLocation(location).also {
    log.debug("getLocation($location) = ${it}")
  }
  override fun getLocationAsPaths(location: JavaFileManager.Location): /* default */ Iterable<Path> = fileManager.getLocationAsPaths(location).also {
    log.debug("getLocationAsPaths($location) = ${it}")
  }
  override fun asPath(file: FileObject): /* default */ Path = fileManager.asPath(file).also {
    log.debug("asPath($file) = ${it}")
  }
  override fun setPathFactory(f: StandardJavaFileManager.PathFactory): /* default */ Unit = fileManager.setPathFactory(f).also {
    log.debug("setPathFactory($f) = ${it}")
  }
}

// TODO: move util/List.kt
fun <T> List<T>.rest(): List<T> = this.subList(1, this.size)

class MemoryEntry() {
  var value: MemoryFileTree? = null

  private operator fun get(key: String): MemoryEntry {
    this.value = this.value ?: MemoryDirectory()
    return when (val v = this.value) {
      null, is MemoryFileObject /*, is MemoryJavaFileObject */ -> TODO()
      is MemoryDirectory -> v.children.getOrPut(key) { MemoryEntry() }
    }
  }

  // TODO: Do archives behave as both files and dirs?
  operator fun get(key: List<String>): MemoryFileTree? =
    if (key.isEmpty()) this.value else this[key.first()][key.rest()]

  operator fun set(key: List<String>, value: MemoryFileTree?) {
    if (key.isEmpty()) this.value = value else (this[key.first()])[key.rest()] = value
  }
}

sealed interface MemoryFileTree

class MemoryDirectory : MemoryFileTree {
  val children = mutableMapOf<String, MemoryEntry>()
}

open class MemoryFileObject(
  private val name: String, bytes: ByteArray, val deleteAction: () -> Boolean,
) : MemoryFileTree, FileObject {
  private val buffer = IOByteArray(bytes)
  private var lastModified = 0L // TODO
  // private val name = "${location.name}/${path.join('/')}"

  override fun toUri(): URI = URI("location:${name}")
  override fun getName(): String = name
  override fun openInputStream(): InputStream = IOByteArray.ByteArrayInputStream(buffer)
  override fun openOutputStream(): OutputStream = MemoryFileOutputStream()
  override fun openReader(ignoreEncodingErrors: Boolean): Reader = TODO()
    // InputStreamReader(
    //   openInputStream(),
    //   (if (ignoreEncodingErrors) CodingErrorAction.REPLACE else CodingErrorAction.REPORT).let { action ->
    //     fileManager.getDecoder().onMalformedInput(action).onUnmappableCharacter(action)
    //   },
    // )
  override fun getCharContent(ignoreEncodingErrors: Boolean): CharSequence = openReader(ignoreEncodingErrors).readText()
  override fun openWriter(): Writer = OutputStreamWriter(openOutputStream())
  override fun getLastModified(): Long = lastModified
  override fun delete(): Boolean = deleteAction()

  inner class MemoryFileOutputStream() : IOByteArray.ByteArrayOutputStream(buffer) {
    // TODO: other functions
    override fun write(b: Int): Unit {
      synchronized(this@MemoryFileObject) {
        // TODO: lastModified = ...
        super.write(b)
      }
    }
  }
}

// class MemoryJavaFileObject(
//   private val fileManager: MemoryJavaFileManager, private val path: List<String>, bytes: ByteArray, val theKind: JavaFileObject.Kind
// ) : MemoryFileObject(fileManager, path, bytes), MemoryFileTree, JavaFileObject {
//   // val path.dropLast, last + theKind.extension
//   private val pathString by lazy { toUri().getPath() }
//   override fun getKind(): JavaFileObject.Kind = theKind // TODO: rename theKind
//   override fun isNameCompatible(simpleName: String, kind: JavaFileObject.Kind): Boolean =
//     kind == this.kind && (simpleName + kind.extension).let { baseName ->
//       pathString == baseName || pathString.endsWith("/" + baseName)
//     }
//   override fun getNestingKind(): NestingKind? = null // TODO
//   override fun getAccessLevel(): Modifier? = null // TODO
// }
