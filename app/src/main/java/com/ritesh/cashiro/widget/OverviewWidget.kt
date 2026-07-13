package com.ritesh.cashiro.widget

import android.content.Context
import kotlinx.coroutines.flow.first
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.ritesh.cashiro.MainActivity
import com.ritesh.cashiro.R
import com.ritesh.cashiro.data.preferences.ThemeStyle
import com.ritesh.cashiro.utils.CurrencyFormatter
import java.math.BigDecimal

object WidgetStateKeys {
    val amountsHidden = booleanPreferencesKey("amounts_hidden")
    val overviewRange = stringPreferencesKey("overview_range")
    val refreshToken = longPreferencesKey("refresh_token")
}

const val MASKED_AMOUNT = "••••••"

internal enum class WidgetAccessState {
    CHECKING,
    LOCKED,
    UNLOCKED,
}

internal sealed interface WidgetContentState<out T> {
    data object Loading : WidgetContentState<Nothing>
    data class Data<T>(val value: T) : WidgetContentState<T>
    data object Error : WidgetContentState<Nothing>
}

class OverviewWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(250.dp, 60.dp),
            DpSize(250.dp, 110.dp),
            DpSize(250.dp, 180.dp),
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = widgetEntryPoint(context)
        val prefs = entryPoint.userPreferencesRepository()
        val initialUserPrefs = runCatching { prefs.userPreferences.first() }.getOrNull()

        provideContent {
            val userPrefs by prefs.userPreferences.collectAsState(initial = initialUserPrefs)
            CashiroGlanceTheme(
                context = context,
                isAmoledMode = userPrefs?.isAmoledMode ?: false,
                themeStyle = userPrefs?.themeStyle ?: ThemeStyle.DYNAMIC,
                accentColor = userPrefs?.accentColor ?: com.ritesh.cashiro.data.preferences.AccentColor.BLUE,
                isDarkThemeEnabled = userPrefs?.isDarkThemeEnabled,
            ) {
                OverviewContent(context)
            }
        }
    }
}

class OverviewWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = OverviewWidget()
}

@Composable
private fun OverviewContent(context: Context) {
    val prefs = currentState<Preferences>()
    val hidden = prefs[WidgetStateKeys.amountsHidden] ?: false
    // Use the configured range from widget settings, fallback to stored range or default
    val configuredRange = prefs[WidgetConfigKeys.overviewRange]
    val range = OverviewRange.fromPrefValue(configuredRange ?: prefs[WidgetStateKeys.overviewRange])
    val refreshToken = prefs[WidgetStateKeys.refreshToken] ?: 0L

    val accessState by produceState(
        initialValue = WidgetAccessState.CHECKING,
        refreshToken,
    ) {
        value = resolveWidgetAccessState(context)
    }

    val contentState by produceState<WidgetContentState<OverviewData>>(
        initialValue = WidgetContentState.Loading,
        accessState,
        refreshToken,
    ) {
        if (accessState == WidgetAccessState.UNLOCKED) {
            value = runCatching { loadOverviewData(context) }
                .fold(
                    onSuccess = { WidgetContentState.Data(it) },
                    onFailure = { WidgetContentState.Error },
                )
        }
    }

    val compact = LocalSize.current.height < 90.dp

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(24.dp)
            .padding(if (compact) 10.dp else 16.dp)
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        if (compact) {
            CompactOverviewContent(context, hidden, accessState, contentState)
            return@Column
        }
        // Header row
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = context.getString(R.string.app_name),
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            Image(
                provider = ImageProvider(R.drawable.ic_widget_refresh),
                contentDescription = context.getString(R.string.widget_refresh),
                colorFilter = androidx.glance.ColorFilter.tint(GlanceTheme.colors.onSurfaceVariant),
                modifier = GlanceModifier
                    .size(20.dp)
                    .clickable(actionRunCallback<RefreshOverviewAction>()),
            )
            Spacer(modifier = GlanceModifier.width(8.dp))
            PrivacyEyeButton(context, hidden, actionRunCallback<ToggleOverviewHiddenAction>())
        }
        Spacer(modifier = GlanceModifier.height(8.dp))

        if (accessState != WidgetAccessState.UNLOCKED) {
            LockedView(context, modifier = GlanceModifier.fillMaxWidth().defaultWeight())
        } else {
            when (val state = contentState) {
                WidgetContentState.Loading -> Box(
                    modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = GlanceTheme.colors.primary)
                }
                WidgetContentState.Error -> WidgetErrorView(
                    context = context,
                    retryAction = actionRunCallback<RefreshOverviewAction>(),
                    modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                )
                is WidgetContentState.Data -> {
                    val totals = state.value.totalsByRange[range]
                    Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                        val income = totals?.income ?: BigDecimal.ZERO
                        val expense = totals?.expense ?: BigDecimal.ZERO
                        val net = income.subtract(expense)
                        val currency = state.value.currency

                        StatCard(
                            label = context.getString(R.string.widget_income),
                            amount = income,
                            currency = currency,
                            color = CashiroWidgetColors.income,
                            container = CashiroWidgetColors.incomeContainer,
                            iconRes = R.drawable.ic_widget_arrow_income,
                            hidden = hidden,
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        )
                        Spacer(modifier = GlanceModifier.width(6.dp))
                        StatCard(
                            label = context.getString(R.string.widget_spent),
                            amount = expense,
                            currency = currency,
                            color = CashiroWidgetColors.expense,
                            container = CashiroWidgetColors.expenseContainer,
                            iconRes = R.drawable.ic_widget_arrow_expense,
                            hidden = hidden,
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                        )
                        Spacer(modifier = GlanceModifier.width(6.dp))
                        StatCard(
                            label = context.getString(R.string.widget_net),
                            amount = net.abs(),
                            currency = currency,
                            color = if (net >= BigDecimal.ZERO) CashiroWidgetColors.income else CashiroWidgetColors.expense,
                            container = CashiroWidgetColors.netContainer,
                            iconRes = R.drawable.ic_widget_net,
                            hidden = hidden,
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                            prefix = if (net >= BigDecimal.ZERO) "+" else "-",
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactOverviewContent(
    context: Context,
    hidden: Boolean,
    accessState: WidgetAccessState,
    contentState: WidgetContentState<OverviewData>,
) {
    Row(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = context.getString(R.string.app_name),
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        Spacer(modifier = GlanceModifier.defaultWeight())
        when {
            accessState != WidgetAccessState.UNLOCKED -> Text(
                text = context.getString(R.string.widget_locked_title),
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
            )
            contentState is WidgetContentState.Data -> {
                val totals = contentState.value.totalsByRange[OverviewRange.ONE_DAY]
                CompactAmount(
                    amount = totals?.income ?: BigDecimal.ZERO,
                    currency = contentState.value.currency,
                    hidden = hidden,
                    iconRes = R.drawable.ic_widget_arrow_income,
                    contentDescription = context.getString(R.string.type_income),
                    color = CashiroWidgetColors.income,
                )
                Spacer(modifier = GlanceModifier.width(8.dp))
                CompactAmount(
                    amount = totals?.expense ?: BigDecimal.ZERO,
                    currency = contentState.value.currency,
                    hidden = hidden,
                    iconRes = R.drawable.ic_widget_arrow_expense,
                    contentDescription = context.getString(R.string.type_expense),
                    color = CashiroWidgetColors.expense,
                )
            }
            contentState == WidgetContentState.Error -> Text(
                text = context.getString(R.string.widget_load_error_short),
                style = TextStyle(color = GlanceTheme.colors.error, fontSize = 12.sp),
            )
            else -> CircularProgressIndicator(
                color = GlanceTheme.colors.primary,
                modifier = GlanceModifier.size(18.dp),
            )
        }
        Spacer(modifier = GlanceModifier.width(8.dp))
        Image(
            provider = ImageProvider(R.drawable.ic_widget_refresh),
            contentDescription = context.getString(R.string.widget_refresh),
            colorFilter = androidx.glance.ColorFilter.tint(GlanceTheme.colors.onSurfaceVariant),
            modifier = GlanceModifier
                .size(20.dp)
                .clickable(actionRunCallback<RefreshOverviewAction>()),
        )
        Spacer(modifier = GlanceModifier.width(8.dp))
        PrivacyEyeButton(context, hidden, actionRunCallback<ToggleOverviewHiddenAction>())
    }
}

@Composable
private fun CompactAmount(
    amount: BigDecimal,
    currency: String,
    hidden: Boolean,
    iconRes: Int,
    contentDescription: String,
    color: ColorProvider,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            provider = ImageProvider(iconRes),
            contentDescription = contentDescription,
            colorFilter = androidx.glance.ColorFilter.tint(color),
            modifier = GlanceModifier.size(16.dp),
        )
        Spacer(modifier = GlanceModifier.width(3.dp))
        Text(
            text = if (hidden) MASKED_AMOUNT else formatCompactCurrency(amount, currency),
            style = TextStyle(
                color = color,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            ),
            maxLines = 1,
        )
    }
}

@Composable
internal fun LockedView(
    context: Context,
    modifier: GlanceModifier = GlanceModifier,
    compact: Boolean = false,
) {
    Box(
        modifier = modifier.clickable(actionStartActivity<MainActivity>()),
        contentAlignment = Alignment.Center,
    ) {
        if (compact) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    provider = ImageProvider(R.drawable.ic_widget_lock),
                    contentDescription = context.getString(R.string.widget_locked_title),
                    colorFilter = androidx.glance.ColorFilter.tint(GlanceTheme.colors.onSurfaceVariant),
                    modifier = GlanceModifier.size(24.dp),
                )
                Spacer(modifier = GlanceModifier.width(8.dp))
                Text(
                    text = context.getString(R.string.widget_locked_title),
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        } else Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                provider = ImageProvider(R.drawable.ic_widget_lock),
                contentDescription = context.getString(R.string.widget_locked_title),
                colorFilter = androidx.glance.ColorFilter.tint(GlanceTheme.colors.onSurfaceVariant),
                modifier = GlanceModifier.size(28.dp),
            )
            Spacer(modifier = GlanceModifier.height(6.dp))
            Text(
                text = context.getString(R.string.widget_locked_title),
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Spacer(modifier = GlanceModifier.height(2.dp))
            Text(
                text = context.getString(R.string.widget_locked_subtitle),
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 11.sp,
                ),
            )
        }
    }
}

@Composable
internal fun WidgetErrorView(
    context: Context,
    retryAction: androidx.glance.action.Action,
    modifier: GlanceModifier = GlanceModifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = context.getString(R.string.widget_load_error),
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
            )
            Spacer(modifier = GlanceModifier.height(6.dp))
            Text(
                text = context.getString(R.string.widget_retry),
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                ),
                modifier = GlanceModifier.clickable(retryAction).padding(6.dp),
            )
        }
    }
}

@Composable
internal fun PrivacyEyeButton(
    context: Context,
    hidden: Boolean,
    onToggle: androidx.glance.action.Action,
) {
    Image(
        provider = ImageProvider(
            if (hidden) R.drawable.ic_widget_eye_off else R.drawable.ic_widget_eye
        ),
        contentDescription = context.getString(
            if (hidden) R.string.widget_show_amounts else R.string.widget_hide_amounts
        ),
        colorFilter = androidx.glance.ColorFilter.tint(GlanceTheme.colors.onSurfaceVariant),
        modifier = GlanceModifier
            .size(22.dp)
            .clickable(onToggle),
    )
}

internal suspend fun toggleHiddenState(context: Context, glanceId: GlanceId) {
    updateAppWidgetState(context, glanceId) { prefs ->
        prefs[WidgetStateKeys.amountsHidden] = !(prefs[WidgetStateKeys.amountsHidden] ?: false)
    }
}

@Composable
private fun StatCard(
    label: String,
    amount: BigDecimal,
    currency: String,
    color: ColorProvider,
    container: ColorProvider,
    iconRes: Int,
    hidden: Boolean,
    modifier: GlanceModifier,
    prefix: String = "",
) {
    Column(
        modifier = modifier
            .background(container)
            .cornerRadius(16.dp)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                provider = ImageProvider(iconRes),
                contentDescription = null,
                colorFilter = androidx.glance.ColorFilter.tint(color),
                modifier = GlanceModifier.size(14.dp),
            )
            Spacer(modifier = GlanceModifier.width(4.dp))
            Text(
                text = label,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            text = if (hidden) MASKED_AMOUNT else prefix + CurrencyFormatter.formatCurrency(amount, currency),
            style = TextStyle(
                color = color,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            ),
            maxLines = 1,
        )
    }
}

class ToggleOverviewHiddenAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        toggleHiddenState(context, glanceId)
        OverviewWidget().update(context, glanceId)
    }
}

class SetOverviewRangeAction : ActionCallback {
    companion object {
        val rangeParam = ActionParameters.Key<String>("overview_range")
    }

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[WidgetStateKeys.overviewRange] = parameters[rangeParam] ?: OverviewRange.ONE_DAY.prefValue
        }
        OverviewWidget().update(context, glanceId)
    }
}

class RefreshOverviewAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[WidgetStateKeys.refreshToken] = System.currentTimeMillis()
        }
        OverviewWidget().update(context, glanceId)
    }
}
