package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import com.ritesh.parser.core.test.ExpectedTransaction
import com.ritesh.parser.core.test.ParserTestCase
import com.ritesh.parser.core.test.ParserTestUtils
import org.junit.jupiter.api.TestFactory
import java.math.BigDecimal

class BancoAgricolaPortTest {
    @TestFactory fun syntheticFormats() = ParserTestUtils.runTestSuite(
        BancoAgricolaParser(), listOf(
            ParserTestCase("Synthetic transaction", "Compra por USD100.00 en Example Shop el 01/01/2026.", "AGRICOLA", ExpectedTransaction(amount = BigDecimal("100.00"), currency = "USD", type = TransactionType.EXPENSE)),
            ParserTestCase("Unrelated message", "Example service notice", "AGRICOLA", shouldParse = false)
        ), suiteName = "Synthetic BancoAgricola"
    )
}
