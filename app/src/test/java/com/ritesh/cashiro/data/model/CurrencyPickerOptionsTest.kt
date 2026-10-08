package com.ritesh.cashiro.data.model

import org.junit.Assert.*
import org.junit.Test

class CurrencyPickerOptionsTest {
    @Test fun emptyAndPartialResponsesKeepTheSupportedCatalog() {
        val empty = CurrencyPickerOptions.catalog()
        val partial = CurrencyPickerOptions.catalog(remote = listOf(Currency("usd", "Remote dollar", "$")))
        assertTrue(empty.isNotEmpty())
        assertEquals(empty.map { it.code }, partial.map { it.code })
        assertEquals(1, partial.count { it.code == "USD" })
    }

    @Test fun offlineCustomAndSelectedCurrenciesRemainSelectable() {
        val custom = Currency(" xyz ", "Synthetic unit", "X")
        val catalog = CurrencyPickerOptions.catalog(custom = listOf(custom), selectedCode = " qrs ")
        assertEquals("Synthetic unit", catalog.single { it.code == "XYZ" }.name)
        assertTrue(catalog.any { it.code == "QRS" })
        assertEquals("QRS", CurrencyPickerOptions.quickAccess(catalog, "qrs").first().code)
    }

    @Test fun searchMatchesCodeNameAndSymbolIgnoringWhitespace() {
        val currencies = CurrencyPickerOptions.catalog()
        assertEquals(listOf("INR"), CurrencyPickerOptions.matching(currencies, " inr ").map { it.code })
        assertEquals(listOf("INR"), CurrencyPickerOptions.matching(currencies, "Indian Rupee").map { it.code })
        assertTrue(CurrencyPickerOptions.matching(currencies, "₹").any { it.code == "INR" })
        assertTrue(CurrencyPickerOptions.matching(currencies, "no such currency").isEmpty())
    }

    @Test fun transactionFiltersKeepTheirScopeAndNormalizeDuplicates() {
        val currencies = CurrencyPickerOptions.available(listOf("usd", " USD ", "eur", ""), "eur")
        assertEquals(listOf("EUR", "USD"), currencies.map { it.code })
        assertTrue(CurrencyPickerOptions.available(emptyList(), "").isNotEmpty())
    }
}
