package com.ritesh.cashiro

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.ritesh.cashiro.presentation.navigation.AppLock
import com.ritesh.cashiro.presentation.navigation.Home
import com.ritesh.cashiro.presentation.navigation.CashiroNavHost
import com.ritesh.cashiro.presentation.navigation.OnBoarding
import com.ritesh.cashiro.presentation.navigation.Settings
import com.ritesh.cashiro.presentation.navigation.AddTransaction
import com.ritesh.cashiro.presentation.navigation.TransactionDetail
import com.ritesh.cashiro.presentation.ui.theme.CashiroTheme
import com.ritesh.cashiro.presentation.ui.features.settings.applock.AppLockViewModel
import com.ritesh.cashiro.presentation.ui.features.settings.appearance.ThemeViewModel

@Composable
fun CashiroApp(
    themeViewModel: ThemeViewModel = hiltViewModel(),
    appLockViewModel: AppLockViewModel = hiltViewModel(),
    editTransactionId: Long? = null,
    onEditComplete: () -> Unit = {},
    addTransactionTab: Int? = null,
    addTransactionType: String? = null,
    onAddComplete: () -> Unit = {}
) {
    val themeUiState by themeViewModel.themeUiState.collectAsStateWithLifecycle()
    val appLockUiState by appLockViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val darkTheme = themeUiState.isDarkTheme ?: isSystemInDarkTheme()

    val navController = rememberNavController()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Observe lifecycle events and refresh lock state when app resumes from background
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // App came to foreground - check if it should be locked
                actualAppLockViewModel.refreshLockState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Only render the app once the theme state is loaded
    if (!themeUiState.isLoaded) return

    // Determine initial destination based on persisted onboarding status
    val startDestination = remember {
        if (themeUiState.isOnboardingFinished) Home else OnBoarding
    }

    // Observe lock state changes and navigate to lock screen if needed
    // But don't navigate when user is actively in Settings configuring app lock
    LaunchedEffect(appLockUiState.isLocked, appLockUiState.isLockEnabled) {
        if (appLockUiState.isLocked && appLockUiState.isLockEnabled) {
            val currentRoute = navController.currentDestination?.route
            // Don't navigate if already on lock screen or in Settings (user is configuring)
            if (currentRoute != AppLock::class.qualifiedName &&
                currentRoute != Settings::class.qualifiedName) {
                navController.navigate(AppLock) {
                    // Don't add to back stack, force lock screen
                    popUpTo(navController.graph.startDestinationId) { inclusive = false }
                    launchSingleTop = true
                }
            }
        }
    }
    
    // Navigate to transaction detail when editTransactionId changes
    LaunchedEffect(editTransactionId) {
        editTransactionId?.let { transactionId ->
            navController.navigate(TransactionDetail(transactionId))
        }
    }

    // Navigate to Add screen when addTransactionTab changes
    LaunchedEffect(addTransactionTab, addTransactionType) {
        addTransactionTab?.let { tab ->
            navController.navigate(AddTransaction(initialTab = tab, type = addTransactionType))
            onAddComplete()
        }
    }

    CashiroTheme(
        darkTheme = darkTheme,
        themeStyle = themeUiState.themeStyle,
        dynamicColor = themeUiState.isDynamicColorEnabled,
        isAmoledMode = themeUiState.isAmoledMode,
        accentColor = themeUiState.accentColor,
        appFont = themeUiState.appFont,
        blurEffects = themeUiState.blurEffects
    ) {
        CashiroNavHost(
            navController = navController,
            startDestination = startDestination,
            onEditComplete = onEditComplete
        )
    }
}