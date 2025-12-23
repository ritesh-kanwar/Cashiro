package com.ritesh.cashiro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


// Search Box Composable used in Category icon selection bottomSheet
@Composable
fun SearchBarBox(
    modifier: Modifier = Modifier,
    searchQuery: TextFieldValue,
    onSearchQueryChange: (TextFieldValue) -> Unit,
    leadingIcon: @Composable () -> Unit = {},
    trailingIcon: @Composable () -> Unit = {},
    label: @Composable () -> Unit,
) {
    val themeColors = MaterialTheme.colorScheme
    TextField(
        value =  searchQuery,
        onValueChange = onSearchQueryChange,
        placeholder = { label() },
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 0.dp,
                shape = RoundedCornerShape(75.dp)
            )
            .background(themeColors.surfaceContainerLow, shape = RoundedCornerShape(75.dp)),
        singleLine = true,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        textStyle = MaterialTheme.typography.bodyMedium,
        colors = TextFieldDefaults.colors(
            unfocusedPlaceholderColor = themeColors.inverseOnSurface,
            unfocusedIndicatorColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedContainerColor = Color.Transparent,
            focusedLabelColor = themeColors.inverseSurface,
            unfocusedLabelColor = themeColors.inverseSurface,
//            unfocusedTextColor = themeColors.inverseOnSurface.copy(alpha = 0.7f)
        )
    )
}