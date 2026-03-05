package com.cashiroai.shared.data.repository

import com.cashiroai.shared.data.local.dao.SharedUnrecognizedSmsDao
import com.cashiroai.shared.data.local.entity.SharedUnrecognizedSmsEntity
import kotlinx.coroutines.flow.Flow

class RoomSharedUnrecognizedSmsRepository(
    private val dao: SharedUnrecognizedSmsDao
) : SharedUnrecognizedSmsRepository {
    override fun observeVisible(): Flow<List<SharedUnrecognizedSmsEntity>> = dao.observeVisible()
    override suspend fun insert(entity: SharedUnrecognizedSmsEntity): Long = dao.insert(entity)
    override suspend fun markReported(id: Long) = dao.markReported(id)
    override suspend fun softDelete(id: Long) = dao.softDelete(id)
}
