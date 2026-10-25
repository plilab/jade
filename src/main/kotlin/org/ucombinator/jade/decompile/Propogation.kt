package org.ucombinator.jade.decompile
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.stmt.*

import com.github.javaparser.ast.stmt.BlockStmt
import org.jgrapht.graph.DirectedPseudograph
import org.objectweb.asm.tree.AbstractInsnNode
import org.ucombinator.jade.analysis.ControlFlowGraph
import org.ucombinator.jade.analysis.Var
import org.ucombinator.jade.jgrapht.GraphViz
import java.util.LinkedList
import java.util.Queue
import java.util.Stack
import javax.xml.crypto.Data

object Propogation {
  sealed class LatticeValue {
    object Bottom : LatticeValue()
    object Top : LatticeValue()
    data class Constant(val value: Expression) : LatticeValue()
    data class Integer(val value: Int) : LatticeValue()
//    data class Str(val string: String): LatticeValue() // for strings
    data class Var(val string: String): LatticeValue() // create one for variables
//    data class FieldAccess(val path: FieldAccessExpr): LatticeValue() // e.g. java.lang.System.out
    // TODO: possibly ignores side effects
//    data class This(val t: ThisExpr): LatticeValue()
  }

  // {statement : { var : value }}
  val inStates = mutableMapOf<Statement, Map<String, LatticeValue>>()
  val outStates = mutableMapOf<Statement, MutableMap<String, LatticeValue>>()

  var phiVars: Set<String> = setOf() // track phi vars

  fun make(statement: BlockStmt, vars: Map<Var, Set<Pair<AbstractInsnNode, Var?>>>): BlockStmt {
    phiVars = vars.map { (v, _) -> v.name }.toSet() // get phivars
    predecessors.clear()
    successors.clear()
    buildGraph(statement)
    analyse(statement)
    val res = transform(statement)
    return res
  }

  data class DataflowGraph(
    val entry: Statement,
    val predecessors: Map<Statement, Set<Statement>>,
    val successors: Map<Statement, Set<Statement>>,
    val nodes: Set<Statement>
  )
  val successors = mutableMapOf<Statement, MutableSet<Statement>>()
  val predecessors = mutableMapOf<Statement, MutableSet<Statement>>()
  val nodes = mutableSetOf<Statement>()

  fun buildGraph(statement: BlockStmt): DataflowGraph {
    build(statement, null)
    return DataflowGraph(statement.statements[0], predecessors, successors, nodes)
  }
  fun build(statement: BlockStmt, exit: Statement?) {
//    statement.walk { stmt -> println("walking.. stmt is $stmt") }
    val statements = statement.statements
    for (i in statements.indices) {
      val curr = statements[i]
//      println("processing $curr")
//      println("processing $curr children is:")
//      println(curr.walk { node -> println("within curr, node is $node") })
      val next =
        if (i + 1 < statements.size) {
          statements[i + 1]
        } else {
          exit
        }
//      println("type of staement is ${curr.javaClass}")
      when (curr) {
        is LabeledStmt -> { // these are the targets
          val inner = curr.statement
          edge(curr, inner)
//          println("type of labelled statement is ${curr.statement.javaClass}")
          if (inner is WhileStmt) {
            val whileStmt = inner as WhileStmt
            val body = whileStmt.body
            if (body is BlockStmt && body.statements.isNonEmpty) {
              edge(inner, body.statements[0])
              build(body, curr) // loops back
            }
            if (next != null) {
              edge(inner, next)
            }
          } else if (inner is BlockStmt) {
//            println("inside block stmt ${curr.statement}")
            val blockStmt = inner
//            println("the first of blkstmt is ${blockStmt.statements[0]}")
            if (inner.statements.isNonEmpty) {
              edge(inner, inner.statements[0])
              build(inner, next)
            } else if (next != null) {
              edge(curr, next)
            }

          }

        }
        is BlockStmt -> {
            if (curr.statements.isNotEmpty()) {
              edge(curr, curr.statements[0])
              build(curr, next)
            } else if (next != null) {
              edge(curr, next)
            }

        }
        else -> if (next != null) {
          edge(curr, next)
        }
      }
    }
  }
  private fun edge(source: Statement, target: Statement) {
    successors.getOrPut(source) { mutableSetOf()}.add(target)
    predecessors.getOrPut(target) { mutableSetOf()}.add(source)
    nodes.add(source)
    nodes.add(target)
  }


  fun analyse(statement: BlockStmt) {
    val statements = statement.statements

    if (statement.isEmpty) {
      return
    }
    val workList: Queue<Statement> = LinkedList()
    statements.forEach {
      outStates[it] = mutableMapOf()
      inStates[it] = mutableMapOf()
      workList.add(it)
    } // initialize
    workList.add(statements[0]) // get entry
//    workList.add(graph.entry)

    while (!workList.isEmpty()) {
      val stmt = workList.poll()
      val predecessor = predecessors[stmt] ?: emptySet()
      val incoming = if (predecessor.isEmpty()) emptyMap()
      else predecessor.map { outStates[it] ?: emptyMap() }
        .reduce { acc, map -> resolve(acc, map) } // collects incoming var mappings

      inStates[stmt] = incoming

      val newOut = transfer(stmt, incoming)

      if (newOut != outStates[stmt]) {
        // if updated, reprocess successors since values flowing to them changed
        outStates[stmt] = newOut.toMutableMap()
        successors[stmt]?.forEach { v ->
//          println("size of successor is ${successors[stmt]?.size}")
//          println("successor of $stmt is ")
//          println(v)
          if (!workList.contains(v))
          workList.add(v)
        }
      }
    }
//    println(outStates)
  }

  fun transfer(statement: Statement, state: Map<String, LatticeValue>): Map<String, LatticeValue> {
    // based on the expression, determine what value it is mapped to
    val res = state.toMutableMap()
    when (statement) {
      is ExpressionStmt -> {
        when (val expr = statement.expression) {
          is AssignExpr -> {
            val target = expr.target
            if (target is NameExpr) {
              // depends on value assigned
              res[target.nameAsString] = eval(expr.value, state)
            }
          }
          is VariableDeclarationExpr -> {
            for (v in expr.variables) {
              val initialVal = v.initializer.orElse(null)
              res[v.nameAsString] =
                if (initialVal != null) {
                  // determined by initialized value
                  eval(initialVal, state)
                } else {
                  LatticeValue.Bottom // var declaration, initialize val to Bottom
              }
            }
          }
        }
      }
    }
    return res
  }

  fun eval(expr: Expression, state: Map<String, LatticeValue>): LatticeValue =
    // Maps to a value in the lattice
    when (expr) {
      is IntegerLiteralExpr -> LatticeValue.Constant(expr)
      is NameExpr -> {
        if (phiVars.contains(expr.nameAsString)) {
          // assigns a phiVar which has an existing mapping
          // we don't want to propagate the value of a phiVar
          LatticeValue.Var(expr.nameAsString) // copy propagation
        } else {
          val value = state[expr.nameAsString]
          when (value) {
            is LatticeValue.Constant -> value
            is LatticeValue.Var -> value
            else -> LatticeValue.Var(expr.nameAsString) // wrap in a var, cld be a function
          }
        }
      }
      is StringLiteralExpr -> LatticeValue.Constant(expr)
      is FieldAccessExpr -> LatticeValue.Constant(expr)
      is ThisExpr -> LatticeValue.Constant(expr)
      else -> LatticeValue.Top
    }


  fun resolve(newState: Map<String, LatticeValue>, prevState: Map<String, LatticeValue>): Map<String, LatticeValue> {
    // find and resolve to least upper bound
    val res = newState.toMutableMap()
    prevState.forEach { (name, valueOld) ->
      val valueNew = newState[name] ?: LatticeValue.Bottom // get the value of name in the new state
      res[name] = when {
        valueNew == valueOld -> valueNew // if same, return either
        valueNew == LatticeValue.Bottom -> valueOld // just return B
        valueOld == LatticeValue.Bottom -> valueNew
        else -> LatticeValue.Top // join
      }
    }
    return res
  }

private fun transform(originalRoot: BlockStmt): BlockStmt {
  val clonedRoot = originalRoot.clone()

  // We walk the original and clone in parallel
  applyPropagation(originalRoot, clonedRoot)

  return clonedRoot
}

  private fun applyPropagation(original: Node, cloned: Node) {

    if (original is Statement && cloned is Statement) {
      val mapping = inStates[original] ?: emptyMap()

      // walk the cloned node n replace
      cloned.walk(NameExpr::class.java) { node ->
        // Check if this NameExpr is a usage (not a target)
//        var inCondition = false
//        var curr : Node? = node
//        while (curr != null) {
//          val parent = curr.parentNode.orElse(null)
//          println("log")
//          if (parent is IfStmt && parent.condition == curr) inCondition = true
//          if (parent is WhileStmt && parent.condition == curr) inCondition = true
//          curr = parent
//        }
//        if (!inCondition) {
          val isUsage =
            node.parentNode.map {
              when (it) {
                is AssignExpr -> it.target != node
                is UnaryExpr -> false
                // is MethodCallExpr -> {
                //   val value = mapping[node.nameAsString]
                //   !( value is LatticeValue.Constant &&  value.value is IntegerLiteralExpr)
                // }
                // if var in method call is mapped to constant, don't replace as it shld exist
                else -> true
              }
            }.orElse(true)
          if (isUsage) {
            val value = mapping[node.nameAsString]
            replaceWithLattice(node, value)
          }
//        }
      }
    }
    for (i in original.childNodes.indices) {
      if (i < cloned.childNodes.size) {
        applyPropagation(original.childNodes[i], cloned.childNodes[i])
      }
    }
  }
private fun replaceWithLattice(node: NameExpr, value: LatticeValue?) {
  when (value) {
    // is LatticeValue.Integer -> node.replace(IntegerLiteralExpr(value.value.toString()))
    is LatticeValue.Constant ->  {
      // println(value.value)
      node.replace(value.value)
//      when (val expr = value.value) {
//        is IntegerLiteralExpr -> node.replace(expr)
//        is StringLiteralExpr ->

//      }
    }
//    is LatticeValue.Str -> )
    is LatticeValue.Var -> {
      // Only replace if the name actually changed
      if (node.nameAsString != value.string) {
        node.replace(NameExpr(value.string))
      }
    }
//    is LatticeValue.FieldAccess -> node.replace(value.path)
//    is LatticeValue.This -> node.replace(value.t)
    else -> {} // Keep original if Bottom or Top
  }
}}