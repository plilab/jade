package org.ucombinator.jade.asm

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.util.Textifier
import org.objectweb.asm.util.TraceMethodVisitor
import org.ucombinator.jade.util.Errors
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.reflect.Modifier

/** Represents a single instruction in a method. Wrapper for `org.objectweb.asm.tree.AbstractInsnNode`.
 *
 * @property method The method that this instruction belongs to.
 * @property insn The actual instruction.
 */
data class Insn(val method: MethodNode, val insn: AbstractInsnNode) : Comparable<Insn> {
  /** TODO:doc.
   *
   * @return TODO:doc
   */
  fun index(): Int = method.instructions.indexOf(insn)

  /** TODO:doc.
   *
   * @return TODO:doc
   */
  fun next(): Insn = Insn(method, insn.next)

  /** TODO:doc.
   *
   * @return TODO:doc
   */
  fun shortString(): String = InsnTextifier.shortString(method, insn)

  /** TODO:doc.
   *
   * @return TODO:doc
   */
  fun longString(): String = InsnTextifier.longString(method, insn)

  override fun toString(): String = InsnTextifier.longString(method, insn)

  // NOTE: valid only for Insn for the same method
  override fun compareTo(other: Insn): Int {
    assert(this.method === other.method) // TODO: assert message or log message
    return when {
      this === other || this.insn === other.insn -> 0
      this.index() < other.index() -> -1
      this.index() > other.index() -> 1
      else -> Errors.fatal("Incomparable Insn ${this.longString()} and ${other.longString()}")
    }
  }

  /** TODO:doc. */
  companion object InsnTextifier : Textifier(Opcodes.ASM9) {
    /** TODO:doc. */
    private val stringWriter = StringWriter()

    /** TODO:doc. */
    private val stringBuffer = stringWriter.buffer

    /** TODO:doc. */
    private val printWriter = PrintWriter(stringWriter)

    /** TODO:doc. */
    private val methodVisitor = TraceMethodVisitor(this)

    /** TODO:doc. */
    private var insnList: InsnList? = null

    /** TODO:doc.
     *
     * @param method TODO:doc
     * @param insn TODO:doc
     * @return TODO:doc
     */
    fun shortString(method: MethodNode, insn: AbstractInsnNode): String = shortString(method.instructions, insn)

    /** TODO:doc.
     *
     * @param insnList TODO:doc (TODO: rename to insns?)
     * @param insn TODO:doc
     * @return TODO:doc
     */
    fun shortString(insnList: InsnList, insn: AbstractInsnNode): String {
      // Ensure labels have the correct name
      if (insnList !== this.insnList) {
        this.insnList = insnList
        this.labelNames = mutableMapOf()
        for (insn in insnList) {
          if (insn is LabelNode) {
            this.labelNames.put(insn.label, "L${insnList.indexOf(insn)}")
          }
        }
      }
      insn.accept(methodVisitor)
      @Suppress("DEBUG_PRINT") // this print is to printWriter, not stdout
      print(printWriter)
      printWriter.flush()
      val string = stringBuffer.toString().trim()
      stringBuffer.setLength(0)
      this.text.clear()
      return string
    }

    /** TODO:doc.
     *
     * @param method TODO:doc
     * @param insn TODO:doc
     * @return TODO:doc
     */
    fun longString(method: MethodNode, insn: AbstractInsnNode): String = longString(method.instructions, insn)

    /** TODO:doc.
     *
     * @param insnList TODO:doc (TODO: rename to insns?)
     * @param insn TODO:doc
     * @return TODO:doc
     */
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

    /** TODO:doc. */
    private val typeToInt: Map<String, Int> = AbstractInsnNode::class.java.declaredFields.filter {
      // As of ASM 7.1, all final public static int members of AbstractInsNode are ones we want. Updates beware.
      it.type == Int::class.java && it.modifiers == (Modifier.FINAL or Modifier.PUBLIC or Modifier.STATIC)
    }.map {
      val x = (it[null] as Integer).toInt()
      Pair<String, Int>(it.name, x)
    }.toMap()

    /** TODO:doc. */
    private val intToType: Map<Int, String> = typeToInt.toList().map { it.second to it.first }.toMap()
  }
}
