package com.ritesh.cashiro.receiver

object BankNotificationConfig {
    private val packageToAlias = mapOf(
        "com.chase.intl" to "ChaseUK",
        "com.avuscapital.trading212" to "Trading212",
        "com.avanza.ambitwizfbl" to "FaysalBank",
        "finansbank.enpara" to "Enpara",
        "com.enparabank.retail" to "Enpara"
    )

    val notificationAliases: Set<String> = packageToAlias.values.toSet()

    private val allowedPackages: Set<String> = packageToAlias.keys

    fun isAllowed(packageName: String): Boolean = allowedPackages.contains(packageName)

    fun senderAlias(packageName: String): String? = packageToAlias[packageName]

    fun extractMessage(notification: android.app.Notification): String? {
        val extras = notification.extras ?: return null
        val textKey = extras.getString("android.text")
            ?: extras.getString("android.title")
            ?: extras.getString("android.subText")
            ?: extras.getString("android.infoText")
            ?: extras.getString("android.bigText")
            ?: extras.getCharSequence("android.text")?.toString()
            ?: extras.getCharSequence("android.title")?.toString()
            ?: extras.getCharSequence("android.bigText")?.toString()
        return textKey
    }
}
