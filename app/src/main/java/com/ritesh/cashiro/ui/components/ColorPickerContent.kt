package com.ritesh.cashiro.ui.components

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ritesh.cashiro.ui.effects.BlurredAnimatedVisibility

@Composable
fun ColorPickerContent(
    initialColor: Int,
    onColorChanged: (Int) -> Unit,
    showCustomOnly: Boolean = false,
    modifier: Modifier = Modifier
) {
    var showColors by remember { mutableStateOf(!showCustomOnly) }
    var selectedColor by remember { mutableIntStateOf(initialColor) }

    // HSV state for custom picker
    val hsv = remember { FloatArray(3) }
    AndroidColor.colorToHSV(initialColor, hsv)
    var hue by remember { mutableFloatStateOf(hsv[0]) }
    var saturation by remember { mutableFloatStateOf(hsv[1]) }
    var value by remember { mutableFloatStateOf(hsv[2]) }

    // Hex input state
    var hexInput by
    remember(selectedColor) {
        mutableStateOf(
            Integer.toHexString(selectedColor).uppercase().padStart(8, '0').takeLast(6)
        )
    }

    // Update selectedColor when HSV changes (in custom mode)
    fun updateColorFromHsv() {
        selectedColor = AndroidColor.HSVToColor(floatArrayOf(hue, saturation, value))
        hexInput = Integer.toHexString(selectedColor).uppercase().padStart(8, '0').takeLast(6)
        onColorChanged(selectedColor)
    }

    // Update HSV from hex input
    fun updateFromHex(hex: String) {
        try {
            if (hex.length == 6) {
                val color = AndroidColor.parseColor("#$hex")
                selectedColor = color
                val newHsv = FloatArray(3)
                AndroidColor.colorToHSV(color, newHsv)
                hue = newHsv[0]
                saturation = newHsv[1]
                value = newHsv[2]
                onColorChanged(selectedColor)
            }
        } catch (_: Exception) {
            // Invalid hex, ignore
        }
    }

    val presetColors = remember {
        listOf(
            Color(0xFF33B5E5), // Cyan (default)
            Color(0xFFF44336), // Red
            Color(0xFFE91E63), // Pink
            Color(0xFFFF4081), // Pink A200
            Color(0xFF9C27B0), // Purple
            Color(0xFF673AB7), // Deep Purple
            Color(0xFF3F51B5), // Indigo
            Color(0xFF2196F3), // Blue
            Color(0xFF03A9F4), // Light Blue
            Color(0xFF00BCD4), // Cyan
            Color(0xFF009688), // Teal
            Color(0xFF4CAF50), // Green
            Color(0xFF8BC34A), // Light Green
            Color(0xFFCDDC39), // Lime
            Color(0xFFFFEB3B), // Yellow
            Color(0xFFFFC107), // Amber
            Color(0xFFFF9800), // Orange
            Color(0xFFFF5722), // Deep Orange
            Color(0xFF795548), // Brown
            Color(0xFF607D8B), // Blue Grey
            Color(0xFF9E9E9E), // Grey
        )
    }

    val shadeColors =
        remember(selectedColor) {
            val baseHsv = FloatArray(3)
            AndroidColor.colorToHSV(selectedColor, baseHsv)
            listOf(0.2f, 0.4f, 0.6f, 0.8f, 1.0f).map { v ->
                Color(AndroidColor.HSVToColor(floatArrayOf(baseHsv[0], baseHsv[1], v)))
            }
        }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (showColors) "Presets" else "Custom Color",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (!showCustomOnly) {
                TextButton(onClick = { showColors = !showColors }) {
                    Text(if (showColors) "Switch to Custom" else "Switch to Presets")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        BlurredAnimatedVisibility(!showColors || showCustomOnly) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Custom Color Picker (HSV)
                // Saturation-Value Panel
                Box(
                    modifier =
                        Modifier.fillMaxWidth()
                            .aspectRatio(1.5f)
                            .clip(RoundedCornerShape(12.dp))
                            .pointerInput(hue) {
                                detectTapGestures { offset ->
                                    saturation = (offset.x / size.width).coerceIn(0f, 1f)
                                    value = 1f - (offset.y / size.height).coerceIn(0f, 1f)
                                    updateColorFromHsv()
                                }
                            }
                            .pointerInput(hue) {
                                detectDragGestures { change, _ ->
                                    change.consume()
                                    saturation = (change.position.x / size.width).coerceIn(0f, 1f)
                                    value = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                                    updateColorFromHsv()
                                }
                            }
                ) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val hueColor = Color(AndroidColor.HSVToColor(floatArrayOf(hue, 1f, 1f)))
                        // White to Hue gradient (horizontal)
                        drawRect(
                            brush = Brush.horizontalGradient(colors = listOf(Color.White, hueColor))
                        )
                        // Transparent to Black gradient (vertical)
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black)
                            )
                        )
                        // Selector circle
                        val selectorX = saturation * size.width
                        val selectorY = (1f - value) * size.height
                        drawCircle(
                            color = Color.White,
                            radius = 8.dp.toPx(),
                            center = Offset(selectorX, selectorY),
                            style = Stroke(width = 2.dp.toPx())
                        )
                        drawCircle(
                            color = Color.Black,
                            radius = 7.dp.toPx(),
                            center = Offset(selectorX, selectorY),
                            style = Stroke(width = 0.5.dp.toPx())
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Hue Bar
                Box(
                    modifier =
                        Modifier.fillMaxWidth()
                            .height(24.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    hue = (offset.x / size.width * 360f).coerceIn(0f, 360f)
                                    updateColorFromHsv()
                                }
                            }
                            .pointerInput(Unit) {
                                detectDragGestures { change, _ ->
                                    change.consume()
                                    hue = (change.position.x / size.width * 360f).coerceIn(0f, 360f)
                                    updateColorFromHsv()
                                }
                            }
                ) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val hueColors = (0..360 step 30).map { h ->
                            Color(AndroidColor.HSVToColor(floatArrayOf(h.toFloat(), 1f, 1f)))
                        }
                        drawRect(brush = Brush.horizontalGradient(colors = hueColors))
                        // Hue selector
                        val selectorX = (hue / 360f) * size.width
                        drawCircle(
                            color = Color.White,
                            radius = 10.dp.toPx(),
                            center = Offset(
                                selectorX.coerceIn(10.dp.toPx(), size.width - 10.dp.toPx()),
                                size.height / 2
                            ),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Color Preview and Hex input
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier.size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(selectedColor))
                    )
                    // Editable Hex Input
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                            .background(
                                MaterialTheme.colorScheme.surfaceContainerHigh,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "#",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        BasicTextField(
                            value = hexInput,
                            onValueChange = { newValue ->
                                val filtered = newValue.uppercase().filter {
                                    it.isDigit() || it in 'A'..'F'
                                }.take(6)
                                hexInput = filtered
                                if (filtered.length == 6) {
                                    updateFromHex(filtered)
                                }
                            },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Ascii,
                                capitalization = KeyboardCapitalization.Characters
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
        BlurredAnimatedVisibility(showColors || !showCustomOnly) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                LazyRow(
//                columns = GridCells.Adaptive(minSize = 48.dp),
//                verticalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(presetColors) { color ->
                        val isSelected = color.toArgb() == selectedColor
                        Box(
                            modifier = Modifier.size(48.dp).aspectRatio(1f)
                                .clip(CircleShape)
                                .background(color)
                                .clickable {
                                    selectedColor = color.toArgb()
                                    onColorChanged(selectedColor)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Shade Row
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    shadeColors.forEach { color ->
                        val isSelected = color.toArgb() == selectedColor
                        Box(
                            modifier = Modifier.size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(color)
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    selectedColor = color.toArgb()
                                    onColorChanged(selectedColor)
                                }
                        )
                    }
                }
            }
        }
    }
}
