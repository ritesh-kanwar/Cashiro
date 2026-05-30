package com.ritesh.cashiro.presentation.ui.features.settings.importstatement

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ritesh.cashiro.presentation.effects.overScrollVertical
import com.ritesh.cashiro.presentation.ui.components.CustomTitleTopAppBar
import com.ritesh.cashiro.presentation.ui.components.ListItem
import com.ritesh.cashiro.presentation.ui.components.ListItemPosition
import com.ritesh.cashiro.presentation.ui.components.toShape
import com.ritesh.cashiro.presentation.ui.theme.*
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportStatementScreen(
    onNavigateBack: () -> Unit,
    viewModel: ImportStatementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
        onResult = { uris ->
            if (uris.isNotEmpty()) {
                viewModel.importStatement(uris)
            }
        }
    )

    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val scrollBehaviorLarge = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val hazeState = remember { HazeState() }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehaviorLarge.nestedScrollConnection),
        containerColor = Color.Transparent,
        topBar = {
            CustomTitleTopAppBar(
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehaviorLarge,
                title = "Import Statement",
                hasBackButton = true,
                hasActionButton = true,
                navigationContent = {
                    Box(
                        modifier = Modifier
                            .animateContentSize()
                            .padding(start = 16.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onNavigateBack,
                            ),
                    ) {
                        IconButton(
                            onClick = onNavigateBack,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                contentColor = MaterialTheme.colorScheme.onBackground
                            )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                modifier = Modifier.size(Dimensions.Icon.small)
                            )
                        }
                    }
                },
                hazeState = hazeState
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(hazeState)
                .background(MaterialTheme.colorScheme.background)
                .overScrollVertical()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(Dimensions.Padding.content),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedContent(
                targetState = uiState,
                label = "ImportStateTransition"
            ) { state ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (state) {
                        is ImportStatementUiState.Idle -> {
                            IdleContent(
                                onSelectPdf = { pdfPicker.launch("application/pdf") }
                            )
                        }
                        is ImportStatementUiState.Loading -> {
                            LoadingContent(progress = state.progress)
                        }
                        is ImportStatementUiState.Success -> {
                            SuccessContent(
                                result = state.result,
                                onImportAnother = {
                                    viewModel.resetState()
                                    pdfPicker.launch("application/pdf")
                                },
                                onDone = onNavigateBack
                            )
                        }
                        is ImportStatementUiState.Error -> {
                            ErrorContent(
                                message = state.message,
                                onTryAgain = {
                                    viewModel.resetState()
                                    pdfPicker.launch("application/pdf")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IdleContent(onSelectPdf: () -> Unit) {
    Spacer(modifier = Modifier.height(Spacing.lg))
    
    // Using Cashiro's typical list item design for a more premium look
    ListItem(
        headline = { Text("Google Pay & PhonePe", fontWeight = FontWeight.Bold) },
        supporting = { Text("Import transactions directly from your generated PDF statements. Duplicates are auto-detected and skipped.") },
        leading = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(purple_light, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = purple_dark
                )
            }
        },
        shape = ListItemPosition.Single.toShape(),
        padding = PaddingValues(0.dp),
        onClick = onSelectPdf
    )

    Spacer(modifier = Modifier.height(Spacing.xl))

    Button(
        onClick = onSelectPdf,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(Dimensions.CornerRadius.large)
    ) {
        Icon(
            imageVector = Icons.Default.PictureAsPdf,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(Spacing.sm))
        Text("Select PDF Statement", style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun LoadingContent(progress: Float) {
    Spacer(modifier = Modifier.height(Spacing.xl))

    // Animated Realtime Progress
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "progressAnimation"
    )

    ListItem(
        headline = { Text("Importing transactions...", fontWeight = FontWeight.Bold) },
        supporting = { 
            Column {
                Text("Parsing PDF and skipping duplicates")
                Spacer(modifier = Modifier.height(Spacing.md))
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }
        },
        leading = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(orange_light, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Autorenew,
                    contentDescription = null,
                    tint = orange_dark
                )
            }
        },
        shape = ListItemPosition.Single.toShape(),
        padding = PaddingValues(0.dp)
    )
}

@Composable
private fun SuccessContent(
    result: com.ritesh.cashiro.data.statement.StatementImportResult.Success,
    onImportAnother: () -> Unit,
    onDone: () -> Unit
) {
    Spacer(modifier = Modifier.height(Spacing.lg))
    
    // Overview Item
    ListItem(
        headline = { Text("Import Complete", fontWeight = FontWeight.Bold) },
        supporting = { Text("Successfully processed PDF statement") },
        leading = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(green_light, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = green_dark
                )
            }
        },
        shape = ListItemPosition.Top.toShape(),
        padding = PaddingValues(0.dp)
    )

    ListItem(
        headline = { Text("Transactions Imported") },
        trailing = { 
            Text(
                text = "${result.imported}", 
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            ) 
        },
        shape = if (result.skippedDuplicates > 0 || result.enriched > 0) ListItemPosition.Middle.toShape() else ListItemPosition.Bottom.toShape(),
        padding = PaddingValues(0.dp)
    )

    if (result.enriched > 0) {
        ListItem(
            headline = { Text("Transactions Enriched") },
            trailing = { 
                Text(
                    text = "${result.enriched}", 
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                ) 
            },
            shape = if (result.skippedDuplicates > 0) ListItemPosition.Middle.toShape() else ListItemPosition.Bottom.toShape(),
            padding = PaddingValues(0.dp)
        )
    }

    if (result.skippedDuplicates > 0) {
        ListItem(
            headline = { Text("Duplicates Skipped") },
            supporting = { 
                Text("Exact: ${result.skippedByHash} • Ref: ${result.skippedByReference} • Date/Amt: ${result.skippedByAmountDate}")
            },
            trailing = { 
                Text(
                    text = "${result.skippedDuplicates}", 
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ) 
            },
            shape = ListItemPosition.Bottom.toShape(),
            padding = PaddingValues(0.dp)
        )
    }

    Spacer(modifier = Modifier.height(Spacing.lg))

    Button(
        onClick = onImportAnother,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(Dimensions.CornerRadius.large)
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(Spacing.sm))
        Text("Import Another", style = MaterialTheme.typography.titleMedium)
    }

    OutlinedButton(
        onClick = onDone,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(Dimensions.CornerRadius.large)
    ) {
        Text("Done", style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onTryAgain: () -> Unit
) {
    Spacer(modifier = Modifier.height(Spacing.xl))

    ListItem(
        headline = { Text("Import Failed", fontWeight = FontWeight.Bold) },
        supporting = { Text(message) },
        leading = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(red_light, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    tint = red_dark
                )
            }
        },
        shape = ListItemPosition.Single.toShape(),
        padding = PaddingValues(0.dp)
    )

    Spacer(modifier = Modifier.height(Spacing.xl))

    Button(
        onClick = onTryAgain,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(Dimensions.CornerRadius.large)
    ) {
        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(Spacing.sm))
        Text("Try Again", style = MaterialTheme.typography.titleMedium)
    }
}
