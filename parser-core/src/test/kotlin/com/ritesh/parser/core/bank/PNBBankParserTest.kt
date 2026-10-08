package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import com.ritesh.parser.core.test.ExpectedTransaction
import com.ritesh.parser.core.test.ParserTestCase
import com.ritesh.parser.core.test.ParserTestUtils
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.math.BigDecimal

class PNBBankParserTest {
    @TestFactory
    fun alerts() = ParserTestUtils.runTestSuite(
        parser = PNBBankParser(),
        testCases = listOf(
            ParserTestCase("UPI payee", "A/c XX1000 debited by ₹100.00 to Example Shop thru UPI:000000000001. Avl Bal ₹900.00", "PNBBNK",
                ExpectedTransaction(currency = "INR", amount = BigDecimal("100.00"), type = TransactionType.EXPENSE, merchant = "Example Shop", accountLast4 = "1000", balance = BigDecimal("900.00"), reference = "000000000001")),
            ParserTestCase("UPI merchant containing IMPS", "A/c XX1000 debited by ₹100.00 to Example IMPS Shop thru UPI:000000000001. Avl Bal ₹900.00", "PNBBNK",
                ExpectedTransaction(currency = "INR", amount = BigDecimal("100.00"), type = TransactionType.EXPENSE, merchant = "Example IMPS Shop")),
            ParserTestCase("IMPS transfer fallback", "A/c XX1000 debited by Rs.100.00 thru IMPS Ref:000000000001. Avl Bal Rs.900.00", "PNBBNK",
                ExpectedTransaction(currency = "INR", amount = BigDecimal("100.00"), type = TransactionType.EXPENSE, merchant = "IMPS Transfer")),
            ParserTestCase("Short mask", "Your a/c no XX000 is debited for Rs.100.00. Avl Bal Rs.900.00", "PNBBNK",
                ExpectedTransaction(currency = "INR", amount = BigDecimal("100.00"), type = TransactionType.EXPENSE, accountLast4 = "000", balance = BigDecimal("900.00"))),
            ParserTestCase("Long mask", "Ac XXXXX00001000 Debited with Rs.100.00. Aval Bal Rs.900.00 CR", "PNBBNK",
                ExpectedTransaction(currency = "INR", amount = BigDecimal("100.00"), type = TransactionType.EXPENSE, accountLast4 = "1000")),
            ParserTestCase("UPI sender", "A/c XX1000 credited by Rs.100.00 by Example Shop thru UPI:000000000001. Avl Bal Rs.900.00", "PNBBNK",
                ExpectedTransaction(currency = "INR", amount = BigDecimal("100.00"), type = TransactionType.INCOME, merchant = "Example Shop")),
            ParserTestCase("Existing From format", "A/c XX1000 credited with Rs.100.00 From Example Shop/UPI:000000000001. Avl Bal Rs.900.00", "PNBBNK",
                ExpectedTransaction(currency = "INR", amount = BigDecimal("100.00"), type = TransactionType.INCOME, merchant = "Example Shop")),
            ParserTestCase("Debit card reference", "A/c XX1000 debited by Rs.100.00 thru debitcard RRN-000000000001. Avl Bal Rs.900.00", "PNBBNK",
                ExpectedTransaction(currency = "INR", amount = BigDecimal("100.00"), type = TransactionType.EXPENSE, reference = "000000000001")),
            ParserTestCase("Mandate is not an expense", "Your UPI-Mandate is successfully created towards Example Shop for Rs.100.00 from A/c No.XX1000. UMN:example@upi", "PNBBNK", shouldParse = false)
        ),
        handleCases = listOf("PNBBNK" to true, "Punjab National Bank" to true, "UNKNOWN" to false),
        suiteName = "PNB synthetic alerts"
    )

    @Test fun mandateCreatesSubscription() {
        val result = PNBBankParser().parseMandateSubscription("Your UPI-Mandate is successfully created towards Example Shop for Rs.100.00 from A/c No.XX1000. UMN:example@upi")
        assertEquals("Example Shop", result?.merchant)
        assertEquals(BigDecimal("100.00"), result?.amount)
    }
    @Test fun rupeeSymbolMandateCreatesSubscription() {
        val result = PNBBankParser().parseMandateSubscription("Your UPI-Mandate is successfully created towards Example IMPS Shop for ₹100.00 from A/c No.XX1000. UMN:example@upi")
        assertEquals("Example IMPS Shop", result?.merchant)
        assertEquals(BigDecimal("100.00"), result?.amount)
    }
}
