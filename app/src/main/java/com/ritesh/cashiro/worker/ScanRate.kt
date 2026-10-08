package com.ritesh.cashiro.worker

/** Fixed memory completion window; only the scan writer records samples. */
internal class ScanRate(private val startMillis: Long) {
    private val times = LongArray(8192)
    private var head = 0
    private var written = 0

    fun record(now: Long) {
        times[head] = now
        head = (head + 1) and (times.size - 1)
        written = (written + 1).coerceAtMost(times.size)
    }

    fun messagesPerSecond(now: Long, processed: Int): Double {
        val recent = (0 until written).count { times[it] > now - 2000L && times[it] <= now }
        if (recent >= 10) return recent / 2.0
        val elapsed = (now - startMillis) / 1000.0
        return if (elapsed > 0.1) processed / elapsed else 0.0
    }

    fun remainingMillis(now: Long, processed: Int, total: Int): Long {
        val speed = messagesPerSecond(now, processed)
        return if (speed > 0) ((total - processed).coerceAtLeast(0) / speed * 1000).toLong() else 0L
    }
}
