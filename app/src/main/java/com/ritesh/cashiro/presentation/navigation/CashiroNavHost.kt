package com.ritesh.cashiro.presentation.navigation

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import com.ritesh.cashiro.data.preferences.NavigationBarStyle
import com.ritesh.cashiro.presentation.ui.components.SmsParsingProgressDialog
import com.ritesh.cashiro.presentation.ui.features.accounts.AccountDetailScreen
import com.ritesh.cashiro.presentation.ui.features.accounts.AddAccountScreen
import com.ritesh.cashiro.presentation.ui.features.accounts.ManageAccountsScreen
import com.ritesh.cashiro.presentation.ui.features.add.AddScreen
import com.ritesh.cashiro.presentation.ui.features.analytics.AnalyticsScreen
import com.ritesh.cashiro.presentation.ui.features.budgets.BudgetDetailScreen
import com.ritesh.cashiro.presentation.ui.features.budgets.BudgetHistoryScreen
import com.ritesh.cashiro.presentation.ui.features.budgets.BudgetsScreen
import com.ritesh.cashiro.presentation.ui.features.categories.CategoriesScreen
import com.ritesh.cashiro.presentation.ui.features.chat.ChatScreen
import com.ritesh.cashiro.presentation.ui.features.home.HomeScreen
import com.ritesh.cashiro.presentation.ui.features.home.HomeViewModel
import com.ritesh.cashiro.presentation.ui.features.onboarding.OnBoardingScreen
import com.ritesh.cashiro.presentation.ui.features.profile.ProfileScreen
import com.ritesh.cashiro.presentation.ui.features.settings.SettingsScreen
import com.ritesh.cashiro.presentation.ui.features.settings.about.AboutScreen
import com.ritesh.cashiro.presentation.ui.features.settings.about.LicensesScreen
import com.ritesh.cashiro.presentation.ui.features.settings.appearance.AppearanceScreen
import com.ritesh.cashiro.presentation.ui.features.settings.appearance.ThemeViewModel
import com.ritesh.cashiro.presentation.ui.features.settings.applock.AppLockScreen
import com.ritesh.cashiro.presentation.ui.features.settings.dataprivacy.DataPrivacyScreen
import com.ritesh.cashiro.presentation.ui.features.settings.developer.DeveloperScreen
import com.ritesh.cashiro.presentation.ui.features.settings.notifications.NotificationScreen
import com.ritesh.cashiro.presentation.ui.features.settings.rules.CreateRuleScreen
import com.ritesh.cashiro.presentation.ui.features.settings.rules.RulesScreen
import com.ritesh.cashiro.presentation.ui.features.settings.rules.RulesViewModel
import com.ritesh.cashiro.presentation.ui.features.settings.sms.SMSScreen
import com.ritesh.cashiro.presentation.ui.features.settings.unrecognized.UnrecognizedSmsScreen
import com.ritesh.cashiro.presentation.ui.features.settings.webhooks.WebhookEditorScreen
import com.ritesh.cashiro.presentation.ui.features.settings.webhooks.WebhooksScreen
import com.ritesh.cashiro.presentation.ui.features.subscriptions.SubscriptionsScreen
import com.ritesh.cashiro.presentation.ui.features.transactions.ExportTransactionsDialog
import com.ritesh.cashiro.presentation.ui.features.transactions.TransactionDetailScreen
import com.ritesh.cashiro.presentation.ui.features.transactions.TransactionsScreen
import com.ritesh.cashiro.presentation.ui.features.transactions.TransactionsViewModel
import com.ritesh.cashiro.presentation.ui.icons.Iconax
import com.ritesh.cashiro.presentation.ui.icons.ImportArrow01
import com.ritesh.cashiro.presentation.ui.theme.Dimensions
import com.ritesh.cashiro.presentation.ui.theme.Spacing
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeEffectScope
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalHazeApi::class)
@Composable
fun CashiroNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: Any = Home,
    onEditComplete: () -> Unit = {}
) {
    // Use a stable start destination
    val stableStartDestination = remember { startDestination }
    
    // Get theme settings for bottom nav style
    val themeViewModel: ThemeViewModel = hiltViewModel()
    val themeUiState by themeViewModel.themeUiState.collectAsState()
    
    // Track current destination for bottom nav visibility
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route
    
    // Check if current route is in bottom nav routes
    val showBottomNav = BOTTOM_NAV_ROUTES.any { qualifiedName ->
        currentRoute?.contains(qualifiedName ?: "") == true
    }

    val homeViewModel: HomeViewModel = hiltViewModel()
    val transactionsViewModel: TransactionsViewModel = hiltViewModel()
    val homeUiState by homeViewModel.uiState.collectAsState()
    val transactionsUiState by transactionsViewModel.uiState.collectAsState()
    val smsScanWorkInfo by homeViewModel.smsScanWorkInfo.collectAsState()
    val view = LocalView.current

    // State for full resync confirmation dialog
    var showFullResyncDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

    val isHomeScreen = currentRoute?.contains(Home::class.qualifiedName ?: "") == true
    val isTransactionsScreen = currentRoute?.contains(Transactions::class.qualifiedName ?: "") == true
    val isAddTransactionScreen = currentRoute?.contains(AddTransaction::class.qualifiedName ?: "") == true
    val isSubscriptionsScreen = currentRoute?.contains(Subscriptions::class.qualifiedName ?: "") == true
    val isBudgetDetailScreen = currentRoute?.contains(BudgetDetail::class.qualifiedName ?: "") == true


    val hazeState = remember { HazeState() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        SharedTransitionLayout {
            NavHost(
                navController = navController,
                startDestination = stableStartDestination,
                modifier = Modifier.fillMaxSize().hazeSource(hazeState),
            ) {
                // App Lock Screen
                composable<AppLock>(
                    enterTransition = CashiroTransitions.noneEnter,
                    exitTransition = CashiroTransitions.noneExit,
                    popEnterTransition = CashiroTransitions.noneEnter,
                    popExitTransition = CashiroTransitions.noneExit
                ) {
                    AppLockScreen(
                        onUnlocked = {
                            navController.safeNavigate(Home) {
                                popUpTo(AppLock) { inclusive = true }
                            }
                        }
                    )
                }

                // Onboarding Screen
                composable<OnBoarding>(
                    enterTransition = CashiroTransitions.noneEnter,
                    exitTransition = CashiroTransitions.noneExit,
                    popEnterTransition = CashiroTransitions.noneEnter,
                    popExitTransition = CashiroTransitions.noneExit
                ) {
                    OnBoardingScreen(
                        onOnBoardingComplete = {
                            navController.safeNavigate(Home) {
                                popUpTo(OnBoarding) { inclusive = true }
                            }
                        }
                    )
                }

                /* BOTTOM NAV SCREENS ---- */
                // Home Screen
                composable<Home>(
                    enterTransition = CashiroTransitions.verticalSlideEnter,
                    exitTransition = CashiroTransitions.verticalSlideExit,
                    popEnterTransition = CashiroTransitions.verticalSlidePopEnter,
                    popExitTransition = CashiroTransitions.verticalSlidePopExit
                ) {
                    HomeScreen(
                        navController = navController,
                        onNavigateToSettings = { navController.safeNavigate(Settings) },
                        onNavigateToChat = { navController.safeNavigate(Chat) },
                        onNavigateToTransactions = { navController.safeNavigate(Transactions()) },
                        onNavigateToTransactionsWithSearch = {
                            navController.safeNavigate(Transactions(focusSearch = true))
                        },
                        onNavigateToSubscriptions = { navController.safeNavigate(Subscriptions) },
                        onNavigateToBudgets = { id ->
                            if (id != null) {
                                navController.safeNavigate(BudgetDetail(budgetId = id, sharedElementKey = "budget_card_$id"))
                            } else {
                                navController.safeNavigate(Budgets())
                            }
                        },
                        onNavigateToBudgetHistory = { id ->
                            navController.safeNavigate(BudgetHistory(id))
                        },
                        onTransactionClick = { transactionId, key ->
                            navController.safeNavigate(TransactionDetail(transactionId, key))
                        },
                        onFullResyncClick = { showFullResyncDialog = true },
                        animatedContentScope = this@composable,
                    )
                }

                // Analytics Screen
                composable<Analytics>(
                    enterTransition = CashiroTransitions.verticalSlideEnter,
                    exitTransition = CashiroTransitions.verticalSlideExit,
                    popEnterTransition = CashiroTransitions.verticalSlidePopEnter,
                    popExitTransition = CashiroTransitions.verticalSlidePopExit
                ) {
                    AnalyticsScreen(
                        onNavigateToTransactions = { category, merchant, period, currency ->
                            navController.safeNavigate(
                                Transactions(
                                    category = category,
                                    merchant = merchant,
                                    period = period,
                                    currency = currency
                                )
                            )
                        },
                        animatedContentScope = this@composable,
                        blurEffects = themeUiState.blurEffects,
                    )
                }

                // Chat Screen
                composable<Chat>(
                    enterTransition = CashiroTransitions.verticalSlideEnter,
                    exitTransition = CashiroTransitions.verticalSlideExit,
                    popEnterTransition = CashiroTransitions.verticalSlidePopEnter,
                    popExitTransition = CashiroTransitions.verticalSlidePopExit
                ) {
                    ChatScreen(
                        modifier = Modifier.imePadding(),
                        onNavigateToSettings = { navController.safeNavigate(Settings) },
                        onNavigateBack = { navController.safePopBackStack() }
                    )
                }

                /* SETTINGS & SUB-SCREENS ---- */
                composable<Settings>(
                    enterTransition = CashiroTransitions.horizontalSlideEnter,
                    exitTransition = CashiroTransitions.horizontalSlideExit,
                    popEnterTransition = CashiroTransitions.horizontalSlidePopEnter,
                    popExitTransition = CashiroTransitions.horizontalSlidePopExit
                ) {
                    SettingsScreen(
                        onNavigateBack = { navController.safePopBackStack() },
                        onNavigateToCategories = { navController.safeNavigate(Categories) },
                        onNavigateToManageAccounts = { navController.safeNavigate(ManageAccounts) },
                        onNavigateToRules = { navController.safeNavigate(Rules) },
                        onNavigateToAppearance = { navController.safeNavigate(Appearance) },
                        onNavigateToProfile = { navController.safeNavigate(Profile) },
                        onNavigateToSms = { navController.safeNavigate(SmsSettings) },
                        onNavigateToNotifications = { navController.safeNavigate(NotificationSettings) },
                        onNavigateToWebhooks = { navController.safeNavigate(Webhooks) },
                        onNavigateToBudgets = { navController.safeNavigate(Budgets()) },
                        onNavigateToDataPrivacy = { navController.safeNavigate(DataPrivacy) },
                        onNavigateToAbout = { navController.safeNavigate(About) },
                        blurEffects = themeUiState.blurEffects
                    )
                }

                composable<Webhooks>(
                    enterTransition = CashiroTransitions.horizontalSlideEnter,
                    exitTransition = CashiroTransitions.horizontalSlideExit,
                    popEnterTransition = CashiroTransitions.horizontalSlidePopEnter,
                    popExitTransition = CashiroTransitions.horizontalSlidePopExit
                ) {
                    WebhooksScreen(
                        onNavigateBack = { navController.safePopBackStack() },
                        onNavigateToEditor = { profileId ->
                            navController.safeNavigate(WebhookEditor(profileId))
                        }
                    )
                }

                composable<WebhookEditor>(
                    enterTransition = CashiroTransitions.horizontalSlideEnter,
                    exitTransition = CashiroTransitions.horizontalSlideExit,
                    popEnterTransition = CashiroTransitions.horizontalSlidePopEnter,
                    popExitTransition = CashiroTransitions.horizontalSlidePopExit
                ) { backStackEntry ->
                    val route = backStackEntry.toRoute<WebhookEditor>()
                    WebhookEditorScreen(
                        profileId = route.profileId,
                        onNavigateBack = { navController.safePopBackStack() }
                    )
                }

                composable<About>(
                    enterTransition = CashiroTransitions.horizontalSlideEnter,
                    exitTransition = CashiroTransitions.horizontalSlideExit,
                    popEnterTransition = CashiroTransitions.horizontalSlidePopEnter,
                    popExitTransition = CashiroTransitions.horizontalSlidePopExit
                ) {
                    AboutScreen(
                        onNavigateBack = { navController.safePopBackStack() },
                        onNavigateToLicenses = { navController.safeNavigate(Licenses) },
                        onNavigateToDeveloper = { navController.safeNavigate(DeveloperOptions) },
                        blurEffects = themeUiState.blurEffects
                    )
                }

                composable<Licenses>(
                    enterTransition = CashiroTransitions.horizontalSlideEnter,
                    exitTransition = CashiroTransitions.horizontalSlideExit,
                    popEnterTransition = CashiroTransitions.horizontalSlidePopEnter,
                    popExitTransition = CashiroTransitions.horizontalSlidePopExit
                ) {
                    LicensesScreen(
                        onNavigateBack = { navController.safePopBackStack() }
                    )
                }

                composable<DataPrivacy>(
                    enterTransition = CashiroTransitions.horizontalSlideEnter,
                    exitTransition = CashiroTransitions.horizontalSlideExit,
                    popEnterTransition = CashiroTransitions.horizontalSlidePopEnter,
                    popExitTransition = CashiroTransitions.horizontalSlidePopExit
                ) {
                    DataPrivacyScreen(
                        onNavigateBack = { navController.safePopBackStack() },
                        onNavigateToAccounts = { navController.safeNavigate(ManageAccounts) },
                        blurEffects = themeUiState.blurEffects
                    )
                }

                composable<DeveloperOptions>(
                    enterTransition = CashiroTransitions.horizontalSlideEnter,
                    exitTransition = CashiroTransitions.horizontalSlideExit,
                    popEnterTransition = CashiroTransitions.horizontalSlidePopEnter,
                    popExitTransition = CashiroTransitions.horizontalSlidePopExit
                ) {
                    DeveloperScreen(
                        onNavigateBack = { navController.safePopBackStack() }
                    )
                }

                composable<SmsSettings>(
                    enterTransition = CashiroTransitions.horizontalSlideEnter,
                    exitTransition = CashiroTransitions.horizontalSlideExit,
                    popEnterTransition = CashiroTransitions.horizontalSlidePopEnter,
                    popExitTransition = CashiroTransitions.horizontalSlidePopExit
                ) {
                    SMSScreen(
                        onNavigateBack = { navController.safePopBackStack() },
                        onNavigateToUnrecognizedSms = { navController.safeNavigate(UnrecognizedSms) },
                        blurEffects = themeUiState.blurEffects
                    )
                }

                composable<Profile>(
                    enterTransition = CashiroTransitions.horizontalSlideEnter,
                    exitTransition = CashiroTransitions.horizontalSlideExit,
                    popEnterTransition = CashiroTransitions.horizontalSlidePopEnter,
                    popExitTransition = CashiroTransitions.horizontalSlidePopExit
                ) {
                    ProfileScreen(
                        onNavigateBack = { navController.safePopBackStack() }
                    )
                }

                composable<Appearance>(
                    enterTransition = CashiroTransitions.horizontalSlideEnter,
                    exitTransition = CashiroTransitions.horizontalSlideExit,
                    popEnterTransition = CashiroTransitions.horizontalSlidePopEnter,
                    popExitTransition = CashiroTransitions.horizontalSlidePopExit
                ) {
                    AppearanceScreen(
                        onNavigateBack = { navController.safePopBackStack() }
                    )
                }

                composable<NotificationSettings>(
                    enterTransition = CashiroTransitions.horizontalSlideEnter,
                    exitTransition = CashiroTransitions.horizontalSlideExit,
                    popEnterTransition = CashiroTransitions.horizontalSlidePopEnter,
                    popExitTransition = CashiroTransitions.horizontalSlidePopExit
                ) {
                    NotificationScreen(
                        onNavigateBack = { navController.safePopBackStack() },
                        blurEffects = themeUiState.blurEffects,
                    )
                }

                composable<Categories>(
                    enterTransition = CashiroTransitions.horizontalSlideEnter,
                    exitTransition = CashiroTransitions.horizontalSlideExit,
                    popEnterTransition = CashiroTransitions.horizontalSlidePopEnter,
                    popExitTransition = CashiroTransitions.horizontalSlidePopExit
                ) {
                    CategoriesScreen(
                        onNavigateBack = { navController.safePopBackStack() },
                        blurEffects = themeUiState.blurEffects
                    )
                }

                composable<UnrecognizedSms>(
                    enterTransition = CashiroTransitions.horizontalSlideEnter,
                    exitTransition = CashiroTransitions.horizontalSlideExit,
                    popEnterTransition = CashiroTransitions.horizontalSlidePopEnter,
                    popExitTransition = CashiroTransitions.horizontalSlidePopExit
                ) {
                    UnrecognizedSmsScreen(
                        onNavigateBack = { navController.safePopBackStack() }
                    )
                }


                composable<ManageAccounts>(
                    enterTransition = CashiroTransitions.horizontalSlideEnter,
                    exitTransition = CashiroTransitions.horizontalSlideExit,
                    popEnterTransition = CashiroTransitions.horizontalSlidePopEnter,
                    popExitTransition = CashiroTransitions.horizontalSlidePopExit
                ) {
                    ManageAccountsScreen(
                        onNavigateBack = { navController.safePopBackStack() },
                        onNavigateToAccountDetail = { bankName, last4 ->
                            navController.safeNavigate(AccountDetail(bankName, last4))
                        },
                        blurEffects = themeUiState.blurEffects
                    )
                }

                composable<AddAccount>(
                    enterTransition = CashiroTransitions.horizontalSlideEnter,
                    exitTransition = CashiroTransitions.horizontalSlideExit,
                    popEnterTransition = CashiroTransitions.horizontalSlidePopEnter,
                    popExitTransition = CashiroTransitions.horizontalSlidePopExit
                ) {
                    AddAccountScreen(
                        onNavigateBack = { navController.safePopBackStack() }
                    )
                }

                composable<Rules>(
                    enterTransition = CashiroTransitions.horizontalSlideEnter,
                    exitTransition = CashiroTransitions.horizontalSlideExit,
                    popEnterTransition = CashiroTransitions.horizontalSlidePopEnter,
                    popExitTransition = CashiroTransitions.horizontalSlidePopExit
                ) {
                    RulesScreen(
                        onNavigateBack = { navController.safePopBackStack() },
                        onNavigateToCreateRule = { navController.safeNavigate(CreateRule()) },
                        onEditRule = { rule ->
                            navController.safeNavigate(CreateRule(ruleId = rule.id))
                        },
                        blurEffects = themeUiState.blurEffects
                    )
                }

                composable<CreateRule>(
                    enterTransition = CashiroTransitions.horizontalSlideEnter,
                    exitTransition = CashiroTransitions.horizontalSlideExit,
                    popEnterTransition = CashiroTransitions.horizontalSlidePopEnter,
                    popExitTransition = CashiroTransitions.horizontalSlidePopExit
                ) { backStackEntry ->
                    val createRuleRoute = backStackEntry.toRoute<CreateRule>()
                    val rulesViewModel: RulesViewModel = hiltViewModel()
                    val rules by rulesViewModel.rules.collectAsState()
                    val existingRule = remember(createRuleRoute.ruleId, rules) {
                        rules.find { it.id == createRuleRoute.ruleId }
                    }
                    
                    CreateRuleScreen(
                        onNavigateBack = { navController.safePopBackStack() },
                        onSaveRule = { rule ->
                            if (createRuleRoute.ruleId != null) {
                                rulesViewModel.updateRule(rule)
                            } else {
                                rulesViewModel.createRule(rule)
                            }
                            navController.safePopBackStack()
                        },
                        existingRule = existingRule,
                        rulesViewModel = rulesViewModel
                    )
                }

                /* DETAIL SCREENS (with shared transitions) ---- */
                composable<TransactionDetail>(
                    enterTransition = CashiroTransitions.noneEnter,
                    exitTransition = CashiroTransitions.noneExit,
                    popEnterTransition = CashiroTransitions.noneEnter,
                    popExitTransition = CashiroTransitions.noneExit
                ) { backStackEntry ->
                    val transactionDetail = backStackEntry.toRoute<TransactionDetail>()
                    TransactionDetailScreen(
                        transactionId = transactionDetail.transactionId,
                        sharedElementKey = transactionDetail.sharedElementKey,
                        onNavigateBack = {
                            onEditComplete()
                            navController.safePopBackStack()
                        },
                        animatedContentScope = this@composable,
                        blurEffects = themeUiState.blurEffects,
                    )
                }

                composable<AddTransaction>(
                    enterTransition = CashiroTransitions.noneEnter,
                    exitTransition = CashiroTransitions.noneExit,
                    popEnterTransition = CashiroTransitions.noneEnter,
                    popExitTransition = CashiroTransitions.noneExit
                ) {
                    Box(Modifier.fillMaxSize())
                }

                composable<AccountDetail>(
                    enterTransition = CashiroTransitions.horizontalSlideEnter,
                    exitTransition = CashiroTransitions.horizontalSlideExit,
                    popEnterTransition = CashiroTransitions.horizontalSlidePopEnter,
                    popExitTransition = CashiroTransitions.horizontalSlidePopExit
                ) { backStackEntry ->
                    val accountDetail = backStackEntry.toRoute<AccountDetail>()
                    AccountDetailScreen(
                        navController = navController,
                        bankName = accountDetail.bankName,
                        accountLast4 = accountDetail.accountLast4,
                        animatedContentScope = this@composable
                    )
                }

                composable<Subscriptions>(
                    enterTransition = CashiroTransitions.noneEnter,
                    exitTransition = CashiroTransitions.noneExit,
                    popEnterTransition = CashiroTransitions.noneEnter,
                    popExitTransition = CashiroTransitions.noneExit
                ) {
                    SubscriptionsScreen(
                        onNavigateBack = { navController.safePopBackStack() },
                        onEditSubscription = { id ->
                            navController.safeNavigate(AddTransaction(initialTab = 1, subscriptionId = id))
                        },
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedContentScope = this@composable
                    )
                }

                composable<Transactions>(
                    enterTransition = CashiroTransitions.noneEnter,
                    exitTransition = CashiroTransitions.noneExit,
                    popEnterTransition = CashiroTransitions.noneEnter,
                    popExitTransition = CashiroTransitions.noneExit
                ) { backStackEntry ->
                    val transactions = backStackEntry.toRoute<Transactions>()
                    TransactionsScreen(
                        transactionsViewModel = transactionsViewModel,
                        initialCategory = transactions.category,
                        initialMerchant = transactions.merchant,
                        initialPeriod = transactions.period,
                        initialCurrency = transactions.currency,
                        initialType = transactions.type,
                        focusSearch = transactions.focusSearch,
                        onNavigateBack = { navController.safePopBackStack() },
                        onTransactionClick = { transactionId, key ->
                            navController.safeNavigate(TransactionDetail(transactionId, key))
                        },
                        onNavigateToSettings = {
                            navController.safeNavigate(Settings)
                        },
                        animatedContentScope = this@composable,
                        blurEffects = themeUiState.blurEffects
                    )
                }

                composable<Budgets>(
                    enterTransition = CashiroTransitions.horizontalSlideEnter,
                    exitTransition = CashiroTransitions.horizontalSlideExit,
                    popEnterTransition = CashiroTransitions.horizontalSlidePopEnter,
                    popExitTransition = CashiroTransitions.horizontalSlidePopExit
                ) { backStackEntry ->
                    val budgets = backStackEntry.toRoute<Budgets>()
                    BudgetsScreen(
                        onNavigateBack = { navController.safePopBackStack() },
                        onBudgetClick = { id, key ->
                            navController.safeNavigate(BudgetDetail(budgetId = id, sharedElementKey = key))
                        },
                        onHistoryClick = { id ->
                            navController.safeNavigate(BudgetHistory(id))
                        },
                        animatedContentScope = this@composable,
                        sharedElementPrefix = budgets.sharedElementPrefix,
                        blurEffects = themeUiState.blurEffects
                    )
                }

                composable<BudgetDetail>(
                    enterTransition = CashiroTransitions.horizontalSlideEnter,
                    exitTransition = CashiroTransitions.horizontalSlideExit,
                    popEnterTransition = CashiroTransitions.horizontalSlidePopEnter,
                    popExitTransition = CashiroTransitions.horizontalSlidePopExit
                ) { backStackEntry ->
                    val budgetDetail = backStackEntry.toRoute<BudgetDetail>()
                    BudgetDetailScreen(
                        budgetId = budgetDetail.budgetId,
                        startDate = budgetDetail.startDate,
                        endDate = budgetDetail.endDate,
                        onNavigateBack = { navController.safePopBackStack() },
                        onNavigateToHistory = { id -> navController.safeNavigate(BudgetHistory(id)) },
                        onTransactionClick = { transactionId, key ->
                            navController.safeNavigate(TransactionDetail(transactionId, key))
                        },
                        animatedContentScope = this@composable,
                        sharedElementKey = budgetDetail.sharedElementKey,
                        blurEffects = themeUiState.blurEffects
                    )
                }

                composable<BudgetHistory>(
                    enterTransition = CashiroTransitions.horizontalSlideEnter,
                    exitTransition = CashiroTransitions.horizontalSlideExit,
                    popEnterTransition = CashiroTransitions.horizontalSlidePopEnter,
                    popExitTransition = CashiroTransitions.horizontalSlidePopExit
                ) { backStackEntry ->
                    val budgetHistory = backStackEntry.toRoute<BudgetHistory>()
                    BudgetHistoryScreen(
                        budgetId = budgetHistory.budgetId,
                        onNavigateBack = { navController.safePopBackStack() },
                        onNavigateToDetail = { id, start, end ->
                            navController.safeNavigate(BudgetDetail(
                                budgetId = id,
                                startDate = start?.toString(),
                                endDate = end?.toString()
                            ))
                        }
                    )
                }
            }
        }

        SharedTransitionLayout {
            // Add Screen Overlay - Handled here for shared transition from FAB
            AnimatedVisibility(
                visible = isAddTransactionScreen,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                val addTransaction = if (isAddTransactionScreen) {
                    try { navBackStackEntry?.toRoute<AddTransaction>() ?: AddTransaction() }
                    catch (_: Exception) { AddTransaction() }
                } else AddTransaction()

                AddScreen(
                    onNavigateBack = { navController.safePopBackStack() },
                    animatedVisibilityScope = this@AnimatedVisibility,
                    initialTab = addTransaction.initialTab,
                    subscriptionId = addTransaction.subscriptionId,
                    transactionType = addTransaction.type,
                    blurEffects = themeUiState.blurEffects,
                )
            }

            // FABs Container - Shown on Home, Transactions, Subscriptions, and Budget Detail
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                AnimatedVisibility(
                    visible = isHomeScreen || isTransactionsScreen || isSubscriptionsScreen || isBudgetDetailScreen,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut(),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(Dimensions.Padding.content)
                        .padding(
                            bottom = when (themeUiState.navigationBarStyle) {
                                NavigationBarStyle.FLOATING if showBottomNav -> 56.dp
                                NavigationBarStyle.NORMAL if showBottomNav -> 84.dp
                                else -> 10.dp
                            }
                        )
                        .navigationBarsPadding()
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        val smallFabContainerColor =  MaterialTheme.colorScheme.tertiaryContainer
                        val smallFabContentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        // Secondary FAB (Sync or Download)
                        if (isHomeScreen || isTransactionsScreen) {
                            SmallFloatingActionButton(
                                onClick = {
                                    if (isHomeScreen) {
                                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                        homeViewModel.scanSmsMessages()
                                    } else if (isTransactionsScreen) {
                                        showExportDialog = true
                                    }
                                },
                                modifier = Modifier
                                    .pointerInput(isHomeScreen) {
                                        if (isHomeScreen) {
                                            detectTapGestures(
                                                onLongPress = {
                                                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                                    showFullResyncDialog = true
                                                }
                                            )
                                        } else {
                                            // Use default click handling for Transactions screen
                                            detectTapGestures(onTap = {
                                                if (isTransactionsScreen) showExportDialog = true
                                            })
                                        }
                                    },
                                containerColor = smallFabContainerColor,
                                contentColor = smallFabContentColor,
                            ) {
                                if (isHomeScreen) {
                                    Icon(
                                        imageVector = Icons.Rounded.Sync,
                                        contentDescription = "Sync SMS transactions",
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Iconax.ImportArrow01,
                                        contentDescription = "Export Transactions",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        val fabContainerColor =  MaterialTheme.colorScheme.primaryContainer
                        val fabContentColor = MaterialTheme.colorScheme.onPrimaryContainer

                        // Add FAB
                        FloatingActionButton(
                            onClick = { 
                                val initialTab = if (isSubscriptionsScreen) 1 else 0
                                navController.safeNavigate(AddTransaction(initialTab = initialTab)) 
                            },
                            modifier = Modifier
                                .then(
                                    Modifier.sharedBounds(
                                        rememberSharedContentState(key = "fab_to_add"),
                                        animatedVisibilityScope = this@AnimatedVisibility,
                                        boundsTransform = { _, _ ->
                                            spring(
                                                stiffness = Spring.StiffnessLow,
                                                dampingRatio = Spring.DampingRatioLowBouncy
                                            )
                                        },
                                        resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(
                                            contentScale = ContentScale.FillBounds,
                                            alignment = Alignment.Center
                                        )
                                    )
                                        .skipToLookaheadSize()
                                )
                                .then(
                                    if (themeUiState.blurEffects) Modifier
                                        .clip(MaterialTheme.shapes.large)
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
                            contentColor = fabContentColor,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Add Transaction or Subscription",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            // Full Resync Confirmation Dialog
            if (showFullResyncDialog) {
                val containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                AlertDialog(
                    onDismissRequest = { showFullResyncDialog = false },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    title = { Text("Full Resync") },
                    text = {
                        Text(
                            "This will reprocess all SMS messages from scratch. " +
                                    "Use this to fix issues caused by updated bank parsers.\n\n" +
                                    "This may take a few seconds depending on your message history."
                        )
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
                                    onClick = { showFullResyncDialog = false },
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
                                        .weight(0.8f)
                                        .fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Cancel",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                Button(
                                    onClick = {
                                        showFullResyncDialog = false
                                        homeViewModel.scanSmsMessages(
                                            forceResync = true
                                        )
                                    },
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
                                        .weight(1f)
                                        .fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Resync All",
                                        style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                    },
                    containerColor = if (themeUiState.blurEffects)
                        MaterialTheme.colorScheme.surfaceContainerLow.copy(0.5f)
                    else MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .then(
                            if (themeUiState.blurEffects) Modifier.hazeEffect(
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
                    shape = RoundedCornerShape(16.dp),
                    dismissButton = {}
                )
            }


            // Export Transactions Dialog (Only when on TransactionsScreen)
            if (showExportDialog && isTransactionsScreen) {
                ExportTransactionsDialog(
                    transactions = transactionsUiState.transactions,
                    onDismiss = { showExportDialog = false },
                    blurEffects = themeUiState.blurEffects,
                    hazeState = hazeState
                )
            }

            // SMS Parsing Progress Dialog
            SmsParsingProgressDialog(
                isVisible = homeUiState.isScanning,
                workInfo = smsScanWorkInfo,
                onDismiss = { homeViewModel.cancelSmsScan() },
                onCancel = { homeViewModel.cancelSmsScan() },
                blurEffects = themeUiState.blurEffects,
                hazeState = hazeState
            )
        }

        // Bottom Navigation
        CashiroBottomNavigation(
            navController = navController,
            currentDestination = currentDestination,
            navigationBarStyle = themeUiState.navigationBarStyle,
            hideLabels = themeUiState.hideNavigationLabels,
            hidePill = themeUiState.hidePillIndicator,
            blurEffects = themeUiState.blurEffects,
            visible = showBottomNav,
            modifier = Modifier.align(Alignment.BottomCenter),
            hazeState = hazeState
        )
    }
}
