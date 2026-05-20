package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import com.ritesh.parser.core.test.ExpectedTransaction
import com.ritesh.parser.core.test.ParserTestCase
import com.ritesh.parser.core.test.ParserTestUtils
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import java.math.BigDecimal

class BPCEParserTest {

    @TestFactory
<<<<<<< ours
    fun `bpce parser tests`(): List<DynamicTest> {
=======
    fun `bpce parser handles key paths`(): List<DynamicTest> {
>>>>>>> theirs
        val parser = BPCEParser()

        val testCases = listOf(
            ParserTestCase(
                name = "Instant transfer",
                message = "Caisse d'Epargne: nous vous confirmons la réalisation de votre virement instantané de 1000,00 EUR du 01/12/2025 à 00h00m00s vers NAME FIRST NAME",
                sender = "38015",
                expected = ExpectedTransaction(
                    amount = BigDecimal("1000.00"),
                    currency = "EUR",
                    type = TransactionType.EXPENSE,
                    merchant = "NAME FIRST NAME"
                )
            ),
            ParserTestCase(
                name = "Instant transfer to PayPal",
                message = "Caisse d'Epargne: nous vous confirmons la réalisation de votre virement instantané de 1000,00 EUR du 03/12/2025 à 18h46m18s vers PAYPAL",
                sender = "38015",
                expected = ExpectedTransaction(
                    amount = BigDecimal("1000.00"),
                    currency = "EUR",
                    type = TransactionType.EXPENSE,
                    merchant = "PAYPAL"
                )
            ),
            ParserTestCase(
                name = "Ignore addition of beneficiary",
                message = "Caisse d'Epargne : Virements - Ajout d'un bénéficiaire le 02/12/2025 sur internet. Si vous n'avez pas initié cette opération, contactez votre agence.",
                sender = "38015",
<<<<<<< ours
                expected = null,
=======
>>>>>>> theirs
                shouldParse = false
            )
        )

<<<<<<< ours
        return ParserTestUtils.runTestSuite(
            parser = parser,
            testCases = testCases,
=======
        val handleCases = listOf(
            "38015" to true,
            "UNKNOWN" to false
        )

        return ParserTestUtils.runTestSuite(
            parser = parser,
            testCases = testCases,
            handleCases = handleCases,
>>>>>>> theirs
            suiteName = "BPCE Parser"
        )
    }
}
