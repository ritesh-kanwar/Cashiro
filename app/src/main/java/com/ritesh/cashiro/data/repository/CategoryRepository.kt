package com.ritesh.cashiro.data.repository

import com.ritesh.cashiro.data.database.dao.CategoryDao
import com.ritesh.cashiro.data.database.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao
) {
    
    fun getAllCategories(): Flow<List<CategoryEntity>> {
        return categoryDao.getAllCategories()
    }
    
    fun getExpenseCategories(): Flow<List<CategoryEntity>> {
        return categoryDao.getExpenseCategories()
    }
    
    fun getIncomeCategories(): Flow<List<CategoryEntity>> {
        return categoryDao.getIncomeCategories()
    }
    
    suspend fun getCategoryById(categoryId: Long): CategoryEntity? {
        return categoryDao.getCategoryById(categoryId)
    }
    
    suspend fun getCategoryByName(categoryName: String): CategoryEntity? {
        return categoryDao.getCategoryByName(categoryName)
    }
    
    suspend fun createCategory(
        name: String,
        color: String,
        iconResId: Int = 0,
        isIncome: Boolean = false
    ): Long {
        val category = CategoryEntity(
            name = name,
            color = color,
            iconResId = iconResId,
            isSystem = false,
            isIncome = isIncome,
            displayOrder = 999
        )
        return categoryDao.insertCategory(category)
    }
    
    suspend fun updateCategory(category: CategoryEntity) {
        categoryDao.updateCategory(
            category.copy(updatedAt = LocalDateTime.now())
        )
    }
    
    suspend fun deleteCategory(categoryId: Long): Boolean {
        // Only delete non-system categories
        val category = categoryDao.getCategoryById(categoryId)
        if (category != null && !category.isSystem) {
            categoryDao.deleteCategory(categoryId)
            return true
        }
        return false
    }
    
    suspend fun categoryExists(categoryName: String): Boolean {
        return categoryDao.categoryExists(categoryName)
    }
    
    suspend fun initializeDefaultCategories() {
        // Only initialize if no categories exist
        if (categoryDao.getCategoryCount() == 0) {
            val defaultCategories = listOf(
                CategoryEntity(name = "Food & Dining", color = "#FC8019", iconResId = com.ritesh.cashiro.R.drawable.type_food_dining, isSystem = true, isIncome = false, displayOrder = 1),
                CategoryEntity(name = "Groceries", color = "#5AC85A", iconResId = com.ritesh.cashiro.R.drawable.type_groceries_glass_of_milk, isSystem = true, isIncome = false, displayOrder = 2),
                CategoryEntity(name = "Transportation", color = "#000000", iconResId = com.ritesh.cashiro.R.drawable.type_travel_transport_automobile, isSystem = true, isIncome = false, displayOrder = 3),
                CategoryEntity(name = "Shopping", color = "#FF9900", iconResId = com.ritesh.cashiro.R.drawable.type_shopping_shopping_cart, isSystem = true, isIncome = false, displayOrder = 4),
                CategoryEntity(name = "Bills & Utilities", color = "#4CAF50", iconResId = com.ritesh.cashiro.R.drawable.type_finance_dollar_banknote, isSystem = true, isIncome = false, displayOrder = 5),
                CategoryEntity(name = "Entertainment", color = "#E50914", iconResId = com.ritesh.cashiro.R.drawable.type_tool_electronic_video_game, isSystem = true, isIncome = false, displayOrder = 6),
                CategoryEntity(name = "Healthcare", color = "#10847E", iconResId = com.ritesh.cashiro.R.drawable.type_health_hospital, isSystem = true, isIncome = false, displayOrder = 7),
                CategoryEntity(name = "Investments", color = "#00D09C", iconResId = com.ritesh.cashiro.R.drawable.type_finance_bar_chart, isSystem = true, isIncome = false, displayOrder = 8),
                CategoryEntity(name = "Banking", color = "#004C8F", iconResId = com.ritesh.cashiro.R.drawable.type_finance_bank, isSystem = true, isIncome = false, displayOrder = 9),
                CategoryEntity(name = "Personal Care", color = "#6A4C93", iconResId = com.ritesh.cashiro.R.drawable.type_health_stethoscope, isSystem = true, isIncome = false, displayOrder = 10),
                CategoryEntity(name = "Education", color = "#673AB7", iconResId = com.ritesh.cashiro.R.drawable.type_stationary_fountain_pen, isSystem = true, isIncome = false, displayOrder = 11),
                CategoryEntity(name = "Mobile", color = "#2A3890", iconResId = com.ritesh.cashiro.R.drawable.type_tool_electronic_mobile_phone, isSystem = true, isIncome = false, displayOrder = 12),
                CategoryEntity(name = "Fitness", color = "#FF3278", iconResId = com.ritesh.cashiro.R.drawable.type_sports_trophy, isSystem = true, isIncome = false, displayOrder = 13),
                CategoryEntity(name = "Insurance", color = "#0066CC", iconResId = com.ritesh.cashiro.R.drawable.type_health_pill, isSystem = true, isIncome = false, displayOrder = 14),
                CategoryEntity(name = "Travel", color = "#00BCD4", iconResId = com.ritesh.cashiro.R.drawable.type_travel_transport_airplane, isSystem = true, isIncome = false, displayOrder = 15),
                CategoryEntity(name = "Salary", color = "#4CAF50", iconResId = com.ritesh.cashiro.R.drawable.type_finance_dollar_banknote, isSystem = true, isIncome = true, displayOrder = 16),
                CategoryEntity(name = "Income", color = "#4CAF50", iconResId = com.ritesh.cashiro.R.drawable.type_finance_dollar_banknote, isSystem = true, isIncome = true, displayOrder = 17),
                CategoryEntity(name = "Others", color = "#757575", iconResId = com.ritesh.cashiro.R.drawable.type_food_dining, isSystem = true, isIncome = false, displayOrder = 18)
            )
            categoryDao.insertCategories(defaultCategories)
        }
    }
}