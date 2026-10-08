package com.ritesh.cashiro.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import android.content.Context
import android.net.Uri
import android.provider.Telephony
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.ritesh.cashiro.R
import com.ritesh.cashiro.data.database.entity.UnrecognizedSmsEntity
import com.ritesh.cashiro.data.preferences.UserPreferencesRepository
import com.ritesh.cashiro.data.repository.AccountBalanceRepository
import com.ritesh.cashiro.data.repository.LlmRepository
import com.ritesh.cashiro.data.repository.SubscriptionRepository
import com.ritesh.cashiro.data.repository.TransactionRepository
import com.ritesh.cashiro.data.repository.UnrecognizedSmsRepository
import com.ritesh.cashiro.data.manager.SmsTransactionProcessor
import com.ritesh.cashiro.data.manager.SmsScanParamsCalculator
import com.ritesh.parser.core.ParsedTransaction
import com.ritesh.parser.core.SmsFilter
import com.ritesh.parser.core.bank.BankParserFactory
import com.ritesh.parser.core.bank.FederalBankParser
import com.ritesh.parser.core.bank.HDFCBankParser
import com.ritesh.parser.core.bank.IndianBankParser
import com.ritesh.parser.core.bank.SBIBankParser
import com.ritesh.parser.core.bank.IndusIndBankParser
import com.ritesh.cashiro.utils.capitalizeFirst
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.system.measureTimeMillis

/**
 * Optimized SMS Worker with parallel processing and progress tracking.
 * This worker provides significant performance improvements through:
 * 1. Parallel processing of SMS messages
 * 2. Progress reporting with estimated time completion
 * 3. Optimized database operations
 * 4. Efficient memory usage
 */
@HiltWorker
class OptimizedSmsReaderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val transactionRepository: TransactionRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val accountBalanceRepository: AccountBalanceRepository,
    private val llmRepository: LlmRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val unrecognizedSmsRepository: UnrecognizedSmsRepository,
    private val ignoredAccounts: com.ritesh.cashiro.data.preferences.IgnoredAccountsStore,
    private val smsTransactionProcessor: SmsTransactionProcessor
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val TAG = "OptimizedSmsReaderWorker"
        const val WORK_NAME = "optimized_sms_reader_work"

        // Input keys
        const val INPUT_FORCE_RESYNC = "input_force_resync"

        // Progress keys
        const val PROGRESS_TOTAL = "progress_total"
        const val PROGRESS_PROCESSED = "progress_processed"
        const val PROGRESS_PARSED = "progress_parsed"
        const val PROGRESS_SAVED = "progress_saved"
        const val PROGRESS_FAILED = "progress_failed"
        const val PROGRESS_BLOCKED = "progress_blocked"
        const val PROGRESS_TIME_ELAPSED = "progress_time_elapsed"
        const val PROGRESS_ESTIMATED_TIME_REMAINING = "progress_estimated_time_remaining"
        const val PROGRESS_CURRENT_BATCH = "progress_current_batch"
        const val PROGRESS_TOTAL_BATCHES = "progress_total_batches"

        // SMS Content Provider columns
        private val SMS_PROJECTION = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.DATE,
            Telephony.Sms.BODY,
            Telephony.Sms.TYPE
        )

        // Parallel processing configuration
    }

    /**
     * Calculates optimal batch size based on available cores and total messages
     */
    private fun calculateOptimalBatchSize(totalMessages: Int, availableCores: Int): Int {
        return when {
            totalMessages < 100 -> 10 // Small datasets: small batches for better progress tracking
            totalMessages < 500 -> 25 // Medium datasets: moderate batches
            totalMessages < 2000 -> 50 // Large datasets: standard batches
            else -> {
                // Very large datasets: scale batch size with cores but cap at reasonable limit
                val coreBasedBatch = availableCores * 15
                minOf(coreBasedBatch, 200)
            }
        }
    }

    /** Bound CPU parser work; the writer still serializes balance mutations. */
    private fun calculateParseParallelism(availableCores: Int): Int {
        return (availableCores - 1).coerceIn(1, 4)
    }

    data class ProcessingStats(
        var totalMessages: Int = 0,
        var processedMessages: Int = 0,
        var parsedTransactions: Int = 0,
        var savedTransactions: Int = 0,
        var blockedTransactions: Int = 0,
        var failedTransactions: Int = 0,
        var subscriptionCount: Int = 0,
        var startTime: Long = System.currentTimeMillis(),
        var messagesPerSecond: Double = 0.0
    ) {
        fun updateTimeElapsed(): Long = System.currentTimeMillis() - startTime

        private val rate = ScanRate(startTime)
        fun recordCompletion() = rate.record(System.currentTimeMillis())
        fun updateMessagesPerSecond() {
            messagesPerSecond = rate.messagesPerSecond(System.currentTimeMillis(), processedMessages)
        }
        fun getEstimatedTimeRemaining(): Long =
            rate.remainingMillis(System.currentTimeMillis(), processedMessages, totalMessages)
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val channelId = "sms_scan"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(NotificationChannel(channelId, applicationContext.getString(R.string.scanning_sms_messages_title), NotificationManager.IMPORTANCE_LOW))
        }
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setContentTitle(applicationContext.getString(R.string.scanning_messages))
            .setOngoing(true)
            .setProgress(0, 0, true)
            .build()
        return ForegroundInfo(1002, notification)
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Check if this is a force resync request
            val forceResync = inputData.getBoolean(INPUT_FORCE_RESYNC, false)
            Log.d(TAG, "Starting optimized SMS reading and parsing work... (forceResync: $forceResync)")

            // If force resync, clear existing data first
            if (forceResync) {
                Log.d(TAG, "Force resync: Rebuilding uncurated SMS records...")
                smsTransactionProcessor.prepareForRescan()
                Log.d(TAG, "Force resync: Curated records retained, starting scan")
            }

            val stats = ProcessingStats()

            // Calculate scan parameters
            val lastScanTimestamp = userPreferencesRepository.getLastScanTimestamp().first() ?: 0L
            val scanMonths = userPreferencesRepository.getSmsScanMonths()
            val scanAllTime = userPreferencesRepository.getSmsScanAllTime()
            val lastScanPeriod = userPreferencesRepository.getLastScanPeriod().first() ?: 0
            val now = System.currentTimeMillis()

            val scanParams = SmsScanParamsCalculator.compute(
                forceResync, lastScanTimestamp, scanMonths, scanAllTime, lastScanPeriod, now, zoneId
            )
            val needsFullScan = scanParams.needsFullScan
            val scanStartTime = scanParams.scanStartTime

            // Get total count upfront for stats
            val totalMsgCount = getSmsAndRcsCount(scanStartTime)
            stats.totalMessages = totalMsgCount
            Log.d(TAG, "Found $totalMsgCount SMS & RCS messages to process")

            // Calculate optimal batch size and parse parallelism
            val availableCores = Runtime.getRuntime().availableProcessors()
            val batchSize = calculateOptimalBatchSize(totalMsgCount, availableCores)
            val parseParallelism = calculateParseParallelism(availableCores)

            Log.d(TAG, "Auto-calculated optimization parameters:")
            Log.d(TAG, "- Available CPU cores: $availableCores")
            Log.d(TAG, "- Batch size: $batchSize")
            Log.d(TAG, "- Parse parallelism: $parseParallelism")
            Log.d(TAG, "- Total batches: ${(totalMsgCount + batchSize - 1) / batchSize}")

            // Report initial progress
            setProgress(
                workDataOf(
                    PROGRESS_TOTAL to totalMsgCount,
                    PROGRESS_PROCESSED to 0,
                    PROGRESS_PARSED to 0,
                    PROGRESS_SAVED to 0,
                    PROGRESS_TIME_ELAPSED to 0L,
                    PROGRESS_ESTIMATED_TIME_REMAINING to 0L,
                    PROGRESS_CURRENT_BATCH to 1,
                    PROGRESS_TOTAL_BATCHES to (totalMsgCount + batchSize - 1) / batchSize
                )
            )

            // Process messages via 3-stage channel pipeline
            val processingTime = measureTimeMillis {
                processWithChannelPipeline(scanStartTime, stats, batchSize, parseParallelism)
            }

            stats.updateTimeElapsed()
            stats.updateMessagesPerSecond()

            Log.d(
                TAG, """
                SMS parsing completed in ${processingTime}ms:
                - Total Messages: ${stats.totalMessages}
                - Processed: ${stats.processedMessages}
                - Parsed Transactions: ${stats.parsedTransactions}
                - Saved Transactions: ${stats.savedTransactions}
                - Subscriptions: ${stats.subscriptionCount}
                - Processing Speed: ${"%.2f".format(stats.messagesPerSecond)} msg/sec
            """.trimIndent()
            )

            smsTransactionProcessor.cleanupDuplicates()

            // Clean up old unrecognized SMS entries
            try {
                unrecognizedSmsRepository.cleanupOldEntries()
                Log.d(TAG, "Cleaned up old unrecognized SMS entries")
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error cleaning up unrecognized SMS: ${e.message}")
            }

            // Update system prompt with new financial data if any transactions were saved
            if (stats.savedTransactions > 0) {
                try {
                    llmRepository.updateSystemPrompt()
                    Log.d(TAG, "Updated system prompt with latest financial data")
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating system prompt: ${e.message}")
                }
            }

            // Continue past failed messages, but keep them eligible for the next scan.
            if (stats.failedTransactions == 0) {
                userPreferencesRepository.setLastScanTimestamp(now)
                if (needsFullScan) userPreferencesRepository.setLastScanPeriod(scanParams.completedPeriod)
            }

            val completion = workDataOf(
                PROGRESS_TOTAL to stats.totalMessages,
                PROGRESS_PROCESSED to stats.processedMessages,
                PROGRESS_PARSED to stats.parsedTransactions,
                PROGRESS_SAVED to stats.savedTransactions,
                PROGRESS_BLOCKED to stats.blockedTransactions,
                PROGRESS_FAILED to stats.failedTransactions,
                PROGRESS_TIME_ELAPSED to stats.updateTimeElapsed(),
                PROGRESS_ESTIMATED_TIME_REMAINING to 0L,
                PROGRESS_CURRENT_BATCH to (stats.processedMessages + batchSize - 1) / batchSize,
                PROGRESS_TOTAL_BATCHES to (stats.totalMessages + batchSize - 1) / batchSize
            )
            setProgress(completion)
            if (stats.failedTransactions > 0) Result.failure(completion) else Result.success(completion)

        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error in optimized SMS parsing work", e)
            Result.failure()
        }
    }

    private val senderParsers = java.util.concurrent.ConcurrentHashMap<String, List<com.ritesh.parser.core.bank.BankParser>>()
    private val zoneId = ZoneId.systemDefault()
    private fun hasFinancialParser(sender: String): Boolean =
        senderParsers.computeIfAbsent(sender) { BankParserFactory.getParsers(it) }.isNotEmpty()
    private val recentMessageCutoff = LocalDateTime.now(zoneId).minusDays(30)

    private suspend fun processWithChannelPipeline(
        scanStartTime: Long,
        stats: ProcessingStats,
        batchSize: Int,
        parseParallelism: Int
    ) = coroutineScope {
        val input = Channel<SmsMessage>(512)
        val output = Channel<ParseResult>(512)
        val scanContext = smsTransactionProcessor.loadScanContext()
        val unrecognizedBatch = ArrayList<UnrecognizedSmsEntity>(50)
        suspend fun flushUnrecognized() {
            if (unrecognizedBatch.isNotEmpty()) {
                unrecognizedSmsRepository.insertAll(unrecognizedBatch)
                unrecognizedBatch.clear()
            }
        }
        val feeder = launch(Dispatchers.IO) {
            try {
                coroutineScope {
                    launch { streamSmsToChannel(input, scanStartTime) }
                    launch { streamRcsToChannel(input, scanStartTime) }
                }
            } finally { input.close() }
        }
        val parsers = (0 until parseParallelism).map {
            launch(Dispatchers.Default) {
                for (message in input) output.send(parseMessage(message))
            }
        }
        val closer = launch {
            try { parsers.forEach { it.join() } } finally { output.close() }
        }
        suspend fun report(finished: Boolean = false) {
            stats.updateMessagesPerSecond()
            setProgress(workDataOf(
                PROGRESS_TOTAL to stats.totalMessages,
                PROGRESS_PROCESSED to stats.processedMessages,
                PROGRESS_PARSED to stats.parsedTransactions,
                PROGRESS_SAVED to stats.savedTransactions,
                PROGRESS_BLOCKED to stats.blockedTransactions,
                PROGRESS_FAILED to stats.failedTransactions,
                PROGRESS_TIME_ELAPSED to stats.updateTimeElapsed(),
                PROGRESS_ESTIMATED_TIME_REMAINING to if (finished) 0L else stats.getEstimatedTimeRemaining(),
                PROGRESS_CURRENT_BATCH to (stats.processedMessages + batchSize - 1) / batchSize,
                PROGRESS_TOTAL_BATCHES to (stats.totalMessages + batchSize - 1) / batchSize
            ))
        }
        // One writer commits each transaction and its balance atomically.
        // Parser queues overlap CPU work with writes without leaving partial ledger rows.
        var lastReportTime = 0L
        for (result in output) {
            when (result) {
                is ParseResult.Prepared -> {
                    val date = LocalDateTime.ofInstant(Instant.ofEpochMilli(result.sms.timestamp), zoneId)
                    val subscription = processSubscriptionNotifications(result.parser, result.sms, date, date.isAfter(recentMessageCutoff))
                    stats.subscriptionCount += subscription.subscriptionCount
                    if (!subscription.shouldSkipTransaction && result.parsed != null) {
                        stats.parsedTransactions++
                        val saved = smsTransactionProcessor.saveParsedTransaction(
                            result.parsed, result.sms.body, scanContext
                        )
                        if (saved.success) stats.savedTransactions++
                        if (saved.blocked) stats.blockedTransactions++
                        if (saved.persistenceFailed) stats.failedTransactions++
                    }
                }
                is ParseResult.Unrecognized -> {
                    if (SmsFilter.isTransactionMessage(result.sms.body)) {
                        unrecognizedBatch.add(UnrecognizedSmsEntity(
                            sender = result.sms.sender, smsBody = result.sms.body,
                            receivedAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(result.sms.timestamp), zoneId)
                        ))
                        if (unrecognizedBatch.size >= 50) flushUnrecognized()
                    }
                }
                is ParseResult.Skipped -> Unit
            }
            stats.processedMessages++
            stats.recordCompletion()
            val completedAt = System.currentTimeMillis()
            if (stats.processedMessages == 1 || completedAt - lastReportTime >= 250L) {
                report()
                lastReportTime = completedAt
            }
        }
        flushUnrecognized()
        feeder.join()
        closer.join()
        report(finished = true)
    }

    private fun parseMessage(sms: SmsMessage): ParseResult {
        return try {
            val parsers = senderParsers.computeIfAbsent(sms.sender) { BankParserFactory.getParsers(it) }
            if (parsers.isEmpty()) {
                val sender = sms.sender.uppercase()
                if (sender.endsWith("-T") || sender.endsWith("-S")) ParseResult.Unrecognized(sms)
                else ParseResult.Skipped
            } else {
                ParseResult.Prepared(sms, parsers.first(), parsers.firstNotNullOfOrNull { it.parse(sms.body, sms.sender, sms.timestamp) })
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Unable to parse message", e)
            ParseResult.Skipped
        }
    }

    private sealed class ParseResult {
        data class Prepared(val sms: SmsMessage, val parser: com.ritesh.parser.core.bank.BankParser, val parsed: ParsedTransaction?) : ParseResult()
        data class Unrecognized(val sms: SmsMessage) : ParseResult()
        data object Skipped : ParseResult()
    }

private data class SubscriptionResult(
    val shouldSkipTransaction: Boolean,
    val subscriptionCount: Int
)

private suspend fun processSubscriptionNotifications(
    parser: com.ritesh.parser.core.bank.BankParser,
    sms: SmsMessage,
    smsDateTime: LocalDateTime,
    isRecentMessage: Boolean
): SubscriptionResult {
    // ─── Generic balance-update check (works for ALL parsers) ─────────────────
    // This covers HDFC, IndusInd, SBI CC statements, and any future parser that
    // overrides isBalanceUpdateNotification/parseBalanceUpdate in BankParser.
    if (parser.isBalanceUpdateNotification(sms.body)) {
        val balanceUpdateInfo = parser.parseBalanceUpdate(sms.body)
        if (balanceUpdateInfo != null && !ignoredAccounts.isIgnored(balanceUpdateInfo.bankName, parser.getCurrency(), balanceUpdateInfo.accountLast4)) {
            try {
                com.ritesh.cashiro.data.preferences.BankAccountMergeStore.mutationMutex.withLock {
                accountBalanceRepository.insertBalanceUpdate(
                    bankName = balanceUpdateInfo.bankName,
                    accountLast4 = if (balanceUpdateInfo.isCreditCard) balanceUpdateInfo.accountLast4 else
                        accountBalanceRepository.resolveAccountLast4(balanceUpdateInfo.bankName, balanceUpdateInfo.accountLast4, parser.getCurrency()),
                    balance = balanceUpdateInfo.balance,
                    timestamp = balanceUpdateInfo.asOfDate ?: smsDateTime,
                    currency = parser.getCurrency()
                )
                }
                Log.d(TAG, "Saved balance update for ${balanceUpdateInfo.bankName} " +
                    "(isCreditCard=${balanceUpdateInfo.isCreditCard})")
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error saving balance update for ${parser.getBankName()}: ${e.message}")
            }
        }
        return SubscriptionResult(true, 0) // Skip transaction parsing for balance/statement updates
    }
    // ──────────────────────────────────────────────────────────────────────────

    if (parser is com.ritesh.parser.core.bank.PNBBankParser && parser.isUPIMandateNotification(sms.body)) {
        val mandate = parser.parseUPIMandateSubscription(sms.body)
        if (isRecentMessage && mandate != null) {
            try {
                com.ritesh.cashiro.data.preferences.BankAccountMergeStore.mutationMutex.withLock {
                    subscriptionRepository.createOrUpdateFromMandate(mandate, parser.getBankName(), sms.body)
                }
                return SubscriptionResult(true, 1)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Unable to save PNB mandate")
            }
        }
        return SubscriptionResult(true, 0)
    }
    return when (parser) {
        is SBIBankParser -> {
            if (parser.isUPIMandateNotification(sms.body)) {
                if (!isRecentMessage) {
                    Log.d(TAG, "Skipping old SBI UPI-Mandate from ${smsDateTime.toLocalDate()}")
                    return SubscriptionResult(
                        false,
                        0
                    ) // Continue with transaction parsing for old messages
                }

                val upiMandateInfo = parser.parseUPIMandateSubscription(sms.body)
                if (upiMandateInfo != null) {
                    try {
                        val subscriptionId = subscriptionRepository.createOrUpdateFromSBIMandate(
                            upiMandateInfo,
                            parser.getBankName(),
                            sms.body
                        )
                        Log.d(
                            TAG,
                            "Created/Updated SBI UPI-Mandate subscription: $subscriptionId for ${upiMandateInfo.merchant}"
                        )
                        return SubscriptionResult(
                            true,
                            1
                        ) // Skip transaction parsing, count 1 subscription
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.e(TAG, "Error saving SBI UPI-Mandate subscription: ${e.message}")
                    }
                }
            }
            SubscriptionResult(false, 0) // Continue with transaction parsing
        }

        is FederalBankParser -> {
            // Check for E-Mandate creation notifications
            // Note: Mandate creation messages should be processed regardless of age
            // as they create future subscriptions
            if (parser.isMandateCreationNotification(sms.body)) {
                // Don't skip old mandate creation messages - they create future subscriptions
                if (!isRecentMessage) {
                    Log.d(
                        TAG,
                        "Processing older Federal Bank Mandate Creation from ${smsDateTime.toLocalDate()} - mandate creation processed regardless of age"
                    )
                }

                val eMandateInfo = parser.parseEMandateSubscription(sms.body)
                if (eMandateInfo != null) {
                    try {
                        val subscriptionId = subscriptionRepository.createOrUpdateFromFederalBankMandate(
                            eMandateInfo,
                            parser.getBankName(),
                            sms.body
                        )
                        Log.d(
                            TAG,
                            "Created/Updated Federal Bank E-Mandate subscription: $subscriptionId for ${eMandateInfo.merchant}"
                        )
                        return SubscriptionResult(
                            true,
                            1
                        ) // Skip transaction parsing, count 1 subscription
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.e(TAG, "Error saving Federal Bank E-Mandate subscription: ${e.message}")
                    }
                }
            }

            // Check for Future Debit notifications (payment due messages)
            // Note: Payment due messages should be processed regardless of age if they're for future dates
            val futureDebitInfo = parser.parseFutureDebit(sms.body)
            if (futureDebitInfo != null) {
                // Check if the payment due date is in the future
                val isFuturePayment = try {
                    val paymentDate = java.time.LocalDate.parse(
                        futureDebitInfo.nextDeductionDate,
                        java.time.format.DateTimeFormatter.ofPattern(futureDebitInfo.dateFormat)
                    )
                    paymentDate.isAfter(java.time.LocalDate.now())
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // If we can't parse the date, assume it's recent and apply normal filtering
                    isRecentMessage
                }

                if (!isFuturePayment && !isRecentMessage) {
                    Log.d(
                        TAG,
                        "Skipping old Federal Bank Future Debit from ${smsDateTime.toLocalDate()} - payment date is not in future"
                    )
                    return SubscriptionResult(
                        false,
                        0
                    ) // Continue with transaction parsing for old messages
                }

                // Process future debit messages regardless of SMS age if payment date is future
                if (!isRecentMessage && isFuturePayment) {
                    Log.d(
                        TAG,
                        "Processing older Federal Bank Future Debit from ${smsDateTime.toLocalDate()} - payment date is future: ${futureDebitInfo.nextDeductionDate}"
                    )
                }

                try {
                    val subscriptionId = subscriptionRepository.createOrUpdateFromFederalBankMandate(
                        futureDebitInfo,
                        parser.getBankName(),
                        sms.body
                    )
                    Log.d(
                        TAG,
                        "Created/Updated Federal Bank future debit subscription: $subscriptionId for ${futureDebitInfo.merchant}"
                    )
                    return SubscriptionResult(
                        true,
                        1
                    ) // Skip transaction parsing, count 1 subscription
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Error saving Federal Bank future debit subscription: ${e.message}")
                }
            }

            SubscriptionResult(false, 0) // Continue with transaction parsing if no mandate found
        }

        is HDFCBankParser -> {
            var subscriptionCount = 0

            // Check for E-Mandate notifications
            if (parser.isEMandateNotification(sms.body)) {
                if (!isRecentMessage) {
                    Log.d(TAG, "Skipping old HDFC E-Mandate from ${smsDateTime.toLocalDate()}")
                    return SubscriptionResult(
                        false,
                        0
                    ) // Continue with transaction parsing for old messages
                }

                val eMandateInfo = parser.parseEMandateSubscription(sms.body)
                if (eMandateInfo != null) {
                    try {
                        val subscriptionId = subscriptionRepository.createOrUpdateFromEMandate(
                            eMandateInfo,
                            parser.getBankName(),
                            sms.body
                        )
                        Log.d(
                            TAG,
                            "Created/Updated HDFC E-Mandate subscription: $subscriptionId for ${eMandateInfo.merchant}"
                        )
                        subscriptionCount++
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.e(TAG, "Error saving HDFC E-Mandate subscription: ${e.message}")
                    }
                }
            }

            // Check for Future Debit notifications
            if (parser.isFutureDebitNotification(sms.body)) {
                if (!isRecentMessage) {
                    Log.d(TAG, "Skipping old HDFC Future Debit from ${smsDateTime.toLocalDate()}")
                    return SubscriptionResult(
                        false,
                        0
                    ) // Continue with transaction parsing for old messages
                }

                val futureDebitInfo = parser.parseFutureDebit(sms.body)
                if (futureDebitInfo != null) {
                    try {
                        val subscriptionId = subscriptionRepository.createOrUpdateFromEMandate(
                            futureDebitInfo,
                            parser.getBankName(),
                            sms.body
                        )
                        Log.d(
                            TAG,
                            "Created/Updated HDFC future debit subscription: $subscriptionId for ${futureDebitInfo.merchant}"
                        )
                        subscriptionCount++
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.e(TAG, "Error saving HDFC future debit subscription: ${e.message}")
                    }
                }
            }

            if (subscriptionCount > 0) {
                SubscriptionResult(
                    true,
                    subscriptionCount
                ) // Skip transaction parsing for subscriptions
            } else {
                SubscriptionResult(false, 0) // Continue with transaction parsing
            }
        }

        is IndianBankParser -> {
            if (parser.isMandateNotification(sms.body)) {
                if (!isRecentMessage) {
                    Log.d(TAG, "Skipping old Indian Bank Mandate from ${smsDateTime.toLocalDate()}")
                    return SubscriptionResult(
                        false,
                        0
                    ) // Continue with transaction parsing for old messages
                }

                val mandateInfo = parser.parseMandateSubscription(sms.body)
                if (mandateInfo != null) {
                    try {
                        val subscriptionId =
                            subscriptionRepository.createOrUpdateFromIndianBankMandate(
                                mandateInfo,
                                parser.getBankName(),
                                sms.body
                            )
                        Log.d(
                            TAG,
                            "Created/Updated Indian Bank subscription: $subscriptionId for ${mandateInfo.merchant}"
                        )
                        return SubscriptionResult(
                            true,
                            1
                        ) // Skip transaction parsing, count 1 subscription
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.e(TAG, "Error saving Indian Bank subscription: ${e.message}")
                    }
                }
            }
            SubscriptionResult(false, 0) // Continue with transaction parsing
        }

        else -> SubscriptionResult(false, 0) // Continue with transaction parsing for other banks
    }
}



private fun getSmsAndRcsCount(scanStartTime: Long): Int {
    var total = 0
    try {
        val fastCount = try {
            applicationContext.contentResolver.query(
                Telephony.Sms.CONTENT_URI, arrayOf("COUNT(*)"),
                "${Telephony.Sms.TYPE} = ? AND ${Telephony.Sms.DATE} >= ?",
                arrayOf(Telephony.Sms.MESSAGE_TYPE_INBOX.toString(), scanStartTime.toString()), null
            )?.use { if (it.moveToFirst()) it.getInt(0) else null }
        } catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (e: Exception) { null }
        if (fastCount != null) total += fastCount else {
            val smsCursor = applicationContext.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(Telephony.Sms._ID),
                "${Telephony.Sms.TYPE} = ? AND ${Telephony.Sms.DATE} >= ?",
                arrayOf(Telephony.Sms.MESSAGE_TYPE_INBOX.toString(), scanStartTime.toString()),
                null
            )
            smsCursor?.use {
                total += it.count
            }
        }
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "Error counting SMS: ${e.message}")
    }

    try {
        val scanStartTimeSeconds = scanStartTime / 1000
        val mmsCursor = applicationContext.contentResolver.query(
            Uri.parse("content://mms"),
            arrayOf("tr_id"),
            "date >= ?",
            arrayOf(scanStartTimeSeconds.toString()),
            null
        )
        mmsCursor?.use { cursor ->
            val trIdIndex = cursor.getColumnIndex("tr_id")
            while (cursor.moveToNext()) {
                val trId = if (trIdIndex >= 0) cursor.getString(trIdIndex) ?: "" else ""
                if (trId.startsWith("proto:")) {
                    // Extract sender from tr_id to verify if it's from recognized financial sender
                    val sender = extractRcsSender(trId)
                    if (sender != null) {
                        if (hasFinancialParser(sender)) {
                            total++
                        }
                    }
                }
            }
        }
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "Error counting RCS: ${e.message}")
    }
    return total
}

private suspend fun streamSmsToChannel(
    channel: Channel<SmsMessage>,
    scanStartTime: Long
) {
    try {
        val cursor = applicationContext.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            SMS_PROJECTION,
            "${Telephony.Sms.TYPE} = ? AND ${Telephony.Sms.DATE} >= ?",
            arrayOf(Telephony.Sms.MESSAGE_TYPE_INBOX.toString(), scanStartTime.toString()),
            "${Telephony.Sms.DATE} ASC"  // Process oldest first (chronological order)
        )

        cursor?.use {
            val idIndex = it.getColumnIndexOrThrow(Telephony.Sms._ID)
            val addressIndex = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val dateIndex = it.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val bodyIndex = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val typeIndex = it.getColumnIndexOrThrow(Telephony.Sms.TYPE)

            while (it.moveToNext()) {
                val message = SmsMessage(
                    id = it.getLong(idIndex),
                    sender = it.getString(addressIndex) ?: "",
                    timestamp = it.getLong(dateIndex),
                    body = it.getString(bodyIndex) ?: "",
                    type = it.getInt(typeIndex)
                )
                channel.send(message)
            }
        }
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Exception) {
        throw e
    }
}

private suspend fun streamRcsToChannel(
    channel: Channel<SmsMessage>,
    scanStartTime: Long
) {
    try {
        val scanStartTimeSeconds = scanStartTime / 1000

        val mmsCursor = applicationContext.contentResolver.query(
            Uri.parse("content://mms"),
            arrayOf("_id", "thread_id", "date", "tr_id", "m_id"),
            "date >= ?",
            arrayOf(scanStartTimeSeconds.toString()),
            "date ASC"  // Process oldest first (chronological order)
        )

        mmsCursor?.use { cursor ->
            while (cursor.moveToNext()) {
                val messageId = cursor.getLong(cursor.getColumnIndexOrThrow("_id"))
                val date = cursor.getLong(cursor.getColumnIndexOrThrow("date"))
                val trIdIndex = cursor.getColumnIndex("tr_id")
                val trId = if (trIdIndex >= 0) cursor.getString(trIdIndex) ?: "" else ""

                // Check if this is an RCS message (has proto: in tr_id)
                if (trId.startsWith("proto:")) {
                    // Extract sender from tr_id (it's base64 encoded protobuf)
                    val sender = extractRcsSender(trId)

                    if (sender == null || !hasFinancialParser(sender)) continue

                    // Get message text from parts
                    var messageText = getRcsMessageText(messageId)

                    // If it's JSON (RCS Rich Card), extract the actual text
                    if (messageText != null && messageText.trim().startsWith("{")) {
                        messageText = extractTextFromRcsJson(messageText)
                    }

                    if (messageText != null) {
                        channel.send(SmsMessage(
                            id = messageId, sender = sender, timestamp = date * 1000,
                            body = messageText, type = Telephony.Sms.MESSAGE_TYPE_INBOX
                        ))
                    }
                }
            }
        }
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "Error streaming RCS messages: ${e.message}")
    }
}

/**
 * Extracts sender name from RCS tr_id field
 * The tr_id contains base64 encoded protobuf data with sender info
 */
private fun extractRcsSender(trId: String): String? {
    return try {
        // Remove "proto:" prefix and decode base64
        val base64Data = trId.removePrefix("proto:")
        val decodedBytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
        val decodedString = String(decodedBytes)

        // Look for sender patterns in the decoded data
        // Pattern 1: Agent ID like "ask_apollo_9xdchzx9_agent@rbm.goog"
        val agentPattern = Regex("""([a-z_]+)_[a-z0-9]+_agent@rbm\.goog""")
        agentPattern.find(decodedString)?.let { match ->
            // Convert agent ID to readable name (e.g., "ask_apollo" -> "Ask Apollo")
            return match.groupValues[1].split("_").joinToString(" ") {
                it.capitalizeFirst()
            }
        }

        // Pattern 2: Look for actual sender name in the data
        // RCS messages often have the business name directly in the protobuf
        val namePattern = Regex("""[\x12\x1a][\x00-\x20]([A-Za-z][A-Za-z\s]+)""")
        namePattern.find(decodedString)?.let { match ->
            val name = match.groupValues[1].trim()
            if (name.length > 3 && name.length < 50) {
                return name
            }
        }

        // If no pattern matches, return null
        null
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "Error extracting RCS sender: ${e.message}")
        null
    }
}

/**
 * Gets the text content of an RCS/MMS message from its parts
 */
private fun getRcsMessageText(messageId: Long): String? {
    return try {
        // First, let's see what parts exist for this message
        val partsCursor = applicationContext.contentResolver.query(
            Uri.parse("content://mms/part"),
            arrayOf("_id", "ct", "text", "_data"),
            "mid = ?",
            arrayOf(messageId.toString()),
            null
        )

        partsCursor?.use { cursor ->
            while (cursor.moveToNext()) {
                val partId = cursor.getLong(cursor.getColumnIndexOrThrow("_id"))
                val ctIndex = cursor.getColumnIndex("ct")
                val contentType = if (ctIndex >= 0) cursor.getString(ctIndex) ?: "" else ""

                // Look for text content
                if (contentType.startsWith("text/") || contentType == "application/smil") {
                    // Try to get text directly from the text column
                    val textIndex = cursor.getColumnIndex("text")
                    if (textIndex >= 0) {
                        val text = cursor.getString(textIndex)
                        if (!text.isNullOrEmpty()) {
                            return text
                        }
                    }

                    // Try to read from _data path (file storage)
                    val dataIndex = cursor.getColumnIndex("_data")
                    if (dataIndex >= 0) {
                        val dataPath = cursor.getString(dataIndex)
                        if (!dataPath.isNullOrEmpty()) {
                            // Try to read the file
                            try {
                                val partUri = Uri.parse("content://mms/part/$partId")
                                val inputStream =
                                    applicationContext.contentResolver.openInputStream(partUri)
                                val text = inputStream?.bufferedReader()?.use { it.readText() }
                                if (!text.isNullOrEmpty()) {
                                    return text
                                }
                            } catch (e: kotlinx.coroutines.CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                // Ignore read errors
                            }
                        }
                    }
                }
            }
        }

        null
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "Error getting RCS message text: ${e.message}", e)
        null
    }
}

/**
 * Extracts text content from RCS JSON (Rich Cards)
 */
private fun extractTextFromRcsJson(json: String): String? {
    return try {
        val jsonObject = org.json.JSONObject(json)
        val texts = mutableListOf<String>()

        // Navigate through the JSON structure to find text
        fun extractTexts(obj: Any?, depth: Int = 0) {
            if (depth > 10) return // Prevent infinite recursion

            when (obj) {
                is org.json.JSONObject -> {
                    // Priority order for text fields
                    val textFields = listOf(
                        "text",           // Plain text message
                        "message",        // Message body
                        "body",           // Body content
                        "title",          // Card title
                        "description",    // Card description
                        "content",        // Content field
                        "caption"         // Media caption
                    )

                    for (field in textFields) {
                        if (obj.has(field)) {
                            val value = obj.getString(field)
                            if (value.isNotEmpty() && !value.startsWith("{")) {
                                texts.add(value)
                            }
                        }
                    }

                    // Recursively search nested objects
                    obj.keys().forEach { key ->
                        if (key !in listOf("media", "suggestions", "postback", "urlAction")) {
                            try {
                                extractTexts(obj.get(key), depth + 1)
                            } catch (e: kotlinx.coroutines.CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                // Skip problematic fields
                            }
                        }
                    }
                }

                is org.json.JSONArray -> {
                    for (i in 0 until obj.length()) {
                        extractTexts(obj.get(i), depth + 1)
                    }
                }
            }
        }

        // Check if it's a simple text message (not a rich card)
        if (jsonObject.has("text")) {
            return jsonObject.getString("text")
        }

        // Check for message.text structure
        if (jsonObject.has("message")) {
            val message = jsonObject.getJSONObject("message")
            if (message.has("text")) {
                return message.getString("text")
            }
        }

        // Extract from complex structures
        extractTexts(jsonObject)

        // Combine all found texts
        if (texts.isNotEmpty()) {
            return texts.distinct().joinToString(" | ")
        }

        // If no text found, it might be a media-only message
        null
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "Error parsing RCS JSON: ${e.message}")
        // Not JSON, return as plain text
        json
    }
}

private data class SmsMessage(
    val id: Long,
    val sender: String,
    val timestamp: Long,
    val body: String,
    val type: Int
)
}
