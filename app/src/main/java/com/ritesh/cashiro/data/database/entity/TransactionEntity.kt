package com.ritesh.cashiro.data.database.entity

import androidx.annotation.StringRes
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ritesh.cashiro.R
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity(tableName = "transactions", indices = [Index(value = ["transaction_hash"], unique = true)])
data class TransactionEntity(
        @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0,
        @ColumnInfo(name = "amount") val amount: BigDecimal,
        @ColumnInfo(name = "merchant_name") val merchantName: String,
        @ColumnInfo(name = "category") val category: String,
        @ColumnInfo(name = "subcategory") val subcategory: String? = null,
        @ColumnInfo(name = "transaction_type") val transactionType: TransactionType,
        @ColumnInfo(name = "date_time") val dateTime: LocalDateTime,
        @ColumnInfo(name = "description") val description: String? = null,
        @ColumnInfo(name = "sms_body") val smsBody: String? = null,
        @ColumnInfo(name = "bank_name") val bankName: String? = null,
        @ColumnInfo(name = "sms_sender") val smsSender: String? = null,
        @ColumnInfo(name = "account_number") val accountNumber: String? = null,
        @ColumnInfo(name = "balance_after") val balanceAfter: BigDecimal? = null,
        @ColumnInfo(name = "transaction_hash", defaultValue = "") val transactionHash: String,
        @ColumnInfo(name = "is_recurring") val isRecurring: Boolean = false,
        @ColumnInfo(name = "is_deleted", defaultValue = "0") val isDeleted: Boolean = false,
        @ColumnInfo(name = "created_at") val createdAt: LocalDateTime = LocalDateTime.now(),
        @ColumnInfo(name = "updated_at") val updatedAt: LocalDateTime = LocalDateTime.now(),
        @ColumnInfo(name = "currency", defaultValue = "INR") val currency: String = "INR",
        @ColumnInfo(name = "from_account") val fromAccount: String? = null,
        @ColumnInfo(name = "to_account") val toAccount: String? = null,
        @ColumnInfo(name = "from_bank_name") val fromBankName: String? = null,
        @ColumnInfo(name = "to_bank_name") val toBankName: String? = null,
        @ColumnInfo(name = "reference") val reference: String? = null,
        @ColumnInfo(name = "billing_cycle") val billingCycle: String? = null,
        @ColumnInfo(name = "attachments", defaultValue = "") val attachments: String = "",
        @ColumnInfo(name = "is_sample", defaultValue = "0") val isSample: Boolean = false
)

enum class TransactionType(@StringRes val labelRes: Int) {
    INCOME(R.string.type_income),
    EXPENSE(R.string.type_expense),
    CREDIT(R.string.type_credit),
    TRANSFER(R.string.type_transfer),
    INVESTMENT(R.string.type_investment),
    BALANCE_UPDATE(R.string.type_balance_update),
    LENT(R.string.type_lent),
    BORROWED(R.string.type_borrowed)
}
