package com.ritesh.cashiro.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(
        tableName = "subcategories",
        indices = [Index(value = ["category_id"])],
        foreignKeys =
                [
                        ForeignKey(
                                entity = CategoryEntity::class,
                                parentColumns = ["id"],
                                childColumns = ["category_id"],
                                onDelete = ForeignKey.CASCADE
                        )]
)
data class SubcategoryEntity(
        @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0,
        @ColumnInfo(name = "category_id") val categoryId: Long,
        @ColumnInfo(name = "name") val name: String,
        @ColumnInfo(name = "created_at") val createdAt: LocalDateTime = LocalDateTime.now(),
        @ColumnInfo(name = "updated_at") val updatedAt: LocalDateTime = LocalDateTime.now()
)
