package com.ritesh.cashiro.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.ritesh.cashiro.presentation.categories.CategoriesScreen
import com.ritesh.cashiro.ui.MainScreen
import com.ritesh.cashiro.ui.screens.settings.SettingsScreen
import com.ritesh.cashiro.ui.viewmodel.ThemeViewModel

@Composable
fun CashiroNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    themeViewModel: ThemeViewModel = hiltViewModel(),
    startDestination: Any = Home,
    onEditComplete: () -> Unit = {}
) {
    // Use a stable start destination
    val stableStartDestination = remember { startDestination }
    
    NavHost(
        navController = navController,
        startDestination = stableStartDestination,
        modifier = modifier,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None }
    ) {
        composable<AppLock>(
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            com.ritesh.cashiro.ui.screens.AppLockScreen(
                onUnlocked = {
                    navController.navigate(Home) {
                        popUpTo(AppLock) { inclusive = true }
                    }
                }
            )
        }
        composable<Permission>(
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            com.ritesh.cashiro.ui.screens.PermissionScreen(
                onPermissionGranted = {
                    navController.navigate(Home) {
                        popUpTo(Permission) { inclusive = true }
                    }
                }
            )
        }
        composable<Home>(
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            MainScreen(
                rootNavController = navController
            )
        }
        
        composable<Settings>(
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            SettingsScreen(
                themeViewModel = themeViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToCategories = {
                    navController.navigate(Categories)
                },
                onNavigateToUnrecognizedSms = {
                    navController.navigate(UnrecognizedSms)
                },
                onNavigateToFaq = {
                    navController.navigate(Faq)
                }
            )
        }
        
        composable<Categories>(
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            CategoriesScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable<TransactionDetail>(
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) { backStackEntry ->
            val transactionDetail = backStackEntry.toRoute<TransactionDetail>()
            com.ritesh.cashiro.presentation.transactions.TransactionDetailScreen(
                transactionId = transactionDetail.transactionId,
                onNavigateBack = {
                    onEditComplete()
                    navController.popBackStack()
                }
            )
        }
        
        composable<AddTransaction>(
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            com.ritesh.cashiro.presentation.add.AddScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable<UnrecognizedSms>(
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            com.ritesh.cashiro.ui.screens.unrecognized.UnrecognizedSmsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable<Faq>(
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            com.ritesh.cashiro.ui.screens.settings.FAQScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable<Rules>(
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            com.ritesh.cashiro.ui.screens.rules.RulesScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToCreateRule = {
                    navController.navigate(CreateRule)
                }
            )
        }

        composable<CreateRule>(
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            val rulesViewModel: com.ritesh.cashiro.ui.viewmodel.RulesViewModel = hiltViewModel()
            com.ritesh.cashiro.ui.screens.rules.CreateRuleScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSaveRule = { rule ->
                    rulesViewModel.createRule(rule)
                    navController.popBackStack()
                }
            )
        }
        
        composable<AccountDetail>(
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) { backStackEntry ->
            val accountDetail = backStackEntry.toRoute<AccountDetail>()
            com.ritesh.cashiro.presentation.accounts.AccountDetailScreen(
                navController = navController
            )
        }
        
    }
}