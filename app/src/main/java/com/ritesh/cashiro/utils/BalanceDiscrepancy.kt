package com.ritesh.cashiro.utils

import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import com.ritesh.cashiro.data.database.entity.TransactionEntity
import com.ritesh.cashiro.data.database.entity.TransactionType
import java.math.BigDecimal
import java.time.LocalDateTime

/** The bank's explicit balance is authoritative; this difference explains missing ledger activity. */
data class BalanceDiscrepancy(
    val transactionId: Long,
    val anchorId: Long,
    val bankName: String,
    val accountNumber: String,
    val expected: BigDecimal,
    val reported: BigDecimal,
    val currency: String,
    val since: LocalDateTime,
    val at: LocalDateTime
) {
    val delta: BigDecimal get() = reported - expected

    companion object {
        val TOLERANCE: BigDecimal = BigDecimal.ONE
        fun adjustmentHash(transactionId: Long) = "manual_balance_adjustment_v1_$transactionId"

        fun compute(
            reporting: TransactionEntity,
            previous: AccountBalanceEntity,
            window: List<TransactionEntity>
        ): BalanceDiscrepancy? {
            val reported = reporting.balanceAfter ?: return null
            val bank = reporting.bankName?.takeIf(String::isNotBlank) ?: return null
            val account = reporting.accountNumber?.takeIf(String::isNotBlank) ?: return null
            if (reporting.id <= 0 || reporting.isDeleted || reporting.transactionType in
                setOf(TransactionType.CREDIT, TransactionType.TRANSFER, TransactionType.BALANCE_UPDATE)) return null
            if (previous.isCreditCard || previous.bankName != bank || previous.accountLast4 != account ||
                previous.currency != reporting.currency || previous.timestamp >= reporting.dateTime) return null
            var expected = previous.balance
            for (row in window) {
                if (row.isDeleted || row.currency != reporting.currency || row.dateTime <= previous.timestamp ||
                    row.dateTime > reporting.dateTime) continue
                val effect = effectOn(row, bank, account) ?: return null
                // Ordering of separate transactions with the same timestamp is unknown.
                if (effect.signum() != 0 && row.dateTime == reporting.dateTime && row.id != reporting.id &&
                    row.transactionHash != adjustmentHash(reporting.id)) return null
                expected += effect
            }
            return BalanceDiscrepancy(reporting.id, previous.id, bank, account, expected, reported,
                reporting.currency, previous.timestamp, reporting.dateTime)
                .takeIf { it.delta.abs() > TOLERANCE }
        }

        /** Null means a potentially matching transfer leg cannot be assigned to a bank safely. */
        fun effectOn(row: TransactionEntity, bank: String, account: String): BigDecimal? {
            if (row.transactionType == TransactionType.TRANSFER) {
                if ((row.fromAccount.isNullOrBlank() || row.toAccount.isNullOrBlank()) &&
                    row.accountNumber == account && row.bankName == bank) return null
                val fromBank = row.fromBankName?.takeIf(String::isNotBlank)
                    ?: row.bankName?.takeIf(String::isNotBlank)
                val toBank = row.toBankName?.takeIf(String::isNotBlank)
                if (row.fromAccount == account && fromBank == null || row.toAccount == account && toBank == null) return null
                val outgoing = row.fromAccount == account && fromBank == bank
                val incoming = row.toAccount == account && toBank == bank
                return (if (incoming) row.amount else BigDecimal.ZERO) -
                    (if (outgoing) row.amount else BigDecimal.ZERO)
            }
            if (row.bankName != bank || row.accountNumber != account) return BigDecimal.ZERO
            return when (row.transactionType) {
                TransactionType.INCOME, TransactionType.BORROWED -> row.amount
                TransactionType.EXPENSE, TransactionType.INVESTMENT, TransactionType.LENT -> -row.amount
                TransactionType.BALANCE_UPDATE -> BigDecimal.ZERO
                TransactionType.CREDIT -> null
                TransactionType.TRANSFER -> error("Handled above")
            }
        }
    }
}
