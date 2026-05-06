package com.ritesh.cashiro.data.database.converter

import androidx.room.TypeConverter
import com.ritesh.cashiro.data.database.entity.SubscriptionState
import com.ritesh.cashiro.data.database.entity.TransactionType
import com.ritesh.cashiro.data.database.entity.BudgetPeriod
import com.ritesh.cashiro.data.database.entity.BudgetTrackType
import com.ritesh.cashiro.data.database.entity.BudgetType
import com.ritesh.cashiro.data.database.entity.WebhookDataType
import com.ritesh.cashiro.data.database.entity.WebhookLogStatus
import com.ritesh.cashiro.data.database.entity.WebhookRangePreset
import com.ritesh.cashiro.data.webhook.WebhookSyncReason
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class Converters {
    // Handle both ISO format (2025-08-01T18:17:16) and space format (2025-08-01 18:17:16)
    private val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    private val dateTimeFormatterWithSpace = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    
    @TypeConverter
    fun fromBigDecimal(value: BigDecimal?): String? {
        return value?.toString()
    }
    
    @TypeConverter
    fun toBigDecimal(value: String?): BigDecimal? {
        return value?.let { BigDecimal(it) }
    }
    
    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime?): String? {
        return value?.format(dateTimeFormatter)
    }
    
    @TypeConverter
    fun toLocalDateTime(value: String?): LocalDateTime? {
        return value?.let { dateStr ->
            try {
                // Try ISO format first (with 'T')
                LocalDateTime.parse(dateStr, dateTimeFormatter)
            } catch (e: Exception) {
                // Fall back to space format
                LocalDateTime.parse(dateStr, dateTimeFormatterWithSpace)
            }
        }
    }
    
    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? {
        return value?.format(dateFormatter)
    }
    
    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? {
        return value?.let { dateString ->
            // Handle invalid dates
            if (dateString == "0000-00-00" || dateString.isBlank()) {
                return null
            }
            try {
                LocalDate.parse(dateString, dateFormatter)
            } catch (e: Exception) {
                android.util.Log.w("Converters", "Invalid date format: $dateString", e)
                null
            }
        }
    }
    
    @TypeConverter
    fun fromTransactionType(value: TransactionType): String {
        return value.name
    }
    
    @TypeConverter
    fun toTransactionType(value: String): TransactionType {
        return TransactionType.valueOf(value)
    }
    
    @TypeConverter
    fun fromSubscriptionState(value: SubscriptionState): String {
        return value.name
    }
    
    @TypeConverter
    fun toSubscriptionState(value: String): SubscriptionState {
        return SubscriptionState.valueOf(value)
    }

    @TypeConverter
    fun fromBudgetPeriod(value: BudgetPeriod): String {
        return value.name
    }

    @TypeConverter
    fun toBudgetPeriod(value: String): BudgetPeriod {
        return try {
            BudgetPeriod.valueOf(value)
        } catch (e: Exception) {
            BudgetPeriod.MONTHLY
        }
    }

    @TypeConverter
    fun fromBudgetTrackType(value: BudgetTrackType): String {
        return value.name
    }

    @TypeConverter
    fun toBudgetTrackType(value: String): BudgetTrackType {
        return try {
            BudgetTrackType.valueOf(value)
        } catch (e: Exception) {
            BudgetTrackType.ALL_TRANSACTIONS
        }
    }

    @TypeConverter
    fun fromBudgetType(value: BudgetType): String {
        return value.name
    }

    @TypeConverter
    fun toBudgetType(value: String): BudgetType {
        return try {
            BudgetType.valueOf(value)
        } catch (e: Exception) {
            BudgetType.EXPENSE
        }
    }

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return value.joinToString(",")
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return if (value.isBlank()) {
            emptyList()
        } else {
            value.split(",")
        }
    }

    @TypeConverter
    fun fromWebhookLogStatus(value: WebhookLogStatus): String = value.name

    @TypeConverter
    fun toWebhookLogStatus(value: String): WebhookLogStatus = WebhookLogStatus.fromLegacy(value)

    @TypeConverter
    fun fromWebhookDataType(value: WebhookDataType): String = value.name

    @TypeConverter
    fun toWebhookDataType(value: String): WebhookDataType =
        runCatching { WebhookDataType.valueOf(value) }.getOrElse { WebhookDataType.SUMMARY }

    @TypeConverter
    fun fromWebhookRangePreset(value: WebhookRangePreset): String = value.name

    @TypeConverter
    fun toWebhookRangePreset(value: String): WebhookRangePreset =
        runCatching { WebhookRangePreset.valueOf(value) }
            .getOrElse { WebhookRangePreset.SINCE_LAST_SUCCESS }

    @TypeConverter
    fun fromWebhookSyncReason(value: WebhookSyncReason): String = value.name

    @TypeConverter
    fun toWebhookSyncReason(value: String): WebhookSyncReason =
        runCatching { WebhookSyncReason.valueOf(value) }.getOrElse { WebhookSyncReason.MANUAL }
}