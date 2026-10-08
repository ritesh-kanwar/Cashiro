package com.ritesh.cashiro

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.ritesh.cashiro.debug.UiVerificationActivity
import com.ritesh.cashiro.presentation.ui.features.settings.dataprivacy.CsvImportEntry
import com.ritesh.cashiro.presentation.ui.features.settings.dataprivacy.CsvImportProgressDialog
import org.junit.Rule
import org.junit.Test

class CsvImportUiTest {
    @get:Rule val compose = createAndroidComposeRule<UiVerificationActivity>()
    private fun verify(dark: Boolean) {
        val processing = mutableStateOf(false)
        compose.setContent {
            MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                CsvImportEntry { processing.value = true }
                CsvImportProgressDialog(processing.value)
            }
        }
        val resources = compose.activity.resources
        compose.onNodeWithText(resources.getString(R.string.import_csv_statement_sub)).assertIsDisplayed()
        compose.onNodeWithText(resources.getString(R.string.import_csv_statement)).performClick()
        compose.onNodeWithText(resources.getString(R.string.csv_import_progress)).assertIsDisplayed()
    }
    @Test fun csvEntryOpensProgressInLightTheme() = verify(false)
    @Test fun csvEntryOpensProgressInDarkTheme() = verify(true)
}
