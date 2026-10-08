package com.ritesh.cashiro.domain.usecase

import androidx.room.withTransaction
import com.ritesh.cashiro.data.database.CashiroDatabase
import com.ritesh.cashiro.data.database.entity.TransactionEntity
import com.ritesh.cashiro.data.database.entity.TransactionType
import com.ritesh.cashiro.utils.BalanceDiscrepancy
import javax.inject.Inject

class AddBalanceAdjustmentUseCase @Inject constructor(
    private val database: CashiroDatabase,
    private val detector: DetectBalanceDiscrepancyUseCase
) {
    enum class Result { ADDED, ALREADY_RECORDED, CHANGED }

    /** Revalidate inside the write transaction; no balance row is inserted or recalculated. */
    suspend fun execute(displayed: BalanceDiscrepancy): Result = database.withTransaction {
        val dao = database.transactionDao()
        val hash = BalanceDiscrepancy.adjustmentHash(displayed.transactionId)
        if (dao.getTransactionByHash(hash) != null) return@withTransaction Result.ALREADY_RECORDED
        val reporting = dao.getTransactionById(displayed.transactionId) ?: return@withTransaction Result.CHANGED
        val current = detector.execute(reporting) ?: return@withTransaction Result.CHANGED
        if (current != displayed) return@withTransaction Result.CHANGED
        val at = current.at.minusSeconds(1).takeIf { it > current.since } ?: current.at
        val id = dao.insertTransaction(TransactionEntity(
            amount = current.delta.abs(),
            merchantName = "Balance adjustment",
            category = "Others",
            transactionType = if (current.delta.signum() < 0) TransactionType.EXPENSE else TransactionType.INCOME,
            dateTime = at,
            bankName = current.bankName,
            accountNumber = current.accountNumber,
            currency = current.currency,
            description = "Untracked amount explaining a bank-reported balance difference",
            transactionHash = hash
        ))
        if (id == -1L) Result.ALREADY_RECORDED else Result.ADDED
    }
}
