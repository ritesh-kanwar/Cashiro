package com.ritesh.cashiro.data.database.dao

import androidx.room.*
<<<<<<< ours
import com.ritesh.cashiro.data.database.entity.BudgetCategoryLimitEntity
import com.ritesh.cashiro.data.database.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow
=======
import com.ritesh.cashiro.data.database.entity.BudgetCategoryEntity
import com.ritesh.cashiro.data.database.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
>>>>>>> theirs

@Dao
interface BudgetDao {

<<<<<<< ours
    // Budget operations
    @Query("SELECT * FROM budgets ORDER BY year DESC, month DESC")
    fun getAllBudgets(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE is_active = 1 ORDER BY year DESC, month DESC")
    fun getActiveBudgets(): Flow<List<BudgetEntity>>
=======
    @Query("SELECT * FROM budgets WHERE is_active = 1 ORDER BY created_at DESC")
    fun getActiveBudgets(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets ORDER BY created_at DESC")
    fun getAllBudgets(): Flow<List<BudgetEntity>>
    
    @Query("SELECT * FROM budget_categories ORDER BY budget_id")
    fun getAllBudgetCategories(): Flow<List<BudgetCategoryEntity>>

    @Query("""
        SELECT * FROM budgets
        WHERE is_active = 1
        AND :today >= start_date
        AND :today <= end_date
        ORDER BY created_at DESC
    """)
    fun getCurrentBudgets(today: LocalDate): Flow<List<BudgetEntity>>
>>>>>>> theirs

    @Query("SELECT * FROM budgets WHERE id = :budgetId")
    suspend fun getBudgetById(budgetId: Long): BudgetEntity?

<<<<<<< ours
    @Query("SELECT * FROM budgets WHERE year = :year AND month = :month LIMIT 1")
    suspend fun getBudgetByYearMonth(year: Int, month: Int): BudgetEntity?

    @Query("SELECT * FROM budgets WHERE year = :year AND month = :month AND is_active = 1")
    fun getActiveBudgetsForMonth(year: Int, month: Int): Flow<List<BudgetEntity>>
=======
    @Query("SELECT * FROM budgets WHERE id = :budgetId")
    fun getBudgetByIdFlow(budgetId: Long): Flow<BudgetEntity?>

    @Query("SELECT * FROM budgets WHERE name = :name AND is_active = 1 LIMIT 1")
    suspend fun getActiveBudgetByName(name: String): BudgetEntity?
>>>>>>> theirs

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity): Long

    @Update
    suspend fun updateBudget(budget: BudgetEntity)

<<<<<<< ours
    @Query("DELETE FROM budgets WHERE id = :budgetId")
    suspend fun deleteBudget(budgetId: Long)

    @Query("DELETE FROM budgets")
    suspend fun deleteAllBudgets()

    @Query("DELETE FROM budgets WHERE is_sample = 1")
    suspend fun deleteSampleBudgets()

    // Category limit operations
    @Query("SELECT * FROM budget_category_limits")
    fun getAllCategoryLimits(): Flow<List<BudgetCategoryLimitEntity>>

    @Query("SELECT * FROM budget_category_limits WHERE budget_id = :budgetId")
    fun getCategoryLimitsForBudget(budgetId: Long): Flow<List<BudgetCategoryLimitEntity>>

    @Query("SELECT * FROM budget_category_limits WHERE budget_id = :budgetId")
    suspend fun getCategoryLimitsForBudgetSync(budgetId: Long): List<BudgetCategoryLimitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategoryLimit(limit: BudgetCategoryLimitEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategoryLimits(limits: List<BudgetCategoryLimitEntity>)

    @Update
    suspend fun updateCategoryLimit(limit: BudgetCategoryLimitEntity)

    @Query("DELETE FROM budget_category_limits WHERE id = :limitId")
    suspend fun deleteCategoryLimit(limitId: Long)

    @Query("DELETE FROM budget_category_limits WHERE budget_id = :budgetId")
    suspend fun deleteCategoryLimitsForBudget(budgetId: Long)

    @Query("DELETE FROM budget_category_limits WHERE budget_id = :budgetId AND category_name = :categoryName")
    suspend fun deleteCategoryLimitByName(budgetId: Long, categoryName: String)
=======
    @Query("UPDATE budgets SET is_active = 0, updated_at = datetime('now') WHERE id = :budgetId")
    suspend fun deactivateBudget(budgetId: Long)

    @Delete
    suspend fun deleteBudget(budget: BudgetEntity)

    @Query("DELETE FROM budgets WHERE id = :budgetId")
    suspend fun deleteBudgetById(budgetId: Long)

    // Budget Categories
    @Query("SELECT * FROM budget_categories WHERE budget_id = :budgetId")
    fun getCategoriesForBudget(budgetId: Long): Flow<List<BudgetCategoryEntity>>

    @Query("SELECT * FROM budget_categories WHERE budget_id = :budgetId")
    suspend fun getCategoriesForBudgetList(budgetId: Long): List<BudgetCategoryEntity>

    @Query("SELECT category_name FROM budget_categories WHERE budget_id = :budgetId")
    suspend fun getCategoryNamesForBudget(budgetId: Long): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgetCategory(category: BudgetCategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgetCategories(categories: List<BudgetCategoryEntity>)

    @Query("DELETE FROM budget_categories WHERE budget_id = :budgetId")
    suspend fun deleteCategoriesForBudget(budgetId: Long)

    @Query("DELETE FROM budget_categories WHERE id = :categoryId")
    suspend fun deleteBudgetCategoryById(categoryId: Long)

    // Get budgets that include a specific category (for spending calculation)
    @Query("""
        SELECT b.* FROM budgets b
        INNER JOIN budget_categories bc ON b.id = bc.budget_id
        WHERE bc.category_name = :categoryName
        AND b.is_active = 1
        AND :today >= b.start_date
        AND :today <= b.end_date
    """)
    suspend fun getBudgetsForCategory(categoryName: String, today: LocalDate): List<BudgetEntity>

    // Get budgets that include all categories
    @Query("""
        SELECT * FROM budgets
        WHERE include_all_categories = 1
        AND is_active = 1
        AND :today >= start_date
        AND :today <= end_date
    """)
    suspend fun getBudgetsWithAllCategories(today: LocalDate): List<BudgetEntity>

    @Query("DELETE FROM budgets")
    suspend fun deleteAllBudgets()
>>>>>>> theirs
}
