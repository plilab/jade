package org.ucombinator.jade.decompile

import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.expr.SimpleName
import com.github.javaparser.ast.stmt.*
import org.ucombinator.jade.util.Errors
import org.ucombinator.jade.util.Log

object RemoveUnusedLabels {
  private val log = Log {}

  fun make(statement: Statement): Statement {
    val usedLabels = computeUsedLabels(statement)
    println("labels used: ${usedLabels}")
    return keepOnlyLabels(usedLabels, statement)
  }

   fun computeUsedLabels(node: Expression): Set<SimpleName> = when (node) {
     is AssignExpr -> computeUsedLabels(node.target) + computeUsedLabels(node.value)
     is NameExpr -> setOf(node.name)
     is VariableDeclarationExpr -> node.variables.map { it.name }.toSet()
     else -> emptySet()
   }
  fun computeUsedLabels(node: Statement): Set<SimpleName> = when (node) {
    // is AssertStmt ->
    is BlockStmt -> node.statements.flatMap(::computeUsedLabels).toSet()
    is BreakStmt -> node.label.orElse(null)?.let { setOf(it) } ?: emptySet()
    is ContinueStmt -> node.label.orElse(null)?.let { setOf(it) } ?: emptySet()
    is DoStmt -> computeUsedLabels(node.body)
    is EmptyStmt -> emptySet()
    // is ExplicitConstructorInvocationStmt ->
    is ExpressionStmt -> computeUsedLabels(node.expression)
    is ForEachStmt -> computeUsedLabels(node.body)
    is ForStmt -> computeUsedLabels(node.body)
    is IfStmt -> computeUsedLabels(node.thenStmt) + node.elseStmt.orElse(null)?.let(::computeUsedLabels).orEmpty()
    is LabeledStmt -> computeUsedLabels(node.statement)
    // is LocalClassDeclarationStmt ->
    // is LocalRecordDeclarationStmt ->
    is ReturnStmt -> emptySet() // has expression
    is SwitchStmt -> node.entries.flatMap { it.statements.flatMap { computeUsedLabels(it) } }.toSet()
    // is SynchronizedStmt ->
    is ThrowStmt -> emptySet() // has expression
    is TryStmt -> computeUsedLabels(node.tryBlock) + node.catchClauses.flatMap { computeUsedLabels(it.body) } + node.finallyBlock.orElse(null)?.let(::computeUsedLabels).orEmpty()
    // is UnparsableStmt -> Errors.unmatchedValue(statement)
    is WhileStmt -> computeUsedLabels(node.body)
    // is YieldStmt ->
    else -> Errors.unmatchedType(node)
  }

  fun keepOnlyLabels(labels: Set<SimpleName>, node: Statement): Statement = when (node) {
    // is AssertStmt ->
    is BlockStmt -> BlockStmt(NodeList(node.statements.map { keepOnlyLabels(labels, it) }))
    is BreakStmt -> node
    is ContinueStmt -> node
    is DoStmt -> DoStmt(keepOnlyLabels(labels, node.body), node.condition)
    is EmptyStmt -> node
    // is ExplicitConstructorInvocationStmt ->
    is ExpressionStmt -> node
    is ForEachStmt -> ForEachStmt(node.variable, node.iterable, keepOnlyLabels(labels, node.body))
    is ForStmt -> ForStmt(node.initialization, node.compare.orElse(null), node.update, keepOnlyLabels(labels, node.body))
    is IfStmt -> IfStmt(node.condition, keepOnlyLabels(labels, node.thenStmt), node.elseStmt.map { keepOnlyLabels(labels, it) }.orElse(null))
    is LabeledStmt ->
      if (node.label in labels) LabeledStmt(node.label, keepOnlyLabels(labels, node.statement))
      else keepOnlyLabels(labels, node.statement)
    // is LocalClassDeclarationStmt ->
    // is LocalRecordDeclarationStmt ->
    is ReturnStmt -> node
    is SwitchStmt -> SwitchStmt(node.selector, NodeList(node.entries.map{ SwitchEntry(it.labels, it.type, NodeList(it.statements.map { keepOnlyLabels(labels, it) }),it.isDefault) }))
    // is SynchronizedStmt ->
    is ThrowStmt -> node
    is TryStmt -> TryStmt(BlockStmt(NodeList(keepOnlyLabels(labels, node.tryBlock))), NodeList(node.catchClauses.map { CatchClause(it.parameter, BlockStmt(NodeList(keepOnlyLabels(labels, it.body)))) }), node.finallyBlock.map{BlockStmt(NodeList(keepOnlyLabels(labels,it)))}.orElse(null))
    // is UnparsableStmt -> Errors.unmatchedValue(statement)
    is WhileStmt -> WhileStmt(node.condition, keepOnlyLabels(labels, node.body))
    // is YieldStmt ->
    else -> Errors.unmatchedType(node)
  }
}
