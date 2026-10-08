package com.ritesh.cashiro.presentation.ui.features.add

import com.ritesh.cashiro.utils.SubscriptionUtils

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import com.ritesh.cashiro.data.database.entity.SubcategoryEntity
import com.ritesh.cashiro.data.database.entity.TransactionType
import com.ritesh.cashiro.data.repository.AccountBalanceRepository
import com.ritesh.cashiro.data.repository.CurrencyRepository
import com.ritesh.cashiro.data.repository.SubcategoryRepository
import com.ritesh.cashiro.data.service.AttachmentService
import com.ritesh.cashiro.domain.usecase.AddEditLendBorrowPersonUseCase
import com.ritesh.cashiro.domain.usecase.AddSubscriptionUseCase
import com.ritesh.cashiro.domain.usecase.AddTransactionUseCase
import com.ritesh.cashiro.domain.usecase.GetCategoriesUseCase
import com.ritesh.cashiro.domain.usecase.GetLendBorrowPersonsUseCase
import com.ritesh.cashiro.domain.usecase.MarkTransactionAsLoanUseCase
import com.ritesh.cashiro.domain.usecase.UpdateSubscriptionUseCase
import com.ritesh.cashiro.domain.model.PersonCategory
import com.ritesh.cashiro.data.database.entity.LendBorrowType
import com.ritesh.cashiro.data.repository.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalTime
import com.ritesh.cashiro.R

@HiltViewModel
class AddViewModel
@Inject
constructor(
    private val addTransactionUseCase: AddTransactionUseCase,
    private val addSubscriptionUseCase: AddSubscriptionUseCase,
    private val addEditLendBorrowPersonUseCase: AddEditLendBorrowPersonUseCase,
    private val getLendBorrowPersonsUseCase: GetLendBorrowPersonsUseCase,
    private val markTransactionAsLoanUseCase: MarkTransactionAsLoanUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val subcategoryRepository: SubcategoryRepository,
    private val accountBalanceRepository: AccountBalanceRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val updateSubscriptionUseCase: UpdateSubscriptionUseCase,
    private val currencyRepository: CurrencyRepository,
    val attachmentService: AttachmentService,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val sharedPrefs = context.getSharedPreferences("account_prefs", Context.MODE_PRIVATE)


    // General UI State
    private val _uiState = MutableStateFlow(AddUiState())
    val uiState: StateFlow<AddUiState> = _uiState.asStateFlow()

    // Transaction Tab State
    private val _transactionUiState = MutableStateFlow(TransactionUiState())
    val transactionUiState: StateFlow<TransactionUiState> = _transactionUiState.asStateFlow()

    // Subscription Tab State
    private val _subscriptionUiState = MutableStateFlow(SubscriptionUiState())
    val subscriptionUiState: StateFlow<SubscriptionUiState> = _subscriptionUiState.asStateFlow()

    // Categories for dropdowns
    val categories =
        getCategoriesUseCase
            .execute()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    // People available for linking LENT/BORROWED transactions to a Khata person.
    val persons =
        getLendBorrowPersonsUseCase()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    // Accounts for dropdown
    val accounts = accountBalanceRepository
            .getAllLatestBalances()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    // Effective base currency (unified > default > main account) used as the
    // currency when no specific account is selected.
    val baseCurrency: StateFlow<String> = currencyRepository.effectiveBaseCurrencyCode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "INR"
        )

    // All Subcategories for sheet
    val allSubcategories = subcategoryRepository.subcategoriesMap



    // Subcategories for the selected category
    private val _transactionSubcategories =
        MutableStateFlow<List<SubcategoryEntity>>(
            emptyList()
        )
    val transactionSubcategories:
            StateFlow<List<SubcategoryEntity>> =
        _transactionSubcategories.asStateFlow()

    private val _subscriptionSubcategories =
        MutableStateFlow<List<SubcategoryEntity>>(
            emptyList()
        )
    val subscriptionSubcategories:
            StateFlow<List<SubcategoryEntity>> =
        _subscriptionSubcategories.asStateFlow()

    // Transaction attachments
    private val _transactionAttachments = MutableStateFlow<List<String>>(emptyList())
    val transactionAttachments: StateFlow<List<String>> = _transactionAttachments.asStateFlow()

    // Subscription attachments
    private val _subscriptionAttachments = MutableStateFlow<List<String>>(emptyList())
    val subscriptionAttachments: StateFlow<List<String>> = _subscriptionAttachments.asStateFlow()

    private var initializationJob: kotlinx.coroutines.Job? = null

    init {
        initializeData()
    }

    private fun initializeData() {
        // Load default subcategories for both tabs
        updateTransactionSubcategories("Miscellaneous")
        updateSubscriptionSubcategories("Subscription")

        // Pre-select main account before applying an external draft.
        initializationJob?.cancel()
        initializationJob = viewModelScope.launch {
            val effectiveCurrency = currencyRepository.effectiveBaseCurrencyCode.first()
            accountBalanceRepository.getAllLatestBalances().first().let { availableAccounts ->
                val mainAccountKey = sharedPrefs.getString("main_account", null)
                if (mainAccountKey != null) {
                    val mainAccount = availableAccounts.find { 
                        "${it.bankName}_${it.accountLast4}" == mainAccountKey 
                    }
                    if (mainAccount != null) {
                        _transactionUiState.update { it.copy(selectedAccount = mainAccount, currency = mainAccount.currency) }
                        _subscriptionUiState.update { it.copy(selectedAccount = mainAccount, currency = mainAccount.currency) }
                        return@let
                    }
                }
                // No main account (e.g. cashew import with multiple accounts):
                // fall back to the effective base currency.
                _transactionUiState.update { it.copy(currency = effectiveCurrency) }
                _subscriptionUiState.update { it.copy(currency = effectiveCurrency) }
            }
        }
    }

    suspend fun prefillSharedText(text: String) {
        initializationJob?.join()
        val boundedText = text.take(1000)
        _transactionUiState.update { state ->
            val amount = SharedTextAmount.amountForCurrency(boundedText, state.currency)
            state.copy(amount = amount?.toPlainString().orEmpty(), notes = boundedText)
        }
    }

    fun resetAllStates() {
        _transactionUiState.value = TransactionUiState()
        _subscriptionUiState.value = SubscriptionUiState()
        _transactionAttachments.value = emptyList()
        _subscriptionAttachments.value = emptyList()
        initializeData()
    }

    // Transaction Tab Functions
    fun updateTransactionAmount(amount: String) {
        val filtered = amount.filter { it.isDigit() || it == '.' }
        val decimalCount = filtered.count { it == '.' }
        val validAmount = if (decimalCount <= 1) filtered else _transactionUiState.value.amount

        _transactionUiState.update { currentState ->
            currentState.copy(amount = validAmount, amountError = validateAmount(validAmount))
        }
    }

    fun updateTransactionType(type: TransactionType) {
        val newCategory = when (type) {
            TransactionType.INCOME -> "Income"
            TransactionType.EXPENSE -> "Miscellaneous"
            TransactionType.INVESTMENT -> "Investment"
            TransactionType.CREDIT -> "Shopping"
            TransactionType.TRANSFER -> "Self Transfer"
            TransactionType.LENT -> "Lent"
            TransactionType.BORROWED -> "Borrowed"
            else -> _transactionUiState.value.category
        }

        _transactionUiState.update { currentState ->
            currentState.copy(
                transactionType = type,
                category = newCategory,
                subcategory = null,
                categoryError = validateCategory(newCategory)
            )
        }

        // Update subcategories for the new default category
        updateTransactionSubcategories(newCategory)
    }

    fun updateTransactionMerchant(merchant: String) {
        _transactionUiState.update { currentState ->
            currentState.copy(merchant = merchant, merchantError = validateMerchant(merchant))
        }
    }

    fun updateTransactionCategory(category: String) {
        _transactionUiState.update { currentState ->
            currentState.copy(
                category = category,
                subcategory = null,
                categoryError = validateCategory(category)
            )
        }

        updateTransactionSubcategories(category)
    }

    private fun updateTransactionSubcategories(category: String) {
        viewModelScope.launch {
            val cat = categories.value.find { it.name == category }
            if (cat != null) {
                subcategoryRepository.getSubcategoriesByCategoryId(cat.id).collect {
                    _transactionSubcategories.value = it
                }
            } else {
                _transactionSubcategories.value = emptyList()
            }
        }
    }

    fun updateTransactionSubcategory(subcategory: String?) {
        _transactionUiState.update { currentState -> currentState.copy(subcategory = subcategory) }
    }

    fun updateTransactionDate(dateMillis: Long) {
        val instant = Instant.ofEpochMilli(dateMillis)
        val localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()
        val currentTime = _transactionUiState.value.date.toLocalTime()
        val newDateTime = LocalDateTime.of(localDate, currentTime)

        _transactionUiState.update { currentState -> currentState.copy(date = newDateTime) }
    }

    fun updateTransactionTime(hour: Int, minute: Int) {
        val currentDate = _transactionUiState.value.date.toLocalDate()
        val newDateTime = currentDate.atTime(hour, minute)

        _transactionUiState.update { currentState -> currentState.copy(date = newDateTime) }
    }

    fun updateTransactionNotes(notes: String) {
        _transactionUiState.update { currentState -> currentState.copy(notes = notes) }
    }

    fun updateTransactionRecurring(isRecurring: Boolean) {
        _transactionUiState.update { currentState -> currentState.copy(isRecurring = isRecurring) }
    }

    fun saveTransaction(onSuccess: () -> Unit) {
        val state = _transactionUiState.value

        val isLoanType = state.transactionType == TransactionType.LENT ||
            state.transactionType == TransactionType.BORROWED
        val amountError = validateAmount(state.amount)
        val merchantError = if (!isLoanType) validateMerchant(state.merchant) else null
        val categoryError = validateCategory(state.category)

        // Additional validation for Transfer transactions
        if (state.transactionType == TransactionType.TRANSFER) {
            if (state.selectedAccount == null || state.targetAccount == null) {
                _transactionUiState.update { currentState ->
                    currentState.copy(
                        error = context.getString(R.string.err_source_target_required)
                    )
                }
                return
            }
            
            if (state.selectedAccount?.id == state.targetAccount?.id) {
                _transactionUiState.update { currentState ->
                    currentState.copy(
                        error = context.getString(R.string.err_accounts_different)
                    )
                }
                return
            }
        }

        if (amountError != null || merchantError != null || categoryError != null) {
            _transactionUiState.update { currentState ->
                currentState.copy(
                    amountError = amountError,
                    merchantError = merchantError,
                    categoryError = categoryError
                )
            }
            return
        }

        viewModelScope.launch {
            try {
                _transactionUiState.update { it.copy(isLoading = true) }

                val amount = BigDecimal(state.amount)

                val transactionId = addTransactionUseCase.execute(
                    amount = amount,
                    merchant = state.merchant.trim(),
                    category = state.category,
                    subcategory = state.subcategory,
                    type = state.transactionType,
                    date = state.date,
                    notes = state.notes.takeIf { it.isNotBlank() },
                    isRecurring = state.isRecurring,
                    bankName = state.selectedAccount?.bankName,
                    accountLast4 = state.selectedAccount?.accountLast4,
                    currency = state.currency,
                    sourceAccountId = state.selectedAccount?.id,
                    targetAccountBankName = state.targetAccount?.bankName,
                    targetAccountLast4 = state.targetAccount?.accountLast4,
                    attachments = attachmentService.joinAttachments(_transactionAttachments.value)
                )

                if (isLoanType && state.selectedPersonId != null) {
                    val selectedPerson = persons.value.find { it.id == state.selectedPersonId }
                    val loanTitle = selectedPerson?.name ?: state.merchant.trim()
                    markTransactionAsLoanUseCase(
                        transactionId = transactionId,
                        personId = state.selectedPersonId,
                        type = if (state.transactionType == TransactionType.LENT) {
                            LendBorrowType.LENT
                        } else {
                            LendBorrowType.BORROWED
                        },
                        amount = amount,
                        currency = state.currency,
                        title = loanTitle,
                        merchant = loanTitle,
                        category = state.category,
                        date = state.date,
                        dueDate = state.dueDate,
                        accountId = state.selectedAccount?.id
                    )
                }

                onSuccess()
            } catch (e: Exception) {
                _transactionUiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        error = e.message ?: context.getString(R.string.err_save_transaction)
                    )
                }
            }
        }
    }

    fun updateTransactionAccount(
        account: AccountBalanceEntity?
    ) {
        viewModelScope.launch {
            val currency = account?.currency ?: currencyRepository.effectiveBaseCurrencyCode.first()
            _transactionUiState.update { currentState -> 
                currentState.copy(
                    selectedAccount = account,
                    currency = currency
                ) 
            }
        }
    }

    fun updateTransactionTargetAccount(
        account: AccountBalanceEntity?
    ) {
        _transactionUiState.update { currentState -> currentState.copy(targetAccount = account) }
    }

    fun updateTransactionPersonId(personId: Long?) {
        _transactionUiState.update { currentState -> currentState.copy(selectedPersonId = personId) }
    }

    fun updateTransactionDueDate(dueDate: LocalDateTime?) {
        _transactionUiState.update { currentState -> currentState.copy(dueDate = dueDate) }
    }

    fun addPerson(
        name: String,
        phone: String?,
        notes: String?,
        color: String,
        avatar: String?,
        category: PersonCategory?
    ) {
        viewModelScope.launch {
            addEditLendBorrowPersonUseCase.addPerson(
                name, phone, notes, color, avatar, category
            )
        }
    }

    // Attachment management for transactions
    fun addTransactionAttachment(path: String) {
        _transactionAttachments.update { it + path }
    }

    fun removeTransactionAttachment(path: String) {
        attachmentService.deleteAttachment(path)
        _transactionAttachments.update { it - path }
    }

    // Attachment management for subscriptions
    fun addSubscriptionAttachment(path: String) {
        _subscriptionAttachments.update { it + path }
    }

    fun removeSubscriptionAttachment(path: String) {
        attachmentService.deleteAttachment(path)
        _subscriptionAttachments.update { it - path }
    }

    // Subscription Edit Loading
    fun loadSubscriptionForEdit(id: Long) {
        viewModelScope.launch {
            try {
                _subscriptionUiState.update { it.copy(isLoading = true) }
                val subscription = subscriptionRepository.getSubscriptionById(id)
                if (subscription != null) {
                    val cycle = subscription.billingCycle ?: "Monthly"
                    val isCustom = cycle.startsWith("custom_")
                    var customCount = 1
                    var customUnit = "month"
                    var customEndDate: LocalDate? = null
                    var displayCycle = cycle

                    if (isCustom) {
                        val parts = cycle.split("_")
                        customCount = parts.getOrNull(1)?.toIntOrNull() ?: 1
                        customUnit = parts.getOrNull(2) ?: "month"
                        val endDateStr = parts.getOrNull(3)
                        if (endDateStr != null && endDateStr != "forever") {
                            customEndDate = try { LocalDate.parse(endDateStr) } catch (e: Exception) { null }
                        }
                        displayCycle = "Custom"
                    }

                    _subscriptionUiState.update { state ->
                        state.copy(
                            subscriptionId = subscription.id,
                            serviceName = subscription.merchantName,
                            amount = subscription.amount.toString(),
                            billingCycle = displayCycle,
                            isCustomCycle = isCustom,
                            customCycleCount = customCount,
                            customCycleUnit = customUnit,
                            customCycleEndDate = customEndDate,
                            nextPaymentDate = subscription.nextPaymentDate ?: LocalDate.now(),
                            category = subscription.category ?: "Subscription",
                            subcategory = subscription.subcategory,
                            currency = subscription.currency,
                            notes = subscription.smsBody ?: "",
                            isLoading = false
                        )
                    }
                    
                    // Funding accounts are identified by bank, suffix and currency.
                    val matchedAccount = if (subscription.bankName != null && subscription.accountLast4 != null) {
                        accountBalanceRepository.getLatestBalance(
                            subscription.bankName, subscription.accountLast4, subscription.currency
                        )
                    } else null
                    _subscriptionUiState.update { it.copy(selectedAccount = matchedAccount) }
                    
                    // Update subcategories for the loaded category
                    updateSubscriptionSubcategories(subscription.category ?: "Subscription")
                } else {
                    _subscriptionUiState.update { it.copy(isLoading = false, error = context.getString(R.string.err_subscription_not_found)) }
                }
            } catch (e: Exception) {
                _subscriptionUiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }


    // Subscription Tab Functions
    fun updateSubscriptionService(service: String) {
        _subscriptionUiState.update { currentState ->
            currentState.copy(
                serviceName = service,
                serviceError = if (service.isBlank()) context.getString(R.string.err_service_required) else null
            )
        }
    }

    fun updateSubscriptionAmount(amount: String) {
        val filtered = amount.filter { it.isDigit() || it == '.' }
        val decimalCount = filtered.count { it == '.' }
        val validAmount = if (decimalCount <= 1) filtered else _subscriptionUiState.value.amount

        _subscriptionUiState.update { currentState ->
            currentState.copy(amount = validAmount, amountError = validateAmount(validAmount))
        }
    }

    fun updateSubscriptionBillingCycle(cycle: String) {
        _subscriptionUiState.update { currentState ->
            currentState.copy(
                billingCycle = cycle, 
                billingCycleError = null,
                isCustomCycle = cycle == "Custom"
            )
        }
    }

    fun updateSubscriptionCustomCycleCount(count: Int) {
        _subscriptionUiState.update { it.copy(customCycleCount = count) }
    }

    fun updateSubscriptionCustomCycleUnit(unit: String) {
        _subscriptionUiState.update { it.copy(customCycleUnit = unit) }
    }

    fun updateSubscriptionCustomCycleEndDate(date: LocalDate?) {
        _subscriptionUiState.update { it.copy(customCycleEndDate = date) }
    }

    fun updateSubscriptionNextPaymentDate(dateMillis: Long) {
        val instant = Instant.ofEpochMilli(dateMillis)
        val localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()

        _subscriptionUiState.update { currentState ->
            currentState.copy(nextPaymentDate = localDate)
        }
    }

    fun updateSubscriptionCategory(category: String) {
        _subscriptionUiState.update { currentState ->
            currentState.copy(
                category = category,
                subcategory = null,
                categoryError = validateCategory(category)
            )
        }

        updateSubscriptionSubcategories(category)
    }

    private fun updateSubscriptionSubcategories(category: String) {
        viewModelScope.launch {
            val cat = categories.value.find { it.name == category }
            if (cat != null) {
                subcategoryRepository.getSubcategoriesByCategoryId(cat.id).collect {
                    _subscriptionSubcategories.value = it
                }
            } else {
                _subscriptionSubcategories.value = emptyList()
            }
        }
    }

    fun updateSubscriptionSubcategory(subcategory: String?) {
        _subscriptionUiState.update { currentState -> currentState.copy(subcategory = subcategory) }
    }

    fun updateSubscriptionNotes(notes: String) {
        _subscriptionUiState.update { currentState -> currentState.copy(notes = notes) }
    }

    fun updateSubscriptionAccount(
        account: AccountBalanceEntity?
    ) {
        viewModelScope.launch {
            val currency = account?.currency ?: currencyRepository.effectiveBaseCurrencyCode.first()
            _subscriptionUiState.update { currentState -> 
                currentState.copy(
                    selectedAccount = account,
                    currency = currency
                ) 
            }
        }
    }

    fun saveSubscription(onSuccess: () -> Unit) {
        val state = _subscriptionUiState.value
        Log.d("AddViewModel", "saveSubscription called with state: $state")

        // Validate all fields
        val serviceError = if (state.serviceName.isBlank()) context.getString(R.string.err_service_required) else null
        val amountError = validateAmount(state.amount)
        val categoryError = validateCategory(state.category)

        Log.d(
            "AddViewModel",
            "Validation - serviceError: $serviceError, amountError: $amountError, categoryError: $categoryError"
        )

        if (serviceError != null || amountError != null || categoryError != null) {
            _subscriptionUiState.update { currentState ->
                currentState.copy(
                    serviceError = serviceError,
                    amountError = amountError,
                    categoryError = categoryError
                )
            }
            return
        }

        viewModelScope.launch {
            try {
                Log.d("AddViewModel", "Starting to save subscription...")
                _subscriptionUiState.update { it.copy(isLoading = true) }

                val amount = BigDecimal(state.amount)

                val billingCycleToSave = if (state.isCustomCycle) {
                    "custom_${state.customCycleCount}_${state.customCycleUnit.lowercase()}_${state.customCycleEndDate ?: "forever"}"
                } else {
                    state.billingCycle
                }

                if (state.subscriptionId != null) {
                    // Update existing subscription
                    val existingSubscription = subscriptionRepository.getSubscriptionById(state.subscriptionId)
                    if (existingSubscription != null) {
                        val updatedSubscription = existingSubscription.copy(
                            merchantName = state.serviceName.trim(),
                            amount = amount,
                            nextPaymentDate = state.nextPaymentDate,
                            billingCycle = billingCycleToSave,
                            category = state.category,
                            subcategory = state.subcategory,
                            bankName = state.selectedAccount?.bankName,
                            accountLast4 = state.selectedAccount?.accountLast4,
                            currency = state.currency,
                            smsBody = state.notes.takeIf { it.isNotBlank() },
                            updatedAt = java.time.LocalDateTime.now()
                        )
                        updateSubscriptionUseCase.execute(updatedSubscription)
                        Log.d("AddViewModel", "Subscription updated successfully: ${state.subscriptionId}")
                    } else {
                        throw Exception(context.getString(R.string.err_subscription_not_found))
                    }
                } else {
                    // Create new subscription
                    val transactionDate = state.nextPaymentDate.atTime(LocalTime.now())
                    
                    addTransactionUseCase.execute(
                        amount = amount,
                        merchant = state.serviceName.trim(),
                        category = state.category,
                        subcategory = state.subcategory,
                        type = TransactionType.EXPENSE, // Subscriptions are expenses
                        date = transactionDate,
                        notes = state.notes.takeIf { it.isNotBlank() },
                        isRecurring = true, // It is part of a subscription
                        bankName = state.selectedAccount?.bankName,
                        accountLast4 = state.selectedAccount?.accountLast4,
                        currency = state.currency,
                        sourceAccountId = state.selectedAccount?.id,
                        billingCycle = billingCycleToSave,
                        createSubscription = false
                    )

                    val actualNextPaymentDate = SubscriptionUtils.calculateNextPaymentDate(state.nextPaymentDate, billingCycleToSave)
                    Log.d("AddViewModel", "DEBUG_SUBSCRIPTION: fromDate=${state.nextPaymentDate}, billingCycle=$billingCycleToSave, today=${LocalDate.now()}, result=$actualNextPaymentDate")

                    val subscriptionId =
                        addSubscriptionUseCase.execute(
                            merchantName = state.serviceName.trim(),
                            amount = amount,
                            nextPaymentDate = actualNextPaymentDate,
                            billingCycle = billingCycleToSave,
                            category = state.category,
                            subcategory = state.subcategory,
                            bankName = state.selectedAccount?.bankName,
                            accountLast4 = state.selectedAccount?.accountLast4,
                            autoRenewal = false, // Not implemented yet
                            paymentReminder = false, // Not implemented yet
                            currency = state.currency,
                            notes = state.notes.takeIf { it.isNotBlank() },
                            lastPaidDate = state.nextPaymentDate
                        )

                    Log.d("AddViewModel", "Subscription saved successfully with ID: $subscriptionId")
                }
                onSuccess()
            } catch (e: Exception) {
                Log.e("AddViewModel", "Error saving subscription", e)
                e.printStackTrace()
                _subscriptionUiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        error = e.message ?: context.getString(R.string.err_save_subscription)
                    )
                }
            } finally {
                _subscriptionUiState.update { it.copy(isLoading = false) }
            }
        }
    }
    

    // Validation helpers
    private fun validateAmount(amount: String): String? {
        return when {
            amount.isBlank() -> context.getString(R.string.err_amount_required)
            amount.toDoubleOrNull() == null -> context.getString(R.string.err_invalid_amount)
            amount.toDouble() <= 0 -> context.getString(R.string.err_amount_positive)
            else -> null
        }
    }

    private fun validateMerchant(merchant: String): String? {
        return when {
            merchant.isBlank() -> context.getString(R.string.err_merchant_required)
            merchant.length < 2 -> context.getString(R.string.err_too_short)
            else -> null
        }
    }

    private fun validateCategory(category: String): String? {
        return when {
            category.isBlank() -> context.getString(R.string.err_category_required)
            else -> null
        }
    }
}

// UI State Classes
data class AddUiState(val currentTab: Int = 0)

data class TransactionUiState(
    val amount: String = "",
    val amountError: String? = null,
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val merchant: String = "",
    val merchantError: String? = null,
    val category: String = "Miscellaneous",
    val subcategory: String? = null,
    val categoryError: String? = null,
    val date: LocalDateTime = LocalDateTime.now(),
    val notes: String = "",
    val isRecurring: Boolean = false,
    val selectedAccount: AccountBalanceEntity? = null,
    val targetAccount: AccountBalanceEntity? = null,
    val currency: String = "INR",
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedPersonId: Long? = null,
    val dueDate: LocalDateTime? = null
) {
    private val isLoanType: Boolean
        get() = transactionType == TransactionType.LENT ||
            transactionType == TransactionType.BORROWED

    val isValid: Boolean
        get() {
            val baseValid = amount.isNotBlank() &&
                    amount.toDoubleOrNull() != null &&
                    amount.toDouble() > 0 &&
                    category.isNotBlank() &&
                    amountError == null &&
                    categoryError == null
            return if (isLoanType) {
                baseValid && selectedPersonId != null
            } else {
                baseValid && merchant.isNotBlank() && merchantError == null
            }
        }
}

data class SubscriptionUiState(
    val subscriptionId: Long? = null,
    val serviceName: String = "",
    val serviceError: String? = null,
    val amount: String = "",
    val amountError: String? = null,
    val billingCycle: String = "Monthly",
    val billingCycleError: String? = null,
    val nextPaymentDate: LocalDate = LocalDate.now(), // Default to today as "First Payment Date"
    val category: String = "Subscription",
    val subcategory: String? = null,
    val categoryError: String? = null,
    val selectedAccount: AccountBalanceEntity? = null,
    val currency: String = "INR",
    val notes: String = "",
    val isCustomCycle: Boolean = false,
    val customCycleCount: Int = 1,
    val customCycleUnit: String = "month",
    val customCycleEndDate: LocalDate? = null,
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val isValid: Boolean
        get() =
            serviceName.isNotBlank() &&
                    amount.isNotBlank() &&
                    amount.toDoubleOrNull() != null &&
                    amount.toDouble() > 0 &&
                    billingCycle.isNotBlank() &&
                    category.isNotBlank() &&
                    serviceError == null &&
                    amountError == null &&
                    categoryError == null
}
