package com.ritesh.cashiro.data.parser.pdf

import android.util.Log
import com.ritesh.parser.core.ParsedTransaction
import com.ritesh.parser.core.TransactionType
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Interface for PDF statement parsers that extract transactions from bank PDFs.
 */
interface PdfStatementParser {
    /**
     * Checks if this parser can handle the given PDF text.
     */
    fun canHandle(text: String): Boolean

    /**
     * Parses the PDF text and returns a list of [ParsedTransaction]s.
     */
    fun parse(text: String): List<ParsedTransaction>
}

/**
 * Parser for GPay (Google Pay) transaction history PDF statements.
 */
class GPayPdfParser : PdfStatementParser {
    override fun canHandle(text: String): Boolean {
        val canHandle = (text.contains("GPay", ignoreCase = true) || text.contains("Google Pay", ignoreCase = true)) 
                        && text.contains("UPI Transaction ID", ignoreCase = true)
        Log.d("PDF_PARSER_DEBUG", "GPayPdfParser canHandle: $canHandle")
        return canHandle
    }

    override fun parse(text: String): List<ParsedTransaction> {
        Log.d("PDF_PARSER_DEBUG", "GPayPdfParser starting parse. Text size: ${text.length}")
        val transactions = mutableListOf<ParsedTransaction>()

        // Regex to find start of a transaction (Date pattern)
        // More flexible date: "01 Aug 2025" or "01 Aug, 2025"
        // Also handling possible newlines between date and time
        val dateRegex = Regex(
            """(\d{1,2}\s+[A-Za-z]{3},?\s+\d{4})\s*(\d{1,2}:\d{2}\s+[AP]M)""",
            RegexOption.IGNORE_CASE
        )

        val matches = dateRegex.findAll(text).toList()
        if (matches.isEmpty()) return emptyList()

        val allRows = mutableListOf<String>()
        for (i in matches.indices) {
            val start = matches[i].range.first
            val end = if (i + 1 < matches.size) matches[i + 1].range.first else text.length
            allRows.add(text.substring(start, end).replace(Regex("""\s+"""), " "))
        }

        val amountRegex = Regex("""(?:₹|Rs\.?)\s*([0-9,]+(?:\.\d+)?)""", RegexOption.IGNORE_CASE)

        for (row in allRows) {
            val dateMatch = dateRegex.find(row) ?: continue
            val dateStr = dateMatch.groupValues[1]
            val timeStr = dateMatch.groupValues[2]

            val dateTime = try {
                val cleanedDate = dateStr.replace(",", "")
                LocalDateTime.parse(
                    "$cleanedDate $timeStr",
                    DateTimeFormatter.ofPattern("d MMM yyyy h:mm a", Locale.ENGLISH)
                )
            } catch (e: Exception) {
                try {
                    val cleanedDate = dateStr.replace(",", "")
                    LocalDateTime.parse(
                        "$cleanedDate $timeStr",
                        DateTimeFormatter.ofPattern("dd MMM yyyy hh:mm a", Locale.ENGLISH)
                    )
                } catch (e2: Exception) {
                    continue
                }
            }

            val amountMatch = amountRegex.find(row)
            val amountStr = amountMatch?.groupValues?.get(1)?.replace(",", "") ?: continue
            val amount = BigDecimal(amountStr)

            val isIncome = row.contains("Received from", ignoreCase = true)
            val type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE

            // Non-greedy merchant extraction
            val merchantMatch = if (isIncome) {
                Regex(
                    """(?:Received from|Paid by)\s+(.+?)(?=\s+UPI Transaction ID|Paid to|Paid by|$)""",
                    RegexOption.IGNORE_CASE
                ).find(row)
            } else {
                Regex(
                    """Paid to\s+(.+?)(?=\s+UPI Transaction ID|Paid to|Paid by|$)""",
                    RegexOption.IGNORE_CASE
                ).find(row)
            }

            val merchant = merchantMatch?.groupValues?.get(1)?.trim() ?: "Unknown"

            // Bank info is usually at the end before the amount
            // For Expense: "Paid by [Bank] [Last4] [Amount]"
            // For Income: "Paid to [Bank] [Last4] [Amount]"
            val bankMatch = if (isIncome) {
                Regex(
                    """Paid to\s+(.+?)\s+X*(\d{2,4})(?=\s*[₹Rs])""",
                    RegexOption.IGNORE_CASE
                ).find(row)
            } else {
                Regex(
                    """Paid by\s+(.+?)\s+X*(\d{2,4})(?=\s*[₹Rs])""",
                    RegexOption.IGNORE_CASE
                ).find(row)
            }
            val bankName = bankMatch?.groupValues?.get(1)?.trim()
            val accountLast4 = bankMatch?.groupValues?.get(2)

            val upiMatch = Regex("""UPI Transaction ID:\s*(\d+)""").find(row)
            val upiId = upiMatch?.groupValues?.get(1)

            val originalMessage = buildString {
                if (isIncome) {
                    append("Received from $merchant\n")
                } else {
                    append("Paid to $merchant\n")
                }
                if (upiId != null) append("UPI Transaction ID: $upiId\n")
                if (bankName != null && accountLast4 != null) {
                    if (isIncome) {
                        append("Paid to $bankName $accountLast4")
                    } else {
                        append("Paid by $bankName $accountLast4")
                    }
                }
            }.trim()

            transactions.add(
                ParsedTransaction(
                    amount = amount,
                    type = type,
                    merchant = merchant,
                    reference = upiId,
                    accountLast4 = accountLast4,
                    bankName = bankName ?: "GPay",
                    smsBody = originalMessage,
                    timestamp = dateTime.atZone(java.time.ZoneId.systemDefault()).toInstant()
                        .toEpochMilli(),
                    sender = "GPay PDF",
                    balance = null
                )
            )
        }

        return transactions
    }
}


/**
 * Parser for PhonePe transaction history PDF statements.
 */
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
                    "MMM dd, yyyy hh:mm a",
                    "MMMM dd, yyyy h:mm a",
                    "MMMM d, yyyy h:mm a",
                    "dd MMMM yyyy h:mm a",
                    "d MMMM yyyy h:mm a",
                    "MMMM dd, yyyy hh:mm a"
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
            
            // Extract account number: looks for "Credited to" or "Paid by" followed by something like "XX1234" or "XXXXXX12"
            // Also supports shorter account suffixes (e.g., 2 digits). 
            // Use non-greedy prefix to prevent capturing too few digits if 4 are available.
            val bankMatch = Regex("""(?:Credited to|Paid by|Account)\s*[:\s]*[0-9Xx]*?X*(\d{2,4})\b""", RegexOption.IGNORE_CASE).find(row)
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
                    reference = transId ?: utrNo,
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
