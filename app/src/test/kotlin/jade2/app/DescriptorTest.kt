package org.ucombinator.jade.classfile

import com.github.javaparser.ast.`type`.PrimitiveType
import kotlin.test.*

object DescriptorTest {
  object Foo {
  @Test fun testGetMessage() {
    assertEquals("Hello      World!", "Hello      World!")
  }
}

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
//     "Non-void" in {
//       val (params, result) = Descriptor.methodDescriptor("(ZJ[[Ljava/lang/Object;)[[Ljava/lang/Object;")
//       assertResult(3) { params.length }
//       assertResult(PrimitiveType.Primitive.BOOLEAN) { params(0).asPrimitiveType().getType }
//       assertResult(PrimitiveType.Primitive.LONG) { params(1).asPrimitiveType().getType }
//       assertResult("java.lang.Object[][]") { params(2).asArrayType().toString }
//       assertResult("java.lang.Object[][]") { result.asArrayType().toString }
//     }
//     "Void" in {
//       val (params, result) = Descriptor.methodDescriptor("()V")
//       assert(params.isEmpty)
//       assert(result.isVoidType)
//     }
//   }
  }
  object `class name` {
//     "is correct" in {
//       assertResult("abc.def.Ghi") { Descriptor.className("abc/def/Ghi").toString }
//     }
//   }
//   "classNameType" - {
//     "is correct on a String" in {
//       assertResult("abc.def.Ghi") { Descriptor.classNameType("abc/def/Ghi").toString }
//     }
//     "is correct on a Name" in {
//       assertResult("abc.def.Ghi") { Descriptor.classNameType(Descriptor.className("abc/def/Ghi")).toString }
//     }
//   }
  }

}
