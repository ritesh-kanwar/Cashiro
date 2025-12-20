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
            ActionType.SET -> value.isNotBlank()
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