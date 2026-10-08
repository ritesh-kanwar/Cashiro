package com.ritesh.cashiro.data.csv

import com.opencsv.CSVReaderBuilder
import com.ritesh.cashiro.data.database.entity.TransactionEntity
import com.ritesh.cashiro.data.database.entity.TransactionType
import java.io.Reader
import java.math.BigDecimal
import java.security.MessageDigest
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** Header-based reader for Cashiro exports and compatible transaction CSV files. */
@Singleton
class CsvTransactionImporter @Inject constructor() {
    data class ImportParseResult(
        val transactions: List<TransactionEntity>,
        val failedCount: Int,
        val failureReasons: List<String> = emptyList(),
        val fatalError: Boolean = false
    )

    fun parse(reader: Reader): ImportParseResult {
        val transactions = mutableListOf<TransactionEntity>()
        val reasons = mutableListOf<String>()
        var failed = 0
        var rowNumber = 1
        try {
            CSVReaderBuilder(reader).withMultilineLimit(100).build().use { csv ->
                val header = csv.readNext()
                    ?: return ImportParseResult(emptyList(), 0, listOf("The CSV file is empty."), true)
                val names = header.map { it.removePrefix("\uFEFF").trim().lowercase(Locale.ROOT) }
                if (names.size != names.distinct().size) {
                    return ImportParseResult(emptyList(), 0, listOf("Duplicate column names."), true)
                }
                val columns = names.withIndex().associate { it.value to it.index }
                val missing = listOf("date", "amount", "type").filterNot(columns::containsKey)
                if (missing.isNotEmpty()) {
                    return ImportParseResult(emptyList(), 0,
                        listOf("Missing required columns: ${missing.joinToString()}"), true)
                }
                while (true) {
                    val row = csv.readNext() ?: break
                    rowNumber++
                    if (row.all(String::isBlank)) continue
                    try {
                        require(row.size <= header.size) { "Too many columns" }
                        transactions.add(parseRow(row, columns))
                    } catch (_: IllegalArgumentException) {
                        failed++
                        if (reasons.size < 20) reasons.add("Row $rowNumber has invalid or missing transaction fields.")
                    } catch (_: java.time.DateTimeException) {
                        failed++
                        if (reasons.size < 20) reasons.add("Row $rowNumber has an invalid date or time.")
                    }
                }
            }
        } catch (_: com.opencsv.exceptions.CsvValidationException) {
            return ImportParseResult(emptyList(), failed + 1, listOf("Malformed CSV near row $rowNumber."), true)
        } catch (_: java.io.IOException) {
            return ImportParseResult(emptyList(), failed + 1, listOf("The CSV file could not be read."), true)
        }
        return ImportParseResult(transactions, failed, reasons)
    }

    private fun parseRow(row: Array<String>, columns: Map<String, Int>): TransactionEntity {
        fun cell(name: String): String? = columns[name]?.let(row::getOrNull)?.trim()?.takeIf(String::isNotEmpty)
        val date = LocalDate.parse(requireNotNull(cell("date")))
        val time = cell("time")?.let(LocalTime::parse) ?: LocalTime.MIDNIGHT
        val amount = BigDecimal(requireNotNull(cell("amount")))
        require(amount.signum() > 0 && amount.precision() <= 30 && amount.scale() in 0..8)
        val type = when (val label = requireNotNull(cell("type")).uppercase(Locale.ROOT)) {
            "CREDIT CARD" -> TransactionType.CREDIT
            "BALANCE UPDATE" -> TransactionType.BALANCE_UPDATE
            else -> TransactionType.valueOf(label)
        }
        val currency = (cell("currency") ?: "INR").uppercase(Locale.ROOT)
        require(Regex("[A-Z]{3}").matches(currency))
        val entity = TransactionEntity(
            amount = amount,
            merchantName = cell("merchant") ?: "Unknown",
            category = cell("category") ?: "Others",
            subcategory = cell("subcategory"),
            transactionType = type,
            dateTime = LocalDateTime.of(date, time),
            description = cell("description"),
            smsBody = cell("sms body"),
            smsSender = cell("sms sender"),
            bankName = cell("bank") ?: "Imported",
            accountNumber = cell("account"),
            balanceAfter = cell("balance after")?.let(::BigDecimal),
            currency = currency,
            reference = cell("reference"),
            fromAccount = cell("from account"),
            toAccount = cell("to account"),
            fromBankName = cell("from bank"),
            toBankName = cell("to bank"),
            transactionHash = ""
        )
        return entity.copy(transactionHash = identityHash(entity))
    }

    companion object {
        /** Length-prefixed fields avoid delimiter collisions; scale does not change identity. */
        fun identityHash(row: TransactionEntity): String {
            val fields = listOf(
                row.dateTime.toString(), row.merchantName.trim(),
                row.amount.stripTrailingZeros().toPlainString(), row.transactionType.name,
                row.currency.uppercase(Locale.ROOT), row.bankName.orEmpty().trim().lowercase(Locale.ROOT),
                row.accountNumber.orEmpty().trim()
            )
            val content = fields.joinToString("") { "${it.length}:$it" }
            return "CSV_v2_" + MessageDigest.getInstance("SHA-256")
                .digest(content.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
        }
    }
}
