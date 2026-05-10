package com.ritesh.cashiro.data.webhook

import com.ritesh.cashiro.data.database.entity.WebhookLogEntity
import com.ritesh.cashiro.data.preferences.UserPreferencesRepository
import com.ritesh.cashiro.data.repository.WebhookRepository
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

data class WebhookSyncRunResult(
    val anySuccess: Boolean,
    val anyRetryableFailure: Boolean
)

internal data class BatchOutcome(
    val cursorUpdates: List<WebhookCursorUpdate>,
    val attempt: WebhookAttemptResult
)

/**
 * All-or-nothing: returns null if any batch failed, so the cursor stays put and the
 * worker's retry re-ships the entire range. Receivers dedup by stable transaction id.
 */
internal fun resolveCursorAdvance(
    outcomes: List<BatchOutcome>,
    sendTestPayload: Boolean
): List<WebhookCursorUpdate>? {
    if (sendTestPayload) return null
    if (outcomes.isEmpty()) return null
    if (outcomes.any { !it.attempt.success }) return null
    return outcomes.last().cursorUpdates
}

@Singleton
class WebhookSyncManager @Inject constructor(
    private val webhookRepository: WebhookRepository,
    private val payloadBuilder: WebhookPayloadBuilder,
    private val deliveryService: WebhookDeliveryService,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    suspend fun syncAll(reason: WebhookSyncReason, sendTestPayload: Boolean = false): WebhookSyncRunResult {
        if (!userPreferencesRepository.isDeveloperModeEnabled.first()) {
            // Defence in depth: if a user toggles dev mode off while WorkManager has work
            // enqueued or AlarmManager has alarms armed, those callbacks must not fire deliveries.
            return WebhookSyncRunResult(anySuccess = false, anyRetryableFailure = false)
        }
        val profiles = webhookRepository.getEnabledProfiles()
        var anySuccess = false
        var anyRetryableFailure = false

        profiles.forEach { profile ->
            val result = syncProfile(profile.id, reason, sendTestPayload)
            anySuccess = anySuccess || result.anySuccess
            anyRetryableFailure = anyRetryableFailure || result.anyRetryableFailure
        }
        return WebhookSyncRunResult(anySuccess = anySuccess, anyRetryableFailure = anyRetryableFailure)
    }

    suspend fun syncProfile(profileId: String, reason: WebhookSyncReason, sendTestPayload: Boolean = false): WebhookSyncRunResult {
        // The dev-mode gate is read once in syncAll() (the only public entry point) and inherited
        // here. syncProfile is package-private-ish; keeping a second .first() read on the same
        // immutable-within-the-call Flow would be a wasted DataStore round-trip per profile.
        val profile = webhookRepository.getProfile(profileId)
            ?: return WebhookSyncRunResult(anySuccess = false, anyRetryableFailure = false)
        val headers = webhookRepository.decodeHeaders(profile.headersJson)
        val dataTypes = webhookRepository.decodeDataTypes(profile.dataTypes)
        val currency = userPreferencesRepository.baseCurrency.first()
        val cursorState = webhookRepository.getCursors(profile.id)
        val batches = payloadBuilder.build(profile, dataTypes, currency, cursorState, sendTestPayload)
        val outcomes = mutableListOf<BatchOutcome>()

        for (batch in batches) {
            val attempt = deliveryService.deliver(profile.url, headers, batch.envelope)
            webhookRepository.appendLog(
                WebhookLogEntity(
                    profileId = profile.id,
                    profileName = profile.name,
                    syncReason = reason,
                    status = if (attempt.success) com.ritesh.cashiro.data.database.entity.WebhookLogStatus.SUCCESS
                        else com.ritesh.cashiro.data.database.entity.WebhookLogStatus.FAILURE,
                    message = attempt.message,
                    httpStatus = attempt.httpStatus,
                    batchCount = batch.envelope.batch.count
                )
            )
            outcomes += BatchOutcome(batch.cursorUpdates, attempt)
            if (!attempt.success) {
                webhookRepository.markFailure(profile.id, attempt.message)
                break
            }
        }

        val cursorUpdates = resolveCursorAdvance(outcomes, sendTestPayload)
        if (cursorUpdates != null) {
            webhookRepository.markSuccess(profile.id, LocalDateTime.now(), cursorUpdates)
        }

        val anySuccess = outcomes.any { it.attempt.success }
        val anyRetryableFailure = outcomes.any { !it.attempt.success && it.attempt.retryable }
        return WebhookSyncRunResult(anySuccess = anySuccess, anyRetryableFailure = anyRetryableFailure)
    }
}
