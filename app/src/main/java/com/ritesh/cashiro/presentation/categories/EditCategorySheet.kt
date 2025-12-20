package com.ritesh.cashiro.presentation.categories

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.ritesh.cashiro.R
import com.ritesh.cashiro.data.database.entity.CategoryEntity
import com.ritesh.cashiro.ui.components.ColorPickerContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCategorySheet(
        category: CategoryEntity?,
        onDismiss: () -> Unit,
        onSave: (name: String, color: String, iconResId: Int, isIncome: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(category?.name ?: "") }
    var colorHex by remember { mutableStateOf(category?.color ?: "#33B5E5") }
    var iconResId by remember {
        mutableIntStateOf(category?.iconResId ?: R.drawable.type_food_dining)
    }
    var isIncome by remember { mutableStateOf(category?.isIncome ?: false) }

    var showIconSelector by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (showIconSelector) {
        ModalBottomSheet(
                onDismissRequest = { showIconSelector = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                dragHandle = { BottomSheetDefaults.DragHandle() },
                containerColor = MaterialTheme.colorScheme.surface
        ) {
            CategoryIconSelector(
                    selectedIconId = iconResId,
                    onIconSelected = {
                        iconResId = it
                        showIconSelector = false
                    }
            )
        }
    }

    Column(
            modifier =
                    Modifier.fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Preview Section
        Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                    modifier =
                            Modifier.size(80.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorHex.toColorInt()).copy(alpha = 0.2f))
                                    .clickable { showIconSelector = true },
                    contentAlignment = Alignment.Center
            ) {
                Icon(
                        painter = painterResource(id = iconResId),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = Color.Unspecified
                )

                // Add icon overlay
                Box(
                        modifier =
                                Modifier.align(Alignment.BottomEnd)
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
                    text = name.ifEmpty { "PREVIEW" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(colorHex.toColorInt())
            )
        }

        // Category Details Section
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
            )

            // Type SwitchER with TabIndicator effect
            TypeSwitcher(
                    isIncome = isIncome,
                    onTypeChange = { isIncome = it },
                    modifier = Modifier.fillMaxWidth()
            )
        }

        // Color Picker Section
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

        // Save Button
        Button(
                onClick = { onSave(name, colorHex, iconResId, isIncome) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = name.isNotBlank()
        ) {
            Text(
                    text = if (category == null) "Create Category" else "Update Category",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun TypeSwitcher(
        isIncome: Boolean,
        onTypeChange: (Boolean) -> Unit,
        modifier: Modifier = Modifier
) {
    val themeColors = MaterialTheme.colorScheme

    BoxWithConstraints(
            modifier =
                    modifier.height(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(themeColors.surfaceVariant.copy(alpha = 0.5f))
                            .padding(4.dp)
    ) {
        val maxWidth = maxWidth
        val indicatorWidth = maxWidth / 2
        val indicatorOffset by
                animateDpAsState(
                        targetValue = if (isIncome) indicatorWidth else 0.dp,
                        animationSpec = tween(durationMillis = 300),
                        label = "Type indicator offset"
                )

        // Animated Indicator
        Box(
                modifier =
                        Modifier.offset(x = indicatorOffset)
                                .width(indicatorWidth)
                                .fillMaxHeight()
                                .shadow(2.dp, RoundedCornerShape(12.dp))
                                .clip(RoundedCornerShape(12.dp))
                                .background(themeColors.primary)
        )

        Row(modifier = Modifier.fillMaxSize()) {
            TypeButton(
                    text = "EXPENSE",
                    isSelected = !isIncome,
                    onClick = { onTypeChange(false) },
                    modifier = Modifier.weight(1f)
            )
            TypeButton(
                    text = "INCOME",
                    isSelected = isIncome,
                    onClick = { onTypeChange(true) },
                    modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TypeButton(
        text: String,
        isSelected: Boolean,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
) {
    Box(
            modifier =
                    modifier.fillMaxHeight()
                            .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onClick
                            ),
            contentAlignment = Alignment.Center
    ) {
        Text(
                text = text,
                color =
                        if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
        )
    }
}
