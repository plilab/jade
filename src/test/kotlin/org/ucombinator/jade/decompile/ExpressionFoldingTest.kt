package org.ucombinator.jade.decompile

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.expr.AssignExpr
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.stmt.BlockStmt
import org.jgrapht.graph.DirectedPseudograph
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.analysis.BasicValue
import org.ucombinator.jade.analysis.ExpressionGraph
import org.ucombinator.jade.analysis.Var
import org.ucombinator.jade.asm.Insn
import org.ucombinator.jade.javaparser.JavaParser
import java.io.ByteArrayOutputStream
import java.lang.reflect.InvocationTargetException
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import javax.tools.ToolProvider
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExpressionFoldingTest {
  @TempDir lateinit var directory: Path

  private fun fixture(source: String): BlockStmt {
    val body = StaticJavaParser.parseBlock(source)
    val method = MethodNode()
    val variables = body.findAll(VariableDeclarator::class.java).associate { declaration ->
      val label = LabelNode().also(method.instructions::add)
      val variable = Var.Instruction(BasicValue.INT_VALUE, Insn(method, label))
      declaration.setData(JavaParser.variable, variable)
      declaration.nameAsString to variable
    }
    body.findAll(NameExpr::class.java).forEach { name ->
      variables[name.nameAsString]?.let { name.setData(JavaParser.variable, it) }
    }
    return body
  }

  private data class Outcome(val value: Any?, val exception: String?, val trace: String)

  /** These hand-written AST fixtures use metadata-derived edges; CasesTest covers real bytecode SSA. */
  private fun fixtureGraph(body: BlockStmt): ExpressionGraph {
    val graph = DirectedPseudograph<ExpressionGraph.ExpressionNode, ExpressionGraph.ExpressionEdge>(
      ExpressionGraph.ExpressionEdge::class.java,
    )
    val nodes = body.findAll(VariableDeclarator::class.java).associate { declaration ->
      val variable = declaration.getData(JavaParser.variable)
      variable to ExpressionGraph.ExpressionNode(variable.name, ExpressionGraph.Kind.VAR, declaration.nameAsString)
        .also(graph::addVertex)
    }
    body.statements.forEachIndexed { index, statement ->
      val assignment = if (statement.isExpressionStmt) statement.asExpressionStmt().expression as? AssignExpr else null
      val target = assignment?.target?.takeIf { it.containsData(JavaParser.variable) }
        ?.getData(JavaParser.variable)?.let(nodes::get)
        ?: ExpressionGraph.ExpressionNode("statement$index", ExpressionGraph.Kind.VAR, "statement$index")
          .also(graph::addVertex)
      statement.findAll(NameExpr::class.java)
        .filter { it !== assignment?.target && it.containsData(JavaParser.variable) }
        .forEach { use ->
          val source = nodes.getValue(use.getData(JavaParser.variable))
          graph.addEdge(source, target, ExpressionGraph.ExpressionEdge(source, target))
        }
    }
    return ExpressionGraph(graph, emptySet(), nodes)
  }

  private fun execute(name: String, body: BlockStmt): Outcome {
    val source = directory.resolve("$name.java")
    Files.writeString(source, """
      public class $name {
        public static String trace = "";
        static int first() { trace += "1"; return 1; }
        static int second() { trace += "2"; return 2; }
        public static Object run() $body
      }
    """.trimIndent())
    val diagnostics = ByteArrayOutputStream()
    val exit = ToolProvider.getSystemJavaCompiler().run(
      null, diagnostics, diagnostics, "-proc:none", "-d", directory.toString(), source.toString(),
    )
    assertEquals(0, exit, "$diagnostics\n$body")
    return URLClassLoader(arrayOf(directory.toUri().toURL())).use { loader ->
      val type = loader.loadClass(name)
      try {
        Outcome(type.getMethod("run").invoke(null), null, type.getField("trace").get(null) as String)
      } catch (e: InvocationTargetException) {
        Outcome(null, e.cause!!.javaClass.name, type.getField("trace").get(null) as String)
      }
    }
  }

  @ParameterizedTest
  @ValueSource(strings = [
    "{ int a = 3; int b = 4; int t; int u; int v; t = a + 1; u = b + 2; v = t * u; return v; }",
    "{ long t; t = 2147483647; return t + 1; }",
    "{ int a = 16777217; float t; t = a; return (double) t; }",
    "{ char t; t = 65; return \"\" + t; }",
    "{ Integer boxed = null; int t; t = boxed; return t; }",
    "{ int t; int u; t = first(); u = second(); return u + t; }",
    "{ int t; t = first(); return t + t; }",
    "{ int[] a = {3}; int t; t = a[0]; a[0] = 9; return t * 10 + a[0]; }",
    "{ int[] a = {3}; int[] t; int i; t = a; i = 0; return t[i]; }",
    "{ int[] a = {3}; int[] t; int i; int v; t = a; i = 0; v = 9; t[i] = v; return a[0]; }",
    "{ int[] a = {3}; int t; t = first(); a[second()] = t; return 0; }",
    "{ int[] a = {3}; Integer boxed = null; int t; t = boxed; a[second()] = t; return 0; }",
    "{ int a = 1; int t; t = a; a = 2; return t; }",
    "{ int z = 0; int t; t = 1 / z; second(); return t; }",
    "{ Byte t; t = 1; return t; }",
  ])
  fun compiledResultPreservesValuesExceptionsAndCallOrder(source: String) {
    val expected = execute("Original", fixture(source))
    for (withGraph in listOf(false, true)) {
      val body = fixture(source)
      val graph = if (withGraph) fixtureGraph(body) else null
      ExpressionFolding.make(body, expressionGraph = graph)
      assertEquals(expected, execute(if (withGraph) "GraphFolded" else "Folded", body), "withGraph=$withGraph")
      val after = body.toString()
      ExpressionFolding.make(body, expressionGraph = graph)
      assertEquals(after, body.toString(), "Folding should reach a fixed point; withGraph=$withGraph")
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = [false, true])
  fun arithmeticChainsLoseTheirTemporariesAndKeepGrouping(withGraph: Boolean) {
    val body = fixture("{ int a = 3; int b = 4; int t; int u; int v; t = a + 1; u = b + 2; v = t * u; return v; }")
    ExpressionFolding.make(body, expressionGraph = if (withGraph) fixtureGraph(body) else null)
    assertEquals(listOf("a", "b"), body.findAll(VariableDeclarator::class.java).map { it.nameAsString })
    assertTrue(body.findAll(AssignExpr::class.java).isEmpty())
    assertEquals("return ((a + 1) * (b + 2));", body.statements.last().toString())
  }

  @ParameterizedTest
  @ValueSource(booleans = [false, true])
  fun reanalysisKeepsEarlierCallAheadOfAnInlinedLaterCall(withGraph: Boolean) {
    val body = fixture("{ int t; int u; t = first(); u = second(); return u + t; }")
    ExpressionFolding.make(body, expressionGraph = if (withGraph) fixtureGraph(body) else null)
    assertEquals(listOf("t"), body.findAll(VariableDeclarator::class.java).map { it.nameAsString })
    val assignment = body.findAll(AssignExpr::class.java).single()
    assertEquals("first", assignment.value.asMethodCallExpr().nameAsString)
    assertEquals(1, body.findAll(MethodCallExpr::class.java).count { it.nameAsString == "second" })
  }

  @ParameterizedTest
  @ValueSource(booleans = [false, true])
  fun phiInputsHandlersAndOpaqueUsesArePreserved(withGraph: Boolean) {
    val body = fixture("{ int t; t = 1; return t; }")
    val graph = if (withGraph) fixtureGraph(body) else null
    val variable = body.findAll(VariableDeclarator::class.java).single().getData(JavaParser.variable)
    val original = body.toString()
    ExpressionFolding.make(body, phiInputs = setOf(variable), expressionGraph = graph)
    assertEquals(original, body.toString())
    ExpressionFolding.make(body, hasExceptionHandlers = true, expressionGraph = graph)
    assertEquals(original, body.toString())
    val placeholder = JavaParser.noop("hidden use").also { it.setData(JavaParser.unsupported, setOf(variable)) }
    body.statements.add(0, placeholder)
    ExpressionFolding.make(body, expressionGraph = graph)
    assertFalse(body.findAll(AssignExpr::class.java).isEmpty())
  }
}
