package com.ritesh.cashiro.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
<<<<<<< ours
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Entity representing a monthly budget.
 * Users can create multiple budgets, each tied to a specific month and year.
 */
@Entity(tableName = "budgets")
=======
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(
    tableName = "budgets",
    indices = [
        Index(value = ["name"]),
        Index(value = ["is_active"])
    ]
)
>>>>>>> theirs
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

<<<<<<< ours
    @ColumnInfo(name = "amount")
    val amount: BigDecimal,

    @ColumnInfo(name = "year")
    val year: Int,

    @ColumnInfo(name = "month")
    val month: Int,
=======
    @ColumnInfo(name = "limit_amount")
    val limitAmount: BigDecimal,

    @ColumnInfo(name = "period_type")
    val periodType: BudgetPeriodType,

    @ColumnInfo(name = "start_date")
    val startDate: LocalDate,

    @ColumnInfo(name = "end_date")
    val endDate: LocalDate,
>>>>>>> theirs

    @ColumnInfo(name = "currency", defaultValue = "INR")
    val currency: String = "INR",

    @ColumnInfo(name = "is_active", defaultValue = "1")
    val isActive: Boolean = true,

<<<<<<< ours
=======
    @ColumnInfo(name = "include_all_categories", defaultValue = "0")
    val includeAllCategories: Boolean = false,

    @ColumnInfo(name = "color", defaultValue = "#1565C0")
    val color: String = "#1565C0",

>>>>>>> theirs
    @ColumnInfo(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @ColumnInfo(name = "updated_at")
<<<<<<< ours
    val updatedAt: LocalDateTime = LocalDateTime.now(),

    // --- New Fields ---
    @ColumnInfo(name = "start_date", defaultValue = "")
    val startDate: LocalDateTime = LocalDateTime.now(),

    @ColumnInfo(name = "end_date", defaultValue = "")
    val endDate: LocalDateTime = LocalDateTime.now().plusMonths(1),

    @ColumnInfo(name = "period_type", defaultValue = "MONTHLY")
    val periodType: BudgetPeriod = BudgetPeriod.MONTHLY,

    @ColumnInfo(name = "track_type", defaultValue = "ALL_TRANSACTIONS")
    val trackType: BudgetTrackType = BudgetTrackType.ALL_TRANSACTIONS,

    @ColumnInfo(name = "budget_type", defaultValue = "EXPENSE")
    val budgetType: BudgetType = BudgetType.EXPENSE,

    @ColumnInfo(name = "account_ids", defaultValue = "")
    val accountIds: List<String> = emptyList(), // List of "BankName:Last4"

    @ColumnInfo(name = "color", defaultValue = "#4CAF50") // Default Green
    val color: String = "#4CAF50",

    @ColumnInfo(name = "is_sample", defaultValue = "0")
    val isSample: Boolean = false
)

enum class BudgetPeriod {
    CUSTOM,
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY
}

enum class BudgetTrackType {
    ADDED_ONLY,
    ALL_TRANSACTIONS
}

enum class BudgetType {
    EXPENSE,
    SAVINGS
=======
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

enum class BudgetPeriodType {
    WEEKLY,
    MONTHLY,
    CUSTOM
>>>>>>> theirs
}
