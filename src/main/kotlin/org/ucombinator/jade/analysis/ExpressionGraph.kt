package org.ucombinator.jade.analysis

import org.jgrapht.graph.DirectedPseudograph

/**
 * Supplies conservative SSA candidate checks and displays analysis annotations.
 */
data class ExpressionGraph(
  val graph: DirectedPseudograph<ExpressionNode, ExpressionEdge>,
  val roots: Set<ExpressionNode>,
  val nodesByVar: Map<Var, ExpressionNode>,
) {
  /** SSA dependencies can veto a candidate; the current AST must still prove movement is safe. */
  fun isSingleUseCandidate(variable: Var): Boolean {
    val node = nodesByVar[variable] ?: return false
    val use = graph.outgoingEdgesOf(node).singleOrNull() ?: return false
    return use.target.kind != Kind.PHI
  }

  data class ExpressionNode(
    val id: String,
    val kind: Kind,
    val label: String,
    val canFold: Boolean = false,
    /** Whether this node is an unused phi or its empty input that can be removed without folding. */
    val canRemove: Boolean = false,
  ) {
    override fun toString(): String = label
  }

  // Each operand occurrence needs its own identity, including repeated operands such as t + t.
  class ExpressionEdge(
    val source: ExpressionNode,
    val target: ExpressionNode,
  )

  enum class Kind {
    VAR,
    EMPTY,
    LITERAL,
    PHI,
  }

  companion object {
    fun make(
      ssa: StaticSingleAssignment,
      folding: ExpressionFoldingAnalysis.Result? = null,
      removablePhis: Set<Var> = emptySet(),
    ): ExpressionGraph {
      val graph = DirectedPseudograph<ExpressionNode, ExpressionEdge>(ExpressionEdge::class.java)

      val varNodes = linkedMapOf<Var, ExpressionNode>()
      val literalNodes = linkedMapOf<String, ExpressionNode>()
      val emptyInputNodes = linkedMapOf<Pair<Var, Int>, ExpressionNode>()

      fun ensureVarNode(v: Var): ExpressionNode {
        return varNodes.getOrPut(v) {
          val node = ExpressionNode(
            id = "v_${varNodes.size}",
            kind = when (v) {
              Var.Empty -> Kind.EMPTY
              is Var.Phi -> Kind.PHI
              else -> Kind.VAR
            },
            label = v.name,
            canFold = v in folding?.foldable.orEmpty(),
            canRemove = v in removablePhis,
          )
          graph.addVertex(node)
          node
        }
      }

      fun ensureLiteralNode(value: String): ExpressionNode {
        return literalNodes.getOrPut(value) {
          val node = ExpressionNode(
            id = "lit_${literalNodes.size}",
            kind = Kind.LITERAL,
            label = value,
          )
          graph.addVertex(node)
          node
        }
      }

      fun connect(source: ExpressionNode, target: ExpressionNode) {
        graph.addVertex(source)
        graph.addVertex(target)
        graph.addEdge(source, target, ExpressionEdge(source, target))
      }

      val constantLiterals = folding?.literalInitializers.orEmpty()

      fun nodeFor(v: Var): ExpressionNode = ensureVarNode(v)

      fun inputNodeFor(v: Var, consumer: Var, inputIndex: Int): ExpressionNode {
        if (v != Var.Empty) return nodeFor(v)
        return emptyInputNodes.getOrPut(consumer to inputIndex) {
          val number = emptyInputNodes.size + 1
          ExpressionNode(
            id = "empty_$number", kind = Kind.EMPTY, label = "emptyVar$number",
            canRemove = consumer in removablePhis,
          )
            .also(graph::addVertex)
        }
      }

      for ((retVar, argVars) in ssa.insnVars.values) {
        val resultNode = ensureVarNode(retVar)
        constantLiterals[retVar]?.let { literal ->
          connect(ensureLiteralNode(literal), resultNode)
        }

        argVars.forEachIndexed { inputIndex, argVar ->
          connect(inputNodeFor(argVar, retVar, inputIndex), resultNode)
        }
      }

      for ((phiVar, inputs) in ssa.phiInputs) {
        val phiNode = ensureVarNode(phiVar)
        inputs.forEachIndexed { inputIndex, (_, inputVar) ->
          if (inputVar != null) {
            connect(inputNodeFor(inputVar, phiVar, inputIndex), phiNode)
          }
        }
      }


      val consumed = (
        ssa.insnVars.values.flatMap { it.second } +
          ssa.phiInputs.values.flatMap { inputs -> inputs.mapNotNull { it.second } }
      ).toSet()

      val leaves = ssa.insnVars.values
        .mapNotNull { it.first }
        .filter { it !in consumed }
        .map(::nodeFor)
        .toSet()

      return ExpressionGraph(graph, leaves, varNodes.toMap())
    }
  }
}
