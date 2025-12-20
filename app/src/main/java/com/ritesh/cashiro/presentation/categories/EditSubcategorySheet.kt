package com.ritesh.cashiro.presentation.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.ritesh.cashiro.R
import com.ritesh.cashiro.data.database.entity.SubcategoryEntity
import com.ritesh.cashiro.ui.components.ColorPickerContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSubcategorySheet(
        subcategory: SubcategoryEntity?,
        categoryColor: String,
        categoryIconResId: Int,
        onDismiss: () -> Unit,
        onSave: (name: String, iconResId: Int, color: String) -> Unit,
        onReset: ((Long) -> Unit)? = null,
        onDelete: ((Long) -> Unit)? = null
) {
    var name by remember { mutableStateOf(subcategory?.name ?: "") }
    var colorHex by remember { mutableStateOf(subcategory?.color ?: categoryColor) }
    var iconResId by remember {
        mutableIntStateOf(subcategory?.iconResId ?: categoryIconResId)
    }

    var showIconSelector by remember { mutableStateOf(false) }

    if (showIconSelector) {
        ModalBottomSheet(
                onDismissRequest = { showIconSelector = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                dragHandle = { BottomSheetDefaults.DragHandle() },
                containerColor = MaterialTheme.colorScheme.surface
        ) {
            CategoryIconSelector(
                    selectedIconId = iconResId,
                    onIconSelected = {
                        iconResId = it
                        showIconSelector = false
                    }
            )
        }
    }

    Column(
            modifier =
                    Modifier.fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Preview Section
        Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            Box(
                modifier =
                    Modifier.size(80.dp)
                        .clickable { showIconSelector = true },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier =
                        Modifier.size(80.dp)
                            .clip(CircleShape)
                            .background(Color(colorHex.toColorInt()).copy(alpha = 0.2f))
                )
                Icon(
                    painter = painterResource(id = iconResId),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = Color.Unspecified
                )

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
                ){
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            Text(
                text = name.ifEmpty { "PREVIEW" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(colorHex.toColorInt())
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Subcategory Name") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )

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

        // Reset Button (only for system subcategories)
        if (subcategory?.isSystem == true && onReset != null) {
            OutlinedButton(
                    onClick = {
                        // Reset to default values
                        name = subcategory.defaultName ?: subcategory.name
                        colorHex = subcategory.defaultColor ?: categoryColor
                        iconResId = subcategory.defaultIconResId ?: categoryIconResId
                        onReset(subcategory.id)
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                        text = "Reset to Default",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                )
            }
        }

        // Save Button
        Button(
                onClick = { onSave(name, iconResId, colorHex) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = name.isNotBlank()
        ) {
            Text(
                    text = if (subcategory == null) "Create Subcategory" else "Update Subcategory",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
            )
        }

        // Delete Button (only for existing subcategories)
        if (subcategory != null && onDelete != null) {
            OutlinedButton(
                    onClick = { 
                        if (!subcategory.isSystem) {
                            onDelete(subcategory.id)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (subcategory.isSystem) 
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        else 
                            MaterialTheme.colorScheme.error
                    ),
                    enabled = !subcategory.isSystem
            ) {
                Text(
                        text = if (subcategory.isSystem) "Cannot Delete System Item" else "Delete Subcategory",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
