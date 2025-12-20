package com.ritesh.cashiro.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import com.ritesh.cashiro.ui.theme.Spacing

@Composable
fun PennyWiseCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    colors: CardColors = CardDefaults.cardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.xs),
            shape = shape,
            colors = colors,
            elevation = elevation
        ) {
            Column(
                modifier = Modifier.padding(Spacing.md)
            ) {
                content()
            }
        }
    } else {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.xs),
            shape = shape,
            colors = colors,
            elevation = elevation
        ) {
            Column(
                modifier = Modifier.padding(Spacing.md)
            ) {
                content()
            }
        }
    }
}