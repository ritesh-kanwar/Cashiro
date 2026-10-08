package com.ritesh.cashiro.data.repository

import com.ritesh.cashiro.data.currency.CurrencyConversionService
import com.ritesh.cashiro.data.database.dao.LendBorrowDao
import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import com.ritesh.cashiro.data.database.entity.LendBorrowPersonEntity
import com.ritesh.cashiro.data.database.entity.LendBorrowTransactionEntity
import com.ritesh.cashiro.data.database.entity.LendBorrowType
import com.ritesh.cashiro.data.database.entity.TransactionEntity
import com.ritesh.cashiro.data.database.entity.TransactionType
import com.ritesh.cashiro.domain.model.LendBorrowPerson
import com.ritesh.cashiro.domain.model.LendBorrowSummary
import com.ritesh.cashiro.domain.model.LendBorrowTransactionItem
import com.ritesh.cashiro.domain.model.PersonCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LendBorrowRepository @Inject constructor(
    private val lendBorrowDao: LendBorrowDao,
    private val transactionRepository: TransactionRepository,
    private val currencyRepository: CurrencyRepository,
    private val currencyConversionService: CurrencyConversionService,
    private val accountBalanceRepository: AccountBalanceRepository
) {

    fun getPersons(): Flow<List<LendBorrowPerson>> {
        return combine(
            lendBorrowDao.getAllPersons(),
            lendBorrowDao.getAllTransactions(),
            currencyRepository.effectiveBaseCurrencyCode
        ) { persons, transactions, baseCurrency ->
            val now = LocalDateTime.now()
            persons.map { person ->
                val personTxns = transactions.filter { it.personId == person.id }

                var totalLent = BigDecimal.ZERO
                var totalBorrowed = BigDecimal.ZERO
                var totalSettledLent = BigDecimal.ZERO
                var totalSettledBorrowed = BigDecimal.ZERO
                var hasOverdue = false
                var activeTxCount = 0

                personTxns.forEach { tx ->
                    if (!tx.isSettled) {
                        activeTxCount++
                        if (tx.dueDate != null && tx.dueDate.isBefore(now)) {
                            hasOverdue = true
                        }
                    }
                    val convertedAmount = if (tx.currency == baseCurrency) {
                        tx.amount
                    } else {
                        currencyConversionService.convertAmountOrNull(
                            amount = tx.amount,
                            fromCurrency = tx.currency,
                            toCurrency = baseCurrency
                        ) ?: return@forEach
                    }

                    when (tx.type) {
                        LendBorrowType.LENT -> totalLent += convertedAmount
                        LendBorrowType.BORROWED -> totalBorrowed += convertedAmount
                        LendBorrowType.SETTLEMENT_LENT -> totalSettledLent += convertedAmount
                        LendBorrowType.SETTLEMENT_BORROWED -> totalSettledBorrowed += convertedAmount
                    }
                }

                // Remaining Lent = total LENT - total SETTLEMENT_LENT
                val remainingLent = (totalLent - totalSettledLent).coerceAtLeast(BigDecimal.ZERO)
                // Remaining Borrowed = total BORROWED - total SETTLEMENT_BORROWED
                val remainingBorrowed = (totalBorrowed - totalSettledBorrowed).coerceAtLeast(BigDecimal.ZERO)
                // Net balance: positive = they owe you, negative = you owe them
                val netBalance = remainingLent - remainingBorrowed

                LendBorrowPerson(
                    id = person.id,
                    name = person.name,
                    phoneNumber = person.phoneNumber,
                    notes = person.notes,
                    color = person.color,
                    avatar = person.avatar,
                    category = person.category?.let { cat ->
                        runCatching { PersonCategory.valueOf(cat) }.getOrNull()
                    },
                    isArchived = person.isArchived,
                    createdAt = person.createdAt,
                    updatedAt = person.updatedAt,
                    totalLent = remainingLent,
                    totalBorrowed = remainingBorrowed,
                    netBalance = netBalance,
                    activeTransactionsCount = activeTxCount,
                    hasOverdue = hasOverdue
                )
            }
        }
    }

    fun getPersonById(personId: Long): Flow<LendBorrowPerson?> {
        return getPersons().map { persons ->
            persons.find { it.id == personId }
        }
    }

    fun getSummary(): Flow<LendBorrowSummary> {
        return getPersons().map { persons ->
            val activePersons = persons.filter { !it.isArchived }
            var totalLentRemaining = BigDecimal.ZERO
            var totalBorrowedRemaining = BigDecimal.ZERO
            val lentPersonsList = mutableListOf<LendBorrowPerson>()
            val borrowedPersonsList = mutableListOf<LendBorrowPerson>()
            var overdueCount = 0

            activePersons.forEach { person ->
                totalLentRemaining += person.totalLent
                totalBorrowedRemaining += person.totalBorrowed
                if (person.totalLent > BigDecimal.ZERO) lentPersonsList.add(person)
                if (person.totalBorrowed > BigDecimal.ZERO) borrowedPersonsList.add(person)
                if (person.hasOverdue) {
                    overdueCount++
                }
            }

            val netBalance = totalLentRemaining - totalBorrowedRemaining

            LendBorrowSummary(
                totalLentRemaining = totalLentRemaining,
                totalBorrowedRemaining = totalBorrowedRemaining,
                netBalance = netBalance,
                activePersonsCount = activePersons.size,
                lentPersonsCount = lentPersonsList.size,
                borrowedPersonsCount = borrowedPersonsList.size,
                overdueCount = overdueCount,
                lentPersons = lentPersonsList.sortedByDescending { it.totalLent },
                borrowedPersons = borrowedPersonsList.sortedByDescending { it.totalBorrowed }
            )
        }
    }

    fun getTransactionsForPerson(personId: Long): Flow<List<LendBorrowTransactionItem>> {
        return combine(
            lendBorrowDao.getTransactionsForPerson(personId),
            currencyRepository.effectiveBaseCurrencyCode
        ) { list, baseCurrency ->
            list.map { entity -> entity.toDomain(baseCurrency) }
        }
    }

    fun getAllTransactions(): Flow<List<LendBorrowTransactionItem>> {
        return combine(
            lendBorrowDao.getAllTransactions(),
            currencyRepository.effectiveBaseCurrencyCode
        ) { list, baseCurrency ->
            list.map { entity -> entity.toDomain(baseCurrency) }
        }
    }

    /**
     * Looks up the lend/borrow entry linked to an existing wallet transaction
     * (i.e. a transaction that was "marked as a loan"). Returns null when no
     * entry is linked.
     */
    suspend fun getEntryForTransaction(transactionId: Long): LendBorrowTransactionItem? {
        val entry = lendBorrowDao.getTransactionByWalletId(transactionId) ?: return null
        val baseCurrency = currencyRepository.effectiveBaseCurrencyCode.first()
        return entry.toDomain(baseCurrency)
    }

    /**
     * Links an existing wallet transaction to a new lend/borrow entry. Unlike
     * [insertTransaction], no new wallet transaction is created and no account
     * balance is adjusted — the wallet transaction already represents the real
     * money movement; this only records the loan ledger entry.
     */
    suspend fun createEntryFromTransaction(
        transactionId: Long,
        personId: Long,
        type: LendBorrowType,
        amount: BigDecimal,
        currency: String,
        title: String,
        dueDate: LocalDateTime? = null,
        date: LocalDateTime = LocalDateTime.now(),
        accountId: Long? = null,
        category: String? = null,
        merchant: String? = null,
        attachments: List<String> = emptyList()
    ): Long {
        val entry = LendBorrowTransactionEntity(
            personId = personId,
            type = type,
            amount = amount,
            currency = currency,
            title = title,
            dueDate = dueDate,
            date = date,
            transactionId = transactionId,
            accountId = accountId,
            category = category,
            merchant = merchant,
            attachments = attachments
        )
        return lendBorrowDao.insertTransaction(entry)
    }

    /**
     * Removes the lend/borrow entry linked to a wallet transaction WITHOUT
     * deleting the underlying wallet transaction itself (the inverse of
     * "mark as loan" / unmark).
     */
    suspend fun deleteEntryForTransaction(transactionId: Long) {
        val entry = lendBorrowDao.getTransactionByWalletId(transactionId) ?: return
        lendBorrowDao.deleteTransactionById(entry.id)
    }

    /** Resolves the display name of a person for UI, if the person still exists. */
    suspend fun getPersonName(personId: Long): String? =
        lendBorrowDao.getPersonByIdSync(personId)?.name

    suspend fun insertPerson(person: LendBorrowPersonEntity): Long {
        return lendBorrowDao.insertPerson(person)
    }

    suspend fun updatePerson(person: LendBorrowPersonEntity) {
        lendBorrowDao.updatePerson(person.copy(updatedAt = LocalDateTime.now()))
    }

    suspend fun deletePerson(personId: Long) {
        lendBorrowDao.getTransactionsForPersonSync(personId).forEach { entry ->
            entry.transactionId?.let { linkedId ->
                transactionRepository.deleteTransactionById(linkedId, hardDelete = true)
            }
        }
        lendBorrowDao.deletePersonById(personId)
    }

    suspend fun insertTransaction(transaction: LendBorrowTransactionEntity): Long {
        val personName = lendBorrowDao.getPersonByIdSync(transaction.personId)?.name ?: transaction.title
        var entry = transaction
        if (entry.transactionId == null) {
            val currency = currencyRepository.effectiveBaseCurrencyCode.first()
            val account = entry.accountId?.let { accountBalanceRepository.getBalanceById(it) }
            // The amount is recorded in the account's currency when one is linked,
            // otherwise the effective base currency at entry time.
            val recordCurrency = account?.currency ?: currency
            entry = entry.copy(currency = recordCurrency)
            val walletId = transactionRepository.insertTransaction(
                buildWalletTransaction(entry, personName, currency, account)
            )
            entry = entry.copy(transactionId = walletId)
            if (account != null) {
                accountBalanceRepository.insertTransactionBalance(
                    bankName = account.bankName,
                    accountLast4 = account.accountLast4,
                    amount = entry.amount,
                    transactionType = walletTransactionType(entry.type),
                    explicitBalance = null,
                    timestamp = entry.date,
                    transactionId = walletId,
                    creditLimit = null,
                    isCreditCard = account.isCreditCard,
                    smsSource = null,
                    currency = currency
                )
            }
        }
        return lendBorrowDao.insertTransaction(entry)
    }

    suspend fun updateTransaction(transaction: LendBorrowTransactionEntity) {
        val existing = lendBorrowDao.getTransactionById(transaction.id) ?: return
        val personName = lendBorrowDao.getPersonByIdSync(transaction.personId)?.name ?: transaction.title
        val linkedId = transaction.transactionId ?: existing.transactionId
        var entry = transaction.copy(transactionId = linkedId)

        if (linkedId != null) {
            val wallet = transactionRepository.getTransactionById(linkedId)
            if (wallet != null) {
                val walletType = walletTransactionType(entry.type)
                transactionRepository.updateTransaction(
                    wallet.copy(
                        amount = entry.amount,
                        merchantName = entry.merchant ?: personName,
                        category = entry.category ?: walletCategory(walletType),
                        transactionType = walletType,
                        dateTime = entry.date,
                        description = entry.title,
                        updatedAt = LocalDateTime.now(),
                        bankName = if (entry.accountId != null) "" else wallet.bankName,
                        attachments = entry.attachments.joinToString(",")
                    )
                )
            }
        } else {
            val currency = currencyRepository.effectiveBaseCurrencyCode.first()
            val account = entry.accountId?.let { accountBalanceRepository.getBalanceById(it) }
            val recordCurrency = account?.currency ?: currency
            entry = entry.copy(currency = recordCurrency)
            val walletId = transactionRepository.insertTransaction(buildWalletTransaction(entry, personName, recordCurrency, account))
            entry = entry.copy(transactionId = walletId)
            if (account != null) {
                accountBalanceRepository.insertTransactionBalance(
                    bankName = account.bankName,
                    accountLast4 = account.accountLast4,
                    amount = entry.amount,
                    transactionType = walletTransactionType(entry.type),
                    explicitBalance = null,
                    timestamp = entry.date,
                    transactionId = walletId,
                    creditLimit = null,
                    isCreditCard = account.isCreditCard,
                    smsSource = null,
                    currency = recordCurrency
                )
            }
        }

        lendBorrowDao.updateTransaction(entry.copy(updatedAt = LocalDateTime.now()))
    }

    suspend fun deleteTransaction(transactionId: Long) {
        val entry = lendBorrowDao.getTransactionById(transactionId) ?: return
        entry.transactionId?.let { linkedId ->
            transactionRepository.deleteTransactionById(linkedId, hardDelete = true)
        }
        lendBorrowDao.deleteTransactionById(transactionId)
    }

    suspend fun settlePerson(
        personId: Long,
        amount: BigDecimal,
        title: String = "Settlement",
        isLentSettlement: Boolean,
        accountId: Long? = null
    ): Long {
        val settlementType = if (isLentSettlement) LendBorrowType.SETTLEMENT_LENT else LendBorrowType.SETTLEMENT_BORROWED
        val entry = LendBorrowTransactionEntity(
            personId = personId,
            type = settlementType,
            amount = amount,
            title = title,
            isSettled = true,
            date = LocalDateTime.now(),
            accountId = accountId,
            merchant = title
        )
        return insertTransaction(entry)
    }

    /**
     * Maps a Khata entry type to a wallet transaction type so ledger activity
     * reflects real money movement: lending/paying back is money out (EXPENSE),
     * borrowing/being repaid is money in (INCOME).
     */
    private fun walletTransactionType(type: LendBorrowType): TransactionType =
        when (type) {
            LendBorrowType.LENT, LendBorrowType.SETTLEMENT_BORROWED -> TransactionType.EXPENSE
            LendBorrowType.BORROWED, LendBorrowType.SETTLEMENT_LENT -> TransactionType.INCOME
        }

    private fun walletCategory(type: TransactionType): String =
        if (type == TransactionType.INCOME) "Income" else "Miscellaneous"

    private fun buildWalletTransaction(
        entry: LendBorrowTransactionEntity,
        personName: String,
        currency: String,
        account: AccountBalanceEntity? = null
    ): TransactionEntity {
        val type = walletTransactionType(entry.type)
        return TransactionEntity(
            amount = entry.amount,
            merchantName = entry.merchant ?: personName,
            category = entry.category ?: walletCategory(type),
            transactionType = type,
            dateTime = entry.date,
            description = entry.title,
            transactionHash = UUID.randomUUID().toString(),
            currency = currency,
            bankName = account?.bankName ?: (if (entry.accountId != null) "Linked Account" else "Manual Entry"),
            accountNumber = account?.accountLast4 ?: (if (entry.accountId != null) "linked" else null),
            isSample = entry.isSample,
            attachments = entry.attachments.joinToString(",")
        )
    }

    private suspend fun LendBorrowTransactionEntity.toDomain(baseCurrency: String): LendBorrowTransactionItem {
        val converted = currencyConversionService.convertAmountOrNull(amount, currency, baseCurrency)
        return LendBorrowTransactionItem(
            id = id,
            personId = personId,
            transactionId = transactionId,
            type = type,
            amount = converted ?: amount,
            originalAmount = amount,
            currency = if (converted != null) baseCurrency else currency,
            title = title,
            dueDate = dueDate,
            isSettled = isSettled,
            date = date,
            createdAt = createdAt,
            updatedAt = updatedAt,
            isSample = isSample,
            accountId = accountId,
            category = category,
            merchant = merchant,
            attachments = attachments
        )
    }
}
