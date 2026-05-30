package com.ritesh.cashiro.presentation.ui.features.transactions

import android.content.Intent
import android.util.Log
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Deselect
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import com.ritesh.cashiro.presentation.ui.components.CashiroCheckbox
import com.ritesh.cashiro.presentation.ui.components.TransactionItem
import com.ritesh.cashiro.presentation.ui.components.ListItem
import com.ritesh.cashiro.presentation.ui.components.ListItemPosition
import com.ritesh.cashiro.presentation.ui.components.toShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import com.ritesh.cashiro.data.database.entity.CategoryEntity
import com.ritesh.cashiro.data.database.entity.SubcategoryEntity
import com.ritesh.cashiro.data.database.entity.SubscriptionEntity
import com.ritesh.cashiro.data.database.entity.TransactionEntity
import com.ritesh.cashiro.data.database.entity.TransactionType
import com.ritesh.cashiro.data.service.AttachmentService
import com.ritesh.cashiro.presentation.common.icons.BrandIcons
import com.ritesh.cashiro.presentation.common.icons.CategoryMapping
import com.ritesh.cashiro.presentation.effects.BlurredAnimatedVisibility
import com.ritesh.cashiro.presentation.effects.overScrollVertical
import com.ritesh.cashiro.presentation.ui.components.AccountSelectionSheet
import com.ritesh.cashiro.presentation.ui.components.AttachmentSection
import com.ritesh.cashiro.presentation.ui.components.BrandIcon
import com.ritesh.cashiro.presentation.ui.components.CashiroCard
import com.ritesh.cashiro.presentation.ui.components.CategoryIcon
import com.ritesh.cashiro.presentation.ui.components.CategorySelectionSheet
import com.ritesh.cashiro.presentation.ui.components.CustomTitleTopAppBar
import com.ritesh.cashiro.presentation.ui.components.DashedLine
import com.ritesh.cashiro.presentation.ui.components.CustomBillingCycleCard
import com.ritesh.cashiro.presentation.ui.components.DatePicker
import com.ritesh.cashiro.presentation.ui.components.DeleteTransactionDialog
import com.ritesh.cashiro.presentation.ui.components.LoadingCircle
import com.ritesh.cashiro.presentation.ui.components.PreferenceSwitch
import com.ritesh.cashiro.presentation.ui.components.SearchBarBox
import com.ritesh.cashiro.presentation.ui.components.TimePicker
import com.ritesh.cashiro.presentation.ui.features.accounts.NumberPad
import com.ritesh.cashiro.presentation.ui.features.add.AmountInput
import com.ritesh.cashiro.presentation.ui.icons.ArrowLeft02
import com.ritesh.cashiro.presentation.ui.icons.Bag
import com.ritesh.cashiro.presentation.ui.icons.Box2
import com.ritesh.cashiro.presentation.ui.icons.Calendar
import com.ritesh.cashiro.presentation.ui.icons.Card
import com.ritesh.cashiro.presentation.ui.icons.CloseCircle
import com.ritesh.cashiro.presentation.ui.icons.DocumentText2
import com.ritesh.cashiro.presentation.ui.icons.Edit2
import com.ritesh.cashiro.presentation.ui.icons.Folder2
import com.ritesh.cashiro.presentation.ui.icons.Iconax
import com.ritesh.cashiro.presentation.ui.icons.Menu
import com.ritesh.cashiro.presentation.ui.icons.Messages
import com.ritesh.cashiro.presentation.ui.icons.RefreshCircle
import com.ritesh.cashiro.presentation.ui.icons.Search
import com.ritesh.cashiro.presentation.ui.icons.VideoTime
import com.ritesh.cashiro.presentation.ui.icons.Wallet3
import com.ritesh.cashiro.presentation.ui.theme.Dimensions
import com.ritesh.cashiro.presentation.ui.theme.Spacing
import com.ritesh.cashiro.presentation.ui.theme.credit_dark
import com.ritesh.cashiro.presentation.ui.theme.credit_light
import com.ritesh.cashiro.presentation.ui.theme.expense_dark
import com.ritesh.cashiro.presentation.ui.theme.expense_light
import com.ritesh.cashiro.presentation.ui.theme.income_dark
import com.ritesh.cashiro.presentation.ui.theme.income_light
import com.ritesh.cashiro.presentation.ui.theme.investment_dark
import com.ritesh.cashiro.presentation.ui.theme.investment_light
import com.ritesh.cashiro.presentation.ui.theme.transfer_dark
import com.ritesh.cashiro.presentation.ui.theme.transfer_light
import com.ritesh.cashiro.utils.CurrencyFormatter
import com.ritesh.cashiro.utils.IconResolutionUtils
import com.ritesh.cashiro.utils.SubscriptionUtils
import com.ritesh.cashiro.utils.formatAmount
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.ui.res.stringResource
import com.ritesh.cashiro.R

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalSharedTransitionApi::class
)
@Composable
fun SharedTransitionScope.TransactionDetailScreen(
    transactionId: Long,
    sharedElementKey: String? = null,
    onNavigateBack: () -> Unit,
    transactionDetailViewModel: TransactionDetailViewModel = hiltViewModel(),
    animatedContentScope: AnimatedContentScope? = null,
    blurEffects: Boolean,
) {
    val uiState by transactionDetailViewModel.uiState.collectAsStateWithLifecycle()
    val transaction = uiState.transaction
    val isEditMode = uiState.isEditMode
    val editableTransaction = uiState.editableTransaction
    val isSaving = uiState.isSaving
    val saveSuccess = uiState.saveSuccess
    val errorMessage = uiState.errorMessage
    val applyToAllFromMerchant = uiState.applyToAllFromMerchant
    val updateExistingTransactions = uiState.updateExistingTransactions
    val existingTransactionCount = uiState.existingTransactionCount
    val showMatchPreviewSheet = uiState.showMatchPreviewSheet
    val matchedTransactions = uiState.matchedTransactions
    val selectedMatchIds = uiState.selectedMatchIds
    val showDeleteDialog = uiState.showDeleteDialog
    val isDeleting = uiState.isDeleting
    val deleteSuccess = uiState.deleteSuccess
    val accountPrimaryCurrency = uiState.primaryCurrency
    val convertedAmount = uiState.convertedAmount
    val availableAccounts by transactionDetailViewModel.availableAccounts.collectAsStateWithLifecycle()
    val allSubcategories by transactionDetailViewModel.allSubcategories.collectAsStateWithLifecycle()
    val categories by transactionDetailViewModel.categories.collectAsStateWithLifecycle()
    val linkedSubscription = uiState.subscription
    val editableAttachments by transactionDetailViewModel.editableAttachments.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val scrollBehaviorLarge = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    var showNumberPad by remember { mutableStateOf(false) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showAccountSheet by remember { mutableStateOf(false) }
    var showTargetAccountSheet by remember { mutableStateOf(false) }
    var showBillingCycleMenu by remember { mutableStateOf(false) }
    var showCustomCountPad by remember { mutableStateOf(false) }
    var showCustomUnitMenu by remember { mutableStateOf(false) }
    var showCustomEndDatePicker by remember { mutableStateOf(false) }
    val hazeState = remember { HazeState() }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Custom Billing Cycle Count Pad
    if (showCustomCountPad && isEditMode) {
        ModalBottomSheet(
            onDismissRequest = { showCustomCountPad = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            NumberPad(
                initialValue = uiState.customCycleCount.toString(),
                onDone = { newCount ->
                    transactionDetailViewModel.updateSubscriptionCustomCycleCount(newCount.toIntOrNull() ?: 1)
                    showCustomCountPad = false
                },
                title = stringResource(R.string.repeat_every)
            )
        }
    }

    // Custom Billing Cycle End Date Picker
    if (showCustomEndDatePicker && isEditMode) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = (uiState.customCycleEndDate ?: LocalDate.now())
                .atStartOfDay()
                .toInstant(java.time.ZoneOffset.UTC)
                .toEpochMilli()
        )
        DatePicker(
            onDismiss = { showCustomEndDatePicker = false },
            onConfirm = {
                datePickerState.selectedDateMillis?.let { millis ->
                    val localDate = java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                    transactionDetailViewModel.updateSubscriptionCustomCycleEndDate(localDate)
                }
                showCustomEndDatePicker = false
            },
            datePickerState = datePickerState,
            blurEffects = blurEffects,
            hazeState = hazeState
        )
    }

    // Show success snackbar
    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.transaction_updated_successfully))
                transactionDetailViewModel.clearSaveSuccess()
            }
        }
    }

    // Show error snackbar
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            scope.launch {
                snackbarHostState.showSnackbar(it)
            }
        }
    }

    LaunchedEffect(transactionId) {
        transactionDetailViewModel.loadTransaction(transactionId)
    }

    // Handle delete success
    LaunchedEffect(deleteSuccess) {
        if (deleteSuccess) {
            onNavigateBack()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehaviorLarge.nestedScrollConnection).then(
            if (animatedContentScope != null) {
                Modifier.sharedBounds(
                    rememberSharedContentState(key = sharedElementKey ?: "transaction_$transactionId"),
                    animatedVisibilityScope = animatedContentScope,
                    boundsTransform = { _, _ ->
                        spring(
                            stiffness =  Spring.StiffnessLow,
                            dampingRatio = Spring.DampingRatioLowBouncy
                        )
                    },
                    resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(ContentScale.Inside, Alignment.Center),
                    clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(Spacing.xxl))
                ).skipToLookaheadSize()
            } else {Modifier}
        ),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            // Show FABs only when not in edit mode and transaction exists
            if (!isEditMode && transaction != null) {
                Column(
                    modifier = Modifier.padding(bottom = Spacing.xxl),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Report Issue FAB
                    FloatingActionButton(
                        onClick = {
                            val reportUrl = transactionDetailViewModel.getReportUrl()
                            Log.d("TransactionDetail", "Report FAB clicked, opening URL: ${reportUrl.take(200)}...")
                            val intent = Intent(Intent.ACTION_VIEW, reportUrl.toUri())
                            try {
                                context.startActivity(intent)
                                Log.d("TransactionDetail", "Successfully launched browser intent")
                            } catch (e: Exception) {
                                Log.e("TransactionDetail", "Error launching browser", e)
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.BugReport,
                            contentDescription = stringResource(R.string.report_issue)
                        )
                    }
                }
            }
        },
        topBar = {
            CustomTitleTopAppBar(
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehaviorLarge,
                title = if (isEditMode) stringResource(R.string.edit_transaction) else stringResource(R.string.transaction_details),
                hasBackButton = true,
                hasActionButton = true,
                hazeState = hazeState,
                navigationContent = {
                    TransactionNavigationContent(
                        isEditMode = isEditMode,
                        onBackClick = {
                            if (isEditMode) {
                                transactionDetailViewModel.cancelEdit()
                            } else {
                                onNavigateBack()
                            }
                        }
                    )
                },
                actionContent = {
                    if(!isEditMode){
                        Box(
                            modifier = Modifier
                                .animateContentSize()
                                .padding(end = 16.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick ={ transactionDetailViewModel.enterEditMode() },
                                ),
                        ) {
                            IconButton(
                                onClick = { transactionDetailViewModel.enterEditMode() },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    contentColor = MaterialTheme.colorScheme.onBackground
                                )
                            ) {
                                Icon(
                                    imageVector = Iconax.Edit2,
                                    contentDescription = stringResource(R.string.edit),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    } else{
                        Box(modifier = Modifier.size(32.dp)) //for edit transaction title alignment
                    }
                }
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            val displayTransaction = if (isEditMode) editableTransaction else transaction
            displayTransaction?.let { txn ->
                TransactionDetailContent(
                    transaction = txn,
                    isEditMode = isEditMode,
                    applyToAllFromMerchant = applyToAllFromMerchant,
                    updateExistingTransactions = updateExistingTransactions,
                    existingTransactionCount = existingTransactionCount,
                    viewModel = transactionDetailViewModel,
                    accountPrimaryCurrency = accountPrimaryCurrency,
                    convertedAmount = convertedAmount,
                    availableAccounts = availableAccounts,
                    onAmountClick = { showNumberPad = true },
                    onCategoryClick = { showCategoryMenu = true },
                    onAccountClick = { showAccountSheet = true },
                    onTargetAccountClick = { showTargetAccountSheet = true },
                    showBillingCycleMenu = showBillingCycleMenu,
                    onBillingCycleMenuChange = { showBillingCycleMenu = it },
                    showCustomCountPad = { showCustomCountPad = it },
                    showCustomUnitMenu = { showCustomUnitMenu = it },
                    showCustomEndDatePicker = { showCustomEndDatePicker = it },
                    paddingValues = paddingValues,
                    categories = categories,
                    subcategoriesMap = allSubcategories,
                    linkedSubscription = linkedSubscription,
                    editableAttachments = editableAttachments,
                    onAddAttachment = transactionDetailViewModel::addAttachment,
                    onRemoveAttachment = transactionDetailViewModel::removeAttachment,
                    blurEffects = blurEffects,
                    hazeState = hazeState,
                    accountIconName = uiState.accountIconName
                )
            }

            if (isEditMode) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.surface,
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        ),
                    contentAlignment = Alignment.BottomCenter
                ){
                    TransactionSaveContent(
                        isSaving = isSaving,
                        onSaveClick = { transactionDetailViewModel.saveChanges() },
                        modifier = Modifier
                    )
                }
            } else{
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.surface,
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        ),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    TextButton(
                        onClick = { transactionDetailViewModel.showDeleteDialog() },
                        enabled = !isSaving,
                        shapes = ButtonDefaults.shapes(),
                        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().navigationBarsPadding(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        contentPadding = PaddingValues(vertical = Spacing.md)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Iconax.Bag,
                                contentDescription = stringResource(R.string.delete_transaction),
                                modifier = Modifier.size(Dimensions.Icon.small)
                            )
                            Spacer(modifier = Modifier.width(Spacing.xs))
                            Text(
                                text = stringResource(R.string.delete),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }

    // NumberPad for Amount Input
    if (showNumberPad && isEditMode) {
        ModalBottomSheet(
            onDismissRequest = { showNumberPad = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            val amount = editableTransaction?.amount?.stripTrailingZeros()?.toPlainString() ?: "0"
            NumberPad(
                initialValue = amount,
                onDone = { newAmount ->
                    transactionDetailViewModel.updateAmount(newAmount)
                    showNumberPad = false
                },
                title = stringResource(R.string.enter_amount)
            )
        }
    }

    // Category Selection Sheet
    if (showCategoryMenu) {
        ModalBottomSheet(
            onDismissRequest = { showCategoryMenu = false },
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            CategorySelectionSheet(
                categories = categories,
                subcategoriesMap = allSubcategories,
                onSelectionComplete = { category, subcategory ->
                    transactionDetailViewModel.updateCategory(category.name)
                    transactionDetailViewModel.updateSubcategory(subcategory?.name)
                    showCategoryMenu = false
                },
                onDismiss = { showCategoryMenu = false }
            )
        }
    }

    // Account Selection Sheets
    if (showAccountSheet) {
        val accounts by transactionDetailViewModel.availableAccounts.collectAsStateWithLifecycle()
        val selectedAccount by transactionDetailViewModel.selectedAccount.collectAsStateWithLifecycle()
        ModalBottomSheet(
            onDismissRequest = { showAccountSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            AccountSelectionSheet(
                accounts = accounts,
                selectedAccount = selectedAccount,
                onAccountSelected = {
                    transactionDetailViewModel.updateTransactionAccount(it)
                    showAccountSheet = false
                },
                showNoneOption = false
            )
        }
    }

    if (showTargetAccountSheet) {
        val accounts by transactionDetailViewModel.availableAccounts.collectAsStateWithLifecycle()
        val targetAccount by transactionDetailViewModel.targetAccount.collectAsStateWithLifecycle()
        ModalBottomSheet(
            onDismissRequest = { showTargetAccountSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            AccountSelectionSheet(
                accounts = accounts,
                selectedAccount = targetAccount,
                title = stringResource(R.string.select_target_account),
                onAccountSelected = {
                    transactionDetailViewModel.updateTransactionTargetAccount(it)
                    showTargetAccountSheet = false
                },
                showNoneOption = false
            )
        }
    }

    // Match Preview Sheet — allows granular per-transaction selection before applying
    if (showMatchPreviewSheet) {
        val previewSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { transactionDetailViewModel.hideMatchPreviewSheet() },
            sheetState = previewSheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            MatchPreviewSheetContent(
                matchedTransactions = matchedTransactions,
                selectedMatchIds = selectedMatchIds,
                searchQuery = uiState.matchSearchQuery,
                searchResults = uiState.matchSearchResults,
                onSearchQueryChange = { transactionDetailViewModel.updateMatchSearchQuery(it) },
                onAddSearchResult = { transactionDetailViewModel.addTransactionToMatchList(it) },
                onToggleSelection = { transactionDetailViewModel.toggleMatchSelection(it) },
                onSelectAll = { transactionDetailViewModel.selectAllMatches() },
                onDeselectAll = { transactionDetailViewModel.deselectAllMatches() },
                onApply = {
                    transactionDetailViewModel.applyToSelectedMatches()
                    scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.updated_transactions_format, selectedMatchIds.size)) }
                },
                onDismiss = { transactionDetailViewModel.hideMatchPreviewSheet() },
                newCategory = editableTransaction?.category ?: ""
            )
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        DeleteTransactionDialog(
            onDismiss = { transactionDetailViewModel.hideDeleteDialog() },
            onDelete = { transactionDetailViewModel.deleteTransaction() },
            isDeleting = isDeleting,
            blurEffects = blurEffects,
            hazeState = hazeState
        )
    }
}

@Composable
private fun TransactionNavigationContent(
    isEditMode: Boolean,
    onBackClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .animateContentSize()
            .padding(start = 16.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onBackClick,
            ),
    ) {
        IconButton(
            onClick = onBackClick,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onBackground
            )
        ) {
            Icon(
                imageVector = if (isEditMode) Icons.Rounded.Close else Iconax.ArrowLeft02,
                contentDescription = if (isEditMode) stringResource(R.string.cancel) else null,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TransactionSaveContent(
    modifier: Modifier = Modifier,
    isSaving: Boolean,
    onSaveClick: () -> Unit
) {
    TextButton(
        onClick = onSaveClick,
        enabled = !isSaving,
        shapes = ButtonDefaults.shapes(),
        modifier = modifier.padding(horizontal = 16.dp).fillMaxWidth().navigationBarsPadding(),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        ),
        contentPadding = PaddingValues(vertical = Spacing.md)
    ) {
        if (isSaving) {
            LoadingCircle(
                modifier = Modifier.size(Dimensions.Icon.small)
            )
        } else {
            Text(
                text = stringResource(R.string.save),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TransactionDetailContent(
    modifier: Modifier = Modifier,
    transaction: TransactionEntity,
    isEditMode: Boolean,
    applyToAllFromMerchant: Boolean,
    updateExistingTransactions: Boolean,
    existingTransactionCount: Int,
    viewModel: TransactionDetailViewModel,
    accountPrimaryCurrency: String,
    convertedAmount: BigDecimal?,
    onAmountClick: () -> Unit,
    onCategoryClick: () -> Unit,
    onAccountClick: () -> Unit,
    onTargetAccountClick: () -> Unit,
    showBillingCycleMenu: Boolean,
    onBillingCycleMenuChange: (Boolean) -> Unit,
    showCustomCountPad: (Boolean) -> Unit,
    showCustomUnitMenu: (Boolean) -> Unit,
    showCustomEndDatePicker: (Boolean) -> Unit,
    paddingValues: PaddingValues,
    availableAccounts: List<AccountBalanceEntity>,
    categories: List<CategoryEntity>,
    subcategoriesMap: Map<Long, List<SubcategoryEntity>>,
    linkedSubscription: SubscriptionEntity? = null,
    editableAttachments: List<String> = emptyList(),
    onAddAttachment: (String) -> Unit = {},
    onRemoveAttachment: (String) -> Unit = {},
    blurEffects: Boolean,
    hazeState: HazeState = remember { HazeState()},
    accountIconName: String?
) {

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .hazeSource(state = hazeState)
            .overScrollVertical()
            .verticalScroll(
                state = rememberScrollState()
            )
            .padding(
                start = Dimensions.Padding.content,
                end = Dimensions.Padding.content,
                top = Dimensions.Padding.content +
                        paddingValues.calculateTopPadding()
            ),
    ) {
        // Header with amount and merchant
        BlurredAnimatedVisibility(
            visible = isEditMode,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
        ) {
            val categoryEntity = categories.find { it.name == transaction.category }
            val subcategoryEntity = if (categoryEntity != null && transaction.subcategory != null) {
                subcategoriesMap[categoryEntity.id]?.find { it.name == transaction.subcategory }
            } else null

            Column(
                modifier =  Modifier
                    .animateContentSize(
                        MaterialTheme.motionScheme.fastSpatialSpec()
                    )
            ) {
                EditableTransactionHeader(
                    transaction = transaction,
                    viewModel = viewModel,
                    onAmountClick = onAmountClick,
                    categoryEntity = categoryEntity,
                    subcategoryEntity = subcategoryEntity,
                    blurEffects = blurEffects,
                    hazeState = hazeState,
                    accountIconName = accountIconName
                )
                Spacer(modifier = Modifier.height(Spacing.lg))
                // SMS Body - Always read-only
                if (!transaction.smsBody.isNullOrBlank()) {
                    SmsBodyCard(transaction.smsBody)
                    Spacer(modifier = Modifier.height(Spacing.md))
                }

                EditableExtractedInfoCard(
                    transaction = transaction,
                    applyToAllFromMerchant = applyToAllFromMerchant,
                    updateExistingTransactions = updateExistingTransactions,
                    existingTransactionCount = existingTransactionCount,
                    onTargetAccountClick = onTargetAccountClick,
                    showBillingCycleMenu = showBillingCycleMenu,
                    onBillingCycleMenuChange = onBillingCycleMenuChange,
                    showCustomCountPad = showCustomCountPad,
                    showCustomUnitMenu = showCustomUnitMenu,
                    showCustomEndDatePicker = showCustomEndDatePicker,
                    viewModel = viewModel,
                    onCategoryClick = onCategoryClick,
                    onAccountClick = onAccountClick
                )

                // Attachments Section in Edit Mode
                Spacer(modifier = Modifier.height(Spacing.md))
                AttachmentSection(
                    attachments = editableAttachments,
                    attachmentService = viewModel.attachmentService,
                    onAddAttachment = onAddAttachment,
                    onRemoveAttachment = onRemoveAttachment,
                    onAttachmentClick = { /* Preview handled internally */ },
                    isEditable = true
                )
                Spacer(modifier = Modifier.height(300.dp)) // For better scroll space
            }

        }
        BlurredAnimatedVisibility(
            visible = !isEditMode,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
        ) {
            val categoryEntity = categories.find { it.name == transaction.category }
            val subcategoryEntity = if (categoryEntity != null && transaction.subcategory != null) {
                subcategoriesMap[categoryEntity.id]?.find { it.name == transaction.subcategory }
            } else null

            Column {
                TransactionReceipt(
                    transaction,
                    accountPrimaryCurrency,
                    convertedAmount,
                    availableAccounts,
                    categories,
                    subcategoriesMap,
                    linkedSubscription,
                    viewModel.attachmentService
                )
                Spacer(modifier = Modifier.height(300.dp)) // for better scroll
            }
        }
    }
}


@Composable
private fun SmsBodyCard(smsBody: String) {
    CashiroCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = Spacing.md, vertical =  Spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Iconax.Messages,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    text = stringResource(R.string.original_sms),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // SMS text in monospace font
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = smsBody,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    modifier = Modifier.padding(Spacing.md)
                )
            }
        }
    }
}

@Composable
private fun EditableTransactionHeader(
    transaction: TransactionEntity,
    viewModel: TransactionDetailViewModel,
    onAmountClick: () -> Unit,
    categoryEntity: CategoryEntity? = null,
    subcategoryEntity: SubcategoryEntity? = null,
    blurEffects: Boolean,
    hazeState: HazeState = remember { HazeState()},
    accountIconName: String?
) {
    CashiroCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        contentPadding = 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // Amount Input
            AmountInput(
                amount = transaction.amount.stripTrailingZeros().toPlainString(),
                currencySymbol = CurrencyFormatter.getCurrencySymbol(transaction.currency),
                onClick = onAmountClick,
                modifier = Modifier.fillMaxWidth()
            )

            // Transaction Type
            Column(modifier = Modifier.fillMaxWidth()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TransactionType.entries.forEach { type ->
                        FilterChip(
                            selected = transaction.transactionType == type,
                            onClick = { viewModel.updateTransactionType(type) },
                            label = {
                                Text(
                                    text = type.name.lowercase().let { if (it.isNotEmpty()) it.take(1).uppercase() + it.drop(1) else it },
                                    maxLines = 1
                                )
                            },
                            leadingIcon = if (transaction.transactionType == type) {
                                {
                                    Icon(
                                        imageVector = when (type) {
                                            TransactionType.INCOME -> Icons.AutoMirrored.Filled.TrendingUp
                                            TransactionType.EXPENSE -> Icons.AutoMirrored.Filled.TrendingDown
                                            TransactionType.CREDIT -> Iconax.Card
                                            TransactionType.TRANSFER -> Icons.Rounded.SwapHoriz
                                            TransactionType.INVESTMENT -> Icons.AutoMirrored.Filled.ShowChart
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(Dimensions.Icon.small)
                                    )
                                }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(0.7f),
                                labelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderWidth = 0.dp,
                                selected = transaction.transactionType == type,
                                enabled = true
                            ),
                        )
                    }
                }
            }

            // Date and Time
            DateTimeField(
                dateTime = transaction.dateTime,
                onDateTimeChange = { viewModel.updateDateTime(it) },
                blurEffects = blurEffects,
                hazeState = hazeState
            )


            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(1.5.dp)
            ) {
                // Merchant Name
                TextField(
                    value = transaction.merchantName,
                    onValueChange = { viewModel.updateMerchantName(it) },
                    label = { Text(stringResource(R.string.merchant_label), fontWeight = FontWeight.SemiBold) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = 4.dp,
                        bottomEnd = 4.dp
                    ),
                    leadingIcon = {
                        BrandIcon(
                            merchantName = transaction.merchantName,
                            size = 26.dp,
                            showBackground = false,
                            categoryEntity = categoryEntity,
                            subcategoryEntity = subcategoryEntity,
                            accountIconName = accountIconName
                        )
                    },
                    isError = transaction.merchantName.isBlank(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f),
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        disabledIndicatorColor = Color.Transparent,
                        disabledLabelColor = MaterialTheme.colorScheme.primary,
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                // Description
                TextField(
                    value = transaction.description ?: "",
                    onValueChange = { viewModel.updateDescription(it) },
                    label = { Text(stringResource(R.string.description_optional_label), fontWeight = FontWeight.SemiBold) },
                    shape = RoundedCornerShape(
                        topStart = 4.dp,
                        topEnd = 4.dp,
                        bottomStart = 16.dp,
                        bottomEnd = 16.dp
                    ),
                    leadingIcon = {
                        Icon(
                            Iconax.DocumentText2,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f)
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EditableExtractedInfoCard(
    transaction: TransactionEntity,
    applyToAllFromMerchant: Boolean,
    updateExistingTransactions: Boolean,
    existingTransactionCount: Int,
    onCategoryClick: () -> Unit,
    onAccountClick: () -> Unit,
    onTargetAccountClick: () -> Unit,
    showBillingCycleMenu: Boolean,
    onBillingCycleMenuChange: (Boolean) -> Unit,
    showCustomCountPad: (Boolean) -> Unit,
    showCustomUnitMenu: (Boolean) -> Unit,
    showCustomEndDatePicker: (Boolean) -> Unit,
    viewModel: TransactionDetailViewModel
) {
    val selectedAccount by viewModel.selectedAccount.collectAsStateWithLifecycle()
    val targetAccount by viewModel.targetAccount.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val allSubcategories by viewModel.allSubcategories.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isCustomCycle = uiState.isCustomCycle

    val selectedCategoryObj = remember(transaction.category, categories) {
        categories.find { it.name == transaction.category }
    }
    
    val categoryId = selectedCategoryObj?.id
    val selectedSubcategoryObj = remember(transaction.subcategory, categoryId, allSubcategories) {
        if (categoryId != null) {
            allSubcategories[categoryId]?.find { it.name == transaction.subcategory }
        } else null
    }

    CashiroCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        contentPadding = 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Account Selection
            val transactionType = transaction.transactionType
            
            BlurredAnimatedVisibility(
                visible = transactionType == TransactionType.TRANSFER,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
            ) {
                // Transfer Type UI
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(
                            MaterialTheme.motionScheme.fastSpatialSpec()
                        ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(1.5.dp)
                        ) {
                            // Source Account Card
                            Card(
                                onClick = onAccountClick,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = 4.dp,
                                    bottomEnd = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                ),
                                border = BorderStroke(0.dp, Color.Transparent)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    BrandIcon(
                                        merchantName = selectedAccount?.bankName ?: "",
                                        accountIconResId = selectedAccount?.iconResId ?: 0,
                                        accountIconName = selectedAccount?.iconName,
                                        size = 26.dp,
                                        showBackground = false
                                    )

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = selectedAccount?.bankName
                                                ?: stringResource(R.string.select_source_account),
                                            style = MaterialTheme.typography.bodyLarge,
                                            color =
                                                if (selectedAccount != null)
                                                    MaterialTheme.colorScheme.onSurface
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (selectedAccount != null) {
                                            Text(
                                                text = if (selectedAccount?.accountLast4 == "wallet") "${selectedAccount?.accountLast4}" else "••${selectedAccount?.accountLast4}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Icon(
                                        imageVector = Icons.Rounded.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Target Account Card
                            Card(
                                onClick = onTargetAccountClick,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(
                                    topStart = 4.dp,
                                    topEnd = 4.dp,
                                    bottomStart = 16.dp,
                                    bottomEnd = 16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                ),
                                border = BorderStroke(0.dp, Color.Transparent)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    BrandIcon(
                                        merchantName = targetAccount?.bankName ?: "",
                                        accountIconResId = targetAccount?.iconResId ?: 0,
                                        accountIconName = targetAccount?.iconName,
                                        size = 26.dp,
                                        showBackground = false
                                    )

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = targetAccount?.bankName
                                                ?: stringResource(R.string.select_target_account),
                                            style = MaterialTheme.typography.bodyLarge,
                                            color =
                                                if (targetAccount != null)
                                                    MaterialTheme.colorScheme.onSurface
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (targetAccount != null) {
                                            Text(
                                                text = if (targetAccount?.accountLast4 == "wallet") "${targetAccount?.accountLast4}" else "••${targetAccount?.accountLast4}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Icon(
                                        imageVector = Icons.Rounded.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        // Exchange Icon
                        Box(
                            modifier = Modifier.fillMaxWidth().align(Alignment.Center),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .shadow(elevation = 3.dp, shape = CircleShape)
                                    .clip(CircleShape)
                                    .background(
                                        MaterialTheme.colorScheme.surface,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SwapVert,
                                    contentDescription = stringResource(R.string.transfer_action),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Category Selection
                    CategoryDropdown(
                        selectedCategory = transaction.category,
                        selectedSubcategory = transaction.subcategory,
                        onClick = onCategoryClick,
                        isTransferType = transactionType == TransactionType.TRANSFER,
                        viewModel = viewModel
                    )
                }
            }
            
            BlurredAnimatedVisibility(
                visible = transactionType != TransactionType.TRANSFER,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
            ){
                // Non-Transfer Type UI
                Column(
                    modifier = Modifier
                        .animateContentSize(
                            MaterialTheme.motionScheme.fastSpatialSpec()
                        )
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(1.5.dp)
                ) {
                    Card(
                        onClick = onAccountClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = 4.dp,
                            bottomEnd = 4.dp
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        border = BorderStroke(0.dp, Color.Transparent)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                                BrandIcon(
                                    merchantName = selectedAccount?.bankName ?: "",
                                    accountIconResId = selectedAccount?.iconResId ?: 0,
                                    accountIconName = selectedAccount?.iconName,
                                    size = 26.dp,
                                    showBackground = false
                                )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedAccount?.bankName ?: stringResource(R.string.select_account),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color =
                                        if (selectedAccount != null)
                                            MaterialTheme.colorScheme.onSurface
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (selectedAccount != null) {
                                    Text(
                                        text = if (selectedAccount?.accountLast4 == "wallet") "${selectedAccount?.accountLast4}" else "••${selectedAccount?.accountLast4}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // Category Selection
                    CategoryDropdown(
                        selectedCategory = transaction.category,
                        selectedSubcategory = transaction.subcategory,
                        onClick = onCategoryClick,
                        viewModel = viewModel
                    )
                }
            }

            // ── Merchant category helpers ─────────────────────────────────
            Spacer(modifier = Modifier.height(Spacing.sm))

            // Option 1: Apply this category to all FUTURE transactions
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(MaterialTheme.motionScheme.fastSpatialSpec())
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            onClick = {viewModel.toggleApplyToAllFromMerchant()},
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        )
                        .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CashiroCheckbox(
                        checked = applyToAllFromMerchant,
                        onCheckedChange = { viewModel.toggleApplyToAllFromMerchant() }
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text(
                        text = stringResource(R.string.apply_category_to_all_future_transactions_format, transaction.merchantName),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                AnimatedVisibility(visible = applyToAllFromMerchant) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = Spacing.sm, end = Spacing.sm, bottom = Spacing.xs),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(
                                    horizontal = Spacing.md,
                                    vertical = Spacing.sm
                                ),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = stringResource(R.string.manual_entry_warning),
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    }
                }
            }

            // Option 2: Update EXISTING transactions (only shown when there are matches)
            if (existingTransactionCount > 0) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(MaterialTheme.motionScheme.fastSpatialSpec())
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                onClick = {viewModel.toggleUpdateExistingTransactions()},
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            )
                            .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CashiroCheckbox(
                            checked = updateExistingTransactions,
                            onCheckedChange = { viewModel.toggleUpdateExistingTransactions() }
                        )
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Text(
                            text = stringResource(R.string.update_existing_transactions_format, existingTransactionCount, if (existingTransactionCount == 1) stringResource(R.string.transaction) else stringResource(R.string.transactions_plural), transaction.merchantName),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    // Preview button — only visible when checkbox is ON
                    AnimatedVisibility(visible = updateExistingTransactions) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = Spacing.sm, end = Spacing.sm, bottom = Spacing.xs),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Surface(
                                onClick = { viewModel.showMatchPreviewSheet() },
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.wrapContentSize()
                            ) {
                                Row(
                                    modifier = Modifier.padding(
                                        horizontal = Spacing.md,
                                        vertical = Spacing.sm
                                    ),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ExpandMore,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.preview_transaction_matches),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xs))

            // Recurring Switch and Billing Cycle
            PreferenceSwitch(
                title = stringResource(R.string.recurring_transaction),
                subtitle = stringResource(R.string.mark_as_repeating_payment),
                checked = transaction.isRecurring,
                onCheckedChange = { viewModel.updateRecurringStatus(it) },
                leadingIcon = {
                    Icon(
                        Iconax.VideoTime,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                isSingle = !transaction.isRecurring,
                isFirst = transaction.isRecurring,
                padding = PaddingValues(horizontal = 0.dp, vertical = 1.5.dp)
            )

            AnimatedVisibility(visible = transaction.isRecurring) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(1.5.dp)
                ) {
                    val billingCycles = listOf(stringResource(R.string.weekly_recurring), stringResource(R.string.monthly_recurring), stringResource(R.string.quarterly_recurring), stringResource(R.string.semi_annual_recurring), stringResource(R.string.annual_recurring), stringResource(R.string.custom_recurring))
                    
                    ExposedDropdownMenuBox(
                        expanded = showBillingCycleMenu,
                        onExpandedChange = { onBillingCycleMenuChange(it) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextField(
                            value = transaction.billingCycle ?: "Monthly",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.billing_cycle), fontWeight = FontWeight.SemiBold) },
                            leadingIcon = {
                                Icon(
                                    Iconax.VideoTime,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showBillingCycleMenu) },
                            shape = RoundedCornerShape(
                                topStart = 4.dp,
                                topEnd = 4.dp,
                                bottomStart = if (isCustomCycle) 4.dp else 16.dp,
                                bottomEnd = if (isCustomCycle) 4.dp else 16.dp
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f)
                            )
                        )

                        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                        BlurredAnimatedVisibility(
                            visible = uiState.isCustomCycle,
                            enter = fadeIn() + slideInVertically { -it },
                            exit = fadeOut() + slideOutVertically { -it }
                        ) {
                            Spacer(modifier = Modifier.height(Spacing.sm))
                            CustomBillingCycleCard(
                                count = uiState.customCycleCount,
                                unit = uiState.customCycleUnit,
                                endDate = uiState.customCycleEndDate,
                                onCountClick = { showCustomCountPad(true) },
                                onUnitSelected = { viewModel.updateSubscriptionCustomCycleUnit(it) },
                                onEndDateClick = { showCustomEndDatePicker(true) },
                                onClearEndDate = { viewModel.updateSubscriptionCustomCycleEndDate(null) },
                                shape = RoundedCornerShape(
                                    topStart = 4.dp,
                                    topEnd = 4.dp,
                                    bottomStart = 16.dp,
                                    bottomEnd = 16.dp
                                )
                            )
                        }

                        ExposedDropdownMenu(
                            expanded = showBillingCycleMenu,
                            onDismissRequest = { onBillingCycleMenuChange(false) },
                            shape = MaterialTheme.shapes.large
                        ) {
                            billingCycles.forEachIndexed { index, cycle ->
                                val isFirstItem = index == 0
                                val isLastItem = index == billingCycles.lastIndex
                                val isMiddleItem = !isFirstItem && !isLastItem
                                DropdownMenuItem(
                                    text = { Text(cycle) },
                                    onClick = {
                                        viewModel.updateBillingCycle(cycle)
                                        onBillingCycleMenuChange(false)
                                    }
                                )
                                // Add a Spacer for middle items
                                if (isMiddleItem || (isFirstItem && billingCycles.size > 2) ) {
                                    HorizontalDivider(
                                        thickness = 1.5.dp,
                                        color = MaterialTheme.colorScheme.surface
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

@Composable
private fun CategoryDropdown(
    selectedCategory: String,
    selectedSubcategory: String?,
    isTransferType: Boolean = false,
    onClick: () -> Unit,
    viewModel: TransactionDetailViewModel
) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val allSubcategories by viewModel.allSubcategories.collectAsStateWithLifecycle()

    val selectedCategoryObj = remember(selectedCategory, categories) {
        categories.find { it.name == selectedCategory }
    }
    val categoryId = selectedCategoryObj?.id
    val selectedSubcategoryObj = remember(selectedSubcategory, categoryId, allSubcategories) {
        if (categoryId != null) {
            allSubcategories[categoryId]?.find { it.name == selectedSubcategory }
        } else null
    }

    val categoryInteractionSource = remember { MutableInteractionSource() }

    Column(modifier = Modifier.fillMaxWidth()) {
        TextField(
            value = selectedCategory,
            onValueChange = {},
            label = { Text(stringResource(R.string.category), fontWeight = FontWeight.SemiBold) },
            readOnly = true,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = categoryInteractionSource,
                    indication = null
                ) {
                    onClick()
                },
            shape = RoundedCornerShape(
                topEnd = if (isTransferType) 16.dp else 4.dp,
                topStart = if (isTransferType) 16.dp else 4.dp,
                bottomEnd = 16.dp,
                bottomStart = 16.dp),
            leadingIcon = {
                val context = LocalContext.current
                val resolvedResId = remember(selectedCategoryObj) {
                    selectedCategoryObj?.let { cat ->
                        if (!cat.iconName.isNullOrEmpty()) {
                            val res = IconResolutionUtils.nameToResId(context, cat.iconName)
                            if (res != 0) res else cat.iconResId
                        } else cat.iconResId
                    } ?: 0
                }

                if (resolvedResId != 0) {
                    Icon(
                        painter = painterResource(id = resolvedResId),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Icon(Iconax.Box2, contentDescription = null)
                }
            },
            trailingIcon = {
                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null)
            },
            enabled = false, // Disable typing, handle click above
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f),
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                disabledIndicatorColor = Color.Transparent,
                disabledLabelColor = MaterialTheme.colorScheme.primary,
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        // Subcategory Display (Read-only, selected via sheet)
        if (selectedSubcategory != null) {
            Spacer(modifier = Modifier.height(Spacing.md))
            TextField(
                value = selectedSubcategory,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.subcategory)) },
                leadingIcon = {
                    val context = LocalContext.current
                    val resolvedResId = remember(selectedSubcategoryObj) {
                        selectedSubcategoryObj?.let { sub ->
                            if (!sub.iconName.isNullOrEmpty()) {
                                val res = IconResolutionUtils.nameToResId(context, sub.iconName)
                                if (res != 0) res else sub.iconResId
                            } else sub.iconResId
                        } ?: 0
                    }

                    if (resolvedResId != 0) {
                        Icon(
                            painter = painterResource(id = resolvedResId),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Icon(
                            Icons.Default.SubdirectoryArrowRight,
                            contentDescription = null
                        )
                    }
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
                colors = if (selectedSubcategoryObj != null) {
                    val color = try {
                        Color(selectedSubcategoryObj.color.toColorInt())
                    } catch (_: Exception) {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    }
                    TextFieldDefaults.colors(
                        focusedContainerColor = color.copy(alpha = 0.2f),
                        unfocusedContainerColor = color.copy(alpha = 0.2f),
                        disabledContainerColor = color.copy(alpha = 0.2f),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f),
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTextColor = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f),
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        disabledIndicatorColor = Color.Transparent,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTextColor = MaterialTheme.colorScheme.onSurface
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateTimeField(
    dateTime: LocalDateTime,
    onDateTimeChange: (LocalDateTime) -> Unit,
    blurEffects: Boolean,
    hazeState: HazeState = remember { HazeState()},

) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(Dimensions.Radius.md)
                )
                .padding(4.dp)
                .clickable(
                    onClick = { showDatePicker = true },
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                val themeColors = MaterialTheme.colorScheme
                Icon(
                    imageVector = Iconax.Calendar,
                    contentDescription = stringResource(R.string.date_picker),
                    tint = themeColors.onSurface
                )
                Spacer(Modifier.size(8.dp))

                val dateLabel =
                    dateTime.format(DateTimeFormatter.ofPattern("dd MMMM"))
                val yearLabel =
                    dateTime.format(DateTimeFormatter.ofPattern("yyyy"))
                Column(
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = yearLabel,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Start,
                        color = themeColors.primary,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = dateLabel,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Start,
                        color = themeColors.onSurface,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.basicMarquee()
                    )
                }
            }
        }

        // Time Button
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp, vertical = 12.dp)
                .clickable { showTimePicker = true },
            contentAlignment = Alignment.CenterEnd
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                val hour = if (dateTime.hour % 12 == 0) 12 else dateTime.hour % 12
                val minute = dateTime.minute
                val amPm = if (dateTime.hour < 12) "AM" else "PM"

                Box(modifier = Modifier
                    .padding(5.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(0.2f),
                        shape = RoundedCornerShape(8.dp)
                    )
                ) {
                    Text(
                        text = String.format("%02d", hour),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(5.dp)
                    )
                }

                Text(
                    text = ":",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                )

                Box(
                    modifier = Modifier
                        .padding(5.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        )
                ) {
                    Text(
                        text = String.format("%02d", minute),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(5.dp)
                    )
                }

                Box(modifier = Modifier.padding(5.dp)) {
                    Text(
                        text = amPm,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dateTime.toLocalDate().toEpochDay() * 24 * 60 * 60 * 1000
        )
        DatePicker(
            onDismiss = { showDatePicker = false },
            onConfirm = {
                datePickerState.selectedDateMillis?.let { millis ->
                    val newDate = Instant.ofEpochMilli(millis)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                    onDateTimeChange(
                        dateTime.withYear(newDate.year)
                            .withMonth(newDate.monthValue)
                            .withDayOfMonth(newDate.dayOfMonth)
                    )
                }
                showDatePicker = false
            },
            datePickerState = datePickerState,
            blurEffects = blurEffects,
            hazeState = hazeState
        )
    }

    // Time Picker Dialog
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = dateTime.hour,
            initialMinute = dateTime.minute
        )
        TimePicker(
            onDismiss = { showTimePicker = false },
            onConfirm = {
                onDateTimeChange(dateTime.withHour(timePickerState.hour)
                    .withMinute(timePickerState.minute))
                showTimePicker = false
            },
            timePickerState = timePickerState,
            blurEffects = blurEffects,
            hazeState = hazeState
        )
    }
}



@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TransactionReceipt(
    transaction: TransactionEntity,
    primaryCurrency: String,
    convertedAmount: BigDecimal?,
    availableAccounts: List<AccountBalanceEntity>,
    categories: List<CategoryEntity>,
    subcategoriesMap: Map<Long, List<SubcategoryEntity>>,
    linkedSubscription: SubscriptionEntity? = null,
    attachmentService: AttachmentService
) {
    val density = LocalDensity.current
    var cutoutOffsetPx by remember { mutableFloatStateOf(with(density) { 420.dp.toPx() }) }
    val cutoutRadius = 10.dp
    val cutoutRadiusPx = with(density) { cutoutRadius.toPx() }
    val scallopRadiusPx = with(density) { 8.dp.toPx() }

    Box(
        modifier = Modifier 
            .fillMaxWidth()
            .padding(top = 24.dp)
    ) {
        // Main Receipt Card
        Surface(
            modifier = Modifier
                .animateContentSize(
                    animationSpec = tween(durationMillis = 300)
                )
                .fillMaxWidth(),
            shape = ReceiptShape(cutoutRadiusPx, cutoutOffsetPx, scallopRadiusPx),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp), // Extra top padding for badge clearance
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val categoryEntity = categories.find { it.name == transaction.category }
                val subcategoryEntity = if (categoryEntity != null && transaction.subcategory != null) {
                    subcategoriesMap[categoryEntity.id]?.find { it.name == transaction.subcategory }
                } else null

                //pill shape merchant
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ){
                    DashedLine(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    )

                    ReceiptBadge(
                        merchantName = transaction.merchantName,
                        categoryEntity = categoryEntity,
                        subcategoryEntity = subcategoryEntity
                    )
                    DashedLine(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    )
                }


                // Transaction Details Columns
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimensions.Padding.content),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Date Section
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.date),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = transaction.dateTime.format(
                                    DateTimeFormatter.ofPattern("d MMM yyyy")
                                ),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Time Section
                        val dateTime = transaction.dateTime
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End
                            ) {
                                val hour = if (dateTime.hour % 12 == 0) 12 else dateTime.hour % 12
                                val minute = dateTime.minute
                                val amPm = if (dateTime.hour < 12) "AM" else "PM"

                                Box(modifier = Modifier
                                    .padding(5.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary.copy(0.2f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                ) {
                                    Text(
                                        text = String.format("%02d", hour),
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        lineHeight = 16.sp,
                                        modifier = Modifier.padding(5.dp)
                                    )
                                }

                                Text(
                                    text = ":",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 16.sp,
                                )

                                Box(
                                    modifier = Modifier
                                        .padding(5.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                ) {
                                    Text(
                                        text = String.format("%02d", minute),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 16.sp,
                                        lineHeight = 16.sp,
                                        modifier = Modifier.padding(5.dp)
                                    )
                                }

                                Box(modifier = Modifier.padding(5.dp)) {
                                    Text(
                                        text = amPm,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 14.sp,
                                    )
                                }
                            }
                        }
                    }

                    ReceiptInfoRow(
                        label = stringResource(R.string.type),
                        value = transaction.transactionType.name.lowercase().let { if (it.isNotEmpty()) it.take(1).uppercase() + it.drop(1) else it }
                    )

                    val subcategoryValue = transaction.subcategory
                    ReceiptInfoRow(
                        label = stringResource(R.string.category),
                        value = transaction.category,
                        subValue = subcategoryValue,
                        icon = {
                            CategoryIcon(
                                category = transaction.category,
                                size = 20.dp,
                                tint = null, // Original colors
                                iconResId = categoryEntity?.iconResId ?: 0,
                                iconName = categoryEntity?.iconName
                            )
                        },
                        subIcon = {
                            if(transaction.subcategory != null) {
                                CategoryIcon(
                                    category = transaction.subcategory,
                                    size = 20.dp,
                                    tint = null, // Original colors
                                    iconResId = subcategoryEntity?.iconResId ?: 0,
                                    iconName = subcategoryEntity?.iconName
                                )
                            }
                        },
                        subcategoryColor = run {
                             if (subcategoryEntity != null) {
                                  try {
                                      Color(subcategoryEntity.color.toColorInt()).copy(alpha = 0.2f)
                                  } catch (_: Exception) {
                                      null
                                  }
                             } else null
                        }
                    )

                    val fromAccount = transaction.fromAccount ?: transaction.accountNumber
                    val toAccount = transaction.toAccount
                    val isTransfer = transaction.transactionType == TransactionType.TRANSFER
                    
                    val fromBankName = if (isTransfer) {
                        availableAccounts.find { it.accountLast4 == fromAccount }?.bankName ?: transaction.bankName ?: fromAccount ?: stringResource(R.string.source)
                    } else {
                        transaction.bankName ?: stringResource(R.string.account)
                    }
                    
                    val toBankName = if (isTransfer && toAccount != null) {
                        availableAccounts.find { it.accountLast4 == toAccount }?.bankName ?: toAccount
                    } else null

                    val fromAccountEntity = availableAccounts.find { it.accountLast4 == fromAccount }
                    val toAccountEntity = toAccount?.let { acc -> availableAccounts.find { it.accountLast4 == acc } }

                    ReceiptInfoRow(
                        label = stringResource(R.string.account),
                        value = if (isTransfer) fromAccount ?: stringResource(R.string.source) else transaction.bankName ?: stringResource(R.string.account),
                        subValue = toAccount,
                        bankName = fromBankName,
                        subBankName = toBankName,
                        isTransfer = isTransfer,
                        icon = {
                            BrandIcon(
                                merchantName = fromBankName,
                                size = 26.dp,
                                showBackground = false,
                                accountIconResId = fromAccountEntity?.iconResId ?: 0,
                                accountIconName = fromAccountEntity?.iconName,
                                accountColorHex = fromAccountEntity?.color
                            )
                        },
                        subIcon = {
                            if (toBankName != null) {
                                BrandIcon(
                                    merchantName = toBankName,
                                    size = 26.dp,
                                    showBackground = false,
                                    accountIconResId = toAccountEntity?.iconResId ?: 0,
                                    accountIconName = toAccountEntity?.iconName,
                                    accountColorHex = toAccountEntity?.color
                                )
                            }
                        }
                    )

                    transaction.balanceAfter?.let {
                        ReceiptInfoRow(
                            label = stringResource(R.string.balance),
                            value = CurrencyFormatter.formatCurrency(it, primaryCurrency),
                            icon = {
                                Icon(
                                    imageVector = Iconax.Wallet3,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        )
                    }

                    if (transaction.isRecurring && linkedSubscription?.nextPaymentDate != null) {
                        ReceiptInfoRow(
                            label = stringResource(R.string.next_billing),
                            value = linkedSubscription.nextPaymentDate.format(
                                DateTimeFormatter.ofPattern("d MMM yyyy")
                            ),
                            icon = {
                                Icon(
                                    imageVector = Iconax.VideoTime,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        )
                    }
                }

                // Expandable Description
                if (!transaction.description.isNullOrBlank()) {
                    var isDescriptionExpanded by remember { mutableStateOf(false) }
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Dimensions.Padding.content)
                            .padding(top = Spacing.md)
                            .animateContentSize(
                                MaterialTheme.motionScheme.fastSpatialSpec()
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { isDescriptionExpanded = !isDescriptionExpanded }
                            ),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                            ){
                                Icon(
                                    imageVector = Iconax.DocumentText2,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                                Text(
                                    text = stringResource(R.string.description),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                            Icon(
                                imageVector = if (isDescriptionExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        
                        BlurredAnimatedVisibility(
                            visible = isDescriptionExpanded,
                            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                            exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = transaction.description,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        lineHeight = 16.sp
                                    ),
                                    modifier = Modifier.padding(Spacing.sm)
                                )
                            }
                        }
                    }
                }

                // Original SMS Content
                if (!transaction.smsBody.isNullOrBlank()) {
                    var isSMSExpanded by remember { mutableStateOf(true) }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(
                                MaterialTheme.motionScheme.fastSpatialSpec()
                            )
                            .padding(horizontal = Dimensions.Padding.content)
                            .padding(top = Spacing.md)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { isSMSExpanded = !isSMSExpanded }
                            ),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                            ){
                                Icon(
                                    imageVector = Iconax.Messages,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = stringResource(R.string.original_sms),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Icon(
                                imageVector = if (isSMSExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }

                        BlurredAnimatedVisibility(
                            visible = isSMSExpanded,
                            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                            exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = transaction.smsBody,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        lineHeight = 16.sp
                                    ),
                                    modifier = Modifier.padding(Spacing.sm)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp)) // Slightly reduced spacer

                // Dashed Line
                DashedLine(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                        .onGloballyPositioned { coordinates ->
                            cutoutOffsetPx = coordinates.positionInParent().y + (coordinates.size.height / 2f)
                        },
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                )

                Spacer(modifier = Modifier.height(Spacing.lg))

                // Amount
                Text(
                    text = stringResource(R.string.amount),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                
                val amountColor = when (transaction.transactionType) {
                    TransactionType.INCOME -> Color(0xFF4CAF50)
                    TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
                    TransactionType.CREDIT -> Color(0xFFFF6B35)  // Orange for credit
                    TransactionType.TRANSFER -> Color(0xFF9C27B0)  // Purple for transfer
                    TransactionType.INVESTMENT -> Color(0xFF00796B)  // Teal for investment
                }
                val sign = when (transaction.transactionType) {
                    TransactionType.INCOME -> "+"
                    TransactionType.EXPENSE -> "-"
                    TransactionType.CREDIT -> "💳"
                    TransactionType.TRANSFER -> "↔"
                    TransactionType.INVESTMENT -> "📈"
                }

                Text(
                    text = "$sign${transaction.formatAmount()}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )

                if (transaction.currency.isNotEmpty() && !transaction.currency.equals(primaryCurrency, ignoreCase = true) && convertedAmount != null) {
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = "≈ ${CurrencyFormatter.formatCurrency(convertedAmount, primaryCurrency)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Normal
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.lg))


                // Attachments
                val attachments = remember(transaction.attachments) {
                    attachmentService.parseAttachments(transaction.attachments)
                }
                val context = LocalContext.current

                if (attachments.isNotEmpty()) {
                    DashedLine(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.md),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        AttachmentSection(
                            attachments = attachments,
                            attachmentService = attachmentService,
                            onAddAttachment = {},
                            onRemoveAttachment = {},
                            onAttachmentClick = { path ->
                                val uri = attachmentService.getAttachmentUri(path)
                                if (uri != null) {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, attachmentService.getAttachmentMimeType(path))
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        // Handle error
                                    }
                                }
                            },
                            isEditable = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiptBadge(
    merchantName: String,
    categoryEntity: CategoryEntity? = null,
    subcategoryEntity: SubcategoryEntity? = null
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shadowElevation = 2.dp,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)),
        modifier = Modifier.wrapContentSize()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            BrandIcon(
                merchantName = merchantName,
                size = 26.dp,
                showBackground = true,
                categoryEntity = categoryEntity,
                subcategoryEntity = subcategoryEntity,
                accountIconName = null // Not an account icon in this context
            )
            Text(
                text = merchantName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun ReceiptInfoRow(
    label: String,
    value: String,
    isTransfer: Boolean = false,
    subValue: String? = null,
    bankName: String? = null,
    subBankName: String? = null,
    icon: (@Composable () -> Unit)? = null,
    subIcon: (@Composable () -> Unit)? = null,
    subcategoryColor: Color? = null
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        when (label) {
            "Type", "Balance", "Next Billing" -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    DashedLine(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    )
                    Box(
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(0.5f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Row {
                            if (icon != null) {
                                icon()
                            }
                            Spacer(modifier = Modifier.width(Spacing.xs))
                            Text(
                                text = value,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }

            "Category" -> {
                if (subValue != null) {
                    Text(
                        text = label.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        val categoryColor = CategoryMapping.categories[value]?.color?.copy(0.2f) ?: MaterialTheme.colorScheme.surfaceVariant
                        Box(
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .background(
                                    color = categoryColor,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                            ) {
                                if (icon != null) {
                                    icon()
                                }
                                Text(
                                    text = value,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        val resolvedSubcategoryColor = subcategoryColor ?: CategoryMapping.categories[value]?.color?.copy(0.2f) ?: MaterialTheme.colorScheme.surfaceVariant
                        if (subValue != null) {
                            DashedLine(
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                            )

                            Box(
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .background(
                                        color = resolvedSubcategoryColor,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                                ) {
                                    if (subIcon != null) {
                                        subIcon()
                                    }
                                    Text(
                                        text = subValue,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                } else{
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        DashedLine(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        )
                        val categoryColor = CategoryMapping.categories[value]?.color?.copy(0.2f) ?: MaterialTheme.colorScheme.surfaceVariant
                        Box(
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .background(
                                    color = categoryColor,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                            ) {
                                if (icon != null) {
                                    icon()
                                }
                                Text(
                                    text = value,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            "Account" -> {
                if (isTransfer) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement =Arrangement.spacedBy(Spacing.sm) ,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        val fromColorStr = bankName?.let { BrandIcons.getBrandColor(it) }
                        val fromColor = fromColorStr?.let { Color(it.toColorInt()).copy(0.2f) } ?: MaterialTheme.colorScheme.surfaceVariant
                        val toColorStr = subBankName?.let { BrandIcons.getBrandColor(it) }
                        val toColor = toColorStr?.let { Color(it.toColorInt()).copy(0.2f) } ?: MaterialTheme.colorScheme.surfaceVariant

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ){
                            Text(
                                text = stringResource(R.string.from),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.fillMaxWidth().weight(1f)
                            )
                            Text(
                                text = stringResource(R.string.to_destination),
                                style = MaterialTheme.typography.labelMedium,
                                textAlign = TextAlign.End,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.fillMaxWidth().weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .background(
                                        color = fromColor,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                                ) {
                                    if (icon != null) {
                                        icon()
                                    }
                                    Text(
                                        text = value,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(Spacing.xs))
                            DashedLine(
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                            )
                            Spacer(modifier = Modifier.width(Spacing.xs))
                            Box(
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .background(
                                        color = toColor,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                                ) {
                                    if (subIcon != null) {
                                        subIcon()
                                    }
                                    if (subValue != null) {
                                        Text(
                                            text = subValue,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else{
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        DashedLine(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        )
                        val bankColorStr = bankName?.let { BrandIcons.getBrandColor(it) }
                        val bankColor = bankColorStr?.let { Color(it.toColorInt()).copy(0.2f) } ?: MaterialTheme.colorScheme.surfaceVariant
                        
                        Box(
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .background(
                                    color = bankColor,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                            ) {
                                if (icon != null) {
                                    icon()
                                }
                                Text(
                                    text = value,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            else -> {
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (icon != null) {
                        icon()
                    }
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}


private class ReceiptShape(
    private val cutoutRadius: Float,
    private val cutoutTopOffset: Float,
    private val scallopRadius: Float
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            val scallopDiameter = scallopRadius * 2
            val scallopCount = (size.width / scallopDiameter).toInt().coerceAtLeast(1)
            val actualScallopWidth = size.width / scallopCount

            // Start from bottom-left (after the last scallop)
            moveTo(0f, size.height - scallopRadius)
            
            // Left edge with cutout
            lineTo(0f, cutoutTopOffset + cutoutRadius)
            arcTo(
                rect = Rect(-cutoutRadius, cutoutTopOffset - cutoutRadius, cutoutRadius, cutoutTopOffset + cutoutRadius),
                startAngleDegrees = 90f,
                sweepAngleDegrees = -180f,
                forceMoveTo = false
            )
            lineTo(0f, scallopRadius)
            
            // Top edge with scallops (left to right)
            for (i in 0 until scallopCount) {
                val x = i * actualScallopWidth
                arcTo(
                    rect = Rect(x, 0f, x + actualScallopWidth, actualScallopWidth),
                    startAngleDegrees = 180f,
                    sweepAngleDegrees = 180f,
                    forceMoveTo = false
                )
            }
            
            // Right edge with cutout
            lineTo(size.width, cutoutTopOffset - cutoutRadius)
            arcTo(
                rect = Rect(size.width - cutoutRadius, cutoutTopOffset - cutoutRadius, size.width + cutoutRadius, cutoutTopOffset + cutoutRadius),
                startAngleDegrees = 270f,
                sweepAngleDegrees = -180f,
                forceMoveTo = false
            )
            lineTo(size.width, size.height - scallopRadius)
            
            // Bottom edge with scallops (right to left)
            for (i in 0 until scallopCount) {
                val x = size.width - (i * actualScallopWidth)
                arcTo(
                    rect = Rect(x - actualScallopWidth, size.height - actualScallopWidth, x, size.height),
                    startAngleDegrees = 0f,
                    sweepAngleDegrees = 180f,
                    forceMoveTo = false
                )
            }
            
            close()
        }
        return Outline.Generic(path)
    }
}

// Match Preview Sheet
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MatchPreviewSheetContent(
    matchedTransactions: List<TransactionEntity>,
    selectedMatchIds: Set<Long>,
    searchQuery: String,
    searchResults: List<TransactionEntity>,
    onSearchQueryChange: (String) -> Unit,
    onAddSearchResult: (TransactionEntity) -> Unit,
    onToggleSelection: (Long) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
    newCategory: String
) {

    val isDark = isSystemInDarkTheme()
    Box( modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .animateContentSize()
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // Sheet header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimensions.Padding.content, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.matches_preview),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.matches_selected_format, selectedMatchIds.size, matchedTransactions.size, newCategory),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
                // Select all / deselect all
                val allSelected =
                    selectedMatchIds.size == matchedTransactions.size && matchedTransactions.isNotEmpty()

                IconButton(
                    onClick = {
                        if (allSelected) onDeselectAll() else onSelectAll()
                    },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        contentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    shapes = IconButtonDefaults.shapes(),
                ) {
                    Icon(
                        imageVector = if (allSelected) Icons.Rounded.Deselect else Icons.Rounded.SelectAll,
                        contentDescription = if (allSelected) stringResource(R.string.deselect_all) else stringResource(R.string.select_all),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            var searchTextFieldValue by remember(searchQuery) {
                mutableStateOf(
                    TextFieldValue(
                        text = searchQuery,
                        selection = TextRange(searchQuery.length)
                    )
                )
            }
            // Search Bar
            SearchBarBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimensions.Padding.content, vertical = Spacing.xs),
                searchQuery = searchTextFieldValue,
                onSearchQueryChange = {
                    searchTextFieldValue = it
                    onSearchQueryChange(it.text)
                },
                leadingIcon = {
                    Icon(
                        imageVector = Iconax.Search,
                        contentDescription = stringResource(R.string.search),
                        tint = MaterialTheme.colorScheme.onSurface.copy(0.5f)
                    )
                },
                trailingIcon = {
                    BlurredAnimatedVisibility(searchTextFieldValue.text.isNotEmpty()) {
                        IconButton(onClick = {
                            searchTextFieldValue = TextFieldValue("")
                            onSearchQueryChange("")
                        }) {
                            Icon(
                                imageVector = Iconax.CloseCircle,
                                contentDescription = stringResource(R.string.clear_search),
                                tint = MaterialTheme.colorScheme.onSurface.copy(0.5f)
                            )
                        }
                    }
                },
                label = {
                    Text(
                        text = stringResource(R.string.search_transactions_to_add),
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)
                    )
                }
            )

            // Show Search Results if available
            if (searchQuery.isNotEmpty()) {
                BlurredAnimatedVisibility(searchResults.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.no_additional_transactions_found),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                BlurredAnimatedVisibility(
                    visible = searchResults.isNotEmpty(),
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it }
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .padding(horizontal = Dimensions.Padding.content, vertical = Spacing.sm)
                            .clip(RoundedCornerShape(Dimensions.Padding.content))
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .heightIn(max = 460.dp),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        item {
                            Text(
                                text = stringResource(R.string.search_results),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = Spacing.md, bottom = Spacing.xs)
                            )
                        }
                        itemsIndexed(searchResults, key = { _, txn -> "search_${txn.id}" }) { index, txn ->
                            val position = ListItemPosition.from(index, searchResults.size)
                            ListItem(
                                headline = {
                                    Text(
                                        text = txn.merchantName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                supporting = {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = txn.dateTime.format(DateTimeFormatter.ofPattern("d MMM yyyy")),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        if (!txn.category.isNullOrBlank()) {
                                            Text(
                                                text = txn.category ?: "",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                },
                                leading = {
                                    BrandIcon(
                                        merchantName = txn.merchantName,
                                        category = txn.category,
                                        subcategory = txn.subcategory,
                                        size = 32.dp,
                                        showBackground = true
                                    )
                                },
                                trailing = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                                    ) {
                                        Text(
                                            text = CurrencyFormatter.formatCurrency(
                                                txn.amount,
                                                txn.currency
                                            ),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = when (txn.transactionType) {
                                                TransactionType.INCOME -> MaterialTheme.colorScheme.tertiary
                                                else -> MaterialTheme.colorScheme.error
                                            }
                                        )
                                        Icon(
                                            imageVector = Icons.Rounded.Add,
                                            contentDescription = stringResource(R.string.add_transaction_cd),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                },
                                onClick = { onAddSearchResult(txn) },
                                shape = position.toShape(),
                                padding = PaddingValues(0.dp)
                            )
                        }
                        item{
                            Spacer(modifier = Modifier.height(150.dp))
                        }
                    }
                }
            } else if (matchedTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_matching_transactions_found),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val listState = rememberLazyListState()
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .padding(horizontal = Dimensions.Padding.content, vertical = Spacing.sm)
                        .clip(RoundedCornerShape(Dimensions.Padding.content))
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(max = 460.dp),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    itemsIndexed(
                        items = matchedTransactions,
                        key = { _, txn -> txn.id }
                    ) { index, txn ->
                        val isSelected = selectedMatchIds.contains(txn.id)
                        val position = ListItemPosition.from(index, matchedTransactions.size)
                        ListItem(
                            headline = {
                                Text(
                                    text = txn.merchantName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            },
                            supporting = {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = txn.dateTime.format(
                                            DateTimeFormatter.ofPattern("d MMM yyyy")
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(0.5f)
                                    )
                                    if (!txn.category.isNullOrBlank()) {
                                        Text(
                                            text = txn.category ?: "",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                            },
                            leading = {
                                BrandIcon(
                                    merchantName = txn.merchantName,
                                    category = txn.category,
                                    subcategory = txn.subcategory,
                                    size = 32.dp,
                                    showBackground = true
                                )
                            },
                            trailing = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                                ) {
                                    Text(
                                        text = CurrencyFormatter.formatCurrency(
                                            txn.amount,
                                            txn.currency
                                        ),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = when (txn.transactionType) {
                                            TransactionType.INCOME -> if (!isDark) income_light else income_dark
                                            TransactionType.EXPENSE -> if (!isDark) expense_light else expense_dark
                                            TransactionType.CREDIT -> if (!isDark) credit_light else credit_dark
                                            TransactionType.TRANSFER -> if (!isDark) transfer_light else transfer_dark
                                            TransactionType.INVESTMENT -> if (!isDark) investment_light else investment_dark
                                        }
                                    )

                                    CashiroCheckbox(
                                        checked = isSelected,
                                        onCheckedChange = { onToggleSelection(txn.id) },
                                    )

                                }
                            },
                            selected = isSelected,
                            onClick = { onToggleSelection(txn.id) },
                            shape = position.toShape(),
                            padding = PaddingValues(0.dp),
                            selectedListColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    }
                    item{
                        Spacer(modifier = Modifier.height(150.dp))
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Save button
            Button(
                onClick = onApply,
                enabled = selectedMatchIds.isNotEmpty(),
                modifier = Modifier
                    .height(52.dp)
                    .fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shapes = ButtonDefaults.shapes()
            ) {
                Text(
                    text = if (selectedMatchIds.isEmpty()) stringResource(R.string.select_transactions_to_update)
                    else stringResource(R.string.apply_to_transactions_format, selectedMatchIds.size, if (selectedMatchIds.size == 1) stringResource(R.string.transaction) else stringResource(R.string.transactions_plural)),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}