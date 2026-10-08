package com.ritesh.cashiro.data.mapper

import com.ritesh.cashiro.data.database.entity.TransactionEntity
import com.ritesh.cashiro.data.database.entity.TransactionType
import java.math.BigDecimal
import java.time.LocalDateTime
import org.junit.Test
import org.junit.Assert.*

class TransferAccountSelectionTest {
    private fun row() = TransactionEntity(amount = BigDecimal("100"), merchantName = "Example Shop", category = "Transfer", transactionType = TransactionType.TRANSFER,
        dateTime = LocalDateTime.of(2026, 1, 1, 0, 0), transactionHash = "synthetic", bankName = "Example Bank", accountNumber = "1000",
        fromAccount = "1000", fromBankName = "Example Bank", toAccount = "2000", toBankName = "Other Bank")
    @Test fun bankOnlyTargetEditReversesOldBankAndAppliesNewBank() {
        val old = row()
        val changes = transferBalanceChanges(old, old.copy(toBankName = "Third Bank"), emptyList())
        assertEquals(BigDecimal("-100"), changes[TransferBalanceKey("Other Bank", "2000", "INR")])
        assertEquals(BigDecimal("100"), changes[TransferBalanceKey("Third Bank", "2000", "INR")])
        assertEquals(2, changes.size)
        assertTrue(transferBalanceChanges(old, old, emptyList()).isEmpty())
    }
    @Test fun sourceMoveAndAmountEditApplyNetChanges() {
        val old = row()
        val moved = old.withTransferAccount("3000", "Third Bank", false).copy(amount = BigDecimal("150"))
        val changes = transferBalanceChanges(old, moved, emptyList())
        assertEquals(BigDecimal("100"), changes[TransferBalanceKey("Example Bank", "1000", "INR")])
        assertEquals(BigDecimal("-150"), changes[TransferBalanceKey("Third Bank", "3000", "INR")])
        assertEquals(BigDecimal("50"), changes[TransferBalanceKey("Other Bank", "2000", "INR")])
    }
    @Test fun unresolvedLegacyBankIsReportedInsteadOfSilentlyLosingAnEffect() {
        var unresolved = 0
        val legacy = row().copy(fromBankName = null, bankName = null)
        transferBalanceChanges(legacy, legacy.copy(amount = BigDecimal("150")), emptyList()) { unresolved++ }
        assertTrue(unresolved > 0)
    }
    @Test fun metadataOnlyLegacyTransferEditDoesNotResolveUnknownBank() {
        val legacy = row().copy(fromBankName = null, bankName = null)
        val changes = transferBalanceChanges(legacy, legacy.copy(description = "Synthetic note", category = "Travel"), emptyList()) {
            fail("Metadata edits must not require bank resolution")
        }
        assertTrue(changes.isEmpty())
    }
    @Test fun incomingOwnerAndClearReselectKeepOppositeLeg() {
        val incoming = row().copy(bankName = "Other Bank", accountNumber = "2000")
        val sourceChanged = incoming.withTransferAccount("3000", "Third Bank", false)
        assertEquals("Other Bank", sourceChanged.bankName)
        assertEquals("2000", sourceChanged.accountNumber)
        val reselected = sourceChanged.withTransferAccount(null, null, true).withTransferAccount("4000", "Fourth Bank", true)
        assertEquals("Fourth Bank", reselected.bankName)
        assertEquals("4000", reselected.accountNumber)
        assertEquals("3000", reselected.fromAccount)
        assertEquals("Third Bank", reselected.fromBankName)
    }
}
