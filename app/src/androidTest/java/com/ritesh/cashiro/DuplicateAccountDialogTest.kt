package com.ritesh.cashiro

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.ritesh.cashiro.debug.UiVerificationActivity
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsDisplayed
import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import com.ritesh.cashiro.presentation.ui.features.accounts.DuplicateAccountDialog
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import java.math.BigDecimal
import java.time.LocalDateTime

class DuplicateAccountDialogTest {
    @get:Rule val compose = createAndroidComposeRule<UiVerificationActivity>()
    private fun verify(dark: Boolean) {
        val source = AccountBalanceEntity(bankName = "Example Bank", accountLast4 = "000", balance = BigDecimal.ZERO, timestamp = LocalDateTime.of(2026, 1, 1, 0, 0))
        var merged = 0
        var kept = 0
        compose.setContent {
            MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                DuplicateAccountDialog(source, source.copy(accountLast4 = "1000"), { merged++ }, { kept++ })
            }
        }
        compose.onNodeWithText("Possible duplicate account").assertIsDisplayed()
        compose.onNodeWithText("Keep separate").performClick()
        compose.runOnIdle { assertEquals(0, merged); assertEquals(1, kept) }
        compose.onNodeWithText("Merge and remember").performClick()
        compose.runOnIdle { assertEquals(1, merged) }
    }
    @Test fun lightThemeKeepsDecisionsExplicit() = verify(false)
    @Test fun darkThemeKeepsDecisionsExplicit() = verify(true)
}
