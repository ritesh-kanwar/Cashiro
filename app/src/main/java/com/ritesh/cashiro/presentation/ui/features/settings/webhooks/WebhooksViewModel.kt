package com.ritesh.cashiro.presentation.ui.features.settings.webhooks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ritesh.cashiro.data.database.entity.WebhookDataType
import com.ritesh.cashiro.data.database.entity.WebhookProfileEntity
import com.ritesh.cashiro.data.database.entity.WebhookRangePreset
import com.ritesh.cashiro.data.preferences.UserPreferencesRepository
import com.ritesh.cashiro.data.repository.WebhookRepository
import com.ritesh.cashiro.data.webhook.WebhookHeader
import com.ritesh.cashiro.data.webhook.WebhookProfileDraft
import com.ritesh.cashiro.data.webhook.WebhookSyncManager
import com.ritesh.cashiro.data.webhook.WebhookSyncReason
import com.ritesh.cashiro.data.webhook.WebhookSyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class WebhooksViewModel @Inject constructor(
    private val webhookRepository: WebhookRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val webhookSyncManager: WebhookSyncManager,
    private val webhookSyncScheduler: WebhookSyncScheduler
) : ViewModel() {

    private val syncing = MutableStateFlow(false)
    private val flashMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<WebhooksUiState> = combine(
        webhookRepository.getAllProfiles(),
        webhookRepository.getRecentLogs(),
        userPreferencesRepository.webhookSettings,
        syncing,
        flashMessage
    ) { profiles, logs, settings, isSyncing, message ->
        WebhooksUiState(
            profiles = profiles,
            logs = logs,
            settings = settings,
            isSyncing = isSyncing,
            message = message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WebhooksUiState()
    )

    fun clearMessage() {
        flashMessage.value = null
    }

    fun syncNow() {
        runSync(WebhookSyncReason.MANUAL, sendTestPayload = false)
    }

    fun sendTest() {
        runSync(WebhookSyncReason.TEST, sendTestPayload = true)
    }

    fun saveSettings(newSettings: com.ritesh.cashiro.data.webhook.WebhookSettings) {
        viewModelScope.launch {
            userPreferencesRepository.updateWebhookSettings(newSettings)
            webhookSyncScheduler.applyScheduling()
            // Removed flash message to prevent layout jumps on every minor setting change
        }
    }

    fun toggleProfile(profile: WebhookProfileEntity, enabled: Boolean) {
        viewModelScope.launch {
            webhookRepository.saveProfile(
                profile.toDraft(
                    headers = webhookRepository.decodeHeaders(profile.headersJson)
                ).copy(enabled = enabled)
            )
            flashMessage.value = if (enabled) "Webhook enabled" else "Webhook disabled"
        }
    }

    fun deleteProfile(profileId: String) {
        viewModelScope.launch {
            webhookRepository.deleteProfile(profileId)
            flashMessage.value = "Webhook deleted"
        }
    }

    suspend fun loadDraft(profileId: String?): WebhookProfileDraft {
        if (profileId == null) {
            return WebhookProfileDraft(
                name = "",
                url = "",
                enabled = true,
                dataTypes = setOf(WebhookDataType.SUMMARY, WebhookDataType.TRANSACTIONS),
                rangePreset = WebhookRangePreset.SINCE_LAST_SUCCESS,
                currency = "INR",
                headers = emptyList()
            )
        }
        val profile = webhookRepository.getProfile(profileId)
        return profile?.toDraft(webhookRepository.decodeHeaders(profile.headersJson)) ?: WebhookProfileDraft(
            name = "",
            url = "",
            enabled = true,
            dataTypes = setOf(WebhookDataType.SUMMARY, WebhookDataType.TRANSACTIONS),
            rangePreset = WebhookRangePreset.SINCE_LAST_SUCCESS,
            currency = "INR",
            headers = emptyList()
        )
    }

    fun saveProfile(draft: WebhookProfileDraft, onSaved: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            if (!draft.url.startsWith("http://") && !draft.url.startsWith("https://")) {
                onError("Webhook URL must start with http:// or https://")
                return@launch
            }
            if (draft.name.isBlank()) {
                onError("Webhook name is required")
                return@launch
            }
            if (draft.dataTypes.isEmpty()) {
                onError("Choose at least one data type")
                return@launch
            }
            if (draft.rangePreset == WebhookRangePreset.CUSTOM && (draft.customStart == null || draft.customEnd == null)) {
                onError("Custom range needs both start and end")
                return@launch
            }
            val profileId = webhookRepository.saveProfile(draft)
            flashMessage.value = "Webhook saved"
            onSaved(profileId)
        }
    }

    private fun runSync(reason: WebhookSyncReason, sendTestPayload: Boolean) {
        viewModelScope.launch {
            syncing.value = true
            val result = webhookSyncManager.syncAll(reason, sendTestPayload)
            syncing.value = false
            flashMessage.value = when {
                result.anySuccess && sendTestPayload -> "Synthetic test sent"
                result.anySuccess -> "Webhook sync completed"
                result.anyRetryableFailure -> "Webhook sync failed, retry scheduled"
                else -> "No enabled webhook profiles to sync"
            }
        }
    }
}

private fun WebhookProfileEntity.toDraft(headers: List<WebhookHeader>): WebhookProfileDraft = WebhookProfileDraft(
    id = id,
    name = name,
    url = url,
    enabled = enabled,
    dataTypes = dataTypes.mapNotNull { value -> WebhookDataType.entries.find { it.name == value } }.toSet(),
    rangePreset = WebhookRangePreset.valueOf(rangePreset),
    customStart = customStart,
    customEnd = customEnd,
    currency = currency,
    headers = headers
)
