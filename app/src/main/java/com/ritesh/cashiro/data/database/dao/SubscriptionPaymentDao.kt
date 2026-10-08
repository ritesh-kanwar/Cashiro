package com.ritesh.cashiro.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.ritesh.cashiro.data.database.entity.SubscriptionPaymentEntity
import com.ritesh.cashiro.data.database.entity.TransactionEntity
import java.time.LocalDate
import java.time.LocalDateTime

@Dao
interface SubscriptionPaymentDao {
    @Query("SELECT * FROM subscription_payments ORDER BY subscription_id, scheduled_date")
    suspend fun getAll(): List<SubscriptionPaymentEntity>

    @Insert
    suspend fun insert(payment: SubscriptionPaymentEntity)

    @Query("SELECT * FROM subscription_payments WHERE subscription_id = :subscriptionId AND scheduled_date = :scheduledDate")
    suspend fun forCycle(subscriptionId: Long, scheduledDate: LocalDate): SubscriptionPaymentEntity?

    @Query("SELECT * FROM subscription_payments WHERE subscription_id = :subscriptionId AND marked_date = :paymentDate LIMIT 1")
    suspend fun paidOn(subscriptionId: Long, paymentDate: LocalDate): SubscriptionPaymentEntity?

    @Query("SELECT * FROM subscription_payments WHERE subscription_id = :subscriptionId ORDER BY marked_date DESC LIMIT 1")
    suspend fun latest(subscriptionId: Long): SubscriptionPaymentEntity?

    @Query("SELECT * FROM subscription_payments WHERE transaction_id = :transactionId LIMIT 1")
    suspend fun forTransaction(transactionId: Long): SubscriptionPaymentEntity?

    @Query("SELECT * FROM subscription_payments WHERE sms_hash = :hash LIMIT 1")
    suspend fun forSmsHash(hash: String): SubscriptionPaymentEntity?

    @Query("UPDATE subscription_payments SET sms_hash = :hash WHERE transaction_id = :transactionId")
    suspend fun confirmSms(transactionId: Long, hash: String)

    @Query("""SELECT t.* FROM transactions t
        WHERE t.is_deleted = 0 AND t.transaction_type IN ('EXPENSE', 'CREDIT')
        AND t.bank_name = :bankName COLLATE NOCASE AND t.account_number = :accountLast4
        AND t.currency = :currency COLLATE NOCASE AND t.date_time >= :start AND t.date_time < :end
        AND NOT EXISTS (SELECT 1 FROM subscription_payments p WHERE p.transaction_id = t.id)
        ORDER BY t.date_time DESC""")
    suspend fun linkCandidates(bankName: String, accountLast4: String, currency: String,
        start: LocalDateTime, end: LocalDateTime): List<TransactionEntity>

    @Query("""SELECT t.* FROM transactions t JOIN subscription_payments p ON p.transaction_id = t.id
        WHERE p.sms_hash IS NULL AND p.payment_date = :paymentDate
        AND t.bank_name = :bankName COLLATE NOCASE AND t.account_number = :accountLast4
        AND t.currency = :currency COLLATE NOCASE AND t.transaction_type IN ('EXPENSE', 'CREDIT')""")
    suspend fun awaitingSms(bankName: String, accountLast4: String, currency: String,
        paymentDate: LocalDate): List<TransactionEntity>
}
