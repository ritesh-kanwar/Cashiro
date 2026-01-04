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
import com.ritesh.cashiro.presentation.accounts.AccountDetailScreen
import com.ritesh.cashiro.presentation.add.AddScreen
import com.ritesh.cashiro.presentation.categories.CategoriesScreen
import com.ritesh.cashiro.presentation.profile.ProfileScreen
import com.ritesh.cashiro.presentation.transactions.TransactionDetailScreen
import com.ritesh.cashiro.ui.MainScreen
import com.ritesh.cashiro.ui.screens.AppLockScreen
import com.ritesh.cashiro.ui.screens.OnBoardingScreen
import com.ritesh.cashiro.ui.screens.settings.AppearanceScreen
import com.ritesh.cashiro.ui.screens.settings.FAQScreen
import com.ritesh.cashiro.ui.screens.settings.SettingsScreen
import com.ritesh.cashiro.ui.screens.unrecognized.UnrecognizedSmsScreen
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
                        AppLockScreen(
                                onUnlocked = {
                                        navController.navigate(Home) {
                                                popUpTo(AppLock) { inclusive = true }
                                        }
                                }
                        )
                }
                composable<OnBoarding>(
                        enterTransition = { EnterTransition.None },
                        exitTransition = { ExitTransition.None },
                        popEnterTransition = { EnterTransition.None },
                        popExitTransition = { ExitTransition.None }
                ) {
                        OnBoardingScreen(
                                onOnBoardingComplete = {
                                        navController.navigate(Home) {
                                                popUpTo(OnBoarding) { inclusive = true }
                                        }
                                }
                        )
                }
                composable<Home>(
                        enterTransition = { EnterTransition.None },
                        exitTransition = { ExitTransition.None },
                        popEnterTransition = { EnterTransition.None },
                        popExitTransition = { ExitTransition.None }
                ) { MainScreen(rootNavController = navController) }

                composable<Settings>(
                        enterTransition = { EnterTransition.None },
                        exitTransition = { ExitTransition.None },
                        popEnterTransition = { EnterTransition.None },
                        popExitTransition = { ExitTransition.None }
                ) {
                        SettingsScreen(
                                themeViewModel = themeViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToCategories = { navController.navigate(Categories) },
                                onNavigateToUnrecognizedSms = {
                                        navController.navigate(UnrecognizedSms)
                                },
                                onNavigateToManageAccounts = {
                                        navController.navigate(ManageAccounts)
                                },
                                onNavigateToRules = { navController.navigate(Rules) },
                                onNavigateToFaq = { navController.navigate(Faq) },
                                onNavigateToAppearance = { navController.navigate(Appearance) },
                                onNavigateToProfile = { navController.navigate(Profile) }
                        )
                }

                composable<Profile> {
                        ProfileScreen(
                                onNavigateBack = { navController.popBackStack() },
                                profileViewModel = hiltViewModel()
                        )
                }

                composable<Appearance> {
                        AppearanceScreen(
                                onNavigateBack = { navController.popBackStack() },
                                themeViewModel = themeViewModel
                        )
                }

                composable<Categories>(
                        enterTransition = { EnterTransition.None },
                        exitTransition = { ExitTransition.None },
                        popEnterTransition = { EnterTransition.None },
                        popExitTransition = { ExitTransition.None }
                ) { CategoriesScreen(onNavigateBack = { navController.popBackStack() }) }

                composable<TransactionDetail>(
                        enterTransition = { EnterTransition.None },
                        exitTransition = { ExitTransition.None },
                        popEnterTransition = { EnterTransition.None },
                        popExitTransition = { ExitTransition.None }
                ) { backStackEntry ->
                        val transactionDetail = backStackEntry.toRoute<TransactionDetail>()
                        TransactionDetailScreen(
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
                ) { AddScreen(onNavigateBack = { navController.popBackStack() }) }

                composable<UnrecognizedSms>(
                        enterTransition = { EnterTransition.None },
                        exitTransition = { ExitTransition.None },
                        popEnterTransition = { EnterTransition.None },
                        popExitTransition = { ExitTransition.None }
                ) { UnrecognizedSmsScreen(onNavigateBack = { navController.popBackStack() }) }

                composable<Faq>(
                        enterTransition = { EnterTransition.None },
                        exitTransition = { ExitTransition.None },
                        popEnterTransition = { EnterTransition.None },
                        popExitTransition = { ExitTransition.None }
                ) { FAQScreen(onNavigateBack = { navController.popBackStack() }) }

                composable<AccountDetail>(
                        enterTransition = { EnterTransition.None },
                        exitTransition = { ExitTransition.None },
                        popEnterTransition = { EnterTransition.None },
                        popExitTransition = { ExitTransition.None }
                ) { backStackEntry ->
                        val accountDetail = backStackEntry.toRoute<AccountDetail>()
                        AccountDetailScreen(navController = navController)
                }
        }
}
