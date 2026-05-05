package com.ritesh.cashiro.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ritesh.cashiro.data.database.entity.WebhookCursorEntity
import com.ritesh.cashiro.data.database.entity.WebhookLogEntity
import com.ritesh.cashiro.data.database.entity.WebhookProfileEntity
import java.time.LocalDateTime
import kotlinx.coroutines.flow.Flow

@Dao
interface WebhookProfileDao {
    @Query("SELECT * FROM webhook_profiles ORDER BY updated_at DESC")
    fun getAllProfiles(): Flow<List<WebhookProfileEntity>>

    @Query("SELECT * FROM webhook_profiles WHERE enabled = 1 ORDER BY updated_at DESC")
    suspend fun getEnabledProfiles(): List<WebhookProfileEntity>

    @Query("SELECT * FROM webhook_profiles WHERE id = :profileId LIMIT 1")
    suspend fun getProfileById(profileId: String): WebhookProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: WebhookProfileEntity)

    @Update
    suspend fun updateProfile(profile: WebhookProfileEntity)

    @Query("DELETE FROM webhook_profiles WHERE id = :profileId")
    suspend fun deleteProfile(profileId: String)

    @Query(
        """
        UPDATE webhook_profiles
        SET last_error = :lastError,
            consecutive_failures = consecutive_failures + 1,
            updated_at = :updatedAt
        WHERE id = :profileId
        """
    )
    suspend fun markFailure(profileId: String, lastError: String, updatedAt: LocalDateTime)

    @Query(
        """
        UPDATE webhook_profiles
        SET last_error = NULL,
            consecutive_failures = 0,
            last_synced_at = :syncedAt,
            updated_at = :updatedAt
        WHERE id = :profileId
        """
    )
    suspend fun markSuccess(profileId: String, syncedAt: LocalDateTime, updatedAt: LocalDateTime)
}

@Dao
interface WebhookLogDao {
    @Query("SELECT * FROM webhook_logs ORDER BY created_at DESC LIMIT 100")
    fun getRecentLogs(): Flow<List<WebhookLogEntity>>

    @Query("SELECT * FROM webhook_logs WHERE profile_id = :profileId ORDER BY created_at DESC LIMIT 30")
    fun getRecentLogsForProfile(profileId: String): Flow<List<WebhookLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: WebhookLogEntity)

    @Query(
        """
        DELETE FROM webhook_logs
        WHERE id NOT IN (
            SELECT id FROM webhook_logs ORDER BY created_at DESC LIMIT 100
        )
        """
    )
    suspend fun trimToLatest100()
}

@Dao
interface WebhookCursorDao {
    @Query("SELECT * FROM webhook_cursors WHERE profile_id = :profileId")
    suspend fun getCursorsForProfile(profileId: String): List<WebhookCursorEntity>

    @Query("SELECT * FROM webhook_cursors WHERE profile_id = :profileId AND data_type = :dataType LIMIT 1")
    suspend fun getCursor(profileId: String, dataType: String): WebhookCursorEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCursor(cursor: WebhookCursorEntity)

    @Query("DELETE FROM webhook_cursors WHERE profile_id = :profileId")
    suspend fun deleteForProfile(profileId: String)
}
