package com.ritesh.cashiro.data.backup

import androidx.room.withTransaction
import com.ritesh.cashiro.data.database.CashiroDatabase
import com.ritesh.cashiro.data.database.entity.SubscriptionEntity
import com.ritesh.cashiro.data.database.entity.SubscriptionPaymentEntity

/** Restore foreign identities explicitly, retaining all existing local payment decisions. */
object SubscriptionPaymentBackup {
    fun sameSubscription(left: SubscriptionEntity, right: SubscriptionEntity): Boolean =
        left.merchantName.equals(right.merchantName, ignoreCase = true) &&
            left.amount.compareTo(right.amount) == 0 &&
            left.currency.equals(right.currency, ignoreCase = true) &&
            left.bankName?.lowercase() == right.bankName?.lowercase() &&
            left.accountLast4 == right.accountLast4 && left.umn == right.umn &&
            left.billingCycle == right.billingCycle

    suspend fun restore(database: CashiroDatabase, payments: List<SubscriptionPaymentEntity>,
        subscriptionIds: Map<Long, Long>, transactionIds: Map<Long, Long>) = database.withTransaction {
        val ledger = database.subscriptionPaymentDao()
        payments.forEach { payment ->
            val subscriptionId = subscriptionIds[payment.subscriptionId] ?: return@forEach
            val transactionId = transactionIds[payment.transactionId] ?: return@forEach
            if (database.subscriptionDao().getSubscriptionById(subscriptionId) == null ||
                database.transactionDao().getTransactionById(transactionId) == null) return@forEach
            if (ledger.forCycle(subscriptionId, payment.scheduledDate) != null ||
                ledger.forTransaction(transactionId) != null ||
                payment.smsHash?.let { ledger.forSmsHash(it) != null } == true) return@forEach
            ledger.insert(payment.copy(subscriptionId = subscriptionId, transactionId = transactionId))
        }
    }
}
