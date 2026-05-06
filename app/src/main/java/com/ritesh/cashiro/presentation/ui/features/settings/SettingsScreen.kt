package com.ritesh.cashiro.presentation.ui.features.settings

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Webhook
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.ritesh.cashiro.R
import com.ritesh.cashiro.core.Constants
import com.ritesh.cashiro.presentation.effects.overScrollVertical
import com.ritesh.cashiro.presentation.ui.components.CustomTitleTopAppBar
import com.ritesh.cashiro.presentation.ui.components.DeleteAIModelDialog
import com.ritesh.cashiro.presentation.ui.components.ListItem
import com.ritesh.cashiro.presentation.ui.components.ListItemPosition
import com.ritesh.cashiro.presentation.ui.components.LoadingCircularProgress
import com.ritesh.cashiro.presentation.ui.components.SectionHeader
import com.ritesh.cashiro.presentation.ui.components.toShape
import com.ritesh.cashiro.presentation.ui.features.categories.NavigationContent
import com.ritesh.cashiro.presentation.ui.icons.Bag
import com.ritesh.cashiro.presentation.ui.icons.Box2
import com.ritesh.cashiro.presentation.ui.icons.Clock
import com.ritesh.cashiro.presentation.ui.icons.ExportArrow02
import com.ritesh.cashiro.presentation.ui.icons.Fireworks7
import com.ritesh.cashiro.presentation.ui.icons.Ghost
import com.ritesh.cashiro.presentation.ui.icons.Iconax
import com.ritesh.cashiro.presentation.ui.icons.ImportArrow01
import com.ritesh.cashiro.presentation.ui.icons.MessageProgramming
import com.ritesh.cashiro.presentation.ui.icons.MessageQuestion
import com.ritesh.cashiro.presentation.ui.icons.NotificationBing
import com.ritesh.cashiro.presentation.ui.icons.SecuritySafe
import com.ritesh.cashiro.presentation.ui.icons.Status
import com.ritesh.cashiro.presentation.ui.theme.Dimensions
import com.ritesh.cashiro.presentation.ui.theme.Spacing
import com.ritesh.cashiro.presentation.ui.theme.blue_dark
import com.ritesh.cashiro.presentation.ui.theme.blue_light
import com.ritesh.cashiro.presentation.ui.theme.cyan_dark
import com.ritesh.cashiro.presentation.ui.theme.cyan_light
import com.ritesh.cashiro.presentation.ui.theme.green_dark
import com.ritesh.cashiro.presentation.ui.theme.green_light
import com.ritesh.cashiro.presentation.ui.theme.grey_dark
import com.ritesh.cashiro.presentation.ui.theme.grey_light
import com.ritesh.cashiro.presentation.ui.theme.orange_dark
import com.ritesh.cashiro.presentation.ui.theme.orange_light
import com.ritesh.cashiro.presentation.ui.theme.pink_dark
import com.ritesh.cashiro.presentation.ui.theme.pink_light
import com.ritesh.cashiro.presentation.ui.theme.purple_dark
import com.ritesh.cashiro.presentation.ui.theme.purple_light
import com.ritesh.cashiro.presentation.ui.theme.red_dark
import com.ritesh.cashiro.presentation.ui.theme.red_light
import com.ritesh.cashiro.presentation.ui.theme.yellow_dark
import com.ritesh.cashiro.presentation.ui.theme.yellow_light
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit,
    onNavigateToCategories: () -> Unit = {},
    onNavigateToManageAccounts: () -> Unit = {},
    onNavigateToRules: () -> Unit = {},
    onNavigateToAppearance: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToSms: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToWebhooks: () -> Unit = {},
    onNavigateToBudgets: () -> Unit = {},
    onNavigateToDataPrivacy: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    blurEffects: Boolean
) {
    val uiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val downloadState = uiState.downloadStatus
    val downloadProgress = uiState.downloadProgress
    val totalTransactionsCount by settingsViewModel.totalTransactions.collectAsStateWithLifecycle()
    val userPreferences by settingsViewModel.userPreferences.collectAsStateWithLifecycle(initialValue = null)
    var showDeleteModelDialog by remember { mutableStateOf(false) }

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
                navigationContent = { NavigationContent { onNavigateBack() } }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState)
                .overScrollVertical()
                .verticalScroll(
                    state = rememberScrollState()
                )
                .padding(
                    start = Dimensions.Padding.content,
                    end = Dimensions.Padding.content,
                    top = Dimensions.Padding.content +
                            paddingValues.calculateTopPadding()
                ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(1.5.dp)
            ) {
                val profileImageUri = userPreferences?.profileImageUri?.toUri()
                val profileBackgroundColor = Color(userPreferences?.profileBackgroundColor ?: Color.Transparent.toArgb())

                ListItem(
                    headline = {
                        Text(
                            text = userPreferences?.userName ?: "User",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    },
                    supporting = {
                        Text(
                            text = "$totalTransactionsCount Transactions",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.8f)
                        )
                    },
                    leading = {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                                .background(profileBackgroundColor),
                            contentAlignment = Alignment.Center
                        ) {
                            if (profileImageUri != null) {
                                AsyncImage(
                                    model = profileImageUri,
                                    contentDescription = "Profile",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.avatar_1),
                                    contentDescription = "Profile",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    },
                    trailing = {
                        Icon(
                            Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    },
                    onClick = { onNavigateToProfile() },
                    shape = ListItemPosition.Top.toShape(),
                    listColor = MaterialTheme.colorScheme.primaryContainer,
                    padding = PaddingValues(0.dp)
                )

                ListItem(
                    headline = {
                        Text(
                            text = "Appearances",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    supporting = {
                        Text(
                            text = "App's personalization settings",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leading = {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    color = orange_light,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Palette,
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
                    onClick = { onNavigateToAppearance() },
                    shape = ListItemPosition.Bottom.toShape(),
                    padding = PaddingValues(0.dp)
                )

                Spacer( modifier = Modifier.height(Spacing.md))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(1.5.dp)
                ) {
                    // Manage Accounts
                    ListItem(
                        headline = {
                            Text(
                                text = "Accounts",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        supporting = {
                            Text(
                                text = "Add accounts and update balances",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leading = {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        color = red_light,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.AccountBalance,
                                    contentDescription = null,
                                    tint = red_dark
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
                        onClick = { onNavigateToManageAccounts() },
                        shape = ListItemPosition.Top.toShape(),
                        padding = PaddingValues(0.dp)
                    )

                    // Budgets
                    ListItem(
                        headline = {
                            Text(
                                text = "Budgets",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        supporting = {
                            Text(
                                text = "Set and manage monthly spending limits",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leading = {
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
                                    Iconax.Status,
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
                        onClick = { onNavigateToBudgets() },
                        shape = ListItemPosition.Middle.toShape(),
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
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        color = purple_light,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Iconax.Box2,
                                    contentDescription = null,
                                    tint = purple_dark
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
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        color = orange_light,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Iconax.Fireworks7,
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
                        onClick = { onNavigateToRules() },
                        shape = ListItemPosition.Middle.toShape(),
                        padding = PaddingValues(0.dp)
                    )

                    // Data Privacy
                    ListItem(
                        headline = {
                            Text(
                                text = "Data Privacy",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        supporting = {
                            Text(
                                text = "Manage your data and privacy settings",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leading = {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        color = blue_light,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Iconax.SecuritySafe,
                                    contentDescription = null,
                                    tint = blue_dark
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
                        onClick = { onNavigateToDataPrivacy() },
                        shape = ListItemPosition.Bottom.toShape(),
                        padding = PaddingValues(0.dp)
                    )
                }

                Spacer( modifier = Modifier.height(Spacing.md))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(1.5.dp)
                ) {
                    ListItem(
                        headline = {
                            Text(
                                text = "AI Chat Assistant",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        supporting = {
                            Text(
                                text = when (downloadState) {
                                    DownloadState.NOT_DOWNLOADED -> "Download Qwen 2.5 model (${Constants.ModelDownload.MODEL_SIZE_MB} MB)"
                                    DownloadState.DOWNLOADING -> "Downloading... $downloadProgress%"
                                    DownloadState.PAUSED -> "Download paused. Tap to resume."
                                    DownloadState.COMPLETED -> "Qwen ready for chat"
                                    DownloadState.FAILED -> "Download failed. Tap to retry."
                                    DownloadState.ERROR_INSUFFICIENT_SPACE -> "Not enough storage space"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (downloadState == DownloadState.FAILED || downloadState == DownloadState.ERROR_INSUFFICIENT_SPACE)
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leading = {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        color = yellow_light,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = yellow_dark
                                )
                            }
                        },
                        trailing = {
                            when (downloadState) {
                                DownloadState.NOT_DOWNLOADED -> {
                                    Icon(
                                        Iconax.ImportArrow01,
                                        contentDescription = "Download",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                DownloadState.DOWNLOADING -> {
                                    LoadingCircularProgress(
                                        modifier = Modifier.size(32.dp),
                                        progress = downloadProgress / 100f
                                    )
                                }
                                DownloadState.PAUSED, DownloadState.FAILED -> {
                                    Icon(
                                        Icons.Rounded.Refresh,
                                        contentDescription = "Retry",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                DownloadState.COMPLETED -> {
                                    Icon(
                                        Iconax.Bag,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                DownloadState.ERROR_INSUFFICIENT_SPACE -> {
                                    Icon(
                                        Icons.Rounded.Error,
                                        contentDescription = "Error",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        },
                        onClick = {
                            when (downloadState) {
                                DownloadState.NOT_DOWNLOADED, DownloadState.PAUSED, DownloadState.FAILED -> {
                                    settingsViewModel.startModelDownload()
                                }
                                DownloadState.DOWNLOADING -> {
                                    settingsViewModel.cancelDownload()
                                }
                                DownloadState.COMPLETED -> {
                                    showDeleteModelDialog = true
                                }
                                else -> {}
                            }
                        },
                        shape = ListItemPosition.Top.toShape(),
                        padding = PaddingValues(0.dp)
                    )
                    // Notifications
                    ListItem(
                        headline = {
                            Text(
                                text = "Notifications",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        supporting = {
                            Text(
                                text = "Manage reminder notification settings",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leading = {
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
                                    Iconax.NotificationBing,
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
                        onClick = { onNavigateToNotifications() },
                        shape = ListItemPosition.Middle.toShape(),
                        padding = PaddingValues(0.dp)
                    )
                    // SMS
                    ListItem(
                        headline = {
                            Text(
                                text = "SMS",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        supporting = {
                            Text(
                                text = "Manage SMS settings",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leading = {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        color = cyan_light,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Iconax.Clock,
                                    contentDescription = null,
                                    tint = cyan_dark,
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
                        onClick = { onNavigateToSms() },
                        shape = ListItemPosition.Middle.toShape(),
                        padding = PaddingValues(0.dp)
                    )
                    ListItem(
                        headline = {
                            Text(
                                text = "Webhooks",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        supporting = {
                            Text(
                                text = "BYOAPI finance sync",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leading = {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        color = purple_light,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.Webhook,
                                    contentDescription = null,
                                    tint = purple_dark
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
                        onClick = { onNavigateToWebhooks() },
                        shape = ListItemPosition.Bottom.toShape(),
                        padding = PaddingValues(0.dp)
                    )
                }

                Spacer( modifier = Modifier.height(Spacing.md))
                ListItem(
                    headline = {
                        Text(
                            text = "About",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    supporting = {
                        Text(
                            text = "App version, source code and more",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leading = {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    color = orange_light,
                                    shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.cashiro),
                                contentDescription = "Cashiro Logo",
                                colorFilter = ColorFilter.tint(orange_dark),
                                modifier = Modifier.size(24.dp)
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
                    onClick = { onNavigateToAbout() },
                    shape = ListItemPosition.Single.toShape(),
                    padding = PaddingValues(0.dp)
                )
                Spacer(modifier = Modifier.height(110.dp))
            }
        }

        if (showDeleteModelDialog) {
            DeleteAIModelDialog(
                onDismiss = { showDeleteModelDialog = false },
                onDelete = {
                    settingsViewModel.deleteModel()
                    showDeleteModelDialog = false
                },
                blurEffects = blurEffects,
                hazeState = hazeState
            )
        }
    }
}
