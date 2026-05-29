package com.cashiroai.shared.data.bootstrap

import com.cashiroai.shared.data.model.SharedCategory

internal object DefaultSharedCategories {
    fun create(nowEpochMillis: Long): List<SharedCategory> {
        return DefaultCategoryData.ALL.mapIndexed { index, seed ->
            SharedCategory(
                name = seed.name,
                colorHex = seed.colorHex,
                isSystem = true,
                isIncome = seed.isIncome,
                displayOrder = index + 1,
                createdAtEpochMillis = nowEpochMillis,
                updatedAtEpochMillis = nowEpochMillis
            )
        }
    }
}
