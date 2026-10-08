package com.ritesh.cashiro

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.work.Data
import androidx.work.WorkInfo
import androidx.work.workDataOf
import com.ritesh.cashiro.debug.UiVerificationActivity
import com.ritesh.cashiro.presentation.ui.components.SmsParsingProgressDialog
import com.ritesh.cashiro.worker.OptimizedSmsReaderWorker
import org.junit.Rule
import org.junit.Test
import java.util.UUID

class CompletedScanDialogTest {
    @get:Rule val compose = createAndroidComposeRule<UiVerificationActivity>()
    private fun verify(dark: Boolean) {
        val output = workDataOf(OptimizedSmsReaderWorker.PROGRESS_TOTAL to 20,
            OptimizedSmsReaderWorker.PROGRESS_PROCESSED to 20,
            OptimizedSmsReaderWorker.PROGRESS_PARSED to 4,
            OptimizedSmsReaderWorker.PROGRESS_SAVED to 3)
        val completed = WorkInfo(UUID.randomUUID(), WorkInfo.State.SUCCEEDED, emptySet(), output, Data.EMPTY)
        compose.setContent {
            MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                SmsParsingProgressDialog(isVisible = true, workInfo = completed, onDismiss = {}, blurEffects = false)
            }
        }
        val resources = compose.activity.resources
        compose.onNodeWithText(resources.getString(R.string.all_messages_processed)).assertIsDisplayed()
        compose.onNodeWithText(resources.getQuantityString(R.plurals.transactions_parsed_format, 4, 4), substring = true).assertIsDisplayed()
        compose.onNodeWithText(resources.getQuantityString(R.plurals.saved_transactions_format, 3, 3), substring = true).assertIsDisplayed()
    }
    @Test fun lightThemeShowsSavedOutputAfterProgressIsCleared() = verify(false)
    @Test fun darkThemeShowsSavedOutputAfterProgressIsCleared() = verify(true)
}
