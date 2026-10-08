package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import com.ritesh.parser.core.test.ExpectedTransaction
import com.ritesh.parser.core.test.ParserTestCase
import com.ritesh.parser.core.test.ParserTestUtils
import org.junit.jupiter.api.TestFactory
import java.math.BigDecimal

class SouthIndianGPayReferenceTest {
    @TestFactory fun whitespaceAndReference() = ParserTestUtils.runTestSuite(
        parser = SouthIndianBankParser(),
        testCases = listOf(ParserTestCase(
            "Whitespace before UPI reference and merchant",
            "Your A/c X1000 is debited with Rs.100.00 Info: UPI/IPOS/000000000001/ Example Shop on 01-01-2026. Final balance is Rs.900.00-South Indian Bank",
            "SIBSMS",
            ExpectedTransaction(amount = BigDecimal("100.00"), currency = "INR", type = TransactionType.EXPENSE, merchant = "Example Shop", accountLast4 = "1000", reference = "000000000001")
        ), ParserTestCase(
            "IMPS merchant ends before Final balance and full account uses last four",
            "Your A/c 000000001000 is credited with Rs.100.00 Info: IMPS/EXAMPLE/000000000001/ Example Shop Final balance is Rs.900.00-South Indian Bank",
            "SIBSMS",
            ExpectedTransaction(amount = BigDecimal("100.00"), currency = "INR", type = TransactionType.INCOME, merchant = "Example Shop", accountLast4 = "1000", reference = "000000000001")
        ), ParserTestCase(
            "IMPS reference separated by whitespace and masked account",
            "Your A/c XX1000 is credited with Rs.100.00 Info: IMPS/EXAMPLE/000000000001 Example Shop. Final balance is Rs.900.00-South Indian Bank",
            "SIBSMS",
            ExpectedTransaction(amount = BigDecimal("100.00"), currency = "INR", type = TransactionType.INCOME, accountLast4 = "1000", reference = "000000000001")
        ), ParserTestCase(
            "IMPS merchant retains embedded bal before balance delimiter",
            "Your A/c XX1000 is credited with Rs.100.00 Info: IMPS/EXAMPLE/000000000001/ Example Global Traders Bal:Rs.900.00-South Indian Bank",
            "SIBSMS",
            ExpectedTransaction(amount = BigDecimal("100.00"), currency = "INR", type = TransactionType.INCOME, merchant = "Example Global Traders", accountLast4 = "1000", reference = "000000000001")
        ), ParserTestCase(
            "IMPS merchant retains Bal prefix before compact FinalBal delimiter",
            "Your A/c XX1000 is credited with Rs.100.00 Info: IMPS/EXAMPLE/000000000001/ Example Balaji Shop FinalBal:Rs.900.00-South Indian Bank",
            "SIBSMS",
            ExpectedTransaction(amount = BigDecimal("100.00"), currency = "INR", type = TransactionType.INCOME, merchant = "Example Balaji Shop", accountLast4 = "1000", reference = "000000000001")
        )),
        suiteName = "Synthetic SIB GPay matching"
    )
}
