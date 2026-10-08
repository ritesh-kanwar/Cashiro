package com.ritesh.cashiro.data.manager

import com.ritesh.cashiro.data.preferences.IgnoredAccountsStore
import com.ritesh.cashiro.data.mapper.accountIdentity
import com.ritesh.cashiro.data.mapper.isSourceCard
import android.util.Log
import androidx.room.withTransaction
import com.ritesh.cashiro.data.database.CashiroDatabase
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import com.ritesh.cashiro.data.preferences.BankAccountMergeStore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import com.ritesh.cashiro.data.database.entity.TransactionType
import com.ritesh.cashiro.domain.model.rule.TransactionRule
import com.ritesh.cashiro.receiver.BankNotificationConfig
import com.ritesh.parser.core.ParsedTransaction
import com.ritesh.parser.core.bank.BankParserFactory
import com.ritesh.cashiro.data.database.entity.TransactionEntity
import com.ritesh.cashiro.data.mapper.toEntity
import com.ritesh.cashiro.data.repository.AccountBalanceRepository
import com.ritesh.cashiro.data.repository.MerchantMappingRepository
import com.ritesh.cashiro.data.repository.SubscriptionRepository
import com.ritesh.cashiro.data.repository.TransactionRepository
import com.ritesh.cashiro.domain.repository.RuleRepository
import com.ritesh.cashiro.domain.service.RuleEngine
import com.ritesh.cashiro.data.manager.TransactionDeduplication
import com.ritesh.cashiro.data.manager.DedupResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shared processor for SMS transactions. Used by both SmsBroadcastReceiver
 * and OptimizedSmsReaderWorker to ensure consistent transaction processing.
 */
@Singleton
class SmsTransactionProcessor @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountBalanceRepository: AccountBalanceRepository,
    private val merchantMappingRepository: MerchantMappingRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val ruleRepository: RuleRepository,
    private val ruleEngine: RuleEngine,
    private val database: CashiroDatabase,
    @ApplicationContext private val context: Context,
    private val balanceUpdateProcessor: BalanceUpdateProcessor,
    private val bankAccountMerges: BankAccountMergeStore,
    private val ignoredAccounts: IgnoredAccountsStore,
    private val cardRepository: com.ritesh.cashiro.data.repository.CardRepository
) {
    companion object {
        private const val TAG = "SmsTransactionProcessor"
        private val notificationBanks = BankNotificationConfig.notificationAliases.flatMap { BankParserFactory.getParsers(it) }.map { it.getBankName() }.toSet()
    }

    /**
     * Result of processing an SMS message
     */
    data class ScanContext(
        val merchantCategories: Map<String, String>,
        val rulesByType: Map<TransactionType, List<TransactionRule>>
    )

    suspend fun loadScanContext(): ScanContext = coroutineScope {
        val mappings = async { merchantMappingRepository.getAllMappings().first().associate { it.merchantName to it.category } }
        val rules = async { TransactionType.entries.associateWith { ruleRepository.getActiveRulesByType(it) } }
        ScanContext(mappings.await(), rules.await())
    }

    data class ProcessingResult(
        val success: Boolean,
        val transactionId: Long? = null,
        val reason: String? = null,
        val blocked: Boolean = false,
        val persistenceFailed: Boolean = false
    )

    /**
     * Parses and saves a transaction from an SMS message.
     *
     * @param sender SMS sender address
     * @param body SMS body text
     * @param timestamp SMS timestamp in milliseconds
     * @return ProcessingResult indicating success/failure and transaction ID
     */
    suspend fun processAndSaveTransaction(
        sender: String,
        body: String,
        timestamp: Long
    ): ProcessingResult {
        try {
            // Get all parsers that can handle this sender and let content decide
            val parsers = BankParserFactory.getParsers(sender)
            if (parsers.isEmpty()) return ProcessingResult(
                false,
                reason = "No parser found for sender: $sender"
            )

            val mandateParser = parsers.filterIsInstance<com.ritesh.parser.core.bank.PNBBankParser>()
                .firstOrNull { it.isUPIMandateNotification(body) }
            if (mandateParser != null) {
                val mandate = mandateParser.parseUPIMandateSubscription(body)
                if (mandate != null && timestamp >= System.currentTimeMillis() - java.time.Duration.ofDays(30).toMillis()) {
                    BankAccountMergeStore.mutationMutex.withLock {
                        subscriptionRepository.createOrUpdateFromMandate(mandate, mandateParser.getBankName(), body)
                    }
                }
                return ProcessingResult(false, reason = "Mandate notification")
            }

            // Parse the SMS — try each matching parser in order, return first result
            val parsedTransaction = parsers.firstNotNullOfOrNull { parser ->
                parser.parse(body, sender, timestamp)
            } ?: return ProcessingResult(
                false,
                reason = "Could not parse transaction from SMS"
            )

            Log.d(TAG, "Parsed transaction: ${parsedTransaction.amount} from ${parsedTransaction.bankName}")

            // Save the transaction
            return saveParsedTransaction(parsedTransaction, body)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error processing SMS", e)
            return ProcessingResult(false, reason = e.message)
        }
    }

    /**
     * Saves a parsed transaction to the database with all necessary processing:
     * - Duplicate detection
     * - Merchant mapping
     * - Rule application
     * - Subscription matching
     * - Balance updates
     */
    suspend fun saveParsedTransaction(
        parsedTransaction: ParsedTransaction,
        smsBody: String,
        scanContext: ScanContext? = null
    ): ProcessingResult = BankAccountMergeStore.mutationMutex.withLock {
        try {
            database.withTransaction { saveResolvedTransaction(bankAccountMerges.resolve(parsedTransaction), smsBody, scanContext) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Unable to persist SMS transaction", e)
            ProcessingResult(false, reason = "Unable to persist transaction", persistenceFailed = true)
        }
    }

    suspend fun prepareForRescan() = BankAccountMergeStore.mutationMutex.withLock {
        database.withTransaction {
            // Preserve ignored history: those messages will intentionally not be reimported.
            // For ambiguous card/account digits preserve both histories rather than lose either.
            val ignored = IgnoredAccountsStore.linkedCardKeys(ignoredAccounts.keys(),
                cardRepository.getAllCards().first(), emptyList(), bankAccountMerges.mappings())
            database.transactionDao().getRebuildableSmsTransactions()
                .filterNot { ignoredAccounts.matchesKeys(ignored, it.bankName, it.currency, it.accountNumber) }
                .map { it.id }.chunked(500).forEach { database.transactionDao().deleteTransactionsByIds(it) }
            database.accountBalanceDao().getRebuildableBalances()
                .filterNot { ignoredAccounts.matchesKeys(ignored, it.bankName, it.currency, it.accountLast4) }
                .map { it.id }.chunked(500).forEach { database.accountBalanceDao().deleteBalancesByIds(it) }
            database.ruleApplicationDao().deleteOrphanedApplications()
        }
    }

    suspend fun cleanupDuplicates(): Int = BankAccountMergeStore.mutationMutex.withLock {
        database.withTransaction {
            var removed = 0
            TransactionDeduplication.duplicateClusters(transactionRepository.getAllTransactionsList()).forEach { cluster ->
                var keeper = cluster.keeper
                cluster.duplicates.forEach duplicate@ { duplicate ->
                    if (database.lendBorrowDao().getTransactionByWalletId(duplicate.id) != null) return@duplicate
                    if (database.subscriptionPaymentDao().forTransaction(duplicate.id) != null) return@duplicate
                    val enriched = TransactionDeduplication.mergeUserMetadata(keeper, duplicate) ?: return@duplicate
                    if (enriched != keeper) transactionRepository.updateTransaction(enriched)
                    keeper = enriched
                    database.accountBalanceDao().deleteTransactionBalancesAndRecalculate(duplicate.id)
                    database.transactionDao().deleteTransactionById(duplicate.id)
                    database.ruleApplicationDao().deleteApplicationsByTransaction(duplicate.id.toString())
                    removed++
                }
            }
            database.accountBalanceDao().deletePhantomGPayAccounts()
            removed
        }
    }

    private suspend fun saveResolvedTransaction(parsedTransaction: ParsedTransaction, smsBody: String, scanContext: ScanContext?): ProcessingResult {
        return try {
            val linkedAccount = if (parsedTransaction.isSourceCard) {
                parsedTransaction.accountIdentity?.let { cardRepository.getCard(parsedTransaction.bankName, it) }
                    ?.takeIf { it.currency == parsedTransaction.currency }?.accountLast4
            } else null
            if (ignoredAccounts.isIgnored(parsedTransaction.bankName, parsedTransaction.currency,
                    parsedTransaction.accountIdentity, linkedAccount)) {
                return ProcessingResult(false, reason = "Account ignored", blocked = true)
            }
            // Convert to entity
            val entity = parsedTransaction.toEntity()

            // Check if this transaction was previously deleted or is a duplicate
            if (database.transactionDao().getDeletedBySms(smsBody, parsedTransaction.sender) != null) {
                return ProcessingResult(false, reason = "Transaction was previously deleted")
            }
            val existingTransaction = transactionRepository.getTransactionByHash(entity.transactionHash)
            if (existingTransaction != null) {
                when (TransactionDeduplication.checkHash(existingTransaction)) {
                    DedupResult.PreviouslyDeleted -> {
                        Log.d(TAG, "Skipping previously deleted transaction with hash: ${entity.transactionHash}")
                        return ProcessingResult(false, reason = "Transaction was previously deleted")
                    }
                    DedupResult.HashDuplicate -> {
                        Log.d(TAG, "Transaction already exists: ${entity.transactionHash}")
                        return ProcessingResult(false, reason = "Duplicate transaction")
                    }
                    else -> {} // Not reached
                }
            }

            if (entity.bankName in notificationBanks) {
                val nearby = transactionRepository.getTransactionByAmountAndDate(entity.amount, entity.dateTime.minusMinutes(2), entity.dateTime.plusMinutes(2))
                if (TransactionDeduplication.isBookedByOtherChannel(entity, nearby, BankNotificationConfig.notificationAliases)) {
                    return ProcessingResult(false, reason = "Transaction already booked by another channel")
                }
            }

            var replacement: TransactionEntity? = null
            // Check for UPI duplicate within the time window
            if (TransactionDeduplication.hasUpiReference(entity)) {
                val windowEnd = entity.dateTime.plus(TransactionDeduplication.UPI_DUPLICATE_WINDOW)
                val windowStart = entity.dateTime.minus(TransactionDeduplication.UPI_DUPLICATE_WINDOW)
                val upiCandidates = transactionRepository.getTransactionsByReferenceAndAmount(
                    reference = entity.reference!!,
                    amount = entity.amount,
                    accountLast4 = null,
                    startDate = windowStart,
                    endDate = windowEnd
                )
                val candidateForReplacement = upiCandidates.firstOrNull { existing ->
                    TransactionDeduplication.shouldReplaceWithIncoming(existing, entity)
                }
                if (candidateForReplacement != null) {
                    Log.d(TAG, "Replacing UPI transaction ${candidateForReplacement.id} with incoming from ${entity.bankName}")
                    replacement = candidateForReplacement
                } else {
                    val upiDuplicate = upiCandidates.any { existing ->
                        TransactionDeduplication.isSameUpiTransaction(existing, entity)
                    }
                    if (upiDuplicate) {
                        Log.d(TAG, "UPI duplicate transaction detected for reference: ${entity.reference}")
                        return ProcessingResult(false, reason = "UPI duplicate transaction")
                    }
                }
            }

            // Check for custom merchant mapping
            val customCategory = if (scanContext == null) merchantMappingRepository.getCategoryForMerchant(entity.merchantName)
                else scanContext.merchantCategories[entity.merchantName]
            val entityWithMapping = if (customCategory != null) {
                Log.d(TAG, "Found custom category mapping: ${entity.merchantName} -> $customCategory")
                entity.copy(category = customCategory)
            } else {
                entity
            }

            // Apply rule engine to the transaction
            val activeRules = if (scanContext == null) ruleRepository.getActiveRulesByType(entityWithMapping.transactionType)
                else scanContext.rulesByType[entityWithMapping.transactionType].orEmpty()

            // Check if this transaction should be blocked
            val blockingRule = ruleEngine.shouldBlockTransaction(
                entityWithMapping,
                smsBody,
                activeRules
            )

            if (blockingRule != null) {
                Log.d(TAG, "Transaction blocked by rule: ${blockingRule.name}")
                return ProcessingResult(false, reason = "Blocked by rule", blocked = true)
            }

            val (entityWithRules, ruleApplications) = ruleEngine.evaluateRules(
                entityWithMapping,
                smsBody,
                activeRules
            )

            if (ruleApplications.isNotEmpty()) {
                Log.d(TAG, "Applied ${ruleApplications.size} rules to transaction")
            }

            val resolvedForSubscription = accountBalanceRepository.resolveEntityAccountNumber(entityWithRules, parsedTransaction)
            SubscriptionPaymentReconciliation.reconcile(database, accountBalanceRepository, resolvedForSubscription)?.let { id ->
                return ProcessingResult(false, transactionId = id, reason = "Subscription payment already recorded")
            }

            // Check if this transaction matches an active subscription
            val matchedSubscription = subscriptionRepository.matchTransactionToSubscription(
                resolvedForSubscription
            )

            val finalEntity = if (matchedSubscription != null) {
                Log.d(TAG, "Transaction matched to active subscription: ${matchedSubscription.merchantName}")
                entityWithRules.copy(isRecurring = true)
            } else {
                entityWithRules
            }
            val finalEntityForInsert = accountBalanceRepository.resolveEntityAccountNumber(finalEntity, parsedTransaction)

            val rowId = if (replacement != null) {
                val existing = replacement
                database.ruleApplicationDao().deleteApplicationsByTransaction(existing.id.toString())
                database.accountBalanceDao().deleteTransactionBalancesAndRecalculate(existing.id)
                transactionRepository.updateTransaction(finalEntityForInsert.copy(
                    id = existing.id, transactionHash = existing.transactionHash,
                    createdAt = existing.createdAt, attachments = existing.attachments,
                    description = existing.description ?: finalEntityForInsert.description,
                    subcategory = existing.subcategory ?: finalEntityForInsert.subcategory,
                    isRecurring = existing.isRecurring || finalEntityForInsert.isRecurring,
                    category = existing.category.takeUnless { it == "Miscellaneous" } ?: finalEntityForInsert.category
                ))
                existing.id
            } else transactionRepository.insertTransaction(finalEntityForInsert)
            if (rowId != -1L) {
                Log.d(TAG, "Saved new transaction with ID: $rowId${if (finalEntityForInsert.isRecurring) " (Recurring)" else ""}")

                // Save rule applications if any rules were applied
                if (ruleApplications.isNotEmpty()) {
                    val applicationsWithId = ruleApplications.map { 
                        it.copy(transactionId = rowId.toString())
                    }
                    ruleRepository.saveRuleApplications(applicationsWithId)
                }

                if (matchedSubscription != null) {
                    SubscriptionPaymentReconciliation.recordCharge(database, matchedSubscription, rowId, finalEntityForInsert)
                }

                // Process balance updates
                // Keep the row and its balance in one transaction, including cancellation rollback.
                balanceUpdateProcessor.process(parsedTransaction, finalEntityForInsert, rowId)
                database.accountBalanceDao().deletePhantomGPayAccounts()

                return ProcessingResult(true, transactionId = rowId)
            } else {
                Log.d(TAG, "Transaction already exists (duplicate): ${entity.transactionHash}")
                return ProcessingResult(false, reason = "Duplicate transaction")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Let the Room transaction roll back all related writes before the caller handles failure.
            throw e
        }
    }

}
