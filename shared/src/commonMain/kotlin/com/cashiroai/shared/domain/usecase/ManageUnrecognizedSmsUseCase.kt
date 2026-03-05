package com.cashiroai.shared.domain.usecase

import com.cashiroai.shared.data.local.entity.SharedUnrecognizedSmsEntity
import com.cashiroai.shared.data.repository.SharedUnrecognizedSmsRepository
import com.cashiroai.shared.data.util.currentTimeMillis

class ManageUnrecognizedSmsUseCase(
    private val repository: SharedUnrecognizedSmsRepository
) {
    suspend fun add(sender: String, smsBody: String): Long {
        val now = currentTimeMillis()
        return repository.insert(
            SharedUnrecognizedSmsEntity(
                sender = sender,
                smsBody = smsBody,
                receivedAtEpochMillis = now,
                createdAtEpochMillis = now
            )
        )
    }
}
