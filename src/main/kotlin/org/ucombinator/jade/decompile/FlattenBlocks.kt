package org.ucombinator.jade.decompile

import com.github.javaparser.ast.Node
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.stmt.*
import org.ucombinator.jade.decompile.RemoveUnusedLabels.keepOnlyLabels
import org.ucombinator.jade.util.Errors
import org.ucombinator.jade.util.Log

object FlattenBlocks { // TODO: rename to FlattenBlocks
  private val log = Log {}

  fun make(node: Statement): NodeList<Statement> = when (node) {
    // is AssertStmt -> 
    is BlockStmt -> NodeList(node.statements.flatMap(::make))
    is BreakStmt -> NodeList(node)
    is ContinueStmt -> NodeList(node)
    is DoStmt -> make(node.body)
    is EmptyStmt -> NodeList(node) // commented out ones with the ;
    // is ExplicitConstructorInvocationStmt -> 
    is ExpressionStmt -> NodeList(node)
    is ForEachStmt -> make(node.body)
    is ForStmt -> make(node.body)
    is IfStmt -> NodeList(IfStmt(node.condition, BlockStmt(make(node.thenStmt)), node.elseStmt.map{ BlockStmt(make(it)) }.orElse(null)))
    is LabeledStmt -> {
      val stmt = node.statement
      NodeList(
        when (stmt) {
          // label loops so 'continue' works
          is DoStmt -> LabeledStmt(node.label, DoStmt(BlockStmt(make(stmt)), stmt.condition))
          is ForEachStmt -> LabeledStmt(node.label, ForEachStmt(stmt.variable, stmt.iterable, BlockStmt(make(stmt))))
          is ForStmt -> LabeledStmt(node.label, ForStmt(stmt.initialization, stmt.compare.orElse(null), stmt.update, BlockStmt(make(stmt))))
          is WhileStmt -> LabeledStmt(node.label, WhileStmt(stmt.condition, BlockStmt(make(stmt))))
          // labeled non-loops become blocks
          else -> LabeledStmt(node.label, BlockStmt(make(node.statement)))
        }
      )
    } // TODO: if NodeList.size == 1, then no BlockStmt
    // is LocalClassDeclarationStmt -> 
    // is LocalRecordDeclarationStmt -> 
    is ReturnStmt -> NodeList(node)
    is SwitchStmt -> NodeList(SwitchStmt(node.selector, NodeList(node.entries.map{ SwitchEntry(it.labels, it.type, NodeList(it.statements.map{ BlockStmt(make(it)) }),it.isDefault) })))
    // is SynchronizedStmt -> 
    is ThrowStmt -> NodeList(node)
    // is TryStmt -> 
    // is UnparsableStmt -> Errors.unmatchedValue(statement)
    is WhileStmt -> make(node.body)
    // is YieldStmt -> 
    else -> Errors.unmatchedType(node)
  }
}
