package com.ritesh.cashiro.data.repository

import androidx.room.withTransaction
import com.ritesh.cashiro.data.database.CashiroDatabase
import com.ritesh.cashiro.data.database.dao.WebhookCursorDao
import com.ritesh.cashiro.data.database.dao.WebhookLogDao
import com.ritesh.cashiro.data.database.dao.WebhookProfileDao
import com.ritesh.cashiro.data.database.entity.WebhookCursorEntity
import com.ritesh.cashiro.data.database.entity.WebhookLogEntity
import com.ritesh.cashiro.data.database.entity.WebhookProfileEntity
import com.ritesh.cashiro.data.webhook.WebhookCursorUpdate
import com.ritesh.cashiro.data.webhook.WebhookHeader
import com.ritesh.cashiro.data.webhook.WebhookProfileDraft
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Singleton
class WebhookRepository @Inject constructor(
    private val database: CashiroDatabase,
    private val profileDao: WebhookProfileDao,
    private val logDao: WebhookLogDao,
    private val cursorDao: WebhookCursorDao
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun getAllProfiles(): Flow<List<WebhookProfileEntity>> = profileDao.getAllProfiles()

    suspend fun getEnabledProfiles(): List<WebhookProfileEntity> = profileDao.getEnabledProfiles()

    suspend fun getProfile(profileId: String): WebhookProfileEntity? = profileDao.getProfileById(profileId)

    fun getRecentLogs(): Flow<List<WebhookLogEntity>> = logDao.getRecentLogs()

    fun getRecentLogsForProfile(profileId: String): Flow<List<WebhookLogEntity>> =
        logDao.getRecentLogsForProfile(profileId)

    suspend fun getCursors(profileId: String): List<WebhookCursorEntity> = cursorDao.getCursorsForProfile(profileId)

    suspend fun saveProfile(draft: WebhookProfileDraft): String {
        val now = LocalDateTime.now()
        val entity = WebhookProfileEntity(
            id = draft.id ?: java.util.UUID.randomUUID().toString(),
            name = draft.name.trim(),
            url = draft.url.trim(),
            enabled = draft.enabled,
            dataTypes = draft.dataTypes.map { it.name },
            rangePreset = draft.rangePreset.name,
            customStart = draft.customStart,
            customEnd = draft.customEnd,
            currency = draft.currency.trim().uppercase(),
            headersJson = encodeHeaders(draft.headers),
            updatedAt = now,
            createdAt = now
        )
        val existing = draft.id?.let { profileDao.getProfileById(it) }
        profileDao.upsertProfile(
            if (existing == null) entity else entity.copy(
                createdAt = existing.createdAt,
                consecutiveFailures = existing.consecutiveFailures,
                lastError = existing.lastError,
                lastSyncedAt = existing.lastSyncedAt
            )
        )
        return entity.id
    }

    suspend fun deleteProfile(profileId: String) {
        database.withTransaction {
            cursorDao.deleteForProfile(profileId)
            profileDao.deleteProfile(profileId)
        }
    }

    suspend fun appendLog(log: WebhookLogEntity) {
        logDao.insertLog(log)
        logDao.trimToLatest100()
    }

    suspend fun markSuccess(profileId: String, syncedAt: LocalDateTime, cursorUpdates: List<WebhookCursorUpdate>) {
        database.withTransaction {
            profileDao.markSuccess(profileId, syncedAt, syncedAt)
            cursorUpdates.forEach { update ->
                cursorDao.upsertCursor(
                    WebhookCursorEntity(
                        profileId = profileId,
                        dataType = update.dataType.name,
                        lastSuccessAt = update.successAt,
                        lastRangeEnd = update.rangeEnd,
                        updatedAt = syncedAt
                    )
                )
            }
        }
    }

    suspend fun markFailure(profileId: String, message: String) {
        profileDao.markFailure(profileId, message, LocalDateTime.now())
    }

    fun decodeHeaders(headersJson: String): List<WebhookHeader> {
        return try {
            json.decodeFromString<List<WebhookHeader>>(headersJson)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun encodeHeaders(headers: List<WebhookHeader>): String {
        return json.encodeToString(headers.filter { it.key.isNotBlank() })
    }
}
