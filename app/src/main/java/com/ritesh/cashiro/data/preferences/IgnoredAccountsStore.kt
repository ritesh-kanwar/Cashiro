package com.ritesh.cashiro.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import com.ritesh.cashiro.data.database.entity.CardEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Explicit ingestion policy; never reads or changes the separate Hide preference. */
@Singleton
class IgnoredAccountsStore @Inject constructor(
    @ApplicationContext context: Context,
    private val merges: BankAccountMergeStore
) {
    private val prefs = context.getSharedPreferences("account_prefs", Context.MODE_PRIVATE)
    fun keys(): Set<String> = prefs.getStringSet(KEY, emptySet()).orEmpty().toSet()
    val keysFlow = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY || key == "bank_account_merges" || key == null) trySend(keys())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(keys())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    @Synchronized fun setIgnored(bank: String, currency: String, suffix: String, ignored: Boolean) {
        val key = keyFor(bank, currency, merges.resolveSuffix(bank, currency, suffix))
        val matching = keys().filter { stored ->
            val parts = stored.split('|')
            parts.size == 3 && parts[0] == bank && parts[1] == currency &&
                merges.resolveSuffix(bank, currency, parts[2]) == merges.resolveSuffix(bank, currency, suffix)
        }.toSet()
        prefs.edit().putStringSet(KEY, if (ignored) keys() + key else keys() - matching).apply()
    }
    @Synchronized fun restore(imported: Set<String>) {
        prefs.edit().putStringSet(KEY, keys() + imported.filter { it.split('|').size == 3 }).apply()
    }
    @Synchronized fun move(source: AccountBalanceEntity, target: AccountBalanceEntity) {
        if (isIgnored(source.bankName, source.currency, source.accountLast4)) {
            setIgnored(source.bankName, source.currency, source.accountLast4, false)
            setIgnored(target.bankName, target.currency, target.accountLast4, true)
        }
    }
    fun isIgnored(bank: String?, currency: String, vararg suffixes: String?): Boolean =
        matches(keys(), bank, currency, merges.mappings(), *suffixes)

    fun matchesKeys(keys: Set<String>, bank: String?, currency: String, vararg suffixes: String?): Boolean =
        matches(keys, bank, currency, merges.mappings(), *suffixes)

    companion object {
        private const val KEY = "ignored_accounts_v1"
        fun keyFor(bank: String, currency: String, suffix: String) = "$bank|$currency|$suffix"
        fun matches(keys: Set<String>, bank: String?, currency: String, aliases: Map<String, String>, vararg suffixes: String?): Boolean {
            if (bank.isNullOrBlank()) return false
            val candidates = suffixes.filterNotNull().filter { it.isNotBlank() }
                .map { BankAccountMergeStore.resolveSuffix(bank, currency, it, aliases) }.toSet()
            return keys.any { key ->
                val parts = key.split('|')
                parts.size == 3 && parts[0] == bank && parts[1] == currency &&
                    BankAccountMergeStore.resolveSuffix(bank, currency, parts[2], aliases) in candidates
            }
        }
        /** Do not hide an unrelated real account that happens to share a linked card's digits. */
        fun linkedCardKeys(keys: Set<String>, cards: List<CardEntity>, accounts: List<AccountBalanceEntity>, aliases: Map<String, String>): Set<String> = keys + cards.filter { card ->
            matches(keys, card.bankName, card.currency, aliases, card.accountLast4) &&
                accounts.none { it.bankName == card.bankName && it.currency == card.currency && it.accountLast4 == card.cardLast4 }
        }.map { keyFor(it.bankName, it.currency, it.cardLast4) }
    }
}
