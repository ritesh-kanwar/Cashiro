package com.ritesh.cashiro.presentation.ui.features.transactions

import com.ritesh.cashiro.data.database.entity.SubscriptionEntity
import com.ritesh.cashiro.data.database.entity.TransactionEntity
import com.ritesh.cashiro.domain.model.LendBorrowTransactionItem
import com.ritesh.cashiro.domain.model.PersonInfo
import java.math.BigDecimal
import java.time.LocalDate

data class TransactionDetailUiState(
    val transaction: TransactionEntity? = null,
    val balanceDiscrepancy: com.ritesh.cashiro.utils.BalanceDiscrepancy? = null,
    val isAddingBalanceAdjustment: Boolean = false,
    val primaryCurrency: String = "INR",
    val convertedAmount: BigDecimal? = null,
    val isEditMode: Boolean = false,
    val editableTransaction: TransactionEntity? = null,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null,
    val applyToAllFromMerchant: Boolean = false,
    val updateExistingTransactions: Boolean = false,
    val existingTransactionCount: Int = 0,
    val showDeleteDialog: Boolean = false,
    val isDeleting: Boolean = false,
    val deleteSuccess: Boolean = false,
    val duplicateSuccess: Boolean = false,
    val subscription: SubscriptionEntity? = null,
    val accountIconName: String? = null,
    val isCustomCycle: Boolean = false,
    val customCycleCount: Int = 1,
    val customCycleUnit: String = "month",
    val customCycleEndDate: LocalDate? = null,
    // Preview match sheet state
    val showMatchPreviewSheet: Boolean = false,
    val matchedTransactions: List<TransactionEntity> = emptyList(),
    val selectedMatchIds: Set<Long> = emptySet(),
    val isLoadingMatches: Boolean = false,
    val matchSearchQuery: String = "",
    val matchSearchResults: List<TransactionEntity> = emptyList(),
    val isAmoledMode: Boolean = false,
    val darkThemeConfig: Boolean? = null,
    // Loan (lend/borrow) linking
    val showMarkAsLoanSheet: Boolean = false,
    val showEditLendBorrowSheet: Boolean = false,
    val showUnmarkLoanConfirm: Boolean = false,
    // Lend/borrow entry linked to this transaction (if marked as a loan)
    val linkedLendBorrow: LendBorrowTransactionItem? = null,
    val linkedLoanPersonName: String? = null,
    val linkedLoanPersonAvatar: String? = null,
    val markAsLoanSuccess: Boolean = false,
    val markAsLoanError: String? = null,
    val transactionPersonMapping: Map<Long, PersonInfo> = emptyMap()
)
