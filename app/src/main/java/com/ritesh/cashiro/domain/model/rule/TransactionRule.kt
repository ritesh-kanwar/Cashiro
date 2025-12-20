package com.ritesh.cashiro.domain.model.rule

import java.util.UUID

data class TransactionRule(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String? = null,
    val priority: Int = 100, // Lower number = higher priority
    val conditions: List<RuleCondition>,
    val actions: List<RuleAction>,
    val isActive: Boolean = true,
    val isSystemTemplate: Boolean = false, // System rules can't be deleted
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun validate(): Boolean {
        return name.isNotBlank() &&
               conditions.isNotEmpty() &&
               actions.isNotEmpty()
    }
}