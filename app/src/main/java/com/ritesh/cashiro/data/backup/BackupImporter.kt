package com.ritesh.cashiro.data.backup

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.gson.GsonBuilder
import androidx.room.withTransaction
import com.ritesh.cashiro.data.database.CashiroDatabase
import com.ritesh.cashiro.data.database.entity.*
import com.ritesh.cashiro.data.preferences.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: CashiroDatabase,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    
    private val gson = GsonBuilder()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeTypeAdapter())
        .registerTypeAdapter(LocalDate::class.java, LocalDateTypeAdapter())
        .registerTypeAdapter(java.math.BigDecimal::class.java, BigDecimalTypeAdapter())
        .create()
    
    /**
     * Import backup from a file URI
     */
    suspend fun importBackup(
        uri: Uri,
        strategy: ImportStrategy = ImportStrategy.MERGE
    ): ImportResult = withContext(Dispatchers.IO) {
        try {
            // Read and parse the backup file
            val backup = readBackupFile(uri)
            
            // Validate backup version
            if (!isCompatibleVersion(backup)) {
                return@withContext ImportResult.Error("Incompatible backup version")
            }
            
            // Import based on strategy
            when (strategy) {
                ImportStrategy.REPLACE_ALL -> replaceAllData(backup)
                ImportStrategy.MERGE -> mergeData(backup)
                ImportStrategy.SELECTIVE -> mergeData(backup) // For now, same as merge
            }
        } catch (e: Exception) {
            Log.e("BackupImporter", "Import failed", e)
            ImportResult.Error("Import failed: ${e.message}")
        }
    }
    
    /**
     * Read and parse backup file
     */
    private suspend fun readBackupFile(uri: Uri): PennyWiseBackup {
        return withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val reader = BufferedReader(InputStreamReader(inputStream))
                val content = reader.readText()
                gson.fromJson(content, PennyWiseBackup::class.java)
            } ?: throw Exception("Failed to read backup file")
        }
    }
    
    /**
     * Check if backup version is compatible
     */
    private fun isCompatibleVersion(backup: PennyWiseBackup): Boolean {
        // For now, accept all v1.x backups
        return backup.format.startsWith("PennyWise Backup v1")
    }
    
    /**
     * Replace all existing data with backup data
     */
    private suspend fun replaceAllData(backup: PennyWiseBackup): ImportResult {
        var importedTransactions = 0
        var importedCategories = 0
        
        return database.withTransaction {
            try {
                // Clear existing data
                database.transactionDao().deleteAllTransactions()
                database.categoryDao().deleteAllCategories()
                database.cardDao().deleteAllCards()
                database.accountBalanceDao().deleteAllBalances()
                database.subscriptionDao().deleteAllSubscriptions()
                database.merchantMappingDao().deleteAllMappings()
                database.unrecognizedSmsDao().deleteAll()
                database.chatDao().deleteAllMessages()
                
                // Import all data
                backup.database.categories.forEach { category ->
                    database.categoryDao().insertCategory(category)
                    importedCategories++
                }
                
                backup.database.transactions.forEach { transaction ->
                    database.transactionDao().insertTransaction(transaction)
                    importedTransactions++
                }
                
                backup.database.cards.forEach { card ->
                    database.cardDao().insertCard(card)
                }
                
                backup.database.accountBalances.forEach { balance ->
                    database.accountBalanceDao().insertBalance(balance)
                }
                
                backup.database.subscriptions.forEach { subscription ->
                    database.subscriptionDao().insertSubscription(subscription)
                }
                
                backup.database.merchantMappings.forEach { mapping ->
                    database.merchantMappingDao().insertMapping(mapping)
                }
                
                backup.database.unrecognizedSms.forEach { sms ->
                    database.unrecognizedSmsDao().insert(sms)
                }
                
                backup.database.chatMessages.forEach { message ->
                    database.chatDao().insertMessage(message)
                }
                
                // Import preferences
                importPreferences(backup.preferences)
                
                ImportResult.Success(
                    importedTransactions = importedTransactions,
                    importedCategories = importedCategories,
                    skippedDuplicates = 0
                )
            } catch (e: Exception) {
                throw e
            }
        }
    }
    
    /**
     * Merge backup data with existing data
     */
    private suspend fun mergeData(backup: PennyWiseBackup): ImportResult {
        var importedTransactions = 0
        var importedCategories = 0
        var skippedDuplicates = 0
        
        return database.withTransaction {
            try {
                // Get existing data for duplicate checking
                val existingTransactionHashes = database.transactionDao()
                    .getAllTransactions().first()
                    .map { it.transactionHash }
                    .toSet()
                
                val existingCategories = database.categoryDao()
                    .getAllCategories().first()
                    .map { it.name }
                    .toSet()
                
                // Import categories (merge by name)
                backup.database.categories.forEach { category ->
                    if (!existingCategories.contains(category.name)) {
                        // Generate new ID for imported category
                        val newCategory = category.copy(id = 0)
                        database.categoryDao().insertCategory(newCategory)
                        importedCategories++
                    }
                }
                
                // Import transactions (skip duplicates by hash)
                backup.database.transactions.forEach { transaction ->
                    if (!existingTransactionHashes.contains(transaction.transactionHash)) {
                        // Generate new ID for imported transaction
                        val newTransaction = transaction.copy(id = 0)
                        database.transactionDao().insertTransaction(newTransaction)
                        importedTransactions++
                    } else {
                        skippedDuplicates++
                    }
                }
                
                // Import other entities with duplicate checking
                importCardsWithMerge(backup.database.cards)
                importAccountBalancesWithMerge(backup.database.accountBalances)
                importSubscriptionsWithMerge(backup.database.subscriptions)
                importMerchantMappingsWithMerge(backup.database.merchantMappings)
                
                // Import preferences (merge with existing)
                importPreferences(backup.preferences)
                
                ImportResult.Success(
                    importedTransactions = importedTransactions,
                    importedCategories = importedCategories,
                    skippedDuplicates = skippedDuplicates
                )
            } catch (e: Exception) {
                throw e
            }
        }
    }
    
    /**
     * Import cards with duplicate checking
     */
    private suspend fun importCardsWithMerge(cards: List<CardEntity>) {
        val existingCards = database.cardDao().getAllCards().first()
        val existingCardKeys = existingCards.map { "${it.bankName}_${it.cardLast4}" }.toSet()
        
        cards.forEach { card ->
            val key = "${card.bankName}_${card.cardLast4}"
            if (!existingCardKeys.contains(key)) {
                val newCard = card.copy(id = 0)
                database.cardDao().insertCard(newCard)
            }
        }
    }
    
    /**
     * Import account balances with duplicate checking
     */
    private suspend fun importAccountBalancesWithMerge(balances: List<AccountBalanceEntity>) {
        // For balances, we'll import all as they represent historical data
        balances.forEach { balance ->
            val newBalance = balance.copy(id = 0)
            database.accountBalanceDao().insertBalance(newBalance)
        }
    }
    
    /**
     * Import subscriptions with duplicate checking
     */
    private suspend fun importSubscriptionsWithMerge(subscriptions: List<SubscriptionEntity>) {
        val existingSubscriptions = database.subscriptionDao().getAllSubscriptions().first()
        val existingKeys = existingSubscriptions.map { "${it.merchantName}_${it.amount}" }.toSet()
        
        subscriptions.forEach { subscription ->
            val key = "${subscription.merchantName}_${subscription.amount}"
            if (!existingKeys.contains(key)) {
                val newSubscription = subscription.copy(id = 0)
                database.subscriptionDao().insertSubscription(newSubscription)
            }
        }
    }
    
    /**
     * Import merchant mappings with merge
     */
    private suspend fun importMerchantMappingsWithMerge(mappings: List<MerchantMappingEntity>) {
        mappings.forEach { mapping ->
            // Merchant mappings use merchant name as primary key, so just insert/replace
            database.merchantMappingDao().insertMapping(mapping)
        }
    }
    
    /**
     * Import user preferences
     */
    private suspend fun importPreferences(preferences: PreferencesSnapshot) {
        // Theme preferences
        preferences.theme.isDarkThemeEnabled?.let {
            userPreferencesRepository.updateDarkTheme(it)
        }
        userPreferencesRepository.updateDynamicColor(preferences.theme.isDynamicColorEnabled)
        
        // SMS preferences
        userPreferencesRepository.updateHasSkippedSmsPermission(preferences.sms.hasSkippedSmsPermission)
        userPreferencesRepository.updateSmsScanMonths(preferences.sms.smsScanMonths)
        preferences.sms.lastScanTimestamp?.let {
            userPreferencesRepository.updateLastScanTimestamp(it)
        }
        preferences.sms.lastScanPeriod?.let {
            userPreferencesRepository.updateLastScanPeriod(it)
        }
        
        // Developer preferences
        userPreferencesRepository.updateDeveloperMode(preferences.developer.isDeveloperModeEnabled)
        preferences.developer.systemPrompt?.let {
            userPreferencesRepository.updateSystemPrompt(it)
        }
        
        // App preferences
        userPreferencesRepository.updateHasShownScanTutorial(preferences.app.hasShownScanTutorial)
        preferences.app.firstLaunchTime?.let {
            userPreferencesRepository.updateFirstLaunchTime(it)
        }
        userPreferencesRepository.updateHasShownReviewPrompt(preferences.app.hasShownReviewPrompt)
        preferences.app.lastReviewPromptTime?.let {
            userPreferencesRepository.updateLastReviewPromptTime(it)
        }
    }
}