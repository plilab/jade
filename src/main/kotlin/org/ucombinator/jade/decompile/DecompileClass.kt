package org.ucombinator.jade.decompile

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.ImportDeclaration
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.PackageDeclaration
import com.github.javaparser.ast.body.*
import com.github.javaparser.ast.comments.BlockComment
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.type.ClassOrInterfaceType
import com.github.javaparser.ast.type.ReferenceType
import com.github.javaparser.ast.type.Type
import com.github.javaparser.ast.type.TypeParameter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*
import org.ucombinator.jade.classfile.ClassName
import org.ucombinator.jade.classfile.Descriptor
import org.ucombinator.jade.classfile.Flags
import org.ucombinator.jade.classfile.Signature
import org.ucombinator.jade.classfile.MethodSignature
import org.ucombinator.jade.javaparser.JavaParser
import org.ucombinator.jade.util.Lists.pairs
import org.ucombinator.jade.util.Lists.tail
import org.ucombinator.jade.util.Lists.zipAll
import org.ucombinator.jade.util.Tuples.Fourple
import org.ucombinator.jade.util.Tuples._1
import org.ucombinator.jade.util.Tuples._2
import org.ucombinator.jade.util.Tuples._3

// TODO: rename package to `translate` or `transform` or `transformation`?
object DecompileClass {
  // TODO: ktlint: "${foo}"

  fun decompileLiteral(node: Any?): Expression? =
    when (node) {
      // TODO: improve formatting of literals?
      null -> null
      is Int -> IntegerLiteralExpr(node.toString())
      is Long -> LongLiteralExpr(node.toString())
      is Float -> DoubleLiteralExpr("${node}F") // `JavaParser` uses Doubles for Floats
      is Double -> DoubleLiteralExpr("${node}D")
      is String -> StringLiteralExpr(node)
      is org.objectweb.asm.Type -> ClassExpr(Descriptor.fieldDescriptor(node.descriptor))
      else -> throw Exception("unimplemented literal '$node'")
    }

  private fun typeToName(t: Type?): Name? =
    when (t) {
      null -> null
      is ClassOrInterfaceType -> Name(typeToName(t.scope.orElse(null)), t.name.identifier)
      else -> throw Exception("failed to convert type $t to a name")
    }

  private fun decompileAnnotation(node: AnnotationNode): AnnotationExpr {
    val name = typeToName(Descriptor.fieldDescriptor(node.desc))
    val vs = node.values
    return when {
      vs === null -> MarkerAnnotationExpr(name)
      vs.size == 1 -> SingleMemberAnnotationExpr(name, decompileLiteral(vs.first()))
      else ->
        NormalAnnotationExpr(
          name,
          NodeList(vs.pairs().map { MemberValuePair(it.first as String, decompileLiteral(it.second)) })
        )
    }
  }

  private fun decompileAnnotations(vararg nodes: List<AnnotationNode>?): NodeList<AnnotationExpr> =
    NodeList<AnnotationExpr>(nodes.filterNotNull().flatMap { it.map(::decompileAnnotation) })

  private fun decompileField(node: FieldNode): FieldDeclaration {
    // // attrs (ignore?)
    val modifiers = Flags.toModifiers(Flags.fieldFlags(node.access))
    val annotations: NodeList<AnnotationExpr> = decompileAnnotations(
      node.visibleAnnotations,
      node.invisibleAnnotations,
      node.visibleTypeAnnotations,
      node.invisibleTypeAnnotations
    )
    val type: Type =
      if (node.signature === null) {
        Descriptor.fieldDescriptor(node.desc)
      } else {
        Signature.typeSignature(node.signature)
      }
    val name = SimpleName(node.name)
    val initializer = decompileLiteral(node.value)
    val variables = NodeList<VariableDeclarator>(VariableDeclarator(type, name, initializer))

    return FieldDeclaration(modifiers, annotations, variables)
  }

  // TODO: flatten Pair<Pair<...>>
  private fun decompileParameter(
    method: MethodNode,
    paramCount: Int,
    parameter: IndexedValue<Fourple<Type?, ParameterNode?, List<AnnotationNode>?, List<AnnotationNode>?>>
  ): Parameter {
    val index = parameter.index
    val (type, node, a1, a2) = parameter.value
    val flags = if (node === null) listOf() else Flags.parameterFlags(node.access)
    val modifiers = Flags.toModifiers(flags)
    val annotations: NodeList<AnnotationExpr> = decompileAnnotations(a1, a2, null, null)
    val isVarArgs: Boolean =
      Flags.methodFlags(method.access).contains(Flags.ACC_VARARGS) &&
        index == paramCount - 1
    val varArgsAnnotations = NodeList<AnnotationExpr>() // TODO?
    // TODO: make consistent with analysis.ParameterVar
    val name: SimpleName = SimpleName(if (node === null) "parameter${index + 1}" else node.name)
    return Parameter(modifiers, annotations, type, isVarArgs, varArgsAnnotations, name)
  }

  fun parameterTypes(desc: List<Type>, sig: List<Type>, params: List<ParameterNode>): List<Type> =
    when {
      desc.isNotEmpty() &&
        params.isNotEmpty() && (
        Flags.parameterFlags(params.first().access).contains(Flags.ACC_SYNTHETIC) ||
          Flags.parameterFlags(params.first().access).contains(Flags.ACC_MANDATED)
        )
      // TODO: Flags.checkParameter(access, Modifier)
      -> listOf(desc.first()) + parameterTypes(desc.tail(), sig, params.tail())
      desc.isNotEmpty() &&
        sig.isNotEmpty() &&
        params.isNotEmpty()
      -> listOf(sig.first()) + parameterTypes(desc.tail(), sig.tail(), params.tail())
      params.isEmpty()
      -> sig
      else -> throw Exception("failed to construct parameter types: $desc, $sig, $params")
    }

  fun <A> nullToSeq(x: List<A>?): List<A> = if (x === null) listOf() else x

  fun decompileMethod(classNode: ClassNode, node: MethodNode): BodyDeclaration<out BodyDeclaration<*>> {
    // attr (ignore?)
    // instructions
    // tryCatchBlocks
    // localVariables
    // visibleLocalVariableAnnotations
    // invisibleLocalVariableAnnotations
    // TODO: JPModifier.Keyword.DEFAULT
    // TODO: catch exceptions and return a stub method
    val modifiers = Flags.toModifiers(Flags.methodFlags(node.access))
    val annotations: NodeList<AnnotationExpr> = decompileAnnotations(
      node.visibleAnnotations,
      node.invisibleAnnotations,
      node.visibleTypeAnnotations,
      node.invisibleTypeAnnotations
    )
    val descriptor = Descriptor.methodDescriptor(node.desc)
    val sig =
      if (node.signature === null) {
        MethodSignature(listOf(), descriptor.parameterTypes, descriptor.returnType, node.exceptions.map(ClassName::classNameType))
      } else {
        Signature.methodSignature(node.signature)
      }
    val parameterNodes = nullToSeq(node.parameters)
    if (node.parameters != null && sig.parameterTypes.size != node.parameters.size) {
      // TODO: check if always in an enum
    }
    val typeParameters: NodeList<TypeParameter> = NodeList(sig.typeParameters)
    val ps =
      zipAll(
        parameterTypes(descriptor.parameterTypes, sig.parameterTypes, parameterNodes),
        parameterNodes,
        nullToSeq(node.visibleParameterAnnotations.toList()),
        nullToSeq(node.invisibleParameterAnnotations.toList())
      ).withIndex()
    val parameters: NodeList<Parameter> = NodeList(ps.map { decompileParameter(node, sig.parameterTypes.size, it) })
    val type: Type = sig.returnType
    val thrownExceptions: NodeList<ReferenceType> = NodeList(sig.exceptionTypes)
    val name: SimpleName = SimpleName(node.name)
    val body: BlockStmt = DecompileMethodBody.decompileBodyStub(node)
    val receiverParameter: ReceiverParameter? = null // TODO
    val bodyDeclaration = when (node.name) {
      "<clinit>" ->
        InitializerDeclaration(true, body)
      "<init>" ->
        ConstructorDeclaration(
          modifiers,
          annotations,
          typeParameters,
          SimpleName(ClassName.className(classNode.name).identifier) /*TODO*/,
          parameters,
          thrownExceptions,
          body,
          receiverParameter
        )
      else ->
        MethodDeclaration(
          modifiers,
          annotations,
          typeParameters,
          type,
          name,
          parameters,
          thrownExceptions,
          body,
          receiverParameter
        )
    }
    // TODO: Decompile.methods.add(bodyDeclaration to ((classNode, node)))
    return bodyDeclaration
  }

  fun decompileClass(node: ClassNode): CompilationUnit {
    val comment = BlockComment(
      """
        |* Source File: ${node.sourceFile}
        |* Class-file Format Version: ${node.version}
        |* Source Debug Extension: ${node.sourceDebug} // See JSR-45 https://www.jcp.org/en/jsr/detail?id=045
        |
      """.trimMargin()
    )
    // outerClass
    // outerMethod
    // outerMethodDesc
    // attr (ignore?)
    // innerClasses
    // nestHostClass
    // nestMember

    val fullClassName: Name = ClassName.className(node.name)

    val packageDeclaration =
      PackageDeclaration(NodeList<AnnotationExpr>() /*TODO*/, fullClassName.qualifier.orElse(Name()))
    val imports = NodeList<ImportDeclaration>() // TODO

    val classOrInterfaceDeclaration = run {
      // TODO: assert ACC_SUPER
      val modifiers = Flags.toModifiers(Flags.classFlags(node.access))
      val annotations: NodeList<AnnotationExpr> = decompileAnnotations(
        node.visibleAnnotations,
        node.invisibleAnnotations,
        node.visibleTypeAnnotations,
        node.invisibleTypeAnnotations
      )
      val isInterface: Boolean = (node.access and Opcodes.ACC_INTERFACE) != 0
      val simpleName = SimpleName(fullClassName.identifier)
      // `extendedTypes` may be multiple if on an interface
      // TODO: test if should be Descriptor.className
      val (
        typeParameters: NodeList<TypeParameter>,
        extendedTypes: NodeList<ClassOrInterfaceType>,
        implementedTypes: NodeList<ClassOrInterfaceType>,
        permittedTypes: NodeList<ClassOrInterfaceType> // TODO: implement
      ) =
        if (node.signature === null) {
          // TODO: maybe change Fourple to MethodSignature
          Fourple(
            NodeList<TypeParameter>(),
            if (node.superName === null) NodeList() else NodeList(ClassName.classNameType(node.superName)),
            NodeList(node.interfaces.map { ClassName.classNameType(it) }),
            NodeList<ClassOrInterfaceType>()
          )
        } else {
          val s = Signature.classSignature(node.signature)
          Fourple(NodeList(s.typeParameters), NodeList(s.superclass), NodeList(s.interfaces), NodeList<ClassOrInterfaceType>())
        }
      val members: NodeList<BodyDeclaration<*>> = run {
        val list = NodeList<BodyDeclaration<*>>()
        list.addAll(NodeList(node.fields.map(::decompileField)))
        list.addAll(NodeList(node.methods.map { decompileMethod(node, it) }))
        // TODO
        list
      }

      ClassOrInterfaceDeclaration(
        modifiers,
        annotations,
        isInterface,
        simpleName,
        typeParameters,
        extendedTypes,
        implementedTypes,
        permittedTypes,
        members
      )
    }

    if (classOrInterfaceDeclaration.isInterface) {
      classOrInterfaceDeclaration.setExtendedTypes(classOrInterfaceDeclaration.implementedTypes)
      classOrInterfaceDeclaration.setImplementedTypes(NodeList())
    }

    val types = NodeList<TypeDeclaration<*>>()
    types.add(classOrInterfaceDeclaration)

    // TODO: ModuleExportNode
    // TODO: ModuleNode
    // TODO: ModuleOpenNode
    // TODO: ModuleProvideNode
    // TODO: ModuleRequireNode
    val module = null // TODO node.module

    val compilationUnit = CompilationUnit(packageDeclaration, imports, types, module)
    JavaParser.setComment(compilationUnit, comment)
    Decompile.classes += compilationUnit to node
    // TODO: this.log.debug("++++ decompile class ++++\n" + compilationUnit.toString())

    return compilationUnit
  }
}
