package org.ucombinator.jade.decompile

import com.github.javaparser.StaticJavaParser.parseBlock

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class deleteEmptyStatementsTest {
  @Test
  fun removeEmptyStatementsTest() {
    val input = parseBlock("""
      |{
      |  int x = 1;
      |  ;
      |  ;
      |  return;
      |}
    """.trimMargin())
    val original = input.clone()

    val expected = parseBlock("{ int x = 1; return; }")
    val result = OptimizeMethodBody.deleteEmptyStatements(input)

    assertEquals(expected,result)
  }
}