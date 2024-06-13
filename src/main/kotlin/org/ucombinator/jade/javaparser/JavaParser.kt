package org.ucombinator.jade.javaparser

import com.github.javaparser.ast.Node
import com.github.javaparser.ast.comments.BlockComment
import com.github.javaparser.ast.comments.Comment
import com.github.javaparser.ast.stmt.EmptyStmt

/** TODO:doc. */
object JavaParser {
  /** TODO:doc.
   *
   * @param A TODO:doc
   * @param node TODO:doc
   * @param comment TODO:doc
   * @return TODO:doc
   */
  fun <A : Node> setComment(node: A, comment: Comment?): A = node.apply { setComment(comment) }

  /** TODO:doc.
   *
   * @param comment TODO:doc
   * @return TODO:doc
   */
  fun noop(comment: String): EmptyStmt = JavaParser.setComment(EmptyStmt(), BlockComment(comment))
}
