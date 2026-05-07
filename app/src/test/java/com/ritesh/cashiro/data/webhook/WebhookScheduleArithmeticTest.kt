package com.ritesh.cashiro.data.webhook

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pins the day-roll logic that decides when an "every day at HH:mm" slot should next fire.
 * The pure side of the scheduler — the AlarmManager scheduling itself is Android-side and tested
 * through instrumentation/QA, but if this arithmetic is wrong we either over-fire (slot still in
 * the past) or skip a day (slot pushed too far forward).
 */
class WebhookScheduleArithmeticTest {

    @Test
    fun `slot later today returns today`() {
        val now = LocalDateTime.of(2026, 5, 6, 9, 0)
        val time = WebhookScheduledTime(hour = 21, minute = 0)

        val next = WebhookScheduleArithmetic.nextRunFor(time, now)

        assertEquals(LocalDateTime.of(2026, 5, 6, 21, 0), next)
    }

    @Test
    fun `slot already passed today rolls to tomorrow`() {
        val now = LocalDateTime.of(2026, 5, 6, 14, 0)
        val time = WebhookScheduledTime(hour = 8, minute = 0)

        val next = WebhookScheduleArithmetic.nextRunFor(time, now)

        assertEquals(LocalDateTime.of(2026, 5, 7, 8, 0), next)
    }

    @Test
    fun `slot at the same minute as now is treated as already passed`() {
        // Boundary: if we re-armed at exactly the slot's minute, firing again "in zero seconds"
        // would be a duplicate. Push to tomorrow.
        val now = LocalDateTime.of(2026, 5, 6, 8, 0, 30)
        val time = WebhookScheduledTime(hour = 8, minute = 0)

        val next = WebhookScheduleArithmetic.nextRunFor(time, now)

        assertEquals(LocalDateTime.of(2026, 5, 7, 8, 0), next)
    }

    @Test
    fun `slot one minute after now keeps today's date`() {
        val now = LocalDateTime.of(2026, 5, 6, 7, 59, 59)
        val time = WebhookScheduledTime(hour = 8, minute = 0)

        val next = WebhookScheduleArithmetic.nextRunFor(time, now)

        assertEquals(LocalDateTime.of(2026, 5, 6, 8, 0), next)
    }

    @Test
    fun `slot at midnight rolls to next day when called late`() {
        val now = LocalDateTime.of(2026, 5, 6, 23, 30)
        val time = WebhookScheduledTime(hour = 0, minute = 0)

        val next = WebhookScheduleArithmetic.nextRunFor(time, now)

        assertEquals(LocalDateTime.of(2026, 5, 7, 0, 0), next)
    }

    @Test
    fun `slot at midnight rolls to tomorrow when re-armed at 23-59`() {
        // Edge: if we somehow re-armed at 23:59 of day N for a 00:00 slot of day N+1
        // (e.g. user toggled the time while screen was open at 23:59), today's 00:00 is in the
        // past, so we expect tomorrow. This pins it.
        val now = LocalDateTime.of(2026, 5, 6, 23, 59)
        val time = WebhookScheduledTime(hour = 0, minute = 0)

        val next = WebhookScheduleArithmetic.nextRunFor(time, now)

        assertEquals(LocalDateTime.of(2026, 5, 7, 0, 0), next)
    }
}
