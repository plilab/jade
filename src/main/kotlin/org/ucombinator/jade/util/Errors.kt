package org.ucombinator.jade.util

object Errors {
  fun unmatchedType(x: Any): Nothing = fatal("Type ${x::class.java.name} not handled by match of value ${x}")

  fun unmatchedValue(x: Any): Nothing = fatal("Value not handled by match: ${x}")

  @Suppress("TooGenericExceptionThrown")
  fun fatal(msg: String): Nothing = throw Exception("Fatal error: ${msg}")
}
