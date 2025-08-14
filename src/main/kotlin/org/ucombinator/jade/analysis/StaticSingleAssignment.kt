package org.ucombinator.jade.analysis

import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.TryCatchBlockNode
import org.objectweb.asm.tree.analysis.Analyzer
import org.objectweb.asm.tree.analysis.AnalyzerException
import org.objectweb.asm.tree.analysis.BasicValue
import org.objectweb.asm.tree.analysis.Frame
import org.objectweb.asm.tree.analysis.Interpreter
import org.ucombinator.jade.asm.Insn
import org.ucombinator.jade.asm.TypedBasicInterpreter
import org.ucombinator.jade.util.Errors

/** Represents the result of static single assignment (SSA) analysis.
 *
 * @property frames An array of ASM `Frame` objects representing states of variables.
 * @property insnVars A map that represents instruction nodes. Its keys are the instruction nodes and values are a pair
 *   whose first element is the variable and second element the list of versions of that variable.
 * @property phiInputs A map that represents Phi nodes. Its keys are target variables of the phi nodes and values are
 *   the set of (instruction, variable) pairs that are inputs to the Phi function.
 */
data class StaticSingleAssignment(
  val frames: Array<Frame<Var>>,
  val insnVars: Map<AbstractInsnNode, Pair<Var, List<Var>>>,
  val phiInputs: Map<Var, Set<Pair<AbstractInsnNode, Var?>>>, // TODO: change Set to Map? 
  // TODO: change to phiInputs: Map<Var.Phi, Set<Var?>>
) {
  companion object {
    /** Performs a static single assignment (SSA) analysis on the given method and returns the result.
     *
     * @param owner The name of the class that owns the method.
     * @param method The method to analyze.
     * @param cfg The control flow graph of the method.
     * @return The result of the SSA analysis, represented as a [StaticSingleAssignment] object.
     */
    fun make(owner: String, method: MethodNode, cfg: ControlFlowGraph): StaticSingleAssignment {
      val interpreter = SsaInterpreter(method)

      // Hook into a method that is called whenever `analyze` starts working on a new instruction
      val oldInstructions = method.instructions
      method.instructions = object : InsnList() {
        override fun get(index: Int): AbstractInsnNode {
          val insn = super.get(index)
          interpreter.copyOperationPosition = 0
          interpreter.originInsn = insn
          return insn
        }
      }

      for (i in oldInstructions) {
        method.instructions.add(i)
      }

      val frames = SsaAnalyzer(cfg, interpreter).analyze(owner, method)

      return StaticSingleAssignment(frames, interpreter.insnVars, interpreter.phiInputs)
    }
  }

  // Immutable reverse lookup index computed from `phiInputs` at construction time
  private val reverseLookupIndex: Map<Var, Set<Var>> = run {
    // Edges from source variable to dependent phi variable
    val edges = phiInputs.asSequence().flatMap { (phiIn, phiOutPairs) ->
      phiOutPairs.asSequence().mapNotNull { (_, phiOut) -> phiOut?.let { it to phiIn } }
    }

    // Group dependents by source and convert to Set to avoid duplicates
    edges.groupBy(keySelector = { it.first }, valueTransform = { it.second })
      .mapValues { (_, dependents) -> dependents.toSet() }
  }

  fun reverseLookup(variable: Var): Set<Var> {
    return reverseLookupIndex[variable] ?: emptySet()
  }
}

// TODO: maybe handle `this` var specially (i.e., no phivar)
// TODO: extend TypedBasicInterpreter?

/** TODO:doc.
 *
 * @property method TODO:doc
 */
private class SsaInterpreter(val method: MethodNode) : Interpreter<Var>(Opcodes.ASM9) {
  // Variables to be put in output
  val insnVars = mutableMapOf<AbstractInsnNode, Pair<Var, List<Var>>>()
  val phiInputs = mutableMapOf<Var, Set<Pair<AbstractInsnNode, Var?>>>()

  // Other bookkeeping variables
  var copyOperationPosition: Int = 0 // For `copyOperation()`
  var originInsn: AbstractInsnNode? = null // For `merge`
  var returnTypeValue: Var.Return? = null // There is no getReturn method on frames, so we save it here

  /** TODO:doc.
   *
   * @param key TODO:doc
   * @param insn TODO:doc
   * @param value TODO:doc
   * @param ignoreNull TODO:doc
   */
  fun phiInputs(key: Var.Phi, insn: AbstractInsnNode?, value: Var?, ignoreNull: Boolean = false) {
    if (!ignoreNull || value != null) {
      val usedKey = key.change()
      val entry = insn!! to value
      this.phiInputs.put(usedKey, this.phiInputs.getOrElse(usedKey, { setOf() }).plus(entry))
    }
  }

  override fun newValue(type: Type): Var = Errors.fatal("Impossible call of newValue on $type")

  override fun newParameterValue(isInstanceMethod: Boolean, local: Int, type: Type): Var =
    Var.Parameter(TypedBasicInterpreter.newValue(type)!!, local)

  override fun newReturnTypeValue(type: Type): Var? {
    // ASM requires that we return null when type is Type.VOID_TYPE
    this.returnTypeValue =
      if (type == Type.VOID_TYPE) null else Var.Return(TypedBasicInterpreter.newReturnTypeValue(type))
    return this.returnTypeValue
  }

  override fun newEmptyValue(local: Int): Var = Var.Empty

  override fun newExceptionValue(
    tryCatchBlockNode: TryCatchBlockNode,
    handlerFrame: Frame<Var>,
    exceptionType: Type,
  ): Var =
    Var.Exception(
      // TODO: wrong cast?
      TypedBasicInterpreter.newExceptionValue(tryCatchBlockNode, handlerFrame as Frame<BasicValue>, exceptionType),
      Insn(method, tryCatchBlockNode.handler),
    )

  /** TODO:doc.
   *
   * @param insn TODO:doc
   * @param args TODO:doc
   * @param ret TODO:doc
   * @return TODO:doc
   */
  fun record(insn: AbstractInsnNode, args: List<Var>, ret: Var): Var {
    this.insnVars.put(insn, Pair(ret, args))
    return ret
  }

  // TODO: do we need @Throws?
  @Throws(AnalyzerException::class)
  override fun newOperation(insn: AbstractInsnNode): Var =
    record(insn, listOf(), Var.Instruction(TypedBasicInterpreter.newOperation(insn), Insn(method, insn)))

  @Throws(AnalyzerException::class)
  override fun copyOperation(insn: AbstractInsnNode, value: Var): Var {
    this.copyOperationPosition += 1
    return record(
      insn,
      listOf(value),
      Var.Copy(
        TypedBasicInterpreter.copyOperation(insn, value.basicValue),
        Insn(method, insn),
        this.copyOperationPosition,
      ),
    )
  }

  @Throws(AnalyzerException::class)
  override fun unaryOperation(insn: AbstractInsnNode, value: Var): Var =
    record(
      insn,
      listOf(value),
      Var.Instruction(TypedBasicInterpreter.unaryOperation(insn, value.basicValue), Insn(method, insn)),
    )

  @Throws(AnalyzerException::class)
  override fun binaryOperation(insn: AbstractInsnNode, value1: Var, value2: Var): Var =
    record(
      insn,
      listOf(value1, value2),
      Var.Instruction(
        TypedBasicInterpreter.binaryOperation(insn, value1.basicValue, value2.basicValue),
        Insn(method, insn),
      ),
    )

  @Throws(AnalyzerException::class)
  override fun ternaryOperation(insn: AbstractInsnNode, value1: Var, value2: Var, value3: Var): Var =
    record(
      insn,
      listOf(value1, value2, value3),
      Var.Instruction(
        TypedBasicInterpreter.ternaryOperation(insn, value1.basicValue, value2.basicValue, value3.basicValue),
        Insn(method, insn),
      ),
    )

  @Throws(AnalyzerException::class)
  override fun naryOperation(insn: AbstractInsnNode, values: MutableList<out Var>): Var =
    record(
      insn,
      values,
      Var.Instruction(
        TypedBasicInterpreter.naryOperation(insn, values.map { it.basicValue }),
        Insn(method, insn),
      ),
    )

  @Throws(AnalyzerException::class)
  override fun returnOperation(insn: AbstractInsnNode, value: Var, expected: Var) {
    // TODO: capture `expected` Var somehow
    // Note that `unaryOperation` is also called whenever `returnOperation` is called.
    // We override the effect of `unaryOperation` by calling `record` with `null` here.
    // TODO: explain why we do not do this
    // TypedBasicInterpreter.returnOperation(insn, value.basicValue, expected.basicValue)
    // record(insn, List(value), null)
  }

  override fun merge(value1: Var, value2: Var): Var {
    when {
      value1 is Var.Phi -> {
        val newValue1 = value1.change()
        phiInputs(newValue1, this.originInsn, value2)
        return newValue1
      }
      value1 === Var.Empty -> return value2
      value2 === Var.Empty -> return value1
      value1 === value2 -> return value1
      else -> throw Exception("unexpected merge: value1: $value1 value2: $value2")
    }
  }
}

/** TODO:doc.
 *
 * @property cfg TODO:doc
 * @property interpreter TODO:doc
 */
private class SsaAnalyzer(val cfg: ControlFlowGraph, val interpreter: SsaInterpreter) : Analyzer<Var>(interpreter) {
  override fun init(owner: String, method: MethodNode) {
    // We override this method because it runs near the start of `Analyzer.analyze`
    // but after the `Analyzer.frames` array is created.
    //
    // We use this method to:
    //  (1) initialize the frames at join points and
    //  (2) set the returnValue of each frame (as it is not updated by `Frame.merge`)
    //
    // We cannot do (1) in `merge` as all merged-in values need to know what instruction they
    // came from.  By the time `merge` runs, that information for the first value is gone.
    for (insn in method.instructions) {
      val insnIndex = method.instructions.indexOf(insn)
      val minimumInEdges = if (insnIndex == 0) 0 else 1
      if (cfg.graph.incomingEdgesOf(Insn(method, insn)).size > minimumInEdges ||
        method.tryCatchBlocks.any { it.handler === insn }
      ) {
        // We are at a join point
        val cfgFrame = cfg.frames[insnIndex]
        val frame = this.frames[insnIndex] ?: Frame<Var>(cfgFrame.locals, cfgFrame.maxStackSize)
        // Note that Frame.returnValue is null until `frame.setReturn` later in this method
        for (i in 0 until cfgFrame.locals) {
          assert((insnIndex == 0) == (frame.getLocal(i) != null))
          val phi = Var.Phi(cfgFrame.getLocal(i), Insn(method, insn), i) // Note: not `.used`
          this.interpreter.phiInputs(phi.change(), this.interpreter.originInsn, frame.getLocal(i), true)
          frame.setLocal(i, phi)
        }
        // Note that we use `push` instead of `setStack` as the `Frame` constructor
        // starts with an empty stack regardless of `stackSize`
        assert(frame.stackSize == 0)
        for (i in 0 until cfgFrame.stackSize) {
          assert((insnIndex == 0) == (frame.getStack(i) != null))
          val phi = Var.Phi(cfgFrame.getStack(i), Insn(method, insn), i + frame.locals)
          this.interpreter.phiInputs(phi.change(), this.interpreter.originInsn, frame.getStack(i), true)
          frame.push(phi)
        }
        this.frames[insnIndex] = frame
      }
    }

    // Set the `Frame.returnValue` as it is not updated by `Frame.merge`.
    // This gets passed as to `returnOperation` as `expected`.
    for (frame in this.frames) {
      // Unreachable code has null frames, so skip those
      frame?.setReturn(interpreter.returnTypeValue)
    }
  }
}
