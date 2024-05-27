package org.ucombinator.jade.jgrapht

import org.jgrapht.Graph
import org.jgrapht.alg.lca.EulerTourRMQLCAFinder
import org.jgrapht.graph.SimpleDirectedGraph
import org.ucombinator.jade.util.Errors

typealias DominatorTree<V> = Graph<V, Dominator.Edge<V>>

data class Dominator<V>(val tree: Graph<V, Dominator.Edge<V>>, val root: V) {
  private val lca = EulerTourRMQLCAFinder(tree, root)
  fun dominates(a: V, b: V): Boolean = lca.getLCA(a, b) == a
  fun dominatesSource(a: V, b: Dominator.Edge<V>): Boolean = lca.getLCA(a, tree.getEdgeSource(b)) == a
  fun dominatesTarget(a: V, b: Dominator.Edge<V>): Boolean = lca.getLCA(a, tree.getEdgeSource(b)) == a

  final data class Edge<V>(val source: V, val target: V)

  companion object {
    fun <V, E> isDominator(tree: Graph<V, E>, v1: V, v2: V): Boolean =
      if (v1 == v2) {
        true
      } else {
        val edges = tree.outgoingEdgesOf(v2)
        when (val size = edges.size) {
          0 -> false
          1 -> isDominator(tree, v1, tree.getEdgeTarget(edges.first()))
          else -> Errors.unmatchedValue(size)
        }
      }

    // This implements the algorithm in the paper:
    //   THOMAS LENGAUER and ROBERT ENDRE TARJAN. A Fast Algorithm for Finding Dominators in a Flowgraph.
    //   ACM Transactions on Programming Languages and Systems, Vol. 1, No. 1, July 1979, Pages 121-141.
    // Based on the code at https://gist.github.com/yuzeh/a5e6602dfdb0db3c2130c10537db54d7
    // A useful description: https://eden.dei.uc.pt/~amilcar/pdf/CompilerInJava.pdf
    @Suppress(
      "CONFUSING_IDENTIFIER_NAMING",
      "LOCAL_VARIABLE_EARLY_DECLARATION",
      "VARIABLE_HAS_PREFIX",
      "VARIABLE_NAME_INCORRECT_FORMAT"
    )
    fun <V, E> dominatorTree(graph: Graph<V, E>, start: V): DominatorTree<V> {
      // The original algorithm dealt in Ints, not Vs.
      fun successors(v: V): Iterable<V> = graph.outgoingEdgesOf(v).map(graph::getEdgeTarget)
      fun predecessors(v: V): Iterable<V> = graph.incomingEdgesOf(v).map(graph::getEdgeSource)
      val numNodes: Int = graph.vertexSet().size

      var N = 0

      val bucket = mutableMapOf<V, Set<V>>() // buckets of nodes with the same sdom
      for (vertex in graph.vertexSet()) {
        bucket.put(vertex, setOf<V>())
      }

      val dfnum = mutableMapOf<V, Int>() // The order of nodes reached in DFS
      val vertex = MutableList<V?>(numNodes) { null } // The vertex assigned to a given number
      val parent = mutableMapOf<V, V>() // The parent of node in DFS tree
      val semi = mutableMapOf<V, V>() // The semidominaor of each V
      val ancestor = mutableMapOf<V, V>() // Used by ancestorWithLowestSemi. Mutable and path compressed.
      val idom = mutableMapOf<V, V>() // The idom (once known)
      val samedom = mutableMapOf<V, V>() // The node determined to have same idom
      val best = mutableMapOf<V, V>() // The ancestor of V with lowest semidominator

      // Finds the ancestor of v with the lowest semidominator. Uses path compression to keep runtime down.
      fun ancestorWithLowestSemi(v: V): V {
        val a = ancestor.getValue(v) // ancestor initially means parent; only modified here
        if (ancestor.contains(a)) { // if defined
          val b = ancestorWithLowestSemi(a)
          ancestor.put(v, ancestor.getValue(a))
          if (dfnum.getValue(semi.getValue(b)) < dfnum.getValue(semi.getValue(best.getValue(v)))) {
            best.put(v, b)
          }
        }
        return best.getValue(v)
      }

      // Helper function: p is parent of n
      fun link(p: V, n: V) {
        ancestor.put(n, p)
        best.put(n, n)
      }

      // Setup DFS tree
      var stack: MutableList<Pair<V?, V>> = mutableListOf(Pair(null, start))
      while (stack.isNotEmpty()) {
        val (p, n) = stack.removeLast()
        if (!dfnum.contains(n)) {
          dfnum.put(n, N)
          vertex.set(N, n)
          parent.put(n, p!!)
          N += 1
          for (w in successors(n)) {
            stack.add(Pair(n, w))
          }
        }
      }

      // Iterate over nodes from bottom of DFS tree to top.
      for (i in (N - 1) downTo 0) { // TODO: check boundry conditions
        val n = vertex[i]!!
        val p = parent.getValue(n)
        var s = p

        // Find the semidominator of v
        for (v in predecessors(n)) {
          val sPrime =
            // Determine if pred is an ancestor in DFS tree.
            if (dfnum.getValue(v) <= dfnum.getValue(n)) {
              v
            } else {
              semi.getValue(ancestorWithLowestSemi(v))
            }
          if (dfnum.getValue(sPrime) < dfnum.getValue(s)) { // Pick lowest
            s = sPrime
          }
        }

        semi.put(n, s)
        bucket.put(s, bucket.getValue(s).plus(n))

        link(p, n)

        // For each bucket, find ancestor with lowest semi. If it has the same
        // semi, that semi is the idom. If not, it has the same semidominator.
        for (v in bucket.getValue(p)) {
          val y = ancestorWithLowestSemi(v)
          if (semi.getValue(y) === semi.getValue(v)) {
            idom.put(v, p)
          } else {
            samedom.put(v, y)
          }
        }
        bucket.put(p, setOf())
      }

      // Iterate and assign idom based on samedom. Order guarantees idom will be defined in time.
      for (i in 0 until N) {
        val n = vertex[i]!!
        if (samedom.contains(n)) {
          idom.put(n, idom.getValue(samedom.getValue(n)))
        }
      }

      // Algorithm complete; remaining code is just for translation to expected result structure
      val tree = SimpleDirectedGraph<V, Edge<V>>(Edge::class.java as Class<Edge<V>>)
      tree.addVertex(start) // TODO: suspicious: may or may not be key in idom
      for ((key, _) in idom) {
        tree.addVertex(key)
      }
      for ((key, value) in idom) {
        tree.addEdge(key, value, Edge(key, value))
      }
      return tree
    }
  }
}
