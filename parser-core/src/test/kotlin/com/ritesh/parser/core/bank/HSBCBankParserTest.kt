package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
<<<<<<< ours
import com.ritesh.parser.core.bank.HSBCBankParser
=======
>>>>>>> theirs
import com.ritesh.parser.core.test.ExpectedTransaction
import com.ritesh.parser.core.test.ParserTestCase
import com.ritesh.parser.core.test.ParserTestUtils
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class HSBCBankParserTest {

<<<<<<< ours
    @Test
    fun `hsbc bank parser handles expected scenarios`() {
        val parser = HSBCBankParser()

=======
    private val parser = HSBCBankParser()

    @Test
    fun `hsbc parser handles key paths`() {
>>>>>>> theirs
        ParserTestUtils.printTestHeader(
            parserName = "HSBC Bank",
            bankName = parser.getBankName(),
            currency = parser.getCurrency()
        )

<<<<<<< ours
        val testCases = listOf(
            // Issue #118 - Credit transactions with account format A/c XXX-XXX***-XXX
            ParserTestCase(
                name = "NEFT Credit with UTR - Format A/c 074-260***-006",
                message = "HSBC: A/c 074-260***-006 is credited with INR 5000.00 on 27NOV at 06.33.02 with UTR CHASH00007392391 as NEFT from CHAS A/c ***6983 of John Doe . Your Avl Bal is INR 15000.50.",
                sender = "HSBC",
                expected = ExpectedTransaction(
                    amount = BigDecimal("5000.00"),
                    currency = "INR",
                    type = TransactionType.INCOME,
                    merchant = "CHAS A/c ***6983 of John Doe",
                    accountLast4 = "0006",
                    balance = BigDecimal("15000.50"),
                    reference = "CHASH00007392391"
                )
            ),

            ParserTestCase(
                name = "NEFT Credit with UTR - Different account format",
                message = "HSBC: A/c 123-456***-789 is credited with INR 2500.75 on 15DEC at 10.15.30 with UTR NEFT12345678901 as NEFT from AXIS A/c ***1234 of Jane Smith . Your Avl Bal is INR 50000.00.",
                sender = "HSBC",
                expected = ExpectedTransaction(
                    amount = BigDecimal("2500.75"),
                    currency = "INR",
                    type = TransactionType.INCOME,
                    merchant = "AXIS A/c ***1234 of Jane Smith",
                    accountLast4 = "0789",
                    balance = BigDecimal("50000.00"),
                    reference = "NEFT12345678901"
                )
            ),

            // Debit card transaction
            ParserTestCase(
                name = "Debit Card Purchase",
                message = "Thank you for using HSBC Debit Card XXXXX71xx at IKEA INDIA . for INR 49.00 on 12-04-25.",
                sender = "HSBC",
                expected = ExpectedTransaction(
                    amount = BigDecimal("49.00"),
=======
        val cases = listOf(
            ParserTestCase(
                name = "Outgoing NEFT Transfer - credited to other bank",
                message = "HSBC: Dear HSBC Customer, your NEFT transaction with reference number HSBCN00106726185 for INR 150,000.00 has been credited to the HDFC A/c XXXXXXXXXX6956 of AKASH KEDIA on 01-01-2026 at 15:36:47 .",
                sender = "VM-HSBCIN-S",
                expected = ExpectedTransaction(
                    amount = BigDecimal("150000.00"),
                    currency = "INR",
                    type = TransactionType.TRANSFER,
                    merchant = "AKASH KEDIA"
                )
            ),
            ParserTestCase(
                name = "Debit Card Purchase",
                message = "HSBC: Thank you for using HSBC Debit Card XXXXX71xx for INR 305.00 on 15-Dec-25 at IKEA INDIA .",
                sender = "VM-HSBCIN",
                expected = ExpectedTransaction(
                    amount = BigDecimal("305.00"),
>>>>>>> theirs
                    currency = "INR",
                    type = TransactionType.EXPENSE,
                    merchant = "IKEA INDIA",
                    accountLast4 = "71xx"
                )
            ),
<<<<<<< ours

            // Credit card transaction
            ParserTestCase(
                name = "Credit Card Purchase",
                message = "Your HSBC creditcard xxxxx1234 used at AMAZON for INR 305.00 on 15-04-25.",
                sender = "HSBC",
                expected = ExpectedTransaction(
                    amount = BigDecimal("305.00"),
                    currency = "INR",
                    type = TransactionType.CREDIT,
                    merchant = "AMAZON",
                    accountLast4 = "1234"
                )
            ),

            // Payment transaction
            ParserTestCase(
                name = "Payment Transaction",
                message = "HSBC: INR 1000.50 is paid from account XXXXXX4567 to ELECTRICITY BOARD on 20APR with ref 222222222222. Your available bal is INR 8000.00.",
                sender = "HSBC",
                expected = ExpectedTransaction(
                    amount = BigDecimal("1000.50"),
                    currency = "INR",
                    type = TransactionType.EXPENSE,
                    merchant = "ELECTRICITY BOARD",
                    accountLast4 = "4567",
                    balance = BigDecimal("8000.00"),
                    reference = "222222222222"
=======
            ParserTestCase(
                name = "Incoming NEFT Credit",
                message = "HSBC: INR 50,000.00 is credited to your A/c 074-260***-006 as NEFT from CHAS A/c ***6983 of John Doe .",
                sender = "VM-HSBCIN-S",
                expected = ExpectedTransaction(
                    amount = BigDecimal("50000.00"),
                    currency = "INR",
                    type = TransactionType.INCOME,
                    merchant = "CHAS A/c ***6983 of John Doe",
                    accountLast4 = "0006"
                )
            ),
            ParserTestCase(
                name = "Payment from Account",
                message = "HSBC: INR 1,234.56 is paid from your A/c 074-260***-006 to AMAZON on 20-Dec-25. Your Avl Bal is INR 98,765.44 .",
                sender = "HSBCIN",
                expected = ExpectedTransaction(
                    amount = BigDecimal("1234.56"),
                    currency = "INR",
                    type = TransactionType.EXPENSE,
                    merchant = "AMAZON",
                    accountLast4 = "0006",
                    balance = BigDecimal("98765.44")
>>>>>>> theirs
                )
            )
        )

<<<<<<< ours
        val handleChecks = listOf(
            "HSBC" to true,
            "HSBCIN" to true,
            "AX-HSBC-S" to true,
            "JD-HSBCIN-T" to true,
            "HDFC" to false,
            "UNKNOWN" to false
        )

        val result = ParserTestUtils.runTestSuite(
            parser = parser,
            testCases = testCases,
            handleCases = handleChecks,
            suiteName = "HSBC Bank Parser Tests"
        )
=======
        ParserTestUtils.runTestSuite(parser, cases)
>>>>>>> theirs
    }
}
