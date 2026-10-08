package com.ritesh.cashiro.data.preferences

import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import org.junit.Test
import org.junit.Assert.*
import java.math.BigDecimal
import java.time.LocalDateTime

class BankAccountMergeStoreTest {
    private fun account(suffix: String) = AccountBalanceEntity(bankName = "Example Bank", accountLast4 = suffix, currency = "INR", balance = BigDecimal.ZERO, timestamp = LocalDateTime.of(2026, 1, 1, 0, 0))
    @Test fun onlyUnambiguousBankCurrencyPairsAreSuggested() {
        val short = account("000")
        val full = account("1000")
        assertEquals(1, BankAccountMergeStore.duplicatePairs(listOf(short, full)).size)
        assertTrue(BankAccountMergeStore.duplicatePairs(listOf(short, full, account("2000"))).isEmpty())
        assertTrue(BankAccountMergeStore.duplicatePairs(listOf(short, full.copy(currency = "USD"))).isEmpty())
        assertTrue(BankAccountMergeStore.duplicatePairs(listOf(short, full.copy(bankName = "Other Bank"))).isEmpty())
        assertTrue(BankAccountMergeStore.duplicatePairs(listOf(short, full.copy(isWallet = true))).isEmpty())
    }
    @Test fun aliasesRequireConfirmationAndStayBankScoped() {
        assertEquals("000", BankAccountMergeStore.resolveSuffix("Example Bank", "INR", "000", emptyMap()))
        val mappings = BankAccountMergeStore.mappingsAfterMerge(emptyMap(), account("000"), account("1000"))
        assertEquals("1000", BankAccountMergeStore.resolveSuffix("Example Bank", "INR", "000", mappings))
        assertEquals("000", BankAccountMergeStore.resolveSuffix("Other Bank", "INR", "000", mappings))
        assertEquals("000", BankAccountMergeStore.resolveSuffix("Example Bank", "USD", "000", mappings))
        assertEquals("2000", BankAccountMergeStore.mappingsAfterMerge(mappings, account("1000"), account("2000")).values.single())
        assertTrue(BankAccountMergeStore.mappingsAfterMerge(mappings, account("1000"), account("2001")).isEmpty())
    }
}
