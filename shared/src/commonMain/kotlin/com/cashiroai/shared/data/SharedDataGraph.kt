package com.cashiroai.shared.data

import com.cashiroai.shared.data.bootstrap.SharedDataInitializer
import com.cashiroai.shared.data.local.SharedDatabase
import com.cashiroai.shared.data.local.SharedDatabaseFactory
import com.cashiroai.shared.data.repository.RoomSharedAccountRepository
import com.cashiroai.shared.data.repository.RoomSharedBudgetRepository
import com.cashiroai.shared.data.repository.RoomSharedCategoryRepository
import com.cashiroai.shared.data.repository.RoomSharedExchangeRateRepository
import com.cashiroai.shared.data.repository.RoomSharedMerchantMappingRepository
import com.cashiroai.shared.data.repository.RoomSharedRuleRepository
import com.cashiroai.shared.data.repository.RoomSharedSplitRepository
import com.cashiroai.shared.data.repository.RoomSharedSubscriptionRepository
import com.cashiroai.shared.data.repository.RoomSharedTransactionRepository
import com.cashiroai.shared.data.repository.RoomSharedUnrecognizedSmsRepository
import com.cashiroai.shared.data.repository.SharedAccountRepository
import com.cashiroai.shared.data.repository.SharedBudgetRepository
import com.cashiroai.shared.data.repository.SharedCategoryRepository
import com.cashiroai.shared.data.repository.SharedExchangeRateRepository
import com.cashiroai.shared.data.repository.SharedMerchantMappingRepository
import com.cashiroai.shared.data.repository.SharedRuleRepository
import com.cashiroai.shared.data.repository.SharedSplitRepository
import com.cashiroai.shared.data.repository.SharedSubscriptionRepository
import com.cashiroai.shared.data.repository.SharedTransactionRepository
import com.cashiroai.shared.data.repository.SharedUnrecognizedSmsRepository

class SharedDataGraph private constructor(
    val database: SharedDatabase,
    val transactionRepository: SharedTransactionRepository,
    val categoryRepository: SharedCategoryRepository,
    val subscriptionRepository: SharedSubscriptionRepository,
    val accountRepository: SharedAccountRepository,
    val splitRepository: SharedSplitRepository,
    val merchantMappingRepository: SharedMerchantMappingRepository,
    val ruleRepository: SharedRuleRepository,
    val exchangeRateRepository: SharedExchangeRateRepository,
    val budgetRepository: SharedBudgetRepository,
    val unrecognizedSmsRepository: SharedUnrecognizedSmsRepository
) {
    private val initializer = SharedDataInitializer(categoryRepository)

    suspend fun initialize() {
        initializer.seedDefaultCategoriesIfNeeded()
    }

    companion object {
        fun create(factory: SharedDatabaseFactory = SharedDatabaseFactory()): SharedDataGraph {
            val database = factory.createDatabase()
            return SharedDataGraph(
                database = database,
                transactionRepository = RoomSharedTransactionRepository(database.transactionDao()),
                categoryRepository = RoomSharedCategoryRepository(database.categoryDao()),
                subscriptionRepository = RoomSharedSubscriptionRepository(database.subscriptionDao()),
                accountRepository = RoomSharedAccountRepository(database.accountBalanceDao(), database.cardDao()),
                splitRepository = RoomSharedSplitRepository(database.transactionSplitDao()),
                merchantMappingRepository = RoomSharedMerchantMappingRepository(database.merchantMappingDao()),
                ruleRepository = RoomSharedRuleRepository(database.ruleDao(), database.ruleApplicationDao()),
                exchangeRateRepository = RoomSharedExchangeRateRepository(database.exchangeRateDao()),
                budgetRepository = RoomSharedBudgetRepository(database.budgetDao(), database.categoryBudgetLimitDao()),
                unrecognizedSmsRepository = RoomSharedUnrecognizedSmsRepository(database.unrecognizedSmsDao())
            )
        }
    }
}
