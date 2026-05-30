package com.ritesh.cashiro.presentation.ui.features.analytics

import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ritesh.cashiro.presentation.ui.components.BalancePoint
import com.ritesh.cashiro.presentation.ui.components.CategoryIcon
import com.ritesh.cashiro.presentation.effects.BlurredAnimatedVisibility
import com.ritesh.cashiro.presentation.ui.theme.Spacing
import com.ritesh.cashiro.presentation.common.icons.CategoryMapping
import androidx.compose.ui.res.stringResource
import com.ritesh.cashiro.R
import ir.ehsannarmani.compose_charts.ColumnChart
import ir.ehsannarmani.compose_charts.LineChart
import ir.ehsannarmani.compose_charts.PieChart
import ir.ehsannarmani.compose_charts.models.AnimationMode
import ir.ehsannarmani.compose_charts.models.BarProperties
import ir.ehsannarmani.compose_charts.models.Bars
import ir.ehsannarmani.compose_charts.models.DividerProperties
import ir.ehsannarmani.compose_charts.models.DotProperties
import ir.ehsannarmani.compose_charts.models.DrawStyle
import ir.ehsannarmani.compose_charts.models.GridProperties
import ir.ehsannarmani.compose_charts.models.HorizontalIndicatorProperties
import ir.ehsannarmani.compose_charts.models.LabelHelperProperties
import ir.ehsannarmani.compose_charts.models.LabelProperties
import ir.ehsannarmani.compose_charts.models.Line
import ir.ehsannarmani.compose_charts.models.LineProperties
import ir.ehsannarmani.compose_charts.models.Pie
import ir.ehsannarmani.compose_charts.models.StrokeStyle
import ir.ehsannarmani.compose_charts.models.ZeroLineProperties
import com.ritesh.cashiro.presentation.common.TransactionTypeFilter
import com.ritesh.cashiro.utils.CurrencyFormatter
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import com.ritesh.cashiro.presentation.ui.theme.Dimensions
import kotlin.math.abs

@Composable
fun SpendingLineChart(
    data: List<BalancePoint>,
    currency: String,
    typeFilters: Set<TransactionTypeFilter> = setOf(TransactionTypeFilter.EXPENSE)
) {
    if (data.isEmpty()) return
    val themeColors = MaterialTheme.colorScheme
    val suffixMillion = stringResource(R.string.suffix_million)
    val suffixThousand = stringResource(R.string.suffix_thousand)
    val totalLabel = stringResource(R.string.total)

    val cashFlowData = remember(data) { data.map { it.balance.toDouble() } }
    val labels = remember(data) {
        val isYearly = data.size > 1 && data.all { it.timestamp.dayOfYear == 1 }
        val isMonthly = !isYearly && data.all { it.timestamp.dayOfMonth == 1 }
        val spansMultipleYears = if (data.isNotEmpty()) {
            data.first().timestamp.year != data.last().timestamp.year
        } else false

        data.map {
            val date = it.timestamp
            when {
                isYearly -> date.format(DateTimeFormatter.ofPattern("yyyy"))
                isMonthly && spansMultipleYears -> date.format(DateTimeFormatter.ofPattern("MMM yy"))
                isMonthly -> date.format(DateTimeFormatter.ofPattern("MMM"))
                else -> date.format(DateTimeFormatter.ofPattern("dd MMM"))
            }
        }
    }

    LineChart(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .padding(vertical = Spacing.md),
        data = listOf(
            Line(
                label = if (typeFilters.contains(TransactionTypeFilter.ALL) || typeFilters.size > 2) {
                    totalLabel
                } else {
                    typeFilters.joinToString(" & ") { it.label }
                },
                values = cashFlowData,
                color = SolidColor(themeColors.primary),
                firstGradientFillColor = themeColors.primary.copy(alpha = 0.3f),
                secondGradientFillColor = Color.Transparent,
                strokeAnimationSpec = tween(1500, easing = EaseInOutCubic),
                gradientAnimationDelay = 750,
                drawStyle = DrawStyle.Stroke(width = 2.dp),
                curvedEdges = true,
                dotProperties = DotProperties(
                    enabled = true,
                    color = SolidColor(themeColors.primary),
                    strokeWidth = 3.dp,
                    radius = 4.dp,
                    strokeColor = SolidColor(themeColors.surface)
                )
            )
        ),
        dividerProperties = DividerProperties(
            enabled = true,
            xAxisProperties = LineProperties(
                color = SolidColor(themeColors.onSurface.copy(alpha = 0f)),
                thickness = 0.dp
            ),
            yAxisProperties = LineProperties(
                color = SolidColor(themeColors.onSurface.copy(alpha = 0f)),
                thickness = 0.dp
            )
        ),
        indicatorProperties = HorizontalIndicatorProperties(
            enabled = true,
            textStyle = TextStyle.Default.copy(
                fontSize = 10.sp,
                color = themeColors.onSurfaceVariant.copy(0.6f),
                textAlign = TextAlign.Center
            ),
            contentBuilder = { value -> formatPremiumCurrency(value, currency, suffixMillion, suffixThousand) }
        ),
        labelHelperProperties = LabelHelperProperties(
            enabled = true,
            textStyle = TextStyle.Default.copy(
                fontSize = 10.sp,
                color = themeColors.onSurface,
                textAlign = TextAlign.End
            ),
        ),
        labelProperties = LabelProperties(
            enabled = true,
            textStyle = TextStyle.Default.copy(
                fontSize = 10.sp,
                color = themeColors.onSurface.copy(0.6f),
                textAlign = TextAlign.End
            ),
            labels = labels,
            padding = 16.dp,
            rotation = LabelProperties.Rotation(
                mode = LabelProperties.Rotation.Mode.Force,
                degree = -45f
            )
        ),
        zeroLineProperties = ZeroLineProperties(
            enabled = true,
            style = StrokeStyle.Dashed(),
            color = SolidColor(themeColors.onSurface.copy(alpha = 0.1f)),
        ),
        gridProperties = GridProperties(
            enabled = true,
            xAxisProperties = GridProperties.AxisProperties(
                enabled = true,
                style = StrokeStyle.Dashed(),
                color = SolidColor(themeColors.onSurface.copy(alpha = 0.1f))
            ),
            yAxisProperties = GridProperties.AxisProperties(
                enabled = true,
                style = StrokeStyle.Dashed(),
                color = SolidColor(themeColors.onSurface.copy(alpha = 0.1f))
            )
        ),
        animationMode = AnimationMode.Together(delayBuilder = { it * 200L }),
    )
}

@Composable
fun SpendingBarChart(
    data: List<BalancePoint>,
    currency: String,
    typeFilters: Set<TransactionTypeFilter> = setOf(TransactionTypeFilter.EXPENSE)
) {
    if (data.isEmpty()) return
    val themeColors = MaterialTheme.colorScheme
    val suffixMillion = stringResource(R.string.suffix_million)
    val suffixThousand = stringResource(R.string.suffix_thousand)
    val totalLabel = stringResource(R.string.total)

    val columnData = remember(data) {
        val isYearly = data.size > 1 && data.all { it.timestamp.dayOfYear == 1 }
        val isMonthly = !isYearly && data.all { it.timestamp.dayOfMonth == 1 }
        val spansMultipleYears = if (data.isNotEmpty()) {
            data.first().timestamp.year != data.last().timestamp.year
        } else false

        data.map { point ->
            val label = when {
                isYearly -> point.timestamp.format(DateTimeFormatter.ofPattern("yyyy"))
                isMonthly && spansMultipleYears -> point.timestamp.format(DateTimeFormatter.ofPattern("MMM yy"))
                isMonthly -> point.timestamp.format(DateTimeFormatter.ofPattern("MMM"))
                else -> point.timestamp.format(DateTimeFormatter.ofPattern("dd MMM"))
            }
            Bars(
                label = label,
                values = listOf(
                    Bars.Data(
                        label = if (typeFilters.contains(TransactionTypeFilter.ALL) || typeFilters.size > 2) {
                            totalLabel
                        } else {
                            typeFilters.joinToString(" & ") { it.label }
                        },
                        value = point.balance.toDouble(),
                        color = SolidColor(themeColors.primary.copy(alpha = 0.8f))
                    )
                )
            )
        }
    }

    val maxValue = remember(data) { (data.map { it.balance.toDouble() }.maxOrNull() ?: 0.0) * 1.2 }

    ColumnChart(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .padding(vertical = Spacing.md),
        data = columnData,
        maxValue = maxValue,
        barProperties = BarProperties(
            cornerRadius = Bars.Data.Radius.Rectangle(
                topLeft = 6.dp,
                topRight = 6.dp,
                bottomLeft = 6.dp,
                bottomRight = 6.dp
            ),
            spacing = 8.dp,
            thickness = 12.dp
        ),
        dividerProperties = DividerProperties(
            enabled = true,
            xAxisProperties = LineProperties(
                color = SolidColor(themeColors.onSurface.copy(alpha = 0f)),
                thickness = 0.dp
            ),
            yAxisProperties = LineProperties(
                color = SolidColor(themeColors.onSurface.copy(alpha = 0f)),
                thickness = 0.dp
            )
        ),
        indicatorProperties = HorizontalIndicatorProperties(
            enabled = true,
            textStyle = TextStyle.Default.copy(
                fontSize = 10.sp,
                color = themeColors.onSurfaceVariant.copy(0.6f),
                textAlign = TextAlign.Center
            ),
            contentBuilder = { value -> formatPremiumCurrency(value, currency, suffixMillion, suffixThousand) }
        ),
        labelHelperProperties = LabelHelperProperties(
            enabled = true,
            textStyle = TextStyle.Default.copy(
                fontSize = 10.sp,
                color = themeColors.onSurface,
                textAlign = TextAlign.End
            ),
        ),
        labelProperties = LabelProperties(
            enabled = true,
            textStyle = TextStyle.Default.copy(
                fontSize = 10.sp,
                color = themeColors.onSurface.copy(0.6f),
                textAlign = TextAlign.End
            ),
            padding = 16.dp,
            rotation = LabelProperties.Rotation(
                mode = LabelProperties.Rotation.Mode.Force,
                degree = -45f
            )
        ),
        gridProperties = GridProperties(
            enabled = true,
            xAxisProperties = GridProperties.AxisProperties(
                enabled = true,
                style = StrokeStyle.Dashed(),
                color = SolidColor(themeColors.onSurface.copy(alpha = 0.1f))
            ),
            yAxisProperties = GridProperties.AxisProperties(
                enabled = true,
                style = StrokeStyle.Dashed(),
                color = SolidColor(themeColors.onSurface.copy(alpha = 0.1f))
            )
        ),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
    )
}

@Composable
fun CategoryPieChart(
    modifier: Modifier = Modifier,
    categories: List<CategoryData>,
    currency: String
) {
    if (categories.isEmpty()) return

    val total = categories.fold(java.math.BigDecimal.ZERO) { acc, category -> acc + category.amount }.toDouble()
    if (total == 0.0) return

    val pieData = remember(categories) {
        categories.map { category ->
            Pie(
                label = category.name,
                data = category.amount.toDouble(),
                color = CategoryMapping.categories[category.name]?.color ?: Color.Gray,
                selectedColor = (CategoryMapping.categories[category.name]?.color
                    ?: Color.Gray).copy(alpha = 0.8f),
                selected = false,
                scaleAnimEnterSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                colorAnimEnterSpec = tween(500)
            )
        }
    }

    var chartData by remember(pieData) { mutableStateOf(pieData) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(Spacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            PieChart(
                modifier = Modifier.size(160.dp),
                data = chartData,
                onPieClick = { clickedPie ->
                    val pieIndex = chartData.indexOf(clickedPie)
                    chartData = chartData.mapIndexed { index, pie ->
                        pie.copy(selected = index == pieIndex)
                    }
                },
                selectedScale = 1.1f,
                scaleAnimEnterSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                colorAnimEnterSpec = tween(500),
                style = Pie.Style.Stroke(width = 12.dp)
            )

            // Center Icon Overlay
            val selectedPie = chartData.find { it.selected }
            BlurredAnimatedVisibility(
                visible = selectedPie != null,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() +scaleOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                if (selectedPie != null && selectedPie.label != null) {
                    CategoryIcon(
                        category = selectedPie.label!!,
                        size = 32.dp,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(selectedPie.color.copy(alpha = 0.2f))
                            .padding(12.dp)
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(start = Spacing.md)
        ) {
            items(chartData.sortedByDescending { it.data }) { pie ->
                LegendItem(
                    label = pie.label ?: "Unknown",
                    value = pie.data,
                    color = pie.color,
                    isSelected = pie.selected,
                    currency = currency,
                    totalAmount = total,
                    onClick = {
                        val pieIndex = chartData.indexOf(pie)
                        chartData = chartData.mapIndexed { index, p ->
                            p.copy(selected = index == pieIndex)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun LegendItem(
    label: String,
    value: Double,
    color: Color,
    currency: String,
    isSelected: Boolean,
    totalAmount: Double,
    onClick: () -> Unit
) {
    val percentage = (value / totalAmount * 100).toInt()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) else Color.Transparent)
            .padding(6.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = formatPremiumCurrency(value, currency, stringResource(R.string.suffix_million), stringResource(R.string.suffix_thousand)),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.percentage_format, percentage),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun SpendingHeatmap(
    data: List<BalancePoint>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return

    val maxAmount = remember(data) { data.map { it.balance.toDouble() }.maxOrNull() ?: 1.0 }
    val groupedData = remember(data) {
        data.associate { it.timestamp.toLocalDate() to it.balance.toDouble() }
    }
    
    val sortedDates = remember(data) { data.map { it.timestamp.toLocalDate() }.distinct().sorted() }
    val startDate = sortedDates.first().with(java.time.DayOfWeek.MONDAY)
    val endDate = sortedDates.last()
    
    val totalWeeks = ChronoUnit.WEEKS.between(startDate, endDate.plusDays(1)).toInt() + 1
    
    val monthLabels = remember(startDate, endDate) {
        val allMonthStarts = mutableListOf<Pair<Int, String>>()
        var current = startDate
        var lastMonth = -1
        var weekIndex = 0
        
        while (current <= endDate) {
            if (current.monthValue != lastMonth) {
                val formatter = DateTimeFormatter.ofPattern("MMM")
                allMonthStarts.add(weekIndex to current.format(formatter))
                lastMonth = current.monthValue
            }
            current = current.plusWeeks(1)
            weekIndex++
        }

        val filteredLabels = mutableListOf<Pair<Int, String>>()
        for (i in allMonthStarts.indices) {
            val (week, label) = allMonthStarts[i]
            
            if (i == 0 && allMonthStarts.size > 1) {
                val nextWeek = allMonthStarts[1].first
                if (nextWeek - week < 4) continue
            }
            
            if (filteredLabels.isEmpty()) {
                filteredLabels.add(week to label)
            } else {
                val lastAddedWeek = filteredLabels.last().first
                if (week - lastAddedWeek >= 4) {
                    filteredLabels.add(week to label)
                }
            }
        }
        filteredLabels
    }

    val scrollState = rememberScrollState()
    LaunchedEffect(data) {
        scrollState.scrollTo(scrollState.maxValue)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.horizontalScroll(scrollState)
        ) {
            // Heatmap Grid
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                for (w in 0 until totalWeeks) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (d in 0 until 7) {
                            val date = startDate.plusWeeks(w.toLong()).plusDays(d.toLong())
                            val amount = groupedData[date] ?: 0.0
                            val intensity = if (maxAmount > 0) (amount / maxAmount).toFloat().coerceIn(0f, 1f) else 0f
                            
                            val primary = MaterialTheme.colorScheme.primary
                            val color = when {
                                date > endDate -> MaterialTheme.colorScheme.surfaceContainerHigh
                                amount == 0.0 -> MaterialTheme.colorScheme.surfaceContainerHigh
                                intensity < 0.25f -> primary.copy(alpha = 0.25f)
                                intensity < 0.5f -> primary.copy(alpha = 0.5f)
                                intensity < 0.75f -> primary.copy(alpha = 0.75f)
                                else -> primary
                            }

                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(color)
                            )
                        }
                    }
                }
            }

            // Labels
            Box(modifier = Modifier.fillMaxWidth()) {
                monthLabels.forEach { (weekIndex, label) ->
                    val xOffset = (weekIndex * 20).dp // 16.dp size + 4.dp space
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        modifier = Modifier.offset(x = xOffset)
                    )
                }
            }
        }
    }
}

fun formatPremiumCurrency(value: Double, currency: String, suffixMillion: String, suffixThousand: String): String {
    val absValue = abs(value)
    val symbol = CurrencyFormatter.getCurrencySymbol(currency)
    
    return when {
        absValue >= 1_000_000 -> {
            val millions = absValue / 1_000_000
            val suffix = suffixMillion
            "${symbol}${String.format("%.1f", millions)}${suffix}"
        }
        absValue >= 1_000 -> {
            val thousands = absValue / 1_000
            val suffix = suffixThousand
            "${symbol}${String.format("%.1f", thousands)}${suffix}"
        }
        else -> "${symbol}${absValue.toInt()}"
    }
}
