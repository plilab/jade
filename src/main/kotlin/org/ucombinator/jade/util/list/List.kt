package org.ucombinator.jade.util.list

import org.ucombinator.jade.util.tuple.Fourple

fun <A, B, C, D> zipAll(a: List<A>, b: List<B>, c: List<C>, d: List<D>): List<Fourple<A?, B?, C?, D?>> {
  val length = listOf(a, b, c, d).map { it.size }.maxOrNull() ?: 0
  return (0..length).map {
    Fourple(
      a.getOrNull(it),
      b.getOrNull(it),
      c.getOrNull(it),
      d.getOrNull(it),
    )
  }
}

fun <T> List<T>.tail(): List<T> = this.subList(1, this.size)

fun <A, B, C> List<Pair<A, B>>.map(f: (A, B) -> C): List<C> = this.map { p -> f(p.first, p.second) }

fun <A> List<A>.pairs(): List<Pair<A, A>> = (0..this.size step 2).map { Pair(this[it], this[it + 1]) }
