package com.ritesh.cashiro.data.backup

import android.content.Context
import android.os.Build
import com.google.gson.GsonBuilder
import com.ritesh.cashiro.BuildConfig
import android.net.Uri
import com.ritesh.cashiro.data.database.CashiroDatabase
import com.ritesh.cashiro.data.preferences.UserPreferencesRepository
import com.ritesh.cashiro.data.repository.WebhookRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java.io.FileOutputStream
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton
import com.ritesh.cashiro.data.database.entity.*
import androidx.core.net.toUri

@Singleton
class BackupExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: CashiroDatabase,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val webhookRepository: WebhookRepository
) {
    
    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeTypeAdapter())
        .registerTypeAdapter(LocalDate::class.java, LocalDateTypeAdapter())
        .registerTypeAdapter(BigDecimal::class.java, BigDecimalTypeAdapter())
        .create()
    
    /**
     * Export complete app data to a backup file
     */
    suspend fun exportBackup(
        config: BackupConfiguration = BackupConfiguration()
    ): ExportResult {
        return try {
            // Collect all data
            val backup = createBackup(config)
            
            // Create backup file
            val file = createBackupFile()
            
            ZipOutputStream(FileOutputStream(file)).use { zipOut ->
                // Write JSON to file
                val jsonEntry = ZipEntry("backup.json")
                zipOut.putNextEntry(jsonEntry)
                zipOut.write(gson.toJson(backup).toByteArray())
                zipOut.closeEntry()

                // Write Attachments
                if (config.privacy == ExportPrivacy.FULL && config.includeTransactionalData) {
                    val filesDir = context.filesDir
                    // Collect all unique attachment paths
                    val allAttachments = backup.database.transactions
                        .flatMap { it.attachments.split(",") }
                        .filter { it.isNotBlank() }
                        .toSet()

                    allAttachments.forEach { path ->
                        // path from DB is like "attachments/filename.ext"
                        val attachmentFile = File(filesDir, path)
                        if (attachmentFile.exists()) {
                            // Use the path directly as entry name to preserve structure
                            val entry = ZipEntry(path)
                            zipOut.putNextEntry(entry)
                            attachmentFile.inputStream().use { input ->
                                input.copyTo(zipOut)
                            }
                            zipOut.closeEntry()
                        }
                    }
                }

                // Write Profile Images
                if (config.includeProfileData) {
                    val prefs = userPreferencesRepository.userPreferences.first()
                    
                    prefs.profileImageUri?.let { uriStr ->
                        copyToZip(uriStr, "profile/profile_image", zipOut)
                    }
                    
                    prefs.bannerImageUri?.let { uriStr ->
                        copyToZip(uriStr, "profile/banner_image", zipOut)
                    }
                }
            }
            
            ExportResult.Success(file)
        } catch (e: Exception) {
            ExportResult.Error("Export failed: ${e.message}")
        }
    }
    
    /**
     * Create backup data structure
     */
    private suspend fun createBackup(config: BackupConfiguration): CashiroBackup {
        // Get all database data
        val transactions = if (config.includeTransactionalData) database.transactionDao().getAllTransactions().first() else emptyList()
        val categories = if (config.includeProfileData) database.categoryDao().getAllCategories().first() else emptyList()
        val cards = if (config.includeProfileData) database.cardDao().getAllCards().first() else emptyList()
        val accountBalances = if (config.includeTransactionalData) database.accountBalanceDao().getAllBalances().first() else emptyList()
        val subscriptions = if (config.includeBudgets) database.subscriptionDao().getAllSubscriptions().first() else emptyList()
        val merchantMappings = if (config.includeProfileData) database.merchantMappingDao().getAllMappings().first() else emptyList()
        val unrecognizedSms = if (config.includeTransactionalData) database.unrecognizedSmsDao().getAllUnrecognizedSms().first() else emptyList()
        val chatMessages = if (config.includeTransactionalData) database.chatDao().getAllMessages().first() else emptyList()
        val budgets = if (config.includeBudgets) database.budgetDao().getAllBudgets().first() else emptyList()
        val budgetCategoryLimits = if (config.includeBudgets) database.budgetDao().getAllCategoryLimits().first() else emptyList()
        val subcategories = if (config.includeProfileData) database.subcategoryDao().getAllSubcategories().first() else emptyList()
        val rules = if (config.includeAppPreferences) database.ruleDao().getAllRules().first() else emptyList()
        val ruleApplications = if (config.includeTransactionalData) database.ruleApplicationDao().getRecentApplications(1000).first() else emptyList() // Limit to recent apps for backup size
        val webhookProfiles = if (config.includeAppPreferences) {
            val baseCurrency = userPreferencesRepository.baseCurrency.first()
            database.webhookProfileDao().getAllProfiles().first().map { profile ->
                WebhookProfileBackup(
                    id = profile.id,
                    name = profile.name,
                    url = profile.url,
                    enabled = profile.enabled,
                    dataTypes = profile.dataTypes,
                    rangePreset = profile.rangePreset.name,
                    customStart = profile.customStart,
                    customEnd = profile.customEnd,
                    currency = baseCurrency,
                    headers = com.ritesh.cashiro.data.repository.WebhookHeaderEncoder.sanitizeForExport(
                        webhookRepository.decodeHeaders(profile.headersJson)
                    )
                )
            }
        } else emptyList()
        
        // Get preferences from repository
        val prefs = userPreferencesRepository.userPreferences.first()
        val systemPrompt = userPreferencesRepository.getSystemPrompt().first()
        val firstLaunchTime = userPreferencesRepository.getFirstLaunchTime().first()
        val hasShownReviewPrompt = userPreferencesRepository.getHasShownReviewPrompt().first()
        val lastReviewPromptTime = userPreferencesRepository.getLastReviewPromptTime().first()
        val lastScanTimestamp = userPreferencesRepository.getLastScanTimestamp().first()
        val lastScanPeriod = userPreferencesRepository.getLastScanPeriod().first()
        
        // Calculate statistics
        val dateRange = if (transactions.isNotEmpty()) {
            val sorted = transactions.sortedBy { it.dateTime }
            DateRange(
                earliest = sorted.first().dateTime.toString(),
                latest = sorted.last().dateTime.toString()
            )
        } else null
        
        // Apply privacy settings if needed
        val finalTransactions = when (config.privacy) {
            ExportPrivacy.FULL -> transactions
            ExportPrivacy.MASKED -> transactions.map { it.copy(
                smsBody = "[REDACTED]",
                accountNumber = it.accountNumber?.takeLast(4)?.let { "****$it" }
            )}
            ExportPrivacy.ANONYMOUS -> transactions.map { it.copy(
                merchantName = "Merchant",
                description = null,
                smsBody = "[REDACTED]",
                accountNumber = "****"
            )}
        }
        
        return CashiroBackup(
            metadata = BackupMetadata(
                exportId = UUID.randomUUID().toString(),
                appVersion = BuildConfig.VERSION_NAME,
                databaseVersion = 48,
                device = "${Build.MANUFACTURER} ${Build.MODEL}",
                androidVersion = Build.VERSION.SDK_INT,
                statistics = BackupStatistics(
                    totalTransactions = transactions.size,
                    totalCategories = categories.size,
                    totalCards = cards.size,
                    totalSubscriptions = subscriptions.size,
                    totalSubcategories = subcategories.size,
                    totalRules = rules.size,
                    dateRange = dateRange
                )
            ),
            database = DatabaseSnapshot(
                transactions = finalTransactions,
                categories = categories,
                cards = cards,
                accountBalances = accountBalances,
                subscriptions = subscriptions,
                merchantMappings = merchantMappings,
                unrecognizedSms = if (config.privacy == ExportPrivacy.FULL) unrecognizedSms else emptyList(),
                chatMessages = if (config.privacy == ExportPrivacy.FULL) chatMessages else emptyList(),
                budgets = budgets,
                budgetCategoryLimits = budgetCategoryLimits,
                subcategories = subcategories,
                rules = rules,
                ruleApplications = ruleApplications,
                webhookProfiles = webhookProfiles
            ),
            preferences = PreferencesSnapshot(
                theme = ThemePreferences(
                    isDarkThemeEnabled = if (config.includeAppPreferences) prefs.isDarkThemeEnabled else null,
                    isDynamicColorEnabled = if (config.includeAppPreferences) prefs.isDynamicColorEnabled else true,
                    isAmoledMode = if (config.includeAppPreferences) prefs.isAmoledMode else null,
                    navigationBarStyle = if (config.includeAppPreferences) prefs.navigationBarStyle.name else null,
                    appFont = if (config.includeAppPreferences) prefs.appFont.name else null,
                    themeStyle = if (config.includeAppPreferences) prefs.themeStyle.name else null,
                    accentColor = if (config.includeAppPreferences) prefs.accentColor.name else null,
                    hideNavigationLabels = if (config.includeAppPreferences) prefs.hideNavigationLabels else null,
                    hidePillIndicator = if (config.includeAppPreferences) prefs.hidePillIndicator else null,
                    blurEffects = if (config.includeAppPreferences) prefs.blurEffects else null,
                    appIcon = if (config.includeAppPreferences) prefs.appIcon.name else null
                ),
                sms = SmsPreferences(
                    hasSkippedSmsPermission = prefs.hasSkippedSmsPermission,
                    smsScanMonths = prefs.smsScanMonths,
                    lastScanTimestamp = lastScanTimestamp,
                    lastScanPeriod = lastScanPeriod
                ),
                developer = DeveloperPreferences(
                    isDeveloperModeEnabled = prefs.isDeveloperModeEnabled,
                    systemPrompt = systemPrompt
                ),
                app = AppPreferences(
                    hasShownScanTutorial = prefs.hasShownScanTutorial,
                    firstLaunchTime = firstLaunchTime,
                    hasShownReviewPrompt = hasShownReviewPrompt,
                    lastReviewPromptTime = lastReviewPromptTime
                ),
                profile = if (config.includeProfileData) ProfilePreferences(
                    userName = prefs.userName,
                    profileImageUri = if (prefs.profileImageUri != null) "profile/profile_image" else null,
                    profileBackgroundColor = prefs.profileBackgroundColor,
                    bannerImageUri = if (prefs.bannerImageUri != null) "profile/banner_image" else null,
                    showBannerImage = prefs.showBannerImage
                ) else null,
                homeWidgets = if (config.includeAppPreferences) {
                    val order = userPreferencesRepository.homeWidgetsOrder.first()
                    val hidden = userPreferencesRepository.hiddenHomeWidgets.first()
                    HomeWidgetPreferences(
                        order = order.map { it.name },
                        hidden = hidden.map { it.name }
                    )
                } else null
            )
        )
    }
    
    /**
     * Create backup file in cache directory
     */
    private fun createBackupFile(): File {
        val exportDir = File(context.cacheDir, "backups")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }
        
        val timestamp = LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyy_MM_dd_HHmmss")
        )
        val fileName = "Cashiro_Backup_$timestamp.zip"
        
        return File(exportDir, fileName)
    }


    private fun copyToZip(uriString: String, entryName: String, zipOut: ZipOutputStream) {
        try {
            val uri = uriString.toUri()
            val inputStream = context.contentResolver.openInputStream(uri)
            
            inputStream?.use { input ->
                val entry = ZipEntry(entryName)
                zipOut.putNextEntry(entry)
                input.copyTo(zipOut)
                zipOut.closeEntry()
            }
        } catch (e: Exception) {
            // Log or ignore image copy failure to avoid failing entire backup
            e.printStackTrace()
        }
    }
}
