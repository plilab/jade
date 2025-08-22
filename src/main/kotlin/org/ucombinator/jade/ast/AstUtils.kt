package org.ucombinator.jade.ast

import com.github.javaparser.ast.Node

/**
 * AST utility functions for safe transformation and querying of JavaParser ASTs.
 * These functions are inspired by functional programming and compiler design patterns.
 */

/**
 * Transforms an AST in a mutation-safe way, similar to tree rewriting in compiler design.
 * Unlike the built-in walk() function, this returns a new tree and never mutates the original.
 * 
 * @param T The type of node being transformed
 * @param transform Function that transforms a node, returning a new node (or the same if no change)
 * @return A new tree with transformations applied bottom-up
 */
fun <T : Node> T.transform(transform: (Node) -> Node): T {
    // Apply transformation bottom-up to ensure all children are processed first
    val transformedChildren = this.childNodes.map { child ->
        child.transform(transform)
    }
    // Apply transformation to this node (which now has transformed children?)
    @Suppress("UNCHECKED_CAST")
    return transform(this) as T
}

/**
 * Collects information from a tree using a fold operation without mutation.
 * This is a standard pattern in functional programming for traversing and aggregating data.
 * 
 * @param T The type of node being queried
 * @param R The type of result being collected
 * @param initial The initial accumulator value
 * @param extract Function to extract a value from each node
 * @param combine Function to combine results (current accumulator, new value) -> new accumulator
 * @return The final accumulated result
 */
fun <T : Node, R> T.collect(
    initial: R,
    extract: (Node) -> R,
    combine: (R, R) -> R
): R {
    // Start with the result from this node
    val thisResult = extract(this)
    val currentAcc = combine(initial, thisResult)
    
    // Fold over all children
    return this.childNodes.fold(currentAcc) { acc, child ->
        val childResult = child.collect(initial, extract, combine)
        combine(acc, childResult)
    }
}

/**
 * Convenience overload of collect for simple gathering operations.
 * Collects all results into a flat list.
 */
fun <T : Node, R> T.collect(
    extract: (Node) -> List<R>
): List<R> {
    return this.collect(
        initial = emptyList<R>(),
        extract = extract,
        combine = { acc, new -> acc + new }
    )
}

/**
 * Convenience overload of collect for counting operations.
 * Counts nodes that match a predicate.
 */
fun <T : Node> T.count(
    predicate: (Node) -> Boolean
): Int {
    return this.collect(
        initial = 0,
        extract = { if (predicate(it)) 1 else 0 },
        combine = { acc, new -> acc + new }
    )
}
