package org.ucombinator.jade.analysis

/** Finds removable phi values using an existing expression graph, without changing the graph or SSA. */
object UnusedPhiAnalysis {
  fun analyze(ssa: StaticSingleAssignment, expressionGraph: ExpressionGraph): Set<Var> {
    // Every instruction's phi operands are live, even when its result is unused.
    val live = ssa.insnVars.values.flatMap { it.second }
      .filterIsInstance<Var.Phi>()
      .map(expressionGraph.nodesByVar::getValue)
      .toMutableSet()
    val pending = ArrayDeque(live)
    while (pending.isNotEmpty()) {
      val node = pending.removeFirst()
      // Edges point from input to consumer, so follow incoming phi dependencies.
      for (edge in expressionGraph.graph.incomingEdgesOf(node)) {
        val input = edge.source
        if (input.kind == ExpressionGraph.Kind.PHI && live.add(input)) pending.addLast(input)
      }
    }
    // This includes dead chains and dead phi-only cycles.
    return ssa.phiInputs.keys.filterTo(mutableSetOf()) { expressionGraph.nodesByVar.getValue(it) !in live }
  }
}
