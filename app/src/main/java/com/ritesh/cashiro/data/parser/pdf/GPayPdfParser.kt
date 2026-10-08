package com.ritesh.cashiro.data.parser.pdf

import com.ritesh.parser.core.ParsedTransaction
import com.ritesh.parser.core.TransactionType
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Parses Google Pay PDF statement exports into [ParsedTransaction] objects.
 * For income transactions the anchor is "Received from ..." and the account
 * line reads "Paid to Example Bank 1000" — which is NOT a new transaction.
 * We distinguish transaction anchors from account lines by checking that a
 * "Paid to" line does NOT end with 4 digits (which would make it a bank line).
 */
class GPayPdfParser : PdfStatementParser {

    // ─── Companion ────────────────────────────────────────────────────────────

    companion object {
        private const val DATE_FORMAT_PATTERN = "dd MMM, yyyy hh:mm a"
    }

    private val IST = TimeZone.getTimeZone("Asia/Kolkata")

    // ─── Patterns ─────────────────────────────────────────────────────────────

    // Matches "Paid to <merchant>" — but NOT "Paid to Example Bank 1000"
    // (account lines always end with 4 digits, merchant names never do)
    private val merchantAnchorRegex = Regex(
        """^Paid\s+to\s+(.+)$""", RegexOption.IGNORE_CASE
    )
    private val receivedAnchorRegex = Regex(
        """^Received\s+from\s+(.+)$""", RegexOption.IGNORE_CASE
    )
    // Stricter version for anchor detection and account extraction: only matches
    // if the name before the 4 digits contains a known account keyword (Bank, Card, A/c).
    // This prevents "Paid to Example Shop 2000" from being misclassified as an account line.
    private val bankAccountLineRegex = Regex(
        """^Paid\s+(?:by|to)\s+((?:.*\b)?(?:Bank|Card|A/c)\b.*?)\s+(\d{4})$""", RegexOption.IGNORE_CASE
    )
    private val upiIdRegex = Regex(
        """UPI\s+Transaction\s+ID[:\s]+(\d+)""", RegexOption.IGNORE_CASE
    )
    // Amount on its own line — currency prefix (₹, Rs.) required; bare numbers
    // like the year line ("2025") are intentionally excluded by the [₹Rs.\s]+ prefix.
    private val amountRegex = Regex("""^(?:₹|Rs\.?)\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)$""")

    // Date parts — each on its own line
    private val dateLineRegex = Regex("""^(\d{1,2})\s+(\w{3}),?$""")       // "01 Sep,"
    private val fullDateLineRegex = Regex("""^(\d{1,2}\s+\w{3},\s+20\d{2})$""") // "15 Oct, 2025"
    private val datePrefixedRowRegex = Regex("""^(\d{1,2}\s+\w{3},\s+20\d{2})\s+(.+)$""")
    private val yearLineRegex = Regex("""^(20\d{2})$""")                    // "2025"
    private val timeLineRegex = Regex("""^(\d{1,2}:\d{2})\s*([AaPp][Mm])(?:\s+(.+))?$""") // "03:02 PM ..."

    // ─── Public API ───────────────────────────────────────────────────────────

    override fun canHandle(text: String): Boolean {
        val lower = text.lowercase()
        val result = ("google pay" in lower || "gpay" in lower) && "upi transaction id" in lower
        return result
    }

    override fun parse(text: String): List<ParsedTransaction> {

        val blocks = splitIntoBlocks(text)

        val transactions = blocks.mapNotNull { block ->
            parseBlock(block)
        }

        return transactions
    }

    // ─── Block splitting ──────────────────────────────────────────────────────

    /** Normalize Cashiro's compact PDF extraction layout before applying anchor parsing. */
    private fun normalizeLayout(text: String): String = text
        .replace(Regex("""[ \t]+"""), " ")
        .replace(Regex("""(\d{1,2}\s+\w{3},?\s+20\d{2})[ \t]+(?=\d{1,2}:\d{2}\s*[AaPp][Mm])"""), "$1\n")
        .replace(Regex("""(\d{1,2}:\d{2}\s*[AaPp][Mm])[ \t]+(?=Paid to|Received from)""", RegexOption.IGNORE_CASE), "$1\n")
        .replace(Regex("""(?<![AaPp][Mm])[ \t]+(?=UPI\s+Transaction\s+ID)""", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("""(UPI\s+Transaction\s+ID[:\s]+\d+)[ \t]+(?=Paid (?:by|to))""", RegexOption.IGNORE_CASE), "$1\n")
        .replace(Regex("""[ \t]+(?=(?:₹|Rs\.?)\s*\d)"""), "\n")

    /**
     * Splits raw PDF text into one string per transaction.
     *
     * The challenge: date lines (`"01 Sep,"`, `"2025"`, `"03:02 PM"`) appear on
     * the lines BEFORE the transaction anchor in GPay exports. We keep a rolling
     * "pending" triplet of the most-recent date/year/time lines seen, and each
     * time a new anchor arrives we prepend that triplet to the new block before
     * consuming it. Date-pattern lines are never appended to the *current* block
     * because they belong to the *next* transaction — otherwise N+1's date lines
     * would bleed into N's block and confuse `extractTimestamp`.
     *
     * An anchor is "Paid to <X>" where X is not a bank account line, or
     * "Received from <X>". This excludes "Paid to Example Bank 1000"
     * (the account line in income transactions) from being treated as an anchor.
     */
    private fun splitIntoBlocks(text: String): List<String> {
        val blocks = mutableListOf<String>()
        val current = StringBuilder()
        var pendingDate: String? = null
        var pendingYear: String? = null
        var pendingTime: String? = null
        var inBlock = false

        for (rawLine in normalizeLayout(text).lines()) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue

            val isDate = dateLineRegex.matches(line)
            val isFullDate = fullDateLineRegex.matches(line)
            val isYear = yearLineRegex.matches(line)
            val isTime = timeLineRegex.matches(line)

            val datePrefixedRow = datePrefixedRowRegex.find(line)
            if (datePrefixedRow != null) {
                val fullDate = datePrefixedRow.groupValues[1]
                val rest = datePrefixedRow.groupValues[2].trim()
                val rowParts = splitAnchorAndAmount(rest)
                if (isTransactionAnchor(rowParts.anchorLine)) {
                    if (inBlock && current.isNotEmpty()) {
                        blocks.add(current.toString().trim())
                        current.clear()
                    }
                    current.appendLine(fullDate)
                    current.appendLine(rowParts.anchorLine)
                    rowParts.amountLine?.let { current.appendLine(it) }
                    pendingDate = null
                    pendingYear = null
                    pendingTime = null
                    inBlock = true
                    continue
                }
            }

            if (isTransactionAnchor(line)) {
                if (inBlock && current.isNotEmpty()) {
                    blocks.add(current.toString().trim())
                    current.clear()
                }
                // Prepend the most-recent pending date triplet so this block
                // owns its own dates — then clear pending so the next transaction
                // starts fresh.
                pendingDate?.let { current.appendLine(it) }
                pendingYear?.let { current.appendLine(it) }
                pendingTime?.let { current.appendLine(it) }
                pendingDate = null
                pendingYear = null
                pendingTime = null
                current.appendLine(line)
                inBlock = true
                continue
            }

            // Date-pattern lines are NEVER appended to the current block — they
            // always belong to the next anchor's transaction. Track the most
            // recent triplet so the next anchor can consume it.
            if (isDate) { pendingDate = line; continue }
            if (isFullDate) { pendingDate = line; pendingYear = null; continue }
            if (isYear) { pendingYear = line; continue }
            if (isTime && inBlock && !timeLineRegex.find(line)?.groupValues?.getOrNull(3).isNullOrBlank()) {
                current.appendLine(line)
                continue
            }
            if (isTime) { pendingTime = line; continue }

            if (inBlock) current.appendLine(line)
        }

        if (current.isNotEmpty()) blocks.add(current.toString().trim())

        return blocks
    }

    /**
     * Returns true only for genuine transaction anchors.
     * Account lines like "Paid to Example Bank 1000" are excluded by
     * matching against [bankAccountLineRegex] which requires a bank/card keyword.
     * This prevents "Paid to Example Shop 2000" from being misclassified.
     */
    private fun isTransactionAnchor(line: String): Boolean {
        if (receivedAnchorRegex.matches(line)) return true
        if (merchantAnchorRegex.matches(line)) {
            return !bankAccountLineRegex.matches(line)
        }
        return false
    }

    // ─── Block parsing ────────────────────────────────────────────────────────

    private fun parseBlock(block: String): ParsedTransaction? {
        val lines = block.lines().map { it.trim() }.filter { it.isNotEmpty() }


        // Find the anchor line (may not be the first line due to prepended date lines)
        val anchorLine = lines.firstOrNull { isTransactionAnchor(it) }
        if (anchorLine == null) {
            return null
        }

        val isExpense = merchantAnchorRegex.matches(anchorLine)
        val type      = if (isExpense) TransactionType.EXPENSE else TransactionType.INCOME
        val merchant  = extractMerchant(anchorLine, isExpense)

        if (merchant == null) {
            return null
        }

        val amount = extractAmount(lines)
        if (amount == null) {
            return null
        }

        val timestamp = extractTimestamp(lines)
        val upiId     = lines.firstNotNullOfOrNull { upiIdRegex.find(it)?.groupValues?.get(1) }
        val account   = extractAccountInfo(lines)

        return ParsedTransaction(
            amount       = amount,
            type         = type,
            merchant     = merchant,
            reference    = upiId,
            accountLast4 = account.last4,
            balance      = null,
            smsBody      = block,
            sender       = "GPay PDF",
            timestamp    = timestamp ?: System.currentTimeMillis(),
            bankName     = account.bankName ?: "Google Pay",
        )
    }

    // ─── Field extractors ─────────────────────────────────────────────────────

    private fun extractMerchant(anchorLine: String, isExpense: Boolean): String? {
        val regex = if (isExpense) merchantAnchorRegex else receivedAnchorRegex
        return regex.find(anchorLine)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun extractAmount(lines: List<String>): BigDecimal? {
        for (line in lines) {
            val raw = amountRegex.find(line)?.groupValues?.get(1) ?: continue
            val cleaned = raw.replace(",", "")
            val amount  = cleaned.toBigDecimalOrNull()
            if (amount != null) return amount
        }
        return null
    }

    /**
     * Assembles timestamp from three separate lines:
     *   "01 Sep,"  → day=1, month=Sep
     *   "2025"     → year
     *   "03:02 PM" → time
     *
     * Builds the string "01 Sep, 2025 03:02 PM" and parses it.
     */
    private fun extractTimestamp(lines: List<String>): Long? {
        var dateLine: String? = null
        var yearLine: String? = null
        var timeLine: String? = null

        for (line in lines) {
            when {
                dateLine == null && (dateLineRegex.matches(line) || fullDateLineRegex.matches(line)) -> {
                    dateLine = line
                }
                yearLine == null && yearLineRegex.matches(line) -> yearLine = line
                timeLine == null && timeLineRegex.matches(line) -> {
                    val match = timeLineRegex.find(line)
                    timeLine = listOfNotNull(
                        match?.groupValues?.getOrNull(1),
                        match?.groupValues?.getOrNull(2)
                    ).joinToString(" ")
                }
            }
            if (dateLine != null && (yearLine != null || fullDateLineRegex.matches(dateLine)) && timeLine != null) break
        }

        if (dateLine == null || timeLine == null || (yearLine == null && !fullDateLineRegex.matches(dateLine))) {
            return null
        }

        // Normalise: "01 Sep," → "01 Sep" then build "01 Sep, 2025 03:02 PM"
        val dateClean = dateLine.trimEnd(',', ' ')
        val timeClean = timeLine.replace(Regex("""\s+"""), " ").uppercase()
        val combined = if (fullDateLineRegex.matches(dateLine)) {
            "$dateClean $timeClean"
        } else {
            "$dateClean, $yearLine $timeClean"
        }

        return try {
            val sdf = SimpleDateFormat(DATE_FORMAT_PATTERN, Locale.ENGLISH).apply {
                timeZone = IST
                isLenient = false
            }
            val parsed = sdf.parse(combined)
            if (parsed == null) {
                null
            } else {
                parsed.time
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun splitAnchorAndAmount(value: String): RowParts {
        val amountMatch = Regex("""\s+((?:₹|Rs\.?)\s*[0-9][0-9,]*(?:\.[0-9]{1,2})?)$""")
            .find(value)
            ?: return RowParts(value.trim(), null)

        val anchorLine = value.substring(0, amountMatch.range.first).trim()
        return RowParts(anchorLine, amountMatch.groupValues[1].trim())
    }

    /**
     * Extracts bank name and last 4 digits from the account line.
     * Expense: "Paid by Example Bank 1000"
     * Income:  "Paid to Example Bank 1000"
     * Uses [bankAccountLineRegex] to stay consistent with [isTransactionAnchor].
     */
    private fun extractAccountInfo(lines: List<String>): AccountInfo {
        val accountLine = lines.firstOrNull { bankAccountLineRegex.matches(it) }
        if (accountLine == null) {
            return AccountInfo(null, null)
        }

        val match    = bankAccountLineRegex.find(accountLine)
        val bankName = match?.groupValues?.getOrNull(1)?.trim()
        val last4 = match?.groupValues?.getOrNull(2)?.trim()

        return AccountInfo(bankName, last4)
    }

    // ─── Data classes ─────────────────────────────────────────────────────────

    private data class AccountInfo(val bankName: String?, val last4: String?)
    private data class RowParts(val anchorLine: String, val amountLine: String?)
}
