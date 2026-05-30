package com.ritesh.cashiro.presentation.ui.features.transactions

import com.ritesh.cashiro.utils.SubscriptionUtils

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ritesh.cashiro.data.currency.CurrencyConversionService
import com.ritesh.cashiro.data.database.entity.CategoryEntity
import com.ritesh.cashiro.data.database.entity.TransactionEntity
import com.ritesh.cashiro.data.database.entity.TransactionType
import com.ritesh.cashiro.data.repository.AccountBalanceRepository
import com.ritesh.cashiro.data.repository.CategoryRepository
import com.ritesh.cashiro.data.repository.MerchantMappingRepository
import com.ritesh.cashiro.data.repository.TransactionRepository
import com.ritesh.cashiro.data.repository.SubcategoryRepository
import com.ritesh.cashiro.data.repository.SubscriptionRepository
import com.ritesh.cashiro.data.database.entity.SubscriptionEntity
import com.ritesh.cashiro.data.database.entity.SubscriptionState
import com.ritesh.cashiro.data.service.AttachmentService
import com.ritesh.cashiro.core.Constants
import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import com.ritesh.cashiro.data.database.entity.SubcategoryEntity
import com.ritesh.cashiro.utils.CurrencyFormatter
import com.ritesh.cashiro.utils.DeviceEncryption
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.net.URLEncoder
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TransactionDetailViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val merchantMappingRepository: MerchantMappingRepository,
    private val categoryRepository: CategoryRepository,
    private val subcategoryRepository: SubcategoryRepository,
    private val accountBalanceRepository: AccountBalanceRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val currencyConversionService: CurrencyConversionService,
    val attachmentService: AttachmentService,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionDetailUiState())
    val uiState: StateFlow<TransactionDetailUiState> = _uiState.asStateFlow()

    // Categories should be based on transaction type
    val categories: StateFlow<List<CategoryEntity>> = _uiState.map { state ->
        val transaction = state.editableTransaction ?: state.transaction
        transaction?.transactionType == TransactionType.INCOME
    }.flatMapLatest { isIncome ->
        if (isIncome) {
            categoryRepository.getIncomeCategories()
        } else {
            categoryRepository.getExpenseCategories()
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Available accounts for linking (excluding hidden accounts)
    private val sharedPrefs =
        context.getSharedPreferences("account_prefs", Context.MODE_PRIVATE)

    val availableAccounts: StateFlow<List<AccountBalanceEntity>> = accountBalanceRepository.getAllLatestBalances()
        .map { balances ->
            val hiddenAccounts =
                sharedPrefs.getStringSet("hidden_accounts", emptySet()) ?: emptySet()
            balances
                .filter { balance ->
                    val key = "${balance.bankName}_${balance.accountLast4}"
                    !hiddenAccounts.contains(key)
                }
                .distinctBy { "${it.bankName}_${it.accountLast4}" }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val selectedAccount: StateFlow<AccountBalanceEntity?> = _uiState.map { state ->
        val transaction = state.editableTransaction
        if (transaction == null) return@map null
        availableAccounts.value.find {
            it.bankName == transaction.bankName && it.accountLast4 == transaction.accountNumber
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val targetAccount: StateFlow<AccountBalanceEntity?> = _uiState.map { state ->
        val transaction = state.editableTransaction
        if (transaction == null) return@map null
        availableAccounts.value.find { it.accountLast4 == transaction.toAccount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)


    val allSubcategories: StateFlow<Map<Long, List<SubcategoryEntity>>> =
        subcategoryRepository.subcategoriesMap

    // Attachment state for edit mode
    private val _editableAttachments = MutableStateFlow<List<String>>(emptyList())
    val editableAttachments: StateFlow<List<String>> = _editableAttachments.asStateFlow()

    fun loadTransaction(transactionId: Long) {
        viewModelScope.launch {
            val transaction = transactionRepository.getTransactionById(transactionId)
            _uiState.update { it.copy(transaction = transaction) }
            val accountIconName = transaction?.let { txn ->
                accountBalanceRepository.getLatestBalance(txn.bankName ?: "", txn.accountNumber ?: "")?.iconName
            }
            _uiState.update { it.copy(accountIconName = accountIconName) }
            transaction?.let {
                determinePrimaryCurrency(it)
                calculateConvertedAmount(it)
                findLinkedSubscription(it)
            }
        }
    }

    private suspend fun findLinkedSubscription(transaction: TransactionEntity) {
        if (transaction.isRecurring) {
            val linked = subscriptionRepository.matchTransactionToSubscription(
                transaction.merchantName,
                transaction.amount
            )
            _uiState.update { it.copy(subscription = linked) }
        } else {
            _uiState.update { it.copy(subscription = null) }
        }
    }

    private suspend fun determinePrimaryCurrency(transaction: TransactionEntity) {
        val bankName = transaction.bankName
        val accountNumber = transaction.accountNumber
        val primaryCurrency = if (!bankName.isNullOrEmpty() && !accountNumber.isNullOrEmpty()) {
            accountBalanceRepository.getLatestBalance(bankName, accountNumber)?.currency
                ?: CurrencyFormatter.getBankBaseCurrency(bankName)
        } else if (!bankName.isNullOrEmpty()) {
            CurrencyFormatter.getBankBaseCurrency(bankName)
        } else {
            transaction.currency.takeIf { it.isNotEmpty() } ?: "INR"
        }
        _uiState.update { it.copy(primaryCurrency = primaryCurrency) }
    }

    private suspend fun calculateConvertedAmount(transaction: TransactionEntity) {
        val primaryCurrency = _uiState.value.primaryCurrency
        if (transaction.currency.isNotEmpty() && !transaction.currency.equals(
                primaryCurrency,
                ignoreCase = true
            )
        ) {
            // Convert the amount to the primary currency
            val converted = currencyConversionService.convertAmount(
                amount = transaction.amount,
                fromCurrency = transaction.currency,
                toCurrency = primaryCurrency
            )
            _uiState.update { it.copy(convertedAmount = converted) }
        } else {
            // No conversion needed if currencies are the same
            _uiState.update { it.copy(convertedAmount = null) }
        }
    }

    fun enterEditMode() {
        val currentTransaction = _uiState.value.transaction
        val billingCycle = currentTransaction?.billingCycle
        
        var isCustom = false
        var count = 1
        var unit = "month"
        var endDate: LocalDate? = null

        if (billingCycle?.startsWith("custom_") == true) {
            isCustom = true
            val parts = billingCycle.split("_")
            count = parts.getOrNull(1)?.toIntOrNull() ?: 1
            unit = parts.getOrNull(2) ?: "month"
            val endDateStr = parts.getOrNull(3)
            if (endDateStr != null && endDateStr != "forever") {
                endDate = try { LocalDate.parse(endDateStr) } catch (e: Exception) { null }
            }
        }

        _uiState.update { state ->
            state.copy(
                editableTransaction = state.transaction?.copy(),
                isEditMode = true,
                errorMessage = null,
                isCustomCycle = isCustom,
                customCycleCount = count,
                customCycleUnit = unit,
                customCycleEndDate = endDate
            )
        }
        // Initialize editable attachments from transaction
        _uiState.value.transaction?.let { txn ->
            _editableAttachments.value = attachmentService.parseAttachments(txn.attachments)
        }

        // Load count of other transactions from same merchant (always, not just for SMS)
        _uiState.value.transaction?.let { txn ->
            viewModelScope.launch {
                val matches = transactionRepository.getTransactionsByMerchantContains(
                    txn.merchantName,
                    txn.id
                )
                _uiState.update { it.copy(
                    existingTransactionCount = matches.size,
                    matchedTransactions = matches,
                    selectedMatchIds = matches.map { m -> m.id }.toSet()
                ) }
            }
        }
    }

    fun exitEditMode() {
        _uiState.update {
            it.copy(
                editableTransaction = null,
                isEditMode = false,
                errorMessage = null,
                applyToAllFromMerchant = false,
                updateExistingTransactions = false,
                existingTransactionCount = 0
            )
        }
        _editableAttachments.value = emptyList()
    }

    fun toggleApplyToAllFromMerchant() {
        _uiState.update { it.copy(applyToAllFromMerchant = !it.applyToAllFromMerchant) }
    }

    fun toggleUpdateExistingTransactions() {
        _uiState.update { it.copy(updateExistingTransactions = !it.updateExistingTransactions) }
    }

    fun showMatchPreviewSheet() {
        _uiState.update { it.copy(showMatchPreviewSheet = true) }
    }

    fun hideMatchPreviewSheet() {
        _uiState.update { it.copy(showMatchPreviewSheet = false) }
    }

    fun toggleMatchSelection(transactionId: Long) {
        val current = _uiState.value.selectedMatchIds
        val updated = if (current.contains(transactionId)) current - transactionId
                      else current + transactionId
        _uiState.update { it.copy(selectedMatchIds = updated) }
    }

    fun selectAllMatches() {
        val allIds = _uiState.value.matchedTransactions.map { it.id }.toSet()
        _uiState.update { it.copy(selectedMatchIds = allIds) }
    }

    fun deselectAllMatches() {
        _uiState.update { it.copy(selectedMatchIds = emptySet()) }
    }

    /** Adds a transaction found via the Transactions search screen to the match preview list. */
    fun addTransactionToMatchList(transaction: TransactionEntity) {
        val current = _uiState.value.matchedTransactions
        if (current.any { it.id == transaction.id }) return
        _uiState.update { state ->
            state.copy(
                matchedTransactions = current + transaction,
                selectedMatchIds = state.selectedMatchIds + transaction.id,
                matchSearchQuery = "",
                matchSearchResults = emptyList()
            )
        }
    }

    fun updateMatchSearchQuery(query: String) {
        _uiState.update { it.copy(matchSearchQuery = query) }
        if (query.isBlank()) {
            _uiState.update { it.copy(matchSearchResults = emptyList()) }
            return
        }
        viewModelScope.launch {
            try {
                val results = transactionRepository.searchTransactionsList(query)
                val existingIds = _uiState.value.matchedTransactions.map { it.id }.toSet()
                val currentId = _uiState.value.transaction?.id ?: -1L
                val filtered = results.filter { it.id !in existingIds && it.id != currentId }
                _uiState.update { it.copy(matchSearchResults = filtered) }
            } catch (e: Exception) {
                // Ignore search errors
            }
        }
    }

    /** Applies the current editable category to only the user-selected transactions. */
    fun applyToSelectedMatches() {
        val state = _uiState.value
        val category = state.editableTransaction?.category ?: return
        val ids = state.selectedMatchIds
        if (ids.isEmpty()) return

        viewModelScope.launch {
            try {
                val toUpdate = state.matchedTransactions
                    .filter { it.id in ids }
                    .map { it.copy(category = category) }
                toUpdate.forEach { transactionRepository.updateTransaction(it) }
                _uiState.update { it.copy(showMatchPreviewSheet = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to update transactions: ${e.message}") }
            }
        }
    }

    fun updateMerchantName(name: String) {
        _uiState.update { it.copy(editableTransaction = it.editableTransaction?.copy(merchantName = name)) }
        validateMerchantName(name)
    }

    fun updateAmount(amountStr: String) {
        val amount = amountStr.toBigDecimalOrNull()
        if (amount != null && amount > BigDecimal.ZERO) {
            _uiState.update { it.copy(editableTransaction = it.editableTransaction?.copy(amount = amount), errorMessage = null) }
        } else if (amountStr.isNotEmpty()) {
            _uiState.update { it.copy(errorMessage = "Amount must be a positive number") }
        }
    }

    fun updateTransactionType(type: TransactionType) {
        _uiState.update { it.copy(editableTransaction = it.editableTransaction?.copy(transactionType = type)) }

        // Auto-select category based on type
        val newCategory = when (type) {
            TransactionType.INCOME -> "Salary"
            TransactionType.EXPENSE -> "Miscellaneous"
            TransactionType.CREDIT -> "Credit Bill"
            TransactionType.TRANSFER -> {
                val targetAccount = _uiState.value.editableTransaction?.toAccount
                if (targetAccount == "wallet") "Cash Withdrawal" else "Self Transfer"
            }

            TransactionType.INVESTMENT -> "Investment"
        }

        updateCategory(newCategory)
    }

    fun addAttachment(path: String) {
        _editableAttachments.update { it + path }
    }

    fun removeAttachment(path: String) {
        attachmentService.deleteAttachment(path)
        _editableAttachments.update { it - path }
    }

    fun updateCategory(category: String) {
        _uiState.update { state ->
            val current = state.editableTransaction
            state.copy(
                editableTransaction = current?.copy(
                    category = category.ifEmpty { "Miscellaneous" },
                    subcategory = if (current.category != category) null else current.subcategory
                )
            )
        }
    }

    fun updateSubcategory(subcategory: String?) {
        _uiState.update { it.copy(editableTransaction = it.editableTransaction?.copy(subcategory = subcategory)) }
    }

    fun updateDateTime(dateTime: LocalDateTime) {
        _uiState.update { it.copy(editableTransaction = it.editableTransaction?.copy(dateTime = dateTime)) }
    }

    fun updateDescription(description: String?) {
        _uiState.update { it.copy(editableTransaction = it.editableTransaction?.copy(description = if (description.isNullOrEmpty()) null else description)) }
    }

    fun updateRecurringStatus(isRecurring: Boolean) {
        _uiState.update { state ->
            val current = state.editableTransaction
            state.copy(
                editableTransaction = current?.copy(
                    isRecurring = isRecurring,
                    billingCycle = if (isRecurring) current.billingCycle ?: "Monthly" else null
                ),
                isCustomCycle = if (!isRecurring) false else state.isCustomCycle
            )
        }
    }

    fun updateBillingCycle(cycle: String) {
        _uiState.update { it.copy(
            editableTransaction = it.editableTransaction?.copy(billingCycle = cycle),
            isCustomCycle = cycle == "Custom"
        ) }
    }

    fun updateSubscriptionCustomCycleCount(count: Int) {
        _uiState.update { it.copy(customCycleCount = count) }
    }

    fun updateSubscriptionCustomCycleUnit(unit: String) {
        _uiState.update { it.copy(customCycleUnit = unit) }
    }

    fun updateSubscriptionCustomCycleEndDate(date: LocalDate?) {
        _uiState.update { it.copy(customCycleEndDate = date) }
    }

    fun updateAccountNumber(accountNumber: String?) {
        _uiState.update { it.copy(editableTransaction = it.editableTransaction?.copy(accountNumber = if (accountNumber.isNullOrEmpty()) null else accountNumber)) }
    }

    fun updateTransactionAccount(account: AccountBalanceEntity?) {
        _uiState.update { state ->
            val current = state.editableTransaction
            state.copy(
                editableTransaction = current?.copy(
                    bankName = account?.bankName ?: "Manual Entry",
                    accountNumber = account?.accountLast4,
                    currency = account?.currency ?: current.currency
                ),
                accountIconName = account?.iconName // Update the icon name in state too
            )
        }
    }

    fun updateTransactionTargetAccount(account: AccountBalanceEntity?) {
        _uiState.update { it.copy(editableTransaction = it.editableTransaction?.copy(toAccount = account?.accountLast4)) }

        // Update category if type is TRANSFER
        _uiState.value.editableTransaction?.let { txn ->
            if (txn.transactionType == TransactionType.TRANSFER) {
                val newCategory = if (account?.accountLast4 == "wallet") {
                    "Cash Withdrawal"
                } else {
                    "Self Transfer"
                }
                updateCategory(newCategory)
            }
        }
    }

    fun updateCurrency(currency: String) {
        _uiState.update { it.copy(editableTransaction = it.editableTransaction?.copy(currency = currency)) }
        // Recalculate converted amount when currency changes
        _uiState.value.editableTransaction?.let { transaction ->
            viewModelScope.launch {
                calculateConvertedAmount(transaction)
            }
        }
    }

    fun saveChanges() {
        val state = _uiState.value
        val toSave = state.editableTransaction ?: return

        // Validate before saving
        if (toSave.merchantName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Merchant name is required") }
            return
        }

        if (toSave.amount <= BigDecimal.ZERO) {
            _uiState.update { it.copy(errorMessage = "Amount must be positive") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                // Handle custom cycle serialization
                val finalBillingCycle = if (state.isCustomCycle) {
                    val endDateStr = state.customCycleEndDate?.toString() ?: "forever"
                    "custom_${state.customCycleCount}_${state.customCycleUnit}_$endDateStr"
                } else toSave.billingCycle

                // Normalize merchant name before saving
                val normalizedTransaction = toSave.copy(
                    merchantName = normalizeMerchantName(toSave.merchantName),
                    billingCycle = finalBillingCycle,
                    updatedAt = LocalDateTime.now(),
                    attachments = attachmentService.joinAttachments(_editableAttachments.value)
                )

                transactionRepository.updateTransaction(normalizedTransaction)

                // Handle Balance Updates
                val originalTransaction = state.transaction
                if (originalTransaction != null) {
                    updateAccountBalances(originalTransaction, normalizedTransaction)
                }

                // Sync with subscriptions if recurring
                syncSubscriptionForTransaction(normalizedTransaction)

                // Save merchant mapping if checkbox is checked
                if (state.applyToAllFromMerchant) {
                    merchantMappingRepository.setMapping(
                        normalizedTransaction.merchantName,
                        normalizedTransaction.category
                    )
                }

                // Update existing transactions if checkbox is checked (fuzzy/contains match)
                if (state.updateExistingTransactions) {
                    transactionRepository.updateCategoryAndSubcategoryForMerchantContains(
                        normalizedTransaction.merchantName,
                        normalizedTransaction.category,
                        normalizedTransaction.subcategory
                    )
                }

                _uiState.update {
                    it.copy(
                        transaction = normalizedTransaction,
                        saveSuccess = true,
                        isEditMode = false,
                        editableTransaction = null,
                        errorMessage = null,
                        applyToAllFromMerchant = false,
                        updateExistingTransactions = false,
                        existingTransactionCount = 0,
                        matchedTransactions = emptyList(),
                        selectedMatchIds = emptySet(),
                        showMatchPreviewSheet = false
                    )
                }
                findLinkedSubscription(normalizedTransaction) // Refresh linked subscription
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to save changes: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun cancelEdit() {
        exitEditMode()
    }

    fun clearSaveSuccess() {
        _uiState.update { it.copy(saveSuccess = false) }
    }

    private fun validateMerchantName(name: String) {
        _uiState.update { it.copy(errorMessage = if (name.isBlank()) "Merchant name is required" else null) }
    }

    /**
     * Normalizes merchant name to consistent format.
     * Converts all-caps to proper case, preserves already mixed case.
     */
    private fun normalizeMerchantName(name: String): String {
        val trimmed = name.trim()

        // If it's all uppercase, convert to proper case
        return if (trimmed == trimmed.uppercase()) {
            trimmed.lowercase().split(" ").joinToString(" ") { word ->
                word.let { if (it.isNotEmpty()) it.take(1).uppercase() + it.drop(1) else it }
            }
        } else {
            // Already has mixed case, keep as is
            trimmed
        }
    }

    fun getReportUrl(): String {
        val txn = _uiState.value.transaction ?: return ""

        val smsBody = txn.smsBody ?: "Transaction: ${txn.merchantName} - ${txn.amount}"
        val sender = txn.smsSender ?: "Unknown Sender"
        val bank = txn.bankName ?: "Manual"

        val issueTitle = "[Parsing Issue] ${txn.merchantName} - ${txn.amount}"
        val issueBody = """
            ### Transaction Details
            - **Merchant:** ${txn.merchantName}
            - **Amount:** ${txn.amount} ${txn.currency}
            - **Type:** ${txn.transactionType}
            - **Bank:** $bank
            - **Sender:** $sender
            
            ### Original SMS
            ```
            $smsBody
            ```
            
            ### Expected Behavior
            _Describe what was wrong (e.g., wrong category, wrong date, etc.)_
        """.trimIndent()

        val encodedTitle = URLEncoder.encode(issueTitle, "UTF-8")
        val encodedBody = URLEncoder.encode(issueBody, "UTF-8")

        return "https://github.com/ritesh-kanwar/Cashiro/issues/new?title=$encodedTitle&body=$encodedBody"
    }

    fun showDeleteDialog() {
        _uiState.update { it.copy(showDeleteDialog = true) }
    }

    fun hideDeleteDialog() {
        _uiState.update { it.copy(showDeleteDialog = false) }
    }

    fun deleteTransaction() {
        viewModelScope.launch {
            _uiState.value.transaction?.let { txn ->
                _uiState.update { it.copy(isDeleting = true, showDeleteDialog = false) }

                try {
                    transactionRepository.deleteTransaction(txn)
                    _uiState.update { it.copy(deleteSuccess = true) }
                } catch (e: Exception) {
                    _uiState.update { it.copy(errorMessage = "Failed to delete transaction") }
                } finally {
                    _uiState.update { it.copy(isDeleting = false) }
                }
            }
        }
    }

    private suspend fun syncSubscriptionForTransaction(transaction: TransactionEntity) {
        if (transaction.isRecurring) {
            val existing = subscriptionRepository.matchTransactionToSubscription(
                transaction.merchantName,
                transaction.amount
            )

            val nextPaymentDate = SubscriptionUtils.calculateNextPaymentDate(
                (transaction.dateTime ?: LocalDateTime.now()).toLocalDate(),
                transaction.billingCycle
            )

            val subscription = existing?.copy(
                amount = transaction.amount,
                nextPaymentDate = nextPaymentDate,
                category = transaction.category,
                subcategory = transaction.subcategory,
                bankName = transaction.bankName,
                currency = transaction.currency,
                billingCycle = transaction.billingCycle,
                state = SubscriptionState.ACTIVE,
                updatedAt = LocalDateTime.now()
            )
                ?: SubscriptionEntity(
                    merchantName = transaction.merchantName,
                    amount = transaction.amount,
                    nextPaymentDate = nextPaymentDate,
                    state = SubscriptionState.ACTIVE,
                    bankName = transaction.bankName,
                    category = transaction.category,
                    subcategory = transaction.subcategory,
                    currency = transaction.currency,
                    billingCycle = transaction.billingCycle,
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now()
                )
            subscriptionRepository.insertSubscription(subscription)
        } else {
            // Find existing matching subscription and hide it
            val existing = subscriptionRepository.matchTransactionToSubscription(
                transaction.merchantName,
                transaction.amount
            )
            if (existing != null) {
                subscriptionRepository.hideSubscription(existing.id)
            }
        }
    }


    private suspend fun updateAccountBalances(
        oldTransaction: TransactionEntity,
        newTransaction: TransactionEntity
    ) {
        //Revert effect of old transaction
        revertBalanceEffect(oldTransaction)

        //Apply effect of new transaction
        applyBalanceEffect(newTransaction)
    }

    private suspend fun revertBalanceEffect(transaction: TransactionEntity) {
        val bankName = transaction.bankName
        val accountLast4 = transaction.accountNumber

        if (bankName != null && accountLast4 != null) {
            when (transaction.transactionType) {
                TransactionType.INCOME -> {
                    // Originally added, so subtract to revert
                    updateBalance(
                        bankName,
                        accountLast4,
                        transaction.amount.negate(),
                        transaction.currency
                    )
                }

                TransactionType.EXPENSE, TransactionType.INVESTMENT -> {
                    // Originally subtracted, so add to revert
                    updateBalance(bankName, accountLast4, transaction.amount, transaction.currency)
                }

                TransactionType.CREDIT -> {
                    // No balance update to revert for now
                }

                TransactionType.TRANSFER -> {
                    // Revert source: Originally subtracted, so add
                    updateBalance(bankName, accountLast4, transaction.amount, transaction.currency)

                   //find an account with that last4.
                    transaction.toAccount?.let { targetLast4 ->
                        findAccountByLast4(targetLast4)?.let { targetAccount ->
                            updateBalance(
                                targetAccount.bankName,
                                targetLast4,
                                transaction.amount.negate(),
                                transaction.currency
                            )
                        }
                    }
                }
            }
        }
    }

    private suspend fun applyBalanceEffect(transaction: TransactionEntity) {
        val bankName = transaction.bankName
        val accountLast4 = transaction.accountNumber

        if (bankName != null && accountLast4 != null) {
            when (transaction.transactionType) {
                TransactionType.INCOME -> {
                    updateBalance(bankName, accountLast4, transaction.amount, transaction.currency)
                }

                TransactionType.EXPENSE, TransactionType.INVESTMENT -> {
                    updateBalance(
                        bankName,
                        accountLast4,
                        transaction.amount.negate(),
                        transaction.currency
                    )
                }

                TransactionType.CREDIT -> {
                    // No balance update for now
                }

                TransactionType.TRANSFER -> {
                    // Source: Subtract
                    updateBalance(
                        bankName,
                        accountLast4,
                        transaction.amount.negate(),
                        transaction.currency
                    )

                    // Target: Add
                    transaction.toAccount?.let { targetLast4 ->
                        findAccountByLast4(targetLast4)?.let { targetAccount ->
                            updateBalance(
                                targetAccount.bankName,
                                targetLast4,
                                transaction.amount,
                                transaction.currency
                            )
                        }
                    }
                }
            }
        }
    }

    private suspend fun updateBalance(
        bankName: String,
        accountLast4: String,
        amountDelta: BigDecimal,
        currency: String,
        transactionId: Long? = null
    ) {
        val currentBalance = accountBalanceRepository.getLatestBalance(bankName, accountLast4)
        val newBalance = (currentBalance?.balance ?: BigDecimal.ZERO) + amountDelta

        accountBalanceRepository.insertBalance(
            AccountBalanceEntity(
                bankName = bankName,
                accountLast4 = accountLast4,
                balance = newBalance,
                timestamp = LocalDateTime.now(),
                transactionId = transactionId,
                sourceType = "MANUAL_EDIT",
                iconResId = currentBalance?.iconResId ?: 0,
                isCreditCard = currentBalance?.isCreditCard ?: false,
                isWallet = currentBalance?.isWallet ?: false,
                creditLimit = currentBalance?.creditLimit,
                currency = currency
            )
        )
    }

    private suspend fun findAccountByLast4(last4: String): AccountBalanceEntity? {
        return accountBalanceRepository.getAllLatestBalances().first()
            .find { it.accountLast4 == last4 }
    }
}
