package org.ucombinator.jade.javaparser

import com.github.javaparser.ast.DataKey
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.comments.BlockComment
import com.github.javaparser.ast.comments.Comment
import com.github.javaparser.ast.stmt.EmptyStmt
import com.github.javaparser.ast.type.Type
import org.ucombinator.jade.analysis.Var

object JavaParser {
  val variable = object : DataKey<Var>() {}

  val variableType = object : DataKey<Type>() {}

  val isTypeName = object : DataKey<Boolean>() {}

  val transparentPlaceholder = object : DataKey<Boolean>() {}

  /** contains unsupported instructions */
  val unsupported = object : DataKey<Set<Var>>() {}

  /** TODO:doc.
   *
   * @param A TODO:doc
   * @param node TODO:doc
   * @param comment TODO:doc
   * @return TODO:doc
   */
  fun <A : Node> setComment(node: A, comment: Comment?): A = node.apply { setComment(comment) }

  /** A diagnostic placeholder is a movement barrier unless explicitly known to be harmless. */
  fun noop(comment: String, transparentForFolding: Boolean = false): EmptyStmt =
    JavaParser.setComment(EmptyStmt(), BlockComment(comment)).also {
      it.setData(transparentPlaceholder, transparentForFolding)
    }
}
