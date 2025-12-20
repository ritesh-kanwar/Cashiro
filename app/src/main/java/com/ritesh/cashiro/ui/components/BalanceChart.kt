package com.ritesh.cashiro.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ritesh.cashiro.ui.theme.Spacing
import com.ritesh.cashiro.utils.CurrencyFormatter
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class BalancePoint(
    val timestamp: LocalDateTime,
    val balance: BigDecimal,
    val currency: String = "INR"
)

@Composable
fun BalanceChart(
    primaryCurrency: String,
    balanceHistory: List<BalancePoint>,
    modifier: Modifier = Modifier,
    height: Int = 200
) {
    if (balanceHistory.isEmpty()) return

    // Sort by timestamp to ensure chronological order (oldest to newest)
    val sortedHistory = remember(balanceHistory) {
        balanceHistory.sortedBy { it.timestamp }
    }

    // Apply data smoothing to reduce noise
    val smoothedHistory = remember(sortedHistory) {
        smoothBalanceData(sortedHistory)
    }
    
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
    val backgroundColor = MaterialTheme.colorScheme.surface
    
    // Calculate min and max for scaling using smoothed data
    val maxBalance = smoothedHistory.maxOf { it.balance }
    val minBalance = smoothedHistory.minOf { it.balance }
    val balanceRange = maxBalance - minBalance
    
    // Add some padding to the range
    val paddedMin = minBalance - (balanceRange * BigDecimal("0.1"))
    val paddedMax = maxBalance + (balanceRange * BigDecimal("0.1"))
    val paddedRange = paddedMax - paddedMin
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp),
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md)
        ) {
            // Y-axis labels (max and min balance)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = CurrencyFormatter.formatCurrency(paddedMax, primaryCurrency),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Balance Trend",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Chart Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    
                    // Draw horizontal grid lines
                    val gridLines = 4
                    for (i in 0..gridLines) {
                        val y = canvasHeight * (i.toFloat() / gridLines)
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(canvasWidth, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    }
                    
                    if (smoothedHistory.size < 2) {
                        // Just draw a single point if we only have one data point
                        val point = smoothedHistory.first()
                        val y = canvasHeight * (1f - ((point.balance - paddedMin).toFloat() / paddedRange.toFloat()))
                        drawCircle(
                            color = lineColor,
                            radius = 4.dp.toPx(),
                            center = Offset(canvasWidth / 2, y)
                        )
                    } else {
                        // Draw the line chart
                        val path = Path()
                        val points = mutableListOf<Offset>()

                        smoothedHistory.forEachIndexed { index, point ->
                            val x = canvasWidth * (index.toFloat() / (smoothedHistory.size - 1).coerceAtLeast(1))
                            val y = canvasHeight * (1f - ((point.balance - paddedMin).toFloat() / paddedRange.toFloat()))

                            val offset = Offset(x, y)
                            points.add(offset)

                            if (index == 0) {
                                path.moveTo(x, y)
                            } else {
                                path.lineTo(x, y)
                            }
                        }
                        
                        // Draw gradient fill under the line
                        val fillPath = Path().apply {
                            addPath(path)
                            lineTo(canvasWidth, canvasHeight)
                            lineTo(0f, canvasHeight)
                            close()
                        }
                        
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    lineColor.copy(alpha = 0.3f),
                                    lineColor.copy(alpha = 0.05f)
                                ),
                                startY = 0f,
                                endY = canvasHeight
                            )
                        )
                        
                        // Draw the line
                        drawPath(
                            path = path,
                            color = lineColor,
                            style = Stroke(
                                width = 2.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                        
                        // Draw points
                        points.forEach { point ->
                            drawCircle(
                                color = backgroundColor,
                                radius = 4.dp.toPx(),
                                center = point
                            )
                            drawCircle(
                                color = lineColor,
                                radius = 4.dp.toPx(),
                                center = point,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    }
                }
            }
            
            // X-axis labels (dates) - oldest on left, newest on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (smoothedHistory.isNotEmpty()) {
                    // First (oldest) date on the left
                    Text(
                        text = smoothedHistory.first().timestamp.format(
                            DateTimeFormatter.ofPattern("MMM d")
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (smoothedHistory.size > 1) {
                        // Last (newest) date on the right
                        Text(
                            text = smoothedHistory.last().timestamp.format(
                                DateTimeFormatter.ofPattern("MMM d")
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Min balance label
            Text(
                text = CurrencyFormatter.formatCurrency(paddedMin, primaryCurrency),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Smooth balance data to reduce noise in the chart
 * Applies time-based aggregation and moving average smoothing
 */
private fun smoothBalanceData(
    balanceHistory: List<BalancePoint>,
    maxPoints: Int = 50  // Maximum number of points to display
): List<BalancePoint> {
    if (balanceHistory.size <= maxPoints) {
        return balanceHistory
    }

    // Calculate the time span of the data
    val timeSpan = balanceHistory.last().timestamp.toLocalDate()
        .toEpochDay() - balanceHistory.first().timestamp.toLocalDate().toEpochDay()

    // Determine the aggregation interval based on time span and desired max points
    val intervalDays = maxOf(1, (timeSpan / maxPoints).toInt())

    // Group data by intervals and calculate averages
    val groupedData = balanceHistory.groupBy { point ->
        val dayIndex = (point.timestamp.toLocalDate().toEpochDay() -
            balanceHistory.first().timestamp.toLocalDate().toEpochDay()) / intervalDays
        dayIndex
    }

    return groupedData.map { (_, group) ->
        val avgBalance = group.map { it.balance }.reduce { acc, balance -> acc + balance } /
            BigDecimal(group.size.toLong())
        val middleIndex = group.size / 2
        val representativePoint = group[middleIndex]

        BalancePoint(
            timestamp = representativePoint.timestamp,
            balance = avgBalance,
            currency = representativePoint.currency
        )
    }.sortedBy { it.timestamp }
}