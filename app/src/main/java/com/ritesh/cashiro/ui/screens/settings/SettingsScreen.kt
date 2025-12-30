package com.ritesh.cashiro.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ritesh.cashiro.core.Constants
import com.ritesh.cashiro.presentation.categories.NavigationContent
import com.ritesh.cashiro.ui.components.CashiroCard
import com.ritesh.cashiro.ui.components.CustomTitleTopAppBar
import com.ritesh.cashiro.ui.components.ListItem
import com.ritesh.cashiro.ui.components.ListItemPosition
import com.ritesh.cashiro.ui.components.PreferenceSwitch
import com.ritesh.cashiro.ui.components.SectionHeader
import com.ritesh.cashiro.ui.components.listItemPadding
import com.ritesh.cashiro.ui.components.toShape
import com.ritesh.cashiro.ui.theme.Dimensions
import com.ritesh.cashiro.ui.theme.Spacing
import com.ritesh.cashiro.ui.viewmodel.ThemeViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import com.ritesh.cashiro.R
import androidx.core.net.toUri
import com.ritesh.cashiro.ui.effects.overScrollVertical

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeViewModel: ThemeViewModel,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit,
    onNavigateToCategories: () -> Unit = {},
    onNavigateToUnrecognizedSms: () -> Unit = {},
    onNavigateToManageAccounts: () -> Unit = {},
    onNavigateToFaq: () -> Unit = {},
    onNavigateToRules: () -> Unit = {},
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    appLockViewModel: com.ritesh.cashiro.ui.viewmodel.AppLockViewModel = hiltViewModel()
) {
    val themeUiState by themeViewModel.themeUiState.collectAsStateWithLifecycle()
    val appLockUiState by appLockViewModel.uiState.collectAsStateWithLifecycle()
    val downloadState by settingsViewModel.downloadState.collectAsStateWithLifecycle()
    val downloadProgress by settingsViewModel.downloadProgress.collectAsStateWithLifecycle()
    val downloadedMB by settingsViewModel.downloadedMB.collectAsStateWithLifecycle()
    val totalMB by settingsViewModel.totalMB.collectAsStateWithLifecycle()
    val isDeveloperModeEnabled by
            settingsViewModel.isDeveloperModeEnabled.collectAsStateWithLifecycle(
                    initialValue = false
            )
    val smsScanMonths by
            settingsViewModel.smsScanMonths.collectAsStateWithLifecycle(initialValue = 3)
    val smsScanAllTime by
            settingsViewModel.smsScanAllTime.collectAsStateWithLifecycle(initialValue = false)
    val importExportMessage by settingsViewModel.importExportMessage.collectAsStateWithLifecycle()
    val exportedBackupFile by settingsViewModel.exportedBackupFile.collectAsStateWithLifecycle()
    var showSmsScanDialog by remember { mutableStateOf(false) }
    var showExportOptionsDialog by remember { mutableStateOf(false) }
    var showTimeoutDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // File picker for import
    val importLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
            onResult = { uri -> uri?.let { settingsViewModel.importBackup(it) } }
        )

    // File saver for export
    val exportSaveLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
            onResult = { uri -> uri?.let { settingsViewModel.saveBackupToFile(it) } }
        )

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val hazeState = remember { HazeState() }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CustomTitleTopAppBar(
                title = "Settings",
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehavior,
                hazeState = hazeState,
                hasBackButton = true,
                onBackClick = onNavigateBack,
                navigationContent = { NavigationContent(onNavigateBack) }
            )
        }
    ) { paddingValues ->
        Surface(
            modifier = modifier
                .fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(state = hazeState)
                    .overScrollVertical()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = Dimensions.Padding.content,
                        end = Dimensions.Padding.content,
                        top = Dimensions.Padding.content +
                                    paddingValues.calculateTopPadding()
                    ),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                // Theme Settings Section
                SectionHeader(
                    title = "Appearance",
                    modifier = Modifier.padding(start = Spacing.md)
                )

                CashiroCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(Dimensions.Padding.content),
                        verticalArrangement = Arrangement.spacedBy(Spacing.md)
                    ) {
                        // Theme Mode Selection
                        Column {
                            Text(
                                text = "Theme",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(Spacing.xs))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                            ) {
                                FilterChip(
                                    selected = themeUiState.isDarkTheme == null,
                                    onClick = { themeViewModel.updateDarkTheme(null) },
                                    label = { Text("System") },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = themeUiState.isDarkTheme == false,
                                    onClick = { themeViewModel.updateDarkTheme(false) },
                                    label = { Text("Light") },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = themeUiState.isDarkTheme == true,
                                    onClick = { themeViewModel.updateDarkTheme(true) },
                                    label = { Text("Dark") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Security Section
                SectionHeader(
                    title = "Security",
                    modifier = Modifier.padding(start = Spacing.md)
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(1.5.dp)
                ) {
                    PreferenceSwitch(
                        title = "App Lock",
                        subtitle =
                            if (appLockUiState.canUseBiometric) {
                                "Protect your data with biometric authentication"
                            } else {
                                appLockUiState.biometricCapability.getErrorMessage()
                            },
                        checked = appLockUiState.isLockEnabled,
                        onCheckedChange = { enabled ->
                            appLockViewModel.setAppLockEnabled(enabled)
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        padding = PaddingValues(0.dp),
                        isSingle = !appLockUiState.isLockEnabled,
                        isFirst = appLockUiState.isLockEnabled,
                    )

                    // Lock Timeout Setting (only show if app lock is enabled)
                    AnimatedVisibility(visible = appLockUiState.isLockEnabled) {
                        ListItem(
                            headline = { Text("Lock Timeout") },
                            supporting = {
                                Text(
                                    when (appLockUiState.timeoutMinutes) {
                                        0 ->
                                            "Lock immediately when app goes to background"

                                        1 -> "Lock after 1 minute in background"
                                        else ->
                                            "Lock after ${appLockUiState.timeoutMinutes} minutes in background"
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
                    title = "Data Management",
                    modifier = Modifier.padding(start = Spacing.md)
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(1.5.dp)
                ) {
                    // Manage Accounts
                    ListItem(
                        headline = {
                            Text(
                                text = "Manage Accounts",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        supporting = {
                            Text(
                                text = "Add manual accounts and update balances",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leading = {
                            Icon(
                                Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailing = {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = { onNavigateToManageAccounts() },
                        shape = ListItemPosition.Top.toShape(),
                        padding = PaddingValues(0.dp)
                    )

                    // Categories
                    ListItem(
                        headline = {
                            Text(
                                text = "Categories",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        supporting = {
                            Text(
                                text = "Manage expense and income categories",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leading = {
                            Icon(
                                Icons.Default.Category,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailing = {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = { onNavigateToCategories() },
                        shape = ListItemPosition.Middle.toShape(),
                        padding = PaddingValues(0.dp)
                    )

                    // Smart Rules
                    ListItem(
                        headline = {
                            Text(
                                text = "Smart Rules",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        supporting = {
                            Text(
                                text = "Automatic transaction categorization",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leading = {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailing = {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = { onNavigateToRules() },
                        shape = ListItemPosition.Middle.toShape(),
                        padding = PaddingValues(0.dp)
                    )

                    // Export Data
                    ListItem(
                        headline = {
                            Text(
                                text = "Export Data",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        supporting = {
                            Text(
                                text = "Backup all data to a file",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leading = {
                            Icon(
                                Icons.Default.Upload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailing = {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = { settingsViewModel.exportBackup() },
                        shape = ListItemPosition.Middle.toShape(),
                        padding = PaddingValues(0.dp)
                    )

                    // Import Data
                    ListItem(
                        headline = {
                            Text(
                                text = "Import Data",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        supporting = {
                            Text(
                                text = "Restore data from backup",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leading = {
                            Icon(
                                Icons.Default.Download,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailing = {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = { importLauncher.launch("*/*") },
                        shape = ListItemPosition.Middle.toShape(),
                        padding = PaddingValues(0.dp)
                    )

                    // SMS Scan Period
                    ListItem(
                        headline = {
                            Text(
                                text = "SMS Scan Period",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        supporting = {
                            Text(
                                text =
                                    if (smsScanAllTime) "Scan all SMS messages"
                                    else "Scan last $smsScanMonths months of messages",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leading = {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailing = {
                            Text(
                                text = if (smsScanAllTime) "All Time" else "$smsScanMonths months",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        },
                        onClick = { showSmsScanDialog = true },
                        shape = ListItemPosition.Bottom.toShape(),
                        padding = PaddingValues(0.dp)
                    )
                }

                // AI Features Section
                SectionHeader(
                    title = "AI Features",
                    modifier = Modifier.padding(start = Spacing.md)
                )

                CashiroCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(Dimensions.Padding.content),
                        verticalArrangement = Arrangement.spacedBy(Spacing.md)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "AI Chat Assistant",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text =
                                        when (downloadState) {
                                            DownloadState.NOT_DOWNLOADED ->
                                                "Download Qwen 2.5 model (${Constants.ModelDownload.MODEL_SIZE_MB} MB)"
                                            DownloadState.DOWNLOADING ->
                                                "Downloading Qwen model..."
                                            DownloadState.PAUSED -> "Download interrupted"
                                            DownloadState.COMPLETED -> "Qwen ready for chat"
                                            DownloadState.FAILED -> "Download failed"
                                            DownloadState.ERROR_INSUFFICIENT_SPACE ->
                                                "Not enough storage space" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Action area based on state
                            when (downloadState) {
                                DownloadState.NOT_DOWNLOADED -> {
                                    Button(onClick = { settingsViewModel.startModelDownload() }) {
                                        Icon(Icons.Default.Download, contentDescription = null)
                                        Spacer(modifier = Modifier.width(Spacing.xs))
                                        Text("Download")
                                    }
                                }
                                DownloadState.DOWNLOADING -> {
                                    Text(
                                        text = "$downloadProgress%",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                DownloadState.PAUSED -> {
                                    Button(onClick = { settingsViewModel.startModelDownload() }) {
                                        Icon(Icons.Default.Download, contentDescription = null)
                                        Spacer(modifier = Modifier.width(Spacing.xs))
                                        Text("Retry")
                                    }
                                }
                                DownloadState.COMPLETED -> {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = "Downloaded",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        TextButton(onClick = { settingsViewModel.deleteModel() }) {
                                            Text("Delete")
                                        }
                                    }
                                }
                                DownloadState.FAILED -> {
                                    Button(
                                        onClick = { settingsViewModel.startModelDownload() },
                                        colors =
                                            ButtonDefaults.buttonColors(
                                                containerColor =
                                                    MaterialTheme.colorScheme.error
                                            )
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null)
                                        Spacer(modifier = Modifier.width(Spacing.xs))
                                        Text("Retry")
                                    }
                                }
                                DownloadState.ERROR_INSUFFICIENT_SPACE -> {
                                    Icon(
                                        Icons.Default.Error,
                                        contentDescription = "Error",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }

                        // Progress details during download
                        AnimatedVisibility(
                            visible = downloadState == DownloadState.DOWNLOADING,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                            ) {
                                LinearProgressIndicator(
                                    progress = { downloadProgress / 100f },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "$downloadedMB MB / $totalMB MB",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Button(onClick = { settingsViewModel.cancelDownload() }, modifier = Modifier.fillMaxWidth(), colors =
                                    ButtonDefaults.buttonColors(
                                        containerColor =
                                            MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Icon(Icons.Default.Cancel, contentDescription = null)
                                    Spacer(modifier = Modifier.width(Spacing.xs))
                                    Text("Cancel Download")
                                }
                            }
                        }

                        // Info about AI features
                        if (downloadState == DownloadState.NOT_DOWNLOADED ||
                                        downloadState == DownloadState.ERROR_INSUFFICIENT_SPACE
                        ) {
                            HorizontalDivider()
                            Text(
                                text =
                                    "Chat with Qwen AI about your expenses and get financial insights. " +
                                            "All conversations stay private on your device.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Unrecognized Messages Section (only show if count > 0)
                val unreportedCount by
                        settingsViewModel.unreportedSmsCount.collectAsStateWithLifecycle()

                if (unreportedCount > 0) {
                    SectionHeader(
                        title = "Help Improve PennyWise",
                        modifier = Modifier.padding(start = Spacing.md)
                    )

                    CashiroCard(modifier = Modifier.fillMaxWidth(), onClick = {
                        Log.d("SettingsScreen", "Navigating to UnrecognizedSms screen")
                        onNavigateToUnrecognizedSms()
                    }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Dimensions.Padding.content),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Unrecognized Bank Messages",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "$unreportedCount message${if (unreportedCount > 1) "s"
                                    else ""} from potential banks",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                    Text(unreportedCount.toString())
                                }

                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = "View Messages",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Developer Section
                SectionHeader(
                    title = "Developer",
                    modifier = Modifier.padding(start = Spacing.md)
                )


                PreferenceSwitch(
                    title = "Developer Mode",
                    subtitle = "Show technical information in chat",
                    checked = isDeveloperModeEnabled,
                    onCheckedChange = { settingsViewModel.toggleDeveloperMode(it) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.DeveloperMode,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        ) },
                    padding = PaddingValues(0.dp),
                    isSingle = true
                )

                // Support Section
                SectionHeader(
                    title = "Support & Community",
                    modifier = Modifier.padding(start = Spacing.md)
                )

                val context = LocalContext.current

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(1.5.dp)
                ) {
                    ListItem(
                        headline = {
                            Text(
                                text = "Help & FAQ",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        supporting = {
                            Text(
                                text = "Frequently asked questions and help",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leading = {
                            Icon(
                                Icons.AutoMirrored.Filled.Help,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailing = {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = { onNavigateToFaq() },
                        shape = ListItemPosition.Top.toShape(),
                        padding = PaddingValues(0.dp)
                    )
                    ListItem(
                        headline = {
                            Text(
                                text = "Report an Issue",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        supporting = {
                            Text(
                                text = "Submit bug reports or bank requests on GitHub",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leading = {
                            Icon(
                                Icons.Default.BugReport,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailing = {
                            Icon(
                                Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = { onNavigateToFaq() },
                        shape = ListItemPosition.Bottom.toShape(),
                        padding = PaddingValues(0.dp),
                        modifier =
                            Modifier.clickable {
                                val intent =
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        "https://github.com/sarim2000/pennywiseai-tracker/issues/new/choose".toUri()
                                    )
                                context.startActivity(intent)
                            }
                    )
                }

                Spacer(
                    modifier = Modifier.height(110.dp)
                )
            }
        }
        // SMS Scan Period Dialog
        if (showSmsScanDialog) {
            AlertDialog(
                onDismissRequest = { showSmsScanDialog = false },
                title = { Text("SMS Scan Period") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        Text(
                            text =
                                "Choose how many months of SMS history to scan for transactions",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(Spacing.md))

                        // All Time option first, then period options including 24 months
                        // for 2 years coverage
                        val options = listOf(-1) + listOf(1, 2, 3, 6, 12, 24)
                        options.forEach { months ->
                            Row(
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .clickable {
                                            if (months == -1) {
                                                settingsViewModel
                                                    .updateSmsScanAllTime(
                                                        true
                                                    )
                                                showSmsScanDialog = false
                                            } else {
                                                settingsViewModel
                                                    .updateSmsScanMonths(
                                                        months
                                                    )
                                                settingsViewModel
                                                    .updateSmsScanAllTime(
                                                        false
                                                    )
                                                showSmsScanDialog = false
                                            }
                                        }
                                        .padding(vertical = Spacing.sm),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val isSelected =
                                    if (months == -1) smsScanAllTime
                                    else smsScanMonths == months && !smsScanAllTime
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        if (months == -1) {
                                            settingsViewModel.updateSmsScanAllTime(true)
                                            showSmsScanDialog = false
                                        } else {
                                            settingsViewModel.updateSmsScanMonths(
                                                months
                                            )
                                            settingsViewModel.updateSmsScanAllTime(
                                                false
                                            )
                                            showSmsScanDialog = false
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.width(Spacing.md))
                                Text(
                                    text =
                                        when (months) {
                                            -1 -> "All Time"
                                            1 -> "1 month"
                                            24 -> "2 years"
                                            else -> "$months months"
                                        },
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSmsScanDialog = false }) { Text("Cancel") }
                }
            )
        }

        // Show import/export message
        importExportMessage?.let { message ->
            // Check if we have an exported file ready
            if (exportedBackupFile != null && message.contains("successfully! Choose")) {
                showExportOptionsDialog = true
            } else {
                LaunchedEffect(message) {
                    // Auto-clear message after 5 seconds
                    kotlinx.coroutines.delay(5000)
                    settingsViewModel.clearImportExportMessage()
                }

                AlertDialog(
                    onDismissRequest = { settingsViewModel.clearImportExportMessage() },
                    title = { Text("Backup Status") },
                    text = { Text(message) },
                    confirmButton = {
                        TextButton(
                            onClick = { settingsViewModel.clearImportExportMessage() }
                        ) { Text("OK") }
                    }
                )
            }
        }

        // Export options dialog
        if (showExportOptionsDialog && exportedBackupFile != null) {
            val timestamp =
                java.time.LocalDateTime.now()
                    .format(
                        java.time.format.DateTimeFormatter.ofPattern(
                            "yyyy_MM_dd_HHmmss"
                        )
                    )
            val fileName = "PennyWise_Backup_$timestamp.pennywisebackup"

            AlertDialog(
                onDismissRequest = {
                    showExportOptionsDialog = false
                    settingsViewModel.clearImportExportMessage()
                },
                title = { Text("Save Backup") },
                text = {
                    Column {
                        Text("Backup created successfully!")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Choose how you want to save it:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                },
                confirmButton = {
                    Row {
                        TextButton(
                            onClick = {
                                exportSaveLauncher.launch(fileName)
                                showExportOptionsDialog = false
                                settingsViewModel.clearImportExportMessage()
                            }
                        ) {
                            Icon(Icons.Default.SaveAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save to Files")
                        }

                        TextButton(
                            onClick = {
                                settingsViewModel.shareBackup()
                                showExportOptionsDialog = false
                                settingsViewModel.clearImportExportMessage()
                            }
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share")
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showExportOptionsDialog = false
                            settingsViewModel.clearImportExportMessage()
                        }
                    ) { Text("Cancel") }
                }
            )
        }

        // Lock Timeout Dialog
        if (showTimeoutDialog) {
            AlertDialog(
                onDismissRequest = { showTimeoutDialog = false },
                title = { Text("Lock Timeout") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        Text(
                            text =
                                "Choose when to lock the app after it goes to background",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(Spacing.md))

                        val timeoutOptions =
                            listOf(
                                0 to "Immediately",
                                1 to "1 minute",
                                5 to "5 minutes",
                                15 to "15 minutes"
                            )

                        timeoutOptions.forEach { (minutes, label) ->
                            Row(
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .clickable {
                                            appLockViewModel.setTimeoutMinutes(
                                                minutes
                                            )
                                            showTimeoutDialog = false
                                        }
                                        .padding(vertical = Spacing.sm),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = appLockUiState.timeoutMinutes == minutes,
                                    onClick = {
                                        appLockViewModel.setTimeoutMinutes(minutes)
                                        showTimeoutDialog = false
                                    }
                                )
                                Spacer(modifier = Modifier.width(Spacing.sm))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showTimeoutDialog = false }) { Text("Done") }
                }
            )
        }
    }
}
