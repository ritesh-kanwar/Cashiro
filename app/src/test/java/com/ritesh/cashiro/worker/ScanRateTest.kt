package com.ritesh.cashiro.worker

import org.junit.Assert.*
import org.junit.Test

class ScanRateTest {
    @Test fun recentCompletionsReflectChangedSpeedInsteadOfLifetimeAverage() {
        val rate = ScanRate(0)
        repeat(100) { rate.record(1000L) }
        repeat(20) { rate.record(9000L) }
        assertEquals(10.0, rate.messagesPerSecond(10000, 120), 0.001)
        assertEquals(8000L, rate.remainingMillis(10000, 120, 200))
    }
    @Test fun warmupUsesElapsedSpeedAndCompletionClampsRemainingTime() {
        val rate = ScanRate(1000)
        rate.record(1500)
        assertEquals(2.0, rate.messagesPerSecond(1500, 1), 0.001)
        assertEquals(0L, rate.remainingMillis(1500, 2, 1))
        assertEquals(0L, ScanRate(1000).remainingMillis(1000, 0, 10))
    }
}
