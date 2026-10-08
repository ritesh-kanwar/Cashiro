package com.ritesh.cashiro.presentation.ui.features.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ritesh.cashiro.R
import com.ritesh.cashiro.utils.BalanceDiscrepancy
import com.ritesh.cashiro.utils.CurrencyFormatter

@Composable
fun BalanceDiscrepancyCard(
    discrepancy: BalanceDiscrepancy,
    isAdding: Boolean,
    onAddAdjustment: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.balance_discrepancy_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.balance_discrepancy_values,
                CurrencyFormatter.formatCurrency(discrepancy.expected, discrepancy.currency),
                CurrencyFormatter.formatCurrency(discrepancy.reported, discrepancy.currency)))
            Text(stringResource(R.string.balance_discrepancy_explanation), style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onAddAdjustment, enabled = !isAdding) {
                Text(stringResource(
                    if (discrepancy.delta.signum() < 0) R.string.balance_adjustment_expense else R.string.balance_adjustment_income,
                    CurrencyFormatter.formatCurrency(discrepancy.delta.abs(), discrepancy.currency)))
            }
        }
    }
}
