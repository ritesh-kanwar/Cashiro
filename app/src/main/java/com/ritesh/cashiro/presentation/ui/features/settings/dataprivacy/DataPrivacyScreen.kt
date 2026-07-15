package com.ritesh.cashiro.presentation.ui.features.settings.dataprivacy

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import com.ritesh.cashiro.presentation.ui.components.CashiroCheckbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import com.ritesh.cashiro.R
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ritesh.cashiro.data.backup.BackupConfiguration
import com.ritesh.cashiro.presentation.effects.overScrollVertical
import com.ritesh.cashiro.presentation.ui.components.CustomTitleTopAppBar
import com.ritesh.cashiro.presentation.ui.components.ListItem
import com.ritesh.cashiro.presentation.ui.components.ListItemPosition
import com.ritesh.cashiro.presentation.ui.components.PreferenceSwitch
import com.ritesh.cashiro.presentation.ui.components.SectionHeader
import com.ritesh.cashiro.presentation.ui.components.toShape
import com.ritesh.cashiro.presentation.ui.features.categories.NavigationContent
import com.ritesh.cashiro.presentation.ui.features.settings.applock.AppLockViewModel
import com.ritesh.cashiro.presentation.ui.icons.Iconax
import com.ritesh.cashiro.presentation.ui.icons.Padlock
import com.ritesh.cashiro.presentation.ui.theme.Dimensions
import com.ritesh.cashiro.presentation.ui.theme.Spacing
import com.ritesh.cashiro.presentation.ui.theme.green_dark
import com.ritesh.cashiro.presentation.ui.theme.green_light
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeEffectScope
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalHazeApi::class
)
@Composable
fun DataPrivacyScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAccounts: () -> Unit = {},
    appLockViewModel: AppLockViewModel = hiltViewModel(),
    viewModel: DataPrivacyViewModel = hiltViewModel(),
    blurEffects: Boolean
) {
    val appLockUiState by appLockViewModel.uiState.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val hazeState = remember { HazeState() }

    var showTimeoutDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

    // Launcher for selecting a backup file to import
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let { viewModel.importBackup(it) }
        }
    )

    // Launcher for selecting a PDF statement to import
    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let { viewModel.analyzePdfStatement(it) }
        }
    )

    // Launcher for saving the exported backup file
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
        onResult = { uri ->
            if (uri != null) {
                viewModel.saveBackupToFile(uri)
            } else {
                viewModel.clearExportedFile()
            }
        }
    )

    // Handle import/export messages
    LaunchedEffect(uiState.importExportMessage) {
        uiState.importExportMessage?.let {
            val result = snackbarHostState.showSnackbar(
                message = it,
                actionLabel = if (uiState.hasNewAccountsCreated) "View Accounts" else null
            )
            if (result == SnackbarResult.ActionPerformed) {
                onNavigateToAccounts()
            }
            viewModel.clearImportExportMessage()
        }
    }

    // Handle export success (trigger file saver)
    LaunchedEffect(uiState.exportedBackupFile) {
        uiState.exportedBackupFile?.let {
            exportLauncher.launch("cashiro_backup_${System.currentTimeMillis()}.zip")
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CustomTitleTopAppBar(
                title = stringResource(R.string.data_privacy_title),
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehavior,
                hazeState = hazeState,
                hasBackButton = true,
                navigationContent = { NavigationContent { onNavigateBack() } }
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = {
                    Snackbar(
                        snackbarData = it,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.large,
                    )
                }
            ) }
    ) { paddingValues ->
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(state = hazeState)
                    .overScrollVertical()
                    .verticalScroll(rememberScrollState())
                    .padding(top = paddingValues.calculateTopPadding())
                    .padding(horizontal = Dimensions.Padding.content),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                // Security Section
                SectionHeader(
                    title = stringResource(R.string.security_section),
                    modifier = Modifier.padding(start = Spacing.md, top = Spacing.md))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    PreferenceSwitch(
                        title = stringResource(R.string.app_lock),
                        subtitle =
                        if (appLockUiState.canUseBiometric) {
                            stringResource(R.string.app_lock_biometric_sub)
                        } else {
                            appLockUiState.biometricCapability.getErrorMessage()
                        },
                        checked = appLockUiState.isLockEnabled,
                        onCheckedChange = { enabled ->
                            appLockViewModel.setAppLockEnabled(enabled)
                        },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        color = green_light,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Iconax.Padlock,
                                    contentDescription = null,
                                    tint = green_dark
                                )
                            }
                        },
                        padding = PaddingValues(0.dp),
                        isSingle = !appLockUiState.isLockEnabled,
                        isFirst = true,
                    )

                    // Lock Timeout Setting
                    AnimatedVisibility(visible = appLockUiState.isLockEnabled) {
                        ListItem(
                            headline = { Text(stringResource(R.string.lock_timeout)) },
                            supporting = {
                                Text(
                                    when (appLockUiState.timeoutMinutes) {
                                        0 -> stringResource(R.string.lock_timeout_immediate)
                                        1 -> stringResource(R.string.lock_timeout_1min)
                                        else -> stringResource(R.string.lock_timeout_minutes, appLockUiState.timeoutMinutes)
                                    }
                                )
                            },
                            trailing = {
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            onClick = { showTimeoutDialog = true },
                            shape = ListItemPosition.Bottom.toShape(),
                            padding = PaddingValues(0.dp),
                        )
                    }
                }

                // Data Management Section
                SectionHeader(
                    title = stringResource(R.string.data_management_section),
                    modifier = Modifier.padding(start = Spacing.md, top = Spacing.md)
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    ListItem(
                        headline = { Text(stringResource(R.string.export_data)) },
                        supporting = { Text(stringResource(R.string.export_data_sub)) },
                        onClick = { showExportDialog = true },
                        shape = ListItemPosition.Top.toShape(),
                        padding = PaddingValues(0.dp)
                    )
                    ListItem(
                        headline = { Text(stringResource(R.string.import_data)) },
                        supporting = { Text(stringResource(R.string.import_data_sub)) },
                        onClick = { importLauncher.launch(arrayOf("application/json")) },
                        shape = ListItemPosition.Middle.toShape(),
                        padding = PaddingValues(0.dp)
                    )
                    ListItem(
                        headline = { Text(stringResource(R.string.import_pdf_statement)) },
                        supporting = { Text(stringResource(R.string.import_pdf_statement_sub)) },
                        trailing = {
                            Icon(
                                Icons.Rounded.PictureAsPdf,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = { pdfLauncher.launch(arrayOf("application/pdf")) },
                        shape = ListItemPosition.Bottom.toShape(),
                        padding = PaddingValues(0.dp)
                    )
                }

                // Add bottom spacing
                Spacer(modifier = Modifier.size(Spacing.xl))
            }
        }
    }

    // Timeout Dialog
    if (showTimeoutDialog) {
        val options = listOf(0, 1, 5, 15, 30)
        val containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        AlertDialog(
            onDismissRequest = { showTimeoutDialog = false },
            title = { Text(stringResource(R.string.lock_timeout)) },
            text = {
                Column {
                    options.forEach { minutes ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .selectable(
                                    selected = appLockUiState.timeoutMinutes == minutes,
                                    onClick = {
                                        appLockViewModel.setTimeoutMinutes(minutes)
                                        showTimeoutDialog = false
                                    }
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = appLockUiState.timeoutMinutes == minutes,
                                onClick = null
                            )
                            Text(
                                text = when (minutes) {
                                    0 -> stringResource(R.string.immediately)
                                    1 -> stringResource(R.string.one_minute)
                                    else -> stringResource(R.string.minutes_format, minutes)
                                },
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showTimeoutDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(0.5f),
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier
                        .padding(horizontal = Spacing.xl)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            },
            containerColor = if (blurEffects)
                MaterialTheme.colorScheme.surfaceContainerLow.copy(0.5f)
            else MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .then(
                    if (blurEffects) Modifier.hazeEffect(
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
        )
    }

    // Export Dialog
    if (showExportDialog) {
        ExportOptionsDialog(
            onDismiss = { showExportDialog = false },
            onConfirm = { config ->
                viewModel.exportBackup(config)
                showExportDialog = false
            },
            blurEffects = blurEffects,
            hazeState = hazeState
        )
    }

    // PDF Processing / Error dialog
    if (uiState.isPdfProcessing || uiState.pdfProcessingError != null) {
        PdfProcessingDialog(
            isVisible = uiState.isPdfProcessing,
            error = uiState.pdfProcessingError,
            onDismissError = { viewModel.dismissPdfImport() },
            blurEffects = blurEffects,
            hazeState = hazeState
        )
    }

    // PDF Import Review BottomSheet (Unified review of accounts and transactions)
    uiState.pdfAnalysisResult?.let { result ->
        PdfImportSheet(
            analysisResult = result,
            availableAccounts = uiState.availableAccounts,
            onConfirm = { transactionDecisions, accountDecisions, accountMappings, shouldUpdateBalances ->
                viewModel.confirmPdfImport(
                    accountDecisions = accountDecisions,
                    accountMappings = accountMappings,
                    transactionDecisions = transactionDecisions,
                    shouldUpdateBalances = shouldUpdateBalances
                )
            },
            onDismiss = { viewModel.dismissPdfImport() }
        )
    }
}

@OptIn(ExperimentalHazeApi::class)
@Composable
fun ExportOptionsDialog(
    onDismiss: () -> Unit,
    onConfirm: (BackupConfiguration) -> Unit,
    blurEffects: Boolean ,
    hazeState: HazeState = remember { HazeState() }
) {
    var includeTransactional by remember { mutableStateOf(true) }
    var includeProfile by remember { mutableStateOf(true) }
    var includeBudgets by remember { mutableStateOf(true) }
    var includePreferences by remember { mutableStateOf(true) }
    val containerColor = MaterialTheme.colorScheme.surfaceContainerLow

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.export_data)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    stringResource(R.string.select_data_to_backup),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ExportCheckbox(stringResource(R.string.transactional_data), includeTransactional) { includeTransactional = it }
                ExportCheckbox(stringResource(R.string.profile_data), includeProfile) { includeProfile = it }
                ExportCheckbox(stringResource(R.string.budgets), includeBudgets) { includeBudgets = it }
                ExportCheckbox(stringResource(R.string.app_preferences), includePreferences) { includePreferences = it }
            }
        },
        confirmButton = {
            Box(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Button(
                        onClick = onDismiss,
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
                            .padding(start = Spacing.xl)
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.cancel),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Button(
                        onClick = {
                            onConfirm(
                            BackupConfiguration(
                                includeTransactionalData = includeTransactional,
                                includeProfileData = includeProfile,
                                includeBudgets = includeBudgets,
                                includeAppPreferences = includePreferences
                            ))},
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
                            .padding(end = Spacing.xl)
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                            Text(
                                text = stringResource(R.string.export),
                                style = MaterialTheme.typography.titleMedium
                            )

                    }

                }
            }
        },
        containerColor = if (blurEffects)
            MaterialTheme.colorScheme.surfaceContainerLow.copy(0.5f)
        else MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (blurEffects) Modifier.hazeEffect(
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
        dismissButton = {},

    )
}

@Composable
fun ExportCheckbox(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs)
            .toggleable(
                value = checked,
                onValueChange = onCheckedChange
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CashiroCheckbox(checked = checked, onCheckedChange = null)
        Text(
            text = text,
            modifier = Modifier.padding(start = Spacing.sm),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
