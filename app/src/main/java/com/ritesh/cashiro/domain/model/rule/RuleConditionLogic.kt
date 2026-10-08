package com.ritesh.cashiro.domain.model.rule

import com.ritesh.cashiro.data.database.entity.TransactionType

/** Each connector belongs to its following condition; mixed operators fold left to right. */
internal fun List<RuleCondition>.matchesConditions(matches: (RuleCondition) -> Boolean): Boolean {
    if (isEmpty()) return true
    var result = matches(first())
    for (condition in drop(1)) {
        val next = matches(condition)
        result = when (condition.logicalOperator) {
            LogicalOperator.AND -> result && next
            LogicalOperator.OR -> result || next
        }
    }
    return result
}

/** A conservative prefilter: an OR sibling can match even when TYPE does not. */
internal fun List<RuleCondition>.mayMatchType(type: TransactionType): Boolean {
    if (drop(1).any { it.logicalOperator == LogicalOperator.OR }) return true
    return filter { it.field == TransactionField.TYPE }.all { condition ->
        when (condition.operator) {
            ConditionOperator.EQUALS -> condition.value.equals(type.name, ignoreCase = true)
            ConditionOperator.NOT_EQUALS -> !condition.value.equals(type.name, ignoreCase = true)
            ConditionOperator.IN -> condition.value.split(',').any { it.trim().equals(type.name, ignoreCase = true) }
            ConditionOperator.NOT_IN -> condition.value.split(',').none { it.trim().equals(type.name, ignoreCase = true) }
            else -> true
        }
    }
}

/** Invalid numeric operands never match, including a NOT_EQUALS condition. */
internal fun numericEquality(left: String, right: String, negate: Boolean = false): Boolean {
    val first = left.trim().toBigDecimalOrNull() ?: return false
    val second = right.trim().toBigDecimalOrNull() ?: return false
    val equal = first.compareTo(second) == 0
    return if (negate) !equal else equal
}
