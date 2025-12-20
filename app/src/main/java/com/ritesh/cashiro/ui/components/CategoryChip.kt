package com.ritesh.cashiro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
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

@Composable
fun CategoryChip(
    category: CategoryEntity,
    modifier: Modifier = Modifier,
    showText: Boolean = true
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        val iconResId = if (category.iconResId != 0) category.iconResId else R.drawable.type_food_dining
        
        Icon(
            painter = painterResource(id = iconResId),
            contentDescription = null,
            modifier = Modifier
                .size(24.dp)
                .background(
                    color = parseColor(category.color),
                    shape = CircleShape
                )
                .padding(4.dp),
            tint = Color.Unspecified
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Category name
        if (showText) {
            Text(
                text = category.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
        Color(android.graphics.Color.parseColor(cleanColor))
    } catch (e: Exception) {
        // Fallback to grey if color parsing fails
        Color(0xFF757575)
    }
}