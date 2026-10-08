package com.ritesh.cashiro.presentation.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.ritesh.cashiro.R
import com.ritesh.cashiro.data.model.CurrencyPickerOptions
import com.ritesh.cashiro.presentation.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencySelectionBottomSheet(
    selectedCurrency: String,
    availableCurrencies: List<String>,
    onCurrencySelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        CurrencySelectionContent(selectedCurrency, availableCurrencies, onCurrencySelected = {
            onCurrencySelected(it)
            onDismiss()
        })
    }
}

/** Scrollable content is independent of navigation and network state. */
@Composable
internal fun CurrencySelectionContent(
    selectedCurrency: String,
    availableCurrencies: List<String>,
    onCurrencySelected: (String) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    val currencies = remember(availableCurrencies, selectedCurrency) {
        CurrencyPickerOptions.available(availableCurrencies, selectedCurrency)
    }
    val matches = remember(currencies, query) { CurrencyPickerOptions.matching(currencies, query) }
    Column(
        modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f)
            .padding(horizontal = Spacing.lg).padding(bottom = Spacing.lg)
    ) {
        Text(
            text = stringResource(R.string.select_currency),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = Spacing.md).fillMaxWidth()
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text(stringResource(R.string.search_currencies)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear))
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.md)
        )
        if (matches.isEmpty()) {
            Text(
                text = stringResource(R.string.currency_not_available),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = Spacing.md)
            )
        }
        LazyColumn(modifier = Modifier.weight(1f).testTag("currency_picker_list")) {
            itemsIndexed(matches, key = { _, currency -> currency.code }) { index, currency ->
                val selected = currency.code.equals(selectedCurrency.trim(), ignoreCase = true)
                ListItem(
                    headline = { Text(currency.name) },
                    supporting = { Text("${currency.code} · ${currency.symbol}") },
                    trailing = { RadioButton(selected = selected, onClick = null) },
                    selected = selected,
                    onClick = { onCurrencySelected(currency.code) },
                    shape = ListItemPosition.from(index, matches.size).toShape()
                )
            }
        }
    }
}
