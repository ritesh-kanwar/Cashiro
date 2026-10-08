package com.ritesh.cashiro.data.manager

import com.ritesh.cashiro.data.database.entity.TransactionEntity
import java.time.Duration

sealed class DedupResult {
    data object NotDuplicate : DedupResult()
    data object PreviouslyDeleted : DedupResult()
    data object HashDuplicate : DedupResult()
    data object UpiDuplicate : DedupResult()
    data class Replaced(val replacementId: Long) : DedupResult()
}

object TransactionDeduplication {
    fun checkHash(existing: TransactionEntity): DedupResult =
        if (existing.isDeleted) DedupResult.PreviouslyDeleted else DedupResult.HashDuplicate

    private val upiReferencePattern = Regex("""\d{12}""")
    val UPI_DUPLICATE_WINDOW: Duration = Duration.ofHours(24)

    fun hasUpiReference(transaction: TransactionEntity): Boolean =
        transaction.reference?.let { upiReferencePattern.matches(it) } == true

    fun isSameUpiTransaction(
        existing: TransactionEntity,
        incoming: TransactionEntity,
        window: Duration = UPI_DUPLICATE_WINDOW
    ): Boolean {
        if (!hasUpiReference(existing) || !hasUpiReference(incoming)) return false
        if (existing.reference != incoming.reference) return false
        if (existing.transactionType != incoming.transactionType) return false
        if (existing.currency != incoming.currency) return false
        if (existing.amount.compareTo(incoming.amount) != 0) return false
        if (!accountsMatch(existing.accountNumber, incoming.accountNumber)) return false

        // Allow up to 24 hours (UPI_DUPLICATE_WINDOW) for unique UPI references to handle carrier delays
        val gap = Duration.between(existing.dateTime, incoming.dateTime).abs()
        return gap <= window
    }

    // Notification-vs-SMS cross-channel check: the same charge can carry a
    // different sender code and body text per channel, so content hashes
    // diverge. Callers pre-filter candidates by amount and time window; bank
    // and merchant identity decide. Merchant names are mapper-normalized on
    // both sides, so raw vs title-case renders still compare equal.
    fun isSameCharge(
        existing: TransactionEntity,
        incoming: TransactionEntity
    ): Boolean {
        if (existing.bankName != incoming.bankName) return false
        if (existing.amount.compareTo(incoming.amount) != 0) return false
        if (existing.transactionType != incoming.transactionType) return false
        // Equal numbers in different currencies are different charges (a
        // multi-currency provider can post £0.24 and €0.24 together).
        if (existing.currency != incoming.currency) return false
        return existing.merchantName.equals(incoming.merchantName, ignoreCase = true)
    }

    /**
     * Cross-channel check: when a bank both texts and pushes an app notification
     * for the same charge, whichever is saved second must not book it again. The
     * two carry different senders (shortcode vs app alias), so their hashes never
     * match. Callers run this inside the same lock as the insert, so an SMS and a
     * notification arriving together can't both pass before either is saved.
     *
     * Only a row from the *other* channel counts — two identical SMS charges a
     * minute apart are two real purchases — and the accounts must agree, so an
     * equal withdrawal from a different account is kept.
     */
    fun isBookedByOtherChannel(
        incoming: TransactionEntity,
        nearby: List<TransactionEntity>,
        notificationAliases: Set<String>
    ): Boolean {
        val incomingIsNotification = incoming.smsSender in notificationAliases
        return nearby.any {
            (it.smsSender in notificationAliases) != incomingIsNotification &&
                isSameCharge(it, incoming) &&
                accountsMatch(it.accountNumber, incoming.accountNumber)
        }
    }

    fun shouldReplaceWithIncoming(
        existing: TransactionEntity,
        incoming: TransactionEntity
    ): Boolean {
        if (!isSameUpiTransaction(existing, incoming)) return false

        val existingIsPartnerBank = existing.bankName.equals("State Bank of India", ignoreCase = true)
        val incomingIsPartnerBank = incoming.bankName.equals("State Bank of India", ignoreCase = true)
        if (existingIsPartnerBank && !incomingIsPartnerBank) return true
        if (!existingIsPartnerBank && incomingIsPartnerBank) return false

        return existing.balanceAfter == null && incoming.balanceAfter != null
    }

    data class DuplicateCluster(val keeper: TransactionEntity, val duplicates: List<TransactionEntity>)

    fun duplicateIdsToDelete(transactions: List<TransactionEntity>): List<Long> =
        duplicateClusters(transactions).flatMap { it.duplicates.map { row -> row.id } }

    fun duplicateClusters(transactions: List<TransactionEntity>): List<DuplicateCluster> = transactions
        .filter { !it.isDeleted && hasUpiReference(it) }
        .groupBy { DuplicateKey(it.reference.orEmpty(), it.amount.stripTrailingZeros().toPlainString(),
            it.transactionType.name, it.currency) }
        .values.flatMap { group ->
            val clusters = mutableListOf<MutableList<TransactionEntity>>()
            group.sortedWith(compareBy<TransactionEntity> { it.dateTime }.thenBy { it.id }).forEach { row ->
                val cluster = clusters.firstOrNull { members ->
                    isSameUpiTransaction(members.first(), row) &&
                        members.all { accountsMatch(it.accountNumber, row.accountNumber) }
                }
                if (cluster == null) clusters.add(mutableListOf(row)) else cluster.add(row)
            }
            clusters.map { members ->
                val keeper = members.minWith(transactionQualityComparator)
                DuplicateCluster(keeper, members.filter { it.id != keeper.id })
            }.filter { it.duplicates.isNotEmpty() }
        }

    /** Preserve conflicting category choices separately; combine all notes and attachment paths. */
    fun mergeUserMetadata(keeper: TransactionEntity, duplicate: TransactionEntity): TransactionEntity? {
        fun custom(value: String?) = !value.isNullOrBlank() && value.lowercase() !in
            setOf("miscellaneous", "others", "other", "unknown")
        if (custom(keeper.category) && custom(duplicate.category) && keeper.category != duplicate.category) return null
        if (!keeper.subcategory.isNullOrBlank() && !duplicate.subcategory.isNullOrBlank() && keeper.subcategory != duplicate.subcategory) return null
        val notes = listOfNotNull(keeper.description?.takeIf { it.isNotBlank() }, duplicate.description?.takeIf { it.isNotBlank() }).distinct()
        val attachments = (keeper.attachments.split(',') + duplicate.attachments.split(',')).filter { it.isNotBlank() }.distinct()
        return keeper.copy(
            category = keeper.category.takeIf { custom(it) } ?: duplicate.category.takeIf { custom(it) } ?: keeper.category,
            subcategory = keeper.subcategory?.takeIf { it.isNotBlank() } ?: duplicate.subcategory,
            description = notes.takeIf { it.isNotEmpty() }?.joinToString("\n"),
            attachments = attachments.joinToString(","), isRecurring = keeper.isRecurring || duplicate.isRecurring
        )
    }

    private val transactionQualityComparator = compareBy<TransactionEntity>(
        { it.bankName.equals("State Bank of India", ignoreCase = true) },
        { it.balanceAfter == null },
        { it.dateTime },
        { it.id }
    )

    private fun accountsMatch(existingAccount: String?, incomingAccount: String?): Boolean {
        return existingAccount.isNullOrBlank() ||
                incomingAccount.isNullOrBlank() ||
                existingAccount == incomingAccount
    }

    private data class DuplicateKey(
        val reference: String,
        val amount: String,
        val transactionType: String,
        val currency: String
    )
}
