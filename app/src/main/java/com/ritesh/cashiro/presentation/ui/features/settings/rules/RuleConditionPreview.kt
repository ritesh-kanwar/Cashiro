package com.ritesh.cashiro.presentation.ui.features.settings.rules

import com.ritesh.cashiro.domain.model.rule.LogicalOperator
import com.ritesh.cashiro.domain.model.rule.RuleCondition

/** Render the same left-to-right grouping used by the rule engine. */
internal fun conditionPreview(conditions: List<RuleCondition>, describe: (RuleCondition) -> String): String {
    if (conditions.isEmpty()) return "all transactions"
    val mixed = conditions.drop(1).map { it.logicalOperator }.distinct().size > 1
    var result = describe(conditions.first())
    conditions.drop(1).forEachIndexed { index, condition ->
        if (mixed && index > 0) result = "($result)"
        val connector = if (condition.logicalOperator == LogicalOperator.OR) "OR" else "AND"
        result += " $connector ${describe(condition)}"
    }
    return result
}
