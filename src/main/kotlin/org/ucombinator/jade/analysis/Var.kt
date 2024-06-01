package org.ucombinator.jade.analysis

import org.objectweb.asm.tree.analysis.BasicValue
import org.objectweb.asm.tree.analysis.Value
import org.ucombinator.jade.asm.Insn


/*
 * Represents a variable in Jade's analyses.
 *
 * @property name The name of the variable.
 * @property basicValue The value of the variable.
 *
 */
sealed class Var(val name: String) : Value {
  abstract val basicValue: BasicValue?
  override fun getSize(): Int = basicValue?.size ?: 0
}

// TODO: improve variable names (also use jvm bytecode debug info for variable names)

object EmptyVar : Var("emptyVar") {
  override val basicValue: BasicValue = BasicValue.UNINITIALIZED_VALUE
}
/* ktlint-disable */
data class ParameterVar  (override val basicValue: BasicValue, val local: Int) :
  Var("parameterVar${local + 1}") // TODO: +1 parameter if non-static
data class ReturnVar     (override val basicValue: BasicValue) :
  Var("returnVar") // TODO: used only for "expected" return type
data class ExceptionVar  (override val basicValue: BasicValue, val insn: Insn) :
  Var("exceptionVar${insn.index()}")
data class InstructionVar(override val basicValue: BasicValue?, val insn: Insn) :
  Var("insnVar${insn.index()}")
data class CopyVar       (override val basicValue: BasicValue, val insn: Insn, val version: Int) :
  Var("copyVar${insn.index()}_${version}")
data class PhiVar(
  override val basicValue: BasicValue, val insn: Insn, val index: Int, var changed: Boolean = false
) : Var("phiVar${insn.index()}_${index}") {
/* ktlint-enable */
  // TODO: use private constructor to hide `changed`
  // Note that `changed` has to be in the parameters so that the analysis sees that the value has changed
  private var changedPhiVar: PhiVar? = null
  fun change(): PhiVar {
    if (this.changedPhiVar === null) {
      this.changedPhiVar = this.copy(changed = true)
      this.changedPhiVar!!.changedPhiVar = this.changedPhiVar
    }
    return this.changedPhiVar!!
  }
}
