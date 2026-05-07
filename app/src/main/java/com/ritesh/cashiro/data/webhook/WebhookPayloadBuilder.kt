package com.ritesh.cashiro.data.webhook

import com.ritesh.cashiro.BuildConfig
import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import com.ritesh.cashiro.data.database.entity.BudgetEntity
import com.ritesh.cashiro.data.database.entity.SubscriptionEntity
import com.ritesh.cashiro.data.database.entity.TransactionEntity
import com.ritesh.cashiro.data.database.entity.TransactionType
import com.ritesh.cashiro.data.database.entity.WebhookCursorEntity
import com.ritesh.cashiro.data.database.entity.WebhookDataType
import com.ritesh.cashiro.data.database.entity.WebhookProfileEntity
import com.ritesh.cashiro.data.database.entity.WebhookRangePreset
import com.ritesh.cashiro.data.repository.AccountBalanceRepository
import com.ritesh.cashiro.data.repository.BudgetRepository
import com.ritesh.cashiro.data.repository.SubscriptionRepository
import com.ritesh.cashiro.data.repository.TransactionRepository
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class WebhookPayloadBuilder @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val accountBalanceRepository: AccountBalanceRepository,
    private val subscriptionRepository: SubscriptionRepository
) {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    suspend fun build(
        profile: WebhookProfileEntity,
        selectedTypes: Set<WebhookDataType>,
        currency: String,
        cursors: List<WebhookCursorEntity>,
        sendTestPayload: Boolean
    ): List<WebhookBatchPayload> {
        if (sendTestPayload) {
            return listOf(buildTestPayload(profile, selectedTypes, currency))
        }

        val range = resolveRange(profile, cursors)
        val cursorUpdates = mutableListOf<WebhookCursorUpdate>()
        val summary = if (WebhookDataType.SUMMARY in selectedTypes) {
            cursorUpdates += WebhookCursorUpdate(WebhookDataType.SUMMARY, LocalDateTime.now(), range.end)
            buildSummary(range, currency)
        } else {
            null
        }
        val budgets = if (WebhookDataType.BUDGETS in selectedTypes) {
            cursorUpdates += WebhookCursorUpdate(WebhookDataType.BUDGETS, LocalDateTime.now(), range.end)
            buildBudgets(range, currency)
        } else {
            emptyList()
        }
        val accounts = if (WebhookDataType.ACCOUNTS in selectedTypes) {
            cursorUpdates += WebhookCursorUpdate(WebhookDataType.ACCOUNTS, LocalDateTime.now(), range.end)
            buildAccounts(currency)
        } else {
            emptyList()
        }
        val subscriptions = if (WebhookDataType.SUBSCRIPTIONS in selectedTypes) {
            cursorUpdates += WebhookCursorUpdate(WebhookDataType.SUBSCRIPTIONS, LocalDateTime.now(), range.end)
            buildSubscriptions(currency)
        } else {
            emptyList()
        }

        val transactionBatches = if (WebhookDataType.TRANSACTIONS in selectedTypes) {
            buildTransactionBatches(profile, currency, range, cursors)
        } else {
            listOf(emptyList())
        }

        return transactionBatches.mapIndexed { index, transactions ->
            val updates = cursorUpdates.toMutableList()
            if (WebhookDataType.TRANSACTIONS in selectedTypes) {
                updates += WebhookCursorUpdate(WebhookDataType.TRANSACTIONS, LocalDateTime.now(), range.end)
            }
            WebhookBatchPayload(
                envelope = WebhookEnvelope(
                    generatedAt = LocalDateTime.now().format(formatter),
                    app = WebhookAppInfo(name = "Cashiro", version = BuildConfig.VERSION_NAME),
                    profile = WebhookProfileInfo(id = profile.id, name = profile.name),
                    request = WebhookRequestInfo(
                        range = range.preset.name.lowercase(),
                        start = range.start.format(formatter),
                        end = range.end.format(formatter),
                        currency = currency,
                        dataTypes = selectedTypes.map { it.name.lowercase() }
                    ),
                    batch = WebhookBatchInfo(
                        id = UUID.randomUUID().toString(),
                        index = index + 1,
                        count = transactionBatches.size
                    ),
                    summary = if (index == 0) summary else null,
                    transactions = transactions,
                    budgets = if (index == 0) budgets else emptyList(),
                    accounts = if (index == 0) accounts else emptyList(),
                    subscriptions = if (index == 0) subscriptions else emptyList()
                ),
                cursorUpdates = updates,
                // Only the first batch carries summary/budgets/accounts/subscriptions; later
                // batches are transactions-only. Counting them on every batch overstated metrics
                // for multi-batch syncs.
                itemCount = transactions.size +
                    if (index == 0) {
                        budgets.size + accounts.size + subscriptions.size + (if (summary != null) 1 else 0)
                    } else 0
            )
        }
    }

    private suspend fun buildSummary(range: WebhookDateRange, currency: String): WebhookSummaryPayload {
        val transactions = transactionRepository.getTransactionsFiltered(
            startDate = range.start.toLocalDate(),
            endDate = range.end.toLocalDate(),
            currency = currency,
            transactionType = null
        ).first().filterNot { it.isSample }
        val income = transactions.filter { it.transactionType == TransactionType.INCOME }.sumAmounts()
        val expense = transactions.filter { it.transactionType == TransactionType.EXPENSE }.sumAmounts()
        val categories = transactions
            .groupBy { it.category to it.subcategory }
            .map { (key, items) ->
                WebhookCategorySummaryPayload(
                    category = key.first,
                    subcategory = key.second,
                    transactionCount = items.size,
                    amount = items.sumAmounts().asPlainStringSafe()
                )
            }
            .sortedByDescending { it.amount.toBigDecimalOrNull() ?: BigDecimal.ZERO }
        return WebhookSummaryPayload(
            totalIncome = income.asPlainStringSafe(),
            totalExpense = expense.asPlainStringSafe(),
            netAmount = (income - expense).asPlainStringSafe(),
            currency = currency,
            categories = categories
        )
    }

    private suspend fun buildBudgets(range: WebhookDateRange, currency: String): List<WebhookBudgetPayload> {
        val budgets = budgetRepository.getAllBudgets().first()
            .filter { !it.isSample && it.currency.equals(currency, ignoreCase = true) }
        return budgets.map { budget ->
            val spending = transactionRepository.getTransactionsBetweenDates(
                budget.startDate,
                budget.endDate
            ).first()
                .filter { !it.isSample && it.currency.equals(currency, ignoreCase = true) && it.transactionType == TransactionType.EXPENSE }
                .sumAmounts()
            val limits = budgetRepository.getCategoryLimitsForBudgetSync(budget.id)
            WebhookBudgetPayload(
                id = "budget_${budget.id}",
                name = budget.name,
                amount = budget.amount.asPlainStringSafe(),
                currency = budget.currency,
                periodType = budget.periodType.name,
                budgetType = budget.budgetType.name,
                startDate = budget.startDate.format(formatter),
                endDate = budget.endDate.format(formatter),
                spending = spending.asPlainStringSafe(),
                categories = limits.map {
                    WebhookBudgetCategoryPayload(
                        category = it.categoryName,
                        limit = it.limitAmount.asPlainStringSafe()
                    )
                }
            )
        }
    }

    private suspend fun buildAccounts(currency: String): List<WebhookAccountPayload> {
        val accounts = accountBalanceRepository.getAllLatestBalances().first()
            .filter { !it.isSample && it.currency.equals(currency, ignoreCase = true) }
        return accounts.map { account ->
            WebhookAccountPayload(
                id = "account_${account.bankName}_${account.accountLast4}",
                bankName = account.bankName,
                accountLast4 = account.accountLast4,
                balance = account.balance.asPlainStringSafe(),
                currency = account.currency,
                creditLimit = account.creditLimit?.asPlainStringSafe(),
                isCreditCard = account.isCreditCard,
                isWallet = account.isWallet,
                timestamp = account.timestamp.format(formatter)
            )
        }
    }

    private suspend fun buildSubscriptions(currency: String): List<WebhookSubscriptionPayload> {
        val subscriptions = subscriptionRepository.getAllSubscriptions().first()
            .filter { !it.isSample && it.currency.equals(currency, ignoreCase = true) }
        return subscriptions.map {
            WebhookSubscriptionPayload(
                id = "subscription_${it.id}",
                merchant = it.merchantName,
                amount = it.amount.asPlainStringSafe(),
                currency = it.currency,
                state = it.state.name,
                category = it.category,
                subcategory = it.subcategory,
                billingCycle = it.billingCycle,
                nextPaymentDate = it.nextPaymentDate?.toString(),
                lastPaidDate = it.lastPaidDate?.toString()
            )
        }
    }

    private suspend fun buildTransactionBatches(
        profile: WebhookProfileEntity,
        currency: String,
        range: WebhookDateRange,
        cursors: List<WebhookCursorEntity>
    ): List<List<WebhookTransactionPayload>> {
        val transactions = if (profile.rangePreset == WebhookRangePreset.SINCE_LAST_SUCCESS) {
            val cursor = cursors.firstOrNull { it.dataType == WebhookDataType.TRANSACTIONS }?.lastSuccessAt
                ?: LocalDateTime.of(1970, 1, 1, 0, 0)
            transactionRepository.getTransactionsUpdatedBetween(cursor, range.end, currency)
        } else {
            transactionRepository.getTransactionsBetweenDatesByCurrency(range.start, range.end, currency)
        }
        return transactions
            .filter { !it.isSample }
            .map {
                if (it.isDeleted) {
                    WebhookTransactionPayload(
                        id = it.transactionHash.ifBlank { "txn_${it.id}" },
                        action = "delete",
                        updatedAt = it.updatedAt.format(formatter)
                    )
                } else {
                    WebhookTransactionPayload(
                        id = it.transactionHash.ifBlank { "txn_${it.id}" },
                        action = "upsert",
                        amount = it.amount.asPlainStringSafe(),
                        currency = it.currency,
                        merchant = it.merchantName,
                        description = it.description,
                        category = it.category,
                        subcategory = it.subcategory,
                        transactionType = it.transactionType.name,
                        dateTime = it.dateTime.format(formatter),
                        updatedAt = it.updatedAt.format(formatter),
                        bankName = it.bankName,
                        accountLast4 = it.accountNumber?.takeLast(4)
                    )
                }
            }
            .chunked(250)
            .ifEmpty { listOf(emptyList()) }
    }

    private fun buildTestPayload(
        profile: WebhookProfileEntity,
        selectedTypes: Set<WebhookDataType>,
        currency: String
    ): WebhookBatchPayload {
        return WebhookBatchPayload(
            envelope = WebhookEnvelope(
                generatedAt = LocalDateTime.now().format(formatter),
                app = WebhookAppInfo(name = "Cashiro", version = BuildConfig.VERSION_NAME),
                profile = WebhookProfileInfo(id = profile.id, name = profile.name),
                request = WebhookRequestInfo(
                    range = "test",
                    start = LocalDateTime.now().minusMinutes(1).format(formatter),
                    end = LocalDateTime.now().format(formatter),
                    currency = currency,
                    dataTypes = selectedTypes.map { it.name.lowercase() }
                ),
                batch = WebhookBatchInfo(
                    id = UUID.randomUUID().toString(),
                    index = 1,
                    count = 1
                ),
                summary = WebhookSummaryPayload(
                    totalIncome = "1200",
                    totalExpense = "450",
                    netAmount = "750",
                    currency = currency,
                    categories = listOf(
                        WebhookCategorySummaryPayload("Food", "Dining", 2, "250"),
                        WebhookCategorySummaryPayload("Travel", "Cab", 1, "200")
                    )
                ),
                transactions = listOf(
                    WebhookTransactionPayload(
                        id = "test_txn_1",
                        action = "upsert",
                        amount = "250",
                        currency = currency,
                        merchant = "Webhook Test Merchant",
                        description = "Synthetic test payload",
                        category = "Food",
                        subcategory = "Dining",
                        transactionType = TransactionType.EXPENSE.name,
                        dateTime = LocalDateTime.now().minusHours(2).format(formatter),
                        updatedAt = LocalDateTime.now().format(formatter),
                        bankName = "Cashiro",
                        accountLast4 = "1234"
                    )
                )
            ),
            cursorUpdates = emptyList(),
            itemCount = 1
        )
    }

    private fun resolveRange(
        profile: WebhookProfileEntity,
        cursors: List<WebhookCursorEntity>
    ): WebhookDateRange {
        val now = LocalDateTime.now()
        val today = LocalDate.now()
        val range = WebhookRange.fromFlat(profile.rangePreset, profile.customStart, profile.customEnd)
        return when (range) {
            WebhookRange.SinceLastSuccess -> {
                val cursor = cursors.firstOrNull { it.dataType == WebhookDataType.TRANSACTIONS }?.lastSuccessAt
                    ?: LocalDateTime.of(1970, 1, 1, 0, 0)
                WebhookDateRange(WebhookRangePreset.SINCE_LAST_SUCCESS, cursor, now)
            }
            WebhookRange.Today -> WebhookDateRange(WebhookRangePreset.TODAY, today.atStartOfDay(), now)
            WebhookRange.CurrentWeek ->
                WebhookDateRange(WebhookRangePreset.CURRENT_WEEK, today.startOfWeek().atStartOfDay(), now)
            WebhookRange.CurrentMonth ->
                WebhookDateRange(WebhookRangePreset.CURRENT_MONTH, today.withDayOfMonth(1).atStartOfDay(), now)
            WebhookRange.PreviousMonth -> {
                val previous = today.minusMonths(1)
                WebhookDateRange(
                    WebhookRangePreset.PREVIOUS_MONTH,
                    previous.withDayOfMonth(1).atStartOfDay(),
                    previous.withDayOfMonth(previous.lengthOfMonth()).atTime(LocalTime.MAX)
                )
            }
            WebhookRange.Last30Days ->
                WebhookDateRange(WebhookRangePreset.LAST_30_DAYS, today.minusDays(29).atStartOfDay(), now)
            is WebhookRange.Custom ->
                // The sealed type guarantees both bounds are present here; no `?:` fallback needed.
                WebhookDateRange(WebhookRangePreset.CUSTOM, range.start, range.end)
        }
    }
}

private fun Iterable<TransactionEntity>.sumAmounts(): BigDecimal =
    fold(BigDecimal.ZERO) { acc, item -> acc + item.amount }
