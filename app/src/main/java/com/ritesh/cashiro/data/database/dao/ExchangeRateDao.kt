package com.ritesh.cashiro.data.database.dao

import androidx.room.*
import com.ritesh.cashiro.data.database.entity.ExchangeRateEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface ExchangeRateDao {
    @Query("SELECT * FROM exchange_rates WHERE from_currency = :fromCurrency AND to_currency = :toCurrency ORDER BY updated_at DESC LIMIT 1")
    suspend fun getExchangeRateIgnoringExpiry(fromCurrency: String, toCurrency: String): ExchangeRateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExchangeRate(exchangeRate: ExchangeRateEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExchangeRates(exchangeRates: List<ExchangeRateEntity>)

    @Query("SELECT * FROM exchange_rates WHERE from_currency = :fromCurrency AND to_currency = :toCurrency AND expires_at > :currentTime")
    suspend fun getExchangeRate(fromCurrency: String, toCurrency: String, currentTime: LocalDateTime = LocalDateTime.now()): ExchangeRateEntity?

    @Query("SELECT * FROM exchange_rates WHERE from_currency = :fromCurrency AND to_currency = :toCurrency AND expires_at > :currentTime")
    fun getExchangeRateFlow(fromCurrency: String, toCurrency: String, currentTime: LocalDateTime = LocalDateTime.now()): Flow<ExchangeRateEntity?>

    @Query("SELECT * FROM exchange_rates WHERE from_currency = :fromCurrency AND expires_at > :currentTime")
    suspend fun getExchangeRatesForCurrency(fromCurrency: String, currentTime: LocalDateTime = LocalDateTime.now()): List<ExchangeRateEntity>

    @Query("SELECT * FROM exchange_rates WHERE from_currency = :fromCurrency AND expires_at_unix > :currentTimeUnix")
    suspend fun getExchangeRatesForCurrencyUnix(fromCurrency: String, currentTimeUnix: Long): List<ExchangeRateEntity>

    @Query("SELECT * FROM exchange_rates WHERE from_currency = :fromCurrency ORDER BY updated_at DESC")
    suspend fun getAllRatesForCurrency(fromCurrency: String): List<ExchangeRateEntity>

    @Query("SELECT * FROM exchange_rates WHERE updated_at < :expiryTime")
    suspend fun getExpiredRates(expiryTime: LocalDateTime): List<ExchangeRateEntity>

    @Query("DELETE FROM exchange_rates WHERE is_custom = 0 AND updated_at < :expiryTime")
    suspend fun deleteExpiredRates(expiryTime: LocalDateTime): Int

    @Query("SELECT COUNT(*) FROM exchange_rates WHERE from_currency = :fromCurrency AND to_currency = :toCurrency AND expires_at > :currentTime")
    suspend fun hasValidRate(fromCurrency: String, toCurrency: String, currentTime: LocalDateTime = LocalDateTime.now()): Int

    @Query("SELECT * FROM exchange_rates ORDER BY updated_at DESC LIMIT 1")
    suspend fun getLatestRate(): ExchangeRateEntity?

    // Get all unique currencies that have exchange rates
    @Query("SELECT DISTINCT from_currency FROM exchange_rates WHERE expires_at > :currentTime")
    suspend fun getAvailableCurrencies(currentTime: LocalDateTime = LocalDateTime.now()): List<String>

    // Batch get rates for multiple currency pairs
    @Query("SELECT * FROM exchange_rates WHERE (from_currency = :fromCurrency1 AND to_currency = :toCurrency1) OR (from_currency = :fromCurrency2 AND to_currency = :toCurrency2) AND expires_at > :currentTime")
    suspend fun getMultipleRates(
        fromCurrency1: String, toCurrency1: String,
        fromCurrency2: String, toCurrency2: String,
        currentTime: LocalDateTime = LocalDateTime.now()
    ): List<ExchangeRateEntity>

    @Query("SELECT MAX(expires_at_unix) FROM exchange_rates WHERE from_currency = :fromCurrency")
    suspend fun getMaxExpiryTimeUnix(fromCurrency: String): Long?

    @Query("SELECT * FROM exchange_rates WHERE from_currency = :fromCurrency AND to_currency = :toCurrency AND is_custom = 1")
    suspend fun getCustomRate(fromCurrency: String, toCurrency: String): ExchangeRateEntity?

    @Query("SELECT * FROM exchange_rates WHERE from_currency = :fromCurrency AND is_custom = 1")
    suspend fun getCustomRatesForCurrency(fromCurrency: String): List<ExchangeRateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCustomRate(entity: ExchangeRateEntity)

    @Query("DELETE FROM exchange_rates WHERE from_currency = :fromCurrency AND to_currency = :toCurrency AND is_custom = 1")
    suspend fun resetCustomRate(fromCurrency: String, toCurrency: String)

    @Query("SELECT * FROM exchange_rates")
    fun getAllRates(): Flow<List<ExchangeRateEntity>>

    @Query("DELETE FROM exchange_rates")
    suspend fun deleteAllRates()
}