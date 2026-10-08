package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import com.ritesh.parser.core.test.ExpectedTransaction
import com.ritesh.parser.core.test.ParserTestCase
import com.ritesh.parser.core.test.ParserTestUtils
import com.ritesh.parser.core.test.SimpleTestCase
import org.junit.jupiter.api.TestFactory
import java.math.BigDecimal

class SliceParserTest {
    @TestFactory
    fun `banking and legacy card formats`() = ParserTestUtils.runTestSuite(
        SliceParser(),
        listOf(
            ParserTestCase(
                "SFB outgoing UPI is a bank expense",
                "Rs.100.00 sent from a/c XX1000 to Example Shop (UPI Ref: 000000000001). Avl. Bal. Rs.900.00",
                "VA-SLCBNK-S",
                ExpectedTransaction(BigDecimal("100.00"), "INR", TransactionType.EXPENSE,
                    merchant = "Example Shop", accountLast4 = "1000", reference = "000000000001",
                    balance = BigDecimal("900.00"), isFromCard = false)
            ),
            ParserTestCase(
                "SFB incoming UPI retains payer and reference",
                "Rs.100.00 received in slice A/c XX1000 from Example Payer via UPI (Ref ID: 000000000002). Avl. Bal. Rs.900.00",
                "SLICE",
                ExpectedTransaction(BigDecimal("100.00"), "INR", TransactionType.INCOME,
                    merchant = "Example Payer", accountLast4 = "1000", reference = "000000000002",
                    balance = BigDecimal("900.00"), isFromCard = false)
            ),
            ParserTestCase(
                "SFB card spend retains bank expense classification",
                "Your transaction of Rs.100.00 at Example Shop from a/c XX1000 is successful. Avl. Bal. Rs.900.00",
                "SLICE",
                ExpectedTransaction(BigDecimal("100.00"), "INR", TransactionType.EXPENSE,
                    merchant = "Example Shop", accountLast4 = "1000", balance = BigDecimal("900.00"), isFromCard = true)
            ),
            ParserTestCase(
                "AutoPay is an account debit",
                "Successfully paid Rs.100.00 from slice a/c XX1000 to Example Service on 01-Jan-26 via UPI AutoPay",
                "SLICE",
                ExpectedTransaction(BigDecimal("100.00"), "INR", TransactionType.EXPENSE,
                    merchant = "Example Service", accountLast4 = "1000", isFromCard = false)
            ),
            ParserTestCase(
                "Bare sent format preserves punctuated payee",
                "Rs.100.00 sent to Example & Co-2 (UPI Ref: 000000000003)",
                "JK-SLICEIT",
                ExpectedTransaction(BigDecimal("100.00"), "INR", TransactionType.EXPENSE,
                    merchant = "Example & Co-2", reference = "000000000003")
            ),
            ParserTestCase(
                "Modern debit is not credit card debt",
                "Rs.100.00 debited from your slice account",
                "JD-SLCEIT-S",
                ExpectedTransaction(BigDecimal("100.00"), "INR", TransactionType.EXPENSE)
            ),
            ParserTestCase(
                "Legacy explicit slice card remains credit",
                "Your transaction of Rs.100.00 on Example Store is successful on your slice card ending 1000",
                "SLICE",
                ExpectedTransaction(BigDecimal("100.00"), "INR", TransactionType.CREDIT,
                    merchant = "Example Store", isFromCard = true)
            ),
            ParserTestCase(
                "Date phrase does not become merchant",
                "Your transaction of Rs.100.00 on 01 Jan is successful on your slice card ending 1000",
                "SLICE",
                ExpectedTransaction(BigDecimal("100.00"), "INR", TransactionType.CREDIT, merchant = "Slice")
            ),
            ParserTestCase("Unsuccessful transaction", "Your transaction of Rs.100.00 at Example Shop is unsuccessful", "SLICE", shouldParse = false),
            ParserTestCase("Declined despite success keyword", "Your transaction of Rs.100.00 at Example Shop was declined, not successful", "SLICE", shouldParse = false),
            ParserTestCase("OTP is not spending", "000000 is your OTP for txn of Rs.100.00 on slice card ending 1000", "SLICE", shouldParse = false),
            ParserTestCase("Revoked AutoPay mandate", "Your UPI AutoPay mandate for Rs.100.00 to Example Service is revoked", "SLICE", shouldParse = false),
            ParserTestCase("Paused AutoPay mandate", "Your UPI AutoPay mandate for Rs.100.00 is paused", "SLICE", shouldParse = false),
            ParserTestCase("Suspended AutoPay mandate", "Your UPI AutoPay mandate for Rs.100.00 is suspended", "SLICE", shouldParse = false),
            ParserTestCase("Collect request is not income", "UPI collect request received for Rs.100.00 from Example Shop", "SLICE", shouldParse = false),
            ParserTestCase("Payment request is not income", "Payment request received for Rs.100.00 from Example Shop", "SLICE", shouldParse = false),
            ParserTestCase("Requested payment is not spending", "Example Shop has requested Rs.100.00 through UPI", "SLICE", shouldParse = false)
        ),
        handleCases = listOf("VA-SLCBNK-S" to true, "JK-SLICEIT" to true, "JD-SLCEIT-S" to true, "OTHERBANK" to false),
        suiteName = "Slice synthetic banking and legacy regression coverage"
    )

    @TestFactory
    fun `factory resolves SFB sender`() = ParserTestUtils.runFactoryTestSuite(
        listOf(SimpleTestCase(
            bankName = "Slice", sender = "VA-SLCBNK-S", currency = "INR",
            message = "Rs.100.00 sent from a/c XX1000 to Example Shop (UPI Ref: 000000000001)",
            expected = ExpectedTransaction(BigDecimal("100.00"), "INR", TransactionType.EXPENSE,
                merchant = "Example Shop", accountLast4 = "1000"), shouldHandle = true
        )), "Slice SFB factory coverage"
    )
}
