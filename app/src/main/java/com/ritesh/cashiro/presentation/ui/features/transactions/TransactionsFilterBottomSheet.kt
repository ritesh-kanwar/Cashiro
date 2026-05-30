package com.ritesh.cashiro.presentation.ui.features.transactions

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.ritesh.cashiro.presentation.common.TransactionTypeFilter
import com.ritesh.cashiro.presentation.effects.BlurredAnimatedVisibility
import com.ritesh.cashiro.presentation.ui.components.AccountFilterChip
import com.ritesh.cashiro.presentation.ui.components.CurrencyCard
import com.ritesh.cashiro.presentation.ui.features.accounts.NumberPad
import com.ritesh.cashiro.presentation.ui.components.TypeFilterIcon
import com.ritesh.cashiro.presentation.ui.theme.Dimensions
import com.ritesh.cashiro.presentation.ui.theme.Spacing
import com.ritesh.cashiro.utils.CurrencyFormatter
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
import com.ritesh.cashiro.presentation.ui.icons.Category2
import com.ritesh.cashiro.presentation.ui.icons.Iconax
import com.ritesh.cashiro.presentation.ui.icons.ReceiptEdit
import com.ritesh.cashiro.utils.IconResolutionUtils
import com.ritesh.cashiro.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TransactionsFilterBottomSheet(
    viewModel: TransactionsViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    val categoryFilter by viewModel.categoryFilter.collectAsState()
    val subcategoryFilter by viewModel.subcategoryFilter.collectAsState()
    val amountRangeFilter by viewModel.amountRangeFilter.collectAsState()
    val accountsFilter by viewModel.accountsFilter.collectAsState()
    val currenciesFilter by viewModel.currenciesFilter.collectAsState()
    val typeFilter by viewModel.transactionTypeFilter.collectAsState()
    
    val categoriesMap by viewModel.categories.collectAsState()
    val subcategoriesMap by viewModel.subcategories.collectAsState()
    val accountsMap by viewModel.accountsMap.collectAsState()
    val availableCurrencies by viewModel.availableCurrencies.collectAsState()
    val maxTransactionAmount by viewModel.maxTransactionAmount.collectAsState()

    // Derived lists
    val categoryList = remember(categoriesMap) { categoriesMap.values.toList() }
    val currentSubcategoriesList = remember(categoryFilter, subcategoriesMap, categoriesMap) {
        if (categoryFilter.isNotEmpty()) {
            val selectedCategoryIds = categoryFilter.mapNotNull { categoriesMap[it]?.id }.toSet()
            subcategoriesMap.values.filter { it.categoryId in selectedCategoryIds }.toList()
        } else {
            emptyList()
        }
    }

    // Amount Range State
    val maxBound = remember(maxTransactionAmount, amountRangeFilter) {
        val filterMax = amountRangeFilter?.second?.toFloat() ?: 0f
        maxOf(maxTransactionAmount, filterMax).takeIf { it > 0f } ?: 1000000f
    }
    
    var sliderPosition by remember(amountRangeFilter, maxBound) {
        val start = amountRangeFilter?.first?.toFloat() ?: 0f
        val end = amountRangeFilter?.second?.toFloat() ?: maxBound
        mutableStateOf(start..end)
    }

    var showNumberPadForMin by remember { mutableStateOf(false) }
    var showNumberPadForMax by remember { mutableStateOf(false) }
    var tempAmountInput by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight(0.9f)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 100.dp)
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                ) {
                    Spacer(modifier = Modifier.height(Spacing.xl))
                    //Transaction Types
                    LazyRow(
                        contentPadding = PaddingValues(
                            horizontal = Dimensions.Padding.content,
                            vertical = Spacing.sm
                        ),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        items(TransactionTypeFilter.entries) { type ->
                            FilterChip(
                                selected = if (type == TransactionTypeFilter.ALL) typeFilter.isEmpty() else typeFilter.contains(type),
                                onClick = { viewModel.setTransactionTypeFilter(type) },
                                leadingIcon = if (if (type == TransactionTypeFilter.ALL) typeFilter.isEmpty() else typeFilter.contains(type)) {
                                    { TypeFilterIcon(type) }
                                } else null,
                                label = {
                                    Text(
                                        type.name.lowercase()
                                            .let { if (it.isNotEmpty()) it.take(1).uppercase() + it.drop(1) else it })
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            )
                        }
                    }
                    // Categories
                    if (categoryList.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.categories),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(
                                horizontal = Dimensions.Padding.content,
                                vertical = Spacing.sm
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = Dimensions.Padding.content),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                        ) {
                            item {
                                FilterCategoryItem(
                                    name = "All",
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    iconVector = Iconax.Category2,
                                    isSelected = categoryFilter.isEmpty(),
                                    onClick = {
                                        viewModel.clearCategoryFilter()
                                        viewModel.setSubcategoryFilter(null)
                                    }
                                )
                            }
                            items(categoryList, key = { it.id }) { category ->
                                val color = try {
                                    Color(category.color.toColorInt())
                                } catch (e: Exception) {
                                    MaterialTheme.colorScheme.primary
                                }
                                FilterCategoryItem(
                                    name = category.name,
                                    color = color.copy(alpha = 0.2f),
                                    iconResId = category.iconResId,
                                    iconName = category.iconName,
                                    iconVector = Iconax.Category2,
                                    isSelected = categoryFilter.contains(category.name),
                                    onClick = {
                                        viewModel.setCategoryFilter(category.name)
                                        // Optional: Clear subcategories that don't belong to any selected category anymore
                                        //  keep the subcategories selected until explicitly cleared
                                    }
                                )
                            }
                        }
                    }

                    // Subcategories
                    BlurredAnimatedVisibility(
                        visible = currentSubcategoriesList.isNotEmpty(),
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Column {
                            Text(
                                text = if (categoryFilter.size == 1) categoryFilter.first() else stringResource(R.string.subcategories),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(
                                    start = Dimensions.Padding.content,
                                    end = Dimensions.Padding.content,
                                    top = Spacing.md,
                                    bottom = Spacing.sm
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = Dimensions.Padding.content),
                                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                            ) {
                                item {
                                    FilterCategoryItem(
                                        name = "All",
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        iconVector = Iconax.Category2,
                                        isSelected = subcategoryFilter.isEmpty(),
                                        onClick = { viewModel.setSubcategoryFilter(null) },
                                        isSmall = true
                                    )
                                }
                                items(currentSubcategoriesList, key = { it.id }) { subcat ->
                                    val color = try {
                                        Color(subcat.color.toColorInt())
                                    } catch (e: Exception) {
                                        MaterialTheme.colorScheme.primary
                                    }
                                    FilterCategoryItem(
                                        name = subcat.name,
                                        color = color.copy(alpha = 0.2f),
                                        iconResId = subcat.iconResId,
                                        iconName = subcat.iconName,
                                        iconVector = Iconax.Category2,
                                        isSelected = subcategoryFilter.contains(subcat.name),
                                        onClick = { viewModel.setSubcategoryFilter(subcat.name) },
                                        isSmall = true
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(Spacing.lg))

                    // Amount Range
                    Text(
                        text = stringResource(R.string.amount_range),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = Dimensions.Padding.content),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Column(
                        modifier = Modifier
                            .padding(Dimensions.Padding.content)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                shape = RoundedCornerShape(Dimensions.Padding.content)
                            )
                            .padding(horizontal = Dimensions.Padding.content, vertical = Spacing.sm)

                    ) {

                        val baseCurrencySymbol =
                            CurrencyFormatter.getCurrencySymbol(viewModel.baseCurrency.collectAsState().value)

                        RangeSlider(
                            value = sliderPosition,
                            onValueChange = { sliderPosition = it },
                            valueRange = 0f..maxBound,
                            onValueChangeFinished = {
                                viewModel.setAmountRangeFilter(
                                    BigDecimal(sliderPosition.start.toDouble()).setScale(
                                        2,
                                        RoundingMode.HALF_UP
                                    ),
                                    BigDecimal(sliderPosition.endInclusive.toDouble()).setScale(
                                        2,
                                        RoundingMode.HALF_UP
                                    )
                                )
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(Spacing.md),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(Spacing.md))
                                    .clickable {
                                        tempAmountInput = sliderPosition.start.toLong()
                                            .toString(); showNumberPadForMin = true
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(Spacing.sm),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "$baseCurrencySymbol ${
                                            CurrencyFormatter.formatAmount(
                                                BigDecimal(sliderPosition.start.toDouble()).setScale(
                                                    0,
                                                    RoundingMode.HALF_UP
                                                ),
                                                viewModel.baseCurrency.collectAsState().value
                                            )
                                        }",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Iconax.ReceiptEdit,
                                        contentDescription = "Edit min",
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            Text(
                                stringResource(R.string.min_max),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )

                            Surface(
                                shape = RoundedCornerShape(Spacing.md),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(Spacing.md))
                                    .clickable {
                                        tempAmountInput = sliderPosition.endInclusive.toLong()
                                            .toString(); showNumberPadForMax = true
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(Spacing.sm),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "$baseCurrencySymbol ${
                                            CurrencyFormatter.formatAmount(
                                                BigDecimal(sliderPosition.endInclusive.toDouble()).setScale(
                                                    0,
                                                    RoundingMode.HALF_UP
                                                ),
                                                viewModel.baseCurrency.collectAsState().value
                                            )
                                        }",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Iconax.ReceiptEdit,
                                        contentDescription = "Edit max",
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(Spacing.lg))

                    // Accounts
                    if (accountsMap.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.accounts),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = Dimensions.Padding.content),
                            color = MaterialTheme.colorScheme.primary
                        )
                        LazyRow(
                            contentPadding = PaddingValues(
                                horizontal = Dimensions.Padding.content,
                                vertical = Spacing.sm
                            ),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            items(
                                accountsMap.values.toList(),
                                key = { "${it.bankName}_${it.accountLast4}" }) { account ->
                                val accountKey = "${account.bankName}_${account.accountLast4}"
                                Box(modifier = Modifier.width(220.dp)) {
                                    AccountFilterChip(
                                        account = account,
                                        isSelected = accountsFilter.contains(accountKey),
                                        onClick = { viewModel.toggleAccountFilter(accountKey) }
                                    )
                                }
                            }
                        }
                    }

                    // Currencies
                    if (availableCurrencies.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        Text(
                            text = stringResource(R.string.currencies),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = Dimensions.Padding.content),
                            color = MaterialTheme.colorScheme.primary
                        )
                        LazyRow(
                            contentPadding = PaddingValues(
                                horizontal = Dimensions.Padding.content,
                                vertical = Spacing.sm
                            ),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            items(availableCurrencies) { currencyCode ->
                                val currency =
                                    com.ritesh.cashiro.data.model.Currency.getByCode(currencyCode)
                                if (currency != null) {
                                    CurrencyCard(
                                        currency = currency,
                                        isSelected = currenciesFilter.contains(currencyCode),
                                        onCurrencyCardClick = {
                                            viewModel.toggleCurrencyFilter(
                                                currencyCode
                                            )
                                        }
                                    )
                                } else {
                                    FilterChip(
                                        selected = currenciesFilter.contains(currencyCode),
                                        onClick = { viewModel.toggleCurrencyFilter(currencyCode) },
                                        label = { Text(currencyCode) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.surface,
                                Color.Transparent,
                            )
                        )
                    )
                    .padding(horizontal = Dimensions.Padding.content, vertical = Spacing.xs),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.filters),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()

                )
            }

            // Action Buttons at Bottom
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
                    )
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    // Apply button
                    Button(
                        onClick = { onDismiss() },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = MaterialTheme.shapes.extraExtraLarge
                    ) {
                        Text(
                            text = stringResource(R.string.apply),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Reset button
                    IconButton(
                        onClick = { viewModel.resetFilters() },
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                MaterialTheme.colorScheme.secondaryContainer,
                                shape = MaterialTheme.shapes.extraExtraLarge
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.RestartAlt,
                            contentDescription = "Reset to default",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }

    // NumberPad Bottom Sheets for Min/Max
    if (showNumberPadForMin || showNumberPadForMax) {
        val isMin = showNumberPadForMin
        ModalBottomSheet(
            onDismissRequest = { 
                showNumberPadForMin = false
                showNumberPadForMax = false 
            },
            containerColor = MaterialTheme.colorScheme.background,
        ) {
            NumberPad(
                initialValue = tempAmountInput.ifEmpty { "0" },
                title = if (isMin) stringResource(R.string.set_minimum_amount) else stringResource(R.string.set_maximum_amount),
                doneButtonLabel = stringResource(R.string.confirm),
                onDone = { resultString ->
                    // Remove any trailing periods or whitespace and parse safely
                    val cleanString = resultString.trim().replace(Regex("\\.+\$"), "")
                    val value = cleanString.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    
                    if (isMin) {
                        val max = amountRangeFilter?.second ?: BigDecimal(sliderPosition.endInclusive.toDouble())
                        if (value <= max) {
                            viewModel.setAmountRangeFilter(value, max)
                            sliderPosition = value.toFloat()..max.toFloat()
                        } else {
                            viewModel.setAmountRangeFilter(value, value)
                            sliderPosition = value.toFloat()..value.toFloat()
                        }
                    } else {
                        val min = amountRangeFilter?.first ?: BigDecimal(sliderPosition.start.toDouble())
                        if (value >= min) {
                            viewModel.setAmountRangeFilter(min, value)
                            sliderPosition = min.toFloat()..value.toFloat()
                        } else {
                            viewModel.setAmountRangeFilter(value, value)
                            sliderPosition = value.toFloat()..value.toFloat()
                        }
                    }
                    showNumberPadForMin = false
                    showNumberPadForMax = false
                }
            )
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun FilterCategoryItem(
    name: String,
    color: Color,
    iconResId: Int = 0,
    iconName: String? = null,
    iconVector: ImageVector? = null,
    isSelected: Boolean,
    onClick: () -> Unit,
    isSmall: Boolean = false
) {
    val context = LocalContext.current
    val size = if (isSmall) 54.dp else 66.dp
    val iconSize = if (isSmall) 44.dp else 58.dp

    val resolvedResId = remember(iconName, iconResId) {
        if (!iconName.isNullOrEmpty()) {
            val res = IconResolutionUtils.nameToResId(context, iconName)
            if (res != 0) res else IconResolutionUtils.getSafeResId(context, iconResId, 0)
        } else {
            IconResolutionUtils.getSafeResId(context, iconResId, 0)
        }
    }

    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.width(size + 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(18.dp))
                .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else color)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            if (resolvedResId != 0) {
                Icon(
                    painter = painterResource(id = resolvedResId),
                    contentDescription = name,
                    modifier = Modifier.size(iconSize).padding(4.dp),
                    tint =  Color.Unspecified
                )
            } else {
                Icon(
                    imageVector = iconVector ?: Iconax.Category2,
                    contentDescription = name,
                    modifier = Modifier.size(iconSize).padding(4.dp),
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Selection indicator badge
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .size(16.dp)
                        .background(MaterialTheme.colorScheme.tertiary, RoundedCornerShape(percent = 50))
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiary,
                        modifier = Modifier.padding(2.dp).fillMaxSize()
                    )
                }
            }
        }
        
        Text(
            text = name,
            style = if (isSmall) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier.basicMarquee(
                iterations = Int.MAX_VALUE,
                repeatDelayMillis = 1000,
            )
        )
    }
}
