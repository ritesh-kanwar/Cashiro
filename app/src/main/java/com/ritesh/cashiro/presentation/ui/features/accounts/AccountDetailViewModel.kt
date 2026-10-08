package com.ritesh.cashiro.presentation.ui.features.accounts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ritesh.cashiro.data.currency.CurrencyConversionService
import com.ritesh.cashiro.data.database.entity.TransactionType
import com.ritesh.cashiro.data.repository.AccountBalanceRepository
import com.ritesh.cashiro.data.repository.CategoryRepository
import com.ritesh.cashiro.data.repository.SubcategoryRepository
import com.ritesh.cashiro.data.repository.TransactionRepository
import com.ritesh.cashiro.data.repository.CurrencyRepository
import com.ritesh.cashiro.data.repository.LendBorrowRepository
import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import com.ritesh.cashiro.data.database.entity.TransactionEntity
import com.ritesh.cashiro.domain.model.LendBorrowPerson
import com.ritesh.cashiro.domain.model.LendBorrowTransactionItem
import com.ritesh.cashiro.domain.model.PersonInfo
import com.ritesh.cashiro.presentation.ui.components.BalancePoint
import com.ritesh.cashiro.utils.CurrencyFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDateTime
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AccountDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val transactionRepository: TransactionRepository,
    private val accountBalanceRepository: AccountBalanceRepository,
    private val categoryRepository: CategoryRepository,
    private val subcategoryRepository: SubcategoryRepository,
    private val currencyRepository: CurrencyRepository,
    private val currencyConversionService: CurrencyConversionService,
    private val lendBorrowRepository: LendBorrowRepository
) : ViewModel() {
    
    private val bankName: String = savedStateHandle.get<String>("bankName") ?: ""
    private val accountLast4: String = savedStateHandle.get<String>("accountLast4") ?: ""
    
    private val _uiState = MutableStateFlow(AccountDetailUiState())
    val uiState: StateFlow<AccountDetailUiState> = _uiState.asStateFlow()

    val categoriesMap = categoryRepository.getAllCategories()
        .map { cats -> cats.associateBy { it.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val subcategoriesMap = subcategoryRepository.getAllSubcategories()
        .map { subcats -> subcats.associateBy { it.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
    
    private val _selectedDateRange = MutableStateFlow(DateRange.LAST_30_DAYS)
    val selectedDateRange: StateFlow<DateRange> = _selectedDateRange.asStateFlow()
    
    init {
        loadAccountData()
        observeTransactions()
        observeBalanceHistory()
        observeBalanceChartData()
    }
    
    private fun loadAccountData() {
        _uiState.update { it.copy(
            bankName = bankName,
            accountLast4 = accountLast4,
            isLoading = true
        ) }
    }
    
    private fun observeTransactions() {
        viewModelScope.launch {
            combine(
                selectedDateRange,
                transactionRepository.getTransactionsByAccount(bankName, accountLast4),
                currencyRepository.effectiveBaseCurrencyCode,
                accountBalanceRepository.getLatestBalanceFlow(bankName, accountLast4),
                currencyConversionService.rateChangeTrigger,
                lendBorrowRepository.getAllTransactions(),
                lendBorrowRepository.getPersons()
            ) { args: Array<Any?> ->
                val dateRange = args[0] as DateRange
                val allTransactions = args[1] as List<TransactionEntity>
                val mainCurrency = args[2] as String
                val latestBalance = args[3] as AccountBalanceEntity?
                val lbTransactions = args[5] as List<LendBorrowTransactionItem>
                val persons = args[6] as List<LendBorrowPerson>

                val (startDate, endDate) = getDateRangeValues(dateRange)

                val filteredTransactions = if (dateRange == DateRange.ALL_TIME) {
                    allTransactions
                } else {
                    allTransactions.filter { transaction ->
                        transaction.dateTime.isAfter(startDate) &&
                        transaction.dateTime.isBefore(endDate)
                    }
                }

                val accountPrimaryCurrency = latestBalance?.currency ?: getPrimaryCurrencyForAccount(bankName)
                val hasMultipleCurrencies = filteredTransactions.map { it.currency }.distinct().size > 1

                // Refresh exchange rates if we have multiple currencies
                if (hasMultipleCurrencies) {
                    val accountCurrencies = filteredTransactions.map { it.currency }.distinct()
                    currencyConversionService.refreshExchangeRatesForAccount(accountCurrencies)
                }

                // Calculate total income and expenses converted to the effective base currency
                var totalIncome = BigDecimal.ZERO
                var totalExpenses = BigDecimal.ZERO

                filteredTransactions.forEach { transaction ->
                    val convertedAmount = if (transaction.currency != mainCurrency) {
                        currencyConversionService.convertAmountOrNull(
                            amount = transaction.amount,
                            fromCurrency = transaction.currency,
                            toCurrency = mainCurrency
                        ) ?: return@forEach
                    } else {
                        transaction.amount
                    }

                    if (transaction.transactionType == TransactionType.INCOME || transaction.transactionType == TransactionType.BORROWED) {
                        totalIncome += convertedAmount
                    } else if (transaction.transactionType == TransactionType.TRANSFER) {
                        val isSender = transaction.bankName == bankName && 
                            (transaction.accountNumber == accountLast4 || transaction.fromAccount == accountLast4)
                        val isReceiver = !isSender && transaction.toAccount == accountLast4

                        if (isReceiver) {
                            totalIncome += convertedAmount
                        } else if (isSender) {
                            totalExpenses += convertedAmount
                        }
                    } else {
                        totalExpenses += convertedAmount
                    }
                }

                // Create person mapping
                val personMap = persons.associateBy { it.id }
                val transactionPersonMapping = lbTransactions
                    .filter { it.transactionId != null }
                    .associate { lb ->
                        val person = personMap[lb.personId]
                        lb.transactionId!! to PersonInfo(
                            name = person?.name ?: lb.title,
                            color = person?.color ?: "#4CAF50",
                            avatar = person?.avatar
                        )
                    }

                // Calculate converted amounts for the UI (TransactionItem) based on Main App Currency
                val converted = filteredTransactions
                    .filter { it.currency != mainCurrency }
                    .mapNotNull { tx ->
                        currencyConversionService.convertAmountOrNull(tx.amount, tx.currency, mainCurrency)?.let { tx.id to it }
                    }.toMap()

                _uiState.update { state ->
                    state.copy(
                        transactions = filteredTransactions,
                        totalIncome = totalIncome,
                        totalExpenses = totalExpenses,
                        netBalance = totalIncome - totalExpenses,
                        primaryCurrency = accountPrimaryCurrency,
                        baseCurrency = mainCurrency,
                        hasMultipleCurrencies = hasMultipleCurrencies,
                        convertedAmounts = converted,
                        transactionPersonMapping = transactionPersonMapping,
                        isLoading = false
                    )
                }
            }.collectLatest { }
        }
    }
    
    private fun observeBalanceHistory() {
        viewModelScope.launch {
            accountBalanceRepository.getLatestBalanceFlow(bankName, accountLast4)
                .collect { latestBalance ->
                    _uiState.update { state ->
                        state.copy(currentBalance = latestBalance)
                    }
                }
        }
        
        viewModelScope.launch {
            selectedDateRange.flatMapLatest { dateRange ->
                val (startDate, endDate) = getDateRangeValues(dateRange)
                accountBalanceRepository.getBalanceHistory(
                    bankName, 
                    accountLast4,
                    startDate,
                    endDate
                )
            }.collect { balanceHistory ->
                _uiState.update { state ->
                    state.copy(balanceHistory = balanceHistory)
                }
            }
        }
    }
    
    private fun observeBalanceChartData() {
        viewModelScope.launch {
            combine(selectedDateRange, currencyConversionService.rateChangeTrigger) { dateRange, _ -> dateRange }
                .flatMapLatest { dateRange ->
                val (startDate, endDate) = getDateRangeValues(dateRange)

                val chartStartDate = when (dateRange) {
                    DateRange.LAST_7_DAYS -> endDate.minusDays(14)
                    DateRange.LAST_30_DAYS -> endDate.minusMonths(2)
                    DateRange.LAST_3_MONTHS -> endDate.minusMonths(4)
                    DateRange.LAST_6_MONTHS -> endDate.minusMonths(8)
                    DateRange.LAST_YEAR -> endDate.minusMonths(15)
                    DateRange.ALL_TIME -> LocalDateTime.of(2000, 1, 1, 0, 0)
                }

                accountBalanceRepository.getBalanceHistory(
                    bankName,
                    accountLast4,
                    chartStartDate,
                    endDate
                )
            }.collect { balanceHistory ->
                val effectiveCurrency = currencyRepository.effectiveBaseCurrencyCode.first()

                val chartData = balanceHistory.mapNotNull { entity ->
                    val convertedBalance = if (entity.currency != effectiveCurrency) {
                        currencyConversionService.convertAmountOrNull(
                            entity.balance,
                            entity.currency,
                            effectiveCurrency
                        ) ?: return@mapNotNull null
                    } else entity.balance

                    BalancePoint(
                        timestamp = entity.timestamp,
                        balance = convertedBalance,
                        currency = effectiveCurrency
                    )
                }

                _uiState.update { state ->
                    state.copy(balanceChartData = chartData)
                }
            }
        }
    }
    
    fun selectDateRange(dateRange: DateRange) {
        _selectedDateRange.value = dateRange
    }
    
    private fun getDateRangeValues(dateRange: DateRange): Pair<LocalDateTime, LocalDateTime> {
        val endDate = LocalDateTime.now()
        val startDate = when (dateRange) {
            DateRange.LAST_7_DAYS -> endDate.minusDays(7)
            DateRange.LAST_30_DAYS -> endDate.minusDays(30)
            DateRange.LAST_3_MONTHS -> endDate.minusMonths(3)
            DateRange.LAST_6_MONTHS -> endDate.minusMonths(6)
            DateRange.LAST_YEAR -> endDate.minusYears(1)
            DateRange.ALL_TIME -> LocalDateTime.of(2000, 1, 1, 0, 0)
        }
        return startDate to endDate
    }

    private fun getPrimaryCurrencyForAccount(bankName: String): String {
        return CurrencyFormatter.getBankBaseCurrency(bankName)
    }
}