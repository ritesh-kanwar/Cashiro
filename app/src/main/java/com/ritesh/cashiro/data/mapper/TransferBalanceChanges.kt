package com.ritesh.cashiro.data.mapper

import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import com.ritesh.cashiro.data.database.entity.TransactionEntity
import com.ritesh.cashiro.data.database.entity.TransactionType
import java.math.BigDecimal

internal data class TransferBalanceKey(val bank: String, val suffix: String, val currency: String)

/** Undo the old legs and apply the new legs, coalescing unchanged account identities. */
internal fun transferBalanceChanges(old: TransactionEntity, new: TransactionEntity, accounts: List<AccountBalanceEntity>, onUnresolved: (String) -> Unit = {}): Map<TransferBalanceKey, BigDecimal> {
    if (old.transactionType == new.transactionType && old.amount.compareTo(new.amount) == 0 &&
        old.currency == new.currency && old.accountNumber == new.accountNumber && old.bankName == new.bankName &&
        old.fromAccount == new.fromAccount && old.fromBankName == new.fromBankName &&
        old.toAccount == new.toAccount && old.toBankName == new.toBankName) return emptyMap()

    fun effects(row: TransactionEntity): Map<TransferBalanceKey, BigDecimal> {
        val result = mutableMapOf<TransferBalanceKey, BigDecimal>()
        fun add(suffix: String?, bank: String?, effect: BigDecimal) {
            if (suffix == null) return
            val resolvedBank = bank ?: accounts.filter { it.accountLast4 == suffix && it.currency == row.currency }
                .distinctBy { it.bankName }.singleOrNull()?.bankName ?: run {
                    onUnresolved("Transfer account identity is missing or ambiguous")
                    return
                }
            val key = TransferBalanceKey(resolvedBank, suffix, row.currency)
            result[key] = (result[key] ?: BigDecimal.ZERO) + effect
        }
        if (row.transactionType == TransactionType.TRANSFER) {
            val from = row.fromAccount ?: row.accountNumber?.takeUnless { it == row.toAccount }
            val fromBank = row.fromBankName ?: row.bankName?.takeIf { row.accountNumber == from && from != row.toAccount }
            val toBank = row.toBankName ?: row.bankName?.takeIf { row.accountNumber == row.toAccount && from != row.toAccount }
            add(from, fromBank, -row.amount)
            add(row.toAccount, toBank, row.amount)
        } else {
            val effect = when (row.transactionType) {
                TransactionType.INCOME, TransactionType.CREDIT, TransactionType.BORROWED -> row.amount
                TransactionType.EXPENSE, TransactionType.INVESTMENT, TransactionType.LENT -> -row.amount
                else -> BigDecimal.ZERO
            }
            add(row.accountNumber, row.bankName, effect)
        }
        return result
    }
    val before = effects(old)
    val after = effects(new)
    return (before.keys + after.keys).associateWith { (after[it] ?: BigDecimal.ZERO) - (before[it] ?: BigDecimal.ZERO) }
        .filterValues { it.compareTo(BigDecimal.ZERO) != 0 }
}
