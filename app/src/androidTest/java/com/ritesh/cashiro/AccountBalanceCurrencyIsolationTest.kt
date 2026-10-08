package com.ritesh.cashiro

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.ritesh.cashiro.data.database.CashiroDatabase
import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import com.ritesh.cashiro.data.database.entity.TransactionEntity
import com.ritesh.cashiro.data.database.entity.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime

class AccountBalanceCurrencyIsolationTest {
    private val start = LocalDateTime.of(2026, 1, 1, 12, 0)
    private val bank = "Example Bank"
    private val suffix = "1000"

    private suspend fun withDatabase(block: suspend (CashiroDatabase) -> Unit) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, CashiroDatabase::class.java).build()
        try { block(db) } finally { db.close() }
    }

    private suspend fun seed(db: CashiroDatabase, currency: String, value: String, minute: Long = 0,
                             source: String = "MANUAL", credit: Boolean = false): Long =
        db.accountBalanceDao().insertBalance(AccountBalanceEntity(
            bankName = bank, accountLast4 = suffix, currency = currency,
            balance = BigDecimal(value), timestamp = start.plusMinutes(minute),
            sourceType = source, isCreditCard = credit
        ))

    private suspend fun expense(db: CashiroDatabase, currency: String, amount: String, minute: Long): Long {
        val timestamp = start.plusMinutes(minute)
        val id = db.transactionDao().insertTransaction(TransactionEntity(
            amount = BigDecimal(amount), merchantName = "Example Shop", category = "Shopping",
            transactionType = TransactionType.EXPENSE, dateTime = timestamp,
            bankName = bank, accountNumber = suffix, currency = currency,
            transactionHash = "synthetic_${currency}_$minute"
        ))
        db.accountBalanceDao().insertTransactionBalance(bank, suffix, BigDecimal(amount),
            TransactionType.EXPENSE, null, timestamp, id, null, false, null, currency)
        return id
    }

    private suspend fun assertLatest(db: CashiroDatabase, currency: String, expected: String) {
        val latest = db.accountBalanceDao().getLatestBalanceForCurrency(bank, suffix, currency)
        assertNotNull(latest)
        assertEquals(0, latest!!.balance.compareTo(BigDecimal(expected)))
    }

    @Test fun currenciesCanCoexistAtIdenticalTimestampAndBothRemainSelectable() = runBlocking {
        withDatabase { db ->
            seed(db, "INR", "1000")
            seed(db, "USD", "200")
            assertLatest(db, "INR", "1000")
            assertLatest(db, "USD", "200")
            assertEquals(setOf("INR", "USD"), db.accountBalanceDao().getAllLatestBalances().first().map { it.currency }.toSet())
            assertEquals(2, db.accountBalanceDao().getAccountCount().first())
        }
    }

    @Test fun backdatedInsertCascadesOnlyWithinCurrencyAndIgnoresForeignAnchor() = runBlocking {
        withDatabase { db ->
            seed(db, "INR", "1000")
            seed(db, "USD", "200", 2, "SMS_BALANCE", credit = true)
            expense(db, "INR", "100", 4)
            expense(db, "INR", "50", 1)
            assertLatest(db, "INR", "850")
            assertLatest(db, "USD", "200")
            assertFalse(db.accountBalanceDao().getLatestBalanceForCurrency(bank, suffix, "INR")!!.isCreditCard)
        }
    }

    @Test fun manualFallbackComesFromSameCurrency() = runBlocking {
        withDatabase { db ->
            seed(db, "USD", "200", -1, credit = true)
            seed(db, "INR", "1000", 2)
            expense(db, "INR", "50", 0)
            assertLatest(db, "INR", "950")
            assertLatest(db, "USD", "200")
            assertFalse(db.accountBalanceDao().getLatestBalanceForCurrency(bank, suffix, "INR")!!.isCreditCard)
        }
    }

    @Test fun duplicateRemovalRecalculatesSameCurrencyPastForeignAnchor() = runBlocking {
        withDatabase { db ->
            seed(db, "INR", "1000")
            val duplicate = expense(db, "INR", "50", 1)
            seed(db, "USD", "200", 2, "SMS_BALANCE")
            expense(db, "INR", "100", 3)
            db.accountBalanceDao().deleteTransactionBalancesAndRecalculate(duplicate)
            assertLatest(db, "INR", "900")
            assertLatest(db, "USD", "200")
        }
    }
}
