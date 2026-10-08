package com.ritesh.cashiro.data.backup

import com.google.gson.GsonBuilder
import com.ritesh.cashiro.data.database.entity.SubscriptionEntity
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class SubscriptionPaymentBackupTest {
    @Test fun oldBackupsWithoutLedgerDecodeAsEmpty() {
        val snapshot = GsonBuilder()
            .registerTypeAdapter(java.time.LocalDateTime::class.java, LocalDateTimeTypeAdapter())
            .registerTypeAdapter(java.time.LocalDate::class.java, LocalDateTypeAdapter())
            .registerTypeAdapter(BigDecimal::class.java, BigDecimalTypeAdapter())
            .create().fromJson("{}", DatabaseSnapshot::class.java)
        assertTrue(snapshot.subscriptionPayments.orEmpty().isEmpty())
    }
    @Test fun subscriptionMergeUsesFundingIdentityAndMandate() {
        val base = SubscriptionEntity(merchantName = "Example service", amount = BigDecimal("100"), nextPaymentDate = null,
            bankName = "Example Bank", accountLast4 = "1000", currency = "INR", billingCycle = "Monthly")
        assertTrue(SubscriptionPaymentBackup.sameSubscription(base, base.copy(id = 20, amount = BigDecimal("100.00"))))
        assertFalse(SubscriptionPaymentBackup.sameSubscription(base, base.copy(currency = "USD")))
        assertFalse(SubscriptionPaymentBackup.sameSubscription(base, base.copy(bankName = "Other Bank")))
        assertFalse(SubscriptionPaymentBackup.sameSubscription(base, base.copy(accountLast4 = "2000")))
        assertFalse(SubscriptionPaymentBackup.sameSubscription(base, base.copy(umn = "synthetic-mandate")))
        assertFalse(SubscriptionPaymentBackup.sameSubscription(base, base.copy(billingCycle = "Yearly")))
    }
}
