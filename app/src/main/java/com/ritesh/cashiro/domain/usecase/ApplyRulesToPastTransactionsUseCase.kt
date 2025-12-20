package com.ritesh.cashiro.domain.usecase

import com.ritesh.cashiro.data.database.entity.TransactionEntity
import com.ritesh.cashiro.domain.model.rule.TransactionRule
import com.ritesh.cashiro.domain.repository.RuleRepository
import com.ritesh.cashiro.domain.service.RuleEngine
import com.ritesh.cashiro.data.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

data class BatchApplyResult(
    val totalProcessed: Int,
    val totalUpdated: Int,
    val totalDeleted: Int = 0,
    val errors: List<String> = emptyList()
)

class ApplyRulesToPastTransactionsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val ruleRepository: RuleRepository,
    private val ruleEngine: RuleEngine
) {
    /**
     * Apply a specific rule to all past transactions
     */
    suspend fun applyRuleToAllTransactions(
        rule: TransactionRule,
        onProgress: (processed: Int, total: Int) -> Unit = { _, _ -> }
    ): BatchApplyResult {
        val allTransactions = transactionRepository.getAllTransactionsList()
        return processTransactionsWithRule(allTransactions, rule, onProgress)
    }

    /**
     * Apply a specific rule to uncategorized transactions only
     */
    suspend fun applyRuleToUncategorizedTransactions(
        rule: TransactionRule,
        onProgress: (processed: Int, total: Int) -> Unit = { _, _ -> }
    ): BatchApplyResult {
        val uncategorizedTransactions = transactionRepository.getUncategorizedTransactions()
        return processTransactionsWithRule(uncategorizedTransactions, rule, onProgress)
    }

    /**
     * Apply all active rules to all past transactions
     */
    suspend fun applyAllActiveRulesToTransactions(
        onProgress: (processed: Int, total: Int) -> Unit = { _, _ -> }
    ): BatchApplyResult {
        val activeRules = ruleRepository.getActiveRules()
        val allTransactions = transactionRepository.getAllTransactionsList()

        var totalUpdated = 0
        var totalDeleted = 0
        val errors = mutableListOf<String>()

        allTransactions.forEachIndexed { index, transaction ->
            onProgress(index + 1, allTransactions.size)

            try {
                // Skip already deleted transactions
                if (transaction.isDeleted) {
                    return@forEachIndexed
                }

                // Get SMS body if available
                val smsBody = transaction.smsBody

                // Check if any rule would block this transaction
                val blockingRule = ruleEngine.shouldBlockTransaction(
                    transaction,
                    smsBody,
                    activeRules
                )

                if (blockingRule != null) {
                    // Soft delete the transaction
                    val deletedTransaction = transaction.copy(isDeleted = true)
                    transactionRepository.updateTransaction(deletedTransaction)
                    totalDeleted++
                } else {
                    // Apply rules to transaction
                    val (updatedTransaction, ruleApplications) = ruleEngine.evaluateRules(
                        transaction,
                        smsBody,
                        activeRules
                    )

                    // If transaction was modified, update it
                    if (ruleApplications.isNotEmpty()) {
                        transactionRepository.updateTransaction(updatedTransaction)
                        ruleRepository.saveRuleApplications(ruleApplications)
                        totalUpdated++
                    }
                }
            } catch (e: Exception) {
                errors.add("Error processing transaction ${transaction.id}: ${e.message}")
            }
        }

        return BatchApplyResult(
            totalProcessed = allTransactions.size,
            totalUpdated = totalUpdated,
            totalDeleted = totalDeleted,
            errors = errors
        )
    }

    private suspend fun processTransactionsWithRule(
        transactions: List<TransactionEntity>,
        rule: TransactionRule,
        onProgress: (processed: Int, total: Int) -> Unit
    ): BatchApplyResult {
        var totalUpdated = 0
        var totalDeleted = 0
        val errors = mutableListOf<String>()

        transactions.forEachIndexed { index, transaction ->
            onProgress(index + 1, transactions.size)

            try {
                // Skip already deleted transactions
                if (transaction.isDeleted) {
                    return@forEachIndexed
                }

                // Get SMS body if available
                val smsBody = transaction.smsBody

                // Check if this transaction should be blocked
                val shouldBlock = ruleEngine.shouldBlockTransaction(
                    transaction,
                    smsBody,
                    listOf(rule)
                ) != null

                if (shouldBlock) {
                    // Soft delete the transaction
                    val deletedTransaction = transaction.copy(isDeleted = true)
                    transactionRepository.updateTransaction(deletedTransaction)
                    totalDeleted++
                } else {
                    // Apply regular rule actions
                    val (updatedTransaction, ruleApplications) = ruleEngine.evaluateRules(
                        transaction,
                        smsBody,
                        listOf(rule)
                    )

                    // If transaction was modified, update it
                    if (ruleApplications.isNotEmpty()) {
                        transactionRepository.updateTransaction(updatedTransaction)
                        ruleRepository.saveRuleApplications(ruleApplications)
                        totalUpdated++
                    }
                }
            } catch (e: Exception) {
                errors.add("Error processing transaction ${transaction.id}: ${e.message}")
            }
        }

        return BatchApplyResult(
            totalProcessed = transactions.size,
            totalUpdated = totalUpdated,
            totalDeleted = totalDeleted,
            errors = errors
        )
    }

    /**
     * Preview what would happen if rules were applied (without actually applying)
     */
    suspend fun previewRuleApplication(
        rule: TransactionRule,
        limit: Int = 10
    ): Flow<List<Pair<TransactionEntity, TransactionEntity>>> = flow {
        val transactions = transactionRepository.getAllTransactionsList().take(limit)
        val previews = mutableListOf<Pair<TransactionEntity, TransactionEntity>>()

        transactions.forEach { transaction ->
            val smsBody = transaction.smsBody
            val (updatedTransaction, ruleApplications) = ruleEngine.evaluateRules(
                transaction,
                smsBody,
                listOf(rule)
            )

            if (ruleApplications.isNotEmpty()) {
                previews.add(transaction to updatedTransaction)
            }
        }

        emit(previews)
    }
}