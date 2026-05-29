package com.ritesh.cashiro.data.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ritesh.cashiro.data.database.converter.Converters
import com.ritesh.cashiro.data.database.dao.AccountBalanceDao
import com.ritesh.cashiro.data.database.dao.BankNotificationDao
import com.ritesh.cashiro.data.database.dao.BudgetDao
import com.ritesh.cashiro.data.database.dao.CardDao
import com.cashiroai.shared.data.bootstrap.DefaultCategoryData
import com.ritesh.cashiro.data.database.dao.CategoryDao
import com.ritesh.cashiro.data.database.dao.ChatDao
import com.ritesh.cashiro.data.database.dao.ExchangeRateDao
import com.ritesh.cashiro.data.database.dao.MerchantMappingDao
import com.ritesh.cashiro.data.database.dao.RuleApplicationDao
import com.ritesh.cashiro.data.database.dao.RuleDao
import com.ritesh.cashiro.data.database.dao.SubcategoryDao
import com.ritesh.cashiro.data.database.dao.SubscriptionDao
import com.ritesh.cashiro.data.database.dao.BudgetDao
import com.ritesh.cashiro.data.database.dao.TransactionDao
import com.ritesh.cashiro.data.database.dao.UnrecognizedSmsDao
import com.ritesh.cashiro.data.database.dao.WebhookCursorDao
import com.ritesh.cashiro.data.database.dao.WebhookLogDao
import com.ritesh.cashiro.data.database.dao.WebhookProfileDao
import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import com.ritesh.cashiro.data.database.entity.BudgetCategoryLimitEntity
import com.ritesh.cashiro.data.database.entity.BudgetEntity
import com.ritesh.cashiro.data.database.entity.CardEntity
import com.ritesh.cashiro.data.database.entity.CategoryEntity
import com.ritesh.cashiro.data.database.entity.ChatMessage
import com.ritesh.cashiro.data.database.entity.ExchangeRateEntity
import com.ritesh.cashiro.data.database.entity.MerchantMappingEntity
import com.ritesh.cashiro.data.database.entity.RuleApplicationEntity
import com.ritesh.cashiro.data.database.entity.RuleEntity
import com.ritesh.cashiro.data.database.entity.SubcategoryEntity
import com.ritesh.cashiro.data.database.entity.SubscriptionEntity
import com.ritesh.cashiro.data.database.entity.TransactionEntity
import com.ritesh.cashiro.data.database.entity.UnrecognizedSmsEntity
import com.ritesh.cashiro.data.database.entity.WebhookCursorEntity
import com.ritesh.cashiro.data.database.entity.WebhookLogEntity
import com.ritesh.cashiro.data.database.entity.WebhookProfileEntity

/**
 * The Cashiro Room database.
 *
 * This database stores all financial transaction data locally on the device.
 *
 * @property version Current database version. Increment this when making schema changes.
 * @property entities List of all entities (tables) in the database.
 * @property exportSchema Set to true in production to export schema for version control.
 * @property autoMigrations List of automatic migrations between versions.
 */
@Database(
    entities =
        [
            TransactionEntity::class,
            SubscriptionEntity::class,
            ChatMessage::class,
            MerchantMappingEntity::class,
            CategoryEntity::class,
            AccountBalanceEntity::class,
            UnrecognizedSmsEntity::class,
            CardEntity::class,
            RuleEntity::class,
            RuleApplicationEntity::class,
            ExchangeRateEntity::class,
            SubcategoryEntity::class,
            BudgetEntity::class,
            BudgetCategoryLimitEntity::class,
            WebhookProfileEntity::class,
            WebhookLogEntity::class,
            WebhookCursorEntity::class
        ],
    version = 51,
    exportSchema = true,
    autoMigrations =
        [
            AutoMigration(from = 27, to = 28),
            AutoMigration(from = 28, to = 29),
            AutoMigration(from = 29, to = 30, spec = Migration29To30::class),
            AutoMigration(from = 30, to = 31),
            AutoMigration(from = 31, to = 32, spec = Migration31To32::class),
            AutoMigration(from = 32, to = 33),
            AutoMigration(from = 33, to = 34),
            AutoMigration(from = 34, to = 35, spec = Migration34To35::class),
            AutoMigration(from = 35, to = 36),
            AutoMigration(from = 36, to = 37),
            AutoMigration(from = 37, to = 38),
            AutoMigration(from = 38, to = 39),
            AutoMigration(from = 39, to = 40),
            AutoMigration(from = 40, to = 41, spec = Migration40To41::class),
            AutoMigration(from = 41, to = 42),
            AutoMigration(from = 42, to = 43),
            AutoMigration(from = 43, to = 44, spec = Migration43To44::class),
            AutoMigration(from = 44, to = 45, spec = Migration44To45::class),
            AutoMigration(from = 45, to = 46, spec = Migration45To46::class),
            AutoMigration(from = 46, to = 47, spec = Migration46To47::class),
            AutoMigration(from = 47, to = 48)
        ]
)
@TypeConverters(Converters::class)
abstract class CashiroDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun chatDao(): ChatDao
    abstract fun merchantMappingDao(): MerchantMappingDao
    abstract fun categoryDao(): CategoryDao
    abstract fun accountBalanceDao(): AccountBalanceDao
    abstract fun unrecognizedSmsDao(): UnrecognizedSmsDao
    abstract fun cardDao(): CardDao
    abstract fun ruleDao(): RuleDao
    abstract fun ruleApplicationDao(): RuleApplicationDao
    abstract fun exchangeRateDao(): ExchangeRateDao
    abstract fun subcategoryDao(): SubcategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun webhookProfileDao(): WebhookProfileDao
    abstract fun webhookLogDao(): WebhookLogDao
    abstract fun webhookCursorDao(): WebhookCursorDao

    companion object {
        const val DATABASE_NAME = "pennywise_database"

        @Volatile private var INSTANCE: CashiroDatabase? = null

        /**
         * Returns a singleton instance of the database. This is used by components that don't have
         * access to Hilt injection (like BroadcastReceivers).
         */
        fun getInstance(context: android.content.Context): CashiroDatabase {
            return INSTANCE
                ?: synchronized(this) {
                    val instance =
                        Room.databaseBuilder(
                            context.applicationContext,
                            CashiroDatabase::class.java,
                            DATABASE_NAME
                        )
                            .addMigrations(
                                MIGRATION_12_14,
                                MIGRATION_13_14,
                                MIGRATION_14_15,
                                MIGRATION_20_21,
                                MIGRATION_21_22,
                                MIGRATION_22_23,
                                MIGRATION_29_30,
                                MIGRATION_48_49,
                                MIGRATION_49_50,
                                MIGRATION_50_51
                            )
                            .build()
                    INSTANCE = instance
                    instance
                }
        }

        /**
         * Sets the singleton instance. Called by Hilt module to ensure the same instance is used
         * throughout the app.
         */
        fun setInstance(database: CashiroDatabase) {
            INSTANCE = database
        }

        /**
         * Manual migration from version 1 to 2. Example of how to write manual migrations when
         * auto-migration isn't sufficient.
         */
        val MIGRATION_1_2 =
                object : Migration(1, 2) {
                    override fun migrate(db: SupportSQLiteDatabase) {
                        // Example: Add a new column
                        // db.execSQL("ALTER TABLE transactions ADD COLUMN tags TEXT")
                    }
                }

        /**
         * Manual migration from version 13 to 14. Adds is_deleted column and unique constraint,
         * handling existing duplicates.
         */
        val MIGRATION_13_14 =
            object : Migration(13, 14) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    // Check if sms_sender column already exists in transactions table
                    val cursor = db.query("PRAGMA table_info(transactions)")
                    var hasSenderColumn = false
                    while (cursor.moveToNext()) {
                        val nameIndex = cursor.getColumnIndex("name")
                        if (nameIndex == -1) continue
                        val columnName = cursor.getString(nameIndex)
                        if (columnName == "sms_sender") {
                            hasSenderColumn = true
                            break
                        }
                    }
                    cursor.close()

                    // Add sms_sender column to transactions table only if it doesn't exist
                    if (!hasSenderColumn) {
                        db.execSQL("ALTER TABLE transactions ADD COLUMN sms_sender TEXT")
                    }

                    // Check if is_deleted column already exists in unrecognized_sms table
                    val cursor2 = db.query("PRAGMA table_info(unrecognized_sms)")
                    var hasIsDeletedColumn = false
                    while (cursor2.moveToNext()) {
                        val nameIndex2 = cursor2.getColumnIndex("name")
                        if (nameIndex2 == -1) continue
                        val columnName = cursor2.getString(nameIndex2)
                        if (columnName == "is_deleted") {
                            hasIsDeletedColumn = true
                            break
                        }
                    }
                    cursor2.close()

                    // Only proceed with unrecognized_sms migration if needed
                    if (!hasIsDeletedColumn) {
                        // First, add the is_deleted column with default value
                        db.execSQL(
                            "ALTER TABLE unrecognized_sms ADD COLUMN is_deleted INTEGER NOT NULL DEFAULT 0"
                        )

                        // Create a temporary table with the new schema (including unique
                        // constraint)
                        db.execSQL(
                            """
                        CREATE TABLE unrecognized_sms_new (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            sender TEXT NOT NULL,
                            sms_body TEXT NOT NULL,
                            received_at TEXT NOT NULL,
                            reported INTEGER NOT NULL,
                            is_deleted INTEGER NOT NULL DEFAULT 0,
                            created_at TEXT NOT NULL
                        )
                    """
                        )

                        // Copy data from old table, keeping only the most recent of duplicates
                        db.execSQL(
                            """
                        INSERT INTO unrecognized_sms_new (id, sender, sms_body, received_at, reported, is_deleted, created_at)
                        SELECT id, sender, sms_body, received_at, reported, is_deleted, created_at
                        FROM unrecognized_sms
                        WHERE id IN (
                            SELECT MAX(id)
                            FROM unrecognized_sms
                            GROUP BY sender, sms_body
                        )
                    """
                        )

                        // Drop the old table
                        db.execSQL("DROP TABLE unrecognized_sms")

                        // Rename the new table to the original name
                        db.execSQL(
                            "ALTER TABLE unrecognized_sms_new RENAME TO unrecognized_sms"
                        )

                        // Create the unique index
                        db.execSQL(
                            "CREATE UNIQUE INDEX index_unrecognized_sms_sender_sms_body ON unrecognized_sms (sender, sms_body)"
                        )
                    }
                }
            }

        /**
         * Manual migration from version 12 to 14. Handles direct upgrade from 12 to 14, combining
         * migrations 12->13 and 13->14.
         */
        val MIGRATION_12_14 =
            object : Migration(12, 14) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    // Same as MIGRATION_13_14 since we need to handle both cases
                    MIGRATION_13_14.migrate(db)
                }
            }

        /** Manual migration from version 14 to 15. Adds sms_body column to subscriptions table. */
        val MIGRATION_14_15 =
            object : Migration(14, 15) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    // Add sms_body column to subscriptions table
                    db.execSQL("ALTER TABLE subscriptions ADD COLUMN sms_body TEXT")
                }
            }

        /**
         * Manual migration from version 20 to 21. Makes next_payment_date nullable in subscriptions
         * table. This fixes the issue where v2.15.18 had non-nullable field but v2.15.19+ needs
         * nullable.
         */
        val MIGRATION_20_21 =
            object : Migration(20, 21) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    // SQLite doesn't support ALTER COLUMN, so we need to recreate the table
                    // Step 1: Create new subscriptions table with nullable next_payment_date
                    db.execSQL(
                        """
                    CREATE TABLE subscriptions_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        merchant_name TEXT NOT NULL,
                        amount TEXT NOT NULL,
                        next_payment_date TEXT,
                        state TEXT NOT NULL,
                        bank_name TEXT,
                        umn TEXT,
                        category TEXT,
                        sms_body TEXT,
                        created_at TEXT NOT NULL,
                        updated_at TEXT NOT NULL
                    )
                """
                    )

                    // Step 2: Copy data from old table to new table
                    db.execSQL(
                        """
                    INSERT INTO subscriptions_new (id, merchant_name, amount, next_payment_date, state, bank_name, umn, category, sms_body, created_at, updated_at)
                    SELECT id, merchant_name, amount, next_payment_date, state, bank_name, umn, category, sms_body, created_at, updated_at
                    FROM subscriptions
                """
                    )

                    // Step 3: Drop old table
                    db.execSQL("DROP TABLE subscriptions")

                    // Step 4: Rename new table to original name
                    db.execSQL("ALTER TABLE subscriptions_new RENAME TO subscriptions")
                }
            }

        /**
         * Manual migration from version 21 to 22. Adds transaction_rules and rule_applications
         * tables for the rule engine. Note: This migration is kept for users who might be on v21.
         */
        val MIGRATION_21_22 =
            object : Migration(21, 22) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    // Create transaction_rules table
                    db.execSQL(
                        """
                    CREATE TABLE IF NOT EXISTS transaction_rules (
                        id TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT,
                        priority INTEGER NOT NULL,
                        conditions TEXT NOT NULL,
                        actions TEXT NOT NULL,
                        is_active INTEGER NOT NULL,
                        is_system_template INTEGER NOT NULL,
                        created_at TEXT NOT NULL,
                        updated_at TEXT NOT NULL
                    )
                """
                    )

                    // Create indices for transaction_rules
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_transaction_rules_priority_is_active ON transaction_rules (priority, is_active)"
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_transaction_rules_name ON transaction_rules (name)"
                    )

                    // Create rule_applications table
                    db.execSQL(
                        """
                    CREATE TABLE rule_applications (
                        id TEXT PRIMARY KEY NOT NULL,
                        rule_id TEXT NOT NULL,
                        rule_name TEXT NOT NULL,
                        transaction_id TEXT NOT NULL,
                        fields_modified TEXT NOT NULL,
                        applied_at TEXT NOT NULL,
                        FOREIGN KEY(rule_id) REFERENCES transaction_rules(id) ON DELETE CASCADE,
                        FOREIGN KEY(transaction_id) REFERENCES transactions(id) ON DELETE CASCADE
                    )
                """
                    )

                    // Create indices for rule_applications
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_rule_applications_rule_id ON rule_applications (rule_id)"
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_rule_applications_transaction_id ON rule_applications (transaction_id)"
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_rule_applications_applied_at ON rule_applications (applied_at)"
                    )
                }
            }

        /**
         * Manual migration from version 22 to 23. Adds transaction_rules and rule_applications
         * tables for the rule engine. This is for users who were already on v22 before the rules
         * feature was added.
         */
        val MIGRATION_22_23 =
            object : Migration(22, 23) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    // Drop table if exists to ensure clean state
                    db.execSQL("DROP TABLE IF EXISTS transaction_rules")
                    db.execSQL("DROP TABLE IF EXISTS rule_applications")

                    // Create transaction_rules table with all required columns
                    db.execSQL(
                        """
                    CREATE TABLE transaction_rules (
                        id TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT,
                        priority INTEGER NOT NULL,
                        conditions TEXT NOT NULL,
                        actions TEXT NOT NULL,
                        is_active INTEGER NOT NULL,
                        is_system_template INTEGER NOT NULL,
                        created_at TEXT NOT NULL,
                        updated_at TEXT NOT NULL
                    )
                """
                    )

                    // Create indices for transaction_rules
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_transaction_rules_priority_is_active ON transaction_rules (priority, is_active)"
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_transaction_rules_name ON transaction_rules (name)"
                    )

                    // Create rule_applications table
                    db.execSQL(
                        """
                    CREATE TABLE rule_applications (
                        id TEXT PRIMARY KEY NOT NULL,
                        rule_id TEXT NOT NULL,
                        rule_name TEXT NOT NULL,
                        transaction_id TEXT NOT NULL,
                        fields_modified TEXT NOT NULL,
                        applied_at TEXT NOT NULL,
                        FOREIGN KEY(rule_id) REFERENCES transaction_rules(id) ON DELETE CASCADE,
                        FOREIGN KEY(transaction_id) REFERENCES transactions(id) ON DELETE CASCADE
                    )
                """
                    )

                    // Create indices for rule_applications
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_rule_applications_rule_id ON rule_applications (rule_id)"
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_rule_applications_transaction_id ON rule_applications (transaction_id)"
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_rule_applications_applied_at ON rule_applications (applied_at)"
                    )
                }
            }
    }

    /**
     * Example AutoMigrationSpec for renaming tables or columns. Uncomment and modify when needed.
     */
    // @RenameTable(fromTableName = "transactions", toTableName = "user_transactions")
    // @RenameColumn(
    //     tableName = "transactions",
    //     fromColumnName = "merchant_name",
    //     toColumnName = "vendor_name"
    // )
    // class Migration1To2 : AutoMigrationSpec {
    //     override fun onPostMigrate(db: SupportSQLiteDatabase) {
    //         // Perform additional operations after migration if needed
    //         // Example: Update default values, create indexes, etc.
    //     }
    // }
}

/**
 * Migration from version 4 to 5.
 * - Removes sessionId column from chat_messages table
 * - Adds isSystemPrompt column to chat_messages table
 */
@DeleteColumn.Entries(DeleteColumn(tableName = "chat_messages", columnName = "sessionId"))
class Migration4To5 : AutoMigrationSpec

/**
 * Migration from version 7 to 8.
 * - Adds categories table with default categories
 */
class Migration7To8 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        super.onPostMigrate(db)

        // Insert default categories
        val categories =
            listOf(
                Triple("Food & Dining", "#FC8019", false),
                Triple("Groceries", "#5AC85A", false),
                Triple("Transportation", "#000000", false),
                Triple("Shopping", "#FF9900", false),
                Triple("Bills & Utilities", "#4CAF50", false),
                Triple("Entertainment", "#E50914", false),
                Triple("Healthcare", "#10847E", false),
                Triple("Investments", "#00D09C", false),
                Triple("Banking", "#004C8F", false),
                Triple("Personal Care", "#6A4C93", false),
                Triple("Education", "#673AB7", false),
                Triple("Mobile", "#2A3890", false),
                Triple("Fitness", "#FF3278", false),
                Triple("Insurance", "#0066CC", false),
                Triple("Travel", "#00BCD4", false),
                Triple("Salary", "#4CAF50", true),
                Triple("Income", "#4CAF50", true),
                Triple("Others", "#757575", false)
            )

        categories.forEachIndexed { index, (name, color, isIncome) ->
            db.execSQL(
                    """
                INSERT INTO categories (name, color, is_system, is_income, display_order, created_at, updated_at)
                VALUES (?, ?, 1, ?, ?, datetime('now'), datetime('now'))
            """.trimIndent(),
                    arrayOf<Any>(name, color, if (isIncome) 1 else 0, index + 1)
            )
        }
    }
}

/**
 * Migration from version 10 to 11.
 * - Adds account_balances table for tracking account balance history
 * - Migrates existing balance data from transactions table
 */
class Migration10To11 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        super.onPostMigrate(db)

        // Migrate existing balance data from transactions table
        db.execSQL(
                """
            INSERT INTO account_balances (bank_name, account_last4, balance, timestamp, transaction_id, created_at)
            SELECT 
                bank_name,
                account_number,
                balance_after,
                date_time,
                id,
                created_at
            FROM transactions
            WHERE balance_after IS NOT NULL 
                AND bank_name IS NOT NULL 
                AND account_number IS NOT NULL
        """.trimIndent()
        )
    }
}

/**
 * Manual migration from version 29 to 30. Adds new fields to categories and subcategories tables
 * for enhanced functionality.
 */
/**
 * Manual migration 48 -> 49.
 *
 * Normalises the webhook_profiles table by extracting the previously-blob columns
 * `data_types` (CSV) and `headers_json` (JSON array) into proper child tables, and
 * drops the now-redundant per-profile `currency` column (use the app baseCurrency).
 * Also collapses any legacy webhook_logs.status values ("DELIVERED" -> "SUCCESS",
 * "ERROR"/"FAILED" -> "FAILURE") so the typed enum reads cleanly.
 */
val MIGRATION_48_49 =
    object : Migration(48, 49) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 1) Create new child tables.
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS webhook_profile_headers (
                    profile_id TEXT NOT NULL,
                    ordinal INTEGER NOT NULL,
                    key TEXT NOT NULL,
                    value TEXT NOT NULL,
                    PRIMARY KEY(profile_id, ordinal),
                    FOREIGN KEY(profile_id) REFERENCES webhook_profiles(id) ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_webhook_profile_headers_profile_id ON webhook_profile_headers(profile_id)")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS webhook_profile_data_types (
                    profile_id TEXT NOT NULL,
                    data_type TEXT NOT NULL,
                    PRIMARY KEY(profile_id, data_type),
                    FOREIGN KEY(profile_id) REFERENCES webhook_profiles(id) ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_webhook_profile_data_types_profile_id ON webhook_profile_data_types(profile_id)")

            // 2) Copy data from the soon-to-be-dropped columns into the child tables.
            val profileCursor = db.query("SELECT id, data_types, headers_json FROM webhook_profiles")
            while (profileCursor.moveToNext()) {
                val profileId = profileCursor.getString(0)
                val dataTypesCsv = profileCursor.getString(1) ?: ""
                val headersJson = profileCursor.getString(2) ?: "[]"

                dataTypesCsv.split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .distinct()
                    .forEach { dataType ->
                        db.execSQL(
                            "INSERT OR IGNORE INTO webhook_profile_data_types (profile_id, data_type) VALUES (?, ?)",
                            arrayOf<Any>(profileId, dataType)
                        )
                    }

                runCatching {
                    val parsed = kotlinx.serialization.json.Json.parseToJsonElement(headersJson)
                    val array = (parsed as? kotlinx.serialization.json.JsonArray) ?: emptyList()
                    array.forEachIndexed { index, element ->
                        val obj = element as? kotlinx.serialization.json.JsonObject ?: return@forEachIndexed
                        val key = (obj["key"] as? kotlinx.serialization.json.JsonPrimitive)?.content.orEmpty()
                        val value = (obj["value"] as? kotlinx.serialization.json.JsonPrimitive)?.content.orEmpty()
                        if (key.isNotEmpty()) {
                            db.execSQL(
                                "INSERT OR REPLACE INTO webhook_profile_headers (profile_id, ordinal, key, value) VALUES (?, ?, ?, ?)",
                                arrayOf<Any>(profileId, index, key, value)
                            )
                        }
                    }
                }
            }
            profileCursor.close()

            // 3) Recreate webhook_profiles without data_types / headers_json / currency.
            db.execSQL(
                """
                CREATE TABLE webhook_profiles_new (
                    id TEXT NOT NULL PRIMARY KEY,
                    name TEXT NOT NULL,
                    url TEXT NOT NULL,
                    enabled INTEGER NOT NULL DEFAULT 1,
                    range_preset TEXT NOT NULL,
                    custom_start TEXT,
                    custom_end TEXT,
                    last_error TEXT,
                    consecutive_failures INTEGER NOT NULL DEFAULT 0,
                    last_synced_at TEXT,
                    created_at TEXT NOT NULL,
                    updated_at TEXT NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO webhook_profiles_new (
                    id, name, url, enabled, range_preset, custom_start, custom_end,
                    last_error, consecutive_failures, last_synced_at, created_at, updated_at
                ) SELECT
                    id, name, url, enabled, range_preset, custom_start, custom_end,
                    last_error, consecutive_failures, last_synced_at, created_at, updated_at
                FROM webhook_profiles
                """.trimIndent()
            )
            db.execSQL("DROP TABLE webhook_profiles")
            db.execSQL("ALTER TABLE webhook_profiles_new RENAME TO webhook_profiles")

            // 4) Normalise legacy log status values to the new SUCCESS/FAILURE pair.
            db.execSQL("UPDATE webhook_logs SET status = 'SUCCESS' WHERE status IN ('SUCCESS', 'DELIVERED', 'success', 'delivered')")
            db.execSQL("UPDATE webhook_logs SET status = 'FAILURE' WHERE status NOT IN ('SUCCESS', 'FAILURE')")
        }
    }

/**
 * Manual migration 49 -> 50.
 *
 * Splits the operational/status fields out of webhook_profiles into a sibling
 * webhook_profile_status table so editor saves no longer touch sync state and vice-versa.
 */
val MIGRATION_49_50 =
    object : Migration(49, 50) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 1) Create the new status table.
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS webhook_profile_status (
                    profile_id TEXT NOT NULL PRIMARY KEY,
                    last_error TEXT,
                    consecutive_failures INTEGER NOT NULL DEFAULT 0,
                    last_synced_at TEXT,
                    updated_at TEXT NOT NULL,
                    FOREIGN KEY(profile_id) REFERENCES webhook_profiles(id) ON DELETE CASCADE
                )
                """.trimIndent()
            )

            // 2) Copy operational state from webhook_profiles into the new table.
            db.execSQL(
                """
                INSERT INTO webhook_profile_status (
                    profile_id, last_error, consecutive_failures, last_synced_at, updated_at
                ) SELECT
                    id, last_error, consecutive_failures, last_synced_at, updated_at
                FROM webhook_profiles
                """.trimIndent()
            )

            // 3) Recreate webhook_profiles without the operational columns.
            db.execSQL(
                """
                CREATE TABLE webhook_profiles_new (
                    id TEXT NOT NULL PRIMARY KEY,
                    name TEXT NOT NULL,
                    url TEXT NOT NULL,
                    enabled INTEGER NOT NULL DEFAULT 1,
                    range_preset TEXT NOT NULL,
                    custom_start TEXT,
                    custom_end TEXT,
                    created_at TEXT NOT NULL,
                    updated_at TEXT NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO webhook_profiles_new (
                    id, name, url, enabled, range_preset, custom_start, custom_end, created_at, updated_at
                ) SELECT
                    id, name, url, enabled, range_preset, custom_start, custom_end, created_at, updated_at
                FROM webhook_profiles
                """.trimIndent()
            )
            db.execSQL("DROP TABLE webhook_profiles")
            db.execSQL("ALTER TABLE webhook_profiles_new RENAME TO webhook_profiles")
        }
    }

/**
 * Manual migration 50 -> 51.
 *
 * Collapses the over-normalised webhook schema back to a single profile row that owns its
 * operational state and inlines the small data_types CSV / headers JSON blobs. Child tables
 * (webhook_profile_headers, webhook_profile_data_types, webhook_profile_status) are dropped.
 *
 * The pragmatic call: at this app's actual scale (single-digit profiles, single-digit child
 * rows per profile) the join-table cost outweighed the 1NF benefit.
 */
val MIGRATION_50_51 =
    object : Migration(50, 51) {
        override fun migrate(db: SupportSQLiteDatabase) {
            data class Aggregated(
                val dataTypesCsv: String,
                val headersJson: String,
                val lastError: String?,
                val consecutiveFailures: Int,
                val lastSyncedAt: String?
            )
            val perProfile = mutableMapOf<String, Aggregated>()
            val ids = mutableListOf<String>()
            db.query("SELECT id FROM webhook_profiles").use { c ->
                while (c.moveToNext()) ids += c.getString(0)
            }
            ids.forEach { id ->
                val dataTypes = mutableListOf<String>()
                db.query(
                    "SELECT data_type FROM webhook_profile_data_types WHERE profile_id = ?",
                    arrayOf<Any>(id)
                ).use { c -> while (c.moveToNext()) dataTypes += c.getString(0) }

                val headerPairs = mutableListOf<Pair<String, String>>()
                db.query(
                    "SELECT key, value FROM webhook_profile_headers WHERE profile_id = ? ORDER BY ordinal ASC",
                    arrayOf<Any>(id)
                ).use { c ->
                    while (c.moveToNext()) headerPairs += c.getString(0) to c.getString(1)
                }
                val headersJson = if (headerPairs.isEmpty()) "[]" else {
                    headerPairs.joinToString(prefix = "[", postfix = "]") { (k, v) ->
                        """{"key":${jsonString(k)},"value":${jsonString(v)}}"""
                    }
                }

                var lastError: String? = null
                var consecutiveFailures = 0
                var lastSyncedAt: String? = null
                db.query(
                    "SELECT last_error, consecutive_failures, last_synced_at FROM webhook_profile_status WHERE profile_id = ? LIMIT 1",
                    arrayOf<Any>(id)
                ).use { c ->
                    if (c.moveToNext()) {
                        lastError = if (c.isNull(0)) null else c.getString(0)
                        consecutiveFailures = c.getInt(1)
                        lastSyncedAt = if (c.isNull(2)) null else c.getString(2)
                    }
                }

                perProfile[id] = Aggregated(
                    dataTypesCsv = dataTypes.joinToString(","),
                    headersJson = headersJson,
                    lastError = lastError,
                    consecutiveFailures = consecutiveFailures,
                    lastSyncedAt = lastSyncedAt
                )
            }

            db.execSQL(
                """
                CREATE TABLE webhook_profiles_new (
                    id TEXT NOT NULL PRIMARY KEY,
                    name TEXT NOT NULL,
                    url TEXT NOT NULL,
                    enabled INTEGER NOT NULL DEFAULT 1,
                    range_preset TEXT NOT NULL,
                    custom_start TEXT,
                    custom_end TEXT,
                    data_types TEXT NOT NULL DEFAULT '',
                    headers_json TEXT NOT NULL DEFAULT '[]',
                    last_error TEXT,
                    consecutive_failures INTEGER NOT NULL DEFAULT 0,
                    last_synced_at TEXT,
                    created_at TEXT NOT NULL,
                    updated_at TEXT NOT NULL
                )
                """.trimIndent()
            )
            db.query(
                "SELECT id, name, url, enabled, range_preset, custom_start, custom_end, created_at, updated_at FROM webhook_profiles"
            ).use { c ->
                while (c.moveToNext()) {
                    val id = c.getString(0)
                    val agg = perProfile[id] ?: Aggregated("", "[]", null, 0, null)
                    db.execSQL(
                        """
                        INSERT INTO webhook_profiles_new (
                            id, name, url, enabled, range_preset, custom_start, custom_end,
                            data_types, headers_json, last_error, consecutive_failures, last_synced_at,
                            created_at, updated_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """.trimIndent(),
                        arrayOf<Any?>(
                            id, c.getString(1), c.getString(2), c.getInt(3), c.getString(4),
                            if (c.isNull(5)) null else c.getString(5),
                            if (c.isNull(6)) null else c.getString(6),
                            agg.dataTypesCsv, agg.headersJson,
                            agg.lastError, agg.consecutiveFailures, agg.lastSyncedAt,
                            c.getString(7), c.getString(8)
                        )
                    )
                }
            }

            db.execSQL("DROP TABLE IF EXISTS webhook_profile_headers")
            db.execSQL("DROP TABLE IF EXISTS webhook_profile_data_types")
            db.execSQL("DROP TABLE IF EXISTS webhook_profile_status")
            db.execSQL("DROP TABLE webhook_profiles")
            db.execSQL("ALTER TABLE webhook_profiles_new RENAME TO webhook_profiles")
        }

        private fun jsonString(value: String): String {
            val sb = StringBuilder("\"")
            value.forEach { ch ->
                when {
                    ch == '\\' -> sb.append("\\\\")
                    ch == '"' -> sb.append("\\\"")
                    ch == '\n' -> sb.append("\\n")
                    ch == '\r' -> sb.append("\\r")
                    ch == '\t' -> sb.append("\\t")
                    ch.code < 0x20 -> sb.append("\\u%04x".format(ch.code))
                    else -> sb.append(ch)
                }
            }
            sb.append("\"")
            return sb.toString()
        }
    }

val MIGRATION_29_30 =
    object : Migration(29, 30) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Add new columns to categories table
            db.execSQL("ALTER TABLE categories ADD COLUMN description TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE categories ADD COLUMN default_name TEXT")
            db.execSQL("ALTER TABLE categories ADD COLUMN default_color TEXT")
            db.execSQL("ALTER TABLE categories ADD COLUMN default_icon_res_id INTEGER")
            db.execSQL("ALTER TABLE categories ADD COLUMN default_description TEXT")

            // Add new columns to subcategories table
            db.execSQL(
                "ALTER TABLE subcategories ADD COLUMN icon_res_id INTEGER NOT NULL DEFAULT 0"
            )
            db.execSQL(
                "ALTER TABLE subcategories ADD COLUMN color TEXT NOT NULL DEFAULT '#757575'"
            )
            db.execSQL(
                "ALTER TABLE subcategories ADD COLUMN is_system INTEGER NOT NULL DEFAULT 0"
            )
            db.execSQL("ALTER TABLE subcategories ADD COLUMN default_name TEXT")
            db.execSQL("ALTER TABLE subcategories ADD COLUMN default_icon_res_id INTEGER")
            db.execSQL("ALTER TABLE subcategories ADD COLUMN default_color TEXT")
        }
    }

/** Migration from version 29 to 30. AutoMigrationSpec to handle any post-migration data updates. */
class Migration29To30 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        super.onPostMigrate(db)
        // Post-migration updates if needed
        // Default values are already set in the entity definitions
    }
}

/** Migration from version 31 to 32. Migrates 'Others' category to 'Miscellaneous'. */
class Migration31To32 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        super.onPostMigrate(db)

        // Update transactions categorized as 'Others' to 'Miscellaneous'
        db.execSQL("UPDATE transactions SET category = 'Miscellaneous' WHERE category = 'Others'")

        // Update subscriptions categorized as 'Others' to 'Miscellaneous'
        db.execSQL("UPDATE subscriptions SET category = 'Miscellaneous' WHERE category = 'Others'")

        // Update merchant mappings from 'Others' to 'Miscellaneous'
        db.execSQL("UPDATE merchant_mappings SET category = 'Miscellaneous' WHERE category = 'Others'")

        // Delete the 'Others' category from categories table
        db.execSQL("DELETE FROM categories WHERE name = 'Others'")
    }
}
/** Migration from version 34 to 35. Adds is_wallet column to account_balances table. */
class Migration34To35 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        super.onPostMigrate(db)
        // No post-migration needed as default value is handled
    }
}
/** Migration from version 40 to 41. Unifies old category names with new ones. */
class Migration40To41 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        super.onPostMigrate(db)

        val oldToNewMap = mapOf(
            "Food & Dining" to "Food & Drinks",
            "Transportation" to "Transport",
            "Bills & Utilities" to "Bill",
            "Healthcare" to "Medical",
            "Personal Care" to "Personal",
            "Investments" to "Investment",
            "Mobile" to "Bill",
            "Banking" to "Miscellaneous",
            "Education" to "Miscellaneous"
        )

        oldToNewMap.forEach { (old, new) ->
            // Update transactions
            db.execSQL("UPDATE transactions SET category = ? WHERE category = ?", arrayOf(new, old))

            // Update subscriptions
            db.execSQL("UPDATE subscriptions SET category = ? WHERE category = ?", arrayOf(new, old))

            // Update merchant mappings
            db.execSQL("UPDATE merchant_mappings SET category = ? WHERE category = ?", arrayOf(new, old))

            // Update rules (actions) - this is stored as JSON in the database,
            // but we can do a simple string replace for the value if it's stored as plain text in the JSON
            // TransactionRule actions are serialized. We might need a more careful approach here
            // if we want to be 100% sure, but simple string replacement in the 'actions' column
            // usually works for SQLite JSON if the structure is simple.
            // However, to be safe, let's just do it for categories.
            db.execSQL("UPDATE transaction_rules SET actions = REPLACE(actions, ?, ?) WHERE actions LIKE ?",
                arrayOf("\"value\":\"$old\"", "\"value\":\"$new\"", "%\"value\":\"$old\"%"))
        }

        // Delete old system categories from categories table
        oldToNewMap.keys.forEach { oldCategory ->
            db.execSQL("DELETE FROM categories WHERE name = ?", arrayOf(oldCategory))
        }
    }
}

/** Migration from version 43 to 44. Adds 'Income' default category and its subcategories. */
class Migration43To44 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        super.onPostMigrate(db)
        
        // Insert Income category
        db.execSQL(
            """
            INSERT OR IGNORE INTO categories (
                name, color, icon_res_id, icon_name, description, is_system, is_income, display_order,
                default_name, default_color, default_icon_res_id, default_icon_name, default_description,
                created_at, updated_at
            )
            VALUES (?, ?, ?, ?, ?, 1, 1, 0, ?, ?, ?, ?, ?, datetime('now'), datetime('now'))
            """.trimIndent(),
            arrayOf<Any>(
                "Income", "#4CAF50", com.ritesh.cashiro.R.drawable.type_finance_money_bag, "type_finance_money_bag", "Generic income",
                "Income", "#4CAF50", com.ritesh.cashiro.R.drawable.type_finance_money_bag, "type_finance_money_bag", "Generic income"
            )
        )
        
        // Find the inserted or existing Income category ID
        val cursor = db.query("SELECT id FROM categories WHERE name = 'Income'")
        var incomeCategoryId: Long = -1
        if (cursor.moveToFirst()) {
            incomeCategoryId = cursor.getLong(0)
        }
        cursor.close()
        
        if (incomeCategoryId != -1L) {
            val incomeSubcategories = listOf(
                Triple("Freelance", com.ritesh.cashiro.R.drawable.type_stationary_clipboard, "#4CAF50"),
                Triple("Business", com.ritesh.cashiro.R.drawable.type_finance_classical_building, "#8BC34A"),
                Triple("Bonus", com.ritesh.cashiro.R.drawable.type_stationary_wrapped_gift, "#FFEB3B"),
                Triple("Gift", com.ritesh.cashiro.R.drawable.type_stationary_wrapped_gift, "#FF9800"),
                Triple("Interest", com.ritesh.cashiro.R.drawable.type_finance_chart_decreasing, "#8BC34A"),
                Triple("Refund", com.ritesh.cashiro.R.drawable.type_finance_currency_exchange, "#03A9F4"),
                Triple("Other", com.ritesh.cashiro.R.drawable.type_stationary_clipboard, "#9E9E9E"),
            )
            
            incomeSubcategories.forEach { (name, iconResId, color) ->
                val iconName = when(name) {
                    "Freelance" -> "type_stationary_clipboard"
                    "Business" -> "type_finance_classical_building"
                    "Bonus" -> "type_stationary_wrapped_gift"
                    "Gift" -> "type_stationary_wrapped_gift"
                    "Interest" -> "type_finance_chart_decreasing"
                    "Refund" -> "type_finance_currency_exchange"
                    "Other" -> "type_stationary_clipboard"
                    else -> ""
                }
                
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO subcategories (
                        category_id, name, icon_res_id, icon_name, color, is_system,
                        default_name, default_color, default_icon_res_id, default_icon_name
                    )
                    VALUES (?, ?, ?, ?, ?, 1, ?, ?, ?, ?)
                    """.trimIndent(),
                    arrayOf<Any>(
                        incomeCategoryId, name, iconResId, iconName, color,
                        name, color, iconResId, iconName
                    )
                )
            }
        }
    }
}

/** 
 * Migration from version 44 to 45. 
 * Migrates 'Salary' category to 'Income' category with 'Salary' subcategory.
 */
class Migration44To45 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        super.onPostMigrate(db)
        
        // Find Income Category ID
        val cursorIncome = db.query("SELECT id FROM categories WHERE name = 'Income'")
        var incomeCategoryId: Long = -1
        if (cursorIncome.moveToFirst()) {
            incomeCategoryId = cursorIncome.getLong(0)
        }
        cursorIncome.close()

        if (incomeCategoryId != -1L) {
            // Ensure Salary subcategory exists under Income
            db.execSQL(
                """
                INSERT OR IGNORE INTO subcategories (
                    category_id, name, icon_res_id, icon_name, color, is_system,
                    default_name, default_color, default_icon_res_id, default_icon_name
                )
                VALUES (?, 'Salary', ?, 'type_finance_coin', '#8BC34A', 1, 'Salary', '#8BC34A', ?, 'type_finance_coin')
                """.trimIndent(),
                arrayOf<Any>(
                    incomeCategoryId, 
                    com.ritesh.cashiro.R.drawable.type_finance_coin,
                    com.ritesh.cashiro.R.drawable.type_finance_coin
                )
            )

            // Update transactions categorized as 'Salary' to 'Income' and subcategory 'Salary'
            val cursorHasSubcategory = db.query("PRAGMA table_info(transactions)")
            var hasSubcat = false
            while(cursorHasSubcategory.moveToNext()){
                if(cursorHasSubcategory.getString(1) == "subcategory") {
                    hasSubcat = true
                    break
                }
            }
            cursorHasSubcategory.close()
            
            if(hasSubcat) {
                db.execSQL("UPDATE transactions SET category = 'Income', subcategory = 'Salary' WHERE category = 'Salary'")
            } else {
                db.execSQL("UPDATE transactions SET category = 'Income' WHERE category = 'Salary'")
            }

            // Update subscriptions categorized as 'Salary' to 'Income'
            db.execSQL("UPDATE subscriptions SET category = 'Income' WHERE category = 'Salary'")

            // Update merchant mappings from 'Salary' to 'Income'
            db.execSQL("UPDATE merchant_mappings SET category = 'Income' WHERE category = 'Salary'")

            // Update rules
            db.execSQL("UPDATE transaction_rules SET actions = REPLACE(actions, '\"value\":\"Salary\"', '\"value\":\"Income\"') WHERE actions LIKE '%\"value\":\"Salary\"%'")
        }

        // Delete the old 'Salary' category from categories table
        db.execSQL("DELETE FROM categories WHERE name = 'Salary'")
    }
}

/** 
 * Migration from version 45 to 46. 
 * Populates icon_name for existing categories, subcategories and account balances.
 * This preserves stability across app updates.
 */
class Migration45To46 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        super.onPostMigrate(db)
        
        // Categories (System ones)
        val categoryMappings = mapOf(
            "Food & Drinks" to "type_food_stuffed_flatbread",
            "Transport" to "type_travel_transport_airplane",
            "Shopping" to "type_shopping_shopping_bags",
            "Groceries" to "type_groceries_bread",
            "Home" to "type_event_and_place_house",
            "Entertainment" to "type_snack_popcorn",
            "Events" to "type_event_and_place_party_popper",
            "Travel" to "type_travel_transport_luggage",
            "Medical" to "type_health_pill",
            "Personal" to "type_tool_electronic_scissors",
            "Fitness" to "type_sports_baseball",
            "Services" to "type_tool_electronic_high_voltage",
            "Bill" to "type_travel_transport_admission_tickets",
            "Subscription" to "type_tool_electronic_clapper_board",
            "EMI" to "type_travel_transport_automobile",
            "Credit Bill" to "type_stationary_card_file_box",
            "Investment" to "type_flower_and_tree_herb",
            "Support" to "type_health_stethoscope",
            "Insurance" to "type_health_mending_heart",
            "Tax" to "type_finance_chart_decreasing",
            "Top-up" to "type_finance_money_bag",
            "Children" to "type_event_and_place_houses",
            "Pet Care" to "type_animal_dog_face",
            "Business" to "type_finance_classical_building",
            "Miscellaneous" to "type_stationary_clipboard",
            "Self Transfer" to "type_finance_bank",
            "Savings" to "type_sports_bullseye",
            "Gift" to "type_stationary_wrapped_gift",
            "Lent" to "type_finance_money_with_wings",
            "Donation" to "type_health_drop_of_blood",
            "Hidden Charges" to "type_animal_goblin",
            "Cash Withdrawal" to "type_finance_dollar_banknote",
            "Income" to "type_finance_money_bag"
        )
        
        categoryMappings.forEach { (name, iconName) ->
            db.execSQL("UPDATE categories SET icon_name = ?, default_icon_name = ? WHERE name = ?", arrayOf(iconName, iconName, name))
        }

        // Subcategories (System ones)
        // partial list of the most common ones
        val subcategoryMappings = mapOf(
            "Eating out" to "type_food_dining",
            "Take Away" to "type_food_takeout",
            "Tea & Coffee" to "type_beverages_tea",
            "Fast Food" to "type_food_hamburger",
            "Snacks" to "type_snack_cookie",
            "Swiggy" to "ic_brand_swiggy",
            "Zomato" to "ic_brand_zomato",
            "Sweets" to "type_sweet_cupcake",
            "Uber" to "ic_brand_uber",
            "Rapido" to "ic_brand_rapido",
            "Auto" to "type_travel_transport_auto_rickshaw",
            "Cab" to "type_travel_transport_taxi",
            "Train" to "type_travel_transport_high_speed_train",
            "Metro" to "type_travel_transport_metro",
            "Bus" to "type_travel_transport_bus",
            "Bike" to "type_travel_transport_motorcycle",
            "Fuel" to "type_travel_transport_fuel_pump",
            "Clothes" to "type_shopping_necktie",
            "Footwear" to "type_shopping_mans_shoe",
            "Electronics" to "type_tool_electronic_desktop_computer",
            "Vegetables" to "type_vegetable_broccoli",
            "Fruits" to "type_fruit_mango",
            "Dairy" to "type_groceries_glass_of_milk",
            "Salary" to "type_finance_coin",
            "Freelance" to "type_stationary_clipboard",
            "Business" to "type_finance_classical_building",
            "Bonus" to "type_stationary_wrapped_gift",
            "Gift" to "type_stationary_wrapped_gift",
            "Interest" to "type_finance_chart_decreasing",
            "Refund" to "type_finance_currency_exchange"
        )
        
        subcategoryMappings.forEach { (name, iconName) ->
            db.execSQL("UPDATE subcategories SET icon_name = ?, default_icon_name = ? WHERE name = ? AND is_system = 1", arrayOf(iconName, iconName, name))
        }
    }
}

/** 
 * Migration from version 46 to 47. 
 * Populates icon_name for all the remaining default subcategories that were missed in Migration45To46.
 */
class Migration46To47 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        super.onPostMigrate(db)
        
        val subcategoryMappings = mapOf(
            "Eating out" to "type_food_dining",
            "Take Away" to "type_food_takeout",
            "Tea & Coffee" to "type_beverages_tea",
            "Fast Food" to "type_food_hamburger",
            "Snacks" to "type_snack_cookie",
            "Swiggy" to "ic_brand_swiggy",
            "Zomato" to "ic_brand_zomato",
            "Sweets" to "type_sweet_cupcake",
            "Liquor" to "type_beverages_beer",
            "Beverages" to "type_beverages_bubble_tea",
            "Date" to "type_food_sushi",
            "Pizza" to "type_food_pizza",
            "Tiffin" to "type_food_bento_box",
            "Uber" to "ic_brand_uber",
            "Rapido" to "ic_brand_rapido",
            "Auto" to "type_travel_transport_auto_rickshaw",
            "Cab" to "type_travel_transport_taxi",
            "Train" to "type_travel_transport_high_speed_train",
            "Metro" to "type_travel_transport_metro",
            "Bus" to "type_travel_transport_bus",
            "Bike" to "type_travel_transport_motorcycle",
            "Fuel" to "type_travel_transport_fuel_pump",
            "Ev Charge" to "type_tool_electronic_high_voltage",
            "Flights" to "type_travel_transport_airplane",
            "Parking" to "type_travel_transport_ticket",
            "FASTag" to "type_travel_transport_ticket",
            "Tolls" to "type_travel_transport_ticket",
            "Lounge" to "type_travel_transport_luggage",
            "Fine" to "type_travel_transport_ticket",
            "Clothes" to "type_shopping_necktie",
            "Footwear" to "type_shopping_mans_shoe",
            "Electronics" to "type_tool_electronic_mobile_phone",
            "Festival" to "type_event_and_place_firecracker",
            "Video games" to "type_tool_electronic_video_game",
            "Books" to "type_stationary_blue_book",
            "Plants" to "type_flower_and_tree_potted_plant",
            "Jewellery" to "type_shopping_gem_stone",
            "Furniture" to "type_event_and_place_couch_and_lamp",
            "Appliances" to "type_tool_electronic_television",
            "Utensils" to "type_tool_electronic_hammer_and_wrench",
            "Vehicle" to "type_travel_transport_automobile",
            "Cosmetics" to "type_shopping_nail_polish",
            "Toys" to "type_stationary_toys",
            "Stationery" to "type_stationary_artist_palette",
            "Glasses" to "type_shopping_glasses",
            "Devotional" to "type_event_and_place_diya_lamp",
            "Staples" to "type_vegetable_beans",
            "Vegetables" to "type_vegetable_broccoli",
            "Fruits" to "type_fruit_mango",
            "Meat" to "type_groceries_cut_of_meat",
            "Eggs" to "type_groceries_egg",
            "Bakery" to "type_groceries_baguette_bread",
            "Dairy" to "type_groceries_glass_of_milk",
            "Zepto" to "ic_brand_zepto",
            "Essentials" to "type_groceries_basket",
            "Toiletries" to "type_groceries_soap",
            "Decor" to "type_flower_and_tree_hibiscus",
            "Cleaning" to "type_flower_and_tree_leaf_fluttering_in_wind",
            "Upkeep" to "type_groceries_sponge",
            "Painting" to "type_stationary_artist_palette",
            "Renovation" to "type_tool_electronic_hammer_and_wrench",
            "Pest-control" to "type_animal_lady_beetle",
            "Construction" to "type_tool_electronic_hammer",
            "Movies" to "type_snack_french_fries",
            "Shows" to "type_tool_electronic_clapper_board",
            "Bowling" to "type_sports_bowling",
            "Tickets" to "type_travel_transport_admission_tickets",
            "Party" to "type_event_and_place_party_popper",
            "Birthday" to "type_sweet_birthday_cake",
            "Spiritual" to "type_event_and_place_diya_lamp",
            "Wedding" to "type_event_and_place_wedding",
            "Activities" to "type_sports_trophy",
            "Camping" to "type_event_and_place_camping",
            "Hotel" to "type_event_and_place_hotel",
            "Commute" to "type_event_and_place_couch_and_lamp",
            "Visa fees" to "type_travel_transport_ticket",
            "Hostel" to "type_finance_classical_building",
            "Airbnb" to "ic_brand_airbnb",
            "Oyo" to "ic_brand_oyo",
            "Medicines" to "type_health_pill",
            "Hospital" to "type_health_hospital",
            "Clinic" to "type_health_stethoscope",
            "Dentist" to "type_health_tooth",
            "Lab test" to "type_shopping_lab_coat",
            "Hygiene" to "type_health_adhesive_bandage",
            "Self-care" to "type_groceries_lotion_bottle",
            "Grooming" to "type_tool_electronic_scissors",
            "Hobbies" to "type_sports_basketball",
            "Vices" to "type_event_and_place_firecracker",
            "Therapy" to "type_health_mending_heart",
            "Gym" to "type_sports_flexed_biceps_light",
            "Badminton" to "type_sports_badminton",
            "Football" to "type_sports_soccer_ball",
            "Cricket" to "type_sports_cricket_game",
            "Classes" to "type_stationary_books",
            "Equipment" to "type_tool_electronic_screwdriver",
            "Nutrition" to "type_vegetable_pea_pod",
            "Laundry" to "type_shopping_necktie",
            "Tailor" to "type_shopping_scarf",
            "Courier" to "type_travel_transport_package",
            "Carpenter" to "type_tool_electronic_carpentry_saw",
            "Plumber" to "type_tool_electronic_toolbox",
            "Mechanic" to "type_tool_electronic_hammer_and_wrench",
            "Photographer" to "type_tool_electronic_camera_with_flash",
            "Driver" to "type_travel_transport_oncoming_taxi",
            "Vehicle Wash" to "type_travel_transport_automobile",
            "Electrician" to "type_tool_electronic_high_voltage",
            "Xerox" to "type_stationary_card_index",
            "Legal" to "type_stationary_reminder_ribbon",
            "Advisor" to "type_stationary_bookmark",
            "Repair" to "type_tool_electronic_hammer_and_pick",
            "Logistics" to "type_travel_transport_delivery_truck",
            "Phone" to "type_tool_electronic_mobile_phone",
            "Rent" to "type_event_and_place_house",
            "Water" to "type_beverages_sake",
            "Electricity" to "type_tool_electronic_high_voltage",
            "Gas" to "type_travel_transport_fuel_pump",
            "Internet" to "type_travel_transport_globe_showing_asia_australia",
            "House Help" to "type_flower_and_tree_potted_plant",
            "Education" to "type_stationary_writing_hand_light",
            "DTH" to "type_tool_electronic_video_camera",
            "Cook" to "type_food_curry_rice",
            "Maintenance" to "type_tool_electronic_hammer_and_wrench",
            "Software" to "type_tool_electronic_software",
            "News" to "type_stationary_newspaper",
            "Netflix" to "ic_brand_netflix",
            "Prime" to "ic_brand_amazon_prime",
            "Youtube" to "ic_brand_youtube",
            "Youtube Music" to "ic_brand_youtube_music",
            "Spotify" to "ic_brand_spotify",
            "Google" to "ic_brand_google",
            "Learning" to "type_stationary_writing_hand_light",
            "Apple Tv" to "ic_brand_apple_tv",
            "Apple Music" to "ic_brand_apple_music",
            "Bumble" to "ic_brand_bumble",
            "JioCinema" to "ic_brand_jiocinema",
            "Google Play" to "ic_brand_google_play",
            "Xbox" to "ic_brand_xbox",
            "PlayStation" to "ic_brand_playstation",
            "Disney Plus" to "ic_brand_disney_plus",
            "Zee5" to "ic_brand_zee5",
            "ChatGPT" to "ic_brand_chatgpt",
            "Claude" to "ic_brand_claude",
            "Grok" to "ic_brand_grok",
            "House" to "type_event_and_place_house",
            "Credit Card" to "type_finance_credit_card",
            "Simpl" to "ic_brand_simpl",
            "Slice" to "ic_brand_slice",
            "lazypay" to "ic_brand_lazypay",
            "Amazon Pay" to "ic_brand_amazon",
            "Mutual Funds" to "type_flower_and_tree_herb",
            "Stocks" to "type_finance_chart_increasing",
            "IPO" to "type_finance_bar_chart",
            "PPF" to "type_finance_dollar_banknote",
            "Fixed Deposit" to "type_finance_deposit",
            "Recurring Deposit" to "type_finance_tip",
            "Assets" to "type_finance_classical_building",
            "Crypto" to "type_finance_crypto",
            "Gold" to "type_finance_coin",
            "Parents" to "type_human_parents",
            "Spouse" to "type_human_woman",
            "Mom" to "type_human_old_woman",
            "Dad" to "type_human_older_person",
            "Pocket Money" to "type_finance_money_bag",
            "Health" to "type_health_drop_of_blood",
            "Life" to "type_health_mending_heart",
            "Income Tax" to "type_finance_chart_increasing",
            "GST" to "type_finance_tax_due",
            "Property Tax" to "type_finance_classical_building",
            "UPI Lite" to "type_finance_bank",
            "Paytm" to "ic_brand_paytm",
            "Amazon" to "ic_brand_amazon",
            "PhonePe" to "ic_brand_phonepe",
            "Google pay" to "ic_brand_google_pay",
            "Necessities" to "type_stationary_pencil",
            "Medical" to "type_health_pill",
            "Care" to "type_health_adhesive_bandage",
            "Tuition Fee" to "type_finance_money_bag",
            "Classes Fee" to "type_stationary_books",
            "School Fee" to "type_stationary_open_book",
            "College Fee" to "type_finance_classical_building",
            "Food" to "type_vegetable_beans",
            "Vet" to "type_health_vet",
            "Salary" to "type_finance_coin",
            "Inventory" to "type_travel_transport_inventory",
            "Marketing" to "type_finance_bar_chart",
            "Tax" to "type_finance_tax_due",
            "Insurance" to "type_finance_insurance",
            "Service" to "type_tool_electronic_light_bulb",
            "Tip" to "type_finance_tip",
            "Verification" to "type_tool_electronic_magnifying_glass_tilted_left",
            "Forex" to "type_finance_currency_exchange",
            "Deposit" to "type_finance_deposit",
            "Gift Cards" to "type_stationary_gift_card",
            "Freelance" to "type_stationary_clipboard",
            "Business" to "type_finance_classical_building",
            "Bonus" to "type_stationary_wrapped_gift",
            "Gift" to "type_stationary_wrapped_gift",
            "Interest" to "type_finance_chart_decreasing",
            "Refund" to "type_finance_currency_exchange",
            "Other" to "type_stationary_clipboard"
        )
        
        subcategoryMappings.forEach { (name, iconName) ->
            db.execSQL("UPDATE subcategories SET icon_name = ?, default_icon_name = ? WHERE name = ? AND is_system = 1", arrayOf(iconName, iconName, name))
        }
    }
}
