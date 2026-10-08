package com.ritesh.cashiro.data.csv

import com.ritesh.cashiro.data.database.entity.TransactionType
import org.junit.Assert.*
import org.junit.Test
import java.io.StringReader

class CsvTransactionImporterTest {
    private val importer = CsvTransactionImporter()
    private fun parse(text: String) = importer.parse(StringReader(text))

    @Test fun reorderedHeadersQuotesAndOptionalFieldsRoundTrip() {
        val result = parse("""
            Type,Amount,Date,Merchant,Description,Subcategory,Currency,Bank,Account,SMS Body
            Lent,50.00,2026-01-01,"Example, Shop","line one
            line two",Example Detail,usd,Example Bank,1000,Example message
        """.trimIndent())
        assertEquals(0, result.failedCount)
        val row = result.transactions.single()
        assertEquals("Example, Shop", row.merchantName)
        assertTrue(row.description!!.contains("line two"))
        assertEquals("Example Detail", row.subcategory)
        assertEquals("USD", row.currency)
        assertEquals(TransactionType.LENT, row.transactionType)
        assertEquals("Example message", row.smsBody)
    }

    @Test fun acceptsAllCashiroExportTypeLabels() {
        val labels = listOf("Income", "Expense", "Credit Card", "Transfer", "Investment", "Balance Update", "Lent", "Borrowed")
        val result = parse("Date,Amount,Type\n" + labels.joinToString("\n") { "2026-01-01,50,$it" })
        assertEquals(8, result.transactions.size)
        assertEquals(0, result.failedCount)
    }

    @Test fun identityIsScaleIndependentButSeparatesBankCurrencyAndAccount() {
        val result = parse("""
            Date,Amount,Type,Bank,Currency,Account
            2026-01-01,50,Expense,Example Bank,INR,1000
            2026-01-01,50.00,Expense,Example Bank,INR,1000
            2026-01-01,50,Expense,Other Bank,INR,1000
            2026-01-01,50,Expense,Example Bank,USD,1000
            2026-01-01,50,Expense,Example Bank,INR,2000
        """.trimIndent())
        val hashes = result.transactions.map { it.transactionHash }
        assertEquals(hashes[0], hashes[1])
        assertEquals(4, hashes.toSet().size)
    }

    @Test fun rejectsBadDatesAmountsTypesAndHeadersWithoutLeakingCells() {
        val result = parse("""
            Date,Amount,Type
            2026-02-31,50,Expense
            2026-01-01,-50,Expense
            2026-01-01,50,private-invalid-type
            2026-01-01,50,Expense
        """.trimIndent())
        assertEquals(3, result.failedCount)
        assertEquals(1, result.transactions.size)
        assertFalse(result.failureReasons.joinToString().contains("private-invalid-type"))
        assertTrue(parse("Date,Amount\n2026-01-01,50").fatalError)
        assertTrue(parse("Date,Amount,Type,DATE").fatalError)
    }

    @Test fun malformedQuotedFileIsRejectedAtomically() {
        val result = parse("Date,Amount,Type,Merchant\n2026-01-01,50,Expense,Valid\n2026-01-02,50,Expense,\"unfinished")
        assertTrue(result.fatalError)
        assertTrue(result.transactions.isEmpty())
    }

    @Test fun bomAndMissingOptionalColumnsAreAccepted() {
        val row = parse("\uFEFFDate,Amount,Type\n2026-01-01,50,Borrowed").transactions.single()
        assertEquals(TransactionType.BORROWED, row.transactionType)
        assertEquals("INR", row.currency)
        assertEquals("Others", row.category)
    }
}
