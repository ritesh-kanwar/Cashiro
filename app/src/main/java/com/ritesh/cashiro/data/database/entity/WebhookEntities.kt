package com.ritesh.cashiro.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime
import java.util.UUID

@Entity(tableName = "webhook_profiles")
data class WebhookProfileEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "url")
    val url: String,
    @ColumnInfo(name = "enabled", defaultValue = "1")
    val enabled: Boolean = true,
    @ColumnInfo(name = "range_preset")
    val rangePreset: WebhookRangePreset = WebhookRangePreset.SINCE_LAST_SUCCESS,
    @ColumnInfo(name = "custom_start")
    val customStart: LocalDateTime? = null,
    @ColumnInfo(name = "custom_end")
    val customEnd: LocalDateTime? = null,
    @ColumnInfo(name = "data_types", defaultValue = "")
    val dataTypes: List<String> = listOf(WebhookDataType.SUMMARY.name, WebhookDataType.TRANSACTIONS.name),
    @ColumnInfo(name = "headers_json", defaultValue = "[]")
    val headersJson: String = "[]",
    @ColumnInfo(name = "last_error")
    val lastError: String? = null,
    @ColumnInfo(name = "consecutive_failures", defaultValue = "0")
    val consecutiveFailures: Int = 0,
    @ColumnInfo(name = "last_synced_at")
    val lastSyncedAt: LocalDateTime? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

@Entity(
    tableName = "webhook_logs",
    foreignKeys = [
        ForeignKey(
            entity = WebhookProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profile_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("profile_id"), Index("created_at")]
)
data class WebhookLogEntity(
    @ColumnInfo(name = "id")
    @androidx.room.PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "profile_id")
    val profileId: String,
    // Snapshotted from webhook_profiles.name at the time of delivery so renames don't rewrite history.
    @ColumnInfo(name = "profile_name")
    val profileName: String,
    @ColumnInfo(name = "sync_reason")
    val syncReason: com.ritesh.cashiro.data.webhook.WebhookSyncReason,
    @ColumnInfo(name = "status")
    val status: WebhookLogStatus,
    @ColumnInfo(name = "message")
    val message: String,
    @ColumnInfo(name = "http_status")
    val httpStatus: Int? = null,
    @ColumnInfo(name = "batch_count", defaultValue = "0")
    val batchCount: Int = 0,
    @ColumnInfo(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now()
)

@Entity(
    tableName = "webhook_cursors",
    primaryKeys = ["profile_id", "data_type"],
    foreignKeys = [
        ForeignKey(
            entity = WebhookProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profile_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("profile_id")]
)
data class WebhookCursorEntity(
    @ColumnInfo(name = "profile_id")
    val profileId: String,
    @ColumnInfo(name = "data_type")
    val dataType: WebhookDataType,
    @ColumnInfo(name = "last_success_at")
    val lastSuccessAt: LocalDateTime? = null,
    @ColumnInfo(name = "last_range_end")
    val lastRangeEnd: LocalDateTime? = null,
    @ColumnInfo(name = "updated_at")
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

enum class WebhookDataType {
    SUMMARY,
    TRANSACTIONS,
    BUDGETS,
    ACCOUNTS,
    SUBSCRIPTIONS
}

enum class WebhookLogStatus {
    SUCCESS,
    FAILURE;

    companion object {
        fun fromLegacy(value: String): WebhookLogStatus = when (value.uppercase()) {
            "SUCCESS", "DELIVERED" -> SUCCESS
            else -> FAILURE
        }
    }
}

enum class WebhookRangePreset {
    SINCE_LAST_SUCCESS,
    TODAY,
    CURRENT_WEEK,
    CURRENT_MONTH,
    PREVIOUS_MONTH,
    LAST_30_DAYS,
    CUSTOM
}
