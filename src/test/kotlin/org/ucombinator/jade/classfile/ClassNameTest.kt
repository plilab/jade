package org.ucombinator.jade.classfile

import com.github.javaparser.ast.expr.FieldAccessExpr
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.TypeExpr

import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.expect

@Suppress("BACKTICKS_PROHIBITED", "ktlint:standard:class-naming")
object ClassNameTest {
  @Test
  fun `className()`() {
    expect("abc.def.Ghi") { ClassName.className("abc/def/Ghi").toString() }
  }

  @Test
  fun `classNameExpr()`() {
    val expression = ClassName.classNameExpr("abc/def/Ghi")

    assertIs<TypeExpr>(expression)
    expect("abc.def.Ghi") { expression.toString() }
    expect("abc.def.Ghi.field") { FieldAccessExpr(expression.clone(), "field").toString() }
    expect("abc.def.Ghi.method()") { MethodCallExpr(expression.clone(), "method").toString() }
  }

  @Test
  fun `classNameType(String)`() {
    expect("abc.def.Ghi") { ClassName.classNameType("abc/def/Ghi").toString() }
  }

  @Test
  fun `classNameType(Name)`() {
    expect("abc.def.Ghi") { ClassName.classNameType(ClassName.className("abc/def/Ghi")).toString() }
  }
}
