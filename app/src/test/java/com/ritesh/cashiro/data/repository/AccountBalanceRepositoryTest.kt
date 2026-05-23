package com.ritesh.cashiro.data.repository

import com.ritesh.cashiro.data.database.dao.AccountBalanceDao
import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.assertEquals

class AccountBalanceRepositoryTest {

    @Test
    fun resolveAccountLast4ExpandsUniqueSameBankSuffix() = runTest {
        val dao = FakeAccountBalanceDao(
            suffixMatches = mapOf("Indian Overseas Bank" to mapOf("99" to listOf("1999")))
        )
        val repository = AccountBalanceRepository(dao, null)

        assertEquals("1999", repository.resolveAccountLast4("Indian Overseas Bank", "99"))
    }

    @Test
    fun resolveAccountLast4DoesNotGuessAmbiguousSuffix() = runTest {
        val dao = FakeAccountBalanceDao(
            suffixMatches = mapOf("Test Bank" to mapOf("99" to listOf("1999", "2099")))
        )
        val repository = AccountBalanceRepository(dao, null)

        assertEquals("99", repository.resolveAccountLast4("Test Bank", "99"))
    }

    @Test
    fun resolveAccountLast4DoesNotQueryForBlankSuffix() = runTest {
        val dao = FakeAccountBalanceDao()
        val repository = AccountBalanceRepository(dao, null)

        assertEquals("", repository.resolveAccountLast4("Test Bank", ""))
        assertEquals(0, dao.suffixLookupCount)
    }

    private class FakeAccountBalanceDao(
        private val suffixMatches: Map<String, Map<String, List<String>>> = emptyMap()
    ) : AccountBalanceDao {
        var suffixLookupCount = 0

        override suspend fun insertBalance(balance: AccountBalanceEntity): Long = 1

        override suspend fun getLatestBalance(bankName: String, accountLast4: String): AccountBalanceEntity? = null

        override suspend fun getAccountLast4sEndingWith(bankName: String, suffix: String): List<String> {
            suffixLookupCount++
            return suffixMatches[bankName]?.get(suffix).orEmpty()
        }

        override fun getLatestBalanceFlow(
            bankName: String,
            accountLast4: String
        ): Flow<AccountBalanceEntity?> = flowOf(null)

        override fun getAllLatestBalances(): Flow<List<AccountBalanceEntity>> = flowOf(emptyList())

        override fun getAllBalances(): Flow<List<AccountBalanceEntity>> = flowOf(emptyList())

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

        override suspend fun updateBalanceById(id: Long, newBalance: BigDecimal) = Unit

        override suspend fun getBalanceCountForAccount(bankName: String, accountLast4: String): Int = 0

        override suspend fun deleteAccount(bankName: String, accountLast4: String): Int = 0

        override suspend fun updateAccountBankName(
            oldBankName: String,
            accountLast4: String,
            newBankName: String
        ): Int = 0

        override suspend fun getAccountByLast4(accountLast4: String): AccountBalanceEntity? = null
    }
}
