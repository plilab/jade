package org.ucombinator.jade.asm

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.* // ktlint-disable no-wildcard-imports
import org.objectweb.asm.util.Textifier
import org.objectweb.asm.util.TraceMethodVisitor
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.reflect.Modifier

data class Insn(val method: MethodNode, val insn: AbstractInsnNode) {
  fun index(): Int = method.instructions.indexOf(insn)
  fun next(): Insn = Insn(method, insn.next)
  fun shortString(): String = InsnTextifier.shortString(method, insn)
  fun longString(): String = InsnTextifier.longString(method, insn)
  override fun toString(): String = InsnTextifier.longString(method, insn)

  companion object InsnTextifier : Textifier(Opcodes.ASM9) {
    private val stringWriter = StringWriter()
    private val stringBuffer = stringWriter.buffer
    private val printWriter = PrintWriter(stringWriter)
    private val methodVisitor = TraceMethodVisitor(this)
    private var insnList: InsnList? = null

    fun shortString(method: MethodNode, insn: AbstractInsnNode): String =
      shortString(method.instructions, insn)

    fun shortString(insnList: InsnList, insn: AbstractInsnNode): String {
      // Ensure labels have the correct name
      if (insnList !== this.insnList) {
        this.insnList = insnList
        this.labelNames = java.util.HashMap()
        for (insn in insnList) {
          when (insn) {
            is LabelNode -> this.labelNames.put(insn.label, "L${insnList.indexOf(insn)}")
            else -> { /* Do nothing */ }
          }
        }
      }
      insn.accept(methodVisitor)
      print(printWriter)
      printWriter.flush()
      val string = stringBuffer.toString().trim()
      stringBuffer.setLength(0)
      this.text.clear()
      return string
    }

    fun longString(method: MethodNode, insn: AbstractInsnNode): String =
      longString(method.instructions, insn)

    fun longString(insnList: InsnList, insn: AbstractInsnNode): String {
      val index = insnList.indexOf(insn)
      val string = shortString(insnList, insn)
      val insnType = intToType.getValue(insn.type)
      val typeString =
        if (insnType.endsWith("INSN")) {
          insnType.replace("_INSN", "")
        } else {
          "*" + insnType
        }

      // Not currently needed but keep it around so we can find it again
      // val opcode = if (i.opcode == -1) { "no_opcode" } else Printer.OPCODES(i.opcode)

      return "$index:$string ($typeString)"
    }

    val typeToInt: Map<String, Int> =
      AbstractInsnNode::class.java.declaredFields
        .filter {
          // As of ASM 7.1, all final public static int members of AbstractInsNode are ones we want. Updates beware.
          it.type == Int::class.java && it.modifiers == (Modifier.FINAL or Modifier.PUBLIC or Modifier.STATIC)
        }
        .map {
          val x = (it.get(null) as Integer).toInt()
          Pair<String, Int>(it.name, x)
        }
        .toMap()

    val intToType: Map<Int, String> = typeToInt.toList().map { Pair(it.second, it.first) }.toMap()

    // // NOTE: valid only for Insn for the same method
    // implicit val ordering: Ordering[Insn] = new Ordering[Insn] {
    //   override def compare(x: Insn, y: Insn): Int = {
    //     assert(x.method eq y.method) // TODO: assert message or log message
    //     if ((x eq y) || (x.insn eq y.insn)) { 0 }
    //     else if (x.index < y.index) { -1 }
    //     else if (x.index > y.index) { 1 }
    //     else { Errors.fatal(f"Incomparable Insn ${x.longString} and ${y.longString}") }
    //   }
    // }
  }
}
