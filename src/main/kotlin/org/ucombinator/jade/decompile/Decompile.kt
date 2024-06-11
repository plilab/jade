package org.ucombinator.jade.decompile

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.BodyDeclaration
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import org.objectweb.asm.ClassReader
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.AnalyzerAdapter
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import org.ucombinator.jade.util.Log
import java.io.File
import org.ucombinator.jade.util.AtomicWriteFile

// import org.objectweb.asm.util.Textifier
// import org.objectweb.asm.util.TraceClassVisitor
// import org.ucombinator.jade.util.Log
// import org.ucombinator.jade.util.VFS
// import java.io.PrintWriter
// import java.io.StringWriter

// TODO: nested class?
// TODO: error message
// TODO: load flag
// TODO: support stdin for files to decompile
// TODO: skip over ct.jar as it is just signatures.  Maybe don't skip second load if it is better.

/**
 * Handles processing files and decomposing them into classes and methods.  Processing of class-level constructs and
 * method bodies are delegated to `DecompileClass` and `DecompileMethodBody` respectively.
 */
object Decompile {
  private val log = Log {}

  // Maps decompiled tree nodes to ASM nodes
  val classes = mutableMapOf<CompilationUnit, ClassNode>()
  val methods = mutableMapOf<BodyDeclaration<out BodyDeclaration<*>>, Pair<ClassNode, MethodNode>>()

  /**
   * The main entry point for the decompiler. It takes a list of files as input and attempts to decompile them.
   *
   * @param files The list of files to decompile.
   */
  fun main(files: List<File>, outputDir: File) {
    // Temporary code that decompiles only one .class file
    require(files.size == 1)
    val file = files.first()
    val classReader = ClassReader(file.readBytes())
    val compilationUnit = decompileClassFile(file.toString(), file.toString(), classReader, 0)
    
    log.debug("stubCompilationUnit\n${compilationUnit}")

    // Write to .java file of the same name as .class file (e.g. SampleClass.class -> SampleClass.java)
    val classFileName = file.getName()

    if (!classFileName.endsWith(".class")) {
        throw Exception("Invalid file name: file $classFileName does not end with .class")
    }

    // TODO: the /tmp must be replaced with argument passed into decompile command
    val javaFileName = "tmp/" + classFileName.replace(".class", ".java")

    val fileToWrite = File(javaFileName)

    AtomicWriteFile.write(fileToWrite, "${compilationUnit}", false) 
    
    // Log to debug log
    for (type in compilationUnit.types) {
      log.debug("type: ${type.javaClass}")
      if (type is ClassOrInterfaceDeclaration) {
        val classNode = type.getData(DecompileClass.CLASS_NODE)!!
        // TODO: for (callable in type.members.iterator().filterIsInstance<CallableDeclaration<*>>()) {
        for (callable in type.constructors + type.methods) {
          val methodNode = callable.getData(DecompileClass.METHOD_NODE)!!
          DecompileMethodBody.decompileBody(classNode, methodNode, callable)
          log.debug("method: $callable")
        }
      } else {
        TODO()
      }
    }

    log.debug("compilationUnit\n${compilationUnit}")

    // val readFiles = ReadFiles()
    // for (file in files) {
    //   readFiles.dir(file)
    // }
    // for ((k, _) in readFiles.result) {
    //   println("k $k")
    // }

    // for (((name, readers), classIndex) <- VFS.classes.zipWithIndex) {
    //   for ((path, classReader) <- readers) { // TODO: pick "best" classReader
    //     // TODO: why don't we combine the class and method passes?
    //     // Decompile class structure
    //     val compilationUnit = decompileClassFile(name, path.toString, classReader, classIndex)

    //     // Decompile method bodies
    //     for (typ <- compilationUnit.types.iterator().asScala) {
    //       val members = typ.members.iterator().asScala.flatMap(x => Decompile.methods[x].map((_, x))).toList
    //       for ((((classNode, methodNode), bodyDeclaration), methodIndex) <- members.zipWithIndex) {
    //         this.log.debug("!!!!!!!!!!!!")
    //         this.log.info(
    //           "Decompiling [${classIndex + 1} of ${VFS.classes.size}] ${classNode.name} [${methodIndex + 1} of " +
    //           "${members.size}] ${methodNode.name} (signature = ${methodNode.signature}, " +
    //           "descriptor = ${methodNode.desc})"
    //         )
    //         DecompileMethodBody.decompileBody(classNode, methodNode, bodyDeclaration)
    //       }
    //     }

    //     this.log.debug(f"compilationUnit\n${compilationUnit}")
    //   }
    // }
  }

  /**
   * Decompiles a class file and returns the corresponding CompilationUnit.
   *
   * @param name The name of the class file.
   * @param owner The owner of the class (e.g., package name).
   * @param cr The ClassReader object for the class file.
   * @param i The index of the class file within the list to be decompiled.
   * @return The decompiled CompilationUnit, or null if there's an error.
   */
  fun decompileClassFile(name: String, owner: String, cr: ClassReader, i: Int): CompilationUnit {
    val classNode = object : ClassNode(Opcodes.ASM9) {
      override fun visitMethod(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<String>?
      ): MethodVisitor =
        if (true) {
          super.visitMethod(access, name, descriptor, signature, exceptions)
        } else {
          AnalyzerAdapter(
            owner,
            access,
            name,
            descriptor,
            object : MethodVisitor(Opcodes.ASM9, super.visitMethod(access, name, descriptor, signature, exceptions)) {
              override fun visitInsn(opcode: Int) = super.visitInsn(opcode)
            }
          )
        }
    }
    cr.accept(classNode, ClassReader.EXPAND_FRAMES) // TODO: Do we actually need ClassReader.EXPAND_FRAMES?

    if (classNode.name == null) TODO()
    // this.log.debug("class name: " + classNode.name)

    // this.asmLog.whenDebugEnabled {
    //   val stringWriter = StringWriter()
    //   classNode.accept(TraceClassVisitor(null, Textifier(), PrintWriter(stringWriter)))
    //   this.asmLog.debug("++++ asm ++++\n" + stringWriter.toString())
    // }

    return DecompileClass.decompileClass(classNode)
  }
}
