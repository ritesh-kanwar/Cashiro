package com.ritesh.cashiro.presentation.ui.features.settings.dataprivacy

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.ritesh.cashiro.data.statement.StatementTransactionEnricher
import com.ritesh.cashiro.data.mapper.toEntity
import com.ritesh.cashiro.data.preferences.BankAccountMergeStore
import kotlinx.coroutines.sync.withLock
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ritesh.cashiro.data.backup.BackupConfiguration
import com.ritesh.cashiro.data.backup.BackupExporter
import com.ritesh.cashiro.data.backup.BackupImporter
import com.ritesh.cashiro.data.backup.CashewImporter
import com.ritesh.cashiro.data.backup.ExportResult
import com.ritesh.cashiro.data.backup.ImportResult
import com.ritesh.cashiro.data.backup.ImportStrategy
import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import com.ritesh.cashiro.data.database.entity.TransactionEntity
import com.ritesh.cashiro.data.database.entity.TransactionType
import com.ritesh.cashiro.data.csv.CsvImportService
import com.ritesh.cashiro.data.parser.pdf.PaytmPdfParser
import com.ritesh.cashiro.data.parser.pdf.SlicePdfParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import com.ritesh.cashiro.R
import com.ritesh.cashiro.data.parser.pdf.GPayPdfParser
import com.ritesh.cashiro.data.parser.pdf.PhonePePdfParser
import com.ritesh.cashiro.data.preferences.UserPreferencesRepository
import com.ritesh.cashiro.data.repository.AccountBalanceRepository
import com.ritesh.cashiro.data.repository.TransactionRepository
import com.ritesh.cashiro.domain.repository.RuleRepository
import com.ritesh.cashiro.domain.service.RuleEngine
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class DataPrivacyViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupExporter: BackupExporter,
    private val backupImporter: BackupImporter,
    private val cashewImporter: CashewImporter,
    private val csvImportService: CsvImportService,
    private val transactionRepository: TransactionRepository,
    private val accountBalanceRepository: AccountBalanceRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val ruleRepository: RuleRepository,
    private val ruleEngine: RuleEngine
) : ViewModel() {

    private val sharedPrefs = context.getSharedPreferences("account_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(DataPrivacyUiState())
    val uiState: StateFlow<DataPrivacyUiState> = _uiState.asStateFlow()

    fun exportBackup(config: BackupConfiguration) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(importExportMessage = "Creating backup...") }
                when (val result = backupExporter.exportBackup(config)) {
                    is ExportResult.Success -> {
                        _uiState.update { it.copy(
                            exportedBackupFile = result.file,
                            importExportMessage = "Backup created successfully! Choose where to save it."
                        ) }
                    }
                    is ExportResult.Error -> {
                        _uiState.update { it.copy(importExportMessage = "Export failed: ${result.message}") }
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(importExportMessage = "Export error: ${e.message}") }
            }
        }
    }

    fun saveBackupToFile(uri: Uri) {
        viewModelScope.launch {
            try {
                _uiState.value.exportedBackupFile?.let { file ->
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        file.inputStream().use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    _uiState.update { it.copy(
                        importExportMessage = "Backup saved successfully!",
                        exportedBackupFile = null
                    ) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(importExportMessage = "Failed to save backup: ${e.message}") }
            }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(importExportMessage = "Importing backup...") }
                when (val result = backupImporter.importBackup(uri, ImportStrategy.MERGE)) {
                    is ImportResult.Success -> {
                        _uiState.update { it.copy(importExportMessage = "Import successful! Imported ${result.importedTransactions} transactions, ${result.importedCategories} categories.") }
                        maybePromptMainAccountSelection(result.importedAccounts)
                    }
                    is ImportResult.Error -> {
                        _uiState.update { it.copy(importExportMessage = "Import failed: ${result.message}") }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(importExportMessage = "Import error: ${e.message}") }
            }
        }
    }

    fun importCsv(uri: Uri) {
        if (_uiState.value.isCsvProcessing) return
        _uiState.update { it.copy(isCsvProcessing = true) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use {
                    csvImportService.importCsv(it)
                } ?: error("Cannot open CSV")
                val message = if (result.importedCount == 0 && result.duplicateCount == 0 && result.failureReasons.isNotEmpty()) {
                    context.getString(R.string.csv_import_error)
                } else {
                    context.getString(R.string.csv_import_result, result.importedCount, result.duplicateCount, result.failedCount)
                }
                _uiState.update { it.copy(importExportMessage = message) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update { it.copy(importExportMessage = context.getString(R.string.csv_import_error)) }
            } finally {
                _uiState.update { it.copy(isCsvProcessing = false) }
            }
        }
    }

    fun importCashew(uri: Uri) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(importExportMessage = "Importing from Cashew...") }
                when (val result = cashewImporter.importCashew(uri)) {
                    is ImportResult.Success -> {
                        val attachmentSuffix = if (result.importedAttachments > 0 || result.failedAttachments > 0) {
                            ", ${result.importedAttachments} attachments" +
                                (if (result.failedAttachments > 0) " (${result.failedAttachments} failed)" else "")
                        } else ""
                        _uiState.update { it.copy(importExportMessage = "Cashew import successful! Imported ${result.importedTransactions} transactions, ${result.importedCategories} categories$attachmentSuffix.") }
                        maybePromptMainAccountSelection(result.importedAccounts)
                    }
                    is ImportResult.Error -> {
                        _uiState.update { it.copy(importExportMessage = "Cashew import failed: ${result.message}") }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(importExportMessage = "Cashew import error: ${e.message}") }
            }
        }
    }

    /**
     * After a successful import, if the backup contained multiple accounts and the user
     * does not currently have a valid main account (e.g. a Cashew backup with several
     * wallets imported into a fresh app), prompt them to pick which account should act
     * as the main account.
     */
    private suspend fun maybePromptMainAccountSelection(importedAccountCount: Int) {
        if (importedAccountCount < 2) return

        val mainAccountKey = sharedPrefs.getString("main_account", null)
        val accounts = accountBalanceRepository.getAllLatestBalances().first()

        // Only prompt when there is no main account, or the configured main account no
        // longer refers to an existing account (stale/invalid).
        val mainAccountValid = accounts.any {
            "${it.bankName}_${it.accountLast4}" == mainAccountKey
        }
        if (mainAccountKey.isNullOrBlank() || !mainAccountValid) {
            _uiState.update {
                it.copy(showMainAccountSelection = true, mainAccountSelectionAccounts = accounts)
            }
        }
    }

    fun selectMainAccount(account: AccountBalanceEntity) {
        viewModelScope.launch {
            val key = "${account.bankName}_${account.accountLast4}"
            sharedPrefs.edit { putString("main_account", key) }
            userPreferencesRepository.updateBaseCurrency(account.currency)

            // Keep the built-in Cash wallet in sync with the main account's currency
            val cashWallet = accountBalanceRepository.getLatestBalance("Cash", "wallet")
            if (cashWallet != null && cashWallet.currency != account.currency) {
                accountBalanceRepository.insertBalance(
                    cashWallet.copy(
                        id = 0,
                        currency = account.currency,
                        timestamp = LocalDateTime.now(),
                        sourceType = "MAIN_ACCOUNT_SYNC"
                    )
                )
            }
            _uiState.update {
                it.copy(showMainAccountSelection = false, mainAccountSelectionAccounts = emptyList())
            }
        }
    }

    fun dismissMainAccountSelection() {
        _uiState.update {
            it.copy(showMainAccountSelection = false, mainAccountSelectionAccounts = emptyList())
        }
    }
    
    fun shareBackup() {
        _uiState.value.exportedBackupFile?.let { file ->
            shareBackupFile(file)
        }
    }

    private fun shareBackupFile(file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Cashiro Backup")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(Intent.createChooser(intent, "Share Backup").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            Log.e("DataPrivacyViewModel", "Error sharing backup file", e)
        }
    }
    
    //Parse the PDF and emit analysis result
    fun analyzePdfStatement(uri: Uri) {
        if (_uiState.value.isPdfProcessing) return
        _uiState.update { it.copy(isPdfProcessing = true, pdfAnalysisResult = null, pdfProcessingError = null) }
        viewModelScope.launch(Dispatchers.IO) {
            try {

                PDFBoxResourceLoader.init(context)
                val text = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    PDDocument.load(inputStream).use { document ->
                        PDFTextStripper().getText(document)
                    }
                } ?: throw Exception("Failed to open PDF")

                val parsers = listOf(GPayPdfParser(), PhonePePdfParser(), PaytmPdfParser(), SlicePdfParser())
                var parsedTransactions = emptyList<com.ritesh.parser.core.ParsedTransaction>()

                for (parser in parsers) {
                    if (parser.canHandle(text)) {
                        val result = parser.parse(text)
                        if (result.isNotEmpty()) {
                            parsedTransactions = result
                            break
                        }
                    }
                }

                if (parsedTransactions.isEmpty()) {
                    throw Exception("No transactions found in this PDF. Please ensure you are importing a supported GPay, PhonePe, Paytm or Slice statement.")
                }

                val accounts = accountBalanceRepository.getAllLatestBalances().first()
                val accountMatches = parsedTransactions.filter { it.accountLast4 != null }
                    .distinctBy { pdfAccountKey(it.bankName, it.currency, it.accountLast4!!) }
                    .map { parsed ->
                        val suffix = parsed.accountLast4!!
                        val candidates = accounts.filter {
                            it.accountLast4 == suffix && it.currency == parsed.currency && !it.isWallet && !it.isCreditCard
                        }
                        val existing = candidates.singleOrNull { it.bankName == parsed.bankName }
                        PdfAccountMatch(suffix, parsed.bankName, existing, parsed.currency)
                    }

                // Enrich transactions with duplicate detection
                val transactionItems = parsedTransactions.map { parsed ->
                    val dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(parsed.timestamp), ZoneId.systemDefault())
                    val potentialDuplicates = transactionRepository.findPotentialDuplicates(
                        amount = parsed.amount,
                        startDate = dateTime.toLocalDate().atStartOfDay(),
                        endDate = dateTime.toLocalDate().atTime(java.time.LocalTime.MAX)
                    )

                    val statement = parsed.toEntity()
                    val duplicateMatch = StatementTransactionEnricher.findUniqueMatch(potentialDuplicates, statement)

                    PdfTransactionImportItem(
                        parsed = parsed,
                        duplicateMatch = duplicateMatch
                    )
                }

                _uiState.update {
                    it.copy(
                        isPdfProcessing = false,
                        pdfAnalysisResult = PdfAnalysisResult(
                            pendingTransactions = parsedTransactions,
                            transactionItems = transactionItems,
                            transactionCount = parsedTransactions.size,
                            accountMatches = accountMatches
                        )
                    )
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("DataPrivacyViewModel", "Error analyzing PDF")
                _uiState.update { it.copy(isPdfProcessing = false, pdfProcessingError = "Unable to import this PDF. Check that it is an unlocked, supported statement.") }
            }
        }
    }


    fun confirmPdfImport(
        accountDecisions: Map<String, AccountImportDecision>,
        transactionDecisions: Map<Int, TransactionImportDecision>
    ) {
        val analysis = _uiState.value.pdfAnalysisResult ?: return
        if (_uiState.value.isPdfProcessing) return
        _uiState.update { it.copy(isPdfProcessing = true) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                var importedCount = 0
                var newAccountsCreated = false
                BankAccountMergeStore.mutationMutex.withLock {
                // Resolve each provider/account identity independently.
                val resolvedAccounts = mutableMapOf<String, Pair<String, String>>()

                for (match in analysis.accountMatches) {
                    val hasSelectedTransaction = analysis.transactionItems.withIndex().any { (index, item) ->
                        (transactionDecisions[index] ?: item.initialDecision) == TransactionImportDecision.IMPORT_NEW &&
                            item.parsed.accountLast4?.let { pdfAccountKey(item.parsed.bankName, item.parsed.currency, it) } == match.key
                    }
                    if (!hasSelectedTransaction) continue
                    val decision = accountDecisions[match.key] ?: AccountImportDecision.MERGE_WITH_EXISTING
                    val accountPair: Pair<String, String> = if (decision == AccountImportDecision.MERGE_WITH_EXISTING && match.existingAccount != null) {
                        match.existingAccount.bankName to match.last4
                    } else {
                        // Create the account for this statement provider.
                        val newAccount = AccountBalanceEntity(
                            bankName = match.bankNameInPdf,
                            accountLast4 = match.last4,
                            balance = java.math.BigDecimal.ZERO,
                            timestamp = LocalDateTime.now(),
                            sourceType = "PDF_IMPORT",
                            iconName = "type_finance_bank",
                            currency = match.currency
                        )
                        // Only insert if not already existing for that last4+bank combo.
                        val existing = accountBalanceRepository.getLatestBalance(match.bankNameInPdf, match.last4, match.currency)
                        if (existing == null) {
                            accountBalanceRepository.insertBalance(newAccount)
                            newAccountsCreated = true
                        }
                        match.bankNameInPdf to match.last4
                    }
                    resolvedAccounts[match.key] = accountPair
                }

                analysis.transactionItems.forEachIndexed { index, item ->
                    val decision = transactionDecisions[index] ?: item.initialDecision
                    if (decision == TransactionImportDecision.SKIP) return@forEachIndexed

                    val parsed = item.parsed

                    if (decision == TransactionImportDecision.OVERRIDE_EXISTING && item.duplicateMatch != null) {
                        val existing = transactionRepository.getTransactionById(item.duplicateMatch.id)
                        if (existing != null && !existing.isDeleted) {
                            val enriched = StatementTransactionEnricher.enrich(existing, parsed.toEntity())
                            if (StatementTransactionEnricher.hasEnrichment(existing, enriched)) {
                                transactionRepository.updateTransaction(enriched)
                                importedCount++
                            }
                        }
                        return@forEachIndexed
                    }

                    val hash = generateHash(parsed.smsBody, parsed.amount.toString(), parsed.timestamp)
                    
                    // Check if already exists (incase another transaction in same PDF has same hash, though unlikely)
                    if (transactionRepository.getTransactionByHash(hash) == null) {
                        val resolved = parsed.accountLast4?.let { resolvedAccounts[pdfAccountKey(parsed.bankName, parsed.currency, it)] }
                        val transaction = TransactionEntity(
                            amount = parsed.amount,
                            merchantName = parsed.merchant ?: "Unknown",
                            category = "Miscellaneous",
                            transactionType = when (parsed.type) {
                                com.ritesh.parser.core.TransactionType.INCOME -> TransactionType.INCOME
                                else -> TransactionType.EXPENSE
                            },
                            dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(parsed.timestamp), ZoneId.systemDefault()),
                            smsBody = parsed.smsBody,
                            bankName = resolved?.first ?: parsed.bankName,
                            accountNumber = resolved?.second ?: parsed.accountLast4,
                            transactionHash = hash,
                            reference = parsed.reference,
                            smsSender = parsed.sender,
                            currency = parsed.currency
                        )

                        // Apply rules to the transaction
                        val activeRules = ruleRepository.getActiveRulesByType(transaction.transactionType)

                        // Check if this transaction should be blocked
                        val blockingRule = ruleEngine.shouldBlockTransaction(
                            transaction,
                            transaction.smsBody,
                            activeRules
                        )

                        if (blockingRule != null) {
                            Log.d("DataPrivacyViewModel", "Transaction blocked by rule")
                            return@forEachIndexed
                        }

                        // Apply non-blocking rules
                        val (entityWithRules, ruleApplications) = ruleEngine.evaluateRules(
                            transaction,
                            transaction.smsBody,
                            activeRules
                        )

                        val rowId = transactionRepository.insertTransaction(entityWithRules)
                        if (rowId != -1L) {
                            if (ruleApplications.isNotEmpty()) {
                                // Update transactionId in ruleApplications before saving
                                val applicationsWithId = ruleApplications.map { 
                                    it.copy(transactionId = rowId.toString())
                                }
                                ruleRepository.saveRuleApplications(applicationsWithId)
                                Log.d("DataPrivacyViewModel", "Applied ${ruleApplications.size} rules to transaction")
                            }
                            importedCount++
                        }
                    }
                }

                }
                _uiState.update {
                    it.copy(
                        isPdfProcessing = false,
                        pdfAnalysisResult = null,
                        importExportMessage = "Successfully imported $importedCount transactions from PDF!",
                        hasNewAccountsCreated = newAccountsCreated
                    )
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("DataPrivacyViewModel", "Error committing PDF import")
                _uiState.update { it.copy(isPdfProcessing = false, pdfProcessingError = "Unable to import this PDF. Check that it is an unlocked, supported statement.") }
            }
        }
    }

    // Dismiss the PDF import dialog without saving.
    fun dismissPdfImport() {
        _uiState.update { it.copy(isPdfProcessing = false, pdfAnalysisResult = null, pdfProcessingError = null) }
    }

    private fun generateHash(message: String, amount: String, timestamp: Long): String {
        val input = "$message$amount$timestamp"
        return MessageDigest.getInstance("MD5")
            .digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    fun clearImportExportMessage() {
        _uiState.update { it.copy(importExportMessage = null, hasNewAccountsCreated = false) }
    }
    
    fun clearExportedFile() {
        _uiState.update { it.copy(exportedBackupFile = null) }
    }

    private fun extractUtr(text: String?): String? {
        if (text == null) return null
        // Matches UPI: 123 456... or UTR No. 123 456... etc (allowing spaces in digits)
        val utrRegex = Regex("""(?:UPI[:\s]*|UTR\s+No\.?[:\s]*|Ref\s+No\.?[:\s]*|ID[:\s]*)([\d\s]+)""", RegexOption.IGNORE_CASE)
        return utrRegex.find(text)?.groupValues?.get(1)?.replace(Regex("""\D"""), "")
    }
}
