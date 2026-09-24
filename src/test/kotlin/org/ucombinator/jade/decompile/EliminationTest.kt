package org.ucombinator.jade.decompile

import com.github.javaparser.StaticJavaParser.parseBlock

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EliminationTest {
  @Test
  fun removesDeadStatementsWithoutModifyingInput() {
    val input = parseBlock("""
      |{
      |  int unused = 1;
      |  unused = 2;
      |  return;
      |}
    """.trimMargin())
    val original = input.clone()

    assertEquals(parseBlock("{ return; }"), Elimination.make(input))
    assertEquals(original, input)
  }

  @Test
  fun preservesVariablesUsedByDeclarationInitializers() {
    val input = parseBlock("""
      |{
      |  int a = 1;
      |  int b = a;
      |  return b;
      |}
    """.trimMargin())

    assertEquals(input.clone(), Elimination.make(input))
  }

  @Test
  fun propagatesLivenessBackThroughLoopBodies() {
    val input = parseBlock("""
      |{
      |  int x = 0;
      |  while (x < 3) {
      |    x = x + 1;
      |  }
      |  return x;
      |}
    """.trimMargin())

    assertEquals(input.clone(), Elimination.make(input))
  }

  @Test
  fun preservesVariablesUsedInsideNestedBlocks() {
    val input = parseBlock("""
      |{
      |  int a = 1;
      |  {
      |    consume(a);
      |  }
      |  return;
      |}
    """.trimMargin())

    assertEquals(input.clone(), Elimination.make(input))
  }

  @Test
  fun preservesVariablesUsedInsideLabeledBlocks() {
    val input = parseBlock("""
      |{
      |  int a = 1;
      |  label: {
      |    consume(a);
      |  }
      |  return;
      |}
    """.trimMargin())

    assertEquals(input.clone(), Elimination.make(input))
  }

  @Test
  fun removesEveryOccurrenceOfIdenticalDeadStatements() {
    val input = parseBlock("""
      |{
      |  x = 1;
      |  x = 1;
      |  return;
      |}
    """.trimMargin())

    assertEquals(parseBlock("{ return; }"), Elimination.make(input))
  }

  @Test
  fun preservesLabeledLoopBackEdgesAndFallthrough() {
    val input = parseBlock("""
      |{
      |  int x = 1;
      |  loop: while (true) {
      |    break loop;
      |  }
      |  return x;
      |}
    """.trimMargin())
    val label = input.statements[1].asLabeledStmt()
    val loop = label.statement.asWhileStmt()
    val bodyEnd = loop.body.asBlockStmt().statements[0]
    val exit = input.statements[2]
    val graph = Elimination.buildGraph(input)

    assertEquals(input.clone(), Elimination.make(input))
    assertEquals(setOf(loop, exit), graph.successors.getValue(label))
    assertEquals(setOf(bodyEnd), graph.successors.getValue(loop))
    assertEquals(setOf(label), graph.successors.getValue(bodyEnd))
  }

  @Test
  fun exposesLivenessAsAnIndependentResult() {
    val input = parseBlock("""
      |{
      |  int x = 1;
      |  return x;
      |}
    """.trimMargin())
    val declaration = input.statements[0]
    val result = Elimination.analyzeLiveness(Elimination.buildGraph(input))

    assertEquals(setOf("x"), result.liveOut.getValue(declaration))
    assertEquals(emptySet(), result.liveIn.getValue(declaration))
  }

  @Test
  fun supportsEmptyBlocks() {
    val input = parseBlock("{}")
    val graph = Elimination.buildGraph(input)

    assertNull(graph.entry)
    assertTrue(graph.nodes.isEmpty())
    assertEquals(input, Elimination.make(input))
  }

  @Test
  fun removesDeadDeclarationAndDiscardableInitializerDependencies() {
    val input = parseBlock("""
      |{
      |  int source = 1;
      |  int unused = source + 1;
      |  return;
      |}
    """.trimMargin())

    assertEquals(parseBlock("{ return; }"), Elimination.make(input))
  }

  @Test
  fun preservesSideEffectingDeclarationInitializerAndDependencies() {
    val input = parseBlock("""
      |{
      |  int argument = 1;
      |  int unused = consume(argument);
      |  return;
      |}
    """.trimMargin())

    assertEquals(input.clone(), Elimination.make(input))
  }

  @Test
  fun removesDeadDeclarationWithoutInitializer() {
    val input = parseBlock("""
      |{
      |  int unused;
      |  return;
      |}
    """.trimMargin())

    assertEquals(parseBlock("{ return; }"), Elimination.make(input))
  }

  @Test
  fun preservesSideEffectingAssignmentAndTargetDeclaration() {
    val input = parseBlock("""
      |{
      |  int argument = 1;
      |  int target = 0;
      |  target = consume(argument);
      |  return;
      |}
    """.trimMargin())

    assertEquals(input.clone(), Elimination.make(input))
  }

  @Test
  fun removesDeadAssignmentAndDiscardableDependencies() {
    val input = parseBlock("""
      |{
      |  int source = 1;
      |  int target = 0;
      |  target = source + 1;
      |  return;
      |}
    """.trimMargin())

    assertEquals(parseBlock("{ return; }"), Elimination.make(input))
  }

  @Test
  fun preservesFieldWrites() {
    val input = parseBlock("""
      |{
      |  int value = 1;
      |  receiver.field = value;
      |  return;
      |}
    """.trimMargin())

    assertEquals(input.clone(), Elimination.make(input))
  }

  @Test
  fun preservesInitializersThatMayThrow() {
    val input = parseBlock("""
      |{
      |  int divisor = 0;
      |  int unused = 1 / divisor;
      |  return;
      |}
    """.trimMargin())

    assertEquals(input.clone(), Elimination.make(input))
  }
}
