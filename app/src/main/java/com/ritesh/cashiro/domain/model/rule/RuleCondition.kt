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
                when (operator) {
                    ConditionOperator.IN, ConditionOperator.NOT_IN ->
                        value.split(",").all { it.trim().toBigDecimalOrNull() != null }
                    else -> value.toBigDecimalOrNull() != null
                }
            }
            TransactionField.TYPE -> when (operator) {
                ConditionOperator.EQUALS, ConditionOperator.NOT_EQUALS ->
                    runCatching { com.ritesh.cashiro.data.database.entity.TransactionType.valueOf(value.uppercase()) }.isSuccess
                else -> true
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
    BANK_NAME,    // Bank name from SMS
    SUBCATEGORY   // Transaction subcategory
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

/** The operators offered by Cashiro's condition editor. */
fun supportedOperators(field: TransactionField): List<ConditionOperator> = when (field) {
    TransactionField.AMOUNT -> listOf(
        ConditionOperator.LESS_THAN, ConditionOperator.GREATER_THAN, ConditionOperator.EQUALS
    )
    else -> listOf(
        ConditionOperator.CONTAINS, ConditionOperator.EQUALS, ConditionOperator.STARTS_WITH
    )
}
