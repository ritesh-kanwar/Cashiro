package com.ritesh.cashiro

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.ritesh.cashiro.presentation.ui.features.transactions.BalanceDiscrepancyCard
import com.ritesh.cashiro.utils.BalanceDiscrepancy
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime

class BalanceDiscrepancyCardTest {
    @get:Rule val compose = createComposeRule()
    @Test fun adjustmentActionDisablesWhileSavingInDarkTheme() = verifySavingGuard(dark = true)
    @Test fun adjustmentActionDisablesWhileSavingInLightTheme() = verifySavingGuard(dark = false)

    private fun verifySavingGuard(dark: Boolean) {
        val adding = mutableStateOf(false)
        var clicks = 0
        val at = LocalDateTime.of(2026, 1, 1, 12, 0)
        val discrepancy = BalanceDiscrepancy(1, 1, "Example Bank", "1000", BigDecimal("900"), BigDecimal("850"), "INR", at.minusHours(1), at)
        compose.setContent {
            MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                BalanceDiscrepancyCard(discrepancy, adding.value, { clicks++; adding.value = true })
            }
        }
        compose.onNodeWithText("untracked expense", substring = true).performClick()
        compose.onNodeWithText("untracked expense", substring = true).assertIsNotEnabled()
        assertEquals(1, clicks)
    }
}
