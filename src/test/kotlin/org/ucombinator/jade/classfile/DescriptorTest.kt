package org.ucombinator.jade.classfile

import com.github.javaparser.ast.type.PrimitiveType
import kotlin.test.*

@Suppress("BACKTICKS_PROHIBITED")
object DescriptorTest {
  object `field descriptor` {
    @Test fun `base types`() {
      val types = mapOf(
        PrimitiveType.Primitive.BOOLEAN to "Z",
        PrimitiveType.Primitive.CHAR to "C",
        PrimitiveType.Primitive.BYTE to "B",
        PrimitiveType.Primitive.SHORT to "S",
        PrimitiveType.Primitive.INT to "I",
        PrimitiveType.Primitive.LONG to "J",
        PrimitiveType.Primitive.FLOAT to "F",
        PrimitiveType.Primitive.DOUBLE to "D",
      )
      for ((p, s) in types) {
        assertSame(p, Descriptor.fieldDescriptor(s).asPrimitiveType().type)
      }
    }

    @Test fun `Object type`() {
      expect(
        "java.lang.Object",
        { Descriptor.fieldDescriptor("Ljava/lang/Object;").asClassOrInterfaceType().toString() }
      )
    }

    @Test fun `array types`() {
      assertEquals(
        "boolean[][]",
        Descriptor.fieldDescriptor("[[Z").asArrayType().toString()
      )
      assertEquals(
        "java.lang.Object[][]",
        Descriptor.fieldDescriptor("[[Ljava/lang/Object;").asArrayType().toString()
      )
    }
  }
  // TODO: ktlint closing paren on same line

  object `method descriptor` {
    @Test fun `non-void`() {
      val (params, result) = Descriptor.methodDescriptor("(ZJ[[Ljava/lang/Object;)[[Ljava/lang/Object;")
      expect(3) { params.size }
      expect(PrimitiveType.Primitive.BOOLEAN) { params[0].asPrimitiveType().type }
      expect(PrimitiveType.Primitive.LONG) { params[1].asPrimitiveType().type }
      expect("java.lang.Object[][]") { params[2].asArrayType().toString() }
      expect("java.lang.Object[][]") { result.asArrayType().toString() }
    }

    @Test fun `void`() {
      val (params, result) = Descriptor.methodDescriptor("()V")
      assert(params.isEmpty())
      assert(result.isVoidType())
    }
  }
}
