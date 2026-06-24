package com.ritesh.cashiro.data.repository

import android.content.ContextWrapper
import com.ritesh.cashiro.data.database.dao.AccountBalanceDao
import com.ritesh.cashiro.data.database.dao.AccountBalanceTransactionInfo
import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import com.ritesh.cashiro.data.database.entity.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AccountBalanceRepositoryTest {

    @Test
    fun insertTransactionBalanceCalculatesFromBalanceAtTransactionTimeAndRecalculatesLaterCalculatedRows() = runTest {
        val transactionTime = LocalDateTime.of(2026, 5, 23, 20, 35)
        val dao = FakeAccountBalanceDao(
            latestBalances = mutableMapOf(
                accountKey("Test Bank", "1234") to balance(
                    id = 1,
                    bankName = "Test Bank",
                    accountLast4 = "1234",
                    balance = BigDecimal("100.00"),
                    timestamp = transactionTime.minusMinutes(5)
                )
            ),
            balanceAtOrBefore = balance(
                id = 1,
                bankName = "Test Bank",
                accountLast4 = "1234",
                balance = BigDecimal("100.00"),
                timestamp = transactionTime.minusMinutes(5)
            ),
            balancesAfter = listOf(
                AccountBalanceTransactionInfo(
                    id = 2,
                    balance = BigDecimal("90.00"),
                    sourceType = "TRANSACTION_CALCULATED",
                    isCreditCard = false,
                    transactionId = 22,
                    transactionAmount = BigDecimal("10.00"),
                    transactionType = TransactionType.EXPENSE.name,
                    transactionBalanceAfter = null
                )
            )
        )
        val repository = AccountBalanceRepository(dao, ContextWrapper(null))

        repository.insertTransactionBalance(
            bankName = "Test Bank",
            accountLast4 = "1234",
            amount = BigDecimal("50.00"),
            transactionType = TransactionType.INCOME,
            explicitBalance = null,
            timestamp = transactionTime,
            transactionId = 11,
            creditLimit = null,
            isCreditCard = false,
            smsSource = "credited alert",
            currency = "INR"
        )

        assertEquals(BigDecimal("150.00"), dao.insertedBalances.single().balance)
        assertEquals(BigDecimal("140.00"), dao.updatedBalances[2])
    }

    @Test
    fun insertTransactionBalanceDoesNotRecalculateAfterExplicitBalanceBoundary() = runTest {
        val transactionTime = LocalDateTime.of(2026, 5, 23, 20, 35)
        val dao = FakeAccountBalanceDao(
            latestBalances = mutableMapOf(
                accountKey("Test Bank", "1234") to balance(
                    id = 1,
                    bankName = "Test Bank",
                    accountLast4 = "1234",
                    balance = BigDecimal("100.00"),
                    timestamp = transactionTime.minusMinutes(5)
                )
            ),
            balanceAtOrBefore = balance(
                id = 1,
                bankName = "Test Bank",
                accountLast4 = "1234",
                balance = BigDecimal("100.00"),
                timestamp = transactionTime.minusMinutes(5)
            ),
            balancesAfter = listOf(
                AccountBalanceTransactionInfo(
                    id = 2,
                    balance = BigDecimal("200.00"),
                    sourceType = "TRANSACTION_SMS_BALANCE",
                    isCreditCard = false,
                    transactionId = 22,
                    transactionAmount = BigDecimal("10.00"),
                    transactionType = TransactionType.EXPENSE.name,
                    transactionBalanceAfter = BigDecimal("200.00")
                )
            )
        )
        val repository = AccountBalanceRepository(dao, ContextWrapper(null))

        repository.insertTransactionBalance(
            bankName = "Test Bank",
            accountLast4 = "1234",
            amount = BigDecimal("50.00"),
            transactionType = TransactionType.INCOME,
            explicitBalance = null,
            timestamp = transactionTime,
            transactionId = 11,
            creditLimit = null,
            isCreditCard = false,
            smsSource = "credited alert",
            currency = "INR"
        )

        assertEquals(emptyMap(), dao.updatedBalances)
    }

    @Test
    fun insertTransactionBalanceDoesNotRecalculateAfterManualBalanceBoundary() = runTest {
        val transactionTime = LocalDateTime.of(2026, 5, 23, 20, 35)
        val dao = FakeAccountBalanceDao(
            latestBalances = mutableMapOf(
                accountKey("Test Bank", "1234") to balance(
                    id = 1,
                    bankName = "Test Bank",
                    accountLast4 = "1234",
                    balance = BigDecimal("100.00"),
                    timestamp = transactionTime.minusMinutes(5)
                )
            ),
            balanceAtOrBefore = balance(
                id = 1,
                bankName = "Test Bank",
                accountLast4 = "1234",
                balance = BigDecimal("100.00"),
                timestamp = transactionTime.minusMinutes(5)
            ),
            balancesAfter = listOf(
                AccountBalanceTransactionInfo(
                    id = 2,
                    balance = BigDecimal("200.00"),
                    sourceType = "MANUAL",
                    isCreditCard = false,
                    transactionId = 22,
                    transactionAmount = BigDecimal("10.00"),
                    transactionType = TransactionType.EXPENSE.name,
                    transactionBalanceAfter = null
                )
            )
        )
        val repository = AccountBalanceRepository(dao, ContextWrapper(null))

        repository.insertTransactionBalance(
            bankName = "Test Bank",
            accountLast4 = "1234",
            amount = BigDecimal("50.00"),
            transactionType = TransactionType.INCOME,
            explicitBalance = null,
            timestamp = transactionTime,
            transactionId = 11,
            creditLimit = null,
            isCreditCard = false,
            smsSource = "credited alert",
            currency = "INR"
        )

        assertEquals(emptyMap(), dao.updatedBalances)
    }

    @Test
    fun resolveAccountLast4ExpandsUniqueSameBankSuffix() = runTest {
        val dao = FakeAccountBalanceDao(
            suffixMatches = mapOf("Indian Overseas Bank" to mapOf("99" to listOf("1999")))
        )
        val repository = AccountBalanceRepository(dao, ContextWrapper(null))

        assertEquals("1999", repository.resolveAccountLast4("Indian Overseas Bank", "99"))
    }

    @Test
    fun resolveAccountLast4DoesNotGuessAmbiguousSuffix() = runTest {
        val dao = FakeAccountBalanceDao(
            suffixMatches = mapOf("Test Bank" to mapOf("99" to listOf("1999", "2099")))
        )
        val repository = AccountBalanceRepository(dao, ContextWrapper(null))

        assertEquals("99", repository.resolveAccountLast4("Test Bank", "99"))
    }

    @Test
    fun resolveAccountLast4DoesNotQueryForNonDigitSuffix() = runTest {
        val dao = FakeAccountBalanceDao()
        val repository = AccountBalanceRepository(dao, ContextWrapper(null))

        assertEquals("%", repository.resolveAccountLast4("Test Bank", "%"))
        assertEquals(0, dao.suffixLookupCount)
    }

    @Test
    fun resolveAccountLast4TrimsToLast4ForLongInputs() = runTest {
        val dao = FakeAccountBalanceDao()
        val repository = AccountBalanceRepository(dao, ContextWrapper(null))

        assertEquals("6789", repository.resolveAccountLast4("Test Bank", "123456789"))
    }

    @Test
    fun resolveAccountLast4DoesNotQueryForBlankSuffix() = runTest {
        val dao = FakeAccountBalanceDao()
        val repository = AccountBalanceRepository(dao, ContextWrapper(null))

        assertEquals("", repository.resolveAccountLast4("Test Bank", ""))
        assertEquals(0, dao.suffixLookupCount)
    }

    @Test
    fun insertTransactionBalanceReducesCreditCardBalanceForIncomePayment() = runTest {
        val transactionTime = LocalDateTime.of(2026, 5, 23, 20, 35)
        val dao = FakeAccountBalanceDao(
            latestBalances = mutableMapOf(
                accountKey("Test Bank", "1234") to balance(
                    id = 1,
                    bankName = "Test Bank",
                    accountLast4 = "1234",
                    balance = BigDecimal("100.00"),
                    timestamp = transactionTime.minusMinutes(5),
                    isCreditCard = true
                )
            ),
            balanceAtOrBefore = balance(
                id = 1,
                bankName = "Test Bank",
                accountLast4 = "1234",
                balance = BigDecimal("100.00"),
                timestamp = transactionTime.minusMinutes(5),
                isCreditCard = true
            )
        )
        val repository = AccountBalanceRepository(dao, ContextWrapper(null))

        repository.insertTransactionBalance(
            bankName = "Test Bank",
            accountLast4 = "1234",
            amount = BigDecimal("30.00"),
            transactionType = TransactionType.INCOME,
            explicitBalance = null,
            timestamp = transactionTime,
            transactionId = 11,
            creditLimit = null,
            isCreditCard = true,
            smsSource = "payment alert",
            currency = "INR"
        )

        assertEquals(BigDecimal("70.00"), dao.insertedBalances.single().balance)
    }

    @Test
    fun backdatedInsertRecalculatesCalculatedRowsForward() = runTest {
        val dao = FakeAccountBalanceDao()
        val repository = AccountBalanceRepository(dao, ContextWrapper(null))

        // Seed initial balance at T1 (1000)
        val t1 = LocalDateTime.of(2025, 1, 1, 10, 0)
        dao.seedBalance(AccountBalanceEntity(bankName = "Test Bank", accountLast4 = "1234", balance = BigDecimal("1000"), timestamp = t1, sourceType = "CALCULATED"))

        // Seed subsequent CALCULATED balance at T3 (1200, representing income of 200)
        val t3 = LocalDateTime.of(2025, 1, 3, 10, 0)
        val balanceEntityT3 = AccountBalanceEntity(bankName = "Test Bank", accountLast4 = "1234", balance = BigDecimal("1200"), timestamp = t3, sourceType = "CALCULATED", transactionId = 100L)
        dao.seedBalance(balanceEntityT3)
        // Associated transaction for T3 is INCOME of 200
        dao.transactionMap[100L] = Pair(BigDecimal("200"), "INCOME")

        // Now, insert a backdated transaction at T2 (between T1 and T3) of EXPENSE of 300
        val t2 = LocalDateTime.of(2025, 1, 2, 10, 0)
        repository.insertTransactionBalance(
            bankName = "Test Bank",
            accountLast4 = "1234",
            amount = BigDecimal("300"),
            transactionType = TransactionType.EXPENSE,
            explicitBalance = null,
            smsSource = "EXPENSE of 300",
            currency = "INR",
            timestamp = t2,
            transactionId = 200L,
            creditLimit = null,
            isCreditCard = false
        )

        // The backdated row at T2 should be: T1 balance (1000) - T2 expense (300) = 700
        val balanceT2 = dao.balances.find { it.timestamp == t2 }
        assertNotNull(balanceT2)
        assertEquals(BigDecimal("700"), balanceT2.balance)

        // The subsequent row at T3 should be recalculated: T2 balance (700) + T3 income (200) = 900
        val balanceT3 = dao.balances.find { it.timestamp == t3 }
        assertNotNull(balanceT3)
        assertEquals(BigDecimal("900"), balanceT3.balance)
    }

    @Test
    fun explicitSmsBalanceStopsRecalculation() = runTest {
        val dao = FakeAccountBalanceDao()
        val repository = AccountBalanceRepository(dao, ContextWrapper(null))

        val t1 = LocalDateTime.of(2025, 1, 1, 10, 0)
        dao.seedBalance(AccountBalanceEntity(bankName = "Test Bank", accountLast4 = "1234", balance = BigDecimal("1000"), timestamp = t1, sourceType = "CALCULATED"))

        // Seed subsequent explicit SMS balance row at T3 (explicit balance of 1500)
        val t3 = LocalDateTime.of(2025, 1, 3, 10, 0)
        dao.seedBalance(AccountBalanceEntity(
            bankName = "Test Bank",
            accountLast4 = "1234",
            balance = BigDecimal("1500"),
            timestamp = t3,
            sourceType = "SMS_BALANCE", // boundary
            transactionId = 100L
        ))
        dao.transactionMap[100L] = Pair(BigDecimal("200"), "INCOME")

        // Insert backdated transaction at T2 (EXPENSE of 300)
        val t2 = LocalDateTime.of(2025, 1, 2, 10, 0)
        repository.insertTransactionBalance(
            bankName = "Test Bank",
            accountLast4 = "1234",
            amount = BigDecimal("300"),
            transactionType = TransactionType.EXPENSE,
            explicitBalance = null,
            smsSource = "EXPENSE of 300",
            currency = "INR",
            timestamp = t2,
            transactionId = 200L,
            creditLimit = null,
            isCreditCard = false
        )

        // The backdated row at T2 should be 700
        val balanceT2 = dao.balances.find { it.timestamp == t2 }
        assertNotNull(balanceT2)
        assertEquals(BigDecimal("700"), balanceT2.balance)

        // The subsequent row at T3 (explicit boundary) should NOT be changed
        val balanceT3 = dao.balances.find { it.timestamp == t3 }
        assertNotNull(balanceT3)
        assertEquals(BigDecimal("1500"), balanceT3.balance)
    }

    @Test
    fun manualBalanceStopsRecalculation() = runTest {
        val dao = FakeAccountBalanceDao()
        val repository = AccountBalanceRepository(dao, ContextWrapper(null))

        val t1 = LocalDateTime.of(2025, 1, 1, 10, 0)
        dao.seedBalance(AccountBalanceEntity(bankName = "Test Bank", accountLast4 = "1234", balance = BigDecimal("1000"), timestamp = t1, sourceType = "CALCULATED"))

        // Seed subsequent manual balance row at T3
        val t3 = LocalDateTime.of(2025, 1, 3, 10, 0)
        dao.seedBalance(AccountBalanceEntity(
            bankName = "Test Bank",
            accountLast4 = "1234",
            balance = BigDecimal("2000"),
            timestamp = t3,
            sourceType = "MANUAL", // boundary
            transactionId = 100L
        ))
        dao.transactionMap[100L] = Pair(BigDecimal("200"), "INCOME")

        // Insert backdated transaction at T2 (EXPENSE of 300)
        val t2 = LocalDateTime.of(2025, 1, 2, 10, 0)
        repository.insertTransactionBalance(
            bankName = "Test Bank",
            accountLast4 = "1234",
            amount = BigDecimal("300"),
            transactionType = TransactionType.EXPENSE,
            explicitBalance = null,
            smsSource = "EXPENSE of 300",
            currency = "INR",
            timestamp = t2,
            transactionId = 200L,
            creditLimit = null,
            isCreditCard = false
        )

        // The backdated row at T2 should be 700
        val balanceT2 = dao.balances.find { it.timestamp == t2 }
        assertNotNull(balanceT2)
        assertEquals(BigDecimal("700"), balanceT2.balance)

        // The subsequent row at T3 (manual boundary) should NOT be changed
        val balanceT3 = dao.balances.find { it.timestamp == t3 }
        assertNotNull(balanceT3)
        assertEquals(BigDecimal("2000"), balanceT3.balance)
    }

    @Test
    fun creditCardIncomePaymentReducesOutstandingBalance() = runTest {
        val dao = FakeAccountBalanceDao()
        val repository = AccountBalanceRepository(dao, ContextWrapper(null))

        val t1 = LocalDateTime.of(2025, 1, 1, 10, 0)
        dao.seedBalance(AccountBalanceEntity(
            bankName = "Test Bank",
            accountLast4 = "1234",
            balance = BigDecimal("2000"),
            timestamp = t1,
            sourceType = "CALCULATED",
            isCreditCard = true
        ))

        // Insert INCOME transaction at T2 (payment to credit card of 500)
        val t2 = LocalDateTime.of(2025, 1, 2, 10, 0)
        repository.insertTransactionBalance(
            bankName = "Test Bank",
            accountLast4 = "1234",
            amount = BigDecimal("500"),
            transactionType = TransactionType.INCOME,
            explicitBalance = null,
            smsSource = "Payment of 500",
            currency = "INR",
            timestamp = t2,
            transactionId = 200L,
            creditLimit = null,
            isCreditCard = true
        )

        // Outstanding balance should decrease: 2000 - 500 = 1500
        val balanceT2 = dao.balances.find { it.timestamp == t2 }
        assertNotNull(balanceT2)
        assertEquals(BigDecimal("1500"), balanceT2.balance)
    }

    private class FakeAccountBalanceDao(
        private val latestBalances: MutableMap<String, AccountBalanceEntity> = mutableMapOf(),
        private val balanceAtOrBefore: AccountBalanceEntity? = null,
        private val balancesAfter: List<AccountBalanceTransactionInfo> = emptyList(),
        private val suffixMatches: Map<String, Map<String, List<String>>> = emptyMap()
    ) : AccountBalanceDao {
        val insertedBalances = mutableListOf<AccountBalanceEntity>()
        val updatedBalances = mutableMapOf<Long, BigDecimal>()
        var suffixLookupCount = 0
        val balances = mutableListOf<AccountBalanceEntity>()
        val transactionMap = mutableMapOf<Long, Pair<BigDecimal, String>>()
        private var nextId = 1L

        init {
            // Seed constructor values to dynamic balances as fallback
            latestBalances.values.forEach { seedBalance(it) }
            balanceAtOrBefore?.let { seedBalance(it) }
        }

        fun seedBalance(entity: AccountBalanceEntity) {
            if (balances.none { it.id == entity.id && entity.id != 0L }) {
                balances.add(entity.copy(id = entity.id.takeIf { it != 0L } ?: nextId++))
            }
        }

        override suspend fun insertBalance(balance: AccountBalanceEntity): Long {
            val id = balance.id.takeIf { it != 0L } ?: nextId++
            val saved = balance.copy(id = id)
            balances.add(saved)
            insertedBalances.add(saved)
            latestBalances[accountKey(balance.bankName, balance.accountLast4)] = saved
            return id
        }

        override suspend fun getLatestBalance(bankName: String, accountLast4: String): AccountBalanceEntity? {
            return latestBalances[accountKey(bankName, accountLast4)]
                ?: balances.filter { it.bankName == bankName && it.accountLast4 == accountLast4 }
                    .maxByOrNull { it.timestamp }
        }

        override suspend fun getLatestBalanceOnOrBefore(
            bankName: String,
            accountLast4: String,
            timestamp: LocalDateTime
        ): AccountBalanceEntity? {
            return balanceAtOrBefore
                ?: balances.filter {
                    it.bankName == bankName && it.accountLast4 == accountLast4 && !it.timestamp.isAfter(timestamp)
                }.maxByOrNull { it.timestamp }
        }

        override suspend fun getBalancesAfterWithTransactions(
            bankName: String,
            accountLast4: String,
            timestamp: LocalDateTime
        ): List<AccountBalanceTransactionInfo> {
            if (balancesAfter.isNotEmpty()) {
                return balancesAfter
            }
            return balances.filter {
                it.bankName == bankName && it.accountLast4 == accountLast4 && it.timestamp.isAfter(timestamp)
            }.sortedBy { it.timestamp }
             .map { bal ->
                 val tx = bal.transactionId?.let { transactionMap[it] }
                 AccountBalanceTransactionInfo(
                     id = bal.id,
                     balance = bal.balance,
                     sourceType = bal.sourceType,
                     isCreditCard = bal.isCreditCard,
                     transactionId = bal.transactionId,
                     transactionAmount = tx?.first,
                     transactionType = tx?.second,
                     transactionBalanceAfter = null
                 )
             }
        }

        override suspend fun getAccountLast4sEndingWith(bankName: String, suffix: String): List<String> {
            suffixLookupCount++
            return suffixMatches[bankName]?.get(suffix).orEmpty()
        }

        override fun getLatestBalanceFlow(
            bankName: String,
            accountLast4: String
        ): Flow<AccountBalanceEntity?> = flowOf(latestBalances[accountKey(bankName, accountLast4)])

        override fun getAllLatestBalances(): Flow<List<AccountBalanceEntity>> = flowOf(latestBalances.values.toList())

        override fun getAllBalances(): Flow<List<AccountBalanceEntity>> = flowOf(latestBalances.values.toList())

        override suspend fun deleteAllBalances() = Unit

        override suspend fun deleteSampleBalances() = Unit

        override fun getCurrentMonthLatestBalances(): Flow<List<AccountBalanceEntity>> = flowOf(emptyList())

        override fun getTotalBalance(): Flow<BigDecimal?> = flowOf(BigDecimal.ZERO)

        override fun getBalanceHistory(
            bankName: String,
            accountLast4: String,
            startDate: LocalDateTime,
            endDate: LocalDateTime
        ): Flow<List<AccountBalanceEntity>> = flowOf(emptyList())

        override fun getAccountCount(): Flow<Int> = flowOf(0)

        override suspend fun deleteOldBalances(beforeDate: LocalDateTime): Int = 0

        override suspend fun updateBalance(balance: AccountBalanceEntity) = Unit

        override suspend fun deleteBalance(balance: AccountBalanceEntity) = Unit

        override suspend fun getBalanceHistoryForAccount(
            bankName: String,
            accountLast4: String
        ): List<AccountBalanceEntity> = emptyList()

        override suspend fun deleteBalanceById(id: Long) = Unit

        override suspend fun updateBalanceById(id: Long, newBalance: BigDecimal) {
            updatedBalances[id] = newBalance
            val index = balances.indexOfFirst { it.id == id }
            if (index != -1) {
                balances[index] = balances[index].copy(balance = newBalance)
            }
        }

        override suspend fun getBalanceCountForAccount(bankName: String, accountLast4: String): Int = balances.filter { it.bankName == bankName && it.accountLast4 == accountLast4 }.size

        override suspend fun deleteAccount(bankName: String, accountLast4: String): Int {
            val beforeSize = balances.size
            balances.removeAll { it.bankName == bankName && it.accountLast4 == accountLast4 }
            return beforeSize - balances.size
        }

        override suspend fun updateAccountBankName(
            oldBankName: String,
            accountLast4: String,
            newBankName: String
        ): Int = 0

        override suspend fun getAccountByLast4(accountLast4: String): AccountBalanceEntity? = null
    }

    private companion object {
        fun accountKey(bankName: String, accountLast4: String) = "$bankName:$accountLast4"

        fun balance(
            id: Long,
            bankName: String,
            accountLast4: String,
            balance: BigDecimal,
            timestamp: LocalDateTime,
            isCreditCard: Boolean = false
        ) = AccountBalanceEntity(
            id = id,
            bankName = bankName,
            accountLast4 = accountLast4,
            balance = balance,
            timestamp = timestamp,
            isCreditCard = isCreditCard
        )
    }
}
