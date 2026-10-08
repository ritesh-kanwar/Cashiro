package com.ritesh.cashiro

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ritesh.cashiro.data.csv.CsvImportService
import com.ritesh.cashiro.data.csv.CsvTransactionImporter
import com.ritesh.cashiro.data.database.CashiroDatabase
import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import com.ritesh.cashiro.data.database.entity.TransactionEntity
import com.ritesh.cashiro.data.database.entity.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.StringReader
import java.math.BigDecimal
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class CsvImportPersistenceTest {
    private lateinit var db: CashiroDatabase
    private lateinit var service: CsvImportService
    private val date = LocalDateTime.of(2026, 1, 1, 12, 0)
    private val header = "Date,Time,Amount,Type,Merchant,Bank,Currency,Account,Category,Description,Balance After"
    private fun csv(vararg rows: String) = StringReader(header + "\n" + rows.joinToString("\n"))
    private fun row(
        bank: String = "Example Bank",
        currency: String = "INR",
        account: String = "1000",
        amount: String = "50.00",
        merchant: String = "Example shop"
    ) = "2026-01-01,12:00,$amount,Expense,$merchant,$bank,$currency,$account,Imported category,Imported note,9999"

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext, CashiroDatabase::class.java
        ).build()
        service = CsvImportService(db, CsvTransactionImporter())
    }

    @After fun tearDown() { db.close() }

    private suspend fun seedCuratedTransaction(): TransactionEntity {
        val original = TransactionEntity(
            amount = BigDecimal("50"), merchantName = "Example shop", category = "Curated category",
            subcategory = "Curated detail", transactionType = TransactionType.EXPENSE, dateTime = date,
            description = "Curated note", smsBody = "Synthetic debit alert", smsSender = "EXAMPLE-T",
            bankName = "Example Bank", accountNumber = "1000", balanceAfter = BigDecimal("950"),
            transactionHash = "original-sms-hash", isRecurring = true, attachments = "synthetic-attachment",
            createdAt = date.minusDays(1), updatedAt = date
        )
        val id = db.transactionDao().insertTransaction(original)
        return db.transactionDao().getTransactionById(id)!!
    }

    @Test fun repeatedImportIsIdempotentAndBankCurrencyAccountRemainPartOfIdentity() = runBlocking {
        val rows = arrayOf(row(), row(amount = "50"), row(bank = "Other Bank"), row(currency = "USD"), row(account = "2000"))
        val first = service.importCsv(csv(*rows))
        assertEquals(4, first.importedCount)
        assertEquals(1, first.duplicateCount)
        assertEquals(0, first.failedCount)
        val persisted = db.transactionDao().getAllTransactions().first().sortedBy { it.id }
        assertEquals(4, persisted.size)
        assertEquals(4, persisted.map { Triple(it.bankName, it.currency, it.accountNumber) }.toSet().size)
        val second = service.importCsv(csv(*rows))
        assertEquals(0, second.importedCount)
        assertEquals(5, second.duplicateCount)
        assertEquals(0, second.failedCount)
        assertEquals(persisted, db.transactionDao().getAllTransactions().first().sortedBy { it.id })
    }

    @Test fun existingSmsMetadataAndBalanceAnchorsSurviveDuplicateAndNewImports() = runBlocking {
        val original = seedCuratedTransaction()
        db.accountBalanceDao().insertBalance(AccountBalanceEntity(
            bankName = "Example Bank", accountLast4 = "1000", balance = BigDecimal("950"),
            timestamp = date, sourceType = "SMS_BALANCE", transactionId = original.id,
            smsSource = "Synthetic balance anchor", createdAt = date
        ))
        val balancesBefore = db.accountBalanceDao().getAllBalances().first()
        val result = service.importCsv(csv(row(), row(merchant = "Other example shop")))
        assertEquals(1, result.importedCount)
        assertEquals(1, result.duplicateCount)
        assertEquals(0, result.failedCount)
        assertEquals(original, db.transactionDao().getTransactionById(original.id))
        assertEquals(balancesBefore, db.accountBalanceDao().getAllBalances().first())
        assertEquals(2, db.transactionDao().getAllTransactions().first().size)
        assertEquals(2, service.importCsv(csv(row(), row(merchant = "Other example shop"))).duplicateCount)
        assertEquals(original, db.transactionDao().getTransactionById(original.id))
        assertEquals(balancesBefore, db.accountBalanceDao().getAllBalances().first())
    }

    @Test fun deletedSmsWithOriginalHashRemainsDeletedWhenItsExportIsImported() = runBlocking {
        val deleted = seedCuratedTransaction().copy(isDeleted = true)
        db.transactionDao().updateTransaction(deleted)
        val first = service.importCsv(csv(row()))
        assertEquals(0, first.importedCount)
        assertEquals(1, first.duplicateCount)
        assertEquals(0, first.failedCount)
        assertEquals(deleted, db.transactionDao().getTransactionById(deleted.id))
        assertTrue(db.transactionDao().getAllTransactions().first().isEmpty())
        val repeated = service.importCsv(csv(row()))
        assertEquals(0, repeated.importedCount)
        assertEquals(1, repeated.duplicateCount)
        assertEquals(deleted, db.transactionDao().getTransactionByHash("original-sms-hash"))
        db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM transactions").use {
            assertTrue(it.moveToFirst())
            assertEquals(1, it.getInt(0))
        }
    }

    @Test fun databaseFailureRollsBackEarlierRowsInTheSameImport() = runBlocking {
        val original = seedCuratedTransaction()
        // Abort the second insert after the first insert has succeeded inside the transaction.
        db.openHelper.writableDatabase.execSQL("""
            CREATE TRIGGER reject_example_csv BEFORE INSERT ON transactions
            WHEN NEW.merchant_name = 'Example rejected'
            BEGIN SELECT RAISE(ABORT, 'Synthetic import failure'); END
        """.trimIndent())
        var failure: Exception? = null
        try {
            service.importCsv(csv(row(merchant = "Example accepted"), row(merchant = "Example rejected")))
        } catch (error: Exception) {
            failure = error
        }
        assertNotNull("The injected database failure must reach the caller", failure)
        assertEquals(listOf(original), db.transactionDao().getAllTransactions().first())
        db.openHelper.writableDatabase.execSQL("DROP TRIGGER reject_example_csv")
        assertEquals(2, service.importCsv(csv(row(merchant = "Example accepted"), row(merchant = "Example rejected"))).importedCount)
    }

    @Test fun malformedFileCannotPersistItsValidPrefix() = runBlocking {
        val original = seedCuratedTransaction()
        val result = service.importCsv(StringReader(
            header + "\n" + row(merchant = "Example valid prefix") + "\n2026-01-02,12:00,50,Expense,\"unfinished"
        ))
        assertEquals(0, result.importedCount)
        assertTrue(result.failedCount > 0)
        assertEquals(listOf(original), db.transactionDao().getAllTransactions().first())
    }
}
