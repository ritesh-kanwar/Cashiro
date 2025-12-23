package com.ritesh.cashiro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ritesh.cashiro.data.database.entity.CategoryEntity
import com.ritesh.cashiro.R
import androidx.core.graphics.toColorInt
import com.ritesh.cashiro.ui.effects.BlurredAnimatedVisibility

@Composable
fun CategoryChip(
    category: CategoryEntity,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    showText: Boolean = true
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        val iconResId = if (category.iconResId != 0) category.iconResId else R.drawable.type_food_dining
        if (onClick != null) {
            IconButton(
                onClick = onClick,
                modifier = Modifier.size(44.dp).
                background(
                    color = parseColor(category.color).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(16.dp)
                )
            ) {
                Icon(
                    painter = painterResource(id = iconResId),
                    contentDescription = null,
                    modifier = Modifier
                        .size(34.dp)
                        .padding(4.dp),
                    tint = Color.Unspecified
                )
            }
        }

        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Category name
        if (showText) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = MaterialTheme.typography.titleMedium.fontWeight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                BlurredAnimatedVisibility(visible = category.description.isNotEmpty()) {
                    Text(
                        text = category.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        fontWeight = MaterialTheme.typography.bodySmall.fontWeight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

        }
    }
}

/**
 * A simple colored dot indicator for a category.
 */
@Composable
fun CategoryDot(
    color: String,
    modifier: Modifier = Modifier,
    size: Int = 8
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .background(
                color = parseColor(color),
                shape = CircleShape
            )
    )
}

/**
 * Overload for displaying category by name and color without entity.
 */
@Composable
fun CategoryChip(
    categoryName: String,
    categoryColor: String,
    modifier: Modifier = Modifier,
    showText: Boolean = true
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        // Colored dot indicator
        CategoryDot(
            color = categoryColor,
            modifier = Modifier.padding(end = if (showText) 6.dp else 0.dp)
        )
        
        // Category name
        if (showText) {
            Text(
                text = categoryName,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Helper function to parse color string to Compose Color.
 * Handles hex colors like "#FF0000" or "FF0000".
 */
private fun parseColor(colorString: String): Color {
    return try {
        val cleanColor = if (colorString.startsWith("#")) colorString else "#$colorString"
        Color(cleanColor.toColorInt())
    } catch (e: Exception) {
        // Fallback to grey if color parsing fails
        Color(0xFF757575)
    }
}