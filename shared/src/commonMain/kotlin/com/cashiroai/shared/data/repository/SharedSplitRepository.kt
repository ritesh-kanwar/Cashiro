package com.cashiroai.shared.data.repository

import com.cashiroai.shared.data.local.entity.SharedTransactionSplitEntity
import kotlinx.coroutines.flow.Flow

interface SharedSplitRepository {
    fun observeSplits(transactionId: Long): Flow<List<SharedTransactionSplitEntity>>
    suspend fun replaceSplits(transactionId: Long, splits: List<SharedTransactionSplitEntity>)
}
