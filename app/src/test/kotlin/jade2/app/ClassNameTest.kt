package org.ucombinator.jade.classfile

import com.github.javaparser.ast.`type`.PrimitiveType
import kotlin.test.*

object ClassNameTest {
  @Test fun `className()`() {
    expect("abc.def.Ghi") { ClassName.className("abc/def/Ghi").toString() }
  }
  object `classNameType()` {
    @Test fun `on a String`() {
      expect("abc.def.Ghi") { ClassName.classNameType("abc/def/Ghi").toString() }
    }
    @Test fun `on a Name`() {
      expect("abc.def.Ghi") { ClassName.classNameType(ClassName.className("abc/def/Ghi")).toString() }
    }
  }
}
