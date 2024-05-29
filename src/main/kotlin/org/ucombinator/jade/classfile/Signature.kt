package org.ucombinator.jade.classfile

import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.expr.SimpleName
import com.github.javaparser.ast.type.*
import org.ucombinator.jade.util.Errors
import org.ucombinator.jade.util.Tuples.Fourple
import org.objectweb.asm.util.TraceSignatureVisitor
import org.objectweb.asm.signature.SignatureReader
import org.objectweb.asm.signature.SignatureWriter

// https://docs.oracle.com/javase/specs/jvms/se22/html/jvms-4.html#jvms-4.7.9.1
// https://gitlab.ow2.org/asm/asm/-/blob/master/asm/src/main/java/org/objectweb/asm/signature/SignatureReader.java
// https://github.com/openjdk/jdk/blob/jdk-23%2B23/src/java.base/share/classes/sun/reflect/generics/parser/SignatureParser.java

// TODO: use these instead of Triple and Fourple
data class ClassSignature(val typeParameters: List<TypeParameter>, val superclass: ClassOrInterfaceType, val interfaces: List<ClassOrInterfaceType>)
data class MethodSignature(val typeParameters: List<TypeParameter>, val parameterTypes: List<Type>, val returnType: Type, val exceptionTypes: List<ReferenceType>)

object Signature {
  // TODO: document: Check that SignatureReader parses to the end of the string without error
  private fun checkSignature(string: String, accept: (SignatureReader, SignatureWriter) -> Unit): Unit {
    val signatureWriter = SignatureWriter()
    try {
      accept(SignatureReader(string), signatureWriter)
    } catch (e: StringIndexOutOfBoundsException) { throw IllegalArgumentException(e) }
    val result = signatureWriter.toString()
    if (!string.startsWith(result)) {
      throw IllegalArgumentException("""Signature "${string}" reconstituted to "${result}"""")
    } else if (result != string) {
      throw IllegalArgumentException("""Unexpected characters "${string.removePrefix(result)}" at end of signature "${string}".""")
    }
  }

  fun typeSignature(string: String): Type {
    checkSignature(string, SignatureReader::acceptType)
    var type = null as Type?
    SignatureReader(string).acceptType(DelegatingSignatureVisitor(TypeSignatureVisitor({ type = it; null })))
    return type ?: throw IllegalArgumentException("""no type for signature "$string"""")
  }

  fun classSignature(string: String): Triple<List<TypeParameter>, ClassOrInterfaceType, List<ClassOrInterfaceType>> {
    checkSignature(string, SignatureReader::accept)
    val visitor = ClassSignatureVisitor()
    SignatureReader(string).accept(DelegatingSignatureVisitor(visitor))
    return Triple(visitor.typeParameters, visitor.superclass ?: throw IllegalArgumentException("""no superclass for signature "$string""""), visitor.interfaces)
  }

  // TODO: rename arg to signature
  fun methodSignature(string: String): Fourple<List<TypeParameter>, List<Type>, Type, List<ReferenceType>> {
    checkSignature(string, SignatureReader::accept)
    val visitor = MethodSignatureVisitor()
    SignatureReader(string).accept(DelegatingSignatureVisitor(visitor))
    return Fourple(visitor.typeParameters, visitor.parameterTypes, visitor.returnType ?: throw IllegalArgumentException("""no return type for signature "$string""""), visitor.exceptionTypes)
  }
}
