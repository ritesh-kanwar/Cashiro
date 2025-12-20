package com.ritesh.cashiro.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(
    tableName = "categories",
    indices = [Index(value = ["name"], unique = true)]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    
    @ColumnInfo(name = "name")
    val name: String,
    
    @ColumnInfo(name = "color")
    val color: String,

    @ColumnInfo(name = "icon_res_id", defaultValue = "0")
    val iconResId: Int = 0,
    
    @ColumnInfo(name = "is_system")
    val isSystem: Boolean = false,
    
    @ColumnInfo(name = "is_income")
    val isIncome: Boolean = false,
    
    @ColumnInfo(name = "display_order")
    val displayOrder: Int = 999,
    
    @ColumnInfo(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: LocalDateTime = LocalDateTime.now()
)