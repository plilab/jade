package org.ucombinator.jade.classfile

import com.github.javaparser.ast.`type`.PrimitiveType
import kotlin.test.*

object DescriptorTest {
  object `field descriptor` {
    @Test fun `base types`() {
      val types = mapOf(
        Pair(PrimitiveType.Primitive.BOOLEAN, "Z"),
        Pair(PrimitiveType.Primitive.CHAR, "C"),
        Pair(PrimitiveType.Primitive.BYTE, "B"),
        Pair(PrimitiveType.Primitive.SHORT, "S"),
        Pair(PrimitiveType.Primitive.INT, "I"),
        Pair(PrimitiveType.Primitive.LONG, "J"),
        Pair(PrimitiveType.Primitive.FLOAT, "F"),
        Pair(PrimitiveType.Primitive.DOUBLE, "D"),
      )
      for ((p, s) in types) {
        assertSame(p, Descriptor.fieldDescriptor(s).asPrimitiveType().getType())
      }
    }
    @Test fun `Object type`() {
      expect(
        "java.lang.Object",
        { Descriptor.fieldDescriptor("Ljava/lang/Object;").asClassOrInterfaceType().toString() })
    }
    @Test fun `array types`() {
      assertEquals("boolean[][]",
        Descriptor.fieldDescriptor("[[Z").asArrayType().toString())
      assertEquals("java.lang.Object[][]",
        Descriptor.fieldDescriptor("[[Ljava/lang/Object;").asArrayType().toString())
    }
  }

  object `method descriptor` {
    @Test fun `non-void`() {
      val (params, result) = Descriptor.methodDescriptor("(ZJ[[Ljava/lang/Object;)[[Ljava/lang/Object;")
      expect(3) { params.size }
      expect(PrimitiveType.Primitive.BOOLEAN) { params.get(0).asPrimitiveType().getType() }
      expect(PrimitiveType.Primitive.LONG) { params.get(1).asPrimitiveType().getType() }
      expect("java.lang.Object[][]") { params.get(2).asArrayType().toString() }
      expect("java.lang.Object[][]") { result.asArrayType().toString() }
    }
    @Test fun `void`() {
      val (params, result) = Descriptor.methodDescriptor("()V")
      assert(params.isEmpty())
      assert(result.isVoidType())
    }
  }
}
