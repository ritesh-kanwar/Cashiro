package com.ritesh.cashiro.presentation.accounts

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.text.KeyboardOptions
import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import com.ritesh.cashiro.ui.theme.*
import com.ritesh.cashiro.utils.CurrencyFormatter
import java.math.BigDecimal
import java.time.format.DateTimeFormatter

@Suppress("DEPRECATION")
@Composable
fun BalanceHistoryDialog(
    bankName: String,
    accountLast4: String,
    balanceHistory: List<AccountBalanceEntity>,
    onDismiss: () -> Unit,
    onDeleteBalance: (Long) -> Unit,
    onUpdateBalance: (Long, BigDecimal) -> Unit
) {
    // Get the primary currency for this account
    val accountPrimaryCurrency = remember(bankName) {
        CurrencyFormatter.getBankBaseCurrency(bankName)
    }
    var editingId by remember { mutableStateOf<Long?>(null) }
    var editingValue by remember { mutableStateOf("") }
    var showDeleteConfirmation by remember { mutableStateOf<Long?>(null) }
    var expandedSources by remember { mutableStateOf<Set<Long>>(emptySet()) }
    val clipboard = LocalClipboardManager.current
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.8f),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Dimensions.Padding.content)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Balance History",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$bankName ••$accountLast4",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                
                Spacer(modifier = Modifier.height(Spacing.md))
                
                if (balanceHistory.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No balance history available",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    // Balance History List
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        items(balanceHistory) { balance ->
                            val isLatest = balance == balanceHistory.first()
                            val isOnlyRecord = balanceHistory.size == 1
                            val isExpanded = expandedSources.contains(balance.id)
                            
                            BalanceHistoryItem(
                                balance = balance,
                                isLatest = isLatest,
                                isOnlyRecord = isOnlyRecord,
                                isExpanded = isExpanded,
                                editingId = editingId,
                                editingValue = editingValue,
                                accountPrimaryCurrency = accountPrimaryCurrency,
                                onEditClick = {
                                    editingId = balance.id
                                    editingValue = balance.balance.toPlainString()
                                },
                                onDeleteClick = {
                                    showDeleteConfirmation = balance.id
                                },
                                onEditValueChange = { value ->
                                    if (value.isEmpty() || value.matches(Regex("^\\d*\\.?\\d*$"))) {
                                        editingValue = value
                                    }
                                },
                                onSaveEdit = {
                                    editingValue.toBigDecimalOrNull()?.let { newBalance ->
                                        onUpdateBalance(balance.id, newBalance)
                                        editingId = null
                                        editingValue = ""
                                    }
                                },
                                onCancelEdit = {
                                    editingId = null
                                    editingValue = ""
                                },
                                onToggleExpand = {
                                    expandedSources = if (isExpanded) {
                                        expandedSources - balance.id
                                    } else {
                                        expandedSources + balance.id
                                    }
                                },
                                clipboard = clipboard
                            )
                        }
                    }
                }
                
                // Info text
                Text(
                    text = "${balanceHistory.size} record(s) • Latest balance is shown in accounts",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.sm)
                )
            }
        }
    }
    
    // Delete confirmation dialog
    showDeleteConfirmation?.let { balanceId ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = null },
            title = { Text("Delete Balance Record") },
            text = { Text("Are you sure you want to delete this balance record? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteBalance(balanceId)
                        showDeleteConfirmation = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Suppress("DEPRECATION")
@Composable
private fun BalanceHistoryItem(
    balance: AccountBalanceEntity,
    isLatest: Boolean,
    isOnlyRecord: Boolean,
    isExpanded: Boolean,
    editingId: Long?,
    editingValue: String,
    accountPrimaryCurrency: String,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onEditValueChange: (String) -> Unit,
    onSaveEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onToggleExpand: () -> Unit,
    clipboard: androidx.compose.ui.platform.ClipboardManager
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = if (isLatest) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isLatest) 2.dp else 1.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            // Header with date and badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Date with icon
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = balance.timestamp.format(
                                DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a")
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Badges row
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Current badge
                        if (isLatest) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = "CURRENT",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        
                        // Source type badge
                        val (sourceIcon, sourceText, sourceColor) = when (balance.sourceType) {
                            "TRANSACTION" -> Triple("📱", "Transaction", MaterialTheme.colorScheme.tertiary)
                            "SMS_BALANCE" -> Triple("💬", "Balance SMS", MaterialTheme.colorScheme.secondary)
                            "CARD_LINK" -> Triple("💳", "Card Link", MaterialTheme.colorScheme.primary)
                            "MANUAL" -> Triple("✏️", "Manual", MaterialTheme.colorScheme.onSurfaceVariant)
                            else -> if (balance.transactionId != null) 
                                Triple("📱", "Transaction", MaterialTheme.colorScheme.tertiary) 
                            else 
                                Triple("", "", MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        
                        if (sourceText.isNotEmpty()) {
                            Surface(
                                color = sourceColor.copy(alpha = 0.12f),
                                shape = MaterialTheme.shapes.small
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(sourceIcon, style = MaterialTheme.typography.labelSmall)
                                    Text(
                                        text = sourceText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = sourceColor
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Action buttons
                if (editingId != balance.id && !isOnlyRecord) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = onEditClick,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
            
            // Divider
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                thickness = 0.5.dp
            )
            
            // Balance display or edit field
            if (editingId == balance.id) {
                // Edit mode
                Column(
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    OutlinedTextField(
                        value = editingValue,
                        onValueChange = onEditValueChange,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("New Balance") },
                        leadingIcon = {
                            Text(
                                text = CurrencyFormatter.getCurrencySymbol(accountPrimaryCurrency),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                    
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Button(
                            onClick = onSaveEdit,
                            enabled = editingValue.toBigDecimalOrNull() != null,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save")
                        }
                        OutlinedButton(
                            onClick = onCancelEdit,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cancel")
                        }
                    }
                }
            } else {
                // Display mode - Balance
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Balance",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyFormatter.formatCurrency(balance.balance, accountPrimaryCurrency),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isLatest) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }
            
            // SMS Source (if available)
            balance.smsSource?.let { smsSource ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .clickable { onToggleExpand() },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.sm)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Message,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "SMS Source",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (!isExpanded) {
                                        Text(
                                            text = "(${smsSource.length} chars)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                Text(
                                    text = if (isExpanded) smsSource else "${smsSource.take(80)}...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isExpanded) {
                                    IconButton(
                                        onClick = {
                                            clipboard.setText(AnnotatedString(smsSource))
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ContentCopy,
                                            contentDescription = "Copy",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}