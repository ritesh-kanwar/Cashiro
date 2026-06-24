package com.ritesh.cashiro.domain.usecase

import android.content.ContextWrapper
import com.ritesh.cashiro.data.database.dao.AccountBalanceDao
import com.ritesh.cashiro.data.database.dao.AccountBalanceTransactionInfo
import com.ritesh.cashiro.data.database.dao.SubscriptionDao
import com.ritesh.cashiro.data.database.dao.TransactionDao
import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import com.ritesh.cashiro.data.database.entity.TransactionEntity
import com.ritesh.cashiro.data.database.entity.TransactionType
import com.ritesh.cashiro.data.repository.AccountBalanceRepository
import com.ritesh.cashiro.data.repository.SubscriptionRepository
import com.ritesh.cashiro.data.repository.TransactionRepository
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AddTransactionUseCaseTest {

    private val testBank = "Test Bank"
    private val testLast4 = "1234"
    private val testCurrency = "INR"
    private val baseTime = LocalDateTime.of(2025, 1, 15, 10, 30)
    private var transactionDao: FakeTransactionDao? = null

    @Test
    fun `CREDIT transaction adds to existing outstanding balance`() = runTest {
        val balanceDao = FakeAccountBalanceDao()
        val useCase = createUseCase(balanceDao)

        balanceDao.seedBalance(
            AccountBalanceEntity(
                bankName = testBank,
                accountLast4 = testLast4,
                balance = BigDecimal("2000"),
                timestamp = baseTime.minusDays(1),
                isCreditCard = true,
                creditLimit = BigDecimal("50000")
            )
        )

        useCase.execute(
            amount = BigDecimal("500"),
            merchant = "Ice Cream Shop",
            category = "Food",
            type = TransactionType.CREDIT,
            date = baseTime,
            bankName = testBank,
            accountLast4 = testLast4,
            currency = testCurrency
        )

        val latest = balanceDao.getLatestBalance(testBank, testLast4)
        assertNotNull(latest)
        assertEquals(BigDecimal("2500"), latest!!.balance)
        assertEquals(true, latest.isCreditCard)
        assertEquals(BigDecimal("50000"), latest.creditLimit)
    }

    @Test
    fun `CREDIT transaction creates first balance entry`() = runTest {
        val balanceDao = FakeAccountBalanceDao()
        val useCase = createUseCase(balanceDao)

        useCase.execute(
            amount = BigDecimal("750"),
            merchant = "Coffee Shop",
            category = "Food",
            type = TransactionType.CREDIT,
            date = baseTime,
            bankName = testBank,
            accountLast4 = testLast4,
            currency = testCurrency
        )

        val latest = balanceDao.getLatestBalance(testBank, testLast4)
        assertNotNull(latest)
        assertEquals(BigDecimal("750"), latest!!.balance)
    }

    @Test
    fun `INCOME transaction adds to balance`() = runTest {
        val balanceDao = FakeAccountBalanceDao()
        val useCase = createUseCase(balanceDao)

        balanceDao.seedBalance(
            AccountBalanceEntity(
                bankName = testBank,
                accountLast4 = testLast4,
                balance = BigDecimal("10000"),
                timestamp = baseTime.minusDays(1)
            )
        )

        useCase.execute(
            amount = BigDecimal("5000"),
            merchant = "Salary",
            category = "Income",
            type = TransactionType.INCOME,
            date = baseTime,
            bankName = testBank,
            accountLast4 = testLast4,
            currency = testCurrency
        )

        val latest = balanceDao.getLatestBalance(testBank, testLast4)
        assertNotNull(latest)
        assertEquals(BigDecimal("15000"), latest!!.balance)
    }

    @Test
    fun `EXPENSE transaction subtracts from balance`() = runTest {
        val balanceDao = FakeAccountBalanceDao()
        val useCase = createUseCase(balanceDao)

        balanceDao.seedBalance(
            AccountBalanceEntity(
                bankName = testBank,
                accountLast4 = testLast4,
                balance = BigDecimal("10000"),
                timestamp = baseTime.minusDays(1)
            )
        )

        useCase.execute(
            amount = BigDecimal("3000"),
            merchant = "Rent",
            category = "Housing",
            type = TransactionType.EXPENSE,
            date = baseTime,
            bankName = testBank,
            accountLast4 = testLast4,
            currency = testCurrency
        )

        val latest = balanceDao.getLatestBalance(testBank, testLast4)
        assertNotNull(latest)
        assertEquals(BigDecimal("7000"), latest!!.balance)
    }

    @Test
    fun `INVESTMENT transaction subtracts from balance`() = runTest {
        val balanceDao = FakeAccountBalanceDao()
        val useCase = createUseCase(balanceDao)

        balanceDao.seedBalance(
            AccountBalanceEntity(
                bankName = testBank,
                accountLast4 = testLast4,
                balance = BigDecimal("50000"),
                timestamp = baseTime.minusDays(1)
            )
        )

        useCase.execute(
            amount = BigDecimal("10000"),
            merchant = "Mutual Fund SIP",
            category = "Investment",
            type = TransactionType.INVESTMENT,
            date = baseTime,
            bankName = testBank,
            accountLast4 = testLast4,
            currency = testCurrency
        )

        val latest = balanceDao.getLatestBalance(testBank, testLast4)
        assertNotNull(latest)
        assertEquals(BigDecimal("40000"), latest!!.balance)
    }

    @Test
    fun `CREDIT transaction preserves isCreditCard and creditLimit`() = runTest {
        val balanceDao = FakeAccountBalanceDao()
        val useCase = createUseCase(balanceDao)

        balanceDao.seedBalance(
            AccountBalanceEntity(
                bankName = testBank,
                accountLast4 = testLast4,
                balance = BigDecimal("5000"),
                timestamp = baseTime.minusDays(1),
                isCreditCard = true,
                creditLimit = BigDecimal("100000")
            )
        )

        useCase.execute(
            amount = BigDecimal("1500"),
            merchant = "Electronics",
            category = "Shopping",
            type = TransactionType.CREDIT,
            date = baseTime,
            bankName = testBank,
            accountLast4 = testLast4,
            currency = testCurrency
        )

        val latest = balanceDao.getLatestBalance(testBank, testLast4)
        assertEquals(BigDecimal("6500"), latest!!.balance)
        assertEquals(true, latest.isCreditCard)
        assertEquals(BigDecimal("100000"), latest.creditLimit)
    }

    @Test
    fun `CREDIT multiple transactions accumulate correctly`() = runTest {
        val balanceDao = FakeAccountBalanceDao()
        val useCase = createUseCase(balanceDao)

        balanceDao.seedBalance(
            AccountBalanceEntity(
                bankName = testBank,
                accountLast4 = testLast4,
                balance = BigDecimal.ZERO,
                timestamp = baseTime.minusDays(2),
                isCreditCard = true,
                creditLimit = BigDecimal("50000")
            )
        )

        useCase.execute(
            amount = BigDecimal("200"),
            merchant = "Lunch",
            category = "Food",
            type = TransactionType.CREDIT,
            date = baseTime.minusDays(1),
            bankName = testBank,
            accountLast4 = testLast4,
            currency = testCurrency
        )

        useCase.execute(
            amount = BigDecimal("350"),
            merchant = "Dinner",
            category = "Food",
            type = TransactionType.CREDIT,
            date = baseTime,
            bankName = testBank,
            accountLast4 = testLast4,
            currency = testCurrency
        )

        val latest = balanceDao.getLatestBalance(testBank, testLast4)
        assertEquals(BigDecimal("550"), latest!!.balance)
    }

    @Test
    fun `deleting CREDIT transaction reverses the balance`() = runTest {
        val balanceDao = FakeAccountBalanceDao()
        val (useCase, transactionRepo) = createUseCaseWithRepo(balanceDao)

        balanceDao.seedBalance(
            AccountBalanceEntity(
                bankName = testBank,
                accountLast4 = testLast4,
                balance = BigDecimal("25000"),
                timestamp = baseTime.minusDays(1),
                isCreditCard = true,
                creditLimit = BigDecimal("50000")
            )
        )

        useCase.execute(
            amount = BigDecimal("5000"),
            merchant = "Ice Cream",
            category = "Food",
            type = TransactionType.CREDIT,
            date = baseTime,
            bankName = testBank,
            accountLast4 = testLast4,
            currency = testCurrency
        )

        var latest = balanceDao.getLatestBalance(testBank, testLast4)
        assertEquals(BigDecimal("30000"), latest!!.balance)

        val transaction = transactionDao!!.insertedTransactions.last()
        transactionRepo.deleteTransaction(transaction)

        latest = balanceDao.getLatestBalance(testBank, testLast4)
        assertEquals(BigDecimal("25000"), latest!!.balance)
    }

    @Test
    fun `deleting EXPENSE transaction reverses the balance`() = runTest {
        val balanceDao = FakeAccountBalanceDao()
        val (useCase, transactionRepo) = createUseCaseWithRepo(balanceDao)

        balanceDao.seedBalance(
            AccountBalanceEntity(
                bankName = testBank,
                accountLast4 = testLast4,
                balance = BigDecimal("25000"),
                timestamp = baseTime.minusDays(1)
            )
        )

        useCase.execute(
            amount = BigDecimal("5000"),
            merchant = "Ice Cream",
            category = "Food",
            type = TransactionType.EXPENSE,
            date = baseTime,
            bankName = testBank,
            accountLast4 = testLast4,
            currency = testCurrency
        )

        var latest = balanceDao.getLatestBalance(testBank, testLast4)
        assertEquals(BigDecimal("20000"), latest!!.balance)

        val transaction = transactionDao!!.insertedTransactions.last()
        transactionRepo.deleteTransaction(transaction)

        latest = balanceDao.getLatestBalance(testBank, testLast4)
        assertEquals(BigDecimal("25000"), latest!!.balance)
    }

    @Test
    fun `undoing CREDIT delete re-applies the balance`() = runTest {
        val balanceDao = FakeAccountBalanceDao()
        val (useCase, transactionRepo) = createUseCaseWithRepo(balanceDao)

        balanceDao.seedBalance(
            AccountBalanceEntity(
                bankName = testBank,
                accountLast4 = testLast4,
                balance = BigDecimal("25000"),
                timestamp = baseTime.minusDays(1),
                isCreditCard = true,
                creditLimit = BigDecimal("50000")
            )
        )

        useCase.execute(
            amount = BigDecimal("5000"),
            merchant = "Ice Cream",
            category = "Food",
            type = TransactionType.CREDIT,
            date = baseTime,
            bankName = testBank,
            accountLast4 = testLast4,
            currency = testCurrency
        )

        val transaction = transactionDao!!.insertedTransactions.last()
        transactionRepo.deleteTransaction(transaction)

        var latest = balanceDao.getLatestBalance(testBank, testLast4)
        assertEquals(BigDecimal("25000"), latest!!.balance)

        transactionRepo.undoDeleteTransaction(
            transaction.copy(isDeleted = true)
        )

        latest = balanceDao.getLatestBalance(testBank, testLast4)
        assertEquals(BigDecimal("30000"), latest!!.balance)
    }

    private fun createUseCase(balanceDao: FakeAccountBalanceDao): AddTransactionUseCase {
        return createUseCaseWithRepo(balanceDao).first
    }

    private fun createUseCaseWithRepo(
        balanceDao: FakeAccountBalanceDao
    ): Pair<AddTransactionUseCase, TransactionRepository> {
        val context = ContextWrapper(null)
        val accountBalanceRepo = AccountBalanceRepository(balanceDao, context)
        val dao = FakeTransactionDao()
        transactionDao = dao
        val transactionRepo = TransactionRepository(dao, accountBalanceRepo)
        val subscriptionDao = FakeSubscriptionDao()
        val subscriptionRepo = SubscriptionRepository(subscriptionDao)
        return Pair(
            AddTransactionUseCase(transactionRepo, subscriptionRepo, accountBalanceRepo),
            transactionRepo
        )
    }

    private class FakeTransactionDao : TransactionDao {
        var insertedTransactions = mutableListOf<TransactionEntity>()
        private var nextId = 1L

        override suspend fun insertTransaction(transaction: TransactionEntity): Long {
            val id = nextId++
            insertedTransactions.add(transaction.copy(id = id))
            return id
        }

        override suspend fun getTransactionByHash(transactionHash: String): TransactionEntity? = null

        override fun getAllTransactions(): Flow<List<TransactionEntity>> = flowOf(emptyList())
        override fun getTransactionCount(): Flow<Int> = flowOf(0)
        override suspend fun getTransactionById(transactionId: Long): TransactionEntity? = null
        override fun getTransactionsBetweenDates(
            startDate: LocalDateTime, endDate: LocalDateTime
        ): Flow<List<TransactionEntity>> = flowOf(emptyList())
        override fun getTransactionsFiltered(
            startDate: LocalDateTime, endDate: LocalDateTime,
            currency: String, transactionType: TransactionType?
        ): Flow<List<TransactionEntity>> = flowOf(emptyList())
        override fun getTransactionsByType(type: TransactionType): Flow<List<TransactionEntity>> = flowOf(emptyList())
        override fun getTransactionsByCategory(category: String): Flow<List<TransactionEntity>> = flowOf(emptyList())
        override fun searchTransactions(searchQuery: String): Flow<List<TransactionEntity>> = flowOf(emptyList())
        override suspend fun searchTransactionsList(searchQuery: String): List<TransactionEntity> = emptyList()
        override fun getAllCategories(): Flow<List<String>> = flowOf(emptyList())
        override suspend fun getTopCategoriesByUsage(limit: Int): List<String> = emptyList()
        override fun getAllMerchants(): Flow<List<String>> = flowOf(emptyList())
        override suspend fun getTotalAmountByTypeAndPeriod(
            type: TransactionType, startDate: LocalDateTime, endDate: LocalDateTime
        ): Double? = null
        override suspend fun insertTransactions(transactions: List<TransactionEntity>) = Unit
        override suspend fun updateTransaction(transaction: TransactionEntity) = Unit
        override suspend fun deleteTransaction(transaction: TransactionEntity) = Unit
        override suspend fun deleteTransactionById(transactionId: Long) = Unit
        override suspend fun deleteAllTransactions() = Unit
        override suspend fun deleteSampleTransactions() = Unit
        override suspend fun updateCategoryForMerchant(merchantName: String, newCategory: String) = Unit
        override suspend fun updateCategoryAndSubcategoryForMerchantContains(
            merchantName: String, newCategory: String, newSubcategory: String?
        ) = Unit
        override suspend fun getTransactionCountForMerchant(merchantName: String, excludeId: Long): Int = 0
        override suspend fun getTransactionsByMerchantContains(
            merchantName: String, excludeId: Long
        ): List<TransactionEntity> = emptyList()
        override fun getAllCurrencies(): Flow<List<String>> = flowOf(emptyList())
        override fun getCurrenciesForPeriod(
            startDate: LocalDateTime, endDate: LocalDateTime
        ): Flow<List<String>> = flowOf(emptyList())
        override suspend fun softDeleteTransaction(transactionId: Long) = Unit
        override suspend fun softDeleteByHash(transactionHash: String) = Unit
        override suspend fun softDeleteTransactions(transactionIds: List<Long>) = Unit
        override suspend fun deleteTransactionsByIds(transactionIds: List<Long>) = Unit
        override suspend fun getTransactionsBetweenDatesList(
            startDate: LocalDateTime, endDate: LocalDateTime
        ): List<TransactionEntity> = emptyList()
        override fun getTransactionsByAccount(
            bankName: String, accountLast4: String
        ): Flow<List<TransactionEntity>> = flowOf(emptyList())
        override fun getTransactionsByAccountAndDateRange(
            bankName: String, accountLast4: String,
            startDate: LocalDateTime, endDate: LocalDateTime
        ): Flow<List<TransactionEntity>> = flowOf(emptyList())
        override suspend fun updateAccountForTransactions(
            oldBankName: String, oldAccountNumber: String,
            newBankName: String, newAccountNumber: String
        ) = Unit
        override suspend fun updateTransactionsCategory(
            oldCategory: String, newCategory: String, newSubcategory: String?
        ) = Unit
        override suspend fun getTransactionCountByCategory(category: String): Int = 0
        override suspend fun findPotentialDuplicates(
            startDate: LocalDateTime, endDate: LocalDateTime
        ): List<TransactionEntity> = emptyList()
        override suspend fun getTransactionsUpdatedBetween(
            updatedAfter: LocalDateTime, updatedBefore: LocalDateTime, currency: String
        ): List<TransactionEntity> = emptyList()
        override suspend fun getTransactionsBetweenDatesByCurrency(
            startDate: LocalDateTime, endDate: LocalDateTime, currency: String
        ): List<TransactionEntity> = emptyList()
    }

    private class FakeSubscriptionDao : SubscriptionDao {
        override suspend fun insertSubscription(subscription: com.ritesh.cashiro.data.database.entity.SubscriptionEntity): Long = 1
        override fun getAllSubscriptions(): Flow<List<com.ritesh.cashiro.data.database.entity.SubscriptionEntity>> = flowOf(emptyList())
        override fun getSubscriptionsByState(state: com.ritesh.cashiro.data.database.entity.SubscriptionState): Flow<List<com.ritesh.cashiro.data.database.entity.SubscriptionEntity>> = flowOf(emptyList())
        override fun getActiveSubscriptions(): Flow<List<com.ritesh.cashiro.data.database.entity.SubscriptionEntity>> = flowOf(emptyList())
        override fun getUpcomingSubscriptions(date: java.time.LocalDate): Flow<List<com.ritesh.cashiro.data.database.entity.SubscriptionEntity>> = flowOf(emptyList())
        override suspend fun getActiveSubscriptionByMerchant(merchantName: String): com.ritesh.cashiro.data.database.entity.SubscriptionEntity? = null
        override suspend fun getHiddenSubscriptionByMerchant(merchantName: String): com.ritesh.cashiro.data.database.entity.SubscriptionEntity? = null
        override suspend fun getSubscriptionByUmn(umn: String): com.ritesh.cashiro.data.database.entity.SubscriptionEntity? = null
        override suspend fun getSubscriptionByMerchantAmountAndDate(merchantName: String, amount: BigDecimal, paymentDate: java.time.LocalDate): com.ritesh.cashiro.data.database.entity.SubscriptionEntity? = null
        override suspend fun getSubscriptionByMerchantAndAmount(merchantName: String, amount: BigDecimal): com.ritesh.cashiro.data.database.entity.SubscriptionEntity? = null
        override suspend fun getSubscriptionById(id: Long): com.ritesh.cashiro.data.database.entity.SubscriptionEntity? = null
        override suspend fun updateSubscription(subscription: com.ritesh.cashiro.data.database.entity.SubscriptionEntity) = Unit
        override suspend fun updateSubscriptionState(id: Long, state: com.ritesh.cashiro.data.database.entity.SubscriptionState) = Unit
        override suspend fun updateNextPaymentDate(id: Long, nextPaymentDate: java.time.LocalDate) = Unit
        override suspend fun updatePaymentStatus(id: Long, nextPaymentDate: java.time.LocalDate, lastPaidDate: java.time.LocalDate?) = Unit
        override suspend fun deleteSubscription(subscription: com.ritesh.cashiro.data.database.entity.SubscriptionEntity) = Unit
        override suspend fun deleteSubscriptionById(id: Long) = Unit
        override suspend fun getSubscriptionsByStateList(state: com.ritesh.cashiro.data.database.entity.SubscriptionState): List<com.ritesh.cashiro.data.database.entity.SubscriptionEntity> = emptyList()
        override suspend fun deleteSampleSubscriptions() = Unit
        override suspend fun deleteAllSubscriptions() = Unit
    }

    private class FakeAccountBalanceDao : AccountBalanceDao {
        private val balances = mutableMapOf<Pair<String, String>, MutableList<AccountBalanceEntity>>()
        private var nextId = 1L

        fun seedBalance(entity: AccountBalanceEntity) {
            val key = Pair(entity.bankName, entity.accountLast4)
            balances.getOrPut(key) { mutableListOf() }.add(
                entity.copy(id = nextId++)
            )
        }

        override suspend fun insertBalance(balance: AccountBalanceEntity): Long {
            val key = Pair(balance.bankName, balance.accountLast4)
            val id = nextId++
            balances.getOrPut(key) { mutableListOf() }.add(balance.copy(id = id))
            return id
        }

        override suspend fun getLatestBalance(bankName: String, accountLast4: String): AccountBalanceEntity? {
            val key = Pair(bankName, accountLast4)
            return balances[key]?.maxByOrNull { it.timestamp }
        }

        override suspend fun getAccountLast4sEndingWith(bankName: String, suffix: String): List<String> = emptyList()
        override suspend fun getLatestBalanceOnOrBefore(
            bankName: String,
            accountLast4: String,
            timestamp: LocalDateTime
        ): AccountBalanceEntity? {
            val key = Pair(bankName, accountLast4)
            return balances[key]
                ?.filter { !it.timestamp.isAfter(timestamp) }
                ?.maxByOrNull { it.timestamp }
        }
        override suspend fun getBalancesAfterWithTransactions(
            bankName: String,
            accountLast4: String,
            timestamp: LocalDateTime
        ): List<AccountBalanceTransactionInfo> = emptyList()
        override fun getLatestBalanceFlow(bankName: String, accountLast4: String): Flow<AccountBalanceEntity?> = flowOf(null)
        override fun getAllLatestBalances(): Flow<List<AccountBalanceEntity>> = flowOf(emptyList())
        override fun getAllBalances(): Flow<List<AccountBalanceEntity>> = flowOf(emptyList())
        override suspend fun deleteAllBalances() = Unit
        override suspend fun deleteSampleBalances() = Unit
        override fun getCurrentMonthLatestBalances(): Flow<List<AccountBalanceEntity>> = flowOf(emptyList())
        override fun getTotalBalance(): Flow<BigDecimal?> = flowOf(BigDecimal.ZERO)
        override fun getBalanceHistory(bankName: String, accountLast4: String, startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<AccountBalanceEntity>> = flowOf(emptyList())
        override fun getAccountCount(): Flow<Int> = flowOf(0)
        override suspend fun deleteOldBalances(beforeDate: LocalDateTime): Int = 0
        override suspend fun updateBalance(balance: AccountBalanceEntity) = Unit
        override suspend fun deleteBalance(balance: AccountBalanceEntity) = Unit
        override suspend fun getBalanceHistoryForAccount(bankName: String, accountLast4: String): List<AccountBalanceEntity> = emptyList()
        override suspend fun deleteBalanceById(id: Long) = Unit
        override suspend fun updateBalanceById(id: Long, newBalance: BigDecimal) = Unit
        override suspend fun getBalanceCountForAccount(bankName: String, accountLast4: String): Int = balances[Pair(bankName, accountLast4)]?.size ?: 0
        override suspend fun deleteAccount(bankName: String, accountLast4: String): Int {
            val key = Pair(bankName, accountLast4)
            val size = balances[key]?.size ?: 0
            balances.remove(key)
            return size
        }
        override suspend fun updateAccountBankName(oldBankName: String, accountLast4: String, newBankName: String): Int = 0
        override suspend fun getAccountByLast4(accountLast4: String): AccountBalanceEntity? = null
    }
}
