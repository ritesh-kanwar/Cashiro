package com.ritesh.cashiro.presentation.ui.features.home

import android.app.Activity
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.ritesh.cashiro.R
import com.ritesh.cashiro.data.database.entity.CategoryEntity
import com.ritesh.cashiro.data.database.entity.SubcategoryEntity
import com.ritesh.cashiro.data.database.entity.SubscriptionEntity
import com.ritesh.cashiro.data.preferences.HomeWidget
import com.ritesh.cashiro.presentation.effects.overScrollVertical
import com.ritesh.cashiro.presentation.effects.rememberOverscrollFlingBehavior
import com.ritesh.cashiro.presentation.navigation.AccountDetail
import com.ritesh.cashiro.presentation.navigation.NotificationSettings
import com.ritesh.cashiro.presentation.navigation.UnrecognizedSms
import com.ritesh.cashiro.presentation.navigation.safeNavigate
import com.ritesh.cashiro.presentation.ui.components.AccountCarousel
import com.ritesh.cashiro.presentation.ui.components.BalanceCard
import com.ritesh.cashiro.presentation.ui.components.BudgetCarousel
import com.ritesh.cashiro.presentation.ui.components.CurrencySelectionBottomSheet
import com.ritesh.cashiro.presentation.ui.components.CustomTitleTopAppBar
import com.ritesh.cashiro.presentation.ui.components.GreetingCard
import com.ritesh.cashiro.presentation.ui.components.HeatmapWidget
import com.ritesh.cashiro.presentation.ui.components.ListItem
import com.ritesh.cashiro.presentation.ui.components.ListItemPosition
import com.ritesh.cashiro.presentation.ui.components.LoadingCircle
import com.ritesh.cashiro.presentation.ui.components.PreferenceSwitch
import com.ritesh.cashiro.presentation.ui.components.SectionHeader
import com.ritesh.cashiro.presentation.ui.components.SubscriptionIconsStack
import com.ritesh.cashiro.presentation.ui.components.TransactionItem
import com.ritesh.cashiro.presentation.ui.components.toShape
import com.ritesh.cashiro.presentation.ui.features.settings.appearance.ThemeViewModel
import com.ritesh.cashiro.presentation.ui.icons.AiCommentary
import com.ritesh.cashiro.presentation.ui.icons.Convertshape2
import com.ritesh.cashiro.presentation.ui.icons.Gallery
import com.ritesh.cashiro.presentation.ui.icons.Iconax
import com.ritesh.cashiro.presentation.ui.icons.ReceiptItem
import com.ritesh.cashiro.presentation.ui.icons.RefreshCircle
import com.ritesh.cashiro.presentation.ui.icons.Search
import com.ritesh.cashiro.presentation.ui.icons.Setting2
import com.ritesh.cashiro.presentation.ui.theme.Dimensions
import com.ritesh.cashiro.presentation.ui.theme.Spacing
import com.ritesh.cashiro.presentation.ui.theme.expense_dark
import com.ritesh.cashiro.presentation.ui.theme.expense_light
import com.ritesh.cashiro.presentation.ui.theme.income_dark
import com.ritesh.cashiro.presentation.ui.theme.income_light
import com.ritesh.cashiro.utils.CurrencyFormatter
import com.ritesh.cashiro.utils.bottomFade
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeDefaults.tint
import dev.chrisbanes.haze.HazeEffectScope
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class,
    ExperimentalHazeApi::class
)
@Composable
fun SharedTransitionScope.HomeScreen(
    homeViewModel: HomeViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel(),
    navController: NavController,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToChat: () -> Unit = {},
    onNavigateToTransactions: () -> Unit = {},
    onNavigateToTransactionsWithSearch: () -> Unit = {},
    onNavigateToSubscriptions: () -> Unit = {},
    onNavigateToBudgets: (Long?) -> Unit = {},
    onNavigateToBudgetHistory: (Long) -> Unit = {},
    onTransactionClick: (Long, String) -> Unit = { _, _ -> },
    onFullResyncClick: () -> Unit = {},
    animatedContentScope: AnimatedContentScope? = null,
) {

    val uiState by homeViewModel.uiState.collectAsState()
    val themeUiState by themeViewModel.themeUiState.collectAsState()
    val deletedTransaction by homeViewModel.deletedTransaction.collectAsState()
    val categoriesMap by homeViewModel.categoriesMap.collectAsStateWithLifecycle()
    val subcategoriesMap by homeViewModel.subcategoriesMap.collectAsStateWithLifecycle()
    val accountsMap by homeViewModel.accountsMap.collectAsStateWithLifecycle()
    val homeWidgets by homeViewModel.homeWidgets.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    val hazeState = remember { HazeState()}
    val hazeStateBanner = remember { HazeState()}
    val blurEffects = themeUiState.blurEffects

    val snackbarHostState = remember { SnackbarHostState() }
    
    var lastBackPressTime by remember { mutableLongStateOf(0L) }
    val context = LocalContext.current
    
    
    BackHandler {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBackPressTime < 2000) {
            (context as? Activity)?.finish()
        } else {
            lastBackPressTime = currentTime
            Toast.makeText(context, context.getString(R.string.press_back_again_to_close), Toast.LENGTH_SHORT).show()
        }
    }
    val scope = rememberCoroutineScope()

    var showMoreBottomSheet by remember { mutableStateOf(false) }
    var showEditWidgetsSheet by remember { mutableStateOf(false) }

    // Haptic feedback
    val view = LocalView.current


    // Check for app updates and reviews when the screen is first displayed
    LaunchedEffect(Unit) {
        // Refresh account balances to ensure proper currency conversion
        homeViewModel.refreshAccountBalances()

        // Check for app updates
        activity?.let {
            val componentActivity = it as ComponentActivity
            homeViewModel.checkForAppUpdate(
                activity = componentActivity,
                snackbarHostState = snackbarHostState,
                scope = scope
            )

            // Check for in-app review eligibility
            homeViewModel.checkForInAppReview(componentActivity)
        }
    }

    // ensures changes from ManageAccountsScreen are reflected immediately
    DisposableEffect(Unit) {
        homeViewModel.refreshHiddenAccounts()
        onDispose {}
    }

    // Handle delete undo snackbar
    LaunchedEffect(deletedTransaction) {
        deletedTransaction?.let { transaction ->
            // Clear the state immediately to prevent re-triggering
            homeViewModel.clearDeletedTransaction()

            scope.launch {
                val result =
                    snackbarHostState.showSnackbar(
                        message = context.getString(R.string.transaction_deleted),
                        actionLabel = context.getString(R.string.undo),
                        duration = SnackbarDuration.Short
                    )
                if (result == SnackbarResult.ActionPerformed) {
                    // Pass the transaction directly since state is already
                    // cleared
                    homeViewModel.undoDeleteTransaction(transaction)
                }
            }
        }
    }

    // Clear snackbar when navigating away
    DisposableEffect(Unit) { onDispose { snackbarHostState.currentSnackbarData?.dismiss() } }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val lazyListState = rememberLazyListState()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = Color.Transparent,
        topBar = {
            CustomTitleTopAppBar(
                title = stringResource(R.string.cashiro_title),
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehavior,
                hazeState = hazeState,
                hasBackButton = false,
                extraInfoCard = {
                    GreetingCard(
                        userName = uiState.userName,
                        profileImageUri = uiState.profileImageUri,
                        profileBackgroundColor = uiState.profileBackgroundColor,
                        unreadUpdatesCount = uiState.unreadUpdatesCount,
                        onProfileClick = onNavigateToSettings,
                        onNotificationClick = { navController.safeNavigate(NotificationSettings) },
                        onMoreClick = { showMoreBottomSheet = true },
                        onUpdatesClick = { navController.safeNavigate(UnrecognizedSms) }
                    )
                },
                navigationContent = {
                    Box(
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(uiState.profileBackgroundColor)
                            .clickable { onNavigateToSettings() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (uiState.profileImageUri != null) {
                            AsyncImage(
                                model = uiState.profileImageUri,
                                contentDescription = stringResource(R.string.profile),
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.avatar_1),
                                contentDescription = stringResource(R.string.profile),
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                },
                actionContent = {
                    val containerColor = MaterialTheme.colorScheme.surfaceContainer
                    Box(
                        modifier =
                            Modifier.padding(end = 16.dp)
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    color = if (blurEffects) containerColor.copy(0.5f)
                                    else containerColor,
                                    shape = CircleShape
                                )
                                .then(
                                    if (blurEffects) Modifier.hazeEffect(
                                        state = hazeState,
                                        block = fun HazeEffectScope.() {
                                            style = HazeDefaults.style(
                                                backgroundColor = Color.Transparent,
                                                tint = tint(containerColor),
                                                blurRadius = 20.dp,
                                                noiseFactor = -1f,
                                            )
                                            blurredEdgeTreatment = BlurredEdgeTreatment.Unbounded
                                        }
                                    ) else Modifier
                                )
                                .clickable(
                                    onClick = { showMoreBottomSheet = true },
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MoreHoriz,
                            contentDescription = stringResource(R.string.more_options),
                            tint = MaterialTheme.colorScheme.inverseSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = {
                    Snackbar(
                        snackbarData = it,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.large,
                    )
                }
            )
        }

    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                // Banner Image Background
                AnimatedVisibility(
                    visible = uiState.showBannerImage,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .align(Alignment.TopCenter)
                    ) {
                        if (uiState.bannerImageUri != null) {
                            AsyncImage(
                                model = uiState.bannerImageUri,
                                contentDescription = stringResource(R.string.banner),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .hazeSource(hazeStateBanner)
                                    .alpha(0.5f)
                                    .bottomFade(0.4f)
                                    .hazeSource(hazeStateBanner),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.banner_bg_image),
                                contentDescription = stringResource(R.string.banner),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .alpha(0.5f)
                                    .bottomFade(0.4f)
                                    .hazeSource(hazeStateBanner),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }

            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(state = hazeState)
                    .overScrollVertical(),
                flingBehavior = rememberOverscrollFlingBehavior { lazyListState },
                contentPadding =
                    PaddingValues(
                        top = Dimensions.Padding.content + paddingValues.calculateTopPadding(),
                        bottom = 0.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                homeWidgets.forEach { widgetModel ->
                    if (widgetModel.isVisible) {
                        when (widgetModel.widget) {
                            HomeWidget.NETWORTH_SUMMARY -> {
                                item(key = "net_worth") {
                                    NetworthSummaryCards(
                                        uiState = uiState,
                                        onCurrencySelected = {
                                            homeViewModel.selectCurrency(it)
                                        },
                                        blurEffects = blurEffects && uiState.showBannerImage,
                                        hazeState = hazeStateBanner
                                    )
                                }
                            }
                            HomeWidget.TRANSACTION_HEATMAP -> {
                                item(key = "transaction_heatmap") {
                                    HeatmapWidget(
                                        data = uiState.transactionHeatmap,
                                        blurEffects = blurEffects && uiState.showBannerImage,
                                        hazeState = hazeStateBanner
                                    )
                                }
                            }
                            HomeWidget.BUDGET_CAROUSEL -> {
                                if (uiState.activeBudgets.isNotEmpty()) {
                                    item(key = "budget_carousel") {
                                        var lastBudgetClickTime by remember { mutableLongStateOf(0L) }
                                        BudgetCarousel(
                                            budgets = uiState.activeBudgets,
                                            onBudgetClick = {
                                                val currentTime = System.currentTimeMillis()
                                                if (currentTime - lastBudgetClickTime > 500) {
                                                    lastBudgetClickTime = currentTime
                                                    onNavigateToBudgets(it)
                                                }
                                            },
                                            onEditClick = {
                                                val currentTime = System.currentTimeMillis()
                                                if (currentTime - lastBudgetClickTime > 500) {
                                                    lastBudgetClickTime = currentTime
                                                    onNavigateToBudgets(it)
                                                }
                                            },
                                            onHistoryClick = onNavigateToBudgetHistory,
                                            animatedVisibilityScope = animatedContentScope,
                                        )
                                    }
                                }
                            }
                            HomeWidget.ACCOUNT_CAROUSEL -> {
                                if (uiState.creditCards.isNotEmpty() ||
                                    uiState.accountBalances.isNotEmpty()
                                ) {
                                    item(key = "account_carousel") {
                                        AccountCarousel(
                                            creditCards = uiState.creditCards,
                                            bankAccounts = uiState.accountBalances,
                                            onAccountClick = { bankName, accountLast4 ->
                                                navController.safeNavigate(
                                                    AccountDetail(
                                                        bankName = bankName,
                                                        accountLast4 = accountLast4
                                                    )
                                                )
                                            },
                                            animatedContentScope = animatedContentScope,
                                            blurEffects = blurEffects && uiState.showBannerImage,
                                            hazeState = hazeStateBanner
                                        )
                                    }
                                }
                            }
                            HomeWidget.UPCOMING_SUBSCRIPTIONS -> {
                                if (uiState.upcomingSubscriptions.isNotEmpty()) {
                                    item(key = "upcoming_subscriptions") {
                                        val cardModifier = Modifier.padding(
                                            start = Dimensions.Padding.content,
                                            end = Dimensions.Padding.content,
                                        )

                                        if (animatedContentScope != null) {
                                            UpcomingSubscriptionsCard(
                                                subscriptions = uiState.upcomingSubscriptions,
                                                totalAmount = uiState.upcomingSubscriptionsTotal,
                                                currency = uiState.upcomingSubscriptionsCurrency,
                                                categoriesMap = categoriesMap,
                                                subcategoriesMap = subcategoriesMap,
                                                onClick = onNavigateToSubscriptions,
                                                blurEffects = blurEffects && uiState.showBannerImage,
                                                hazeState = hazeStateBanner,
                                                modifier = cardModifier.sharedBounds(
                                                    rememberSharedContentState(key = "upcoming_subscriptions_card"),
                                                    animatedVisibilityScope = animatedContentScope,
                                                    boundsTransform = { _, _ ->
                                                        spring(
                                                            stiffness = Spring.StiffnessLow,
                                                            dampingRatio = Spring.DampingRatioNoBouncy
                                                        )
                                                    },
                                                    resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(
                                                        contentScale = ContentScale.Fit,
                                                        alignment = Alignment.Center
                                                    ),
                                                    renderInOverlayDuringTransition = false
                                                )
                                                    .skipToLookaheadSize()
                                            )
                                        } else {
                                            UpcomingSubscriptionsCard(
                                                subscriptions = uiState.upcomingSubscriptions,
                                                totalAmount = uiState.upcomingSubscriptionsTotal,
                                                currency = uiState.upcomingSubscriptionsCurrency,
                                                categoriesMap = categoriesMap,
                                                subcategoriesMap = subcategoriesMap,
                                                onClick = onNavigateToSubscriptions,
                                                blurEffects = blurEffects && uiState.showBannerImage,
                                                hazeState = hazeStateBanner,
                                                modifier = cardModifier
                                            )
                                        }
                                    }
                                }
                            }
                            HomeWidget.RECENT_TRANSACTIONS -> {
                                item(key = "recent_transactions") {
                                    val containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                    Column {
                                        Surface(
                                            modifier = Modifier
                                                .padding(horizontal = Spacing.md)
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(Dimensions.Radius.lg))
                                                .then(
                                                    if (blurEffects && uiState.showBannerImage) Modifier.hazeEffect(
                                                        state = hazeStateBanner,
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
                                            shape = RoundedCornerShape(Dimensions.Radius.lg),
                                            color = if (blurEffects && uiState.showBannerImage) MaterialTheme.colorScheme.surface.copy(0.5f)
                                            else MaterialTheme.colorScheme.surface,
                                            contentColor = Color.Transparent,
                                        ) {
                                            Column {
                                                SectionHeader(
                                                    title = stringResource(R.string.recent),
                                                    action = {
                                                        Row(
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            // Search button
                                                            TextButton(
                                                                onClick = onNavigateToTransactionsWithSearch,
                                                                modifier = Modifier.then(
                                                                    if (animatedContentScope != null) {
                                                                        Modifier.sharedBounds(
                                                                            rememberSharedContentState(key = "transactions_search"),
                                                                            animatedVisibilityScope = animatedContentScope,
                                                                            boundsTransform = { _, _ ->
                                                                                spring(
                                                                                    stiffness = Spring.StiffnessLow,
                                                                                    dampingRatio = Spring.DampingRatioNoBouncy
                                                                                )
                                                                            },
                                                                            resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(
                                                                                contentScale = ContentScale.None,
                                                                                alignment = Alignment.Center
                                                                            ),
                                                                            renderInOverlayDuringTransition = false
                                                                        )
                                                                            .skipToLookaheadSize()
                                                                    } else Modifier
                                                                )
                                                            ) {
                                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                                    Icon(
                                                                        imageVector = Iconax.Search,
                                                                        contentDescription = stringResource(R.string.search_transactions),
                                                                        modifier = Modifier.size(Dimensions.Icon.small),
                                                                        tint = MaterialTheme.colorScheme.primary
                                                                    )
                                                                    Spacer(modifier = Modifier.width(4.dp))
                                                                    Text(stringResource(R.string.search))
                                                                }

                                                            }
                                                        }
                                                    },
                                                    modifier = Modifier.padding(
                                                        start = 16.dp,
                                                        end = 8.dp,
                                                    )
                                                )

                                                if (uiState.isLoading) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(Dimensions.Component.minTouchTarget * 2),
                                                        contentAlignment = Alignment.Center
                                                    ) { LoadingCircle() }
                                                } else if (uiState.recentTransactions.isEmpty()) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(Dimensions.Padding.card),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Column(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalAlignment = Alignment.CenterHorizontally,
                                                            ) {
                                                            Icon(
                                                                imageVector = Iconax.ReceiptItem,
                                                                contentDescription = null,
                                                                modifier = Modifier.size(48.dp),
                                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                            Spacer(modifier = Modifier.height(Spacing.md))
                                                            Text(
                                                                text = stringResource(R.string.no_transactions_yet),
                                                                style = MaterialTheme.typography.bodyLarge,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                } else {
                                                    uiState.recentTransactions.forEachIndexed { index, transaction ->
                                                        val categoryEntity = categoriesMap[transaction.category]
                                                        val subcategoryEntity =
                                                            if (categoryEntity != null && transaction.subcategory != null) {
                                                                subcategoriesMap[transaction.subcategory]
                                                            } else null
                                                        val position = ListItemPosition.from(
                                                            index,
                                                            uiState.recentTransactions.size
                                                        )

                                                        val accountEntity = accountsMap["${transaction.bankName}_${transaction.accountNumber}"]
                                                        TransactionItem(
                                                            transaction = transaction,
                                                            categoryEntity = categoryEntity,
                                                            subcategoryEntity = subcategoryEntity,
                                                            accountIconResId = accountEntity?.iconResId ?: 0,
                                                            accountIconName = accountEntity?.iconName,
                                                            accountColorHex = accountEntity?.color,
                                                            convertedAmount = uiState.convertedAmounts[transaction.id],
                                                            mainCurrency = uiState.baseCurrency,
                                                            onClick = {
                                                                onTransactionClick(
                                                                    transaction.id,
                                                                    "transaction_${transaction.id}"
                                                                )
                                                            },
                                                            shape = position.toShape(),
                                                            modifier = Modifier.fillMaxWidth(),
                                                            animatedContentScope = animatedContentScope,
                                                            sharedElementKey = "transaction_${transaction.id}"
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(Spacing.sm))
                                        Box(
                                            modifier = Modifier.fillMaxWidth(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            TextButton(
                                                onClick = onNavigateToTransactions,
                                                modifier = Modifier
                                                    .then(
                                                        if (animatedContentScope != null) {
                                                            Modifier.sharedBounds(
                                                                rememberSharedContentState(key = "transactions_screen"),
                                                                animatedVisibilityScope = animatedContentScope,
                                                                boundsTransform = { _, _ ->
                                                                    spring(
                                                                        stiffness = Spring.StiffnessLow,
                                                                        dampingRatio = Spring.DampingRatioNoBouncy
                                                                    )
                                                                },
                                                                resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(
                                                                    contentScale = ContentScale.None,
                                                                    alignment = Alignment.Center
                                                                ),
                                                                renderInOverlayDuringTransition = false
                                                            )
                                                                .skipToLookaheadSize()
                                                        } else Modifier
                                                    )
                                                    .clip(RoundedCornerShape(Dimensions.Radius.lg))
                                                    .then(
                                                        if (blurEffects && uiState.showBannerImage) Modifier.hazeEffect(
                                                            state = hazeStateBanner,
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
                                                    )
                                                    .height(26.dp),
                                                colors = ButtonDefaults.textButtonColors(
                                                    contentColor = MaterialTheme.colorScheme.primary,
                                                    containerColor = if (blurEffects && uiState.showBannerImage)
                                                        MaterialTheme.colorScheme.surfaceContainerLow.copy(0.7f)
                                                    else MaterialTheme.colorScheme.surfaceContainerLow
                                                ),
                                                contentPadding = PaddingValues(0.dp)
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.view_all),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                                                    modifier = Modifier.padding(horizontal = Spacing.md)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item{
                    Spacer(Modifier.height(200.dp)) //Extra space for better scroll
                }
            }

            // More Options BottomSheet
            if (showMoreBottomSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showMoreBottomSheet = false },
                    sheetState = rememberModalBottomSheetState(),
                    containerColor = MaterialTheme.colorScheme.surface,
                    dragHandle = { BottomSheetDefaults.DragHandle() }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 32.dp)
                            .padding(horizontal = 16.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.more_options),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = Spacing.sm).fillMaxWidth()
                        )



                        // Edit Widgets Option
                        ListItem(
                            headline = { Text(stringResource(R.string.edit_widgets)) },
                            leading = {
                                Icon(
                                    imageVector = Iconax.Convertshape2,
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                showMoreBottomSheet = false
                                showEditWidgetsSheet = true
                            },
                            shape = ListItemPosition.Top.toShape()
                        )

                        // Settings Option
                        ListItem(
                            headline = { Text(stringResource(R.string.settings)) },
                            leading = {
                                Icon(
                                    imageVector = Iconax.Setting2,
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                showMoreBottomSheet = false
                                onNavigateToSettings()
                            },
                            shape = ListItemPosition.Middle.toShape()
                        )

                        // Ask AI Option
                        ListItem(
                            headline = { Text(stringResource(R.string.ask_ai)) },
                            leading = {
                                Icon(
                                    imageVector = Iconax.AiCommentary,
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                showMoreBottomSheet = false
                                onNavigateToChat()
                            },
                            shape = ListItemPosition.Middle.toShape()
                        )

                        // Sync SMS Option
                        ListItem(
                            headline = { Text(stringResource(R.string.sync_sms)) },
                            leading = {
                                Icon(
                                    imageVector = Iconax.RefreshCircle,
                                    contentDescription = null,
                                )
                            },
                            supporting = { Text(stringResource(R.string.long_press_for_full_resync)) },
                            onClick = {
                                showMoreBottomSheet = false
                                homeViewModel.scanSmsMessages()
                            },
                            onLongClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                showMoreBottomSheet = false
                                onFullResyncClick()
                            },
                            shape = ListItemPosition.Middle.toShape()
                        )

                        // Banner Image Toggle
                        PreferenceSwitch(
                            title = stringResource(R.string.show_banner_image),
                            leadingIcon = {
                                Icon(
                                    imageVector = Iconax.Gallery,
                                    contentDescription = null,
                                )
                            },
                            checked = uiState.showBannerImage,
                            onCheckedChange = { homeViewModel.toggleBannerImage() },
                            isLast = true
                        )
                    }
                }
            }

            // Breakdown Dialog
            if (uiState.showBreakdownDialog) {
                BreakdownDialog(
                    currentMonthIncome = uiState.currentMonthIncome,
                    currentMonthExpenses = uiState.currentMonthExpenses,
                    currentMonthTotal = uiState.currentMonthTotal,
                    lastMonthIncome = uiState.lastMonthIncome,
                    lastMonthExpenses = uiState.lastMonthExpenses,
                    lastMonthTotal = uiState.lastMonthTotal,
                    onDismiss = { homeViewModel.hideBreakdownDialog() }
                )
            }

            // Edit Widgets Sheet
            if (showEditWidgetsSheet) {
                EditWidgetsSheet(
                    onDismissRequest = { showEditWidgetsSheet = false },
                    sheetState = rememberModalBottomSheetState(),
                    widgets = homeWidgets,
                    onToggleVisibility = homeViewModel::toggleHomeWidgetVisibility,
                    onReorder = homeViewModel::updateWidgetsOrder
                )
            }
        }
    }

}




@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BreakdownDialog(
    currentMonthIncome: BigDecimal,
    currentMonthExpenses: BigDecimal,
    currentMonthTotal: BigDecimal,
    lastMonthIncome: BigDecimal,
    lastMonthExpenses: BigDecimal,
    lastMonthTotal: BigDecimal,
    onDismiss: () -> Unit
) {
    val now = LocalDate.now()
    val currentPeriod = "${now.month.name.lowercase().let { if (it.isNotEmpty()) it.take(1).uppercase() + it.drop(1) else it }} 1-${now.dayOfMonth}"
    val lastMonth = now.minusMonths(1)
    val lastPeriod = "${lastMonth.month.name.lowercase().let { if (it.isNotEmpty()) it.take(1).uppercase() + it.drop(1) else it }} 1-${now.dayOfMonth}"

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md), // Reduced horizontal padding for wider modal
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(Dimensions.Padding.card),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                // Title
                Text(
                    text = stringResource(R.string.calculation_breakdown),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                // Current Period Section
                Text(
                    text = currentPeriod,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                BreakdownRow(
                    label = stringResource(R.string.income),
                    amount = currentMonthIncome,
                    isIncome = true
                )

                BreakdownRow(
                    label = stringResource(R.string.expenses),
                    amount = currentMonthExpenses,
                    isIncome = false
                )

                HorizontalDivider()

                BreakdownRow(
                    label = stringResource(R.string.net_balance),
                    amount = currentMonthTotal,
                    isIncome = currentMonthTotal >= BigDecimal.ZERO,
                    isBold = true
                )

                Spacer(modifier = Modifier.height(Spacing.sm))

                // Last Period Section
                Text(
                    text = lastPeriod,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                BreakdownRow(
                    label = stringResource(R.string.income),
                    amount = lastMonthIncome,
                    isIncome = true
                )

                BreakdownRow(
                    label = stringResource(R.string.expenses),
                    amount = lastMonthExpenses,
                    isIncome = false
                )

                HorizontalDivider()

                BreakdownRow(
                    label = stringResource(R.string.net_balance),
                    amount = lastMonthTotal,
                    isIncome = lastMonthTotal >= BigDecimal.ZERO,
                    isBold = true
                )

                // Formula explanation
                Spacer(modifier = Modifier.height(Spacing.sm))
                Card(
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.breakdown_formula_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(Spacing.sm),
                        textAlign = TextAlign.Center
                    )
                }

                // Close button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) { Text(stringResource(R.string.close)) }
            }
        }
    }
}

@Composable
private fun BreakdownRow(
    label: String,
    amount: BigDecimal,
    isIncome: Boolean,
    isBold: Boolean = false
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = "${if (isIncome) "+" else "-"}${CurrencyFormatter.formatCurrency(amount.abs())}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color =
                if (isIncome) {
                    if (!isSystemInDarkTheme()) income_light else income_dark
                } else {
                    if (!isSystemInDarkTheme()) expense_light else expense_dark
                }
        )
    }
}

@OptIn(ExperimentalHazeApi::class)
@Composable
private fun UpcomingSubscriptionsCard(
    modifier: Modifier = Modifier,
    subscriptions: List<SubscriptionEntity>,
    totalAmount: BigDecimal,
    currency: String,
    categoriesMap: Map<String, CategoryEntity> = emptyMap(),
    subcategoriesMap: Map<String, SubcategoryEntity> = emptyMap(),
    onClick: () -> Unit = {},
    blurEffects: Boolean,
    hazeState: HazeState = remember { HazeState() }
) {
    val containerColor = MaterialTheme.colorScheme.surfaceContainerLow

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimensions.Radius.xxl))
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
        shape = RoundedCornerShape(Dimensions.Radius.xxl),
        colors = CardDefaults.cardColors(
            containerColor = if (blurEffects) MaterialTheme.colorScheme.surfaceContainerLow.copy(0.5f)
            else MaterialTheme.colorScheme.surfaceContainerLow
        ),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Dimensions.Padding.content),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.padding(start = 12.dp)
            ) {
                Text(
                    text = stringResource(R.string.subscriptions_count_format, subscriptions.size),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(
                        alpha = Dimensions.Alpha.subtitle
                    )
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        text =
                            CurrencyFormatter.formatCurrency(
                                totalAmount,
                                currency
                            ).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text =
                            stringResource(R.string.per_month).uppercase(),
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(
                            alpha = Dimensions.Alpha.subtitle
                        ),
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

            }

            SubscriptionIconsStack(
                subscriptions = subscriptions,
                iconSize = 38.dp,
                modifier = Modifier.padding(end = Spacing.sm),
                borderColor = MaterialTheme.colorScheme.surfaceContainerLow,
                categoriesMap = categoriesMap,
                subcategoriesMap = subcategoriesMap
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NetworthSummaryCards(
    uiState: HomeUiState,
    onCurrencySelected: (String) -> Unit = {},
    blurEffects: Boolean,
    hazeState: HazeState = remember { HazeState() },
) {
    var showCurrencySheet by remember { mutableStateOf(false) }

    if (showCurrencySheet) {
        CurrencySelectionBottomSheet(
            selectedCurrency = uiState.selectedCurrency,
            availableCurrencies = uiState.availableCurrencies,
            onCurrencySelected = {
                onCurrencySelected(it)
                showCurrencySheet = false
            },
            onDismiss = { showCurrencySheet = false }
        )
    }

    val context = LocalContext.current
    val abbreviatedName = remember(uiState.userName) {
        if (uiState.userName.contains(" ")) {
            uiState.userName.split(" ")
                .filter { it.isNotBlank() }
                .take(2)
                .map { it[0] }
                .joinToString("")
                .uppercase()
        } else if (uiState.userName.length > 4) {
            uiState.userName.filter { it !in "aeiouAEIOU" }.take(4).uppercase().ifEmpty { 
                uiState.userName.take(4).uppercase() 
            }
        } else {
            uiState.userName.uppercase()
        }
    }

    val dateRangeLabel = remember(uiState.balanceHistory) {
        if (uiState.balanceHistory.size < 2) ""
        else {
            val start = uiState.balanceHistory.first().timestamp.toLocalDate()
            val end = uiState.balanceHistory.last().timestamp.toLocalDate()
            val days = ChronoUnit.DAYS.between(start, end)
            context.getString(R.string.last_days_format, days)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {

        BalanceCard(
            totalBalance = uiState.totalBalance,
            monthlyChange = uiState.monthlyChange,
            monthlyChangePercent = uiState.monthlyChangePercent,
            currency = uiState.selectedCurrency,
            abbreviatedName = abbreviatedName,
            userName = uiState.userName,
            balanceHistory = uiState.balanceHistory,
            dateRangeLabel = dateRangeLabel,
            thisMonthValue = CurrencyFormatter.formatCurrency(uiState.currentMonthTotal, uiState.selectedCurrency),
            thisYearValue = CurrencyFormatter.formatCurrency(uiState.currentYearTotal, uiState.selectedCurrency),
            availableCurrenciesCount = uiState.availableCurrencies.size,
            onCurrencyClick = { showCurrencySheet = true },
            blurEffects = blurEffects,
            hazeState = hazeState,
            modifier = Modifier.padding(
                start = Dimensions.Padding.content,
                end = Dimensions.Padding.content,
            )
        )
    }
}





