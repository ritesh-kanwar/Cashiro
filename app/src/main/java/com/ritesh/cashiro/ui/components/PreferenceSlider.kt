package com.ritesh.cashiro.ui.components
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ritesh.cashiro.R
import com.ritesh.cashiro.ui.effects.BlurredAnimatedVisibility


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferenceSlider(
    modifier: Modifier = Modifier,
    state: SliderState,
    interactionSource: MutableInteractionSource,
    title: String ,
    visible: Boolean = true,
    isFaded: Boolean = false,
    showReset: Boolean = false,
    resetValue: Float = 0f,
    isFirst: Boolean = false,
    isLast: Boolean = false,
    isSingle: Boolean = false,
    listColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    selectedListColor: Color = MaterialTheme.colorScheme.primaryContainer

) {
    val alpha by animateFloatAsState(
        targetValue = if (!isFaded) 1f else 0.4f,
        label = "width_alpha"
    )
    ListItem(
        headline = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${state.value.toInt()}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.background,
                                shape = MaterialTheme.shapes.small
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        },
        supporting = {
            Row(
                modifier = Modifier.animateContentSize().fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Slider(
                    state = state,
                    modifier = Modifier.weight(1f).fillMaxWidth().height(40.dp),
                    enabled = visible,
                    interactionSource = interactionSource,
                    thumb = {
                        SliderDefaults.Thumb(
                            interactionSource = interactionSource,
                            colors = SliderDefaults.colors(),
                            enabled = visible
                        )
                    },
                    track = { sliderState ->
                        SliderDefaults.Track(
                            colors = SliderDefaults.colors(),
                            enabled = visible,
                            sliderState = sliderState,
                            modifier = Modifier.fillMaxWidth().height(40.dp),
                        )
                    }
                )

                BlurredAnimatedVisibility(showReset && state.value != resetValue) {
                    IconButton(
                        onClick = { state.value = resetValue },
                        modifier = Modifier.weight(1f).padding(start = 8.dp).background(
                            color = MaterialTheme.colorScheme.primary.copy(0.2f),
                            shape = RoundedCornerShape(40.dp)
                        ).size(40.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.primary,
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.RestartAlt,
                            contentDescription = "Reset",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        },
        modifier = modifier.alpha(alpha),
        shape = if (isFirst) ListItemPosition.Top.toShape()
        else if (isLast) ListItemPosition.Bottom.toShape()
        else if (isSingle) ListItemPosition.Single.toShape()
        else ListItemPosition.Middle.toShape(),
        listColor = listColor,
        selectedListColor = selectedListColor,
        padding = listItemPadding
    )
}