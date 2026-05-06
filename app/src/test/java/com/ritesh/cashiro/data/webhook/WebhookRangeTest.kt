package com.ritesh.cashiro.data.webhook

import com.ritesh.cashiro.data.database.entity.WebhookRangePreset
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The persistence shape stores the range as three flat columns (preset, customStart, customEnd) but
 * the domain type is a sealed class so it's impossible for callers to construct illegal combos like
 * "TODAY with a customStart". These tests pin the bidirectional mapping at the boundary.
 */
class WebhookRangeTest {

    @Test
    fun `Custom range round trips through the flat representation`() {
        val start = LocalDateTime.of(2026, 5, 1, 0, 0)
        val end = LocalDateTime.of(2026, 5, 31, 23, 59)
        val original = WebhookRange.Custom(start = start, end = end)

        val flat = original.toFlat()
        val restored = WebhookRange.fromFlat(flat.preset, flat.customStart, flat.customEnd)

        assertEquals(WebhookRangePreset.CUSTOM, flat.preset)
        assertEquals(start, flat.customStart)
        assertEquals(end, flat.customEnd)
        assertEquals(original, restored)
    }

    @Test
    fun `non-custom presets flatten with null start and end`() {
        val flat = WebhookRange.SinceLastSuccess.toFlat()
        assertEquals(WebhookRangePreset.SINCE_LAST_SUCCESS, flat.preset)
        assertNull(flat.customStart)
        assertNull(flat.customEnd)
    }

    @Test
    fun `fromFlat ignores stray custom dates on non-custom presets`() {
        val stray = LocalDateTime.of(2026, 5, 1, 0, 0)
        // Defensive: if a previous bug left junk in the custom_start column on a TODAY row,
        // fromFlat must still produce the canonical Today, not Custom.
        val recovered = WebhookRange.fromFlat(WebhookRangePreset.TODAY, stray, stray)
        assertEquals(WebhookRange.Today, recovered)
    }

    @Test
    fun `fromFlat with CUSTOM preset but missing dates falls back to SinceLastSuccess`() {
        // The schema can't enforce CUSTOM => bounds-present (column-level); this is the runtime guard.
        val recovered = WebhookRange.fromFlat(WebhookRangePreset.CUSTOM, null, null)
        assertEquals(WebhookRange.SinceLastSuccess, recovered)
    }

    @Test
    fun `every WebhookRangePreset is reachable via fromFlat with appropriate inputs`() {
        // Catches the case where a new preset is added but the converter forgets to handle it.
        WebhookRangePreset.entries.forEach { preset ->
            val start = if (preset == WebhookRangePreset.CUSTOM) LocalDateTime.now() else null
            val end = if (preset == WebhookRangePreset.CUSTOM) LocalDateTime.now() else null
            val range = WebhookRange.fromFlat(preset, start, end)
            // Each branch should produce a non-null sealed instance; the assertion is that we don't
            // throw or land in some default branch unexpectedly when a new preset is introduced.
            assertEquals(preset, range.toFlat().preset)
        }
    }
}
