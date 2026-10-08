package com.ritesh.cashiro.data.currency

import com.ritesh.cashiro.data.database.dao.ExchangeRateDao
import com.ritesh.cashiro.data.database.entity.ExchangeRateEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CurrencyConversionService @Inject constructor(
    private val exchangeRateDao: ExchangeRateDao,
    private val exchangeRateProvider: ExchangeRateProvider
) {
    private val fetchMutex = Mutex()
    @Volatile private var fetchGeneration = 0L
    private val responses = mutableMapOf<String, ExchangeRateResponseWithMetadata>()
    private val failedFetches = mutableMapOf<String, Long>()
    private val receivedAt = mutableMapOf<String, Long>()

    // Emits a new value whenever a custom rate is saved or reset, so ViewModels can react
    private val _rateChangeTrigger = MutableStateFlow(0L)
    val rateChangeTrigger: StateFlow<Long> = _rateChangeTrigger.asStateFlow()

    /**
     * Convert amount from one currency to another
     */
    suspend fun convertAmountOrNull(
        amount: BigDecimal,
        fromCurrency: String,
        toCurrency: String,
        forceRefresh: Boolean = false
    ): BigDecimal? {
        if (fromCurrency.trim().equals(toCurrency.trim(), ignoreCase = true)) return amount
        val rate = getExchangeRate(fromCurrency, toCurrency, forceRefresh) ?: return null
        return amount.multiply(rate).setScale(2, RoundingMode.HALF_UP)
    }

    /**
     * Get exchange rate between two currencies
     */
    suspend fun getExchangeRate(
        fromCurrency: String,
        toCurrency: String,
        forceRefresh: Boolean = false
    ): BigDecimal? {
        val requestedGeneration = fetchGeneration
        return fetchMutex.withLock {
            val from = fromCurrency.trim().uppercase(java.util.Locale.ROOT)
            val to = toCurrency.trim().uppercase(java.util.Locale.ROOT)
            if (from == to) return@withLock BigDecimal.ONE
            // Custom rates take precedence even during a requested refresh.
            exchangeRateDao.getCustomRate(from, to)?.rate?.takeIf { it.signum() > 0 }?.let { return@withLock it }
            exchangeRateDao.getCustomRate(to, from)?.rate?.takeIf { it.signum() > 0 }?.let {
                return@withLock BigDecimal.ONE.divide(it, MathContext(10))
            }
            if (!forceRefresh) {
                exchangeRateDao.getExchangeRate(from, to)?.rate?.takeIf { it.signum() > 0 }?.let { return@withLock it }
                exchangeRateDao.getExchangeRate(to, from)?.rate?.takeIf { it.signum() > 0 }?.let {
                    return@withLock BigDecimal.ONE.divide(it, MathContext(10))
                }
            }
            val response = fetchResponse("USD", forceRefresh && requestedGeneration == fetchGeneration)
            response?.let {
                val fromRate = if (from == "USD") BigDecimal.ONE else it.rates[from]
                val toRate = if (to == "USD") BigDecimal.ONE else it.rates[to]
                if (fromRate != null && fromRate.signum() > 0 && toRate != null && toRate.signum() > 0) {
                    return@withLock toRate.divide(fromRate, MathContext(10))
                }
            }
            // Offline or unsupported pairs may still have a usable historical rate.
            exchangeRateDao.getExchangeRateIgnoringExpiry(from, to)?.rate?.takeIf { it.signum() > 0 }?.let { return@withLock it }
            exchangeRateDao.getExchangeRateIgnoringExpiry(to, from)?.rate?.takeIf { it.signum() > 0 }?.let {
                return@withLock BigDecimal.ONE.divide(it, MathContext(10))
            }
            val fromUsd = if (from == "USD") BigDecimal.ONE else exchangeRateDao.getExchangeRateIgnoringExpiry("USD", from)?.rate
            val toUsd = if (to == "USD") BigDecimal.ONE else exchangeRateDao.getExchangeRateIgnoringExpiry("USD", to)?.rate
            if (fromUsd != null && fromUsd.signum() > 0 && toUsd != null && toUsd.signum() > 0) toUsd.divide(fromUsd, MathContext(10)) else null
        }
    }

    /** One response serves concurrent requests for different pairs, including unsupported codes. */
    private suspend fun fetchResponse(base: String, force: Boolean = false): ExchangeRateResponseWithMetadata? {
        val now = System.currentTimeMillis() / 1000
        if (!force) responses[base]?.takeIf { it.nextUpdateTimeUnix > now || now - (receivedAt[base] ?: 0L) < 300 }?.let { return it }
        if (!force && failedFetches[base]?.let { now - it < 300 } == true) return responses[base]
        val response = try {
            exchangeRateProvider.fetchAllExchangeRatesWithMetadata(base)
        } catch (e: CancellationException) { throw e } catch (_: Exception) { null }
        fetchGeneration++
        if (response == null || !response.baseCurrency.equals(base, ignoreCase = true)) {
            failedFetches[base] = now
            return responses[base]
        }
        val normalized = response.copy(rates = response.rates.mapKeys { it.key.trim().uppercase(java.util.Locale.ROOT) }.filterValues { it.signum() > 0 })
        if (normalized.rates.isEmpty()) {
            failedFetches[base] = now
            return responses[base]
        }
        responses[base] = normalized
        receivedAt[base] = now
        val customPairs = exchangeRateDao.getCustomRatesForCurrency(base).map { it.toCurrency }.toSet()
        val entities = normalized.rates.filter { (currency, rate) -> currency !in customPairs && rate.signum() > 0 }.map { (currency, rate) ->
            ExchangeRateEntity(fromCurrency = base, toCurrency = currency, rate = rate, provider = response.provider,
                updatedAt = LocalDateTime.ofInstant(Instant.ofEpochSecond(response.lastUpdateTimeUnix), ZoneId.systemDefault()),
                expiresAt = LocalDateTime.ofInstant(Instant.ofEpochSecond(response.nextUpdateTimeUnix), ZoneId.systemDefault()),
                updatedAtUnix = response.lastUpdateTimeUnix, expiresAtUnix = response.nextUpdateTimeUnix)
        }
        exchangeRateDao.insertExchangeRates(entities)
        failedFetches.remove(base)
        return normalized
    }

    /**
     * Check if we have a valid rate for this currency pair
     */
    suspend fun hasValidRate(fromCurrency: String, toCurrency: String): Boolean {
        if (fromCurrency.trim().equals(toCurrency.trim(), ignoreCase = true)) {
            return true
        }

        val from = fromCurrency.trim().uppercase(java.util.Locale.ROOT)
        val to = toCurrency.trim().uppercase(java.util.Locale.ROOT)
        return exchangeRateDao.getExchangeRate(from, to)?.rate?.signum() == 1 ||
            exchangeRateDao.getExchangeRate(to, from)?.rate?.signum() == 1

    }

    /**
     * Refresh exchange rates for an account's currencies
     */
    suspend fun refreshExchangeRatesForAccount(currencies: List<String>) {
        if (currencies.size < 2) return // No conversion needed for single currency

        // Get unique currencies and ensure USD is included for API compatibility
        val uniqueCurrencies = currencies.distinct().toMutableList()
        if (!uniqueCurrencies.contains("USD")) {
            uniqueCurrencies.add("USD")
        }

        refreshExchangeRates(uniqueCurrencies)
    }

    /**
     * Refresh exchange rates for specific currencies using USD as base
     */
    suspend fun refreshExchangeRates(currencies: List<String>) = fetchMutex.withLock {
        val codes = currencies.map { it.trim().uppercase(java.util.Locale.ROOT) }.distinct().filter { it != "USD" }
        if (codes.all { exchangeRateDao.getExchangeRate("USD", it) != null }) return@withLock
        fetchResponse("USD")
        Unit
    }

    suspend fun fetchAndSaveAllRates(baseCurrency: String, targetCurrencies: List<String> = emptyList()) = fetchMutex.withLock {
        fetchResponse(baseCurrency.trim().uppercase(java.util.Locale.ROOT), force = true)
        Unit
    }

    suspend fun saveCustomRate(fromCurrency: String, toCurrency: String, rate: BigDecimal) = fetchMutex.withLock {
        require(rate.signum() > 0) { "Exchange rate must be positive" }
        val now = LocalDateTime.now()
        val entity = ExchangeRateEntity(
            fromCurrency = fromCurrency.trim().uppercase(java.util.Locale.ROOT),
            toCurrency = toCurrency.trim().uppercase(java.util.Locale.ROOT),
            rate = rate,
            provider = "custom",
            updatedAt = now,
            updatedAtUnix = now.atZone(ZoneId.systemDefault()).toEpochSecond(),
            expiresAt = now.plusYears(100),
            expiresAtUnix = now.plusYears(100).atZone(ZoneId.systemDefault()).toEpochSecond(),
            isCustom = true
        )
        exchangeRateDao.upsertCustomRate(entity)
        _rateChangeTrigger.value++
    }

    suspend fun resetCustomRate(fromCurrency: String, toCurrency: String) = fetchMutex.withLock {
        exchangeRateDao.resetCustomRate(fromCurrency.trim().uppercase(java.util.Locale.ROOT), toCurrency.trim().uppercase(java.util.Locale.ROOT))
        _rateChangeTrigger.value++
    }

    /**
     * Retrieve stored conversions for a base currency from the local database.
     */
    suspend fun getStoredConversions(baseCurrency: String): Pair<List<ExchangeRateEntity>, Long> {
        val rates = exchangeRateDao.getAllRatesForCurrency(baseCurrency.trim().uppercase(java.util.Locale.ROOT))
        val lastUpdated = rates.maxByOrNull { it.updatedAtUnix }?.updatedAtUnix ?: 0L
        return Pair(rates, lastUpdated)
    }

    /**
     * Check if we should refresh rates for the given base currency
     * Returns true if rates are stale or we don't have any rates
     */
    private suspend fun shouldRefreshRates(baseCurrency: String): Boolean {
        val currentTimeUnix = System.currentTimeMillis() / 1000

        // Use the efficient Unix timestamp query to get the latest expiry time
        val maxExpiryTimeUnix = exchangeRateDao.getMaxExpiryTimeUnix(baseCurrency)

        // If we don't have any rates, or they're from old records (timestamp 0), or they've expired, refresh
        return maxExpiryTimeUnix == null || maxExpiryTimeUnix == 0L || maxExpiryTimeUnix < currentTimeUnix
    }

    /**
     * Check if overall rates are stale across all currencies
     */
    private suspend fun areOverallRatesStale(): Boolean {
        return shouldRefreshRates("USD") // USD is our main base currency, so check its rates
    }

    /**
     * Get information about rate freshness for debugging
     */
    suspend fun getRateFreshnessInfo(): RateFreshnessInfo {
        val currentTime = LocalDateTime.now()
        val usdRates = exchangeRateDao.getExchangeRatesForCurrency("USD", currentTime)
        val latestRate = exchangeRateDao.getLatestRate()

        return RateFreshnessInfo(
            hasValidUsdRates = usdRates.isNotEmpty(),
            validUsdRatesCount = usdRates.size,
            latestUpdateTime = latestRate?.updatedAt,
            latestExpiryTime = usdRates.maxByOrNull { it.expiresAt }?.expiresAt,
            isStale = areOverallRatesStale(),
            currentTime = currentTime
        )
    }

    /**
     * Clear expired rates from database
     */
    suspend fun cleanupExpiredRates() {
        val expiryTime = LocalDateTime.now().minusDays(7) // Keep rates for 7 days
        exchangeRateDao.deleteExpiredRates(expiryTime)
    }

    /**
     * Get all available currencies with exchange rates
     */
    suspend fun getAvailableCurrencies(): List<String> {
        return exchangeRateDao.getAvailableCurrencies()
    }

    /**
     * Convert multiple amounts to base currency
     */
    suspend fun convertToBaseCurrency(
        transactions: List<TransactionData>,
        baseCurrency: String
    ): Map<String, BigDecimal> {
        val convertedAmounts = mutableMapOf<String, BigDecimal>()

        transactions.forEach { transaction ->
            val convertedAmount = convertAmountOrNull(
                amount = transaction.amount,
                fromCurrency = transaction.currency,
                toCurrency = baseCurrency
            )
            if (convertedAmount != null) convertedAmounts[transaction.id] = convertedAmount
        }

        return convertedAmounts
    }

    data class TransactionData(
        val id: String,
        val amount: BigDecimal,
        val currency: String
    )

    data class RateFreshnessInfo(
        val hasValidUsdRates: Boolean,
        val validUsdRatesCount: Int,
        val latestUpdateTime: LocalDateTime?,
        val latestExpiryTime: LocalDateTime?,
        val isStale: Boolean,
        val currentTime: LocalDateTime
    )
}