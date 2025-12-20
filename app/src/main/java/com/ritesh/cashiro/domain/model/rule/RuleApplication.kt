package com.ritesh.cashiro.domain.model.rule

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class RuleApplication(
    val id: String = UUID.randomUUID().toString(),
    val ruleId: String,
    val ruleName: String,
    val transactionId: String,
    val fieldsModified: List<FieldModification>,
    val appliedAt: Long = System.currentTimeMillis()
)

@Serializable
data class FieldModification(
    val field: TransactionField,
    val oldValue: String?,
    val newValue: String?,
    val actionType: ActionType
)