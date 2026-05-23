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

    private class FakeAccountBalanceDao(
        private val latestBalances: MutableMap<String, AccountBalanceEntity> = mutableMapOf(),
        private val balanceAtOrBefore: AccountBalanceEntity? = null,
        private val balancesAfter: List<AccountBalanceTransactionInfo> = emptyList(),
        private val suffixMatches: Map<String, Map<String, List<String>>> = emptyMap()
    ) : AccountBalanceDao {
        val insertedBalances = mutableListOf<AccountBalanceEntity>()
        val updatedBalances = mutableMapOf<Long, BigDecimal>()
        var suffixLookupCount = 0

        override suspend fun insertBalance(balance: AccountBalanceEntity): Long {
            val id = (insertedBalances.size + 100).toLong()
            val inserted = balance.copy(id = id)
            insertedBalances += inserted
            latestBalances[accountKey(balance.bankName, balance.accountLast4)] = inserted
            return id
        }

        override suspend fun getLatestBalance(bankName: String, accountLast4: String): AccountBalanceEntity? {
            return latestBalances[accountKey(bankName, accountLast4)]
        }

        override suspend fun getAccountLast4sEndingWith(bankName: String, suffix: String): List<String> {
            suffixLookupCount++
            return suffixMatches[bankName]?.get(suffix).orEmpty()
        }

        override suspend fun getLatestBalanceOnOrBefore(
            bankName: String,
            accountLast4: String,
            timestamp: LocalDateTime
        ): AccountBalanceEntity? = balanceAtOrBefore

        override suspend fun getBalancesAfterWithTransactions(
            bankName: String,
            accountLast4: String,
            timestamp: LocalDateTime
        ): List<AccountBalanceTransactionInfo> = balancesAfter

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
        }

        override suspend fun getBalanceCountForAccount(bankName: String, accountLast4: String): Int = 0

        override suspend fun deleteAccount(bankName: String, accountLast4: String): Int = 0

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
