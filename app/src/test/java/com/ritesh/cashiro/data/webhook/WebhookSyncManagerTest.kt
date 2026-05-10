package com.ritesh.cashiro.data.webhook

import com.ritesh.cashiro.data.database.entity.WebhookDataType
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WebhookSyncManagerTest {

    private fun cursorUpdates(rangeEnd: LocalDateTime): List<WebhookCursorUpdate> = listOf(
        WebhookCursorUpdate(
            dataType = WebhookDataType.TRANSACTIONS,
            successAt = rangeEnd,
            rangeEnd = rangeEnd
        )
    )

    private fun success(): WebhookAttemptResult =
        WebhookAttemptResult(success = true, httpStatus = 200, message = "Delivered (HTTP 200)")

    private fun retryableFailure(): WebhookAttemptResult =
        WebhookAttemptResult(success = false, httpStatus = 503, message = "HTTP 503", retryable = true)

    private fun nonRetryableFailure(): WebhookAttemptResult =
        WebhookAttemptResult(success = false, httpStatus = 400, message = "HTTP 400", retryable = false)

    @Test
    fun `all batches succeed - cursor advances to last batch's cursorUpdates`() {
        val rangeEnd = LocalDateTime.of(2026, 1, 1, 23, 59, 59)
        val updates = cursorUpdates(rangeEnd)
        val outcomes = listOf(
            BatchOutcome(updates, success()),
            BatchOutcome(updates, success())
        )

        assertEquals(updates, resolveCursorAdvance(outcomes, sendTestPayload = false))
    }

    @Test
    fun `batch 1 succeeds and batch 2 retryably fails - cursor must not advance`() {
        // If we advanced past batch 1, batch 2's txns would be excluded from the retry's range.
        val updates = cursorUpdates(LocalDateTime.of(2026, 1, 1, 23, 59, 59))
        val outcomes = listOf(
            BatchOutcome(updates, success()),
            BatchOutcome(updates, retryableFailure())
        )

        assertNull(resolveCursorAdvance(outcomes, sendTestPayload = false))
    }

    @Test
    fun `batch 1 succeeds and batch 2 fails non-retryably - cursor must not advance`() {
        val updates = cursorUpdates(LocalDateTime.of(2026, 1, 1, 23, 59, 59))
        val outcomes = listOf(
            BatchOutcome(updates, success()),
            BatchOutcome(updates, nonRetryableFailure())
        )

        assertNull(resolveCursorAdvance(outcomes, sendTestPayload = false))
    }

    @Test
    fun `single batch failure - cursor does not advance`() {
        val outcomes = listOf(BatchOutcome(cursorUpdates(LocalDateTime.now()), retryableFailure()))

        assertNull(resolveCursorAdvance(outcomes, sendTestPayload = false))
    }

    @Test
    fun `single batch success - cursor advances`() {
        val rangeEnd = LocalDateTime.of(2026, 1, 1, 23, 59, 59)
        val outcomes = listOf(BatchOutcome(cursorUpdates(rangeEnd), success()))

        val advance = resolveCursorAdvance(outcomes, sendTestPayload = false)

        assertEquals(1, advance?.size)
        assertEquals(rangeEnd, advance!!.first().successAt)
    }

    @Test
    fun `sendTestPayload never advances cursor even when delivery succeeds`() {
        val outcomes = listOf(BatchOutcome(cursorUpdates(LocalDateTime.now()), success()))

        assertNull(resolveCursorAdvance(outcomes, sendTestPayload = true))
    }

    @Test
    fun `empty outcomes list does not advance cursor`() {
        assertNull(resolveCursorAdvance(emptyList(), sendTestPayload = false))
    }
}
