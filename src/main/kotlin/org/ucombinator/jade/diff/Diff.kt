package org.ucombinator.jade.diff

import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.Attribute
import org.objectweb.asm.util.TraceClassVisitor
import org.objectweb.asm.util.TraceMethodVisitor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.util.Textifier
import org.objectweb.asm.Opcodes
import org.ucombinator.jade.util.Log

import java.io.File
import java.io.StringWriter
import java.io.PrintWriter

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** TODO:doc. */
object Diff {
  private val log = Log {}
  /** TODO:doc.
   *
   * @param old TODO:doc
   * @param new TODO:doc
   */
  fun main(old: File, new: File) {
    // byte for byte comparison
    // call javap
    // TODO: clickt (argument parsing only); main (glue); lib
    // TODO: diff directories and sets of files
    diff(old.readBytes(), new.readBytes())
  }

  // TODO: global constant for Opcodes.ASM9
  // ClassReader(InputStream), ClassReader(className: String)
  fun diff(old: ByteArray, new: ByteArray): List<Change> =
    if (old.contentEquals(new)) {
      println("Bytecode is the same!")
      emptyList()
    } else {
      diff(ClassReader(old), ClassReader(new))
    }

  // TODO: move to clasfile package
  fun classNodeOfClassReader(classReader: ClassReader): ClassNode =
    // TODO: Do we actually need ClassReader.EXPAND_FRAMES?
    ClassNode(Opcodes.ASM9).also { classReader.accept(it, ClassReader.EXPAND_FRAMES) }

  // TODO: move to clasfile package
  fun textOfClassNode(classNode: ClassNode, methodBodies: Boolean): String =
    StringWriter().also {
      // TODO: TextifierDelegate
      val textifier = if (methodBodies) Textifier() else NoMethodBodiesTextifier()
      classNode.accept(TraceClassVisitor(null, textifier, PrintWriter(it)))
    }.toString()

  // TODO: methodBodies: boolean
  fun diff(old: ClassReader, new: ClassReader): List<Change> =
    diff(classNodeOfClassReader(old), classNodeOfClassReader(new))

  fun diff(old: ClassNode, new: ClassNode): List<Change> {
    // refer to https://asm.ow2.io/javadoc/org/objectweb/asm/tree/ClassNode.html

    // println("------------------")
    // println(textOfClassNode(old, false))
    // println("------------------")
    // println(textOfClassNode(old, true))
    // println("------------------")
    // if (textOfClassNode(old) == textOfClassNode(new)) emptyList()
    // else TODO()
    // TODO: string diff
    // TODO: structure only vs code
    // TODO: deeper diff

    fun <T, R> printDiff(old: T, new: T, logName: String, by: (T) -> R, format: (R) -> String): Unit {
      val oldR = by(old)
      val newR = by(new)
      if (oldR == newR) {
        log.info {"Field '${logName}' matches."}
      } else {
        log.info{"Field '${logName}' does not match.\n---\nOLD: ${format(oldR)}\n\nNEW: ${format(newR)}\n---"}
      }
    }

    // still need another comparison function beyond just .equals, for the more complicated objects
    // need to write something to convert the actual flags to 
    // assume that methods and functions are in the same order
    // watch out for method overloading, need to think more about this. return types don't count!!
    // method sigs are stored in (1) descriptor and (2) someplace which includes type params for generic types, added in java 8
    // add another method to just check the methods to see if they're the same
    // check everything minus method bodies first
    // check method bodies only when decompile works properly
    // develop the diff system where it'll be easy to run lots of diff tests eventually
    // want to be able to run diff on two folders, two jar files, even until maven ips
    // basically make the system extensible
    // add a flag for method bodies. basically this means you bypass method bodies and then you can test the
    // diff on everything else.
    // think more about how to make the system extensible and be able to test everything

    val now = LocalDateTime.now()
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    val formatted = now.format(formatter)

    log.info {"Starting Diff log. Current date and time: ${formatted}"}

    // Basic class information
    printDiff(old, new, "version", ClassNode::version) {"version: ${it.toString()}"} // int TODO: hex? 
    printDiff(old, new, "access", ClassNode::access) {"access: ${it.toString()}"} // int TODO: format as actual flags
    printDiff(old, new, "name", ClassNode::name) {"name: $it"} // String
    printDiff(old, new, "signature", ClassNode::signature) {"signature: $it"} // String
    printDiff(old, new, "superName", ClassNode::superName) {"superName: $it"} // String
    printDiff(old, new, "interfaces", ClassNode::interfaces) {"interFaces: $it"} // List<String>

    // Source and debug information
    printDiff(old, new, "sourceFile", ClassNode::sourceFile) {"sourceFile: $it"} // String
    printDiff(old, new, "sourceDebug", ClassNode::sourceDebug) {"sourceDebug: $it"} // String

    // Enclosing context
    printDiff(old, new, "module", ClassNode::module) {"module: $it"} // 
    printDiff(old, new, "outerClass", ClassNode::outerClass) {"outerClass: $it"}
    printDiff(old, new, "outerMethod", ClassNode::outerMethod) {"outerMethod: $it"}
    printDiff(old, new, "outerMethodDesc", ClassNode::outerMethodDesc) {"outerMethodDesc: $it"}

    // Annotations
    printDiff(old, new, "visibleAnnotations", ClassNode::visibleAnnotations) {"visibleAnnotations: $it"}
    printDiff(old, new, "invisibleAnnotations", ClassNode::invisibleAnnotations) {"invisibleAnnotations: $it"}
    printDiff(old, new, "visibleTypeAnnotations", ClassNode::visibleTypeAnnotations) {"visibleTypeAnnotations: $it"}
    printDiff(old, new, "invisibleTypeAnnotations", ClassNode::invisibleTypeAnnotations) {"invisibleTypeAnnotations: $it"}

    // Attrs
    printDiff(old, new, "attrs", ClassNode::attrs) {"attrs: $it"}

    // Nest related stuff
    printDiff(old, new, "innterClasses", ClassNode::innerClasses) {"innerClasses: $it"}
    printDiff(old, new, "nestHostClass", ClassNode::nestHostClass) {"name: $it"}
    printDiff(old, new, "nestMembers", ClassNode::nestMembers) {"nestMembers: $it"}
    printDiff(old, new, "permittedSubclasses", ClassNode::permittedSubclasses) {"permittedSubclasses: $it"}
    printDiff(old, new, "recordComponents", ClassNode::recordComponents) {"recordComponents: $it"}
    printDiff(old, new, "fields", ClassNode::fields) {"fields: $it"}
    printDiff(old, new, "methods", ClassNode::methods) {"methods: $it"}

    log.info {"Finished comparing files."}

    // List<String> interfaces
    // ModuleNode module
    // List<AnnotationNode> visibleAnnotations
    // List<AnnotationNode> invisibleAnnotations
    // List<TypeAnnotationNode> visibleTypeAnnotations
    // List<TypeAnnotationNode> invisibleTypeAnnotations
    // List<Attribute> attrs
    // List<InnerClassNode> innerClasses
    // String nestHostClass
    // List<String> nestMembers
    // List<String> permittedSubclasses
    // List<RecordComponentNode> recordComponents
    // List<FieldNode> fields // sort
    // List<MethodNode> methods // sort
    // TODO()
    return emptyList<Change>() // TODO: diff


/*
  Attribute (subclasses ModuleHashesAttribute, ModuleResolutionAttribute, ModuleTargetAttribute):
    String type
    byte[] content
    Attribute nextAttribute
  ModuleHashesAttribute:
    String algorithm
    List<String> modules
    List<byte[]> hashes
  ModuleResolutionAttribute:
    ...
  ModuleTargetAttribute:
    String platform

  AnnotationNode (subclass TypeAnnotationNode):
    String desc
    List<Object> values
  TypeAnnotationNode (subclass LocalVariableAnnotationNode):
    int typeRef (see TypeReference)
    TypePath typePath
  LocalVariableAnnotationNode:
    List<LabelNode> start
    List<LabelNode> end
    List<Integer> index

  TypePath:
    int getLength()
    int getStep(int index)
    int getStepArgument(int index)
    String toString()
  LabelNode:
    ...


  ModuleNode:
    String name
    int access
    String version
    String mainClass
    List<String> packages
    List<ModuleRequireNode> requires
    List<ModuleExportNode> exports
    List<ModuleOpenNode> opens
    List<String> uses
    List<ModuleProvideNode> provides
  ModuleRequireNode:
    String module
    int access
    String version
  ModuleExportNode:
    String packaze
    int access
    List<String> modules
  ModuleOpenNode:
    String packaze
    int access
    List<String> modules
  ModuleProvideNode:
    String service
    List<String> providers

  InnerClassNode:
    String name
    String outerName
    String innerName
    int access

  RecordComponentNode:
    String name
    String descriptor
    String signature
    List<AnnotationNode> visibleAnnotations
    List<AnnotationNode> invisibleAnnotations
    List<TypeAnnotationNode> visibleTypeAnnotations
    List<TypeAnnotationNode> invisibleTypeAnnotations
    List<Attribute> attrs

  FieldNode:
    int access
    String name
    String desc
    String signature
    Object value
    List<AnnotationNode> visibleAnnotations
    List<AnnotationNode> invisibleAnnotations
    List<TypeAnnotationNode> visibleTypeAnnotations
    List<TypeAnnotationNode> invisibleTypeAnnotations
    List<Attribute> attrs

  MethodNode:
    int access
    String name
    String desc
    String signature
    List<String> exceptions
    List<ParameterNode> parameters
    List<AnnotationNode> visibleAnnotations
    List<AnnotationNode> invisibleAnnotations
    List<TypeAnnotationNode> visibleTypeAnnotations
    List<TypeAnnotationNode> invisibleTypeAnnotations
    List<Attribute> attrs
    Object annotationDefault
    int visibleAnnotableParameterCount
    List<AnnotationNode>[] visibleParameterAnnotations
    int invisibleAnnotableParameterCount
    List<AnnotationNode>[] invisibleParameterAnnotations
    InsnList instructions
    List<TryCatchBlockNode> tryCatchBlocks
    int maxStack
    int maxLocals
    List<LocalVariableNode> localVariables
    List<LocalVariableAnnotationNode> visibleLocalVariableAnnotations
    List<LocalVariableAnnotationNode> invisibleLocalVariableAnnotations
    // private boolean visited;

  ParameterNode:
    String name
    int access

  TryCatchBlockNode:
    LabelNode start
    LabelNode end
    LabelNode handler
    String type
    List<TypeAnnotationNode> visibleTypeAnnotations
    List<TypeAnnotationNode> invisibleTypeAnnotations

  LocalVariableNode:
    String name
    String desc
    String signature
    LabelNode start
    LabelNode end
    int index

  InsnList:
    ListIterator<AbstractInsnNode> iterator()

  AbstractInsnNode (many subclasses):
    List<TypeAnnotationNode> visibleTypeAnnotations
    List<TypeAnnotationNode> invisibleTypeAnnotations

*/

  // diffLists(key)
  //   map<Object, List<Index>>
  //   add old
  //   remove new




  }
}

// TODO: should we pass api as argument
class NoMethodBodiesTextifier() : Textifier(Opcodes.ASM9) {
  val log = Log {}
  private var codeStart = -1;
  override fun visitCode() {
    log.info { "visitCode" }
    super.visitCode()
    codeStart = text.size
  }
  override fun visitMethodEnd() {
    super.visitMethodEnd()
    super.text.subList(codeStart, text.size).clear()
    codeStart = -1
  }
  override fun createTextifier(): Textifier = NoMethodBodiesTextifier()
}
class Change
