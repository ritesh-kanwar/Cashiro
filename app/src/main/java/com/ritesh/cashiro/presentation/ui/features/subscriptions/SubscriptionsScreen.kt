package com.ritesh.cashiro.presentation.ui.features.subscriptions

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ritesh.cashiro.data.database.entity.CategoryEntity
import com.ritesh.cashiro.data.database.entity.SubcategoryEntity
import com.ritesh.cashiro.data.database.entity.SubscriptionEntity
import com.ritesh.cashiro.presentation.effects.overScrollVertical
import com.ritesh.cashiro.presentation.effects.rememberOverscrollFlingBehavior
import com.ritesh.cashiro.presentation.ui.components.BrandIcon
import com.ritesh.cashiro.presentation.ui.components.CashiroCard
import com.ritesh.cashiro.presentation.ui.components.CustomTitleTopAppBar
import com.ritesh.cashiro.presentation.ui.components.DeleteSubscriptionDialog
import com.ritesh.cashiro.presentation.ui.components.LoadingCircle
import com.ritesh.cashiro.presentation.ui.features.categories.NavigationContent
import com.ritesh.cashiro.presentation.ui.icons.Bag
import com.ritesh.cashiro.presentation.ui.icons.Calendar
import com.ritesh.cashiro.presentation.ui.icons.Edit2
import com.ritesh.cashiro.presentation.ui.icons.Iconax
import com.ritesh.cashiro.presentation.ui.icons.VideoPlay
import com.ritesh.cashiro.presentation.ui.theme.Dimensions
import com.ritesh.cashiro.presentation.ui.theme.Spacing
import com.ritesh.cashiro.presentation.ui.theme.expense_dark
import com.ritesh.cashiro.presentation.ui.theme.expense_light
import com.ritesh.cashiro.utils.CurrencyFormatter
import com.ritesh.cashiro.utils.SubscriptionUtils
import com.ritesh.cashiro.utils.formatAmount
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import androidx.core.graphics.toColorInt
import com.ritesh.cashiro.presentation.ui.components.SubtitleTag
import com.ritesh.cashiro.presentation.ui.theme.credit_dark
import com.ritesh.cashiro.presentation.ui.theme.credit_light
import com.ritesh.cashiro.presentation.ui.theme.income_dark
import com.ritesh.cashiro.presentation.ui.theme.income_light
import com.ritesh.cashiro.presentation.ui.theme.investment_dark
import com.ritesh.cashiro.presentation.ui.theme.investment_light
import com.ritesh.cashiro.presentation.ui.theme.transfer_dark
import com.ritesh.cashiro.presentation.ui.theme.transfer_light

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun SubscriptionsScreen(
    subscriptionsViewModel: SubscriptionsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onEditSubscription: (Long) -> Unit = {},
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedContentScope: AnimatedVisibilityScope? = null
) {
    val uiState by subscriptionsViewModel.uiState.collectAsState()
    val categoriesMap by subscriptionsViewModel.categoriesMap.collectAsState()
    val subcategoriesMap by subscriptionsViewModel.subcategoriesMap.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.lastHiddenSubscription) {
        uiState.lastHiddenSubscription?.let { subscription ->
            val result = snackbarHostState.showSnackbar(
                message = "${subscription.merchantName} hidden",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                subscriptionsViewModel.undoHide()
            }
        }
    }
    
    val hazeState = remember { HazeState() }
    val lazyListState = rememberLazyListState()
    val scrollBehaviorSmall = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val scrollBehaviorLarge = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    

    Box(
        modifier = Modifier.then(
            if (sharedTransitionScope != null && animatedContentScope != null) {
                with(sharedTransitionScope) {
                    Modifier.sharedBounds(
                        rememberSharedContentState(key = "upcoming_subscriptions_card"),
                        animatedVisibilityScope = animatedContentScope,
                        boundsTransform = { _, _ ->
                            spring(
                                stiffness = Spring.StiffnessLow,
                                dampingRatio = Spring.DampingRatioLowBouncy
                            )
                        },
                        resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(
                            contentScale = ContentScale.Fit
                        )
                    )
                    .skipToLookaheadSize()
                }
            } else Modifier
        ).background(MaterialTheme.colorScheme.background)
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehaviorLarge.nestedScrollConnection),
        topBar = {
            CustomTitleTopAppBar(
                scrollBehaviorSmall = scrollBehaviorSmall,
                scrollBehaviorLarge = scrollBehaviorLarge,
                title = "Subscriptions",
                hasBackButton = true,
                hazeState = hazeState,
                navigationContent = { NavigationContent(onNavigateBack) }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        val selectedSubscription = uiState.selectedSubscription
        var subscriptionToDelete by remember { mutableStateOf<SubscriptionEntity?>(null) }
        
        if (subscriptionToDelete != null) {
            DeleteSubscriptionDialog(
                subscriptionName = subscriptionToDelete!!.merchantName,
                onDismiss = { subscriptionToDelete = null },
                onDelete = {
                    subscriptionToDelete?.let {
                        subscriptionsViewModel.hideSubscription(it.id)
                    }
                    subscriptionToDelete = null
                },
                hazeState = hazeState
            )
        }

        if (selectedSubscription != null) {
            PaymentStatusBottomSheet(
                subscription = selectedSubscription,
                categoryEntity = categoriesMap[selectedSubscription.category],
                subcategoryEntity = subcategoriesMap[selectedSubscription.subcategory],
                convertedAmount = uiState.convertedAmounts[selectedSubscription.id],
                targetCurrency = uiState.targetCurrency,
                onDismiss = { subscriptionsViewModel.selectSubscription(null) },
                onMarkAsPaid = { subscriptionsViewModel.markAsPaid(selectedSubscription) },
                onEdit = {
                    subscriptionsViewModel.selectSubscription(null)
                    onEditSubscription(selectedSubscription.id)
                }
            )
        }
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState)
                .overScrollVertical(),
            flingBehavior = rememberOverscrollFlingBehavior { lazyListState },
            contentPadding = PaddingValues(
                start = Dimensions.Padding.content,
                end = Dimensions.Padding.content,
                top = paddingValues.calculateTopPadding() + Spacing.md,
                bottom = paddingValues.calculateBottomPadding() + Dimensions.Padding.content
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            // Total Monthly & Yearly Subscriptions Summary
            item {
                TotalSubscriptionsSummary(
                    monthlyAmount = uiState.totalMonthlyAmount,
                    yearlyAmount = uiState.totalYearlyAmount,
                    activeCount = uiState.activeSubscriptions.size,
                    currency = uiState.targetCurrency,
                    conversionFailureCount = uiState.conversionFailureCount
                )
            }
            
            // Active Subscriptions
            if (uiState.activeSubscriptions.isNotEmpty()) {
                items(
                    items = uiState.activeSubscriptions,
                    key = { it.id }
                ) { subscription ->
                    val categoryEntity = categoriesMap[subscription.category]
                    val subcategoryEntity = if (categoryEntity != null && subscription.subcategory != null) {
                        subcategoriesMap[subscription.subcategory]
                    } else null

                    SwipeableSubscriptionItem(
                        subscription = subscription,
                        categoryEntity = categoryEntity,
                        subcategoryEntity = subcategoryEntity,
                        convertedAmount = uiState.convertedAmounts[subscription.id],
                        targetCurrency = uiState.targetCurrency,
                        onDelete = { subscriptionToDelete = subscription },
                        onEdit = {
                            onEditSubscription(subscription.id)
                        },
                        onClick = { subscriptionsViewModel.selectSubscription(subscription) }
                    )
                }
            }
            
            // Empty State
            if (uiState.activeSubscriptions.isEmpty() && !uiState.isLoading) {
                item {
                    EmptySubscriptionsState()
                }
            }
            
            // Loading State
            if (uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        LoadingCircle()
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun TotalSubscriptionsSummary(
    monthlyAmount: BigDecimal,
    yearlyAmount: BigDecimal,
    activeCount: Int,
    currency: String,
    conversionFailureCount: Int = 0
) {
    CashiroCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total Subscriptions",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$activeCount active",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                Box(
                    modifier =
                        Modifier.padding(end = Spacing.xs)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(0.5f),
                                shape = CircleShape
                            ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Iconax.VideoPlay,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(Spacing.md))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(0.7f),
                            shape = MaterialTheme.shapes.large
                        )
                        .padding(Spacing.sm),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "MONTHLY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Text(
                        text = CurrencyFormatter.formatCurrency(monthlyAmount, currency),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(0.7f),
                            shape = MaterialTheme.shapes.large
                        )
                        .padding(Spacing.sm),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "YEARLY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))

                    Text(
                        text = CurrencyFormatter.formatCurrency(yearlyAmount, currency),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            if (conversionFailureCount > 0) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = "$conversionFailureCount subscription(s) couldn't be converted to $currency, totals may be incomplete.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableSubscriptionItem(
    subscription: SubscriptionEntity,
    categoryEntity: CategoryEntity? = null,
    subcategoryEntity: SubcategoryEntity? = null,
    convertedAmount: BigDecimal? = null,
    targetCurrency: String? = null,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onClick: () -> Unit
) {
    var showSmsBody by remember { mutableStateOf(false) }
    
    val dismissState = rememberSwipeToDismissBoxState()
    var isInitialized by remember { mutableStateOf(false) }

    // Handle dismissal events
    LaunchedEffect(dismissState.currentValue) {
        if (isInitialized) {
            when (dismissState.currentValue) {
                SwipeToDismissBoxValue.EndToStart -> {
                    // Swiped Left -> Edit
                    onEdit()
                    dismissState.reset()
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    // Swiped Right -> Delete
                    onDelete()
                    dismissState.reset()
                }
                else -> {}
            }
        }
        isInitialized = true
    }

    // Reset the swipe state when the subscription is restored (Undo)
    LaunchedEffect(subscription) {
        if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
            dismissState.reset()
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val color = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.error
                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.primary
                else -> Color.Transparent
            }
            val alignment = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                else -> Alignment.Center
            }
            val icon = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Iconax.Bag
                SwipeToDismissBoxValue.EndToStart -> Iconax.Edit2
                else -> Iconax.Edit2
            }
            val label = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> "Delete"
                SwipeToDismissBoxValue.EndToStart -> "Edit"
                else -> ""
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = color,
                        shape = MaterialTheme.shapes.extraLarge
                    )
                    .padding(horizontal = Dimensions.Padding.content),
                contentAlignment = alignment
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (direction == SwipeToDismissBoxValue.StartToEnd) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        content = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onClick() },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Dimensions.Padding.content),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Brand Icon
                        BrandIcon(
                            merchantName = subscription.merchantName,
                            size = 48.dp,
                            showBackground = true,
                            categoryEntity = categoryEntity,
                            subcategoryEntity = subcategoryEntity
                        )
                        
                        // Content
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = Spacing.sm)
                        ) {
                            Text(
                                text = subscription.merchantName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val today = LocalDate.now()
                                val subscriptionDate = subscription.nextPaymentDate

                                // Date Tag
                                if (subscriptionDate != null) {
                                    val daysUntilNext = ChronoUnit.DAYS.between(today, subscriptionDate)
                                    val isOverdue = subscriptionDate.isBefore(today) && 
                                                   (subscription.lastPaidDate == null || subscription.lastPaidDate!!.isBefore(subscriptionDate))

                                    val dateTagColor = remember(subscriptionDate) {
                                        val colors = listOf(income_dark, expense_dark, credit_dark, transfer_dark, investment_dark)
                                        val dateHash = subscriptionDate.hashCode()
                                        val index = Math.abs(dateHash) % colors.size
                                        colors[index]
                                    }

                                    SubtitleTag(
                                        icon = {
                                            Icon(
                                                imageVector = Iconax.Calendar,
                                                contentDescription = null,
                                                modifier = Modifier.size(10.dp),
                                                tint = (if (isOverdue || daysUntilNext <= 3) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant).copy(0.85f)
                                            )
                                        },
                                        text = when {
                                            isOverdue -> "Overdue"
                                            daysUntilNext == 0L -> "Due today"
                                            daysUntilNext == 1L -> "Due tomorrow"
                                            daysUntilNext in 2..7 -> "Due in $daysUntilNext days"
                                            else -> subscriptionDate.format(DateTimeFormatter.ofPattern("MMM d"))
                                        },
                                        color = if (isOverdue || daysUntilNext <= 3) MaterialTheme.colorScheme.error else dateTagColor
                                    )
                                }

                                // Billing Cycle Tag
                                SubtitleTag(
                                    text = SubscriptionUtils.formatBillingCycle(subscription.billingCycle),
                                    color = MaterialTheme.colorScheme.tertiary
                                )

                                // Category Tag
                                categoryEntity?.let { category ->
                                    SubtitleTag(
                                        text = category.name,
                                        color = try {
                                            Color(category.color.toColorInt())
                                        } catch (e: Exception) {
                                            MaterialTheme.colorScheme.primary
                                        }
                                    )
                                }

                                // SMS indicator if available
                                if (!subscription.smsBody.isNullOrBlank()) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Chat,
                                        contentDescription = "SMS available",
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f)
                                    )
                                }
                            }
                        }
                        
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = subscription.formatAmount(),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = if (!isSystemInDarkTheme()) expense_light else expense_dark
                            )

                            if (convertedAmount != null && targetCurrency != null && subscription.currency != targetCurrency) {
                                Text(
                                    text = "≈ ${CurrencyFormatter.formatCurrency(convertedAmount, targetCurrency)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }
                    }
                }
                
                // SMS Body Display
                if (showSmsBody && !subscription.smsBody.isNullOrBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Dimensions.Padding.content)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Chat,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(Spacing.sm))
                                Text(
                                    text = if (subscription.bankName == "Manual Entry") "Notes" else "Original SMS",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(Spacing.sm))
                            

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = subscription.smsBody,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    modifier = Modifier.padding(Spacing.md)
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentStatusBottomSheet(
    subscription: SubscriptionEntity,
    categoryEntity: CategoryEntity? = null,
    subcategoryEntity: SubcategoryEntity? = null,
    convertedAmount: BigDecimal? = null,
    targetCurrency: String? = null,
    onDismiss: () -> Unit,
    onMarkAsPaid: () -> Unit,
    onEdit: () -> Unit
) {
    var showSmsBody by remember { mutableStateOf(false) }
    val today = LocalDate.now()
    val isOverdue = subscription.nextPaymentDate?.isBefore(today) == true && 
                    (subscription.lastPaidDate == null || subscription.lastPaidDate!!.isBefore(subscription.nextPaymentDate!!))
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Track Payment",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.5f))
                        .clickable { onEdit() }
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector =Iconax.Edit2,
                        contentDescription = "Edit Subscription",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isOverdue) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer.copy(0.7f)
                ),
                shape = MaterialTheme.shapes.large
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BrandIcon(
                        merchantName = subscription.merchantName,
                        size = 56.dp,
                        showBackground = true,
                        categoryEntity = categoryEntity,
                        subcategoryEntity = subcategoryEntity
                    )
                    
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "Payment for ${subscription.merchantName}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isOverdue) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = subscription.formatAmount(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isOverdue) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        val cycleLabel = SubscriptionUtils.formatBillingCycle(subscription.billingCycle)
                        val displayAmount = convertedAmount ?: subscription.amount
                        val displayCurrency = if (convertedAmount != null) {
                            targetCurrency ?: subscription.currency
                        } else {
                            subscription.currency
                        }
                        val monthlyEq = SubscriptionUtils.monthlyEquivalent(displayAmount, subscription.billingCycle)
                        val cycleSubtitle = if (monthlyEq.compareTo(displayAmount) == 0) {
                            cycleLabel
                        } else {
                            "$cycleLabel · ≈ ${CurrencyFormatter.formatCurrency(monthlyEq, displayCurrency)}/mo"
                        }
                        Text(
                            text = cycleSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isOverdue) MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                    else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                        if (subscription.nextPaymentDate != null) {
                            Text(
                                text = if (isOverdue) "Overdue since ${subscription.nextPaymentDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}"
                                       else "Due on ${subscription.nextPaymentDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isOverdue) MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f) 
                                       else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Is this subscription paid?",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(56.dp)
                ) {
                    Text("No", style = MaterialTheme.typography.titleMedium)
                }
                
                Button(
                    onClick = onMarkAsPaid,
                    modifier = Modifier.weight(1f).height(56.dp)
                ) {
                    Text("Yes, it's paid", style = MaterialTheme.typography.titleMedium)
                }
            }
            
            if (!subscription.smsBody.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(24.dp))
                
                TextButton(
                    onClick = { showSmsBody = !showSmsBody }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (showSmsBody) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (showSmsBody) "Hide Original Message" else "Show Original Message")
                    }
                }
                
                if (showSmsBody) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = subscription.smsBody,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace
                            ),
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptySubscriptionsState() {
    CashiroCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.Padding.content),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Iconax.VideoPlay,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Spacing.md))
            Text(
                text = "No subscriptions detected yet",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = "Sync your SMS to detect subscriptions",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
