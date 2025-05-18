package org.ucombinator.jade.jgrapht.dominator

import org.jgrapht.Graph
import org.jgrapht.graph.AsSubgraph
import org.jgrapht.graph.DirectedPseudograph
import org.jgrapht.graph.SimpleDirectedGraph

import kotlin.test.Test
import kotlin.test.expect

private typealias Vertex = Char

object DominatorTest {
  // Collection or Iterable or List
  fun <A> Iterable<A>.forEachSubset(action: (List<A>) -> Unit) {
    val list = this.toList()
    fun go(i: Int, acc: List<A>) {
      if (i == list.size) action(acc)
      else { go(i + 1, acc); go(i + 1, acc + list[i]) }
    }
    go(0, emptyList())
  }

  // TODO: quick check random graphs
  @Test fun test() {
    // Test taken from the paper:
    //
    //   Thomas Lengauer and Robert Endre Tarjan. A Fast Algorithm for Finding Dominators in a Flowgraph.
    //   ACM Transactions on Programming Languages and Systems, Vol. 1, No. 1, July 1979, Pages 121-141.
    //   https://doi.org/10.1145/357062.357071
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
      // Added beyond standard test to include isolated vertex and unreachable vertex going to root
      'X' to listOf('R'), // test dead edges into root
      'Y' to listOf('Y'), // test dead edges
    )
    val graph = DirectedPseudograph<Vertex, Pair<Vertex, Vertex>>(Pair::class.java as Class<Pair<Vertex, Vertex>>).apply {
      for ((source, _) in graphEdges) {
        addVertex(source)
      }
      for ((source, targets) in graphEdges) {
        for (target in targets) {
          addEdge(source, target, source to target)
        }
      }
    }
    // TODO: JGraphT(vertexes, edges)
    // TODO: GOTCHAS document: why JGraphT requires unique object for each edge (and each edge must contain src and dst)
    // TODO: GOTCHAS document: JGraphT: simplegraph vs Pseudograph

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
      'X' to setOf(),
      'Y' to setOf(),
    )

    val tree = SimpleDirectedGraph<Vertex, Dominator.Edge<Vertex>>(Dominator.Edge::class.java as Class<Dominator.Edge<Vertex>>).apply {
      for ((source, _) in treeEdges) {
        addVertex(source)
      }
      for ((source, targets) in treeEdges) {
        for (target in targets) {
          addEdge(source, target, Dominator.Edge<Vertex>(source, target))
        }
      }
    }

    expect(tree) { DominatorReference.dominatorTree(graph, root) }
    expect(tree) { Dominator.dominatorTree(graph, root) }

    fun check(graph: Graph<Vertex, Pair<Vertex, Vertex>>, root: Vertex) {
      try {
        val ref = DominatorReference.dominatorTree(graph, root)
        expect(ref) { Dominator.dominatorTree(graph, root) }
      } catch (e: Throwable) {
        println("root: $root graph: $graph")
        throw e
      }
    }
    for (r in graph.vertexSet()) {
      check(graph, r)
    }

    // TODO: longer running, more thorough tests
    // graph.edgeSet().forEachSubset { edges ->
    //   val subgraph = AsSubgraph(graph, graph.vertexSet(), edges.toSet())
    //   check(subgraph, root)
    //   for (r in graph.vertexSet()) { check(subgraph, r) }
    // }
  }
}
