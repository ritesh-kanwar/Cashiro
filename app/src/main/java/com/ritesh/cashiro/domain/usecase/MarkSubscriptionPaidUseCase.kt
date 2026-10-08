package com.ritesh.cashiro.domain.usecase

import androidx.room.withTransaction
import com.ritesh.cashiro.data.database.CashiroDatabase
import com.ritesh.cashiro.data.database.entity.*
import com.ritesh.cashiro.data.preferences.BankAccountMergeStore
import com.ritesh.cashiro.data.repository.AccountBalanceRepository
import com.ritesh.cashiro.utils.SubscriptionUtils
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

/** Record or link one payment, its account balance and its schedule in one Room transaction. */
class MarkSubscriptionPaidUseCase @Inject constructor(
    private val database: CashiroDatabase,
    private val balances: AccountBalanceRepository
) {
    sealed interface Result {
        data class Paid(val transactionId: Long, val linked: Boolean) : Result
        data object AlreadyPaid : Result
        data class ChooseExisting(val transactions: List<TransactionEntity>) : Result
    }

    suspend fun execute(
        subscriptionId: Long,
        expectedScheduledDate: LocalDate?,
        paymentDate: LocalDate = LocalDate.now(),
        linkedTransactionId: Long? = null,
        recordAnotherPayment: Boolean = false
    ): Result = BankAccountMergeStore.mutationMutex.withLock {
        database.withTransaction {
            val subscriptions = database.subscriptionDao()
            val payments = database.subscriptionPaymentDao()
            val subscription = requireNotNull(subscriptions.getSubscriptionById(subscriptionId)) { "Subscription no longer exists" }
            require(subscription.amount.signum() > 0) { "Payment amount must be positive" }
            require(subscription.state == SubscriptionState.ACTIVE) { "Subscription is hidden" }
            val scheduled = subscription.nextPaymentDate ?: paymentDate
            val lastMark = payments.latest(subscriptionId)?.markedDate ?: subscription.lastPaidDate
            if (lastMark != null && paymentDate.isBefore(scheduled) &&
                paymentDate.isBefore(advancePaymentCycle(lastMark, subscription.billingCycle))) {
                return@withTransaction Result.AlreadyPaid
            }
            if (subscription.nextPaymentDate != expectedScheduledDate ||
                subscription.lastPaidDate == paymentDate || payments.forCycle(subscriptionId, scheduled) != null ||
                payments.paidOn(subscriptionId, paymentDate) != null) return@withTransaction Result.AlreadyPaid

            val bank = subscription.bankName?.takeIf { it.isNotBlank() }
            val suffix = subscription.accountLast4?.takeIf { it.isNotBlank() }?.let {
                requireNotNull(bank) { "Choose a bank for the funding account" }
                balances.resolveAccountLast4(bank, it, subscription.currency)
            }
            val account = if (bank != null && suffix != null) {
                requireNotNull(balances.getLatestBalance(bank, suffix, subscription.currency)) { "Funding account no longer exists" }
            } else null
            val candidates = if (bank != null && suffix != null) {
                payments.linkCandidates(bank, suffix, subscription.currency,
                    paymentDate.minusDays(7).atStartOfDay(), paymentDate.plusDays(2).atStartOfDay())
                    .filter { it.amount.compareTo(subscription.amount) == 0 &&
                        it.merchantName.trim().equals(subscription.merchantName.trim(), ignoreCase = true) }
            } else emptyList()
            if (linkedTransactionId == null && candidates.isNotEmpty() && !recordAnotherPayment) {
                return@withTransaction Result.ChooseExisting(candidates)
            }

            val linked = linkedTransactionId?.let { id ->
                requireNotNull(candidates.singleOrNull { it.id == id }) { "Payment is no longer available to link" }
            }
            val paidDate = linked?.dateTime?.toLocalDate() ?: paymentDate
            val transactionId = if (linked != null) linked.id else {
                val timestamp = paymentDate.atTime(java.time.LocalTime.now())
                val type = if (account?.isCreditCard == true) TransactionType.CREDIT else TransactionType.EXPENSE
                val row = TransactionEntity(
                    amount = subscription.amount, merchantName = subscription.merchantName,
                    category = subscription.category ?: "Subscription", subcategory = subscription.subcategory,
                    transactionType = type, dateTime = timestamp, description = "Subscription payment",
                    bankName = bank, accountNumber = suffix, currency = subscription.currency,
                    transactionHash = "subpay-$subscriptionId-$scheduled", isRecurring = true,
                    billingCycle = subscription.billingCycle
                )
                val id = database.transactionDao().insertTransaction(row)
                require(id != -1L) { "This payment was already recorded" }
                if (account != null) {
                    balances.insertTransactionBalance(
                        bankName = account.bankName, accountLast4 = account.accountLast4, amount = row.amount,
                        transactionType = type, explicitBalance = null, timestamp = timestamp, transactionId = id,
                        creditLimit = null, isCreditCard = account.isCreditCard, smsSource = null,
                        currency = row.currency, isWallet = account.isWallet
                    )
                }
                id
            }
            payments.insert(SubscriptionPaymentEntity(
                subscriptionId, scheduled, paidDate, transactionId,
                smsHash = linked?.takeIf { !it.smsBody.isNullOrBlank() && !it.smsSender.isNullOrBlank() }?.transactionHash,
                markedDate = paymentDate
            ))
            subscriptions.updateSubscription(subscription.copy(
                nextPaymentDate = SubscriptionUtils.calculateNextPaymentDate(scheduled, subscription.billingCycle),
                lastPaidDate = paidDate, accountLast4 = suffix, updatedAt = LocalDateTime.now()
            ))
            Result.Paid(transactionId, linked != null)
        }
    }
}

/** A single cycle, without the schedule's catch-up-to-today behavior. */
internal fun advancePaymentCycle(date: LocalDate, billingCycle: String?): LocalDate {
    val cycle = billingCycle?.lowercase() ?: "monthly"
    val custom = cycle.split("_")
    if (custom.firstOrNull() == "custom") {
        val count = custom.getOrNull(1)?.toLongOrNull()?.takeIf { it > 0 } ?: return date.plusMonths(1)
        return when (custom.getOrNull(2)) {
            "day" -> date.plusDays(count)
            "week" -> date.plusWeeks(count)
            "month" -> date.plusMonths(count)
            "year" -> date.plusYears(count)
            else -> date.plusMonths(1)
        }
    }
    return when (cycle) {
        "weekly" -> date.plusWeeks(1)
        "quarterly" -> date.plusMonths(3)
        "semi-annual" -> date.plusMonths(6)
        "annual" -> date.plusYears(1)
        else -> date.plusMonths(1)
    }
}
