package com.ritesh.cashiro.data.backup

import com.google.gson.annotations.SerializedName
import com.ritesh.cashiro.data.database.entity.*
import com.ritesh.cashiro.data.webhook.WebhookHeader
import java.time.LocalDateTime

/**
 * Root container for Cashiro backup data
 */
data class CashiroBackup(
    @SerializedName("_format")
    val format: String = "Cashiro Backup v1.0",
    
    @SerializedName("_warning")
    val warning: String = "Contains sensitive financial data. Keep this file secure.",
    
    @SerializedName("_created")
    val created: String = LocalDateTime.now().toString(),
    
    @SerializedName("metadata")
    val metadata: BackupMetadata,
    
    @SerializedName("database")
    val database: DatabaseSnapshot,
    
    @SerializedName("preferences")
    val preferences: PreferencesSnapshot
)

/**
 * Metadata about the backup
 */
data class BackupMetadata(
    @SerializedName("export_id")
    val exportId: String,
    
    @SerializedName("app_version")
    val appVersion: String,
    
    @SerializedName("database_version")
    val databaseVersion: Int,
    
    @SerializedName("device")
    val device: String,
    
    @SerializedName("android_version")
    val androidVersion: Int,
    
    @SerializedName("statistics")
    val statistics: BackupStatistics
)

/**
 * Statistics about the backup content
 */
data class BackupStatistics(
    @SerializedName("total_transactions")
    val totalTransactions: Int,
    
    @SerializedName("total_categories")
    val totalCategories: Int,
    
    @SerializedName("total_cards")
    val totalCards: Int,
    
    @SerializedName("total_subscriptions")
    val totalSubscriptions: Int,
    
    @SerializedName("total_subcategories")
    val totalSubcategories: Int = 0,

    @SerializedName("total_rules")
    val totalRules: Int = 0,

    @SerializedName("date_range")
    val dateRange: DateRange?
)

/**
 * Date range of transactions
 */
data class DateRange(
    @SerializedName("earliest")
    val earliest: String?,
    
    @SerializedName("latest")
    val latest: String?
)

/**
 * Complete database snapshot
 */
data class DatabaseSnapshot(
    @SerializedName("transactions")
    val transactions: List<TransactionEntity>,
    
    @SerializedName("categories")
    val categories: List<CategoryEntity>,
    
    @SerializedName("cards")
    val cards: List<CardEntity>,
    
    @SerializedName("account_balances")
    val accountBalances: List<AccountBalanceEntity>,
    
    @SerializedName("subscriptions")
    val subscriptions: List<SubscriptionEntity>,
    
    @SerializedName("merchant_mappings")
    val merchantMappings: List<MerchantMappingEntity>,
    
    @SerializedName("unrecognized_sms")
    val unrecognizedSms: List<UnrecognizedSmsEntity>,

    @SerializedName("budgets")
    val budgets: List<BudgetEntity> = emptyList(),

    @SerializedName("budget_category_limits")
    val budgetCategoryLimits: List<BudgetCategoryLimitEntity> = emptyList(),
    
    @SerializedName("subcategories")
    val subcategories: List<SubcategoryEntity> = emptyList(),

    @SerializedName("rules")
    val rules: List<RuleEntity> = emptyList(),

    @SerializedName("rule_applications")
    val ruleApplications: List<RuleApplicationEntity> = emptyList(),

    @SerializedName("webhook_profiles")
    val webhookProfiles: List<WebhookProfileBackup> = emptyList(),

    @SerializedName("chat_messages")
    val chatMessages: List<ChatMessage>
)

data class WebhookProfileBackup(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("url")
    val url: String,
    @SerializedName("enabled")
    val enabled: Boolean,
    @SerializedName("data_types")
    val dataTypes: List<String>,
    @SerializedName("range_preset")
    val rangePreset: String,
    // Stored as ISO-8601 strings rather than LocalDateTime because Gson has no built-in
    // TypeAdapter for java.time and would reflect on the class's internal fields.
    @SerializedName("custom_start")
    val customStart: String? = null,
    @SerializedName("custom_end")
    val customEnd: String? = null,
    @SerializedName("headers")
    val headers: List<WebhookHeader> = emptyList()
)

/**
 * User preferences snapshot
 */
data class PreferencesSnapshot(
    @SerializedName("theme")
    val theme: ThemePreferences,
    
    @SerializedName("sms")
    val sms: SmsPreferences,
    
    @SerializedName("developer")
    val developer: DeveloperPreferences,
    
    @SerializedName("app")
    val app: AppPreferences,

    @SerializedName("profile")
    val profile: ProfilePreferences? = null,

    @SerializedName("home_widgets")
    val homeWidgets: HomeWidgetPreferences? = null
)

/**
 * Home widget preferences
 */
data class HomeWidgetPreferences(
    @SerializedName("order")
    val order: List<String>,

    @SerializedName("hidden")
    val hidden: List<String>
)

/**
 * Theme-related preferences
 */
data class ThemePreferences(
    @SerializedName("is_dark_theme_enabled")
    val isDarkThemeEnabled: Boolean?,
    
    @SerializedName("is_dynamic_color_enabled")
    val isDynamicColorEnabled: Boolean,

    @SerializedName("is_amoled_mode")
    val isAmoledMode: Boolean? = null,

    @SerializedName("navigation_bar_style")
    val navigationBarStyle: String? = null,

    @SerializedName("app_font")
    val appFont: String? = null,

    @SerializedName("theme_style")
    val themeStyle: String? = null,

    @SerializedName("accent_color")
    val accentColor: String? = null,

    @SerializedName("hide_navigation_labels")
    val hideNavigationLabels: Boolean? = null,

    @SerializedName("hide_pill_indicator")
    val hidePillIndicator: Boolean? = null,

    @SerializedName("blur_effects")
    val blurEffects: Boolean? = null,

    @SerializedName("app_icon")
    val appIcon: String? = null
)

/**
 * SMS-related preferences
 */
data class SmsPreferences(
    @SerializedName("has_skipped_sms_permission")
    val hasSkippedSmsPermission: Boolean,
    
    @SerializedName("sms_scan_months")
    val smsScanMonths: Int,
    
    @SerializedName("last_scan_timestamp")
    val lastScanTimestamp: Long?,
    
    @SerializedName("last_scan_period")
    val lastScanPeriod: Int?
)

/**
 * Developer mode preferences
 */
data class DeveloperPreferences(
    @SerializedName("is_developer_mode_enabled")
    val isDeveloperModeEnabled: Boolean,
    
    @SerializedName("system_prompt")
    val systemPrompt: String?
)

/**
 * App-related preferences
 */
data class AppPreferences(
    @SerializedName("has_shown_scan_tutorial")
    val hasShownScanTutorial: Boolean,
    
    @SerializedName("first_launch_time")
    val firstLaunchTime: Long?,
    
    @SerializedName("has_shown_review_prompt")
    val hasShownReviewPrompt: Boolean,
    
    @SerializedName("last_review_prompt_time")
    val lastReviewPromptTime: Long?
)

/**
 * Profile-related preferences
 */
data class ProfilePreferences(
    @SerializedName("user_name")
    val userName: String = "User",

    @SerializedName("profile_image_uri")
    val profileImageUri: String? = null,

    @SerializedName("profile_background_color")
    val profileBackgroundColor: Int = 0,

    @SerializedName("banner_image_uri")
    val bannerImageUri: String? = null,

    @SerializedName("show_banner_image")
    val showBannerImage: Boolean = false
)

/**
 * Import result
 */
sealed class ImportResult {
    data class Success(
        val importedTransactions: Int,
        val importedCategories: Int,
        val skippedDuplicates: Int
    ) : ImportResult()
    
    data class Error(val message: String) : ImportResult()
}

/**
 * Export result
 */
sealed class ExportResult {
    data class Success(val file: java.io.File) : ExportResult()
    data class Error(val message: String) : ExportResult()
    data class Progress(val current: Int, val total: Int) : ExportResult()
}

/**
 * Import strategy options
 */
enum class ImportStrategy {
    REPLACE_ALL,    // Replace all existing data
    MERGE,          // Merge with existing data (skip duplicates)
    SELECTIVE       // User selects what to import
}

/**
 * Privacy level for export
 */
enum class ExportPrivacy {
    FULL,          // Export everything as-is
    MASKED,        // Mask sensitive data like account numbers
    ANONYMOUS      // Remove merchant names and descriptions
}

/**
 * Configuration for backup export
 */
data class BackupConfiguration(
    val privacy: ExportPrivacy = ExportPrivacy.FULL,
    val includeTransactionalData: Boolean = true,
    val includeProfileData: Boolean = true,
    val includeBudgets: Boolean = true,
    val includeAppPreferences: Boolean = true
)
