package com.ritesh.cashiro

import android.content.Intent

/** Shared text is a bounded local draft; it never saves a transaction by itself. */
internal object SharedTextIntent {
    const val MAX_LENGTH = 1000
    fun read(intent: Intent?): String? {
        if (intent?.action != Intent.ACTION_SEND || intent.type != "text/plain") return null
        return runCatching { intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()?.trim()?.take(MAX_LENGTH)?.ifEmpty { null } }.getOrNull()
    }
}
