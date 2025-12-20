package com.ritesh.cashiro.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ritesh.cashiro.data.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val themeUiState: StateFlow<ThemeUiState> = userPreferencesRepository.userPreferences
        .map { preferences ->
            ThemeUiState(
                isDarkTheme = preferences.isDarkThemeEnabled,
                isDynamicColorEnabled = preferences.isDynamicColorEnabled,
                hasSkippedSmsPermission = preferences.hasSkippedSmsPermission
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ThemeUiState()
        )

    fun updateDarkTheme(enabled: Boolean?) {
        viewModelScope.launch {
            userPreferencesRepository.updateDarkThemeEnabled(enabled)
        }
    }

    fun updateDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateDynamicColorEnabled(enabled)
        }
    }
}

data class ThemeUiState(
    val isDarkTheme: Boolean? = null, // null = follow system
    val isDynamicColorEnabled: Boolean = false, // Default to custom theme colors
    val hasSkippedSmsPermission: Boolean = false
)