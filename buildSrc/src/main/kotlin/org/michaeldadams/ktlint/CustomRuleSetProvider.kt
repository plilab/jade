package org.michaeldadams.ktlint

import com.pinterest.ktlint.cli.ruleset.core.api.RuleSetProviderV3
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import com.pinterest.ktlint.rule.engine.core.api.RuleSetId

val CUSTOM_RULE_SET_ID = "org-michaeldadams"

public class CustomRuleSetProvider : RuleSetProviderV3(RuleSetId(CUSTOM_RULE_SET_ID)) {
  override fun getRuleProviders(): Set<RuleProvider> = setOf(
    RuleProvider { RequireReturnTypeRule() },
    RuleProvider { LongStringTemplateRule() },
  )
}
