package org.ucombinator.jade.decompile

import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.expr.AssignExpr
import com.github.javaparser.ast.expr.CastExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.expr.ThisExpr
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt
import com.github.javaparser.ast.stmt.ExpressionStmt
import com.github.javaparser.ast.stmt.Statement
import org.ucombinator.jade.util.Log

object RewriteConstructorCalls {
  private val log = Log {}

  fun make(statements: NodeList<Statement>): NodeList<Statement> {
    if (statements.isEmpty()) return statements

    // TODO: Preserve the constructor prologue permitted by Java 25 flexible constructor bodies instead of
    // moving every explicit super() call to the first statement.

    // Build assignment map: variable name -> RHS expression
    val assignMap: Map<String, Expression> = statements
      .filterIsInstance<ExpressionStmt>()
      .map { it.expression }
      .filterIsInstance<AssignExpr>()
      .filter { it.target is NameExpr }
      .associate { (it.target as NameExpr).nameAsString to it.value }

    // Find the super()/this() <init> call (scope must be ThisExpr), resolve its arguments, and move it first
    return statements
      .indexOfFirst { stmt ->
        stmt is ExpressionStmt &&
          stmt.expression.let {
            it is MethodCallExpr &&
              it.name.identifier == "<init>" &&
              it.scope.map { scope -> scope is ThisExpr }.orElse(false)
          }
      }
      .takeIf { it >= 0 }
      ?.let { i ->
        val initExpr = (statements[i] as ExpressionStmt).expression as MethodCallExpr
        // this() is already converted to ExplicitConstructorInvocationStmt by DecompileInsn,
        // so any <init> MethodCallExpr with ThisExpr scope reaching here is super()
        val superCall = ExplicitConstructorInvocationStmt(
          false, null,
          NodeList(initExpr.arguments.map { resolve(it, assignMap) }),
        )
        NodeList(listOf(superCall) + statements.filterIndexed { j, _ -> j != i })
      }
      ?: statements
  }

  private fun resolve(
      expr: Expression,
      map: Map<String, Expression>,
  ): Expression = when (expr) {
    is NameExpr ->
      expr.nameAsString
        .takeIf { it !in map || it.startsWith("parameterVar") }
        ?.let { expr }
        ?: resolve(map.getValue(expr.nameAsString), map)
    is CastExpr ->
      CastExpr(expr.type.clone(), resolve(expr.expression, map))
    is MethodCallExpr ->
      expr.clone().also { clone ->
        expr.scope.ifPresent { clone.setScope(resolve(it, map)) }
        clone.arguments = NodeList(expr.arguments.map { resolve(it, map) })
      }
    else -> expr
  }
}
