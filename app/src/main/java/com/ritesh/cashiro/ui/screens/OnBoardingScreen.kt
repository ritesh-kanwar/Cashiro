package com.ritesh.cashiro.ui.screens

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ritesh.cashiro.R
import com.ritesh.cashiro.presentation.profile.PresetAvatarSelection
import com.ritesh.cashiro.presentation.profile.ProfileCardPreview
import com.ritesh.cashiro.ui.components.ColorPickerContent
import com.ritesh.cashiro.ui.effects.overScrollVertical
import com.ritesh.cashiro.ui.theme.Dimensions
import com.ritesh.cashiro.ui.theme.Spacing
import com.ritesh.cashiro.ui.viewmodel.OnBoardingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnBoardingScreen(
    onOnBoardingComplete: () -> Unit,
    viewModel: OnBoardingViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val multiplePermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val readSmsGranted = permissions[Manifest.permission.READ_SMS] == true
            if (readSmsGranted) {
                viewModel.onPermissionResult(true)
                onOnBoardingComplete()
            } else {
                viewModel.onPermissionDenied()
            }
        }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            OnBoardingBottomBar(
                currentStep = uiState.currentStep,
                onBack = { viewModel.previousStep() },
                onContinue = {
                    when (uiState.currentStep) {
                        1 -> viewModel.nextStep()
                        2 -> viewModel.saveProfile()
                        3 -> {
                            val permissions =
                                mutableListOf(
                                    Manifest.permission.READ_SMS,
                                    Manifest.permission.RECEIVE_SMS
                                )
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            multiplePermissionLauncher.launch(permissions.toTypedArray())
                        }
                    }
                },
                isContinueEnabled =
                    when (uiState.currentStep) {
                        2 -> uiState.profileState.editedUserName.isNotBlank()
                        else -> true
                    }
            )
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = uiState.currentStep,
            transitionSpec = {
                if (targetState > initialState) {
                    slideInHorizontally { it } + fadeIn() togetherWith
                            slideOutHorizontally { -it } + fadeOut()
                } else {
                    slideInHorizontally { -it } + fadeIn() togetherWith
                            slideOutHorizontally { it } + fadeOut()
                }
                    .using(SizeTransform(clip = false))
            },
            label = "OnBoardingStepTransition"
        ) { step ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                when (step) {
                    1 -> WelcomeStep()
                    2 ->
                        ProfileStep(
                            state = uiState.profileState,
                            onNameChange = { viewModel.onNameChange(it) },
                            onProfileImageChange = { viewModel.onProfileImageChange(it) },
                            onBackgroundColorChange = {
                                viewModel.onBackgroundColorChange(it)
                            }
                        )
                    3 -> PermissionsStep(showRationale = uiState.showRationale)
                }
            }
        }
    }
}

@Composable
fun WelcomeStep() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.xl)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.mipmap.ic_launcher_foreground),
            contentDescription = "App Logo",
            modifier = Modifier.size(180.dp),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.height(Spacing.xl))

        Text(
            text = "Welcome to Cashiro",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(Spacing.md))

        Text(
            text =
                "Your personal finance assistant that stays on your device. Manage transactions, track budgets, and gain insights effortlessly.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProfileStep(
    state: com.ritesh.cashiro.presentation.profile.EditProfileState,
    onNameChange: (String) -> Unit,
    onProfileImageChange: (Uri?) -> Unit,
    onBackgroundColorChange: (Color) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = Spacing.lg)
            .overScrollVertical()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        Text(
            text = "Set Up Your Profile",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = Spacing.md)
        )

        ProfileCardPreview(
            profileImageUri = state.editedProfileImageUri,
            backgroundColor = state.editedProfileBackgroundColor,
            bannerImageUri = state.editedBannerImageUri,
            modifier = Modifier.padding(vertical = Spacing.md,horizontal = Spacing.md)
        )

        TextField(
            value = state.editedUserName,
            onValueChange = onNameChange,
            label = { Text("What should we call you?") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.md),
            shape = RoundedCornerShape(Dimensions.Radius.md),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )

        Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
            PresetAvatarSelection(
                selectedUri = state.editedProfileImageUri,
                onSelect = onProfileImageChange
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md)
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(Dimensions.Radius.md)
                )
                .padding(Spacing.md)
        ) {
            ColorPickerContent(
                initialColor = state.editedProfileBackgroundColor.toArgb(),
                onColorChanged = { onBackgroundColorChange(Color(it)) }
            )
        }
    }
}

@Composable
fun PermissionsStep(showRationale: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.xl)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.MailOutline,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(Spacing.xl))

        Text(
            text = "Automatic Detection",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(Spacing.md))

        Text(
            text =
                "Cashiro can automatically detect and categorize your bank transactions from SMS messages, saving you time and effort.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(Spacing.lg))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(Spacing.md)) {
                Text(
                    text = "Your Privacy Matters",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text =
                        "• Only transaction messages are processed\n" +
                                "• All data stays on your device\n" +
                                "• No personal messages are read",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        if (showRationale) {
            Spacer(modifier = Modifier.height(Spacing.md))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text =
                        "Without SMS access, you'll need to manually add all your transactions. We only read bank transaction messages, not personal conversations.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(Spacing.md)
                )
            }
        }
    }
}

@Composable
fun OnBoardingBottomBar(
    currentStep: Int,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    isContinueEnabled: Boolean
) {
    Surface(tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(Spacing.lg).navigationBarsPadding(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currentStep > 1) {
                TextButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = null)
                    Spacer(Modifier.width(Spacing.sm))
                    Text("Back")
                }
            } else {
                Spacer(Modifier.width(80.dp))
            }

            Button(
                onClick = onContinue,
                enabled = isContinueEnabled,
                modifier = Modifier.height(56.dp),
                shape = RoundedCornerShape(Dimensions.Radius.md)
            ) {
                Text(
                    text =
                        when (currentStep) {
                            1 -> "Get Started"
                            2 -> "Save & Continue"
                            3 -> "Enable & Finish"
                            else -> "Continue"
                        }
                )
                Spacer(Modifier.width(Spacing.sm))
                Icon(Icons.Rounded.ArrowForward, contentDescription = null)
            }
        }
    }
}
