package com.ritesh.cashiro.presentation.ui.features.accounts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.LocalContext
import com.ritesh.cashiro.presentation.ui.features.categories.IconSelector
import com.ritesh.cashiro.presentation.ui.theme.Dimensions
import com.ritesh.cashiro.presentation.ui.theme.Spacing
import com.ritesh.cashiro.utils.CurrencyFormatter
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.ArrowDropDown
import com.ritesh.cashiro.presentation.ui.components.CurrencyBottomSheet
import androidx.compose.ui.res.stringResource
import com.ritesh.cashiro.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountScreen(
    onNavigateBack: () -> Unit,
    manageAccountsViewModel: ManageAccountsViewModel = hiltViewModel()
) {
    val formState by manageAccountsViewModel.formState.collectAsState()
    var showTypeDropdown by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Dimensions.Padding.content),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        // Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(R.string.add_account_info),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // Error Message
        formState.errorMessage?.let { error ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        // Account Type Dropdown
        ExposedDropdownMenuBox(
            expanded = showTypeDropdown,
            onExpandedChange = { showTypeDropdown = it }
        ) {
            OutlinedTextField(
                value = formState.accountType.name.lowercase()
                    .let { if (it.isNotEmpty()) it.take(1).uppercase() + it.drop(1) else it },
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.account_type_label)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showTypeDropdown) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
                leadingIcon = {
                    Icon(
                        imageVector = when (formState.accountType) {
                            AccountType.SAVINGS, AccountType.CURRENT -> Icons.Default.AccountBalance
                            AccountType.CREDIT -> Icons.Default.CreditCard
                            AccountType.WALLET -> Icons.Default.AccountBalanceWallet
                        },
                        contentDescription = null
                    )
                }
            )

            ExposedDropdownMenu(
                expanded = showTypeDropdown,
                onDismissRequest = { showTypeDropdown = false }
            ) {
                AccountType.values().forEach { type ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                type.name.lowercase().let { if (it.isNotEmpty()) it.take(1).uppercase() + it.drop(1) else it }
                            )
                        },
                        onClick = {
                            manageAccountsViewModel.updateAccountType(type)
                            showTypeDropdown = false
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = when (type) {
                                    AccountType.SAVINGS, AccountType.CURRENT -> Icons.Default.AccountBalance
                                    AccountType.CREDIT -> Icons.Default.CreditCard
                                    AccountType.WALLET -> Icons.Default.AccountBalanceWallet
                                },
                                contentDescription = null
                            )
                        }
                    )
                }
            }
        }
    }

    // Icon Selector
    Text(stringResource(R.string.account_icon_label), style = MaterialTheme.typography.labelMedium)
    val context = LocalContext.current
    Box(modifier = Modifier.height(200.dp).fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small)) {
        IconSelector(
            context = context,
            selectedIconName = formState.iconName,
            onIconSelected = { manageAccountsViewModel.updateIcon(it) }
        )
    }

    // Account Name
    OutlinedTextField(
        value = formState.bankName,
        onValueChange = manageAccountsViewModel::updateBankName,
        label = { Text(stringResource(R.string.account_name_required_label)) },
        placeholder = {
            Text(
                when (formState.accountType) {
                    AccountType.SAVINGS, AccountType.CURRENT -> stringResource(R.string.placeholder_bank_name)
                    AccountType.CREDIT -> stringResource(R.string.placeholder_credit_name)
                    AccountType.WALLET -> stringResource(R.string.placeholder_wallet_name)
                }
            )
        },
        leadingIcon = {
            Icon(Icons.Default.Business, contentDescription = null)
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words
        )
    )

    // Last 4 Digits
    OutlinedTextField(
        value = formState.accountLast4,
        onValueChange = manageAccountsViewModel::updateAccountLast4,
        label = { Text(stringResource(R.string.last_4_digits_required_label)) },
        placeholder = { Text(stringResource(R.string.placeholder_last_4_digits)) },
        leadingIcon = {
            Icon(Icons.Default.Tag, contentDescription = null)
        },
        supportingText = {
            Text(stringResource(R.string.enter_last_4_digits_hint))
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number
        )
    )

    // Current Balance
    OutlinedTextField(
        value = formState.balance,
        onValueChange = manageAccountsViewModel::updateBalance,
        label = { Text(stringResource(R.string.current_balance_required_label)) },
        placeholder = { Text(stringResource(R.string.placeholder_decimal)) },
        leadingIcon = {
            Text(
                CurrencyFormatter.getCurrencySymbol(formState.currency),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 12.dp)
            )
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal
        )
    )

    // Currency Selection
    var showCurrencySheet by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = "${formState.currency} (${CurrencyFormatter.getCurrencySymbol(formState.currency)})",
        onValueChange = {},
        label = { Text(stringResource(R.string.currency_label)) },
        readOnly = true,
        modifier = Modifier.fillMaxWidth(),
        trailingIcon = {
            IconButton(onClick = { showCurrencySheet = true }) {
                Icon(Icons.Default.ArrowDropDown, contentDescription = stringResource(R.string.select_currency))
            }
        },
        leadingIcon = {
            Icon(Icons.Default.Language, contentDescription = null)
        }
    )

    if (showCurrencySheet) {
        CurrencyBottomSheet(
            selectedCurrency = formState.currency,
            onCurrencySelected = {
                manageAccountsViewModel.updateCurrency(it)
                showCurrencySheet = false
            },
            onDismiss = { showCurrencySheet = false }
        )
    }

    // Credit Limit (only for credit cards)
    if (formState.accountType == AccountType.CREDIT) {
        OutlinedTextField(
            value = formState.creditLimit,
            onValueChange = manageAccountsViewModel::updateCreditLimit,
            label = { Text(stringResource(R.string.credit_limit_label)) },
            placeholder = { Text(stringResource(R.string.placeholder_decimal)) },
            leadingIcon = {
                Icon(Icons.Default.CreditScore, contentDescription = null)
            },
            supportingText = {
                Text(stringResource(R.string.credit_limit_hint))
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal
            )
        )
    }

    // Save Button
    Button(
        onClick = {
            manageAccountsViewModel.addAccount()
            if (formState.errorMessage == null) {
                onNavigateBack()
            }
        },
        enabled = formState.isValid,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(stringResource(R.string.save_account))
    }

    // Add some bottom padding for better scroll experience
    Spacer(modifier = Modifier.height(16.dp))
}
