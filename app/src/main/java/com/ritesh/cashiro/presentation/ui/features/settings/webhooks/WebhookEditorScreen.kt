package com.ritesh.cashiro.presentation.ui.features.settings.webhooks

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ritesh.cashiro.data.database.entity.WebhookDataType
import com.ritesh.cashiro.data.database.entity.WebhookRangePreset
import com.ritesh.cashiro.data.webhook.WebhookHeader
import com.ritesh.cashiro.data.webhook.WebhookProfileDraft
import com.ritesh.cashiro.presentation.ui.components.CustomTitleTopAppBar
import com.ritesh.cashiro.presentation.ui.components.ListItem
import com.ritesh.cashiro.presentation.ui.components.ListItemPosition
import com.ritesh.cashiro.presentation.ui.components.SectionHeader
import com.ritesh.cashiro.presentation.ui.components.toShape
import com.ritesh.cashiro.presentation.ui.features.categories.NavigationContent
import com.ritesh.cashiro.presentation.ui.theme.Dimensions
import com.ritesh.cashiro.presentation.ui.theme.Spacing
import dev.chrisbanes.haze.HazeState
import java.time.LocalDate
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WebhookEditorScreen(
    profileId: String?,
    onNavigateBack: () -> Unit,
    viewModel: WebhooksViewModel = hiltViewModel()
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val pinned = TopAppBarDefaults.pinnedScrollBehavior()
    val hazeState = remember { HazeState() }
    var loaded by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("INR") }
    var enabled by remember { mutableStateOf(true) }
    var rangePreset by remember { mutableStateOf(WebhookRangePreset.SINCE_LAST_SUCCESS) }
    var customStart by remember { mutableStateOf("") }
    var customEnd by remember { mutableStateOf("") }
    val selectedTypes = remember { mutableStateListOf(WebhookDataType.SUMMARY, WebhookDataType.TRANSACTIONS) }
    val headers = remember { mutableStateListOf<WebhookHeader>() }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(profileId) {
        val draft = viewModel.loadDraft(profileId)
        name = draft.name
        url = draft.url
        currency = draft.currency
        enabled = draft.enabled
        rangePreset = draft.rangePreset
        customStart = draft.customStart?.toString().orEmpty()
        customEnd = draft.customEnd?.toString().orEmpty()
        selectedTypes.clear()
        selectedTypes.addAll(draft.dataTypes)
        headers.clear()
        headers.addAll(draft.headers.ifEmpty { listOf(WebhookHeader("", "")) })
        loaded = true
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CustomTitleTopAppBar(
                title = if (profileId == null) "New Webhook" else "Edit Webhook",
                scrollBehaviorSmall = pinned,
                scrollBehaviorLarge = scrollBehavior,
                hazeState = hazeState,
                hasBackButton = true,
                navigationContent = { NavigationContent { onNavigateBack() } }
            )
        }
    ) { paddingValues ->
        if (!loaded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = Dimensions.Padding.content,
                    end = Dimensions.Padding.content,
                    top = Dimensions.Padding.content + paddingValues.calculateTopPadding()
                )
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            SectionHeader(title = "Endpoint")
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(1.5.dp)
            ) {
                CashiroTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Profile name",
                    leading = Icons.Rounded.Badge,
                    shape = topFieldShape()
                )
                CashiroTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = "Webhook URL",
                    leading = Icons.Rounded.Link,
                    shape = middleFieldShape()
                )
                CashiroTextField(
                    value = currency,
                    onValueChange = { currency = it.uppercase() },
                    label = "Currency",
                    leading = Icons.Rounded.AttachMoney,
                    shape = bottomFieldShape()
                )
            }

            ListItem(
                headline = {
                    Text(
                        text = "Enabled",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                },
                supporting = {
                    Text(
                        text = "Include this profile in scheduled syncs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailing = {
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                },
                shape = ListItemPosition.Single.toShape(),
                padding = PaddingValues(0.dp)
            )

            Spacer(modifier = Modifier.height(Spacing.xs))
            SectionHeader(title = "Data types")
            Text(
                text = "Select which data is delivered to your endpoint.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                WebhookDataType.entries.forEach { type ->
                    val selected = selectedTypes.contains(type)
                    FilterChip(
                        selected = selected,
                        onClick = {
                            if (selected) selectedTypes.remove(type) else selectedTypes.add(type)
                        },
                        label = { Text(type.name.lowercase()) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xs))
            SectionHeader(title = "Time range")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                WebhookRangePreset.entries.forEach { preset ->
                    FilterChip(
                        selected = rangePreset == preset,
                        onClick = { rangePreset = preset },
                        label = { Text(preset.name.lowercase().replace('_', ' ')) }
                    )
                }
            }

            if (rangePreset == WebhookRangePreset.CUSTOM) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(1.5.dp)
                ) {
                    CashiroTextField(
                        value = customStart,
                        onValueChange = { customStart = it },
                        label = "Custom start",
                        placeholder = "yyyy-MM-dd or yyyy-MM-ddTHH:mm:ss",
                        leading = Icons.Rounded.CalendarMonth,
                        shape = topFieldShape()
                    )
                    CashiroTextField(
                        value = customEnd,
                        onValueChange = { customEnd = it },
                        label = "Custom end",
                        placeholder = "yyyy-MM-dd or yyyy-MM-ddTHH:mm:ss",
                        leading = Icons.Rounded.Event,
                        shape = bottomFieldShape()
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xs))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Icon(
                    Icons.Rounded.Code,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Optional headers",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = { headers.add(WebhookHeader("", "")) },
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier.height(32.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add", style = MaterialTheme.typography.labelMedium)
                }
            }

            headers.forEachIndexed { index, header ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(Dimensions.Padding.content),
                        verticalArrangement = Arrangement.spacedBy(Spacing.md)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            Text(
                                text = "Header ${index + 1}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(
                                onClick = { headers.removeAt(index) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Delete,
                                    contentDescription = "Remove header",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(1.5.dp)
                        ) {
                            CashiroTextField(
                                value = header.key,
                                onValueChange = { headers[index] = header.copy(key = it) },
                                label = "Header name",
                                leading = Icons.Rounded.VpnKey,
                                shape = topFieldShape()
                            )
                            CashiroTextField(
                                value = header.value,
                                onValueChange = { headers[index] = header.copy(value = it) },
                                label = "Header value",
                                leading = Icons.Rounded.Code,
                                shape = bottomFieldShape()
                            )
                        }
                    }
                }
            }

            errorMessage?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(Spacing.sm))
            Button(
                onClick = {
                    val draft = WebhookProfileDraft(
                        id = profileId,
                        name = name,
                        url = url,
                        enabled = enabled,
                        dataTypes = selectedTypes.toSet(),
                        rangePreset = rangePreset,
                        customStart = parseDateTimeInput(customStart),
                        customEnd = parseDateTimeInput(customEnd),
                        currency = currency,
                        headers = headers.filter { it.key.isNotBlank() }
                    )
                    viewModel.saveProfile(
                        draft = draft,
                        onSaved = { onNavigateBack() },
                        onError = { errorMessage = it }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save webhook")
            }

            if (profileId != null) {
                OutlinedButton(
                    onClick = {
                        viewModel.deleteProfile(profileId)
                        onNavigateBack()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Rounded.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text("Delete webhook")
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xl))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CashiroTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    leading: ImageVector? = null,
    trailing: (@Composable () -> Unit)? = null,
    shape: Shape = singleFieldShape(),
    singleLine: Boolean = true,
    keyboardOptions: androidx.compose.foundation.text.KeyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default,
    keyboardActions: androidx.compose.foundation.text.KeyboardActions = androidx.compose.foundation.text.KeyboardActions.Default,
    suffix: (@Composable () -> Unit)? = null
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontWeight = FontWeight.SemiBold) },
        placeholder = placeholder?.let { { Text(it) } },
        leadingIcon = leading?.let {
            { Icon(it, contentDescription = null) }
        },
        trailingIcon = trailing,
        suffix = suffix,
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    )
}

@Composable
internal fun topFieldShape(): Shape = RoundedCornerShape(
    topStart = Dimensions.Radius.md,
    topEnd = Dimensions.Radius.md,
    bottomStart = Dimensions.Radius.xs,
    bottomEnd = Dimensions.Radius.xs
)

@Composable
internal fun middleFieldShape(): Shape = RoundedCornerShape(Dimensions.Radius.xs)

@Composable
internal fun bottomFieldShape(): Shape = RoundedCornerShape(
    topStart = Dimensions.Radius.xs,
    topEnd = Dimensions.Radius.xs,
    bottomStart = Dimensions.Radius.md,
    bottomEnd = Dimensions.Radius.md
)

@Composable
internal fun singleFieldShape(): Shape = RoundedCornerShape(Dimensions.Radius.md)

private fun parseDateTimeInput(value: String): LocalDateTime? {
    if (value.isBlank()) return null
    return runCatching { LocalDateTime.parse(value) }
        .getOrElse {
            runCatching { LocalDate.parse(value).atStartOfDay() }.getOrNull()
        }
}
