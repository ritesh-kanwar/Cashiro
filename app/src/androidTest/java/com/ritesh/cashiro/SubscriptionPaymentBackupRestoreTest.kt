package com.ritesh.cashiro

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ritesh.cashiro.data.backup.SubscriptionPaymentBackup
import com.ritesh.cashiro.data.database.CashiroDatabase
import com.ritesh.cashiro.data.database.entity.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class SubscriptionPaymentBackupRestoreTest {
    @Test fun restoresRemappedIdsAndSkipsConflictsOrMissingReferences() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, CashiroDatabase::class.java).build()
        val day = LocalDate.of(2026, 1, 1)
        try {
            val subscriptionId = db.subscriptionDao().insertSubscription(SubscriptionEntity(id = 101,
                merchantName = "Example service", amount = BigDecimal("100"), nextPaymentDate = day.plusMonths(1)))
            fun transaction(id: Long) = TransactionEntity(id = id, amount = BigDecimal("100"), merchantName = "Example service",
                category = "Subscriptions", transactionType = TransactionType.EXPENSE, dateTime = day.atStartOfDay(),
                transactionHash = "synthetic-restore-$id", isDeleted = true)
            val transactionId = db.transactionDao().insertTransaction(transaction(201))
            val anotherTransaction = db.transactionDao().insertTransaction(transaction(202))
            val payment = SubscriptionPaymentEntity(1, day, day, 2, "synthetic-replay-hash", day.plusDays(1))
            val subscriptions = mapOf(1L to subscriptionId)
            val transactions = mapOf(2L to transactionId, 3L to anotherTransaction)
            SubscriptionPaymentBackup.restore(db, listOf(payment), subscriptions, transactions)
            val restored = db.subscriptionPaymentDao().forCycle(subscriptionId, day)!!
            assertEquals(transactionId, restored.transactionId)
            assertEquals(day.plusDays(1), restored.markedDate)
            assertEquals("synthetic-replay-hash", restored.smsHash)
            val conflicts = listOf(payment, payment.copy(transactionId = 3),
                payment.copy(scheduledDate = day.plusDays(1)),
                payment.copy(scheduledDate = day.plusDays(2), transactionId = 3),
                payment.copy(subscriptionId = 99, scheduledDate = day.plusDays(3)),
                payment.copy(transactionId = 99, scheduledDate = day.plusDays(4)))
            SubscriptionPaymentBackup.restore(db, conflicts, subscriptions, transactions)
            assertEquals(listOf(restored), db.subscriptionPaymentDao().getAll())
            assertNotNull(db.transactionDao().getTransactionById(transactionId))
        } finally { db.close() }
    }
}
