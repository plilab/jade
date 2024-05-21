package org.ucombinator.jade.classfile

import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.expr.SimpleName
import com.github.javaparser.ast.type.*
import org.antlr.v4.runtime.CommonToken
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.ListTokenSource
import org.objectweb.asm.signature.SignatureVisitor
import org.objectweb.asm.Opcodes
import org.ucombinator.jade.util.Errors
import org.ucombinator.jade.util.Tuples.Fourple

// ClassSignature = ( visitFormalTypeParameter visitClassBound? visitInterfaceBound* )* (visitSuperclass visitInterface* )
// MethodSignature = ( visitFormalTypeParameter visitClassBound? visitInterfaceBound* )* (visitParameterType* visitReturnType visitExceptionType* )
// TypeSignature = visitBaseType | visitTypeVariable | visitArrayType | ( visitClassType visitTypeArgument* ( visitInnerClassType visitTypeArgument* )* visitEnd ) )

data class ClassSignature(val typeParameters: List<TypeParameter>, val superclass: ClassOrInterfaceType, val interfaces: List<ClassOrInterfaceType>)
data class MethodSignature(val typeParameters: List<TypeParameter>, val parameterTypes: List<Type>, val returnType: Type, val exceptionTypes: List<ReferenceType>)

sealed interface Cont {
  fun add(type: Type): Unit
}
// TODO: ClassSignatureCont MethodSignatureCont TypeSignatureCont
data class TopCont(var result: Type? = null) : Cont {
  override fun add(type: Type): Unit { result = type }
}
// TODO: ClassSignatureCont -> ClassSignatureVisitor
// TODO: Does ClassOrInterfaceType need null scope?
// data class ClassSignatureCont(var typeParameters: List<TypeParameter> = List(), var superclass: ClassOrInterfaceType = ClassOrInterfaceType(null, "<unknown>"), var interfaces: List<ClassOrInterfaceType> = List()) : Cont {
//   override fun add(type: Type): Unit {
//     println("ClassSignatureCont.add")
//     TODO() }
// }
// TODO: this isn't what UnknownType is meant for but we need something unique and non-null
data class ArrayCont(val cont: Cont, val result: ArrayType = ArrayType(UnknownType())) : Cont {
  override fun add(type: Type): Unit { cont.add(result.setComponentType(type)) }
}
// TODO: explain why result is var
data class ClassTypeCont(val cont: Cont, val name: String, var result: ClassOrInterfaceType = name.split("/").fold(null as ClassOrInterfaceType?) { scope, name -> ClassOrInterfaceType(scope, name) }!!) : Cont {
  // TODO: parse `name`
  override fun add(type: Type): Unit {
    println("add: $name ($type)")
    TODO()
  }
}
// TODO: omit cont?
data class TypeArgumentCont(val cont: Cont, val wildcard: Char, val typeArguments: NodeList<Type>) : Cont {
  override fun add(type: Type): Unit {
    typeArguments.add(
      when (wildcard) {
        // TODO: remove casts
        SignatureVisitor.EXTENDS -> WildcardType(type as ReferenceType)
        SignatureVisitor.SUPER -> WildcardType(null, type as ReferenceType, NodeList())
        SignatureVisitor.INSTANCEOF -> type // TODO: this is wrong
        else -> {
          println("wildcard: $wildcard")
          TODO()
        }
      }
    )
  } // NOTE: no call to cont.add
}


// TODO: open vs final
// TODO: DelegateSignatureVisitor vs DelegateSignatureVisitor?
open class DelegateSignatureVisitor(val level: Int) {
  fun indent() {
    for (i in 0 until level) {
      print("  ")
    }
  }
  fun trace(name: String): Unit {
    indent()
    println(name)
  }
  open fun visitFormalTypeParameter(name: String): DelegateSignatureVisitor? { trace("visitFormalTypeParameter"); TODO() }
  open fun visitClassBound(): DelegateSignatureVisitor? { trace("visitClassBound"); TODO() }
  open fun visitInterfaceBound(): DelegateSignatureVisitor? { trace("visitInterfaceBound"); TODO() }
  open fun visitSuperclass(): DelegateSignatureVisitor? { trace("visitSuperclass"); TODO() }
  open fun visitInterface(): DelegateSignatureVisitor? { trace("visitInterface"); TODO() }
  open fun visitParameterType(): DelegateSignatureVisitor? { trace("visitParameterType"); TODO() }
  open fun visitReturnType(): DelegateSignatureVisitor? { trace("visitReturnType"); TODO() }
  open fun visitExceptionType(): DelegateSignatureVisitor? { trace("visitExceptionType"); TODO() }
  open fun visitBaseType(descriptor: Char): DelegateSignatureVisitor? { trace("visitBaseType"); TODO() }
  open fun visitTypeVariable(name: String): DelegateSignatureVisitor? { trace("visitTypeVariable"); TODO() }
  open fun visitArrayType(): DelegateSignatureVisitor? { trace("visitArrayType"); TODO() }
  open fun visitClassType(name: String): DelegateSignatureVisitor? { trace("visitClassType"); TODO() }
  open fun visitInnerClassType(name: String): DelegateSignatureVisitor? { trace("visitInnerClassType"); TODO() }
  open fun visitTypeArgument(): DelegateSignatureVisitor? { trace("visitTypeArgument"); TODO() }
  open fun visitTypeArgument(wildcard: Char): DelegateSignatureVisitor? { trace("visitTypeArgument"); TODO() }
  open fun visitEnd(): DelegateSignatureVisitor? { trace("visitEnd"); TODO() }
}

fun descriptorToType(descriptor: Char): Type = when (descriptor) {
  'B' -> PrimitiveType.byteType()
  'C' -> PrimitiveType.charType()
  'D' -> PrimitiveType.doubleType()
  'F' -> PrimitiveType.floatType()
  'I' -> PrimitiveType.intType()
  'J' -> PrimitiveType.longType()
  'S' -> PrimitiveType.shortType()
  'Z' -> PrimitiveType.booleanType()
  'V' -> VoidType()
  else -> TODO("impossible case in JadeSignatureVisitor.visitBaseType: $descriptor")
}

fun toTypeArgument(): Type = WildcardType()
fun toTypeArgument(wildcard: Char, type: Type): Type =
  when (wildcard) {
    // TODO: remove casts
    SignatureVisitor.EXTENDS -> WildcardType(type as ReferenceType)
    SignatureVisitor.SUPER -> WildcardType(null, type as ReferenceType, NodeList())
    SignatureVisitor.INSTANCEOF -> type // TODO: this is wrong
    else -> {
      println("wildcard: $wildcard")
      TODO()
    }
  }

// TODO: level
// TODO: omit receiver?
class TypeSignatureVisitor(level: Int, val receiver: TypeReceiver) : DelegateSignatureVisitor(level) {
  override fun visitBaseType(descriptor: Char): DelegateSignatureVisitor? {
    trace("visitBaseType")
    return receiver.receive(descriptorToType(descriptor))
  }
  override fun visitTypeVariable(name: String): DelegateSignatureVisitor? {
    trace("visitTypeVariable")
    return receiver.receive(TypeParameter(name))
  }
  override fun visitArrayType(): DelegateSignatureVisitor? {
    trace("visitArrayType")
    return TypeSignatureVisitor(level + 1, ArrayTypeReceiver(receiver))
  }
  override fun visitClassType(name: String): DelegateSignatureVisitor? {
    trace("visitClassType")
    return ClassTypeVisitor(level + 1, receiver, name)
  }
}

// TODO: wrap in checked visitor and catch exceptions to detect malformed signatures (and add these to tests)
abstract class FormalTypeParameterVisitor(level: Int) : DelegateSignatureVisitor(level) {
  val typeParameters = mutableListOf<TypeParameter>()
  // TODO: factor into base class
  // Type parameters (both class signatures and method signatures)
  override fun visitFormalTypeParameter(name: String): DelegateSignatureVisitor? {
    indent()
    println("visitFormalTypeParameter: $name")
    typeParameters.add(TypeParameter(name))
    return this
  }
  override fun visitClassBound(): DelegateSignatureVisitor? {
    indent()
    println("visitClassBound!")
    return TypeSignatureVisitor(level + 1, object: TypeReceiver {
      override fun receive(type: Type): DelegateSignatureVisitor? {
        typeParameters.last().getTypeBound().add(
          when (type) {
            is ClassOrInterfaceType -> type
            is TypeParameter -> {
              assert(type.typeBound.isEmpty(), { "non-empty type bounds in $type" }) // TODO: why is this here?
              // TODO: mark this as a type parameter
              ClassOrInterfaceType(null, type.name, null)
            }
            else -> Errors.unmatchedType(type) // TODO
          }
        )// as? ClassOrInterfaceType ?: TODO("$type"))
        return this@FormalTypeParameterVisitor
      }
    })
  }
  override fun visitInterfaceBound(): DelegateSignatureVisitor? {
    indent()
    println("visitInterfaceBound!")
    return TypeSignatureVisitor(level + 1, object: TypeReceiver {
      override fun receive(type: Type): DelegateSignatureVisitor? {
        typeParameters.last().getTypeBound().add(type as? ClassOrInterfaceType ?: TODO())
        return this@FormalTypeParameterVisitor
      }
    })
  }
}

// TODO: wrap in checked visitor and catch exceptions to detect malformed signatures (and add these to tests)
class ClassSignatureVisitor(level: Int) : FormalTypeParameterVisitor(level) {
  var superclass: ClassOrInterfaceType? = null // TODO: "as" cast
  val interfaces = mutableListOf<ClassOrInterfaceType>()
  override fun visitSuperclass(): DelegateSignatureVisitor? {
    indent()
    println("visitSuperclass!")
    return TypeSignatureVisitor(level + 1, object: TypeReceiver {
      override fun receive(type: Type): DelegateSignatureVisitor? {
        superclass = type as? ClassOrInterfaceType ?: TODO()
        return this@ClassSignatureVisitor
      }
    })
  }
  override fun visitInterface(): DelegateSignatureVisitor? {
    indent()
    println("visitInterface!")
    // TOOD: factor into function
    return TypeSignatureVisitor(level + 1, object: TypeReceiver {
      override fun receive(type: Type): DelegateSignatureVisitor? {
        interfaces.add(type as? ClassOrInterfaceType ?: TODO())
        return this@ClassSignatureVisitor
      }
    })
  }
}

class MethodSignatureVisitor(level: Int) : FormalTypeParameterVisitor(level) {
  val parameterTypes = mutableListOf<Type>()
  var returnType: Type? = null // TODO: "as" cast
  val exceptionTypes = mutableListOf<ReferenceType>()
  override fun visitParameterType(): DelegateSignatureVisitor? {
    trace("visitParameterType")
    return TypeSignatureVisitor(level + 1, object: TypeReceiver {
      override fun receive(type: Type): DelegateSignatureVisitor? {
        parameterTypes.add(type)
        return this@MethodSignatureVisitor
      }
    })
  }
  override fun visitReturnType(): DelegateSignatureVisitor? {
    trace("visitReturnType")
    return TypeSignatureVisitor(level + 1, object: TypeReceiver {
      override fun receive(type: Type): DelegateSignatureVisitor? {
        returnType = type
        return this@MethodSignatureVisitor
      }
    })
  }
  override fun visitExceptionType(): DelegateSignatureVisitor? {
    trace("visitExceptionType")
    return TypeSignatureVisitor(level + 1, object: TypeReceiver {
      override fun receive(type: Type): DelegateSignatureVisitor? {
        exceptionTypes.add(type as? ReferenceType ?: TODO())
        return this@MethodSignatureVisitor
      }
    })
  }
}

// TODO: TypeReceiver -> ClassOrInterfaceTypeReceiver??
class ClassTypeVisitor(level: Int, val receiver: TypeReceiver, name: String) : DelegateSignatureVisitor(level) {
  var result = name.split("/").fold(null as ClassOrInterfaceType?, ::ClassOrInterfaceType)!!
  private fun typeArguments(): NodeList<Type> {
    val typeArguments = result.typeArguments.orElse(NodeList<Type>())
    result.setTypeArguments(typeArguments)
    return typeArguments
  }
  override fun visitInnerClassType(name: String): DelegateSignatureVisitor? {
    trace("visitInnerClassType")
    result = ClassOrInterfaceType(result, name)
    return this
  }
  override fun visitTypeArgument(): DelegateSignatureVisitor? {
    trace("visitTypeArgument")
    typeArguments().add(toTypeArgument())
    return this
  }
  override fun visitTypeArgument(wildcard: Char): DelegateSignatureVisitor? {
    trace("visitTypeArgument")
    return TypeSignatureVisitor(level + 1, TypeArgumentReceiver(this, wildcard, typeArguments()))
  }
  override fun visitEnd(): DelegateSignatureVisitor? {
    trace("visitEnd")
    return receiver.receive(result)
  }
}

interface TypeReceiver {
  fun receive(type: Type): DelegateSignatureVisitor?
}
data class TypeSignatureReceiver(var result: Type? = null) : TypeReceiver {
  override fun receive(type: Type): DelegateSignatureVisitor? {
    result = type
    return null
  }
}
// TODO: move to a local class
data class ArrayTypeReceiver(val parent: TypeReceiver) : TypeReceiver {
  override fun receive(type: Type): DelegateSignatureVisitor? = parent.receive(ArrayType(type))
}
// TODO: move to a local class
data class TypeArgumentReceiver(val parent: ClassTypeVisitor, val wildcard: Char, val typeArguments: NodeList<Type>) : TypeReceiver {
  override fun receive(type: Type): DelegateSignatureVisitor? {
    typeArguments.add(toTypeArgument(wildcard, type))
    return parent
  }
}

// TODO: we use this so we can dynamically change what visitor is running
data class DelegatingSignatureVisitor(var delegate: DelegateSignatureVisitor?) : SignatureVisitor(Opcodes.ASM9) {
  override public fun visitFormalTypeParameter(name: String): Unit { delegate = delegate!!.visitFormalTypeParameter(name) }
  override public fun visitClassBound(): SignatureVisitor { delegate = delegate!!.visitClassBound(); return this }
  override public fun visitInterfaceBound(): SignatureVisitor { delegate = delegate!!.visitInterfaceBound(); return this }
  override public fun visitSuperclass(): SignatureVisitor { delegate = delegate!!.visitSuperclass(); return this }
  override public fun visitInterface(): SignatureVisitor { delegate = delegate!!.visitInterface(); return this }
  override public fun visitParameterType(): SignatureVisitor { delegate = delegate!!.visitParameterType(); return this }
  override public fun visitReturnType(): SignatureVisitor { delegate = delegate!!.visitReturnType(); return this }
  override public fun visitExceptionType(): SignatureVisitor { delegate = delegate!!.visitExceptionType(); return this }
  override public fun visitBaseType(descriptor: Char): Unit { delegate = delegate!!.visitBaseType(descriptor) }
  override public fun visitTypeVariable(name: String): Unit { delegate = delegate!!.visitTypeVariable(name) }
  override public fun visitArrayType(): SignatureVisitor { delegate = delegate!!.visitArrayType(); return this }
  override public fun visitClassType(name: String): Unit { delegate = delegate!!.visitClassType(name) }
  override public fun visitInnerClassType(name: String): Unit { delegate = delegate!!.visitInnerClassType(name) }
  override public fun visitTypeArgument(): Unit { delegate = delegate!!.visitTypeArgument() }
  override public fun visitTypeArgument(wildcard: Char): SignatureVisitor { delegate = delegate!!.visitTypeArgument(wildcard); return this }
  override public fun visitEnd(): Unit { delegate = delegate!!.visitEnd() }
}


// TODO!!: delegate to subvisitor
data class JadeSignatureVisitor(val level: Int, var cont: Cont) : SignatureVisitor(Opcodes.ASM9) {
  fun indent() {
    for (i in 0 until level) {
      print("  ")
    }
  }
  // Type parameters (both class signatures and method signatures)
  override public fun visitFormalTypeParameter(name: String): Unit {
    indent()
    println("visitFormalTypeParameter: $name")
  }
  override public fun visitClassBound(): SignatureVisitor {
    indent()
    println("visitClassBound!")
    return JadeSignatureVisitor(level + 1, cont)
  }
  override public fun visitInterfaceBound(): SignatureVisitor {
    indent()
    println("visitInterfaceBound!")
    return JadeSignatureVisitor(level + 1, cont)
  }
  // Class Signatures
  override public fun visitSuperclass(): SignatureVisitor {
    indent()
    println("visitSuperclass!")
    return JadeSignatureVisitor(level + 1, cont)
  }
  override public fun visitInterface(): SignatureVisitor {
    indent()
    println("visitInterface!")
    return JadeSignatureVisitor(level + 1, cont)
  }
  // Method signatures
  override public fun visitParameterType(): SignatureVisitor {
    indent()
    println("visitParameterType!")
    return JadeSignatureVisitor(level + 1, cont)
  }
  override public fun visitReturnType(): SignatureVisitor {
    indent()
    println("visitReturnType!")
    return JadeSignatureVisitor(level + 1, cont)
  }
  override public fun visitExceptionType(): SignatureVisitor {
    indent()
    println("visitExceptionType!")
    return JadeSignatureVisitor(level + 1, cont)
  }
  ////////////////////////////
  override public fun visitBaseType(descriptor: Char): Unit {
    indent()
    println("visitBaseType: $descriptor")
    cont.add(
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
        else -> TODO("impossible case in JadeSignatureVisitor.visitBaseType: $descriptor")
      }
    )
  }
  override public fun visitTypeVariable(name: String): Unit {
    indent()
    println("visitTypeVariable: $name")
    cont.add(TypeParameter(name))
  }
  override public fun visitArrayType(): SignatureVisitor {
    indent()
    println("visitArrayType!")
    return JadeSignatureVisitor(level + 1, ArrayCont(cont))
  }
  // TODO: NOTE: visitClassType and visitEnd do not follow the nesting principle of
  // the rest so we have to make cont mutable
  override public fun visitClassType(name: String): Unit {
    indent()
    println("visitClassType: $name")
    cont = ClassTypeCont(cont, name)
  }
  override public fun visitInnerClassType(name: String): Unit {
    indent()
    println("visitInnerClassType: $name")
    // TODO: copy code from inner?
    // TODO: move?
    val c = (cont as? ClassTypeCont ?: TODO())
    c.result = ClassOrInterfaceType(c.result, name)
  }
  override public fun visitTypeArgument(): Unit {
    indent()
    println("visitTypeArgument")
    // TODO: move into TypeArgumentCont
    val c = (cont as? ClassTypeCont ?: TODO())
    val typeArguments = c.result.typeArguments.orElse(NodeList<Type>())
    c.result.setTypeArguments(typeArguments)
    typeArguments.add(WildcardType())
  }
  override public fun visitTypeArgument(wildcard: Char): SignatureVisitor {
    indent()
    println("visitTypeArgument!: $wildcard")
    // TODO: move into TypeArgumentCont
    val c = (cont as? ClassTypeCont ?: TODO())
    val typeArguments = c.result.typeArguments.orElse(NodeList<Type>())
    c.result.setTypeArguments(typeArguments)
    return JadeSignatureVisitor(level + 1, TypeArgumentCont(cont, wildcard, typeArguments))
  }
  override public fun visitEnd(): Unit {
    indent()
    println("visitEnd")
    // TODO: rewrite
    val c = (cont as? ClassTypeCont ?: TODO())
    cont = c.cont
    cont.add(c.result)
  }
}
