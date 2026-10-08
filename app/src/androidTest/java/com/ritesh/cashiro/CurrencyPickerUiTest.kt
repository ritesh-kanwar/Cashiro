package com.ritesh.cashiro

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import com.ritesh.cashiro.data.model.Currency
import com.ritesh.cashiro.debug.UiVerificationActivity
import com.ritesh.cashiro.presentation.ui.components.CurrencySelectionContent
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CurrencyPickerUiTest {
    @get:Rule val compose = createAndroidComposeRule<UiVerificationActivity>()

    private fun verify(dark: Boolean) {
        var selection = ""
        compose.setContent {
            MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                CurrencySelectionContent("USD", Currency.SUPPORTED_CURRENCIES.map { it.code }, { selection = it })
            }
        }
        compose.onNodeWithText("Select Currency").assertIsDisplayed()
        compose.onNodeWithTag("currency_picker_list").performScrollToNode(hasText("Thai Baht"))
        compose.onNodeWithText("Thai Baht").performClick()
        compose.runOnIdle { assertEquals("THB", selection) }
        compose.onNodeWithText("Search currencies...").performTextInput("Indian")
        compose.onNodeWithText("Indian Rupee").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals("INR", selection) }
        compose.onNodeWithContentDescription("Clear").performClick()
        compose.onNodeWithText("Search currencies...").performTextInput("no such currency")
        compose.onNodeWithText("Currency not available").assertIsDisplayed()
    }

    @Test fun lightThemeSupportsScrollingAndSearch() = verify(false)
    @Test fun darkThemeSupportsScrollingAndSearch() = verify(true)
}
