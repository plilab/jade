package org.ucombinator.jade.decompile

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.VarInsnNode
import org.ucombinator.jade.analysis.ControlFlowGraph
import org.ucombinator.jade.analysis.StaticSingleAssignment

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@Suppress("ktlint:standard:no-unit-return")
class DecompileIfTest {
  @Test
  fun ifEqNegatesBooleanOperand(): Unit {
    assertEquals("!parameterVar1", decompileCondition(Opcodes.IFEQ, "(Z)V"))
  }

  @Test
  fun ifNeUsesBooleanOperandDirectly(): Unit {
    assertEquals("parameterVar1", decompileCondition(Opcodes.IFNE, "(Z)V"))
  }

  @Test
  fun ifEqComparesIntegerOperandWithZero(): Unit {
    assertEquals("parameterVar1 == 0", decompileCondition(Opcodes.IFEQ, "(I)V"))
  }

  @Test
  fun ifNeComparesIntegerOperandWithZero(): Unit {
    assertEquals("parameterVar1 != 0", decompileCondition(Opcodes.IFNE, "(I)V"))
  }

  private fun decompileCondition(opcode: Int, descriptor: String): String {
    val owner = "example/Conditions"
    val target = LabelNode()
    val jump = JumpInsnNode(opcode, target)
    val method = MethodNode(
      Opcodes.ASM9,
      Opcodes.ACC_STATIC,
      "condition",
      descriptor,
      null,
      null,
    ).also {
      it.instructions.add(VarInsnNode(Opcodes.ILOAD, 0))
      it.instructions.add(jump)
      it.instructions.add(InsnNode(Opcodes.RETURN))
      it.instructions.add(target)
      it.instructions.add(InsnNode(Opcodes.RETURN))
      it.maxStack = 1
      it.maxLocals = 1
    }
    val cfg = ControlFlowGraph.make(owner, method)
    val ssa = StaticSingleAssignment.make(owner, method, cfg)
    val classNode = ClassNode(Opcodes.ASM9).also { it.name = owner }

    val (_, decompiled) = DecompileInsn.decompileInsn(jump, ssa, classNode)
    return assertIs<DecompiledInsn.If>(decompiled).condition.toString()
  }
}
