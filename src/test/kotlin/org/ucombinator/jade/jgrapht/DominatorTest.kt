package org.ucombinator.jade.jgrapht

import org.jgrapht.graph.SimpleDirectedGraph

import kotlin.test.Test
import kotlin.test.expect

private typealias Vertex = Char

object DominatorTest {
  @Test fun test() {
    val graph = SimpleDirectedGraph<Vertex, Pair<Vertex, Vertex>>(Pair::class.java as Class<Pair<Vertex, Vertex>>)
    // Test taken from the paper:
    //   THOMAS LENGAUER and ROBERT ENDRE TARJAN. A Fast Algorithm for Finding Dominators in a Flowgraph.
    //   ACM Transactions on Programming Languages and Systems, Vol. 1, No. 1, July 1979, Pages 121-141.
    val root = 'R'
    val graphEdges = listOf<Pair<Vertex, List<Vertex>>>(
      'R' to listOf('A', 'B', 'C'),
      'A' to listOf('D'),
      'B' to listOf('A', 'D', 'E'),
      'C' to listOf('F', 'G'),
      'D' to listOf('L'),
      'E' to listOf('H'),
      'F' to listOf('I'),
      'G' to listOf('I', 'J'),
      'H' to listOf('E', 'K'),
      'I' to listOf('K'),
      'J' to listOf('I'),
      'K' to listOf('R', 'I'),
      'L' to listOf('H'),
    )
    for (vertex: Vertex in graphEdges.map(Pair<Vertex, List<Vertex>>::first)) {
      graph.addVertex(vertex)
    }
    for ((source, targets) in graphEdges) {
      for (target in targets) {
        graph.addEdge(source, target, source to target)
      }
    }

    val treeEdges = mapOf(
      'R' to setOf(),
      'A' to setOf('R'),
      'B' to setOf('R'),
      'C' to setOf('R'),
      'D' to setOf('R'),
      'E' to setOf('R'),
      'F' to setOf('C'),
      'G' to setOf('C'),
      'H' to setOf('R'),
      'I' to setOf('R'),
      'J' to setOf('G'),
      'K' to setOf('R'),
      'L' to setOf('D'),
    )

    expect(treeEdges) {
      val result = Dominator.dominatorTree(graph, root)
      result.vertexSet().map { v -> v to result.outgoingEdgesOf(v).map { it.target }.toSet() }.toMap()
    }
  }
}
