package com.ritesh.cashiro.data.repository

import android.content.Context
import com.ritesh.cashiro.data.database.dao.AccountBalanceDao
import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import com.ritesh.cashiro.data.database.entity.TransactionType
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.time.LocalDateTime
import com.ritesh.cashiro.data.database.entity.TransactionEntity
import com.ritesh.parser.core.ParsedTransaction
import dagger.hilt.android.qualifiers.ApplicationContext
import com.ritesh.cashiro.utils.IconResolutionUtils
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountBalanceRepository @Inject constructor(
    private val accountBalanceDao: AccountBalanceDao,
    @ApplicationContext private val context: Context
) {
    private companion object {
        const val SOURCE_TRANSACTION_CALCULATED = "TRANSACTION_CALCULATED"
        const val SOURCE_TRANSACTION_SMS_BALANCE = "TRANSACTION_SMS_BALANCE"
        const val SOURCE_MANUAL = "MANUAL"
        const val SOURCE_MANUAL_EDIT = "MANUAL_EDIT"
        const val SOURCE_SMS_BALANCE = "SMS_BALANCE"
    }

    suspend fun insertBalance(balance: AccountBalanceEntity): Long {
        val balanceWithIconName = if (balance.iconName.isEmpty() && balance.iconResId != 0) {
            balance.copy(iconName = IconResolutionUtils.resIdToName(context, balance.iconResId))
        } else {
            balance
        }
        return accountBalanceDao.insertBalance(balanceWithIconName)
    }
    
    suspend fun getLatestBalance(bankName: String, accountLast4: String): AccountBalanceEntity? {
        return accountBalanceDao.getLatestBalance(bankName, accountLast4)
    }

    suspend fun resolveAccountLast4(bankName: String, accountLast4: String): String {
        if (accountLast4.isBlank()) {
            return accountLast4
        }

        if (!accountLast4.all { it.isDigit() }) {
            return accountLast4
        }

        if (accountLast4.length >= 4) {
            return accountLast4.takeLast(4)
        }

        val matches = accountBalanceDao.getAccountLast4sEndingWith(bankName, accountLast4)
        return if (matches.size == 1) matches.first() else accountLast4
    }

    suspend fun resolveEntityAccountNumber(
        entity: TransactionEntity,
        parsedTransaction: ParsedTransaction
    ): TransactionEntity {
        if (!parsedTransaction.isFromCard && entity.bankName != null && entity.accountNumber != null) {
            return entity.copy(
                accountNumber = resolveAccountLast4(entity.bankName, entity.accountNumber)
            )
        }
        return entity
    }

    fun getLatestBalanceFlow(bankName: String, accountLast4: String): Flow<AccountBalanceEntity?> {
        return accountBalanceDao.getLatestBalanceFlow(bankName, accountLast4)
    }
    
    fun getAllLatestBalances(): Flow<List<AccountBalanceEntity>> {
        return accountBalanceDao.getAllLatestBalances()
    }
    
    fun getTotalBalance(): Flow<BigDecimal?> {
        return accountBalanceDao.getTotalBalance()
    }
    
    fun getBalanceHistory(
        bankName: String,
        accountLast4: String,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Flow<List<AccountBalanceEntity>> {
        return accountBalanceDao.getBalanceHistory(bankName, accountLast4, startDate, endDate)
    }
    
    fun getAccountCount(): Flow<Int> {
        return accountBalanceDao.getAccountCount()
    }
    
    suspend fun deleteOldBalances(beforeDate: LocalDateTime): Int {
        return accountBalanceDao.deleteOldBalances(beforeDate)
    }
    
    suspend fun updateBalance(balance: AccountBalanceEntity) {
        accountBalanceDao.updateBalance(balance)
    }
    
    fun getAllBalances(): Flow<List<AccountBalanceEntity>> {
        return accountBalanceDao.getAllBalances()
    }

    suspend fun deleteBalance(balance: AccountBalanceEntity) {
        accountBalanceDao.deleteBalance(balance)
    }

    suspend fun insertBalanceFromTransaction(
        bankName: String?,
        accountLast4: String?,
        balance: BigDecimal?,
        creditLimit: BigDecimal? = null,
        timestamp: LocalDateTime,
        transactionId: Long?,
        isCreditCard: Boolean = false
    ) {
        if (bankName != null && accountLast4 != null && (balance != null || creditLimit != null)) {
            val latest = getLatestBalance(bankName, accountLast4)
            val balanceEntity = AccountBalanceEntity(
                bankName = bankName,
                accountLast4 = accountLast4,
                balance = balance ?: BigDecimal.ZERO,
                timestamp = timestamp,
                transactionId = transactionId,
                creditLimit = creditLimit ?: latest?.creditLimit,
                isCreditCard = isCreditCard || (latest?.isCreditCard ?: false),
                iconResId = latest?.iconResId ?: 0,
                iconName = latest?.iconName ?: "",
                isWallet = latest?.isWallet ?: false,
                color = latest?.color ?: "#33B5E5"
            )
            insertBalance(balanceEntity)
        }
    }

    suspend fun insertTransactionBalance(
        bankName: String,
        accountLast4: String,
        amount: BigDecimal,
        transactionType: TransactionType,
        explicitBalance: BigDecimal?,
        timestamp: LocalDateTime,
        transactionId: Long?,
        creditLimit: BigDecimal?,
        isCreditCard: Boolean,
        smsSource: String?,
        currency: String
    ): Long {
        val latest = getLatestBalance(bankName, accountLast4)
        val previous = accountBalanceDao.getLatestBalanceOnOrBefore(bankName, accountLast4, timestamp)
        val accountIsCreditCard = isCreditCard || (previous?.isCreditCard ?: latest?.isCreditCard ?: false)
        val newBalance = explicitBalance ?: calculateBalance(
            currentBalance = previous?.balance ?: BigDecimal.ZERO,
            amount = amount,
            transactionType = transactionType,
            isCreditCard = accountIsCreditCard
        )

        val balanceEntity = AccountBalanceEntity(
            bankName = bankName,
            accountLast4 = accountLast4,
            balance = newBalance,
            timestamp = timestamp,
            transactionId = transactionId,
            creditLimit = creditLimit ?: previous?.creditLimit ?: latest?.creditLimit,
            isCreditCard = accountIsCreditCard,
            smsSource = smsSource?.take(500),
            sourceType = if (explicitBalance != null) {
                SOURCE_TRANSACTION_SMS_BALANCE
            } else {
                SOURCE_TRANSACTION_CALCULATED
            },
            currency = currency,
            iconResId = previous?.iconResId ?: latest?.iconResId ?: 0,
            iconName = previous?.iconName ?: latest?.iconName ?: "",
            isWallet = previous?.isWallet ?: latest?.isWallet ?: false,
            color = previous?.color ?: latest?.color ?: "#33B5E5"
        )

        val balanceId = insertBalance(balanceEntity)
        recalculateBalancesAfter(bankName, accountLast4, timestamp, newBalance)
        return balanceId
    }

    private suspend fun recalculateBalancesAfter(
        bankName: String,
        accountLast4: String,
        timestamp: LocalDateTime,
        startingBalance: BigDecimal
    ) {
        var runningBalance = startingBalance
        accountBalanceDao.getBalancesAfterWithTransactions(bankName, accountLast4, timestamp).forEach { row ->
            val sourceType = row.sourceType
            val hasExplicitBalance = row.transactionBalanceAfter != null ||
                    sourceType == SOURCE_TRANSACTION_SMS_BALANCE ||
                    sourceType == SOURCE_SMS_BALANCE ||
                    sourceType == SOURCE_MANUAL ||
                    sourceType == SOURCE_MANUAL_EDIT

            if (hasExplicitBalance || row.transactionId == null) {
                runningBalance = row.balance
                return@forEach
            }

            val amount = row.transactionAmount
            val transactionType = row.transactionType?.let { runCatching { TransactionType.valueOf(it) }.getOrNull() }
            if (amount == null || transactionType == null) {
                runningBalance = row.balance
                return@forEach
            }

            val recalculated = calculateBalance(
                currentBalance = runningBalance,
                amount = amount,
                transactionType = transactionType,
                isCreditCard = row.isCreditCard
            )

            if (recalculated != row.balance) {
                accountBalanceDao.updateBalanceById(row.id, recalculated)
            }
            runningBalance = recalculated
        }
    }

    private fun calculateBalance(
        currentBalance: BigDecimal,
        amount: BigDecimal,
        transactionType: TransactionType,
        isCreditCard: Boolean
    ): BigDecimal {
        return when {
            isCreditCard && transactionType == TransactionType.INCOME ->
                (currentBalance - amount).max(BigDecimal.ZERO)
            isCreditCard -> currentBalance + amount
            transactionType == TransactionType.INCOME -> currentBalance + amount
            transactionType == TransactionType.EXPENSE || transactionType == TransactionType.INVESTMENT ->
                (currentBalance - amount).max(BigDecimal.ZERO)
            else -> currentBalance
        }
    }

    suspend fun insertBalanceUpdate(
        bankName: String,
        accountLast4: String,
        balance: BigDecimal,
        timestamp: LocalDateTime,
        smsSource: String? = null,
        sourceType: String? = null,
        currency: String = "INR"
    ): Long {
        val latest = getLatestBalance(bankName, accountLast4)
        val balanceEntity = AccountBalanceEntity(
            bankName = bankName,
            accountLast4 = accountLast4,
            balance = balance,
            timestamp = timestamp,
            transactionId = null,
            smsSource = smsSource?.take(500),  // Limit to 500 chars
            sourceType = sourceType,
            currency = currency,
            iconResId = latest?.iconResId ?: 0,
            iconName = latest?.iconName ?: "",
            isWallet = latest?.isWallet ?: false,
            isCreditCard = latest?.isCreditCard ?: false,
            creditLimit = latest?.creditLimit,
            color = latest?.color ?: "#33B5E5"
        )
        return insertBalance(balanceEntity)
    }
    
    suspend fun getBalanceHistoryForAccount(bankName: String, accountLast4: String): List<AccountBalanceEntity> {
        return accountBalanceDao.getBalanceHistoryForAccount(bankName, accountLast4)
    }
    
    suspend fun deleteBalanceById(id: Long) {
        accountBalanceDao.deleteBalanceById(id)
    }
    
    suspend fun updateBalanceById(id: Long, newBalance: BigDecimal) {
        accountBalanceDao.updateBalanceById(id, newBalance)
    }
    
    suspend fun getBalanceCountForAccount(bankName: String, accountLast4: String): Int {
        return accountBalanceDao.getBalanceCountForAccount(bankName, accountLast4)
    }

    suspend fun deleteAccount(bankName: String, accountLast4: String): Int {
        return accountBalanceDao.deleteAccount(bankName, accountLast4)
    }

    suspend fun updateAccountBankName(oldBankName: String, accountLast4: String, newBankName: String): Int {
        return accountBalanceDao.updateAccountBankName(oldBankName, accountLast4, newBankName)
    }

    suspend fun deleteAllBalances() {
        accountBalanceDao.deleteAllBalances()
    }

    suspend fun deleteSampleBalances() {
        accountBalanceDao.deleteSampleBalances()
    }

    suspend fun getAccountByLast4(accountLast4: String): AccountBalanceEntity? {
        return accountBalanceDao.getAccountByLast4(accountLast4)
    }
}
