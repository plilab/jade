package org.ucombinator.jade.classfile

import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.expr.SimpleName
import com.github.javaparser.ast.type.*
import org.antlr.v4.runtime.CommonToken
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.ListTokenSource
import org.ucombinator.jade.classfile.SignatureParser.* // ktlint-disable no-unused-imports no-wildcard-imports
import org.ucombinator.jade.util.Errors
import org.ucombinator.jade.util.Tuples.Fourple

object Signature {
  fun typeSignature(string: String): Type =
    convert(parser(string).javaTypeSignature())

  fun classSignature(string: String): Triple<List<TypeParameter>, ClassOrInterfaceType, List<ClassOrInterfaceType>> =
    convert(parser(string).classSignature())

  fun methodSignature(string: String): Fourple<List<TypeParameter>, List<Type>, Type, List<ReferenceType>> =
    convert(parser(string).methodSignature())

  private fun parser(string: String): SignatureParser =
    SignatureParser(CommonTokenStream(ListTokenSource(string.map { CommonToken(it.code, it.toString()) })))

  // TODO: ktlint: allow /////////////////
  // /////////////////////////////////////////////////////////////
  // TODO

  fun convert(tree: BaseTypeContext): Type =
    when (tree) {
      is ByteContext -> PrimitiveType.byteType()
      is CharContext -> PrimitiveType.charType()
      is DoubleContext -> PrimitiveType.doubleType()
      is FloatContext -> PrimitiveType.floatType()
      is IntContext -> PrimitiveType.intType()
      is LongContext -> PrimitiveType.longType()
      is ShortContext -> PrimitiveType.shortType()
      is BooleanContext -> PrimitiveType.booleanType()
      else -> TODO("impossible case in Signature.convert: $tree")
    }

  fun convert(@Suppress("UNUSED_PARAMETER") tree: VoidDescriptorContext): VoidType =
    VoidType()

  // /////////////////////////////////////////////////////////////
  // Java type signature

  fun convert(tree: JavaTypeSignatureContext): Type =
    when (tree) {
      is JavaTypeReferenceContext -> convert(tree.referenceTypeSignature())
      is JavaTypeBaseContext -> convert(tree.baseType())
      else -> TODO("impossible case in Signature.convert: $tree")
    }

  // /////////////////////////////////////////////////////////////
  // Reference type signature

  fun convert(tree: ReferenceTypeSignatureContext): ReferenceType =
    when (tree) {
      is ReferenceTypeClassContext -> convert(tree.classTypeSignature())
      is ReferenceTypeVariableContext -> convert(tree.typeVariableSignature())
      is ReferenceTypeArrayContext -> convert(tree.arrayTypeSignature())
      else -> TODO("impossible case in Signature.convert: $tree")
    }

  fun convert(tree: ClassTypeSignatureContext): ClassOrInterfaceType =
    convertSuffix(
      tree.classTypeSignatureSuffix(),
      convert(
        tree.simpleClassTypeSignature(),
        convert(
          tree.packageSpecifier()
        )
      )
    )

  fun convert(tree: PackageSpecifierContext?): ClassOrInterfaceType? =
    when (tree) {
      null -> null
      else -> convert(listOf(tree), null)
    }

  fun convert(tree: List<PackageSpecifierContext>, scope: ClassOrInterfaceType?): ClassOrInterfaceType? =
    when {
      tree.isEmpty() -> scope
      else ->
        convert(
          // TODO: 'plus' causes quadratic behavior
          tree.first().packageSpecifier().plus(tree.subList(1, tree.size)),
          ClassOrInterfaceType(scope, tree.first().identifier().text)
        )
    }

  fun convert(tree: SimpleClassTypeSignatureContext, scope: ClassOrInterfaceType?): ClassOrInterfaceType =
    ClassOrInterfaceType(
      scope,
      SimpleName(tree.identifier().text),
      convert(tree.typeArguments())
    )

  fun convert(tree: TypeArgumentsContext?): NodeList<Type>? =
    when (tree) {
      null -> null
      else -> NodeList(tree.typeArgument().map(::convert))
    }

  fun convert(tree: TypeArgumentContext): Type =
    when (tree) {
      is TypeArgumentNonStarContext -> convert(tree.wildcardIndicator(), convert(tree.referenceTypeSignature()))
      is TypeArgumentStarContext -> WildcardType()
      else -> TODO("impossible case in Signature.convert: $tree")
    }

  fun convert(tree: WildcardIndicatorContext?, ref: ReferenceType): Type =
    when (tree) {
      null -> ref
      is WildcardPlusContext -> WildcardType(ref)
      is WildcardMinusContext -> WildcardType(null, ref, NodeList())
      else -> TODO("impossible case in Signature.convert: $tree")
    }

  // NOTE: Renamed to deconflict with:
  //   convert(tree: List<PackageSpecifierContext>, scope: ClassOrInterfaceType?): ClassOrInterfaceType?
  fun convertSuffix(tree: List<ClassTypeSignatureSuffixContext>, scope: ClassOrInterfaceType): ClassOrInterfaceType =
    when {
      tree.isEmpty() -> scope
      else ->
        convertSuffix(
          tree.subList(1, tree.size),
          convert(
            tree.first().simpleClassTypeSignature(),
            scope
          )
        )
    }

  fun convert(tree: ClassTypeSignatureSuffixContext, scope: ClassOrInterfaceType): ClassOrInterfaceType =
    convert(tree.simpleClassTypeSignature(), scope)

  fun convert(tree: TypeVariableSignatureContext): TypeParameter =
    TypeParameter(tree.identifier().text)

  fun convert(tree: ArrayTypeSignatureContext): ArrayType =
    ArrayType(convert(tree.javaTypeSignature()))

  // /////////////////////////////////////////////////////////////
  // Class signature

  fun convert(tree: ClassSignatureContext):
    Triple<List<TypeParameter>, ClassOrInterfaceType, List<ClassOrInterfaceType>> =
    Triple(
      convert(tree.typeParameters()),
      convert(tree.superclassSignature()),
      tree.superinterfaceSignature().map(::convert)
    )

  fun convert(tree: TypeParametersContext?): List<TypeParameter> =
    when (tree) {
      null -> listOf()
      else -> tree.typeParameter().map(::convert)
    }

  private fun referenceTypeToClassOrInterfaceType(t: ReferenceType): ClassOrInterfaceType =
    when (t) {
      is ClassOrInterfaceType -> t
      is TypeParameter -> {
        assert(t.typeBound.isEmpty(), { "non-empty type bounds in $t" })
        // TODO: mark this as a type parameter
        ClassOrInterfaceType(null, t.name, null)
      }
      else -> Errors.unmatchedType(t)
    }

  fun convert(tree: TypeParameterContext): TypeParameter =
    TypeParameter(
      tree.identifier().text,
      NodeList(
        convert(tree.classBound())
          .plus(tree.interfaceBound().map(::convert))
          .map(::referenceTypeToClassOrInterfaceType)
      )
    )

  // NOTE: The returned list is either zero or one element long
  fun convert(tree: ClassBoundContext): List<ReferenceType> =
    when (val ref = tree.referenceTypeSignature()) {
      null -> listOf()
      else -> listOf(convert(ref))
    }

  fun convert(tree: InterfaceBoundContext): ReferenceType =
    convert(tree.referenceTypeSignature())

  fun convert(tree: SuperclassSignatureContext): ClassOrInterfaceType =
    convert(tree.classTypeSignature())

  fun convert(tree: SuperinterfaceSignatureContext): ClassOrInterfaceType =
    convert(tree.classTypeSignature())

  // /////////////////////////////////////////////////////////////
  // Method signature

  fun convert(tree: MethodSignatureContext): Fourple<List<TypeParameter>, List<Type>, Type, List<ReferenceType>> =
    Fourple(
      convert(tree.typeParameters()),
      tree.javaTypeSignature().map(::convert),
      convert(tree.result()),
      tree.throwsSignature().map(::convert)
    )

  fun convert(tree: ResultContext): Type =
    when (tree) {
      is ResultNonVoidContext -> convert(tree.javaTypeSignature())
      is ResultVoidContext -> convert(tree.voidDescriptor())
      else -> TODO("impossible case in Signature.convert: $tree")
    }

  fun convert(tree: ThrowsSignatureContext): ReferenceType =
    when (tree) {
      is ThrowsClassContext -> convert(tree.classTypeSignature())
      is ThrowsVariableContext -> convert(tree.typeVariableSignature())
      else -> TODO("impossible case in Signature.convert: $tree")
    }

  // /////////////////////////////////////////////////////////////
  // Field signature

  // NOTE: This method is unused
  fun convert(tree: FieldSignatureContext): ReferenceType =
    convert(tree.referenceTypeSignature())
}
