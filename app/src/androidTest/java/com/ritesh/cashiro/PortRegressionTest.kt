package com.ritesh.cashiro

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ritesh.cashiro.data.database.CashiroDatabase
import com.ritesh.cashiro.data.database.entity.*
import com.ritesh.cashiro.data.preferences.BankAccountMergeStore
import com.ritesh.cashiro.data.backup.AppPreferences
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class PortRegressionTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun transaction(bank: String = "Example Bank", suffix: String = "000", hash: String = "example") =
        TransactionEntity(amount = BigDecimal("100"), merchantName = "Example Shop", category = "Transfer", transactionType = TransactionType.TRANSFER,
            dateTime = LocalDateTime.of(2026, 1, 1, 12, 0), bankName = bank, accountNumber = suffix, transactionHash = hash,
            fromAccount = "2000", fromBankName = "Other Bank", toAccount = suffix, toBankName = bank)
    private fun balance(suffix: String, source: String, transactionId: Long? = null) =
        AccountBalanceEntity(bankName = "State Bank of India", accountLast4 = suffix, balance = BigDecimal("900"), timestamp = LocalDateTime.of(2026, 1, 1, 12, 0), sourceType = source, transactionId = transactionId)

    @Test fun freshDatabaseAndBankQualifiedTransferRepair() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, CashiroDatabase::class.java).build()
        try {
            val dao = db.transactionDao()
            val owned = dao.insertTransaction(transaction())
            val unrelated = dao.insertTransaction(transaction(bank = "Other Bank", hash = "other"))
            val ids = dao.getAccountTransferLegRefIds("Example Bank", "000")
            assertEquals(listOf(owned), ids)
            dao.retargetTransferLegRefs(ids, "Example Bank", "Example Bank", "000", "1000", LocalDateTime.now())
            assertEquals("1000", dao.getTransactionById(owned)?.toAccount)
            assertEquals("000", dao.getTransactionById(unrelated)?.toAccount)
            assertEquals("Other Bank", dao.getTransactionById(owned)?.fromBankName)
        } finally { db.close() }
    }

    @Test fun upgradeFrom62PreservesRowsAndAddsNullableBankFields() = verifyUpgrade(62)
    @Test fun upgradeFrom61UsesSharedManualAndAutomaticMigrationPath() = verifyUpgrade(61)

    private fun verifyUpgrade(version: Int) = runBlocking {
        val name = "synthetic-port-migration-$version.db"
        context.deleteDatabase(name)
        val json = InstrumentationRegistry.getInstrumentation().context.assets
            .open("com.ritesh.cashiro.data.database.CashiroDatabase/$version.json").bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        context.openOrCreateDatabase(name, 0, null).use { sql ->
            val entities = json.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                val table = entity.getString("tableName")
                sql.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = entity.optJSONArray("indices") ?: org.json.JSONArray()
                for (j in 0 until indices.length()) sql.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
            }
            val setup = json.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) sql.execSQL(setup.getString(i))
            sql.execSQL("""INSERT INTO transactions (id, amount, merchant_name, category, transaction_type, date_time, transaction_hash, is_recurring, is_deleted, created_at, updated_at, currency, attachments, is_sample)
                VALUES (1, '100', 'Example Shop', 'Food', 'EXPENSE', '2026-01-01T12:00', 'existing', 0, 0, '2026-01-01T12:00', '2026-01-01T12:00', 'INR', '', 0)""")
            sql.execSQL("UPDATE transactions SET transaction_type = 'TRANSFER', bank_name = 'Example Bank', account_number = '1000', from_account = '1000', to_account = '3000' WHERE id = 1")
            for ((bank, suffix) in listOf("Other Bank" to "3000", "Example Bank" to "2000", "Other Bank" to "2000")) {
                sql.execSQL("INSERT INTO account_balances (bank_name, account_last4, balance, timestamp, created_at, currency, source_type) VALUES (?, ?, '1000', '2026-01-01T12:00', '2026-01-01T12:00', 'INR', 'MANUAL')", arrayOf(bank, suffix))
            }
            sql.execSQL("INSERT INTO transactions (id, amount, merchant_name, category, transaction_type, date_time, transaction_hash, is_recurring, is_deleted, created_at, updated_at, currency, attachments, is_sample, bank_name, account_number, from_account, to_account) SELECT 2, amount, merchant_name, category, transaction_type, date_time, 'ambiguous', is_recurring, is_deleted, created_at, updated_at, currency, attachments, is_sample, 'Example Bank', '2000', '2000', '2000' FROM transactions WHERE id = 1")
            sql.execSQL("INSERT INTO transactions (id, amount, merchant_name, category, transaction_type, date_time, transaction_hash, is_recurring, is_deleted, created_at, updated_at, currency, attachments, is_sample, bank_name, account_number, from_account, to_account) SELECT 3, amount, merchant_name, category, transaction_type, date_time, 'no-bank-evidence', is_recurring, is_deleted, created_at, updated_at, currency, attachments, is_sample, bank_name, account_number, from_account, '4000' FROM transactions WHERE id = 1")
            sql.version = version
        }
        val db = Room.databaseBuilder(context, CashiroDatabase::class.java, name).addMigrations(*CashiroDatabase.ALL_MIGRATIONS).build()
        try {
            assertEquals("Example Shop", db.transactionDao().getTransactionById(1)?.merchantName)
            assertEquals("Example Bank", db.transactionDao().getTransactionById(1)?.fromBankName)
            assertEquals("Other Bank", db.transactionDao().getTransactionById(1)?.toBankName)
            assertNull(db.transactionDao().getTransactionById(2)?.fromBankName)
            assertNull(db.transactionDao().getTransactionById(2)?.toBankName)
            assertEquals("Example Bank", db.transactionDao().getTransactionById(3)?.fromBankName)
            assertNull(db.transactionDao().getTransactionById(3)?.toBankName)
            val rowId = db.transactionDao().insertTransaction(transaction().copy(fromBankName = null, toBankName = null))
            assertNull(db.transactionDao().getTransactionById(rowId)?.toBankName)
            assertEquals(64, db.openHelper.readableDatabase.version)
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun phantomCleanupPreservesManualAndSoftDeletedHistory() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, CashiroDatabase::class.java).build()
        try {
            val deletedId = db.transactionDao().insertTransaction(transaction(bank = "State Bank of India", suffix = "3000").copy(isDeleted = true))
            db.accountBalanceDao().insertBalance(balance("1000", "TRANSACTION_CALCULATED"))
            db.accountBalanceDao().insertBalance(balance("2000", "MANUAL"))
            db.accountBalanceDao().insertBalance(balance("3000", "TRANSACTION", deletedId))
            db.accountBalanceDao().insertBalance(balance("4000", "SMS_BALANCE"))
            assertEquals(1, db.accountBalanceDao().deletePhantomGPayAccounts())
            assertNotNull(db.accountBalanceDao().getLatestBalance("State Bank of India", "2000"))
            assertNotNull(db.accountBalanceDao().getLatestBalance("State Bank of India", "3000"))
            assertNotNull(db.accountBalanceDao().getLatestBalance("State Bank of India", "4000"))
        } finally { db.close() }
    }

    @Test fun removingDuplicateRecalculatesLaterSourceBalancesUntilAnIndependentAnchor() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, CashiroDatabase::class.java).build()
        try {
            val dao = db.accountBalanceDao()
            val time = LocalDateTime.of(2026, 1, 1, 12, 0)
            dao.insertBalance(balance("1000", "MANUAL").copy(balance = BigDecimal("1000"), timestamp = time.minusMinutes(1)))
            val duplicate = db.transactionDao().insertTransaction(transaction(bank = "State Bank of India", suffix = "1000", hash = "duplicate").copy(transactionType = TransactionType.EXPENSE))
            dao.insertBalance(balance("1000", "TRANSACTION_CALCULATED", duplicate))
            val real = db.transactionDao().insertTransaction(transaction(bank = "State Bank of India", suffix = "1000", hash = "real").copy(amount = BigDecimal("50"), transactionType = TransactionType.EXPENSE, dateTime = time.plusMinutes(1)))
            dao.insertBalance(balance("1000", "TRANSACTION_CALCULATED", real).copy(balance = BigDecimal("850"), timestamp = time.plusMinutes(1)))
            dao.insertBalance(balance("1000", "SMS_BALANCE").copy(balance = BigDecimal("700"), timestamp = time.plusMinutes(2)))
            dao.deleteTransactionBalancesAndRecalculate(duplicate)
            assertEquals(0, dao.getBalanceByTransactionId(real)!!.balance.compareTo(BigDecimal("950")))
            assertEquals(0, dao.getLatestBalance("State Bank of India", "1000")!!.balance.compareTo(BigDecimal("700")))
        } finally { db.close() }
    }

    @Test fun oldBackupDefaultsAndRememberedAliases() {
        val old = Gson().fromJson("{}", AppPreferences::class.java)
        assertTrue(old.bankAccountMerges.orEmpty().isEmpty())
        val store = BankAccountMergeStore(context)
        store.forgetAccount("Example Bank", "000")
        assertEquals("000", store.resolveSuffix("Example Bank", "INR", "000"))
        store.remember("Example Bank", "INR", "000", "1000")
        assertEquals("1000", BankAccountMergeStore(context).resolveSuffix("Example Bank", "INR", "000"))
        assertEquals("000", store.resolveSuffix("Example Bank", "USD", "000"))
        store.forgetAccount("Example Bank", "1000")
    }
}
