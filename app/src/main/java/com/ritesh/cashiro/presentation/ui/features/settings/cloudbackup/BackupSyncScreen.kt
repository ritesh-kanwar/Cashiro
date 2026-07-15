package com.ritesh.cashiro.presentation.ui.features.settings.cloudbackup

import android.app.Activity
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.ritesh.cashiro.R
import com.ritesh.cashiro.data.backup.BackupConfiguration
import com.ritesh.cashiro.data.cloud.BackupSchedule
import com.ritesh.cashiro.data.cloud.CloudFileInfo
import com.ritesh.cashiro.data.cloud.CloudProviderType
import com.ritesh.cashiro.data.cloud.SyncStatus
import com.ritesh.cashiro.presentation.effects.overScrollVertical
import com.ritesh.cashiro.presentation.ui.components.CashiroCheckbox
import com.ritesh.cashiro.presentation.ui.components.CustomTitleTopAppBar
import com.ritesh.cashiro.presentation.ui.components.DeleteCloudSnapshotDialog
import com.ritesh.cashiro.presentation.ui.components.GenericTypeSwitcher
import com.ritesh.cashiro.presentation.ui.components.ListItem
import com.ritesh.cashiro.presentation.ui.components.ListItemPosition
import com.ritesh.cashiro.presentation.ui.components.PreferenceSwitch
import com.ritesh.cashiro.presentation.ui.components.SectionHeader
import com.ritesh.cashiro.presentation.ui.components.toShape
import com.ritesh.cashiro.presentation.ui.features.categories.NavigationContent
import com.ritesh.cashiro.presentation.ui.features.settings.dataprivacy.DataPrivacyViewModel
import com.ritesh.cashiro.presentation.ui.features.settings.dataprivacy.PdfImportSheet
import com.ritesh.cashiro.presentation.ui.features.settings.dataprivacy.PdfProcessingDialog
import com.ritesh.cashiro.presentation.ui.icons.Bag
import com.ritesh.cashiro.presentation.ui.icons.DirectboxReceive
import com.ritesh.cashiro.presentation.ui.icons.DirectboxSend
import com.ritesh.cashiro.presentation.ui.icons.Folder2
import com.ritesh.cashiro.presentation.ui.icons.Iconax
import com.ritesh.cashiro.presentation.ui.icons.Information
import com.ritesh.cashiro.presentation.ui.theme.Dimensions
import com.ritesh.cashiro.presentation.ui.theme.Spacing
import com.ritesh.cashiro.presentation.ui.theme.blue_dark
import com.ritesh.cashiro.presentation.ui.theme.blue_light
import com.ritesh.cashiro.presentation.ui.theme.green_dark
import com.ritesh.cashiro.presentation.ui.theme.green_light
import com.ritesh.cashiro.presentation.ui.theme.orange_dark
import com.ritesh.cashiro.presentation.ui.theme.orange_light
import com.ritesh.cashiro.presentation.ui.theme.yellow_dark
import com.ritesh.cashiro.presentation.ui.theme.yellow_light
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeEffectScope
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalHazeApi::class)
@Composable
fun BackupSyncScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAccounts: () -> Unit = {},
    viewModel: BackupSyncViewModel = hiltViewModel(),
    dataPrivacyViewModel: DataPrivacyViewModel = hiltViewModel(),
    blurEffects: Boolean
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dataPrivacyUiState by dataPrivacyViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val hazeState = remember { HazeState() }

    var webDavUrl by remember(uiState.webDavConfig.url) { mutableStateOf(uiState.webDavConfig.url) }
    var webDavUser by remember(uiState.webDavConfig.username) { mutableStateOf(uiState.webDavConfig.username) }
    var webDavPass by remember(uiState.webDavConfig.passwordOrToken) { mutableStateOf(uiState.webDavConfig.passwordOrToken) }

    var selectedTab by remember { mutableIntStateOf(0) }
    var showScheduleDialog by remember { mutableStateOf(false) }
    var showRetentionDialog by remember { mutableStateOf(false) }
    var showE2eDialog by remember { mutableStateOf(false) }
    var e2ePassphraseInput by remember { mutableStateOf("") }
    var restorePassphraseInput by remember { mutableStateOf("") }
    var snapshotToDelete by remember { mutableStateOf<CloudFileInfo?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }

    // File launchers for Data Management
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
        onResult = { uri -> 
            uri?.let { dataPrivacyViewModel.saveBackupToFile(it) }
            if (uri == null) dataPrivacyViewModel.clearExportedFile()
        }
    )

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri -> uri?.let { dataPrivacyViewModel.importBackup(it) } }
    )

    val pdfImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri -> uri?.let { dataPrivacyViewModel.analyzePdfStatement(it) } }
    )

    // Handle export completion
    LaunchedEffect(dataPrivacyUiState.exportedBackupFile) {
        dataPrivacyUiState.exportedBackupFile?.let { file ->
            exportLauncher.launch(file.name)
        }
    }

    // Handle data management messages
    LaunchedEffect(dataPrivacyUiState.importExportMessage) {
        dataPrivacyUiState.importExportMessage?.let { message ->
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = message,
                    actionLabel = if (dataPrivacyUiState.hasNewAccountsCreated) context.getString(R.string.review) else null,
                    withDismissAction = true
                )
                if (result == SnackbarResult.ActionPerformed) {
                    onNavigateToAccounts()
                }
                dataPrivacyViewModel.clearImportExportMessage()
            }
        }
    }

    // Setup Google Sign-In Client for Google Drive AppData
    val gDriveSignInClient = remember(context) {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope("https://www.googleapis.com/auth/drive.appdata"))
            .build()
        GoogleSignIn.getClient(context, options)
    }

    val gDriveSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                viewModel.handleGoogleSignInResult(account)
            } catch (e: ApiException) {
                Log.e("BackupSyncScreen", "Google Sign-In failed code=${e.statusCode}", e)
                scope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.signin_failed_code_format, e.statusCode))
                }
            }
        }
    }

    val recoverableAuthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // User granted permission, try sign-in again
            val account = GoogleSignIn.getLastSignedInAccount(context)
            viewModel.handleGoogleSignInResult(account)
        }
        viewModel.clearRecoverableAuthIntent()
    }

    LaunchedEffect(uiState.recoverableAuthIntent) {
        uiState.recoverableAuthIntent?.let { intent ->
            recoverableAuthLauncher.launch(intent)
        }
    }

    // Handle sync status snackbars
    LaunchedEffect(uiState.syncStatus) {
        when (val status = uiState.syncStatus) {
            is SyncStatus.Success -> {
                scope.launch {
                    snackbarHostState.showSnackbar(status.message)
                    viewModel.dismissSyncStatus()
                }
            }
            is SyncStatus.Error -> {
                scope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.error_format, status.message))
                    viewModel.dismissSyncStatus()
                }
            }
            else -> {}
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CustomTitleTopAppBar(
                title = stringResource(R.string.backup_sync_title),
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehavior,
                hazeState = hazeState,
                hasBackButton = true,
                navigationContent = { NavigationContent { onNavigateBack() } }
            )
        },
        snackbarHost = {
            Column(
                modifier = Modifier
                    .padding(horizontal = Dimensions.Padding.content)
                    .navigationBarsPadding()
                    .padding(bottom = Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                // Active Operation Progress Bar
                AnimatedVisibility(
                    visible = uiState.syncStatus is SyncStatus.Syncing ||
                            uiState.syncStatus is SyncStatus.BackingUp ||
                            uiState.syncStatus is SyncStatus.Restoring,
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it }
                ) {
                    when (val status = uiState.syncStatus) {
                        is SyncStatus.Syncing -> OperationProgressCard(status.message)
                        is SyncStatus.BackingUp -> OperationProgressCard(status.message, status.progress)
                        is SyncStatus.Restoring -> OperationProgressCard(status.message, status.progress)
                        else -> {}
                    }
                }

                SnackbarHost(hostState = snackbarHostState) {
                    Snackbar(
                        snackbarData = it,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.large
                    )
                }
            }
        }
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
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(top = paddingValues.calculateTopPadding())
                    .padding(horizontal = Dimensions.Padding.content),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {

                // Backup / Restore Switcher
                GenericTypeSwitcher(
                    selectedIndex = selectedTab,
                    onIndexChange = { selectedTab = it },
                    options = listOf(
                        stringResource(R.string.backup_tab),
                        stringResource(R.string.restore)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.sm)
                )

                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        slideInVertically { height -> height } + fadeIn() togetherWith
                        slideOutVertically { height -> -height } + fadeOut()
                    },
                    contentAlignment = Alignment.TopStart,
                    label = "backupRestoreContent"
                ) { tab ->
                    if (tab == 0) {
                        // BACKUP TAB
                        Column(
                            verticalArrangement = Arrangement.spacedBy(Spacing.md),
                            modifier = Modifier.fillMaxWidth()
                        ) {

                            // Cloud Provider Selection Section
                            SectionHeader(
                                title = stringResource(R.string.storage_provider),
                                modifier = Modifier.padding(start = Spacing.md, top = Spacing.md)
                            )
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(1.5.dp)
                            ) {
                                ProviderOptionItem(
                                    title = CloudProviderType.LOCAL_ONLY.getLocalizedDisplayName(),
                                    subtitle = stringResource(R.string.store_backups_local_desc),
                                    providerType = CloudProviderType.LOCAL_ONLY,
                                    isSelected = uiState.activeProviderType == CloudProviderType.LOCAL_ONLY,
                                    position = ListItemPosition.Top,
                                    onClick = { viewModel.setActiveProviderType(CloudProviderType.LOCAL_ONLY) }
                                )
                                ProviderOptionItem(
                                    title = CloudProviderType.WEBDAV.getLocalizedDisplayName(),
                                    subtitle = stringResource(R.string.webdav_provider_desc),
                                    providerType = CloudProviderType.WEBDAV,
                                    isSelected = uiState.activeProviderType == CloudProviderType.WEBDAV,
                                    position = ListItemPosition.Middle,
                                    onClick = { viewModel.setActiveProviderType(CloudProviderType.WEBDAV) }
                                )
                                ProviderOptionItem(
                                    title = CloudProviderType.GOOGLE_DRIVE.getLocalizedDisplayName(),
                                    subtitle = stringResource(R.string.google_drive_provider_desc),
                                    providerType = CloudProviderType.GOOGLE_DRIVE,
                                    isSelected = uiState.activeProviderType == CloudProviderType.GOOGLE_DRIVE,
                                    position = ListItemPosition.Bottom,
                                    onClick = { viewModel.setActiveProviderType(CloudProviderType.GOOGLE_DRIVE) }
                                )
                            }

                            // WebDAV Configuration Card
                            AnimatedVisibility(visible = uiState.activeProviderType == CloudProviderType.WEBDAV) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    shape = MaterialTheme.shapes.large
                                ) {
                                    Column(
                                        modifier = Modifier.padding(Spacing.md),
                                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.webdav_credentials_title),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        TextField(
                                            value = webDavUrl,
                                            onValueChange = { webDavUrl = it },
                                            label = { Text(stringResource(R.string.server_url_hint)) },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                            shape = RoundedCornerShape(16.dp),
                                            colors = TextFieldDefaults.colors(
                                                focusedContainerColor = MaterialTheme.colorScheme.surface.copy(
                                                    0.5f
                                                ),
                                                unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(
                                                    0.5f
                                                ),
                                                focusedIndicatorColor = Color.Transparent,
                                                unfocusedIndicatorColor = Color.Transparent,
                                                focusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(
                                                    0.5f
                                                ),
                                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(
                                                    0.5f
                                                ),
                                            ),
                                        )
                                        TextField(
                                            value = webDavUser,
                                            onValueChange = { webDavUser = it },
                                            label = { Text(stringResource(R.string.username)) },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                            shape = RoundedCornerShape(16.dp),
                                            colors = TextFieldDefaults.colors(
                                                focusedContainerColor = MaterialTheme.colorScheme.surface.copy(
                                                    0.5f
                                                ),
                                                unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(
                                                    0.5f
                                                ),
                                                focusedIndicatorColor = Color.Transparent,
                                                unfocusedIndicatorColor = Color.Transparent,
                                                focusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(
                                                    0.5f
                                                ),
                                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(
                                                    0.5f
                                                ),
                                            ),
                                        )
                                        TextField(
                                            value = webDavPass,
                                            onValueChange = { webDavPass = it },
                                            label = { Text(stringResource(R.string.password_or_token)) },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                            visualTransformation = PasswordVisualTransformation(),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = TextFieldDefaults.colors(
                                                focusedContainerColor = MaterialTheme.colorScheme.surface.copy(
                                                    0.5f
                                                ),
                                                unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(
                                                    0.5f
                                                ),
                                                focusedIndicatorColor = Color.Transparent,
                                                unfocusedIndicatorColor = Color.Transparent,
                                                focusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(
                                                    0.5f
                                                ),
                                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(
                                                    0.5f
                                                ),
                                            ),
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Button(
                                                onClick = {
                                                    viewModel.testConnection(
                                                        CloudProviderType.WEBDAV
                                                    )
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                                ),
                                                modifier = Modifier.weight(1f),
                                            ) {
                                                Text(stringResource(R.string.test_connection))
                                            }
                                            Button(
                                                onClick = {
                                                    viewModel.updateWebDavConfig(
                                                        webDavUrl,
                                                        webDavUser,
                                                        webDavPass,
                                                        true
                                                    )
                                                },
                                                modifier = Modifier.weight(1f),
                                            ) {
                                                Text(stringResource(R.string.save_configuration))
                                            }
                                        }
                                    }
                                }
                            }

                            // Google Drive Configuration Card
                            AnimatedVisibility(visible = uiState.activeProviderType == CloudProviderType.GOOGLE_DRIVE) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    shape = MaterialTheme.shapes.large
                                ) {
                                    Column {
                                        Text(
                                            text = stringResource(R.string.google_drive_configuration),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(Spacing.md,),
                                        )

                                        if (uiState.isGoogleDriveSignedIn) {
                                            ListItem(
                                                headline = { Text(uiState.googleDriveConfig.accountEmail) },
                                                supporting = { Text(stringResource(R.string.connected_to_google_drive)) },
                                                leading = {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(48.dp)
                                                            .background(green_light, CircleShape),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            Icons.Default.Email,
                                                            contentDescription = null,
                                                            tint = green_dark
                                                        )
                                                    }
                                                },
                                                padding = PaddingValues(0.dp)
                                            )
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                                                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                                            ) {
                                                Button(
                                                    onClick = {
                                                        gDriveSignInClient.signOut()
                                                        viewModel.onGoogleDriveSignOut()
                                                    },
                                                    modifier = Modifier.weight(1f),
                                                ) {
                                                    Text(stringResource(R.string.sign_out))
                                                }
                                                Button(
                                                    onClick = {
                                                        viewModel.testConnection(
                                                            CloudProviderType.GOOGLE_DRIVE
                                                        )
                                                    },
                                                    modifier = Modifier.weight(1f),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                                    ),
                                                ) {
                                                    Text(stringResource(R.string.test_connection))
                                                }
                                            }
                                        } else {
                                            Text(
                                                text = stringResource(R.string.google_drive_signin_desc),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
                                            )
                                            Spacer(modifier = Modifier.size(Spacing.sm))
                                            Button(
                                                onClick = {
                                                    gDriveSignInLauncher.launch(gDriveSignInClient.signInIntent)
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = Spacing.md)
                                                    .padding(bottom = Spacing.md)
                                            ) {
                                                Icon(
                                                    Icons.Default.Email,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.size(8.dp))
                                                Text(stringResource(R.string.sign_in_with_google))
                                            }
                                        }
                                    }
                                }
                            }

                            if (uiState.activeProviderType == CloudProviderType.LOCAL_ONLY) {
                                SectionHeader(
                                    title = stringResource(R.string.local_backup_section),
                                    modifier = Modifier.padding(start = Spacing.md)
                                )
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(1.5.dp)
                                ) {
                                    // Export Data
                                    ListItem(
                                        headline = { Text(stringResource(R.string.export_data)) },
                                        supporting = { Text(stringResource(R.string.export_data_sub)) },
                                        leading = {
                                            Box(
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .background(yellow_light, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Iconax.DirectboxSend,
                                                    contentDescription = null,
                                                    tint = yellow_dark
                                                )
                                            }
                                        },
                                        trailing = {
                                            Icon(
                                                Icons.Rounded.ChevronRight,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        },
                                        onClick = { showExportDialog = true },
                                        shape = ListItemPosition.Single.toShape(),
                                        padding = PaddingValues(0.dp)
                                    )
                                }
                            } else {
                                // When Nextcloud/WebDAV or Google Drive is selected, show Encryption & Automation, Manual Actions, Cloud Snapshot sections
                                // E2E Security & Schedules
                                SectionHeader(
                                    title = stringResource(R.string.encryption_and_automation),
                                    modifier = Modifier.padding(start = Spacing.md)
                                )
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(1.5.dp)
                                ) {
                                    PreferenceSwitch(
                                        title = stringResource(R.string.e2e_encryption),
                                        subtitle = if (uiState.isE2eEnabled) stringResource(R.string.e2e_enabled_desc) else stringResource(
                                            R.string.e2e_disabled_desc
                                        ),
                                        checked = uiState.isE2eEnabled,
                                        onCheckedChange = { enabled ->
                                            if (enabled) {
                                                e2ePassphraseInput = uiState.e2ePassphrase
                                                showE2eDialog = true
                                            } else {
                                                viewModel.setE2eEncryption(false, "")
                                            }
                                        },
                                        leadingIcon = {
                                            Box(
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .background(green_light, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.Security,
                                                    contentDescription = null,
                                                    tint = green_dark
                                                )
                                            }
                                        },
                                        padding = PaddingValues(0.dp),
                                        isFirst = true
                                    )

                                    ListItem(
                                        headline = { Text(stringResource(R.string.automatic_backup_schedule)) },
                                        supporting = { Text(uiState.backupSchedule.getLocalizedDisplayName()) },
                                        leading = {
                                            Box(
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .background(yellow_light, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.CloudSync,
                                                    contentDescription = null,
                                                    tint = yellow_dark
                                                )
                                            }
                                        },
                                        trailing = {
                                            Icon(
                                                Icons.Default.ChevronRight,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        },
                                        onClick = { showScheduleDialog = true },
                                        shape = ListItemPosition.Middle.toShape(),
                                        padding = PaddingValues(0.dp)
                                    )

                                    ListItem(
                                        headline = { Text(stringResource(R.string.backup_retention_limit)) },
                                        supporting = {
                                            Text(
                                                stringResource(
                                                    R.string.keep_snapshots_format,
                                                    uiState.retentionLimit
                                                )
                                            )
                                        },
                                        leading = {
                                            Box(
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .background(orange_light, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.Storage,
                                                    contentDescription = null,
                                                    tint = orange_dark
                                                )
                                            }
                                        },
                                        trailing = {
                                            Icon(
                                                Icons.Default.ChevronRight,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        },
                                        onClick = { showRetentionDialog = true },
                                        shape = ListItemPosition.Bottom.toShape(),
                                        padding = PaddingValues(0.dp)
                                    )
                                }

                                // Manual Operations
                                SectionHeader(
                                    title = stringResource(R.string.manual_actions),
                                    modifier = Modifier.padding(start = Spacing.md)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                                ) {
                                    Button(
                                        onClick = { viewModel.performManualBackup() },
                                        modifier = Modifier.weight(1f),
                                        enabled = uiState.syncStatus is SyncStatus.Idle
                                    ) {
                                        Icon(
                                            Icons.Default.Cloud,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.size(8.dp))
                                        Text(stringResource(R.string.create_backup))
                                    }
                                    Button(
                                        onClick = { viewModel.performManualSync() },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        ),
                                        enabled = uiState.syncStatus is SyncStatus.Idle
                                    ) {
                                        Icon(
                                            Icons.Default.CloudDone,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.size(8.dp))
                                        Text(stringResource(R.string.sync_devices))
                                    }
                                }

                                // Remote Cloud Snapshots Section
                                SectionHeader(
                                    title = stringResource(R.string.cloud_snapshots),
                                    modifier = Modifier.padding(start = Spacing.md)
                                )
                                if (uiState.isLoadingSnapshots) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(Spacing.lg),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                } else if (uiState.remoteSnapshots.isEmpty()) {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                                        shape = MaterialTheme.shapes.large
                                    ) {
                                        Text(
                                            text = stringResource(R.string.no_cloud_snapshots),
                                            modifier = Modifier.padding(Spacing.md),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(1.5.dp)
                                    ) {
                                        uiState.remoteSnapshots.forEachIndexed { index, snapshot ->
                                            val pos = when {
                                                uiState.remoteSnapshots.size == 1 -> ListItemPosition.Single
                                                index == 0 -> ListItemPosition.Top
                                                index == uiState.remoteSnapshots.size - 1 -> ListItemPosition.Bottom
                                                else -> ListItemPosition.Middle
                                            }
                                            SnapshotListItem(
                                                snapshot = snapshot,
                                                position = pos,
                                                onRestore = { viewModel.restoreSnapshot(snapshot) },
                                                onDelete = { snapshotToDelete = snapshot }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (tab == 1) {
                    // RESTORE TAB
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(1.5.dp)
                    ) {
                        // Import Data
                        ListItem(
                            headline = { Text(stringResource(R.string.import_data)) },
                            supporting = { Text(stringResource(R.string.import_data_sub)) },
                            leading = {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(green_light, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Iconax.DirectboxReceive,
                                        contentDescription = null,
                                        tint = green_dark
                                    )
                                }
                            },
                            trailing = {
                                Icon(
                                    Icons.Rounded.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            onClick = { importLauncher.launch("*/*") },
                            shape = ListItemPosition.Top.toShape(),
                            padding = PaddingValues(0.dp)
                        )

                        // Import PDF Statement
                        ListItem(
                            headline = { Text(stringResource(R.string.import_pdf_statement)) },
                            supporting = { Text(stringResource(R.string.import_pdf_statement_sub)) },
                            leading = {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(orange_light, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PictureAsPdf,
                                        contentDescription = null,
                                        tint = orange_dark
                                    )
                                }
                            },
                            trailing = {
                                Icon(
                                    Icons.Rounded.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            onClick = { pdfImportLauncher.launch("application/pdf") },
                            shape = ListItemPosition.Bottom.toShape(),
                            padding = PaddingValues(0.dp)
                        )

                        // PDF Support Warning
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = Spacing.sm),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                            shape = MaterialTheme.shapes.large,
                        ) {
                            Row(
                                modifier = Modifier.padding(Spacing.md),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                            ) {
                                Icon(
                                    imageVector = Iconax.Information,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                                ) {
                                    Text(
                                        text = stringResource(R.string.pdf_support_warning_title),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                                    )
                                    Text(
                                        text = stringResource(R.string.pdf_support_warning_sub),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer.copy(0.8f)
                                    )
                                }
                            }
                        }
                    }
                    }
                }

                Spacer(modifier = Modifier.size(Spacing.xl))
            }
        }
    }

    // Schedule Selection Dialog
    if (showScheduleDialog) {
        val schedules = BackupSchedule.values()
        val containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        AlertDialog(
            onDismissRequest = { showScheduleDialog = false },
            title = { Text(stringResource(R.string.backup_schedule)) },
            text = {
                Column {
                    schedules.forEach { schedule ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .selectable(
                                    selected = uiState.backupSchedule == schedule,
                                    onClick = {
                                        viewModel.setBackupSchedule(schedule)
                                        showScheduleDialog = false
                                    }
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = uiState.backupSchedule == schedule, onClick = null)
                            Text(text = schedule.getLocalizedDisplayName(), modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showScheduleDialog = false },
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

    // Retention Selection Dialog
    if (showRetentionDialog) {
        val limits = listOf(5, 10, 20, 50)
        val containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        AlertDialog(
            onDismissRequest = { showRetentionDialog = false },
            title = { Text(stringResource(R.string.retention_limit)) },
            text = {
                Column {
                    limits.forEach { limit ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .selectable(
                                    selected = uiState.retentionLimit == limit,
                                    onClick = {
                                        viewModel.setRetentionLimit(limit)
                                        showRetentionDialog = false
                                    }
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = uiState.retentionLimit == limit, onClick = null)
                            Text(text = stringResource(R.string.keep_backups_format, limit), modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showRetentionDialog = false },
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

    // E2E Passphrase Dialog
    if (showE2eDialog) {
        val containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        AlertDialog(
            onDismissRequest = { showE2eDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text(stringResource(R.string.e2e_encryption)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.enter_passphrase_desc),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    TextField(
                        value = e2ePassphraseInput,
                        onValueChange = { e2ePassphraseInput = it },
                        label = { Text(stringResource(R.string.passphrase)) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface.copy(0.5f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(0.5f),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(0.5f),
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(0.5f),
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(1.5.dp),
                    ) {
                        Button(
                            onClick = { showE2eDialog = false },
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
                                if (e2ePassphraseInput.isNotBlank()) {
                                    viewModel.setE2eEncryption(true, e2ePassphraseInput)
                                    showE2eDialog = false
                                }
                            },
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
                                text = stringResource(R.string.enable_and_save),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            },
            containerColor = if (blurEffects) containerColor.copy(0.5f) else containerColor,
            dismissButton = {},
            modifier = Modifier
                .clip(RoundedCornerShape(Dimensions.Radius.md))
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
            shape = MaterialTheme.shapes.large
        )
    }

    // Restore E2E Passphrase Dialog
    if (uiState.showRestorePassphraseDialog) {
        val containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        AlertDialog(
            onDismissRequest = { viewModel.dismissRestorePassphraseDialog() },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Restore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text(stringResource(R.string.encrypted_backup_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.restore_passphrase_desc),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    TextField(
                        value = restorePassphraseInput,
                        onValueChange = { restorePassphraseInput = it },
                        label = { Text(stringResource(R.string.passphrase)) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface.copy(0.5f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(0.5f),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(0.5f),
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(0.5f),
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(1.5.dp),
                    ) {
                        Button(
                            onClick = {
                                viewModel.dismissRestorePassphraseDialog()
                                restorePassphraseInput = ""
                            },
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
                                if (restorePassphraseInput.isNotBlank()) {
                                    viewModel.retryRestoreWithPassphrase(restorePassphraseInput)
                                }
                            },
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
                                text = stringResource(R.string.restore),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            },
            containerColor = if (blurEffects) containerColor.copy(0.5f) else containerColor,
            dismissButton = {},
            modifier = Modifier
                .clip(RoundedCornerShape(Dimensions.Radius.md))
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
            shape = MaterialTheme.shapes.large
        )
    }

    // Delete Cloud Snapshot Confirmation Dialog
    if (snapshotToDelete != null) {
        val snapshot = snapshotToDelete!!
        DeleteCloudSnapshotDialog(
            snapshotName = snapshot.name,
            onDismiss = { snapshotToDelete = null },
            onDelete = {
                viewModel.deleteSnapshot(snapshot)
                snapshotToDelete = null
            },
            blurEffects = blurEffects,
            hazeState = hazeState
        )
    }

    // Export Options Dialog for Data Management
    if (showExportDialog) {
        ExportOptionsDialog(
            onDismiss = { showExportDialog = false },
            onConfirm = { config ->
                dataPrivacyViewModel.exportBackup(config)
                showExportDialog = false
            },
            blurEffects = blurEffects,
            hazeState = hazeState
        )
    }

    // PDF Processing / Error dialog
    if (dataPrivacyUiState.isPdfProcessing || dataPrivacyUiState.pdfProcessingError != null) {
        PdfProcessingDialog(
            isVisible = dataPrivacyUiState.isPdfProcessing,
            error = dataPrivacyUiState.pdfProcessingError,
            onDismissError = { dataPrivacyViewModel.dismissPdfImport() },
            blurEffects = blurEffects,
            hazeState = hazeState
        )
    }

    // PDF Import Review BottomSheet (Unified review of accounts and transactions)
    dataPrivacyUiState.pdfAnalysisResult?.let { result ->
        PdfImportSheet(
            analysisResult = result,
            availableAccounts = dataPrivacyUiState.availableAccounts,
            onConfirm = { transactionDecisions, accountDecisions, accountMappings, shouldUpdateBalances ->
                dataPrivacyViewModel.confirmPdfImport(
                    accountDecisions = accountDecisions,
                    accountMappings = accountMappings,
                    transactionDecisions = transactionDecisions,
                    shouldUpdateBalances = shouldUpdateBalances
                )
            },
            onDismiss = { dataPrivacyViewModel.dismissPdfImport() }
        )
    }
}

@Composable
fun OperationProgressCard(message: String, progress: Int? = null) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            if (progress != null) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun ProviderOptionItem(
    title: String,
    subtitle: String,
    providerType: CloudProviderType,
    isSelected: Boolean,
    position: ListItemPosition,
    onClick: () -> Unit
) {
    ListItem(
        headline = { Text(title, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
        supporting = { Text(subtitle) },
        leading = {
            when (providerType) {
                CloudProviderType.WEBDAV -> Icon(
                    painter = painterResource(R.drawable.ic_brand_nextcloud),
                    contentDescription = title,
                    modifier = Modifier.size(24.dp),
                    tint = Color.Unspecified
                )
                CloudProviderType.GOOGLE_DRIVE -> Icon(
                    painter = painterResource(R.drawable.ic_brand_google_drive),
                    contentDescription = title,
                    modifier = Modifier.size(24.dp),
                    tint = Color.Unspecified
                )
                CloudProviderType.LOCAL_ONLY -> Icon(
                    imageVector = Iconax.Folder2,
                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    contentDescription = title,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        selectedListColor = MaterialTheme.colorScheme.primaryContainer,
        selected = isSelected,
        onClick = onClick,
        shape = position.toShape(),
        padding = PaddingValues(0.dp)
    )
}

@Composable
fun SnapshotListItem(
    snapshot: CloudFileInfo,
    position: ListItemPosition,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }
    val timeStr = dateFormat.format(Date(snapshot.lastModified))
    val sizeMb = stringResource(R.string.size_mb_format, snapshot.size / (1024f * 1024f))

    ListItem(
        headline = { Text(snapshot.name) },
        supporting = { Text(stringResource(R.string.snapshot_subtitle_format, timeStr, sizeMb)) },
        trailing = {
            Row {
                IconButton(onClick = onRestore) {
                    Icon(Icons.Default.Restore, contentDescription = stringResource(R.string.restore_snapshot), tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Iconax.Bag, contentDescription = stringResource(R.string.delete_snapshot), tint = MaterialTheme.colorScheme.error)
                }
            }
        },
        onClick = {},
        shape = position.toShape(),
        padding = PaddingValues(0.dp)
    )
}

@OptIn(ExperimentalHazeApi::class)
@Composable
fun ExportOptionsDialog(
    onDismiss: () -> Unit,
    onConfirm: (BackupConfiguration) -> Unit,
    blurEffects: Boolean,
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
                    horizontalArrangement = Arrangement.spacedBy(1.5.dp),
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
            .padding(vertical = 4.dp)
            .toggleable(
                value = checked,
                onValueChange = onCheckedChange
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CashiroCheckbox(checked = checked, onCheckedChange = null)
        Text(
            text = text,
            modifier = Modifier.padding(start = 8.dp),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun CloudProviderType.getLocalizedDisplayName(): String {
    return when (this) {
        CloudProviderType.LOCAL_ONLY -> stringResource(R.string.provider_local_only)
        CloudProviderType.WEBDAV -> stringResource(R.string.provider_webdav)
        CloudProviderType.GOOGLE_DRIVE -> stringResource(R.string.provider_google_drive)
    }
}

@Composable
private fun BackupSchedule.getLocalizedDisplayName(): String {
    return when (this) {
        BackupSchedule.MANUAL -> stringResource(R.string.schedule_manual)
        BackupSchedule.DAILY -> stringResource(R.string.schedule_daily)
        BackupSchedule.WEEKLY -> stringResource(R.string.schedule_weekly)
        BackupSchedule.MONTHLY -> stringResource(R.string.schedule_monthly)
    }
}
