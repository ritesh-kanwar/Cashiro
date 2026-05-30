package com.ritesh.cashiro.data.statement

import android.content.Context
import android.net.Uri
import com.ritesh.cashiro.data.database.entity.TransactionEntity
import com.ritesh.cashiro.data.repository.TransactionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.time.LocalDateTime
import javax.inject.Inject

class ImportStatementUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    @ApplicationContext private val context: Context
) {
    suspend fun import(
        uris: List<Uri>,
        onProgress: (Float) -> Unit = {}
    ): StatementImportResult = withContext(Dispatchers.IO) {
        var totalParsed = 0
        var totalImported = 0
        var totalEnriched = 0
        var totalSkippedDuplicates = 0
        var totalSkippedByHash = 0
        var totalSkippedByReference = 0
        var totalSkippedByAmountDate = 0
        var errorMessage: String? = null

        val totalFiles = uris.size

        for ((index, uri) in uris.withIndex()) {
            try {
                val baseProgress = index.toFloat() / totalFiles
                val fileProgressWeight = 1f / totalFiles

                val text = PdfTextExtractor.extractText(context, uri)

                val parser = PdfParserFactory.getParser(text)
                if (parser == null) {
                    errorMessage = "Unsupported statement format in one or more files."
                    continue
                }

                val parsedTransactions = parser.parse(text)
                if (parsedTransactions.isEmpty()) {
                    continue
                }

                val result = StatementImportProcessor(repositoryStore()).process(parsedTransactions) { fileProgress ->
                    val overallProgress = baseProgress + (fileProgress * fileProgressWeight)
                    onProgress(overallProgress)
                }

                totalParsed += result.totalParsed
                totalImported += result.imported
                totalEnriched += result.enriched
                totalSkippedDuplicates += result.skippedDuplicates
                totalSkippedByHash += result.skippedByHash
                totalSkippedByReference += result.skippedByReference
                totalSkippedByAmountDate += result.skippedByAmountDate
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to import one or more statements."
            }
        }
        
        // Ensure progress hits 1f at the very end
        onProgress(1f)

        if (totalParsed == 0 && errorMessage != null) {
            StatementImportResult.Error(errorMessage)
        } else if (totalParsed == 0) {
            StatementImportResult.Error("No transactions found in the selected statements.")
        } else {
            StatementImportResult.Success(
                totalParsed = totalParsed,
                imported = totalImported,
                enriched = totalEnriched,
                skippedDuplicates = totalSkippedDuplicates,
                skippedByHash = totalSkippedByHash,
                skippedByReference = totalSkippedByReference,
                skippedByAmountDate = totalSkippedByAmountDate
            )
        }
    }

    private fun repositoryStore() = object : StatementImportProcessor.TransactionStore {
        override suspend fun getTransactionByHash(transactionHash: String): TransactionEntity? =
            transactionRepository.getTransactionByHash(transactionHash)

        override suspend fun findStatementMergeCandidate(
            transaction: TransactionEntity
        ): TransactionEntity? =
            transactionRepository.findStatementMergeCandidate(transaction)

        override suspend fun updateTransaction(transaction: TransactionEntity) {
            transactionRepository.updateTransaction(transaction)
        }

        override suspend fun getTransactionByAmountAndDate(
            amount: BigDecimal,
            dateStart: LocalDateTime,
            dateEnd: LocalDateTime
        ): List<TransactionEntity> =
            transactionRepository.getTransactionByAmountAndDate(amount, dateStart, dateEnd)

        override suspend fun insertTransactions(transactions: List<TransactionEntity>) {
            transactionRepository.insertTransactions(transactions)
        }
    }
}
