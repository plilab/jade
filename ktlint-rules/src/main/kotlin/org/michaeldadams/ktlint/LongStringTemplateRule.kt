package org.michaeldadams.ktlint

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleAutocorrectApproveHandler
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.ruleset.standard.rules.StringTemplateRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

class LongStringTemplateRule :
  Rule(
    ruleId = RuleId("${CUSTOM_RULE_SET_ID}:long-string-template"),
    about = About(
      // maintainer = "Your name",
      // repositoryUrl = "https://github.com/your/project/",
      // issueTrackerUrl = "https://github.com/your/project/issues",
    ),
  ),
  RuleAutocorrectApproveHandler {
  val stringTemplateRule = StringTemplateRule()
  override fun beforeVisitChildNodes(
    node: ASTNode,
    emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
  ) {
    if (node.elementType == ElementType.SHORT_STRING_TEMPLATE_ENTRY) {
      emit(node.treePrev.startOffset + 1, "Missing curly braces", false)
    }

    stringTemplateRule.beforeVisitChildNodes(node) {
        offset, errorMessage, canBeAutoCorrected ->
      if (errorMessage != "Redundant curly braces") {
        emit(offset, errorMessage, canBeAutoCorrected)
      } else {
        AutocorrectDecision.NO_AUTOCORRECT
      }
   }
  }
}
