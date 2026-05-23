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
                name = "Balance credit with account and AcBal",
                message = """
                    Rs.4740.08 Credited to SB-xxx1234 AcBal:65040.08 CLRBal: 65083.90
                    [NEFT-CITI- ] SAMPLE-BRANCH on 20-05-2026 15:01:34.IOB.
                """.trimIndent(),
                sender = "JD-IOBCHN-S",
                expected = ExpectedTransaction(
                    amount = BigDecimal("4740.08"),
                    currency = "INR",
                    type = TransactionType.INCOME,
                    merchant = "CITI",
                    accountLast4 = "1234",
                    balance = BigDecimal("65040.08")
                )
            ),
            ParserTestCase(
                name = "UPI balance debit extracts account and balance",
                message = """
                    Rs.70000.00 Debited to SB-xxx1234 AcBal:8054.76 CLRBal: 8098.58
                    [UPI/636773 ] SAMPLE-BRANCH on 01-01-2026 16:11:16.IOB.
                """.trimIndent(),
                sender = "VM-IOBCHN-S",
                expected = ExpectedTransaction(
                    amount = BigDecimal("70000.00"),
                    currency = "INR",
                    type = TransactionType.EXPENSE,
                    merchant = "UPI",
                    accountLast4 = "1234",
                    balance = BigDecimal("8054.76"),
                    reference = "636773"
                )
            ),
            ParserTestCase(
                name = "UPI credit detail with short masked account",
                message = """
                    Your a/c no. XXXXX99 is credited by Rs.10000.00 on 2026-05-04 19:35:21.874,
                    from SAMPLE PAYER-samplepayer@bank(UPI Ref no 122603588789).Payer Remark - UPI -IOB
                """.trimIndent(),
                sender = "BV-IOBCHN-S",
                expected = ExpectedTransaction(
                    amount = BigDecimal("10000.00"),
                    currency = "INR",
                    type = TransactionType.INCOME,
                    merchant = "UPI - SAMPLE PAYER (samplepayer@bank)",
                    accountLast4 = "99",
                    reference = "122603588789"
                )
            ),
            ParserTestCase(
                name = "Towards account debit format",
                message = """
                    Your account has been debited towards XXXX1234 for Rs. 4383.00 on 03/07/2025.
                    If not you, contact tollfree 18008904445. -IOB
                """.trimIndent(),
                sender = "BZ-IOBCHN-S",
                expected = ExpectedTransaction(
                    amount = BigDecimal("4383.00"),
                    currency = "INR",
                    type = TransactionType.EXPENSE,
                    accountLast4 = "1234"
                )
            ),
            ParserTestCase(
                name = "IMPS credit detail format",
                message = """
                    Dear Customer, Your A/C:XXX1234 is credited by Rs. 1.00 on 07/07/2025
                    from a/c XXX3413 (IMPS Ref id:518809468661)? IOB
                """.trimIndent(),
                sender = "AD-IOBCHN-S",
                expected = ExpectedTransaction(
                    amount = BigDecimal("1.00"),
                    currency = "INR",
                    type = TransactionType.INCOME,
                    accountLast4 = "1234"
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
