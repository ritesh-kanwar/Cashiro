package com.ritesh.cashiro.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ritesh.cashiro.data.database.CashiroDatabase
import com.ritesh.cashiro.data.database.dao.AccountBalanceDao
import com.ritesh.cashiro.data.database.dao.BudgetDao
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
import com.ritesh.cashiro.data.database.dao.WebhookCursorDao
import com.ritesh.cashiro.data.database.dao.WebhookLogDao
import com.ritesh.cashiro.data.database.dao.WebhookProfileDao
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
import com.ritesh.cashiro.utils.IconResolutionUtils

/** Hilt module that provides database-related dependencies. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Provides the singleton instance of CashiroDatabase.
     *
     * @param context Application context
     * @return Configured Room database instance
     */
    @Provides
    @Singleton
    fun provideCashiroDatabase(@ApplicationContext context: Context): CashiroDatabase {
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
                .addCallback(DatabaseCallback(context))
                .build()

        // Set the singleton instance so BroadcastReceivers can access it
        CashiroDatabase.setInstance(database)

        return database
    }

    /**
     * Provides the TransactionDao from the database.
     *
     * @param database The CashiroDatabase instance
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
     * @param database The CashiroDatabase instance
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
     * @param database The CashiroDatabase instance
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
     * @param database The CashiroDatabase instance
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
     * @param database The CashiroDatabase instance
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
     * @param database The CashiroDatabase instance
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
     * @param database The CashiroDatabase instance
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
     * @param database The CashiroDatabase instance
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
     * @param database The CashiroDatabase instance
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
     * @param database The CashiroDatabase instance
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
     * @param database The CashiroDatabase instance
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

    @Provides
    @Singleton
    fun provideBudgetDao(database: CashiroDatabase): BudgetDao {
        return database.budgetDao()
    }

    @Provides
    @Singleton
    fun provideWebhookProfileDao(database: CashiroDatabase): WebhookProfileDao {
        return database.webhookProfileDao()
    }

    @Provides
    @Singleton
    fun provideWebhookLogDao(database: CashiroDatabase): WebhookLogDao {
        return database.webhookLogDao()
    }

    @Provides
    @Singleton
    fun provideWebhookCursorDao(database: CashiroDatabase): WebhookCursorDao {
        return database.webhookCursorDao()
    }
}
/** Database callback to seed initial data when database is first created */
class DatabaseCallback(private val context: Context) : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        // Seed default categories and subcategories for new installations
        CoroutineScope(Dispatchers.IO).launch { 
            seedCategories(db, context)
            seedSubcategories(db, context)
        }
    }
    private fun seedCategories(db: SupportSQLiteDatabase, context: Context) {
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
                iconResId = R.drawable.type_travel_transport_airplane,
                color = "#0066CC",
                isIncome = false,
                displayOrder = 2
            ),
            CategoryData(
                name = "Shopping",
                description = "Clothes, shoes, furnitures etc.",
                iconResId = R.drawable.type_shopping_shopping_bags,
                color = "#893BBE",
                isIncome = false,
                displayOrder = 3
            ),
            CategoryData(
                name = "Groceries",
                description = "Kitchen and other household supplies",
                iconResId = R.drawable.type_groceries_bread,
                color = "#9E7155",
                isIncome = false,
                displayOrder = 4
            ),
            CategoryData(
                name = "Home",
                description = "Household related expenses",
                iconResId = R.drawable.type_event_and_place_house,
                color = "#FFC107",
                isIncome = false,
                displayOrder = 5
            ),
            CategoryData(
                name = "Entertainment",
                description = "Movies, Concerts and other recreations",
                iconResId = R.drawable.type_snack_popcorn,
                color = "#CC1A56",
                isIncome = false,
                displayOrder = 6
            ),
            CategoryData(
                name = "Events",
                description = "Being social while putting a dent in bank account",
                iconResId = R.drawable.type_event_and_place_party_popper,
                color = "#9C27B0",
                isIncome = false,
                displayOrder = 7
            ),
            CategoryData(
                name = "Travel",
                description = "Exploration, fun and vacations!",
                iconResId = R.drawable.type_travel_transport_luggage,
                color = "#0066CC",
                isIncome = false,
                displayOrder = 8
            ),
            CategoryData(
                name = "Medical",
                description = "Medicines, Doctor consultations etc",
                iconResId = R.drawable.type_health_pill,
                color = "#FF0041",
                isIncome = false,
                displayOrder = 9
            ),
            CategoryData(
                name = "Personal",
                description = "Money spent on yourself",
                iconResId = R.drawable.type_tool_electronic_scissors,
                color = "#9C27B0",
                isIncome = false,
                displayOrder = 10
            ),
            CategoryData(
                name = "Fitness",
                description = "Things to keep your biological machinery in tune",
                iconResId = R.drawable.type_sports_baseball,
                color = "#91CC4D",
                isIncome = false,
                displayOrder = 11
            ),
            CategoryData(
                name = "Services",
                description = "Professional tasks provided for a fee",
                iconResId = R.drawable.type_tool_electronic_high_voltage,
                color = "#FF9800",
                isIncome = false,
                displayOrder = 12
            ),
            CategoryData(
                name = "Bill",
                description = "Rent, Wi-fi, electricity and other bills",
                iconResId = R.drawable.type_travel_transport_admission_tickets,
                color = "#FF0041",
                isIncome = false,
                displayOrder = 13
            ),
            CategoryData(
                name = "Subscription",
                description = "Recurring payment to online services",
                iconResId = R.drawable.type_tool_electronic_clapper_board,
                color = "#5CCC4D",
                isIncome = false,
                displayOrder = 14
            ),
            CategoryData(
                name = "EMI",
                description = "Repayment of Loan",
                iconResId = R.drawable.type_travel_transport_automobile,
                color = "#FF0041",
                isIncome = false,
                displayOrder = 15
            ),
            CategoryData(
                name = "Credit Bill",
                description = "Credit Card & other services settlement",
                iconResId = R.drawable.type_stationary_card_file_box,
                color = "#FF9800",
                isIncome = false,
                displayOrder = 16
            ),
            CategoryData(
                name = "Investment",
                description = "Money put towards investment",
                iconResId = R.drawable.type_flower_and_tree_herb,
                color = "#91CC4D",
                isIncome = false,
                displayOrder = 17
            ),
            CategoryData(
                name = "Support",
                description = "Financial support for loved ones",
                iconResId = R.drawable.type_health_stethoscope,
                color = "#673AB7",
                isIncome = false,
                displayOrder = 18
            ),
            CategoryData(
                name = "Insurance",
                description = "Payment towards insurance premiums",
                iconResId = R.drawable.type_health_mending_heart,
                color = "#FF0041",
                isIncome = false,
                displayOrder = 19
            ),
            CategoryData(
                name = "Tax",
                description = "Income tax, property tac, etc",
                iconResId = R.drawable.type_finance_chart_decreasing,
                color = "#FF5722",
                isIncome = false,
                displayOrder = 20
            ),
            CategoryData(
                name = "Top-up",
                description = "Money added to online wallet",
                iconResId = R.drawable.type_finance_money_bag,
                color = "#FF9800",
                isIncome = false,
                displayOrder = 21
            ),
            CategoryData(
                name = "Children",
                description = "It takes a village to raise a child & a ton of cash",
                iconResId = R.drawable.type_event_and_place_houses,
                color = "#8BC34A",
                isIncome = false,
                displayOrder = 22
            ),
            CategoryData(
                name = "Pet Care",
                description = "Money spent taking care of your snugglebug",
                iconResId = R.drawable.type_animal_dog_face,
                color = "#F44336",
                isIncome = false,
                displayOrder = 23
            ),
            CategoryData(
                name = "Business",
                description = "24/7 over 9 to 5",
                iconResId = R.drawable.type_finance_classical_building,
                color = "#795548",
                isIncome = false,
                displayOrder = 24
            ),
            CategoryData(
                name = "Miscellaneous",
                description = "Everything else",
                iconResId = R.drawable.type_stationary_clipboard,
                color = "#91CC4D",
                isIncome = false,
                displayOrder = 25
            ),
            CategoryData(
                name = "Self Transfer",
                description = "Transfer between personal Bank accounts",
                iconResId = R.drawable.type_finance_bank,
                color = "#795548",
                isIncome = false,
                displayOrder = 26
            ),
            CategoryData(
                name = "Savings",
                description = "For goals and dreams",
                iconResId = R.drawable.type_sports_bullseye,
                color = "#FF0041",
                isIncome = false,
                displayOrder = 27
            ),
            CategoryData(
                name = "Gift",
                description = "Money gifted or spent buying gifts :)",
                iconResId = R.drawable.type_stationary_wrapped_gift,
                color = "#FF5722",
                isIncome = false,
                displayOrder = 25
            ),
            CategoryData(
                name = "Lent",
                description = "Money lent with expectation of return",
                iconResId = R.drawable.type_finance_money_with_wings,
                color = "#4CAF50",
                isIncome = false,
                displayOrder = 28
            ),
            CategoryData(
                name = "Donation",
                description = "Contributions to charities and NGOs",
                iconResId = R.drawable.type_health_drop_of_blood,
                color = "#FF4081",
                isIncome = false,
                displayOrder = 29
            ),
            CategoryData(
                name = "Hidden Charges",
                description = "Bank's Hidden subscription charges",
                iconResId = R.drawable.type_animal_goblin,
                color = "#F44336",
                isIncome = false,
                displayOrder = 30
            ),
            CategoryData(
                name = "Cash Withdrawal",
                description = "Cash taken out from ATM or bank",
                iconResId = R.drawable.type_finance_dollar_banknote,
                color = "#8BC34A",
                isIncome = false,
                displayOrder = 31
            ),
            CategoryData(
                name = "Income",
                description = "Generic income",
                iconResId = R.drawable.type_finance_money_bag,
                color = "#4CAF50",
                isIncome = true,
                displayOrder = 0
            )
        )
        categories.forEach { cat ->
            val iconName = IconResolutionUtils.resIdToName(context, cat.iconResId)
            db.execSQL(
                """
                INSERT OR IGNORE INTO categories (
                    name, color, icon_res_id, icon_name, description, is_system, is_income, display_order,
                    default_name, default_color, default_icon_res_id, default_icon_name, default_description,
                    created_at, updated_at
                )
                VALUES (?, ?, ?, ?, ?, 1, ?, ?, ?, ?, ?, ?, ?, datetime('now'), datetime('now'))
            """.trimIndent(),
                arrayOf<Any>(
                    cat.name, cat.color, cat.iconResId, iconName, cat.description, 
                    if (cat.isIncome) 1 else 0, cat.displayOrder,
                    cat.name, cat.color, cat.iconResId, iconName, cat.description
                )
            )
        }
    }
    private fun seedSubcategories(db: SupportSQLiteDatabase, context: Context) {
        // Get category IDs
        val foodCategoryId = getCategoryId(db, "Food & Drinks")
        val transportCategoryId = getCategoryId(db, "Transport")
        val shoppingCategoryId = getCategoryId(db, "Shopping")
        val groceriesCategoryId = getCategoryId(db, "Groceries")
        val homeCategoryId = getCategoryId(db, "Home")
        val entertainmentCategoryId = getCategoryId(db, "Entertainment")
        val eventsCategoryId = getCategoryId(db, "Events")
        val travelCategoryId = getCategoryId(db, "Travel")
        val medicalCategoryId = getCategoryId(db, "Medical")
        val personalCategoryId = getCategoryId(db, "Personal")
        val fitnessCategoryId = getCategoryId(db, "Fitness")
        val servicesCategoryId = getCategoryId(db, "Services")
        val billCategoryId = getCategoryId(db, "Bill")
        val subscriptionCategoryId = getCategoryId(db, "Subscription")
        val emiCategoryId = getCategoryId(db, "EMI")
        val creditBillCategoryId = getCategoryId(db, "Credit Bill")
        val investmentCategoryId = getCategoryId(db, "Investment")
        val supportCategoryId = getCategoryId(db, "Support")
        val insuranceCategoryId = getCategoryId(db, "Insurance")
        val taxCategoryId = getCategoryId(db, "Tax")
        val topUpCategoryId = getCategoryId(db, "Top-up")
        val childrenCategoryId = getCategoryId(db, "Children")
        val petCareCategoryId = getCategoryId(db, "Pet Care")
        val businessCategoryId = getCategoryId(db, "Business")
        val miscellaneousCategoryId = getCategoryId(db, "Miscellaneous")
        val selfTransferCategoryId = getCategoryId(db, "Self Transfer")
        val savingsCategoryId = getCategoryId(db, "Savings")
        val giftCategoryId = getCategoryId(db, "Gift")
        val lentCategoryId = getCategoryId(db, "Lent")
        val donationCategoryId = getCategoryId(db, "Donation")
        val hiddenChargesCategoryId = getCategoryId(db, "Hidden Charges")
        val cashWithdrawalCategoryId = getCategoryId(db, "Cash Withdrawal")
        val returnCategoryId = getCategoryId(db, "Return")
        val incomeCategoryId = getCategoryId(db, "Income")

        

        // Food & Drinks subcategories with specific icons and colors
        val foodSubcategories = listOf(
            SubcategoryData("Eating out", R.drawable.type_food_dining, "#423D3A"),
            SubcategoryData("Take Away", R.drawable.type_food_takeout, "#423D3A"),
            SubcategoryData("Tea & Coffee", R.drawable.type_beverages_tea, "#B75300"),
            SubcategoryData("Fast Food", R.drawable.type_food_hamburger, "#FF5722"),
            SubcategoryData("Snacks", R.drawable.type_snack_cookie, "#993414"),
            SubcategoryData("Swiggy", R.drawable.ic_brand_swiggy, "#FF5722"),
            SubcategoryData("Zomato", R.drawable.ic_brand_zomato, "#FF0041"),
            SubcategoryData("Sweets", R.drawable.type_sweet_cupcake, "#9C27B0"),
            SubcategoryData("Liquor", R.drawable.type_beverages_beer, "#FF9800"),
            SubcategoryData("Beverages", R.drawable.type_beverages_bubble_tea, "#995B00"),
            SubcategoryData("Date", R.drawable.type_food_sushi, "#FF5722"),
            SubcategoryData("Pizza", R.drawable.type_food_pizza, "#FF5722"),
            SubcategoryData("Tiffin", R.drawable.type_food_bento_box, "#66483D"),
        )
        
        foodSubcategories.forEach { subcategory ->
            insertSubcategory(
                db, context, foodCategoryId, subcategory.name,
                subcategory.iconResId,
                subcategory.color
            )
        }
        // Transport subcategories
        val transportSubcategories = listOf(
            SubcategoryData("Uber", R.drawable.ic_brand_uber, "#423D3A"),
            SubcategoryData("Rapido", R.drawable.ic_brand_rapido, "#423D3A"),
            SubcategoryData("Auto", R.drawable.type_travel_transport_auto_rickshaw, "#B75300"),
            SubcategoryData("Cab", R.drawable.type_travel_transport_taxi, "#FF5722"),
            SubcategoryData("Train", R.drawable.type_travel_transport_high_speed_train, "#993414"),
            SubcategoryData("Metro", R.drawable.type_travel_transport_metro, "#FF5722"),
            SubcategoryData("Bus", R.drawable.type_travel_transport_bus, "#FF0041"),
            SubcategoryData("Bike", R.drawable.type_travel_transport_motorcycle, "#9C27B0"),
            SubcategoryData("Fuel", R.drawable.type_travel_transport_fuel_pump, "#FF9800"),
            SubcategoryData("Ev Charge", R.drawable.type_tool_electronic_high_voltage, "#995B00"),
            SubcategoryData("Flights", R.drawable.type_travel_transport_airplane, "#FF5722"),
            SubcategoryData("Parking", R.drawable.type_travel_transport_ticket, "#FF5722"),
            SubcategoryData("FASTag", R.drawable.type_travel_transport_ticket, "#66483D"),
            SubcategoryData("Tolls", R.drawable.type_travel_transport_ticket, "#66483D"),
            SubcategoryData("Lounge", R.drawable.type_travel_transport_luggage, "#66483D"),
            SubcategoryData("Fine", R.drawable.type_travel_transport_ticket, "#66483D"),
        )

        transportSubcategories.forEach { subcategory ->
            insertSubcategory(
                db, context, transportCategoryId, subcategory.name,
                subcategory.iconResId,
                subcategory.color
            )
        }

        // Shopping subcategories
        val shoppingSubcategories = listOf(
            SubcategoryData("Clothes", R.drawable.type_shopping_necktie, "#423D3A"),
            SubcategoryData("Footwear", R.drawable.type_shopping_mans_shoe, "#423D3A"),
            SubcategoryData("Electronics", R.drawable.type_tool_electronic_desktop_computer, "#B75300"),
            SubcategoryData("Festival", R.drawable.type_event_and_place_firecracker, "#FF5722"),
            SubcategoryData("Video games", R.drawable.type_tool_electronic_video_game, "#993414"),
            SubcategoryData("Books", R.drawable.type_stationary_blue_book, "#FF5722"),
            SubcategoryData("Plants", R.drawable.type_flower_and_tree_potted_plant, "#FF0041"),
            SubcategoryData("Jewellery", R.drawable.type_shopping_gem_stone, "#9C27B0"),
            SubcategoryData("Furniture", R.drawable.type_event_and_place_couch_and_lamp, "#FF9800"),
            SubcategoryData("Appliances", R.drawable.type_tool_electronic_television, "#995B00"),
            SubcategoryData("Utensils", R.drawable.type_tool_electronic_hammer_and_wrench, "#FF5722"),
            SubcategoryData("Vehicle", R.drawable.type_travel_transport_automobile, "#FF5722"),
            SubcategoryData("Cosmetics", R.drawable.type_shopping_nail_polish, "#66483D"),
            SubcategoryData("Toys", R.drawable.type_shopping_top_hat, "#66483D"),
            SubcategoryData("Stationery", R.drawable.type_stationary_artist_palette, "#66483D"),
            SubcategoryData("Glasses", R.drawable.type_shopping_glasses, "#66483D"),
            SubcategoryData("Devotional", R.drawable.type_event_and_place_diya_lamp, "#66483D"),
        )
        shoppingSubcategories.forEach { subcategory ->
            insertSubcategory(
                db, context, shoppingCategoryId, subcategory.name,
                subcategory.iconResId,
                subcategory.color
            )
        }

        // Groceries subcategories
        val groceriesSubcategories = listOf(
            SubcategoryData("Staples", R.drawable.type_vegetable_beans, "#423D3A"),
            SubcategoryData("Vegetables", R.drawable.type_vegetable_broccoli, "#423D3A"),
            SubcategoryData("Fruits", R.drawable.type_fruit_mango, "#B75300"),
            SubcategoryData("Meat", R.drawable.type_groceries_cut_of_meat, "#FF5722"),
            SubcategoryData("Eggs", R.drawable.type_groceries_egg, "#993414"),
            SubcategoryData("Bakery", R.drawable.type_groceries_baguette_bread, "#FF5722"),
            SubcategoryData("Dairy", R.drawable.type_groceries_glass_of_milk, "#FF0041"),
            SubcategoryData("Zepto", R.drawable.ic_brand_zepto, "#9C27B0"),
        )
        groceriesSubcategories.forEach { subcategory ->
            insertSubcategory(
                db, context, groceriesCategoryId, subcategory.name,
                subcategory.iconResId,
                subcategory.color
            )
        }

        // Home subcategories
        val homeSubcategories = listOf(
            SubcategoryData("Essentials", R.drawable.type_groceries_basket, "#423D3A"),
            SubcategoryData("Toiletries", R.drawable.type_groceries_soap, "#423D3A"),
            SubcategoryData("Decor", R.drawable.type_flower_and_tree_hibiscus, "#B75300"),
            SubcategoryData("Cleaning", R.drawable.type_flower_and_tree_leaf_fluttering_in_wind, "#FF5722"),
            SubcategoryData("Upkeep", R.drawable.type_groceries_sponge, "#993414"),
            SubcategoryData("Painting", R.drawable.type_stationary_artist_palette, "#FF5722"),
            SubcategoryData("Renovation", R.drawable.type_tool_electronic_hammer_and_wrench, "#FF0041"),
            SubcategoryData("Pest-control", R.drawable.type_animal_lady_beetle, "#9C27B0"),
            SubcategoryData("Construction", R.drawable.type_tool_electronic_hammer, "#FF9800"),
        )
        homeSubcategories.forEach { subcategory ->
            insertSubcategory(
                db, context, homeCategoryId, subcategory.name,
                subcategory.iconResId,
                subcategory.color
            )
        }

        // Entertainment subcategories
        val entertainmentSubcategories = listOf(
            SubcategoryData("Movies", R.drawable.type_snack_french_fries, "#423D3A"),
            SubcategoryData("Shows", R.drawable.type_tool_electronic_clapper_board, "#423D3A"),
            SubcategoryData("Bowling", R.drawable.type_sports_bowling, "#B75300"),
            SubcategoryData("Tickets", R.drawable.type_travel_transport_admission_tickets, "#FF5722"),
        )
        entertainmentSubcategories.forEach { subcategory ->
            insertSubcategory(
                db, context, entertainmentCategoryId, subcategory.name,
                subcategory.iconResId,
                subcategory.color
            )
        }
        // Events subcategories
        val eventsSubcategories = listOf(
            SubcategoryData("Party", R.drawable.type_event_and_place_party_popper, "#423D3A"),
            SubcategoryData("Birthday", R.drawable.type_sweet_birthday_cake, "#FF5722"),
            SubcategoryData("Spiritual", R.drawable.type_event_and_place_diya_lamp, "#423D3A"),
            SubcategoryData("Wedding", R.drawable.type_event_and_place_wedding, "#B75300"),
        )
        eventsSubcategories.forEach { subcategory ->
            insertSubcategory(
                db, context, eventsCategoryId, subcategory.name,
                subcategory.iconResId,
                subcategory.color
            )
        }

        // travel subcategories
        val travelSubcategories = listOf(
            SubcategoryData("Activities", R.drawable.type_sports_trophy, "#423D3A"),
            SubcategoryData("Camping", R.drawable.type_event_and_place_camping, "#423D3A"),
            SubcategoryData("Hotel", R.drawable.type_event_and_place_hotel, "#B75300"),
            SubcategoryData("Commute", R.drawable.type_event_and_place_couch_and_lamp, "#FF5722"),
            SubcategoryData("Visa fees", R.drawable.type_travel_transport_ticket, "#993414"),
            SubcategoryData("Hostel", R.drawable.type_finance_classical_building, "#FF5722"),
            SubcategoryData("Airbnb", R.drawable.ic_brand_airbnb, "#FF0041"),
            SubcategoryData("Oyo", R.drawable.ic_brand_oyo, "#9C27B0"),
        )
        travelSubcategories.forEach { subcategory ->
            insertSubcategory(
                db, context, travelCategoryId, subcategory.name,
                subcategory.iconResId,
                subcategory.color
            )
        }

        // medical subcategories
        val medicalSubcategories = listOf(
            SubcategoryData("Medicines", R.drawable.type_health_pill, "#423D3A"),
            SubcategoryData("Hospital", R.drawable.type_health_hospital, "#423D3A"),
            SubcategoryData("Clinic", R.drawable.type_health_stethoscope, "#B75300"),
            SubcategoryData("Dentist", R.drawable.type_health_tooth, "#FF5722"),
            SubcategoryData("Lab test", R.drawable.type_shopping_lab_coat, "#993414"),
            SubcategoryData("Hygiene", R.drawable.type_health_adhesive_bandage, "#FF5722"),
        )
        medicalSubcategories.forEach { subcategory ->
            insertSubcategory(
                db, context, medicalCategoryId, subcategory.name,
                subcategory.iconResId,
                subcategory.color
            )
        }

        // personal subcategories
        val personalSubcategories = listOf(
            SubcategoryData("Self-care", R.drawable.type_groceries_lotion_bottle, "#423D3A"),
            SubcategoryData("Grooming", R.drawable.type_tool_electronic_scissors, "#423D3A"),
            SubcategoryData("Hobbies", R.drawable.type_sports_basketball, "#B75300"),
            SubcategoryData("Vices", R.drawable.type_event_and_place_firecracker, "#FF5722"),
            SubcategoryData("Therapy", R.drawable.type_health_mending_heart, "#993414"),
        )
        personalSubcategories.forEach { subcategory ->
            insertSubcategory(
                db, context, personalCategoryId, subcategory.name,
                subcategory.iconResId,
                subcategory.color
            )
        }

        // fitness subcategories
        val fitnessSubcategories = listOf(
            SubcategoryData("Gym", R.drawable.type_sports_flexed_biceps_light, "#423D3A"),
            SubcategoryData("Badminton", R.drawable.type_sports_badminton, "#423D3A"),
            SubcategoryData("Football", R.drawable.type_sports_soccer_ball, "#B75300"),
            SubcategoryData("Cricket", R.drawable.type_sports_cricket_game, "#FF5722"),
            SubcategoryData("Classes", R.drawable.type_stationary_books, "#993414"),
            SubcategoryData("Equipment", R.drawable.type_tool_electronic_screwdriver, "#FF5722"),
            SubcategoryData("Nutrition", R.drawable.type_vegetable_broccoli, "#FF0041"),
        )
        fitnessSubcategories.forEach { subcategory ->
            insertSubcategory(
                db, context, fitnessCategoryId, subcategory.name,
                subcategory.iconResId,
                subcategory.color
            )
        }

        // services subcategories
        val servicesSubcategories = listOf(
            SubcategoryData("Laundry", R.drawable.type_shopping_necktie, "#423D3A"),
            SubcategoryData("Tailor", R.drawable.type_shopping_scarf, "#423D3A"),
            SubcategoryData("Courier", R.drawable.type_travel_transport_package, "#B75300"),
            SubcategoryData("Carpenter", R.drawable.type_tool_electronic_carpentry_saw, "#FF5722"),
            SubcategoryData("Plumber", R.drawable.type_tool_electronic_toolbox, "#993414"),
            SubcategoryData("Mechanic", R.drawable.type_tool_electronic_hammer_and_wrench, "#FF5722"),
            SubcategoryData("Photographer", R.drawable.type_tool_electronic_camera_with_flash, "#FF0041"),
            SubcategoryData("Driver", R.drawable.type_travel_transport_oncoming_taxi, "#9C27B0"),
            SubcategoryData("Vehicle Wash", R.drawable.type_travel_transport_automobile, "#FF9800"),
            SubcategoryData("Electrician", R.drawable.type_tool_electronic_high_voltage, "#995B00"),
            SubcategoryData("Painting", R.drawable.type_stationary_artist_palette, "#FF5722"),
            SubcategoryData("Xerox", R.drawable.type_stationary_card_index, "#FF5722"),
            SubcategoryData("Legal", R.drawable.type_stationary_reminder_ribbon, "#66483D"),
            SubcategoryData("Advisor", R.drawable.type_stationary_bookmark, "#66483D"),
            SubcategoryData("Repair", R.drawable.type_tool_electronic_hammer_and_pick, "#66483D"),
            SubcategoryData("Logistics", R.drawable.type_travel_transport_delivery_truck, "#66483D"),
        )
        servicesSubcategories.forEach { subcategory ->
            insertSubcategory(
                db, context, servicesCategoryId, subcategory.name,
                subcategory.iconResId,
                subcategory.color
            )
        }

        // bill subcategories
        val billSubcategories = listOf(
            SubcategoryData("Phone", R.drawable.type_tool_electronic_mobile_phone, "#423D3A"),
            SubcategoryData("Rent", R.drawable.type_event_and_place_house, "#423D3A"),
            SubcategoryData("Water", R.drawable.type_beverages_sake, "#B75300"),
            SubcategoryData("Electricity", R.drawable.type_tool_electronic_high_voltage, "#FF5722"),
            SubcategoryData("Gas", R.drawable.type_travel_transport_fuel_pump, "#993414"),
            SubcategoryData("Internet", R.drawable.type_travel_transport_globe_showing_asia_australia, "#FF5722"),
            SubcategoryData("House Help", R.drawable.type_flower_and_tree_potted_plant, "#FF0041"),
            SubcategoryData("Education", R.drawable.type_event_and_place_graduation_cap, "#9C27B0"),
            SubcategoryData("DTH", R.drawable.type_tool_electronic_video_camera, "#FF9800"),
            SubcategoryData("Cook", R.drawable.type_food_curry_rice, "#995B00"),
            SubcategoryData("Maintenance", R.drawable.type_tool_electronic_hammer_and_wrench, "#FF5722"),
        )
        billSubcategories.forEach { subcategory ->
            insertSubcategory(
                db, context, billCategoryId, subcategory.name,
                subcategory.iconResId,
                subcategory.color
            )
        }

        // subscription subcategories
        val subscriptionSubcategories = listOf(
            SubcategoryData("Software", R.drawable.type_tool_electronic_laptop, "#423D3A"),
            SubcategoryData("News", R.drawable.type_stationary_newspaper, "#423D3A"),
            SubcategoryData("Netflix", R.drawable.ic_brand_netflix, "#B75300"),
            SubcategoryData("Prime", R.drawable.ic_brand_amazon_prime, "#FF5722"),
            SubcategoryData("Youtube", R.drawable.ic_brand_youtube, "#993414"),
            SubcategoryData("Youtube Music", R.drawable.ic_brand_youtube_music, "#FF9800"),
            SubcategoryData("Spotify", R.drawable.ic_brand_spotify, "#FF5722"),
            SubcategoryData("Google", R.drawable.ic_brand_google, "#FF0041"),
            SubcategoryData("Learning", R.drawable.type_stationary_writing_hand_light, "#9C27B0"),
            SubcategoryData("Apple Tv", R.drawable.ic_brand_apple_tv, "#FF9800"),
            SubcategoryData("Apple Music", R.drawable.ic_brand_apple_music, "#FF9800"),
            SubcategoryData("Bumble", R.drawable.ic_brand_bumble, "#995B00"),
            SubcategoryData("JioCinema", R.drawable.ic_brand_jiocinema, "#FF5722"),
            SubcategoryData("Google Play", R.drawable.ic_brand_google_play, "#FF5722"),
            SubcategoryData("Xbox", R.drawable.ic_brand_xbox, "#66483D"),
            SubcategoryData("PlayStation", R.drawable.ic_brand_playstation, "#66483D"),
            SubcategoryData("Disney Plus", R.drawable.ic_brand_disney_plus, "#66483D"),
            SubcategoryData("Zee5", R.drawable.ic_brand_zee5, "#66483D"),
            SubcategoryData("ChatGPT", R.drawable.ic_brand_chatgpt, "#66483D"),
            SubcategoryData("Claude", R.drawable.ic_brand_claude, "#66483D"),
            SubcategoryData("Grok", R.drawable.ic_brand_grok, "#66483D"),
        )
        subscriptionSubcategories.forEach { subcategory ->
            insertSubcategory(
                db, context, subscriptionCategoryId, subcategory.name,
                subcategory.iconResId,
                subcategory.color
            )
        }

        // emi subcategories
        val emiSubcategories = listOf(
            SubcategoryData("Electronics", R.drawable.type_tool_electronic_software, "#423D3A"),
            SubcategoryData("House", R.drawable.type_event_and_place_house, "#423D3A"),
            SubcategoryData("Vehicle", R.drawable.type_travel_transport_automobile, "#B75300"),
            SubcategoryData("Education", R.drawable.type_stationary_writing_hand_light, "#FF5722"),
        )
        emiSubcategories.forEach { subcategory ->
            insertSubcategory(
                db, context, emiCategoryId, subcategory.name,
                subcategory.iconResId,
                subcategory.color
            )
        }

        // creditBill subcategories
        val creditBillSubcategories = listOf(
            SubcategoryData("Credit Card", R.drawable.type_finance_credit_card, "#423D3A"),
            SubcategoryData("Simpl", R.drawable.ic_brand_simpl, "#423D3A"),
            SubcategoryData("Slice", R.drawable.ic_brand_slice, "#B75300"),
            SubcategoryData("lazypay", R.drawable.ic_brand_lazypay, "#FF5722"),
            SubcategoryData("Amazon Pay", R.drawable.ic_brand_amazon, "#993414"),
        )
        creditBillSubcategories.forEach { subcategory ->
            insertSubcategory(
                db, context, creditBillCategoryId, subcategory.name,
                subcategory.iconResId,
                subcategory.color
            )
        }

        // investment subcategories
        val investmentSubcategories = listOf(
            SubcategoryData("Mutual Funds", R.drawable.type_flower_and_tree_herb, "#423D3A"),
            SubcategoryData("Stocks", R.drawable.type_finance_chart_increasing, "#423D3A"),
            SubcategoryData("IPO", R.drawable.type_finance_bar_chart, "#B75300"),
            SubcategoryData("PPF", R.drawable.type_finance_dollar_banknote, "#FF5722"),
            SubcategoryData("Fixed Deposit", R.drawable.type_finance_deposit, "#993414"),
            SubcategoryData("Recurring Deposit", R.drawable.type_finance_tip, "#FF5722"),
            SubcategoryData("Assets", R.drawable.type_finance_classical_building, "#FF0041"),
            SubcategoryData("Crypto", R.drawable.type_finance_crypto, "#9C27B0"),
            SubcategoryData("Gold", R.drawable.type_finance_coin, "#FF9800"),
        )
        investmentSubcategories.forEach { subcategory ->
            insertSubcategory(
                db, context, investmentCategoryId, subcategory.name,
                subcategory.iconResId,
                subcategory.color
            )
        }

        // support subcategories
        val supportSubcategories = listOf(
            SubcategoryData("Parents", R.drawable.type_human_parents, "#423D3A"),
            SubcategoryData("Spouse", R.drawable.type_human_woman, "#423D3A"),
            SubcategoryData("Mom", R.drawable.type_human_old_woman, "#B75300"),
            SubcategoryData("Dad", R.drawable.type_human_older_person, "#FF5722"),
            SubcategoryData("Pocket Money", R.drawable.type_finance_money_bag, "#993414"),
        )
        supportSubcategories.forEach { subcategory ->
            insertSubcategory(
                db, context, supportCategoryId, subcategory.name,
                subcategory.iconResId,
                subcategory.color
            )
        }

        // insurance subcategories
        val insuranceSubcategories = listOf(
            SubcategoryData("Health", R.drawable.type_health_drop_of_blood, "#423D3A"),
            SubcategoryData("Vehicle", R.drawable.type_travel_transport_automobile, "#423D3A"),
            SubcategoryData("Life", R.drawable.type_health_mending_heart, "#B75300"),
            SubcategoryData("Electronics", R.drawable.type_tool_electronic_mobile_phone, "#FF5722"),
        )
        insuranceSubcategories.forEach { subcategory ->
            insertSubcategory(
                db, context, insuranceCategoryId, subcategory.name,
                subcategory.iconResId,
                subcategory.color
            )
        }

        // tax subcategories
        val taxSubcategories = listOf(
            SubcategoryData("Income Tax", R.drawable.type_finance_chart_increasing, "#423D3A"),
            SubcategoryData("GST", R.drawable.type_finance_tax_due, "#423D3A"),
            SubcategoryData("Property Tax", R.drawable.type_finance_classical_building, "#B75300"),
        )
        taxSubcategories.forEach { subcategory ->
            insertSubcategory(
                db, context, taxCategoryId, subcategory.name,
                subcategory.iconResId,
                subcategory.color
            )
        }

        // topUp subcategories
        val topUpSubcategories = listOf(
            SubcategoryData("UPI Lite", R.drawable.type_finance_bank, "#423D3A"),
            SubcategoryData("Paytm", R.drawable.ic_brand_paytm, "#423D3A"),
            SubcategoryData("Amazon", R.drawable.ic_brand_amazon, "#B75300"),
            SubcategoryData("PhonePe", R.drawable.ic_brand_phonepe, "#FF5722"),
            SubcategoryData("Google pay", R.drawable.ic_brand_google_pay, "#993414"),
        )
        topUpSubcategories.forEach { subcategory ->
            insertSubcategory(
                db, context, topUpCategoryId, subcategory.name,
                subcategory.iconResId,
                subcategory.color
            )
        }

        // children subcategories
        val childrenSubcategories = listOf(
            SubcategoryData("Nutrition", R.drawable.type_vegetable_pea_pod, "#423D3A"),
            SubcategoryData("Necessities", R.drawable.type_stationary_pencil, "#423D3A"),
            SubcategoryData("Toys", R.drawable.type_stationary_toys, "#B75300"),
            SubcategoryData("Medical", R.drawable.type_health_pill, "#FF5722"),
            SubcategoryData("Care", R.drawable.type_health_adhesive_bandage, "#993414"),
            SubcategoryData("Tuition Fee", R.drawable.type_finance_money_bag, "#FF5722"),
            SubcategoryData("Classes Fee", R.drawable.type_stationary_books, "#FF0041"),
            SubcategoryData("School Fee", R.drawable.type_stationary_open_book, "#9C27B0"),
            SubcategoryData("College Fee", R.drawable.type_finance_classical_building, "#FF9800"),
        )
        childrenSubcategories.forEach { subcategory ->
            insertSubcategory(
                db, context, childrenCategoryId, subcategory.name,
                subcategory.iconResId,
                subcategory.color
            )
        }

        // petCare subcategories
        val petCareSubcategories = listOf(
            SubcategoryData("Food", R.drawable.type_vegetable_beans, "#423D3A"),
            SubcategoryData("Toys", R.drawable.type_stationary_toys, "#423D3A"),
            SubcategoryData("Grooming", R.drawable.type_tool_electronic_scissors, "#B75300"),
            SubcategoryData("Vet", R.drawable.type_health_vet, "#FF5722"),
        )
        petCareSubcategories.forEach { subcategory ->
            insertSubcategory(
                db, context, petCareCategoryId, subcategory.name,
                subcategory.iconResId,
                subcategory.color
            )
        }

        // business subcategories
        val businessSubcategories = listOf(
            SubcategoryData("Salary", R.drawable.type_finance_money_with_wings, "#423D3A"),
            SubcategoryData("Inventory", R.drawable.type_travel_transport_inventory, "#423D3A"),
            SubcategoryData("Rent", R.drawable.type_event_and_place_house, "#B75300"),
            SubcategoryData("Logistics", R.drawable.type_travel_transport_delivery_truck, "#FF5722"),
            SubcategoryData("Software", R.drawable.type_tool_electronic_software, "#993414"),
            SubcategoryData("Marketing", R.drawable.type_finance_bar_chart, "#FF5722"),
            SubcategoryData("Tax", R.drawable.type_finance_tax_due, "#FF0041"),
            SubcategoryData("Insurance", R.drawable.type_finance_insurance, "#9C27B0"),
            SubcategoryData("Service", R.drawable.type_tool_electronic_light_bulb, "#FF9800"),
        )
        businessSubcategories.forEach { subcategory ->
            insertSubcategory(
                db, context, businessCategoryId, subcategory.name,
                subcategory.iconResId,
                subcategory.color
            )
        }

        // subscription subcategories
        val miscellaneousSubcategories = listOf(
            SubcategoryData("Tip", R.drawable.type_finance_tip, "#423D3A"),
            SubcategoryData("Verification", R.drawable.type_tool_electronic_magnifying_glass_tilted_left, "#423D3A"),
            SubcategoryData("Forex", R.drawable.type_finance_currency_exchange, "#B75300"),
            SubcategoryData("Deposit", R.drawable.type_finance_deposit, "#FF5722"),
            SubcategoryData("Gift Cards", R.drawable.type_stationary_gift_card, "#993414"),
        )
        miscellaneousSubcategories.forEach { subcategory ->
            insertSubcategory(
                db, context, miscellaneousCategoryId, subcategory.name,
                subcategory.iconResId,
                subcategory.color
            )
        }

        // Income subcategories
        val incomeSubcategories = listOf(
            SubcategoryData("Salary", R.drawable.type_finance_coin, "#8BC34A"),
            SubcategoryData("Freelance", R.drawable.type_stationary_clipboard, "#4CAF50"),
            SubcategoryData("Business", R.drawable.type_finance_classical_building, "#8BC34A"),
            SubcategoryData("Bonus", R.drawable.type_stationary_wrapped_gift, "#FFEB3B"),
            SubcategoryData("Gift", R.drawable.type_stationary_wrapped_gift, "#FF9800"),
            SubcategoryData("Interest", R.drawable.type_finance_chart_decreasing, "#8BC34A"),
            SubcategoryData("Refund", R.drawable.type_finance_currency_exchange, "#03A9F4"),
            SubcategoryData("Other", R.drawable.type_stationary_clipboard, "#9E9E9E"),
        )
        incomeSubcategories.forEach { subcategory ->
            insertSubcategory(
                db, context, incomeCategoryId, subcategory.name,
                subcategory.iconResId,
                subcategory.color
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
        context: Context,
        categoryId: Long,
        name: String,
        iconResId: Int,
        color: String
    ) {
        val iconName = IconResolutionUtils.resIdToName(context, iconResId)
        db.execSQL(
            """
            INSERT OR IGNORE INTO subcategories (
                category_id, name, icon_res_id, icon_name, color, is_system,
                default_name, default_color, default_icon_res_id, default_icon_name,
                created_at, updated_at
            )
            VALUES (?, ?, ?, ?, ?, 1, ?, ?, ?, ?, datetime('now'), datetime('now'))
        """.trimIndent(),
            arrayOf<Any>(categoryId, name, iconResId, iconName, color, name, color, iconResId, iconName)
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
    
    private data class SubcategoryData(
        val name: String,
        val iconResId: Int,
        val color: String
    )
}
