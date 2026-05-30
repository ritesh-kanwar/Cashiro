package com.ritesh.cashiro.presentation.ui.features.add
import androidx.compose.ui.text.style.TextAlign

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.pow
import kotlin.math.roundToLong

@Composable
fun AmountInput(
    amount: String,
    currencySymbol: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    amountFontSize: Dp = 50.dp,
    contentAlignment: Alignment = Alignment.Center,
) {
    // Amount input container
    Box(
        contentAlignment = contentAlignment,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .background(
                color = MaterialTheme.colorScheme.background,
                shape = RoundedCornerShape(10.dp)
            )
            .padding(10.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = currencySymbol,
                fontSize = amountFontSize.value.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (amount.isEmpty() || amount == "0") MaterialTheme.colorScheme.inverseSurface.copy(
                    0.5f
                ) else MaterialTheme.colorScheme.inverseSurface
            )
            AnimatedCounterText(
                amount = amount,
                fontSize = amountFontSize.value.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun AnimatedCounterText(
    amount: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 24.sp,
    maxLines: Int = 1,
    fontWeight: FontWeight? = FontWeight.Normal,
    animationSpec: AnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow
    ),
    textStyle: TextStyle = TextStyle(
        textAlign = TextAlign.Start,
        textMotion = TextMotion.Animated,
    ),
    specialKeys: Set<Char> = setOf('+', '-', '*', '/', '(', ')', '%', '×', '÷'),
    onAnimationComplete: () -> Unit = {},
    enableDynamicSizing: Boolean = true
) {
    // Parse the input amount and clean it up
    val cleanedAmount = if (amount.isEmpty() || amount.any { it in specialKeys } || amount.all { it == '0' }) {
        "0"
    } else {
        val trimmedInput = amount.trimStart('0').ifEmpty { "0" }
        if (trimmedInput.contains('.')) {
            val (integerPart, fractionalPart) = trimmedInput.split('.')
            val cleanedFractionalPart = fractionalPart.trimEnd('0')
            if (cleanedFractionalPart.isEmpty()) {
                integerPart
            } else {
                "$integerPart.$cleanedFractionalPart"
            }
        } else {
            trimmedInput
        }
    }

    // Convert to numeric value for animation
    val targetValue = try {
        cleanedAmount.toFloat()
    } catch (e: NumberFormatException) {
        0f
    }

    // Track first appearance to ensure animation runs on initial display
    var isFirstAppearance by remember { mutableStateOf(true) }

    // Remember the previous amount to force animation when mode changes
    var previousAmount by remember { mutableStateOf("") }

    // Force animation start from zero on first appearance or mode change
    val startValue = if (isFirstAppearance || previousAmount != amount) 0f else targetValue

    // Animation target state with key to force recomposition
    val animatedValue by animateFloatAsState(
        targetValue = startValue,
        animationSpec = animationSpec,
        label = "Counter Animation",
        finishedListener = { onAnimationComplete() }
    )

    // Update tracking states after composition
    LaunchedEffect(amount) {
        if (previousAmount != amount) {
            previousAmount = amount
            if (isFirstAppearance) {
                isFirstAppearance = false
            }
        }
    }

    // Format the animated value properly
    val displayText = formatAnimatedValue(animatedValue, cleanedAmount)

    // Calculate dynamic font size based on display text length
    val dynamicFontSize = remember(displayText, enableDynamicSizing) {
        if (enableDynamicSizing) {
            calculateDynamicFontSizeForAnimatedCounterText(displayText)
        } else {
            fontSize.value.toInt()
        }
    }

    Text(
        text = " $displayText",
        modifier = modifier,
        style = textStyle,
        fontSize = dynamicFontSize.sp,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        fontWeight = fontWeight,
        color = if (amount.isEmpty() || amount == "0")
            MaterialTheme.colorScheme.inverseSurface.copy(0.5f)
        else
            MaterialTheme.colorScheme.inverseSurface
    )
}

// Helper function to format the animated value to match the target format
private fun formatAnimatedValue(value: Float, targetString: String): String {
    val hasDecimal = targetString.contains('.')
    return if (hasDecimal) {
        val decimalPart = targetString.substringAfter('.', "")
        val decimalPlaces = decimalPart.length
        val factor = 10.0.pow(decimalPlaces.toDouble()).toFloat()
        val roundedValue = (value * factor).roundToLong() / factor

        // Special handling for trailing zeros, similar to the reference code
        if (decimalPart.all { it == '0' }) {
            // If all decimal places are zeros, format as integer with commas
            String.format(Locale.US, "%,d", roundedValue.roundToLong())
        } else if (roundedValue.roundToLong().toFloat() != roundedValue) {
            // If there's a meaningful decimal part
            val pattern = "%,.${decimalPlaces}f"
            String.format(Locale.US, pattern, roundedValue)
        } else {
            // Otherwise, round to whole number with commas
            String.format(Locale.US, "%,d", roundedValue.roundToLong())
        }
    } else {
        // Format integer with commas
        String.format(Locale.US, "%,d", value.roundToLong())
    }
}

// Calculate dynamic font size based on amount length
fun calculateDynamicFontSizeForAnimatedCounterText(amount: String): Int {
    // Remove commas and spaces for length calculation
    val cleanAmount = amount.replace(",", "").replace(" ", "")
    val length = cleanAmount.length

    return when {
        length <= 4 -> 50
        length <= 6 -> 45
        length <= 8 -> 38
        length <= 10 -> 32
        length <= 12 -> 26
        length <= 15 -> 22
        length <= 18 -> 18
        else -> 14
    }
}
