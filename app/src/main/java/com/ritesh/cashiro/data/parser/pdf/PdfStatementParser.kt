package com.ritesh.cashiro.data.parser.pdf

import android.util.Log
import com.ritesh.parser.core.ParsedTransaction
import com.ritesh.parser.core.TransactionType
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

interface PdfStatementParser {
    fun canHandle(text: String): Boolean
    fun parse(text: String): List<ParsedTransaction>
}

class GPayPdfParser : PdfStatementParser {

    companion object {
        private const val TAG = "GPayPdfParser"
        private const val DATE_FORMAT_PATTERN = "dd MMM, yyyy hh:mm a"
        private const val DATE_BUFFER_SIZE = 8
    }

    private val IST = TimeZone.getTimeZone("Asia/Kolkata")

    private val merchantAnchorRegex = Regex("""^Paid\s+to\s+(.+)$""", RegexOption.IGNORE_CASE)
    private val receivedAnchorRegex = Regex("""^Received\s+from\s+(.+)$""", RegexOption.IGNORE_CASE)
    private val bankAccountLineRegex = Regex("""^Paid\s+(?:by|to)\s+(.+)\s+(Bank|Card|A/c)\s+(\d{4})$""", RegexOption.IGNORE_CASE)
    private val upiIdRegex = Regex("""UPI\s+Transaction\s+ID[:\s]+(\d+)""", RegexOption.IGNORE_CASE)
    private val amountRegex = Regex("""^(?:₹|Rs\.?)\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)$""")
    private val dateLineRegex = Regex("""^(\d{1,2})\s+(\w{3}),?$""")
    private val yearLineRegex = Regex("""^(20\d{2})$""")
    private val timeLineRegex = Regex("""^(\d{1,2}:\d{2})\s*([AaPp][Mm])$""")

    override fun canHandle(text: String): Boolean {
        val lower = text.lowercase()
        val result = ("google pay" in lower || "gpay" in lower) && "upi transaction id" in lower
        Log.d(TAG, "canHandle=$result")
        return result
    }

    override fun parse(text: String): List<ParsedTransaction> {
        Log.i(TAG, "Starting parse — text length=${text.length}")
        val blocks = splitIntoBlocks(text)
        Log.i(TAG, "Split into ${blocks.size} blocks")
        val transactions = blocks.mapIndexedNotNull { index, block -> parseBlock(block, index) }
        Log.i(TAG, "Finished: ${transactions.size}/${blocks.size} transactions parsed")
        return transactions
    }

    private fun splitIntoBlocks(text: String): List<String> {
        val blocks = mutableListOf<String>()
        val buffer = ArrayDeque<String>()
        val current = StringBuilder()
        var inBlock = false

        for (rawLine in text.lines()) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue

            if (isTransactionAnchor(line)) {
                if (inBlock && current.isNotEmpty()) {
                    blocks.add(current.toString().trim())
                    current.clear()
                }
                buffer.forEach { current.appendLine(it) }
                buffer.clear()
                inBlock = true
            }

            if (inBlock) {
                current.appendLine(line)
            } else {
                buffer.addLast(line)
                if (buffer.size > DATE_BUFFER_SIZE) buffer.removeFirst()
            }
        }

        if (current.isNotEmpty()) blocks.add(current.toString().trim())
        return blocks
    }

    private fun isTransactionAnchor(line: String): Boolean {
        if (receivedAnchorRegex.matches(line)) return true
        if (merchantAnchorRegex.matches(line)) {
            return !bankAccountLineRegex.matches(line)
        }
        return false
    }

    private fun parseBlock(block: String, index: Int): ParsedTransaction? {
        val lines = block.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val anchorLine = lines.firstOrNull { isTransactionAnchor(it) }
        if (anchorLine == null) {
            Log.w(TAG, "Block[$index] — no anchor found, skipping")
            return null
        }

        val isExpense = merchantAnchorRegex.matches(anchorLine)
        val type = if (isExpense) TransactionType.EXPENSE else TransactionType.INCOME
        val merchant = extractMerchant(anchorLine, isExpense)
        if (merchant == null) {
            Log.w(TAG, "Block[$index] — could not extract merchant from: '$anchorLine'")
            return null
        }

        val amount = extractAmount(lines)
        if (amount == null) {
            Log.w(TAG, "Block[$index] merchant='$merchant' — no amount found")
            return null
        }

        val timestamp = extractTimestamp(lines, merchant)
        val upiId = lines.firstNotNullOfOrNull { upiIdRegex.find(it)?.groupValues?.get(1) }
        val account = extractAccountInfo(lines)

        return ParsedTransaction(
            amount = amount,
            type = type,
            merchant = merchant,
            reference = upiId,
            accountLast4 = account.last4,
            balance = null,
            smsBody = block,
            sender = "GPay PDF",
            timestamp = timestamp ?: System.currentTimeMillis(),
            bankName = account.bankName ?: "Google Pay"
        )
    }

    private fun extractMerchant(anchorLine: String, isExpense: Boolean): String? {
        val regex = if (isExpense) merchantAnchorRegex else receivedAnchorRegex
        return regex.find(anchorLine)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun extractAmount(lines: List<String>): BigDecimal? {
        for (line in lines) {
            val raw = amountRegex.find(line)?.groupValues?.get(1) ?: continue
            val cleaned = raw.replace(",", "")
            val amount = cleaned.toBigDecimalOrNull()
            if (amount != null) return amount
        }
        return null
    }

    private fun extractTimestamp(lines: List<String>, merchant: String): Long? {
        var dateLine: String? = null
        var yearLine: String? = null
        var timeLine: String? = null

        for (line in lines) {
            when {
                dateLine == null && dateLineRegex.matches(line) -> dateLine = line
                yearLine == null && yearLineRegex.matches(line) -> yearLine = line
                timeLine == null && timeLineRegex.matches(line) -> timeLine = line
            }
            if (dateLine != null && yearLine != null && timeLine != null) break
        }

        if (dateLine == null || yearLine == null || timeLine == null) {
            Log.e(TAG, "Incomplete timestamp for '$merchant'")
            return null
        }

        val dateClean = dateLine.trimEnd(',', ' ')
        val timeClean = timeLine.replace(Regex("""\s+"""), " ").uppercase()
        val combined = "$dateClean, $yearLine $timeClean"

        return try {
            val sdf = SimpleDateFormat(DATE_FORMAT_PATTERN, Locale.ENGLISH).apply {
                timeZone = IST
                isLenient = false
            }
            sdf.parse(combined)?.time
        } catch (e: Exception) {
            Log.e(TAG, "Date parse exception for '$merchant' — input='$combined': ${e.message}")
            null
        }
    }

    private fun extractAccountInfo(lines: List<String>): AccountInfo {
        val accountLine = lines.firstOrNull { bankAccountLineRegex.matches(it) }
        if (accountLine == null) return AccountInfo(null, null)

        val match = bankAccountLineRegex.find(accountLine)
        val bankName = if (match != null) {
            val first = match.groupValues.getOrNull(1)?.trim()
            val second = match.groupValues.getOrNull(2)?.trim()
            listOfNotNull(first, second).joinToString(" ").takeIf { it.isNotBlank() }
        } else null
        val last4 = match?.groupValues?.getOrNull(3)?.trim()
        return AccountInfo(bankName, last4)
    }

    private data class AccountInfo(val bankName: String?, val last4: String?)
}


class PhonePePdfParser : PdfStatementParser {
    override fun canHandle(text: String): Boolean {
        val canHandle = text.contains("PhonePe", ignoreCase = true) || text.contains("Phone Pe", ignoreCase = true)
        Log.e("PDF_PARSER_DEBUG", "PhonePePdfParser canHandle: $canHandle")
        return canHandle
    }

    override fun parse(text: String): List<ParsedTransaction> {
        Log.e("PDF_PARSER_DEBUG", "PhonePePdfParser starting parse. Text length: ${text.length}")
        val transactions = mutableListOf<ParsedTransaction>()
        
        // Flexible Date Regex for PhonePe statements
        // Matches "Feb 06, 2026" or "06 Feb, 2026"
        // Permissive separator between hours/minutes (e.g. "04??26 pm" or "04:26 pm")
        val dateRegex = Regex("""(\d{1,2}\s+[A-Za-z]{3,10},?\s*\d{4}|[A-Za-z]{3,10}\s+\d{1,2},?\s*\d{4})\s*(\d{1,2}[^\d\n\r]{1,5}\d{2}\s*[ap]m)""", RegexOption.IGNORE_CASE)
        
        val matches = dateRegex.findAll(text).toList()
        Log.d("PDF_PARSER_DEBUG", "Found ${matches.size} date matches for PhonePe")

        if (matches.isEmpty()) {
            val firstPart = text.take(500)
            val hexDump = firstPart.map { String.format("\\u%04x", it.toInt()) }.joinToString("")
            Log.e("PDF_PARSER_DEBUG", "No date matches found in PhonePe text. First 500 chars: $firstPart")
            Log.e("PDF_PARSER_DEBUG", "Hex dump of first 500 chars: $hexDump")
            return emptyList()
        }

        val allRows = mutableListOf<String>()
        for (i in matches.indices) {
            val start = matches[i].range.first
            val end = if (i + 1 < matches.size) matches[i + 1].range.first else text.length
            val rowText = text.substring(start, end).replace(Regex("""\s+"""), " ")
            allRows.add(rowText)
            Log.v("PDF_PARSER_DEBUG", "PhonePe Row $i: $rowText")
        }

        val amountRegex = Regex("""(?:₹|Rs\.?)\s*([0-9,]+(?:\.\d+)?)""", RegexOption.IGNORE_CASE)

        for (row in allRows) {
            val dateMatch = dateRegex.find(row) ?: continue
            val dateStr = dateMatch.groupValues[1]
            val timeStr = dateMatch.groupValues[2]
            
            val dateTime = try {
                // Normalize 4-letter month abbeviation "Sept" -> "Sep" before parsing
                val normalizedDateStr = dateStr
                    .replace(Regex("""\bSept\b""", RegexOption.IGNORE_CASE), "Sep")
                val cleanedTime = timeStr.replace(Regex("""[^0-9\s[ap]m]+""", RegexOption.IGNORE_CASE), ":")
                val combined = "$normalizedDateStr $cleanedTime".replace(Regex("""\s+"""), " ")
                
                // Try various formatters
                val patterns = listOf(
                    "MMM dd, yyyy h:mm a",
                    "MMM d, yyyy h:mm a",
                    "dd MMM yyyy h:mm a",
                    "d MMM yyyy h:mm a",
                    "MMM dd, yyyy hh:mm a"
                )
                
                var parsedDate: LocalDateTime? = null
                for (pattern in patterns) {
                    try {
                        parsedDate = LocalDateTime.parse(combined, DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH))
                        break
                    } catch (e: Exception) {
                        try {
                            parsedDate = LocalDateTime.parse(combined.replace(",", ""), DateTimeFormatter.ofPattern(pattern.replace(",", ""), Locale.ENGLISH))
                            break
                        } catch (e2: Exception) {
                            val uppercaseCombined = combined.replace("am", "AM").replace("pm", "PM")
                            try {
                                parsedDate = LocalDateTime.parse(uppercaseCombined, DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH))
                                break
                            } catch (e3: Exception) {
                                try {
                                    parsedDate = LocalDateTime.parse(uppercaseCombined.replace(",", ""), DateTimeFormatter.ofPattern(pattern.replace(",", ""), Locale.ENGLISH))
                                    break
                                } catch (e4: Exception) {
                                    // continue
                                }
                            }
                        }
                    }
                }
                parsedDate ?: throw Exception("All patterns failed for: $combined")
            } catch (e: Exception) {
                Log.w("PDF_PARSER_DEBUG", "Failed to parse date: $dateStr $timeStr - ${e.message}")
                continue
            }
            
            // Primary: look for ₹ or Rs. symbol
            val amountMatch = amountRegex.find(row)
            var amountStr = amountMatch?.groupValues?.get(1)?.replace(",", "")
            // Fallback: for Gift Card credits and similar where amount has no ₹ symbol
            // e.g. the amount column just has a plain number at end of line
            if (amountStr == null) {
                val plainAmountMatch = Regex("""\b([0-9]+(?:\.[0-9]{1,2})?)\s*$""").find(row)
                amountStr = plainAmountMatch?.groupValues?.get(1)
            }
            if (amountStr == null) {
                Log.w("PDF_PARSER_DEBUG", "No amount found in row: $row")
                continue
            }
            val amount = BigDecimal(amountStr)
            
            val isIncome = row.contains("CREDIT", ignoreCase = true)
            val type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE
            
            // Non-greedy merchant extraction
            // Handles "Transact ion ID" typo and merged separators
            val merchantMatch = if (isIncome) {
                // Covers: "Received from X", "Credited by X", "Cashback from X", "From X"
                Regex("""(?:Received from|Cashback from|Credited by|Paid by|From)\s+(.+?)(?=\s+(?:Transact\s*ion|UTR|CREDIT|DEBIT|₹|Rs|$))""", RegexOption.IGNORE_CASE).find(row)
            } else {
                Regex("""(?:Paid to)\s+(.+?)(?=\s+(?:Transact\s*ion|UTR|CREDIT|DEBIT|₹|Rs|$))""", RegexOption.IGNORE_CASE).find(row)
            }
            
            // Fallback for merchant if the explicit "Paid to" etc prefix is merged with amount
            var merchant = merchantMatch?.groupValues?.get(1)?.trim() ?: run {
                // If "Paid to" is merged like "106Paid to",  try a different approach
                val fallbackMatch = Regex("""Paid to\s+(.+?)(?=\s+(?:Transact\s*ion|UTR|CREDIT|DEBIT|$))""", RegexOption.IGNORE_CASE).find(row)
                fallbackMatch?.groupValues?.get(1)?.trim()
            } ?: run {
                Log.w("PDF_PARSER_DEBUG", "Merchant not found in row: $row")
                "Unknown"
            }
            
            // Clean up common prefixes that might leak into merchant
            merchant = merchant.replace(Regex("""^Paid to\s+""", RegexOption.IGNORE_CASE), "").trim()
            
            val bankMatch = Regex("""(?:Credited to|Paid by)\s+\d*X+(\d{4})""").find(row)
            val accountLast4 = bankMatch?.groupValues?.get(1)
            
            val transIdMatch = Regex("""Transact\s*ion\s+ID\s*[:\s]*([A-Z0-9]+)""", RegexOption.IGNORE_CASE).find(row)
            val transId = transIdMatch?.groupValues?.get(1)
            val utrNoMatch = Regex("""UTR\s+No\.\s*[:\s]*([\d\s]+)""", RegexOption.IGNORE_CASE).find(row)
            val utrNo = utrNoMatch?.groupValues?.get(1)?.replace(Regex("""\D"""), "")
            
            val originalMessage = buildString {
                if (isIncome) {
                    append("Received from $merchant\n")
                } else {
                    append("Paid to $merchant\n")
                }
                if (transId != null) append("Transaction ID: $transId\n")
                if (utrNo != null) append("UTR No. $utrNo\n")
                
                val fullAccMatch = Regex("""(?:Credited to|Paid by)\s+([0-9X]+)""").find(row)
                val fullAcc = fullAccMatch?.groupValues?.get(1)
                
                if (fullAcc != null) {
                    if (isIncome) {
                        append("Credited to $fullAcc")
                    } else {
                        append("Paid by $fullAcc")
                    }
                }
                append("\nType: ${if (isIncome) "CREDIT" else "DEBIT"}")
            }.trim()

            transactions.add(
                ParsedTransaction(
                    amount = amount,
                    type = type,
                    merchant = merchant,
                    reference = utrNo ?: transId,
                    accountLast4 = accountLast4,
                    bankName = "PhonePe",
                    smsBody = originalMessage,
                    timestamp = dateTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    sender = "PhonePe PDF",
                    balance = null
                )
            )
        }
        
        Log.d("PDF_PARSER_DEBUG", "Final transactions count for PhonePe: ${transactions.size}")
        return transactions
    }
}
