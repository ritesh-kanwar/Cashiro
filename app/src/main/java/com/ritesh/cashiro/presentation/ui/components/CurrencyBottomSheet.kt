package com.ritesh.cashiro.presentation.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ritesh.cashiro.R
import com.ritesh.cashiro.data.currency.model.CurrencyConversion
import com.ritesh.cashiro.data.model.Currency
import com.ritesh.cashiro.data.model.CurrencyPickerOptions
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import com.ritesh.cashiro.presentation.accounts.CurrencyViewModel
import com.ritesh.cashiro.presentation.effects.BlurredAnimatedVisibility
import com.ritesh.cashiro.presentation.effects.rememberOverscrollFlingBehavior
import com.ritesh.cashiro.presentation.ui.theme.Dimensions
import com.ritesh.cashiro.presentation.ui.icons.CloseCircle
import com.ritesh.cashiro.presentation.ui.icons.Iconax
import com.ritesh.cashiro.presentation.ui.icons.Information
import com.ritesh.cashiro.presentation.ui.icons.Search
import com.ritesh.cashiro.presentation.ui.theme.Spacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun CurrencyBottomSheet(
    selectedCurrency: String,
    onCurrencySelected: (String) -> Unit,
    onDismiss: () -> Unit,
    viewModel: CurrencyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    var showAllCurrencies by rememberSaveable { mutableStateOf(false) }
    var showExchangeRateInfo by rememberSaveable { mutableStateOf(false) }
    var showExchangeRateSheet by rememberSaveable { mutableStateOf(false) }
    var showAddCustomCurrencySheet by rememberSaveable { mutableStateOf(false) }
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val exchangeRatesSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = stringResource(R.string.currencies),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .fillMaxWidth()
                )

                // Search Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SearchBarBox(
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        label = { Text(stringResource(R.string.search_currencies)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Iconax.Search,
contentDescription = stringResource(R.string.search),
                                tint = MaterialTheme.colorScheme.onSurface.copy(0.5f)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.text.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = TextFieldValue("") }) {
                                    Icon(
                                        imageVector = Iconax.CloseCircle,
                                        contentDescription = stringResource(R.string.clear),
                                        tint = MaterialTheme.colorScheme.onSurface.copy(0.5f)
                                    )
                                }
                            } else {
                                IconButton(onClick = { showExchangeRateInfo = true }) {
                                    Icon(
                                        imageVector = Iconax.Information,
                                        contentDescription = "Info",
                                        tint = MaterialTheme.colorScheme.onSurface.copy(0.5f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        },
                        modifier = Modifier.padding(horizontal = Spacing.md).padding(bottom = 16.dp)
                    )
                }


                // Scrollable List Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(
                            state = scrollState,
                            flingBehavior = rememberOverscrollFlingBehavior { scrollState }
                        )
                ) {
                    val sourceCurrencies = remember(uiState.currencies, selectedCurrency) {
                        CurrencyPickerOptions.catalog(custom = uiState.currencies, selectedCode = selectedCurrency)
                    }
                    val filteredCurrencies = remember(sourceCurrencies, searchQuery.text) {
                        CurrencyPickerOptions.matching(sourceCurrencies, searchQuery.text)
                    }

                    if (filteredCurrencies.isEmpty()) {
                        Text(
                            text = stringResource(R.string.currency_not_available),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    } else {
                        // Display currencies in a FlowRow
                        val displayedCurrencies =
                            if (showAllCurrencies || searchQuery.text.isNotBlank()) {
                                filteredCurrencies
                            } else {
                                CurrencyPickerOptions.quickAccess(filteredCurrencies, selectedCurrency)
                            }

                        FlowRow(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(Dimensions.Radius.md)),
                            maxItemsInEachRow = 3,
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            displayedCurrencies.forEach { currency ->
                                CurrencyCard(
                                    currency = currency,
                                    isSelected = currency.code.equals(
                                        selectedCurrency,
                                        ignoreCase = true
                                    ),
                                    onCurrencyCardClick = {
                                        scope.launch { sheetState.hide() }
                                            .invokeOnCompletion {
                                                if (!sheetState.isVisible) {
                                                    onCurrencySelected(currency.code)
                                                }
                                            }
                                    }
                                )
                            }
                        }

                        // View All Currencies button
                        if (!showAllCurrencies && searchQuery.text.isBlank() && displayedCurrencies.size < filteredCurrencies.size) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                TextButton(
                                    onClick = { showAllCurrencies = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Transparent,
                                        contentColor = MaterialTheme.colorScheme.inverseSurface.copy(
                                            0.5f
                                        )
                                    ),
                                    shapes = ButtonDefaults.shapes()
                                ) {
                                    Text(
                                        text = stringResource(R.string.view_all_currencies),
                                        fontSize = 12.sp,
                                        modifier = Modifier
                                            .background(
                                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                                shape = RoundedCornerShape(15.dp)
                                            )
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(80.dp))
                }
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
                // Save button
                Button(
                    onClick = { showAddCustomCurrencySheet = true },
                    modifier = Modifier
                        .height(56.dp),
                    shape = MaterialTheme.shapes.extraExtraLarge
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.add_custom_currency),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Exchange Rate Info Dialog
            BlurredAnimatedVisibility(
                visible = showExchangeRateInfo,
                enter = fadeIn() + scaleIn(
                    animationSpec = tween(durationMillis = 300),
                    initialScale = 0f,
                ),
                exit = fadeOut() + scaleOut(
                    animationSpec = tween(durationMillis = 100),
                    targetScale = 0f,
                ),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { showExchangeRateInfo = false }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .shadow(
                                elevation = 25.dp,
                                RoundedCornerShape(15.dp),
                                clip = true,
                                spotColor = Color.Black,
                                ambientColor = Color.Black
                            )
                            .fillMaxWidth(0.85f)
                            .clip(RoundedCornerShape(15.dp))
                            .background(
                                MaterialTheme.colorScheme.surface,
                                RoundedCornerShape(15.dp)
                            )
                            .clickable(onClick = {})
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(15.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = "Info",
                                tint = MaterialTheme.colorScheme.primary.copy(0.5f),
                                modifier = Modifier.size(50.dp)
                            )
                            Text(
                                text = stringResource(R.string.exchange_rates_notice_title),
                                textAlign = TextAlign.Center,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.inverseSurface,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = stringResource(R.string.exchange_rates_notice_text),
                                textAlign = TextAlign.Center,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.inverseSurface.copy(0.8f),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = { showExchangeRateInfo = false },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                        contentColor = MaterialTheme.colorScheme.inverseSurface
                                    ),
                                ) {
                                    Text(text = stringResource(R.string.ok))
                                }
                                Button(
                                    onClick = {
                                        showExchangeRateSheet = true
                                        showExchangeRateInfo = false
                                        viewModel.loadConversions(selectedCurrency)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = Color.White
                                    ),
                                ) {
                                    Text(text = stringResource(R.string.exchange_rates), color = MaterialTheme.colorScheme.onPrimary)
                                }
                            }
                        }
                    }
                }
            }

            if (showExchangeRateSheet) {
                ExchangeRatesBottomSheet(
                    uiState = uiState,
                    onDismiss = { showExchangeRateSheet = false },
                    sheetState = exchangeRatesSheetState,
                    onSaveCustomRate = { fromCurrency, toCurrency, rate ->
                        viewModel.saveCustomRate(fromCurrency, toCurrency, rate)
                    },
                    onResetCustomRate = { fromCurrency, toCurrency ->
                        viewModel.resetCustomRate(fromCurrency, toCurrency)
                    }
                )
            }
            
            if (showAddCustomCurrencySheet) {
                AddCustomCurrencyBottomSheet(
                    onDismiss = { showAddCustomCurrencySheet = false },
                    onSave = { name, symbol, code, rate ->
                        viewModel.addCustomCurrency(name, symbol, code, rate)
                        showAddCustomCurrencySheet = false
                        showExchangeRateSheet = true // Optional: show rates to confirm
                    }
                )
            }
        }
    }
}

@Composable
fun CurrencyCard(
    currency: Currency,
    isSelected: Boolean = false,
    onCurrencyCardClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .sizeIn(minWidth = 90.dp, minHeight = 70.dp, maxHeight = 90.dp, maxWidth = 110.dp)
            .clip(RoundedCornerShape(Dimensions.Radius.md))
            .selectable(selected = isSelected, role = Role.RadioButton, onClick = onCurrencyCardClick)
            .padding(vertical = 5.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = if (isSelected)
                MaterialTheme.colorScheme.onPrimaryContainer
            else
                MaterialTheme.colorScheme.inverseSurface
        ),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(5.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = currency.code.uppercase(),
                lineHeight = 12.sp,
                fontSize = 12.sp,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(0.7f)
                else
                    MaterialTheme.colorScheme.inverseSurface.copy(0.5f)
            )
            Text(
                text = currency.symbol,
                lineHeight = 20.sp,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.inverseSurface,
                modifier = Modifier.padding(vertical = 5.dp)
            )
            Text(
                text = currency.name,
                lineHeight = 10.sp,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(0.7f)
                else
                    MaterialTheme.colorScheme.inverseSurface.copy(0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .basicMarquee(
                        iterations = Int.MAX_VALUE,
                        animationMode = MarqueeAnimationMode.Immediately,
                        initialDelayMillis = 1000,
                        velocity = 30.dp
                    )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExchangeRatesBottomSheet(
    uiState: CurrencyViewModel.CurrencyUiState,
    onDismiss: () -> Unit,
    sheetState: SheetState,
    onSaveCustomRate: (fromCurrency: String, toCurrency: String, rate: Double) -> Unit = { _, _, _ -> },
    onResetCustomRate: (fromCurrency: String, toCurrency: String) -> Unit = { _, _ -> }
) {
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    var editTarget by remember { mutableStateOf<CurrencyConversion?>(null) }
    var editRateText by remember { mutableStateOf("") }
    val filteredConversions = uiState.conversions.filter {
        it.currencyCode.contains(searchQuery.text, ignoreCase = true) ||
                it.symbol.contains(searchQuery.text, ignoreCase = true)
    }
    val baseCurrencyCode = uiState.selectedCurrency?.code ?: ""

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.exchange_rates),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
            )
            Spacer(modifier = Modifier.size(12.dp))

            SearchBarBox(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                label = { Text(stringResource(R.string.search_currencies)) },
                leadingIcon = {
                    Icon(
                        imageVector = Iconax.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurface.copy(0.5f)
                    )
                },
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (uiState.isLoadingConversions) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.conversionError != null) {
                Text(
                    text = uiState.conversionError,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    item {
                        Text(
                            text = stringResource(R.string.rates_relative_to, baseCurrencyCode),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        )
                    }
                    itemsIndexed(filteredConversions) { index, conversion ->
                        ExchangeRateItem(
                            conversion = conversion,
                            baseCurrency = uiState.selectedCurrency,
                            isFirst = index == 0,
                            isLast = index == filteredConversions.size - 1,
                            onClick = {
                                editTarget = conversion
                                editRateText = conversion.displayRate
                            }
                        )
                    }

                    if (uiState.lastUpdated > 0) {
                        item {
                            val formatter = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
                            val dateStr = remember(uiState.lastUpdated) { formatter.format(Date(uiState.lastUpdated * 1000)) }
                            Text(
                                text = stringResource(R.string.last_updated, dateStr),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    if (editTarget != null) {
        val conversion = editTarget!!
        AlertDialog(
            onDismissRequest = { editTarget = null },
            title = {
                Text(
                    text = "${conversion.currencyCode} (${conversion.symbol})",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.exchange_rate_format, baseCurrencyCode, conversion.displayRate, conversion.symbol),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextField(
                        value = editRateText,
                        onValueChange = { editRateText = it },
                        label = { Text(stringResource(R.string.custom_rate)) },
                        shape = RoundedCornerShape(Spacing.md),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface.copy(0.5f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(0.5f),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                0.7f
                            )
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (conversion.isCustom) {
                        Text(
                            text = stringResource(R.string.custom_rate_set),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            confirmButton = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(1.5.dp),
                    ) {
                        Button(
                            onClick = { editTarget = null },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(0.5f),
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(
                                topStart = Dimensions.Radius.xxl,
                                topEnd = Dimensions.Radius.xs,
                                bottomStart = Dimensions.Radius.xxl,
                                bottomEnd = Dimensions.Radius.xs
                            ),
                            modifier = Modifier
                                .padding(start = Spacing.xl)
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(R.string.cancel),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        if (conversion.isCustom) {
                            Button(
                                onClick = {
                                    onResetCustomRate(baseCurrencyCode, conversion.currencyCode)
                                    editTarget = null
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(0.5f),
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                shape = RoundedCornerShape(
                                    topStart = Dimensions.Radius.xs,
                                    topEnd = Dimensions.Radius.xs,
                                    bottomStart = Dimensions.Radius.xs,
                                    bottomEnd = Dimensions.Radius.xs
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ) {
                                Text(
                                    text = stringResource(R.string.reset),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }

                        Button(
                            onClick = {
                                val rate = editRateText.toDoubleOrNull()
                                if (rate != null && rate > 0) {
                                    onSaveCustomRate(baseCurrencyCode, conversion.currencyCode, rate)
                                }
                                editTarget = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(
                                topStart = Dimensions.Radius.xs,
                                topEnd = Dimensions.Radius.xxl,
                                bottomStart = Dimensions.Radius.xs,
                                bottomEnd = Dimensions.Radius.xxl
                            ),
                            modifier = Modifier
                                .padding(end = Spacing.xl)
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(R.string.save),
                                style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            dismissButton = {},
            modifier = Modifier
                .clip(RoundedCornerShape(Dimensions.Radius.md)),
            shape = MaterialTheme.shapes.large
        )
    }
}

@Composable
fun ExchangeRateItem(
    conversion: CurrencyConversion,
    baseCurrency: Currency?,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit = {}
) {
    val baseSymbol = baseCurrency?.symbol ?: ""

    ListItem(
        headlineContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${conversion.currencyCode} (${conversion.symbol})",
                    fontWeight = FontWeight.Bold,
                    fontFamily = MaterialTheme.typography.bodyLarge.fontFamily
                )
                if (conversion.isCustom) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = stringResource(R.string.custom_label),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        },
        supportingContent = {
            Text(
                text = "1 $baseSymbol = ${conversion.displayRate} ${conversion.symbol}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .clip(
                RoundedCornerShape(
                    topStart = if (isFirst) 16.dp else 0.dp,
                    topEnd = if (isFirst) 16.dp else 0.dp,
                    bottomStart = if (isLast) 16.dp else 0.dp,
                    bottomEnd = if (isLast) 16.dp else 0.dp
                )
            )
    )
    if (!isLast) {
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AddCustomCurrencyBottomSheet(
    onDismiss: () -> Unit,
    onSave: (name: String, symbol: String, code: String, rate: Double) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()
    var countryName by remember { mutableStateOf("") }
    var symbol by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(
                        state = scrollState,
                        flingBehavior = rememberOverscrollFlingBehavior { scrollState }
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.custom_currency),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )


                TextField(
                    value = countryName,
                    onValueChange = { countryName = it },
                    shape = RoundedCornerShape(Spacing.md),
                    label = { Text(stringResource(R.string.currency_country_name))},
                    placeholder = { Text(stringResource(R.string.currency_name_hint)) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            0.7f
                        ),
                        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(0.7f),
                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(0.7f)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    TextField(
                        value = code,
                        onValueChange = { if (it.length <= 4) code = it.uppercase() },
                        label = { Text(stringResource(R.string.currency_code))},
                        placeholder = { Text(stringResource(R.string.currency_code_hint)) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                0.7f
                            ),
                            focusedPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(0.7f),
                            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(0.7f)
                        ),
                        shape = RoundedCornerShape(Spacing.md),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    TextField(
                        value = symbol,
                        onValueChange = { if (it.length <= 3) symbol = it },
                        label = { Text(stringResource(R.string.currency_symbol))},
                        placeholder = { Text(stringResource(R.string.currency_symbols_hint)) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                0.7f
                            ),
                            focusedPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(0.7f),
                            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(0.7f)
                        ),
                        shape = RoundedCornerShape(Spacing.md),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                TextField(
                    value = rate,
                    onValueChange = { rate = it },
                    label = { Text(stringResource(R.string.exchange_rate))},
                    placeholder = { Text(stringResource(R.string.exchange_rate_hint)) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            0.7f
                        ),
                        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(0.7f),
                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(0.7f)
                    ),
                    shape = RoundedCornerShape(Spacing.md),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                Text(
                    text = stringResource(R.string.base_currency_hint),
                    color = MaterialTheme.colorScheme.onSurface.copy(0.8f),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 12.dp)
                )

                if (showError) {
                    Text(
                        text = stringResource(R.string.please_fill_all_fields),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(modifier = Modifier.height(80.dp))
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
                // Save button
                Button(
                    onClick = {
                        val rateValue = rate.toDoubleOrNull()
                        if (countryName.isNotBlank() && symbol.isNotBlank() && code.isNotBlank() && rateValue != null && rateValue > 0) {
                            onSave(countryName, symbol, code, rateValue)
                        } else {
                            showError = true
                        }
                    },
                    modifier = Modifier
                        .height(56.dp),
                    shape = MaterialTheme.shapes.extraExtraLarge
                ) {
                    Text(
                        text = stringResource(R.string.save_currency),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
