package com.ritesh.cashiro.data.parser.pdf

import com.ritesh.parser.core.TransactionType
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneId

class GPayPdfParserTest {
    private val parser = GPayPdfParser()
    private fun epoch(day: Int, hour: Int) = LocalDateTime.of(2026, 1, day, hour, 0).atZone(ZoneId.of("Asia/Kolkata")).toInstant().toEpochMilli()

    @Test fun rollingDatesBelongToTheirOwnRowsIncludingFinalIncomeRow() {
        val rows = parser.parse("""
            Google Pay
            01 Jan,
            2026
            01:00 PM
            Paid to Example Shop
            UPI Transaction ID: 000000000001
            Paid by Example Bank 1000
            ₹100.00
            02 Jan,
            2026
            02:00 PM
            Received from Example Sender
            UPI Transaction ID: 000000000002
            Paid to Example Bank 1000
            ₹200.00
        """.trimIndent())
        assertEquals(2, rows.size)
        assertEquals(epoch(1, 13), rows[0].timestamp)
        assertEquals(epoch(2, 14), rows[1].timestamp)
        assertEquals(TransactionType.INCOME, rows[1].type)
        assertEquals("Example Sender", rows[1].merchant)
        assertEquals("Example Bank", rows[1].bankName)
        assertEquals("1000", rows[1].accountLast4)
        assertEquals(BigDecimal("200.00"), rows[1].amount)
    }

    @Test fun inlineDateMerchantAmountAndTimeReferenceAreParsed() {
        val row = parser.parse("""
            GPay
            01 Jan, 2026 Paid to Example Shop 2000 ₹100.00
            01:00 PM UPI Transaction ID: 000000000001
            Paid by Example Bank 1000
        """.trimIndent()).single()
        assertEquals("Example Shop 2000", row.merchant)
        assertEquals(epoch(1, 13), row.timestamp)
        assertEquals("000000000001", row.reference)
        assertEquals(BigDecimal("100.00"), row.amount)
    }

    @Test fun bareYearIsNotAnAmountAndAccountLineIsNotAnAnchor() {
        assertTrue(parser.parse("""
            Google Pay
            01 Jan,
            2026
            01:00 PM
            Received from Example Sender
            UPI Transaction ID: 000000000001
            Paid to Example Bank 1000
        """.trimIndent()).isEmpty())
    }
}
