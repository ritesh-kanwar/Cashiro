package com.ritesh.cashiro.presentation.ui.features.accounts

import android.content.Context
import androidx.room.withTransaction
import com.ritesh.cashiro.data.database.CashiroDatabase
import com.ritesh.cashiro.data.preferences.BankAccountMergeStore
import kotlinx.coroutines.sync.withLock
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import com.ritesh.cashiro.data.database.entity.CardEntity
import com.ritesh.cashiro.data.database.entity.CardType
import com.ritesh.cashiro.data.preferences.UserPreferencesRepository
import com.ritesh.cashiro.data.repository.AccountBalanceRepository
import com.ritesh.cashiro.data.repository.CardRepository
import com.ritesh.cashiro.data.repository.TransactionRepository
import com.ritesh.cashiro.utils.CurrencyFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.math.BigDecimal
import java.time.LocalDateTime
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.ritesh.cashiro.R
import com.ritesh.cashiro.utils.IconResolutionUtils
import androidx.core.content.edit

data class ManageAccountsUiState(
    val accounts: List<AccountBalanceEntity> = emptyList(),
    val hiddenAccounts: Set<String> = emptySet(),
    val ignoredAccounts: Set<String> = emptySet(),
    val dismissedDuplicatePairs: Set<String> = emptySet(),
    val balanceHistory: List<AccountBalanceEntity> = emptyList(),
    val linkedCards: Map<String, List<CardEntity>> = emptyMap(),
    val orphanedCards: List<CardEntity> = emptyList(),
    val isLoading: Boolean = false,
    val mainAccountKey: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

data class AccountFormState(
    val bankName: String = "",
    val accountLast4: String = "",
    val balance: String = "",
    val creditLimit: String = "",
    val accountType: AccountType = AccountType.SAVINGS,
    val iconResId: Int = 0,
    val iconName: String = "",
    val currency: String = "INR",
    val isValid: Boolean = false,
    val errorMessage: String? = null
)

enum class AccountType {
    SAVINGS,
    CURRENT,
    CREDIT,
    WALLET
}

@HiltViewModel
class ManageAccountsViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val accountBalanceRepository: AccountBalanceRepository,
    private val cardRepository: CardRepository,
    private val transactionRepository: TransactionRepository,
    private val database: CashiroDatabase,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val bankAccountMerges: BankAccountMergeStore,
    private val ignoredAccounts: com.ritesh.cashiro.data.preferences.IgnoredAccountsStore
) : ViewModel() {

    private val sharedPrefs = context.getSharedPreferences("account_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(ManageAccountsUiState())
    val uiState: StateFlow<ManageAccountsUiState> = _uiState.asStateFlow()

    private val _formState = MutableStateFlow(AccountFormState())
    val formState: StateFlow<AccountFormState> = _formState.asStateFlow()

    val defaultCurrencyForNewAccounts: StateFlow<String> = combine(
        userPreferencesRepository.defaultCurrencyEnabled,
        userPreferencesRepository.defaultCurrencyCode
    ) { enabled, code ->
        if (enabled && !code.isNullOrBlank()) code else "INR"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "INR")

    init {
        loadAccounts()
        loadHiddenAccounts()
        viewModelScope.launch { ignoredAccounts.keysFlow.collect { keys -> _uiState.update { it.copy(ignoredAccounts = keys) } } }
        loadMainAccount()
        loadCards()
        initializeDefaultWallet()
        initFormCurrency()
    }

    fun isAccountIgnored(account: AccountBalanceEntity): Boolean =
        ignoredAccounts.isIgnored(account.bankName, account.currency, account.accountLast4)

    fun setAccountIgnored(account: AccountBalanceEntity, ignored: Boolean) {
        ignoredAccounts.setIgnored(account.bankName, account.currency, account.accountLast4, ignored)
    }

    private fun initFormCurrency() {
        viewModelScope.launch {
            userPreferencesRepository.defaultCurrencyEnabled.first().let { enabled ->
                if (enabled) {
                    userPreferencesRepository.defaultCurrencyCode.first()?.let { code ->
                        if (code.isNotBlank()) {
                            _formState.update { it.copy(currency = code) }
                        }
                    }
                }
            }
        }
    }

    private suspend fun resolveDefaultCurrency(): String {
        if (userPreferencesRepository.defaultCurrencyEnabled.first()) {
            userPreferencesRepository.defaultCurrencyCode.first()?.let { code ->
                if (code.isNotBlank()) return code
            }
        }
        return "INR"
    }

    private fun initializeDefaultWallet() {
        viewModelScope.launch {
            val wallet = accountBalanceRepository.getLatestBalance("Cash", "wallet")
            if (wallet == null) {
                val baseCurrency = userPreferencesRepository.userPreferences.first().baseCurrency
                accountBalanceRepository.insertBalance(
                    AccountBalanceEntity(
                        bankName = "Cash",
                        accountLast4 = "wallet",
                        balance = BigDecimal.ZERO,
                        timestamp = LocalDateTime.now(),
                        sourceType = "MANUAL",
                        isWallet = true,
                        iconResId = R.drawable.type_finance_dollar_banknote,
                        iconName = "type_finance_dollar_banknote",
                        color = "#4CAF50",
                        currency = baseCurrency
                    )
                )
            }
        }
    }

    private fun loadAccounts() {
        viewModelScope.launch {
            accountBalanceRepository.getAllLatestBalances().collect { accounts ->
                _uiState.update { it.copy(accounts = accounts) }
            }
        }
    }

    private fun loadCards() {
        viewModelScope.launch {
            cardRepository.getAllCards().collect { allCards ->
                // Group cards by linked account
                val linkedCardsMap = mutableMapOf<String, MutableList<CardEntity>>()
                val orphaned = mutableListOf<CardEntity>()

                for (card in allCards) {
                    when {
                        card.cardType == CardType.DEBIT && card.accountLast4 != null -> {
                            linkedCardsMap.getOrPut(card.accountLast4) { mutableListOf() }.add(card)
                        }
                        card.cardType == CardType.DEBIT && card.accountLast4 == null -> {
                            orphaned.add(card)
                        }
                    // Credit cards are not orphaned, they're standalone
                    }
                }

                _uiState.update { it.copy(linkedCards = linkedCardsMap, orphanedCards = orphaned) }
            }
        }
    }

    private fun loadHiddenAccounts() {
        val hidden = sharedPrefs.getStringSet("hidden_accounts", emptySet()) ?: emptySet()
        val dismissed = sharedPrefs.getStringSet("dismissed_duplicate_pairs", emptySet()).orEmpty().toSet()
        _uiState.update { it.copy(hiddenAccounts = hidden, dismissedDuplicatePairs = dismissed) }
    }

    fun dismissDuplicatePair(key: String) {
        val dismissed = _uiState.value.dismissedDuplicatePairs + key
        sharedPrefs.edit { putStringSet("dismissed_duplicate_pairs", dismissed) }
        _uiState.update { it.copy(dismissedDuplicatePairs = dismissed) }
    }

    private fun loadMainAccount() {
        val main = sharedPrefs.getString("main_account", null)
        _uiState.update { it.copy(mainAccountKey = main) }
    }

    fun setAsMainAccount(bankName: String, accountLast4: String) {
        val key = "${bankName}_${accountLast4}"
        sharedPrefs.edit { putString("main_account", key) }
        _uiState.update { it.copy(mainAccountKey = key, successMessage = "Main account set successfully") }
        viewModelScope.launch {
            // Persist this account's currency as the app-wide base currency
            val account = _uiState.value.accounts.find {
                it.bankName == bankName && it.accountLast4 == accountLast4
            }
            if (account != null) {
                userPreferencesRepository.updateBaseCurrency(account.currency)
                
                // Also update the in-built Cash wallet to match the main account's currency
                val cashWallet = accountBalanceRepository.getLatestBalance("Cash", "wallet")
                if (cashWallet != null && cashWallet.currency != account.currency) {
                    accountBalanceRepository.insertBalance(
                        cashWallet.copy(
                            id = 0,
                            currency = account.currency,
                            timestamp = LocalDateTime.now(),
                            sourceType = "MAIN_ACCOUNT_SYNC"
                        )
                    )
                }
            }
            delay(3000)
            _uiState.update { it.copy(successMessage = null) }
        }
    }

    fun updateBankName(name: String) {
        _formState.update {
            it.copy(bankName = name, isValid = validateForm(name, it.accountLast4, it.balance))
        }
    }

    fun updateAccountLast4(last4: String) {
        // Only allow 4 characters
        if (last4.length <= 4) {
            _formState.update {
                it.copy(
                    accountLast4 = last4,
                    isValid = validateForm(it.bankName, last4, it.balance)
                )
            }
        }
    }

    fun updateBalance(balance: String) {
        // Only allow valid numeric input
        if (balance.isEmpty() || balance.matches(Regex("^\\d*\\.?\\d*$"))) {
            _formState.update {
                it.copy(
                    balance = balance,
                    isValid = validateForm(it.bankName, it.accountLast4, balance)
                )
            }
        }
    }

    fun updateCreditLimit(limit: String) {
        // Only allow valid numeric input
        if (limit.isEmpty() || limit.matches(Regex("^\\d*\\.?\\d*$"))) {
            _formState.update { it.copy(creditLimit = limit) }
        }
    }

    fun updateAccountType(type: AccountType) {
        _formState.update { it.copy(accountType = type) }
    }

    fun updateIcon(iconName: String) {
        val iconResId = IconResolutionUtils.nameToResId(context, iconName)
        _formState.update { it.copy(iconResId = iconResId, iconName = iconName) }
    }

    fun updateCurrency(currency: String) {
        _formState.update { it.copy(currency = currency) }
    }

    private fun validateForm(bankName: String, last4: String, balance: String): Boolean {
        return bankName.isNotBlank() &&
                last4.length == 4 &&
                balance.isNotBlank() &&
                balance.toDoubleOrNull() != null
    }

    fun addAccount(
        bankName: String,
        balance: BigDecimal,
        accountLast4: String,
        iconResId: Int,
        iconName: String,
        colorHex: String,
        isCreditCard: Boolean = false,
        isWallet: Boolean = false,
        creditLimit: BigDecimal? = null,
        currency: String = "INR"
    ) {
        viewModelScope.launch {
            // Check for duplicates
            val existingAccount = accountBalanceRepository.getLatestBalance(bankName, accountLast4)

            if (existingAccount != null) {
                _uiState.update {
                    it.copy(
                            errorMessage = "Account with this name and last 4 digits already exists"
                    )
                }
                return@launch
            }

            accountBalanceRepository.insertBalance(
                AccountBalanceEntity(
                    bankName = bankName,
                    accountLast4 = accountLast4,
                    balance = balance,
                    creditLimit = creditLimit,
                    timestamp = LocalDateTime.now(),
                    isCreditCard = isCreditCard,
                    isWallet = isWallet,
                    iconResId = iconResId,
                    iconName = iconName,
                    sourceType = "MANUAL",
                    currency = currency,
                    color = colorHex
                )
            )

            _uiState.update { it.copy(successMessage = "Account added successfully") }
            delay(3000)
            _uiState.update { it.copy(successMessage = null) }
        }
    }

    fun addAccount() {
        val state = _formState.value
        if (!state.isValid) return

        val creditLimit =
                if (state.accountType == AccountType.CREDIT && state.creditLimit.isNotBlank()) {
                    BigDecimal(state.creditLimit)
                } else null

        addAccount(
            bankName = state.bankName,
            balance = BigDecimal(state.balance),
            accountLast4 = state.accountLast4,
            iconResId = state.iconResId,
            iconName = state.iconName,
            colorHex = "#33B5E5", // Default or handle color
            isCreditCard = (state.accountType == AccountType.CREDIT),
            isWallet = (state.accountType == AccountType.WALLET),
            creditLimit = creditLimit,
            currency = state.currency
        )

        // Clear form
        viewModelScope.launch {
            _formState.value = AccountFormState(currency = resolveDefaultCurrency())
        }
    }

    fun updateAccountBalance(bankName: String, accountLast4: String, newBalance: BigDecimal) {
        viewModelScope.launch {
            // Get the latest balance to preserve credit limit
            val latestBalance = accountBalanceRepository.getLatestBalance(bankName, accountLast4)

            accountBalanceRepository.insertBalance(AccountBalanceEntity(
                bankName = bankName,
                accountLast4 = accountLast4,
                balance = newBalance,
                creditLimit = latestBalance?.creditLimit,
                timestamp = LocalDateTime.now(),
                iconResId = latestBalance?.iconResId ?: 0,
                iconName = latestBalance?.iconName ?: "",
                isWallet = latestBalance?.isWallet ?: false,
                sourceType = "MANUAL",
                currency = latestBalance?.currency ?: resolveDefaultCurrency(),
                color = latestBalance?.color ?: "#33B5E5"
            )
            )
        }
    }

    fun updateCreditCard(
            bankName: String,
            accountLast4: String,
            newBalance: BigDecimal,
            newLimit: BigDecimal
    ) {
        viewModelScope.launch {
            val latestBalance = accountBalanceRepository.getLatestBalance(bankName, accountLast4)
            accountBalanceRepository.insertBalance(
                AccountBalanceEntity(
                    bankName = bankName,
                    accountLast4 = accountLast4,
                    balance = newBalance,
                    creditLimit = newLimit,
                    timestamp = LocalDateTime.now(),
                    isCreditCard = true,
                    sourceType = "MANUAL",
                    iconResId = latestBalance?.iconResId ?: 0,
                    iconName = latestBalance?.iconName ?: "type_finance_credit_card",
                    currency = latestBalance?.currency ?: resolveDefaultCurrency(),
                    color = latestBalance?.color ?: "#E91E63"
                )
            )
        }
    }

    fun toggleAccountVisibility(bankName: String, accountLast4: String) {
        val key = "${bankName}_${accountLast4}"
        val hidden = _uiState.value.hiddenAccounts.toMutableSet()

        if (hidden.contains(key)) {
            hidden.remove(key)
        } else {
            hidden.add(key)
        }

        // Save to SharedPreferences
        sharedPrefs.edit().putStringSet("hidden_accounts", hidden).apply()

        // Update UI state
        _uiState.update { it.copy(hiddenAccounts = hidden) }
    }

    fun isAccountHidden(bankName: String, accountLast4: String): Boolean {
        val key = "${bankName}_${accountLast4}"
        return _uiState.value.hiddenAccounts.contains(key)
    }

    fun clearError() {
        _formState.update { it.copy(errorMessage = null) }
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun loadBalanceHistory(bankName: String, accountLast4: String) {
        viewModelScope.launch {
            val history =
                    accountBalanceRepository.getBalanceHistoryForAccount(bankName, accountLast4)
            _uiState.update { it.copy(balanceHistory = history) }
        }
    }

    fun deleteBalanceRecord(id: Long, bankName: String, accountLast4: String) {
        viewModelScope.launch {
            // Check if this is the only record
            val count = accountBalanceRepository.getBalanceCountForAccount(bankName, accountLast4)
            if (count > 1) {
                accountBalanceRepository.deleteBalanceById(id)
                // Reload history and accounts
                loadBalanceHistory(bankName, accountLast4)
                loadAccounts()
            }
        }
    }

    fun updateBalanceRecord(
            id: Long,
            newBalance: BigDecimal,
            bankName: String,
            accountLast4: String
    ) {
        viewModelScope.launch {
            accountBalanceRepository.updateBalanceById(id, newBalance)
            // Reload history and accounts
            loadBalanceHistory(bankName, accountLast4)
            loadAccounts()
        }
    }

    fun clearBalanceHistory() {
        _uiState.update { it.copy(balanceHistory = emptyList()) }
    }

    fun linkCardToAccount(cardId: Long, accountLast4: String) {
        viewModelScope.launch {
            try {
                Log.d(
                        "ManageAccountsViewModel",
                        "Starting to link card $cardId to account $accountLast4"
                )

                // Get card info first to know if there's a balance to copy
                val card = cardRepository.getCardById(cardId)
                val hasBalance = card?.lastBalance != null

                // Link the card to the account
                cardRepository.linkCardToAccount(cardId, accountLast4)

                // If card had a balance, copy it to the account
                if (card != null && hasBalance) {
                    try {
                        val insertedId =
                                accountBalanceRepository.insertBalanceUpdate(
                                        bankName = card.bankName,
                                        accountLast4 = accountLast4,
                                        balance = card.lastBalance!!,
                                        timestamp = card.lastBalanceDate ?: LocalDateTime.now(),
                                        smsSource = card.lastBalanceSource,
                                        sourceType = "CARD_LINK"
                                )
                        Log.d(
                                "ManageAccountsViewModel",
                                "Balance copied to account. Insert ID: $insertedId"
                        )

                        // Show success message with balance
                        val message =
                                "Card linked successfully. Balance updated to ${CurrencyFormatter.formatCurrency(card.lastBalance)}"
                        _uiState.update { it.copy(successMessage = message) }
                    } catch (e: Exception) {
                        Log.e(
                                "ManageAccountsViewModel",
                                "Failed to copy balance: ${e.message}",
                                e
                        )
                        // Still show success for linking, but note the balance issue
                        _uiState.update {
                            it.copy(
                                    successMessage =
                                            "Card linked successfully (balance update failed)"
                            )
                        }
                    }
                } else {
                    // No balance to copy, just show link success
                    _uiState.update { it.copy(successMessage = "Card linked successfully") }
                }

                // Clear message after delay
                delay(3000)
                _uiState.update { it.copy(successMessage = null) }

                loadCards()
                loadAccounts()
            } catch (e: Exception) {
                Log.e("ManageAccountsViewModel", "Failed to link card", e)
                _uiState.update { it.copy(errorMessage = "Failed to link card: ${e.message}") }
            }
        }
    }

    fun unlinkCard(cardId: Long) {
        viewModelScope.launch {
            cardRepository.unlinkCard(cardId)
            loadCards()
        }
    }

    fun deleteCard(cardId: Long) {
        viewModelScope.launch {
            try {
                Log.d("ManageAccountsViewModel", "Deleting card with ID: $cardId")
                cardRepository.deleteCard(cardId)
                _uiState.update { it.copy(successMessage = "Card deleted successfully") }

                // Clear message after delay
                delay(2000)
                _uiState.update { it.copy(successMessage = null) }

                loadCards()
            } catch (e: Exception) {
               Log.e("ManageAccountsViewModel", "Failed to delete card", e)
                _uiState.update { it.copy(errorMessage = "Failed to delete card: ${e.message}") }
            }
        }
    }

    fun setCardActive(cardId: Long, isActive: Boolean) {
        viewModelScope.launch {
            cardRepository.setCardActive(cardId, isActive)
            loadCards()
        }
    }

    fun deleteAccount(bankName: String, accountLast4: String) {
        viewModelScope.launch {
            try {
                // Unlink any cards linked to this account
                val linkedCards = _uiState.value.linkedCards[accountLast4] ?: emptyList()
                linkedCards.filter { it.bankName == bankName }.forEach { card -> cardRepository.unlinkCard(card.id) }

                // Delete all balance records for this account
                val deletedCount = BankAccountMergeStore.mutationMutex.withLock {
                    val count = accountBalanceRepository.deleteAccount(bankName, accountLast4)
                    bankAccountMerges.forgetAccount(bankName, accountLast4)
                    count
                }

                // Remove from hidden accounts if present
                val key = "${bankName}_${accountLast4}"
                val hidden = _uiState.value.hiddenAccounts.toMutableSet()
                hidden.remove(key)
                sharedPrefs.edit().putStringSet("hidden_accounts", hidden).apply()

                // Remove from main account if present
                if (_uiState.value.mainAccountKey == key) {
                    sharedPrefs.edit().remove("main_account").apply()
                }

                _uiState.update {
                    it.copy(
                            hiddenAccounts = hidden,
                            mainAccountKey = if (it.mainAccountKey == key) null else it.mainAccountKey,
                            successMessage =
                                    "Account deleted successfully ($deletedCount balance records removed)"
                    )
                }

                // Clear message after delay
                delay(3000)
                _uiState.update { it.copy(successMessage = null) }

                loadCards() // Reload cards to update UI
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to delete account: ${e.message}") }
            }
        }
    }

    fun editAccount(
            oldBankName: String,
            accountLast4: String,
            newBankName: String,
            newBalance: BigDecimal,
            newCreditLimit: BigDecimal?,
            isCreditCard: Boolean,
            isWallet: Boolean,
            newIconResId: Int,
            newIconName: String,
            newColorHex: String,
            newCurrency: String? = null
    ) {
        viewModelScope.launch {
            try {
                val latestBalance = accountBalanceRepository.getLatestBalance(oldBankName, accountLast4)
                val effectiveCurrency = newCurrency?.takeIf { it.isNotBlank() }
                    ?: latestBalance?.currency
                    ?: resolveDefaultCurrency()

                if (latestBalance != null && (effectiveCurrency != latestBalance.currency ||
                        isCreditCard != latestBalance.isCreditCard || isWallet != latestBalance.isWallet)) {
                    BankAccountMergeStore.mutationMutex.withLock { bankAccountMerges.forgetAccount(oldBankName, accountLast4) }
                }
                // Update bank name if changed
                if (newBankName != oldBankName) {
                    BankAccountMergeStore.mutationMutex.withLock {
                        database.withTransaction {
                            retargetTransferLegs(oldBankName, accountLast4, newBankName, accountLast4)
                            transactionRepository.updateAccountForTransactions(oldBankName, accountLast4, newBankName, accountLast4)
                            accountBalanceRepository.updateAccountBankName(oldBankName, accountLast4, newBankName)
                        }
                        bankAccountMerges.forgetAccount(oldBankName, accountLast4)
                    }

                    // Update hidden accounts preference if bank name changed
                    val oldKey = "${oldBankName}_${accountLast4}"
                    val newKey = "${newBankName}_${accountLast4}"
                    val hidden = _uiState.value.hiddenAccounts.toMutableSet()
                    if (hidden.contains(oldKey)) {
                        hidden.remove(oldKey)
                        hidden.add(newKey)
                        sharedPrefs.edit { putStringSet("hidden_accounts", hidden) }
                        _uiState.update { it.copy(hiddenAccounts = hidden) }
                    }

                    // Update main account preference if bank name changed
                    if (_uiState.value.mainAccountKey == oldKey) {
                        sharedPrefs.edit { putString("main_account", newKey) }
                        _uiState.update { it.copy(mainAccountKey = newKey) }
                    }
                }

                latestBalance?.let { ignoredAccounts.move(it, it.copy(bankName = newBankName, currency = effectiveCurrency)) }
                // Insert new balance record with updated values
                accountBalanceRepository.insertBalance(
                    AccountBalanceEntity(
                        bankName = newBankName,
                        accountLast4 = accountLast4,
                        balance = newBalance,
                        creditLimit = newCreditLimit,
                        timestamp = LocalDateTime.now(),
                        isCreditCard = isCreditCard,
                        isWallet = isWallet,
                        sourceType = "MANUAL",
                        iconResId = newIconResId,
                        iconName = newIconName,
                        currency = effectiveCurrency,
                        color = newColorHex
                    )
                )

                _uiState.update { it.copy(successMessage = "Account updated successfully") }


                delay(3000)
                _uiState.update { it.copy(successMessage = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to update account: ${e.message}") }
            }
        }
    }

    private suspend fun retargetTransferLegs(sourceBank: String, sourceSuffix: String, targetBank: String, targetSuffix: String) {
        val dao = database.transactionDao()
        dao.getAccountTransferLegRefIds(sourceBank, sourceSuffix).chunked(500).forEach { ids ->
            dao.retargetTransferLegRefs(ids, sourceBank, targetBank, sourceSuffix, targetSuffix, LocalDateTime.now())
        }
    }

    fun repairDuplicateAccounts(source: AccountBalanceEntity, target: AccountBalanceEntity) {
        val pair = BankAccountMergeStore.duplicatePairs(_uiState.value.accounts).firstOrNull {
            it.first.bankName == source.bankName && it.first.accountLast4 == source.accountLast4 &&
                it.second.bankName == target.bankName && it.second.accountLast4 == target.accountLast4
        } ?: return
        mergeAccounts(pair.second, listOf(pair.first), null)
    }

    fun mergeAccounts(targetAccount: AccountBalanceEntity, sourceAccounts: List<AccountBalanceEntity>, newBalance: BigDecimal?) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                require(sourceAccounts.isNotEmpty()) { "Select at least one source account" }
                BankAccountMergeStore.mutationMutex.withLock {
                    val target = accountBalanceRepository.getLatestBalance(targetAccount.bankName, targetAccount.accountLast4)
                        ?: error("Target account no longer exists")
                    val sources = sourceAccounts.distinctBy { it.bankName to it.accountLast4 }.map { source ->
                        accountBalanceRepository.getLatestBalance(source.bankName, source.accountLast4)
                            ?: error("Source account no longer exists")
                    }
                    require(sources.none { it.bankName == target.bankName && it.accountLast4 == target.accountLast4 }) { "Choose different accounts" }
                    require(sources.all { it.currency == target.currency }) { "Accounts must use the same currency" }
                    require(sources.all { it.isCreditCard == target.isCreditCard && it.isWallet == target.isWallet }) { "Accounts must have the same type" }
                    database.withTransaction {
                        sources.forEach { source ->
                            retargetTransferLegs(source.bankName, source.accountLast4, target.bankName, target.accountLast4)
                            transactionRepository.updateAccountForTransactions(source.bankName, source.accountLast4, target.bankName, target.accountLast4)
                            (_uiState.value.linkedCards[source.accountLast4] ?: emptyList())
                                .filter { it.bankName == source.bankName && it.currency == source.currency }
                                .forEach { card ->
                                    if (source.bankName == target.bankName) cardRepository.linkCardToAccount(card.id, target.accountLast4)
                                    else cardRepository.unlinkCard(card.id)
                                }
                            accountBalanceRepository.deleteAccount(source.bankName, source.accountLast4)
                        }
                        if (newBalance != null) accountBalanceRepository.insertBalance(target.copy(
                            id = 0, balance = newBalance, timestamp = LocalDateTime.now(),
                            transactionId = null, smsSource = null, sourceType = "MERGE"
                        ))
                    }
                    sources.forEach { ignoredAccounts.move(it, target); bankAccountMerges.onMerge(it, target) }
                    val sourceKeys = sources.map { "${it.bankName}_${it.accountLast4}" }.toSet()
                    val targetKey = "${target.bankName}_${target.accountLast4}"
                    val hidden = _uiState.value.hiddenAccounts - sourceKeys
                    val main = if (_uiState.value.mainAccountKey in sourceKeys) targetKey else _uiState.value.mainAccountKey
                    sharedPrefs.edit().putStringSet("hidden_accounts", hidden).putString("main_account", main).apply()
                    _uiState.update { it.copy(hiddenAccounts = hidden, mainAccountKey = main) }
                }
                _uiState.update { it.copy(isLoading = false, successMessage = "Accounts merged successfully") }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to merge accounts: ${e.message}") }
            }
        }
    }
}
