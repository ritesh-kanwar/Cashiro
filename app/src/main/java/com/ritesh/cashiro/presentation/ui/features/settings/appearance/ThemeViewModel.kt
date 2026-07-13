package com.ritesh.cashiro.presentation.ui.features.settings.appearance

import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ritesh.cashiro.data.preferences.NavigationBarStyle
import com.ritesh.cashiro.data.preferences.AppFont
import com.ritesh.cashiro.data.preferences.ThemeStyle
import com.ritesh.cashiro.data.preferences.AccentColor
import com.ritesh.cashiro.data.preferences.AppIcon
import com.ritesh.cashiro.data.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.ritesh.cashiro.widget.updateCashiroWidgets

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    val themeUiState: StateFlow<ThemeUiState> = userPreferencesRepository.userPreferences
        .map { preferences ->
            ThemeUiState(
                isDarkTheme = preferences.isDarkThemeEnabled,
                isDynamicColorEnabled = preferences.isDynamicColorEnabled,
                hasSkippedSmsPermission = preferences.hasSkippedSmsPermission,
                isAmoledMode = preferences.isAmoledMode,
                navigationBarStyle = preferences.navigationBarStyle,
                appFont = preferences.appFont,
                themeStyle = preferences.themeStyle,
                accentColor = preferences.accentColor,
                hideNavigationLabels = preferences.hideNavigationLabels,
                hidePillIndicator = preferences.hidePillIndicator,
                blurEffects = preferences.blurEffects,
                isOnboardingFinished = preferences.hasShownScanTutorial,
                currentAppIcon = preferences.appIcon,
                isLoaded = true
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ThemeUiState(isLoaded = false)
        )

    fun updateDarkTheme(enabled: Boolean?) {
        updateWidgetAppearance { userPreferencesRepository.updateDarkThemeEnabled(enabled) }
    }

    fun updateDynamicColor(enabled: Boolean) {
        updateWidgetAppearance { userPreferencesRepository.updateDynamicColorEnabled(enabled) }
    }

    fun updateAmoledMode(enabled: Boolean) {
        updateWidgetAppearance { userPreferencesRepository.updateAmoledMode(enabled) }
    }

    fun updateNavigationBarStyle(style: NavigationBarStyle) {
        viewModelScope.launch {
            userPreferencesRepository.updateNavigationBarStyle(style)
        }
    }

    fun updateAppFont(font: AppFont) {
        viewModelScope.launch {
            userPreferencesRepository.updateAppFont(font)
        }
    }

    fun updateThemeStyle(style: ThemeStyle) {
        updateWidgetAppearance { userPreferencesRepository.updateThemeStyle(style) }
    }

    fun updateAccentColor(color: AccentColor) {
        updateWidgetAppearance { userPreferencesRepository.updateAccentColor(color) }
    }

    fun updateHideNavigationLabels(hide: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateHideNavigationLabels(hide)
        }
    }

    fun updateHidePillIndicator(hide: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateHidePillIndicator(hide)
        }
    }

    fun updateBlurEffects(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateBlurEffects(enabled)
        }
    }

    fun updateAppIcon(icon: AppIcon) = viewModelScope.launch {
        userPreferencesRepository.updateAppIcon(icon)
    }

    private fun updateWidgetAppearance(updatePreference: suspend () -> Unit) {
        viewModelScope.launch {
            updatePreference()
            updateCashiroWidgets(context)
        }
    }
}

data class ThemeUiState(
    val isDarkTheme: Boolean? = null, // null = follow system
    val isDynamicColorEnabled: Boolean = false, // Default to custom theme colors
    val hasSkippedSmsPermission: Boolean = false,
    val isAmoledMode: Boolean = false,
    val navigationBarStyle: NavigationBarStyle = NavigationBarStyle.FLOATING,
    val appFont: AppFont = AppFont.SYSTEM,
    val themeStyle: ThemeStyle = ThemeStyle.DYNAMIC,
    val accentColor: AccentColor = AccentColor.BLUE,
    val hideNavigationLabels: Boolean = false,
    val hidePillIndicator: Boolean = false,
    val blurEffects: Boolean = true,
    val isOnboardingFinished: Boolean = false,
    val currentAppIcon: AppIcon = AppIcon.ORIGINAL,
    val isLoaded: Boolean = false
)
