package com.ritesh.cashiro.data.model

/**
 * Represents a currency with its code, name, and symbol
 */
data class Currency(
    val code: String,      // e.g., "USD"
    val name: String,      // e.g., "US Dollar"
    val symbol: String     // e.g., "$"
) {
    companion object {
        /**
         * List of supported currencies based on CurrencyFormatter mappings
         */
        val SUPPORTED_CURRENCIES = listOf(
            Currency("INR", "Indian Rupee", "₹"),
            Currency("BDT", "Bangladeshi Taka", "৳"),
            Currency("NGN", "Nigerian Naira", "₦"),
            Currency("LKR", "Sri Lankan Rupee", "Rs"),
            Currency("EGP", "Egyptian Pound", "E£"),
            Currency("IRR", "Iranian Rial", "﷼"),
            Currency("USD", "US Dollar", "$"),
            Currency("EUR", "Euro", "€"),
            Currency("GBP", "British Pound", "£"),
            Currency("AED", "UAE Dirham", "AED"),
            Currency("SGD", "Singapore Dollar", "S$"),
            Currency("CAD", "Canadian Dollar", "C$"),
            Currency("AUD", "Australian Dollar", "A$"),
            Currency("JPY", "Japanese Yen", "¥"),
            Currency("CNY", "Chinese Yuan", "¥"),
            Currency("NPR", "Nepalese Rupee", "₨"),
            Currency("ETB", "Ethiopian Birr", "ብር"),
            Currency("THB", "Thai Baht", "฿"),
            Currency("MYR", "Malaysian Ringgit", "RM"),
            Currency("KWD", "Kuwaiti Dinar", "KD"),
            Currency("KRW", "South Korean Won", "₩"),
            Currency("SAR", "Saudi Riyal", "﷼"),
            Currency("BYN", "Belarusian Ruble", "Br"),
            Currency("COP", "Colombian Peso", "$"),
            Currency("KES", "Kenyan Shilling", "Ksh"),
            Currency("CHF", "Swiss Franc", "Fr"),
            Currency("SEK", "Swedish Krona", "kr"),
            Currency("NZD", "New Zealand Dollar", "$"),
            Currency("MXN", "Mexican Peso", "$")
        )

        /**
         * Popular currency codes for quick access
         */
        val POPULAR_CURRENCY_CODES = listOf(
            "INR", "USD", "EUR", "GBP", "JPY", "AUD", "CAD", "CHF",
            "CNY", "SEK", "NZD", "MXN", "SGD", "AED", "KRW"
        )

        /**
         * Get currency by code
         */
        fun getByCode(code: String): Currency? {
            return SUPPORTED_CURRENCIES.find { it.code.equals(code, ignoreCase = true) }
        }
    }
}
