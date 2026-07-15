package com.ritesh.cashiro.presentation.ui.features.settings.dataprivacy

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import com.ritesh.cashiro.data.database.entity.TransactionEntity
import com.ritesh.cashiro.presentation.effects.BlurredAnimatedVisibility
import com.ritesh.cashiro.presentation.effects.overScrollVertical
import com.ritesh.cashiro.presentation.ui.components.BrandIcon
import com.ritesh.cashiro.presentation.ui.components.GenericTypeSwitcher
import com.ritesh.cashiro.presentation.ui.components.ListItem
import com.ritesh.cashiro.presentation.ui.components.SectionHeader
import com.ritesh.cashiro.presentation.ui.components.listBottomItemShape
import com.ritesh.cashiro.presentation.ui.components.listTopItemShape
import com.ritesh.cashiro.presentation.ui.icons.Iconax
import com.ritesh.cashiro.presentation.ui.icons.Information
import com.ritesh.cashiro.presentation.ui.icons.ReceiptItem
import com.ritesh.cashiro.presentation.ui.theme.Dimensions
import com.ritesh.cashiro.presentation.ui.theme.Spacing
import com.ritesh.parser.core.ParsedTransaction
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * A bottom sheet for reviewing and confirming transactions extracted from a PDF.
 * Allows users to map accounts, handle duplicates, and toggle balance updates.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfImportSheet(
    analysisResult: PdfAnalysisResult,
    availableAccounts: List<AccountBalanceEntity>,
    onConfirm: (
        transactionDecisions: Map<Int, TransactionImportDecision>,
        accountDecisions: Map<String, AccountImportDecision>,
        accountMappings: Map<String, AccountBalanceEntity?>,
        shouldUpdateBalances: Boolean
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    // Decisions for accounts
    val accountDecisions = remember(analysisResult) {
        val map = mutableStateMapOf<String, AccountImportDecision>()
        analysisResult.accountMatches.forEach { match ->
            map[match.last4] = if (match.hasExistingMatch) AccountImportDecision.MERGE_WITH_EXISTING else AccountImportDecision.CREATE_NEW
        }
        map
    }

    // Custom mappings (extracted last4 -> selected app account)
    val accountMappings = remember(analysisResult) {
        val map = mutableStateMapOf<String, AccountBalanceEntity?>()
        analysisResult.accountMatches.forEach { match ->
            map[match.last4] = match.existingAccount
        }
        map
    }

    // Global decision for balance updates (deduction logic)
    var shouldUpdateBalances by remember { mutableStateOf(true) }

    // Decisions for transactions
    val transactionDecisions = remember(analysisResult) {
        val map = mutableStateMapOf<Int, TransactionImportDecision>()
        analysisResult.transactionItems.forEachIndexed { index, item ->
            map[index] = item.initialDecision
        }
        map
    }

    var filterIndex by remember { mutableIntStateOf(0) } // 0: All, 1: Duplicates

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = Dimensions.Radius.lg, topEnd = Dimensions.Radius.lg)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = Spacing.md)
            ) {
                // Header Content
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Review Import",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${analysisResult.transactionCount} transactions from ${analysisResult.accountMatches.size} accounts",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    IconButton(
                        onClick = onDismiss,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary.copy(0.2f),
                            contentColor = MaterialTheme.colorScheme.tertiary
                        ),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            Iconax.ReceiptItem,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp))
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.md))

                // Bulk Actions Logic
                val duplicatesWithIndices = remember(analysisResult, transactionDecisions) {
                    analysisResult.transactionItems.mapIndexed { index, item -> index to item }
                        .filter { it.second.duplicateMatch != null }
                }
                val isAllSkipped = duplicatesWithIndices.isNotEmpty() && duplicatesWithIndices.all { (idx, _) -> 
                    transactionDecisions[idx] == TransactionImportDecision.SKIP 
                }
                val isAllOverridden = duplicatesWithIndices.isNotEmpty() && duplicatesWithIndices.all { (idx, _) -> 
                    transactionDecisions[idx] == TransactionImportDecision.OVERRIDE_EXISTING 
                }

                // Main List
                LazyColumn(
                    modifier = Modifier
                        .animateContentSize()
                        .weight(1f)
                        .fillMaxWidth()
                        .overScrollVertical(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                    contentPadding = PaddingValues(bottom = 120.dp) // Extra padding for the floating bar
                ) {
                    // Account Mappings Section
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                            modifier = Modifier
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.surface,
                                            MaterialTheme.colorScheme.surface,
                                            MaterialTheme.colorScheme.surface,
                                            Color.Transparent
                                        )
                                    )
                                )
                                .padding(
                                    top = Spacing.md,
                                )

                        ) {
                            Spacer(
                                modifier =
                                    Modifier
                                        .height(16.dp)
                                        .width(4.dp)
                                        .background(
                                            MaterialTheme.colorScheme.tertiary,
                                            RoundedCornerShape(14.dp)
                                        )

                            )
                            SectionHeader(title = "Bank Accounts")
                        }
                    }
                    
                    items(analysisResult.accountMatches) { accountMatch ->
                        PdfAccountDecisionCard(
                            match = accountMatch,
                            availableAccounts = availableAccounts,
                            currentDecision = accountDecisions[accountMatch.last4] ?: AccountImportDecision.CREATE_NEW,
                            selectedMapping = accountMappings[accountMatch.last4],
                            onDecisionChanged = { accountDecisions[accountMatch.last4] = it },
                            onMappingChanged = { accountMappings[accountMatch.last4] = it }
                        )
                    }

                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { shouldUpdateBalances = !shouldUpdateBalances },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        ) {
                            Row(
                                modifier = Modifier.padding(Spacing.md),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Update Account Balances",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Automatically adjust balances based on all imported transactions",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = shouldUpdateBalances,
                                    onCheckedChange = { shouldUpdateBalances = it },
                                )
                            }
                        }
                    }

                    // Transactions Section
                    stickyItemHeader {
                        Column(modifier = Modifier
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.surface,
                                        MaterialTheme.colorScheme.surface,
                                        MaterialTheme.colorScheme.surface,
                                        Color.Transparent
                                    )
                                )
                            )
                            .padding(
                                top = Spacing.md,
                            )
                        ){
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                            ) {
                                Spacer(
                                    modifier =
                                        Modifier
                                            .height(16.dp)
                                            .width(4.dp)
                                            .background(
                                                MaterialTheme.colorScheme.tertiary,
                                                RoundedCornerShape(14.dp)
                                            )

                                )
                                SectionHeader(title = "Transactions")
                            }

                            Spacer(modifier = Modifier.height(Spacing.sm))

                            GenericTypeSwitcher(
                                selectedIndex = filterIndex,
                                onIndexChange = { filterIndex = it },
                                options = listOf("All", "Duplicates"),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Bulk Actions for Duplicates
                    item {
                        BlurredAnimatedVisibility(
                            visible = (filterIndex == 1),
                            enter = fadeIn() + slideInVertically { it },
                            exit = fadeOut() + slideOutVertically { it }
                        ) {
                            Row(
                                modifier = Modifier
                                    .animateContentSize()
                                    .fillMaxWidth()
                                    .padding(vertical = Spacing.xs),
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                            ) {
                                Button(
                                    onClick = {
                                        duplicatesWithIndices.forEach { (index, _) ->
                                            transactionDecisions[index] =
                                                TransactionImportDecision.SKIP
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isAllSkipped) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                        contentColor = if (isAllSkipped) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onErrorContainer
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        "Skip All",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }

                                Button(
                                    onClick = {
                                        duplicatesWithIndices.forEach { (index, _) ->
                                            transactionDecisions[index] =
                                                TransactionImportDecision.OVERRIDE_EXISTING
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isAllOverridden) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                        contentColor = if (isAllOverridden) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onErrorContainer
                                    )
                                ) {
                                    Text(
                                        "Override All",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                        }
                    }

                    val displayedItemsWithIndices = analysisResult.transactionItems.mapIndexed { index, item -> 
                        index to item 
                    }.filter { (_, item) ->
                        when (filterIndex) {
                            1 -> item.duplicateMatch != null
                            else -> true
                        }
                    }

                    item {
                        BlurredAnimatedVisibility(
                            visible = (filterIndex == 1 && displayedItemsWithIndices.isEmpty()),
                            enter = fadeIn() + slideInVertically { it },
                            exit = fadeOut() + slideOutVertically { it }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.xl),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = Color(0xFF2E7D32).copy(alpha = 0.5f)
                                )
                                Spacer(Modifier.height(Spacing.md))
                                Text(
                                    "No duplicates detected!",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "All transactions are ready for import.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                        alpha = 0.7f
                                    )
                                )
                            }
                        }
                    }

                    items(
                        items = displayedItemsWithIndices,
                        key = { pair -> pair.first }
                    ) { (originalIndex, item) ->
                        TransactionImportItemCard(
                            item = item,
                            currentDecision = transactionDecisions[originalIndex] ?: item.initialDecision,
                            onDecisionChanged = { transactionDecisions[originalIndex] = it }
                        )
                    }
                }
            }

            // Bottom Action Bar with Gradient Fade
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text(
                            text = "Cancel",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = { 
                            onConfirm(
                                transactionDecisions.toMap(),
                                accountDecisions.toMap(),
                                accountMappings.toMap(),
                                shouldUpdateBalances
                            )
                        },
                        modifier = Modifier
                            .weight(1.5f)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "Import",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionImportItemCard(
    item: PdfTransactionImportItem,
    currentDecision: TransactionImportDecision,
    onDecisionChanged: (TransactionImportDecision) -> Unit
) {
    var showComparison by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.duplicateMatch != null) 
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.05f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = if (item.duplicateMatch != null) 
            BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
        else null
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            // Row with Details and Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Merchant Icon
                BrandIcon(
                    merchantName = item.parsed.merchant ?: "Unknown",
                    size = 40.dp,
                    showBackground = true,
                    category = "Miscellaneous"
                )

                Spacer(modifier = Modifier.width(Spacing.sm))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.parsed.merchant ?: "Unknown Merchant",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatTimestamp(item.parsed.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₹${item.parsed.amount}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (item.parsed.type == com.ritesh.parser.core.TransactionType.INCOME) 
                            Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                    )
                }
            }

            // Duplicate Indicator & Decision Toggle
            if (item.duplicateMatch != null) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Iconax.Information,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Potential duplicate found",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "COMPARE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onError,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.error)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                .clickable { showComparison = true }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.sm))

                // Decision Segmented Control for Duplicates
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    DecisionButton(
                        text = "Skip",
                        selected = currentDecision == TransactionImportDecision.SKIP,
                        modifier = Modifier.weight(1f),
                        onClick = { onDecisionChanged(TransactionImportDecision.SKIP) }
                    )
                    DecisionButton(
                        text = "Override",
                        selected = currentDecision == TransactionImportDecision.OVERRIDE_EXISTING,
                        modifier = Modifier.weight(1f),
                        onClick = { onDecisionChanged(TransactionImportDecision.OVERRIDE_EXISTING) }
                    )
                }
            } else {
                // Simple Import/Skip for non-duplicates
                Spacer(modifier = Modifier.height(Spacing.sm))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    DecisionChip(
                        text = if (currentDecision == TransactionImportDecision.SKIP) "Skipped" else "To Import",
                        selected = currentDecision == TransactionImportDecision.IMPORT_NEW,
                        onClick = { 
                            onDecisionChanged(if (currentDecision == TransactionImportDecision.IMPORT_NEW) TransactionImportDecision.SKIP else TransactionImportDecision.IMPORT_NEW)
                        }
                    )
                }
            }
        }
    }

    if (showComparison && item.duplicateMatch != null) {
        DuplicateComparisonSheet(
            parsed = item.parsed,
            existing = item.duplicateMatch,
            onDismiss = { showComparison = false }
        )
    }
}

@Composable
fun DecisionButton(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun DecisionChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = if (!selected) BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selected) {
                Icon(Icons.Rounded.CheckCircle, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(4.dp))
            } else {
                Icon(Icons.Rounded.ErrorOutline, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicateComparisonSheet(
    parsed: ParsedTransaction,
    existing: TransactionEntity,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md)
                .padding(bottom = Spacing.xl)
        ) {
            Text(
                text = "Transaction Comparison",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = Spacing.xs)
            )
            Text(
                text = "Compare extracted PDF details with your existing records.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = Spacing.lg)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                item {
                    SectionHeader(title = "Amount", modifier = Modifier.padding(top = Spacing.sm, start = Spacing.md))
                }
                item {
                    ComparisonListGroup(
                        label = "Amount",
                        pdfValue = "₹${parsed.amount}",
                        existingValue = "₹${existing.amount}",
                        isMatch = parsed.amount.toDouble() == existing.amount.toDouble()
                    )
                }

                item {
                    SectionHeader(title = "Date/Time", modifier = Modifier.padding(top = Spacing.sm, start = Spacing.md))
                }
                item {
                    ComparisonListGroup(
                        label = "Date",
                        pdfValue = formatTimestamp(parsed.timestamp),
                        existingValue = formatLocalDateTime(existing.dateTime),
                        isMatch = false
                    )
                }

                item {
                    SectionHeader(title = "Merchant", modifier = Modifier.padding(top = Spacing.sm, start = Spacing.md))
                }
                item {
                    ComparisonListGroup(
                        label = "Merchant",
                        pdfValue = parsed.merchant ?: "Unknown",
                        existingValue = existing.merchantName,
                        isMatch = parsed.merchant?.equals(existing.merchantName, ignoreCase = true) == true
                    )
                }

                item {
                    SectionHeader(title = "Account", modifier = Modifier.padding(top = Spacing.sm, start = Spacing.md))
                }
                item {
                    ComparisonListGroup(
                        label = "Account",
                        pdfValue = "**** ${parsed.accountLast4 ?: ""}",
                        existingValue = "**** ${existing.accountNumber ?: ""}",
                        isMatch = true
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.lg))
            
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Close")
            }
        }
    }
}

@Composable
fun ComparisonListGroup(
    label: String,
    pdfValue: String,
    existingValue: String,
    isMatch: Boolean
) {
    Column {
        ListItem(
            headline = { Text(pdfValue) },
            supporting = { Text("Extracted from PDF") },
            leading = {
                Icon(
                    Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            padding = PaddingValues(horizontal = 0.dp, vertical = 1.5.dp),
            shape = listTopItemShape
        )
        ListItem(
            headline = { Text(existingValue) },
            supporting = { Text("Existing in Database") },
            leading = {
                Icon(
                    Icons.Default.History,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
            },
            padding = PaddingValues(horizontal = 0.dp, vertical = 1.5.dp),
            shape = listBottomItemShape
        )
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val dateTime = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(timestamp), java.time.ZoneId.systemDefault())
    return dateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy · hh:mm a"))
}

private fun formatLocalDateTime(dateTime: LocalDateTime): String {
    return dateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy · hh:mm a"))
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
private fun androidx.compose.foundation.lazy.LazyListScope.stickyItemHeader(
    key: Any? = null,
    contentType: Any? = null,
    content: @Composable androidx.compose.foundation.lazy.LazyItemScope.(Int) -> Unit
) {
    stickyHeader(key = key, contentType = contentType, content = content)
}
