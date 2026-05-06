package com.ritesh.cashiro.data.webhook

import com.ritesh.cashiro.data.database.entity.WebhookRangePreset
import java.time.LocalDateTime

/**
 * Time range a webhook profile syncs over. The persistence layer flattens this into three columns
 * (preset + nullable start/end) for SQL compatibility, but in-memory we keep the bounds bound to
 * the only case that actually has them so it's impossible to construct e.g. `Today(start, end)`.
 */
sealed class WebhookRange {
    data object SinceLastSuccess : WebhookRange()
    data object Today : WebhookRange()
    data object CurrentWeek : WebhookRange()
    data object CurrentMonth : WebhookRange()
    data object PreviousMonth : WebhookRange()
    data object Last30Days : WebhookRange()
    data class Custom(val start: LocalDateTime, val end: LocalDateTime) : WebhookRange()

    fun toFlat(): WebhookRangeFlat = when (this) {
        SinceLastSuccess -> WebhookRangeFlat(WebhookRangePreset.SINCE_LAST_SUCCESS)
        Today -> WebhookRangeFlat(WebhookRangePreset.TODAY)
        CurrentWeek -> WebhookRangeFlat(WebhookRangePreset.CURRENT_WEEK)
        CurrentMonth -> WebhookRangeFlat(WebhookRangePreset.CURRENT_MONTH)
        PreviousMonth -> WebhookRangeFlat(WebhookRangePreset.PREVIOUS_MONTH)
        Last30Days -> WebhookRangeFlat(WebhookRangePreset.LAST_30_DAYS)
        is Custom -> WebhookRangeFlat(WebhookRangePreset.CUSTOM, customStart = start, customEnd = end)
    }

    companion object {
        fun fromFlat(
            preset: WebhookRangePreset,
            customStart: LocalDateTime?,
            customEnd: LocalDateTime?
        ): WebhookRange = when (preset) {
            WebhookRangePreset.SINCE_LAST_SUCCESS -> SinceLastSuccess
            WebhookRangePreset.TODAY -> Today
            WebhookRangePreset.CURRENT_WEEK -> CurrentWeek
            WebhookRangePreset.CURRENT_MONTH -> CurrentMonth
            WebhookRangePreset.PREVIOUS_MONTH -> PreviousMonth
            WebhookRangePreset.LAST_30_DAYS -> Last30Days
            WebhookRangePreset.CUSTOM ->
                if (customStart != null && customEnd != null) Custom(customStart, customEnd)
                else SinceLastSuccess
        }
    }
}

data class WebhookRangeFlat(
    val preset: WebhookRangePreset,
    val customStart: LocalDateTime? = null,
    val customEnd: LocalDateTime? = null
)
