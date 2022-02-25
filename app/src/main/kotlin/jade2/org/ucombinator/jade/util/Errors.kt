package org.ucombinator.jade.util

object Errors {
  fun unmatchedType(x: Any): Nothing {
    fatal("Type not handled by match: " + x::class.java.getName())
  }

  fun impossibleValue(x: Any): Nothing {
    fatal("Impossible value found: " + x::class.java.getName())
  }

  fun fatal(msg: String): Nothing {
    throw Exception("Fatal error: " + msg)
  }
}
