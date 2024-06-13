package org.ucombinator.jade.util

/** TODO:doc. */
@Suppress("ktlint:standard:function-naming")
object Tuples {
  /** TODO:doc.
   *
   * @return TODO:doc
   */
  @Suppress("FUNCTION_NAME_INCORRECT_CASE", "FunctionNaming")
  fun <A, B> Pair<A, B>._1(): A = this.first

  /** TODO:doc.
   *
   * @return TODO:doc
   */
  @Suppress("FUNCTION_NAME_INCORRECT_CASE", "FunctionNaming")
  fun <A, B> Pair<A, B>._2(): B = this.second

  /** TODO:doc.
   *
   * @return TODO:doc
   */
  @Suppress("FUNCTION_NAME_INCORRECT_CASE", "FunctionNaming")
  fun <A, B, C> Triple<A, B, C>._1(): A = this.first

  /** TODO:doc.
   *
   * @return TODO:doc
   */
  @Suppress("FUNCTION_NAME_INCORRECT_CASE", "FunctionNaming")
  fun <A, B, C> Triple<A, B, C>._2(): B = this.second

  /** TODO:doc.
   *
   * @return TODO:doc
   */
  @Suppress("FUNCTION_NAME_INCORRECT_CASE", "FunctionNaming")
  fun <A, B, C> Triple<A, B, C>._3(): C = this.third

  /** Represents a quadruple of values.
   *
   * @param A type of the first value.
   * @param B type of the second value.
   * @param C type of the third value.
   * @param D type of the fourth value.
   * @property _1 First value.
   * @property _2 Second value.
   * @property _3 Third value.
   * @property _4 Fourth value.
   */
  @Suppress("ConstructorParameterNaming", "VARIABLE_NAME_INCORRECT_FORMAT")
  data class Fourple<out A, out B, out C, out D>(val _1: A, val _2: B, val _3: C, val _4: D)

  /** Represents a quintuple of values.
   *
   * @param A type of the first value.
   * @param B type of the second value.
   * @param C type of the third value.
   * @param D type of the fourth value.
   * @param E type of the fifth value.
   * @property _1 First value.
   * @property _2 Second value.
   * @property _3 Third value.
   * @property _4 Fourth value.
   * @property _5 Fifth value.
   */
  @Suppress("ConstructorParameterNaming", "VARIABLE_NAME_INCORRECT_FORMAT")
  data class Fiveple<out A, out B, out C, out D, out E>(val _1: A, val _2: B, val _3: C, val _4: D, val _5: E)
}
