package org.ucombinator.jade.diff

import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.Attribute
import org.objectweb.asm.TypePath
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.TypeAnnotationNode
import org.objectweb.asm.tree.InnerClassNode
import org.objectweb.asm.tree.RecordComponentNode
import org.objectweb.asm.tree.ModuleNode
import org.objectweb.asm.tree.ModuleExportNode
import org.objectweb.asm.tree.ModuleOpenNode
import org.objectweb.asm.tree.ModuleProvideNode
import org.objectweb.asm.tree.ModuleRequireNode
import org.objectweb.asm.tree.MethodNode
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
    // need to write something to convert the actual flags to (DONE)
    // assume that methods and functions are in the same order (DONE)
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

    // // Basic class information
    // printDiff(old, new, "version", ClassNode::version) {"version: ${it.toString()}"} // int TODO: hex? 
    // printDiff(old, new, "access", ClassNode::access) {"access: ${it.toString()}"} // int TODO: format as actual flags
    // printDiff(old, new, "name", ClassNode::name) {"name: $it"} // String
    // printDiff(old, new, "signature", ClassNode::signature) {"signature: $it"} // String
    // printDiff(old, new, "superName", ClassNode::superName) {"superName: $it"} // String
    // printDiff(old, new, "interfaces", ClassNode::interfaces) {"interFaces: $it"} // List<String>

    // // Source and debug information
    // printDiff(old, new, "sourceFile", ClassNode::sourceFile) {"sourceFile: $it"} // String
    // printDiff(old, new, "sourceDebug", ClassNode::sourceDebug) {"sourceDebug: $it"} // String

    // // Enclosing context
    // printDiff(old, new, "module", ClassNode::module) {"module: $it"} // 
    // printDiff(old, new, "outerClass", ClassNode::outerClass) {"outerClass: $it"}
    // printDiff(old, new, "outerMethod", ClassNode::outerMethod) {"outerMethod: $it"}
    // printDiff(old, new, "outerMethodDesc", ClassNode::outerMethodDesc) {"outerMethodDesc: $it"}

    // // Annotations
    // printDiff(old, new, "visibleAnnotations", ClassNode::visibleAnnotations) {"visibleAnnotations: $it"}
    // printDiff(old, new, "invisibleAnnotations", ClassNode::invisibleAnnotations) {"invisibleAnnotations: $it"}
    // printDiff(old, new, "visibleTypeAnnotations", ClassNode::visibleTypeAnnotations) {"visibleTypeAnnotations: $it"}
    // printDiff(old, new, "invisibleTypeAnnotations", ClassNode::invisibleTypeAnnotations) {"invisibleTypeAnnotations: $it"}

    // // Attrs
    // printDiff(old, new, "attrs", ClassNode::attrs) {"attrs: $it"}

    // // Nest related stuff
    // printDiff(old, new, "innterClasses", ClassNode::innerClasses) {"innerClasses: $it"}
    // printDiff(old, new, "nestHostClass", ClassNode::nestHostClass) {"name: $it"}
    // printDiff(old, new, "nestMembers", ClassNode::nestMembers) {"nestMembers: $it"}
    // printDiff(old, new, "permittedSubclasses", ClassNode::permittedSubclasses) {"permittedSubclasses: $it"}
    // printDiff(old, new, "recordComponents", ClassNode::recordComponents) {"recordComponents: $it"}
    // printDiff(old, new, "fields", ClassNode::fields) {"fields: $it"}
    // printDiff(old, new, "methods", ClassNode::methods) {"methods: $it"}

    // log.info {"Finished comparing files."}

    println("Old name " + old.name)
    println("New name " + new.name)

    var changes = bruteForce(old, new)
    for (i in changes.indices) {
      println("Difference in: " + changes[i].type)
    }

    return changes // TODO: diff
  }

  fun bruteForce(old: ClassNode, new: ClassNode): List<Change> {
    // look into:
    // function pointers are ways to pass HOFs
    // since e.g. old.access is actually a function call ::getAccess (or whatever)
    // write a HOF that runs through all of them to make this easier to read
    // test("attrs", ClassNode::attrs, ::diffAttributeList)
    // function overloading... maybe?? try it and see if its easier to read
    var differences = mutableListOf<Change>()

    if (interpretAccess(old.access).sorted() != interpretAccess(new.access).sorted()) differences += Change("access")
    if (diffAttributeList(old.attrs, new.attrs)) differences += Change("attrs")
    if (diffFieldNodeList(old.fields, new.fields)) differences += Change("fields")
    if (diffInnerClassNodeList(old.innerClasses, new.innerClasses)) differences += Change("innerClasses")
    if (old.interfaces?.sorted() != new.interfaces?.sorted()) differences += Change("interfaces")
    if (diffAnnotationNodeList(old.invisibleAnnotations, new.invisibleAnnotations)) differences += Change("invisibleAnnotations")
    if (diffTypeAnnotationNodeList(old.invisibleTypeAnnotations, new.invisibleTypeAnnotations)) differences += Change("invisibleTypeAnnotations")
    if (diffMethodNodeList(old.methods, new.methods)) differences += Change("methods")
    if (diffModuleNode(old.module, new.module)) differences += Change("module")
    if (old.name != new.name) differences += Change("name")
    if (old.nestHostClass != new.nestHostClass) differences += Change("nestHostClass")
    if (old.nestMembers?.sorted() != new.nestMembers?.sorted()) differences += Change("nestHostClass")
    if (old.outerClass != new.outerClass) differences += Change("outerClass")
    if (old.outerMethod != new.outerMethod) differences += Change("outerMethod")
    if (old.outerMethodDesc != new.outerMethodDesc) differences += Change("outerMethodDesc")
    if (old.permittedSubclasses?.sorted() != new.permittedSubclasses?.sorted()) differences += Change("permittedSubclasses")
    if (diffRecordComponentNodeList(old.recordComponents, new.recordComponents)) differences += Change("recordComponents")
    // https://docs.oracle.com/en/java/javase/17/language/records.html record classes are a thing
    if (old.signature != new.signature) differences += Change("signature")
    if (old.sourceDebug != new.sourceDebug) differences += Change("sourceDebug")
    if (old.sourceFile != new.sourceFile) differences += Change("sourceFile")
    if (old.superName != new.superName) differences += Change("superName")
    if (old.version != new.version) differences += Change("version")
    if (diffAnnotationNodeList(old.visibleAnnotations, new.visibleAnnotations)) differences += Change("visibleAnnotations")
    if (diffTypeAnnotationNodeList(old.visibleTypeAnnotations, new.visibleTypeAnnotations)) differences += Change("visibleTypeAnnotations")

    return differences
  }

  fun interpretAccess(access: Int): List<String> {
    // source: https://asm.ow2.io/javadoc/org/objectweb/asm/Opcodes.html
    // look for function that does this already
    // check if access codes are bit fields/flags vs enums
    var flags = mutableListOf<String>()

    if ((access and Opcodes.ACC_ABSTRACT) != 0) flags += "abstract"
    if ((access and Opcodes.ACC_ANNOTATION) != 0) flags += "annotation"
    if ((access and Opcodes.ACC_BRIDGE) != 0) flags += "bridge"
    if ((access and Opcodes.ACC_DEPRECATED) != 0) flags += "deprecated"
    if ((access and Opcodes.ACC_ENUM) != 0) flags += "enum"
    if ((access and Opcodes.ACC_FINAL) != 0) flags += "final"
    if ((access and Opcodes.ACC_INTERFACE) != 0) flags += "interface"
    if ((access and Opcodes.ACC_MANDATED) != 0) flags += "mandated"
    if ((access and Opcodes.ACC_MODULE) != 0) flags += "module"
    if ((access and Opcodes.ACC_NATIVE) != 0) flags += "native"
    if ((access and Opcodes.ACC_OPEN) != 0) flags += "open"
    if ((access and Opcodes.ACC_PRIVATE) != 0) flags += "private"
    if ((access and Opcodes.ACC_PROTECTED) != 0) flags += "protected"
    if ((access and Opcodes.ACC_PUBLIC) != 0) flags += "public"
    if ((access and Opcodes.ACC_RECORD) != 0) flags += "record"
    if ((access and Opcodes.ACC_STATIC) != 0) flags += "static"
    if ((access and Opcodes.ACC_STATIC_PHASE) != 0) flags += "static_phase"
    if ((access and Opcodes.ACC_STRICT) != 0) flags += "strict"
    if ((access and Opcodes.ACC_SUPER) != 0) flags += "super"
    if ((access and Opcodes.ACC_SYNCHRONIZED) != 0) flags += "synchronized"
    if ((access and Opcodes.ACC_SYNTHETIC) != 0) flags += "synthetic"
    if ((access and Opcodes.ACC_TRANSIENT) != 0) flags += "transient"
    if ((access and Opcodes.ACC_TRANSITIVE) != 0) flags += "transitive"
    if ((access and Opcodes.ACC_VARARGS) != 0) flags += "varargs"
    if ((access and Opcodes.ACC_VOLATILE) != 0) flags += "volatile"

    return flags
  }

  fun diffAttributeList(old: List<Attribute>?, new: List<Attribute>?): Boolean {
    if (old == null || new == null) {
      return old == new
    }

    if (old.size != new.size) return false

    val oldSorted = old.sortedBy{it.type}
    val newSorted = new.sortedBy{it.type}

    for (i in oldSorted.indices) {
      if (!checkAttribute(oldSorted[i], newSorted[i])) {
        return false
      }
    }
    
    return true
  }

  fun diffFieldNodeList(old: List<FieldNode>?, new: List<FieldNode>?): Boolean {
    if (old == null || new == null) {
        return old == new
    }

    if (old.size != new.size) return false

    val oldSorted = old.sortedBy{it.name}
    val newSorted = new.sortedBy{it.name}

    for (i in oldSorted.indices) {
      if (!diffFieldNode(oldSorted[i], newSorted[i])) {
        return false
      }
    }

    return true
  }

  fun checkAttribute(old: Attribute, new: Attribute): Boolean = old.type == new.type

  fun diffFieldNode(old: FieldNode, new: FieldNode): Boolean =
    (old.access == new.access) &&
    diffAttributeList(old.attrs, new.attrs) &&
    (old.desc == new.desc) &&
    diffAnnotationNodeList(old.invisibleAnnotations, new.invisibleAnnotations) &&
    diffTypeAnnotationNodeList(old.invisibleTypeAnnotations, new.invisibleTypeAnnotations) &&
    (old.name == new.name) &&
    (old.signature == new.signature) &&
    (old.value == new.value) &&
    diffAnnotationNodeList(old.visibleAnnotations, new.visibleAnnotations) &&
    diffTypeAnnotationNodeList(old.visibleTypeAnnotations, new.visibleTypeAnnotations)

  fun diffAnnotationNodeList(old: List<AnnotationNode>?, new: List<AnnotationNode>?): Boolean {
    if (old == null || new == null) return old == new
    if (old.size != new.size) return false

    val oldSorted = old.sortedBy{it.desc}
    val newSorted = new.sortedBy{it.desc}

    for (i in oldSorted.indices) {
      if (!checkAnnotationNode(oldSorted[i], newSorted[i])) return false
    }

    return true
  }

  fun checkAnnotationNode(old: AnnotationNode, new: AnnotationNode): Boolean =
    (old.desc == new.desc) &&
    checkAnnotationNodeValues(old.values, new.values)

  fun checkAnnotationNodeValues(old: List<Any?>?, new: List<Any?>?): Boolean {
    if (old == null || new == null) return old == new
    if (old.size != new.size) return false

    for (i in old.indices) {
      if (!checkAnnotationNodeValue(old[i], new[i])) return false
    }

    return true
  }

  fun checkAnnotationNodeValue(old: Any?, new: Any?): Boolean {
    // TODO Fix this, it is broken
    if (old == null || new == null) return old == new

    return when {
        old is Array<*> && new is Array<*> ->
            old.size == new.size && old.indices.all { checkAnnotationNodeValue(old[it], new[it]) }

        old is List<*> && new is List<*> ->
            old.size == new.size && old.indices.all { checkAnnotationNodeValue(old[it], new[it]) }

        old is AnnotationNode && new is AnnotationNode ->
            checkAnnotationNode(old, new)

        old is org.objectweb.asm.Type && new is org.objectweb.asm.Type ->
            old.descriptor == new.descriptor

        else -> old == new
    }
  }

  fun diffTypeAnnotationNodeList(old: List<TypeAnnotationNode>?, new: List<TypeAnnotationNode>?): Boolean {
    if (old == null || new == null) return old == new
    if (old.size != new.size) return false

    for (i in old.indices) {
      if (!diffTypeAnnotationNode(old[i], new[i])) return false
    }

    return true
  }

  fun diffTypeAnnotationNode(old: TypeAnnotationNode, new: TypeAnnotationNode): Boolean {
    // Guaranteed non-null
    if (old.typeRef != new.typeRef) return false
    if (!diffTypePath(old.typePath, new.typePath)) return false

    return true
  }

  fun diffTypePath(old: TypePath, new: TypePath): Boolean = old.toString() == new.toString()
  // Read up to check if this is ok - this is from ChatGPT
  // TODO TODO TODO

  fun diffInnerClassNodeList(old: List<InnerClassNode>?, new: List<InnerClassNode>?): Boolean {
    if (old == null || new == null) return old == new
    if (old.size != new.size) return false

    val oldSorted = old.sortedBy{it.name}
    val newSorted = new.sortedBy{it.name}

    for (i in oldSorted.indices) {
      if (!diffInnerClassNode(oldSorted[i], newSorted[i])) return false
    }

    return true
  }

  fun diffInnerClassNode(old: InnerClassNode, new: InnerClassNode): Boolean =
    (interpretAccess(old.access).sorted() == interpretAccess(new.access).sorted()) &&
    (old.innerName == new.innerName) &&
    (old.name == new.name) &&
    (old.outerName == new.outerName)

  fun diffRecordComponentNodeList(old: List<RecordComponentNode>?, new: List<RecordComponentNode>?): Boolean {
    if (old == null || new == null) return old == new
    if (old.size != new.size) return false

    val oldSorted = old.sortedBy{it.name}
    val newSorted = new.sortedBy{it.name}

    for (i in oldSorted.indices) {
      if (!diffRecordComponentNode(oldSorted[i], newSorted[i])) return false
    }

    return true
  }

  fun diffRecordComponentNode(old: RecordComponentNode, new: RecordComponentNode): Boolean =
    diffAttributeList(old.attrs, new.attrs) &&
    (old.descriptor == new.descriptor) &&
    diffAnnotationNodeList(old.invisibleAnnotations, new.invisibleAnnotations) &&
    diffTypeAnnotationNodeList(old.invisibleTypeAnnotations, new.invisibleTypeAnnotations) &&
    (old.name == new.name) &&
    (old.signature == new.signature) &&
    diffAnnotationNodeList(old.visibleAnnotations, new.visibleAnnotations) &&
    diffTypeAnnotationNodeList(old.visibleTypeAnnotations, new.visibleTypeAnnotations)

  fun diffModuleNode(old: ModuleNode?, new: ModuleNode?): Boolean {
    if (old == null || new == null) return old == new

    return (
      (interpretAccess(old.access).sorted() == interpretAccess(new.access).sorted()) &&
      diffModuleExportNodeList(old.exports, new.exports) &&
      (old.mainClass == new.mainClass) &&
      (old.name == new.name) &&
      diffModuleOpenNodeList(old.opens, new.opens) &&
      (old.packages?.sorted() == new.packages?.sorted()) &&
      diffModuleProvideNodeList(old.provides, new.provides) &&
      diffModuleRequireNodeList(old.requires, new.requires) &&
      (old.uses?.sorted() == new.uses?.sorted()) &&
      (old.version == new.version)
    )
  }

  fun diffModuleExportNodeList(old: List<ModuleExportNode>?, new: List<ModuleExportNode>?): Boolean {
    if (old == null || new == null) return old == new
    if (old.size != new.size) return false

    val oldSorted = old.sortedWith(
      compareBy<ModuleExportNode>(
        {it.packaze},
        {it.modules?.sorted()?.joinToString()},
        {it.access}
      )
    )
    val newSorted = new.sortedWith(
      compareBy<ModuleExportNode>(
        {it.packaze},
        {it.modules?.sorted()?.joinToString()},
        {it.access}
      )
    )

    for (i in oldSorted.indices) {
      if (!diffModuleExportNode(oldSorted[i], newSorted[i])) return false
    }

    return true
  }

  fun diffModuleExportNode(old: ModuleExportNode, new: ModuleExportNode): Boolean =
    (interpretAccess(old.access).sorted() == interpretAccess(new.access).sorted()) &&
    (old.modules?.sorted() == new.modules?.sorted()) &&
    (old.packaze == new.packaze)

  fun diffModuleOpenNodeList(old: List<ModuleOpenNode>?, new: List<ModuleOpenNode>?): Boolean {
    if (old == null || new == null) return old == new
    if (old.size != new.size) return false

    val oldSorted = old.sortedWith(
      compareBy<ModuleOpenNode>(
        {it.packaze},
        {it.modules?.sorted()?.joinToString()},
        {it.access}
      )
    )
    val newSorted = new.sortedWith(
      compareBy<ModuleOpenNode>(
        {it.packaze},
        {it.modules?.sorted()?.joinToString()},
        {it.access}
      )
    )

    for (i in oldSorted.indices) {
      if (!diffModuleOpenNode(oldSorted[i], newSorted[i])) return false
    }

    return true
  }

  fun diffModuleOpenNode(old: ModuleOpenNode, new: ModuleOpenNode): Boolean =
    (interpretAccess(old.access).sorted() == interpretAccess(new.access).sorted()) &&
    (old.modules?.sorted() == new.modules?.sorted()) &&
    (old.packaze == new.packaze)

  fun diffModuleProvideNodeList(old: List<ModuleProvideNode>?, new: List<ModuleProvideNode>?): Boolean {
    if (old == null || new == null) return old == new
    if (old.size != new.size) return false

    val oldSorted = old.sortedWith(
      compareBy<ModuleProvideNode>(
        {it.service},
        {it.providers?.sorted()?.joinToString()}
      )
    )
    val newSorted = new.sortedWith(
      compareBy<ModuleProvideNode>(
        {it.service},
        {it.providers?.sorted()?.joinToString()}
      )
    )

    for (i in oldSorted.indices) {
      if (!diffModuleProvideNode(oldSorted[i], newSorted[i])) return false
    }

    return true
  }

  fun diffModuleProvideNode(old: ModuleProvideNode, new: ModuleProvideNode): Boolean =
    (old.service == new.service) &&
    (old.providers?.sorted() == new.providers?.sorted())

  fun diffModuleRequireNodeList(old: List<ModuleRequireNode>?, new: List<ModuleRequireNode>?): Boolean {
    if (old == null || new == null) return old == new
    if (old.size != new.size) return false

    val oldSorted = old.sortedWith(
      compareBy<ModuleRequireNode>(
        {it.module},
        {it.version},
        {it.access}
      )
    )
    val newSorted = new.sortedWith(
      compareBy<ModuleRequireNode>(
        {it.module},
        {it.version},
        {it.access}
      )
    )

    for (i in oldSorted.indices) {
      if (!diffModuleRequireNode(oldSorted[i], newSorted[i])) return false
    }

    return true
  }

  fun diffModuleRequireNode(old: ModuleRequireNode, new: ModuleRequireNode): Boolean =
    (interpretAccess(old.access).sorted() == interpretAccess(new.access).sorted()) &&
    (old.module == new.module) &&
    (old.version == new.version)

  fun diffMethodNodeList(old: List<MethodNode>?, new: List<MethodNode>?): Boolean {
    if (old == null || new == null) return old == new
    if (old.size != new.size) return false

    val oldSorted = old.sortedBy{it.name} // need to deal with method overloading
    val newSorted = new.sortedBy{it.name}

    for (i in oldSorted.indices) {
      if (!diffMethodNode(oldSorted[i], newSorted[i])) return false
    }

    return true
  }

  fun diffMethodNode(old: MethodNode, new: MethodNode): Boolean =
    (interpretAccess(old.access).sorted() == interpretAccess(new.access).sorted()) &&
    // annotationdefault
    diffAttributeList(old.attrs, new.attrs) &&
    (old.desc == new.desc) &&
    (old.exceptions?.sorted() == new.exceptions?.sorted()) &&
    // instructions
    (old.invisibleAnnotableParameterCount == new.invisibleAnnotableParameterCount) &&
    diffAnnotationNodeList(old.invisibleAnnotations, new.invisibleAnnotations) &&
    // invisiblelocalvariableannotations
    // invisibleparameterannotations
    diffTypeAnnotationNodeList(old.invisibleTypeAnnotations, new.invisibleTypeAnnotations) &&
    // localvariables
    (old.maxLocals == new.maxLocals) &&
    (old.maxStack == new.maxStack) &&
    (old.name == new.name) &&
    // parameters
    (old.signature == new.signature) &&
    // trycatchblocks
    (old.visibleAnnotableParameterCount == new.visibleAnnotableParameterCount) &&
    diffAnnotationNodeList(old.visibleAnnotations, new.visibleAnnotations) &&
    // visiblelocalvariableannotations
    // visibleparameterannotations
    diffTypeAnnotationNodeList(old.visibleTypeAnnotations, new.visibleTypeAnnotations)


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
class Change(val type: String)
