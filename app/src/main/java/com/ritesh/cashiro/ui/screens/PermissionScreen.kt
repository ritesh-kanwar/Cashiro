package com.ritesh.cashiro.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ritesh.cashiro.ui.components.PennyWiseScaffold
import com.ritesh.cashiro.ui.theme.Spacing
import com.ritesh.cashiro.ui.components.CustomTitleTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.ritesh.cashiro.ui.viewmodel.PermissionViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import com.ritesh.cashiro.ui.effects.overScrollVertical
import com.ritesh.cashiro.ui.effects.rememberOverscrollFlingBehavior
import dev.chrisbanes.haze.hazeSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionScreen(
    onPermissionGranted: () -> Unit,
    viewModel: PermissionViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Track which permissions have been granted
    var readSmsGranted by remember { mutableStateOf(false) }
    var receiveSmsGranted by remember { mutableStateOf(false) }

    // Permission launcher for multiple permissions
    val multiplePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        readSmsGranted = permissions[Manifest.permission.READ_SMS] == true
        receiveSmsGranted = permissions[Manifest.permission.RECEIVE_SMS] == true

        // Both SMS permissions granted (or at least READ_SMS for basic functionality)
        if (readSmsGranted) {
            viewModel.onPermissionResult(true)
            onPermissionGranted()
        } else {
            viewModel.onPermissionDenied()
        }
    }


    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val hazeState = remember { HazeState() }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CustomTitleTopAppBar(
                title = "Permissions",
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehavior,
                hazeState = hazeState,
                hasBackButton = false
            )
        }
    ) { innerPadding ->
        val scrollState = rememberScrollState()
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState)
                .overScrollVertical()
                .verticalScroll(scrollState)
                .padding(bottom = innerPadding.calculateBottomPadding())
                .padding(
                    start = Spacing.lg,
                    end = Spacing.lg,
                    bottom = Spacing.lg,
                    top = Spacing.lg + innerPadding.calculateTopPadding()
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            flingBehavior = rememberOverscrollFlingBehavior { scrollState }
        ) {
        item {
            Icon(
                imageVector = Icons.Filled.MailOutline,
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        
        item {
            Spacer(modifier = Modifier.height(Spacing.xl))
        }
        
        item {
            Text(
                text = "Enable Automatic Transaction Detection",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
        }
        
        item {
            Spacer(modifier = Modifier.height(Spacing.md))
        }
        
        item {
            Text(
                text = "PennyWise can automatically detect and categorize your bank transactions from SMS messages, saving you time and effort.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        item {
            Spacer(modifier = Modifier.height(Spacing.lg))
        }
        
        // Privacy card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(Spacing.md)
                ) {
                    Text(
                        text = "Your Privacy Matters",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Text(
                        text = "• Only transaction messages are processed\n" +
                                "• All data stays on your device\n" +
                                "• No personal messages are read\n" +
                                "• You can revoke access anytime in Settings",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(Spacing.xl))
        }
        
        // Show rationale if permission was denied
        item {
            if (uiState.showRationale) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Without SMS access, you'll need to manually add all your transactions. " +
                                "We only read bank transaction messages, not personal conversations.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(Spacing.md)
                    )
                }
                Spacer(modifier = Modifier.height(Spacing.md))
            }
        }
        
        // Primary action button
        item{
            Button(
                onClick = {
                    // Request both READ_SMS and RECEIVE_SMS permissions
                    val permissions = mutableListOf(
                        Manifest.permission.READ_SMS,
                        Manifest.permission.RECEIVE_SMS
                    )

                    // Add POST_NOTIFICATIONS for Android 13+
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                    }

                    multiplePermissionLauncher.launch(permissions.toTypedArray())
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Enable Automatic Detection")
            }
        }
        }
    }
}