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

/** TODO:doc. */
object Diff {
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
    if (old == new) emptyList() else diff(ClassReader(old), ClassReader(new))

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
    println("------------------")
    println(textOfClassNode(old, false))
    println("------------------")
    println(textOfClassNode(old, true))
    println("------------------")
    // if (textOfClassNode(old) == textOfClassNode(new)) emptyList()
    // else TODO()
    // TODO: string diff
    // TODO: structure only vs code
    // TODO: deeper diff

    fun <T, R> printDiff(old: T, new: T, by: (T) -> R, format: (R) -> String): Unit = TODO()
    println(old.name)
    printDiff(old, new, ClassNode::version) { it.toString() } // TODO: hex?
    printDiff(old, new, ClassNode::name) { it }
    printDiff(old, new, ClassNode::signature) { it }
    printDiff(old, new, ClassNode::superName) { it }
    // List<String> interfaces
    printDiff(old, new, ClassNode::sourceFile) { it }
    printDiff(old, new, ClassNode::sourceDebug) { it }
    // ModuleNode module
    printDiff(old, new, ClassNode::outerClass) { it }
    printDiff(old, new, ClassNode::outerMethod) { it }
    printDiff(old, new, ClassNode::outerMethodDesc) { it }
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
    TODO()


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
