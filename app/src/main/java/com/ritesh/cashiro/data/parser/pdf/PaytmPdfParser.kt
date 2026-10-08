package com.ritesh.cashiro.data.parser.pdf

import com.ritesh.parser.core.ParsedTransaction
import com.ritesh.parser.core.TransactionType
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatterBuilder
import java.time.format.ResolverStyle
import java.util.Locale

class PaytmPdfParser : PdfStatementParser {

    private val dateTimeFormat = DateTimeFormatterBuilder().parseCaseInsensitive()
        .appendPattern("d MMM uuuu h:mm a").toFormatter(Locale.ENGLISH)
        .withResolverStyle(ResolverStyle.STRICT)
    private val statementZone = ZoneId.of("Asia/Kolkata")

    private val dateLineRegex = Regex("""^(\d{1,2})\s+([A-Za-z]{3})$""")
    private val timeLineRegex = Regex("""^(\d{1,2}:\d{2})\s*([AaPp][Mm])$""")
    private val signedAmountRegex = Regex("""^([+-])\s*Rs\.?\s*([\d,]+(?:\.\d{1,2})?)$""")

    private val inlineAmountRegex = Regex("""([+-])\s*Rs\.?\s*([\d,]+(?:\.\d{1,2})?)\s*$""")
    private val upiRefRegex = Regex("""UPI\s+Ref\s+No[:\s]+(\d+)""", RegexOption.IGNORE_CASE)
    private val statementPeriodRegex = Regex(
        """(\d{1,2})\s+([A-Za-z]{3})'(\d{2})\s*-\s*(\d{1,2})\s+([A-Za-z]{3})'(\d{2})"""
    )
    private val accountLineRegex = Regex("""^(.+?)\s*-\s*(\d{1,4})$""")

    private val CATEGORY_MARKER = Regex("""^#\s*\S+\s+""")

    private val anchorPrefixes = listOf(
        "Cashback received from",
        "Money sent to",
        "Received from",
        "Paid to",
        "Paid for",
        "Recharge of",
        "Automatic payment for",
        "Automatic payment of",
        "Purchase of",
    )

    private val skipAnchorPrefixes = listOf(
        "Money blocked for",
        "Money unblocked for",
    )

    private val headerPrefixes = listOf(
        "Page ",
        "For any queries",
        "Contact Us",
        "Passbook Payments History",
        "All payments done by you",
        "Date &",
        "Transaction Details",
    )

    override fun canHandle(text: String): Boolean {
        val lower = text.lowercase()
        val result = "paytm" in lower && "upi ref no" in lower

        return result
    }

    override fun parse(text: String): List<ParsedTransaction> {

        val period = extractStatementPeriod(text) ?: return emptyList()
        val blocks = splitIntoBlocks(text)

        val transactions = blocks.mapNotNull { block ->
            parseBlock(block, period)
        }

        return transactions
    }

    private fun splitIntoBlocks(text: String): List<String> {
        val blocks = mutableListOf<String>()
        val current = StringBuilder()
        var pendingDate: String? = null
        var inBlock = false

        for (rawLine in text.lines()) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue
            if (isHeaderLine(line)) continue

            if (dateLineRegex.matches(line)) {
                pendingDate = line
                continue
            }

            if (timeLineRegex.matches(line) && pendingDate != null) {
                if (inBlock && current.isNotEmpty()) {
                    blocks.add(current.toString().trim())
                    current.clear()
                }
                current.appendLine(pendingDate)
                current.appendLine(line)
                pendingDate = null
                inBlock = true
                continue
            }

            pendingDate = null

            if (inBlock) current.appendLine(line)
        }

        if (current.isNotEmpty()) blocks.add(current.toString().trim())

        return blocks
    }

    private fun isHeaderLine(line: String): Boolean {
        if (line == "Time") return true
        return headerPrefixes.any { line.startsWith(it, ignoreCase = true) }
    }

    private fun parseBlock(block: String, period: StatementPeriod): ParsedTransaction? {
        val lines = block.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.size < 3) {
            return null
        }

        val dateLine = lines[0]
        val timeLine = lines[1]
        val anchorLine = lines[2]

        if (skipAnchorPrefixes.any { anchorLine.startsWith(it, ignoreCase = true) }) {
            return null
        }

        val amountMatch = lines.firstNotNullOfOrNull { signedAmountRegex.find(it) }
            ?: lines.firstNotNullOfOrNull { inlineAmountRegex.find(it) }
        if (amountMatch == null) {
            return null
        }
        val type = if (amountMatch.groupValues[1] == "+") TransactionType.INCOME else TransactionType.EXPENSE
        val amount = amountMatch.groupValues[2].replace(",", "").toBigDecimalOrNull()
        if (amount == null || amount.signum() <= 0 || amount > BigDecimal(10_000_000)) {
            return null
        }

        if (anchorPrefixes.none { anchorLine.startsWith(it, ignoreCase = true) }) return null
        val merchant = extractMerchant(lines)
        val upiRef = lines.firstNotNullOfOrNull { upiRefRegex.find(it)?.groupValues?.get(1) }
        val account = extractAccountInfo(lines)
        val timestamp = extractTimestamp(dateLine, timeLine, period) ?: return null

        return ParsedTransaction(
            amount = amount,
            type = type,
            merchant = merchant,
            reference = upiRef,
            accountLast4 = account.last4,
            balance = null,
            smsBody = block,
            sender = "Paytm PDF",
            timestamp = timestamp,
            bankName = account.bankName ?: "Paytm",
        )
    }

    private fun extractMerchant(lines: List<String>): String {
        val anchorLine = lines[2]
        val prefix = anchorPrefixes.firstOrNull { anchorLine.startsWith(it, ignoreCase = true) }
        var merchant = if (prefix != null) anchorLine.substring(prefix.length).trim() else anchorLine

        val cutAt = Regex("""\s+(?:Note:|Tag:)|\s*[+-]\s*Rs\.""").find(merchant)
        if (cutAt != null) {
            return merchant.substring(0, cutAt.range.first).replace(Regex("""\s+"""), " ").trim()
        }

        var i = 3
        while (i < lines.size && i <= 4 && isMerchantContinuation(lines[i])) {
            merchant = "$merchant ${lines[i]}"
            i++
        }

        return merchant.replace(Regex("""\s+"""), " ").trim()
    }

    private fun isMerchantContinuation(line: String): Boolean {
        if (line.startsWith("UPI ID", ignoreCase = true)) return false
        if (line.startsWith("UPI Ref", ignoreCase = true)) return false
        if (line.startsWith("Note:", ignoreCase = true)) return false
        if (line.startsWith("Tag:", ignoreCase = true)) return false
        if (line.startsWith("#")) return false
        if (line.startsWith("Order ID", ignoreCase = true)) return false
        if (line.startsWith("Rs.", ignoreCase = true)) return false
        if (signedAmountRegex.matches(line)) return false
        if (accountLineRegex.matches(line)) return false
        return true
    }


    private fun extractAccountInfo(lines: List<String>): AccountInfo {

        val amountIndex = lines.indexOfFirst { signedAmountRegex.matches(it) }
        if (amountIndex >= 3) {
            val candidates = lines.subList(maxOf(3, amountIndex - 2), amountIndex)
                .filterNot { it.startsWith("#") || it.startsWith("Tag:") || it.startsWith("Note:") || it.startsWith("UPI") }
            accountFrom(candidates.joinToString(" "))?.let { return it }
        }

        for (i in lines.indices.reversed()) {
            if (i < 3) break
            val line = lines[i]
            if (line.startsWith("UPI", ignoreCase = true)) continue
            if (line.startsWith("Order ID", ignoreCase = true)) continue
            accountFrom(line.replace(CATEGORY_MARKER, ""))?.let { return it }
        }
        return AccountInfo(null, null)
    }

    private fun accountFrom(text: String): AccountInfo? {
        val cleaned = text.replace(Regex("""\s+"""), " ").trim()
        val match = accountLineRegex.find(cleaned) ?: return null
        val bankName = match.groupValues[1].trim().takeIf { it.isNotEmpty() }
        val last4 = match.groupValues[2].trim()
        return AccountInfo(bankName, last4)
    }

    private data class StatementPeriod(val startEpoch: Long, val endYear: Int, val endEpoch: Long)

    private fun extractStatementPeriod(text: String): StatementPeriod? {
        val match = statementPeriodRegex.find(text) ?: return null
        val (startDay, startMon, startYy, endDay, endMon, endYy) = match.destructured
        val startYear = 2000 + (startYy.toIntOrNull() ?: return null)
        val endYear = 2000 + (endYy.toIntOrNull() ?: return null)

        val endEpoch = parseDateTime("$endDay ${normalizeMonth(endMon)} $endYear 11:59 PM") ?: return null

        val startEpoch = parseDateTime("$startDay ${normalizeMonth(startMon)} $startYear 12:00 AM") ?: return null
        if (startEpoch > endEpoch || endYear - startYear > 1) return null
        return StatementPeriod(startEpoch, endYear, endEpoch)
    }

    private fun extractTimestamp(dateLine: String, timeLine: String, period: StatementPeriod): Long? {
        val dateMatch = dateLineRegex.find(dateLine) ?: return null
        val timeMatch = timeLineRegex.find(timeLine) ?: return null

        val day = dateMatch.groupValues[1]
        val month = normalizeMonth(dateMatch.groupValues[2])
        val time = "${timeMatch.groupValues[1]} ${timeMatch.groupValues[2].uppercase()}"

        val endYear = period.endYear
        return listOf(endYear, endYear - 1).firstNotNullOfOrNull { year ->
            parseDateTime("$day $month $year $time")
                ?.takeIf { it in period.startEpoch..period.endEpoch }
        }
    }

    private fun parseDateTime(text: String): Long? {
        return try {
            LocalDateTime.parse(text, dateTimeFormat).atZone(statementZone).toInstant().toEpochMilli()
        } catch (_: java.time.DateTimeException) {
            null
        }
    }

    private fun normalizeMonth(raw: String): String =
        raw.lowercase().replaceFirstChar { it.uppercase() }

    private data class AccountInfo(val bankName: String?, val last4: String?)
}
