package com.ritesh.cashiro.widget

import android.content.Context
import kotlinx.coroutines.flow.first
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
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
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
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
import com.ritesh.cashiro.data.database.entity.TransactionType
import com.ritesh.cashiro.data.preferences.ThemeStyle
import com.ritesh.cashiro.presentation.common.icons.CategoryMapping
import com.ritesh.cashiro.presentation.common.icons.IconProvider
import com.ritesh.cashiro.presentation.common.icons.IconResource
import com.ritesh.cashiro.utils.CurrencyFormatter

class AccountsWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(250.dp, 110.dp),
            DpSize(250.dp, 180.dp),
            DpSize(250.dp, 300.dp),
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
                AccountsContent(context)
            }
        }
    }
}

class AccountsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AccountsWidget()
}

@Composable
private fun AccountsContent(context: Context) {
    val prefs = currentState<Preferences>()
    val hidden = prefs[WidgetStateKeys.amountsHidden] ?: false
    val refreshToken = prefs[WidgetStateKeys.refreshToken] ?: 0L

    // Read config filters
    val filterAccountKeys = prefs[WidgetConfigKeys.accountFilterKeys]
    val filterCategories = prefs[WidgetConfigKeys.categoryFilterKeys]

    val accessState by produceState(
        initialValue = WidgetAccessState.CHECKING,
        refreshToken,
    ) {
        value = resolveWidgetAccessState(context)
    }

    val contentState by produceState<WidgetContentState<AccountsWidgetData>>(
        initialValue = WidgetContentState.Loading,
        accessState,
        refreshToken,
    ) {
        if (accessState == WidgetAccessState.UNLOCKED) {
            value = runCatching {
                loadAccountsWidgetData(
                    context = context,
                    filterAccountKeys = filterAccountKeys,
                    filterCategories = filterCategories,
                )
            }.fold(
                onSuccess = { WidgetContentState.Data(it) },
                onFailure = { WidgetContentState.Error },
            )
        }
    }

    val widgetSize = LocalSize.current
    val compact = widgetSize.height < 160.dp
    val maxAccountCards = if (widgetSize.width >= 320.dp) 3 else 2

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(24.dp)
            .padding(if (compact) 12.dp else 16.dp)
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        // Header
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = context.getString(R.string.widget_accounts_title),
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
                    .size(22.dp)
                    .clickable(actionRunCallback<RefreshAccountsWidgetAction>()),
            )
            Spacer(modifier = GlanceModifier.width(8.dp))
            PrivacyEyeButton(context, hidden, actionRunCallback<ToggleAccountsHiddenAction>())
        }
        Spacer(modifier = GlanceModifier.height(12.dp))

        if (accessState != WidgetAccessState.UNLOCKED) {
            LockedView(
                context = context,
                modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                compact = compact,
            )
        } else {
            when (val state = contentState) {
                WidgetContentState.Loading -> Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = GlanceTheme.colors.primary)
                }
                WidgetContentState.Error -> WidgetErrorView(
                    context = context,
                    retryAction = actionRunCallback<RefreshAccountsWidgetAction>(),
                    modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                )
                is WidgetContentState.Data -> {
                    val loaded = state.value
                    if (loaded.accounts.isNotEmpty()) {
                        AccountBalancesRow(context, loaded.accounts, hidden, maxAccountCards)
                        if (!compact) Spacer(modifier = GlanceModifier.height(12.dp))
                    }
                    if (!compact && loaded.transactions.isEmpty()) {
                        Box(
                            modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = context.getString(R.string.widget_no_transactions),
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurfaceVariant,
                                    fontSize = 12.sp,
                                ),
                            )
                        }
                    } else if (!compact) {
                        LazyColumn(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                            items(loaded.transactions) { transaction ->
                                TransactionRow(context, transaction, hidden)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountBalancesRow(
    context: Context,
    accounts: List<AccountItem>,
    hidden: Boolean,
    maxAccounts: Int,
) {
    Row(modifier = GlanceModifier.fillMaxWidth()) {
        accounts.take(maxAccounts).forEachIndexed { index, account ->
            if (index > 0) {
                Spacer(modifier = GlanceModifier.width(8.dp))
            }
            // Deep link: clicking opens account detail
            val accountClickAction = actionRunCallback<OpenAccountDetailAction>(
                androidx.glance.action.actionParametersOf(
                    OpenAccountDetailAction.bankNameParam to account.bankName,
                    OpenAccountDetailAction.accountLast4Param to account.accountLast4,
                )
            )
            // Use account color as tinted background
            val accountBg = try {
                val parsed = android.graphics.Color.parseColor(account.color)
                val alpha40 = Color(parsed).copy(alpha = 0.25f)
                // Convert to a day/night aware provider; Glance doesn't support alpha directly
                // so we use the darker shade for dark mode, lighter for light mode
                val dayColor = Color(parsed).copy(alpha = 0.12f)
                val nightColor = Color(parsed).copy(alpha = 0.22f)
                androidx.glance.color.ColorProvider(
                    day = dayColor,
                    night = nightColor,
                )
            } catch (_: Exception) {
                GlanceTheme.colors.surfaceVariant
            }

            Column(
                modifier = GlanceModifier
                    .defaultWeight()
                    .background(accountBg)
                    .cornerRadius(16.dp)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .clickable(accountClickAction),
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            text = account.bankName,
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 10.sp,
                            ),
                            maxLines = 1,
                        )
                        Text(
                            text = "•• ${account.accountLast4}",
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 10.sp,
                            ),
                            maxLines = 1,
                        )
                    }

                    val iconResource = IconProvider.getIconForTransaction(
                        context = context,
                        merchantName = account.bankName,
                        accountIconResId = account.iconResId,
                        accountIconName = account.iconName
                    )

                    when (iconResource) {
                        is IconResource.DrawableResource -> {
                            Image(
                                provider = ImageProvider(iconResource.resId),
                                contentDescription = account.bankName,
                                modifier = GlanceModifier.size(24.dp),
                            )
                        }
                        is IconResource.TintedResIcon -> {
                            Image(
                                provider = ImageProvider(iconResource.resId),
                                contentDescription = account.bankName,
                                modifier = GlanceModifier.size(24.dp),
                                colorFilter = if (iconResource.tint != Color.Unspecified) {
                                    androidx.glance.ColorFilter.tint(androidx.glance.color.ColorProvider(day = iconResource.tint, night = iconResource.tint))
                                } else null
                            )
                        }
                        else -> {
                            Box(modifier = GlanceModifier.size(24.dp)) {}
                        }
                    }
                }
                Spacer(modifier = GlanceModifier.height(2.dp))
                Text(
                    text = if (hidden) {
                        MASKED_AMOUNT
                    } else {
                        CurrencyFormatter.formatCurrency(account.balance, account.currency)
                    },
                    style = TextStyle(
                        color = if (account.isCreditCard) {
                            CashiroWidgetColors.expense
                        } else {
                            GlanceTheme.colors.onSurface
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun TransactionRow(context: Context, transaction: TransactionItem, hidden: Boolean) {
    // Deep link: clicking opens the specific transaction
    val transactionClickAction = actionRunCallback<OpenTransactionDetailAction>(
        androidx.glance.action.actionParametersOf(
            OpenTransactionDetailAction.transactionIdParam to transaction.id,
        )
    )

    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .clickable(transactionClickAction),
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val iconResource = IconProvider.getIconForTransaction(
                context = context,
                merchantName = transaction.merchant,
                category = transaction.category,
                subcategory = transaction.subcategory
            )
            val brandColor = IconProvider.getColorForTransaction(
                merchantName = transaction.merchant,
                category = transaction.category,
                subcategory = transaction.subcategory
            )

            val bgColor = brandColor?.let { (colorHex, alpha) ->
                try {
                    val parsed = android.graphics.Color.parseColor(colorHex)
                    androidx.glance.color.ColorProvider(
                        day = Color(parsed).copy(alpha = alpha),
                        night = Color(parsed).copy(alpha = alpha),
                    )
                } catch (_: Exception) { null }
            }

            Box(
                modifier = GlanceModifier
                    .size(32.dp)
                    .background(bgColor ?: GlanceTheme.colors.surfaceVariant)
                    .cornerRadius(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                when (iconResource) {
                    is IconResource.DrawableResource -> {
                        Image(
                            provider = ImageProvider(iconResource.resId),
                            contentDescription = transaction.merchant,
                            modifier = GlanceModifier.size(18.dp),
                        )
                    }
                    is IconResource.TintedResIcon -> {
                        Image(
                            provider = ImageProvider(iconResource.resId),
                            contentDescription = transaction.category,
                            modifier = GlanceModifier.size(18.dp),
                            colorFilter = if (iconResource.tint != Color.Unspecified) {
                                androidx.glance.ColorFilter.tint(androidx.glance.color.ColorProvider(day = iconResource.tint, night = iconResource.tint))
                            } else null
                        )
                    }
                    else -> {
                        Text(
                            text = transaction.merchant.take(1).uppercase(),
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                            ),
                        )
                    }
                }
            }

            Spacer(modifier = GlanceModifier.width(10.dp))
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = transaction.merchant,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    maxLines = 1,
                )
                Text(
                    text = transaction.category + " • " + transaction.timeLabel,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 11.sp,
                    ),
                    maxLines = 1,
                )
            }
            Spacer(modifier = GlanceModifier.width(8.dp))
            val sign = when (transaction.type) {
                TransactionType.INCOME -> "+"
                TransactionType.EXPENSE, TransactionType.CREDIT -> "-"
                else -> ""
            }
            val amountColor = when (transaction.type) {
                TransactionType.INCOME -> CashiroWidgetColors.income
                TransactionType.EXPENSE, TransactionType.CREDIT -> CashiroWidgetColors.expense
                else -> GlanceTheme.colors.onSurface
            }
            Text(
                text = if (hidden) {
                    MASKED_AMOUNT
                } else {
                    sign + CurrencyFormatter.formatCurrency(transaction.amount, transaction.currency)
                },
                style = TextStyle(
                    color = amountColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
        }
    }
}

// ----- Deep link action callbacks -----

class OpenTransactionDetailAction : ActionCallback {
    companion object {
        val transactionIdParam = ActionParameters.Key<Long>("transaction_id")
    }

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val transactionId = parameters[transactionIdParam] ?: return
        val intent = Intent(context, MainActivity::class.java).apply {
            action = "com.ritesh.cashiro.action.VIEW_TRANSACTION"
            putExtra("transaction_id", transactionId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(intent)
    }
}

class OpenAccountDetailAction : ActionCallback {
    companion object {
        val bankNameParam = ActionParameters.Key<String>("bank_name")
        val accountLast4Param = ActionParameters.Key<String>("account_last4")
    }

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val bankName = parameters[bankNameParam] ?: return
        val accountLast4 = parameters[accountLast4Param] ?: return
        val intent = Intent(context, MainActivity::class.java).apply {
            action = "com.ritesh.cashiro.action.VIEW_ACCOUNT"
            putExtra("bank_name", bankName)
            putExtra("account_last4", accountLast4)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(intent)
    }
}

class ToggleAccountsHiddenAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        toggleHiddenState(context, glanceId)
        AccountsWidget().update(context, glanceId)
    }
}

class RefreshAccountsWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[WidgetStateKeys.refreshToken] = System.currentTimeMillis()
        }
        AccountsWidget().update(context, glanceId)
    }
}
