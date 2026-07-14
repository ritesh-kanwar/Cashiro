package com.ritesh.cashiro.widget

import android.content.Context
import android.os.Build
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.glance.GlanceTheme
import androidx.glance.color.ColorProvider
import androidx.glance.material3.ColorProviders
import androidx.glance.unit.ColorProvider as SingleColorProvider
import com.ritesh.cashiro.presentation.ui.theme.expense_dark
import com.ritesh.cashiro.presentation.ui.theme.expense_light
import com.ritesh.cashiro.presentation.ui.theme.income_dark
import com.ritesh.cashiro.presentation.ui.theme.income_light
import com.ritesh.cashiro.data.preferences.AccentColor
import com.ritesh.cashiro.data.preferences.ThemeStyle
import com.ritesh.cashiro.presentation.ui.theme.getCustomDarkColorScheme
import com.ritesh.cashiro.presentation.ui.theme.getCustomLightColorScheme

/**
 * Glance color scheme mirroring the app's default (BLUE accent) branded theme from Theme.kt.
 * Used below Android 12; on Android 12+ the widget follows Material You dynamic color,
 * matching the app's default ThemeStyle.DYNAMIC behavior.
 */
private val WidgetLightColorScheme = lightColorScheme(
    primary = Color(0xFF007AFF),
    onPrimary = Color.White,
    secondary = Color(0xFF6866B8),
    onSecondary = Color.White,
    error = Color(0xFFD03859),
    onError = Color.White,
    background = Color(0xFFE2E2E9),
    onBackground = Color(0xFF1A1B20),
    surface = Color(0xFFE5E5EA),
    onSurface = Color(0xFF1A1B20),
    surfaceVariant = Color(0xFFC4C6D0),
    onSurfaceVariant = Color(0xFF44464F),
)

private val WidgetDarkColorScheme = darkColorScheme(
    primary = Color(0xFF007AFF),
    onPrimary = Color.White,
    secondary = Color(0xFF9190E6),
    onSecondary = Color.White,
    error = Color(0xFFFF7878),
    onError = Color.White,
    background = Color(0xFF111318),
    onBackground = Color(0xFFE2E2E9),
    surface = Color(0xFF111318),
    onSurface = Color(0xFFE2E2E9),
    surfaceVariant = Color(0xFF1E1F25),
    onSurfaceVariant = Color(0xFFC4C6D0),
)

private val WidgetAmoledColorScheme = darkColorScheme(
    primary = Color(0xFF007AFF),
    onPrimary = Color.White,
    secondary = Color(0xFF9190E6),
    onSecondary = Color.White,
    error = Color(0xFFFF7878),
    onError = Color.White,
    background = Color.Black,
    onBackground = Color(0xFFE2E2E9),
    surface = Color.Black,
    onSurface = Color(0xFFE2E2E9),
    surfaceVariant = Color(0xFF121212),
    onSurfaceVariant = Color(0xFFC4C6D0),
)

object CashiroWidgetColors {
    val fallback = ColorProviders(
        light = WidgetLightColorScheme,
        dark = WidgetDarkColorScheme,
    )
    val amoled = ColorProviders(
        light = WidgetLightColorScheme,
        dark = WidgetAmoledColorScheme,
    )

    /** Semantic transaction colors from Color.kt, day/night aware. */
    val income: SingleColorProvider = ColorProvider(day = income_light, night = income_dark)
    val expense: SingleColorProvider = ColorProvider(day = expense_light, night = expense_dark)

    /** Soft tinted card backgrounds derived from the semantic colors. */
    val incomeContainer: SingleColorProvider = ColorProvider(
        day = income_light.copy(alpha = 0.12f),
        night = income_dark.copy(alpha = 0.18f),
    )
    val expenseContainer: SingleColorProvider = ColorProvider(
        day = expense_light.copy(alpha = 0.12f),
        night = expense_dark.copy(alpha = 0.18f),
    )
    val netContainer: SingleColorProvider = ColorProvider(
        day = Color.Gray.copy(alpha = 0.12f),
        night = Color.Gray.copy(alpha = 0.18f),
    )
}

@Composable
fun CashiroGlanceTheme(
    context: Context,
    isAmoledMode: Boolean = false,
    themeStyle: ThemeStyle = ThemeStyle.DYNAMIC,
    accentColor: AccentColor = AccentColor.BLUE,
    isDarkThemeEnabled: Boolean? = null,
    content: @Composable () -> Unit
) {
    val supportsDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val lightScheme = when {
        themeStyle == ThemeStyle.DYNAMIC && supportsDynamicColor -> dynamicLightColorScheme(context)
        themeStyle == ThemeStyle.DEFAULT -> getCustomLightColorScheme(accentColor)
        else -> WidgetLightColorScheme
    }
    var darkScheme = when {
        themeStyle == ThemeStyle.DYNAMIC && supportsDynamicColor -> dynamicDarkColorScheme(context)
        themeStyle == ThemeStyle.DEFAULT -> getCustomDarkColorScheme(accentColor)
        else -> WidgetDarkColorScheme
    }
    if (isAmoledMode) {
        darkScheme = darkScheme.copy(
            background = Color.Black,
            surface = Color.Black,
            secondaryContainer = Color.Black,
        )
    }

    val colors = when (isDarkThemeEnabled) {
        true -> ColorProviders(darkScheme)
        false -> ColorProviders(lightScheme)
        null -> ColorProviders(light = lightScheme, dark = darkScheme)
    }
    GlanceTheme(
        colors = colors,
        content = content,
    )
}
