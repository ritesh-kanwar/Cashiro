package com.ritesh.cashiro.data.backup

import android.content.Context
import android.os.Build
import com.google.gson.GsonBuilder
import com.ritesh.cashiro.BuildConfig
import com.ritesh.cashiro.data.database.CashiroDatabase
import com.ritesh.cashiro.data.preferences.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: CashiroDatabase,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    
    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeTypeAdapter())
        .registerTypeAdapter(LocalDate::class.java, LocalDateTypeAdapter())
        .registerTypeAdapter(java.math.BigDecimal::class.java, BigDecimalTypeAdapter())
        .create()
    
    /**
     * Export complete app data to a backup file
     */
    suspend fun exportBackup(
        privacy: ExportPrivacy = ExportPrivacy.FULL
    ): ExportResult {
        return try {
            // Collect all data
            val backup = createBackup(privacy)
            
            // Create backup file
            val file = createBackupFile()
            
            // Write JSON to file
            file.writeText(gson.toJson(backup))
            
            ExportResult.Success(file)
        } catch (e: Exception) {
            ExportResult.Error("Export failed: ${e.message}")
        }
    }
    
    /**
     * Create backup data structure
     */
    private suspend fun createBackup(privacy: ExportPrivacy): PennyWiseBackup {
        // Get all database data
        val transactions = database.transactionDao().getAllTransactions().first()
        val categories = database.categoryDao().getAllCategories().first()
        val cards = database.cardDao().getAllCards().first()
        val accountBalances = database.accountBalanceDao().getAllBalances().first()
        val subscriptions = database.subscriptionDao().getAllSubscriptions().first()
        val merchantMappings = database.merchantMappingDao().getAllMappings().first()
        val unrecognizedSms = database.unrecognizedSmsDao().getAllUnrecognizedSms().first()
        val chatMessages = database.chatDao().getAllMessages().first()
        
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
        val finalTransactions = when (privacy) {
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
        
        return PennyWiseBackup(
            metadata = BackupMetadata(
                exportId = UUID.randomUUID().toString(),
                appVersion = BuildConfig.VERSION_NAME,
                databaseVersion = 20, // Current database version
                device = "${Build.MANUFACTURER} ${Build.MODEL}",
                androidVersion = Build.VERSION.SDK_INT,
                statistics = BackupStatistics(
                    totalTransactions = transactions.size,
                    totalCategories = categories.size,
                    totalCards = cards.size,
                    totalSubscriptions = subscriptions.size,
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
                unrecognizedSms = if (privacy == ExportPrivacy.FULL) unrecognizedSms else emptyList(),
                chatMessages = if (privacy == ExportPrivacy.FULL) chatMessages else emptyList()
            ),
            preferences = PreferencesSnapshot(
                theme = ThemePreferences(
                    isDarkThemeEnabled = prefs.isDarkThemeEnabled,
                    isDynamicColorEnabled = prefs.isDynamicColorEnabled
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
                )
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
        val fileName = "PennyWise_Backup_$timestamp.pennywisebackup"
        
        return File(exportDir, fileName)
    }
}