package com.ritesh.cashiro.data.backup

import com.ritesh.cashiro.data.database.entity.TransactionEntity
import com.ritesh.cashiro.data.database.entity.TransactionType
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime

class TransactionSanitizationTest {
    private fun transaction() = TransactionEntity(
        amount = BigDecimal("100.00"), merchantName = "Example Shop", category = "Transfer",
        transactionType = TransactionType.TRANSFER, dateTime = LocalDateTime.of(2026, 1, 1, 12, 0),
        transactionHash = "synthetic-backup", reference = "000000000001",
        fromAccount = "1000", toAccount = "2000", fromBankName = "Example Bank", toBankName = "Other Bank"
    )

    @Test fun backupSanitizationPreservesReferenceAndTransferIdentity() {
        val original = transaction()
        val restored = original.sanitize()
        assertEquals(original.reference, restored.reference)
        assertEquals(original.fromBankName, restored.fromBankName)
        assertEquals(original.toBankName, restored.toBankName)
        assertEquals(original.fromAccount, restored.fromAccount)
        assertEquals(original.toAccount, restored.toAccount)
        assertEquals(original.transactionHash, restored.transactionHash)
    }

    @Test fun legacyBackupWithoutReferenceKeepsItAbsent() {
        assertNull(transaction().copy(reference = null).sanitize().reference)
    }
}
