package com.ritesh.cashiro.presentation.ui.features.subscriptions

import com.ritesh.cashiro.utils.SubscriptionUtils

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ritesh.cashiro.data.currency.CurrencyConversionService
import com.ritesh.cashiro.data.database.entity.SubscriptionEntity
import com.ritesh.cashiro.data.repository.AccountBalanceRepository
import com.ritesh.cashiro.data.repository.CategoryRepository
import com.ritesh.cashiro.data.repository.SubcategoryRepository
import com.ritesh.cashiro.data.repository.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject
import com.ritesh.cashiro.data.repository.CurrencyRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update

@HiltViewModel
class SubscriptionsViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
    private val accountBalanceRepository: AccountBalanceRepository,
    private val categoryRepository: CategoryRepository,
    private val subcategoryRepository: SubcategoryRepository,
    private val currencyConversionService: CurrencyConversionService,
    private val currencyRepository: CurrencyRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val sharedPrefs = context.getSharedPreferences("account_prefs", Context.MODE_PRIVATE)
    
    private val _uiState = MutableStateFlow(SubscriptionsUiState())
    val uiState: StateFlow<SubscriptionsUiState> = _uiState.asStateFlow()

    val categoriesMap = categoryRepository.getAllCategories()
        .map { cats -> cats.associateBy { it.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val subcategoriesMap = subcategoryRepository.getAllSubcategories()
        .map { subcats -> subcats.associateBy { it.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
    
    init {
        loadSubscriptions()
    }
    
    private fun loadSubscriptions() {
        viewModelScope.launch {
            combine(
                subscriptionRepository.getActiveSubscriptions(),
                currencyRepository.baseCurrencyCode
            ) { subscriptions, targetCurrency ->
                val subscriptionCurrencies = subscriptions.map { it.currency }.distinct()
                if (subscriptionCurrencies.any { it != targetCurrency }) {
                    currencyConversionService.refreshExchangeRatesForAccount(subscriptionCurrencies + targetCurrency)
                }

                var totalMonthlyAmount = BigDecimal.ZERO
                val convertedAmounts = buildMap(subscriptions.size) {
                    subscriptions.forEach { sub ->
                        val converted = if (sub.currency == targetCurrency) {
                            sub.amount
                        } else {
                            currencyConversionService.convertAmount(
                                amount = sub.amount,
                                fromCurrency = sub.currency,
                                toCurrency = targetCurrency
                            ) ?: sub.amount
                        }
                        put(sub.id, converted)
                        totalMonthlyAmount = totalMonthlyAmount.add(
                            SubscriptionUtils.monthlyEquivalent(converted, sub.billingCycle)
                        )
                    }
                }

                _uiState.update {
                    it.copy(
                        activeSubscriptions = subscriptions,
                        totalMonthlyAmount = totalMonthlyAmount,
                        totalYearlyAmount = totalMonthlyAmount.multiply(BigDecimal(12)),
                        targetCurrency = targetCurrency,
                        convertedAmounts = convertedAmounts,
                        isLoading = false
                    )
                }
            }.collectLatest { }
        }
    }
    
    fun hideSubscription(subscriptionId: Long) {
        viewModelScope.launch {
            subscriptionRepository.hideSubscription(subscriptionId)
            _uiState.value = _uiState.value.copy(
                lastHiddenSubscription = _uiState.value.activeSubscriptions.find { it.id == subscriptionId }
            )
        }
    }
    
    fun undoHide() {
        _uiState.value.lastHiddenSubscription?.let { subscription ->
            viewModelScope.launch {
                subscriptionRepository.unhideSubscription(subscription.id)
                _uiState.value = _uiState.value.copy(lastHiddenSubscription = null)
            }
        }
    }

    fun selectSubscription(subscription: SubscriptionEntity?) {
        _uiState.value = _uiState.value.copy(selectedSubscription = subscription)
    }

    fun markAsPaid(subscription: SubscriptionEntity) {
        viewModelScope.launch {
            val today = java.time.LocalDate.now()
            val nextDate = SubscriptionUtils.calculateNextPaymentDate(subscription.nextPaymentDate ?: today, subscription.billingCycle)
            subscriptionRepository.updatePaymentStatus(subscription.id, nextDate, today)
            selectSubscription(null)
        }
    }

}