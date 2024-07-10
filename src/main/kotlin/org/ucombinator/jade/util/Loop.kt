package org.ucombinator.jade.util

// TODO: figure out why we need this instead of just using the while loop
inline fun repeat(block: () -> Unit): Nothing {
  while (true) block()
}
