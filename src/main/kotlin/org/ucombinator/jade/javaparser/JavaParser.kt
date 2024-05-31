package org.ucombinator.jade.javaparser

import com.github.javaparser.ast.Node
import com.github.javaparser.ast.comments.BlockComment
import com.github.javaparser.ast.comments.Comment
import com.github.javaparser.ast.stmt.EmptyStmt

object JavaParser {
  fun <A : Node> setComment(node: A, comment: Comment?): A = node.also { it.setComment(comment) }

  fun noop(comment: String): EmptyStmt = JavaParser.setComment(EmptyStmt(), BlockComment(comment))
}
