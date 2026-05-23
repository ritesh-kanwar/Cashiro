package com.ritesh.parser.core.bank

import com.ritesh.parser.core.test.ExpectedTransaction
import com.ritesh.parser.core.test.ParserTestCase
import com.ritesh.parser.core.test.ParserTestUtils
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class HDFCBankParserTest {
    @Test
    fun `test HDFC Bank Parser comprehensive test suite`() {
        val parser = HDFCBankParser()

        ParserTestUtils.printTestHeader(
            parserName = "HDFC Bank",
            bankName = parser.getBankName(),
            currency = parser.getCurrency()
        )

        val testCases = listOf(
            // Bill Alert - should NOT parse as transaction
            ParserTestCase(
                name = "Bill Alert Notification - Should Not Parse",
                message = """New Bill Alert:
Your AUBA00000NAT3Q Bill 8078064625 of Rs.3953.72 is due on 05-Nov-2025. To pay, login to HDFC Bank Net/Mobile Banking>BillPay
T&C. Ignore if paid""",
                sender = "CP-HDFCBK-S",
                shouldParse = false
            ),

            // Actual transaction examples that SHOULD parse
            ParserTestCase(
                name = "UPI Debit Transaction",
                message = "Rs.500.00 debited from A/c XX1234 on 20-Oct-25 to merchant@upi (UPI Ref No 123456789012)",
                sender = "CP-HDFCBK-S",
                expected = ExpectedTransaction(
                    amount = BigDecimal("500.00"),
                    currency = "INR",
                    type = com.ritesh.parser.core.TransactionType.EXPENSE,
                    accountLast4 = "1234",
                    reference = "123456789012"
                )
            ),
            ParserTestCase(
                name = "Sent UPI transaction to named payee",
                message = """Sent Rs.45.00
From HDFC Bank A/C *1234
To Sample Friend
On 23/05/26
Ref 123456789012
Not You?
Contact bank support/SMS BLOCK UPI""",
                sender = "JD-HDFCBK-S",
                expected = ExpectedTransaction(
                    amount = BigDecimal("45.00"),
                    currency = "INR",
                    type = com.ritesh.parser.core.TransactionType.EXPENSE,
                    merchant = "Sample Friend",
                    accountLast4 = "1234",
                    reference = "123456789012"
                )
            ),
            ParserTestCase(
                name = "Sent UPI transaction to numeric VPA",
                message = """Sent Rs.70.00
From HDFC Bank A/C *1234
To 0000000000@bank
On 23/05/26
Ref 123456789013
Not You?
Contact bank support/SMS BLOCK UPI""",
                sender = "AD-HDFCBK-S",
                expected = ExpectedTransaction(
                    amount = BigDecimal("70.00"),
                    currency = "INR",
                    type = com.ritesh.parser.core.TransactionType.EXPENSE,
                    merchant = "UPI Payee",
                    accountLast4 = "1234",
                    reference = "123456789013"
                )
            )
        )

        val handleCases: List<Pair<String, Boolean>> = listOf(
            "CP-HDFCBK-S" to true,
            "AX-HDFCBK-S" to true,
            "JM-HDFCBK-S" to true,
            "HDFCBANK" to true,
            "SBI" to false,
            "" to false
        )

        val result =
            ParserTestUtils.runTestSuite(parser, testCases, handleCases, "HDFC Bank Parser Tests")

    }
}
