package com.ritesh.cashiro

import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.platform.app.InstrumentationRegistry
import com.ritesh.cashiro.data.database.CashiroDatabase
import com.ritesh.cashiro.data.database.entity.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import com.ritesh.cashiro.data.manager.SmsTransactionProcessor
import com.ritesh.cashiro.data.manager.BalanceUpdateProcessor
import com.ritesh.cashiro.data.repository.*
import com.ritesh.cashiro.domain.service.RuleEngine
import com.ritesh.cashiro.data.mapper.toEntity
import com.ritesh.parser.core.ParsedTransaction
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime

class WorkerPersistenceTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val date = LocalDateTime.of(2026, 1, 1, 12, 0)
    private fun row(hash: String) = TransactionEntity(amount = BigDecimal("100"), merchantName = "Example Shop",
        category = "Food", transactionType = TransactionType.EXPENSE, dateTime = date, transactionHash = hash,
        smsBody = "Synthetic debit alert", smsSender = "EXAMPLE-T", bankName = "Example Bank", accountNumber = "1000")

    @Test fun rescanPreservesCuratedRowsAndAnchorsWhileRemovingRebuildableRows() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, CashiroDatabase::class.java).build()
        try {
            val dao = db.transactionDao()
            val rebuild = dao.insertTransaction(row("rebuild"))
            val preserved = listOf(
                dao.insertTransaction(row("manual").copy(smsSender = null, smsBody = null)),
                dao.insertTransaction(row("pdf").copy(smsSender = "GPay PDF")),
                dao.insertTransaction(row("deleted").copy(isDeleted = true)),
                dao.insertTransaction(row("attached").copy(attachments = "synthetic-attachment")),
                dao.insertTransaction(row("noted").copy(description = "Synthetic imported note")),
                dao.insertTransaction(row("loan"))
            )
            val person = db.lendBorrowDao().insertPerson(LendBorrowPersonEntity(name = "Example Person"))
            db.lendBorrowDao().insertTransaction(LendBorrowTransactionEntity(personId = person,
                transactionId = preserved.last(), type = LendBorrowType.LENT, amount = BigDecimal("100"), title = "Synthetic loan"))
            val balances = db.accountBalanceDao()
            fun balance(suffix: String, source: String?, linked: Long? = null) = AccountBalanceEntity(
                bankName = "Example Bank", accountLast4 = suffix, balance = BigDecimal("900"), timestamp = date,
                sourceType = source, transactionId = linked, smsSource = "Synthetic alert")
            balances.insertBalance(balance("1000", "TRANSACTION", rebuild))
            balances.insertBalance(balance("2000", null))
            balances.insertBalance(balance("3000", "MANUAL"))
            balances.insertBalance(balance("4000", "SMS_BALANCE"))
            balances.insertBalance(balance("5000", "MERGE"))
            balances.insertBalance(balance("6000", "TRANSACTION", preserved[1]))
            db.withTransaction {
                dao.deleteRebuildableSmsTransactions()
                balances.deleteRebuildableBalances()
                db.ruleApplicationDao().deleteOrphanedApplications()
            }
            assertNull(dao.getTransactionById(rebuild))
            preserved.forEach { assertNotNull(dao.getTransactionById(it)) }
            assertNull(balances.getLatestBalance("Example Bank", "1000"))
            assertNull(balances.getLatestBalance("Example Bank", "2000"))
            listOf("3000", "4000", "5000", "6000").forEach { assertNotNull(balances.getLatestBalance("Example Bank", it)) }
            assertEquals(preserved.last(), db.lendBorrowDao().getTransactionsForPersonSync(person).single().transactionId)
        } finally { db.close() }
    }

    private fun processor(db: CashiroDatabase, scope: CoroutineScope): SmsTransactionProcessor {
        val balances = AccountBalanceRepository(db.accountBalanceDao(), context, com.ritesh.cashiro.data.preferences.BankAccountMergeStore(context))
        val transactions = TransactionRepository(db.transactionDao(), balances)
        val engine = RuleEngine(SubcategoryRepository(db.subcategoryDao(), db.categoryDao(), context, scope),
            CategoryRepository(db.categoryDao(), context, scope))
        return SmsTransactionProcessor(transactions, balances, MerchantMappingRepository(db.merchantMappingDao()),
            SubscriptionRepository(db.subscriptionDao()), RuleRepositoryImpl(db.ruleDao(), db.ruleApplicationDao(), engine),
            engine, db, context, BalanceUpdateProcessor(CardRepository(db.cardDao()), balances),
            com.ritesh.cashiro.data.preferences.BankAccountMergeStore(context),
            com.ritesh.cashiro.data.preferences.IgnoredAccountsStore(context, com.ritesh.cashiro.data.preferences.BankAccountMergeStore(context)),
            CardRepository(db.cardDao()))
    }

    @Test fun cleanupKeepsEditedMetadataAndConflictingCategoryRows() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, CashiroDatabase::class.java).build()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            val edited = db.transactionDao().insertTransaction(row("partner").copy(bankName = "State Bank of India",
                accountNumber = null, reference = "000000000001", category = "Food", subcategory = "Example category",
                description = "Synthetic edited note", attachments = "synthetic-a", isRecurring = true))
            val keeper = db.transactionDao().insertTransaction(row("bank").copy(reference = "000000000001",
                category = "Miscellaneous", balanceAfter = BigDecimal("900")))
            val conflict = db.transactionDao().insertTransaction(row("conflict").copy(reference = "000000000001", category = "Travel"))
            assertEquals(1, processor(db, scope).cleanupDuplicates())
            assertNull(db.transactionDao().getTransactionById(edited))
            assertNotNull(db.transactionDao().getTransactionById(conflict))
            val retained = db.transactionDao().getTransactionById(keeper)!!
            assertEquals("Food", retained.category)
            assertEquals("Example category", retained.subcategory)
            assertEquals("Synthetic edited note", retained.description)
            assertEquals("synthetic-a", retained.attachments)
            assertTrue(retained.isRecurring)
            assertEquals("Example Bank", retained.bankName)
            assertEquals(0, retained.balanceAfter!!.compareTo(BigDecimal("900")))
        } finally { scope.cancel(); db.close() }
    }

    @Test fun duplicateCleanupPreservesTransactionAcknowledgedBySubscriptionLedger() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, CashiroDatabase::class.java).build()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            val paidId = db.transactionDao().insertTransaction(row("subscription-paid").copy(
                bankName = "State Bank of India", reference = "123456789012", balanceAfter = null
            ))
            val betterId = db.transactionDao().insertTransaction(row("subscription-bank-confirmation").copy(
                reference = "123456789012", balanceAfter = BigDecimal("900")
            ))
            val subId = db.subscriptionDao().insertSubscription(SubscriptionEntity(
                merchantName = "Example Shop", amount = BigDecimal("100"), nextPaymentDate = date.toLocalDate()
            ))
            db.subscriptionPaymentDao().insert(SubscriptionPaymentEntity(
                subId, date.toLocalDate(), date.toLocalDate(), paidId, "synthetic-payment-confirmation"
            ))
            assertEquals(0, processor(db, scope).cleanupDuplicates())
            assertNotNull(db.transactionDao().getTransactionById(paidId))
            assertNotNull(db.transactionDao().getTransactionById(betterId))
            assertEquals(paidId, db.subscriptionPaymentDao().forSmsHash("synthetic-payment-confirmation")!!.transactionId)
        } finally { scope.cancel(); db.close() }
    }

    @Test fun balanceWriteFailureRollsBackTransactionAndCanBeRetried() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, CashiroDatabase::class.java).build()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            val processor = processor(db, scope)
            val input = ParsedTransaction(BigDecimal("100"), com.ritesh.parser.core.TransactionType.EXPENSE,
                "Example Shop", null, "1000", BigDecimal("900"), smsBody = "Synthetic atomic alert", sender = "EXAMPLE-T",
                timestamp = 1767268800000L, bankName = "Example Bank")
            db.openHelper.writableDatabase.execSQL("CREATE TRIGGER synthetic_balance_failure BEFORE INSERT ON account_balances WHEN NEW.account_last4 = '1000' BEGIN SELECT RAISE(ABORT, 'Synthetic balance failure'); END")
            val scan = SmsTransactionProcessor.ScanContext(emptyMap(), emptyMap())
            val failed = processor.saveParsedTransaction(input, input.smsBody, scan)
            assertFalse(failed.success)
            assertTrue(failed.persistenceFailed)
            val later = input.copy(accountLast4 = "2000", smsBody = "Synthetic later alert", timestamp = input.timestamp + 1000)
            assertTrue(processor.saveParsedTransaction(later, later.smsBody, scan).success)
            assertNull(db.transactionDao().getTransactionByHash(input.toEntity().transactionHash))
            db.openHelper.writableDatabase.execSQL("DROP TRIGGER synthetic_balance_failure")
            assertTrue(processor.saveParsedTransaction(input, input.smsBody, SmsTransactionProcessor.ScanContext(emptyMap(), emptyMap())).success)
            assertNotNull(db.accountBalanceDao().getLatestBalance("Example Bank", "1000"))
        } finally { scope.cancel(); db.close() }
    }

    @Test fun rescanPreservesIgnoredHistoryAndLegacyBalancesThroughConfirmedAliases() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, CashiroDatabase::class.java).build()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val merges = com.ritesh.cashiro.data.preferences.BankAccountMergeStore(context)
        val ignored = com.ritesh.cashiro.data.preferences.IgnoredAccountsStore(context, merges)
        val bank = "Example Rescan Bank"
        try {
            merges.remember(bank, "INR", "789", "6789")
            ignored.setIgnored(bank, "INR", "6789", true)
            val retained = db.transactionDao().insertTransaction(row("ignored-rescan").copy(bankName = bank, accountNumber = "789"))
            val removed = db.transactionDao().insertTransaction(row("tracked-rescan").copy(bankName = bank, accountNumber = "9999"))
            val otherCurrency = db.transactionDao().insertTransaction(row("currency-rescan").copy(bankName = bank, accountNumber = "789", currency = "USD"))
            db.accountBalanceDao().insertBalance(AccountBalanceEntity(bankName = bank, accountLast4 = "789",
                balance = BigDecimal("900"), timestamp = date, transactionId = retained, sourceType = "TRANSACTION"))
            db.accountBalanceDao().insertBalance(AccountBalanceEntity(bankName = bank, accountLast4 = "6789",
                balance = BigDecimal("900"), timestamp = date, sourceType = null, smsSource = "Synthetic legacy balance"))
            processor(db, scope).prepareForRescan()
            assertNotNull(db.transactionDao().getTransactionById(retained))
            assertNull(db.transactionDao().getTransactionById(removed))
            assertNull(db.transactionDao().getTransactionById(otherCurrency))
            assertNotNull(db.accountBalanceDao().getLatestBalance(bank, "789"))
            assertNotNull(db.accountBalanceDao().getLatestBalance(bank, "6789"))
            ignored.setIgnored(bank, "INR", "6789", false)
            processor(db, scope).prepareForRescan()
            assertNull(db.transactionDao().getTransactionById(retained))
            assertNull(db.accountBalanceDao().getLatestBalance(bank, "789"))
            assertNull(db.accountBalanceDao().getLatestBalance(bank, "6789"))
        } finally {
            ignored.setIgnored(bank, "INR", "6789", false)
            merges.forgetAccount(bank, "6789")
            scope.cancel()
            db.close()
        }
    }

    @Test fun explicitIgnoreBlocksPersistenceAndUndoAllowsSameMessage() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, CashiroDatabase::class.java).build()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val merges = com.ritesh.cashiro.data.preferences.BankAccountMergeStore(context)
        val ignored = com.ritesh.cashiro.data.preferences.IgnoredAccountsStore(context, merges)
        try {
            val input = ParsedTransaction(BigDecimal("100"), com.ritesh.parser.core.TransactionType.EXPENSE,
                "Example Shop", null, "6789", BigDecimal("900"), smsBody = "Synthetic ignored alert", sender = "EXAMPLE-T",
                timestamp = 1767268800000L, bankName = "Example Ignore Bank")
            val writer = processor(db, scope)
            val scan = SmsTransactionProcessor.ScanContext(emptyMap(), emptyMap())
            ignored.setIgnored(input.bankName, input.currency, "6789", true)
            val skipped = writer.saveParsedTransaction(input, input.smsBody, scan)
            assertTrue(skipped.blocked)
            assertFalse(skipped.success)
            assertFalse(skipped.persistenceFailed)
            assertNull(db.transactionDao().getTransactionByHash(input.toEntity().transactionHash))
            assertNull(db.accountBalanceDao().getLatestBalance(input.bankName, "6789"))
            ignored.setIgnored(input.bankName, input.currency, "6789", false)
            assertTrue(writer.saveParsedTransaction(input, input.smsBody, scan).success)
            assertNotNull(db.accountBalanceDao().getLatestBalance(input.bankName, "6789"))
        } finally {
            ignored.setIgnored("Example Ignore Bank", "INR", "6789", false)
            scope.cancel()
            db.close()
        }
    }

    @Test fun notificationLookupMatchesAmountsWithDifferentDecimalScales() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, CashiroDatabase::class.java).build()
        try {
            val id = db.transactionDao().insertTransaction(row("scale").copy(amount = BigDecimal("100.00")))
            assertEquals(id, db.transactionDao().getTransactionByAmountAndDate(BigDecimal("100"), date.minusMinutes(2), date.plusMinutes(2)).single().id)
        } finally { db.close() }
    }

    @Test fun batchedUnrecognizedInsertKeepsReportedAndDeletedTombstones() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, CashiroDatabase::class.java).build()
        try {
            val dao = db.unrecognizedSmsDao()
            val existing = UnrecognizedSmsEntity(sender = "EXAMPLE-T", smsBody = "Synthetic alert", receivedAt = date,
                reported = true, isDeleted = true)
            val id = dao.insert(existing)
            dao.insertAll(listOf(existing.copy(id = 0, reported = false, isDeleted = false),
                existing.copy(id = 0, smsBody = "Other synthetic alert", reported = false, isDeleted = false)))
            val retained = dao.findBySenderAndBody(existing.sender, existing.smsBody)!!
            assertEquals(id, retained.id)
            assertTrue(retained.reported)
            assertTrue(retained.isDeleted)
            assertNotNull(dao.findBySenderAndBody(existing.sender, "Other synthetic alert"))
        } finally { db.close() }
    }
}
