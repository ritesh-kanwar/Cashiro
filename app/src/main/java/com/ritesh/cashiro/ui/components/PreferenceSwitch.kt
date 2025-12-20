package com.ritesh.cashiro.ui.components

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.ritesh.cashiro.R
import com.ritesh.cashiro.ui.effects.BlurredAnimatedVisibility

@Composable
fun PreferenceSwitch(
    visible: Boolean = true,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    showLeadingIcon: Boolean = false,
    leadingIcon: Int? = null,
    isFirst: Boolean = false,
    isLast: Boolean = false,
    isSingle: Boolean = false
) {
    BlurredAnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        ListItem(
            headline = { Text(title) },
            supporting = { Text(subtitle) },
            leading = {
                if (showLeadingIcon) {
                    Icon(
                        painter = painterResource(leadingIcon ?: R.drawable.ic_discord),
                        contentDescription = null
                    )
                }
            },
            trailing = {
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    thumbContent = {
                        Icon(
                            if (checked) Icons.Outlined.Check else Icons.Outlined.Close,
                            "Thumb",
                            modifier = Modifier.size(SwitchDefaults.IconSize),
                        )
                    },
                )
            },
            shape =
                if (isFirst) ListItemPosition.Top.toShape()
                else if (isLast) ListItemPosition.Bottom.toShape()
                else if (isSingle) ListItemPosition.Single.toShape()
                else ListItemPosition.Middle.toShape(),
            padding = listItemPadding
        )
    }
}