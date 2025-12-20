package com.ritesh.cashiro.domain.usecase

import com.ritesh.cashiro.data.database.entity.SubscriptionEntity
import com.ritesh.cashiro.data.database.entity.SubscriptionState
import com.ritesh.cashiro.data.database.entity.TransactionEntity
import com.ritesh.cashiro.data.database.entity.TransactionType
import com.ritesh.cashiro.data.repository.SubscriptionRepository
import com.ritesh.cashiro.data.repository.TransactionRepository
import java.math.BigDecimal
import java.security.MessageDigest
import java.time.LocalDateTime
import javax.inject.Inject

class AddTransactionUseCase
@Inject
constructor(
        private val transactionRepository: TransactionRepository,
        private val subscriptionRepository: SubscriptionRepository
) {
    suspend fun execute(
            amount: BigDecimal,
            merchant: String,
            category: String,
            type: TransactionType,
            date: LocalDateTime,
            notes: String? = null,
            subcategory: String? = null,
            isRecurring: Boolean = false
    ) {
        // Generate a unique hash for manual transactions
        val transactionHash =
                generateManualTransactionHash(amount = amount, merchant = merchant, date = date)

        // Create the transaction entity
        val transaction =
                TransactionEntity(
                        amount = amount,
                        merchantName = merchant,
                        category = category,
                        subcategory = subcategory,
                        transactionType = type,
                        dateTime = date,
                        description = notes,
                        smsBody = null, // null indicates manual entry
                        bankName = "Manual Entry",
                        smsSender = null, // null indicates manual entry
                        accountNumber = null,
                        balanceAfter = null,
                        transactionHash = transactionHash,
                        isRecurring = isRecurring,
                        createdAt = LocalDateTime.now(),
                        updatedAt = LocalDateTime.now()
                )

        // Insert the transaction
        val transactionId = transactionRepository.insertTransaction(transaction)

        // If marked as recurring, create a subscription
        if (isRecurring && transactionId != -1L) {
            val nextPaymentDate = date.toLocalDate().plusMonths(1) // Default to monthly

            val subscription =
                    SubscriptionEntity(
                            merchantName = merchant,
                            amount = amount,
                            nextPaymentDate = nextPaymentDate,
                            state = SubscriptionState.ACTIVE,
                            bankName = "Manual Entry",
                            category = category,
                            subcategory = subcategory,
                            createdAt = LocalDateTime.now(),
                            updatedAt = LocalDateTime.now()
                    )

            subscriptionRepository.insertSubscription(subscription)
        }
    }

    private fun generateManualTransactionHash(
            amount: BigDecimal,
            merchant: String,
            date: LocalDateTime
    ): String {
        // Create a unique hash for manual transactions
        // Format: MANUAL_<amount>_<merchant>_<datetime>
        val data = "MANUAL_${amount}_${merchant}_${date}"

        return MessageDigest.getInstance("MD5").digest(data.toByteArray()).joinToString("") {
            "%02x".format(it)
        }
    }
}
