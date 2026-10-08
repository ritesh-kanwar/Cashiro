package com.ritesh.cashiro.domain.usecase

import androidx.room.withTransaction
import com.ritesh.cashiro.data.database.CashiroDatabase
import com.ritesh.cashiro.data.database.entity.TransactionEntity
import com.ritesh.cashiro.utils.BalanceDiscrepancy
import javax.inject.Inject

class DetectBalanceDiscrepancyUseCase @Inject constructor(private val database: CashiroDatabase) {
    suspend fun execute(transaction: TransactionEntity): BalanceDiscrepancy? = database.withTransaction {
        val tx = database.transactionDao().getTransactionById(transaction.id) ?: return@withTransaction null
        val bank = tx.bankName ?: return@withTransaction null
        val account = tx.accountNumber ?: return@withTransaction null
        if (tx.balanceAfter == null || tx.isDeleted) return@withTransaction null
        val history = database.accountBalanceDao().getBalanceHistoryForAccount(bank, account)
            .filter { it.currency == tx.currency }
        if (history.any { it.transactionId == tx.id && it.isCreditCard }) return@withTransaction null
        val previous = history.filter { it.timestamp < tx.dateTime && it.transactionId != tx.id }
            .maxByOrNull { it.timestamp } ?: return@withTransaction null
        val rows = database.transactionDao().getTransactionsBetweenDatesList(previous.timestamp, tx.dateTime)
        BalanceDiscrepancy.compute(tx, previous, rows)
    }
}
