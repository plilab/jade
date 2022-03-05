package org.ucombinator.jade.asm

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*
import org.objectweb.asm.util.Textifier
import org.objectweb.asm.util.TraceMethodVisitor
import org.ucombinator.jade.util.Errors
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.reflect.Modifier

data class Insn(val method: MethodNode, val insn: AbstractInsnNode) : Comparable<Insn> {
  fun index(): Int = method.instructions.indexOf(insn)
  fun next(): Insn = Insn(method, insn.next)
  fun shortString(): String = InsnTextifier.shortString(method, insn)
  fun longString(): String = InsnTextifier.longString(method, insn)
  override fun toString(): String = InsnTextifier.longString(method, insn)

  // NOTE: valid only for Insn for the same method
  override fun compareTo(other: Insn): Int = run {
    assert(this.method === other.method) // TODO: assert message or log message
    when {
      this === other || this.insn === other.insn -> 0
      this.index() < other.index() -> -1
      this.index() > other.index() -> 1
      else -> Errors.fatal("Incomparable Insn ${this.longString()} and ${other.longString()}")
    }
  }

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
          if (insn is LabelNode) {
            this.labelNames.put(insn.label, "L${insnList.indexOf(insn)}")
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
          "*$insnType"
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

    val intToType: Map<Int, String> = typeToInt.toList().map { it.second to it.first }.toMap()
  }
}
