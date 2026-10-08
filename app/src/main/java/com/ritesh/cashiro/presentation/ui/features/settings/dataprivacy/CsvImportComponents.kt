package com.ritesh.cashiro.presentation.ui.features.settings.dataprivacy

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ritesh.cashiro.R
import com.ritesh.cashiro.presentation.ui.components.ListItem
import com.ritesh.cashiro.presentation.ui.components.ListItemPosition
import com.ritesh.cashiro.presentation.ui.components.toShape

@Composable
fun CsvImportEntry(onClick: () -> Unit) {
    ListItem(
        headline = { Text(stringResource(R.string.import_csv_statement)) },
        supporting = { Text(stringResource(R.string.import_csv_statement_sub)) },
        trailing = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
        onClick = onClick,
        shape = ListItemPosition.Middle.toShape(),
        padding = PaddingValues(0.dp)
    )
}

@Composable
fun CsvImportProgressDialog(visible: Boolean) {
    if (!visible) return
    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(R.string.import_csv_statement)) },
        text = { Text(stringResource(R.string.csv_import_progress)) },
        confirmButton = {}
    )
}
