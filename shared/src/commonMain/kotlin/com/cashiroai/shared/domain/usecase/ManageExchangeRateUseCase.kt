package com.cashiroai.shared.domain.usecase

import com.cashiroai.shared.data.local.entity.SharedExchangeRateEntity
import com.cashiroai.shared.data.repository.SharedExchangeRateRepository
import com.cashiroai.shared.core.SharedTimeConstants
import com.cashiroai.shared.data.util.currentTimeMillis

class ManageExchangeRateUseCase(
    private val repository: SharedExchangeRateRepository
) {
    suspend fun upsertRate(
        fromCurrency: String,
        toCurrency: String,
        rateMicros: Long,
        ttlMillis: Long = SharedTimeConstants.MILLIS_PER_DAY
    ) {
        val now = currentTimeMillis()
        repository.upsert(
            SharedExchangeRateEntity(
                fromCurrency = fromCurrency.uppercase(),
                toCurrency = toCurrency.uppercase(),
                rateMicros = rateMicros,
                provider = "manual",
                updatedAtEpochMillis = now,
                expiresAtEpochMillis = now + ttlMillis
            )
        )
    }
}
