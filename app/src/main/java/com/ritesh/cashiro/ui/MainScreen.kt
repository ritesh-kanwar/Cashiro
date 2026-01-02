package com.ritesh.cashiro.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ritesh.cashiro.R
import com.ritesh.cashiro.navigation.AddTransaction
import com.ritesh.cashiro.navigation.AnimatedNavHost
import com.ritesh.cashiro.navigation.TransactionDetail
import com.ritesh.cashiro.navigation.navPage
import com.ritesh.cashiro.presentation.accounts.AddAccountScreen
import com.ritesh.cashiro.presentation.accounts.ManageAccountsScreen
import com.ritesh.cashiro.presentation.categories.CategoriesScreen
import com.ritesh.cashiro.presentation.home.HomeScreen
import com.ritesh.cashiro.presentation.home.HomeViewModel
import com.ritesh.cashiro.presentation.navigation.BottomNavItem
import com.ritesh.cashiro.presentation.subscriptions.SubscriptionsScreen
import com.ritesh.cashiro.presentation.transactions.TransactionsScreen
import com.ritesh.cashiro.ui.components.SpotlightTutorial
import com.ritesh.cashiro.ui.screens.analytics.AnalyticsScreen
import com.ritesh.cashiro.ui.screens.chat.ChatScreen
import com.ritesh.cashiro.ui.screens.rules.CreateRuleScreen
import com.ritesh.cashiro.ui.screens.rules.RulesScreen
import com.ritesh.cashiro.ui.screens.settings.AppearanceScreen
import com.ritesh.cashiro.ui.screens.settings.FAQScreen
import com.ritesh.cashiro.ui.screens.settings.SettingsScreen
import com.ritesh.cashiro.ui.screens.unrecognized.UnrecognizedSmsScreen
import com.ritesh.cashiro.ui.viewmodel.RulesViewModel
import com.ritesh.cashiro.ui.viewmodel.SpotlightViewModel
import com.ritesh.cashiro.ui.viewmodel.ThemeViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainScreen(
        rootNavController: NavHostController? = null,
        navController: NavHostController = rememberNavController(),
        themeViewModel: ThemeViewModel = hiltViewModel(),
        spotlightViewModel: SpotlightViewModel = hiltViewModel()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route
    val spotlightState by spotlightViewModel.spotlightState.collectAsState()

    val navigationItems =
            listOf(BottomNavItem.Home, BottomNavItem.Analytics, BottomNavItem.Settings)

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold() { paddingValues ->
            Box(modifier = Modifier.fillMaxSize()) {
                AnimatedNavHost(
                    navController = navController,
                    startDestination = "home",
                    modifier = Modifier
                        .padding(
                            start = paddingValues.calculateLeftPadding(
                                    LayoutDirection.Ltr
                                ),
                            end = paddingValues.calculateRightPadding(
                                    LayoutDirection.Rtl
                                )
                        ),
                    pages = arrayOf(
                        navPage("home") {
                            val homeViewModel: HomeViewModel = hiltViewModel()
                            HomeScreen(
                                viewModel = homeViewModel,
                                navController = rootNavController ?: navController,
                                onNavigateToSettings = { navController.navigate("settings") },
                                onNavigateToChat = { navController.navigate("chat") },
                                onNavigateToTransactions = { navController.navigate("transactions") },
                                onNavigateToTransactionsWithSearch = {
                                    navController.navigate(
                                        "transactions?focusSearch=true"
                                    ) },
                                onNavigateToSubscriptions = {
                                    navController.navigate("subscriptions") },
                                onNavigateToAddScreen ={
                                    rootNavController?.navigate(AddTransaction) },
                                onTransactionClick = { transactionId ->
                                    rootNavController?.navigate(
                                        TransactionDetail(transactionId)
                                    ) },
                                onFabPositioned = { position ->
                                    spotlightViewModel.updateFabPosition(
                                        position
                                    )
                                }
                            ) },

                        navPage(
                            route = "transactions?category={category}&merchant={merchant}&period={period}&currency={currency}&focusSearch={focusSearch}",
                            arguments = listOf(
                                navArgument("category") {
                                    type = NavType.StringType
                                    nullable = true
                                    defaultValue = null },

                                navArgument("merchant") {
                                    type = NavType.StringType
                                    nullable = true
                                    defaultValue = null },

                                navArgument("period") {
                                    type = NavType.StringType
                                    nullable = true
                                    defaultValue = null },

                                navArgument("currency") {
                                    type = NavType.StringType
                                    nullable = true
                                    defaultValue = null },

                                navArgument("focusSearch") {
                                    type = NavType.BoolType
                                    defaultValue = false
                                }
                            )
                        ) { backStackEntry: NavBackStackEntry ->
                            val category = backStackEntry.arguments?.getString("category")
                            val merchant = backStackEntry.arguments?.getString("merchant")
                            val period = backStackEntry.arguments?.getString("period")
                            val currency = backStackEntry.arguments?.getString("currency")
                            val focusSearch = backStackEntry.arguments?.getBoolean(
                                "focusSearch"
                            ) ?: false
                            TransactionsScreen(
                                initialCategory = category,
                                initialMerchant = merchant,
                                initialPeriod = period,
                                initialCurrency = currency,
                                focusSearch = focusSearch,
                                onNavigateBack = { navController.popBackStack() },
                                onTransactionClick = { transactionId ->
                                    rootNavController?.navigate(
                                        TransactionDetail(transactionId)
                                    ) },
                                onAddTransactionClick = { rootNavController?.navigate(AddTransaction) },
                                onNavigateToSettings = { navController.navigate("settings") }
                            )
                          },

                        navPage("subscriptions") {
                            SubscriptionsScreen(
                                onNavigateBack = { navController.popBackStack() },
                                onAddSubscriptionClick = { rootNavController?.navigate(AddTransaction) }
                            ) },

                        navPage("analytics") {
                            AnalyticsScreen(
                                onNavigateToChat = { navController.navigate("chat") },
                                onNavigateToTransactions = { category, merchant, period, currency ->
                                    val route = buildString {
                                        append("transactions")
                                        val params = mutableListOf<String>()
                                        category?.let {
                                            val encoded = java.net.URLEncoder.encode(
                                                it,
                                                "UTF-8"
                                            )
                                            params.add("category=$encoded")
                                        }
                                        merchant?.let {
                                            val encoded = java.net.URLEncoder.encode(
                                                it,
                                                "UTF-8"
                                            )
                                            params.add("merchant=$encoded")
                                        }
                                        period?.let { params.add("period=$it") }
                                        currency?.let { params.add("currency=$it") }
                                        if (params.isNotEmpty()) {
                                            append("?")
                                            append(params.joinToString("&"))
                                        }
                                    }
                                    navController.navigate(route) },
                                onNavigateToSettings = { navController.navigate("settings") }
                            ) },

                        navPage("chat") {
                            ChatScreen(
                                modifier = Modifier.imePadding(),
                                onNavigateToSettings = { navController.navigate("settings") }
                            ) },

                        navPage("settings") {
                            SettingsScreen(
                                themeViewModel = themeViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToCategories = { navController.navigate("categories") },
                                onNavigateToUnrecognizedSms = { navController.navigate("unrecognized_sms") },
                                onNavigateToManageAccounts = { navController.navigate("manage_accounts") },
                                onNavigateToFaq = { navController.navigate("faq") },
                                onNavigateToRules = { navController.navigate("rules") },
                                onNavigateToAppearance = {navController.navigate("appearance") }
                            ) },

                        navPage("categories") {
                            CategoriesScreen(
                                onNavigateBack = { navController.popBackStack() }
                            ) },

                        navPage("unrecognized_sms") {
                            UnrecognizedSmsScreen(
                                onNavigateBack = { navController.popBackStack() }
                            ) },

                        navPage("faq") {
                            FAQScreen(
                                onNavigateBack = { navController.popBackStack() }
                            ) },

                        navPage("manage_accounts") {
                            ManageAccountsScreen(
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToAddAccount = { navController.navigate("add_account") }
                            ) },

                        navPage("add_account") {
                            AddAccountScreen(
                                onNavigateBack = { navController.popBackStack() }
                            ) },

                        navPage("rules") {
                            RulesScreen(
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToCreateRule = { navController.navigate("create_rule") }
                            ) },

                        navPage("create_rule") {
                            val rulesViewModel: RulesViewModel =
                                hiltViewModel()
                            CreateRuleScreen(
                                onNavigateBack = { navController.popBackStack() },
                                onSaveRule = { rule ->
                                    rulesViewModel.createRule(rule)
                                    navController.popBackStack()
                                }
                            ) },
                        navPage("appearance") {
                            val rulesViewModel: RulesViewModel =
                                hiltViewModel()
                            AppearanceScreen(
                                onNavigateBack = { navController.popBackStack() },
                                themeViewModel = themeViewModel
                            ) },
                        )
                )

                // HorizontalFloatingToolbar
                if (currentRoute in listOf("home", "analytics", "settings")) {
                    HorizontalFloatingToolbar(
                            modifier =
                                    Modifier.align(Alignment.BottomCenter)
                                        .navigationBarsPadding()
//                                            .padding(
//                                                    bottom = 24.dp
//                                            ) // paddingValues.calculateBottomPadding() + 8.dp
                                            // ideally, but we have no bottom bar
                                            .shadow(
                                                    elevation = 16.dp,
                                                    shape = MaterialTheme.shapes.extraLarge
                                            ),
                            expanded = true,
                    ) {
                        navigationItems.forEach { item ->
                            val selected =
                                    currentDestination?.hierarchy?.any { it.route == item.route } ==
                                            true

                            TonalToggleButton(
                                    checked = selected,
                                    onCheckedChange = {
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                Icon(imageVector = item.icon, contentDescription = item.title)
                                AnimatedVisibility(
                                        visible = selected,
                                        enter =
                                                fadeIn() +
                                                        expandHorizontally(
                                                                MaterialTheme.motionScheme
                                                                        .fastSpatialSpec()
                                                        ),
                                        exit =
                                                fadeOut() +
                                                        shrinkHorizontally(
                                                                MaterialTheme.motionScheme
                                                                        .fastSpatialSpec()
                                                        )
                                ) {
                                    Text(
                                            text = item.title,
                                            modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Spotlight Tutorial overlay - outside Scaffold to overlay everything
        if (currentRoute == "home" &&
                        spotlightState.showTutorial &&
                        spotlightState.fabPosition != null
        ) {
            val homeViewModel: com.ritesh.cashiro.presentation.home.HomeViewModel? =
                    navController.currentBackStackEntry?.let { hiltViewModel(it) }

            SpotlightTutorial(
                    isVisible = true,
                    targetPosition = spotlightState.fabPosition,
                    message = "Tap here to scan your SMS messages for transactions",
                    onDismiss = { spotlightViewModel.dismissTutorial() },
                    onTargetClick = { homeViewModel?.scanSmsMessages() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CashiroTopAppBar(
        title: String,
        showBackButton: Boolean = false,
        showSettingsButton: Boolean = true,
        showDiscordButton: Boolean = true,
        onBackClick: () -> Unit = {},
        onSettingsClick: () -> Unit = {},
        onDiscordClick: () -> Unit = {}
) {
    Column {
        TopAppBar(
                title = { Text(title) },
                colors =
                        TopAppBarDefaults.topAppBarColors(
                                containerColor =
                                        MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                        ),
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                            )
                        }
                    }
                },
                actions = {
                    if (showDiscordButton) {
                        IconButton(onClick = onDiscordClick) {
                            Icon(
                                    painter = painterResource(id = R.drawable.ic_discord),
                                    contentDescription = "Join Discord Community",
                                    tint = Color(0xFF5865F2) // Discord brand color
                            )
                        }
                    }
                    if (showSettingsButton) {
                        IconButton(onClick = onSettingsClick) {
                            Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings"
                            )
                        }
                    }
                }
        )
        HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    }
}
