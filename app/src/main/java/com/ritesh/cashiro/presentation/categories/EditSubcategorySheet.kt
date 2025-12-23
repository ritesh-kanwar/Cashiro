package com.ritesh.cashiro.presentation.categories

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.ritesh.cashiro.data.database.entity.SubcategoryEntity
import com.ritesh.cashiro.ui.components.ColorPickerContent

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EditSubcategorySheet(
        subcategory: SubcategoryEntity?,
        categoryColor: String,
        categoryIconResId: Int,
        onDismiss: () -> Unit,
        onSave: (name: String, iconResId: Int, color: String) -> Unit,
        onReset: ((Long) -> Unit)? = null,
        onDelete: ((Long) -> Unit)? = null
) {
    var name by remember { mutableStateOf(subcategory?.name ?: "") }
    var colorHex by remember { mutableStateOf(subcategory?.color ?: categoryColor) }
    var iconResId by remember {
        mutableIntStateOf(subcategory?.iconResId ?: categoryIconResId)
    }

    var showIconSelector by remember { mutableStateOf(false) }

    val lazyListState = rememberLazyListState()

    if (showIconSelector) {
        ModalBottomSheet(
                onDismissRequest = { showIconSelector = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                dragHandle = { BottomSheetDefaults.DragHandle() },
                containerColor = MaterialTheme.colorScheme.surface
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

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        LazyColumn(
            modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Preview Section
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    Box(
                        modifier =
                            Modifier.size(80.dp)
                                .clickable { showIconSelector = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier =
                                Modifier.size(80.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorHex.toColorInt()).copy(alpha = 0.2f))
                        )
                        Icon(
                            painter = painterResource(id = iconResId),
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = Color.Unspecified
                        )

                        // Add icon overlay
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(24.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                .border(
                                    2.dp,
                                    MaterialTheme.colorScheme.surface,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    Text(
                        text = name.ifEmpty { "Subcategory" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(colorHex.toColorInt())
                    )
                }
            }

            item {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(
                        text= "Name",
                        fontWeight = FontWeight.SemiBold
                    ) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(
                        lineHeight = 20.sp,
                        fontSize = 16.sp
                    ),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f)
                    )
                )
            }

            // Color Picker Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        ColorPickerContent(
                            initialColor = colorHex.toColorInt(),
                            onColorChanged = { colorInt ->
                                colorHex = String.format("#%06X", 0xFFFFFF and colorInt)
                            }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(62.dp))
            }
        }
        var checked by remember { mutableStateOf(false) }
        // Save Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                ),
            contentAlignment = Alignment.BottomCenter
        ) {

            SplitButtonLayout(
                leadingButton = {
                    SplitButtonDefaults.LeadingButton(
                        onClick = {onSave(name, iconResId, colorHex) },
                        enabled = name.isNotBlank(),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Text(
                            text = if (subcategory == null) "Create Subcategory" else "Update Subcategory",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth(0.5f)
                        )
                    }
                },
                trailingButton = {
                    val description = "Toggle Button"
                    // Icon-only trailing button should have a tooltip for a11y.
                    TooltipBox(
                        positionProvider =
                            TooltipDefaults.rememberTooltipPositionProvider(
                                TooltipAnchorPosition.Above
                            ),
                        tooltip = { PlainTooltip { Text(description) } },
                        state = rememberTooltipState(),
                    ) {
                        SplitButtonDefaults.TrailingButton(
                            checked = checked,
                            onCheckedChange = { checked = it },
                            modifier =
                                Modifier
                                    .height(52.dp)
                                    .semantics {
                                        stateDescription = if (checked) "Expanded" else "Collapsed"
                                        contentDescription = description
                                    },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            val rotation: Float by
                            animateFloatAsState(
                                targetValue = if (checked) 90f else 0f,
                                label = "Trailing Icon Rotation",
                            )
                            Icon(
                                Icons.Filled.MoreVert,
                                modifier =
                                    Modifier
                                        .size(SplitButtonDefaults.TrailingIconSize)
                                        .graphicsLayer {
                                            this.rotationZ = rotation
                                        },
                                contentDescription = "Localized description",
                            )
                        }
                    }
                },
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .height(56.dp),
            )
            DropdownMenu(
                expanded = checked,
                onDismissRequest = { checked = false },
                modifier = Modifier.align(Alignment.BottomCenter),
                offset = DpOffset(50.dp, 0.dp),
                shape = MaterialTheme.shapes.large
            ) {
                if (subcategory?.isSystem == true && onReset != null) {
                    DropdownMenuItem(
                        text = { Text("Reset") },
                        onClick = {
                            // Reset to default values
                            name = subcategory.defaultName ?: subcategory.name
                            colorHex = subcategory.defaultColor ?: categoryColor
                            iconResId = subcategory.defaultIconResId ?: categoryIconResId
                            onReset(subcategory.id)
                        },
                        leadingIcon = { Icon(Icons.Outlined.RestartAlt, contentDescription = null) },
                    )
                }
                if (subcategory != null && onDelete != null) {
                    DropdownMenuItem(
                        text = { Text(
                            text = "Delete Subcategory"
                        ) },
                        onClick = {
                            if (!subcategory.isSystem) {
                                onDelete(subcategory.id)
                            }
                        },
                        leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                        enabled = !subcategory.isSystem
                    )
                }
            }
        }
    }
}
