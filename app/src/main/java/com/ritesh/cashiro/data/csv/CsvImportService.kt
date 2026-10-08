package com.ritesh.cashiro.data.csv

import androidx.room.withTransaction
import com.ritesh.cashiro.data.database.CashiroDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Reader
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

data class CsvImportResult(
    val importedCount: Int,
    val duplicateCount: Int,
    val failedCount: Int,
    val failureReasons: List<String> = emptyList()
)

/** Import is atomic and never replaces an existing transaction or changes account balances. */
@Singleton
class CsvImportService @Inject constructor(
    private val database: CashiroDatabase,
    private val importer: CsvTransactionImporter
) {
    suspend fun importCsv(reader: Reader): CsvImportResult = withContext(Dispatchers.IO) {
        val parsed = importer.parse(reader)
        if (parsed.fatalError) {
            return@withContext CsvImportResult(0, 0, parsed.failedCount, parsed.failureReasons)
        }
        database.withTransaction {
            val dao = database.transactionDao()
            var imported = 0
            var duplicates = 0
            val seen = mutableSetOf<String>()
            val dayIdentities = mutableMapOf<LocalDate, Set<String>>()
            for (row in parsed.transactions) {
                val hash = row.transactionHash
                if (!seen.add(hash) || dao.getTransactionByHash(hash) != null) {
                    duplicates++
                    continue
                }
                // Cache each day's identities within this transaction, including deleted rows.
                val day = row.dateTime.toLocalDate()
                val existing = dayIdentities.getOrPut(day) {
                    val dayStart = day.atStartOfDay()
                    dao.getTransactionsForCsvIdentity(dayStart, dayStart.plusDays(1))
                        .mapTo(mutableSetOf()) { CsvTransactionImporter.identityHash(it) }
                }
                if (hash in existing) {
                    duplicates++
                } else if (dao.insertTransaction(row) == -1L) {
                    duplicates++
                } else {
                    imported++
                }
            }
            CsvImportResult(imported, duplicates, parsed.failedCount, parsed.failureReasons)
        }
    }
}
