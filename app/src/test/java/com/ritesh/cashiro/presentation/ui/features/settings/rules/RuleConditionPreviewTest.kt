package com.ritesh.cashiro.presentation.ui.features.settings.rules

import com.ritesh.cashiro.domain.model.rule.*
import org.junit.Assert.assertEquals
import org.junit.Test

class RuleConditionPreviewTest {
    private fun condition(name: String, op: LogicalOperator = LogicalOperator.AND) = RuleCondition(TransactionField.MERCHANT, ConditionOperator.EQUALS, name, op)
    @Test fun mixedPreviewBracketsRunningResult() {
        assertEquals("(A OR B) AND C", conditionPreview(listOf(condition("A"), condition("B", LogicalOperator.OR), condition("C"))) { it.value })
    }
    @Test fun homogeneousPreviewNeedsNoBrackets() {
        assertEquals("A OR B OR C", conditionPreview(listOf(condition("A"), condition("B", LogicalOperator.OR), condition("C", LogicalOperator.OR))) { it.value })
    }
    @Test fun emptyPreviewDescribesCatchAll() { assertEquals("all transactions", conditionPreview(emptyList()) { it.value }) }
}
