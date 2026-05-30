package com.ritesh.cashiro.presentation.ui.features.accounts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ritesh.cashiro.R
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import com.ritesh.cashiro.data.database.entity.CardEntity
import com.ritesh.cashiro.data.database.entity.CardType
import com.ritesh.cashiro.presentation.effects.overScrollVertical
import com.ritesh.cashiro.presentation.effects.rememberOverscrollFlingBehavior
import com.ritesh.cashiro.presentation.ui.components.AccountCard
import com.ritesh.cashiro.presentation.ui.components.CustomTitleTopAppBar
import com.ritesh.cashiro.presentation.ui.components.DeleteAccountDialog
import com.ritesh.cashiro.presentation.ui.components.SectionHeader
import com.ritesh.cashiro.presentation.ui.features.categories.NavigationContent
import com.ritesh.cashiro.presentation.ui.icons.Bag
import com.ritesh.cashiro.presentation.ui.icons.EyeSlash
import com.ritesh.cashiro.presentation.ui.icons.Iconax
import com.ritesh.cashiro.presentation.ui.theme.Dimensions
import com.ritesh.cashiro.presentation.ui.theme.LocalBlurEffects
import com.ritesh.cashiro.presentation.ui.theme.Spacing
import com.ritesh.cashiro.utils.CurrencyFormatter
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeEffectScope
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalHazeApi::class
)
@Composable
fun ManageAccountsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAccountDetail: (String, String) -> Unit,
    manageAccountsViewModel: ManageAccountsViewModel = hiltViewModel(),
    blurEffects: Boolean,
) {
    val uiState by manageAccountsViewModel.uiState.collectAsState()
    var showUpdateDialog by remember { mutableStateOf(false) }
    var selectedAccount by remember { mutableStateOf<Pair<String, String>?>(null) }
    var selectedAccountEntity by remember {
        mutableStateOf<AccountBalanceEntity?>(null)
    }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var historyAccount by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var accountToDelete by remember { mutableStateOf<AccountBalanceEntity?>(null) }
    var showHiddenAccounts by remember { mutableStateOf(false) }
    var showAddSheet by remember { mutableStateOf(false) }
    var showEditSheet by remember { mutableStateOf(false) }
    var accountToEdit by remember {
        mutableStateOf<AccountBalanceEntity?>(null)
    }

    var showFloatingLabel by remember { mutableStateOf(true) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val hazeState = remember { HazeState() }
    val lazyListState = rememberLazyListState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Merge Flow States
    var showMergeSelection by remember { mutableStateOf(false) }
    var showMergeBalanceOption by remember { mutableStateOf(false) }
    var showMergeConfirmation by remember { mutableStateOf(false) }
    var showMergeManualInput by remember { mutableStateOf(false) }
    var accountForMerge by remember { mutableStateOf<AccountBalanceEntity?>(null) }

    var selectedMergeAccounts by remember {
        mutableStateOf<List<AccountBalanceEntity>>(emptyList())
    }
    var mergeNewBalance by remember { mutableStateOf<BigDecimal?>(null) }
    var selectedCardForLink by remember { mutableStateOf<CardEntity?>(null) }

    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.firstVisibleItemIndex }.collect { firstVisibleItem ->
            // Show the label only when the list is scrolled to the top
            showFloatingLabel = firstVisibleItem == 0
        }
    }

    // Show snackbar messages
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            scope.launch {
                snackbarHostState.showSnackbar(it)
            }
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            scope.launch {
                snackbarHostState.showSnackbar(it)
                manageAccountsViewModel.clearError()
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CustomTitleTopAppBar(
                title = stringResource(R.string.title_accounts),
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehavior,
                hazeState = hazeState,
                hasBackButton = true,
                navigationContent = { NavigationContent(onNavigateBack) },
                actionContent = {}
            ) },
        floatingActionButton = {
            val fabContainerColor =  MaterialTheme.colorScheme.primaryContainer
            val fabContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ExtendedFloatingActionButton(
                onClick = { showAddSheet = true },
                expanded = showFloatingLabel,
                icon = { Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.add_account_fab_desc)) },
                text = { Text(text = stringResource(R.string.add_account_fab_desc)) },
                shape = if (showFloatingLabel) MaterialTheme.shapes.extraLargeIncreased else MaterialTheme.shapes.large,
                modifier = Modifier
                    .then(
                        if (blurEffects) Modifier
                            .clip(if (showFloatingLabel) MaterialTheme.shapes.extraLargeIncreased else MaterialTheme.shapes.large)
                            .hazeEffect(
                            state = hazeState,
                            block = fun HazeEffectScope.() {
                                style = HazeDefaults.style(
                                    backgroundColor = Color.Transparent,
                                    tint = HazeDefaults.tint(fabContainerColor),
                                    blurRadius = 20.dp,
                                    noiseFactor = -1f,
                                )
                                blurredEdgeTreatment = BlurredEdgeTreatment.Unbounded
                            }
                        ) else Modifier
                    ),
                containerColor = fabContainerColor,
                contentColor = fabContentColor
            ) },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = {
                    Snackbar(
                        snackbarData = it,
                        contentColor =
                            MaterialTheme.colorScheme
                                .onSecondaryContainer,
                        containerColor =
                            MaterialTheme.colorScheme
                                .secondaryContainer,
                        shape = MaterialTheme.shapes.large
                    )
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (uiState.accounts.isEmpty()) {
                // Empty State
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.md)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AccountBalance,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.no_accounts_yet),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.add_account_prompt),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(Spacing.sm))
                    }
                }
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .hazeSource(state = hazeState)
                        .overScrollVertical(),
                    flingBehavior = rememberOverscrollFlingBehavior { lazyListState },
                    contentPadding = PaddingValues(
                        start = Dimensions.Padding.content,
                        end = Dimensions.Padding.content,
                        top = Dimensions.Padding.content +
                                paddingValues.calculateTopPadding(),
                        bottom = Dimensions.Padding.content +
                                paddingValues.calculateBottomPadding()
                    ),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    // Separate visible and hidden accounts
                    val visibleRegularAccounts = uiState.accounts.filter {
                        !it.isCreditCard && !it.isWallet && !manageAccountsViewModel.isAccountHidden(
                            it.bankName,
                            it.accountLast4
                        )
                    }
                    val visibleCreditCards = uiState.accounts.filter {
                        it.isCreditCard && !manageAccountsViewModel.isAccountHidden(
                            it.bankName,
                            it.accountLast4
                        )
                    }
                    val hiddenRegularAccounts = uiState.accounts.filter {
                        !it.isCreditCard && !it.isWallet && manageAccountsViewModel.isAccountHidden(
                            it.bankName,
                            it.accountLast4
                        )
                    }
                    val hiddenCreditCards = uiState.accounts.filter {
                        it.isCreditCard && manageAccountsViewModel.isAccountHidden(
                            it.bankName,
                            it.accountLast4
                        )
                    }
                    val allRegularAccounts = uiState.accounts.filter { !it.isCreditCard && !it.isWallet }
                    val wallets = uiState.accounts.filter { it.isWallet }

                    // Wallets Section
                    if (wallets.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = stringResource(R.string.section_wallets),
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                        items(wallets) { account ->
                            AccountItem(
                                account = account,
                                linkedCards = emptyList(),
                                isHidden = false,
                                isMain = uiState.mainAccountKey == "${account.bankName}_${account.accountLast4}",
                                onSetAsMain = {
                                    manageAccountsViewModel.setAsMainAccount(
                                        account.bankName,
                                        account.accountLast4
                                    )
                                },
                                onToggleVisibility = {
                                    manageAccountsViewModel.toggleAccountVisibility(
                                        account.bankName,
                                        account.accountLast4
                                    )
                                },
                                onUpdateBalance = {
                                    selectedAccount = account.bankName to account.accountLast4
                                    selectedAccountEntity = account
                                    showUpdateDialog = true
                                },
                                onViewHistory = {
                                    historyAccount = account.bankName to account.accountLast4
                                    manageAccountsViewModel.loadBalanceHistory(
                                        account.bankName,
                                        account.accountLast4
                                    )
                                    showHistoryDialog = true
                                },
                                onUnlinkCard = {},
                                onDeleteAccount = {
                                    accountToDelete = account
                                    showDeleteConfirmDialog = true
                                },
                                onEditAccount = {
                                    accountToEdit = account
                                    showEditSheet = true
                                },
                                onAccountClick = {
                                    onNavigateToAccountDetail(account.bankName, account.accountLast4)
                                },
                                onMergeAccount = {
                                    accountForMerge = account
                                    showMergeSelection = true
                                }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(Spacing.md))
                        }
                    }

                    // Regular Bank Accounts Section (Visible Only)
                    if (visibleRegularAccounts.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = stringResource(R.string.section_bank_accounts),
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                        items(visibleRegularAccounts) { account ->
                            AccountItem(
                                account = account,
                                linkedCards = uiState.linkedCards[account.accountLast4]
                                    ?: emptyList(),
                                isHidden = false,
                                isMain = uiState.mainAccountKey == "${account.bankName}_${account.accountLast4}",
                                onSetAsMain = {
                                    manageAccountsViewModel.setAsMainAccount(
                                        account.bankName,
                                        account.accountLast4
                                    )
                                },
                                onToggleVisibility = {
                                    manageAccountsViewModel.toggleAccountVisibility(
                                        account.bankName,
                                        account.accountLast4
                                    )
                                },
                                onUpdateBalance = {
                                    selectedAccount = account.bankName to account.accountLast4
                                    selectedAccountEntity = account
                                    showUpdateDialog = true
                                },
                                onViewHistory = {
                                    historyAccount = account.bankName to account.accountLast4
                                    manageAccountsViewModel.loadBalanceHistory(
                                        account.bankName,
                                        account.accountLast4
                                    )
                                    showHistoryDialog = true
                                },
                                onUnlinkCard = { cardId ->
                                    manageAccountsViewModel.unlinkCard(cardId)
                                },
                                onDeleteAccount = {
                                    accountToDelete = account
                                    showDeleteConfirmDialog = true
                                },
                                onEditAccount = {
                                    accountToEdit = account
                                    showEditSheet = true
                                },
                                onAccountClick = {
                                    onNavigateToAccountDetail(account.bankName, account.accountLast4)
                                },
                                onMergeAccount = {
                                    accountForMerge = account
                                    showMergeSelection = true
                                }
                            )
                        }
                    }
                    // Orphaned Cards Section
                    if (uiState.orphanedCards.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(Spacing.md))

                            SectionHeader(
                                title = stringResource(R.string.section_unlinked_cards),
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                        items(uiState.orphanedCards) { card ->
                            OrphanedCardItem(
                                card = card,
                                accounts = allRegularAccounts,
                                onLinkToAccount = {
                                    selectedCardForLink = card
                                },
                                onDeleteCard = { cardId ->
                                    manageAccountsViewModel.deleteCard(cardId)
                                }
                            )
                        }
                    }
                    // Credit Cards Section (Visible Only)
                    if (visibleCreditCards.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(Spacing.md))
                            SectionHeader(
                                title = stringResource(R.string.section_credit_cards),
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }

                        items(visibleCreditCards) { card ->
                            CreditCardItem(
                                card = card,
                                isHidden = false,
                                isMain = uiState.mainAccountKey == "${card.bankName}_${card.accountLast4}",
                                onSetAsMain = {
                                    manageAccountsViewModel.setAsMainAccount(
                                        card.bankName,
                                        card.accountLast4
                                    )
                                },
                                onToggleVisibility = {
                                    manageAccountsViewModel.toggleAccountVisibility(
                                        card.bankName,
                                        card.accountLast4
                                    )
                                },
                                onUpdateBalance = {
                                    selectedAccount = card.bankName to card.accountLast4
                                    selectedAccountEntity = card
                                    showUpdateDialog = true
                                },
                                onViewHistory = {
                                    historyAccount = card.bankName to card.accountLast4
                                    manageAccountsViewModel.loadBalanceHistory(
                                        card.bankName,
                                        card.accountLast4
                                    )
                                    showHistoryDialog = true
                                },
                                onDeleteAccount = {
                                    accountToDelete = card
                                    showDeleteConfirmDialog = true
                                },
                                onEditAccount = {
                                    accountToEdit = card
                                    showEditSheet = true
                                },
                                onAccountClick = {
                                    onNavigateToAccountDetail(card.bankName, card.accountLast4)
                                },
                                onMergeAccount = {
                                    accountForMerge = card
                                    showMergeSelection = true
                                }
                            )
                        }
                    }

                    // Hidden Accounts Section
                    if (hiddenRegularAccounts.isNotEmpty() || hiddenCreditCards.isNotEmpty()
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(Spacing.md))
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(
                                        onClick = { showHiddenAccounts = !showHiddenAccounts },
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }
                                    ),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(Dimensions.Padding.content),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                                    ) {
                                        Icon(
                                            Iconax.EyeSlash,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = stringResource(R.string.hidden_accounts_count, hiddenRegularAccounts.size + hiddenCreditCards.size),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        if (showHiddenAccounts)
                                            Icons.Rounded.ExpandLess
                                        else
                                            Icons.Rounded.ExpandMore,
                                        contentDescription = if (showHiddenAccounts) stringResource(R.string.collapse) else stringResource(R.string.expand),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        if (showHiddenAccounts) {
                            // Hidden Bank Accounts
                            items(hiddenRegularAccounts) { account ->
                                AccountItem(
                                    account = account,
                                    linkedCards = uiState.linkedCards[
                                        account.accountLast4] ?: emptyList(),
                                    isHidden = true,
                                    isMain = uiState.mainAccountKey == "${account.bankName}_${account.accountLast4}",
                                    onSetAsMain = {
                                        manageAccountsViewModel.setAsMainAccount(
                                            account.bankName,
                                            account.accountLast4
                                        )
                                    },
                                    onToggleVisibility = {
                                        manageAccountsViewModel.toggleAccountVisibility(
                                            account.bankName,
                                            account.accountLast4
                                        )
                                    },
                                    onUpdateBalance = {
                                        selectedAccount =
                                            account.bankName to account.accountLast4
                                        selectedAccountEntity = account
                                        showUpdateDialog = true
                                    },
                                    onViewHistory = {
                                        historyAccount =
                                            account.bankName to account.accountLast4
                                        manageAccountsViewModel.loadBalanceHistory(
                                            account.bankName,
                                            account.accountLast4
                                        )
                                        showHistoryDialog = true
                                    },
                                    onUnlinkCard = { cardId ->
                                        manageAccountsViewModel.unlinkCard(cardId)
                                    },
                                    onDeleteAccount = {
                                        accountToDelete =
                                            account
                                        showDeleteConfirmDialog = true
                                    },
                                    onEditAccount = {
                                        accountToEdit = account
                                        showEditSheet = true
                                    },
                                    onAccountClick = {
                                        onNavigateToAccountDetail(account.bankName, account.accountLast4)
                                    },
                                    onMergeAccount = {
                                        accountForMerge = account
                                        showMergeSelection = true
                                    }
                                )
                            }
                            // Hidden Credit Cards
                            items(hiddenCreditCards) { card ->
                                CreditCardItem(
                                    card = card,
                                    isHidden = true,
                                    isMain = uiState.mainAccountKey == "${card.bankName}_${card.accountLast4}",
                                    onSetAsMain = {
                                        manageAccountsViewModel.setAsMainAccount(
                                            card.bankName,
                                            card.accountLast4
                                        )
                                    },
                                    onToggleVisibility = {
                                        manageAccountsViewModel.toggleAccountVisibility(
                                            card.bankName,
                                            card.accountLast4
                                        )
                                    },
                                    onUpdateBalance = {
                                        selectedAccount = card.bankName to card.accountLast4
                                        selectedAccountEntity = card
                                        showUpdateDialog = true
                                    },
                                    onViewHistory = {
                                        historyAccount = card.bankName to card.accountLast4
                                        manageAccountsViewModel.loadBalanceHistory(
                                            card.bankName,
                                            card.accountLast4
                                        )
                                        showHistoryDialog = true
                                    },
                                    onDeleteAccount = {
                                        accountToDelete = card
                                        showDeleteConfirmDialog = true
                                    },
                                    onEditAccount = {
                                        accountToEdit = card
                                        showEditSheet = true
                                    },
                                    onAccountClick = {
                                        onNavigateToAccountDetail(card.bankName, card.accountLast4)
                                    },
                                    onMergeAccount = {
                                        accountForMerge = card
                                        showMergeSelection = true
                                    }
                                )
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(100.dp)) }
                }
            }
        }
    }

    // Update Balance Sheet
    if (showUpdateDialog && selectedAccount != null && selectedAccountEntity != null) {
        ModalBottomSheet(
            onDismissRequest = {
                showUpdateDialog = false
                selectedAccount = null
                selectedAccountEntity = null
            },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            if (selectedAccountEntity!!.isCreditCard) {
                NumberPad(
                    initialValue =
                        selectedAccountEntity!!.balance.toPlainString(),
                    title = stringResource(R.string.update_outstanding_title),
                    bankName = selectedAccount!!.first,
                    accountLast4 = selectedAccount!!.second,
                    doneButtonLabel = stringResource(R.string.update_outstanding_title),
                    onDone = { newValue ->
                        newValue.toBigDecimalOrNull()?.let { newBalance ->
                            manageAccountsViewModel.updateCreditCard(
                                selectedAccount!!.first,
                                selectedAccount!!.second,
                                newBalance,
                                selectedAccountEntity!!.creditLimit
                                    ?: BigDecimal.ZERO
                            )
                        }
                        showUpdateDialog = false
                        selectedAccount = null
                        selectedAccountEntity = null
                    }
                )
            } else {
                NumberPad(
                    initialValue =
                        selectedAccountEntity!!.balance.toPlainString(),
                    title = stringResource(R.string.update_balance),
                    bankName = selectedAccount!!.first,
                    accountLast4 = selectedAccount!!.second,
                    doneButtonLabel = stringResource(R.string.update_balance),
                    onDone = { newValue ->
                        newValue.toBigDecimalOrNull()?.let { newBalance ->
                            manageAccountsViewModel.updateAccountBalance(
                                selectedAccount!!.first,
                                selectedAccount!!.second,
                                newBalance
                            )
                        }
                        showUpdateDialog = false
                        selectedAccount = null
                        selectedAccountEntity = null
                    }
                )
            }
        }
    }

    // Balance History Sheet
    if (showHistoryDialog && historyAccount != null) {
        ModalBottomSheet(
            onDismissRequest = {
                showHistoryDialog = false
                historyAccount = null
                manageAccountsViewModel.clearBalanceHistory()
            },
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            HistorySheet(
                bankName = historyAccount!!.first,
                accountLast4 = historyAccount!!.second,
                balanceHistory = uiState.balanceHistory,
                onDeleteBalance = { id ->
                    manageAccountsViewModel.deleteBalanceRecord(
                        id,
                        historyAccount!!.first,
                        historyAccount!!.second
                    )
                },
                onUpdateBalance = { id, newBalance ->
                    manageAccountsViewModel.updateBalanceRecord(
                        id,
                        newBalance,
                        historyAccount!!.first,
                        historyAccount!!.second
                    )
                }
            )
        }
    }

    // Delete Account Confirmation Dialog
    if (showDeleteConfirmDialog && accountToDelete != null) {
        DeleteAccountDialog(
            bankName = accountToDelete!!.bankName,
            accountLast4 = accountToDelete!!.accountLast4,
            accountIcon = accountToDelete!!.iconResId,
            accountColor = accountToDelete!!.color,
            isCreditCard = accountToDelete!!.isCreditCard,
            isWallet = accountToDelete!!.isWallet,
            onDismiss = {
                showDeleteConfirmDialog = false
                accountToDelete = null
            },
            onDelete = {
                manageAccountsViewModel.deleteAccount(
                    accountToDelete!!.bankName,
                    accountToDelete!!.accountLast4
                )
                showDeleteConfirmDialog = false
                accountToDelete = null
            },
            hazeState = hazeState,
            blurEffects = blurEffects
        )
    }

    // Edit Account Sheet
    if (showEditSheet && accountToEdit != null) {
        ModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = {
                showEditSheet = false
                accountToEdit = null
            },
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            EditAccountSheet(
                account = accountToEdit!!,
                allAccounts = uiState.accounts,
                onDismiss = {
                    showEditSheet = false
                    accountToEdit = null
                },
                onDelete = {
                    accountToDelete = accountToEdit
                    showEditSheet = false
                    accountToEdit = null
                    showDeleteConfirmDialog = true
                },
                onSave = { bankName, balance, last4, iconResId, iconName, color, isCC, isWallet, limit, currency ->
                    manageAccountsViewModel.editAccount(
                        oldBankName = accountToEdit!!.bankName,
                        accountLast4 = accountToEdit!!.accountLast4,
                        newBankName = bankName,
                        newBalance = balance,
                        newCreditLimit = limit,
                        isCreditCard = isCC,
                        isWallet = isWallet,
                        newIconResId = iconResId,
                        newIconName = iconName,
                        newColorHex = color,
                        newCurrency = currency
                    )
                    showEditSheet = false
                    accountToEdit = null
                }
            )
        }
    }

    // Add Account Sheet
    if (showAddSheet) {
        ModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = { showAddSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            EditAccountSheet(
                allAccounts = uiState.accounts,
                onDismiss = { showAddSheet = false },
                onSave = { bankName, balance, last4, iconResId, iconName, color, isCC, isWallet, limit, currency ->
                    manageAccountsViewModel.addAccount(
                        bankName = bankName,
                        balance = balance,
                        accountLast4 = last4,
                        iconResId = iconResId,
                        iconName = iconName,
                        colorHex = color,
                        isCreditCard = isCC,
                        isWallet = isWallet,
                        creditLimit = limit,
                        currency = currency
                    )
                    showAddSheet = false
                }
            )
        }
    }

    // Merge Account Dialogs
    if (showMergeSelection && accountForMerge != null) {
        MergeAccountSelectionDialog(
            currentAccount = accountForMerge!!,
            allAccounts = uiState.accounts,
            onDismiss = {
                showMergeSelection = false
                accountForMerge = null
                selectedMergeAccounts = emptyList()
            },
            onNext = { accounts ->
                selectedMergeAccounts = accounts
                showMergeSelection = false
                showMergeBalanceOption = true
            }
        )
    }

    if (showMergeBalanceOption && accountForMerge != null && selectedMergeAccounts.isNotEmpty()) {
        MergeBalanceOptionDialog(
            currentAccount = accountForMerge!!,
            selectedAccounts = selectedMergeAccounts,
            onDismiss = {
                showMergeBalanceOption = false
                accountForMerge = null
                selectedMergeAccounts = emptyList()
            },
            onOptionSelected = { option ->
                when (option) {
                    BalanceMergeOption.SUM -> {
                        val sumBalance = selectedMergeAccounts.fold(java.math.BigDecimal.ZERO) { acc, account -> acc + account.balance } + accountForMerge!!.balance
                        mergeNewBalance = sumBalance
                        showMergeBalanceOption = false
                        showMergeConfirmation = true
                    }
                    BalanceMergeOption.MANUAL -> {
                        showMergeBalanceOption = false
                        showMergeManualInput = true
                    }
                    BalanceMergeOption.NONE -> {
                        mergeNewBalance = accountForMerge!!.balance
                        showMergeBalanceOption = false
                        showMergeConfirmation = true
                    }
                }
            }
        )
    }

    if (showMergeManualInput && accountForMerge != null && selectedMergeAccounts.isNotEmpty()) {
        ModalBottomSheet(
            onDismissRequest = {
                showMergeManualInput = false
                accountForMerge = null
                selectedMergeAccounts = emptyList()
            },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            NumberPad(
                initialValue = accountForMerge!!.balance.toPlainString(),
                title = stringResource(R.string.enter_merged_balance),
                bankName = accountForMerge!!.bankName,
                accountLast4 = accountForMerge!!.accountLast4,
                doneButtonLabel = stringResource(R.string.confirm_balance),
                onDone = { newValue ->
                    newValue.toBigDecimalOrNull()?.let { newBalance ->
                        mergeNewBalance = newBalance
                        showMergeManualInput = false
                        showMergeConfirmation = true
                    }
                }
            )
        }
    }

    if (showMergeConfirmation && accountForMerge != null && selectedMergeAccounts.isNotEmpty() && mergeNewBalance != null) {
        MergeConfirmationDialog(
            onDismiss = {
                showMergeConfirmation = false
                accountForMerge = null
                selectedMergeAccounts = emptyList()
                mergeNewBalance = null
            },
            onConfirm = {
                manageAccountsViewModel.mergeAccounts(
                    targetAccount = accountForMerge!!,
                    sourceAccounts = selectedMergeAccounts,
                    newBalance = mergeNewBalance!!
                )
                showMergeConfirmation = false
                accountForMerge = null
                selectedMergeAccounts = emptyList()
                mergeNewBalance = null
            },
            hazeState = hazeState,
            blurEffects = blurEffects
        )
    }

    if (selectedCardForLink != null) {
        val card = selectedCardForLink!!
        val matchingAccounts = uiState.accounts.filter { it.bankName == card.bankName }
        LinkCardDialog(
            card = card,
            accounts = matchingAccounts,
            onDismiss = { selectedCardForLink = null },
            onConfirm = { accountLast4 ->
                scope.launch {
                    manageAccountsViewModel.linkCardToAccount(card.id, accountLast4)
                    selectedCardForLink = null
                }
            },
            hazeState = hazeState,
            blurEffects = blurEffects
        )
    }
}



@Composable
private fun CreditCardItem(
    card: AccountBalanceEntity,
    isHidden: Boolean,
    onToggleVisibility: () -> Unit,
    onUpdateBalance: () -> Unit,
    onViewHistory: () -> Unit,
    onDeleteAccount: () -> Unit,
    isMain: Boolean = false,
    onSetAsMain: () -> Unit = {},
    onEditAccount: () -> Unit = {},
    onAccountClick: () -> Unit = {},
    onMergeAccount: () -> Unit = {}
) {
    val available = (card.creditLimit ?: BigDecimal.ZERO) - card.balance
    val utilization =
        if (card.creditLimit != null && card.creditLimit > BigDecimal.ZERO) {
            ((card.balance.toDouble() / card.creditLimit.toDouble()) * 100).toInt()
        } else {
            0
        }

    val utilizationColor =
        when {
            utilization > 70 -> MaterialTheme.colorScheme.error
            utilization > 30 -> Color(0xFFFF9800) // Orange
            else -> Color(0xFF4CAF50) // Green
        }

    AccountCard(
        account = card,
        isHidden = isHidden,
        onUpdateBalance = onUpdateBalance,
        onEditAccount = onEditAccount,
        onViewHistory = onViewHistory,
        onToggleVisibility = onToggleVisibility,
        onDeleteAccount = onDeleteAccount,
        isMain = isMain,
        onSetAsMain = onSetAsMain,
        onClick = onAccountClick,
        onMergeAccount = onMergeAccount
    ) {
        // Credit Card
        Column(
            modifier = Modifier.padding(horizontal = 16.dp,vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            // Available Credit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.label_available),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = CurrencyFormatter.formatCurrency(
                        available,
                        card.currency
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Credit Limit with Utilization
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.credit_limit_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = CurrencyFormatter.formatCurrency(
                            card.creditLimit ?: BigDecimal.ZERO,
                            card.currency
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = stringResource(R.string.utilization_used, utilization),
                        style = MaterialTheme.typography.bodySmall,
                        color = utilizationColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalHazeApi::class)
@Composable
private fun AccountItem(
    account: AccountBalanceEntity,
    linkedCards: List<CardEntity> = emptyList(),
    isHidden: Boolean,
    onToggleVisibility: () -> Unit,
    onUpdateBalance: () -> Unit,
    onViewHistory: () -> Unit,
    isMain: Boolean = false,
    onSetAsMain: () -> Unit = {},
    onUnlinkCard: (cardId: Long) -> Unit = {},
    onDeleteAccount: () -> Unit = {},
    onEditAccount: () -> Unit = {},
    onAccountClick: () -> Unit = {},
    onMergeAccount: () -> Unit = {}
) {
    Column {
        AccountCard(
            account = account,
            isHidden = isHidden,
            onUpdateBalance = onUpdateBalance,
            onEditAccount = onEditAccount,
            onViewHistory = onViewHistory,
            onToggleVisibility = onToggleVisibility,
            onDeleteAccount = onDeleteAccount,
            isMain = isMain,
            onSetAsMain = onSetAsMain,
            onClick = onAccountClick,
            onMergeAccount = onMergeAccount
        ) {

            // Linked Cards Section
            if (linkedCards.isNotEmpty()) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        text = stringResource(R.string.linked_cards_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = Spacing.xs)
                    )
                    linkedCards.forEach { card ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = Spacing.xs),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp,
                            shadowElevation = 2.dp,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.md),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        "💳",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Column {
                                        Row(modifier = Modifier.padding(start = Spacing.sm),
                                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                                        ) {
                                            Text(
                                                text = "**** **** **** ${card.cardLast4}",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            if (!card.isActive
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.label_inactive),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }
                                }
                                IconButton(
                                    onClick = { onUnlinkCard(card.id) },
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    shapes = IconButtonDefaults.shapes(),
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.LinkOff,
                                        contentDescription = stringResource(R.string.unlink_card_desc),
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun OrphanedCardItem(
    card: CardEntity,
    accounts: List<AccountBalanceEntity>,
    onLinkToAccount: (String) -> Unit,
    onDeleteCard: (Long) -> Unit
) {
    var expandedSource by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(
            onClick = { expandedSource = !expandedSource },
            indication = null,
            interactionSource = remember { MutableInteractionSource() }
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)

    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimensions.Padding.content),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("💳", style = MaterialTheme.typography.titleMedium)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = card.bankName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "**** **** **** ${card.cardLast4}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )

                    }
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shapes = IconButtonDefaults.shapes()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreHoriz,
                            contentDescription = stringResource(R.string.more_options_desc),
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        shape = MaterialTheme.shapes.large,
                        containerColor = Color.Transparent,
                        shadowElevation = 0.dp,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.link_to_account)) },
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.Link,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                showMenu = false
                                onLinkToAccount("")
                            },
                            modifier = Modifier
                                .shadow(
                                    elevation = 2.dp,
                                    shape = RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = 4.dp,
                                        bottomEnd = 4.dp
                                    )
                                )
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceContainer,
                                    shape = RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = 4.dp,
                                        bottomEnd = 4.dp
                                    )
                                )
                        )

                        Spacer(modifier = Modifier.height(1.5.dp))
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(R.string.action_delete),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Iconax.Bag,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            },
                            onClick = {
                                showMenu = false
                                onDeleteCard(card.id)
                            },
                            modifier = Modifier
                                .shadow(
                                    elevation = 2.dp,
                                    shape = RoundedCornerShape(
                                        topStart = 4.dp,
                                        topEnd = 4.dp,
                                        bottomStart = 16.dp,
                                        bottomEnd = 16.dp
                                    )
                                )
                                .background(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(
                                        topStart = 4.dp,
                                        topEnd = 4.dp,
                                        bottomStart = 16.dp,
                                        bottomEnd = 16.dp
                                    )
                                )
                        )
                    }
                }
            }
            Text(
                text = "${if (card.cardType == CardType.CREDIT) stringResource(R.string.credit_card)
                else stringResource(R.string.debit_card)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Show last known balance if available
            if (card.lastBalance != null) {
                Text(
                    text = stringResource(R.string.last_balance, CurrencyFormatter.formatCurrency(card.lastBalance, card.currency)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp)
                )
            }
            // Show source SMS that triggered card detection
            if (card.lastBalanceSource != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Dimensions.Padding.content)
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(0.7f),
                            shape = RoundedCornerShape(Dimensions.Radius.md)
                        )
                        .padding(Dimensions.Padding.content)
                ) {
                    Text(
                        text = if (expandedSource) {
                            stringResource(R.string.sms_source_formatted, card.lastBalanceSource)
                        } else {
                            stringResource(R.string.sms_source_expand, card.lastBalanceSource.take(80))
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (expandedSource) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LinkCardDialog(
    card: CardEntity,
    accounts: List<AccountBalanceEntity>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    blurEffects: Boolean = LocalBlurEffects.current,
    hazeState: HazeState = remember { HazeState() }
) {
    var selectedAccount by remember { mutableStateOf<String?>(null) }
    val containerColor = MaterialTheme.colorScheme.surfaceContainerLow

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Rounded.Link,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(R.string.link_card_title))
                Text(
                    text = "${card.bankName} ••${card.cardLast4}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                if (accounts.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_accounts_found_link, card.bankName),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        text = stringResource(R.string.select_account_link_prompt),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = Spacing.xs)
                    )
                    accounts.forEach { account ->
                        val isSelected = selectedAccount == account.accountLast4
                        Surface(
                            onClick = { selectedAccount = account.accountLast4 },
                            modifier = Modifier.fillMaxWidth(),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(0.3f)
                                   else MaterialTheme.colorScheme.surface.copy(0.5f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary 
                                else MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(Spacing.md),
                                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                com.ritesh.cashiro.presentation.ui.components.BrandIcon(
                                    merchantName = account.bankName,
                                    accountIconResId = account.iconResId,
                                    accountColorHex = account.color,
                                    size = 32.dp
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "••${account.accountLast4}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = CurrencyFormatter.formatCurrency(
                                            account.balance,
                                            account.currency
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Box(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(1.5.dp),
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(0.5f),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(
                            topStart = Dimensions.Radius.xxl,
                            topEnd = Dimensions.Radius.xs,
                            bottomStart = Dimensions.Radius.xxl,
                            bottomEnd = Dimensions.Radius.xs
                        ),
                        modifier = Modifier
                            .padding(start = Spacing.xl)
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.action_cancel),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Button(
                        onClick = { selectedAccount?.let(onConfirm) },
                        enabled = selectedAccount != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape = RoundedCornerShape(
                            topStart = Dimensions.Radius.xs,
                            topEnd = Dimensions.Radius.xxl,
                            bottomStart = Dimensions.Radius.xs,
                            bottomEnd = Dimensions.Radius.xxl
                        ),
                        modifier = Modifier
                            .padding(end = Spacing.xl)
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.action_link),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        },
        containerColor = if (blurEffects) MaterialTheme.colorScheme.surfaceContainerLow.copy(0.5f)
        else MaterialTheme.colorScheme.surfaceContainerLow,
        dismissButton = {},
        modifier = Modifier
            .clip(RoundedCornerShape(Dimensions.Radius.md))
            .then(
                if (blurEffects) Modifier.hazeEffect(
                    state = hazeState,
                    block = fun HazeEffectScope.() {
                        style = HazeDefaults.style(
                            backgroundColor = Color.Transparent,
                            tint = HazeDefaults.tint(containerColor),
                            blurRadius = 20.dp,
                            noiseFactor = -1f,
                        )
                        blurredEdgeTreatment = BlurredEdgeTreatment.Unbounded
                    }
                ) else Modifier
            ),
        shape = MaterialTheme.shapes.large
    )
}


