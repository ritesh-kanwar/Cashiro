package com.ritesh.cashiro.presentation.ui.features.subscriptions

import com.ritesh.cashiro.data.database.entity.SubscriptionEntity
import java.math.BigDecimal

data class SubscriptionsUiState(
    val activeSubscriptions: List<SubscriptionEntity> = emptyList(),
    val totalMonthlyAmount: BigDecimal = BigDecimal.ZERO,
    val totalYearlyAmount: BigDecimal = BigDecimal.ZERO,
    val targetCurrency: String = "INR",
    val isLoading: Boolean = true,
    val lastHiddenSubscription: SubscriptionEntity? = null,
    val selectedSubscription: SubscriptionEntity? = null,
    val convertedAmounts: Map<Long, BigDecimal> = emptyMap(),
    val conversionFailureCount: Int = 0
)
