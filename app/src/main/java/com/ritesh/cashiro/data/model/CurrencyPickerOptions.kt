package com.ritesh.cashiro.data.model

import java.util.Locale

/** The supported catalog stays selectable even before transactions or network data exist. */
object CurrencyPickerOptions {
    fun catalog(
        remote: List<Currency> = emptyList(),
        custom: List<Currency> = emptyList(),
        selectedCode: String? = null
    ): List<Currency> {
        val currencies = (remote + Currency.SUPPORTED_CURRENCIES + custom)
            .filter { it.code.isNotBlank() }
            .associateByTo(linkedMapOf()) { it.code.trim().uppercase(Locale.ROOT) }
        selectedCode?.trim()?.uppercase(Locale.ROOT)?.takeIf { it.isNotEmpty() }?.let { code ->
            currencies.putIfAbsent(code, Currency(code, code, code))
        }
        return currencies.map { (code, currency) -> currency.copy(code = code) }
            .sortedWith(compareBy<Currency> { it.name.lowercase(Locale.ROOT) }.thenBy { it.code })
    }

    fun matching(currencies: List<Currency>, query: String): List<Currency> {
        val search = query.trim()
        return currencies.filter {
            it.code.contains(search, ignoreCase = true) ||
                it.name.contains(search, ignoreCase = true) ||
                it.symbol.contains(search, ignoreCase = true)
        }
    }

    fun quickAccess(currencies: List<Currency>, selectedCode: String): List<Currency> =
        currencies.filter {
            it.code.equals(selectedCode.trim(), ignoreCase = true) || it.code in Currency.POPULAR_CURRENCY_CODES
        }.sortedBy { !it.code.equals(selectedCode.trim(), ignoreCase = true) }

    /** A transaction filter keeps its own scope, while still handling an empty initial list. */
    fun available(codes: List<String>, selectedCode: String): List<Currency> {
        val normalized = (codes + selectedCode).map { it.trim().uppercase(Locale.ROOT) }.filter { it.isNotEmpty() }.distinct()
        if (normalized.isEmpty()) return catalog()
        return normalized.map { code -> Currency.getByCode(code) ?: Currency(code, code, code) }
            .sortedWith(compareBy<Currency> { !it.code.equals(selectedCode.trim(), ignoreCase = true) }.thenBy { it.name })
    }
}
