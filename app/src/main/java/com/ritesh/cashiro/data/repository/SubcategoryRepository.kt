
package com.ritesh.cashiro.data.repository

import com.ritesh.cashiro.data.database.dao.SubcategoryDao
import com.ritesh.cashiro.data.database.dao.CategoryDao
import com.ritesh.cashiro.data.database.entity.SubcategoryEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubcategoryRepository @Inject constructor(
    private val subcategoryDao: SubcategoryDao,
    private val categoryDao: CategoryDao
) {
    fun getSubcategoriesByCategoryId(categoryId: Long): Flow<List<SubcategoryEntity>> {
        return subcategoryDao.getSubcategoriesByCategoryId(categoryId)
    }

    suspend fun createSubcategory(
        categoryId: Long,
        name: String,
        iconResId: Int = 0,
        color: String = "#757575"
    ): Long {
        val subcategory = SubcategoryEntity(
            categoryId = categoryId,
            name = name,
            iconResId = iconResId,
            color = color,
            isSystem = false
        )
        return subcategoryDao.insertSubcategory(subcategory)
    }

    suspend fun updateSubcategory(subcategory: SubcategoryEntity) {
        subcategoryDao.updateSubcategory(
            subcategory.copy(updatedAt = LocalDateTime.now())
        )
    }

    suspend fun resetSubcategoryToDefault(subcategoryId: Long) {
        val subcategory = subcategoryDao.getSubcategoryById(subcategoryId)
        if (subcategory != null && subcategory.isSystem) {
            // Reset to default values
            val resetSubcategory = subcategory.copy(
                name = subcategory.defaultName ?: subcategory.name,
                iconResId = subcategory.defaultIconResId ?: subcategory.iconResId,
                color = subcategory.defaultColor ?: subcategory.color,
                updatedAt = LocalDateTime.now()
            )
            subcategoryDao.updateSubcategory(resetSubcategory)
        }
    }

    suspend fun deleteSubcategory(subcategory: SubcategoryEntity): Boolean {
        // Only delete non-system subcategories
        if (!subcategory.isSystem) {
            subcategoryDao.deleteSubcategory(subcategory)
            return true
        }
        return false
    }

    suspend fun deleteSubcategoryById(subcategoryId: Long): Boolean {
        val subcategory = subcategoryDao.getSubcategoryById(subcategoryId)
        if (subcategory != null && !subcategory.isSystem) {
            subcategoryDao.deleteSubcategoryById(subcategoryId)
            return true
        }
        return false
    }

    suspend fun initializeDefaultSubcategories() {
        if (subcategoryDao.getSubcategoryCount() == 0) {
            val foodCategory = categoryDao.getCategoryByName("Food & Dining")
            if (foodCategory != null) {
                val defaultSubcategories = listOf(
                    "Eat out",
                    "Take Away",
                    "Tea & Coffee",
                    "FastFood",
                    "Snacks"
                ).map { name ->
                    SubcategoryEntity(
                        categoryId = foodCategory.id,
                        name = name
                    )
                }
                subcategoryDao.insertSubcategories(defaultSubcategories)
            }
        }
    }
}
