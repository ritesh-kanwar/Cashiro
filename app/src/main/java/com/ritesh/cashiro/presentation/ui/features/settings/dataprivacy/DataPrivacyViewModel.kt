package com.ritesh.cashiro.presentation.ui.features.settings.dataprivacy

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ritesh.cashiro.data.backup.BackupConfiguration
import com.ritesh.cashiro.data.backup.BackupExporter
import com.ritesh.cashiro.data.backup.BackupImporter
import com.ritesh.cashiro.data.backup.ExportResult
import com.ritesh.cashiro.data.backup.ImportResult
import com.ritesh.cashiro.data.backup.ImportStrategy
import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import com.ritesh.cashiro.data.database.entity.TransactionEntity
import com.ritesh.cashiro.data.database.entity.TransactionType
import com.ritesh.cashiro.data.mapper.toEntity
import com.ritesh.cashiro.data.parser.pdf.GPayPdfParser
import com.ritesh.cashiro.data.parser.pdf.PhonePePdfParser
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
    private val transactionRepository: TransactionRepository,
    private val accountBalanceRepository: AccountBalanceRepository,
    private val ruleRepository: RuleRepository,
    private val ruleEngine: RuleEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(DataPrivacyUiState())
    val uiState: StateFlow<DataPrivacyUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            accountBalanceRepository.getAllLatestBalances().collect { accounts ->
                _uiState.update { it.copy(availableAccounts = accounts) }
            }
        }
    }

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
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isPdfProcessing = true, pdfAnalysisResult = null, pdfProcessingError = null) }

                PDFBoxResourceLoader.init(context)
                val text = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    PDDocument.load(inputStream).use { document ->
                        PDFTextStripper().getText(document)
                    }
                } ?: throw Exception("Failed to open PDF")

                Log.d("DataPrivacyViewModel", "Extracted Text (first 500 chars): ${text.take(500)}")

                val parsers = listOf(GPayPdfParser(), PhonePePdfParser())
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
                    throw Exception("No transactions found in this PDF. Please ensure you are importing a supported GPay or PhonePe statement.")
                }

                // Collect distinct account last-4 values from all transactions.
                // If last4 is null, we use a placeholder to allow mapping.
                val distinctLast4s = parsedTransactions.map { it.accountLast4 ?: "Unknown" }.distinct()

                // For each distinct account, look up whether it exists in the app.
                val accountMatches = distinctLast4s.map { last4 ->
                    val existing = if (last4 != "Unknown") accountBalanceRepository.getAccountByLast4(last4) else null
                    PdfAccountMatch(
                        last4 = last4,
                        bankNameInPdf = parsedTransactions.firstOrNull { (it.accountLast4 ?: "Unknown") == last4 }?.bankName ?: "PhonePe",
                        existingAccount = existing
                    )
                }

                // Enrich transactions with duplicate detection
                val transactionItems = parsedTransactions.map { parsed ->
                    val dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(parsed.timestamp), ZoneId.systemDefault())
                    val potentialDuplicates = transactionRepository.findPotentialDuplicates(
                        amount = parsed.amount,
                        startDate = dateTime.minusMinutes(15),
                        endDate = dateTime.plusMinutes(15)
                    )

                    // Match logic: same amount AND (ref id match OR account last-4 match)
                    val duplicateMatch = potentialDuplicates.find { existing ->
                        // 1. Technical Reference (UTR/UPI) match - Highest confidence
                        val parsedUtr = parsed.reference?.replace(Regex("""\D"""), "")
                        if (!parsedUtr.isNullOrEmpty()) {
                            val existingUtr = extractUtr(existing.smsBody) ?: extractUtr(existing.description)
                            if (existingUtr == parsedUtr) return@find true
                        }
                        
                        // 2. Account match refinement (last 4 digits)
                        val existingAcc = existing.accountNumber
                        val parsedAcc = parsed.accountLast4
                        
                        if (existingAcc == null || parsedAcc == null) {
                            // If we can't verify account (e.g. manual/missing), we match by amount + date (from query)
                            return@find true
                        }

                        // Compare strictly by last 4 digits (digit-only)
                        val existingDigits = existingAcc.replace(Regex("""\D"""), "")
                        val parsedDigits = parsedAcc.replace(Regex("""\D"""), "")
                        val existingLast4 = existingDigits.takeLast(4)
                        val parsedLast4 = parsedDigits.takeLast(4)
                        if (existingLast4 == parsedLast4 && existingLast4.isNotEmpty()) return@find true
                        
                        false
                    }

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
                Log.e("DataPrivacyViewModel", "Error analyzing PDF", e)
                _uiState.update { it.copy(isPdfProcessing = false, pdfProcessingError = e.message) }
            }
        }
    }


    fun confirmPdfImport(
        accountDecisions: Map<String, AccountImportDecision>,
        accountMappings: Map<String, AccountBalanceEntity?>,
        transactionDecisions: Map<Int, TransactionImportDecision>,
        shouldUpdateBalances: Boolean
    ) {
        val analysis = _uiState.value.pdfAnalysisResult ?: return
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isPdfProcessing = true) }

                // Resolve/create account for each last4 based on user's decision.
                val resolvedAccounts = mutableMapOf<String, Pair<String, String>>() // last4 → (bankName, last4)
                val resolvedAccountEntities = mutableMapOf<String, AccountBalanceEntity>()

                var newAccountsCreated = false
                for (match in analysis.accountMatches) {
                    val decision = accountDecisions[match.last4] ?: AccountImportDecision.MERGE_WITH_EXISTING
                    
                    if (decision == AccountImportDecision.MERGE_WITH_EXISTING) {
                        val mappedAccount = accountMappings[match.last4] ?: match.existingAccount
                        if (mappedAccount != null) {
                            resolvedAccounts[match.last4] = mappedAccount.bankName to mappedAccount.accountLast4
                            resolvedAccountEntities[match.last4] = mappedAccount
                        } else {
                            // Fallback to creating new if merge requested but no account selected/found
                            val newAccount = AccountBalanceEntity(
                                bankName = match.bankNameInPdf,
                                accountLast4 = match.last4,
                                balance = java.math.BigDecimal.ZERO,
                                timestamp = LocalDateTime.now(),
                                sourceType = "PDF_IMPORT",
                                iconName = "type_finance_bank"
                            )
                            val existing = accountBalanceRepository.getLatestBalance(match.bankNameInPdf, match.last4)
                            val accountToUse = if (existing == null) {
                                val id = accountBalanceRepository.insertBalance(newAccount)
                                newAccountsCreated = true
                                newAccount.copy(id = id)
                            } else existing
                            
                            resolvedAccounts[match.last4] = accountToUse.bankName to accountToUse.accountLast4
                            resolvedAccountEntities[match.last4] = accountToUse
                        }
                    } else {
                        // Create a new "PhonePe" account for this last4.
                        val newAccount = AccountBalanceEntity(
                            bankName = match.bankNameInPdf,
                            accountLast4 = match.last4,
                            balance = java.math.BigDecimal.ZERO,
                            timestamp = LocalDateTime.now(),
                            sourceType = "PDF_IMPORT",
                            iconName = "type_finance_bank"
                        )
                        // Only insert if not already existing for that last4+bank combo.
                        val existing = accountBalanceRepository.getLatestBalance(match.bankNameInPdf, match.last4)
                        val accountToUse = if (existing == null) {
                            val id = accountBalanceRepository.insertBalance(newAccount)
                            newAccountsCreated = true
                            newAccount.copy(id = id)
                        } else existing
                        
                        resolvedAccounts[match.last4] = accountToUse.bankName to accountToUse.accountLast4
                        resolvedAccountEntities[match.last4] = accountToUse
                    }
                }

                var importedCount = 0
                // Process transactions in chronological order (oldest first) to ensure balance recalculations are correct
                val sortedItems = analysis.transactionItems
                    .mapIndexed { index, item -> index to item }
                    .sortedBy { it.second.parsed.timestamp }

                sortedItems.forEach { (index, item) ->
                    val decision = transactionDecisions[index] ?: item.initialDecision
                    if (decision == TransactionImportDecision.SKIP) return@forEach

                    val parsed = item.parsed

                    // Handle Override logic: Delete existing transaction if it's a duplicate and user wants to override
                    if (decision == TransactionImportDecision.OVERRIDE_EXISTING && item.duplicateMatch != null) {
                        transactionRepository.deleteTransaction(item.duplicateMatch, hardDelete = true)
                    }

                    val hash = generateHash(parsed.smsBody, parsed.amount.toString(), parsed.timestamp)
                    
                    // Check if already exists (incase another transaction in same PDF has same hash, though unlikely)
                    if (transactionRepository.getTransactionByHash(hash) == null) {
                        val key = parsed.accountLast4 ?: "Unknown"
                        val resolved = resolvedAccounts[key]
                        
                        // Use the mapper to get a base entity with all fields (merchant normalization, category mapping, etc.)
                        val baseTransaction = parsed.toEntity()
                        
                        val transaction = baseTransaction.copy(
                            bankName = resolved?.first ?: baseTransaction.bankName,
                            accountNumber = resolved?.second ?: baseTransaction.accountNumber,
                            fromAccount = if (baseTransaction.transactionType != TransactionType.INCOME) (resolved?.second ?: baseTransaction.accountNumber) else null,
                            toAccount = if (baseTransaction.transactionType == TransactionType.INCOME) (resolved?.second ?: baseTransaction.accountNumber) else null,
                            transactionHash = hash
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
                            Log.d("DataPrivacyViewModel", "Transaction blocked by rule: ${blockingRule.name}")
                            return@forEach
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
                            }

                            // Handle balance update automatically for PDF imports
                            val key = parsed.accountLast4 ?: "Unknown"
                            val accountEntity = resolvedAccountEntities[key]

                            if (accountEntity != null && shouldUpdateBalances) {
                                accountBalanceRepository.insertTransactionBalance(
                                    bankName = accountEntity.bankName,
                                    accountLast4 = accountEntity.accountLast4,
                                    amount = entityWithRules.amount,
                                    transactionType = entityWithRules.transactionType,
                                    explicitBalance = null, // We don't have balance in PhonePe PDF usually, let it calculate
                                    timestamp = entityWithRules.dateTime,
                                    transactionId = rowId,
                                    creditLimit = accountEntity.creditLimit,
                                    isCreditCard = accountEntity.isCreditCard,
                                    smsSource = "PDF Import: ${parsed.smsBody.take(200)}",
                                    currency = entityWithRules.currency
                                )
                            }

                            importedCount++
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
                Log.e("DataPrivacyViewModel", "Error committing PDF import", e)
                _uiState.update { it.copy(isPdfProcessing = false, pdfProcessingError = e.message) }
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
