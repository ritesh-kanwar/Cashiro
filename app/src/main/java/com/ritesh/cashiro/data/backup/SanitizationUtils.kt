package com.ritesh.cashiro.data.backup

import com.ritesh.cashiro.data.database.entity.*
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Extension functions to sanitize entities deserialized from backup.
 * Gson's reflection-based deserialization can bypass Kotlin's null-safety,
 * resulting in null values for non-nullable fields if they were missing in the JSON.
 * These functions ensure all non-nullable fields have valid default values.
 */

fun CategoryEntity.sanitize(): CategoryEntity {
    return CategoryEntity(
        id = id,
        name = name ?: "",
        color = color ?: "#757575",
        iconResId = iconResId,
        iconName = iconName ?: "",
        description = description ?: "",
        isSystem = isSystem,
        isIncome = isIncome,
        displayOrder = displayOrder,
        defaultName = defaultName,
        defaultColor = defaultColor,
        defaultIconResId = defaultIconResId,
        defaultIconName = defaultIconName,
        defaultDescription = defaultDescription,
        createdAt = createdAt ?: LocalDateTime.now(),
        updatedAt = updatedAt ?: LocalDateTime.now()
    )
}

fun SubcategoryEntity.sanitize(): SubcategoryEntity {
    return SubcategoryEntity(
        id = id,
        categoryId = categoryId,
        name = name ?: "",
        iconResId = iconResId,
        iconName = iconName ?: "",
        color = color ?: "#757575",
        isSystem = isSystem,
        defaultName = defaultName,
        defaultIconResId = defaultIconResId,
        defaultIconName = defaultIconName,
        defaultColor = defaultColor,
        createdAt = createdAt ?: LocalDateTime.now(),
        updatedAt = updatedAt ?: LocalDateTime.now()
    )
}

fun TransactionEntity.sanitize(): TransactionEntity {
    return TransactionEntity(
        id = id,
        amount = amount ?: BigDecimal.ZERO,
        merchantName = merchantName ?: "",
        category = category ?: "",
        subcategory = subcategory,
        transactionType = transactionType ?: TransactionType.EXPENSE,
        dateTime = dateTime ?: LocalDateTime.now(),
        description = description,
        smsBody = smsBody,
        bankName = bankName,
        smsSender = smsSender,
        accountNumber = accountNumber,
        balanceAfter = balanceAfter,
        transactionHash = transactionHash ?: "",
        isRecurring = isRecurring,
        isDeleted = isDeleted,
        createdAt = createdAt ?: LocalDateTime.now(),
        updatedAt = updatedAt ?: LocalDateTime.now(),
        currency = currency ?: "INR",
        fromAccount = fromAccount,
        toAccount = toAccount,
        fromBankName = fromBankName,
        toBankName = toBankName,
        reference = reference,
        billingCycle = billingCycle,
        attachments = attachments ?: "",
        isSample = isSample
    )
}

fun CardEntity.sanitize(): CardEntity {
    return CardEntity(
        id = id,
        cardLast4 = cardLast4 ?: "",
        cardType = cardType ?: CardType.DEBIT,
        bankName = bankName ?: "",
        accountLast4 = accountLast4,
        nickname = nickname,
        isActive = isActive,
        lastBalance = lastBalance,
        lastBalanceSource = lastBalanceSource,
        lastBalanceDate = lastBalanceDate,
        createdAt = createdAt ?: LocalDateTime.now(),
        updatedAt = updatedAt ?: LocalDateTime.now(),
        currency = currency ?: "INR",
        isSample = isSample
    )
}

fun SubscriptionEntity.sanitize(): SubscriptionEntity {
    return SubscriptionEntity(
        id = id,
        merchantName = merchantName ?: "",
        amount = amount ?: BigDecimal.ZERO,
        nextPaymentDate = nextPaymentDate,
        state = state ?: SubscriptionState.ACTIVE,
        bankName = bankName,
        umn = umn,
        category = category,
        subcategory = subcategory,
        smsBody = smsBody,
        createdAt = createdAt ?: LocalDateTime.now(),
        updatedAt = updatedAt ?: LocalDateTime.now(),
        currency = currency ?: "INR",
        billingCycle = billingCycle,
        lastPaidDate = lastPaidDate,
        isSample = isSample
    )
}

fun BudgetEntity.sanitize(): BudgetEntity {
    return BudgetEntity(
        id = id,
        name = name ?: "",
        amount = amount ?: BigDecimal.ZERO,
        year = year,
        month = month,
        currency = currency ?: "INR",
        isActive = isActive,
        createdAt = createdAt ?: LocalDateTime.now(),
        updatedAt = updatedAt ?: LocalDateTime.now(),
        startDate = startDate ?: LocalDateTime.now(),
        endDate = endDate ?: (startDate ?: LocalDateTime.now()).plusMonths(1),
        periodType = periodType ?: BudgetPeriod.MONTHLY,
        trackType = trackType ?: BudgetTrackType.ALL_TRANSACTIONS,
        budgetType = budgetType ?: BudgetType.EXPENSE,
        accountIds = accountIds ?: emptyList(),
        color = color ?: "#4CAF50",
        isSample = isSample
    )
}

fun AccountBalanceEntity.sanitize(): AccountBalanceEntity {
    return AccountBalanceEntity(
        id = id,
        iconResId = iconResId,
        iconName = iconName ?: "",
        bankName = bankName ?: "",
        accountLast4 = accountLast4 ?: "",
        balance = balance ?: BigDecimal.ZERO,
        timestamp = timestamp ?: LocalDateTime.now(),
        transactionId = transactionId,
        creditLimit = creditLimit,
        isCreditCard = isCreditCard,
        smsSource = smsSource,
        sourceType = sourceType,
        createdAt = createdAt ?: LocalDateTime.now(),
        currency = currency ?: "INR",
        isWallet = isWallet,
        color = color ?: "#33B5E5",
        isSample = isSample
    )
}
