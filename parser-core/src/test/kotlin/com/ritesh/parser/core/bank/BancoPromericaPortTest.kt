package com.ritesh.parser.core.bank

import com.ritesh.parser.core.TransactionType
import com.ritesh.parser.core.test.ExpectedTransaction
import com.ritesh.parser.core.test.ParserTestCase
import com.ritesh.parser.core.test.ParserTestUtils
import org.junit.jupiter.api.TestFactory
import java.math.BigDecimal

class BancoPromericaPortTest {
    @TestFactory fun syntheticFormats() = ParserTestUtils.runTestSuite(
        BancoPromericaParser(), listOf(
            ParserTestCase("Synthetic transaction", "Consumo TTA *1000 por USD100.00 en Example Shop el 01/01/2026.", "PROMERICA", ExpectedTransaction(amount = BigDecimal("100.00"), currency = "USD", type = TransactionType.EXPENSE)),
            ParserTestCase("Unrelated message", "Example service notice", "PROMERICA", shouldParse = false)
        ), suiteName = "Synthetic BancoPromerica"
    )
}
