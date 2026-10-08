package com.ritesh.cashiro.presentation.ui.features.accounts

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ritesh.cashiro.R
import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity

@Composable
fun DuplicateAccountDialog(
    source: AccountBalanceEntity,
    target: AccountBalanceEntity,
    onMerge: () -> Unit,
    onKeepSeparate: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onKeepSeparate,
        title = { Text(stringResource(R.string.duplicate_account_title)) },
        text = { Text(stringResource(R.string.duplicate_account_message, source.bankName, source.accountLast4, target.accountLast4)) },
        confirmButton = { TextButton(onClick = onMerge) { Text(stringResource(R.string.duplicate_account_merge)) } },
        dismissButton = { TextButton(onClick = onKeepSeparate) { Text(stringResource(R.string.duplicate_account_keep)) } }
    )
}
