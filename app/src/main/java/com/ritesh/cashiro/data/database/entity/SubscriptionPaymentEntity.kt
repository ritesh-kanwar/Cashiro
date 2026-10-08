package com.ritesh.cashiro.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import java.time.LocalDate

/** One acknowledged payment per scheduled cycle; a transaction can only pay one cycle. */
@Entity(
    tableName = "subscription_payments",
    primaryKeys = ["subscription_id", "scheduled_date"],
    foreignKeys = [ForeignKey(
        entity = SubscriptionEntity::class, parentColumns = ["id"],
        childColumns = ["subscription_id"], onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["transaction_id"], unique = true), Index(value = ["sms_hash"], unique = true)]
)
data class SubscriptionPaymentEntity(
    @ColumnInfo(name = "subscription_id") val subscriptionId: Long,
    @ColumnInfo(name = "scheduled_date") val scheduledDate: LocalDate,
    @ColumnInfo(name = "payment_date") val paymentDate: LocalDate,
    @ColumnInfo(name = "transaction_id") val transactionId: Long,
    @ColumnInfo(name = "sms_hash") val smsHash: String? = null,
    @ColumnInfo(name = "marked_date") val markedDate: LocalDate = paymentDate
)
