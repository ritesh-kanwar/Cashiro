package com.ritesh.cashiro.presentation.profile

import android.net.Uri
import androidx.compose.ui.graphics.Color

data class ProfileScreenState(
    val userName: String = "User",
    val profileImageUri: Uri? = null,
    val profileBackgroundColor: Color = Color.Transparent,
    val bannerImageUri: Uri? = null,
    val totalTransactions: Int = 0,
    val netWorth: java.math.BigDecimal = java.math.BigDecimal.ZERO,
    val totalIncome: java.math.BigDecimal = java.math.BigDecimal.ZERO,
    val totalExpense: java.math.BigDecimal = java.math.BigDecimal.ZERO,
    val activeSubscriptions: Int = 0,
    val isLoading: Boolean = false,
    val isEditSheetOpen: Boolean = false,
    val hasStoragePermission: Boolean = false,
    val editState: EditProfileState = EditProfileState()
)

data class EditProfileState(
    val editedUserName: String = "",
    val editedProfileImageUri: Uri? = null,
    val editedProfileBackgroundColor: Color = Color.Transparent,
    val editedBannerImageUri: Uri? = null,
    val hasChanges: Boolean = false
)
