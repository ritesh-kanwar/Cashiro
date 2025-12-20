package com.ritesh.cashiro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

enum class ListItemPosition {
    Top,
    Middle,
    Bottom,
    Single;

    companion object {
        fun from(index: Int, size: Int) =
            if (size == 1) Single
            else
                when (index) {
                    0 -> Top
                    size - 1 -> Bottom
                    else -> Middle
                }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
val listTopItemShape
    @Composable
    get() = MaterialTheme.shapes.largeIncreased.copy(
        bottomStart = MaterialTheme.shapes.small.bottomStart,
        bottomEnd = MaterialTheme.shapes.small.bottomEnd
    )

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
val listMiddleItemShape
    @Composable get() = MaterialTheme.shapes.small

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
val listBottomItemShape
    @Composable
    get() = MaterialTheme.shapes.largeIncreased.copy(
        topStart = MaterialTheme.shapes.small.topStart,
        topEnd = MaterialTheme.shapes.small.topEnd
    )

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
val listSingleItemShape
    @Composable get() = MaterialTheme.shapes.largeIncreased

@Composable
fun ListItemPosition.toShape() =
        when (this) {
            ListItemPosition.Top -> listTopItemShape
            ListItemPosition.Middle -> listMiddleItemShape
            ListItemPosition.Bottom -> listBottomItemShape
            ListItemPosition.Single -> listSingleItemShape
        }

val listItemPadding = PaddingValues(horizontal = 16.dp, vertical = 1.5.dp)

@Composable
fun ListItem(
        headline: @Composable () -> Unit,
        modifier: Modifier = Modifier,
        supporting: (@Composable () -> Unit)? = null,
        leading: (@Composable () -> Unit)? = null,
        trailing: (@Composable () -> Unit)? = null,
        selected: Boolean = false,
        shape: CornerBasedShape? = listSingleItemShape,
        padding: PaddingValues = listItemPadding,
        onClick: (() -> Unit)? = null,
        listColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
        selectedListColor: Color = MaterialTheme.colorScheme.primaryContainer
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(padding)
            .then(
                if (shape != null) {
                    Modifier
                        .clip(shape)
                        .background(
                            if (selected) selectedListColor
                            else listColor
                        )
                } else Modifier
            )
            .then(
                if (onClick != null) {
                    Modifier.selectable(
                        selected,
                        true,
                        Role.Button,
                        onClick = onClick
                    )
                } else Modifier
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (leading != null) {
                CompositionLocalProvider(
                        LocalContentColor provides
                                if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.secondary
                ) { Box(contentAlignment = Alignment.Center) { leading() } }
            }

            Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CompositionLocalProvider(
                        LocalContentColor provides
                                if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurface
                ) { ProvideTextStyle(value = MaterialTheme.typography.bodyLarge) { headline() } }

                if (supporting != null) {
                    CompositionLocalProvider(
                            LocalContentColor provides
                                    if (selected)
                                            MaterialTheme.colorScheme.onPrimaryContainer.copy(
                                                    alpha = 0.7f
                                            )
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                    ) {
                        ProvideTextStyle(value = MaterialTheme.typography.bodySmall) {
                            supporting()
                        }
                    }
                }
            }

            if (trailing != null) {
                CompositionLocalProvider(
                        LocalContentColor provides
                                if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurface
                ) { Box(contentAlignment = Alignment.Center) { trailing() } }
            }
        }
    }
}
