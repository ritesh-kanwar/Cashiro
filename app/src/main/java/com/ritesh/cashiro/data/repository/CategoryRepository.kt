package com.ritesh.cashiro.data.repository

import android.content.Context
import com.ritesh.cashiro.data.database.dao.CategoryDao
import com.ritesh.cashiro.data.database.entity.CategoryEntity
import com.ritesh.cashiro.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import dagger.hilt.android.qualifiers.ApplicationContext
import com.ritesh.cashiro.utils.IconResolutionUtils
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao,
    @ApplicationContext private val context: Context,
    @ApplicationScope private val externalScope: CoroutineScope
) {
    
    val categories: StateFlow<List<CategoryEntity>> = categoryDao.getAllCategories()
        .stateIn(
            scope = externalScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )
    
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
        description: String = "",
        color: String,
        iconResId: Int = 0,
        iconName: String = "",
        isIncome: Boolean = false
    ): Long {
        val category = CategoryEntity(
            name = name,
            description = description,
            color = color,
            iconResId = iconResId,
            iconName = iconName,
            isSystem = false,
            isIncome = isIncome,
            displayOrder = 999
        )
        return categoryDao.insertCategory(category)
    }
    
    suspend fun resetCategoryToDefault(categoryId: Long) {
        val category = categoryDao.getCategoryById(categoryId)
        if (category != null && category.isSystem) {
            // Reset to default values
            val resetCategory = category.copy(
                name = category.defaultName ?: category.name,
                description = category.defaultDescription ?: category.description,
                color = category.defaultColor ?: category.color,
                iconResId = category.defaultIconResId ?: category.iconResId,
                iconName = category.defaultIconName ?: category.iconName,
                updatedAt = LocalDateTime.now()
            )
            categoryDao.updateCategoryWithReferences(resetCategory)
        }
    }
    
    suspend fun updateCategory(category: CategoryEntity) {
        categoryDao.updateCategoryWithReferences(
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
}