package com.ritesh.cashiro.presentation.profile

import android.content.Context
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ritesh.cashiro.data.database.entity.TransactionType
import com.ritesh.cashiro.data.preferences.UserPreferencesRepository
import com.ritesh.cashiro.data.repository.AccountBalanceRepository
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
class ProfileScreenViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val accountBalanceRepository: AccountBalanceRepository,
    private val transactionRepository: TransactionRepository,
    private val subscriptionRepository: SubscriptionRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileScreenState())
    val state: StateFlow<ProfileScreenState> = _state.asStateFlow()

    init {
        observePreferences()
        observeTransactionCount()
        observeNetWorth()
        observeMonthlyFinancials()
        observeActiveSubscriptions()
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
        accountBalanceRepository
            .getTotalBalance()
            .onEach { total ->
                _state.update { it.copy(netWorth = total ?: java.math.BigDecimal.ZERO) }
            }
            .launchIn(viewModelScope)
    }

    private fun observeMonthlyFinancials() {
        val now = LocalDate.now()
        val firstDay = now.withDayOfMonth(1)
        val lastDay = now.withDayOfMonth(now.lengthOfMonth())

        // Logic here would ideally be optimized to combine flows if needed,
        // but for now we observe all transactions of the month
        transactionRepository
            .getAllTransactions()
            .onEach { transactions ->
                val monthTransactions =
                    transactions.filter {
                        it.dateTime.toLocalDate().let { date ->
                            !date.isBefore(firstDay) && !date.isAfter(lastDay)
                        }
                    }

                val income =
                    monthTransactions
                        .filter { it.transactionType == TransactionType.INCOME }
                        .sumOf { it.amount }

                val expense =
                    monthTransactions
                        .filter { it.transactionType == TransactionType.EXPENSE }
                        .sumOf { it.amount }

                _state.update { it.copy(totalIncome = income, totalExpense = expense) }
            }
            .launchIn(viewModelScope)
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
