package com.ritesh.cashiro.data.database.dao

import androidx.room.*
import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import com.ritesh.cashiro.data.database.entity.TransactionType
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.time.LocalDateTime

private const val SOURCE_TRANSACTION_CALCULATED = "TRANSACTION_CALCULATED"
private const val SOURCE_TRANSACTION_SMS_BALANCE = "TRANSACTION_SMS_BALANCE"
private const val SOURCE_MANUAL = "MANUAL"
private const val SOURCE_MANUAL_EDIT = "MANUAL_EDIT"
private const val SOURCE_SMS_BALANCE = "SMS_BALANCE"

@Dao
interface AccountBalanceDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBalance(balance: AccountBalanceEntity): Long
    
    @Query("""
        SELECT * FROM account_balances 
        WHERE bank_name = :bankName AND account_last4 = :accountLast4
        ORDER BY timestamp DESC
        LIMIT 1
    """)
    suspend fun getLatestBalance(bankName: String, accountLast4: String): AccountBalanceEntity?

    @Query("""
        SELECT DISTINCT account_last4 FROM account_balances
        WHERE bank_name = :bankName
        AND LENGTH(account_last4) >= 4
        AND account_last4 LIKE '%' || :suffix
    """)
    suspend fun getAccountLast4sEndingWith(bankName: String, suffix: String): List<String>

    @Query("""
        SELECT * FROM account_balances
        WHERE bank_name = :bankName AND account_last4 = :accountLast4
        AND timestamp <= :timestamp
        ORDER BY timestamp DESC, id DESC
        LIMIT 1
    """)
    suspend fun getLatestBalanceOnOrBefore(
        bankName: String,
        accountLast4: String,
        timestamp: LocalDateTime
    ): AccountBalanceEntity?

    @Query("""
        SELECT
            ab.id AS id,
            ab.balance AS balance,
            ab.source_type AS sourceType,
            ab.is_credit_card AS isCreditCard,
            ab.transaction_id AS transactionId,
            t.amount AS transactionAmount,
            t.transaction_type AS transactionType,
            t.balance_after AS transactionBalanceAfter
        FROM account_balances ab
        LEFT JOIN transactions t ON t.id = ab.transaction_id
        WHERE ab.bank_name = :bankName AND ab.account_last4 = :accountLast4
        AND ab.timestamp > :timestamp
        ORDER BY ab.timestamp ASC, ab.id ASC
    """)
    suspend fun getBalancesAfterWithTransactions(
        bankName: String,
        accountLast4: String,
        timestamp: LocalDateTime
    ): List<AccountBalanceTransactionInfo>

    /**
     * Inserts a balance entry linked to a transaction, and sequentially recalculates succeeding balances.
     *
     * @param bankName The name of the bank.
     * @param accountLast4 The last 4 digits of the account number.
     * @param amount The transaction amount.
     * @param transactionType The transaction type.
     * @param explicitBalance The bank-reported explicit balance (if any).
     * @param timestamp The transaction timestamp.
     * @param transactionId The associated transaction ID.
     * @param creditLimit Optionally, a custom credit limit parsed from SMS.
     * @param isCreditCard Whether this account is a credit card.
     * @param smsSource Sanitized SMS snippet source.
     * @param currency The transaction currency.
     */
    @Transaction
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
        val previous = getLatestBalanceOnOrBefore(bankName, accountLast4, timestamp)
        val accountIsCreditCard = isCreditCard || (previous?.isCreditCard ?: latest?.isCreditCard ?: false)
        val newBalance = explicitBalance ?: calculateTransactionBalance(
            currentBalance = previous?.balance ?: BigDecimal.ZERO,
            amount = amount,
            transactionType = transactionType,
            isCreditCard = accountIsCreditCard
        )

        val balanceId = insertBalance(
            AccountBalanceEntity(
                bankName = bankName,
                accountLast4 = accountLast4,
                balance = newBalance,
                timestamp = timestamp,
                transactionId = transactionId,
                creditLimit = if (accountIsCreditCard) {
                    creditLimit?.add(newBalance) ?: previous?.creditLimit ?: latest?.creditLimit
                } else {
                    previous?.creditLimit ?: latest?.creditLimit
                },
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
        )

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
        // synthesised: use regular for loop to support breaking when boundaries are hit
        for (row in getBalancesAfterWithTransactions(bankName, accountLast4, timestamp)) {
            val sourceType = row.sourceType
            val hasExplicitBalance = row.transactionBalanceAfter != null ||
                    sourceType == SOURCE_TRANSACTION_SMS_BALANCE ||
                    sourceType == SOURCE_SMS_BALANCE ||
                    sourceType == SOURCE_MANUAL ||
                    sourceType == SOURCE_MANUAL_EDIT

            // synthesised: stop recalculation when encountering explicit balance or manual boundaries
            if (hasExplicitBalance || row.transactionId == null) {
                break
            }

            val amount = row.transactionAmount
            val transactionType = row.transactionType?.let { runCatching { TransactionType.valueOf(it) }.getOrNull() }
            if (amount == null || transactionType == null) {
                break
            }

            val recalculated = calculateTransactionBalance(
                currentBalance = runningBalance,
                amount = amount,
                transactionType = transactionType,
                isCreditCard = row.isCreditCard
            )

            if (recalculated != row.balance) {
                updateBalanceById(row.id, recalculated)
            }
            runningBalance = recalculated
        }
    }
    
    @Query("""
        SELECT * FROM account_balances 
        WHERE bank_name = :bankName AND account_last4 = :accountLast4
        ORDER BY timestamp DESC
        LIMIT 1
    """)
    fun getLatestBalanceFlow(bankName: String, accountLast4: String): Flow<AccountBalanceEntity?>
    
    @Query("""
        SELECT DISTINCT 
            ab1.id,
            ab1.bank_name,
            ab1.account_last4,
            ab1.balance,
            ab1.timestamp,
            ab1.transaction_id,
            ab1.created_at,
            ab1.credit_limit,
            ab1.is_credit_card,
            ab1.sms_source,
            ab1.source_type,
            ab1.currency,
            ab1.icon_res_id,
            ab1.icon_name,
            ab1.is_wallet,
            ab1.color,
            ab1.is_sample
        FROM account_balances ab1
        INNER JOIN (
            SELECT bank_name, account_last4, MAX(timestamp) as max_timestamp
            FROM account_balances
            GROUP BY bank_name, account_last4
        ) ab2 
        ON ab1.bank_name = ab2.bank_name 
        AND ab1.account_last4 = ab2.account_last4 
        AND ab1.timestamp = ab2.max_timestamp
        ORDER BY ab1.balance DESC
    """)
    fun getAllLatestBalances(): Flow<List<AccountBalanceEntity>>
    
    @Query("SELECT * FROM account_balances ORDER BY timestamp DESC")
    fun getAllBalances(): Flow<List<AccountBalanceEntity>>
    
    @Query("DELETE FROM account_balances")
    suspend fun deleteAllBalances()
    
    @Query("DELETE FROM account_balances WHERE is_sample = 1")
    suspend fun deleteSampleBalances()
    
    @Query("""
        SELECT DISTINCT 
            ab1.id,
            ab1.bank_name,
            ab1.account_last4,
            ab1.balance,
            ab1.timestamp,
            ab1.transaction_id,
            ab1.created_at,
            ab1.credit_limit,
            ab1.is_credit_card,
            ab1.sms_source,
            ab1.source_type,
            ab1.currency,
            ab1.icon_res_id,
            ab1.icon_name,
            ab1.is_wallet,
            ab1.color,
            ab1.is_sample
        FROM account_balances ab1
        INNER JOIN (
            SELECT bank_name, account_last4, MAX(timestamp) as max_timestamp
            FROM account_balances
            WHERE strftime('%Y-%m', timestamp/1000, 'unixepoch') = strftime('%Y-%m', 'now')
            GROUP BY bank_name, account_last4
        ) ab2 
        ON ab1.bank_name = ab2.bank_name 
        AND ab1.account_last4 = ab2.account_last4 
        AND ab1.timestamp = ab2.max_timestamp
        ORDER BY ab1.balance DESC
    """)
    fun getCurrentMonthLatestBalances(): Flow<List<AccountBalanceEntity>>
    
    @Query("""
        SELECT SUM(balance) as total FROM (
            SELECT DISTINCT 
                ab1.balance
            FROM account_balances ab1
            INNER JOIN (
                SELECT bank_name, account_last4, MAX(timestamp) as max_timestamp
                FROM account_balances
                GROUP BY bank_name, account_last4
            ) ab2 
            ON ab1.bank_name = ab2.bank_name 
            AND ab1.account_last4 = ab2.account_last4 
            AND ab1.timestamp = ab2.max_timestamp
        )
    """)
    fun getTotalBalance(): Flow<BigDecimal?>
    
    @Query("""
        SELECT * FROM account_balances
        WHERE bank_name = :bankName AND account_last4 = :accountLast4
        AND timestamp >= :startDate AND timestamp <= :endDate
        ORDER BY timestamp DESC
    """)
    fun getBalanceHistory(
        bankName: String,
        accountLast4: String,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Flow<List<AccountBalanceEntity>>
    
    @Query("""
        SELECT COUNT(DISTINCT bank_name || account_last4) FROM account_balances
    """)
    fun getAccountCount(): Flow<Int>
    
    @Query("DELETE FROM account_balances WHERE timestamp < :beforeDate")
    suspend fun deleteOldBalances(beforeDate: LocalDateTime): Int
    
    @Update
    suspend fun updateBalance(balance: AccountBalanceEntity)
    
    @Delete
    suspend fun deleteBalance(balance: AccountBalanceEntity)
    
    @Query("""SELECT * FROM account_balances 
        WHERE bank_name = :bankName AND account_last4 = :accountLast4
        ORDER BY timestamp DESC""")
    suspend fun getBalanceHistoryForAccount(bankName: String, accountLast4: String): List<AccountBalanceEntity>
    
    @Query("DELETE FROM account_balances WHERE id = :id")
    suspend fun deleteBalanceById(id: Long)
    
    @Query("UPDATE account_balances SET balance = :newBalance WHERE id = :id")
    suspend fun updateBalanceById(id: Long, newBalance: BigDecimal)
    
    @Query("""SELECT COUNT(*) FROM account_balances
        WHERE bank_name = :bankName AND account_last4 = :accountLast4""")
    suspend fun getBalanceCountForAccount(bankName: String, accountLast4: String): Int
 
    @Query("DELETE FROM account_balances WHERE bank_name = :bankName AND account_last4 = :accountLast4")
    suspend fun deleteAccount(bankName: String, accountLast4: String): Int
 
    @Query("UPDATE account_balances SET bank_name = :newBankName WHERE bank_name = :oldBankName AND account_last4 = :accountLast4")
    suspend fun updateAccountBankName(oldBankName: String, accountLast4: String, newBankName: String): Int
 
    /** Finds the latest account record for a given last-4 digits, regardless of bank name. */
    @Query("""
        SELECT * FROM account_balances
        WHERE account_last4 = :accountLast4
        ORDER BY timestamp DESC
        LIMIT 1
    """)
    suspend fun getAccountByLast4(accountLast4: String): AccountBalanceEntity?
}

data class AccountBalanceTransactionInfo(
    val id: Long,
    val balance: BigDecimal,
    val sourceType: String?,
    val isCreditCard: Boolean,
    val transactionId: Long?,
    val transactionAmount: BigDecimal?,
    val transactionType: String?,
    val transactionBalanceAfter: BigDecimal?
)

private fun calculateTransactionBalance(
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
