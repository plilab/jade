package org.ucombinator.jade.analysis

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.analysis.BasicValue
import org.ucombinator.jade.asm.Insn

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertSame

class VarTest {
  private fun instruction(opcode: Int): Insn {
    val method = MethodNode()
    val node = InsnNode(opcode)
    method.instructions.add(node)
    return Insn(method, node)
  }

  @Test
  fun copyUsesSourceName() {
    val source = Var.Parameter(BasicValue.INT_VALUE, 0, false)
    val copy = Var.Copy(source.basicValue, instruction(Opcodes.ILOAD), 1, source)
    val nestedCopy = Var.Copy(copy.basicValue, instruction(Opcodes.ISTORE), 1, copy)

    assertEquals(source.name, copy.name)
    assertEquals(source.name, nestedCopy.name)
    assertSame(source, copy.source)
  }

  /*
   * void increment(int amount) {
   *   this.value += amount; // javac duplicates `this` with DUP.
   * }
   */
  @Test
  fun copyOccurrenceDistinguishesDefinitions() {
    val source = Var.Parameter(BasicValue.INT_VALUE, 0, false)
    val insn = instruction(Opcodes.DUP)

    val firstCopy = Var.Copy(source.basicValue, insn, 1, source)
    val repeatedFirstCopy = Var.Copy(source.basicValue, insn, 1, source)
    val secondCopy = Var.Copy(source.basicValue, insn, 2, source)

    assertEquals(firstCopy, repeatedFirstCopy)
    assertNotEquals(firstCopy, secondCopy)
    assertEquals(firstCopy.name, secondCopy.name)
  }

  /*
   * int getAndIncrement() {
   *   return this.value++; // javac uses DUP_X1 to move different input values.
   * }
   */
  @Test
  fun copySourceDistinguishesDefinitions() {
    val firstSource = Var.Parameter(BasicValue.INT_VALUE, 0, false)
    val secondSource = Var.Parameter(BasicValue.INT_VALUE, 1, false)
    val insn = instruction(Opcodes.DUP_X1)

    val firstCopy = Var.Copy(firstSource.basicValue, insn, 1, firstSource)
    val secondCopy = Var.Copy(secondSource.basicValue, insn, 1, secondSource)

    assertNotEquals(firstCopy, secondCopy)
    assertEquals(firstSource.name, firstCopy.name)
    assertEquals(secondSource.name, secondCopy.name)
  }
}
