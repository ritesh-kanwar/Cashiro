package com.ritesh.cashiro.ui.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ritesh.cashiro.data.preferences.UserPreferencesRepository
import com.ritesh.cashiro.presentation.profile.EditProfileState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@HiltViewModel
class OnBoardingViewModel
@Inject
constructor(
        @ApplicationContext private val context: Context,
        private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnBoardingUiState())
    val uiState: StateFlow<OnBoardingUiState> = _uiState.asStateFlow()

    init {
        checkPermissionStatus()
        observeUserPreferences()
    }

    private fun checkPermissionStatus() {
        val hasPermission =
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
                        PackageManager.PERMISSION_GRANTED

        _uiState.update { it.copy(hasPermission = hasPermission) }
    }

    private fun observeUserPreferences() {
        userPreferencesRepository
                .userPreferences
                .onEach { prefs ->
                    _uiState.update {
                        it.copy(
                                hasSkippedPermission = prefs.hasSkippedSmsPermission,
                                profileState =
                                        it.profileState.copy(
                                                editedUserName = prefs.userName,
                                                editedProfileImageUri =
                                                        prefs.profileImageUri?.let {
                                                            Uri.parse(it)
                                                        },
                                                editedProfileBackgroundColor =
                                                        if (prefs.profileBackgroundColor != 0)
                                                                Color(prefs.profileBackgroundColor)
                                                        else Color.Transparent,
                                                editedBannerImageUri =
                                                        prefs.bannerImageUri?.let { Uri.parse(it) }
                                        )
                        )
                    }
                }
                .launchIn(viewModelScope)
    }

    fun nextStep() {
        _uiState.update { it.copy(currentStep = it.currentStep + 1) }
    }

    fun previousStep() {
        if (_uiState.value.currentStep > 1) {
            _uiState.update { it.copy(currentStep = it.currentStep - 1) }
        }
    }

    fun onNameChange(name: String) {
        _uiState.update {
            it.copy(profileState = it.profileState.copy(editedUserName = name, hasChanges = true))
        }
    }

    fun onProfileImageChange(uri: Uri?) {
        _uiState.update {
            it.copy(
                    profileState =
                            it.profileState.copy(editedProfileImageUri = uri, hasChanges = true)
            )
        }
    }

    fun onBackgroundColorChange(color: Color) {
        _uiState.update {
            it.copy(
                    profileState =
                            it.profileState.copy(
                                    editedProfileBackgroundColor = color,
                                    hasChanges = true
                            )
            )
        }
    }

    fun onBannerImageChange(uri: Uri?) {
        _uiState.update {
            it.copy(
                    profileState =
                            it.profileState.copy(editedBannerImageUri = uri, hasChanges = true)
            )
        }
    }

    fun saveProfile() {
        val profile = _uiState.value.profileState
        viewModelScope.launch {
            userPreferencesRepository.updateUserName(profile.editedUserName)
            userPreferencesRepository.updateProfileImageUri(
                    profile.editedProfileImageUri?.toString()
            )
            userPreferencesRepository.updateProfileBackgroundColor(
                    profile.editedProfileBackgroundColor.toArgb()
            )
            userPreferencesRepository.updateBannerImageUri(profile.editedBannerImageUri?.toString())
            nextStep()
        }
    }

    fun onPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(hasPermission = granted) }
        if (granted) {
            viewModelScope.launch { userPreferencesRepository.updateSkippedSmsPermission(false) }
        }
    }

    fun onPermissionDenied() {
        _uiState.update { it.copy(showRationale = true) }
    }
}

data class OnBoardingUiState(
        val currentStep: Int = 1,
        val hasPermission: Boolean = false,
        val hasSkippedPermission: Boolean = false,
        val showRationale: Boolean = false,
        val profileState: EditProfileState = EditProfileState()
)
