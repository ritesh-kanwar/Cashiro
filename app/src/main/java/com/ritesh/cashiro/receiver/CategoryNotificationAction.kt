package com.ritesh.cashiro.receiver

import android.content.Context
import android.content.Intent
import android.net.Uri

/** Intent data participates in PendingIntent identity; extras and truncated IDs do not. */
internal fun categoryNotificationActionIntent(
    context: Context,
    transactionId: Long,
    actionIndex: Int,
    category: String
): Intent = Intent(context, NotificationActionReceiver::class.java).apply {
    action = NotificationActionReceiver.ACTION_CHANGE_CATEGORY
    data = Uri.parse("cashiro://notification/category/$transactionId/$actionIndex")
    putExtra(NotificationActionReceiver.EXTRA_TRANSACTION_ID, transactionId)
    putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, transactionId.toInt())
    putExtra(NotificationActionReceiver.EXTRA_NEW_CATEGORY, category)
}
