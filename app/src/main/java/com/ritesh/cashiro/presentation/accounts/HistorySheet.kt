package com.ritesh.cashiro.presentation.accounts

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
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
import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import com.ritesh.cashiro.ui.theme.*
import com.ritesh.cashiro.utils.CurrencyFormatter
import java.math.BigDecimal
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HistorySheet(
    bankName: String,
    accountLast4: String,
    balanceHistory: List<AccountBalanceEntity>,
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.md)
    ) {
        // Header
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

        Spacer(modifier = Modifier.height(Spacing.md))

        // Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "${balanceHistory.size} records • Latest balance is shown in accounts",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.md))

        if (balanceHistory.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
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
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                contentPadding = PaddingValues(bottom = Spacing.xl)
            ) {
                items(balanceHistory) { balance ->
                    val isLatest = balance == balanceHistory.first()
                    val isOnlyRecord = balanceHistory.size == 1
                    val isExpanded = expandedSources.contains(balance.id)

                    HistoryRecordItem(
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HistoryRecordItem(
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
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            }
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            // Top Row: Date and Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (editingId != balance.id && !isOnlyRecord) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = onEditClick,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Balance Display or Edit
            if (editingId == balance.id) {
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
                        prefix = {
                            Text(
                                text = CurrencyFormatter.getCurrencySymbol(accountPrimaryCurrency) + " ",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
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
                            Text("Cancel")
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
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

                    // Source Badge
                    val (sourceIcon, sourceText, sourceColor) = when (balance.sourceType) {
                        "TRANSACTION" -> Triple(Icons.Default.SwapHoriz, "Transaction", MaterialTheme.colorScheme.tertiary)
                        "SMS_BALANCE" -> Triple(Icons.AutoMirrored.Filled.Message, "SMS", MaterialTheme.colorScheme.secondary)
                        "CARD_LINK" -> Triple(Icons.Default.CreditCard, "Card Link", MaterialTheme.colorScheme.primary)
                        "MANUAL" -> Triple(Icons.Default.Edit, "Manual", MaterialTheme.colorScheme.onSurfaceVariant)
                        else -> Triple(Icons.Default.Info, "System", MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Surface(
                        color = sourceColor.copy(alpha = 0.1f),
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                sourceIcon,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = sourceColor
                            )
                            Text(
                                text = sourceText.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = sourceColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // SMS Source
            balance.smsSource?.let { smsSource ->
                Surface(
                    onClick = onToggleExpand,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.sm, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "SMS Source",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (isExpanded) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = smsSource,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(
                                onClick = { clipboard.setText(AnnotatedString(smsSource)) },
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "Copy",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
