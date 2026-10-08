package com.ritesh.cashiro.data.parser.pdf

import com.ritesh.parser.core.TransactionType
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId

class AdditionalPdfParserTest {
    private fun paytm(date: String = "31 Dec", anchor: String = "Money sent to Example Shop", amount: String = "- Rs.50.00") = """
        Paytm
        01 DEC'25 - 31 JAN'26
        $date
        8:24 PM
        $anchor
        UPI Ref No: 000000000001
        Note: synthetic fixture
        28 Jun
        Example Bank
        - invalid field
        Tag:
        # Shopping
        Example Bank - 1000
        $amount
    """.trimIndent()

    @Test fun paytmUsesPeriodYearAndPreservesAccount() {
        val parser = PaytmPdfParser()
        assertTrue(parser.canHandle(paytm()))
        val row = parser.parse(paytm()).single()
        assertEquals(2025, Instant.ofEpochMilli(row.timestamp).atZone(ZoneId.of("Asia/Kolkata")).year)
        assertEquals("Example Shop", row.merchant)
        assertEquals("Example Bank", row.bankName)
        assertEquals("1000", row.accountLast4)
        assertEquals(BigDecimal("50.00"), row.amount)
        assertEquals(TransactionType.EXPENSE, row.type)
        assertEquals(TransactionType.INCOME, parser.parse(paytm("01 Jan", "Received from Example Sender", "+ Rs.25")).single().type)
    }

    @Test fun paytmRejectsInvalidDatesMandatesAndUnsignedAmounts() {
        val parser = PaytmPdfParser()
        assertTrue(parser.parse(paytm("31 Feb")).isEmpty())
        assertTrue(parser.parse(paytm("01 Feb")).isEmpty())
        assertTrue(parser.parse(paytm(anchor = "Money blocked for Example Offer")).isEmpty())
        assertTrue(parser.parse(paytm(amount = "Rs.50")).isEmpty())
        assertTrue(parser.parse(paytm().replace("01 DEC'25 - 31 JAN'26", "")).isEmpty())
    }

    @Test fun paytmLeapDayUsesTheValidYearWithinPeriod() {
        val text = paytm("29 Feb").replace("01 DEC'25 - 31 JAN'26", "01 FEB'24 - 31 JAN'25")
        assertEquals(2024, Instant.ofEpochMilli(PaytmPdfParser().parse(text).single().timestamp)
            .atZone(ZoneId.of("Asia/Kolkata")).year)
    }

    private fun slice(rows: String) = """
        slice small finance bank
        A/C number 0000001000
        DATE DETAILS REF NO. AMOUNT BALANCE
        $rows
    """.trimIndent()

    @Test fun sliceHandlesWrappedRowsAndTransferSemantics() {
        val parser = SlicePdfParser()
        val rows = parser.parse(slice("""
            01 Jan '26 UPI-Debit-000000000001-EXAMPLE SHOP-TEST0ABC1234-shop@example.invalid
            Generated on 01 Feb 2026
            2/2
            DATE DETAILS REF NO. AMOUNT BALANCE
            000000000002 -₹50 ₹950
            02 Jan '26 Round ups 000000000003 -₹5 ₹945
            03 Jan '26 Interest Cr. 000000000004 ₹2 ₹947
        """.trimIndent()))
        assertEquals(3, rows.size)
        assertEquals("EXAMPLE SHOP", rows[0].merchant)
        assertEquals("1000", rows[0].accountLast4)
        assertEquals(TransactionType.EXPENSE, rows[0].type)
        assertEquals(TransactionType.TRANSFER, rows[1].type)
        assertEquals(TransactionType.INCOME, rows[2].type)
        assertEquals(BigDecimal("947"), rows[2].balance)
    }

    @Test fun sliceRejectsInvalidDatesAndIncompleteRows() {
        assertTrue(SlicePdfParser().parse(slice("31 Feb '26 Example Shop 000000000001 -₹50 ₹950")).isEmpty())
        assertTrue(SlicePdfParser().parse(slice("01 Jan '26 Example Shop")).isEmpty())
        assertTrue(SlicePdfParser().parse(slice("01 Jan '26 Example Shop 000000000001 -₹0 ₹950")).isEmpty())
    }
}
