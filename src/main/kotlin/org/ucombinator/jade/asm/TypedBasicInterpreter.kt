package org.ucombinator.jade.asm

import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.analysis.BasicInterpreter
import org.objectweb.asm.tree.analysis.BasicValue

/** A version of BasicInterpreter that gives actual types instead of collapsing some types into
 * `BasicValue.REFERENCE_VALUE` (array and object types) or `BasicValue.INT_TYPE` (boolean, char, byte short, int).
 */
object TypedBasicInterpreter : BasicInterpreter(Opcodes.ASM9) {
  override fun newValue(type: Type?): BasicValue? =
    when (type?.sort) {
      null -> BasicValue.UNINITIALIZED_VALUE
      Type.VOID -> null
      else -> BasicValue(type)
    }
}
