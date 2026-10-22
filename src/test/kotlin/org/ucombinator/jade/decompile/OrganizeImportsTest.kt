package org.ucombinator.jade.decompile

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.FieldDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.expr.TypeExpr

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@Suppress("BACKTICKS_PROHIBITED", "ktlint:standard:class-naming", "ktlint:standard:no-unit-return")
object OrganizeImportsTest {
  @Test
  fun `organizeImports imports, sorts, deduplicates, and shortens external types`(): Unit {
    val compilationUnit = parse(
      """
      package example;

      class Sample {
        java.util.List<java.util.Date> dates;
        java.util.Date date;
      }
      """,
    )

    OrganizeImports.organizeImports(compilationUnit)

    assertEquals(listOf("java.util.Date", "java.util.List"), importNames(compilationUnit))
    assertEquals(listOf("List<Date>", "Date"), fieldTypeNames(compilationUnit))

    val organizedSource = compilationUnit.toString()
    OrganizeImports.organizeImports(compilationUnit)
    assertEquals(organizedSource, compilationUnit.toString())
  }

  @Test
  fun `organizeImports shortens implicitly available types without importing them`(): Unit {
    val compilationUnit = parse(
      """
      package example;

      class Sample {
        java.lang.String text;
        example.Other other;
      }
      """,
    )

    OrganizeImports.organizeImports(compilationUnit)

    assertEquals(emptyList(), importNames(compilationUnit))
    assertEquals(listOf("String", "Other"), fieldTypeNames(compilationUnit))
  }

  @Test
  fun `organizeImports shortens references to the top-level declared type`(): Unit {
    val compilationUnit = parse(
      """
      package example;

      class Sample {
        example.Sample copy() {
          return this;
        }
      }
      """,
    )

    OrganizeImports.organizeImports(compilationUnit)

    assertEquals(emptyList(), importNames(compilationUnit))
    val method = compilationUnit.findFirst(MethodDeclaration::class.java).orElseThrow()
    assertEquals("Sample", method.type.toString())
  }

  @Test
  fun `organizeImports processes declarations, annotations, signatures, and method bodies`(): Unit {
    val compilationUnit = parse(
      """
      package example;

      @external.Annotation
      class Sample extends external.Base implements external.Contract {
        external.Result method(external.Argument argument) throws external.Failure {
          external.Widget widget = new external.Widget();
          return null;
        }
      }
      """,
    )

    OrganizeImports.organizeImports(compilationUnit)

    assertEquals(
      listOf(
        "external.Annotation",
        "external.Argument",
        "external.Base",
        "external.Contract",
        "external.Failure",
        "external.Result",
        "external.Widget",
      ),
      importNames(compilationUnit),
    )
    val sourceWithoutImports = compilationUnit.clone().apply { imports.clear() }.toString()
    assertFalse("external." in sourceWithoutImports)
  }

  @Test
  fun `organizeImports leaves colliding external type names qualified`(): Unit {
    val compilationUnit = parse(
      """
      package example;

      class Sample {
        java.util.Date utilDate;
        java.sql.Date sqlDate;
      }
      """,
    )

    OrganizeImports.organizeImports(compilationUnit)

    assertEquals(emptyList(), importNames(compilationUnit))
    assertEquals(listOf("java.util.Date", "java.sql.Date"), fieldTypeNames(compilationUnit))
  }

  @Test
  fun `organizeImports leaves a type qualified when it collides with a declared type`(): Unit {
    val compilationUnit = parse(
      """
      package example;

      class Sample {
        class Date {}

        java.util.Date value;
      }
      """,
    )

    OrganizeImports.organizeImports(compilationUnit)

    assertEquals(emptyList(), importNames(compilationUnit))
    assertEquals(listOf("java.util.Date"), fieldTypeNames(compilationUnit))
  }

  @Test
  fun `organizeImports leaves a type qualified when it collides with a type parameter`(): Unit {
    val compilationUnit = parse(
      """
      package example;

      class Sample<Date> {
        java.util.Date value;
      }
      """,
    )

    OrganizeImports.organizeImports(compilationUnit)

    assertEquals(emptyList(), importNames(compilationUnit))
    assertEquals(listOf("java.util.Date"), fieldTypeNames(compilationUnit))
  }

  @Test
  fun `organizeImports recognizes TypeExpr static owners without changing value scopes`(): Unit {
    val compilationUnit = parse(
      """
      package example;

      class Sample {
        void method() {}
      }
      """,
    )
    val method = compilationUnit.findFirst(MethodDeclaration::class.java).orElseThrow()
    val typeScope = TypeExpr(StaticJavaParser.parseClassOrInterfaceType("java.util.Collections"))
    val staticCall = MethodCallExpr(typeScope, "emptyList")
    val valueCall = MethodCallExpr(NameExpr("service"), "run")
    method.body
      .orElseThrow()
      .addStatement(staticCall)
      .addStatement(valueCall)

    OrganizeImports.organizeImports(compilationUnit)

    assertEquals(listOf("java.util.Collections"), importNames(compilationUnit))
    assertEquals("Collections", staticCall.scope.orElseThrow().toString())
    assertEquals("service", valueCall.scope.orElseThrow().toString())
  }

  private fun parse(source: String): CompilationUnit = StaticJavaParser.parse(source.trimIndent())

  private fun importNames(compilationUnit: CompilationUnit): List<String> =
    compilationUnit.imports.map { it.nameAsString }

  private fun fieldTypeNames(compilationUnit: CompilationUnit): List<String> =
    compilationUnit.findAll(FieldDeclaration::class.java).map { it.commonType.toString() }
}
