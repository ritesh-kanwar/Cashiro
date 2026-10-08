package com.ritesh.cashiro.presentation.accounts

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ritesh.cashiro.data.currency.CurrencyConversionService
import com.ritesh.cashiro.data.currency.ExchangeRateProvider
import com.ritesh.cashiro.data.currency.model.CurrencyConversion
import com.ritesh.cashiro.data.currency.model.CurrencySymbols
import com.ritesh.cashiro.data.model.Currency
import com.ritesh.cashiro.data.model.CurrencyPickerOptions
import kotlinx.coroutines.CancellationException
import com.ritesh.cashiro.data.repository.CurrencyRepository
import com.ritesh.cashiro.data.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class CurrencyViewModel @Inject constructor(
    private val exchangeRateProvider: ExchangeRateProvider,
    private val currencyConversionService: CurrencyConversionService,
    private val currencyRepository: CurrencyRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _uiState = MutableStateFlow(CurrencyUiState())
    val uiState: StateFlow<CurrencyUiState> = _uiState.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    init {
        checkConnectivity()
        monitorNetworkConnectivity()
        loadCurrencies()
        observeBaseCurrency()
    }

    private fun observeBaseCurrency() {
        viewModelScope.launch {
            currencyRepository.effectiveBaseCurrencyCode.collectLatest { effectiveCurrency ->
                val currencies = _uiState.value.currencies
                if (currencies.isNotEmpty()) {
                    val selectedCurrency = currencies.find { it.code.equals(effectiveCurrency, ignoreCase = true) }
                    if (selectedCurrency != null && _uiState.value.selectedCurrency?.code != selectedCurrency.code) {
                        _uiState.update { it.copy(selectedCurrency = selectedCurrency) }
                        loadConversions(selectedCurrency.code)
                    }
                }
            }
        }
    }

    private fun checkConnectivity() {
        val activeNetwork = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        _isConnected.value = networkCapabilities != null &&
                (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
    }

    private fun monitorNetworkConnectivity() {
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _isConnected.value = true
                loadCurrencies()
            }

            override fun onLost(network: Network) {
                _isConnected.value = false
            }
        }
        connectivityManager.registerDefaultNetworkCallback(networkCallback)
    }

    fun loadCurrencies() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            val customCurrencies = userPreferencesRepository.customCurrencies.first().map { it.toCurrency() }
            val effectiveCurrencyCode = currencyRepository.effectiveBaseCurrencyCode.first()
            // Publish local options immediately; opening a picker must not require a network response.
            val localCurrencies = CurrencyPickerOptions.catalog(custom = customCurrencies, selectedCode = effectiveCurrencyCode)
            _uiState.update {
                it.copy(currencies = localCurrencies, selectedCurrency = localCurrencies.find { currency ->
                    currency.code.equals(effectiveCurrencyCode, ignoreCase = true)
                })
            }
            val currencyMap = try {
                exchangeRateProvider.fetchAllCurrencies()
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                null
            }
            val remoteCurrencies = currencyMap.orEmpty().map { (code, name) ->
                Currency(code, name, CurrencySymbols.getSymbol(code))
            }
            val currentCurrencyCode = currencyRepository.effectiveBaseCurrencyCode.first()
            val currencies = CurrencyPickerOptions.catalog(remoteCurrencies, customCurrencies, currentCurrencyCode)
            val selectedCurrency = currencies.find { it.code.equals(currentCurrencyCode, ignoreCase = true) }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    currencies = currencies,
                    selectedCurrency = selectedCurrency,
                    error = null,
                    isOfflineMode = !_isConnected.value
                )
            }
            selectedCurrency?.let { loadConversions(it.code) }
        }
    }

    fun loadConversions(currencyCode: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingConversions = true, conversionError = null) }
            
            val customCurrencies = userPreferencesRepository.customCurrencies.first()
            val customSymbols = customCurrencies.associate { it.code to it.symbol }

            // 1. Load from cache immediately
            val (cachedRates, lastUpdated) = currencyConversionService.getStoredConversions(currencyCode)
            if (cachedRates.isNotEmpty()) {
                val conversions = cachedRates.map { entity ->
                    CurrencyConversion(
                        currencyCode = entity.toCurrency,
                        rate = entity.rate.toDouble(),
                        lastUpdated = entity.updatedAtUnix * 1000,
                        isCustom = entity.isCustom,
                        customSymbol = customSymbols[entity.toCurrency]
                    )
                }.sortedBy { it.currencyCode }
                
                _uiState.update { 
                    it.copy(
                        conversions = conversions, 
                        lastUpdated = lastUpdated,
                        isLoadingConversions = !_isConnected.value // Keep loading if we expect a refresh
                    )
                }
            }

            // 2. Refresh from API if online
            if (_isConnected.value) {
                try {
                    currencyConversionService.fetchAndSaveAllRates(currencyCode)
                    
                    // Reload from updated DB
                    val (updatedRates, updatedTime) = currencyConversionService.getStoredConversions(currencyCode)
                    val conversions = updatedRates.map { entity ->
                        CurrencyConversion(
                            currencyCode = entity.toCurrency,
                            rate = entity.rate.toDouble(),
                            lastUpdated = entity.updatedAtUnix * 1000,
                            isCustom = entity.isCustom,
                            customSymbol = customSymbols[entity.toCurrency]
                        )
                    }.sortedBy { it.currencyCode }

                    _uiState.update {
                        it.copy(
                            isLoadingConversions = false,
                            conversions = conversions,
                            lastUpdated = updatedTime,
                            conversionError = null,
                            isOfflineMode = false
                        )
                    }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            isLoadingConversions = false,
                            conversionError = if (it.conversions.isEmpty()) "Failed to load exchange rates" else null,
                            isOfflineMode = true
                        )
                    }
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoadingConversions = false,
                        isOfflineMode = true,
                        conversionError = if (it.conversions.isEmpty()) "Connect to internet to fetch latest rates" else null
                    )
                }
            }
        }
    }

    fun addCustomCurrency(name: String, symbol: String, code: String, rate: Double) {
        viewModelScope.launch {
            val customCurrency = com.ritesh.cashiro.data.model.CustomCurrency(code.uppercase(), name, symbol)
            userPreferencesRepository.addCustomCurrency(customCurrency)
            
            val baseCurrency = _uiState.value.selectedCurrency?.code ?: "USD"
            currencyConversionService.saveCustomRate(baseCurrency, code.uppercase(), BigDecimal.valueOf(rate))
            
            loadCurrencies()
            loadConversions(baseCurrency)
        }
    }

    fun saveCustomRate(fromCurrency: String, toCurrency: String, rate: Double) {
        viewModelScope.launch {
            currencyConversionService.saveCustomRate(fromCurrency, toCurrency, BigDecimal.valueOf(rate))
            loadConversions(fromCurrency)
        }
    }

    fun resetCustomRate(fromCurrency: String, toCurrency: String) {
        viewModelScope.launch {
            currencyConversionService.resetCustomRate(fromCurrency, toCurrency)
            loadConversions(fromCurrency)
        }
    }

    fun selectCurrency(currencyCode: String) {
        val currency = _uiState.value.currencies.find { it.code.equals(currencyCode, ignoreCase = true) }
        if (currency != null) {
            _uiState.update { it.copy(selectedCurrency = currency) }
            loadConversions(currency.code)
        }
    }

    data class CurrencyUiState(
        val isLoading: Boolean = false,
        val currencies: List<Currency> = emptyList(),
        val selectedCurrency: Currency? = null,
        val error: String? = null,
        val isLoadingConversions: Boolean = false,
        val conversions: List<CurrencyConversion> = emptyList(),
        val conversionError: String? = null,
        val isOfflineMode: Boolean = false,
        val lastUpdated: Long = 0L
    )
}
