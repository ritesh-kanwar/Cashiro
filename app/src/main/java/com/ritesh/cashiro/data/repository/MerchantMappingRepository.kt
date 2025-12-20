package com.ritesh.cashiro.data.repository

import com.ritesh.cashiro.data.database.dao.MerchantMappingDao
import com.ritesh.cashiro.data.database.entity.MerchantMappingEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MerchantMappingRepository @Inject constructor(
    private val merchantMappingDao: MerchantMappingDao
) {
    
    suspend fun getCategoryForMerchant(merchantName: String): String? {
        return merchantMappingDao.getCategoryForMerchant(merchantName)
    }
    
    suspend fun setMapping(merchantName: String, category: String) {
        merchantMappingDao.insertOrUpdateMapping(
            MerchantMappingEntity(
                merchantName = merchantName,
                category = category,
                updatedAt = LocalDateTime.now()
            )
        )
    }
    
    suspend fun removeMapping(merchantName: String) {
        merchantMappingDao.deleteMapping(merchantName)
    }
    
    fun getAllMappings(): Flow<List<MerchantMappingEntity>> {
        return merchantMappingDao.getAllMappings()
    }
    
    suspend fun getMappingCount(): Int {
        return merchantMappingDao.getMappingCount()
    }
}