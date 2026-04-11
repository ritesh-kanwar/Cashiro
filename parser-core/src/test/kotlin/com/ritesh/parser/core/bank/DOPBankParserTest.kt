package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import com.ritesh.parser.core.test.ExpectedTransaction
import com.ritesh.parser.core.test.ParserTestCase
import com.ritesh.parser.core.test.ParserTestUtils
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import java.math.BigDecimal

class DOPBankParserTest {

    @TestFactory
    fun `dop parser handles transaction alerts`(): List<DynamicTest> {
        val parser = DOPBankParser()

<<<<<<< ours
        ParserTestUtils.printTestHeader(
            parserName = "Department of Post",
            bankName = parser.getBankName(),
            currency = parser.getCurrency()
        )

        val testCases = listOf(
            ParserTestCase(
                name = "Credit message 1",
=======
        val testCases = listOf(
            ParserTestCase(
                name = "Credit with Rs amount and balance",
>>>>>>> theirs
                message = "Account  No. XXXXXXXX1234 CREDIT with amount Rs. 5550.00 on 02-03-2026. Balance: Rs.40000.00. [S76543210]",
                sender = "VM-DOPBNK-G",
                expected = ExpectedTransaction(
                    amount = BigDecimal("5550.00"),
                    currency = "INR",
                    type = TransactionType.INCOME,
                    accountLast4 = "1234",
                    balance = BigDecimal("40000.00"),
                    reference = "S76543210"
                )
            ),
            ParserTestCase(
<<<<<<< ours
                name = "Credit message 2",
=======
                name = "Credit from different sender prefix",
>>>>>>> theirs
                message = "Account  No. XXXXXXXX1234 CREDIT with amount Rs. 5550.00 on 02-02-2026. Balance: Rs.37500.00. [S33475450]",
                sender = "BZ-DOPBNK-G",
                expected = ExpectedTransaction(
                    amount = BigDecimal("5550.00"),
                    currency = "INR",
                    type = TransactionType.INCOME,
                    accountLast4 = "1234",
                    balance = BigDecimal("37500.00"),
                    reference = "S33475450"
                )
            ),
            ParserTestCase(
<<<<<<< ours
                name = "Credit message 3",
=======
                name = "Credit with S suffix sender",
>>>>>>> theirs
                message = "Account  No. XXXXXXXX1234 CREDIT with amount Rs. 5550.00 on 02-01-2026. Balance: Rs.32000.00. [S92247102]",
                sender = "BV-DOPBNK-S",
                expected = ExpectedTransaction(
                    amount = BigDecimal("5550.00"),
                    currency = "INR",
                    type = TransactionType.INCOME,
                    accountLast4 = "1234",
                    balance = BigDecimal("32000.00"),
                    reference = "S92247102"
                )
            ),
            ParserTestCase(
<<<<<<< ours
                name = "Credit message 4",
                message = "Account  No. XXXXXXXX1234 CREDIT with amount Rs. 5550.00 on 02-12-2025. Balance: Rs.26000.00. [S52580401]",
                sender = "BT-DOPBNK-G",
                expected = ExpectedTransaction(
                    amount = BigDecimal("5550.00"),
                    currency = "INR",
                    type = TransactionType.INCOME,
                    accountLast4 = "1234",
                    balance = BigDecimal("26000.00"),
                    reference = "S52580401"
                )
            ),
            ParserTestCase(
                name = "Credit message 5",
                message = "Account  No. XXXXXXXX1234 CREDIT with amount Rs. 5550.00 on 01-11-2025. Balance: Rs.20900.00. [S13879515]",
                sender = "BH-DOPBNK-G",
                expected = ExpectedTransaction(
                    amount = BigDecimal("5550.00"),
                    currency = "INR",
                    type = TransactionType.INCOME,
                    accountLast4 = "1234",
                    balance = BigDecimal("20900.00"),
                    reference = "S13879515"
                )
            ),
            ParserTestCase(
                name = "Credit message 6",
                message = "Account  No. XXXXXXXX1234 CREDIT with amount Rs. 5550.00 on 01-10-2025. Balance: Rs.15500.00. [S72876106]",
                sender = "VA-DOPBNK-G",
                expected = ExpectedTransaction(
                    amount = BigDecimal("5550.00"),
                    currency = "INR",
                    type = TransactionType.INCOME,
                    accountLast4 = "1234",
                    balance = BigDecimal("15500.00"),
                    reference = "S72876106"
                )
            ),
            ParserTestCase(
                name = "Credit message 7",
                message = "Account  No. XXXXXXXX1234 CREDIT with amount Rs. 5550.00 on 02-09-2025. Balance: Rs.9990.00. [S34160488]",
                sender = "BV-DOPBNK-G",
                expected = ExpectedTransaction(
                    amount = BigDecimal("5550.00"),
                    currency = "INR",
                    type = TransactionType.INCOME,
                    accountLast4 = "1234",
                    balance = BigDecimal("9990.00"),
                    reference = "S34160488"
=======
                name = "Debit transaction",
                message = "Account No. XXXXXXXX5678 DEBIT with amount Rs. 2000.00 on 15-03-2026. Balance: Rs.18000.00. [D12345678]",
                sender = "VM-DOPBNK-G",
                expected = ExpectedTransaction(
                    amount = BigDecimal("2000.00"),
                    currency = "INR",
                    type = TransactionType.EXPENSE,
                    accountLast4 = "5678",
                    balance = BigDecimal("18000.00"),
                    reference = "D12345678"
>>>>>>> theirs
                )
            )
        )

        val handleChecks = listOf(
            "VM-DOPBNK-G" to true,
            "BZ-DOPBNK-G" to true,
            "BV-DOPBNK-S" to true,
            "BT-DOPBNK-G" to true,
<<<<<<< ours
            "BH-DOPBNK-G" to true,
            "VA-DOPBNK-G" to true,
            "BV-DOPBNK-G" to true,
            "UNKNOWN" to false
=======
            "DOP-ALERTS" to true,
            "ALERT-DOP" to true,
            "DOP" to true,
            "UNKNOWN" to false,
            "HDFC" to false
>>>>>>> theirs
        )

        return ParserTestUtils.runTestSuite(
            parser = parser,
            testCases = testCases,
            handleCases = handleChecks,
            suiteName = "DOP Parser"
        )
    }
}
