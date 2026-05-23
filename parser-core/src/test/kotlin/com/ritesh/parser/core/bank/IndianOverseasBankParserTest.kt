package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import com.ritesh.parser.core.test.ExpectedTransaction
import com.ritesh.parser.core.test.ParserTestCase
import com.ritesh.parser.core.test.ParserTestUtils
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import java.math.BigDecimal

class IndianOverseasBankParserTest {
    @TestFactory
    fun `iob parser handles debit credit and notice formats`(): List<DynamicTest> {
        val parser = IndianOverseasBankParser()

        val cases = listOf(
            ParserTestCase(
                name = "Debited for named payee",
                message = """
                    Your a/c XX1234 debited for payee SAMPLE MERCHANT for Rs. 150.00
                    on 2026-03-25, ref 123456789012.If not you, report to your bank immediately-IOB.
                """.trimIndent(),
                sender = "BZ-IOBCHN-S",
                expected = ExpectedTransaction(
                    amount = BigDecimal("150.00"),
                    currency = "INR",
                    type = TransactionType.EXPENSE,
                    merchant = "SAMPLE MERCHANT",
                    accountLast4 = "1234",
                    reference = "123456789012"
                )
            ),
            ParserTestCase(
                name = "Debited for VPA payee",
                message = """
                    Your a/c XX1234 debited for payee samplepayee@bank for Rs. 996.00
                    on 2026-02-03, ref 123456789013.If not you, report to your bank immediately-IOB.
                """.trimIndent(),
                sender = "VA-IOBCHN-S",
                expected = ExpectedTransaction(
                    amount = BigDecimal("996.00"),
                    currency = "INR",
                    type = TransactionType.EXPENSE,
                    merchant = "samplepayee",
                    accountLast4 = "1234",
                    reference = "123456789013"
                )
            ),
            ParserTestCase(
                name = "SMS charge debit",
                message = """
                    Rs.20.64 Debited to SB-xxx1234 AcBal:65162.78 CLRBal: 65206.60
                    [CHRGS- SMS ] BRANCH ONE on 23-05-2026 08:00:39.IOB.
                """.trimIndent(),
                sender = "JD-IOBCHN-S",
                expected = ExpectedTransaction(
                    amount = BigDecimal("20.64"),
                    currency = "INR",
                    type = TransactionType.EXPENSE,
                    merchant = "SMS Charges",
                    accountLast4 = "1234",
                    balance = BigDecimal("65162.78")
                )
            ),
            ParserTestCase(
                name = "Future SMS charge notice should not parse",
                message = """
                    Dear Customer, Applicable SMS charges will be debited from your SB/CA/CC acct
                    for the period Oct-Dec 2025. For details, pls visit our website/Branch-IOB
                """.trimIndent(),
                sender = "VA-IOBCHN-S",
                shouldParse = false
            ),
            ParserTestCase(
                name = "Disabled card transaction notice should not parse",
                message = """
                    Dear Customer, ECOM txn is not enabled for your Debit Card xx1234.
                    Pls enable Ecom txns using Internet Banking or by visiting your branch - IOB
                """.trimIndent(),
                sender = "AD-IOBCHN",
                shouldParse = false
            )
        )

        val handleCases = listOf(
            "JD-IOBCHN-S" to true,
            "IOBCHN" to true,
            "HDFCBK" to false,
            "" to false
        )

        return ParserTestUtils.runTestSuite(parser, cases, handleCases, "Indian Overseas Bank Parser Tests")
    }
}
