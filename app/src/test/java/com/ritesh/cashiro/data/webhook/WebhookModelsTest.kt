package com.ritesh.cashiro.data.webhook

import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class WebhookModelsTest {

    @Test
    fun `startOfWeek returns same day when given a Monday`() {
        val monday = LocalDate.of(2026, 5, 4) // Monday
        assertEquals(monday, monday.startOfWeek())
    }

    @Test
    fun `startOfWeek walks back to Monday from any later day in the week`() {
        // Week of 2026-05-04 (Monday) through 2026-05-10 (Sunday)
        val expectedMonday = LocalDate.of(2026, 5, 4)
        for (offset in 1..6) {
            val day = expectedMonday.plusDays(offset.toLong())
            assertEquals("startOfWeek(${day.dayOfWeek}) should land on Monday", expectedMonday, day.startOfWeek())
        }
    }

    @Test
    fun `startOfWeek handles month boundary correctly`() {
        // 2026-06-01 is a Monday — verify a Sunday in the previous month rolls back
        val sunday = LocalDate.of(2026, 5, 31)
        assertEquals(LocalDate.of(2026, 5, 25), sunday.startOfWeek())
    }

    @Test
    fun `asPlainStringSafe strips trailing zeros without scientific notation`() {
        assertEquals("100", BigDecimal("100.000").asPlainStringSafe())
        assertEquals("1.5", BigDecimal("1.50").asPlainStringSafe())
        assertEquals("0", BigDecimal("0.00").asPlainStringSafe())
    }

    @Test
    fun `asPlainStringSafe avoids scientific notation for large round numbers`() {
        // BigDecimal("100000000000").stripTrailingZeros() returns 1E+11 — toPlainString must un-scientific it.
        val result = BigDecimal("100000000000").asPlainStringSafe()
        assertEquals("100000000000", result)
    }

    @Test
    fun `WebhookScheduledTime displayTime zero-pads single-digit hour and minute`() {
        assertEquals("08:05", WebhookScheduledTime(hour = 8, minute = 5).displayTime())
        assertEquals("00:00", WebhookScheduledTime(hour = 0, minute = 0).displayTime())
        assertEquals("23:59", WebhookScheduledTime(hour = 23, minute = 59).displayTime())
    }
}
