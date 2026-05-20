package com.ritesh.cashiro.worker

import android.content.Context
import android.net.Uri
import android.provider.Telephony
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import com.ritesh.cashiro.data.database.entity.TransactionType
import com.ritesh.cashiro.data.mapper.toEntity
import com.ritesh.cashiro.data.mapper.toEntityType
import com.ritesh.cashiro.data.preferences.UserPreferencesRepository
import com.ritesh.cashiro.data.repository.AccountBalanceRepository
import com.ritesh.cashiro.data.repository.CardRepository
import com.ritesh.cashiro.data.repository.LlmRepository
import com.ritesh.cashiro.data.repository.MerchantMappingRepository
import com.ritesh.cashiro.data.repository.SubscriptionRepository
import com.ritesh.cashiro.data.repository.TransactionRepository
import com.ritesh.cashiro.data.repository.UnrecognizedSmsRepository
import com.ritesh.cashiro.domain.repository.RuleRepository
import com.ritesh.cashiro.domain.service.RuleEngine
import com.ritesh.cashiro.data.manager.TransactionDeduplication
import com.ritesh.cashiro.utils.CurrencyFormatter
import com.ritesh.parser.core.ParsedTransaction
import com.ritesh.parser.core.bank.BankParserFactory
import com.ritesh.parser.core.bank.FederalBankParser
import com.ritesh.parser.core.bank.HDFCBankParser
import com.ritesh.parser.core.bank.IndianBankParser
import com.ritesh.parser.core.bank.IndusIndBankParser
import com.ritesh.parser.core.bank.SBIBankParser
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.util.concurrent.atomic.AtomicInteger

@HiltWorker
class OptimizedSmsReaderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val transactionRepository: TransactionRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val accountBalanceRepository: AccountBalanceRepository,
    private val cardRepository: CardRepository,
    private val llmRepository: LlmRepository,
    private val merchantMappingRepository: MerchantMappingRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val unrecognizedSmsRepository: UnrecognizedSmsRepository,
    private val ruleRepository: RuleRepository,
    private val ruleEngine: RuleEngine
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val TAG = "OptimizedSmsReaderWorker"
        const val WORK_NAME = "optimized_sms_reader_work"
        const val INPUT_FORCE_RESYNC = "input_force_resync"
        const val PROGRESS_TOTAL = "progress_total"
        const val PROGRESS_PROCESSED = "progress_processed"
        const val PROGRESS_PARSED = "progress_parsed"
        const val PROGRESS_SAVED = "progress_saved"
        const val PROGRESS_BLOCKED = "progress_blocked"
        const val PROGRESS_TIME_ELAPSED = "progress_time_elapsed"
        const val PROGRESS_ESTIMATED_TIME_REMAINING = "progress_estimated_time_remaining"
        const val PROGRESS_ETA_SECONDS = "progress_eta_seconds"
        const val PROGRESS_MSG_PER_SEC = "progress_msg_per_sec"
        const val PROGRESS_CURRENT_BATCH = "progress_current_batch"
        const val PROGRESS_TOTAL_BATCHES = "progress_total_batches"
        const val PROGRESS_REPORT_FREQUENCY = 25
        private val SMS_PROJECTION = arrayOf(Telephony.Sms._ID, Telephony.Sms.ADDRESS, Telephony.Sms.DATE, Telephony.Sms.BODY, Telephony.Sms.TYPE)
    }

    private class ProcessingStats(val total: Int) {
        val processed = AtomicInteger(0)
        val parsed = AtomicInteger(0)
        val saved = AtomicInteger(0)
        val blocked = AtomicInteger(0)
        val duplicates = AtomicInteger(0)
        val subscription = AtomicInteger(0)
        private val startMs = System.currentTimeMillis()

        fun elapsedMs() = System.currentTimeMillis() - startMs
        fun msgPerSec(): Double {
            val sec = elapsedMs() / 1000.0
            return if (sec > 0) processed.get() / sec else 0.0
        }

        fun etaSec(): Long {
            val rate = msgPerSec()
            return if (rate > 0) ((total - processed.get()) / rate).toLong() else 0L
        }
    }

    private data class SmsMessage(
        val id: Long,
        val sender: String,
        val timestamp: Long,
        val body: String,
        val type: Int
    )

    private enum class SaveOutcome {
        SAVED,
        UPDATED_DUPLICATE,
        SKIPPED_DUPLICATE,
        SKIPPED
    }

    private sealed interface ParseResult {
        data class Regular(val parsed: ParsedTransaction, val sms: SmsMessage) : ParseResult
        data class SpecialNotification(val sms: SmsMessage, val action: suspend () -> Unit) : ParseResult
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val forceResync = inputData.getBoolean(INPUT_FORCE_RESYNC, false)
            Log.i(TAG, "Start — forceResync=$forceResync")

            if (forceResync) {
                transactionRepository.deleteAllTransactions()
                accountBalanceRepository.deleteAllBalances()
            }

            val messages = readSmsMessages(forceResync)
            if (messages.isEmpty()) { Log.i(TAG, "No messages to process"); return@withContext Result.success() }

            val stats = ProcessingStats(messages.size)
            Log.i(TAG, "Messages to process: ${messages.size}")

            processPipeline(messages, stats)
            cleanUpAndFinalize(stats)

            Log.i(TAG, buildSummary(stats))
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Fatal error", e)
            Result.failure()
        }
    }

    private suspend fun processPipeline(messages: List<SmsMessage>, stats: ProcessingStats) = coroutineScope {
        val merchantMappingCache = merchantMappingRepository.getAllMappingsAsMap()
        val ruleCache = mapOf(
            com.ritesh.cashiro.data.database.entity.TransactionType.INCOME to
                    ruleRepository.getActiveRulesByType(com.ritesh.cashiro.data.database.entity.TransactionType.INCOME),
            com.ritesh.cashiro.data.database.entity.TransactionType.EXPENSE to
                    ruleRepository.getActiveRulesByType(com.ritesh.cashiro.data.database.entity.TransactionType.EXPENSE),
            com.ritesh.cashiro.data.database.entity.TransactionType.CREDIT to
                    ruleRepository.getActiveRulesByType(com.ritesh.cashiro.data.database.entity.TransactionType.CREDIT),
            com.ritesh.cashiro.data.database.entity.TransactionType.TRANSFER to
                    ruleRepository.getActiveRulesByType(com.ritesh.cashiro.data.database.entity.TransactionType.TRANSFER),
            com.ritesh.cashiro.data.database.entity.TransactionType.INVESTMENT to
                    ruleRepository.getActiveRulesByType(com.ritesh.cashiro.data.database.entity.TransactionType.INVESTMENT)
        )

        val parseChannel = Channel<ParseResult>(capacity = Channel.BUFFERED)
        val unrecognizedBatch = ArrayList<SmsMessage>()

        val parseJob = launch(Dispatchers.IO) {
            for (sms in messages) {
                stats.processed.incrementAndGet()

                val senderUpper = sms.sender.uppercase()
                val isKnownBank = BankParserFactory.isKnownBankSender(sms.sender)
                if ((senderUpper.endsWith("-P") || senderUpper.endsWith("-G")) && !isKnownBank) continue

                val parser = BankParserFactory.getParser(sms.sender)
                if (parser == null) {
                    val upperSender = sms.sender.uppercase()
                    if (upperSender.endsWith("-T") || upperSender.endsWith("-S")) {
                        unrecognizedBatch.add(sms)
                    }
                    continue
                }

                val subscriptionResult = processSubscription(parser, sms)
                if (subscriptionResult != null) {
                    parseChannel.send(subscriptionResult)
                    continue
                }

                val parsed = parser.parse(sms.body, sms.sender, sms.timestamp)
                if (parsed != null) {
                    stats.parsed.incrementAndGet()
                    parseChannel.send(ParseResult.Regular(parsed, sms))
                }

                if (stats.processed.get() % PROGRESS_REPORT_FREQUENCY == 0) {
                    reportProgress(stats)
                }
            }
            parseChannel.close()
        }

        val saveJob = launch(Dispatchers.IO) {
            for (result in parseChannel) {
                when (result) {
                    is ParseResult.SpecialNotification -> {
                        result.action()
                        stats.subscription.incrementAndGet()
                    }
                    is ParseResult.Regular -> {
                        when (saveTransaction(result.parsed, result.sms, merchantMappingCache, ruleCache, stats)) {
                            SaveOutcome.SAVED -> {
                                stats.saved.incrementAndGet()
                            }
                            SaveOutcome.UPDATED_DUPLICATE -> {
                                stats.duplicates.incrementAndGet()
                            }
                            SaveOutcome.SKIPPED_DUPLICATE -> {
                                stats.duplicates.incrementAndGet()
                            }
                            SaveOutcome.SKIPPED -> Unit
                        }
                    }
                }
            }
        }

        parseJob.join()
        saveJob.join()

        flushUnrecognizedBatch(unrecognizedBatch)
        reportProgress(stats)
    }

    private suspend fun processSubscription(parser: com.ritesh.parser.core.bank.BankParser, sms: SmsMessage): ParseResult? {
        val smsDateTime = sms.timestamp.toLocalDateTime()
        val thirtyDaysAgo = java.time.LocalDateTime.now().minusDays(30)
        val isRecent = smsDateTime.isAfter(thirtyDaysAgo)

        return when (parser) {
            is SBIBankParser -> {
                if (!parser.isUPIMandateNotification(sms.body)) null
                else if (!isRecent) null
                else parser.parseUPIMandateSubscription(sms.body)?.let { info ->
                    ParseResult.SpecialNotification(sms) {
                        subscriptionRepository.createOrUpdateFromSBIMandate(info, parser.getBankName(), sms.body)
                    }
                }
            }

            is FederalBankParser -> {
                if (parser.isMandateCreationNotification(sms.body)) {
                    parser.parseEMandateSubscription(sms.body)?.let { info ->
                        return ParseResult.SpecialNotification(sms) {
                            subscriptionRepository.createOrUpdateFromFederalBankMandate(info, parser.getBankName(), sms.body)
                        }
                    }
                }
                val futureDebitInfo = parser.parseFutureDebit(sms.body)
                if (futureDebitInfo != null) {
                    val isFuture = try {
                        val d = java.time.LocalDate.parse(futureDebitInfo.nextDeductionDate, java.time.format.DateTimeFormatter.ofPattern(futureDebitInfo.dateFormat))
                        d.isAfter(java.time.LocalDate.now())
                    } catch (_: Exception) { false }
                    if (!isFuture && !isRecent) null
                    else ParseResult.SpecialNotification(sms) {
                        subscriptionRepository.createOrUpdateFromFederalBankMandate(futureDebitInfo, parser.getBankName(), sms.body)
                    }
                } else null
            }

            is HDFCBankParser -> {
                var result: ParseResult? = null
                if (parser.isEMandateNotification(sms.body) && isRecent) {
                    result = parser.parseEMandateSubscription(sms.body)?.let { info ->
                        ParseResult.SpecialNotification(sms) {
                            subscriptionRepository.createOrUpdateFromEMandate(info, parser.getBankName(), sms.body)
                        }
                    }
                }
                if (result == null && parser.isFutureDebitNotification(sms.body) && isRecent) {
                    result = parser.parseFutureDebit(sms.body)?.let { info ->
                        ParseResult.SpecialNotification(sms) {
                            subscriptionRepository.createOrUpdateFromEMandate(info, parser.getBankName(), sms.body)
                        }
                    }
                }
                if (result == null && parser.isBalanceUpdateNotification(sms.body)) {
                    result = parser.parseBalanceUpdate(sms.body)?.let { info ->
                        ParseResult.SpecialNotification(sms) {
                            accountBalanceRepository.insertBalanceUpdate(
                                bankName = info.bankName,
                                accountLast4 = info.accountLast4 ?: "XXXX",
                                balance = info.balance,
                                timestamp = info.asOfDate ?: smsDateTime,
                                currency = parser.getCurrency()
                            )
                        }
                    }
                }
                result
            }

            is IndianBankParser -> {
                if (!parser.isMandateNotification(sms.body)) null
                else if (!isRecent) null
                else parser.parseMandateSubscription(sms.body)?.let { info ->
                    ParseResult.SpecialNotification(sms) {
                        subscriptionRepository.createOrUpdateFromIndianBankMandate(info, parser.getBankName(), sms.body)
                    }
                }
            }

            is IndusIndBankParser -> {
                if (!parser.isBalanceUpdateNotification(sms.body)) null
                else parser.parseBalanceUpdate(sms.body)?.let { info ->
                    ParseResult.SpecialNotification(sms) {
                        accountBalanceRepository.insertBalanceUpdate(
                            bankName = info.bankName,
                            accountLast4 = info.accountLast4 ?: "XXXX",
                            balance = info.balance,
                            timestamp = info.asOfDate ?: smsDateTime,
                            currency = parser.getCurrency()
                        )
                    }
                }
            }

            else -> null
        }
    }

    private suspend fun saveTransaction(
        parsed: ParsedTransaction,
        sms: SmsMessage,
        merchantMappingCache: Map<String, String>,
        ruleCache: Map<com.ritesh.cashiro.data.database.entity.TransactionType, List<com.ritesh.cashiro.domain.model.rule.TransactionRule>>,
        stats: ProcessingStats
    ): SaveOutcome {
        return try {
            val entity = parsed.toEntity()
            if (transactionRepository.getTransactionByHash(entity.transactionHash) != null) return SaveOutcome.SKIPPED

            val customCategory = merchantMappingCache[entity.merchantName]
            val mapped = if (customCategory != null) entity.copy(category = customCategory) else entity
            val activeRules = ruleCache[mapped.transactionType] ?: emptyList()
            if (ruleEngine.shouldBlockTransaction(mapped, sms.body, activeRules) != null) {
                stats.blocked.incrementAndGet()
                return SaveOutcome.SKIPPED
            }

            val (withRules, ruleApps) = ruleEngine.evaluateRules(mapped, sms.body, activeRules)
            val matchedSub = subscriptionRepository.matchTransactionToSubscription(withRules.merchantName, withRules.amount)
            val finalEntity = if (matchedSub != null) {
                subscriptionRepository.updateNextPaymentDateAfterCharge(matchedSub.id, withRules.dateTime.toLocalDate())
                withRules.copy(isRecurring = true)
            } else withRules

            val duplicate = transactionRepository.findPotentialDuplicates(finalEntity).firstOrNull()
            if (duplicate != null) {
                if (TransactionDeduplication.shouldReplaceWithIncoming(duplicate, finalEntity)) {
                    val replacement = finalEntity.copy(
                        id = duplicate.id,
                        transactionHash = duplicate.transactionHash,
                        isRecurring = duplicate.isRecurring || finalEntity.isRecurring,
                        createdAt = duplicate.createdAt
                    )
                    transactionRepository.updateTransaction(replacement)
                    accountBalanceRepository.deleteBalancesForTransaction(duplicate.id)
                    replaceRuleApplications(duplicate.id, ruleApps)
                    processBalanceUpdate(parsed, replacement, duplicate.id)
                    return SaveOutcome.UPDATED_DUPLICATE
                }
                return SaveOutcome.SKIPPED_DUPLICATE
            }

            val rowId = transactionRepository.insertTransaction(finalEntity)
            if (rowId == -1L) return SaveOutcome.SKIPPED

            saveRuleApplications(rowId, ruleApps)
            processBalanceUpdate(parsed, finalEntity, rowId)
            SaveOutcome.SAVED

        } catch (e: Exception) {
            Log.e(TAG, "Error saving transaction: ${e.message}")
            SaveOutcome.SKIPPED
        }
    }

    private suspend fun processBalanceUpdate(parsed: ParsedTransaction, entity: com.ritesh.cashiro.data.database.entity.TransactionEntity, rowId: Long) {
        val accountLast4 = parsed.accountLast4 ?: return
        val card = if (parsed.isFromCard) {
            cardRepository.getCard(parsed.bankName, accountLast4)
                ?: cardRepository.findOrCreateCard(accountLast4, parsed.bankName, parsed.type.toEntityType() == TransactionType.CREDIT)
                ?.also { c ->
                    cardRepository.getCard(parsed.bankName, accountLast4)
                }
                ?.also { c ->
                    cardRepository.updateCardBalance(c.id, parsed.balance, parsed.smsBody.take(200), parsed.timestamp.toLocalDateTime())
                }
        } else null

        val targetAccount = when {
            card == null -> accountLast4
            card.cardType == com.ritesh.cashiro.data.database.entity.CardType.CREDIT -> accountLast4
            card.cardType == com.ritesh.cashiro.data.database.entity.CardType.DEBIT && card.accountLast4 != null -> card.accountLast4
            else -> return
        }

        val isCreditCard = card?.cardType == com.ritesh.cashiro.data.database.entity.CardType.CREDIT || parsed.type.toEntityType() == TransactionType.CREDIT
        val existing = accountBalanceRepository.getLatestBalance(parsed.bankName, targetAccount)

        val newBalance = when {
            parsed.balance != null -> parsed.balance!!
            isCreditCard -> (existing?.balance ?: BigDecimal.ZERO) + parsed.amount
            existing?.isCreditCard == true && parsed.type.toEntityType() == TransactionType.INCOME ->
                (existing.balance - parsed.amount).max(BigDecimal.ZERO)
            else -> {
                val cur = existing?.balance ?: BigDecimal.ZERO
                when (parsed.type.toEntityType()) {
                    TransactionType.INCOME -> cur + parsed.amount
                    TransactionType.EXPENSE, TransactionType.INVESTMENT -> (cur - parsed.amount).max(BigDecimal.ZERO)
                    else -> cur
                }
            }
        }

        val balanceEntity = AccountBalanceEntity(
            bankName = parsed.bankName,
            accountLast4 = targetAccount,
            balance = newBalance,
            timestamp = entity.dateTime,
            transactionId = rowId,
            creditLimit = existing?.creditLimit,
            isCreditCard = isCreditCard || (existing?.isCreditCard ?: false),
            smsSource = parsed.smsBody.take(500),
            sourceType = "TRANSACTION",
            currency = parsed.currency
        )
        accountBalanceRepository.insertBalance(balanceEntity)
        Log.i(TAG, "Balance saved for ${parsed.bankName} **$targetAccount: ${CurrencyFormatter.formatCurrency(newBalance, parsed.currency)}")
    }

    private suspend fun replaceRuleApplications(
        transactionId: Long,
        ruleApps: List<com.ritesh.cashiro.domain.model.rule.RuleApplication>
    ) {
        ruleRepository.deleteRuleApplicationsForTransaction(transactionId.toString())
        saveRuleApplications(transactionId, ruleApps)
    }

    private suspend fun saveRuleApplications(
        transactionId: Long,
        ruleApps: List<com.ritesh.cashiro.domain.model.rule.RuleApplication>
    ) {
        if (ruleApps.isEmpty()) return
        ruleRepository.saveRuleApplications(
            ruleApps.map { it.copy(transactionId = transactionId.toString()) }
        )
    }

    private suspend fun flushUnrecognizedBatch(batch: ArrayList<SmsMessage>) {
        for (sms in batch) {
            try {
                if (!unrecognizedSmsRepository.exists(sms.sender, sms.body)) {
                    unrecognizedSmsRepository.insert(
                        com.ritesh.cashiro.data.database.entity.UnrecognizedSmsEntity(
                            sender = sms.sender,
                            smsBody = sms.body,
                            receivedAt = sms.timestamp.toLocalDateTime()
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error storing unrecognized SMS: ${e.message}")
            }
        }
        batch.clear()
    }

    private suspend fun cleanUpAndFinalize(stats: ProcessingStats) {
        try { unrecognizedSmsRepository.cleanupOldEntries() } catch (e: Exception) { Log.e(TAG, "Cleanup error: ${e.message}") }
        try {
            val deletedDuplicates = cleanupExistingGPayDuplicates()
            if (deletedDuplicates > 0) {
                Log.i(TAG, "Cleaned up $deletedDuplicates existing GPay duplicate transactions")
            }
        } catch (e: Exception) {
            Log.e(TAG, "GPay duplicate cleanup error: ${e.message}")
        }
        if (stats.saved.get() > 0) {
            try { llmRepository.updateSystemPrompt() } catch (e: Exception) { Log.e(TAG, "Prompt update error: ${e.message}") }
        }
    }

    private suspend fun cleanupExistingGPayDuplicates(): Int {
        val duplicateIds = transactionRepository.findGPayDuplicateIdsForCleanup()
        duplicateIds.forEach { id ->
            transactionRepository.deleteTransactionById(id)
            accountBalanceRepository.deleteBalancesForTransaction(id)
            ruleRepository.deleteRuleApplicationsForTransaction(id.toString())
        }
        return duplicateIds.size
    }

    private suspend fun reportProgress(stats: ProcessingStats) {
        try {
            setProgress(workDataOf(
                PROGRESS_TOTAL to stats.total,
                PROGRESS_PROCESSED to stats.processed.get(),
                PROGRESS_PARSED to stats.parsed.get(),
                PROGRESS_SAVED to stats.saved.get(),
                PROGRESS_BLOCKED to stats.blocked.get(),
                PROGRESS_TIME_ELAPSED to stats.elapsedMs(),
                PROGRESS_ESTIMATED_TIME_REMAINING to (stats.etaSec() * 1000L),
                PROGRESS_ETA_SECONDS to stats.etaSec(),
                PROGRESS_MSG_PER_SEC to stats.msgPerSec(),
                PROGRESS_CURRENT_BATCH to stats.processed.get(),
                PROGRESS_TOTAL_BATCHES to stats.total
            ))
        } catch (_: Exception) {}
    }

    private suspend fun readSmsMessages(forceResync: Boolean = false): List<SmsMessage> {
        val messages = mutableListOf<SmsMessage>()
        try {
            val lastScanTimestamp = userPreferencesRepository.getLastScanTimestamp().first() ?: 0L
            val scanMonths = userPreferencesRepository.getSmsScanMonths()
            val scanAllTime = userPreferencesRepository.getSmsScanAllTime()
            val lastScanPeriod = userPreferencesRepository.getLastScanPeriod().first() ?: 0
            val now = System.currentTimeMillis()

            val scanAllTimeToggled = scanAllTime && lastScanPeriod != -1
            val scanAllTimeToggledOff = !scanAllTime && lastScanPeriod == -1
            val needsFullScan = forceResync || lastScanTimestamp == 0L || (lastScanPeriod >= 0 && scanMonths > lastScanPeriod) || scanAllTimeToggled || scanAllTimeToggledOff

            val scanStartTime = if (needsFullScan) {
                java.util.Calendar.getInstance().apply {
                    if (scanAllTime) add(java.util.Calendar.YEAR, -10)
                    else add(java.util.Calendar.MONTH, -scanMonths)
                    set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
                }.timeInMillis
            } else {
                val threeDaysAgo = now - 3 * 24 * 60 * 60 * 1000L
                val periodLimit = java.util.Calendar.getInstance().apply { add(java.util.Calendar.MONTH, -scanMonths) }.timeInMillis
                maxOf(minOf(lastScanTimestamp, threeDaysAgo), periodLimit)
            }

            applicationContext.contentResolver.query(
                Telephony.Sms.CONTENT_URI, SMS_PROJECTION,
                "${Telephony.Sms.TYPE} = ? AND ${Telephony.Sms.DATE} >= ?",
                arrayOf(Telephony.Sms.MESSAGE_TYPE_INBOX.toString(), scanStartTime.toString()),
                "${Telephony.Sms.DATE} ASC"
            )?.use { c ->
                val idIdx = c.getColumnIndexOrThrow(Telephony.Sms._ID)
                val addressIdx = c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val dateIdx = c.getColumnIndexOrThrow(Telephony.Sms.DATE)
                val bodyIdx = c.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val typeIdx = c.getColumnIndexOrThrow(Telephony.Sms.TYPE)
                while (c.moveToNext()) {
                    messages.add(SmsMessage(c.getLong(idIdx), c.getString(addressIdx) ?: "", c.getLong(dateIdx), c.getString(bodyIdx) ?: "", c.getInt(typeIdx)))
                }
            }

            userPreferencesRepository.setLastScanTimestamp(now)
            if (needsFullScan) userPreferencesRepository.setLastScanPeriod(if (scanAllTime) -1 else scanMonths)

            try { messages.addAll(readRcsMessages(scanStartTime / 1000)) } catch (e: Exception) { Log.e(TAG, "RCS read error: ${e.message}") }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading SMS", e)
        }
        Log.i(TAG, "Loaded ${messages.size} messages (SMS + RCS)")
        return messages
    }

    private fun readRcsMessages(scanStartSeconds: Long): List<SmsMessage> {
        val result = mutableListOf<SmsMessage>()
        applicationContext.contentResolver.query(
            Uri.parse("content://mms"), arrayOf("_id", "thread_id", "date", "tr_id", "m_id"),
            "date >= ?", arrayOf(scanStartSeconds.toString()), "date DESC"
        )?.use { c ->
            while (c.moveToNext()) {
                val messageId = c.getLong(c.getColumnIndexOrThrow("_id"))
                val date = c.getLong(c.getColumnIndexOrThrow("date"))
                val trId = c.getColumnIndex("tr_id").takeIf { it >= 0 }?.let { c.getString(it) } ?: continue
                if (!trId.startsWith("proto:")) continue
                val sender = extractRcsSender(trId) ?: continue
                if (!sender.uppercase().contains("PUNJAB NATIONAL BANK")) continue
                var text = getRcsMessageText(messageId) ?: continue
                if (text.trim().startsWith("{")) text = extractTextFromRcsJson(text) ?: continue
                result.add(SmsMessage(messageId, sender, date * 1000, text, Telephony.Sms.MESSAGE_TYPE_INBOX))
            }
        }
        return result
    }

    private fun extractRcsSender(trId: String): String? = try {
        val decoded = String(android.util.Base64.decode(trId.removePrefix("proto:"), android.util.Base64.DEFAULT))
        Regex("""([a-z_]+)_[a-z0-9]+_agent@rbm\.goog""").find(decoded)?.let { m ->
            return m.groupValues[1].split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
        }
        Regex("""[\x12\x1a][\x00-\x20]([A-Za-z][A-Za-z\s]+)""").find(decoded)?.let { m ->
            val name = m.groupValues[1].trim()
            if (name.length in 4..49) return name
        }
        null
    } catch (_: Exception) { null }

    private fun getRcsMessageText(messageId: Long): String? = try {
        applicationContext.contentResolver.query(Uri.parse("content://mms/part"), null, "mid = ?", arrayOf(messageId.toString()), null)?.use { c ->
            while (c.moveToNext()) {
                val ct = c.getColumnIndex("ct").takeIf { it >= 0 }?.let { c.getString(it) } ?: continue
                if (!ct.startsWith("text/") && ct != "application/smil") continue
                c.getColumnIndex("text").takeIf { it >= 0 }?.let { idx ->
                    c.getString(idx)?.takeIf { it.isNotEmpty() }?.let { return it }
                }
                val partId = c.getLong(c.getColumnIndexOrThrow("_id"))
                try {
                    applicationContext.contentResolver.openInputStream(Uri.parse("content://mms/part/$partId"))
                        ?.bufferedReader()?.use { it.readText() }?.takeIf { it.isNotEmpty() }?.let { return it }
                } catch (_: Exception) {}
            }
            null
        }
    } catch (_: Exception) { null }

    private fun extractTextFromRcsJson(json: String): String? = try {
        val obj = org.json.JSONObject(json)
        obj.optString("text").takeIf { it.isNotEmpty() }
            ?: obj.optJSONObject("message")?.optString("text")?.takeIf { it.isNotEmpty() }
            ?: run {
                val texts = mutableListOf<String>()
                val skipKeys = setOf("media", "suggestions", "postback", "urlAction")
                val textKeys = listOf("text", "message", "body", "title", "description", "content", "caption")
                fun extract(any: Any?, depth: Int = 0) {
                    if (depth > 10) return
                    when (any) {
                        is org.json.JSONObject -> {
                            textKeys.forEach { k -> any.optString(k).takeIf { it.isNotEmpty() && !it.startsWith("{") }?.let { texts.add(it) } }
                            any.keys().forEach { k -> if (k !in skipKeys) try { extract(any.get(k), depth + 1) } catch (_: Exception) {} }
                        }
                        is org.json.JSONArray -> for (i in 0 until any.length()) extract(any.get(i), depth + 1)
                    }
                }
                extract(obj)
                texts.distinct().joinToString(" | ").takeIf { it.isNotEmpty() }
            }
    } catch (_: Exception) { json }

    private fun buildSummary(stats: ProcessingStats) = """
        ┌─────── SMS Worker Complete ──────────────────
        │  Total     : ${stats.total}
        │  Processed : ${stats.processed.get()}
        │  Parsed    : ${stats.parsed.get()}
        │  Saved     : ${stats.saved.get()}
        │  Duplicates: ${stats.duplicates.get()}
        │  Elapsed   : ${stats.elapsedMs()}ms
        │  Speed     : ${"%.1f".format(stats.msgPerSec())} msg/s
        └──────────────────────────────────────────────
    """.trimIndent()
}

private fun Long.toLocalDateTime() = java.time.LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(this), java.time.ZoneId.systemDefault())
