package com.ritesh.cashiro.presentation.ui.features.settings.webhooks

import com.ritesh.cashiro.data.database.entity.WebhookLogEntity
import com.ritesh.cashiro.data.database.entity.WebhookProfileEntity
import com.ritesh.cashiro.data.webhook.WebhookSettings

data class WebhooksUiState(
    val profiles: List<WebhookProfileEntity> = emptyList(),
    val logs: List<WebhookLogEntity> = emptyList(),
    val settings: WebhookSettings = WebhookSettings(),
    val settingsLoaded: Boolean = false,
    val isSyncing: Boolean = false,
    val message: String? = null
)
