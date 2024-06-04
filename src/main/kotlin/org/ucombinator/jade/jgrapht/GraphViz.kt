package org.ucombinator.jade.jgrapht

import org.jgrapht.Graph
import org.jgrapht.nio.DefaultAttribute
import org.jgrapht.nio.dot.DOTExporter
import org.ucombinator.jade.analysis.ControlFlowGraph
import org.ucombinator.jade.asm.Insn
import java.io.StringWriter
import java.io.Writer

object GraphViz {
  fun escape(string: String): String = string.replace("\\\\", """\\\\""").replace("\"", """\\"""")

  fun <N, E> toString(graph: Graph<N, E>): String {
    val writer = StringWriter()
    print(writer, graph)
    return writer.toString()
  }

  fun <N, E> print(writer: Writer, graph: Graph<N, E>) {
    val dotExporter = DOTExporter<N, E>()
    dotExporter.setVertexAttributeProvider {
      mapOf("label" to DefaultAttribute.createAttribute(it.toString()))
    }
    dotExporter.exportGraph(graph, writer)
  }

  fun toString(graph: ControlFlowGraph): String {
    val writer = StringWriter()
    print(writer, graph)
    return writer.toString()
  }

  fun print(writer: Writer, graph: ControlFlowGraph) {
    val dotExporter = DOTExporter<Insn, ControlFlowGraph.Edge>()
    dotExporter.setVertexAttributeProvider {
      mapOf("label" to DefaultAttribute.createAttribute(it.longString()))
    }
    dotExporter.exportGraph(graph.graph, writer)
  }

  fun <V, GE, TE> nestingTree(graph: Graph<V, GE>, tree: Graph<V, TE>, root: V): String {
    val writer = StringWriter()
    nestingTree(writer, graph, tree, root)
    return writer.toString()
  }

  fun <V, GE, TE> nestingTree(
    out: Writer,
    graph: Graph<V, GE>,
    tree: Graph<V, TE>,
    root: V,
    alternateBackgroundColor: Boolean = true,
    flatten: Boolean = true
  ) {
    out.write("digraph {\n")

    val ids = mutableMapOf<V, String>()
    fun id(v: V): String = ids.getOrPut(v, { "n${ids.size}" })

    var cluster = 0
    fun go(indent: String, v: V, backgroundColor: Boolean, soleChild: Boolean) {
      cluster += 1
      // NOTE: subgraph must have a name starting with "cluster" to get GraphViz to draw a box around it
      if (!flatten || !soleChild) {
        out.write("${indent}subgraph cluster$cluster {\n")
        if (alternateBackgroundColor) {
          val bgcolor = if (backgroundColor) "\"#eeeeee\"" else "\"#ffffff\""
          out.write("$indent  bgcolor=$bgcolor;\n")
        }
      }
      val label = "\"${GraphViz.escape(v.toString())}\""
      out.write("$indent  ${id(v)} [label=$label];\n")
      val edges = tree.incomingEdgesOf(v)
      // TODO: edges in trees should always go down
      val sole = if (edges.size == 1) {
        var x = edges.first()
        val y = tree.getEdgeSource(x)
        graph.outgoingEdgesOf(v).map(graph::getEdgeTarget) == setOf(y) &&
          graph.incomingEdgesOf(y).map(graph::getEdgeSource) == setOf(v)
      } else {
        false
      }
      for (child in edges.map(tree::getEdgeSource)) {
        val newIndent = if (!flatten || !sole) "$indent  " else indent
        go(newIndent, child, (flatten && sole) == backgroundColor, sole)
      }
      if (!flatten || !soleChild) {
        out.write("$indent}\n")
      }
    }

    go("  ", root, false, false)
    for (edge in graph.edgeSet()) {
      val source = graph.getEdgeSource(edge)
      val target = graph.getEdgeTarget(edge)
      val constraint = !Dominator.isDominator(tree, target, source)
      out.write("  ${id(source)} -> ${id(target)} [constraint=$constraint];\n")
    }
    out.write("}\n")
  }
}
