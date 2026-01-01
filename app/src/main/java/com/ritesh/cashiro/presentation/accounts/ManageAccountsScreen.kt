package com.ritesh.cashiro.presentation.accounts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import com.ritesh.cashiro.data.database.entity.CardEntity
import com.ritesh.cashiro.presentation.categories.NavigationContent
import com.ritesh.cashiro.ui.components.CustomTitleTopAppBar
import com.ritesh.cashiro.ui.components.SectionHeader
import com.ritesh.cashiro.ui.effects.overScrollVertical
import com.ritesh.cashiro.ui.effects.rememberOverscrollFlingBehavior
import com.ritesh.cashiro.ui.theme.*
import com.ritesh.cashiro.utils.CurrencyFormatter
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import java.math.BigDecimal
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ManageAccountsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddAccount: () -> Unit,
    viewModel: ManageAccountsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showUpdateDialog by remember { mutableStateOf(false) }
    var selectedAccount by remember { mutableStateOf<Pair<String, String>?>(null) }
    var selectedAccountEntity by remember {
        mutableStateOf<com.ritesh.cashiro.data.database.entity.AccountBalanceEntity?>(null)
    }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var historyAccount by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var accountToDelete by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showHiddenAccounts by remember { mutableStateOf(false) }
    var showAddSheet by remember { mutableStateOf(false) }
    var showEditSheet by remember { mutableStateOf(false) }
    var accountToEdit by remember {
        mutableStateOf<com.ritesh.cashiro.data.database.entity.AccountBalanceEntity?>(null)
    }

    var showFloatingLabel by remember { mutableStateOf(true) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val hazeState = remember { HazeState() }

        // Show snackbar messages
        LaunchedEffect(uiState.successMessage) {
            uiState.successMessage?.let {
                scope.launch {
                    snackbarHostState.showSnackbar(it)
                // We might need a way to clear the message in VM if it's not
                // auto-cleared,
                // but VM seems to clear it after delay
                }
            }
        }

        LaunchedEffect(uiState.errorMessage) {
            uiState.errorMessage?.let {
                scope.launch {
                    snackbarHostState.showSnackbar(it)
                    viewModel.clearError()
                }
            }
        }

        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                CustomTitleTopAppBar(
                    title = "Manage Accounts",
                    scrollBehaviorSmall = scrollBehaviorSmall,
                    scrollBehaviorLarge = scrollBehavior,
                    hazeState = hazeState,
                    hasBackButton = true,
                    onBackClick = onNavigateBack,
                    navigationContent = { NavigationContent(onNavigateBack) }
                ) },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { showAddSheet = true },
                    expanded = showFloatingLabel,
                    icon = { Icon(Icons.Default.Add, contentDescription = "Add Account") },
                    text = { Text(text = "Add Account") },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape =
                        if (showFloatingLabel)
                            MaterialTheme.shapes.extraLargeIncreased
                        else MaterialTheme.shapes.large
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
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "No accounts yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Add an account to get started",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    val lazyListState = rememberLazyListState()
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
                            !it.isCreditCard && !viewModel.isAccountHidden(
                                it.bankName,
                                it.accountLast4
                            )
                        }
                        val visibleCreditCards = uiState.accounts.filter {
                            it.isCreditCard && !viewModel.isAccountHidden(
                                it.bankName,
                                it.accountLast4
                            )
                        }
                        val hiddenRegularAccounts = uiState.accounts.filter {
                            !it.isCreditCard && viewModel.isAccountHidden(
                                it.bankName,
                                it.accountLast4
                            )
                        }
                        val hiddenCreditCards = uiState.accounts.filter {
                            it.isCreditCard && viewModel.isAccountHidden(
                                it.bankName,
                                it.accountLast4
                            )
                        }
                        val allRegularAccounts = uiState.accounts.filter { !it.isCreditCard }

                        // Regular Bank Accounts Section (Visible Only)
                        if (visibleRegularAccounts.isNotEmpty()) {
                            item {
                                SectionHeader(
                                    title = "Bank Accounts",
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                            items(visibleRegularAccounts) { account ->
                                AccountItem(
                                    account = account,
                                    linkedCards = uiState.linkedCards[account.accountLast4]
                                        ?: emptyList(),
                                    isHidden = false,
                                    onToggleVisibility = {
                                        viewModel.toggleAccountVisibility(
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
                                        viewModel.loadBalanceHistory(
                                            account.bankName,
                                            account.accountLast4
                                        )
                                        showHistoryDialog = true
                                    },
                                    onUnlinkCard = { cardId ->
                                        viewModel.unlinkCard(cardId)
                                    },
                                    onDeleteAccount = {
                                        accountToDelete = account.bankName to account.accountLast4
                                        showDeleteConfirmDialog = true
                                    },
                                    onEditAccount = {
                                        accountToEdit = account
                                        showEditSheet = true
                                    }
                                )
                            }
                        }
                        // Orphaned Cards Section
                        if (uiState.orphanedCards.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(Spacing.md))
                                Text(
                                    text = "Unlinked Cards",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(vertical = Spacing.xs)
                                )
                            }
                            items(uiState.orphanedCards) { card ->
                                OrphanedCardItem(
                                    card = card,
                                    accounts = allRegularAccounts,
                                    onLinkToAccount = { accountLast4 ->
                                        viewModel.linkCardToAccount(
                                            card.id,
                                            accountLast4
                                        )
                                    },
                                    onDeleteCard = { cardId ->
                                        viewModel.deleteCard(cardId)
                                    }
                                )
                            }
                        }
                        // Credit Cards Section (Visible Only)
                        if (visibleCreditCards.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(Spacing.md))
                                Text(
                                    text = "Credit Cards",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(vertical = Spacing.xs)
                                )
                            }

                            items(visibleCreditCards) { card ->
                                CreditCardItem(
                                    card = card,
                                    isHidden = false,
                                    onToggleVisibility = {
                                        viewModel.toggleAccountVisibility(
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
                                        viewModel.loadBalanceHistory(
                                            card.bankName,
                                            card.accountLast4
                                        )
                                        showHistoryDialog = true
                                    },
                                    onDeleteAccount = {
                                        accountToDelete = card.bankName to card.accountLast4
                                        showDeleteConfirmDialog = true
                                    },
                                    onEditAccount = {
                                        accountToEdit = card
                                        showEditSheet = true
                                    }
                                )
                            }
                        }

                        // Hidden Accounts Section (Collapsible)
                        if (hiddenRegularAccounts.isNotEmpty() || hiddenCreditCards.isNotEmpty()
                        ) {
                            item {
                                Spacer(modifier = Modifier.height(Spacing.md))
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showHiddenAccounts = !showHiddenAccounts },
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
                                                Icons.Default.VisibilityOff,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "Hidden Accounts (${hiddenRegularAccounts.size + hiddenCreditCards.size})",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Icon(
                                            if (showHiddenAccounts)
                                                Icons.Default.ExpandLess
                                            else
                                                Icons.Default.ExpandMore,
                                            contentDescription = if (showHiddenAccounts) "Collapse" else "Expand",
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
                                        onToggleVisibility = {
                                            viewModel.toggleAccountVisibility(
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
                                            viewModel.loadBalanceHistory(
                                                account.bankName,
                                                account.accountLast4
                                            )
                                            showHistoryDialog = true
                                        },
                                        onUnlinkCard = { cardId ->
                                            viewModel.unlinkCard(cardId)
                                        },
                                        onDeleteAccount = {
                                            accountToDelete =
                                                account.bankName to account.accountLast4
                                            showDeleteConfirmDialog = true
                                        },
                                        onEditAccount = {
                                            accountToEdit = account
                                            showEditSheet = true
                                        }
                                    )
                                }
                                // Hidden Credit Cards
                                items(hiddenCreditCards) { card ->
                                    CreditCardItem(
                                        card = card,
                                        isHidden = true,
                                        onToggleVisibility = {
                                            viewModel.toggleAccountVisibility(
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
                                            viewModel.loadBalanceHistory(
                                                card.bankName,
                                                card.accountLast4
                                            )
                                            showHistoryDialog = true
                                        },
                                        onDeleteAccount = {
                                            accountToDelete = card.bankName to card.accountLast4
                                            showDeleteConfirmDialog = true
                                        },
                                        onEditAccount = {
                                            accountToEdit = card
                                            showEditSheet = true
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
                containerColor = MaterialTheme.colorScheme.surface,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                if (selectedAccountEntity!!.isCreditCard) {
                    // For Credit Card, we still use a dedicated layout but in a sheet?
                    // Or just use NumberPad for outstanding balance.
                    // Given the goal is "enhanced NumberPad", let's use it for balance.
                    NumberPad(
                        initialValue =
                            selectedAccountEntity!!.balance.toPlainString(),
                        title = "Update Outstanding",
                        bankName = selectedAccount!!.first,
                        accountLast4 = selectedAccount!!.second,
                        doneButtonLabel = "Update Balance",
                        onDone = { newValue ->
                            newValue.toBigDecimalOrNull()?.let { newBalance ->
                                viewModel.updateCreditCard(
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
                        title = "Update Balance",
                        bankName = selectedAccount!!.first,
                        accountLast4 = selectedAccount!!.second,
                        doneButtonLabel = "Update Balance",
                        onDone = { newValue ->
                            newValue.toBigDecimalOrNull()?.let { newBalance ->
                                viewModel.updateAccountBalance(
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

        // Balance History Dialog
        // Balance History Sheet
        if (showHistoryDialog && historyAccount != null) {
            ModalBottomSheet(
                onDismissRequest = {
                    showHistoryDialog = false
                    historyAccount = null
                    viewModel.clearBalanceHistory()
                },
                containerColor = MaterialTheme.colorScheme.surface,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                HistorySheet(
                    bankName = historyAccount!!.first,
                    accountLast4 = historyAccount!!.second,
                    balanceHistory = uiState.balanceHistory,
                    onDeleteBalance = { id ->
                        viewModel.deleteBalanceRecord(
                            id,
                            historyAccount!!.first,
                            historyAccount!!.second
                        )
                    },
                    onUpdateBalance = { id, newBalance ->
                        viewModel.updateBalanceRecord(
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
            DeleteAccountConfirmDialog(
                bankName = accountToDelete!!.first,
                accountLast4 = accountToDelete!!.second,
                onDismiss = {
                    showDeleteConfirmDialog = false
                    accountToDelete = null
                },
                onConfirm = {
                    viewModel.deleteAccount(
                        accountToDelete!!.first,
                        accountToDelete!!.second
                    )
                    showDeleteConfirmDialog = false
                    accountToDelete = null
                }
            )
        }

        // Edit Account Sheet
        if (showEditSheet && accountToEdit != null) {
            ModalBottomSheet(
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
                    onMerge = { targetAccount, sourceAccounts, newBalance ->
                        viewModel.mergeAccounts(
                            targetAccount,
                            sourceAccounts,
                            newBalance
                        )
                    },
                    onDelete = {
                        accountToDelete = accountToEdit!!.bankName to accountToEdit!!.accountLast4
                        showEditSheet = false
                        accountToEdit = null
                        showDeleteConfirmDialog = true
                    },
                    onSave = { bankName, balance, last4, icon, color ->
                        viewModel.editAccount(
                            oldBankName = accountToEdit!!.bankName,
                            accountLast4 = accountToEdit!!.accountLast4,
                            newBankName = bankName,
                            newBalance = balance,
                            newCreditLimit = accountToEdit!!.creditLimit,
                            isCreditCard = accountToEdit!!.isCreditCard,
                            newIconResId = icon
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
                onDismissRequest = { showAddSheet = false },
                containerColor = MaterialTheme.colorScheme.surface,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                EditAccountSheet(
                    allAccounts = uiState.accounts,
                    onDismiss = { showAddSheet = false },
                    onSave = { bankName, balance, last4, icon, color ->
                        viewModel.addAccount(
                            bankName = bankName,
                            balance = balance,
                            accountLast4 = last4,
                            iconResId = icon,
                            colorHex = color
                        )
                        showAddSheet = false
                    }
                )
            }
        }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AccountCard(
    account: AccountBalanceEntity,
    isHidden: Boolean,
    onUpdateBalance: () -> Unit,
    onEditAccount: () -> Unit,
    onViewHistory: () -> Unit,
    onToggleVisibility: () -> Unit,
    onDeleteAccount: () -> Unit,
    content: @Composable () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isHidden) MaterialTheme.colorScheme.surfaceVariant
            else MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Balance",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                            contentDescription = "More options",
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
                            text = { Text("Update Balance") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Update,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                showMenu = false
                                onUpdateBalance()
                            },
                            modifier =
                                Modifier.shadow(
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
                            text = { Text("Edit Details") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                showMenu = false
                                onEditAccount()
                            },
                            modifier = Modifier
                                .shadow(
                                    elevation = 2.dp,
                                    shape = RoundedCornerShape(
                                        topStart = 4.dp,
                                        topEnd = 4.dp,
                                        bottomStart = 4.dp,
                                        bottomEnd = 4.dp
                                    )
                                )
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceContainer,
                                    shape = RoundedCornerShape(
                                        topStart = 4.dp,
                                        topEnd = 4.dp,
                                        bottomStart = 4.dp,
                                        bottomEnd = 4.dp
                                    )
                                )
                        )

                        Spacer(modifier = Modifier.height(1.5.dp))
                        DropdownMenuItem(
                            text = { Text("History") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.History,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                showMenu = false
                                onViewHistory()
                            },
                            modifier = Modifier
                                .shadow(
                                    elevation = 2.dp,
                                    shape = RoundedCornerShape(
                                        topStart = 4.dp,
                                        topEnd = 4.dp,
                                        bottomStart = 4.dp,
                                        bottomEnd = 4.dp
                                    )
                                )
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceContainer,
                                    shape = RoundedCornerShape(
                                        topStart = 4.dp,
                                        topEnd = 4.dp,
                                        bottomStart = 4.dp,
                                        bottomEnd = 4.dp
                                    )
                                )
                        )

                        Spacer(modifier = Modifier.height(1.5.dp))
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (isHidden) "Show"
                                    else "Hide"
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    if (isHidden)
                                        Icons.Default
                                            .Visibility
                                    else
                                        Icons.Default
                                            .VisibilityOff,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                showMenu = false
                                onToggleVisibility()
                            },
                            modifier = Modifier
                                .shadow(
                                    elevation = 2.dp,
                                    shape = RoundedCornerShape(
                                        topStart = 4.dp,
                                        topEnd = 4.dp,
                                        bottomStart = 4.dp,
                                        bottomEnd = 4.dp
                                    )
                                )
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceContainer,
                                    shape = RoundedCornerShape(
                                        topStart = 4.dp,
                                        topEnd = 4.dp,
                                        bottomStart = 4.dp,
                                        bottomEnd = 4.dp
                                    )
                                )
                        )
                        Spacer(modifier = Modifier.height(1.5.dp))
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Delete",
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            },
                            onClick = {
                                showMenu = false
                                onDeleteAccount()
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

            // Balance
            Text(
                text = CurrencyFormatter.formatCurrency(
                    account.balance,
                    account.currency
                ),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // Bottom Section (Bank Info)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceContainerLow,
                                MaterialTheme.colorScheme.surfaceContainerLow
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = account.bankName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text =
                                "**** **** **** ${account.accountLast4}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f)
                        )
                    }
                    if (account.iconResId != 0) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = account.iconResId),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = Color.Unspecified
                                )
                            }
                        }
                    }
                }
            }
            // Extra content (e.g., Credit Card stats, Linked Cards)
            content()
        }
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
    onEditAccount: () -> Unit = {}
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
        onDeleteAccount = onDeleteAccount
    ) {
        // Credit Card specific details appended to the card
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            HorizontalDivider(
                modifier = Modifier.padding(bottom = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Available Credit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Available",
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
                    text = "Credit Limit",
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
                        text = "($utilization% used)",
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

@Composable
private fun AccountItem(
    account: AccountBalanceEntity,
    linkedCards: List<CardEntity> = emptyList(),
    isHidden: Boolean,
    onToggleVisibility: () -> Unit,
    onUpdateBalance: () -> Unit,
    onViewHistory: () -> Unit,
    onUnlinkCard: (cardId: Long) -> Unit = {},
    onDeleteAccount: () -> Unit = {},
    onEditAccount: () -> Unit = {}
) {
    Column {
        AccountCard(
            account = account,
            isHidden = isHidden,
            onUpdateBalance = onUpdateBalance,
            onEditAccount = onEditAccount,
            onViewHistory = onViewHistory,
            onToggleVisibility = onToggleVisibility,
            onDeleteAccount = onDeleteAccount
        )

        // Linked Cards Section
        if (linkedCards.isNotEmpty()) {
            Column(modifier = Modifier.padding(start = 16.dp, top = 8.dp)) {
                Text(
                    text = "Linked Cards",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = Spacing.xs)
                )
                linkedCards.forEach { card ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = Spacing.xs),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = Spacing.sm,
                                    vertical = Spacing.xs
                                ),
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
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                                    ) {
                                        Text(
                                            text = "••${card.cardLast4}",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        if (!card.isActive
                                        ) {
                                            Text(
                                                text = "(Inactive)",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                            IconButton(
                                onClick = { onUnlinkCard(card.id) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LinkOff,
                                    contentDescription = "Unlink card",
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

@Composable
private fun OrphanedCardItem(
    card: CardEntity,
    accounts: List<AccountBalanceEntity>,
    onLinkToAccount: (String) -> Unit,
    onDeleteCard: (Long) -> Unit
) {
    var showLinkDialog by remember { mutableStateOf(false) }
    var expandedSource by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { expandedSource = !expandedSource },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
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
                            text = "${card.bankName} ••${card.cardLast4}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${if (card.cardType == com.ritesh.cashiro.data.database.entity.CardType.CREDIT) "Credit" 
                            else "Debit"} Card (Unlinked)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        // Show last known balance if available
                        if (card.lastBalance != null) {
                            Text(
                                text = "Last Balance: ${CurrencyFormatter.formatCurrency(card.lastBalance, card.currency)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        // Show source SMS that triggered card detection
                        if (card.lastBalanceSource != null) {
                            Text(
                                text = if (expandedSource) {
                                        "SMS: ${card.lastBalanceSource}"
                                    } else {
                                        "SMS: ${card.lastBalanceSource.take(80)}... (tap to expand)"
                                    },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = if (expandedSource) Int.MAX_VALUE else 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    OutlinedButton(onClick = { showLinkDialog = true }) {
                        Icon(
                            Icons.Default.Link,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Link")
                    }

                    OutlinedButton(
                        onClick = { onDeleteCard(card.id) },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete")
                    }
                }
            }
        }
    }

    if (showLinkDialog) {
        LinkCardDialog(
            card = card,
            accounts = accounts.filter { it.bankName == card.bankName },
            onDismiss = { showLinkDialog = false },
            onConfirm = { accountLast4 ->
                onLinkToAccount(accountLast4)
                showLinkDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LinkCardDialog(
    card: CardEntity,
    accounts: List<AccountBalanceEntity>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var selectedAccount by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Link Card to Account")
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
                        text = "No ${card.bankName} accounts found. Add an account first.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "Select an account to link this card to:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    accounts.forEach { account ->
                        Surface(
                            onClick = { selectedAccount = account.accountLast4 },
                            modifier = Modifier.fillMaxWidth(),
                            color =
                                if (selectedAccount ==
                                    account.accountLast4
                                ) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                            shape = MaterialTheme.shapes.small,
                            border =
                                BorderStroke(
                                    1.dp,
                                    if (selectedAccount ==
                                        account.accountLast4
                                    ) { MaterialTheme.colorScheme.primary
                                    } else { MaterialTheme.colorScheme.outline
                                    }
                                )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(Spacing.sm),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "••${account.accountLast4}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = CurrencyFormatter
                                            .formatCurrency(
                                                account.balance,
                                                account.currency
                                            ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (selectedAccount ==
                                    account.accountLast4
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
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
            TextButton(
                onClick = { selectedAccount?.let(onConfirm) },
                enabled = selectedAccount != null
            ) { Text("Link") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun DeleteAccountConfirmDialog(
    bankName: String,
    accountLast4: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text("Delete Account?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(
                    text = "Are you sure you want to delete this account?",
                    style = MaterialTheme.typography.bodyMedium
                )
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(Spacing.sm),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = bankName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Account ending in $accountLast4",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Text(
                    text = "This will permanently delete all balance history for this account." +
                            " Any linked cards will be unlinked. This action cannot be undone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) { Text("Delete") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
