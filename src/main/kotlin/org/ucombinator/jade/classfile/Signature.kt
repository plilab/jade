package org.ucombinator.jade.classfile

import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.type.ArrayType
import com.github.javaparser.ast.type.ClassOrInterfaceType
import com.github.javaparser.ast.type.PrimitiveType
import com.github.javaparser.ast.type.ReferenceType
import com.github.javaparser.ast.type.Type
import com.github.javaparser.ast.type.TypeParameter
import com.github.javaparser.ast.type.VoidType
import com.github.javaparser.ast.type.WildcardType
import org.objectweb.asm.Opcodes
import org.objectweb.asm.signature.SignatureReader
import org.objectweb.asm.signature.SignatureVisitor
import org.objectweb.asm.signature.SignatureWriter
import org.ucombinator.jade.util.Errors

// See:
// - https://docs.oracle.com/javase/specs/jvms/se22/html/jvms-4.html#jvms-4.7.9.1
// - https://gitlab.ow2.org/asm/asm/-/blob/master/asm/src/main/java/org/objectweb/asm/signature/SignatureReader.java
// - https://github.com/openjdk/jdk/blob/jdk-23%2B23/src/java.base/share/classes/sun/reflect/generics/parser/SignatureParser.java

// TODO: use Delegates.notNull (or custom). See https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.properties/-delegates/not-null.html

data class ClassSignature(
  val typeParameters: List<TypeParameter>,
  val superclass: ClassOrInterfaceType,
  val interfaces: List<ClassOrInterfaceType>
)

data class MethodSignature(
  val typeParameters: List<TypeParameter>,
  val parameterTypes: List<Type>,
  val returnType: Type,
  val exceptionTypes: List<ReferenceType>
)

object Signature {
  // TODO: document: Check that SignatureReader parses to the end of the string without error
  private fun checkSignature(string: String, accept: (SignatureReader, SignatureWriter) -> Unit) {
    val signatureWriter = SignatureWriter()
    try {
      accept(SignatureReader(string), signatureWriter)
    } catch (e: StringIndexOutOfBoundsException) {
      throw IllegalArgumentException(e)
    }
    val result = signatureWriter.toString()
    require(string.startsWith(result)) { """Signature "${string}" reconstituted to "${result}"""" }
    require(string == result) { """Unexpected "${string.removePrefix(result)}" at end of signature "${string}".""" }
  }

  fun typeSignature(string: String): Type {
    checkSignature(string, SignatureReader::acceptType)
    var type = null as Type?
    SignatureReader(string).acceptType(DelegatingSignatureVisitor(TypeSignatureVisitor { type = it; null }))
    return requireNotNull(type) { "no type for signature \"$string\"" }
  }

  fun classSignature(string: String): ClassSignature {
    checkSignature(string, SignatureReader::accept)
    val visitor = ClassSignatureVisitor()
    SignatureReader(string).accept(DelegatingSignatureVisitor(visitor))
    return ClassSignature(
      visitor.typeParameters,
      requireNotNull(visitor.superclass) { "no superclass for signature \"$string\"" },
      visitor.interfaces
    )
  }

  // TODO: rename arg to signature
  fun methodSignature(string: String): MethodSignature {
    checkSignature(string, SignatureReader::accept)
    val visitor = MethodSignatureVisitor()
    SignatureReader(string).accept(DelegatingSignatureVisitor(visitor))
    return MethodSignature(
      visitor.typeParameters,
      visitor.parameterTypes,
      requireNotNull(visitor.returnType) { "no return type for signature \"$string\"" },
      visitor.exceptionTypes
    )
  }
}

// TODO: ktlint: allow /////////////////

// /////////////////////////////////////
// Signature Visitors

// TODO: wrap in checked visitor and catch exceptions to detect malformed signatures (and add these to tests)
// TODO: document: we use this so we can dynamically change what visitor is running
@Suppress("ktlint:standard:blank-line-before-declaration", "ktlint:standard:statement-wrapping", "MaxLineLength")
data class DelegatingSignatureVisitor(var delegate: DelegateSignatureVisitor?) : SignatureVisitor(Opcodes.ASM9) {
  override fun visitFormalTypeParameter(name: String) { delegate = delegate!!.visitFormalTypeParameter(name) }
  override fun visitClassBound(): SignatureVisitor { delegate = delegate!!.visitClassBound(); return this }
  override fun visitInterfaceBound(): SignatureVisitor { delegate = delegate!!.visitInterfaceBound(); return this }
  override fun visitSuperclass(): SignatureVisitor { delegate = delegate!!.visitSuperclass(); return this }
  override fun visitInterface(): SignatureVisitor { delegate = delegate!!.visitInterface(); return this }
  override fun visitParameterType(): SignatureVisitor { delegate = delegate!!.visitParameterType(); return this }
  override fun visitReturnType(): SignatureVisitor { delegate = delegate!!.visitReturnType(); return this }
  override fun visitExceptionType(): SignatureVisitor { delegate = delegate!!.visitExceptionType(); return this }
  override fun visitBaseType(descriptor: Char) { delegate = delegate!!.visitBaseType(descriptor) }
  override fun visitTypeVariable(name: String) { delegate = delegate!!.visitTypeVariable(name) }
  override fun visitArrayType(): SignatureVisitor { delegate = delegate!!.visitArrayType(); return this }
  override fun visitClassType(name: String) { delegate = delegate!!.visitClassType(name) }
  override fun visitInnerClassType(name: String) { delegate = delegate!!.visitInnerClassType(name) }
  override fun visitTypeArgument() { delegate = delegate!!.visitTypeArgument() }
  override fun visitTypeArgument(wildcard: Char): SignatureVisitor { delegate = delegate!!.visitTypeArgument(wildcard); return this }
  override fun visitEnd() { delegate = delegate!!.visitEnd() }
}

// /////////////////////////////////////
// Delegate visitors

@Suppress("ThrowingExceptionsWithoutMessageOrCause", "ktlint:standard:blank-line-before-declaration")
open class DelegateSignatureVisitor {
  open fun visitFormalTypeParameter(name: String): DelegateSignatureVisitor? = throw IllegalArgumentException()
  open fun visitClassBound(): DelegateSignatureVisitor? = throw IllegalArgumentException()
  open fun visitInterfaceBound(): DelegateSignatureVisitor? = throw IllegalArgumentException()
  open fun visitSuperclass(): DelegateSignatureVisitor? = throw IllegalArgumentException()
  open fun visitInterface(): DelegateSignatureVisitor? = throw IllegalArgumentException()
  open fun visitParameterType(): DelegateSignatureVisitor? = throw IllegalArgumentException()
  open fun visitReturnType(): DelegateSignatureVisitor? = throw IllegalArgumentException()
  open fun visitExceptionType(): DelegateSignatureVisitor? = throw IllegalArgumentException()
  open fun visitBaseType(descriptor: Char): DelegateSignatureVisitor? = throw IllegalArgumentException()
  open fun visitTypeVariable(name: String): DelegateSignatureVisitor? = throw IllegalArgumentException()
  open fun visitArrayType(): DelegateSignatureVisitor? = throw IllegalArgumentException()
  open fun visitClassType(name: String): DelegateSignatureVisitor? = throw IllegalArgumentException()
  open fun visitInnerClassType(name: String): DelegateSignatureVisitor? = throw IllegalArgumentException()
  open fun visitTypeArgument(): DelegateSignatureVisitor? = throw IllegalArgumentException()
  open fun visitTypeArgument(wildcard: Char): DelegateSignatureVisitor? = throw IllegalArgumentException()
  open fun visitEnd(): DelegateSignatureVisitor? = throw IllegalArgumentException()
}

typealias TypeReceiver = (Type) -> DelegateSignatureVisitor?
@Suppress("MaxLineLength", "ktlint:standard:function-signature")
class TypeSignatureVisitor(val receiver: TypeReceiver) : DelegateSignatureVisitor() {
  override fun visitBaseType(descriptor: Char): DelegateSignatureVisitor? = receiver(descriptorToType(descriptor))
  override fun visitTypeVariable(name: String): DelegateSignatureVisitor? = receiver(TypeParameter(ClassName.identifier(name)))
  override fun visitArrayType(): DelegateSignatureVisitor? = TypeSignatureVisitor { receiver(ArrayType(it)) }
  override fun visitClassType(name: String): DelegateSignatureVisitor? = ClassTypeVisitor(receiver, name)
}

abstract class FormalTypeParameterVisitor : DelegateSignatureVisitor() {
  val typeParameters = mutableListOf<TypeParameter>()

  override fun visitFormalTypeParameter(name: String): DelegateSignatureVisitor? =
    this.apply { typeParameters.add(TypeParameter(ClassName.identifier(name))) }
  override fun visitClassBound(): DelegateSignatureVisitor? =
    TypeSignatureVisitor { this.apply { typeParameters.last().getTypeBound().add(toTypeParameter(it)) } }
  override fun visitInterfaceBound(): DelegateSignatureVisitor? =
    TypeSignatureVisitor { this.apply { typeParameters.last().getTypeBound().add(it.cast<ClassOrInterfaceType>("non-interface in interface bound")) } }
}

// TODO: wrap in checked visitor and catch exceptions to detect malformed signatures (and add these to tests)
class ClassSignatureVisitor : FormalTypeParameterVisitor() {
  var superclass = null as ClassOrInterfaceType?
  val interfaces = mutableListOf<ClassOrInterfaceType>()

  override fun visitSuperclass(): DelegateSignatureVisitor? =
    TypeSignatureVisitor { this.apply { superclass = it.cast<ClassOrInterfaceType>("non-class in superclass") } }
  override fun visitInterface(): DelegateSignatureVisitor? =
    TypeSignatureVisitor { this.apply { interfaces.add(it.cast<ClassOrInterfaceType>("non-class in interface")) } }
}

@Suppress("ktlint:standard:function-signature")
class MethodSignatureVisitor : FormalTypeParameterVisitor() {
  val parameterTypes = mutableListOf<Type>()
  var returnType = null as Type?
  val exceptionTypes = mutableListOf<ReferenceType>()

  override fun visitParameterType(): DelegateSignatureVisitor? =
    TypeSignatureVisitor { this.apply { require(it !is VoidType) { "void in parameter type" }; parameterTypes.add(it) } }
  override fun visitReturnType(): DelegateSignatureVisitor? =
    TypeSignatureVisitor { this.apply { returnType = it } }
  override fun visitExceptionType(): DelegateSignatureVisitor? =
    TypeSignatureVisitor { this.apply { require(it !is ArrayType) { "array in exception type" }; exceptionTypes.add(it.cast<ReferenceType>("non-reference type in exception type")) } }
}

// TODO: TypeReceiver -> ClassOrInterfaceTypeReceiver??
@Suppress("ktlint:standard:function-signature")
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

private inline fun <reified T> Any.cast(message: String): T = requireNotNull(this as? T) { message }

private fun descriptorToType(descriptor: Char): Type =
  when (descriptor) {
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

private fun toTypeParameter(type: Type): ClassOrInterfaceType =
  when (type) {
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

private fun toTypeArgument(wildcard: Char, type: Type): Type =
  when (wildcard) {
    // TODO: remove need for casts
    SignatureVisitor.EXTENDS -> WildcardType(type.cast<ReferenceType>("non-reference type in type parameter bound"))
    SignatureVisitor.SUPER -> WildcardType(null, type.cast<ReferenceType>("non-reference type in type parameter bound"), NodeList())
    SignatureVisitor.INSTANCEOF -> type // TODO: this may be wrong
    else -> Errors.unmatchedValue(wildcard)
  }
