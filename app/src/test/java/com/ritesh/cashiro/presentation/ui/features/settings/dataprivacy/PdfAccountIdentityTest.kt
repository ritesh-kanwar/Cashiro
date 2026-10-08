package com.ritesh.cashiro.presentation.ui.features.settings.dataprivacy

import org.junit.Assert.assertNotEquals
import org.junit.Test

class PdfAccountIdentityTest {
    @Test fun sameSuffixAtDifferentBanksDoesNotShareAnImportDecision() {
        val first = PdfAccountMatch("1000", "Example Bank", null)
        val second = PdfAccountMatch("1000", "Other Bank", null)
        assertNotEquals(first.key, second.key)
        assertNotEquals(first.key, first.copy(currency = "USD").key)
    }
}
