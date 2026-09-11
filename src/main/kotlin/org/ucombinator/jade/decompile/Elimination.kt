package org.ucombinator.jade.decompile

import com.github.javaparser.ast.Node
import com.github.javaparser.ast.expr.AssignExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.FieldAccessExpr
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.expr.VariableDeclarationExpr
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.EmptyStmt
import com.github.javaparser.ast.stmt.ExpressionStmt
import com.github.javaparser.ast.stmt.IfStmt
import com.github.javaparser.ast.stmt.LabeledStmt
import com.github.javaparser.ast.stmt.ReturnStmt
import com.github.javaparser.ast.stmt.Statement
import com.github.javaparser.ast.stmt.WhileStmt
import org.ucombinator.jade.util.Log

/** Removes assignments and declarations whose values are not subsequently used. */
object Elimination {
  private val log = Log {}

  /**
   * A statement-level control-flow graph.
   *
   * The collections are independent, read-only snapshots. Their statement nodes belong to the
   * source AST and should not be mutated while the graph is in use. [entry] is null when the source
   * block is empty.
   */
  data class DataflowGraph(
    val entry: Statement?,
    val predecessors: Map<Statement, Set<Statement>>,
    val successors: Map<Statement, Set<Statement>>,
    val nodes: Set<Statement>,
  )

  /** Liveness at the entry and exit of every statement in a [DataflowGraph]. */
  data class LivenessResult(
    val liveIn: Map<Statement, Set<String>>,
    val liveOut: Map<Statement, Set<String>>,
  )

  /** Builds a graph, computes liveness, and removes dead stores from a clone of [block]. */
  fun make(block: BlockStmt): BlockStmt {
    val graph = buildGraph(block)
    val liveness = analyzeLiveness(graph)
    return prune(block, liveness)
  }

  /** Builds an independent control-flow graph for [block]. */
  fun buildGraph(block: BlockStmt): DataflowGraph = GraphBuilder().build(block)

  /** Computes liveness to a fixed point without mutating [graph]. */
  fun analyzeLiveness(graph: DataflowGraph): LivenessResult {
    val liveIn = graph.nodes.associateWith { emptySet<String>() }.toMutableMap()
    val liveOut = graph.nodes.associateWith { emptySet<String>() }.toMutableMap()
    val workList = ArrayDeque<Statement>()
    val queued = mutableSetOf<Statement>()

    graph.nodes.forEach {
      workList.add(it)
      queued.add(it)
    }

    while (workList.isNotEmpty()) {
      val current = workList.removeLast()
      queued.remove(current)
      log.debug { "analyzeLiveness: processing ${current}" }

      val outgoing = graph.successors
        .getValue(current)
        .flatMapTo(mutableSetOf()) { liveIn.getValue(it) }
      liveOut[current] = outgoing

      val incoming = computeLiveIn(current, outgoing)
      if (incoming != liveIn[current]) {
        liveIn[current] = incoming
        graph.predecessors.getValue(current).forEach {
          if (queued.add(it)) {
            workList.add(it)
          }
        }
      }
    }

    return LivenessResult(
      liveIn = liveIn.mapValues { (_, variables) -> variables.toSet() },
      liveOut = liveOut.mapValues { (_, variables) -> variables.toSet() },
    )
  }

  /** Builds one control-flow graph using mutable state scoped to that build. */
  private class GraphBuilder {
    private val predecessors = mutableMapOf<Statement, MutableSet<Statement>>()
    private val successors = mutableMapOf<Statement, MutableSet<Statement>>()
    private val nodes = linkedSetOf<Statement>()

    fun build(block: BlockStmt): DataflowGraph {
      buildBlock(block, null)
      return DataflowGraph(
        entry = block.statements.firstOrNull(),
        predecessors = snapshot(predecessors),
        successors = snapshot(successors),
        nodes = nodes.toSet(),
      )
    }

    private fun snapshot(edges: Map<Statement, Set<Statement>>): Map<Statement, Set<Statement>> =
      nodes.associateWith { edges[it]?.toSet() ?: emptySet() }

    private fun addEdge(source: Statement, target: Statement) {
      successors.getOrPut(source) { mutableSetOf() }.add(target)
      predecessors.getOrPut(target) { mutableSetOf() }.add(source)
      nodes.add(source)
      nodes.add(target)
    }

    private fun buildLabeledStatement(current: LabeledStmt, next: Statement?) {
      val innerStatement = current.statement
      addEdge(current, innerStatement)
      log.debug { "type of labelled inner statement is ${innerStatement.javaClass}" }

      when (innerStatement) {
        is WhileStmt -> buildWhileStatement(innerStatement, next, current)
        is BlockStmt -> buildNestedBlock(innerStatement, next, current)
      }
    }

    private fun buildWhileStatement(current: WhileStmt, next: Statement?, loopTarget: Statement = current) {
      val body = current.body
      if (body is BlockStmt && body.statements.isNonEmpty) {
        addEdge(current, body.statements[0])
        buildBlock(body, loopTarget)
      }
      if (next != null) {
        addEdge(loopTarget, next)
      }
    }

    private fun buildNestedBlock(block: BlockStmt, next: Statement?, emptySource: Statement = block) {
      if (block.statements.isNonEmpty) {
        addEdge(block, block.statements[0])
        buildBlock(block, next)
      } else if (next != null) {
        addEdge(emptySource, next)
      }
    }

    private fun buildBlock(block: BlockStmt, exit: Statement?) {
      val statements = block.statements
      for ((current, next) in (statements + exit).zipWithNext()) {
        if (current == null) {
          continue
        }

        nodes.add(current)
        when (current) {
          is LabeledStmt -> buildLabeledStatement(current, next)
          is WhileStmt -> buildWhileStatement(current, next)
          is BlockStmt -> buildNestedBlock(current, next)
          else -> if (next != null) {
            addEdge(current, next)
          }
        }
      }
    }
  }

  private fun computeLiveIn(statement: Statement, liveOut: Set<String>): Set<String> {
    val liveVariables = liveOut.toMutableSet()
    when (statement) {
      is ExpressionStmt -> applyExpressionTransfer(statement, liveVariables)
      is ReturnStmt -> statement.expression.ifPresent {
        liveVariables.addAll(collectReferencedNames(it))
      }
      is WhileStmt -> liveVariables.addAll(collectReferencedNames(statement.condition))
      is IfStmt -> liveVariables.addAll(collectReferencedNames(statement.condition))
    }
    return liveVariables
  }

  private fun applyExpressionTransfer(statement: ExpressionStmt, liveVariables: MutableSet<String>) {
    when (val expression = statement.expression) {
      is AssignExpr -> {
        if (isAssignmentTargetLive(expression, liveVariables)) {
          liveVariables.addAll(collectReferencedNames(expression.value))
        }
      }
      is VariableDeclarationExpr -> {
        for (variable in expression.variables) {
          liveVariables.remove(variable.nameAsString)
          variable.initializer.ifPresent {
            liveVariables.addAll(collectReferencedNames(it))
          }
        }
      }
      else -> liveVariables.addAll(collectReferencedNames(expression))
    }
  }

  private fun isAssignmentTargetLive(expression: AssignExpr, liveVariables: Set<String>): Boolean {
    val target = expression.target
    return target is NameExpr && target.nameAsString in liveVariables
  }

  private fun collectReferencedNames(expression: Expression): Set<String> {
    val variables = mutableSetOf<String>()
    expression.walk {
      if (isVariableReference(it)) {
        variables.add((it as NameExpr).nameAsString)
      }
    }
    return variables
  }

  private fun isVariableReference(node: Node): Boolean =
    node is NameExpr &&
      !node.parentNode.map { it is FieldAccessExpr && it.name == node }.orElse(false)

  private fun shouldPruneAssignment(expression: AssignExpr, liveOut: Set<String>): Boolean {
    val target = expression.target
    return target is NameExpr && target.nameAsString !in liveOut
  }

  private fun shouldPruneDeclaration(expression: VariableDeclarationExpr, liveOut: Set<String>): Boolean =
    expression.variables.all { it.nameAsString !in liveOut }

  private fun findNodesToPrune(
    current: Node,
    cloned: Node,
    liveOutStates: Map<Statement, Set<String>>,
  ): List<Statement> {
    if (current !is Statement || cloned !is Statement) {
      return emptyList()
    }

    val liveOut = liveOutStates[current] ?: emptySet()
    val shouldPruneCurrent = (cloned as? ExpressionStmt)?.expression?.let {
      when (it) {
        is AssignExpr -> shouldPruneAssignment(it, liveOut)
        is VariableDeclarationExpr -> shouldPruneDeclaration(it, liveOut)
        else -> false
      }
    } ?: false

    val nodesToPrune = current.childNodes
      .zip(cloned.childNodes)
      .flatMapTo(mutableListOf()) { (currentChild, clonedChild) ->
        findNodesToPrune(currentChild, clonedChild, liveOutStates)
      }

    if (shouldPruneCurrent) {
      nodesToPrune.add(cloned)
    }
    return nodesToPrune
  }

  private fun prune(block: BlockStmt, liveness: LivenessResult): BlockStmt {
    val cloned = block.clone()
    findNodesToPrune(block, cloned, liveness.liveOut).forEach { it.replace(EmptyStmt()) }

    val statements = cloned.clone().statements
    val emptyStatements = mutableListOf<EmptyStmt>()
    statements.forEach { statement ->
      statement.walk { node ->
        if (node is EmptyStmt) {
          emptyStatements.add(node)
        }
      }
    }
    emptyStatements.forEach { it.remove() }
    return BlockStmt(statements)
  }
}
