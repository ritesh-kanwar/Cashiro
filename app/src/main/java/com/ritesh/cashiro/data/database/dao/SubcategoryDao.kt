package com.ritesh.cashiro.data.database.dao

import androidx.room.*
import com.ritesh.cashiro.data.database.entity.SubcategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubcategoryDao {
    @Query("SELECT * FROM subcategories WHERE category_id = :categoryId ORDER BY name ASC")
    fun getSubcategoriesByCategoryId(categoryId: Long): Flow<List<SubcategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubcategory(subcategory: SubcategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubcategories(subcategories: List<SubcategoryEntity>)

    @Update suspend fun updateSubcategory(subcategory: SubcategoryEntity)

    @Delete suspend fun deleteSubcategory(subcategory: SubcategoryEntity)

    @Query("DELETE FROM subcategories WHERE id = :subcategoryId")
    suspend fun deleteSubcategoryById(subcategoryId: Long)

    @Query("SELECT COUNT(*) FROM subcategories") suspend fun getSubcategoryCount(): Int
}
