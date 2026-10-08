package com.ritesh.cashiro

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.ritesh.cashiro.data.database.CashiroDatabase
import com.ritesh.cashiro.data.manager.BalanceUpdateProcessor
import com.ritesh.cashiro.data.mapper.toEntity
import com.ritesh.cashiro.data.preferences.BankAccountMergeStore
import com.ritesh.cashiro.data.repository.AccountBalanceRepository
import com.ritesh.cashiro.data.repository.CardRepository
import com.ritesh.parser.core.bank.ApolloParser
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class MobileWalletPersistenceTest {
    @Test fun apolloMessagesReuseWalletAndPreserveReportedBalance() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, CashiroDatabase::class.java).build()
        try {
            val balances = AccountBalanceRepository(db.accountBalanceDao(), context, BankAccountMergeStore(context))
            val processor = BalanceUpdateProcessor(CardRepository(db.cardDao()), balances)
            listOf("900.00", "800.00").forEachIndexed { index, balance ->
                val parsed = ApolloParser().parse("Your account is debited with ETB 100.00. Available Balance: ETB $balance", "APOLLO", 1767268800000L + index * 60000)!!
                val entity = balances.resolveEntityAccountNumber(parsed.toEntity(), parsed)
                assertEquals("wallet_ETB", entity.accountNumber)
                processor.process(parsed, entity, db.transactionDao().insertTransaction(entity))
            }
            val accounts = balances.getAllLatestBalances().first()
            assertEquals(1, accounts.size)
            assertTrue(accounts.single().isWallet)
            assertEquals("ETB", accounts.single().currency)
            assertEquals(0, accounts.single().balance.compareTo(BigDecimal("800")))
            assertTrue(BankAccountMergeStore.duplicatePairs(accounts).isEmpty())
        } finally { db.close() }
    }
}
