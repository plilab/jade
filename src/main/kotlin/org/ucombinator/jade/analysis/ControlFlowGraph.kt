package org.ucombinator.jade.analysis

import org.jgrapht.Graph
import org.jgrapht.graph.AsGraphUnion
import org.jgrapht.graph.DirectedPseudograph
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.analysis.Analyzer
import org.objectweb.asm.tree.analysis.BasicValue
import org.objectweb.asm.tree.analysis.Frame
import org.ucombinator.jade.asm.Insn
import org.ucombinator.jade.asm.TypedBasicInterpreter

/** Represents a control flow graph for a method.
 *
 * @property entry The entry instruction of the control flow graph.
 * @property graph Basic control flow graph version.
 * @property graphWithExceptions Control flow graph version with exceptions.
 * @property frames An array of ASM `Frame` objects representing states of variables.
 */
data class ControlFlowGraph(
  val entry: Insn,
  val graph: DirectedPseudograph<Insn, Edge>,
  val graphWithExceptions: Graph<Insn, Edge>,
  val frames: Array<Frame<BasicValue>>,
) {
  /** TODO:doc.
   *
   * @property source TODO:doc
   * @property target TODO:doc
   */
  final data class Edge(val source: Insn, val target: Insn)

  companion object {
    /** TODO:doc.
     *
     * @param owner TODO:doc
     * @param method TODO:doc
     * @return TODO:doc
     */
    fun make(owner: String, method: MethodNode): ControlFlowGraph {
      val graph = DirectedPseudograph<Insn, Edge>(Edge::class.java)
      for (i in method.instructions.toArray()) {
        graph.addVertex(Insn(method, i))
      }
      val analyzer = ControlFlowGraphAnalyzer(method, graph)
      val frames = analyzer.analyze(owner, method)
      val entry = Insn(method, method.instructions.first)
      val g = DirectedPseudograph<Insn, Edge>(Edge::class.java)
      for (handler in method.tryCatchBlocks) {
        val startInsn = Insn(method, handler.start)
        val handlerInsn = Insn(method, handler.handler)
        g.addVertex(startInsn)
        g.addVertex(handlerInsn)
        g.addEdge(startInsn, handlerInsn, Edge(startInsn, handlerInsn))
      }
      val graphWithExceptions: Graph<Insn, Edge> = AsGraphUnion(graph, g)
      return ControlFlowGraph(entry, graph, graphWithExceptions, frames)
    }
  }
}

/** TODO:doc.
 *
 * @property method TODO:doc
 * @property graph TODO:doc
 */
private class ControlFlowGraphAnalyzer(
  val method: MethodNode,
  val graph: DirectedPseudograph<Insn, ControlFlowGraph.Edge>,
) : Analyzer<BasicValue>(TypedBasicInterpreter) {
  protected override fun newControlFlowEdge(insn: Int, successor: Int) {
    val source = Insn(method, this.method.instructions[insn])
    val target = Insn(method, this.method.instructions[successor])
    this.graph.addEdge(source, target, ControlFlowGraph.Edge(source, target))
  }
}
