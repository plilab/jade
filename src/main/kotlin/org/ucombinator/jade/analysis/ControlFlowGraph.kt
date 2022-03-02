package org.ucombinator.jade.analysis

// import scala.jdk.CollectionConverters._

import org.jgrapht.Graph
import org.jgrapht.graph.AsGraphUnion
import org.jgrapht.graph.DirectedPseudograph
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.analysis.Analyzer
import org.objectweb.asm.tree.analysis.BasicValue
import org.objectweb.asm.tree.analysis.Frame
import org.ucombinator.jade.asm.Insn
import org.ucombinator.jade.asm.TypedBasicInterpreter

// import ControlFlowGraph.Edge

data class ControlFlowGraph(
  val entry: Insn,
  val graph: DirectedPseudograph<Insn, Edge>,
  val graphWithExceptions: Graph<Insn, Edge>,
  val frames: Array<Frame<BasicValue>>
) {
  final data class Edge(val source: Insn, val target: Insn)

  companion object {
    fun apply(owner: String, method: MethodNode): ControlFlowGraph {
      val graph = DirectedPseudograph<Insn, Edge>(Edge::class.java)
      for (i in method.instructions.toArray()) {
        graph.addVertex(Insn(method, i))
      }
      val analyzer = ControlFlowGraphAnalyzer(method, graph)
      val frames = analyzer.analyze(owner, method)
      val entry = Insn(method, method.instructions.getFirst())
      val g = DirectedPseudograph<Insn, Edge>(Edge::class.java)
      for (handler in method.tryCatchBlocks) {
        val s = Insn(method, handler.start)
        val h = Insn(method, handler.handler)
        g.addVertex(s)
        g.addVertex(h)
        g.addEdge(s, h, Edge(s, h))
      }
      val graphWithExceptions: Graph<Insn, Edge> = AsGraphUnion(graph, g)
      return ControlFlowGraph(entry, graph, graphWithExceptions, frames)
    }
  }
}

private class ControlFlowGraphAnalyzer(val method: MethodNode, val graph: DirectedPseudograph<Insn, ControlFlowGraph.Edge>) :
  Analyzer<BasicValue>(TypedBasicInterpreter) {
  protected override fun newControlFlowEdge(insn: Int, successor: Int) {
    val source = Insn(method, this.method.instructions.get(insn))
    val target = Insn(method, this.method.instructions.get(successor))
    this.graph.addEdge(source, target, ControlFlowGraph.Edge(source, target))
  }
}
