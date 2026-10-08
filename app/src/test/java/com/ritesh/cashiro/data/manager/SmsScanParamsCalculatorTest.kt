package com.ritesh.cashiro.data.manager

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class SmsScanParamsCalculatorTest {
    private val now = Instant.parse("2026-01-10T12:00:00Z").toEpochMilli()
    private val zone = ZoneId.of("UTC")
    private fun params(last: Long = now - 1000, months: Int = 3, all: Boolean = true,
        previous: Int = -1, force: Boolean = false) = SmsScanParamsCalculator.compute(force, last, months, all, previous, now, zone)
    @Test fun firstAllTimeScanHasNoArbitraryHistoryLimit() {
        val value = params(last = 0)
        assertTrue(value.needsFullScan)
        assertEquals(0L, value.scanStartTime)
        assertEquals(-1, value.completedPeriod)
    }
    @Test fun laterAllTimeScanUsesThreeDayOverlapInsteadOfWholeInbox() {
        val value = params()
        assertFalse(value.needsFullScan)
        assertEquals(now - 3L * 24 * 60 * 60 * 1000, value.scanStartTime)
    }
    @Test fun switchingPeriodsAndIncreasingHistoryRequireFullScans() {
        assertTrue(params(previous = 3).needsFullScan)
        val month = params(all = false)
        assertTrue(month.needsFullScan)
        assertEquals(3, month.completedPeriod)
        assertTrue(params(all = false, previous = 1).needsFullScan)
        assertTrue(params(force = true).needsFullScan)
    }
    @Test fun olderCheckpointIsRetainedButClampedToCurrentMonthLimit() {
        val older = now - 5L * 24 * 60 * 60 * 1000
        assertEquals(older, params(last = older).scanStartTime)
        val value = params(last = 1, all = false, previous = 3)
        assertEquals(Instant.parse("2025-10-10T00:00:00Z").toEpochMilli(), value.scanStartTime)
        assertFalse(value.needsFullScan)
    }
}
