package com.ritesh.cashiro

import android.content.Intent
import android.text.SpannableString
import org.junit.Assert.*
import org.junit.Test

class SharedTextIntentTest {
    @Test fun acceptsOnlyBoundedPlainTextShares() {
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, SpannableString("  450 lunch  "))
        assertEquals("450 lunch", SharedTextIntent.read(send))
        assertNull(SharedTextIntent.read(Intent(Intent.ACTION_VIEW).setType("text/plain").putExtra(Intent.EXTRA_TEXT, "450 lunch")))
        assertNull(SharedTextIntent.read(Intent(Intent.ACTION_SEND).setType("application/json").putExtra(Intent.EXTRA_TEXT, "450 lunch")))
        assertNull(SharedTextIntent.read(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, "   ")))
        assertEquals(1000, SharedTextIntent.read(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, "x".repeat(2000)))?.length)
    }
}
