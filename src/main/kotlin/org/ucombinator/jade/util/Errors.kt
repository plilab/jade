package org.ucombinator.jade.util

object Errors {
  fun unmatchedType(x: Any): Nothing = fatal("Type not handled by match: ${x::class.java.name}")
  fun unmatchedValue(x: Any): Nothing = fatal("Value not handled by match: ${x}")
  fun fatal(msg: String): Nothing = throw Exception("Fatal error: $msg")
}
