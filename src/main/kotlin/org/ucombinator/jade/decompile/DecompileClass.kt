package org.ucombinator.jade.decompile

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.ImportDeclaration
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.PackageDeclaration
import com.github.javaparser.ast.`type`.ClassOrInterfaceType
import com.github.javaparser.ast.`type`.ReferenceType
import com.github.javaparser.ast.`type`.Type
import com.github.javaparser.ast.`type`.TypeParameter
import com.github.javaparser.ast.body.* // ktlint-disable no-wildcard-imports
import com.github.javaparser.ast.comments.BlockComment
import com.github.javaparser.ast.expr.* // ktlint-disable no-wildcard-imports
import com.github.javaparser.ast.stmt.BlockStmt
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.* // ktlint-disable no-wildcard-imports
import org.ucombinator.jade.classfile.ClassName
import org.ucombinator.jade.classfile.Descriptor
import org.ucombinator.jade.classfile.Flags
import org.ucombinator.jade.classfile.Signature
import org.ucombinator.jade.javaparser.JavaParser
import org.ucombinator.jade.util.Log
import org.ucombinator.jade.util.Fourple
import org.ucombinator.jade.util.Fiveple

// TODO: rename package to `translate` or `transform` or `transformation`?
object DecompileClass {
  // TODO: ktlint: "${foo}"

  fun decompileLiteral(node: Any?): Expression? =
    when (node) {
      // TODO: improve formatting of literals?
      null -> null
      is Int -> IntegerLiteralExpr(node.toString())
      is Long -> LongLiteralExpr(node.toString())
      is Float -> DoubleLiteralExpr(node.toString() + "") // `JavaParser` uses Doubles for Floats
      is Double -> DoubleLiteralExpr(node.toString() + "D")
      is String -> StringLiteralExpr(node.toString())
      is org.objectweb.asm.Type -> ClassExpr(Descriptor.fieldDescriptor(node.getDescriptor()))
      else -> throw Exception("unimplemented literal '$node'")
    }

  private fun typeToName(t: Type?): Name? =
    when (t) {
      null -> null
      is ClassOrInterfaceType -> Name(typeToName(t.getScope().orElse(null)), t.getName().getIdentifier())
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
          NodeList<MemberValuePair>(
            (0..vs.size step 2).map { i -> MemberValuePair(vs.get(i) as String, decompileLiteral(vs.get(i + 1))) }
          )
        )
    }
  }

  //  java.util.List<_ <: AnnotationNode>*
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
    val `type`: Type =
      if (node.signature == null) { Descriptor.fieldDescriptor(node.desc) }
      else { Signature.typeSignature(node.signature) }
    val name = SimpleName(node.name)
    val initializer = decompileLiteral(node.value)
    val variables = NodeList<VariableDeclarator>(VariableDeclarator(`type`, name, initializer))

    return FieldDeclaration(modifiers, annotations, variables)
  }

  // TODO: flatten Pair<Pair<...>>
  private fun decompileParameter(
    method: MethodNode,
    paramCount: Int,
    parameter: IndexedValue<Fourple<Type,ParameterNode?, List<AnnotationNode>, List<AnnotationNode>>>
  ): Parameter {
    val index = parameter.index
    val (typ, node, a1, a2) = parameter.value
    val flags =
      if (node == null) { listOf() }
      else { Flags.parameterFlags(node.access) }
    val modifiers = Flags.toModifiers(flags)
    val annotations: NodeList<AnnotationExpr> = decompileAnnotations(a1, a2, null, null)
    val `type`: Type = typ
    val isVarArgs: Boolean =
      Flags.methodFlags(method.access).contains(Flags.ACC_VARARGS) &&
        index == paramCount - 1
    val varArgsAnnotations = NodeList<AnnotationExpr>() // TODO?
    val name: SimpleName =
      SimpleName( // TODO: make consistent with analysis.ParameterVar
        if (node == null) { "parameter${index + 1}" }
        else { node.name }
      )
    return Parameter(modifiers, annotations, `type`, isVarArgs, varArgsAnnotations, name)
  }

  fun <T> List<T>.tail(): List<T> = this.subList(1, this.size)
  fun <A, B> Pair<A,B>._1(): A = this.first
  fun <A, B> Pair<A,B>._2(): B = this.second
  fun <A, B, C> Triple<A,B, C>._1(): A = this.first
  fun <A, B, C> Triple<A,B, C>._2(): B = this.second
  fun <A, B, C> Triple<A,B, C>._3(): C = this.third
  fun parameterTypes(desc: List<Type>, sig: List<Type>, params: List<ParameterNode>): List<Type> =
    when {
      desc.isNotEmpty()
        && params.isNotEmpty()
        && (Flags.parameterFlags(params.first().access).contains(Flags.ACC_SYNTHETIC)
          || Flags.parameterFlags(params.first().access).contains(Flags.ACC_MANDATED))
      -> // TODO: Flags.checkParameter(access, Modifier)
        listOf(desc.first()) +
          parameterTypes(desc.tail(), sig, params.tail())
      desc.isNotEmpty()
        && sig.isNotEmpty()
        && params.isNotEmpty()
      ->
        listOf(sig.first()) +
          parameterTypes(desc.tail(), sig.tail(), params.tail())
      params.isEmpty()
        -> sig
      else -> throw Exception("failed to construct parameter types: $desc, $sig, $params")
    }

  fun <A> nullToSeq(x: List<A>?): List<A> = if (x === null) { listOf() } else { x }
  // fun <A> nullToSeq(x: Array<A>?): Array<A> = if (x === null) { arrayOf() } else { x }
  fun <A,B,C,D> zipAll(a: List<A>, b: List<B>, c: List<C>, d: List<D>): List<Fourple<A,B,C,D>> {
    TODO()
  }


  // BodyDeclaration<_ <: BodyDeclaration<_>>
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
    val descriptor: Pair<List<Type>, Type> = Descriptor.methodDescriptor(node.desc)
    val sig = //: Fourple<List<TypeParameter>, List<Type>, Type, out List<ReferenceType>> =
      if (node.signature != null) { Signature.methodSignature(node.signature) }
      else {
        Fourple(listOf(), descriptor.first, descriptor.second, node.exceptions.map(ClassName::classNameType))
      }
    val parameterNodes = nullToSeq(node.parameters)
    if (node.parameters != null && sig._2.size != node.parameters.size) {
      // TODO: check if always in an enum
    }
    val typeParameters: NodeList<TypeParameter> = NodeList(sig._1)
    val ps =
      zipAll(
        parameterTypes(descriptor._1(), sig._2, parameterNodes),
        parameterNodes,
        nullToSeq(node.visibleParameterAnnotations.toList()),
        nullToSeq(node.invisibleParameterAnnotations.toList()))
      .withIndex()
    val parameters: NodeList<Parameter> = NodeList(ps.map { x -> decompileParameter(node, sig._2.size, x) })
    val `type`: Type = sig._3
    val thrownExceptions: NodeList<ReferenceType> = NodeList(sig._4)
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
          SimpleName(ClassName.className(classNode.name).getIdentifier()) /*TODO*/,
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
          `type`,
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
        |""".trimMargin()
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
      PackageDeclaration(NodeList<AnnotationExpr>() /*TODO*/, fullClassName.getQualifier().orElse(Name()))
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
      val simpleName = SimpleName(fullClassName.getIdentifier())
      // `extendedTypes` may be multiple if on an interface
      // TODO: test if should be Descriptor.className
      val (
        typeParameters: NodeList<TypeParameter>,
        extendedTypes: NodeList<ClassOrInterfaceType>,
        implementedTypes: NodeList<ClassOrInterfaceType>
      ) =
        if (node.signature != null) {
          val s = Signature.classSignature(node.signature)
          Triple(NodeList(s._1()), NodeList(s._2()), NodeList(s._3()))
        } else {
          Triple(
            NodeList(),
            if (node.superName == null) {
              NodeList()
            } else {
              NodeList(ClassName.classNameType(node.superName))
            },
            NodeList(node.interfaces.map { ClassName.classNameType(it) })
          )
        }
        // NodeList[BodyDeclaration[_ <: BodyDeclaration[_]]]
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
        members
      )
    }

    if (classOrInterfaceDeclaration.isInterface) {
      classOrInterfaceDeclaration.setExtendedTypes(classOrInterfaceDeclaration.getImplementedTypes())
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
