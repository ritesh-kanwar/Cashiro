package com.ritesh.cashiro.data.database.dao

import androidx.room.*
import com.ritesh.cashiro.data.database.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow
import com.ritesh.cashiro.data.database.entity.BudgetCategoryLimitEntity
import com.ritesh.cashiro.data.database.entity.RuleEntity
import com.ritesh.cashiro.data.database.entity.withCategoryRenamed
import java.math.BigDecimal
import java.time.LocalDateTime

@Dao
interface CategoryDao {
    
    @Query("SELECT * FROM categories ORDER BY display_order ASC, name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>
    
    @Query("SELECT * FROM categories WHERE is_income = 0 ORDER BY display_order ASC, name ASC")
    fun getExpenseCategories(): Flow<List<CategoryEntity>>
    
    @Query("SELECT * FROM categories WHERE is_income = 1 ORDER BY display_order ASC, name ASC")
    fun getIncomeCategories(): Flow<List<CategoryEntity>>
    
    @Query("SELECT * FROM categories WHERE id = :categoryId")
    suspend fun getCategoryById(categoryId: Long): CategoryEntity?
    
    @Query("SELECT * FROM categories WHERE name = :categoryName LIMIT 1")
    suspend fun getCategoryByName(categoryName: String): CategoryEntity?
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategory(category: CategoryEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategories(categories: List<CategoryEntity>)
    
    @Update
    suspend fun updateCategory(category: CategoryEntity)
    
    @Query("DELETE FROM categories WHERE id = :categoryId AND is_system = 0")
    suspend fun deleteCategory(categoryId: Long)
    
    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCategoryCount(): Int
    
    @Query("SELECT EXISTS(SELECT 1 FROM categories WHERE name = :categoryName)")
    suspend fun categoryExists(categoryName: String): Boolean
    
    @Query("DELETE FROM categories")
    suspend fun deleteAllCategories()

    @Query("SELECT * FROM categories WHERE name = :name COLLATE NOCASE AND id != :id LIMIT 1")
    suspend fun conflictingCategory(name: String, id: Long): CategoryEntity?

    @Query("UPDATE transactions SET category = :newName, updated_at = :now WHERE category = :oldName")
    suspend fun renameTransactions(oldName: String, newName: String, now: LocalDateTime)

    @Query("UPDATE merchant_mappings SET category = :newName WHERE category = :oldName")
    suspend fun renameMappings(oldName: String, newName: String)

    @Query("UPDATE subscriptions SET category = :newName WHERE category = :oldName")
    suspend fun renameSubscriptions(oldName: String, newName: String)

    @Query("UPDATE lend_borrow_transactions SET category_name = :newName WHERE category_name = :oldName")
    suspend fun renameLoanTransactions(oldName: String, newName: String)

    @Query("UPDATE lend_borrow_persons SET category = :newName WHERE category = :oldName")
    suspend fun renameLoanPeople(oldName: String, newName: String)

    @Query("SELECT * FROM budget_category_limits WHERE category_name IN (:oldName, :newName)")
    suspend fun limitsForRename(oldName: String, newName: String): List<BudgetCategoryLimitEntity>

    @Update
    suspend fun updateRenamedLimit(limit: BudgetCategoryLimitEntity)

    @Query("DELETE FROM budget_category_limits WHERE id IN (:ids)")
    suspend fun deleteMergedLimits(ids: List<Long>)

    @Query("SELECT * FROM transaction_rules")
    suspend fun rulesForRename(): List<RuleEntity>

    @Update
    suspend fun updateRenamedRule(rule: RuleEntity)

    /** Rename every stored category reference atomically, retaining subcategory IDs. */
    @Transaction
    suspend fun updateCategoryWithReferences(category: CategoryEntity) {
        require(category.name.isNotBlank()) { "Category name cannot be empty" }
        require(conflictingCategory(category.name, category.id) == null) { "A category with this name already exists" }
        val old = getCategoryById(category.id) ?: error("Category no longer exists")
        if (old.name != category.name) {
            renameTransactions(old.name, category.name, category.updatedAt)
            renameMappings(old.name, category.name)
            renameSubscriptions(old.name, category.name)
            renameLoanTransactions(old.name, category.name)
            renameLoanPeople(old.name, category.name)
            limitsForRename(old.name, category.name).groupBy { it.budgetId }.values.forEach { limits ->
                if (limits.any { it.categoryName == old.name }) {
                    val keeper = limits.firstOrNull { it.categoryName == category.name } ?: limits.first()
                    updateRenamedLimit(keeper.copy(categoryName = category.name,
                        limitAmount = limits.fold(BigDecimal.ZERO) { sum, limit -> sum + limit.limitAmount },
                        updatedAt = category.updatedAt))
                    deleteMergedLimits(limits.filter { it.id != keeper.id }.map { it.id })
                }
            }
            rulesForRename().forEach { rule ->
                val renamed = rule.withCategoryRenamed(old.name, category.name)
                if (renamed != rule) updateRenamedRule(renamed.copy(updatedAt = category.updatedAt))
            }
        }
        updateCategory(category)
    }
}
