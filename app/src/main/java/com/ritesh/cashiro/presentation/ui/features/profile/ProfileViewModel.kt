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
import com.ritesh.cashiro.data.service.AttachmentService
import com.ritesh.cashiro.domain.model.PersonCategory
import com.ritesh.cashiro.domain.usecase.AddEditLendBorrowPersonUseCase
import com.ritesh.cashiro.domain.usecase.GetLendBorrowPersonsUseCase
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
    private val currencyConversionService: CurrencyConversionService,
    private val getLendBorrowPersonsUseCase: GetLendBorrowPersonsUseCase,
    private val addEditPersonUseCase: AddEditLendBorrowPersonUseCase,
    val attachmentService: AttachmentService
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
        observeContacts()
    }

    private fun observeContacts() {
        getLendBorrowPersonsUseCase()
            .onEach { persons ->
                _state.update { it.copy(contacts = persons) }
            }
            .launchIn(viewModelScope)
    }

    private fun observeBaseCurrency() {
        viewModelScope.launch {
            currencyRepository.effectiveBaseCurrencyCode.collectLatest { code ->
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
            currencyRepository.effectiveBaseCurrencyCode,
            currencyConversionService.rateChangeTrigger
        ) { allBalances, baseCurrency, _ ->
            if (allBalances.isEmpty()) return@combine BigDecimal.ZERO

            var total = BigDecimal.ZERO
            for (account in allBalances) {
                val amt = if (account.currency == baseCurrency) {
                    account.balance
                } else {
                    currencyConversionService.convertAmountOrNull(
                        amount = account.balance,
                        fromCurrency = account.currency,
                        toCurrency = baseCurrency
                    )
                }
                total = total.add(amt ?: BigDecimal.ZERO)
            }
            total
        }.onEach { total ->
            _state.update { it.copy(netWorth = total) }
        }.launchIn(viewModelScope)
    }

    private data class MonthlyTotals(val income: BigDecimal, val expense: BigDecimal)

    private fun observeMonthlyFinancials() {
        val now = LocalDate.now()
        val firstDay = now.withDayOfMonth(1)
        val lastDay = now.withDayOfMonth(now.lengthOfMonth())

        combine(
            transactionRepository.getAllTransactions(),
            currencyRepository.effectiveBaseCurrencyCode,
            currencyConversionService.rateChangeTrigger
        ) { transactions, baseCurrency, _ ->
            val monthTransactions =
                transactions.filter {
                    val date = it.dateTime.toLocalDate()
                    !date.isBefore(firstDay) && !date.isAfter(lastDay)
                }

            var income = BigDecimal.ZERO
            var expense = BigDecimal.ZERO
            for (txn in monthTransactions) {
                val amt = if (txn.currency == baseCurrency) {
                    txn.amount
                } else {
                    currencyConversionService.convertAmountOrNull(
                        amount = txn.amount,
                        fromCurrency = txn.currency,
                        toCurrency = baseCurrency
                    )
                }
                if (txn.transactionType == TransactionType.INCOME || txn.transactionType == TransactionType.BORROWED) {
                    income = income.add(amt ?: BigDecimal.ZERO)
                } else if (txn.transactionType == TransactionType.EXPENSE || txn.transactionType == TransactionType.LENT) {
                    expense = expense.add(amt ?: BigDecimal.ZERO)
                }
            }

            MonthlyTotals(income = income, expense = expense)
        }.onEach { totals ->
            _state.update { it.copy(totalIncome = totals.income, totalExpense = totals.expense) }
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

    fun showAddPersonSheet(show: Boolean) {
        _state.update { it.copy(isAddPersonSheetOpen = show) }
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
            addEditPersonUseCase.addPerson(name, phone, notes, color, avatar, category)
            showAddPersonSheet(false)
        }
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
