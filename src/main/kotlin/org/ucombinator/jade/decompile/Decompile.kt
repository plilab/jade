package org.ucombinator.jade.decompile

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.BodyDeclaration
import org.objectweb.asm.ClassReader
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.AnalyzerAdapter
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import org.ucombinator.jade.util.ReadFiles
import java.io.File

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
object Decompile {
  // private val asmLog = childLog("asm")

  val classes = mutableMapOf<CompilationUnit, ClassNode>()
  val methods = mutableMapOf<BodyDeclaration<out BodyDeclaration<*>>, Pair<ClassNode, MethodNode>>()

  fun main(files: List<File>) {
    val readFiles = ReadFiles()
    for (file in files) {
      readFiles.dir(file)
    }
    for ((k, _) in readFiles.result) {
      println("k $k")
    }
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
    TODO()
  }

  fun decompileClassFile(name: String, owner: String, cr: ClassReader, i: Int): CompilationUnit? {
    // TODO: name use "." instead of "/" and "$"
    // this.log.info(f"Decompiling [${i + 1} of ${VFS.classes.size}] ${name} from ${owner}")
    // val log = this.log
    val classNode = object : ClassNode(Opcodes.ASM9) {
      override fun visitMethod(
        access: Int,
        name: String,
        descriptor: String,
        signature: String,
        exceptions: Array<String>
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
              override fun visitInsn(opcode: Int) {
                // log.info("AA LOCALS: " + aa.locals)
                // log.info("AA STACK: " + aa.stack)
                return super.visitInsn(opcode)
              }
            }
          )
        }
    }
    cr.accept(classNode, ClassReader.EXPAND_FRAMES) // TODO: Do we actually need ClassReader.EXPAND_FRAMES?

    if (classNode.name === null) return null // TODO
    // this.log.debug("class name: " + classNode.name)

    // this.asmLog.whenDebugEnabled {
    //   val stringWriter = StringWriter()
    //   classNode.accept(TraceClassVisitor(null, Textifier(), PrintWriter(stringWriter)))
    //   this.asmLog.debug("++++ asm ++++\n" + stringWriter.toString())
    // }

    return DecompileClass.decompileClass(classNode)
  }
}
