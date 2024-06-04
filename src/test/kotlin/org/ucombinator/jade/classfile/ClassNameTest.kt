package org.ucombinator.jade.classfile

import kotlin.test.Test
import kotlin.test.expect

@Suppress("BACKTICKS_PROHIBITED", "ktlint:standard:class-naming")
object ClassNameTest {
  @Test fun `className()`() {
    expect("abc.def.Ghi") { ClassName.className("abc/def/Ghi").toString() }
  }

  @Test fun `classNameExpr()`() {
    expect("abc.def.Ghi") { ClassName.classNameExpr("abc/def/Ghi").toString() }
  }

  @Test fun `classNameType(String)`() {
    expect("abc.def.Ghi") { ClassName.classNameType("abc/def/Ghi").toString() }
  }

  @Test fun `classNameType(Name)`() {
    expect("abc.def.Ghi") { ClassName.classNameType(ClassName.className("abc/def/Ghi")).toString() }
  }
}
