package com.ritesh.cashiro.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import androidx.core.app.NotificationCompat
import com.ritesh.cashiro.MainActivity
import com.ritesh.cashiro.CashiroApplication
import com.ritesh.cashiro.R
import com.ritesh.cashiro.data.manager.SmsTransactionProcessor
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver that intercepts incoming SMS messages in real-time
 * and processes them for transaction data using the shared SmsTransactionProcessor.
 */
class SmsBroadcastReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SmsBroadcastReceiverEntryPoint {
        fun smsTransactionProcessor(): SmsTransactionProcessor
        fun detectBalanceDiscrepancy(): com.ritesh.cashiro.domain.usecase.DetectBalanceDiscrepancyUseCase
        fun transactionRepository(): com.ritesh.cashiro.data.repository.TransactionRepository
    }

    companion object {
        private const val TAG = "SmsBroadcastReceiver"
        const val ACTION_EDIT_TRANSACTION = "com.pennywiseai.tracker.ACTION_EDIT_TRANSACTION"
        const val EXTRA_TRANSACTION_ID = "transaction_id"
        const val CHANNEL_ID = "transaction_notifications"
        const val CHANNEL_NAME = "Transaction Notifications"
    }

    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            return
        }

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) {
            return
        }

        // Combine multi-part SMS messages with their timestamps
        data class SmsData(val body: StringBuilder, var timestamp: Long)
        val smsMap = mutableMapOf<String, SmsData>()
        for (message in messages) {
            val sender = message.originatingAddress ?: continue
            val body = message.messageBody ?: continue
            val timestamp = message.timestampMillis

            val existing = smsMap.getOrPut(sender) { SmsData(StringBuilder(), timestamp) }
            existing.body.append(body)
            // Use the earliest timestamp for multi-part messages
            if (timestamp < existing.timestamp) {
                existing.timestamp = timestamp
            }
        }

        // Get the processor via Hilt EntryPoint
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            SmsBroadcastReceiverEntryPoint::class.java
        )
        val processor = entryPoint.smsTransactionProcessor()

        // Process each unique SMS
        for ((sender, smsData) in smsMap) {
            val body = smsData.body.toString()
            val timestamp = smsData.timestamp
            Log.d(TAG, "Received SMS from: $sender at timestamp: $timestamp")

            processIncomingSms(context, processor, sender, body, timestamp)
        }
    }

    private fun processIncomingSms(
        context: Context,
        processor: SmsTransactionProcessor,
        sender: String,
        body: String,
        timestamp: Long
    ) {
        receiverScope.launch {
            try {
                // Use the shared processor to parse and save the transaction
                val result = processor.processAndSaveTransaction(sender, body, timestamp)

                if (result.success && result.transactionId != null) {
                    Log.d(TAG, "Transaction saved with ID: ${result.transactionId}")

                    // Show notification if app is not in foreground
                    if (!isAppInForeground(context)) {
                        // Get transaction details for notification
                        val parsedTransaction = com.ritesh.parser.core.bank.BankParserFactory.getParsers(sender)
                            .firstNotNullOfOrNull { it.parse(body, sender, timestamp) }

                        if (parsedTransaction != null) {
                            // Get entry point to access repository
                            val entryPoint = EntryPointAccessors.fromApplication(
                                context.applicationContext,
                                SmsBroadcastReceiverEntryPoint::class.java
                            )
                            val repository = entryPoint.transactionRepository()

                            // Fetch the saved transaction to get its category
                            val savedTransaction = repository.getTransactionById(result.transactionId)

                            val balanceMismatch = try {
                                savedTransaction?.let { entryPoint.detectBalanceDiscrepancy().execute(it) } != null
                            } catch (e: kotlinx.coroutines.CancellationException) {
                                throw e
                            } catch (_: Exception) {
                                false
                            }
                            showTransactionNotification(
                                context = context,
                                transactionId = result.transactionId,
                                amount = parsedTransaction.amount.toString(),
                                merchant = parsedTransaction.merchant ?: "Unknown",
                                type = parsedTransaction.type.name,
                                bankName = parsedTransaction.bankName ?: "Bank",
                                category = savedTransaction?.category ?: "Miscellaneous",
                                balanceMismatch = balanceMismatch,
                                repository = repository
                            )
                        }
                    }
                } else {
                    Log.d(TAG, "Transaction not saved: ${result.reason}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing SMS", e)
            }
        }
    }

    private fun isAppInForeground(context: Context): Boolean {
        return try {
            val application = context.applicationContext as? CashiroApplication
            application?.isAppInForeground ?: false
        } catch (e: Exception) {
            false
        }
    }

    private fun showTransactionNotification(
        context: Context,
        transactionId: Long,
        amount: String,
        merchant: String,
        type: String,
        bankName: String,
        category: String,
        balanceMismatch: Boolean,
        repository: com.ritesh.cashiro.data.repository.TransactionRepository
    ) {
        receiverScope.launch {
            try {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                // Create notification channel
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notifications for new transactions"
                }
                notificationManager.createNotificationChannel(channel)

                // Create intent to open transaction detail
                val intent = Intent(context, MainActivity::class.java).apply {
                    action = ACTION_EDIT_TRANSACTION
                    putExtra(EXTRA_TRANSACTION_ID, transactionId)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }

                val pendingIntent = PendingIntent.getActivity(
                    context,
                    transactionId.toInt(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // Format notification content
                val typeEmoji = when (type) {
                    "EXPENSE" -> "💸"
                    "INCOME" -> "💰"
                    "CREDIT" -> "💳"
                    "TRANSFER" -> "🔄"
                    "INVESTMENT" -> "📈"
                    else -> "💵"
                }

                val title = "$typeEmoji $amount - $merchant"
                val content = if (balanceMismatch) context.getString(R.string.balance_discrepancy_notification)
                    else "$category • $bankName"

                // Get top 3 categories by usage (personalized for user)
                val topCategories = try {
                    repository.getTopCategoriesByUsage(3)
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching top categories", e)
                    // Fallback to common categories
                    listOf("Food & Drinks", "Shopping", "Transport")
                }

                // Build notification with category quick actions
                val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.cashiro)
                    .setContentTitle(title)
                    .setContentText(content)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)

                if (balanceMismatch) {
                    notificationBuilder.setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                        .setPublicVersion(NotificationCompat.Builder(context, CHANNEL_ID)
                            .setSmallIcon(R.drawable.cashiro)
                            .setContentTitle(context.getString(R.string.app_name))
                            .setContentText(context.getString(R.string.balance_discrepancy_notification))
                            .setContentIntent(pendingIntent)
                            .setAutoCancel(true)
                            .build())
                }

                // Add quick action buttons for top categories (only if different from current)
                val notificationId = transactionId.toInt()
                topCategories.filter { it != category }.take(3).forEachIndexed { index, topCategory ->
                    val categoryIntent = categoryNotificationActionIntent(
                        context, transactionId, index, topCategory
                    )

                    val categoryPendingIntent = PendingIntent.getBroadcast(
                        context,
                        0, // The data URI identifies the full transaction ID and action slot.
                        categoryIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    notificationBuilder.addAction(
                        0, // No icon for actions
                        topCategory,
                        categoryPendingIntent
                    )
                }

                val notification = notificationBuilder.build()
                notificationManager.notify(notificationId, notification)
            } catch (e: Exception) {
                Log.e(TAG, "Error showing notification", e)
            }
        }
    }
}
