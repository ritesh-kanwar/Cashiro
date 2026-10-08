package com.ritesh.cashiro.data.preferences

import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import com.ritesh.cashiro.data.database.entity.CardEntity
import com.ritesh.cashiro.data.database.entity.CardType
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime

class IgnoredAccountsStoreTest {
    private val bank = "Example Bank"
    private fun key(suffix: String) = IgnoredAccountsStore.keyFor(bank, "INR", suffix)
    private fun matches(keys: Set<String>, suffix: String?, currency: String = "INR", name: String? = bank,
        aliases: Map<String, String> = emptyMap()) = IgnoredAccountsStore.matches(keys, name, currency, aliases, suffix)

    @Test fun ignoresOnlyExplicitBankCurrencyIdentity() {
        val keys = setOf(key("1000"))
        assertTrue(matches(keys, "1000"))
        assertFalse(matches(keys, "1000", currency = "USD"))
        assertFalse(matches(keys, "1000", name = "Other Bank"))
        assertFalse(matches(keys, null))
        assertFalse(matches(keys, "1000", name = null))
        assertFalse(matches(setOf("Example Bank_1000"), "1000")) // Existing Hide keys are not Ignore keys.
    }
    @Test fun confirmedAliasesMatchBothDirectionsWithoutGuessing() {
        val aliases = mapOf(BankAccountMergeStore.mappingKey(bank, "INR", "000") to "1000")
        assertFalse(matches(setOf(key("1000")), "000"))
        assertTrue(matches(setOf(key("1000")), "000", aliases = aliases))
        assertTrue(matches(setOf(key("000")), "1000", aliases = aliases))
        assertFalse(matches(setOf(key("1000")), "000", currency = "USD", aliases = aliases))
    }
    @Test fun walletsRequireTheirExplicitCurrencyIdentity() {
        assertTrue(matches(setOf(key("wallet_INR")), "wallet_INR"))
        assertFalse(matches(setOf(key("wallet_INR")), null))
        assertFalse(matches(setOf(key("wallet_INR")), "wallet_USD"))
    }
    @Test fun linkedCardExpansionPreservesAnUnrelatedAccountWithSameDigits() {
        val ignored = setOf(key("1000"))
        val card = CardEntity(cardLast4 = "2000", bankName = bank, currency = "INR", accountLast4 = "1000", cardType = CardType.DEBIT)
        assertTrue(matches(IgnoredAccountsStore.linkedCardKeys(ignored, listOf(card), emptyList(), emptyMap()), "2000"))
        val collision = AccountBalanceEntity(bankName = bank, accountLast4 = "2000", currency = "INR", balance = BigDecimal.ZERO,
            timestamp = LocalDateTime.of(2026, 1, 1, 0, 0))
        assertFalse(matches(IgnoredAccountsStore.linkedCardKeys(ignored, listOf(card), listOf(collision), emptyMap()), "2000"))
        assertFalse(matches(IgnoredAccountsStore.linkedCardKeys(ignored, listOf(card.copy(currency = "USD")), emptyList(), emptyMap()), "2000"))
    }
    @Test fun removingPolicyRestoresListMatches() {
        assertTrue(matches(setOf(key("1000")), "1000"))
        assertFalse(matches(emptySet(), "1000"))
    }
}
