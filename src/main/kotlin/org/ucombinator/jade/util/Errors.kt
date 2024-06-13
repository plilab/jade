package org.ucombinator.jade.util

/** TODO:doc. */
object Errors {
  /** TODO:doc.
   *
   * @param x TODO:doc
   * @return TODO:doc
   */
  fun unmatchedType(x: Any): Nothing = fatal("Type ${x::class.java.name} not handled by match of value ${x}")

  /** TODO:doc.
   *
   * @param x TODO:doc
   * @return TODO:doc
   */
  fun unmatchedValue(x: Any): Nothing = fatal("Value not handled by match: ${x}")

  /** TODO:doc.
   *
   * @param message TODO:doc
   * @return TODO:doc
   */
  @Suppress("detekt:TooGenericExceptionThrown")
  fun fatal(message: String): Nothing = throw Exception("Fatal error: ${message}")
}
