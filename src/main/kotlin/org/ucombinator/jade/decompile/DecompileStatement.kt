package org.ucombinator.jade.decompile

import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.expr.BooleanLiteralExpr
import com.github.javaparser.ast.expr.VariableDeclarationExpr
import com.github.javaparser.ast.stmt.*
import javassist.bytecode.analysis.ControlFlow.Block
import org.jgrapht.graph.AsSubgraph
import org.jgrapht.graph.MaskSubgraph
import org.objectweb.asm.tree.LabelNode
import org.ucombinator.jade.analysis.*
import org.ucombinator.jade.asm.Insn
import org.ucombinator.jade.classfile.Descriptor
import org.ucombinator.jade.javaparser.JavaParser
import org.ucombinator.jade.jgrapht.dominator.Dominator
import org.ucombinator.jade.util.Errors

// import org.ucombinator.jade.decompile.*
// import com.github.javaparser.ast.body.VariableDeclarator
// import com.github.javaparser.ast.comments.BlockComment
// import com.github.javaparser.ast.type.PrimitiveType
// import org.ucombinator.jade.util.Log
// import com.github.javaparser.ast.type.Type as JavaParserType
// import org.objectweb.asm.Type as AsmType

/*

Example Output:

L1: { L2: { L3: { // <- block
  stmt1
  if (...) break L2;
  stmt2
  if (...) break L3;
  } // end L3
  stmt // target of break L3;
} // end L2
  stmt 3 // target of break L2;

General structure:

{
  {
    {
      stmt
    }
    stmt
  }
  stmt
}

*/

/** TODO:doc. */
object DecompileStatement {
  /*
  As long as one is jumping forwards, we can always encode as a sequence of breaks
  Use topo-sort with a sort order that groups loop heads with their body

  use stack (recursion) of zero-in-degree vertices to group loops
  is it a loop head, which loop head is this part of
   */

  /** TODO:doc.
   *
   * @param cfg TODO:doc
   * @param ssa TODO:doc
   * @param structure TODO:doc
   * @return TODO:doc
   */
  fun make(cfg: ControlFlowGraph, ssa: StaticSingleAssignment, structure: Loops): BlockStmt {
    // TODO: check for SCCs with multiple entry points
    // TODO: LocalClassDeclarationStmt
    val jumpTargets = cfg.graph // TODO: rename to insnOfLabel
      .vertexSet()
      .flatMap { if (it.insn is LabelNode) setOf(it.insn.label to it) else setOf() }
      .toMap()

    // TODO: remove back edges
    val graph = AsSubgraph(MaskSubgraph(cfg.graph, { false }, structure.backEdges::contains))

    fun labelString(label: LabelNode): String = "JADE_${jumpTargets.getValue(label.label).index()}"
    fun endLabelString(label: LabelNode): String = "End of JADE_${jumpTargets.getValue(label.label).index()}"

    fun insnLabelString(insn: Insn): String = "JADE_${insn.index()}" // TODO: overload with labelString

    fun structuredBlock(head: Insn): Pair<Statement, /* pendingOutside */ Set<Insn>> {
      // do statements in instruction order if possible
      // constraints (loops *must* be together):
      // 1. Respect edges
      // 2. Loop instructions must be together (thus avoid exiting loop)
      // 3. Pick following instruction if possible and not goto.
      //    Otherwise, pick the smallest instruction.
      //
      // Any instruction could require a "break" or "continue" attached to it.
      // Only loops are allowed to be continue targets.

      // val headStructure = structure.nesting.getValue(head)

      // worklist of vertexes with no more incoming edges that are inside the current loop (back edges do not count)
      // NOTE: We use TreeSet so we have `minOption()`
      // var pendingInside = TreeSet<Insn>()
      var pendingInside = sortedSetOf<Insn>()

      // worklist of vertexes with no more incoming edges that are outside the current loop (back edges do not count)
      var pendingOutside = setOf<Insn>()

      fun addPending(insns: Set<Insn>) {
        for (insn in insns) {
          assert(graph.inDegreeOf(insn) == 0)
          if (structure.nesting.containsVertex(insn)) { // (MyersList.partialOrdering.gteq(structure.nesting.getValue(insn), headStructure)) {
            pendingInside += insn
          } else {
            pendingOutside += insn
          }
        }
      }

      fun removeOutEdges(insn: Insn): Set<Insn> {
        val outEdges = graph.outgoingEdgesOf(insn)
        val targets = outEdges.map(graph::getEdgeTarget)
        outEdges.forEach(graph::removeEdge)
        addPending(targets.filter { graph.inDegreeOf(it) == 0 }.toSet())
        return outEdges.map(graph::getEdgeTarget).toSet()
      }

      fun simpleStmt(insn: Insn): Statement {
        // ASSUMPTION: we ignore allocs but implement the constructors
        val (retVal, decompiled) = DecompileInsn.decompileInsn(insn.insn, ssa)
        return when (decompiled) {
          is DecompiledInsn.If -> {
            // log.debug { "IF: " + decompiled.labelNode + "///" + decompiled.labelNode.getLabel }
            IfStmt(decompiled.condition, BreakStmt(labelString(decompiled.labelNode)), null)
          }
          is DecompiledInsn.Goto -> ContinueStmt(labelString(decompiled.labelNode)) // TODO: use instruction number?
          else -> DecompileInsn.decompileInsn(retVal, decompiled, ssa)
        }
        // TODO: break vs continue
        // TODO: labels in break or continue
      }
      // TODO: explicitly labeled instructions

      // ASSUMPTION: structured statements have a single entry point
      fun structuredStmt(insn: Insn): Statement {
        //TODO()
        val block = structure.nesting.edgesOf(insn).first()
        val insnIsALoopHead = cfg.graph.incomingEdgesOf(insn).any { structure.backEdges.contains(it) }
        return if (insnIsALoopHead) { // insn is the head of a structured statement
          // TODO: multiple nested structures starting at same place (for now assume everything is a loop)
          val (stmt, newPending) = structuredBlock(insn)
          addPending(newPending)
          // when (block.kind) {
          //   is Loops.Kind.Loop -> {
              val label = labelString(insn.insn as LabelNode) // TODO: do better
              LabeledStmt(label, WhileStmt(BooleanLiteralExpr(true), stmt))
          //   }
          //   is Loops.Kind.Exception -> TODO()
          //   is Loops.Kind.Synchronized -> TODO()
          // }
        } else {
          simpleStmt(insn)
        }
      }

      var currentInsn: Insn? = head
      var currentStmt: Statement = simpleStmt(currentInsn!!)

      // If the next instruction is an outgoing edge via normal control,
      //   if it is available, use it
      //   otherwise, insert a 'break' ('continue' is impossible since we are looking at the next instruction),
      //     then do part two
      // If the next instruction is not an outgoing edge,
      //   use the smallest available
      fun getNextInsn(): Insn? {
        // TODO: switch?
        // TODO: constructor?
        pendingInside.remove(currentInsn)
        val outEdges = removeOutEdges(currentInsn!!)
        val (_, decompiled) = DecompileInsn.decompileInsn(currentInsn!!.insn, ssa)
        val next = currentInsn?.next()
        val insnIsALoopHead = cfg.graph.incomingEdgesOf(currentInsn).any { structure.backEdges.contains(it) }
        //next?.let{ println("curr insn: " + insnLabelString(currentInsn!!) + ", next insn: " + insnLabelString(it)) } // debug
        return when {
          pendingInside.isEmpty() -> {
            // Nothing left to process
            null
          }
          decompiled.usesNextInsn && pendingInside.contains(next) -> {
            // Process the sequentially next instruction if we can
            assert(outEdges.contains(next)) { "next not in out edges of currentInsn: currentInsn=${currentInsn} next=${next} outEdges=${outEdges} pendingInside=${pendingInside}" }
            next
          }
          else -> {
            // Otherwise, take the smallest available instruction
            if (decompiled.usesNextInsn && !insnIsALoopHead) { // don't do on continues, breaks, etc.
              // NOTE [Branch Targets]:
              // We can't go to the sequencially next instruction (probably due to a CFG dependency)
              // so we insert a `break` and pick the next instruction that we can go to.
              // This line is why every statement is a potential break target.
              currentStmt = BlockStmt(NodeList<Statement>(currentStmt, BreakStmt(insnLabelString(next!!))))
            }
            pendingInside.first()
          }
        }
      }

      while (true) {
        var prevInsn: Insn? = currentInsn
        currentInsn = getNextInsn()
        if (currentInsn == null) { break }
        val insnIsALoopHead = cfg.graph.incomingEdgesOf(currentInsn).any { structure.backEdges.contains(it) }
        if (insnIsALoopHead) {
          // Don't rewrap in a labeled BlockStmt to allow for labeled while loop for continue to work
          currentStmt = BlockStmt(NodeList<Statement>(currentStmt, structuredStmt(currentInsn)))
        } else {
          currentStmt = LabeledStmt(
            insnLabelString(currentInsn),
            BlockStmt(NodeList<Statement>(currentStmt, structuredStmt(currentInsn)))
          )
        }

      }
      return Pair(currentStmt, pendingOutside)
    }

    val (stmt, pendingOutside) = structuredBlock(cfg.entry)
    assert(pendingOutside.isEmpty())
    val variables = ssa.insnVars.values.map(Pair<Var, List<Var>>::first) + ssa.phiInputs.keys

    fun decompileVarDecl(v: Var): Statement =
      // TODO: handle the mutability and nullability of v.basicValue in a better way
      v.basicValue.let { basicValue ->
        // TODO: modifiers
        if (basicValue == null || basicValue.type == null) {
          JavaParser.noop(v.toString()) // commented out var here
        } else {
          val t = Descriptor.fieldDescriptor(basicValue.type.descriptor)
          ExpressionStmt(VariableDeclarationExpr(t, v.name))
        }
      }

    val declarations = variables.map(::decompileVarDecl)

    val statements = NodeList<Statement>(declarations.toList())
    statements.add(stmt)
    val stmt2 = BlockStmt(statements)
    return stmt2
  }


}
