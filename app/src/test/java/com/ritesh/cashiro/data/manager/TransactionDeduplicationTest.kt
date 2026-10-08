package com.ritesh.cashiro.data.manager

import com.ritesh.cashiro.data.database.entity.TransactionEntity
import com.ritesh.cashiro.data.database.entity.TransactionType
import com.ritesh.cashiro.data.statement.StatementTransactionEnricher
import org.junit.Test
import org.junit.Assert.*
import java.math.BigDecimal
import java.time.LocalDateTime

class TransactionDeduplicationTest {
    private fun transaction() = TransactionEntity(id = 1, amount = BigDecimal("100.00"), merchantName = "UPI Transaction", category = "Food", transactionType = TransactionType.EXPENSE, dateTime = LocalDateTime.of(2026, 1, 1, 12, 0), bankName = "Example Bank", accountNumber = "1000", transactionHash = "synthetic", reference = "000000000001", balanceAfter = BigDecimal("900.00"))
    @Test fun crossChannelKeepsOppositeDirectionTransactions() {
        val sms = transaction().copy(smsSender = "EXAMPLE-T")
        val push = sms.copy(smsSender = "ExamplePush")
        assertTrue(TransactionDeduplication.isBookedByOtherChannel(sms, listOf(push), setOf("ExamplePush")))
        assertFalse(TransactionDeduplication.isBookedByOtherChannel(sms, listOf(push.copy(transactionType = TransactionType.INCOME)), setOf("ExamplePush")))
    }
    @Test fun delayedAlertsMatchButCurrencyAndAccountCollisionsDoNot() {
        val row = transaction()
        assertTrue(TransactionDeduplication.isSameUpiTransaction(row, row.copy(dateTime = row.dateTime.plusHours(2))))
        assertFalse(TransactionDeduplication.isSameUpiTransaction(row, row.copy(currency = "USD")))
        assertFalse(TransactionDeduplication.isSameUpiTransaction(row, row.copy(accountNumber = "2000")))
        assertFalse(TransactionDeduplication.isSameUpiTransaction(row, row.copy(transactionType = TransactionType.INCOME)))
        assertEquals(listOf(2L), TransactionDeduplication.duplicateIdsToDelete(listOf(row, row.copy(id = 2, bankName = "State Bank of India", balanceAfter = null))))
    }
    @Test fun statementEnrichmentPreservesBankBalanceAndUserMetadata() {
        val bank = transaction()
        val statement = bank.copy(id = 0, merchantName = "Example Shop", bankName = "GPay", balanceAfter = null, category = "Miscellaneous", transactionHash = "pdf")
        val merged = StatementTransactionEnricher.enrich(bank, statement)
        assertEquals("Example Shop", merged.merchantName)
        assertEquals(bank.id, merged.id)
        assertEquals(bank.bankName, merged.bankName)
        assertEquals(bank.balanceAfter, merged.balanceAfter)
        assertEquals(bank.category, merged.category)
        assertEquals(bank.transactionHash, merged.transactionHash)
    }
    @Test fun sameDayStatementFallbackEnrichesOneOlderReferenceLessBankRow() {
        val bank = transaction().copy(reference = null)
        val statement = bank.copy(id = 0, merchantName = "Example Shop", dateTime = bank.dateTime.plusHours(3), reference = "000000000001")
        assertEquals(bank, StatementTransactionEnricher.findUniqueMatch(listOf(bank), statement))
        assertNull(StatementTransactionEnricher.findUniqueMatch(listOf(bank, bank.copy(id = 2)), statement))
        assertNull(StatementTransactionEnricher.findUniqueMatch(listOf(bank.copy(dateTime = bank.dateTime.minusDays(1))), statement))
        assertNull(StatementTransactionEnricher.findUniqueMatch(listOf(bank.copy(reference = "000000000002")), statement))
    }

    @Test fun blankAccountsMatchWithoutBridgingDifferentKnownAccounts() {
        val blank = transaction().copy(id = 1, accountNumber = null)
        val known = blank.copy(id = 2, accountNumber = "1000")
        val other = blank.copy(id = 3, accountNumber = "2000")
        val clusters = TransactionDeduplication.duplicateClusters(listOf(blank, known, other))
        assertEquals(1, clusters.size)
        assertEquals(setOf(1L, 2L), (clusters.single().duplicates + clusters.single().keeper).map { it.id }.toSet())
        assertTrue(TransactionDeduplication.duplicateClusters(listOf(known, other)).isEmpty())
    }
    @Test fun cleanupMetadataCombinesNotesAttachmentsAndRecurrence() {
        val keeper = transaction().copy(category = "Miscellaneous", description = "Synthetic bank note", attachments = "synthetic-a")
        val edited = keeper.copy(category = "Food", subcategory = "Example category", description = "Synthetic edited note",
            attachments = "synthetic-a,synthetic-b", isRecurring = true)
        val merged = TransactionDeduplication.mergeUserMetadata(keeper, edited)!!
        assertEquals("Food", merged.category)
        assertEquals(edited.subcategory, merged.subcategory)
        assertEquals("Synthetic bank note\nSynthetic edited note", merged.description)
        assertEquals("synthetic-a,synthetic-b", merged.attachments)
        assertTrue(merged.isRecurring)
        assertEquals(keeper.bankName, merged.bankName)
        assertEquals(keeper.balanceAfter, merged.balanceAfter)
    }
    @Test fun conflictingUserCategoriesAndSubcategoriesAreNotDiscarded() {
        val keeper = transaction().copy(category = "Food", subcategory = "Example category")
        assertNull(TransactionDeduplication.mergeUserMetadata(keeper, keeper.copy(category = "Travel")))
        assertNull(TransactionDeduplication.mergeUserMetadata(keeper, keeper.copy(subcategory = "Other category")))
    }

    @Test fun referenceProofTakesPriorityOverSameDayFallback() {
        val bank = transaction()
        val statement = bank.copy(id = 0, merchantName = "Example Shop")
        assertEquals(bank, StatementTransactionEnricher.findUniqueMatch(listOf(bank, bank.copy(id = 2, reference = null)), statement))
    }
    @Test fun cleanupPrefersLaterRealBankAndBalanceEvidence() {
        val partner = transaction().copy(id = 1, bankName = "State Bank of India", balanceAfter = null)
        val bank = partner.copy(id = 2, bankName = "Example Bank", dateTime = partner.dateTime.plusMinutes(1), balanceAfter = BigDecimal("900"))
        assertEquals(listOf(1L), TransactionDeduplication.duplicateIdsToDelete(listOf(partner, bank)))
        assertEquals(listOf(1L), TransactionDeduplication.duplicateIdsToDelete(listOf(partner.copy(bankName = "Example Bank"), bank)))
    }

    @Test fun cleanupDoesNotChainTwoTwentyThreeHourGaps() {
        val first = transaction()
        val middle = first.copy(id = 2, dateTime = first.dateTime.plusHours(23))
        val last = first.copy(id = 3, dateTime = first.dateTime.plusHours(46))
        assertEquals(listOf(2L), TransactionDeduplication.duplicateIdsToDelete(listOf(last, middle, first)))
    }
}
