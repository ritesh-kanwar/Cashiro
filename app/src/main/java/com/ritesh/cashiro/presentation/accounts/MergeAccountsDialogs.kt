package com.ritesh.cashiro.presentation.accounts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ritesh.cashiro.R
import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import com.ritesh.cashiro.ui.theme.Spacing
import com.ritesh.cashiro.utils.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MergeAccountSelectionDialog(
    currentAccount: AccountBalanceEntity,
    allAccounts: List<AccountBalanceEntity>,
    onDismiss: () -> Unit,
    onNext: (List<AccountBalanceEntity>) -> Unit
) {
    // Filter out the current account
    val availableAccounts =
        remember(allAccounts, currentAccount) {
            allAccounts.filter {
                it.accountLast4 != currentAccount.accountLast4 ||
                        it.bankName != currentAccount.bankName
            }
        }

    var selectedAccounts by remember { mutableStateOf(setOf<AccountBalanceEntity>()) }

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
                .padding(bottom = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Text(
                text = "Merge Accounts",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Select accounts to merge into ${currentAccount.bankName} " +
                        "(...${currentAccount.accountLast4}). Selected accounts will be deleted after merging.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LazyColumn(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                items(availableAccounts) { account ->
                    val isSelected = selectedAccounts.contains(account)
                    AccountSelectionItem(
                        account = account,
                        isSelected = isSelected,
                        onClick = {
                            selectedAccounts = if (isSelected) {
                                selectedAccounts - account
                            } else {
                                selectedAccounts + account
                            }
                        }
                    )
                }
            }
            Button(
                onClick = { onNext(selectedAccounts.toList()) },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedAccounts.isNotEmpty()
            ) { Text("Next") }
            Spacer(modifier = Modifier.height(Spacing.md))
        }
    }
}

@Composable
fun AccountSelectionItem(account: AccountBalanceEntity, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(
                    alpha = 0.3f
                )
            else MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        else null
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // Icon
            val iconResId = if (account.iconResId != 0) account.iconResId
            else R.drawable.type_finance_bank
            Icon(
                painter = painterResource(id = iconResId),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = Color.Unspecified
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.bankName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "....${account.accountLast4}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = CurrencyFormatter.formatCurrency(
                    account.balance,
                    account.currency
                ),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Icon(
                imageVector = if (isSelected) Icons.Filled.CheckCircle
                    else Icons.Filled.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

enum class BalanceMergeOption {
        SUM,
        MANUAL,
        NONE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MergeBalanceOptionDialog(
    selectedAccounts: List<AccountBalanceEntity>,
    currentAccount: AccountBalanceEntity,
    onDismiss: () -> Unit,
    onOptionSelected: (BalanceMergeOption) -> Unit
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
                .padding(bottom = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Text(
                text = "Update Balance?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            val totalBalance =
                currentAccount.balance + selectedAccounts.sumOf { it.balance }
            MergeOptionItem(
                title = "Sum available balances",
                description = "New balance: ${CurrencyFormatter.formatCurrency(totalBalance, currentAccount.currency)}",
                icon = Icons.Filled.Calculate,
                onClick = { onOptionSelected(BalanceMergeOption.SUM) }
            )
            MergeOptionItem(
                title = "Manually enter balance",
                description = "Set a custom balance after merge",
                icon = Icons.Filled.Edit,
                onClick = { onOptionSelected(BalanceMergeOption.MANUAL) }
            )
            MergeOptionItem(
                title = "Don't change balance",
                description =
                    "Keep current balance of ${CurrencyFormatter.formatCurrency(currentAccount.balance, currentAccount.currency)}",
                icon = Icons.Filled.Close,
                onClick = { onOptionSelected(BalanceMergeOption.NONE) }
            )
            Spacer(modifier = Modifier.height(Spacing.md))
        }
    }
}

@Composable
fun MergeOptionItem(title: String, description: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        MaterialTheme.colorScheme.secondaryContainer,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun MergeConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.MergeType, contentDescription = null) },
        title = { Text(text = "Final Confirmation") },
        text = {
            Text(
                text = "Merging Accounts: All current and past transactions from the merging bank accounts will now show on the merged bank account." +
                        " The original accounts will be deleted.",
                textAlign = TextAlign.Center
            ) },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Merge Accounts") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
