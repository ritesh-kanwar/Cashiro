package com.ritesh.cashiro.domain.model.rule

import kotlinx.serialization.Serializable

@Serializable
data class RuleAction(
    val field: TransactionField,
    val actionType: ActionType,
    val value: String
) {
    fun validate(): Boolean {
        return when (actionType) {
            ActionType.SET -> if (field == TransactionField.TYPE) {
                runCatching { com.ritesh.cashiro.data.database.entity.TransactionType.valueOf(value.uppercase()) }.isSuccess
            } else value.isNotBlank()
            ActionType.APPEND, ActionType.PREPEND -> value.isNotBlank()
            ActionType.CLEAR -> true
            ActionType.ADD_TAG -> value.isNotBlank()
            ActionType.REMOVE_TAG -> value.isNotBlank()
            ActionType.BLOCK -> true  // BLOCK action doesn't need a value
        }
    }
}

@Serializable
enum class ActionType {
    SET,           // Set field to value
    APPEND,        // Append value to field
    PREPEND,       // Prepend value to field
    CLEAR,         // Clear field
    ADD_TAG,       // Add a tag
    REMOVE_TAG,    // Remove a tag
    BLOCK          // Block the transaction from being saved
}

/** Actions supported by Cashiro's rule engine, including subcategory resolution. */
fun supportedActionTypes(field: TransactionField): Set<ActionType> = when (field) {
    TransactionField.CATEGORY, TransactionField.SUBCATEGORY -> setOf(ActionType.SET, ActionType.CLEAR)
    TransactionField.MERCHANT, TransactionField.NARRATION -> setOf(
        ActionType.SET, ActionType.APPEND, ActionType.PREPEND, ActionType.CLEAR
    )
    TransactionField.TYPE -> setOf(ActionType.SET)
    else -> emptySet()
}

fun RuleAction.isExecutable(): Boolean =
    actionType == ActionType.BLOCK || actionType in supportedActionTypes(field)
