package com.ritesh.cashiro

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.ritesh.cashiro.data.database.CashiroDatabase
import com.ritesh.cashiro.data.database.entity.*
import com.ritesh.cashiro.data.manager.BalanceUpdateProcessor
import com.ritesh.cashiro.data.repository.AccountBalanceRepository
import com.ritesh.cashiro.data.repository.CardRepository
import com.ritesh.cashiro.data.mapper.toEntity
import com.ritesh.parser.core.ParsedTransaction
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
import java.math.BigDecimal
import java.time.LocalDateTime

class RuleBalanceRoutingTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun parsed(card: Boolean = false) = ParsedTransaction(BigDecimal("100"), com.ritesh.parser.core.TransactionType.EXPENSE,
        "Example Shop", "000000000001", "1000", BigDecimal("900"), smsBody = "Synthetic debit alert", sender = "EXAMPLE", timestamp = 1767268800000L, bankName = "Example Bank", isFromCard = card)
    @Test fun selectedCreditUsesNewAccountAndAmountWithoutTransplantingAbsoluteBalance() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, CashiroDatabase::class.java).build()
        try {
            val input = parsed()
            db.accountBalanceDao().insertBalance(AccountBalanceEntity(bankName = "Other Bank", accountLast4 = "2000", balance = BigDecimal("1000"), timestamp = LocalDateTime.of(2025, 1, 1, 0, 0), sourceType = "MANUAL"))
            val selected = input.toEntity().copy(bankName = "Other Bank", accountNumber = "2000", transactionType = TransactionType.CREDIT, amount = BigDecimal("25"))
            val id = db.transactionDao().insertTransaction(selected)
            BalanceUpdateProcessor(CardRepository(db.cardDao()), AccountBalanceRepository(db.accountBalanceDao(), context, com.ritesh.cashiro.data.preferences.BankAccountMergeStore(context))).process(input, selected, id)
            val result = db.accountBalanceDao().getLatestBalance("Other Bank", "2000")!!
            assertEquals(0, result.balance.compareTo(BigDecimal("1025")))
            assertFalse(result.isCreditCard)
        } finally { db.close() }
    }
    @Test fun cardIssuerAndBindingSurviveTransactionBankRule() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, CashiroDatabase::class.java).build()
        try {
            val cards = CardRepository(db.cardDao())
            cards.insertCard(CardEntity(bankName = "Example Bank", cardLast4 = "1000", cardType = CardType.DEBIT, accountLast4 = "2000"))
            val input = parsed(card = true)
            val selected = input.toEntity().copy(bankName = "Rule Bank")
            val id = db.transactionDao().insertTransaction(selected)
            BalanceUpdateProcessor(cards, AccountBalanceRepository(db.accountBalanceDao(), context, com.ritesh.cashiro.data.preferences.BankAccountMergeStore(context))).process(input, selected, id)
            assertNotNull(db.accountBalanceDao().getLatestBalance("Example Bank", "2000"))
            assertNull(db.accountBalanceDao().getLatestBalance("Rule Bank", "1000"))
            assertEquals("2000", cards.getCard("Example Bank", "1000")?.accountLast4)
        } finally { db.close() }
    }
    @Test fun originalCreditWithoutCardFlagRetainsIssuerWhenBankRuleChangesIt() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, CashiroDatabase::class.java).build()
        try {
            val input = parsed().copy(type = com.ritesh.parser.core.TransactionType.CREDIT)
            val selected = input.toEntity().copy(bankName = "Rule Bank", accountNumber = "2000")
            val id = db.transactionDao().insertTransaction(selected)
            BalanceUpdateProcessor(CardRepository(db.cardDao()), AccountBalanceRepository(db.accountBalanceDao(), context, com.ritesh.cashiro.data.preferences.BankAccountMergeStore(context))).process(input, selected, id)
            assertNotNull(db.accountBalanceDao().getLatestBalance("Example Bank", "1000"))
            assertNull(db.accountBalanceDao().getLatestBalance("Rule Bank", "2000"))
        } finally { db.close() }
    }
    @Test fun suffixNormalizationRetainsBankReportedAbsoluteBalance() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, CashiroDatabase::class.java).build()
        try {
            val balances = AccountBalanceRepository(db.accountBalanceDao(), context, com.ritesh.cashiro.data.preferences.BankAccountMergeStore(context))
            val input = parsed().copy(accountLast4 = "000001000")
            val selected = balances.resolveEntityAccountNumber(input.toEntity(), input)
            val id = db.transactionDao().insertTransaction(selected)
            BalanceUpdateProcessor(CardRepository(db.cardDao()), balances).process(input, selected, id)
            val result = db.accountBalanceDao().getLatestBalance("Example Bank", "1000")!!
            assertEquals(0, result.balance.compareTo(BigDecimal("900")))
        } finally { db.close() }
    }
    @Test fun deletedRawSmsStillMatchesAfterReplacementRetainsAnOlderHash() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, CashiroDatabase::class.java).build()
        try {
            val incoming = parsed().toEntity()
            val deleted = incoming.copy(transactionHash = "older-partner-hash", isDeleted = true)
            val id = db.transactionDao().insertTransaction(deleted)
            assertNull(db.transactionDao().getTransactionByHash(incoming.transactionHash))
            assertEquals(id, db.transactionDao().getDeletedBySms(incoming.smsBody!!, incoming.smsSender)?.id)
            assertNull(db.transactionDao().getDeletedBySms(incoming.smsBody!!, "OTHER"))
        } finally { db.close() }
    }
}
