package org.ucombinator.jade.analysis

import org.objectweb.asm.*
import org.objectweb.asm.tree.*
import org.objectweb.asm.tree.analysis.*
import org.ucombinator.jade.asm.Insn
import org.ucombinator.jade.asm.TypedBasicInterpreter
import org.ucombinator.jade.analysis.ControlFlowGraph
import org.ucombinator.jade.util.Errors
import org.ucombinator.jade.util.Log
import org.ucombinator.jade.util.Logger

data class StaticSingleAssignment(
    val frames: Array<Frame<Var>>,
    val insnVars: Map<AbstractInsnNode, Pair<Var, List<Var>>>,
    val phiInputs: Map<Var, Set<Pair<AbstractInsnNode, Var?>>>,
)

fun staticSingleAssignment(owner: String, method: MethodNode, cfg: ControlFlowGraph): StaticSingleAssignment {
  val interpreter = SSAInterpreter(method)

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

  val frames = SSAAnalyzer(cfg, interpreter).analyze(owner, method)

  return StaticSingleAssignment(frames, interpreter.insnVars, interpreter.phiInputs)
}

// TODO: maybe handle `this` var specially (i.e., no phivar)
// TODO: extend TypedBasicInterpreter?
private class SSAInterpreter(val method: MethodNode) : Interpreter<Var>(Opcodes.ASM9), Log by Logger() {
  // Variables to be put in output
  val insnVars = mutableMapOf<AbstractInsnNode, Pair<Var, List<Var>>>()
  val phiInputs = mutableMapOf<Var, Set<Pair<AbstractInsnNode, Var?>>>()
  // Other bookkeeping variables
  var copyOperationPosition: Int = 0 // For `copyOperation()`
  var originInsn: AbstractInsnNode? = null // For `merge`
  var returnTypeValue: ReturnVar? = null // There is no getReturn method on frames, so we save it here

  fun phiInputs(key: PhiVar, insn: AbstractInsnNode?, value: Var?, ignoreNull: Boolean = false): Unit {
    if (!ignoreNull || value != null) {
      val usedKey = key.change()
      val entry = Pair(insn!!, value)
      this.phiInputs.put(usedKey, this.phiInputs.getOrElse(usedKey, { setOf() }).plus(entry))
    }
  }

  override fun newValue(`type`: Type): Var = Errors.fatal("Impossible call of newValue on ${`type`}")

  override fun newParameterValue(isInstanceMethod: Boolean, local: Int, `type`: Type): Var =
    ParameterVar(TypedBasicInterpreter.newValue(`type`)!!, local)

  override fun newReturnTypeValue(`type`: Type): Var? {
    // ASM requires that we return null when `type` is Type.VOID_TYPE
    this.returnTypeValue =
      if (`type` == Type.VOID_TYPE) { null }
      else { ReturnVar(TypedBasicInterpreter.newReturnTypeValue(`type`)) }
    return this.returnTypeValue
  }

  override fun newEmptyValue(local: Int): Var =
    EmptyVar

  override fun newExceptionValue(
      tryCatchBlockNode: TryCatchBlockNode,
      handlerFrame: Frame<Var>,
      exceptionType: Type
  ): Var =
    ExceptionVar(
      TypedBasicInterpreter
        .newExceptionValue(tryCatchBlockNode, handlerFrame as Frame<BasicValue>, exceptionType), // TODO: wrong cast?
      Insn(method, tryCatchBlockNode.handler)
    )

  fun record(insn: AbstractInsnNode, args: List<Var>, ret: Var): Var {
    this.insnVars.put(insn, Pair(ret, args))
    return ret
  }

  @Throws(AnalyzerException::class)
  override fun newOperation(insn: AbstractInsnNode): Var =
    record(insn, listOf(), InstructionVar(TypedBasicInterpreter.newOperation(insn), Insn(method, insn)))

  @Throws(AnalyzerException::class)
  override fun copyOperation(insn: AbstractInsnNode, value: Var): Var {
    this.copyOperationPosition += 1
    return record(
      insn,
      listOf(value),
      CopyVar(
        TypedBasicInterpreter.copyOperation(insn, value.basicValue),
        Insn(method, insn),
        this.copyOperationPosition
      )
    )
  }

  @Throws(AnalyzerException::class)
  override fun unaryOperation(insn: AbstractInsnNode, value: Var): Var =
    record(
      insn,
      listOf(value),
      InstructionVar(TypedBasicInterpreter.unaryOperation(insn, value.basicValue), Insn(method, insn))
    )

  @Throws(AnalyzerException::class)
  override fun binaryOperation(insn: AbstractInsnNode, value1: Var, value2: Var): Var =
    record(
      insn,
      listOf(value1, value2),
      InstructionVar(
        TypedBasicInterpreter.binaryOperation(insn, value1.basicValue, value2.basicValue),
        Insn(method, insn)
      )
    )

  @Throws(AnalyzerException::class)
  override fun ternaryOperation(insn: AbstractInsnNode, value1: Var, value2: Var, value3: Var): Var =
    record(
      insn,
      listOf(value1, value2, value3),
      InstructionVar(
        TypedBasicInterpreter.ternaryOperation(insn, value1.basicValue, value2.basicValue, value3.basicValue),
        Insn(method, insn)
      )
    )

  // override fun naryOperation(insn: AbstractInsnNode, values: (MutableList<out Var!>..List<Var!>?)): Var!
  @Throws(AnalyzerException::class)
  // override fun naryOperation(insn: AbstractInsnNode, values: java.util.List<out Var>): Var {
  override fun naryOperation(insn: AbstractInsnNode, values: MutableList<out Var>): Var =
    record(
      insn,
      values,
      InstructionVar(
        TypedBasicInterpreter.naryOperation(insn, values.map { it.basicValue }),
        Insn(method, insn)
      )
    )

  @Throws(AnalyzerException::class)
  override fun returnOperation(insn: AbstractInsnNode, value: Var, expected: Var): Unit =
    // TODO: capture `expected` Var somehow
    // Note that `unaryOperation` is also called whenever `returnOperation` is called.
    // We override the effect of `unaryOperation` by calling `record` with `null` here.
    // TODO: explain why we do not do this
    //TypedBasicInterpreter.returnOperation(insn, value.basicValue, expected.basicValue)
    //record(insn, List(value), null)
    Unit

  override fun merge(value1: Var, value2: Var): Var {
    when {
      value1 is PhiVar -> {
        val newValue1 = value1.change()
        phiInputs(newValue1, this.originInsn, value2)
        return newValue1
      }
      value1 === EmptyVar -> return value2
      value2 === EmptyVar -> return value1
      value1 === value2   -> return value1
      else -> throw Exception("unexpected merge: value1: ${value1} value2: ${value2}")
    }
  }
}

private class SSAAnalyzer(val cfg: ControlFlowGraph, val interpreter: SSAInterpreter) : Analyzer<Var>(interpreter), Log by Logger() {

  override fun init(owner: String, method: MethodNode): Unit {
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
      val minimumInEdges =
        if (insnIndex == 0) { 0 }
        else { 1 }
      if (
        cfg.graph.incomingEdgesOf(Insn(method, insn)).size > minimumInEdges
        || method.tryCatchBlocks.any({ p -> p.handler === insn })
      ) {
        // We are at a join point
        val cfgFrame = cfg.frames.get(insnIndex)
        val frame =
          this.getFrames().get(insnIndex) ?:
          Frame<Var>(cfgFrame.getLocals(), cfgFrame.getMaxStackSize())
        // Note that Frame.returnValue is null until `frame.setReturn` later in this method
        for (i in 0..cfgFrame.getLocals()) {
          assert((insnIndex == 0) == (frame.getLocal(i) != null))
          val phiVar = PhiVar(cfgFrame.getLocal(i), Insn(method, insn), i) // Note: not `.used`
          this.interpreter.phiInputs(phiVar.change(), this.interpreter.originInsn, frame.getLocal(i), true)
          frame.setLocal(i, phiVar)
        }
        // Note that we use `push` instead of `setStack` as the `Frame` constructor
        // starts with an empty stack regardless of `stackSize`
        assert(frame.getStackSize() == 0)
        for (i in 0..cfgFrame.getStackSize()) {
          assert((insnIndex == 0) == (frame.getStack(i) != null))
          val phiVar = PhiVar(cfgFrame.getStack(i), Insn(method, insn), i + frame.getLocals())
          this.interpreter.phiInputs(phiVar.change(), this.interpreter.originInsn, frame.getStack(i), true)
          frame.push(phiVar)
        }
        this.getFrames().set(insnIndex, frame)
      }
    }

    // Set the `Frame.returnValue` as it is not updated by `Frame.merge`.
    // This gets passed as to `returnOperation` as `expected`.
    for (frame in this.getFrames()) {
      // Unreachable code has null frames, so skip those
      if (frame != null) {
        frame.setReturn(interpreter.returnTypeValue)
      }
    }
  }
}
