package org.michaeldadams.ktlint

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleAutocorrectApproveHandler
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

class RequireReturnTypeRule :
  Rule(
    ruleId = RuleId("${CUSTOM_RULE_SET_ID}:require-return-type"),
    about = About(
      // maintainer = "Your name",
      // repositoryUrl = "https://github.com/your/project/",
      // issueTrackerUrl = "https://github.com/your/project/issues",
    ),
  ),
  RuleAutocorrectApproveHandler {
  override fun beforeVisitChildNodes(
    node: ASTNode,
    emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
  ) {
    if (node.elementType == ElementType.FUN) {
      val children = node.getChildren(null)
      val index = children.indexOfLast { it.elementType == ElementType.COLON }
      if (index < 0 ||
        index + 2 > children.lastIndex ||
        children[index + 1].elementType != ElementType.WHITE_SPACE ||
        children[index + 2].elementType != ElementType.TYPE_REFERENCE) {
        emit(node.startOffset, "Missing return type on function", false)
      }
    }
  }
}
