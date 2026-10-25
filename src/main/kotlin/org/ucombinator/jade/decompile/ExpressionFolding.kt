package org.ucombinator.jade.decompile

import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.ExpressionStmt
import com.github.javaparser.ast.type.PrimitiveType
import com.github.javaparser.ast.type.Type
import org.ucombinator.jade.analysis.ExpressionFoldingAnalysis
import org.ucombinator.jade.analysis.ExpressionGraph
import org.ucombinator.jade.analysis.Var
import org.ucombinator.jade.javaparser.JavaParser

/** Folds single-use temporaries in place; safety decisions belong to the AST analysis. */
object ExpressionFolding {
  fun make(
    body: BlockStmt,
    phiInputs: Set<Var> = emptySet(),
    hasExceptionHandlers: Boolean = false,
    expressionGraph: ExpressionGraph? = null,
  ): BlockStmt {
    while (true) {
      val candidates = ExpressionFoldingAnalysis.analyze(
        body, phiInputs, hasExceptionHandlers, expressionGraph,
      ).foldable
      val declarations = body.findAll(VariableDeclarator::class.java)
      val types = declarations.mapNotNull { d -> getVariableOrNull(d)?.let { it to d.type } }.toMap()
      var changed = false
      for (candidate in candidates) {
        val declaration = declarations.singleOrNull { getVariableOrNull(it) == candidate } ?: continue
        val declarationExpression = declaration.parentNode.orElse(null) as? VariableDeclarationExpr ?: continue
        val declarationStatement = declarationExpression.parentNode.orElse(null) as? ExpressionStmt ?: continue
        if (declarationStatement.parentNode.orElse(null) !is BlockStmt) continue

        val definition = body.findAll(AssignExpr::class.java).single { getVariableOrNull(it.target) == candidate }
        val statement = definition.parentNode.get() as ExpressionStmt
        val use = body.findAll(NameExpr::class.java).single {
          getVariableOrNull(it) == candidate && it !== definition.target
        }
        val value = definition.value.clone()
        val sourceType = typeOf(value, types)

        if (declaration.type !is PrimitiveType && sourceType != declaration.type) continue
  
        val replacement = if (sourceType == declaration.type) value
          else CastExpr(declaration.type.clone(), grouped(value)) // TODO: test the cast
        check(use.replace(grouped(replacement)))
        check(statement.remove())
        if (declarationExpression.variables.size == 1) check(declarationStatement.remove())
        else check(declaration.remove())
        changed = true
        break 
      }
      if (!changed) return body
    }
  }

  private fun getVariableOrNull(node: Node): Var? =
    if (node.containsData(JavaParser.variable)) node.getData(JavaParser.variable) else null

  private fun grouped(expression: Expression): Expression = when (expression) {
    is BinaryExpr, is UnaryExpr, is CastExpr, is InstanceOfExpr -> EnclosedExpr(expression)
    else -> expression
    // TODO: does java parser not automatically add parathesis to these
  }

  private fun typeOf(expression: Expression, types: Map<Var, Type>): Type? = when (expression) {
    is NameExpr -> if (expression.containsData(JavaParser.variableType))
      expression.getData(JavaParser.variableType) else types[getVariableOrNull(expression)]
    is EnclosedExpr -> typeOf(expression.inner, types)
    is CastExpr -> expression.type
    is IntegerLiteralExpr -> PrimitiveType.intType()
    is LongLiteralExpr -> PrimitiveType.longType()
    is BooleanLiteralExpr -> PrimitiveType.booleanType()
    is CharLiteralExpr -> PrimitiveType.charType()
    is DoubleLiteralExpr -> if (expression.value.endsWith("f", ignoreCase = true)) //strings are unstructed data, try not to use it
      PrimitiveType.floatType() else PrimitiveType.doubleType() // TODO: look into a helper function
    is BinaryExpr -> if (
      expression.operator in setOf(
        BinaryExpr.Operator.PLUS, BinaryExpr.Operator.MINUS, BinaryExpr.Operator.MULTIPLY,
        BinaryExpr.Operator.DIVIDE, BinaryExpr.Operator.REMAINDER, BinaryExpr.Operator.BINARY_AND,
        BinaryExpr.Operator.BINARY_OR, BinaryExpr.Operator.XOR, BinaryExpr.Operator.LEFT_SHIFT,
        BinaryExpr.Operator.SIGNED_RIGHT_SHIFT, BinaryExpr.Operator.UNSIGNED_RIGHT_SHIFT,
      ) && typeOf(expression.left, types) == PrimitiveType.intType() &&
      typeOf(expression.right, types) == PrimitiveType.intType()
    ) PrimitiveType.intType() else null
    else -> null
  }
}
