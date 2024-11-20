// package org.ucombinator.jade.util

// object Diff {
//   fun <T> diff(old: List<T>, new: List<T>) {
//     // Asymptotically inefficient (O(|old| * |new|)) but okay for small
//     val cost = arrayOf(old.size + 1) // { arrayOf(new.size) }
//     val cost: Array<Array<Triple<Int, Pair<Int, Int>, Type>>>>
//     for (i in 0..old.size) {
//       for (j in 0..new.size) {
//         listOf(
//           if (i == 0 && j == 0) 0 else null,
//           cost.getOrNull(i - 1)?.getOrNull(j - 1),
//           cost.getOrNull(i - 1)?.getOrNull(j)?.plus(1),
//           cost.getOrNull(i)?.getOrNull(j - 1)?.plus(1),
//         ).minNonNull()
//       }
//     }
//     var i = old.size, j = new.size
//     val result
//     while (i > 0 && j > 0) {
//       val c = cost[i - 1][j - 1]
//       (i, j) = c.prev
//       result += c
//     }
//   }
// }
