package org.ucombinator.jade.analysis

import org.objectweb.asm.tree.analysis.BasicValue
import org.objectweb.asm.tree.analysis.Value
import org.ucombinator.jade.asm.Insn

/** Represents a variable in Jade's analyses.
 *
 * @property name The name of the variable.
 */
@Suppress("ktlint:standard:blank-line-before-declaration", "ktlint:standard:statement-wrapping")
sealed class Var(val name: String) : Value {
  /** The value of the variable. */
  abstract val basicValue: BasicValue?
  override fun getSize(): Int = basicValue?.size ?: 0

  // TODO: improve variable names (also use jvm bytecode debug info for variable names)

  /** TODO:doc. */
  object Empty : Var("emptyVar") { override val basicValue: BasicValue = BasicValue.UNINITIALIZED_VALUE }

  /** TODO:doc.
   *
   * @property basicValue TODO:doc
   * @property local TODO:doc
   */
  data class Parameter(override val basicValue: BasicValue, val local: Int) :
    Var("parameterVar${local + 1}") // TODO: +1 parameter if non-static

  /** TODO:doc.
   *
   * @property basicValue TODO:doc
   */
  data class Return(override val basicValue: BasicValue) :
    Var("returnVar") // TODO: used only for "expected" return type

  /** TODO:doc.
   *
   * @property basicValue TODO:doc
   * @property insn TODO:doc
   */
  data class Exception(override val basicValue: BasicValue, val insn: Insn) :
    Var("exceptionVar${insn.index()}")

  /** TODO:doc.
   *
   * @property basicValue TODO:doc
   * @property insn TODO:doc
   */
  data class Instruction(override val basicValue: BasicValue?, val insn: Insn) :
    Var("insnVar${insn.index()}")

  /** TODO:doc.
   *
   * @property basicValue TODO:doc
   * @property insn TODO:doc
   * @property version TODO:doc
   */
  data class Copy(override val basicValue: BasicValue, val insn: Insn, val version: Int) :
    Var("copyVar${insn.index()}_${version}")

  /** TODO:doc.
   *
   * @property basicValue TODO:doc
   * @property insn TODO:doc
   * @property index TODO:doc
   * @property changed TODO:doc
   */
  data class Phi(override val basicValue: BasicValue, val insn: Insn, val index: Int, var changed: Boolean = false) :
    Var("phiVar${insn.index()}_${index}") {
    // TODO: could `changed` be a val?
    // TODO: use private constructor to hide `changed`
    // Note that `changed` has to be in the parameters so that the analysis sees that the value has changed

    /** TODO:doc. */
    private var changedPhiVar: Phi? = null

    /** TODO:doc.
     *
     * @return TODO:doc
     */
    fun change(): Phi {
      if (this.changedPhiVar == null) this.changedPhiVar = this.copy(changed = true).also { changedPhiVar = it }
      return this.changedPhiVar!!
    }
  }
}
