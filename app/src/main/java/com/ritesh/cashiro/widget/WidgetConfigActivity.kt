package com.ritesh.cashiro.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.lifecycle.lifecycleScope
import com.ritesh.cashiro.R
import com.ritesh.cashiro.presentation.ui.theme.CashiroTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

object WidgetConfigKeys {
    val overviewRange = stringPreferencesKey("configured_range")
    val accountFilterKeys = stringSetPreferencesKey("filter_account_keys")
    val categoryFilterKeys = stringSetPreferencesKey("filter_categories")
}

@AndroidEntryPoint
class OverviewWidgetConfigActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appWidgetId = initializeWidgetConfiguration() ?: return

        setContent {
            CashiroTheme {
                OverviewConfigScreen(
                    onConfirm = { selectedRange ->
                        confirmOverviewWidget(appWidgetId, selectedRange)
                    },
                )
            }
        }
    }

    private fun confirmOverviewWidget(appWidgetId: Int, range: OverviewRange) {
        lifecycleScope.launch {
            completeWidgetConfiguration(appWidgetId) { glanceId ->
                updateAppWidgetState(this@OverviewWidgetConfigActivity, glanceId) { prefs ->
                    prefs[WidgetStateKeys.overviewRange] = range.prefValue
                    prefs[WidgetConfigKeys.overviewRange] = range.prefValue
                }
                OverviewWidget().update(this@OverviewWidgetConfigActivity, glanceId)
            }
        }
    }
}

@AndroidEntryPoint
class AccountsWidgetConfigActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appWidgetId = initializeWidgetConfiguration() ?: return

        setContent {
            CashiroTheme {
                AccountsConfigScreen(
                    onConfirm = { accountKeys, categories ->
                        confirmAccountsWidget(appWidgetId, accountKeys, categories)
                    },
                )
            }
        }
    }

    private fun confirmAccountsWidget(
        appWidgetId: Int,
        accountKeys: Set<String>,
        categories: Set<String>,
    ) {
        lifecycleScope.launch {
            completeWidgetConfiguration(appWidgetId) { glanceId ->
                updateAppWidgetState(this@AccountsWidgetConfigActivity, glanceId) { prefs ->
                    if (accountKeys.isNotEmpty()) {
                        prefs[WidgetConfigKeys.accountFilterKeys] = accountKeys
                    } else {
                        prefs.remove(WidgetConfigKeys.accountFilterKeys)
                    }
                    if (categories.isNotEmpty()) {
                        prefs[WidgetConfigKeys.categoryFilterKeys] = categories
                    } else {
                        prefs.remove(WidgetConfigKeys.categoryFilterKeys)
                    }
                }
                AccountsWidget().update(this@AccountsWidgetConfigActivity, glanceId)
            }
        }
    }
}

private fun ComponentActivity.initializeWidgetConfiguration(): Int? {
    setResult(Activity.RESULT_CANCELED)
    enableEdgeToEdge()

    val appWidgetId = intent?.extras?.getInt(
        AppWidgetManager.EXTRA_APPWIDGET_ID,
        AppWidgetManager.INVALID_APPWIDGET_ID,
    ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

    return appWidgetId.takeUnless { it == AppWidgetManager.INVALID_APPWIDGET_ID }
        ?: run {
            finish()
            null
        }
}

private suspend fun ComponentActivity.completeWidgetConfiguration(
    appWidgetId: Int,
    updateWidget: suspend (GlanceId) -> Unit,
) {
    try {
        val glanceId = GlanceAppWidgetManager(this).getGlanceIdBy(appWidgetId)
        updateWidget(glanceId)
        val result = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(Activity.RESULT_OK, result)
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        setResult(Activity.RESULT_CANCELED)
    } finally {
        finish()
    }
}

// ----- Compose Config Screens -----

@Composable
private fun OverviewConfigScreen(onConfirm: (OverviewRange) -> Unit) {
    var selectedRange by remember { mutableStateOf(OverviewRange.ONE_DAY) }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = stringResource(R.string.widget_config_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.widget_config_overview_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = stringResource(R.string.widget_config_select_range),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            OverviewRange.entries.forEach { range ->
                val selected = range == selectedRange
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { selectedRange = range },
                    shape = RoundedCornerShape(16.dp),
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant,
                    // removed tonalElevation since not all material3 versions support it the same way or it might not be needed
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(range.labelRes),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        if (selected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = { onConfirm(selectedRange) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Text(
                    text = stringResource(R.string.widget_config_apply),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccountsConfigScreen(
    onConfirm: (Set<String>, Set<String>) -> Unit,
) {
    var accounts by remember { mutableStateOf<List<AccountItem>>(emptyList()) }
    var categories by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedAccounts by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedCategories by remember { mutableStateOf<Set<String>>(emptySet()) }
    var loading by remember { mutableStateOf(true) }

    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) {
        accounts = runCatching { loadAvailableAccounts(context) }.getOrDefault(emptyList())
        categories = runCatching { loadAvailableCategories(context) }.getOrDefault(emptyList())
        loading = false
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = stringResource(R.string.widget_config_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.widget_config_accounts_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(24.dp))

            if (!loading) {
                // Accounts section
                if (accounts.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.widget_config_filter_accounts),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.widget_config_filter_accounts_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        accounts.forEach { account ->
                            val key = "${account.bankName}::${account.accountLast4}"
                            val selected = key in selectedAccounts
                            FilterChip(
                                label = "${account.bankName} ••${account.accountLast4}",
                                selected = selected,
                                accentColor = runCatching {
                                    Color(android.graphics.Color.parseColor(account.color))
                                }.getOrDefault(MaterialTheme.colorScheme.primary),
                                onClick = {
                                    selectedAccounts = if (selected) {
                                        selectedAccounts - key
                                    } else {
                                        selectedAccounts + key
                                    }
                                },
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Categories section
                if (categories.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.widget_config_filter_categories),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.widget_config_filter_categories_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        categories.forEach { category ->
                            val selected = category in selectedCategories
                            FilterChip(
                                label = category,
                                selected = selected,
                                onClick = {
                                    selectedCategories = if (selected) {
                                        selectedCategories - category
                                    } else {
                                        selectedCategories + category
                                    }
                                },
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { onConfirm(selectedAccounts, selectedCategories) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Text(
                    text = stringResource(R.string.widget_config_apply),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    selected: Boolean,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit,
) {
    val bgColor = if (selected) accentColor.copy(alpha = 0.15f)
    else MaterialTheme.colorScheme.surfaceVariant
    val borderColor = if (selected) accentColor else Color.Transparent
    val textColor = if (selected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor,
        )
    }
}
