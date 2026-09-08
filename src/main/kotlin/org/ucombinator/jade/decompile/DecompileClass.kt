package org.ucombinator.jade.decompile

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.DataKey
import com.github.javaparser.ast.ImportDeclaration
import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.PackageDeclaration
import com.github.javaparser.ast.body.AnnotationDeclaration
import com.github.javaparser.ast.body.BodyDeclaration
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.ConstructorDeclaration
import com.github.javaparser.ast.body.FieldDeclaration
import com.github.javaparser.ast.body.InitializerDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.body.Parameter
import com.github.javaparser.ast.body.ReceiverParameter
import com.github.javaparser.ast.body.TypeDeclaration
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.comments.JavadocComment
import com.github.javaparser.ast.expr.AnnotationExpr
import com.github.javaparser.ast.expr.ArrayInitializerExpr
import com.github.javaparser.ast.expr.ClassExpr
import com.github.javaparser.ast.expr.DoubleLiteralExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.FieldAccessExpr
import com.github.javaparser.ast.expr.IntegerLiteralExpr
import com.github.javaparser.ast.expr.LongLiteralExpr
import com.github.javaparser.ast.expr.MarkerAnnotationExpr
import com.github.javaparser.ast.expr.MemberValuePair
import com.github.javaparser.ast.expr.Name
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.expr.NormalAnnotationExpr
import com.github.javaparser.ast.expr.NullLiteralExpr
import com.github.javaparser.ast.expr.SimpleName
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr
import com.github.javaparser.ast.expr.StringLiteralExpr
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.type.ClassOrInterfaceType
import com.github.javaparser.ast.type.ReferenceType
import com.github.javaparser.ast.type.Type
import com.github.javaparser.ast.type.TypeParameter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.ParameterNode
import org.ucombinator.jade.classfile.ClassName
import org.ucombinator.jade.classfile.Descriptor
import org.ucombinator.jade.classfile.Flag
import org.ucombinator.jade.classfile.MethodDescriptor
import org.ucombinator.jade.classfile.MethodSignature
import org.ucombinator.jade.classfile.Signature
import org.ucombinator.jade.javaparser.JavaParser
import org.ucombinator.jade.util.Errors
import org.ucombinator.jade.util.Lists.pairs
import org.ucombinator.jade.util.Lists.tail
import org.ucombinator.jade.util.Lists.zipAll
import org.ucombinator.jade.util.Tuples.Fourple

import kotlin.jvm.optionals.getOrNull

/**
 * Handles decompiling class-level constructs.
 * It contains various methods that builds JavaParser AST data structures from corresponding ASM data structures.
 */
object DecompileClass {
  /** Root node for the class. */
  @Suppress("VARIABLE_NAME_INCORRECT_FORMAT")
  val CLASS_NODE = object : DataKey<ClassNode>() {}

  /** Root node for methods. */
  @Suppress("VARIABLE_NAME_INCORRECT_FORMAT")
  val METHOD_NODE = object : DataKey<MethodNode>() {}

  /**
   * Transforms node into a JavaParser literal expression.
   *
   * @param node the target node to decompile.
   * @return a JavaParser Expression representing `node`.
   */
  public fun decompileLiteral(node: Any?): Expression? =
    when (node) {
      // TODO: improve formatting of literals?
      null -> NullLiteralExpr()

      is Int -> IntegerLiteralExpr(node.toString())

      is Long -> LongLiteralExpr("${node}L")

      // JavaParser uses Doubles for Floats
      is Float -> DoubleLiteralExpr("${node}F")

      is Double -> DoubleLiteralExpr("${node}D")

      is String -> StringLiteralExpr(node)

      is org.objectweb.asm.Type -> ClassExpr(Descriptor.fieldDescriptor(node.descriptor))

      else -> Errors.unmatchedType(node)
    }

  /**
   * Transforms a JavaParser Type into a Name.
   *
   * @param t the target type.
   * @return a JavaParser Name for `t`.
   * @throws IllegalArgumentException if `t` is not null and not ClassOrInterfaceType.
   */
  private fun typeToName(t: Type?): Name? =
    when (t) {
      null -> null
      is ClassOrInterfaceType -> Name(typeToName(t.scope.orElse(null)), t.name.identifier)
      else -> throw IllegalArgumentException("Failed to convert type $t to a name")
    }

  /**
   * Decompiles the value of the key-value pairs representing annotations' parameters as described in
   * https://asm.ow2.io/javadoc/org/objectweb/asm/tree/AnnotationNode.html#values, for all possible types of such value.
   *
   * For example, it constructs an Expression representing the string literal "ABC" for the annotation @A(param1="ABC").
   *
   * @param parameter the value of the key-value pairs.
   * @return a JavaParser Expression representing `parameter`.
   * @throws IllegalArgumentException if `parameter` is an invalid Array.
   */
  private fun decompileAnnotationParameter(parameter: Any): Expression? =
    when (parameter) {
      is Array<*> -> {
        // enum's representation, e.g. ["Ljava/lang/annotation/RetentionPolicy;", "RETENTION_POLICY"]
        require(parameter.size == 2) {
          """
          Parameter is of type Array.
          It must be Array of String of size 2 representing enums according to
          https://asm.ow2.io/javadoc/org/objectweb/asm/tree/AnnotationNode.html#values,
          but here it's not of size 2.
          """.trimIndent().replace("\n", " ")
        }

        require(parameter.isArrayOf<String>()) {
          """
          Parameter is of type Array.
          It must be Array of String of size 2 representing enums according to
          https://asm.ow2.io/javadoc/org/objectweb/asm/tree/AnnotationNode.html#values,
          but here it's not Array of String.
          """.trimIndent().replace("\n", " ")
        }

        val scope = Descriptor.fieldDescriptor(parameter[0] as String) as ClassOrInterfaceType
        val enumName = SimpleName(parameter[1] as String)

        // TODO: Populate typeArguments
        FieldAccessExpr(ClassName.classNameExpr(scope), NodeList(), enumName)
      }

      is AnnotationNode -> {
        decompileAnnotation(parameter)
      }

      is List<*> -> {
        ArrayInitializerExpr(NodeList(parameter.map { decompileAnnotationParameter(it!!) }))
      }

      else -> {
        decompileLiteral(parameter)
      }
    }

  /**
   * Decompiles an ASM AnnotationNode into a JavaParser AnnotationExpr.
   *
   * @param node the target node to decompile.
   * @return a JavaParser AnnotationExpr representing `node`.
   */
  private fun decompileAnnotation(node: AnnotationNode): AnnotationExpr {
    val name = typeToName(Descriptor.fieldDescriptor(node.desc))

    // TODO: Implement writing to a SingleMemberAnnotation in the case there's only 1 default parameter named "value".
    // Currently the SingleMemberAnnotation is written as a NormalAnnotationExpr with the parameter value=...
    return when {
      node.values == null -> MarkerAnnotationExpr(name)

      else -> NormalAnnotationExpr(
        name,
        NodeList(
          node.values.pairs().map {
            MemberValuePair(it.first as String, decompileAnnotationParameter(it.second))
          },
        ),
      )
    }
  }

  /**
   * Decompiles a list of ASM AnnotationNode.
   *
   * @param nodes list of nodes to decompile.
   * @return a list of decompiled JavaParser AnnotationExpr.
   */
  private fun decompileAnnotations(vararg nodes: List<AnnotationNode>?): NodeList<AnnotationExpr> =
    NodeList<AnnotationExpr>(nodes.filterNotNull().flatMap { it.map(::decompileAnnotation) })

  /**
   * Decompiles a ASM FieldNode into a JavaParser FieldDeclaration.
   *
   * @param node the target node to decompile.
   * @return a JavaParser FieldDeclaration representing `node`.
   */
  private fun decompileField(node: FieldNode): FieldDeclaration {
    // attrs (ignore?)
    val modifiers = Flag.toModifiers(Flag.fieldFlags(node.access))

    val annotations: NodeList<AnnotationExpr> = decompileAnnotations(
      node.visibleAnnotations,
      node.invisibleAnnotations,
      node.visibleTypeAnnotations,
      node.invisibleTypeAnnotations,
    )

    val type = if (node.signature != null) {
      Signature.typeSignature(node.signature)
    } else {
      Descriptor.fieldDescriptor(node.desc)
    }
    val name = SimpleName(node.name)

    // If node.value is null, we don't need to initialize anything.
    // If this field is a primitive field, assigning null is invalid.
    val initializer = node.value?.let { decompileLiteral(it) }
    val variables = NodeList<VariableDeclarator>(VariableDeclarator(type, name, initializer))

    return FieldDeclaration(modifiers, annotations, variables)
  }

  /**
   * Decompiles a ASM ParameterNode into a JavaParser Parameter.
   *
   * @param method the method that `parameter` belongs to.
   * @param paramCount number of parameters in `method`.
   * @param parameter the target parameter to decompile.
   * @return a JavaParser Parameter representing `parameter`.
   */
  private fun decompileParameter(
    method: MethodNode,
    paramCount: Int,
    parameter: IndexedValue<Fourple<Type?, ParameterNode?, List<AnnotationNode>?, List<AnnotationNode>?>>,
  ): Parameter {
    val index = parameter.index
    val (type, node, a1, a2) = parameter.value
    val flags = if (node == null) listOf() else Flag.parameterFlags(node.access)
    val modifiers = Flag.toModifiers(flags)
    val annotations = decompileAnnotations(a1, a2, null, null)
    val isVarArgs = Flag.methodFlags(method.access).contains(Flag.ACC_VARARGS) && index == paramCount - 1
    val varArgsAnnotations = NodeList<AnnotationExpr>() // TODO?

    // TODO: index = 0 or valid index to the pool table, access flags can be synthetic/mandated
    val isStatic = Flag.methodFlags(method.access).contains(Flag.ACC_STATIC)

    // TODO: class files decompiled with the -parameters flags may contain original parameter name
    val parameterVarIndex = if (isStatic) index + 1 else index + 2
    val name = SimpleName(if (node == null) "parameterVar${parameterVarIndex}" else node.name)

    return Parameter(modifiers, annotations, type, isVarArgs, varArgsAnnotations, name)
  }

  /**
   * Checks if a method's needs the default keyword.
   *
   * An interface might contain abstract methods, default methods or static methods
   * See https://docs.oracle.com/javase%2Ftutorial%2F/java/IandI/interfaceDef.html.
   *
   * When our method is either abstract or static, we can directly use the modifier list from `methodNode.access`
   * (access flags from ASM). However, there is no access flag for `default` (despite being present in raw `.class`
   * bytecode files). Therefore, for default methods, default modifier has to be manually added as below.
   *
   * @param classNode the class which the method belongs to.
   * @param methodNode the target method.
   * @return a JavaParser BodyDeclaration representing `methodNode`.
   */
  private fun doesMethodRequireDefaultModifier(classNode: ClassNode, methodNode: MethodNode): Boolean =
    (classNode.access and Opcodes.ACC_INTERFACE) != 0 &&
      ((methodNode.access and Opcodes.ACC_STATIC) == 0) &&
      !DecompileMethodBody.isAbstract(methodNode)

  /**
   * Extracts a list of JavaParser Type from a list of ASM ParameterNode.
   *
   * @param descriptorTypes types provided from a method descriptor.
   * @param signatureTypes types provided from a method signature.
   * @param parameterNodes a list of ParameterNode.
   * @return a list of JavaParser Type representing `parameterNodes`.
   * @throws IllegalArgumentException is `descriptorTypes` is empty, or if parameter types cannot be constructed.
   */
  private fun buildParameterTypes(
    descriptorTypes: List<Type>,
    signatureTypes: List<Type>,
    parameterNodes: List<ParameterNode>,
  ): List<Type> {
    if (parameterNodes.isEmpty()) {
      return signatureTypes
    }

    require(descriptorTypes.isNotEmpty()) {
      "descriptorTypes cannot be empty when parameterNodes is not empty: $descriptorTypes, $signatureTypes, $parameterNodes"
    }

    return when {
      // TODO: Flag.checkParameter(access, Modifier)
      Flag
        .parameterFlags(
          parameterNodes.first().access,
        ).any(listOf(Flag.ACC_SYNTHETIC, Flag.ACC_MANDATED)::contains) -> {
        listOf(descriptorTypes.first()) + buildParameterTypes(
          descriptorTypes.tail(),
          signatureTypes,
          parameterNodes.tail(),
        )
      }

      signatureTypes.isNotEmpty() -> {
        listOf(signatureTypes.first()) + buildParameterTypes(
          descriptorTypes.tail(),
          signatureTypes.tail(),
          parameterNodes.tail(),
        )
      }

      else -> {
        throw IllegalArgumentException(
          "Failed to construct parameter types: $descriptorTypes, $signatureTypes, $parameterNodes",
        )
      }
    }
  }

  /**
   * Creates a MethodSignature representing `methodNode`.
   *
   * @param methodNode the target method.
   * @param descriptor the method's method descriptor.
   * @return a MethodSignature representing `methodNode`.
   */
  private fun buildMethodSignature(methodNode: MethodNode, descriptor: MethodDescriptor): MethodSignature =
    if (methodNode.signature == null) {
      MethodSignature(
        listOf(),
        descriptor.parameterTypes,
        descriptor.returnType,
        methodNode.exceptions.map(ClassName::classNameType),
      )
    } else {
      Signature.methodSignature(methodNode.signature)
    }

  /**
   * Creates a list of parameters for a method.
   *
   * @param classNode the class which the method belongs to.
   * @param methodNode the target method.
   * @param signature the method's signature
   * @return a list of method parameters.
   */
  private fun buildMethodParameters(
    methodNode: MethodNode,
    descriptor: MethodDescriptor,
    signature: MethodSignature,
  ): NodeList<Parameter> {
    val parameterNodes = methodNode.parameters ?: listOf()
    if (methodNode.parameters != null && signature.parameterTypes.size != methodNode.parameters.size) {
      // TODO: check if always in an enum
    }

    return NodeList(
      zipAll(
        buildParameterTypes(descriptor.parameterTypes, signature.parameterTypes, parameterNodes),
        parameterNodes,
        methodNode.visibleParameterAnnotations?.toList() ?: listOf(), // TODO: remove .toList()
        methodNode.invisibleParameterAnnotations?.toList() ?: listOf(),
      ).withIndex().map { decompileParameter(methodNode, signature.parameterTypes.size, it) },
    )
  }

  /**
   * Creates a method body for a given method.
   *
   * @param classNode the class which the method belongs to.
   * @param methodNode the target method to decompile.
   * @param modifiers the method's modifiers.
   * @param annotations the method's annotations
   * @param typeParameters the method's type parameters.
   * @param type the method's return type.
   * @param name the method's name.
   * @param parameters the method's parameters.
   * @param thrownExceptions the method's exceptions.
   * @param receiverParameter the methods's receiver parameter
   * @return the body of `methodNode`.
   */
  private fun buildMethodBodyDeclaration(
    classNode: ClassNode,
    methodNode: MethodNode,
    modifiers: NodeList<Modifier>,
    annotations: NodeList<AnnotationExpr>,
    typeParameters: NodeList<TypeParameter>,
    type: Type,
    name: SimpleName,
    parameters: NodeList<Parameter>,
    thrownExceptions: NodeList<ReferenceType>,
    receiverParameter: ReceiverParameter?,
  ): BodyDeclaration<out BodyDeclaration<*>> {
    // Provide a dummy method declaration, which is populated later
    val dummyDeclaration = MethodDeclaration(
      modifiers,
      annotations,
      typeParameters,
      type,
      name,
      parameters,
      thrownExceptions,
      BlockStmt(),
      receiverParameter,
    )

    val body: BlockStmt? = if (DecompileMethodBody.isAbstract(methodNode)) {
      null
    } else {
      DecompileMethodBody.decompileBody(classNode, methodNode, dummyDeclaration)
    }

    // TODO: temporary until we remove null (remove blank line above when we do)
    @Suppress("NULLABLE_PROPERTY_TYPE")
    return when (methodNode.name) {
      "<clinit>" -> {
        InitializerDeclaration(true, body)
      }

      "<init>" -> {
        // TODO: there was a TODO with no description; maybe it's about checking `name` against `constructorName`?
        val constructorName = SimpleName(ClassName.className(classNode.name).identifier)
        ConstructorDeclaration(
          modifiers,
          annotations,
          typeParameters,
          constructorName,
          parameters,
          thrownExceptions,
          body,
          receiverParameter,
        )
      }

      else -> {
        MethodDeclaration(
          modifiers,
          annotations,
          typeParameters,
          type,
          name,
          parameters,
          thrownExceptions,
          body,
          receiverParameter,
        )
      }
    }
  }

  /**
   * Decompiles a ASM MethodNode into a JavaParser BodyDeclaration.
   *
   * @param classNode the class which the method belongs to.
   * @param methodNode the target method to decompile.
   * @return a JavaParser BodyDeclaration representing `methodNode`.
   */
  public fun decompileMethod(classNode: ClassNode, methodNode: MethodNode): BodyDeclaration<out BodyDeclaration<*>> {
    // attr (ignore?)
    // instructions
    // tryCatchBlocks
    // localVariables
    // visibleLocalVariableAnnotations
    // invisibleLocalVariableAnnotations
    // TODO: JPModifier.Keyword.DEFAULT
    // TODO: catch exceptions and return a stub method

    val modifiers = Flag.toModifiers(Flag.methodFlags(methodNode.access))
    if (doesMethodRequireDefaultModifier(classNode, methodNode)) {
      modifiers.add(Modifier(Modifier.Keyword.DEFAULT))
    }

    val annotations: NodeList<AnnotationExpr> = decompileAnnotations(
      methodNode.visibleAnnotations,
      methodNode.invisibleAnnotations,
      methodNode.visibleTypeAnnotations,
      methodNode.invisibleTypeAnnotations,
    )

    val descriptor = Descriptor.methodDescriptor(methodNode.desc)
    val signature = buildMethodSignature(methodNode, descriptor)
    val typeParameters = NodeList(signature.typeParameters)
    val parameters = buildMethodParameters(methodNode, descriptor, signature)

    val type = signature.returnType
    val thrownExceptions = NodeList(signature.exceptionTypes)
    val name = SimpleName(methodNode.name)

    // TODO: See https://docs.oracle.com/javase/specs/jls/se26/html/jls-8.html#jls-ReceiverParameter
    val receiverParameter: ReceiverParameter? = null

    val bodyDeclaration = buildMethodBodyDeclaration(
      classNode,
      methodNode,
      modifiers,
      annotations,
      typeParameters,
      type,
      name,
      parameters,
      thrownExceptions,
      receiverParameter,
    )
    bodyDeclaration.setData(METHOD_NODE, methodNode)
    // TODO: Decompile.methods.add(bodyDeclaration to ((classNode, node)))
    return bodyDeclaration
  }

  /**
   * Builds the type declaration for a class.
   *
   * @param classNode the target class.
   * @param className the name of the target class.
   * @return a TypeDeclaration representing the class.
   */
  private fun buildClassTypeDeclaration(classNode: ClassNode, className: Name): TypeDeclaration<*> {
    // TODO: assert ACC_SUPER
    val modifiers = Flag.toModifiers(Flag.classFlags(classNode.access))
    val annotations: NodeList<AnnotationExpr> = decompileAnnotations(
      classNode.visibleAnnotations,
      classNode.invisibleAnnotations,
      classNode.visibleTypeAnnotations,
      classNode.invisibleTypeAnnotations,
    )

    val isInterface: Boolean = (classNode.access and Opcodes.ACC_INTERFACE) != 0
    val simpleName = SimpleName(className.identifier)
    val members: NodeList<BodyDeclaration<*>> = run {
      val list = NodeList<BodyDeclaration<*>>()
      list.addAll(NodeList(classNode.fields.map(::decompileField)))
      list.addAll(NodeList(classNode.methods.map { decompileMethod(classNode, it) }))
      list
    }

    if ((classNode.access and Opcodes.ACC_ANNOTATION) != 0) {
      return AnnotationDeclaration(
        modifiers,
        annotations,
        simpleName,
        members,
      )
    }

    val (
      typeParameters: NodeList<TypeParameter>,
      extendedTypes: NodeList<ClassOrInterfaceType>,
      implementedTypes: NodeList<ClassOrInterfaceType>,
      permittedTypes: NodeList<ClassOrInterfaceType>, // TODO: implement
    ) = if (classNode.signature == null) {
      Fourple(
        NodeList<TypeParameter>(),
        if (classNode.superName == null) NodeList() else NodeList(ClassName.classNameType(classNode.superName)),
        NodeList(classNode.interfaces.map { ClassName.classNameType(it) }),
        NodeList<ClassOrInterfaceType>(),
      )
    } else {
      val s = Signature.classSignature(classNode.signature)
      Fourple(
        NodeList(s.typeParameters),
        NodeList(s.superclass),
        NodeList(s.interfaces),
        NodeList<ClassOrInterfaceType>(),
      )
    }

    val classOrInterfaceDeclaration = ClassOrInterfaceDeclaration(
      modifiers,
      annotations,
      isInterface,
      simpleName,
      typeParameters,
      extendedTypes,
      implementedTypes,
      permittedTypes,
      members,
    )

    if (classOrInterfaceDeclaration.isInterface) {
      classOrInterfaceDeclaration.setExtendedTypes(classOrInterfaceDeclaration.implementedTypes)
      classOrInterfaceDeclaration.setImplementedTypes(NodeList())
    }

    classOrInterfaceDeclaration.setData(CLASS_NODE, classNode)
    return classOrInterfaceDeclaration
  }

  /**
   * Decompiles a ASM ClassNode into a JavaParser CompilationUnit.
   *
   * @param node the target node to decompile
   * @return a JavaParser CompilationUnit representing node.
   */
  public fun decompileClass(classNode: ClassNode): CompilationUnit {
    // outerClass
    // outerMethod
    // outerMethodDesc
    // attr (ignore?)
    // nestHostClass
    // nestMember

    val fullClassName: Name = ClassName.className(classNode.name)

    // TODO: NodeList<AnnotationExpr>()
    // val packageDeclaration = PackageDeclaration(NodeList<AnnotationExpr>(), fullClassName.qualifier.orElse(Name()))
    val packageDeclaration = fullClassName.qualifier.getOrNull()?.let {
      // TODO: handle annotations
      PackageDeclaration(NodeList<AnnotationExpr>(), it)
    }

    // TODO: implement import declarations
    val imports = NodeList<ImportDeclaration>()

    val types = NodeList<TypeDeclaration<*>>()
    types.add(buildClassTypeDeclaration(classNode, fullClassName))

    // TODO: ModuleExportNode
    // TODO: ModuleNode
    // TODO: ModuleOpenNode
    // TODO: ModuleProvideNode
    // TODO: ModuleRequireNode
    // TODO: implement module
    val module = null

    // TODO: maybe move CompilationUnit out of this function
    val compilationUnit = CompilationUnit(packageDeclaration, imports, types, module)

    val comment = JavadocComment(
      """
      Source File: ${classNode.sourceFile}
      Class-file Format Version: ${classNode.version}
      Source Debug Extension: ${classNode.sourceDebug} // See JSR-45 https://www.jcp.org/en/jsr/detail?id=045
      """.trimIndent(),
    )
    JavaParser.setComment(compilationUnit, comment)

    // TODO: Decompile.classes += compilationUnit to node
    // TODO: log.debug { "++++ decompile class ++++\n" + compilationUnit.toString() }

    return compilationUnit
  }
}
