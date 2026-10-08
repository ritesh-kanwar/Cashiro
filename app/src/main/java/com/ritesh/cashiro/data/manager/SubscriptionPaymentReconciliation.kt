package com.ritesh.cashiro.data.manager

import androidx.room.withTransaction
import com.ritesh.cashiro.data.database.CashiroDatabase
import com.ritesh.cashiro.data.database.entity.SubscriptionEntity
import com.ritesh.cashiro.data.database.entity.SubscriptionPaymentEntity
import com.ritesh.cashiro.utils.SubscriptionUtils
import com.ritesh.cashiro.utils.PiiRedactor
import com.ritesh.cashiro.data.database.entity.TransactionEntity
import com.ritesh.cashiro.data.database.entity.TransactionType
import com.ritesh.cashiro.data.repository.AccountBalanceRepository
import java.time.LocalDateTime

/** Reconcile a same-day bank confirmation with an acknowledged manual payment exactly once. */
object SubscriptionPaymentReconciliation {
    suspend fun recordCharge(database: CashiroDatabase, subscription: SubscriptionEntity,
        transactionId: Long, incoming: TransactionEntity) = database.withTransaction {
        val paidDate = incoming.dateTime.toLocalDate()
        val scheduled = subscription.nextPaymentDate ?: paidDate
        if (paidDate.isBefore(scheduled.minusDays(7)) ||
            database.subscriptionPaymentDao().forCycle(subscription.id, scheduled) != null ||
            database.subscriptionPaymentDao().forTransaction(transactionId) != null) return@withTransaction
        database.subscriptionPaymentDao().insert(SubscriptionPaymentEntity(
            subscription.id, scheduled, paidDate, transactionId, incoming.transactionHash
        ))
        database.subscriptionDao().updateSubscription(subscription.copy(
            nextPaymentDate = SubscriptionUtils.calculateNextPaymentDate(scheduled, subscription.billingCycle),
            lastPaidDate = paidDate, updatedAt = LocalDateTime.now()
        ))
    }

    suspend fun reconcile(
        database: CashiroDatabase,
        balances: AccountBalanceRepository,
        incoming: TransactionEntity
    ): Long? = database.withTransaction {
        val payments = database.subscriptionPaymentDao()
        payments.forSmsHash(incoming.transactionHash)?.let { return@withTransaction it.transactionId }
        if (incoming.transactionType !in setOf(TransactionType.EXPENSE, TransactionType.CREDIT)) return@withTransaction null
        val bank = incoming.bankName ?: return@withTransaction null
        val suffix = incoming.accountNumber ?: return@withTransaction null
        val candidates = payments.awaitingSms(bank, suffix, incoming.currency, incoming.dateTime.toLocalDate())
            .filter { it.amount.compareTo(incoming.amount) == 0 &&
                it.merchantName.trim().equals(incoming.merchantName.trim(), ignoreCase = true) }
        // Ambiguous charges remain separate; never silently absorb a different purchase.
        val existing = candidates.singleOrNull() ?: return@withTransaction null
        if (!existing.isDeleted) {
            val account = balances.getLatestBalance(bank, suffix, incoming.currency)
            database.accountBalanceDao().deleteTransactionBalancesAndRecalculate(existing.id)
            database.transactionDao().updateTransaction(existing.copy(
                dateTime = incoming.dateTime, smsBody = incoming.smsBody, smsSender = incoming.smsSender,
                reference = incoming.reference ?: existing.reference, balanceAfter = incoming.balanceAfter,
                updatedAt = LocalDateTime.now()
            ))
            if (account != null) {
                balances.insertTransactionBalance(
                    bankName = bank, accountLast4 = suffix, amount = incoming.amount,
                    transactionType = existing.transactionType, explicitBalance = incoming.balanceAfter,
                    timestamp = incoming.dateTime, transactionId = existing.id, creditLimit = null,
                    isCreditCard = account.isCreditCard, smsSource = incoming.smsBody?.let(PiiRedactor::redact),
                    currency = incoming.currency, isWallet = account.isWallet
                )
            }
        }
        // Keep the original transaction hash and curated metadata; this records SMS replay identity.
        payments.confirmSms(existing.id, incoming.transactionHash)
        existing.id
    }
}
