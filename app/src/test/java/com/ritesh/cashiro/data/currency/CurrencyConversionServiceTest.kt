package com.ritesh.cashiro.data.currency

import com.ritesh.cashiro.data.database.dao.ExchangeRateDao
import com.ritesh.cashiro.data.database.entity.ExchangeRateEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.Proxy
import java.math.BigDecimal
import java.time.LocalDateTime

class CurrencyConversionServiceTest {
    private class Store {
        val rates = mutableMapOf<Pair<String, String>, ExchangeRateEntity>()
        val dao = Proxy.newProxyInstance(ExchangeRateDao::class.java.classLoader, arrayOf(ExchangeRateDao::class.java)) { _, method, args ->
            val pair = if (args != null && args.size >= 2 && args[0] is String && args[1] is String) (args[0] as String) to (args[1] as String) else null
            when (method.name) {
                "getCustomRate" -> rates[pair]?.takeIf { it.isCustom }
                "getExchangeRate" -> rates[pair]?.takeIf { it.expiresAt.isAfter(LocalDateTime.now()) }
                "getExchangeRateIgnoringExpiry" -> rates[pair]
                "getCustomRatesForCurrency" -> rates.values.filter { it.fromCurrency == args[0] && it.isCustom }
                "insertExchangeRates" -> {
                    @Suppress("UNCHECKED_CAST")
                    (args[0] as List<ExchangeRateEntity>).forEach { rates[it.fromCurrency to it.toCurrency] = it }
                    Unit
                }
                "upsertCustomRate" -> { val rate = args[0] as ExchangeRateEntity; rates[rate.fromCurrency to rate.toCurrency] = rate; Unit }
                "resetCustomRate" -> { rates.remove(pair); Unit }
                else -> error("Unexpected DAO method: ${method.name}")
            }
        } as ExchangeRateDao

        fun historical(from: String, to: String, value: String) {
            rates[from to to] = ExchangeRateEntity(fromCurrency = from, toCurrency = to, rate = BigDecimal(value),
                provider = "synthetic", updatedAt = LocalDateTime.now().minusDays(30), expiresAt = LocalDateTime.now().minusDays(29))
        }
    }

    private class Provider : ExchangeRateProvider {
        var calls = 0
        var offline = false
        var cancel = false
        override suspend fun fetchAllExchangeRatesWithMetadata(baseCurrency: String): ExchangeRateResponseWithMetadata? {
            calls++
            delay(10)
            if (cancel) throw CancellationException("synthetic cancellation")
            if (offline) return null
            val now = System.currentTimeMillis() / 1000
            return ExchangeRateResponseWithMetadata(mapOf("usd" to BigDecimal.ONE, "eur" to BigDecimal("2"), "inr" to BigDecimal("80")), now + 3600, now, "synthetic", baseCurrency)
        }
        override suspend fun fetchExchangeRate(fromCurrency: String, toCurrency: String): BigDecimal? = error("unused")
        override suspend fun fetchAllExchangeRates(baseCurrency: String): Map<String, BigDecimal>? = error("unused")
        override fun getProviderName() = "synthetic"
        override suspend fun getSupportedCurrencies() = listOf("USD", "EUR", "INR")
        override suspend fun fetchAllCurrencies(): Map<String, String>? = null
    }

    @Test fun missingRatesNeverBecomeFaceValueAndRequestsShareResponse() = runTest {
        val provider = Provider()
        val service = CurrencyConversionService(Store().dao, provider)
        val results = listOf("ZZZ", "EUR", "INR", "ZZZ").map { target -> async { service.convertAmountOrNull(BigDecimal.TEN, " usd ", target) } }.awaitAll()
        assertNull(results[0]); assertEquals(BigDecimal("20.00"), results[1]); assertEquals(BigDecimal("800.00"), results[2]); assertNull(results[3])
        assertEquals(1, provider.calls)
    }

    @Test fun customRatesSurviveForcedRefreshAndReverseLookup() = runTest {
        val store = Store(); val provider = Provider(); val service = CurrencyConversionService(store.dao, provider)
        service.saveCustomRate("usd", "eur", BigDecimal("4"))
        assertEquals(0, BigDecimal("4").compareTo(service.getExchangeRate("USD", "EUR", true)))
        assertEquals(0, BigDecimal("0.25").compareTo(service.getExchangeRate("EUR", "USD", true)))
        service.fetchAndSaveAllRates("USD")
        assertEquals(0, BigDecimal("4").compareTo(service.getExchangeRate("USD", "EUR")))
        assertTrue(store.rates["USD" to "EUR"]!!.isCustom)
    }

    @Test fun concurrentForcedRefreshesCoalesceAcrossPairs() = runTest {
        val provider = Provider(); val service = CurrencyConversionService(Store().dao, provider)
        val results = listOf("EUR", "INR", "ZZZ").map { async { service.getExchangeRate("USD", it, true) } }.awaitAll()
        assertEquals(1, provider.calls)
        assertEquals(0, BigDecimal("2").compareTo(results[0])); assertNull(results[2])
    }

    @Test fun offlineUsesHistoricalCrossRatesAndBacksOffMissingPairs() = runTest {
        val store = Store(); store.historical("USD", "EUR", "2"); store.historical("USD", "INR", "80")
        val provider = Provider().apply { offline = true }; val service = CurrencyConversionService(store.dao, provider)
        assertEquals(BigDecimal("400.00"), service.convertAmountOrNull(BigDecimal.TEN, "EUR", "INR"))
        assertNull(service.getExchangeRate("ZZZ", "INR")); assertNull(service.getExchangeRate("YYY", "EUR"))
        assertEquals(1, provider.calls)
        store.historical("USD", "YYY", "0")
        assertNull(service.getExchangeRate("EUR", "YYY"))
    }

    @Test fun budgetsConvertLimitsAndSpendingTogetherOrKeepOriginalCurrency() = runTest {
        val service = CurrencyConversionService(Store().dao, Provider())
        val budget = com.ritesh.cashiro.data.repository.BudgetWithSpending(
            budget = com.ritesh.cashiro.data.database.entity.BudgetEntity(name = "Synthetic", amount = BigDecimal("100"), year = 2026, month = 1, currency = "USD"),
            currentSpending = BigDecimal("20"),
            categoryLimits = listOf(com.ritesh.cashiro.data.database.entity.BudgetCategoryLimitEntity(budgetId = 1, categoryName = "Food", limitAmount = BigDecimal("30"))),
            categorySpending = mapOf("Food" to BigDecimal("20")), daysRemaining = 10, daysInMonth = 31)
        val converted = budget.inCurrency("EUR", service)
        assertEquals("EUR", converted.budget.currency)
        assertEquals(BigDecimal("200.00"), converted.budget.amount)
        assertEquals(BigDecimal("40.00"), converted.currentSpending)
        assertEquals(BigDecimal("40.00"), converted.categorySpending["Food"])
        assertEquals(BigDecimal("60.00"), converted.categoryLimits.single().limitAmount)
        assertSame(budget, budget.inCurrency("ZZZ", service))
    }

    @Test fun cancellationPropagatesAndDoesNotStartCooldown() = runTest {
        val provider = Provider().apply { cancel = true }; val service = CurrencyConversionService(Store().dao, provider)
        try { service.getExchangeRate("USD", "EUR"); fail("Expected cancellation") } catch (_: CancellationException) { }
        provider.cancel = false
        assertNotNull(service.getExchangeRate("USD", "EUR")); assertEquals(2, provider.calls)
    }
}
