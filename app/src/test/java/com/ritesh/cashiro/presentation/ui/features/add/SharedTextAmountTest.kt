package com.ritesh.cashiro.presentation.ui.features.add

import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class SharedTextAmountTest {
    @Test fun currencyAndShortNoteAmountsAreExtracted() {
        assertEquals(SharedTextAmount.Guess(BigDecimal("450"), null), SharedTextAmount.extract("450 lunch"))
        assertEquals(SharedTextAmount.Guess(BigDecimal("1200.50"), "INR"), SharedTextAmount.extract("Paid Rs. 1,200.50 to Example Shop"))
        assertEquals("USD", SharedTextAmount.extract("$12")?.currency)
        assertEquals("JPY", SharedTextAmount.extract("JPY 500")?.currency)
        assertEquals("BDT", SharedTextAmount.extract("৳20")?.currency)
    }
    @Test fun foreignCurrencyNeverBecomesAccountCurrency() {
        assertNull(SharedTextAmount.amountForCurrency("$12", "INR"))
        assertEquals(BigDecimal("12"), SharedTextAmount.amountForCurrency("$12", "USD"))
        assertEquals(BigDecimal("450"), SharedTextAmount.amountForCurrency("450 lunch", "ETB"))
    }
    @Test fun ambiguousDatesReferencesAndMalformedAmountsStayEmpty() {
        listOf("Ref 1234", "XX1234", "2026/10/08", "2026-10-08", "12.345", "INR 12.345", "12 lunch 30 dinner", "Paid INR 12 balance INR 90", "Account 4567", "USD -12", "0 lunch", "reference 123456789012", "This is a long message with a bare number 500 that could be an identifier").forEach {
            assertNull(it, SharedTextAmount.extract(it))
        }
    }
}
