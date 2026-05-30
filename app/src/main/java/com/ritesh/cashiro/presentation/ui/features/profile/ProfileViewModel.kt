package com.ritesh.cashiro.presentation.ui.features.profile

import android.content.Context
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ritesh.cashiro.data.currency.CurrencyConversionService
import com.ritesh.cashiro.data.database.entity.TransactionType
import com.ritesh.cashiro.data.preferences.UserPreferencesRepository
import com.ritesh.cashiro.data.repository.AccountBalanceRepository
import com.ritesh.cashiro.data.repository.CurrencyRepository
import com.ritesh.cashiro.data.repository.SubscriptionRepository
import com.ritesh.cashiro.data.repository.TransactionRepository
import com.ritesh.cashiro.utils.ImageUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@HiltViewModel
class ProfileViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val accountBalanceRepository: AccountBalanceRepository,
    private val transactionRepository: TransactionRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val currencyRepository: CurrencyRepository,
    private val currencyConversionService: CurrencyConversionService
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileScreenState())
    val state: StateFlow<ProfileScreenState> = _state.asStateFlow()

    init {
        observeBaseCurrency()
        observePreferences()
        observeTransactionCount()
        observeNetWorth()
        observeMonthlyFinancials()
        observeActiveSubscriptions()
    }

    private fun observeBaseCurrency() {
        viewModelScope.launch {
            currencyRepository.baseCurrencyCode.collectLatest { code ->
                _state.update { it.copy(baseCurrency = code) }
            }
        }
    }

    private fun observePreferences() {
        userPreferencesRepository
            .userPreferences
            .onEach { prefs ->
                _state.update {
                    it.copy(
                        userName = prefs.userName,
                        profileImageUri =
                            prefs.profileImageUri?.let { uri -> Uri.parse(uri) },
                        profileBackgroundColor = Color(prefs.profileBackgroundColor),
                        bannerImageUri = prefs.bannerImageUri?.let { uri -> Uri.parse(uri) }
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun observeTransactionCount() {
        transactionRepository
            .getAllTransactions()
            .onEach { transactions ->
                _state.update { it.copy(totalTransactions = transactions.size) }
            }
            .launchIn(viewModelScope)
    }

    private fun observeNetWorth() {
        combine(
            accountBalanceRepository.getAllLatestBalances(),
            currencyRepository.baseCurrencyCode
        ) { allBalances, baseCurrency ->
            if (allBalances.isEmpty()) return@combine BigDecimal.ZERO

            allBalances.fold(java.math.BigDecimal.ZERO) { acc, account ->
                acc + if (account.currency == baseCurrency) {
                    account.balance
                } else {
                    currencyConversionService.convertAmount(
                        amount = account.balance,
                        fromCurrency = account.currency,
                        toCurrency = baseCurrency
                    ) ?: account.balance
                }
            }
        }.onEach { total ->
            _state.update { it.copy(netWorth = total) }
        }.launchIn(viewModelScope)
    }

    private fun observeMonthlyFinancials() {
        val now = LocalDate.now()
        val firstDay = now.withDayOfMonth(1)
        val lastDay = now.withDayOfMonth(now.lengthOfMonth())

        combine(
            transactionRepository.getAllTransactions(),
            currencyRepository.baseCurrencyCode
        ) { transactions, baseCurrency ->
            val monthTransactions =
                transactions.filter {
                    val date = it.dateTime.toLocalDate()
                    !date.isBefore(firstDay) && !date.isAfter(lastDay)
                }

            val income = monthTransactions
                .filter { it.transactionType == TransactionType.INCOME }
                .fold(java.math.BigDecimal.ZERO) { acc, txn ->
                    acc + if (txn.currency == baseCurrency) {
                        txn.amount
                    } else {
                        currencyConversionService.convertAmount(
                            amount = txn.amount,
                            fromCurrency = txn.currency,
                            toCurrency = baseCurrency
                        )
                    }
                }

            val expense = monthTransactions
                .filter { it.transactionType == TransactionType.EXPENSE }
                .fold(java.math.BigDecimal.ZERO) { acc, txn ->
                    acc + if (txn.currency == baseCurrency) {
                        txn.amount
                    } else {
                        currencyConversionService.convertAmount(
                            amount = txn.amount,
                            fromCurrency = txn.currency,
                            toCurrency = baseCurrency
                        )
                    }
                }

            income to expense
        }.onEach { (income, expense) ->
            _state.update { it.copy(totalIncome = income, totalExpense = expense) }
        }.launchIn(viewModelScope)
    }

    private fun observeActiveSubscriptions() {
        subscriptionRepository
            .getActiveSubscriptions()
            .onEach { subscriptions ->
                _state.update { it.copy(activeSubscriptions = subscriptions.size) }
            }
            .launchIn(viewModelScope)
    }

    fun toggleEditSheet() {
        _state.update {
            val newState = !it.isEditSheetOpen
            if (newState) { // Initialize edit state when opening
                it.copy(
                    isEditSheetOpen = true,
                    editState =
                        EditProfileState(
                            editedUserName = it.userName,
                            editedProfileImageUri = it.profileImageUri,
                            editedProfileBackgroundColor = it.profileBackgroundColor,
                            editedBannerImageUri = it.bannerImageUri,
                            hasChanges = false
                        )
                )
            } else {
                it.copy(isEditSheetOpen = false)
            }
        }
    }

    fun dismissEditSheet() {
        _state.update { it.copy(isEditSheetOpen = false) }
    }

    fun updateEditUserName(name: String) {
        _state.update {
            it.copy(editState = it.editState.copy(editedUserName = name, hasChanges = true))
        }
    }

    fun updateEditProfileImage(uri: Uri?) {
        _state.update {
            it.copy(editState = it.editState.copy(editedProfileImageUri = uri, hasChanges = true))
        }
    }

    fun updateEditProfileBackgroundColor(color: Color) {
        _state.update {
            it.copy(
                editState =
                    it.editState.copy(
                        editedProfileBackgroundColor = color,
                        hasChanges = true
                    )
            )
        }
    }

    fun updateEditBannerImage(uri: Uri?) {
        _state.update {
            it.copy(editState = it.editState.copy(editedBannerImageUri = uri, hasChanges = true))
        }
    }

    fun updateStoragePermission(isGranted: Boolean) {
        _state.update { it.copy(hasStoragePermission = isGranted) }
    }

    fun saveProfileChanges() {
        val currentState = _state.value
        val editState = currentState.editState

        viewModelScope.launch {
            // Save profile image to internal storage if it's a new gallery image
            val profileImagePersistentUri =
                editState.editedProfileImageUri?.let { uri ->
                    ImageUtils.saveImageToInternalStorage(context, uri, "profile")
                }

            // Save banner image to internal storage if it's a new gallery image
            val bannerImagePersistentUri =
                editState.editedBannerImageUri?.let { uri ->
                    ImageUtils.saveImageToInternalStorage(context, uri, "banner")
                }

            userPreferencesRepository.updateUserName(editState.editedUserName)
            userPreferencesRepository.updateProfileImageUri(profileImagePersistentUri?.toString())
            userPreferencesRepository.updateProfileBackgroundColor(
                    editState.editedProfileBackgroundColor.toArgb()
            )
            userPreferencesRepository.updateBannerImageUri(bannerImagePersistentUri?.toString())

            _state.update { it.copy(isEditSheetOpen = false) }
        }
    }
}
