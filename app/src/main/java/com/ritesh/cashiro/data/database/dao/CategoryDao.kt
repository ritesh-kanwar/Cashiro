package com.ritesh.cashiro.data.database.dao

import androidx.room.*
import com.ritesh.cashiro.data.database.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

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
}