package jade2.app

import java.io.PrintWriter
import java.io.StringWriter

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*
import org.objectweb.asm.util.Textifier
import org.objectweb.asm.util.TraceMethodVisitor

data class Insn(val method: MethodNode, val insn: AbstractInsnNode) {
  fun index: Int = method.instructions.indexOf(insn)
  fun next: Insn = Insn(method, insn.getNext())
  fun shortString: String = InsnTextifier.shortString(method, insn)
  fun longString: String = InsnTextifier.longString(method, insn)
  override fun toString: String = InsnTextifier.longString(method, insn)
}

object InsnTextifier : Textifier(Opcodes.ASM9) {
  private val stringWriter = StringWriter()
  private val stringBuffer = stringWriter.getBuffer()
  private val printWriter = PrintWriter(stringWriter)
  private val methodVisitor = TraceMethodVisitor(this)
}
