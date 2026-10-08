package com.ritesh.cashiro.presentation.ui.features.subscriptions

import com.ritesh.cashiro.utils.SubscriptionUtils
import com.ritesh.cashiro.domain.usecase.MarkSubscriptionPaidUseCase
import com.ritesh.cashiro.R
import kotlinx.coroutines.CancellationException

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
    private val markSubscriptionPaid: MarkSubscriptionPaidUseCase,
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
                currencyRepository.effectiveBaseCurrencyCode,
                currencyConversionService.rateChangeTrigger
            ) { subscriptions, targetCurrency, _ ->
                val subscriptionCurrencies = subscriptions.map { it.currency }.distinct()
                if (subscriptionCurrencies.any { it != targetCurrency }) {
                    currencyConversionService.refreshExchangeRatesForAccount(subscriptionCurrencies + targetCurrency)
                }

                var totalMonthlyAmount = BigDecimal.ZERO
                var conversionFailures = 0
                val convertedAmounts = buildMap(subscriptions.size) {
                    subscriptions.forEach { sub ->
                        val converted = if (sub.currency == targetCurrency) {
                            sub.amount
                        } else {
                            currencyConversionService.convertAmountOrNull(
                                amount = sub.amount,
                                fromCurrency = sub.currency,
                                toCurrency = targetCurrency
                            )
                        }
                        if (converted == null) {
                            conversionFailures++
                            return@forEach
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
                        conversionFailureCount = conversionFailures,
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
        _uiState.update { it.copy(selectedSubscription = subscription, paymentCandidates = emptyList()) }
    }

    fun clearPaymentMessage() { _uiState.update { it.copy(paymentMessage = null) } }
    fun dismissPaymentCandidates() { _uiState.update { it.copy(paymentCandidates = emptyList()) } }

    fun markAsPaid(subscription: SubscriptionEntity, linkedTransactionId: Long? = null, recordAnotherPayment: Boolean = false) {
        if (_uiState.value.paymentInProgress) return
        _uiState.update { it.copy(paymentInProgress = true) }
        viewModelScope.launch {
            try {
                when (val result = markSubscriptionPaid.execute(subscription.id, subscription.nextPaymentDate,
                    linkedTransactionId = linkedTransactionId, recordAnotherPayment = recordAnotherPayment)) {
                    is MarkSubscriptionPaidUseCase.Result.ChooseExisting ->
                        _uiState.update { it.copy(paymentCandidates = result.transactions) }
                    is MarkSubscriptionPaidUseCase.Result.Paid -> {
                        selectSubscription(null)
                        _uiState.update { it.copy(paymentMessage = context.getString(
                            if (result.linked) R.string.subscription_payment_linked else R.string.subscription_payment_recorded
                        )) }
                    }
                    MarkSubscriptionPaidUseCase.Result.AlreadyPaid -> {
                        selectSubscription(null)
                        _uiState.update { it.copy(paymentMessage = context.getString(R.string.subscription_payment_already_recorded)) }
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _uiState.update { it.copy(paymentMessage = error.message ?: context.getString(R.string.subscription_payment_failed)) }
            } finally {
                _uiState.update { it.copy(paymentInProgress = false) }
            }
        }
    }
}
