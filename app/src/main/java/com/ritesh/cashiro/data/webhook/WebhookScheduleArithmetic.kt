package com.ritesh.cashiro.data.webhook

import java.time.LocalDateTime

/**
 * Pure helpers used by [WebhookSyncScheduler] to compute alarm trigger times. Lives outside the
 * scheduler so it can be unit-tested without an AlarmManager. Always pass `now` explicitly so
 * tests are deterministic.
 */
internal object WebhookScheduleArithmetic {

    /**
     * Returns the next [LocalDateTime] at which [time] should fire, given the current instant.
     *
     * If today's slot is in the future, returns today at HH:mm. If it has already passed (or is
     * exactly the current minute, which would otherwise be re-fired immediately), rolls to the
     * same time tomorrow.
     */
    fun nextRunFor(time: WebhookScheduledTime, now: LocalDateTime): LocalDateTime {
        var next = now.withHour(time.hour).withMinute(time.minute).withSecond(0).withNano(0)
        if (!next.isAfter(now)) {
            next = next.plusDays(1)
        }
        return next
    }
}
