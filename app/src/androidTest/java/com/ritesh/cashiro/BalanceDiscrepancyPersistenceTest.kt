package com.ritesh.cashiro

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ritesh.cashiro.data.database.CashiroDatabase
import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import com.ritesh.cashiro.data.database.entity.TransactionEntity
import com.ritesh.cashiro.data.database.entity.TransactionType
import com.ritesh.cashiro.domain.usecase.AddBalanceAdjustmentUseCase
import com.ritesh.cashiro.domain.usecase.DetectBalanceDiscrepancyUseCase
import com.ritesh.cashiro.utils.BalanceDiscrepancy
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class BalanceDiscrepancyPersistenceTest {
    private lateinit var db: CashiroDatabase
    private lateinit var detector: DetectBalanceDiscrepancyUseCase
    private lateinit var adjust: AddBalanceAdjustmentUseCase
    private val at = LocalDateTime.of(2026, 1, 1, 12, 0)

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, CashiroDatabase::class.java).build()
        detector = DetectBalanceDiscrepancyUseCase(db)
        adjust = AddBalanceAdjustmentUseCase(db, detector)
    }
    @After fun tearDown() { db.close() }

    private suspend fun seed(gapSeconds: Long = 3600): TransactionEntity {
        val row = TransactionEntity(amount = BigDecimal("100"), merchantName = "Example Shop", category = "Others",
            transactionType = TransactionType.EXPENSE, dateTime = at, bankName = "Example Bank", accountNumber = "1000",
            balanceAfter = BigDecimal("850"), transactionHash = "synthetic-report")
        val id = db.transactionDao().insertTransaction(row)
        db.accountBalanceDao().insertBalance(AccountBalanceEntity(bankName = "Example Bank", accountLast4 = "1000",
            balance = BigDecimal("1000"), timestamp = at.minusSeconds(gapSeconds), sourceType = "SMS_BALANCE"))
        db.accountBalanceDao().insertBalance(AccountBalanceEntity(bankName = "Example Bank", accountLast4 = "1000",
            balance = BigDecimal("850"), timestamp = at, sourceType = "TRANSACTION_SMS_BALANCE", transactionId = id))
        return row.copy(id = id)
    }

    @Test fun concurrentAdjustmentIsSingleAndPreservesExplicitAnchors() = runBlocking {
        val tx = seed()
        val displayed = detector.execute(tx)!!
        val anchors = db.accountBalanceDao().getAllBalances().first()
        val results = coroutineScope { listOf(async { adjust.execute(displayed) }, async { adjust.execute(displayed) }).map { it.await() } }
        assertEquals(1, results.count { it == AddBalanceAdjustmentUseCase.Result.ADDED })
        assertEquals(1, results.count { it == AddBalanceAdjustmentUseCase.Result.ALREADY_RECORDED })
        assertEquals(anchors, db.accountBalanceDao().getAllBalances().first())
        assertNull(detector.execute(tx))
        assertEquals(2, db.transactionDao().getAllTransactions().first().size)
    }

    @Test fun staleProposalCannotWriteAndDeletedAdjustmentIsNotRestored() = runBlocking {
        val tx = seed()
        val displayed = detector.execute(tx)!!
        db.transactionDao().updateTransaction(tx.copy(balanceAfter = BigDecimal("840")))
        assertEquals(AddBalanceAdjustmentUseCase.Result.CHANGED, adjust.execute(displayed))
        assertEquals(1, db.transactionDao().getAllTransactions().first().size)
        val fresh = detector.execute(tx)!!
        assertEquals(AddBalanceAdjustmentUseCase.Result.ADDED, adjust.execute(fresh))
        val adjustment = db.transactionDao().getTransactionByHash(BalanceDiscrepancy.adjustmentHash(tx.id))!!
        db.transactionDao().softDeleteTransaction(adjustment.id)
        assertEquals(AddBalanceAdjustmentUseCase.Result.ALREADY_RECORDED, adjust.execute(fresh))
        assertTrue(db.transactionDao().getTransactionById(adjustment.id)!!.isDeleted)
    }

    @Test fun deletingAndUndoingAdjustmentPreservesBankBalanceSnapshots() = runBlocking {
        val tx = seed()
        adjust.execute(detector.execute(tx)!!)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val balances = com.ritesh.cashiro.data.repository.AccountBalanceRepository(
            db.accountBalanceDao(), context, com.ritesh.cashiro.data.preferences.BankAccountMergeStore(context))
        val repository = com.ritesh.cashiro.data.repository.TransactionRepository(db.transactionDao(), balances)
        val adjustment = db.transactionDao().getTransactionByHash(BalanceDiscrepancy.adjustmentHash(tx.id))!!
        val anchors = db.accountBalanceDao().getAllBalances().first()
        repository.deleteTransaction(adjustment)
        assertEquals(anchors, db.accountBalanceDao().getAllBalances().first())
        repository.undoDeleteTransaction(adjustment)
        assertEquals(anchors, db.accountBalanceDao().getAllBalances().first())
        assertNull(detector.execute(tx))
    }

    @Test fun tightSnapshotWindowPlacesAdjustmentAfterAnchor() = runBlocking {
        val tx = seed(gapSeconds = 1)
        val displayed = detector.execute(tx)!!
        adjust.execute(displayed)
        val adjustment = db.transactionDao().getTransactionByHash(BalanceDiscrepancy.adjustmentHash(tx.id))!!
        assertEquals(tx.dateTime, adjustment.dateTime)
        assertNull(detector.execute(tx))
    }
}
