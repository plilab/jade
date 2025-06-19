package org.ucombinator.jade.analysis

import org.jgrapht.graph.DirectedPseudograph
import org.jgrapht.traverse.DepthFirstIterator
import org.ucombinator.jade.asm.Insn
import org.ucombinator.jade.jgrapht.dominator.Dominator

//\\ import org.ucombinator.jade.util.MyersList
// import org.ucombinator.jade.jgrapht.Dominator
// import org.jgrapht.traverse.DepthFirstIterator

/*
Non-Linear Stmt Types
  -Break
  -Continue
  -Return/Throw
  +Try-Catch-Finally
  +Synchronized
  +Do/While/For/For-each
  +If/Switch
Non-linear expressions
  Boolean(&&/||/!/==/!=/</>/<=/>=)
  Trinary Operator/Switch Expression
 */

//typealias Nesting = List<Structure.Block>

// TODO: rename Structure to CodeStructure or CodeNesting or BlockNesting

data class Loops(val nesting: DirectedPseudograph<Insn, Edge>, val backEdges: Set<ControlFlowGraph.Edge>) {
  data class Edge(val child: Insn, val parent: Insn?)

  companion object {
    fun make(cfg: ControlFlowGraph): Loops {
      val dominatorTree = Dominator.dominatorTree(cfg.graph, cfg.entry)
      val backEdges = cfg.graph.edgeSet().filter {
        dominatorTree.dominates(cfg.graph.getEdgeTarget(it), cfg.graph.getEdgeSource(it))
      }.toSet()

      val tree = DirectedPseudograph<Insn, Edge>(Edge::class.java)
      // TODO: what if a insn is not in the dominator tree?  E.g., reachable only by throwing an exception
      val visited = mutableSetOf<Insn>() // keep track of nodes visited
      fun go(insn: Insn, loopHead: Insn?): Unit {
        val parent = loopHead ?: insn // if null then set as curr instruction to avoid NPE
        tree.addVertex(insn)
        if (insn != parent) { // avoid pointing to self?
          tree.addVertex(parent)
          tree.addEdge(parent, insn, Edge(insn, parent))
        }
        val firstVisit = visited.add(insn)
        val insnIsALoopHead = cfg.graph.incomingEdgesOf(insn).any { backEdges.contains(it) }
        val newLoopHead = if (insnIsALoopHead) { insn } else { parent }
          for (child in cfg.graph.outgoingEdgesOf(insn)) {
            if (firstVisit) {
              go(child.target, newLoopHead)
            }
          }
      }
      go(dominatorTree.root, null)

      return Loops(tree, backEdges)
    }
  }
}


/** Represents the domination structure in the CFG.
 *
 * @property nesting A map that associates each instruction with its nesting level within the CFG.
 * @property backEdges A set of edges in the CFG that keeps track of back edges.
 */
//data class Structure(val nesting: Map<Insn, Nesting>, val backEdges: Set<ControlFlowGraph.Edge>) {
//  // TODO: Instead of MyersList use the following or one of the others in that package:
//  //  * https://jgrapht.org/javadoc/org.jgrapht.core/org/jgrapht/alg/lca/EulerTourRMQLCAFinder.html
//  //      Preprocessing Time complexity: O(|V|log(|V|))
//  //      Preprocessing Space complexity: O(|V|log(|V|))
//  //      Query Time complexity: O(1)
//  //      Query Space complexity: O(1)
//  // TODO: name to NestingPath or StructurePath or BlockPath
//
//  /** TODO:doc.
//   *
//   * @property kind TODO:doc
//   * @property headInsn TODO:doc
//   */
//  data class Block(val kind: Kind, val headInsn: Insn)
//  // case class Block(kind: Kind, headInsn: Insn, var parent: Block = null)
//
//  /** TODO:doc. */
//  sealed interface Kind {
//    /** TODO:doc. */
//    object Loop : Kind
//
//    // TODO:
//    // case class Exception(insns: List[Insn], handlers: List[(Insn, Type)]) extends Kind
//    // handlers: dominated by head insn
//    // body: dominated by head but not handlers
//    // finally: ignore until refactoring pass
//    // try ResourceSpecification Block [Catches] [Finally]
//
//    /** TODO:doc. */
//    object Exception : Kind
//
//    // TODO:
//    // Syncronized involves a try-finally pattern
//    // case class Synchronized(value) extends Kind
//
//    /** TODO:doc. */
//    object Synchronized : Kind
//  }
//
//  companion object {
//    /** TODO:doc.
//     *
//     * @param cfg TODO:doc
//     * @return TODO:doc
//     */
//    fun make(cfg: ControlFlowGraph): Structure {
//      // // This dummy works only on code with no loops, try/catches, or synchronized blocks
//       val backEdges = mutableSetOf<ControlFlowGraph.Edge>()
//
//      // // TODO: note that head block is present so we can always safely call
//      // // .head, but its headInsn is null so it doesn't match the first
//      // // instruction of the method
//
//       val nestingRoot: Nesting = listOf(Block(kind = null, headInsn = null, parent = null))
//      // val nestingMap = cfg.graph.vertexSet().asScala.map(_ -> nestingRoot).toMap
//      // //val nestingMap: Map[Insn, Nesting] = Map.empty[Insn, Nesting]
//      // //cfg.graph.vertexSet().asScala.map(_ -> nestingRoot).toMap
//
//      val dominatorTree = Dominator.dominatorTree(cfg.graph, cfg.entry)
//      val heads : Map<Insn, List<Kind>> = mutableMapOf()
//      // // Inner heads are dominated by outer heads.
//
//      val highestBlock : MutableMap<Insn, Block> = mutableMapOf()
//      val lowestBlock : MutableMap<Insn, Block> = mutableMapOf()
//      for (insn in DepthFirstIterator(dominatorTree, cfg.entry)) {
//         val backEdgeSet = cfg.graph.incomingEdgesOf(insn).filter { dominatorTree.dominatesSource(insn, it) }
//         if (backEdgeSet.isNotEmpty()) {
//           backEdges += backEdgeSet
//           val block = Block(Kind.Loop, insn)
//           fun addBlock(insn: Insn): Set<Insn> {
//             when (val oldBlock = highestBlock[insn]) {
//               block ->
//                 // TODO: idea: if we filter by domination of the head, we might allow multiple entry points
//                 if (oldBlock == block) { /* We've already processed this node */ return setOf()
//                 } else {
//                   oldBlock.parent = block
//                   highestBlock[insn] = block
//                   return setOf(oldBlock.headInsn)
//                 }
//
//               else -> {
//                 // We are first ones here, so setup both highest and lowest
//                 highestBlock[insn] = block
//                 lowestBlock[insn] = block
//                 return cfg.graph.incomingEdgesOf(insn).map { cfg.graph.getEdgeSource(it) }.toSet()
//               }
//
//             }
//           }
//
//           fun addBlockBackwards(insn: Insn): Unit {
//             val nextInsns = addBlock(insn)
//             nextInsns.forEach { addBlockBackwards(it) }
//           }
//           // val block = Block(Loop(), insn)
//           addBlock(insn)
//           backEdgeSet.forEach { addBlockBackwards(cfg.graph.getEdgeSource(it)) }
//         }
//              //return lowestBlock
//      }
//
//
//       return Structure(nestingMap, backEdges)
//      // /*
//      //     Loop heads dominate a predicesor
//      //     Loop tree based on Dominator tree?
//      //     Whole loop = all vertecies backwards from predicestor until loop head
//      //  */
//      // TODO()
//
//  }
//}

/** TODO:doc. */
object Exceptions {
  // def apply(cfg: ControlFlowGraph): Unit = {
  //   // TODO: check that handlers properly nest
  //   val union = new AsGraphUnion(cfg.graph, ???) // Edge from entry of try to each handler?
  //   val doms = Dominator.dominatorTree(union, cfg.entry)
  //   for (handler <- cfg.method.tryCatchBlocks.asScala) yield {
  //     val insns = new DepthFirstIterator[Insn, Dominator.Edge[Insn]](
  //       doms, Insn(cfg.method, handler.handler)).asScala.toList.sortBy(_.index)
  //     val indexes = insns.map(_.index)
  //     assert(indexes == (indexes.min to indexes.max).toList)
  //     handler -> insns
  //   }
  // }
}
