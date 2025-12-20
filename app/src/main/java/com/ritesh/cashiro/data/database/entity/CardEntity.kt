package com.ritesh.cashiro.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Entity representing a debit or credit card.
 * 
 * Debit cards can be linked to bank accounts via accountLast4.
 * Credit cards are standalone (accountLast4 is null).
 */
@Entity(
    tableName = "cards",
    indices = [
        Index(value = ["bank_name", "card_last4"], unique = true),
        Index(value = ["card_last4"]),
        Index(value = ["account_last4"])
    ]
)
data class CardEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    
    @ColumnInfo(name = "card_last4")
    val cardLast4: String,
    
    @ColumnInfo(name = "card_type")
    val cardType: CardType,
    
    @ColumnInfo(name = "bank_name")
    val bankName: String,
    
    @ColumnInfo(name = "account_last4")
    val accountLast4: String? = null,  // Links to AccountBalanceEntity for debit cards
    
    @ColumnInfo(name = "nickname")
    val nickname: String? = null,
    
    @ColumnInfo(name = "is_active", defaultValue = "1")
    val isActive: Boolean = true,
    
    // Balance tracking for card transactions
    @ColumnInfo(name = "last_balance")
    val lastBalance: BigDecimal? = null,
    
    @ColumnInfo(name = "last_balance_source")
    val lastBalanceSource: String? = null,  // SMS snippet for debugging
    
    @ColumnInfo(name = "last_balance_date")
    val lastBalanceDate: LocalDateTime? = null,
    
    @ColumnInfo(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: LocalDateTime = LocalDateTime.now(),

    @ColumnInfo(name = "currency", defaultValue = "INR")
    val currency: String = "INR"
)

enum class CardType {
    DEBIT,   // Links to a bank account
    CREDIT   // Standalone credit account
}