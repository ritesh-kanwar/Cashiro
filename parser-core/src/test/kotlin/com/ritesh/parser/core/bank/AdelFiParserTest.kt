package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import com.ritesh.parser.core.test.ExpectedTransaction
import com.ritesh.parser.core.test.ParserTestCase
import com.ritesh.parser.core.test.ParserTestUtils
import com.ritesh.parser.core.test.SimpleTestCase
<<<<<<< ours
import org.junit.jupiter.api.*
=======
import org.junit.jupiter.api.Test
>>>>>>> theirs
import java.math.BigDecimal

class AdelFiParserTest {

    private val parser = AdelFiParser()

<<<<<<< ours
    @TestFactory
    fun `adelfi credit union parser handles key paths`(): List<DynamicTest> {
=======
    @Test
    fun `adelfi credit union parser handles key paths`() {
>>>>>>> theirs
        ParserTestUtils.printTestHeader(
            parserName = "AdelFi Credit Union (USA)",
            bankName = parser.getBankName(),
            currency = parser.getCurrency()
        )

        val cases = listOf(
            ParserTestCase(
                name = "Transaction - Tax Service",
<<<<<<< ours
                message = "Transaction Alert from AdelFi.\n**1234 had a transaction of ($15.00). Description: 8042999971 P AND F TAX INC        CITY        CAUS. Date: Dec 19, 2025",
=======
                message = "Transaction Alert from AdelFi.\n**1234 had a transaction of (\$15.00). Description: 8042999971 P AND F TAX INC        CITY        CAUS. Date: Dec 19, 2025",
>>>>>>> theirs
                sender = "42141",
                expected = ExpectedTransaction(
                    amount = BigDecimal("15.00"),
                    currency = "USD",
                    type = TransactionType.CREDIT,
                    merchant = "P AND F TAX INC        CITY        CAUS",
                    accountLast4 = "1234"
                )
            ),
            ParserTestCase(
                name = "Transaction - Amazon Purchase",
<<<<<<< ours
                message = "Transaction Alert from AdelFi.\n**1234 had a transaction of ($33.79). Description: 235251000999657 AMAZON MKTPL*ZX0Q15PH2 Amzn.com/billWAUS. Date: Dec 19, 2025",
=======
                message = "Transaction Alert from AdelFi.\n**1234 had a transaction of (\$33.79). Description: 235251000999657 AMAZON MKTPL*ZX0Q15PH2 Amzn.com/billWAUS. Date: Dec 19, 2025",
>>>>>>> theirs
                sender = "42141",
                expected = ExpectedTransaction(
                    amount = BigDecimal("33.79"),
                    currency = "USD",
                    type = TransactionType.CREDIT,
                    merchant = "AMAZON MKTPL*ZX0Q15PH2 Amzn.com/billWAUS",
                    accountLast4 = "1234"
                )
            )
        )

<<<<<<< ours
        return ParserTestUtils.runTestSuite(parser, cases)
    }

    @TestFactory
    fun `factory resolves adelfi credit union`(): List<DynamicTest> {
=======
        ParserTestUtils.runTestSuite(parser, cases)
    }

    @Test
    fun `factory resolves adelfi credit union`() {
>>>>>>> theirs
        val cases = listOf(
            SimpleTestCase(
                bankName = "AdelFi",
                sender = "42141",
                currency = "USD",
<<<<<<< ours
                message = "Transaction Alert from AdelFi.\n**1234 had a transaction of ($15.00). Description: 8042999971 P AND F TAX INC        CITY        CAUS. Date: Dec 19, 2025",
=======
                message = "Transaction Alert from AdelFi.\n**1234 had a transaction of (\$15.00). Description: 8042999971 P AND F TAX INC        CITY        CAUS. Date: Dec 19, 2025",
>>>>>>> theirs
                expected = ExpectedTransaction(
                    amount = BigDecimal("15.00"),
                    currency = "USD",
                    type = TransactionType.CREDIT
                ),
                shouldHandle = true
            ),
            SimpleTestCase(
                bankName = "AdelFi",
                sender = "42141",
                currency = "USD",
<<<<<<< ours
                message = "Transaction Alert from AdelFi.\n**5678 had a transaction of ($100.50). Description: 123456789 GROCERY STORE LOCATION CAUS. Date: Dec 20, 2025",
=======
                message = "Transaction Alert from AdelFi.\n**5678 had a transaction of (\$100.50). Description: 123456789 GROCERY STORE LOCATION CAUS. Date: Dec 20, 2025",
>>>>>>> theirs
                expected = ExpectedTransaction(
                    amount = BigDecimal("100.50"),
                    currency = "USD",
                    type = TransactionType.CREDIT
                ),
                shouldHandle = true
            )
        )

<<<<<<< ours
        return ParserTestUtils.runFactoryTestSuite(cases, "Factory smoke tests")
=======
        ParserTestUtils.runFactoryTestSuite(cases, "Factory smoke tests")
>>>>>>> theirs
    }
}
