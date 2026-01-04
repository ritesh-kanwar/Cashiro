package com.ritesh.cashiro.data.repository

import com.ritesh.cashiro.data.database.dao.BudgetDao
import com.ritesh.cashiro.data.database.dao.TransactionDao
<<<<<<< ours
import com.ritesh.cashiro.data.database.entity.BudgetCategoryLimitEntity
import com.ritesh.cashiro.data.database.entity.BudgetEntity
import com.ritesh.cashiro.data.database.entity.BudgetTrackType
import com.ritesh.cashiro.data.database.entity.BudgetType
import com.ritesh.cashiro.data.database.entity.TransactionEntity
import com.ritesh.cashiro.data.database.entity.TransactionType
import com.ritesh.cashiro.data.currency.CurrencyConversionService
import com.ritesh.cashiro.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.LocalDateTime
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

data class BudgetWithSpending(
    val budget: BudgetEntity,
    val currentSpending: BigDecimal,
    val categoryLimits: List<BudgetCategoryLimitEntity>,
    val categorySpending: Map<String, BigDecimal>,
    val daysRemaining: Int,
    val daysInMonth: Int
) {
    val remaining: BigDecimal get() = budget.amount - currentSpending
    val percentUsed: Float get() = if (budget.amount > BigDecimal.ZERO) {
        (currentSpending.toFloat() / budget.amount.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val isOverBudget: Boolean get() = currentSpending > budget.amount
    val spendingPerDay: BigDecimal get() {
        val daysPassed = daysInMonth - daysRemaining
        return if (daysPassed > 0) {
            currentSpending.divide(BigDecimal(daysPassed), 2, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO
    }
    val recommendedDailySpending: BigDecimal get() {
        return if (daysRemaining > 0 && remaining > BigDecimal.ZERO) {
            remaining.divide(BigDecimal(daysRemaining), 2, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO
    }
}

data class CategoryLimitWithSpending(
    val limit: BudgetCategoryLimitEntity,
    val currentSpending: BigDecimal
) {
    val remaining: BigDecimal get() = limit.limitAmount - currentSpending
    val percentUsed: Float get() = if (limit.limitAmount > BigDecimal.ZERO) {
        (currentSpending.toFloat() / limit.limitAmount.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val isOverLimit: Boolean get() = currentSpending > limit.limitAmount
}

=======
import com.ritesh.cashiro.data.database.dao.TransactionSplitDao
import com.ritesh.cashiro.data.database.entity.BudgetCategoryEntity
import com.ritesh.cashiro.data.database.entity.BudgetEntity
import com.ritesh.cashiro.data.database.entity.BudgetPeriodType
import com.ritesh.cashiro.data.database.entity.TransactionType
import com.ritesh.cashiro.data.database.entity.TransactionWithSplits
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import javax.inject.Singleton

>>>>>>> theirs
@Singleton
class BudgetRepository @Inject constructor(
    private val budgetDao: BudgetDao,
    private val transactionDao: TransactionDao,
<<<<<<< ours
    private val currencyConversionService: CurrencyConversionService,
    @ApplicationScope private val externalScope: CoroutineScope
) {

    val allBudgets: StateFlow<List<BudgetEntity>> = budgetDao.getAllBudgets()
        .stateIn(
            scope = externalScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    fun getAllBudgets(): Flow<List<BudgetEntity>> {
        return budgetDao.getAllBudgets()
    }

    fun getActiveBudgets(): Flow<List<BudgetEntity>> {
        return budgetDao.getActiveBudgets()
    }

    suspend fun getBudgetById(budgetId: Long): BudgetEntity? {
        return budgetDao.getBudgetById(budgetId)
    }

    suspend fun getBudgetByYearMonth(year: Int, month: Int): BudgetEntity? {
        return budgetDao.getBudgetByYearMonth(year, month)
    }

    fun getActiveBudgetsForMonth(year: Int, month: Int): Flow<List<BudgetEntity>> {
        return budgetDao.getActiveBudgetsForMonth(year, month)
    }

    suspend fun createBudget(
        name: String,
        amount: BigDecimal,
        year: Int,
        month: Int,
        currency: String = "INR"
    ): Long {
        val budget = BudgetEntity(
            name = name,
            amount = amount,
            year = year,
            month = month,
            currency = currency,
            isActive = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        return budgetDao.insertBudget(budget)
    }

    suspend fun insertBudget(budget: BudgetEntity): Long {
        return budgetDao.insertBudget(budget)
    }

    suspend fun updateBudget(budget: BudgetEntity) {
        budgetDao.updateBudget(budget.copy(updatedAt = LocalDateTime.now()))
    }

    suspend fun deleteBudget(budgetId: Long) {
        budgetDao.deleteBudget(budgetId)
    }

    suspend fun deleteSampleBudgets() {
        budgetDao.deleteSampleBudgets()
    }

    suspend fun deleteAllBudgets() {
        budgetDao.deleteAllBudgets()
    }

    fun getCategoryLimitsForBudget(budgetId: Long): Flow<List<BudgetCategoryLimitEntity>> {
        return budgetDao.getCategoryLimitsForBudget(budgetId)
    }

    suspend fun getCategoryLimitsForBudgetSync(budgetId: Long): List<BudgetCategoryLimitEntity> {
        return budgetDao.getCategoryLimitsForBudgetSync(budgetId)
    }

    suspend fun addCategoryLimit(
        budgetId: Long,
        categoryName: String,
        limitAmount: BigDecimal
    ): Long {
        val limit = BudgetCategoryLimitEntity(
            budgetId = budgetId,
            categoryName = categoryName,
            limitAmount = limitAmount,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        return budgetDao.insertCategoryLimit(limit)
    }

    suspend fun updateCategoryLimit(limit: BudgetCategoryLimitEntity) {
        budgetDao.updateCategoryLimit(limit.copy(updatedAt = LocalDateTime.now()))
    }

    suspend fun deleteCategoryLimit(limitId: Long) {
        budgetDao.deleteCategoryLimit(limitId)
    }

    suspend fun deleteCategoryLimitsForBudget(budgetId: Long) {
        budgetDao.deleteCategoryLimitsForBudget(budgetId)
    }

    // Spending calculation methods
    suspend fun getBudgetWithSpending(budget: BudgetEntity): BudgetWithSpending {
        val startDate = budget.startDate
        val endDate = budget.endDate
        val now = LocalDateTime.now()

        // Get transactions for the budget period
        var transactions = transactionDao.getTransactionsBetweenDatesList(startDate, endDate)
            .filter { !it.isDeleted }
            
        // Filter by budget type
        transactions = if (budget.budgetType == BudgetType.EXPENSE) {
            transactions.filter { it.transactionType == TransactionType.EXPENSE || it.transactionType == TransactionType.CREDIT }
        } else {
            transactions.filter { it.transactionType == TransactionType.INCOME }
        }
        
        // Filter by tracking type
        if (budget.trackType == BudgetTrackType.ADDED_ONLY) {
            transactions = transactions.filter { it.smsBody.isNullOrBlank() }
        }
        
        // Filter by accounts if specified
        if (budget.accountIds.isNotEmpty()) {
            transactions = transactions.filter { txn ->
                val accountKey = "${txn.bankName}:${txn.accountNumber?.takeLast(4) ?: ""}"
                budget.accountIds.any { it.contains(txn.bankName ?: "") && it.contains(txn.accountNumber?.takeLast(4) ?: "") }
            }
        }
        
        // Convert currencies to match the budget's currency
        transactions = transactions.map { txn ->
            if (txn.currency != budget.currency) {
                val convertedAmount = currencyConversionService.convertAmount(txn.amount, txn.currency, budget.currency)
                txn.copy(amount = convertedAmount ?: txn.amount, currency = budget.currency)
            } else {
                txn
            }
        }

        val totalSpending = transactions.sumOf { it.amount }

        // Calculate spending per category
        val categorySpending = transactions
            .groupBy { it.category }
            .mapValues { (_, txns) -> txns.sumOf { it.amount } }

        val categoryLimits = budgetDao.getCategoryLimitsForBudgetSync(budget.id)

        // Calculate days remaining
        val duration = Duration.between(startDate, endDate)
        val totalDays = duration.toDays().toInt().coerceAtLeast(1)
        
        val daysRemaining = if (now.isBefore(startDate)) {
            totalDays
        } else if (now.isAfter(endDate)) {
            0
        } else {
            Duration.between(now, endDate).toDays().toInt().coerceAtLeast(0)
        }

        return BudgetWithSpending(
            budget = budget,
            currentSpending = totalSpending,
            categoryLimits = categoryLimits,
            categorySpending = categorySpending,
            daysRemaining = daysRemaining,
            daysInMonth = totalDays
        )
    }

    fun getBudgetsWithSpendingForMonth(year: Int, month: Int): Flow<List<BudgetWithSpending>> {
        val startOfMonth = YearMonth.of(year, month).atDay(1).atStartOfDay()
        val endOfMonth = YearMonth.of(year, month).atEndOfMonth().atTime(23, 59, 59)
        
        return combine(
            budgetDao.getAllBudgets(),
            transactionDao.getAllTransactions(),
            budgetDao.getAllCategoryLimits()
        ) { budgets, transactions, categoryLimits ->
            budgets.filter { budget ->
                budget.isActive && (
                    (budget.startDate.isBefore(endOfMonth) || budget.startDate.isEqual(endOfMonth)) &&
                    (budget.endDate.isAfter(startOfMonth) || budget.endDate.isEqual(startOfMonth))
                )
            }.map { budget ->
                calculateSpending(budget, transactions, categoryLimits)
            }
        }
    }

    fun getAllBudgetsWithSpending(): Flow<List<BudgetWithSpending>> {
        return combine(
            budgetDao.getAllBudgets(),
            transactionDao.getAllTransactions(),
            budgetDao.getAllCategoryLimits()
        ) { budgets, transactions, categoryLimits ->
            budgets.map { budget ->
                calculateSpending(budget, transactions, categoryLimits)
            }
        }
    }

    private suspend fun calculateSpending(
        budget: BudgetEntity,
        allTransactions: List<TransactionEntity>,
        allCategoryLimits: List<BudgetCategoryLimitEntity>
    ): BudgetWithSpending {
        val startDate = budget.startDate
        val endDate = budget.endDate
        val now = LocalDateTime.now()

        // Filter transactions for this specific budget (already excludes deleted by DAO)
        var transactions = allTransactions.filter { txn ->
            (txn.dateTime.isAfter(startDate) || txn.dateTime.isEqual(startDate)) &&
            (txn.dateTime.isBefore(endDate) || txn.dateTime.isEqual(endDate))
        }

        // Filter by budget type
        transactions = if (budget.budgetType == BudgetType.EXPENSE) {
            transactions.filter { it.transactionType == TransactionType.EXPENSE || it.transactionType == TransactionType.CREDIT }
        } else {
            transactions.filter { it.transactionType == TransactionType.INCOME }
        }
        
        // Filter by tracking type
        if (budget.trackType == BudgetTrackType.ADDED_ONLY) {
            transactions = transactions.filter { it.smsBody.isNullOrBlank() }
        }
        
        // Filter by accounts if specified
        if (budget.accountIds.isNotEmpty()) {
            transactions = transactions.filter { txn ->
                budget.accountIds.any { it.contains(txn.bankName ?: "") && it.contains(txn.accountNumber?.takeLast(4) ?: "") }
            }
        }

        // Convert currencies to match the budget's currency
        transactions = transactions.map { txn ->
            if (txn.currency != budget.currency) {
                val convertedAmount = currencyConversionService.convertAmount(txn.amount, txn.currency, budget.currency)
                txn.copy(amount = convertedAmount ?: txn.amount, currency = budget.currency)
            } else {
                txn
            }
        }

        // Calculate total spending
        val totalSpending = transactions.sumOf { it.amount }

        // Calculate spending per category
        val categorySpending = transactions
            .groupBy { it.category }
            .mapValues { (_, txns) -> txns.sumOf { it.amount } }

        // Get category limits for this budget
        val categoryLimits = allCategoryLimits.filter { it.budgetId == budget.id }

        // Calculate days remaining
        val duration = Duration.between(startDate, endDate)
        val totalDays = duration.toDays().toInt().coerceAtLeast(1)
        
        val daysRemaining = if (now.isBefore(startDate)) {
            totalDays
        } else if (now.isAfter(endDate)) {
            0
        } else {
            Duration.between(now, endDate).toDays().toInt().coerceAtLeast(0)
        }

        return BudgetWithSpending(
            budget = budget,
            currentSpending = totalSpending,
            categoryLimits = categoryLimits,
            categorySpending = categorySpending,
            daysRemaining = daysRemaining,
            daysInMonth = totalDays
        )
    }

    suspend fun getCategoryLimitsWithSpending(budgetId: Long): List<CategoryLimitWithSpending> {
        val budget = budgetDao.getBudgetById(budgetId) ?: return emptyList()
        val budgetWithSpending = getBudgetWithSpending(budget)

        return budgetWithSpending.categoryLimits.map { limit ->
            CategoryLimitWithSpending(
                limit = limit,
                currentSpending = budgetWithSpending.categorySpending[limit.categoryName] ?: BigDecimal.ZERO
            )
        }
    }

    fun getTransactionsForBudget(budget: BudgetEntity): Flow<List<TransactionEntity>> {
        val startDate = budget.startDate
        val endDate = budget.endDate

        return transactionDao.getTransactionsBetweenDates(startDate, endDate)
            .map { transactions ->
                var filtered = transactions.filter { !it.isDeleted }
                
                // Filter by budget type
                filtered = if (budget.budgetType == BudgetType.EXPENSE) {
                    filtered.filter { it.transactionType == TransactionType.EXPENSE || it.transactionType == TransactionType.CREDIT }
                } else {
                    filtered.filter { it.transactionType == TransactionType.INCOME }
                }
                
                // Filter by tracking type
                if (budget.trackType == BudgetTrackType.ADDED_ONLY) {
                    filtered = filtered.filter { it.smsBody.isNullOrBlank() }
                }
                
                // Filter by accounts if specified
                if (budget.accountIds.isNotEmpty()) {
                    filtered = filtered.filter { txn ->
                        budget.accountIds.any { it.contains(txn.bankName ?: "") && it.contains(txn.accountNumber?.takeLast(4) ?: "") }
                    }
                }

                // Convert currencies to match the budget's currency
                filtered = filtered.map { txn ->
                    if (txn.currency != budget.currency) {
                        val convertedAmount = currencyConversionService.convertAmount(txn.amount, txn.currency, budget.currency)
                        txn.copy(amount = convertedAmount ?: txn.amount, currency = budget.currency)
                    } else {
                        txn
                    }
                }
                
                filtered
            }
    }
}
=======
    private val transactionSplitDao: TransactionSplitDao
) {
    fun getActiveBudgets(): Flow<List<BudgetEntity>> =
        budgetDao.getActiveBudgets()

    fun getAllBudgets(): Flow<List<BudgetEntity>> =
        budgetDao.getAllBudgets()

    fun getCurrentBudgets(): Flow<List<BudgetEntity>> =
        budgetDao.getCurrentBudgets(LocalDate.now())

    suspend fun getBudgetById(id: Long): BudgetEntity? =
        budgetDao.getBudgetById(id)

    fun getBudgetByIdFlow(id: Long): Flow<BudgetEntity?> =
        budgetDao.getBudgetByIdFlow(id)

    fun getCategoriesForBudget(budgetId: Long): Flow<List<BudgetCategoryEntity>> =
        budgetDao.getCategoriesForBudget(budgetId)

    suspend fun getCategoryNamesForBudget(budgetId: Long): List<String> =
        budgetDao.getCategoryNamesForBudget(budgetId)

    suspend fun createBudget(
        name: String,
        limitAmount: BigDecimal,
        periodType: BudgetPeriodType,
        startDate: LocalDate,
        endDate: LocalDate,
        currency: String,
        includeAllCategories: Boolean,
        categories: List<String>,
        color: String
    ): Long {
        val now = LocalDateTime.now()
        val budget = BudgetEntity(
            name = name,
            limitAmount = limitAmount,
            periodType = periodType,
            startDate = startDate,
            endDate = endDate,
            currency = currency,
            isActive = true,
            includeAllCategories = includeAllCategories,
            color = color,
            createdAt = now,
            updatedAt = now
        )
        val budgetId = budgetDao.insertBudget(budget)

        if (!includeAllCategories && categories.isNotEmpty()) {
            val budgetCategories = categories.map { categoryName ->
                BudgetCategoryEntity(
                    budgetId = budgetId,
                    categoryName = categoryName
                )
            }
            budgetDao.insertBudgetCategories(budgetCategories)
        }

        return budgetId
    }

    suspend fun updateBudget(
        budgetId: Long,
        name: String,
        limitAmount: BigDecimal,
        periodType: BudgetPeriodType,
        startDate: LocalDate,
        endDate: LocalDate,
        currency: String,
        includeAllCategories: Boolean,
        categories: List<String>,
        color: String
    ) {
        val existingBudget = budgetDao.getBudgetById(budgetId) ?: return
        val updatedBudget = existingBudget.copy(
            name = name,
            limitAmount = limitAmount,
            periodType = periodType,
            startDate = startDate,
            endDate = endDate,
            currency = currency,
            includeAllCategories = includeAllCategories,
            color = color,
            updatedAt = LocalDateTime.now()
        )
        budgetDao.updateBudget(updatedBudget)

        // Update categories
        budgetDao.deleteCategoriesForBudget(budgetId)
        if (!includeAllCategories && categories.isNotEmpty()) {
            val budgetCategories = categories.map { categoryName ->
                BudgetCategoryEntity(
                    budgetId = budgetId,
                    categoryName = categoryName
                )
            }
            budgetDao.insertBudgetCategories(budgetCategories)
        }
    }

    suspend fun deleteBudget(budgetId: Long) {
        budgetDao.deleteBudgetById(budgetId)
    }

    suspend fun deactivateBudget(budgetId: Long) {
        budgetDao.deactivateBudget(budgetId)
    }

    /**
     * Calculate spending for a budget.
     * This queries all EXPENSE transactions within the budget's date range and currency,
     * filtered by the budget's selected categories (or all categories if includeAllCategories is true).
     *
     * For transactions with splits, each split's amount is counted towards its respective category.
     * This ensures accurate budget tracking when a single transaction is split across multiple categories.
     */
    fun getBudgetSpending(budget: BudgetEntity): Flow<BudgetSpending> {
        // Use TransactionWithSplits to properly handle split transactions
        val transactionsWithSplitsFlow = transactionSplitDao.getTransactionsWithSplitsFiltered(
            startDate = budget.startDate.atStartOfDay(),
            endDate = budget.endDate.atTime(23, 59, 59),
            currency = budget.currency
        ).map { allTransactions ->
            // Filter to only include EXPENSE transactions
            allTransactions.filter { it.transaction.transactionType == TransactionType.EXPENSE }
        }

        val categoriesFlow = if (budget.includeAllCategories) {
            kotlinx.coroutines.flow.flowOf(emptyList())
        } else {
            budgetDao.getCategoriesForBudget(budget.id)
        }

        return combine(transactionsWithSplitsFlow, categoriesFlow) { transactionsWithSplits, budgetCategories ->
            val categoryNames = budgetCategories.map { it.categoryName }.toSet()

            // Build category amounts considering splits
            val categoryAmounts = mutableMapOf<String, BigDecimal>()
            var totalSpent = BigDecimal.ZERO
            var transactionCount = 0

            transactionsWithSplits.forEach { txWithSplits ->
                // Get amounts by category (handles both split and non-split transactions)
                val amountsByCategory = txWithSplits.getAmountByCategory()

                amountsByCategory.forEach { (category, amount) ->
                    val categoryName = category.ifEmpty { "Others" }

                    // Check if this category is included in the budget
                    val includeThisCategory = budget.includeAllCategories || categoryName in categoryNames

                    if (includeThisCategory) {
                        totalSpent += amount
                        categoryAmounts[categoryName] = (categoryAmounts[categoryName] ?: BigDecimal.ZERO) + amount
                    }
                }

                // Count the transaction if any portion is included in the budget
                val hasIncludedCategory = if (budget.includeAllCategories) {
                    true
                } else {
                    amountsByCategory.keys.any { cat ->
                        val categoryName = cat.ifEmpty { "Others" }
                        categoryName in categoryNames
                    }
                }
                if (hasIncludedCategory) {
                    transactionCount++
                }
            }

            val remaining = budget.limitAmount - totalSpent

            val percentageUsed = if (budget.limitAmount > BigDecimal.ZERO) {
                (totalSpent.toFloat() / budget.limitAmount.toFloat() * 100f).coerceIn(0f, 100f)
            } else {
                0f
            }

            // Sort category breakdown by amount
            val categoryBreakdown = categoryAmounts
                .toList()
                .sortedByDescending { it.second }
                .toMap()

            BudgetSpending(
                totalSpent = totalSpent,
                remaining = remaining,
                percentageUsed = percentageUsed,
                categoryBreakdown = categoryBreakdown,
                transactionCount = transactionCount
            )
        }
    }

    /**
     * Calculate the daily spending allowance for a budget.
     * This is: (remaining budget) / (days remaining in budget period)
     */
    fun calculateDailyAllowance(budget: BudgetEntity, spent: BigDecimal): BigDecimal {
        val today = LocalDate.now()
        val daysRemaining = ChronoUnit.DAYS.between(today, budget.endDate).toInt() + 1
        val remaining = budget.limitAmount - spent

        return if (daysRemaining > 0 && remaining > BigDecimal.ZERO) {
            remaining.divide(BigDecimal(daysRemaining), 2, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }
    }

    /**
     * Calculate days remaining in the budget period.
     */
    fun getDaysRemaining(budget: BudgetEntity): Int {
        val today = LocalDate.now()
        return (ChronoUnit.DAYS.between(today, budget.endDate).toInt() + 1).coerceAtLeast(0)
    }

    /**
     * Calculate the total days in the budget period.
     */
    fun getTotalDays(budget: BudgetEntity): Int {
        return (ChronoUnit.DAYS.between(budget.startDate, budget.endDate).toInt() + 1).coerceAtLeast(1)
    }

    /**
     * Calculate the progress through the budget period (0.0 to 1.0).
     */
    fun getTimeProgress(budget: BudgetEntity): Float {
        val today = LocalDate.now()
        val totalDays = getTotalDays(budget)
        val daysPassed = ChronoUnit.DAYS.between(budget.startDate, today).toInt() + 1
        return (daysPassed.toFloat() / totalDays.toFloat()).coerceIn(0f, 1f)
    }

    /**
     * Calculate start and end dates based on period type.
     */
    fun calculatePeriodDates(periodType: BudgetPeriodType, customStartDate: LocalDate? = null): Pair<LocalDate, LocalDate> {
        val today = LocalDate.now()
        return when (periodType) {
            BudgetPeriodType.WEEKLY -> {
                val startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val endOfWeek = startOfWeek.plusDays(6)
                startOfWeek to endOfWeek
            }
            BudgetPeriodType.MONTHLY -> {
                val startOfMonth = today.withDayOfMonth(1)
                val endOfMonth = today.with(TemporalAdjusters.lastDayOfMonth())
                startOfMonth to endOfMonth
            }
            BudgetPeriodType.CUSTOM -> {
                val start = customStartDate ?: today
                start to start.plusMonths(1)
            }
        }
    }

    /**
     * Renew a budget for the next period (for recurring budgets).
     */
    suspend fun renewBudget(budget: BudgetEntity) {
        val (newStartDate, newEndDate) = when (budget.periodType) {
            BudgetPeriodType.WEEKLY -> {
                budget.endDate.plusDays(1) to budget.endDate.plusDays(7)
            }
            BudgetPeriodType.MONTHLY -> {
                budget.endDate.plusDays(1) to budget.endDate.plusMonths(1)
            }
            BudgetPeriodType.CUSTOM -> {
                // Custom budgets don't auto-renew
                return
            }
        }

        val updatedBudget = budget.copy(
            startDate = newStartDate,
            endDate = newEndDate,
            updatedAt = LocalDateTime.now()
        )
        budgetDao.updateBudget(updatedBudget)
    }
}

/**
 * Data class representing the spending data for a budget.
 */
data class BudgetSpending(
    val totalSpent: BigDecimal,
    val remaining: BigDecimal,
    val percentageUsed: Float,
    val categoryBreakdown: Map<String, BigDecimal>,
    val transactionCount: Int
)
>>>>>>> theirs
