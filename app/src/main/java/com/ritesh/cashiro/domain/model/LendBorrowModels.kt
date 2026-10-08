package com.ritesh.cashiro.domain.model

import com.ritesh.cashiro.data.database.entity.LendBorrowType
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Domain model representing a Person in the Lendings & Borrowings (Khata) ledger.
 */
enum class PersonCategory {
    FRIEND,
    FAMILY,
    COLLEAGUE,
    OTHER
}

data class LendBorrowPerson(
    val id: Long = 0,
    val name: String,
    val phoneNumber: String? = null,
    val notes: String? = null,
    val color: String = "#4CAF50",
    val avatar: String? = null,
    val category: PersonCategory? = null,
    val isArchived: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val totalLent: BigDecimal = BigDecimal.ZERO,
    val totalBorrowed: BigDecimal = BigDecimal.ZERO,
    val netBalance: BigDecimal = BigDecimal.ZERO, // positive = person owes user (User gets), negative = user owes person
    val activeTransactionsCount: Int = 0,
    val hasOverdue: Boolean = false
)

/**
 * Domain model representing an individual lending, borrowing, or settlement entry.
 */
data class LendBorrowTransactionItem(
    val id: Long = 0,
    val personId: Long,
    val transactionId: Long? = null,
    val type: LendBorrowType,
    val amount: BigDecimal,
    val title: String,
    val dueDate: LocalDateTime? = null,
    val isSettled: Boolean = false,
    val date: LocalDateTime = LocalDateTime.now(),
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val isSample: Boolean = false,
    val accountId: Long? = null,
    val category: String? = null,
    val merchant: String? = null,
    val attachments: List<String> = emptyList(),
    val currency: String = "INR",
    val originalAmount: BigDecimal = amount
)

/**
 * Domain model representing aggregated summary for the Khata dashboard.
 */
data class LendBorrowSummary(
    val totalLentRemaining: BigDecimal = BigDecimal.ZERO,      // Total money lent to people that hasn't been repaid yet
    val totalBorrowedRemaining: BigDecimal = BigDecimal.ZERO,  // Total money borrowed from people that hasn't been repaid yet
    val netBalance: BigDecimal = BigDecimal.ZERO,              // totalLentRemaining - totalBorrowedRemaining
    val activePersonsCount: Int = 0,
    val lentPersonsCount: Int = 0,                             // Number of people who owe money
    val borrowedPersonsCount: Int = 0,                         // Number of people money is owed to
    val overdueCount: Int = 0,
    val lentPersons: List<LendBorrowPerson> = emptyList(),
    val borrowedPersons: List<LendBorrowPerson> = emptyList()
)

data class PersonInfo(
    val name: String,
    val color: String,
    val avatar: String?
)
