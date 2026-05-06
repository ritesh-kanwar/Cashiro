package com.ritesh.cashiro.data.repository

import androidx.room.withTransaction
import com.ritesh.cashiro.data.database.CashiroDatabase
import com.ritesh.cashiro.data.database.dao.WebhookCursorDao
import com.ritesh.cashiro.data.database.dao.WebhookLogDao
import com.ritesh.cashiro.data.database.dao.WebhookProfileDao
import com.ritesh.cashiro.data.database.entity.WebhookCursorEntity
import com.ritesh.cashiro.data.database.entity.WebhookDataType
import com.ritesh.cashiro.data.database.entity.WebhookLogEntity
import com.ritesh.cashiro.data.database.entity.WebhookProfileEntity
import com.ritesh.cashiro.data.webhook.WebhookCursorUpdate
import com.ritesh.cashiro.data.webhook.WebhookHeader
import com.ritesh.cashiro.data.webhook.WebhookProfileDraft
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class WebhookRepository @Inject constructor(
    private val database: CashiroDatabase,
    private val profileDao: WebhookProfileDao,
    private val logDao: WebhookLogDao,
    private val cursorDao: WebhookCursorDao
) {
    fun getAllProfiles(): Flow<List<WebhookProfileEntity>> = profileDao.getAllProfiles()

    suspend fun getEnabledProfiles(): List<WebhookProfileEntity> = profileDao.getEnabledProfiles()

    suspend fun getProfile(profileId: String): WebhookProfileEntity? = profileDao.getProfileById(profileId)

    fun getRecentLogs(): Flow<List<WebhookLogEntity>> = logDao.getRecentLogs()

    fun getRecentLogsForProfile(profileId: String): Flow<List<WebhookLogEntity>> =
        logDao.getRecentLogsForProfile(profileId)

    suspend fun getCursors(profileId: String): List<WebhookCursorEntity> = cursorDao.getCursorsForProfile(profileId)

    suspend fun saveProfile(draft: WebhookProfileDraft): String {
        val now = LocalDateTime.now()
        val id = draft.id ?: java.util.UUID.randomUUID().toString()
        val existing = draft.id?.let { profileDao.getProfileById(it) }
        val entity = WebhookProfileEntity(
            id = id,
            name = draft.name.trim(),
            url = draft.url.trim(),
            enabled = draft.enabled,
            rangePreset = draft.rangePreset,
            customStart = draft.customStart,
            customEnd = draft.customEnd,
            dataTypes = draft.dataTypes.map { it.name },
            headersJson = encodeHeaders(draft.headers),
            // Operational fields are owned by the sync runner — preserve whatever was already there.
            lastError = existing?.lastError,
            consecutiveFailures = existing?.consecutiveFailures ?: 0,
            lastSyncedAt = existing?.lastSyncedAt,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now
        )
        profileDao.upsertProfile(entity)
        return id
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
                        dataType = update.dataType,
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

    fun decodeHeaders(headersJson: String): List<WebhookHeader> = WebhookHeaderEncoder.decode(headersJson)

    fun encodeHeaders(headers: List<WebhookHeader>): String = WebhookHeaderEncoder.encode(headers)

    fun decodeDataTypes(dataTypes: List<String>): Set<WebhookDataType> =
        dataTypes.mapNotNull { value -> runCatching { WebhookDataType.valueOf(value) }.getOrNull() }.toSet()
}
