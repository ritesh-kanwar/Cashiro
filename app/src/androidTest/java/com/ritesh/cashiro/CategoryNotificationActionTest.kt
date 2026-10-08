package com.ritesh.cashiro

import android.app.PendingIntent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ritesh.cashiro.receiver.categoryNotificationActionIntent
import com.ritesh.cashiro.receiver.NotificationActionReceiver
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CategoryNotificationActionTest {
    @Test fun actionsStayDistinctAcrossAdjacentTransactionsSlotsAndFullLongIds() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val identities = listOf(1L to 0, 1L to 1, 1L to 2, 2L to 0, (1L + (1L shl 32)) to 0)
        val tokens = identities.map { (id, slot) ->
            val intent = categoryNotificationActionIntent(context, id, slot, "Example category $slot")
            assertEquals(id, intent.getLongExtra(NotificationActionReceiver.EXTRA_TRANSACTION_ID, -1))
            PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
        try {
            assertEquals(identities.size, tokens.toSet().size)
            val refreshed = PendingIntent.getBroadcast(
                context, 0, categoryNotificationActionIntent(context, 1L, 0, "Example changed"),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            assertEquals(tokens.first(), refreshed)
        } finally { tokens.forEach { it.cancel() } }
    }
}
