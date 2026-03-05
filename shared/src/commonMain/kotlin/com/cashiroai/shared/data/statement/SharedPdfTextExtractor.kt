package com.cashiroai.shared.data.statement

expect object SharedPdfTextExtractor {
    fun extractText(filePath: String): String
}
