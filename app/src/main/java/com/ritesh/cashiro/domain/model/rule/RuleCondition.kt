package com.ritesh.cashiro.domain.model.rule

import kotlinx.serialization.Serializable

@Serializable
data class RuleCondition(
    val field: TransactionField,
    val operator: ConditionOperator,
    val value: String,
    val logicalOperator: LogicalOperator = LogicalOperator.AND
) {
    fun validate(): Boolean {
        return value.isNotBlank() && when (field) {
            TransactionField.AMOUNT -> {
                // For amount fields, ensure value is numeric for comparison operators
                when (operator) {
                    ConditionOperator.LESS_THAN,
                    ConditionOperator.GREATER_THAN,
                    ConditionOperator.LESS_THAN_OR_EQUAL,
                    ConditionOperator.GREATER_THAN_OR_EQUAL -> value.toBigDecimalOrNull() != null
                    else -> true
                }
            }
            else -> true
        }
    }
}

@Serializable
enum class TransactionField {
    AMOUNT,       // Transaction amount
    TYPE,         // INCOME, EXPENSE, or TRANSFER
    CATEGORY,     // Transaction category
    MERCHANT,     // Merchant/vendor name
    NARRATION,    // Description/notes
    SMS_TEXT,     // Original SMS text
    BANK_NAME     // Bank name from SMS
}

@Serializable
enum class ConditionOperator {
    EQUALS,
    NOT_EQUALS,
    CONTAINS,
    NOT_CONTAINS,
    STARTS_WITH,
    ENDS_WITH,
    LESS_THAN,
    GREATER_THAN,
    LESS_THAN_OR_EQUAL,
    GREATER_THAN_OR_EQUAL,
    IN,
    NOT_IN,
    REGEX_MATCHES,
    IS_EMPTY,
    IS_NOT_EMPTY
}

@Serializable
enum class LogicalOperator {
    AND,
    OR
}