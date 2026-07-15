package com.ritesh.cashiro.presentation.ui.features.settings.dataprivacy

import com.ritesh.cashiro.data.backup.BackupConfiguration
import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import com.ritesh.cashiro.data.database.entity.TransactionEntity
import java.io.File

/**
 * Holds the result of the PDF analysis phase.
 */
data class PdfAnalysisResult(
// Parsed transactions waiting to be committed.
    val pendingTransactions: List<com.ritesh.parser.core.ParsedTransaction>,
    // Enriched transaction items with duplicate info and decisions.
    val transactionItems: List<PdfTransactionImportItem>,
    // Number of distinct transactions extracted.
    val transactionCount: Int,
    // Accounts found in this PDF (last4 → existing account or null if new).
    val accountMatches: List<PdfAccountMatch>
)

/**
 * Wrapper for a parsed transaction with enrichment for the import review process.
 */
data class PdfTransactionImportItem(
    val parsed: com.ritesh.parser.core.ParsedTransaction,
    val duplicateMatch: TransactionEntity? = null,
    val initialDecision: TransactionImportDecision = if (duplicateMatch != null) TransactionImportDecision.SKIP else TransactionImportDecision.IMPORT_NEW
)

/**
 * User's decision for a specific transaction being imported.
 */
enum class TransactionImportDecision { IMPORT_NEW, SKIP, OVERRIDE_EXISTING }

/**
 * Decision options for how to handle an account found in a PDF.
 */
data class PdfAccountMatch(
    val last4: String,
    val bankNameInPdf: String,
    // Existing account in the DB that matches, or null if no match.
    val existingAccount: AccountBalanceEntity?
) {
    /**
     * Whether this account already exists in the local database.
     */
    val hasExistingMatch: Boolean get() = existingAccount != null
}

/**
 * User's decision for each account found in the PDF.
 */
enum class AccountImportDecision { MERGE_WITH_EXISTING, CREATE_NEW }

/**
 * UI State for the Data & Privacy settings screen.
 */
data class DataPrivacyUiState(
    val importExportMessage: String? = null,
    val exportedBackupFile: File? = null,
    val backupConfiguration: BackupConfiguration = BackupConfiguration(),

    // PDF import flow
    val isPdfProcessing: Boolean = false,
    val pdfAnalysisResult: PdfAnalysisResult? = null,
    val pdfProcessingError: String? = null,
    val hasNewAccountsCreated: Boolean = false,
    val availableAccounts: List<AccountBalanceEntity> = emptyList()
)
