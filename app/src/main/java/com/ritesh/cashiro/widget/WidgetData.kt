package com.ritesh.cashiro.widget

import android.content.Context
import android.text.format.DateUtils
import androidx.annotation.StringRes
import androidx.glance.appwidget.updateAll
import com.ritesh.cashiro.R
import com.ritesh.cashiro.data.currency.CurrencyConversionService
import com.ritesh.cashiro.data.database.dao.AccountBalanceDao
import com.ritesh.cashiro.data.database.dao.TransactionDao
import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import com.ritesh.cashiro.data.database.entity.TransactionType
import com.ritesh.cashiro.data.preferences.UserPreferencesRepository
import com.ritesh.cashiro.data.repository.AppLockRepository
import com.ritesh.cashiro.data.repository.CurrencyRepository
import com.ritesh.cashiro.utils.CurrencyFormatter
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.flow.first

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun transactionDao(): TransactionDao
    fun accountBalanceDao(): AccountBalanceDao
    fun currencyRepository(): CurrencyRepository
    fun currencyConversionService(): CurrencyConversionService
    fun appLockRepository(): AppLockRepository
    fun userPreferencesRepository(): UserPreferencesRepository
}

internal fun widgetEntryPoint(context: Context): WidgetEntryPoint =
    EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)

enum class OverviewRange(val days: Long, val prefValue: String, @StringRes val labelRes: Int) {
    ONE_DAY(1, "1d", R.string.widget_range_1d),
    THREE_DAYS(3, "3d", R.string.widget_range_3d),
    ONE_WEEK(7, "1w", R.string.widget_range_1w);

    companion object {
        fun fromPrefValue(value: String?): OverviewRange =
            entries.firstOrNull { it.prefValue == value } ?: ONE_DAY
    }
}

data class OverviewTotals(val income: BigDecimal, val expense: BigDecimal)

data class OverviewData(
    val currency: String,
    val totalsByRange: Map<OverviewRange, OverviewTotals>,
)

data class AccountItem(
    val bankName: String,
    val accountLast4: String,
    val balance: BigDecimal,
    val currency: String,
    val isCreditCard: Boolean,
    val color: String,
    val iconResId: Int,
    val iconName: String?,
)

data class TransactionItem(
    val id: Long,
    val merchant: String,
    val amount: BigDecimal,
    val currency: String,
    val type: TransactionType,
    val category: String,
    val subcategory: String?,
    val timeLabel: String,
)

data class AccountsWidgetData(
    val accounts: List<AccountItem>,
    val transactions: List<TransactionItem>,
)

/**
 * Loads income/expense totals for all three ranges in one query, converting every
 * transaction to the app's effective base currency so mixed-currency data sums correctly.
 */
suspend fun loadOverviewData(context: Context): OverviewData {
    val entryPoint = widgetEntryPoint(context)
    val baseCurrency = entryPoint.currencyRepository().effectiveBaseCurrencyCode.first()
    val conversionService = entryPoint.currencyConversionService()

    val today = LocalDate.now()
    val end = today.atTime(23, 59, 59)
    val widestStart = today.minusDays(OverviewRange.ONE_WEEK.days - 1).atStartOfDay()
    val transactions = entryPoint.transactionDao()
        .getTransactionsBetweenDatesList(widestStart, end)
        .filter { !it.isSample }

    val convertedTransactions = transactions.map { transaction ->
        transaction to conversionService.convertAmount(
            amount = transaction.amount,
            fromCurrency = transaction.currency,
            toCurrency = baseCurrency,
        )
    }

    val totalsByRange = OverviewRange.entries.associateWith { range ->
        val rangeStart = today.minusDays(range.days - 1).atStartOfDay()
        convertedTransactions.fold(OverviewTotals(BigDecimal.ZERO, BigDecimal.ZERO)) { totals, converted ->
            val (transaction, amount) = converted
            if (transaction.dateTime < rangeStart) {
                totals
            } else {
                addToOverviewTotals(totals, transaction.transactionType, amount)
            }
        }
    }

    return OverviewData(currency = baseCurrency, totalsByRange = totalsByRange)
}

internal fun addToOverviewTotals(
    totals: OverviewTotals,
    type: TransactionType,
    amount: BigDecimal,
): OverviewTotals = when (type) {
    TransactionType.INCOME -> totals.copy(income = totals.income + amount)
    TransactionType.EXPENSE, TransactionType.CREDIT -> totals.copy(expense = totals.expense + amount)
    else -> totals
}

internal fun formatCompactCurrency(amount: BigDecimal, currency: String): String {
    val absolute = amount.abs()
    val divisorAndSuffix = when {
        absolute >= BigDecimal("1000000000") -> BigDecimal("1000000000") to "B"
        absolute >= BigDecimal("1000000") -> BigDecimal("1000000") to "M"
        absolute >= BigDecimal("1000") -> BigDecimal("1000") to "K"
        else -> return CurrencyFormatter.formatCurrency(amount, currency)
    }
    val (divisor, suffix) = divisorAndSuffix
    val compactAmount = absolute.divide(divisor, 1, RoundingMode.HALF_UP)
        .stripTrailingZeros()
        .toPlainString()
    val sign = if (amount.signum() < 0) "-" else ""
    return "$sign${CurrencyFormatter.getCurrencySymbol(currency)}$compactAmount$suffix"
}

/**
 * Checks whether the widget should show locked state.
 * Widget is locked if:
 * 1. App lock is enabled AND
 * 2. The app hasn't been authenticated (no auth timestamp or expired)
 */
suspend fun isWidgetLocked(context: Context): Boolean {
    val entryPoint = widgetEntryPoint(context)
    val appLockRepository = entryPoint.appLockRepository()
    return appLockRepository.shouldLockApp()
}

internal suspend fun resolveWidgetAccessState(context: Context): WidgetAccessState =
    runCatching { isWidgetLocked(context) }
        .fold(
            onSuccess = { if (it) WidgetAccessState.LOCKED else WidgetAccessState.UNLOCKED },
            onFailure = { WidgetAccessState.LOCKED },
        )

suspend fun updateCashiroWidgets(context: Context) {
    OverviewWidget().updateAll(context)
    AccountsWidget().updateAll(context)
}

/** Returns all distinct account bank+last4 pairs for widget config. */
suspend fun loadAvailableAccounts(context: Context): List<AccountItem> {
    val entryPoint = widgetEntryPoint(context)
    return entryPoint.accountBalanceDao().getAllLatestBalances().first()
        .filter { !it.isSample }
        .map(AccountBalanceEntity::toAccountItem)
}

/** Returns all distinct category names for widget config. */
suspend fun loadAvailableCategories(context: Context): List<String> {
    val entryPoint = widgetEntryPoint(context)
    return entryPoint.transactionDao().getAllCategories().first()
}

/** Loads the latest balance per account plus the most recent transactions. */
suspend fun loadAccountsWidgetData(
    context: Context,
    maxAccounts: Int = 4,
    maxTransactions: Int = 6,
    filterAccountKeys: Set<String>? = null,
    filterCategories: Set<String>? = null,
): AccountsWidgetData {
    val entryPoint = widgetEntryPoint(context)

    val accounts = entryPoint.accountBalanceDao().getAllLatestBalances().first()
        .filter { !it.isSample }
        .let { list ->
            if (filterAccountKeys != null) {
                list.filter { "${it.bankName}::${it.accountLast4}" in filterAccountKeys }
            } else {
                list
            }
        }
        .take(maxAccounts)
        .map(AccountBalanceEntity::toAccountItem)

    val transactions = entryPoint.transactionDao().getAllTransactions().first()
        .asSequence()
        .filter { !it.isSample && it.transactionType != TransactionType.BALANCE_UPDATE }
        .let { seq ->
            if (filterCategories != null) {
                seq.filter { it.category in filterCategories }
            } else {
                seq
            }
        }
        .take(maxTransactions)
        .map { transaction ->
            TransactionItem(
                id = transaction.id,
                merchant = transaction.merchantName,
                amount = transaction.amount,
                currency = transaction.currency,
                type = transaction.transactionType,
                category = transaction.category,
                subcategory = transaction.subcategory,
                timeLabel = relativeTimeLabel(transaction.dateTime),
            )
        }
        .toList()

    return AccountsWidgetData(accounts = accounts, transactions = transactions)
}

private fun AccountBalanceEntity.toAccountItem(): AccountItem = AccountItem(
    bankName = bankName,
    accountLast4 = accountLast4,
    balance = balance,
    currency = currency,
    isCreditCard = isCreditCard,
    color = color,
    iconResId = iconResId,
    iconName = iconName,
)

private fun relativeTimeLabel(dateTime: LocalDateTime): String {
    val millis = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    return DateUtils.getRelativeTimeSpanString(
        millis,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE,
    ).toString()
}
