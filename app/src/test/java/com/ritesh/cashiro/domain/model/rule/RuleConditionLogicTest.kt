package com.ritesh.cashiro.domain.model.rule

import com.ritesh.cashiro.data.database.entity.TransactionType
import org.junit.Assert.*
import org.junit.Test

class RuleConditionLogicTest {
    private fun value(value: Boolean, op: LogicalOperator = LogicalOperator.AND) =
        RuleCondition(TransactionField.MERCHANT, ConditionOperator.EQUALS, value.toString(), op)
    private fun matches(vararg conditions: RuleCondition) = conditions.toList().matchesConditions { it.value.toBoolean() }

    @Test fun emptyConditionsRemainCashiroCatchAll() { assertTrue(emptyList<RuleCondition>().matchesConditions { false }) }
    @Test fun orCanRecoverFromFalseFirstCondition() { assertTrue(matches(value(false), value(true, LogicalOperator.OR))) }
    @Test fun mixedOperatorsUseLeftFoldInsteadOfBooleanPrecedence() {
        assertFalse(matches(value(true), value(false, LogicalOperator.OR), value(false)))
        assertTrue(matches(value(false), value(false), value(true, LogicalOperator.OR)))
    }
    @Test fun firstConnectorDoesNotChangeEvaluation() { assertFalse(matches(value(false, LogicalOperator.OR))) }
    @Test fun typePrefilterRetainsAlternativeBranch() {
        val conditions = listOf(RuleCondition(TransactionField.TYPE, ConditionOperator.EQUALS, "INCOME"), value(true, LogicalOperator.OR))
        assertTrue(conditions.mayMatchType(TransactionType.EXPENSE))
    }
    @Test fun andTypePrefilterRejectsImpossibleTypesAndSupportsLists() {
        assertFalse(listOf(RuleCondition(TransactionField.TYPE, ConditionOperator.EQUALS, "INCOME")).mayMatchType(TransactionType.EXPENSE))
        assertTrue(listOf(RuleCondition(TransactionField.TYPE, ConditionOperator.IN, "income, expense")).mayMatchType(TransactionType.EXPENSE))
        assertFalse(listOf(RuleCondition(TransactionField.TYPE, ConditionOperator.NOT_IN, "income, expense")).mayMatchType(TransactionType.EXPENSE))
    }
    @Test fun amountEqualityIgnoresScale() {
        assertTrue(numericEquality("100.00", "100"))
        assertFalse(numericEquality("100.00", "100", negate = true))
    }
    @Test fun amountInequalityComparesNumericValues() {
        assertFalse(numericEquality("100.01", "100"))
        assertTrue(numericEquality("100.01", "100", negate = true))
    }
    @Test fun invalidAmountsNeverMatchEitherOperator() {
        for (negate in listOf(false, true)) {
            assertFalse(numericEquality("100", "invalid", negate))
            assertFalse(numericEquality("invalid", "100", negate))
            assertFalse(numericEquality("100", "", negate))
        }
    }
}
