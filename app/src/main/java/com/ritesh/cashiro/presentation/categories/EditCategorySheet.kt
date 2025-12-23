package com.ritesh.cashiro.presentation.categories

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.*
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.ritesh.cashiro.R
import com.ritesh.cashiro.data.database.entity.CategoryEntity
import com.ritesh.cashiro.ui.components.ColorPickerContent
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.DpOffset

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EditCategorySheet(
        category: CategoryEntity?,
        onDismiss: () -> Unit,
        onSave: (name: String, description: String, color: String, iconResId: Int, isIncome: Boolean) -> Unit,
        onReset: ((Long) -> Unit)? = null
) {
    var name by remember { mutableStateOf(category?.name ?: "") }
    var description by remember { mutableStateOf(category?.description ?: "") }
    var colorHex by remember { mutableStateOf(category?.color ?: "#33B5E5") }
    var iconResId by remember {
        mutableIntStateOf(category?.iconResId ?: R.drawable.type_food_dining)
    }
    var isIncome by remember { mutableStateOf(category?.isIncome ?: false) }

    var showIconSelector by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
//    val lazyListState = rememberLazyListState()

    if (showIconSelector) {
        ModalBottomSheet(
                onDismissRequest = { showIconSelector = false },
                sheetState = sheetState,
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
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Preview Section

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.large)
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .clickable { showIconSelector = true }
                            .padding(16.dp)
                    ) {

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier =
                                    Modifier.size(64.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Box(
                                    modifier =
                                        Modifier
                                            .size(64.dp)
                                            .clip(MaterialTheme.shapes.large)
                                            .background(
                                                Color(colorHex.toColorInt()).copy(alpha = 0.2f)
                                            ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = iconResId),
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp),
                                        tint = Color.Unspecified
                                    )
                                }

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
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.Start,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = name.ifEmpty { "Category Name" },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = description.ifEmpty { "description about the category" },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onBackground.copy(0.6f)
                                )
                            }

                            IconButton(
                                onClick = {},
                                colors = IconButtonDefaults.iconButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                ),
                                shape = MaterialTheme.shapes.largeIncreased,
                                modifier = Modifier.clip(CircleShape)
                            ) {
                                Icon(
                                    Icons.Default.AddCircle,
                                    contentDescription = "Add Subcategory",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "Preview",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(0.6f)
                    )
                }



                Spacer(modifier = Modifier.height(4.dp))


            // Type SwitchER with TabIndicator effect

                TypeSwitcher(
                    isIncome = isIncome,
                    onTypeChange = { isIncome = it },
                    modifier = Modifier.fillMaxWidth()
                )


                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(
                            text= "Name",
                            fontWeight = FontWeight.SemiBold
                        ) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(
                            lineHeight = 18.sp,
                            fontSize = 16.sp
                        ),
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = 4.dp,
                            bottomEnd = 4.dp
                        ),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                0.7f
                            )
                        )
                    )
                    TextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text(
                            text= "Description",
                            fontWeight = FontWeight.SemiBold
                        ) },
                        placeholder = { Text("e.g., Eating out, Swiggy, Zomato etc.") },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(
                            lineHeight = 18.sp,
                            fontSize = 16.sp
                        ),
                        shape = RoundedCornerShape(
                            topStart = 4.dp,
                            topEnd = 4.dp,
                            bottomStart = 16.dp,
                            bottomEnd = 16.dp
                        ),
                        maxLines = 2,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                0.7f
                            )
                        )
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

            Spacer(modifier = Modifier.height(82.dp))

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
                        onClick = { onSave(name, description, colorHex, iconResId, isIncome) },
                        enabled = name.isNotBlank(),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Text(
                            text = if (category == null) "Create Category" else "Update Category",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
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
                if (category?.isSystem == true && onReset != null) {
                    DropdownMenuItem(
                        text = { Text("Reset") },
                        onClick = {
                            name = category.defaultName ?: category.name
                            description = category.defaultDescription ?: ""
                            colorHex = category.defaultColor ?: category.color
                            iconResId = category.defaultIconResId ?: category.iconResId
                            onReset(category.id)
                        },
                        leadingIcon = { Icon(Icons.Outlined.RestartAlt, contentDescription = null) },
                    )
                }
            }
        }
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
                    modifier
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
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
                        Modifier
                            .offset(x = indicatorOffset)
                            .width(indicatorWidth)
                            .fillMaxHeight()
                            .shadow(2.dp, RoundedCornerShape(18.dp))
                            .clip(RoundedCornerShape(18.dp))
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
                    modifier
                        .fillMaxHeight()
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
