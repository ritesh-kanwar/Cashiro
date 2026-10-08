package com.ritesh.cashiro.data.manager

import java.time.Instant
import java.time.ZoneId

internal data class SmsScanParams(val scanStartTime: Long, val needsFullScan: Boolean, val completedPeriod: Int)

/** Scan settings are captured once and committed only after the worker succeeds. */
internal object SmsScanParamsCalculator {
    private const val ALL_TIME = -1
    private const val OVERLAP_MILLIS = 3L * 24 * 60 * 60 * 1000

    fun compute(forceResync: Boolean, lastScanTimestamp: Long, scanMonths: Int, scanAllTime: Boolean,
        lastScanPeriod: Int, now: Long, zoneId: ZoneId = ZoneId.systemDefault()): SmsScanParams {
        val period = if (scanAllTime) ALL_TIME else scanMonths
        val allTimeChanged = (lastScanPeriod == ALL_TIME) != scanAllTime
        val full = forceResync || lastScanTimestamp == 0L || allTimeChanged ||
            (lastScanPeriod >= 0 && scanMonths > lastScanPeriod)
        val limit = if (scanAllTime) 0L else Instant.ofEpochMilli(now).atZone(zoneId).toLocalDate()
            .minusMonths(scanMonths.toLong()).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val start = if (full) limit else maxOf(minOf(lastScanTimestamp, now - OVERLAP_MILLIS), limit)
        return SmsScanParams(start, full, period)
    }
}
