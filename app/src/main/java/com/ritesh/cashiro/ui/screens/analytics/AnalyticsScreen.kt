package com.ritesh.cashiro.ui.screens.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ritesh.cashiro.presentation.common.TimePeriod
import com.ritesh.cashiro.presentation.common.TransactionTypeFilter
import com.ritesh.cashiro.ui.components.*
import com.ritesh.cashiro.ui.icons.CategoryMapping
import com.ritesh.cashiro.ui.theme.*
import com.ritesh.cashiro.utils.CurrencyFormatter
import com.ritesh.cashiro.utils.DateRangeUtils
import java.math.BigDecimal
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import com.ritesh.cashiro.ui.components.CustomTitleTopAppBar
import com.ritesh.cashiro.ui.effects.overScrollVertical
import com.ritesh.cashiro.ui.effects.rememberOverscrollFlingBehavior
import androidx.compose.foundation.lazy.rememberLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel(),
    onNavigateToChat: () -> Unit = {},
    onNavigateToTransactions: (category: String?, merchant: String?, period: String?, currency: String?) -> Unit = { _, _, _, _ -> },
    onNavigateToSettings: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedPeriod by viewModel.selectedPeriod.collectAsStateWithLifecycle()
    val transactionTypeFilter by viewModel.transactionTypeFilter.collectAsStateWithLifecycle()
    val selectedCurrency by viewModel.selectedCurrency.collectAsStateWithLifecycle()
    val availableCurrencies by viewModel.availableCurrencies.collectAsStateWithLifecycle()
    val customDateRange by viewModel.customDateRange.collectAsStateWithLifecycle()
    var showAdvancedFilters by remember { mutableStateOf(false) }
    var showDateRangePicker by remember { mutableStateOf(false) }

    // Calculate active filter count
    val activeFilterCount = if (transactionTypeFilter != TransactionTypeFilter.EXPENSE) 1 else 0

    // Cache expensive operations
    val timePeriods = remember { TimePeriod.values().toList() }
    val customRangeLabel = remember(customDateRange) {
        DateRangeUtils.formatDateRange(customDateRange)
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollBehaviorSmall = TopAppBarDefaults.pinnedScrollBehavior()
    val hazeState = remember { HazeState() }
    val context = LocalContext.current
    val lazyListState = rememberLazyListState()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CustomTitleTopAppBar(
                title = "Analytics",
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehavior,
                hazeState = hazeState,
                hasBackButton = false,
//                showSettingsButton = true,
//                showDiscordButton = true,
//                onSettingsClick = onNavigateToSettings,
//                onDiscordClick = {
//                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://discord.gg/H3xWeMWjKQ"))
//                    context.startActivity(intent)
//                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()

        ) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .haze(state = hazeState)
                    .background(MaterialTheme.colorScheme.background)
                    .overScrollVertical(),
                flingBehavior = rememberOverscrollFlingBehavior { lazyListState },
                contentPadding = PaddingValues(
                    start = Dimensions.Padding.content,
                    end = Dimensions.Padding.content,
                    top = paddingValues.calculateTopPadding(),
                    bottom = Dimensions.Padding.content
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                // Period Selector - Always visible
                item {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        items(timePeriods) { period ->
                            FilterChip(
                                // Only show CUSTOM as selected if both period is CUSTOM AND dates are set
                                selected = if (period == TimePeriod.CUSTOM) {
                                    selectedPeriod == period && customDateRange != null
                                } else {
                                    selectedPeriod == period
                                },
                                onClick = {
                                    if (period == TimePeriod.CUSTOM) {
                                        showDateRangePicker = true
                                        // Don't change selectedPeriod until user confirms dates
                                    } else {
                                        viewModel.selectPeriod(period)
                                    }
                                },
                                label = {
                                    Text(
                                        if (period == TimePeriod.CUSTOM && customRangeLabel != null) {
                                            customRangeLabel
                                        } else {
                                            period.label
                                        }
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }

                // Currency Selector (if multiple currencies available)
                if (availableCurrencies.size > 1) {
                    item {
                        CurrencyFilterRow(
                            selectedCurrency = selectedCurrency,
                            availableCurrencies = availableCurrencies,
                            onCurrencySelected = { viewModel.selectCurrency(it) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Collapsible Transaction Type Filter
                item {
                    CollapsibleFilterRow(
                        isExpanded = showAdvancedFilters,
                        activeFilterCount = activeFilterCount,
                        onToggle = { showAdvancedFilters = !showAdvancedFilters },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            items(TransactionTypeFilter.values().toList()) { typeFilter ->
                                FilterChip(
                                    selected = transactionTypeFilter == typeFilter,
                                    onClick = { viewModel.setTransactionTypeFilter(typeFilter) },
                                    label = { Text(typeFilter.label) },
                                    leadingIcon = if (transactionTypeFilter == typeFilter) {
                                        {
                                            when (typeFilter) {
                                                TransactionTypeFilter.INCOME -> Icon(
                                                    Icons.AutoMirrored.Filled.TrendingUp,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(Dimensions.Icon.small)
                                                )

                                                TransactionTypeFilter.EXPENSE -> Icon(
                                                    Icons.AutoMirrored.Filled.TrendingDown,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(Dimensions.Icon.small)
                                                )

                                                TransactionTypeFilter.CREDIT -> Icon(
                                                    Icons.Default.CreditCard,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(Dimensions.Icon.small)
                                                )

                                                TransactionTypeFilter.TRANSFER -> Icon(
                                                    Icons.Default.SwapHoriz,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(Dimensions.Icon.small)
                                                )

                                                TransactionTypeFilter.INVESTMENT -> Icon(
                                                    Icons.AutoMirrored.Filled.ShowChart,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(Dimensions.Icon.small)
                                                )

                                                else -> null
                                            }
                                        }
                                    } else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                )
                            }
                        }
                    }
                }

                // Analytics Summary Card
                if (uiState.totalSpending > BigDecimal.ZERO || uiState.transactionCount > 0) {
                    item {
                        AnalyticsSummaryCard(
                            totalAmount = uiState.totalSpending,
                            transactionCount = uiState.transactionCount,
                            averageAmount = uiState.averageAmount,
                            topCategory = uiState.topCategory,
                            topCategoryPercentage = uiState.topCategoryPercentage,
                            currency = uiState.currency,
                            isLoading = uiState.isLoading
                        )
                    }
                }

                // Category Breakdown Section
                if (uiState.categoryBreakdown.isNotEmpty()) {
                    item {
                        CategoryBreakdownCard(
                            categories = uiState.categoryBreakdown,
                            currency = selectedCurrency,
                            onCategoryClick = { category ->
                                onNavigateToTransactions(
                                    category.name,
                                    null,
                                    selectedPeriod.name,
                                    selectedCurrency
                                )
                            }
                        )
                    }
                }

                // Top Merchants Section
                if (uiState.topMerchants.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Top Merchants"
                        )
                    }

                    // All Merchants with expandable list
                    item {
                        ExpandableList(
                            items = uiState.topMerchants,
                            visibleItemCount = 3,
                            modifier = Modifier.fillMaxWidth()
                        ) { merchant ->
                            MerchantListItem(
                                merchant = merchant,
                                currency = selectedCurrency,
                                onClick = {
                                    onNavigateToTransactions(
                                        null,
                                        merchant.name,
                                        selectedPeriod.name,
                                        selectedCurrency
                                    )
                                }
                            )
                        }
                    }
                }


                // Empty state
                if (uiState.topMerchants.isEmpty() && uiState.categoryBreakdown.isEmpty() && !uiState.isLoading) {
                    item {
                        EmptyAnalyticsState()
                    }
                }
            }

//    // Chat FAB
//    SmallFloatingActionButton(
//        onClick = onNavigateToChat,
//        modifier = Modifier
//            .align(Alignment.BottomEnd)
//            .padding(Dimensions.Padding.content),
//        containerColor = MaterialTheme.colorScheme.secondaryContainer,
//        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
//    ) {
//        Icon(
//            imageVector = Icons.AutoMirrored.Filled.Chat,
//            contentDescription = "Open AI Assistant"
//        )
//    }
        }

        if (showDateRangePicker) {
            CustomDateRangePickerDialog(
                onDismiss = { showDateRangePicker = false },
                onConfirm = { startDate, endDate ->
                    viewModel.setCustomDateRange(startDate, endDate)
                    showDateRangePicker = false
                },
                initialStartDate = customDateRange?.first,
                initialEndDate = customDateRange?.second
            )
        }
    }
}

@Composable
private fun CategoryListItem(
    category: CategoryData,
    currency: String
) {
    val categoryInfo = CategoryMapping.categories[category.name]
        ?: CategoryMapping.categories["Others"]!!

    ListItemCard(
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(categoryInfo.color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                CategoryIcon(
                    category = category.name,
                    size = 24.dp,
                    tint = categoryInfo.color
                )
            }
        },
        title = category.name,
        subtitle = "${category.transactionCount} transactions",
        amount = CurrencyFormatter.formatCurrency(category.amount, currency),
        trailingContent = {
            Text(
                text = "${category.percentage.toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

@Composable
private fun MerchantListItem(
    merchant: MerchantData,
    currency: String,
    onClick: () -> Unit = {}
) {
    val subtitle = buildString {
        append("${merchant.transactionCount} ")
        append(if (merchant.transactionCount == 1) "transaction" else "transactions")
        if (merchant.isSubscription) {
            append(" • Subscription")
        }
    }

    ListItemCard(
        leadingContent = {
            BrandIcon(
                merchantName = merchant.name,
                size = 40.dp,
                showBackground = true
            )
        },
        title = merchant.name,
        subtitle = subtitle,
        amount = CurrencyFormatter.formatCurrency(merchant.amount, currency),
        onClick = onClick
    )
}

@Composable
private fun EmptyAnalyticsState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Dimensions.Padding.content),
        contentAlignment = Alignment.Center
    ) {
        CashiroCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimensions.Padding.empty),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "No data available",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = "Start tracking expenses to see analytics",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun CurrencyFilterRow(
    selectedCurrency: String,
    availableCurrencies: List<String>,
    onCurrencySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        item {
            Text(
                text = "Currency:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    vertical = Spacing.sm,
                    horizontal = Spacing.xs
                )
            )
        }
        items(availableCurrencies) { currency ->
            FilterChip(
                selected = selectedCurrency == currency,
                onClick = { onCurrencySelected(currency) },
                label = { Text(currency) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}
