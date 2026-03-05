package com.cashiroai.shared.data.repository

import com.cashiroai.shared.data.model.SharedCategory
import kotlinx.coroutines.flow.Flow

interface SharedCategoryRepository {
    fun observeCategories(): Flow<List<SharedCategory>>
    fun observeCategoriesByIncomeType(isIncome: Boolean): Flow<List<SharedCategory>>
    suspend fun countCategories(): Int
    suspend fun insertCategories(categories: List<SharedCategory>)
}
