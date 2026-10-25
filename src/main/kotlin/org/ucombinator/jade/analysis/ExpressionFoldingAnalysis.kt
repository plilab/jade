package org.ucombinator.jade.analysis

import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.stmt.*
import com.github.javaparser.ast.type.PrimitiveType
import com.github.javaparser.ast.type.Type
import org.ucombinator.jade.javaparser.JavaParser


object ExpressionFoldingAnalysis {
  data class Result(val foldable: Set<Var>, val literalInitializers: Map<Var, String>)

  fun analyze(
    body: BlockStmt,
    phiInputs: Set<Var> = emptySet(),
    hasExceptionHandlers: Boolean = false,
    expressionGraph: ExpressionGraph? = null,
  ): Result {
    val declarations = body.findAll(VariableDeclarator::class.java)
    val types = declarations.mapNotNull { declaration -> getVariableOrNull(declaration)?.let { it to declaration.type } }.toMap()
    fun type(node: Node): Type? = if (node.containsData(JavaParser.variableType))
      node.getData(JavaParser.variableType) else types[getVariableOrNull(node)]


    fun isSimplePrimitiveExpression(expression: Expression): Boolean = when (expression) {
      is BooleanLiteralExpr, is CharLiteralExpr, is IntegerLiteralExpr,
      is LongLiteralExpr, is DoubleLiteralExpr -> true
      is NameExpr -> getVariableOrNull(expression) != Var.Empty && type(expression) is PrimitiveType
      is EnclosedExpr -> isSimplePrimitiveExpression(expression.inner)
      is CastExpr -> expression.type is PrimitiveType && isSimplePrimitiveExpression(expression.expression)
      is UnaryExpr -> !isMutatingOperator(expression) && isSimplePrimitiveExpression(expression.expression)
      is BinaryExpr -> expression.operator !in setOf(BinaryExpr.Operator.DIVIDE, BinaryExpr.Operator.REMAINDER) &&
        isSimplePrimitiveExpression(expression.left) && isSimplePrimitiveExpression(expression.right)
      else -> false
    }

    // Reading a typed local/reference or a literal does not invoke code or access the heap.
    fun isSimpleValue(expression: Expression): Boolean = when (expression) {
      is LiteralExpr -> true
      is NameExpr -> getVariableOrNull(expression)?.let { it != Var.Empty } == true && type(expression) != null
      is EnclosedExpr -> isSimpleValue(expression.inner)
      else -> false
    }

    fun isHarmlessToCross(statement: Statement, dependencies: Set<Var>): Boolean = when (statement) {
      is EmptyStmt -> statement.containsData(JavaParser.transparentPlaceholder) &&
        statement.getData(JavaParser.transparentPlaceholder)
      is ExpressionStmt -> when (val expression = statement.expression) {
        is VariableDeclarationExpr -> expression.variables.all { !it.initializer.isPresent }
        is AssignExpr -> {
          val target = getVariableOrNull(expression.target)
          val targetType = type(expression.target)
          val rhs = expression.value
          val simpleRhs = if (targetType is PrimitiveType) isSimplePrimitiveExpression(rhs) else when (rhs) {
            is NameExpr -> getVariableOrNull(rhs) != null && getVariableOrNull(rhs) != Var.Empty &&
              type(rhs) != null && type(rhs) !is PrimitiveType
            is NullLiteralExpr, is StringLiteralExpr, is ThisExpr -> true
            else -> false
          }
          expression.operator == AssignExpr.Operator.ASSIGN && expression.target is NameExpr &&
            target != null && target !in dependencies && targetType != null && simpleRhs //dependencies stores places in which the variable is used again
        }
        else -> false
      }
      else -> false
    }

    //operations where evaluation does not matter
    fun isSafeToEvaluateBeforeUse(expression: Expression, use: NameExpr): Boolean = expression === use || when (expression) {
      is EnclosedExpr -> isSafeToEvaluateBeforeUse(expression.inner, use)
      is CastExpr -> isSafeToEvaluateBeforeUse(expression.expression, use)
      is UnaryExpr -> !isMutatingOperator(expression) && isSafeToEvaluateBeforeUse(expression.expression, use)
      is BinaryExpr -> if (isWithin(use, expression.left)) isSafeToEvaluateBeforeUse(expression.left, use) else
        expression.operator !in setOf(BinaryExpr.Operator.AND, BinaryExpr.Operator.OR) && // || and && cannot
          isSimplePrimitiveExpression(expression.left) && isSafeToEvaluateBeforeUse(expression.right, use)
      is ArrayAccessExpr -> if (isWithin(use, expression.name)) {
        isSafeToEvaluateBeforeUse(expression.name, use)
      } else {
        isSimpleValue(expression.name) && isSafeToEvaluateBeforeUse(expression.index, use)
      }
      is AssignExpr -> {
        val target = expression.target as? ArrayAccessExpr
        expression.operator == AssignExpr.Operator.ASSIGN && target != null &&
          if (isWithin(use, target)) {
            isSafeToEvaluateBeforeUse(target, use)
          } else {
            // An array store evaluates its reference and index before its assigned value.
            isSimpleValue(target.name) && isSimplePrimitiveExpression(target.index) &&
              isSafeToEvaluateBeforeUse(expression.value, use)
          }
      }
      else -> false //methods etc
    }

  
    val excluded = phiInputs + body.findAll(Node::class.java)
      .filter { it.containsData(JavaParser.unsupported) }
      .flatMap { it.getData(JavaParser.unsupported) } +
      declarations.filter { it.initializer.isPresent }.mapNotNull(::getVariableOrNull)
    val references = body.findAll(NameExpr::class.java).filter { getVariableOrNull(it) != null }.groupBy { getVariableOrNull(it)!! } //where all the Variable Names -> where it is used is stored
    val foldable = linkedSetOf<Var>()
    val literals = linkedMapOf<Var, String>()
    val disabled = hasExceptionHandlers || body.findAll(TryStmt::class.java).isNotEmpty() //if there are exceptions, don't fold for now

    for ((value, names) in references) {
      val definitions = names.mapNotNull { name ->
        (name.parentNode.orElse(null) as? AssignExpr)?.takeIf { it.target === name }
      }
      val definition = definitions.singleOrNull() ?: continue
      if (definition.operator != AssignExpr.Operator.ASSIGN) continue //ignore everything else that is not AssignExpr for now
      if (definition.value is LiteralExpr) literals[value] = definition.value.toString()
      if (disabled || value in excluded || (value !is Var.Instruction && value !is Var.Copy)) continue
      if (expressionGraph != null && !expressionGraph.isSingleUseCandidate(value)) continue

      val use = names.filter { it !== definition.target }.singleOrNull() ?: continue //only fold if its being used once
      val source = definition.parentNode.orElse(null) as? ExpressionStmt ?: continue
      val block = source.parentNode.orElse(null) as? BlockStmt ?: continue
      val destination = ancestors(use).filterIsInstance<Statement>().firstOrNull() ?: continue
      //if (destination.parentNode.orElse(null) !== block || type(definition.target) == null) continue
      if (!isSupportedSourceExpression(definition.value)) continue
      if (ancestors(source).any {
          it is TryStmt || it is SynchronizedStmt || it is LambdaExpr || it is ObjectCreationExpr ||
            it is LocalClassDeclarationStmt || it is LocalRecordDeclarationStmt
        }) continue

      val destinationExpression = when (destination) {
        is ReturnStmt -> destination.expression.orElse(null)
        is ThrowStmt -> destination.expression
        is ExpressionStmt -> (destination.expression as? AssignExpr)?.let {
          if (it.operator != AssignExpr.Operator.ASSIGN) null else when {
            it.target is ArrayAccessExpr -> it
            it.target is NameExpr && getVariableOrNull(it.target) != null &&
              getVariableOrNull(it.target) !is Var.Phi -> it.value
            else -> null
          }
        }
        else -> null
      } ?: continue
      val arrayUse = destinationExpression is AssignExpr ||
        ancestors(use).takeWhile { it !== destination }.any { it is ArrayAccessExpr }
      if (arrayUse) {
        // Limit new array positions to constants/copies, excluding potentially throwing unboxing.
        if (!isSimpleValue(definition.value) || type(definition.target) == null) continue
        if (type(definition.target) is PrimitiveType && !isSimplePrimitiveExpression(definition.value)) continue
      }
      if (!isSafeToEvaluateBeforeUse(destinationExpression, use)) continue

      val start = block.statements.indexOfFirst { it === source }
      val end = block.statements.indexOfFirst { it === destination }
      if (end <= start) continue //make sure its folding forward (not sure if i want to move the check more forward)
      val dependencies = definition.value.findAll(NameExpr::class.java).mapNotNull(::getVariableOrNull).toSet()
      if (block.statements.subList(start + 1, end).all { isHarmlessToCross(it, dependencies) }) foldable.add(value)
    }
    return Result(foldable, literals)
  }

  //these expressions are be moved as a block
  private fun isSupportedSourceExpression(expression: Expression): Boolean = when (expression) {
    is LiteralExpr, is ThisExpr -> true
    is NameExpr -> getVariableOrNull(expression)?.let { it != Var.Empty } == true
    is EnclosedExpr -> isSupportedSourceExpression(expression.inner)
    is BinaryExpr -> isSupportedSourceExpression(expression.left) && isSupportedSourceExpression(expression.right)
    is UnaryExpr -> !isMutatingOperator(expression) && isSupportedSourceExpression(expression.expression)
    is CastExpr -> isSupportedSourceExpression(expression.expression)
    is InstanceOfExpr -> isSupportedSourceExpression(expression.expression)
    is ArrayAccessExpr -> isSupportedSourceExpression(expression.name) && isSupportedSourceExpression(expression.index)
    is FieldAccessExpr -> isTypeName(expression.scope) || isSupportedSourceExpression(expression.scope)
    is MethodCallExpr -> !expression.nameAsString.startsWith("<") && //<init>?
      expression.scope.map { isTypeName(it) || isSupportedSourceExpression(it) }.orElse(true) &&
      expression.arguments.all(::isSupportedSourceExpression)
    else -> false
  }

  private fun isMutatingOperator(expression: UnaryExpr): Boolean = expression.operator in setOf(
    UnaryExpr.Operator.PREFIX_INCREMENT, UnaryExpr.Operator.POSTFIX_INCREMENT,
    UnaryExpr.Operator.PREFIX_DECREMENT, UnaryExpr.Operator.POSTFIX_DECREMENT,
  )

  private fun getVariableOrNull(node: Node): Var? =
    if (node.containsData(JavaParser.variable)) node.getData(JavaParser.variable) else null

  private fun isTypeName(node: Node): Boolean =
    node.containsData(JavaParser.isTypeName) && node.getData(JavaParser.isTypeName)

  private fun ancestors(node: Node): Sequence<Node> =
    generateSequence(node.parentNode.orElse(null)) { it.parentNode.orElse(null) }

  private fun isWithin(node: Node, parent: Node): Boolean = node === parent || ancestors(node).any { it === parent }
}
