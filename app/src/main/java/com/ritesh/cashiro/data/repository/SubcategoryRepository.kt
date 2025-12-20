
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

    suspend fun createSubcategory(categoryId: Long, name: String): Long {
        val subcategory = SubcategoryEntity(
            categoryId = categoryId,
            name = name
        )
        return subcategoryDao.insertSubcategory(subcategory)
    }

    suspend fun updateSubcategory(subcategory: SubcategoryEntity) {
        subcategoryDao.updateSubcategory(
            subcategory.copy(updatedAt = LocalDateTime.now())
        )
    }

    suspend fun deleteSubcategory(subcategory: SubcategoryEntity) {
        subcategoryDao.deleteSubcategory(subcategory)
    }

    suspend fun deleteSubcategoryById(subcategoryId: Long) {
        subcategoryDao.deleteSubcategoryById(subcategoryId)
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
