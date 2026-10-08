package com.ritesh.cashiro

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ritesh.cashiro.data.database.CashiroDatabase
import com.ritesh.cashiro.data.database.entity.*
import com.ritesh.cashiro.data.manager.SubscriptionPaymentReconciliation
import com.ritesh.cashiro.data.preferences.BankAccountMergeStore
import com.ritesh.cashiro.data.repository.AccountBalanceRepository
import com.ritesh.cashiro.data.repository.SubscriptionRepository
import com.ritesh.cashiro.domain.usecase.MarkSubscriptionPaidUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class SubscriptionPaymentTest {
    private lateinit var db: CashiroDatabase
    private lateinit var balances: AccountBalanceRepository
    private lateinit var markPaid: MarkSubscriptionPaidUseCase
    private val today = LocalDate.now()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(context, CashiroDatabase::class.java).build()
        balances = AccountBalanceRepository(db.accountBalanceDao(), context, BankAccountMergeStore(context))
        markPaid = MarkSubscriptionPaidUseCase(db, balances)
    }
    @After fun tearDown() { db.close() }

    private suspend fun seed(): SubscriptionEntity {
        for ((currency, value) in listOf("INR" to "1000", "USD" to "500")) {
            db.accountBalanceDao().insertBalance(AccountBalanceEntity(
                bankName = "Example Bank", accountLast4 = "1000", currency = currency,
                balance = BigDecimal(value), timestamp = today.minusDays(1).atStartOfDay(), sourceType = "MANUAL"
            ))
        }
        val row = SubscriptionEntity(merchantName = "Example service", amount = BigDecimal("100"),
            nextPaymentDate = today, bankName = "Example Bank", accountLast4 = "1000",
            category = "Example category", subcategory = "Example detail", billingCycle = "Monthly")
        return row.copy(id = db.subscriptionDao().insertSubscription(row))
    }

    private fun incoming(bank: String = "Example Bank", currency: String = "INR") = TransactionEntity(
        amount = BigDecimal("100.00"), merchantName = "Example service", category = "SMS category",
        transactionType = TransactionType.EXPENSE, dateTime = today.atTime(12, 0), bankName = bank,
        accountNumber = "1000", currency = currency, transactionHash = "synthetic-sms-$bank-$currency",
        smsBody = "Synthetic subscription debit", smsSender = "EXAMPLE-T", balanceAfter = BigDecimal("900")
    )

    private suspend fun assertBalance(value: String, currency: String = "INR") {
        assertEquals(0, BigDecimal(value).compareTo(balances.getLatestBalance("Example Bank", "1000", currency)!!.balance))
    }

    @Test fun concurrentAndRefreshedRetapsCreateOnePaymentAndDebitOnlyItsCurrency() = runBlocking {
        val sub = seed()
        val results = listOf(async { markPaid.execute(sub.id, sub.nextPaymentDate) },
            async { markPaid.execute(sub.id, sub.nextPaymentDate) }).awaitAll()
        assertEquals(1, results.count { it is MarkSubscriptionPaidUseCase.Result.Paid })
        assertEquals(1, results.count { it is MarkSubscriptionPaidUseCase.Result.AlreadyPaid })
        assertBalance("900")
        assertBalance("500", "USD")
        val current = db.subscriptionDao().getSubscriptionById(sub.id)!!
        assertEquals(today.plusMonths(1), current.nextPaymentDate)
        assertEquals(today, current.lastPaidDate)
        assertEquals(MarkSubscriptionPaidUseCase.Result.AlreadyPaid, markPaid.execute(sub.id, current.nextPaymentDate))
        val row = db.transactionDao().getAllTransactions().first().single()
        assertEquals("1000", row.accountNumber)
        assertEquals("Example detail", row.subcategory)
        assertEquals(row.id, db.subscriptionPaymentDao().forCycle(sub.id, today)!!.transactionId)
    }

    @Test fun linkingExistingSmsDoesNotChangeItsMetadataOrDebitAgain() = runBlocking {
        val sub = seed()
        val smsId = db.transactionDao().insertTransaction(incoming().copy(description = "Curated note"))
        db.transactionDao().insertTransaction(incoming(bank = "Other Bank"))
        db.transactionDao().insertTransaction(incoming(currency = "USD"))
        val original = db.transactionDao().getTransactionById(smsId)!!
        val before = db.accountBalanceDao().getAllBalances().first()
        val choices = markPaid.execute(sub.id, sub.nextPaymentDate) as MarkSubscriptionPaidUseCase.Result.ChooseExisting
        assertEquals(listOf(smsId), choices.transactions.map { it.id })
        val paid = markPaid.execute(sub.id, sub.nextPaymentDate, linkedTransactionId = smsId) as MarkSubscriptionPaidUseCase.Result.Paid
        assertTrue(paid.linked)
        assertEquals(original, db.transactionDao().getTransactionById(smsId))
        assertEquals(before, db.accountBalanceDao().getAllBalances().first())
        assertEquals(smsId, db.subscriptionPaymentDao().forCycle(sub.id, today)!!.transactionId)
    }

    @Test fun laterSmsReconcilesManualPaymentOnceAndPreservesCuratedFields() = runBlocking {
        val sub = seed()
        val paid = markPaid.execute(sub.id, sub.nextPaymentDate) as MarkSubscriptionPaidUseCase.Result.Paid
        assertNull(SubscriptionPaymentReconciliation.reconcile(db, balances, incoming(currency = "USD")))
        assertNull(SubscriptionPaymentReconciliation.reconcile(db, balances, incoming(bank = "Other Bank")))
        assertEquals(paid.transactionId, SubscriptionPaymentReconciliation.reconcile(db, balances, incoming()))
        val row = db.transactionDao().getAllTransactions().first().single()
        assertEquals(paid.transactionId, row.id)
        assertEquals("Example category", row.category)
        assertEquals("Example detail", row.subcategory)
        assertEquals("Subscription payment", row.description)
        assertEquals("Synthetic subscription debit", row.smsBody)
        assertBalance("900")
        assertBalance("500", "USD")
        val before = db.accountBalanceDao().getAllBalances().first()
        assertEquals(paid.transactionId, SubscriptionPaymentReconciliation.reconcile(db, balances, incoming()))
        assertEquals(before, db.accountBalanceDao().getAllBalances().first())
        assertEquals(1, db.transactionDao().getAllTransactions().first().size)
        db.transactionDao().deleteRebuildableSmsTransactions()
        assertNotNull(db.transactionDao().getTransactionById(paid.transactionId))
    }

    @Test fun latePaymentDoesNotDelayTheNextScheduledCycle() = runBlocking {
        val sub = seed().copy(nextPaymentDate = today.minusDays(7))
        db.subscriptionDao().updateSubscription(sub)
        assertTrue(markPaid.execute(sub.id, sub.nextPaymentDate) is MarkSubscriptionPaidUseCase.Result.Paid)
        val current = db.subscriptionDao().getSubscriptionById(sub.id)!!
        assertEquals(sub.nextPaymentDate!!.plusMonths(1), current.nextPaymentDate)
        assertTrue(markPaid.execute(sub.id, current.nextPaymentDate,
            paymentDate = current.nextPaymentDate!!) is MarkSubscriptionPaidUseCase.Result.Paid)
        assertEquals(2, db.transactionDao().getAllTransactions().first().size)
    }

    @Test fun linkingManualExpenseStillAllowsLaterSmsConfirmation() = runBlocking {
        val sub = seed()
        val manualId = db.transactionDao().insertTransaction(incoming().copy(
            transactionHash = "synthetic-manual", smsBody = null, smsSender = null, balanceAfter = null
        ))
        balances.insertTransactionBalance("Example Bank", "1000", BigDecimal("100"), TransactionType.EXPENSE,
            null, today.atTime(12, 0), manualId, null, false, null, "INR")
        val linked = markPaid.execute(sub.id, sub.nextPaymentDate, linkedTransactionId = manualId) as MarkSubscriptionPaidUseCase.Result.Paid
        assertTrue(linked.linked)
        assertNull(db.subscriptionPaymentDao().forTransaction(manualId)!!.smsHash)
        assertEquals(manualId, SubscriptionPaymentReconciliation.reconcile(db, balances, incoming()))
        assertEquals(1, db.transactionDao().getAllTransactions().first().size)
        assertBalance("900")
    }

    @Test fun mandateRefreshPreservesManualFundingChoiceAndPaymentLedger() = runBlocking {
        val sub = seed()
        val paid = markPaid.execute(sub.id, sub.nextPaymentDate) as MarkSubscriptionPaidUseCase.Result.Paid
        val mandate = object : com.ritesh.parser.core.MandateInfo {
            override val amount = BigDecimal("100")
            override val merchant = "Example service"
            override val nextDeductionDate = today.plusMonths(1).toString()
            override val dateFormat = "yyyy-MM-dd"
            override val umn: String? = null
            override val accountLast4 = "2000"
        }
        val repository = SubscriptionRepository(db.subscriptionDao())
        assertEquals(sub.id, repository.createOrUpdateFromMandate(mandate, "Example Bank"))
        assertEquals("1000", db.subscriptionDao().getSubscriptionById(sub.id)!!.accountLast4)
        assertEquals(paid.transactionId, db.subscriptionPaymentDao().forCycle(sub.id, today)!!.transactionId)
        val other = repository.createOrUpdateFromMandate(mandate, "Other Bank")
        assertNotEquals(sub.id, other)
        assertEquals("2000", db.subscriptionDao().getSubscriptionById(other)!!.accountLast4)
    }

    @Test fun scheduleWriteFailureRollsBackTransactionBalanceAndLedger() = runBlocking {
        val sub = seed()
        val before = db.accountBalanceDao().getAllBalances().first()
        db.openHelper.writableDatabase.execSQL("""CREATE TRIGGER reject_subscription_payment
            BEFORE UPDATE ON subscriptions BEGIN SELECT RAISE(ABORT, 'Synthetic schedule failure'); END""")
        var failure: Exception? = null
        try { markPaid.execute(sub.id, sub.nextPaymentDate) } catch (error: Exception) { failure = error }
        assertNotNull(failure)
        assertTrue(db.transactionDao().getAllTransactions().first().isEmpty())
        assertNull(db.subscriptionPaymentDao().forCycle(sub.id, today))
        assertEquals(before, db.accountBalanceDao().getAllBalances().first())
        assertEquals(sub, db.subscriptionDao().getSubscriptionById(sub.id))
        db.openHelper.writableDatabase.execSQL("DROP TRIGGER reject_subscription_payment")
        assertTrue(markPaid.execute(sub.id, sub.nextPaymentDate) is MarkSubscriptionPaidUseCase.Result.Paid)
        assertBalance("900")
    }

    @Test fun deletedManualPaymentCannotBeResurrectedByLaterSms() = runBlocking {
        val sub = seed()
        val paid = markPaid.execute(sub.id, sub.nextPaymentDate) as MarkSubscriptionPaidUseCase.Result.Paid
        val deleted = db.transactionDao().getTransactionById(paid.transactionId)!!.copy(isDeleted = true)
        db.transactionDao().updateTransaction(deleted)
        val before = db.accountBalanceDao().getAllBalances().first()
        assertEquals(paid.transactionId, SubscriptionPaymentReconciliation.reconcile(db, balances, incoming()))
        assertEquals(deleted, db.transactionDao().getTransactionById(paid.transactionId))
        assertTrue(db.transactionDao().getAllTransactions().first().isEmpty())
        assertEquals(before, db.accountBalanceDao().getAllBalances().first())
    }
}
