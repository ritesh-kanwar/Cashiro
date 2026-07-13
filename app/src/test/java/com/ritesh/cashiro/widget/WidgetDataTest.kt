package com.ritesh.cashiro.widget

import com.ritesh.cashiro.data.database.entity.TransactionType
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

class WidgetDataTest {

    @Test
    fun `credit transactions count toward overview expenses`() {
        val totals = addToOverviewTotals(
            totals = OverviewTotals(BigDecimal.ZERO, BigDecimal("10.00")),
            type = TransactionType.CREDIT,
            amount = BigDecimal("25.50"),
        )

        assertEquals(BigDecimal.ZERO, totals.income)
        assertEquals(BigDecimal("35.50"), totals.expense)
    }

    @Test
    fun `transfers do not affect overview totals`() {
        val original = OverviewTotals(BigDecimal("12.00"), BigDecimal("8.00"))

        val totals = addToOverviewTotals(
            totals = original,
            type = TransactionType.TRANSFER,
            amount = BigDecimal("100.00"),
        )

        assertEquals(original, totals)
    }

    @Test
    fun `compact currency abbreviates large amounts`() {
        assertEquals("₹1K", formatCompactCurrency(BigDecimal("1000"), "INR"))
        assertEquals("₹25.6K", formatCompactCurrency(BigDecimal("25635.83"), "INR"))
        assertEquals("₹1.2M", formatCompactCurrency(BigDecimal("1200000"), "INR"))
    }
}
