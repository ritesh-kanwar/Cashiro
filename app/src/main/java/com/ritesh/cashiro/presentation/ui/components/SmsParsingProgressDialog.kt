package com.ritesh.cashiro.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.work.WorkInfo
import com.ritesh.cashiro.R
import com.ritesh.cashiro.presentation.ui.theme.LocalBlurEffects
import com.ritesh.cashiro.worker.OptimizedSmsReaderWorker
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeEffectScope
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect

@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeApi::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun SmsParsingProgressDialog(
    modifier: Modifier = Modifier,
    isVisible: Boolean,
    workInfo: WorkInfo?,
    onDismiss: () -> Unit,
    onCancel: (() -> Unit)? = null,
    blurEffects: Boolean = LocalBlurEffects.current,
    hazeState: HazeState = remember { HazeState() },
) {
    val containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    if (isVisible && workInfo != null) {
        Dialog(onDismissRequest = { }) {
            Card(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .then(
                        if (blurEffects) Modifier.hazeEffect(
                            state = hazeState,
                            block = fun HazeEffectScope.() {
                                style = HazeDefaults.style(
                                    backgroundColor = Color.Transparent,
                                    tint = HazeDefaults.tint(containerColor),
                                    blurRadius = 20.dp,
                                    noiseFactor = -1f,
                                )
                                blurredEdgeTreatment = BlurredEdgeTreatment.Unbounded
                            }
                        ) else Modifier
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (blurEffects) MaterialTheme.colorScheme.surfaceContainerLow.copy(0.5f)
                    else MaterialTheme.colorScheme.surfaceContainerLow,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Title
                    Text(
                        text = stringResource(R.string.scanning_sms_messages_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    // Progress Details
                    ProgressDetails(workInfo = workInfo)

                    // Progress Bar
                    if (workInfo.progress.getInt(OptimizedSmsReaderWorker.PROGRESS_TOTAL, 0) > 0) {
                        val targetProgress = workInfo.progress.getInt(OptimizedSmsReaderWorker.PROGRESS_PROCESSED, 0).toFloat() /
                                workInfo.progress.getInt(OptimizedSmsReaderWorker.PROGRESS_TOTAL, 1).toFloat()
                        val animatedProgress by animateFloatAsState(targetValue = targetProgress, label = "progress")

                        LinearWavyProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                        )
                    }

                    // Cancel Button
                    if (onCancel != null && workInfo.state == WorkInfo.State.RUNNING) {
                        Button(
                            onClick = onCancel,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.Cancel,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.cancel_scan))
                        }
                    } else if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.onPrimary,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.done))
                        }
                    } else if (workInfo.state == WorkInfo.State.FAILED) {
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.textButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceBright,
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text(stringResource(R.string.close))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressDetails(workInfo: WorkInfo) {
    val totalMessages = workInfo.progress.getInt(OptimizedSmsReaderWorker.PROGRESS_TOTAL, 0)
    val processedMessages = workInfo.progress.getInt(OptimizedSmsReaderWorker.PROGRESS_PROCESSED, 0)
    val parsedTransactions = workInfo.progress.getInt(OptimizedSmsReaderWorker.PROGRESS_PARSED, 0)
    val savedTransactions = workInfo.progress.getInt(OptimizedSmsReaderWorker.PROGRESS_SAVED, 0)
    val timeElapsed = workInfo.progress.getLong(OptimizedSmsReaderWorker.PROGRESS_TIME_ELAPSED, 0L)
    val estimatedTimeRemaining = workInfo.progress.getLong(OptimizedSmsReaderWorker.PROGRESS_ESTIMATED_TIME_REMAINING, 0L)
    val currentBatch = workInfo.progress.getInt(OptimizedSmsReaderWorker.PROGRESS_CURRENT_BATCH, 0)
    val totalBatches = workInfo.progress.getInt(OptimizedSmsReaderWorker.PROGRESS_TOTAL_BATCHES, 0)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Main progress text
        if (totalMessages > 0) {
            val progressText = if (processedMessages == totalMessages) {
                stringResource(R.string.all_messages_processed)
            } else {
                stringResource(R.string.processed_messages_format, processedMessages, totalMessages)
            }

            Text(
                text = progressText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }

        // Transaction details
        if (parsedTransactions > 0 || savedTransactions > 0) {
            val parsedText = pluralStringResource(R.plurals.transactions_parsed_format, parsedTransactions, parsedTransactions)
            val savedText = pluralStringResource(R.plurals.saved_transactions_format, savedTransactions, savedTransactions)
            val detailsText = buildAnnotatedString {
                if (parsedTransactions > 0) {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Medium)) {
                        append(parsedText)
                    }
                }
                if (parsedTransactions > 0 && savedTransactions > 0) {
                    append(" • ")
                }
                if (savedTransactions > 0) {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Medium)) {
                        append(savedText)
                    }
                }
            }

            Text(
                text = detailsText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
        }

        // Time information
        if (timeElapsed > 0) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Time elapsed
                Text(
                    text = formatDuration(timeElapsed),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Estimated time remaining
                if (estimatedTimeRemaining > 0 && workInfo.state == WorkInfo.State.RUNNING) {
                    Text(
                        text = stringResource(R.string.time_left_format, formatDuration(estimatedTimeRemaining)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Batch information (for parallel processing)
        if (totalBatches > 1 && workInfo.state == WorkInfo.State.RUNNING) {
            Text(
                text = stringResource(R.string.batch_progress_format, currentBatch, totalBatches),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Status based on work state
        when (workInfo.state) {
            WorkInfo.State.RUNNING -> {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LoadingCircle(modifier = Modifier.size(16.dp))
                    Text(
                        text = stringResource(R.string.processing),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            WorkInfo.State.SUCCEEDED -> {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.scan_completed_success),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            WorkInfo.State.FAILED -> {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.scan_failed_error),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            WorkInfo.State.CANCELLED -> {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Cancel,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.scan_cancelled),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                // For ENQUEUED or BLOCKED states
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LoadingCircle(
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = stringResource(R.string.starting_scan),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun formatDuration(milliseconds: Long): String {
    val seconds = milliseconds / 1000
    val minutes = seconds / 60
    val hours = minutes / 60

    return when {
        hours > 0 -> stringResource(R.string.duration_hours_minutes, hours, minutes % 60)
        minutes > 0 -> stringResource(R.string.duration_minutes_seconds, minutes, seconds % 60)
        else -> stringResource(R.string.duration_seconds, seconds)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SmsParsingProgressIndicator(
    workInfo: WorkInfo?,
    modifier: Modifier = Modifier
) {
    if (workInfo != null && workInfo.state == WorkInfo.State.RUNNING) {
        val totalMessages = workInfo.progress.getInt(OptimizedSmsReaderWorker.PROGRESS_TOTAL, 0)
        val processedMessages = workInfo.progress.getInt(OptimizedSmsReaderWorker.PROGRESS_PROCESSED, 0)
        val timeElapsed = workInfo.progress.getLong(OptimizedSmsReaderWorker.PROGRESS_TIME_ELAPSED, 0L)
        val estimatedTimeRemaining = workInfo.progress.getLong(OptimizedSmsReaderWorker.PROGRESS_ESTIMATED_TIME_REMAINING, 0L)

        if (totalMessages > 0) {
            val targetProgress = processedMessages.toFloat() / totalMessages.toFloat()
            val animatedProgress by animateFloatAsState(targetValue = targetProgress, label = "progress")

            Column(
                modifier = modifier,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                LinearWavyProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                )

                Text(
                    text = stringResource(R.string.scanning_sms_format, processedMessages, totalMessages),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (timeElapsed > 0) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatDuration(timeElapsed),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (estimatedTimeRemaining > 0) {
                            Text(
                                text = stringResource(R.string.time_left_format, formatDuration(estimatedTimeRemaining)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}