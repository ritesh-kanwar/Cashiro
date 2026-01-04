package com.ritesh.cashiro.presentation.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ritesh.cashiro.R
import com.ritesh.cashiro.ui.components.ColorPickerContent
import com.ritesh.cashiro.ui.theme.Dimensions
import com.ritesh.cashiro.ui.theme.Spacing

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EditProfileSheet(
        state: EditProfileState,
        onNameChange: (String) -> Unit,
        onProfileImageChange: (Uri?) -> Unit,
        onBackgroundColorChange: (Color) -> Unit,
        onBannerImageChange: (Uri?) -> Unit,
        onSave: () -> Unit,
        onCancel: () -> Unit
) {

    // Launchers for image picking
    val profileImageLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            if (uri != null) onProfileImageChange(uri)
        }

    val bannerImageLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            if (uri != null) onBannerImageChange(uri)
        }

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(Dimensions.Radius.lg))
                .padding(vertical = Spacing.md)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            Text(
                text = "Edit Profile",
                style = MaterialTheme.typography.titleMediumEmphasized,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = Dimensions.Padding.content)
            )

            // Preview Card
            ProfileCardPreview(
                profileImageUri = state.editedProfileImageUri,
                backgroundColor = state.editedProfileBackgroundColor,
                bannerImageUri = state.editedBannerImageUri,
                modifier = Modifier.padding(horizontal = Dimensions.Padding.content)
            )

            // Name Input
            TextField(
                value = state.editedUserName,
                onValueChange = onNameChange,
                label = { Text("Name", fontWeight = FontWeight.SemiBold) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = Dimensions.Padding.content),
                shape = RoundedCornerShape(Dimensions.Radius.md),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        0.7f
                    )
                ),
                leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null)
                }
            )

            // Profile Image Selection
            Column(
            ) {
                PresetAvatarSelection(
                    selectedUri = state.editedProfileImageUri,
                    onSelect = onProfileImageChange
                )

                Spacer(Modifier.height(Spacing.md))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Dimensions.Padding.content),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Button(
                        onClick = { profileImageLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 4.dp,
                            bottomEnd = 4.dp,
                            bottomStart = 16.dp
                        ),
                        colors = ButtonDefaults.filledTonalButtonColors()
                    ) {
                        Icon(Icons.Rounded.PhotoLibrary, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Gallery")
                    }

                    Button(
                        onClick = { onProfileImageChange(null) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(
                            topStart = 4.dp,
                            topEnd = 16.dp,
                            bottomEnd = 16.dp,
                            bottomStart = 4.dp
                        ),
                        colors = ButtonDefaults.filledTonalButtonColors()
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Clear")
                    }
                }
                Button(
                    onClick = { bannerImageLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Dimensions.Padding.content),
                    shapes = ButtonDefaults.shapes(),
                    colors = ButtonDefaults.filledTonalButtonColors()
                ) {
                    Icon(Icons.Rounded.AddAPhoto, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Change Banner")
                }
            }

            // Background Color Selection
            Box(

                modifier = Modifier
                    .padding(horizontal = Dimensions.Padding.content)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(Dimensions.Radius.md)
                    )
                    .padding(Spacing.md)
            ) {
                ColorPickerContent(
                    initialColor = state.editedProfileBackgroundColor.toArgb(),
                    onColorChanged = { onBackgroundColorChange(Color(it)) }
                )
            }



            Spacer(Modifier.height(160.dp))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            Button(
                onClick = onSave,
                modifier = Modifier
                    .padding(horizontal = Dimensions.Padding.content)
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .height(56.dp),
                shapes = ButtonDefaults.shapes(),
                enabled = state.hasChanges
            ) {
                Icon(Icons.Default.Done, contentDescription = null)
                Spacer(Modifier.width(Spacing.sm))
                Text("Save", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
fun ProfileCardPreview(
    modifier: Modifier = Modifier,
    profileImageUri: Uri?,
    backgroundColor: Color,
    bannerImageUri: Uri?
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(Dimensions.Radius.md))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        // Banner
        if (bannerImageUri != null) {
            AsyncImage(
                model = bannerImageUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.banner_bg_image),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.surface,
                        )
                    )
                )
        )

        // Profile Image
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 16.dp, bottom = 16.dp)
                .size(70.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            if (profileImageUri != null) {
                AsyncImage(
                    model = profileImageUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.avatar_1),
                    contentDescription = "Profile",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@Composable
fun PresetAvatarSelection(selectedUri: Uri?, onSelect: (Uri) -> Unit) {
    val avatars = remember {
        listOf(
            R.drawable.avatar_1,
            R.drawable.avatar_2,
            R.drawable.avatar_3,
            R.drawable.avatar_4,
            R.drawable.avatar_5,
            R.drawable.avatar_6,
            R.drawable.avatar_7,
            R.drawable.avatar_8,
            R.drawable.avatar_9,
            R.drawable.avatar_10
        )
    }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        item { Spacer(Modifier.width(8.dp)) }
        items(avatars) { avatarRes ->
            val avatarUri = Uri.parse("android.resource://com.ritesh.cashiro/$avatarRes")
            val isSelected = selectedUri == avatarUri

            Box(modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerLow
                )
                .border(
                    width = if (isSelected) 3.dp else 0.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else Color.Transparent,
                    shape = CircleShape
                )
                .clickable { onSelect(avatarUri) },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = avatarRes),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
        }
        item { Spacer(Modifier.width(16.dp)) }
    }
}
