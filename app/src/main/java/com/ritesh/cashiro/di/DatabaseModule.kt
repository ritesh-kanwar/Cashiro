package com.ritesh.cashiro.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ritesh.cashiro.data.database.CashiroDatabase
import com.ritesh.cashiro.data.database.dao.AccountBalanceDao
import com.ritesh.cashiro.data.database.dao.CardDao
import com.ritesh.cashiro.data.database.dao.CategoryDao
import com.ritesh.cashiro.data.database.dao.ChatDao
import com.ritesh.cashiro.data.database.dao.ExchangeRateDao
import com.ritesh.cashiro.data.database.dao.MerchantMappingDao
import com.ritesh.cashiro.data.database.dao.RuleApplicationDao
import com.ritesh.cashiro.data.database.dao.RuleDao
import com.ritesh.cashiro.data.database.dao.SubcategoryDao
import com.ritesh.cashiro.data.database.dao.SubscriptionDao
import com.ritesh.cashiro.data.database.dao.TransactionDao
import com.ritesh.cashiro.data.database.dao.UnrecognizedSmsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.ritesh.cashiro.R

/** Hilt module that provides database-related dependencies. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Provides the singleton instance of PennyWiseDatabase.
     *
     * @param context Application context
     * @return Configured Room database instance
     */
    @Provides
    @Singleton
    fun providePennyWiseDatabase(@ApplicationContext context: Context): CashiroDatabase {
        val database =
                Room.databaseBuilder(
                                context,
                                CashiroDatabase::class.java,
                                CashiroDatabase.DATABASE_NAME
                        )
                        // Add manual migrations here when needed
                        .addMigrations(
                                CashiroDatabase.MIGRATION_12_14,
                                CashiroDatabase.MIGRATION_13_14,
                                CashiroDatabase.MIGRATION_14_15,
                                CashiroDatabase.MIGRATION_20_21,
                                CashiroDatabase.MIGRATION_21_22,
                                CashiroDatabase.MIGRATION_22_23
                        )

                        // Enable auto-migrations
                        // Room will automatically detect schema changes between versions

                        // Add callback to seed default data on first creation
                        .addCallback(DatabaseCallback())
                        .build()

        // Set the singleton instance so BroadcastReceivers can access it
        CashiroDatabase.setInstance(database)

        return database
    }

    /**
     * Provides the TransactionDao from the database.
     *
     * @param database The PennyWiseDatabase instance
     * @return TransactionDao for accessing transaction data
     */
    @Provides
    @Singleton
    fun provideTransactionDao(database: CashiroDatabase): TransactionDao {
        return database.transactionDao()
    }

    /**
     * Provides the SubscriptionDao from the database.
     *
     * @param database The PennyWiseDatabase instance
     * @return SubscriptionDao for accessing subscription data
     */
    @Provides
    @Singleton
    fun provideSubscriptionDao(database: CashiroDatabase): SubscriptionDao {
        return database.subscriptionDao()
    }

    /**
     * Provides the ChatDao from the database.
     *
     * @param database The PennyWiseDatabase instance
     * @return ChatDao for accessing chat message data
     */
    @Provides
    @Singleton
    fun provideChatDao(database: CashiroDatabase): ChatDao {
        return database.chatDao()
    }

    /**
     * Provides the MerchantMappingDao from the database.
     *
     * @param database The PennyWiseDatabase instance
     * @return MerchantMappingDao for accessing merchant mapping data
     */
    @Provides
    @Singleton
    fun provideMerchantMappingDao(database: CashiroDatabase): MerchantMappingDao {
        return database.merchantMappingDao()
    }

    /**
     * Provides the CategoryDao from the database.
     *
     * @param database The PennyWiseDatabase instance
     * @return CategoryDao for accessing category data
     */
    @Provides
    @Singleton
    fun provideCategoryDao(database: CashiroDatabase): CategoryDao {
        return database.categoryDao()
    }

    /**
     * Provides the AccountBalanceDao from the database.
     *
     * @param database The PennyWiseDatabase instance
     * @return AccountBalanceDao for accessing account balance data
     */
    @Provides
    @Singleton
    fun provideAccountBalanceDao(database: CashiroDatabase): AccountBalanceDao {
        return database.accountBalanceDao()
    }

    /**
     * Provides the UnrecognizedSmsDao from the database.
     *
     * @param database The PennyWiseDatabase instance
     * @return UnrecognizedSmsDao for accessing unrecognized SMS data
     */
    @Provides
    @Singleton
    fun provideUnrecognizedSmsDao(database: CashiroDatabase): UnrecognizedSmsDao {
        return database.unrecognizedSmsDao()
    }

    /**
     * Provides the CardDao from the database.
     *
     * @param database The PennyWiseDatabase instance
     * @return CardDao for accessing card data
     */
    @Provides
    @Singleton
    fun provideCardDao(database: CashiroDatabase): CardDao {
        return database.cardDao()
    }

    /**
     * Provides the RuleDao from the database.
     *
     * @param database The PennyWiseDatabase instance
     * @return RuleDao for accessing rule data
     */
    @Provides
    @Singleton
    fun provideRuleDao(database: CashiroDatabase): RuleDao {
        return database.ruleDao()
    }

    /**
     * Provides the RuleApplicationDao from the database.
     *
     * @param database The PennyWiseDatabase instance
     * @return RuleApplicationDao for accessing rule application data
     */
    @Provides
    @Singleton
    fun provideRuleApplicationDao(database: CashiroDatabase): RuleApplicationDao {
        return database.ruleApplicationDao()
    }

    /**
     * Provides the ExchangeRateDao from the database.
     *
     * @param database The PennyWiseDatabase instance
     * @return ExchangeRateDao for accessing exchange rate data
     */
    @Provides
    @Singleton
    fun provideExchangeRateDao(database: CashiroDatabase): ExchangeRateDao {
        return database.exchangeRateDao()
    }

    @Provides
    @Singleton
    fun provideSubcategoryDao(database: CashiroDatabase): SubcategoryDao {
        return database.subcategoryDao()
    }
}

// /** Database callback to seed initial data when database is first created */
// class DatabaseCallback : RoomDatabase.Callback() {
//     override fun onCreate(db: SupportSQLiteDatabase) {
//         super.onCreate(db)

//         // Seed default categories for new installations
//         CoroutineScope(Dispatchers.IO).launch { seedCategories(db) }
//     }

//     private fun seedCategories(db: SupportSQLiteDatabase) {
//         val categories =
//                 listOf(
//                         Triple("Food & Dining", "#FC8019", false),
//                         Triple("Groceries", "#5AC85A", false),
//                         Triple("Transportation", "#000000", false),
//                         Triple("Shopping", "#FF9900", false),
//                         Triple("Bills & Utilities", "#4CAF50", false),
//                         Triple("Entertainment", "#E50914", false),
//                         Triple("Healthcare", "#10847E", false),
//                         Triple("Investments", "#00D09C", false),
//                         Triple("Banking", "#004C8F", false),
//                         Triple("Personal Care", "#6A4C93", false),
//                         Triple("Education", "#673AB7", false),
//                         Triple("Mobile", "#2A3890", false),
//                         Triple("Fitness", "#FF3278", false),
//                         Triple("Insurance", "#0066CC", false),
//                         Triple("Travel", "#00BCD4", false),
//                         Triple("Salary", "#4CAF50", true),
//                         Triple("Income", "#4CAF50", true),
//                         Triple("Others", "#757575", false)
//                 )

//         categories.forEachIndexed { index, (name, color, isIncome) ->
//             db.execSQL(
//                     """
//                 INSERT OR IGNORE INTO categories (name, color, is_system, is_income, display_order, created_at, updated_at)
//                 VALUES (?, ?, 1, ?, ?, datetime('now'), datetime('now'))
//             """.trimIndent(),
//                     arrayOf<Any>(name, color, if (isIncome) 1 else 0, index + 1)
//             )
//         }
//     }
// }
/** Database callback to seed initial data when database is first created */
class DatabaseCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        // Seed default categories and subcategories for new installations
        CoroutineScope(Dispatchers.IO).launch { 
            seedCategories(db)
            seedSubcategories(db)
        }
    }
    private fun seedCategories(db: SupportSQLiteDatabase) {
        val categories = listOf(
            CategoryData(
                name = "Food & Drinks",
                description = "Eating out, Swiggy, Zomato etc.",
                iconResId = R.drawable.type_food_stuffed_flatbread,
                color = "#FC8019",
                isIncome = false,
                displayOrder = 1
            ),
            CategoryData(
                name = "Transport",
                description = "Uber, Ola and other modes of transport.",
                iconResId = R.drawable.type_travel_transport_luggage,
                color = "#0066CC",
                isIncome = false,
                displayOrder = 2
            ),
            CategoryData(
                name = "Shopping",
                description = "Clothes, shoes, furnitures etc.",
                iconResId = R.drawable.type_shopping_shopping_bags,
                color = "#2A3890",
                isIncome = false,
                displayOrder = 3
            )
        )
        categories.forEach { cat ->
            db.execSQL(
                """
                INSERT OR IGNORE INTO categories (
                    name, color, icon_res_id, description, is_system, is_income, display_order,
                    default_name, default_color, default_icon_res_id, default_description,
                    created_at, updated_at
                )
                VALUES (?, ?, ?, ?, 1, ?, ?, ?, ?, ?, ?, datetime('now'), datetime('now'))
            """.trimIndent(),
                arrayOf<Any>(
                    cat.name, cat.color, cat.iconResId, cat.description, 
                    if (cat.isIncome) 1 else 0, cat.displayOrder,
                    cat.name, cat.color, cat.iconResId, cat.description
                )
            )
        }
    }
    private fun seedSubcategories(db: SupportSQLiteDatabase) {
        // Get category IDs
        val foodCategoryId = getCategoryId(db, "Food & Drinks")
        val transportCategoryId = getCategoryId(db, "Transport")
        val shoppingCategoryId = getCategoryId(db, "Shopping")
        // Food & Drinks subcategories
        val foodSubcategories = listOf(
            "Eating out", "Take Away", "Tea & Coffee", "Fast Food", "Snacks",
            "Swiggy", "Zomato", "Sweets", "Liquor", "Beverages",
            "Date", "Pizza", "Tiffin", "Others"
        )
        
        foodSubcategories.forEach { name ->
            insertSubcategory(
                db, foodCategoryId, name,
                R.drawable.type_food_stuffed_flatbread,
                "#FC8019"
            )
        }
        // Transport subcategories
        val transportSubcategories = listOf(
            "Uber", "Rapido", "Auto", "Cab", "Train", "Metro", "Bus", "Bike",
            "Fuel", "EV Charge", "Flight", "Parking", "FASTag", "Tolls",
            "Lounge", "Fine", "Others"
        )
        
        transportSubcategories.forEach { name ->
            insertSubcategory(
                db, transportCategoryId, name,
                R.drawable.type_travel_transport_luggage,
                "#0066CC"
            )
        }
        // Shopping subcategories
        val shoppingSubcategories = listOf(
            "Clothes", "Footwear", "Electronics", "Festival", "Video games",
            "Books", "Plants", "Jewellery", "Furniture", "Appliances",
            "Utensils", "Vehicle", "Cosmetics", "Toys", "Stationery",
            "Glasses", "Devotional", "Others"
        )
        
        shoppingSubcategories.forEach { name ->
            insertSubcategory(
                db, shoppingCategoryId, name,
                R.drawable.type_shopping_shopping_bags,
                "#2A3890"
            )
        }
    }
    private fun getCategoryId(db: SupportSQLiteDatabase, categoryName: String): Long {
        val cursor = db.query("SELECT id FROM categories WHERE name = ?", arrayOf(categoryName))
        return if (cursor.moveToFirst()) {
            cursor.getLong(0).also { cursor.close() }
        } else {
            cursor.close()
            -1L
        }
    }
    private fun insertSubcategory(
        db: SupportSQLiteDatabase,
        categoryId: Long,
        name: String,
        iconResId: Int,
        color: String
    ) {
        db.execSQL(
            """
            INSERT OR IGNORE INTO subcategories (
                category_id, name, icon_res_id, color, is_system,
                default_name, default_icon_res_id, default_color,
                created_at, updated_at
            )
            VALUES (?, ?, ?, ?, 1, ?, ?, ?, datetime('now'), datetime('now'))
        """.trimIndent(),
            arrayOf<Any>(categoryId, name, iconResId, color, name, iconResId, color)
        )
    }
    private data class CategoryData(
        val name: String,
        val description: String,
        val iconResId: Int,
        val color: String,
        val isIncome: Boolean,
        val displayOrder: Int
    )
}
