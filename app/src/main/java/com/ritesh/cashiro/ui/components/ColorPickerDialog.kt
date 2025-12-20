package com.ritesh.cashiro.ui.components


import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

@Composable
fun ColorPickerDialog(
    initialColor: Int,
    onColorSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var showColors by remember { mutableStateOf(true) }
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Select Color",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            ColorPickerContent(
                initialColor = initialColor,
                onColorChanged = { selectedColor = it }
            )
        },
        confirmButton = {
            // Material Expressive Button - Primary action
            Button(
                onClick = { onColorSelected(selectedColor) },
                shape = RoundedCornerShape(50),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
            ) { Text("Select", fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            FilledTonalButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(50),
                colors =
                    ButtonDefaults.filledTonalButtonColors(
                        containerColor =
                            MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
            ) {
                Text("Cancel", fontWeight = FontWeight.SemiBold)
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    )
}
