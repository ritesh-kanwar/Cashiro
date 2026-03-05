package com.cashiroai.shared.data.repository

import com.cashiroai.shared.data.local.dao.SharedMerchantMappingDao
import com.cashiroai.shared.data.local.entity.SharedMerchantMappingEntity
import kotlinx.coroutines.flow.Flow

class RoomSharedMerchantMappingRepository(
    private val dao: SharedMerchantMappingDao
) : SharedMerchantMappingRepository {
    override fun observeAll(): Flow<List<SharedMerchantMappingEntity>> = dao.observeAll()
    override suspend fun getByMerchant(merchantName: String): SharedMerchantMappingEntity? = dao.getByMerchant(merchantName)
    override suspend fun upsert(mapping: SharedMerchantMappingEntity) = dao.upsert(mapping)
}
