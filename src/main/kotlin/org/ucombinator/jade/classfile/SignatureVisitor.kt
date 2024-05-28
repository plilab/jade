package org.ucombinator.jade.classfile

import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.type.*
import org.objectweb.asm.signature.SignatureVisitor
import org.objectweb.asm.Opcodes
import org.ucombinator.jade.util.Errors

// TODO: move code to Signature.kt

// /////////////////////////////////////
// Signature Visitors

// TODO: wrap in checked visitor and catch exceptions to detect malformed signatures (and add these to tests)
// TODO: document: we use this so we can dynamically change what visitor is running
data class DelegatingSignatureVisitor(var delegate: DelegateSignatureVisitor?) : SignatureVisitor(Opcodes.ASM9) {
  override fun visitFormalTypeParameter(name: String): Unit { delegate = delegate!!.visitFormalTypeParameter(name) }
  override fun visitClassBound(): SignatureVisitor { delegate = delegate!!.visitClassBound(); return this }
  override fun visitInterfaceBound(): SignatureVisitor { delegate = delegate!!.visitInterfaceBound(); return this }
  override fun visitSuperclass(): SignatureVisitor { delegate = delegate!!.visitSuperclass(); return this }
  override fun visitInterface(): SignatureVisitor { delegate = delegate!!.visitInterface(); return this }
  override fun visitParameterType(): SignatureVisitor { delegate = delegate!!.visitParameterType(); return this }
  override fun visitReturnType(): SignatureVisitor { delegate = delegate!!.visitReturnType(); return this }
  override fun visitExceptionType(): SignatureVisitor { delegate = delegate!!.visitExceptionType(); return this }
  override fun visitBaseType(descriptor: Char): Unit { delegate = delegate!!.visitBaseType(descriptor) }
  override fun visitTypeVariable(name: String): Unit { delegate = delegate!!.visitTypeVariable(name) }
  override fun visitArrayType(): SignatureVisitor { delegate = delegate!!.visitArrayType(); return this }
  override fun visitClassType(name: String): Unit { delegate = delegate!!.visitClassType(name) }
  override fun visitInnerClassType(name: String): Unit { delegate = delegate!!.visitInnerClassType(name) }
  override fun visitTypeArgument(): Unit { delegate = delegate!!.visitTypeArgument() }
  override fun visitTypeArgument(wildcard: Char): SignatureVisitor { delegate = delegate!!.visitTypeArgument(wildcard); return this }
  override fun visitEnd(): Unit { delegate = delegate!!.visitEnd() }
}

// /////////////////////////////////////
// Delegate visitors

open class DelegateSignatureVisitor {
  open fun visitFormalTypeParameter(name: String): DelegateSignatureVisitor? { TODO() }
  open fun visitClassBound(): DelegateSignatureVisitor? { TODO() }
  open fun visitInterfaceBound(): DelegateSignatureVisitor? { TODO() }
  open fun visitSuperclass(): DelegateSignatureVisitor? { TODO() }
  open fun visitInterface(): DelegateSignatureVisitor? { TODO() }
  open fun visitParameterType(): DelegateSignatureVisitor? { TODO() }
  open fun visitReturnType(): DelegateSignatureVisitor? { TODO() }
  open fun visitExceptionType(): DelegateSignatureVisitor? { TODO() }
  open fun visitBaseType(descriptor: Char): DelegateSignatureVisitor? { TODO() }
  open fun visitTypeVariable(name: String): DelegateSignatureVisitor? { TODO() }
  open fun visitArrayType(): DelegateSignatureVisitor? { TODO() }
  open fun visitClassType(name: String): DelegateSignatureVisitor? { TODO() }
  open fun visitInnerClassType(name: String): DelegateSignatureVisitor? { TODO() }
  open fun visitTypeArgument(): DelegateSignatureVisitor? { TODO() }
  open fun visitTypeArgument(wildcard: Char): DelegateSignatureVisitor? { TODO() }
  open fun visitEnd(): DelegateSignatureVisitor? { TODO() }
}

typealias TypeReceiver = (Type) -> DelegateSignatureVisitor?
class TypeSignatureVisitor(val receiver: TypeReceiver) : DelegateSignatureVisitor() {
  override fun visitBaseType(descriptor: Char): DelegateSignatureVisitor? = receiver(descriptorToType(descriptor))
  override fun visitTypeVariable(name: String): DelegateSignatureVisitor? = receiver(TypeParameter(ClassName.identifier(name)))
  override fun visitArrayType(): DelegateSignatureVisitor? = TypeSignatureVisitor({ receiver(ArrayType(it)) })
  override fun visitClassType(name: String): DelegateSignatureVisitor? = ClassTypeVisitor(receiver, name)
}

abstract class FormalTypeParameterVisitor : DelegateSignatureVisitor() {
  val typeParameters = mutableListOf<TypeParameter>()

  override fun visitFormalTypeParameter(name: String): DelegateSignatureVisitor? =
    this.apply { typeParameters.add(TypeParameter(ClassName.identifier(name))) }
  override fun visitClassBound(): DelegateSignatureVisitor? =
    TypeSignatureVisitor { this.apply { typeParameters.last().getTypeBound().add(toTypeParameter(it)) } }
  override fun visitInterfaceBound(): DelegateSignatureVisitor? =
    TypeSignatureVisitor { this.apply { typeParameters.last().getTypeBound().add(it as ClassOrInterfaceType) } }
}

// TODO: wrap in checked visitor and catch exceptions to detect malformed signatures (and add these to tests)
class ClassSignatureVisitor : FormalTypeParameterVisitor() {
  var superclass = null as ClassOrInterfaceType?
  val interfaces = mutableListOf<ClassOrInterfaceType>()

  override fun visitSuperclass(): DelegateSignatureVisitor? =
    TypeSignatureVisitor { this.apply { superclass = it as ClassOrInterfaceType } }
  override fun visitInterface(): DelegateSignatureVisitor? =
    TypeSignatureVisitor { this.apply { interfaces.add(it as ClassOrInterfaceType) } }
}

class MethodSignatureVisitor : FormalTypeParameterVisitor() {
  val parameterTypes = mutableListOf<Type>()
  var returnType = null as Type?
  val exceptionTypes = mutableListOf<ReferenceType>()

  override fun visitParameterType(): DelegateSignatureVisitor? =
    TypeSignatureVisitor { this.apply { if (it is VoidType) TODO(); parameterTypes.add(it) } }
  override fun visitReturnType(): DelegateSignatureVisitor? =
    TypeSignatureVisitor { this.apply { returnType = it } }
  override fun visitExceptionType(): DelegateSignatureVisitor? =
    TypeSignatureVisitor { this.apply { if (it is ArrayType) TODO(); exceptionTypes.add(it as ReferenceType) } }
}

// TODO: TypeReceiver -> ClassOrInterfaceTypeReceiver??
class ClassTypeVisitor(val receiver: TypeReceiver, name: String) : DelegateSignatureVisitor() {
  var result = ClassName.classNameType(name)

  override fun visitInnerClassType(name: String): DelegateSignatureVisitor? =
    this.apply { result = ClassOrInterfaceType(result, ClassName.identifier(name)) }
  override fun visitTypeArgument(): DelegateSignatureVisitor? =
    this.apply { typeArguments(result).add(toTypeArgument()) }
  override fun visitTypeArgument(wildcard: Char): DelegateSignatureVisitor? =
    TypeSignatureVisitor { this.apply { typeArguments(result).add(toTypeArgument(wildcard, it)) } }
  override fun visitEnd(): DelegateSignatureVisitor? =
    receiver(result)
}

// /////////////////////////////////////
// Helper functions

private fun descriptorToType(descriptor: Char): Type = when (descriptor) {
  'B' -> PrimitiveType.byteType()
  'C' -> PrimitiveType.charType()
  'D' -> PrimitiveType.doubleType()
  'F' -> PrimitiveType.floatType()
  'I' -> PrimitiveType.intType()
  'J' -> PrimitiveType.longType()
  'S' -> PrimitiveType.shortType()
  'Z' -> PrimitiveType.booleanType()
  'V' -> VoidType()
  else -> Errors.unmatchedValue(descriptor)
}

private fun toTypeParameter(type: Type): ClassOrInterfaceType = when (type) {
  is ClassOrInterfaceType -> type
  is TypeParameter -> ClassOrInterfaceType(null, type.name, null)
  else -> Errors.unmatchedType(type)
}

private fun typeArguments(type: ClassOrInterfaceType): NodeList<Type> {
  val typeArguments = type.typeArguments.orElse(NodeList<Type>())
  type.setTypeArguments(typeArguments)
  return typeArguments
}

private fun toTypeArgument(): Type = WildcardType()

private fun toTypeArgument(wildcard: Char, type: Type): Type = when (wildcard) {
  // TODO: remove need for casts
  SignatureVisitor.EXTENDS -> WildcardType(type as ReferenceType)
  SignatureVisitor.SUPER -> WildcardType(null, type as ReferenceType, NodeList())
  SignatureVisitor.INSTANCEOF -> type // TODO: this may be wrong
  else -> Errors.unmatchedValue(wildcard)
}
