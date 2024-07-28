package org.ucombinator.jade.classfile

import com.github.javaparser.ast.type.ArrayType
import com.github.javaparser.ast.type.PrimitiveType
import com.github.javaparser.ast.type.Type
import com.github.javaparser.ast.type.VoidType
import org.ucombinator.jade.util.Errors

import org.objectweb.asm.Type as AsmType

/** TODO:doc.
 *
 * @property parameterTypes TODO:doc
 * @property returnType TODO:doc
 */
data class MethodDescriptor(val parameterTypes: List<Type>, val returnType: Type)

// NOTE: There are lot of extra checks because AsmType.getType does not validate its input

/** TODO:doc. */
object Descriptor {
  /** TODO:doc.
   *
   * @param descriptor TODO:doc
   * @return TODO:doc
   */
  fun fieldDescriptor(descriptor: String): Type =
    try {
      val type = AsmType.getType(descriptor)
      require(type.sort != AsmType.METHOD)

      // Check that the entire descriptor is parsed
      require(descriptor == type.descriptor)

      javaParser(type)
    } catch (e: StringIndexOutOfBoundsException) {
      throw IllegalArgumentException(e)
    }

  /** TODO:doc.
   *
   * @param descriptor TODO:doc
   * @return TODO:doc
   */
  fun methodDescriptor(descriptor: String): MethodDescriptor =
    try {
      val type = AsmType.getType(descriptor)
      require(type.sort == AsmType.METHOD)

      val (returnType, argumentTypes) = Pair(type.returnType, type.argumentTypes)
      // Void is not allowed as an argument type
      argumentTypes.forEach { require(it.sort != AsmType.VOID) }
      // Check that the entire descriptor is parsed
      require(descriptor == @Suppress("detekt:SpreadOperator") AsmType.getMethodDescriptor(returnType, *argumentTypes))

      MethodDescriptor(argumentTypes.map(::javaParser), javaParser(returnType))
    } catch (e: StringIndexOutOfBoundsException) {
      throw IllegalArgumentException(e)
    }

  /** TODO:doc.
   *
   * @param type TODO:doc
   * @return TODO:doc
   */
  private fun javaParser(type: AsmType): Type =
    when (type.sort) {
      // Primitive types
      AsmType.BOOLEAN -> PrimitiveType.booleanType()
      AsmType.BYTE -> PrimitiveType.byteType()
      AsmType.CHAR -> PrimitiveType.charType()
      AsmType.DOUBLE -> PrimitiveType.doubleType()
      AsmType.FLOAT -> PrimitiveType.floatType()
      AsmType.INT -> PrimitiveType.intType()
      AsmType.LONG -> PrimitiveType.longType()
      AsmType.SHORT -> PrimitiveType.shortType()
      AsmType.VOID -> VoidType()

      // Non-primitive types
      AsmType.ARRAY -> {
        // Check that the entire descriptor is parsed
        require(type.descriptor.length == type.dimensions + type.elementType.descriptor.length)
        (1..type.dimensions).fold(javaParser(type.elementType)) { x, _ -> ArrayType(x) }
      }
      AsmType.OBJECT -> {
        // Use AsmType.INTERNAL to force ';' at the end
        val internalObject = AsmType.getObjectType(type.internalName)
        // Check that the entire descriptor is parsed
        require(type.descriptor == internalObject.descriptor)
        ClassName.classNameType(internalObject.internalName)
      }

      // Unsupported types
      AsmType.METHOD -> @Suppress("detekt:ThrowingExceptionsWithoutMessageOrCause") throw IllegalArgumentException()
      else -> Errors.unmatchedValue(type.sort)
    }
}
