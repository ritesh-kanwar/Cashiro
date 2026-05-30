package com.ritesh.cashiro.presentation.ui.features.transactions

import android.content.Context
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ritesh.cashiro.data.database.entity.CategoryEntity
import com.ritesh.cashiro.data.database.entity.SubcategoryEntity
import com.ritesh.cashiro.data.database.entity.TransactionEntity
import com.ritesh.cashiro.data.database.entity.TransactionType
import com.ritesh.cashiro.data.repository.CategoryRepository
import com.ritesh.cashiro.data.repository.TransactionRepository
import com.ritesh.cashiro.presentation.common.TimePeriod
import com.ritesh.cashiro.presentation.common.TransactionTypeFilter
import com.ritesh.cashiro.presentation.common.getDateRangeForPeriod
import com.ritesh.cashiro.presentation.common.CurrencyGroupedTotals
import com.ritesh.cashiro.presentation.common.CurrencyTotals
import com.ritesh.cashiro.core.Constants
import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import com.ritesh.cashiro.data.preferences.UserPreferencesRepository
import com.ritesh.cashiro.data.repository.AccountBalanceRepository
import com.ritesh.cashiro.data.repository.CurrencyRepository
import com.ritesh.cashiro.data.repository.SubcategoryRepository
import com.ritesh.cashiro.data.currency.CurrencyConversionService
import com.ritesh.cashiro.utils.CurrencyUtils
import com.ritesh.cashiro.utils.DeviceEncryption
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.net.URLDecoder
import java.net.URLEncoder
import java.time.LocalDate
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val subcategoryRepository: SubcategoryRepository,
    private val accountBalanceRepository: AccountBalanceRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val currencyRepository: CurrencyRepository,
    private val currencyConversionService: CurrencyConversionService,
    private val savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _selectedPeriod = MutableStateFlow(TimePeriod.THIS_MONTH)
    val selectedPeriod: StateFlow<TimePeriod> = _selectedPeriod.asStateFlow()
    
    private val _categoryFilter = MutableStateFlow<Set<String>>(emptySet())
    val categoryFilter: StateFlow<Set<String>> = _categoryFilter.asStateFlow()

    private val _subcategoryFilter = MutableStateFlow<Set<String>>(emptySet())
    val subcategoryFilter: StateFlow<Set<String>> = _subcategoryFilter.asStateFlow()

    private val _amountRangeFilter = MutableStateFlow<Pair<BigDecimal, BigDecimal>?>(null)
    val amountRangeFilter: StateFlow<Pair<BigDecimal, BigDecimal>?> = _amountRangeFilter.asStateFlow()

    private val _accountsFilter = MutableStateFlow<Set<String>>(emptySet())
    val accountsFilter: StateFlow<Set<String>> = _accountsFilter.asStateFlow()

    private val _currenciesFilter = MutableStateFlow<Set<String>>(emptySet())
    val currenciesFilter: StateFlow<Set<String>> = _currenciesFilter.asStateFlow()
    
    private val _transactionTypeFilter = MutableStateFlow<Set<TransactionTypeFilter>>(emptySet())
    val transactionTypeFilter: StateFlow<Set<TransactionTypeFilter>> = _transactionTypeFilter.asStateFlow()
    
    private val _sortOption = MutableStateFlow(SortOption.DATE_NEWEST)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    private val _selectedCurrency = MutableStateFlow("INR") // Default to INR
    val selectedCurrency: StateFlow<String> = _selectedCurrency.asStateFlow()

    val baseCurrency: StateFlow<String> = currencyRepository.baseCurrencyCode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "INR"
        )

    // Store custom date range as epoch days to survive process death
    // Stored as Pair<Long, Long> (startEpochDay, endEpochDay) in SavedStateHandle
    private val _customDateRangeEpochDays = savedStateHandle.getStateFlow<Pair<Long, Long>?>("customDateRange", null)

    // Expose as LocalDate pair for convenience
    val customDateRange: StateFlow<Pair<LocalDate, LocalDate>?> = _customDateRangeEpochDays
        .map { epochDays ->
            epochDays?.let { (startEpochDay, endEpochDay) ->
                LocalDate.ofEpochDay(startEpochDay) to LocalDate.ofEpochDay(endEpochDay)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    private val _uiState = MutableStateFlow(TransactionsUiState())
    val uiState: StateFlow<TransactionsUiState> = _uiState.asStateFlow()

    private val _canLoadData = MutableStateFlow(false)
    
    private val _currencyGroupedTotals = MutableStateFlow(CurrencyGroupedTotals())
    val currencyGroupedTotals: StateFlow<CurrencyGroupedTotals> = _currencyGroupedTotals.asStateFlow()

    // Available currencies for the selected time period and all accounts
    val availableCurrencies: StateFlow<List<String>> = combine(
        selectedPeriod,
        customDateRange,
        accountBalanceRepository.getAllLatestBalances()
    ) { period, customRange, accounts ->
        Triple(period, customRange, accounts)
    }.flatMapLatest { (period, customRange, accounts) ->
        val accountCurrencies = accounts.map { it.currency }.distinct()
        val transactionCurrenciesFlow = if (period == TimePeriod.ALL) {
            transactionRepository.getAllCurrencies()
        } else if (period == TimePeriod.CUSTOM && customRange != null) {
            val (startDate, endDate) = customRange
            val startDateTime = startDate.atStartOfDay()
            val endDateTime = endDate.atTime(23, 59, 59)
            transactionRepository.getCurrenciesForPeriod(startDateTime, endDateTime)
        } else {
            val dateRange = getDateRangeForPeriod(period)
            if (dateRange != null) {
                val (startDate, endDate) = dateRange
                val startDateTime = startDate.atStartOfDay()
                val endDateTime = endDate.atTime(23, 59, 59)
                transactionRepository.getCurrenciesForPeriod(startDateTime, endDateTime)
            } else {
                transactionRepository.getAllCurrencies()
            }
        }
        transactionCurrenciesFlow.map { txCurrencies ->
            (txCurrencies + accountCurrencies).distinct().sortedWith { a, b ->
                when {
                    a == "INR" -> -1 // INR first
                    b == "INR" -> 1
                    else -> a.compareTo(b) // Alphabetical for others
                }
            }
        }
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Computed property for current selected currency totals
    val filteredTotals: StateFlow<FilteredTotals> = _uiState.map { state ->
        var income = BigDecimal.ZERO
        var expenses = BigDecimal.ZERO
        var credit = BigDecimal.ZERO
        var transfer = BigDecimal.ZERO
        var investment = BigDecimal.ZERO

        state.transactions.forEach { tx ->
            val valAmount = state.convertedAmounts[tx.id] ?: tx.amount
            when (tx.transactionType) {
                TransactionType.INCOME -> income += valAmount
                TransactionType.EXPENSE -> expenses += valAmount
                TransactionType.CREDIT -> credit += valAmount
                TransactionType.TRANSFER -> transfer += valAmount
                TransactionType.INVESTMENT -> investment += valAmount
                else -> {}
            }
        }
        val netBalance = income - expenses
        FilteredTotals(
            income = income,
            expenses = expenses,
            credit = credit,
            transfer = transfer,
            investment = investment,
            netBalance = netBalance,
            transactionCount = state.transactions.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FilteredTotals()
    )
    
    private val _deletedTransaction = MutableStateFlow<TransactionEntity?>(null)
    val deletedTransaction: StateFlow<TransactionEntity?> = _deletedTransaction.asStateFlow()
    
    // Selection mode state for bulk deletion
    private val _selectionMode = MutableStateFlow(false)
    val selectionMode: StateFlow<Boolean> = _selectionMode.asStateFlow()
    
    private val _selectedTransactionIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedTransactionIds: StateFlow<Set<Long>> = _selectedTransactionIds.asStateFlow()
    
    private val _deletedTransactions = MutableStateFlow<List<TransactionEntity>?>(null)
    val deletedTransactions: StateFlow<List<TransactionEntity>?> = _deletedTransactions.asStateFlow()
    
    // Track if initial filters have been applied to prevent resetting on back navigation
    private var hasAppliedInitialFilters = false
    
    // Categories flow - will be used to map category names to colors
    val categories: StateFlow<Map<String, CategoryEntity>> = categoryRepository.getAllCategories()
        .map { categoryList ->
            categoryList.associateBy { it.name }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )
    
    // Subcategories flow - will be used to map subcategory names to entities
    val subcategories: StateFlow<Map<String, SubcategoryEntity>> = subcategoryRepository.getAllSubcategories()
        .map { subcategoryList ->
            subcategoryList.associateBy { it.name }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    val accountsMap: StateFlow<Map<String, AccountBalanceEntity>> = accountBalanceRepository.getAllLatestBalances()
        .map { accountList ->
            accountList.associateBy { "${it.bankName}_${it.accountLast4}" }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )
        
    val maxTransactionAmount: StateFlow<Float> = transactionRepository.getAllTransactions()
        .map { transactions ->
            transactions.map { it.amount }.maxOrNull()?.toFloat() ?: 0f
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0f
        )
    
    // SMS scan period for info banner
    val smsScanMonths: StateFlow<Int> = userPreferencesRepository.smsScanMonths
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 3
        )
    
    fun isShowingLimitedData(): Boolean {
        val currentPeriod = _selectedPeriod.value
        val scanMonthsValue = smsScanMonths.value

        return when (currentPeriod) {
            TimePeriod.ALL -> true  // Always show for "All Time"
            TimePeriod.CURRENT_FY -> {
                // Check if FY start is before scan period
                val dateRange = getDateRangeForPeriod(TimePeriod.CURRENT_FY)
                if (dateRange != null) {
                    val (fyStart, _) = dateRange
                    val scanStart = LocalDate.now().minusMonths(scanMonthsValue.toLong())
                    fyStart.isBefore(scanStart)
                } else {
                    false
                }
            }
            TimePeriod.CUSTOM -> {
                // Check if custom range start is before scan period
                val customRange = customDateRange.value
                if (customRange != null) {
                    val (startDate, _) = customRange
                    val scanStart = LocalDate.now().minusMonths(scanMonthsValue.toLong())
                    startDate.isBefore(scanStart)
                } else {
                    false
                }
            }
            else -> false
        }
    }
    
    init {
        // Observe the main account's currency dynamically
        viewModelScope.launch {
            var lastBaseCurrency: String? = null
            currencyRepository.baseCurrencyCode.collectLatest { mainAccountCurrency ->
                if (lastBaseCurrency == null || mainAccountCurrency != lastBaseCurrency) {
                    _selectedCurrency.value = mainAccountCurrency
                    lastBaseCurrency = mainAccountCurrency
                }
            }
        }

        _canLoadData
            .filter { it }
            .flatMapLatest {
                merge(
                    searchQuery.debounce(300).map { "search" },
                    selectedPeriod.map { "period" },
                    categoryFilter.map { "category" },
                    subcategoryFilter.map { "subcategory" },
                    amountRangeFilter.map { "amount" },
                    accountsFilter.map { "accounts" },
                    currenciesFilter.map { "currencies" },
                    transactionTypeFilter.map { "typeFilter" },
                    selectedCurrency.map { "currency" },
                    sortOption.map { "sort" },
                    customDateRange.map { "customDate" },
                    currencyRepository.baseCurrencyCode.map { "baseCurrency" }
                )
            }
            .transformLatest { trigger ->
                // Get current values from all StateFlows
                val query = searchQuery.value
                val period = selectedPeriod.value
                val category = categoryFilter.value
                val subcategory = subcategoryFilter.value
                val amountRange = amountRangeFilter.value
                val accounts = accountsFilter.value
                val filterCurrencies = currenciesFilter.value
                val typeFilter = transactionTypeFilter.value
                val currency = selectedCurrency.value
                val baseCurrencyCode = currencyRepository.baseCurrencyCode.first()
                val sort = sortOption.value
 
                 // Get filtered transactions
                 getFilteredTransactions(query, period, category, subcategory, amountRange, accounts, filterCurrencies, typeFilter)
                     .collect { transactions ->
                         // No longer filtering by selectedCurrency. Show all unless explicitly filtered via filter sheet
                         val currencyFilteredTransactions = transactions
                         
                         // Calculate converted amounts for shown transactions if transaction currency differs from base (main) currency
                         val converted = currencyFilteredTransactions.filter { !it.currency.equals(baseCurrencyCode, ignoreCase = true) }
                             .associate { tx ->
                                 tx.id to currencyConversionService.convertAmount(tx.amount, tx.currency, baseCurrencyCode)
                             }
                         
                         emit(Pair(sortTransactions(currencyFilteredTransactions, sort), converted))
                     }
            }
            .onEach { (transactions, converted) ->
                _uiState.value = _uiState.value.copy(
                    transactions = transactions,
                    groupedTransactions = groupTransactionsByDate(transactions),
                    convertedAmounts = converted,
                    isLoading = false
                )
                // Calculate totals for filtered transactions
                _currencyGroupedTotals.value = calculateCurrencyGroupedTotals(transactions)

                // Auto-select primary currency if not already selected or if current currency no longer exists
                val currentCurrency = selectedCurrency.value
                if (!_currencyGroupedTotals.value.availableCurrencies.contains(currentCurrency) && _currencyGroupedTotals.value.hasAnyCurrency()) {
                    _selectedCurrency.value = _currencyGroupedTotals.value.getPrimaryCurrency(currentCurrency)
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Call this when the transition is finished to start loading heavy data.
     */
    fun startLoading() {
        _canLoadData.value = true
    }
    
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    fun selectPeriod(period: TimePeriod) {
        _selectedPeriod.value = period
    }
    
    fun setCategoryFilter(category: String) {
        val current = _categoryFilter.value
        _categoryFilter.value = if (current.contains(category)) {
            current - category
        } else {
            current + category
        }
    }
    
    fun clearCategoryFilter() {
        _categoryFilter.value = emptySet()
    }

    fun setSubcategoryFilter(subcategory: String?) {
        if (subcategory == null) {
            _subcategoryFilter.value = emptySet()
            return
        }
        val current = _subcategoryFilter.value
        _subcategoryFilter.value = if (current.contains(subcategory)) {
            current - subcategory
        } else {
            current + subcategory
        }
    }

    fun toggleAccountFilter(accountKey: String) {
        val current = _accountsFilter.value
        _accountsFilter.value = if (current.contains(accountKey)) {
            current - accountKey
        } else {
            current + accountKey
        }
    }

    fun toggleCurrencyFilter(currencyCode: String) {
        val current = _currenciesFilter.value
        _currenciesFilter.value = if (current.contains(currencyCode)) {
            current - currencyCode
        } else {
            current + currencyCode
        }
    }

    fun setAmountRangeFilter(min: BigDecimal, max: BigDecimal) {
        _amountRangeFilter.value = Pair(min, max)
    }

    fun clearExtendedFilters() {
        _subcategoryFilter.value = emptySet()
        _amountRangeFilter.value = null
        _accountsFilter.value = emptySet()
        _currenciesFilter.value = emptySet()
    }
    
    fun setTransactionTypeFilter(filter: TransactionTypeFilter) {
        if (filter == TransactionTypeFilter.ALL) {
            _transactionTypeFilter.value = emptySet()
            return
        }
        val current = _transactionTypeFilter.value
        _transactionTypeFilter.value = if (current.contains(filter)) {
            current - filter
        } else {
            current + filter
        }
    }
    
    fun setSortOption(option: SortOption) {
        _sortOption.value = option
    }

    fun selectCurrency(currency: String) {
        _selectedCurrency.value = currency
    }

    /**
     * Sets a custom date range filter and switches the period to CUSTOM.
     * Date range is persisted in SavedStateHandle to survive process death.
     *
     * @param startDate The start date (inclusive)
     * @param endDate The end date (inclusive)
     * @throws IllegalArgumentException if startDate > endDate
     */
    fun setCustomDateRange(startDate: LocalDate, endDate: LocalDate) {
        require(startDate <= endDate) {
            "Start date ($startDate) must be before or equal to end date ($endDate)"
        }
        // Store as epoch days for process death survival
        savedStateHandle["customDateRange"] = startDate.toEpochDay() to endDate.toEpochDay()
        _selectedPeriod.value = TimePeriod.CUSTOM
    }

    /**
     * Clears the custom date range and resets to THIS_MONTH period.
     * Always safe to call - ensures we never have CUSTOM period with null dates.
     */
    fun clearCustomDateRange() {
        savedStateHandle["customDateRange"] = null
        // Always reset to a valid period to prevent CUSTOM with null dates
        if (_selectedPeriod.value == TimePeriod.CUSTOM) {
            _selectedPeriod.value = TimePeriod.THIS_MONTH
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            _deletedTransaction.value = transaction
            transactionRepository.deleteTransaction(transaction)
        }
    }
    
    fun undoDelete() {
        _deletedTransaction.value?.let { transaction ->
            viewModelScope.launch {
                transactionRepository.undoDeleteTransaction(transaction)
                _deletedTransaction.value = null
            }
        }
    }
    
    fun undoDeleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            transactionRepository.undoDeleteTransaction(transaction)
        }
    }
    
    fun clearDeletedTransaction() {
        _deletedTransaction.value = null
    }
    
    // Selection mode methods for bulk deletion
    fun toggleSelectionMode() {
        _selectionMode.value = !_selectionMode.value
        if (!_selectionMode.value) {
            // Clear selections when exiting selection mode
            _selectedTransactionIds.value = emptySet()
        }
    }
    
    fun toggleTransactionSelection(transactionId: Long) {
        _selectedTransactionIds.value = if (_selectedTransactionIds.value.contains(transactionId)) {
            _selectedTransactionIds.value - transactionId
        } else {
            _selectedTransactionIds.value + transactionId
        }
    }
    
    fun selectAllTransactions() {
        val allTransactionIds = _uiState.value.transactions.map { it.id }.toSet()
        _selectedTransactionIds.value = allTransactionIds
    }
    
    fun clearSelection() {
        _selectedTransactionIds.value = emptySet()
    }
    
    fun deleteSelectedTransactions() {
        viewModelScope.launch {
            val selectedIds = _selectedTransactionIds.value
            if (selectedIds.isEmpty()) return@launch
            
            val transactionsToDelete = _uiState.value.transactions.filter { it.id in selectedIds }
            _deletedTransactions.value = transactionsToDelete
            transactionRepository.deleteTransactions(transactionsToDelete)
            
            // Clear selection and exit selection mode
            _selectedTransactionIds.value = emptySet()
            _selectionMode.value = false
        }
    }
    
    fun undoDeleteTransactions(transactions: List<TransactionEntity>) {
        viewModelScope.launch {
            transactionRepository.undoDeleteTransactions(transactions)
        }
    }
    
    fun clearDeletedTransactions() {
        _deletedTransactions.value = null
    }

    fun resetFilters() {
        hasAppliedInitialFilters = false
        clearCategoryFilter()
        clearExtendedFilters()
        updateSearchQuery("")
        clearCustomDateRange()
        selectPeriod(TimePeriod.THIS_MONTH)
        setTransactionTypeFilter(TransactionTypeFilter.ALL)
        setSortOption(SortOption.DATE_NEWEST)
        // Don't reset currency as it might be user preference
    }
    
    fun applyInitialFilters(
        category: String?,
        merchant: String?,
        period: String?,
        currency: String?,
        type: String?
    ) {
        if (!hasAppliedInitialFilters) {
            // Only apply filters once, when first navigating to the screen
            clearCategoryFilter()
            updateSearchQuery("")
            selectPeriod(TimePeriod.THIS_MONTH)
            setTransactionTypeFilter(TransactionTypeFilter.ALL)
            setSortOption(SortOption.DATE_NEWEST)

            category?.let {
                val decoded = if (it.contains("+") || it.contains("%")) {
                    URLDecoder.decode(it, "UTF-8")
                } else it
                _categoryFilter.value = setOf(decoded)
            }

            merchant?.let {
                val decoded = if (it.contains("+") || it.contains("%")) {
                    URLDecoder.decode(it, "UTF-8")
                } else it
                updateSearchQuery(decoded)
            }

            period?.let { periodName ->
                val timePeriod = when (periodName) {
                    "THIS_MONTH" -> TimePeriod.THIS_MONTH
                    "LAST_MONTH" -> TimePeriod.LAST_MONTH
                    "CURRENT_FY" -> TimePeriod.CURRENT_FY
                    "ALL" -> TimePeriod.ALL
                    else -> null
                }
                timePeriod?.let { selectPeriod(it) }
            }

            type?.let { typeName ->
                val typeFilter = when (typeName) {
                    "INCOME" -> TransactionTypeFilter.INCOME
                    "EXPENSE" -> TransactionTypeFilter.EXPENSE
                    "CREDIT" -> TransactionTypeFilter.CREDIT
                    "TRANSFER" -> TransactionTypeFilter.TRANSFER
                    "INVESTMENT" -> TransactionTypeFilter.INVESTMENT
                    else -> TransactionTypeFilter.ALL
                }
                _transactionTypeFilter.value = if (typeFilter == TransactionTypeFilter.ALL) emptySet() else setOf(typeFilter)
            }

            // Only set currency if it's provided (from navigation)
            currency?.let { selectCurrency(it) }

            hasAppliedInitialFilters = true
        }
    }

    fun applyNavigationFilters(
        category: String?,
        merchant: String?,
        period: String?,
        currency: String?,
        type: String?
    ) {
        // This function can be called multiple times for navigation updates
        clearCategoryFilter()
        updateSearchQuery("")
        selectPeriod(TimePeriod.THIS_MONTH)
        setTransactionTypeFilter(TransactionTypeFilter.ALL)
        setSortOption(SortOption.DATE_NEWEST)

        category?.let {
            val decoded = if (it.contains("+") || it.contains("%")) {
                URLDecoder.decode(it, "UTF-8")
            } else it
            _categoryFilter.value = setOf(decoded)
        }

        merchant?.let {
            val decoded = if (it.contains("+") || it.contains("%")) {
                URLDecoder.decode(it, "UTF-8")
            } else it
            updateSearchQuery(decoded)
        }

        period?.let { periodName ->
            val timePeriod = when (periodName) {
                "THIS_MONTH" -> TimePeriod.THIS_MONTH
                "LAST_MONTH" -> TimePeriod.LAST_MONTH
                "CURRENT_FY" -> TimePeriod.CURRENT_FY
                "ALL" -> TimePeriod.ALL
                else -> null
            }
            timePeriod?.let { selectPeriod(it) }
        }

        type?.let { typeName ->
            val typeFilter = when (typeName) {
                "INCOME" -> TransactionTypeFilter.INCOME
                "EXPENSE" -> TransactionTypeFilter.EXPENSE
                "CREDIT" -> TransactionTypeFilter.CREDIT
                "TRANSFER" -> TransactionTypeFilter.TRANSFER
                "INVESTMENT" -> TransactionTypeFilter.INVESTMENT
                else -> TransactionTypeFilter.ALL
            }
            _transactionTypeFilter.value = if (typeFilter == TransactionTypeFilter.ALL) emptySet() else setOf(typeFilter)
        }

        // Only set currency if it's provided (from navigation)
        currency?.let { selectCurrency(it) }
    }
    
    private fun getFilteredTransactions(
        searchQuery: String,
        period: TimePeriod,
        category: Set<String>,
        subcategory: Set<String>,
        amountRange: Pair<BigDecimal, BigDecimal>?,
        accounts: Set<String>,
        filterCurrencies: Set<String>,
        typeFilter: Set<TransactionTypeFilter>
    ): Flow<List<TransactionEntity>> {
        // Start with the base flow
        val baseFlow = transactionRepository.getAllTransactions()
        
        // Apply period filter
        val periodFilteredFlow = when (period) {
            TimePeriod.ALL -> baseFlow
            TimePeriod.CUSTOM -> {
                val customRange = customDateRange.value
                // Guard against invalid state: CUSTOM period must have a date range
                // This should never happen due to clearCustomDateRange() logic, but be defensive
                if (customRange == null) {
                    Log.e("TransactionsViewModel",
                        "CUSTOM period selected but no date range set - falling back to THIS_MONTH")
                    // Auto-correct the invalid state
                    _selectedPeriod.value = TimePeriod.THIS_MONTH
                    val (startDate, endDate) = getDateRangeForPeriod(TimePeriod.THIS_MONTH)!!
                    val startDateTime = startDate.atStartOfDay()
                    val endDateTime = endDate.atTime(23, 59, 59)
                    baseFlow.map { transactions ->
                        transactions.filter { it.dateTime in startDateTime..endDateTime }
                    }
                } else {
                    val (startDate, endDate) = customRange
                    val startDateTime = startDate.atStartOfDay()
                    val endDateTime = endDate.atTime(23, 59, 59)

                    baseFlow.map { transactions ->
                        transactions.filter { it.dateTime in startDateTime..endDateTime }
                    }
                }
            }
            else -> {
                val dateRange = getDateRangeForPeriod(period)
                if (dateRange != null) {
                    val (startDate, endDate) = dateRange
                    val startDateTime = startDate.atStartOfDay()
                    val endDateTime = endDate.atTime(23, 59, 59)

                    baseFlow.map { transactions ->
                        transactions.filter { it.dateTime in startDateTime..endDateTime }
                    }
                } else {
                    baseFlow
                }
            }
        }
        
        // Apply transaction type filter
        val typeFilteredFlow = periodFilteredFlow.map { transactions ->
            if (typeFilter.isEmpty()) {
                transactions
            } else {
                transactions.filter { tx ->
                    typeFilter.any { filter ->
                        when (filter) {
                            TransactionTypeFilter.ALL -> true
                            TransactionTypeFilter.INCOME -> tx.transactionType == TransactionType.INCOME
                            TransactionTypeFilter.EXPENSE -> tx.transactionType == TransactionType.EXPENSE
                            TransactionTypeFilter.CREDIT -> tx.transactionType == TransactionType.CREDIT
                            TransactionTypeFilter.TRANSFER -> tx.transactionType == TransactionType.TRANSFER
                            TransactionTypeFilter.INVESTMENT -> tx.transactionType == TransactionType.INVESTMENT
                        }
                    }
                }
            }
        }

        // Apply new extended filters (Category, Subcategory, Amount Range, Accounts, Currencies)
        val extendedFilteredFlow = typeFilteredFlow.map { transactions ->
            transactions.filter { tx ->
                val matchesCategory = category.isEmpty() || (tx.category != null && category.contains(tx.category))
                val matchesSubcat = subcategory.isEmpty() || (tx.subcategory != null && subcategory.contains(tx.subcategory))
                val matchesAmount = amountRange == null || (tx.amount >= amountRange.first && tx.amount <= amountRange.second)
                val txAccountKey = "${tx.bankName}_${tx.accountNumber}"
                val matchesAccount = accounts.isEmpty() || accounts.contains(txAccountKey)
                val matchesCurrencyOpts = filterCurrencies.isEmpty() || filterCurrencies.contains(tx.currency)
                
                matchesCategory && matchesSubcat && matchesAmount && matchesAccount && matchesCurrencyOpts
            }
        }
        
        // Apply search filter
        return if (searchQuery.isBlank()) {
            extendedFilteredFlow
        } else {
            extendedFilteredFlow.map { transactions ->
                transactions.filter { transaction ->
                    // Check merchant name and description
                    val matchesMerchant = transaction.merchantName.contains(searchQuery, ignoreCase = true)
                    val matchesDescription = transaction.description?.contains(searchQuery, ignoreCase = true) == true
                    
                    // Check SMS body (full text search)
                    val matchesSmsBody = transaction.smsBody?.contains(searchQuery, ignoreCase = true) == true
                    
                    // Check if search query matches amount
                    val matchesAmount = try {
                        // Remove commas and spaces from search query for number parsing
                        val cleanedQuery = searchQuery.replace(",", "").replace(" ", "").trim()
                        
                        // Check if it's a valid number and matches the amount
                        if (cleanedQuery.isNotEmpty() && cleanedQuery.all { it.isDigit() || it == '.' }) {
                            val amountString = transaction.amount.toPlainString()
                            // Support both exact and partial matches
                            amountString.contains(cleanedQuery) || 
                            // Also match formatted amount (e.g., "1,000" matches "1000")
                            amountString.replace(",", "").contains(cleanedQuery)
                        } else {
                            false
                        }
                    } catch (e: Exception) {
                        false
                    }
                    
                    matchesMerchant || matchesDescription || matchesSmsBody || matchesAmount
                }
            }
        }
    }
    
    private fun sortTransactions(transactions: List<TransactionEntity>, sortOption: SortOption): List<TransactionEntity> {
        return when (sortOption) {
            SortOption.DATE_NEWEST -> transactions.sortedByDescending { it.dateTime }
            SortOption.DATE_OLDEST -> transactions.sortedBy { it.dateTime }
            SortOption.AMOUNT_HIGHEST -> transactions.sortedByDescending { it.amount }
            SortOption.AMOUNT_LOWEST -> transactions.sortedBy { it.amount }
            SortOption.MERCHANT_AZ -> transactions.sortedBy { it.merchantName.lowercase() }
            SortOption.MERCHANT_ZA -> transactions.sortedByDescending { it.merchantName.lowercase() }
        }
    }
    
    private fun groupTransactionsByDate(
        transactions: List<TransactionEntity>
    ): Map<DateGroup, List<TransactionEntity>> {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        val weekStart = today.minusWeeks(1)
        
        return transactions.groupBy { transaction ->
            val transactionDate = transaction.dateTime.toLocalDate()
            when {
                transactionDate == today -> DateGroup.TODAY
                transactionDate == yesterday -> DateGroup.YESTERDAY
                transactionDate > weekStart -> DateGroup.THIS_WEEK
                else -> DateGroup.EARLIER
            }
        }
    }
    
    private fun calculateCurrencyGroupedTotals(transactions: List<TransactionEntity>): CurrencyGroupedTotals {
        // Group transactions by currency
        val transactionsByCurrency = transactions.groupBy { it.currency }

        val totalsByCurrency = transactionsByCurrency.mapValues { (currency, currencyTransactions) ->
            val income = currencyTransactions
                .filter { it.transactionType == TransactionType.INCOME }
                .fold(BigDecimal.ZERO) { acc, tx -> acc + tx.amount }

            val expenses = currencyTransactions
                .filter { it.transactionType == TransactionType.EXPENSE }
                .fold(BigDecimal.ZERO) { acc, tx -> acc + tx.amount }

            val credit = currencyTransactions
                .filter { it.transactionType == TransactionType.CREDIT }
                .fold(BigDecimal.ZERO) { acc, tx -> acc + tx.amount }

            val transfer = currencyTransactions
                .filter { it.transactionType == TransactionType.TRANSFER }
                .fold(BigDecimal.ZERO) { acc, tx -> acc + tx.amount }

            val investment = currencyTransactions
                .filter { it.transactionType == TransactionType.INVESTMENT }
                .fold(BigDecimal.ZERO) { acc, tx -> acc + tx.amount }

            CurrencyTotals(
                currency = currency,
                income = income,
                expenses = expenses,
                credit = credit,
                transfer = transfer,
                investment = investment,
                transactionCount = currencyTransactions.size
            )
        }

        val filteredAvailableCurrencies = CurrencyUtils.sortCurrencies(
            totalsByCurrency.keys.toList()
        )

        return CurrencyGroupedTotals(
            totalsByCurrency = totalsByCurrency,
            availableCurrencies = filteredAvailableCurrencies,
            transactionCount = transactions.size
        )
    }
    
    fun getReportUrl(transaction: TransactionEntity): String {
        val smsBody = transaction.smsBody ?: "Transaction: ${transaction.merchantName} - ${transaction.amount}"
        val sender = transaction.smsSender ?: "Unknown Sender"
        val bank = transaction.bankName ?: "Manual"

        val issueTitle = "[Parsing Issue] ${transaction.merchantName} - ${transaction.amount}"
        val issueBody = """
            ### Transaction Details
            - **Merchant:** ${transaction.merchantName}
            - **Amount:** ${transaction.amount} ${transaction.currency}
            - **Type:** ${transaction.transactionType}
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
    
}
