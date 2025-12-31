package com.ritesh.cashiro.presentation.accounts

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.ritesh.cashiro.R
import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import com.ritesh.cashiro.presentation.categories.IconSelector
import com.ritesh.cashiro.ui.components.ColorPickerContent
import com.ritesh.cashiro.ui.theme.Spacing
import com.ritesh.cashiro.utils.CurrencyFormatter
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EditAccountSheet(
        account: AccountBalanceEntity? = null,
        allAccounts: List<AccountBalanceEntity> = emptyList(),
        onDismiss: () -> Unit,
        onMerge: (AccountBalanceEntity, List<AccountBalanceEntity>, BigDecimal?) -> Unit =
                { _, _, _ ->
                },
        onSave:
                (
                        bankName: String,
                        balance: BigDecimal,
                        accountLast4: String,
                        iconResId: Int,
                        colorHex: String) -> Unit
) {
    var bankName by remember { mutableStateOf(account?.bankName ?: "") }
    var balance by remember { mutableStateOf(account?.balance ?: BigDecimal.ZERO) }
    var accountLast4 by remember { mutableStateOf(account?.accountLast4 ?: "") }
    var iconResId by remember {
        mutableStateOf(
                if (account?.iconResId != 0 && account?.iconResId != null) account.iconResId
                else R.drawable.type_finance_bank
        )
    }
    var colorHex by remember { mutableStateOf("#33B5E5") } // Default color

    var showNumberPad by remember { mutableStateOf(false) }
    var showIconSelector by remember { mutableStateOf(false) }

    // Merge Flow States
    var showMergeSelection by remember { mutableStateOf(false) }
    var showMergeBalanceOption by remember { mutableStateOf(false) }
    var showMergeConfirmation by remember { mutableStateOf(false) }
    var showMergeManualInput by remember { mutableStateOf(false) }

    var selectedMergeAccounts by remember {
        mutableStateOf<List<AccountBalanceEntity>>(emptyList())
    }
    var mergeNewBalance by remember { mutableStateOf<BigDecimal?>(null) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (showNumberPad) {
        ModalBottomSheet(
                onDismissRequest = { showNumberPad = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            NumberPad(
                    initialValue = balance.toString(),
                    onDone = {
                        balance = it.toBigDecimalOrNull() ?: BigDecimal.ZERO
                        showNumberPad = false
                    },
                    title = if (account == null) "Enter Amount" else "Update Amount"
            )
        }
    }

    if (showIconSelector) {
        ModalBottomSheet(
                onDismissRequest = { showIconSelector = false },
                containerColor = MaterialTheme.colorScheme.surface,
                dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            IconSelector(
                    selectedIconId = iconResId,
                    onIconSelected = {
                        iconResId = it
                        showIconSelector = false
                    }
            )
        }
    }

    // Merge Dialogs
    if (showMergeSelection && account != null) {
        MergeAccountSelectionDialog(
                currentAccount = account,
                allAccounts = allAccounts,
                onDismiss = { showMergeSelection = false },
                onNext = { accounts ->
                    selectedMergeAccounts = accounts
                    showMergeSelection = false
                    showMergeBalanceOption = true
                }
        )
    }

    if (showMergeBalanceOption && account != null) {
        MergeBalanceOptionDialog(
                selectedAccounts = selectedMergeAccounts,
                currentAccount = account,
                onDismiss = { showMergeBalanceOption = false },
                onOptionSelected = { option ->
                    showMergeBalanceOption = false
                    when (option) {
                        BalanceMergeOption.SUM -> {
                            mergeNewBalance =
                                    account.balance + selectedMergeAccounts.sumOf { it.balance }
                            showMergeConfirmation = true
                        }
                        BalanceMergeOption.MANUAL -> {
                            showMergeManualInput = true
                        }
                        BalanceMergeOption.NONE -> {
                            mergeNewBalance = null
                            showMergeConfirmation = true
                        }
                    }
                }
        )
    }

    if (showMergeManualInput) {
        ModalBottomSheet(
                onDismissRequest = { showMergeManualInput = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            NumberPad(
                    initialValue = "",
                    onDone = {
                        mergeNewBalance = it.toBigDecimalOrNull() ?: BigDecimal.ZERO
                        showMergeManualInput = false
                        showMergeConfirmation = true
                    },
                    title = "Set Final Balance"
            )
        }
    }

    if (showMergeConfirmation && account != null) {
        MergeConfirmationDialog(
                onDismiss = { showMergeConfirmation = false },
                onConfirm = {
                    onMerge(account, selectedMergeAccounts, mergeNewBalance)
                    showMergeConfirmation = false
                    onDismiss() // Close the edit sheet
                }
        )
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
                modifier =
                        Modifier.fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = Spacing.md, vertical = Spacing.md),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                    text = if (account == null) "Add Account" else "Edit Account",
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    fontWeight = FontWeight.Bold
            )

            // Preview Section
            Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                PreviewAccountCard(
                        bankName = bankName.ifEmpty { "Bank Name" },
                        balance = balance,
                        accountLast4 = accountLast4.ifEmpty { "0000" },
                        iconResId = iconResId,
                        colorHex = colorHex,
                        currency = account?.currency ?: "INR"
                )
                Text(
                        text = "Preview",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            // Input Fields
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    // Icon Button
                    Box(
                            modifier =
                                    Modifier.size(58.dp)
                                            .background(
                                                    color =
                                                            Color(colorHex.toColorInt())
                                                                    .copy(alpha = 0.2f),
                                                    shape = MaterialTheme.shapes.large
                                            )
                                            .clickable { showIconSelector = true },
                            contentAlignment = Alignment.Center
                    ) {
                        Icon(
                                painter = painterResource(id = iconResId),
                                contentDescription = null,
                                modifier = Modifier.size(34.dp),
                                tint = Color.Unspecified // Keep original icon colors as per
                        )
                        // Edit badge
                        Box(
                                modifier =
                                        Modifier.align(Alignment.BottomEnd)
                                                .size(16.dp)
                                                .background(
                                                        MaterialTheme.colorScheme.primary,
                                                        CircleShape
                                                )
                                                .border(
                                                        1.dp,
                                                        MaterialTheme.colorScheme.surface,
                                                        CircleShape
                                                ),
                                contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(10.dp),
                                    tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    // Balance Input (opens NumberPad)
                    Surface(
                            onClick = { showNumberPad = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        Row(
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Leading Icon
                            Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            // Label and Value
                            Column(verticalArrangement = Arrangement.Center) {
                                Text(
                                        text = "Balance",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                        text =
                                                CurrencyFormatter.formatCurrency(
                                                        balance,
                                                        account?.currency ?: "INR"
                                                ),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Bank Name Row
                TextField(
                        value = bankName,
                        onValueChange = { bankName = it },
                        label = { Text("Bank Name", fontWeight = FontWeight.SemiBold) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape =
                                RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = 4.dp,
                                        bottomEnd = 4.dp
                                ),
                        colors =
                                TextFieldDefaults.colors(
                                        focusedContainerColor =
                                                MaterialTheme.colorScheme.surfaceContainerLow,
                                        unfocusedContainerColor =
                                                MaterialTheme.colorScheme.surfaceContainerLow,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                                        unfocusedLabelColor =
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                        0.7f
                                                )
                                ),
                        leadingIcon = {
                            Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null)
                        }
                )

                // Account Number
                TextField(
                        value = accountLast4,
                        onValueChange = {
                            if (it.length <= 4 && it.all { char -> char.isDigit() })
                                    accountLast4 = it
                        },
                        label = {
                            Text("Account Number (Last 4)", fontWeight = FontWeight.SemiBold)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. 1234") },
                        singleLine = true,
                        shape =
                                RoundedCornerShape(
                                        topStart = 4.dp,
                                        topEnd = 4.dp,
                                        bottomStart = 16.dp,
                                        bottomEnd = 16.dp
                                ),
                        colors =
                                TextFieldDefaults.colors(
                                        focusedContainerColor =
                                                MaterialTheme.colorScheme.surfaceContainerLow,
                                        unfocusedContainerColor =
                                                MaterialTheme.colorScheme.surfaceContainerLow,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                                        unfocusedLabelColor =
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                        0.7f
                                                )
                                ),
                        leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Color Picker Section
                Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors =
                                CardDefaults.cardColors(
                                        containerColor =
                                                MaterialTheme.colorScheme.surfaceContainerLow
                                )
                ) {
                    Column(modifier = Modifier.padding(Spacing.md)) {
                        ColorPickerContent(
                                initialColor = colorHex.toColorInt(),
                                onColorChanged = { colorInt ->
                                    colorHex = String.format("#%06X", 0xFFFFFF and colorInt)
                                }
                        )
                    }
                }

                // Merge Button (Only for existing accounts)
                if (account != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                            onClick = { showMergeSelection = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors =
                                    ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.primary
                                    )
                    ) {
                        Icon(Icons.Default.MergeType, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Merge Account")
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }

        // Action Button at Bottom
        Box(
                modifier =
                        Modifier.fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .background(
                                        brush =
                                                Brush.verticalGradient(
                                                        colors =
                                                                listOf(
                                                                        Color.Transparent,
                                                                        MaterialTheme.colorScheme
                                                                                .surface,
                                                                        MaterialTheme.colorScheme
                                                                                .surface
                                                                )
                                                )
                                )
                                .padding(Spacing.md)
        ) {
            Button(
                    onClick = { onSave(bankName, balance, accountLast4, iconResId, colorHex) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shapes = ButtonDefaults.shapes(),
                    enabled = bankName.isNotBlank() && accountLast4.length == 4
            ) {
                Text(
                        text = if (account == null) "Add Account" else "Save Changes",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PreviewAccountCard(
        bankName: String,
        balance: BigDecimal,
        accountLast4: String,
        iconResId: Int,
        colorHex: String,
        currency: String
) {
    Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                    CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(24.dp)
    ) {
        Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Balance Section
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp)) {
                Text(
                        text = "Balance",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                        text = CurrencyFormatter.formatCurrency(balance, currency),
                        style =
                                MaterialTheme.typography.displaySmall.copy(
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Bold
                                ),
                        color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Bottom Bank Info Section
            Box(
                    modifier =
                            Modifier.fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                    .padding(16.dp)
            ) {
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                                text = bankName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                                text = "**** **** **** $accountLast4",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f)
                        )
                    }

                    Surface(
                            shape = CircleShape,
                            color = Color(colorHex.toColorInt()).copy(alpha = 0.1f),
                            modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                    painter = painterResource(id = iconResId),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = Color.Unspecified // Keep original icon colors as per
                            )
                        }
                    }
                }
            }
        }
    }
}
