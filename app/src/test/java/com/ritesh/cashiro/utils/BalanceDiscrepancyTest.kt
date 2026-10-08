package com.ritesh.cashiro.utils

import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import com.ritesh.cashiro.data.database.entity.TransactionEntity
import com.ritesh.cashiro.data.database.entity.TransactionType
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime

class BalanceDiscrepancyTest {
    private val at = LocalDateTime.of(2026, 1, 1, 12, 0)
    private val anchor = AccountBalanceEntity(id = 1, bankName = "Example Bank", accountLast4 = "1000",
        balance = BigDecimal("1000"), timestamp = at.minusHours(1), sourceType = "SMS_BALANCE")
    private val report = TransactionEntity(id = 1, amount = BigDecimal("100"), merchantName = "Example Shop",
        category = "Others", transactionType = TransactionType.EXPENSE, dateTime = at,
        bankName = "Example Bank", accountNumber = "1000", balanceAfter = BigDecimal("850"), transactionHash = "synthetic-report")

    @Test fun debitGapPredictsAndAdjustmentClearsItWithoutMovingTheAnchor() {
        val discrepancy = BalanceDiscrepancy.compute(report, anchor, listOf(report))!!
        assertEquals(BigDecimal("900"), discrepancy.expected)
        assertEquals(BigDecimal("-50"), discrepancy.delta)
        val adjustment = report.copy(id = 2, amount = BigDecimal("50"), dateTime = at.minusSeconds(1), balanceAfter = null)
        assertNull(BalanceDiscrepancy.compute(report, anchor, listOf(report, adjustment)))
        assertEquals(BigDecimal("1000"), anchor.balance)
    }

    @Test fun incomeGapIsPositiveAndUnreportedOrCardTransactionsAreSkipped() {
        assertEquals(BigDecimal("50"), BalanceDiscrepancy.compute(report.copy(balanceAfter = BigDecimal("950")), anchor, listOf(report))!!.delta)
        assertNull(BalanceDiscrepancy.compute(report.copy(balanceAfter = null), anchor, listOf(report)))
        assertNull(BalanceDiscrepancy.compute(report.copy(transactionType = TransactionType.CREDIT), anchor, listOf(report)))
        val incompleteTransfer = report.copy(id = 2, transactionType = TransactionType.TRANSFER, dateTime = at.minusMinutes(1))
        assertNull(BalanceDiscrepancy.compute(report, anchor, listOf(report, incompleteTransfer)))
    }

    @Test fun ignoresDifferentBankCurrencyAndDeletedRows() {
        val unrelated = listOf(report.copy(id = 2, bankName = "Other Bank", dateTime = at.minusMinutes(5)),
            report.copy(id = 3, currency = "USD", dateTime = at.minusMinutes(5)),
            report.copy(id = 4, isDeleted = true, dateTime = at.minusMinutes(5)))
        assertEquals(BigDecimal("-50"), BalanceDiscrepancy.compute(report, anchor, unrelated + report)!!.delta)
        assertNull(BalanceDiscrepancy.compute(report, anchor.copy(currency = "USD"), listOf(report)))
        assertNull(BalanceDiscrepancy.compute(report, anchor.copy(isCreditCard = true), listOf(report)))
    }

    @Test fun sameSuffixTransferUsesBothExplicitBanks() {
        val transfer = report.copy(id = 2, transactionType = TransactionType.TRANSFER,
            fromAccount = "1000", toAccount = "1000", fromBankName = "Other Bank", toBankName = "Example Bank",
            dateTime = at.minusMinutes(5), balanceAfter = null)
        assertEquals(BigDecimal("100"), BalanceDiscrepancy.effectOn(transfer, "Example Bank", "1000"))
        assertEquals(BigDecimal("-100"), BalanceDiscrepancy.effectOn(transfer, "Other Bank", "1000"))
        assertEquals(BigDecimal.ZERO, BalanceDiscrepancy.effectOn(transfer, "Third Bank", "1000"))
        assertNull(BalanceDiscrepancy.compute(report, anchor, listOf(transfer.copy(toBankName = null), report)))
    }

    @Test fun includesLendingBorrowingAndInvestmentsAndIgnoresSmallNoise() {
        assertEquals(BigDecimal("-100"), BalanceDiscrepancy.effectOn(report.copy(transactionType = TransactionType.LENT), "Example Bank", "1000"))
        assertEquals(BigDecimal("100"), BalanceDiscrepancy.effectOn(report.copy(transactionType = TransactionType.BORROWED), "Example Bank", "1000"))
        assertEquals(BigDecimal("-100"), BalanceDiscrepancy.effectOn(report.copy(transactionType = TransactionType.INVESTMENT), "Example Bank", "1000"))
        assertNull(BalanceDiscrepancy.compute(report.copy(balanceAfter = BigDecimal("899.50")), anchor, listOf(report)))
    }

    @Test fun excludesSnapshotBoundaryAndRefusesAmbiguousSimultaneousActivity() {
        val previous = report.copy(id = 2, dateTime = anchor.timestamp)
        assertEquals(BigDecimal("-50"), BalanceDiscrepancy.compute(report, anchor, listOf(previous, report))!!.delta)
        assertNull(BalanceDiscrepancy.compute(report, anchor, listOf(report, report.copy(id = 2))))
        val adjustment = report.copy(id = 2, amount = BigDecimal("50"), transactionHash = BalanceDiscrepancy.adjustmentHash(report.id))
        assertNull(BalanceDiscrepancy.compute(report, anchor, listOf(report, adjustment)))
    }
}
