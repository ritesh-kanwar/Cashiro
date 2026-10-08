package com.ritesh.parser.core.bank

import com.ritesh.parser.core.MandateInfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.math.BigDecimal

class MandateAccountSuffixTest {
    private data class Case(
        val name: String,
        val message: String,
        val parse: (String) -> MandateInfo?
    )

    @TestFactory
    fun `mandates expose debiting account through common interface`() = listOf(
        Case("HDFC mandate", "E-Mandate! Rs.100.00 will be deducted from HDFC Bank A/c XX1000 on 01/01/26 For Example Service mandate UMN example@bank", HDFCBankParser()::parseEMandateSubscription),
        Case("HDFC future debit", "INR 100.00 will be debited from HDFC Bank A/c XX1000 on 01/01/2026 for Example Service", HDFCBankParser()::parseFutureDebit),
        Case("SBI creation", "UPI-Mandate successfully created for Rs.100.00 towards Example Service from A/c XX1000. UMN:example@bank", SBIBankParser()::parseUPIMandateSubscription),
        Case("PNB creation", "UPI-Mandate successfully created towards Example Service for Rs.100.00 from A/c XX1000. UMN:example@bank", PNBBankParser()::parseMandateSubscription),
        Case("Indian Bank", "A/c XX1000 mandate set for 01-Jan-26, your account will be debited with INR 100.00 towards Example Service.", IndianBankParser()::parseMandateSubscription),
        Case("Federal creation", "You have successfully created a mandate on Example Service for a maximum amount of Rs.100.00 starting from 01-01-2026. A/c XX1000. Mandate Ref No-example@bank", FederalBankParser()::parseEMandateSubscription),
        Case("Federal future debit", "Payment due for Example Service, INR 100.00 will be processed on 01/01/2026 from A/c XX1000", FederalBankParser()::parseFutureDebit),
        Case("Generic Indian mandate", "INR 100.00 will be debited from A/c XX1000 on 01-Jan-26 towards Example Service.", genericParser()::parseMandateSubscription)
    ).map { case ->
        dynamicTest(case.name) {
            val info: MandateInfo? = case.parse(case.message)
            assertNotNull(info)
            assertEquals(BigDecimal("100.00"), info!!.amount)
            assertEquals("1000", info.accountLast4)
        }
    }

    @Test
    fun `unknown funding account remains unknown and old DTO constructors remain valid`() {
        val message = "INR 100.00 will be debited on 01-Jan-26 towards Example Service."
        val info = genericParser().parseMandateSubscription(message)
        assertNotNull(info)
        assertNull(info!!.accountLast4)
        val legacy: MandateInfo = SBIBankParser.UPIMandateInfo(BigDecimal("100.00"), null, "Example Service", null)
        assertNull(legacy.accountLast4)
    }

    private fun genericParser() = object : BaseIndianBankParser() {
        override fun getBankName() = "Example Bank"
        override fun canHandle(sender: String) = sender == "EXAMPLE"
    }
}
