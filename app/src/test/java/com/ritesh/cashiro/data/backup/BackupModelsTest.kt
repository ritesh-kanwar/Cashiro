package com.ritesh.cashiro.data.backup

import com.google.gson.GsonBuilder
import com.ritesh.cashiro.data.database.entity.*
import com.ritesh.cashiro.data.backup.LocalDateTimeTypeAdapter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime

class BackupModelsTest {

    private val gson = GsonBuilder()
        .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeTypeAdapter())
        .create()

    @Test
    fun backupSerializationWithRules() {
        val backup = CashiroBackup(
            metadata = BackupMetadata(
                exportId = "test-export-1",
                appVersion = "2.15.50",
                databaseVersion = 20,
                device = "Test Device",
                androidVersion = 30,
                statistics = BackupStatistics(
                    totalTransactions = 1,
                    totalCategories = 1,
                    totalCards = 0,
                    totalSubscriptions = 0,
                    totalRules = 1,
                    dateRange = DateRange(earliest = "2024-01-01T00:00:00", latest = "2024-01-02T00:00:00")
                )
            ),
            database = DatabaseSnapshot(
                transactions = listOf(
                    TransactionEntity(
                        id = 1,
                        amount = BigDecimal("100.0"),
                        merchantName = "Test Merchant",
                        category = "Food",
                        transactionType = TransactionType.EXPENSE,
                        dateTime = LocalDateTime.of(2024, 1, 1, 10, 0),
                        description = "Test description",
                        smsBody = "Test SMS",
                        bankName = "Test Bank",
                        smsSender = "TEST",
                        accountNumber = "1234567890",
                        balanceAfter = BigDecimal("1000.0"),
                        transactionHash = "hash123",
                        isRecurring = false,
                        isDeleted = false,
                        createdAt = LocalDateTime.now(),
                        updatedAt = LocalDateTime.now(),
                        currency = "INR",
                        fromAccount = null,
                        toAccount = null,
                        reference = null
                    )
                ),
                categories = listOf(
                    CategoryEntity(
                        id = 1,
                        name = "Food",
                        color = "#FF0000",
                        isSystem = false,
                        isIncome = false,
                        displayOrder = 1,
                        createdAt = LocalDateTime.now(),
                        updatedAt = LocalDateTime.now()
                    )
                ),
                cards = emptyList(),
                accountBalances = emptyList(),
                subscriptions = emptyList(),
                merchantMappings = emptyList(),
                unrecognizedSms = emptyList(),
                chatMessages = emptyList(),
                rules = listOf(
                    RuleEntity(
                        id = "rule-1",
                        name = "Test Rule",
                        description = "Test description",
                        priority = 0,
                        conditions = """{"merchant": ".*"}""",
                        actions = """{"category": "Food"}""",
                        isActive = true,
                        isSystemTemplate = false,
                        createdAt = LocalDateTime.now(),
                        updatedAt = LocalDateTime.now()
                    )
                ),
                ruleApplications = listOf(
                    RuleApplicationEntity(
                        id = "app-1",
                        ruleId = "rule-1",
                        ruleName = "Test Rule",
                        transactionId = "1",
                        fieldsModified = """["category"]""",
                        appliedAt = LocalDateTime.now()
                    )
                )
            ),
            preferences = PreferencesSnapshot(
                theme = ThemePreferences(
                    isDarkThemeEnabled = true,
                    isDynamicColorEnabled = false
                ),
                sms = SmsPreferences(
                    hasSkippedSmsPermission = false,
                    smsScanMonths = 6,
                    lastScanTimestamp = null,
                    lastScanPeriod = null
                ),
                developer = DeveloperPreferences(
                    isDeveloperModeEnabled = false,
                    systemPrompt = null
                ),
                app = AppPreferences(
                    hasShownScanTutorial = true,
                    firstLaunchTime = null,
                    hasShownReviewPrompt = false,
                    lastReviewPromptTime = null
                )
            )
        )

        val json = gson.toJson(backup)
        assertNotNull(json)

        val deserialized = gson.fromJson(json, CashiroBackup::class.java)
        assertNotNull(deserialized)

        assertEquals(backup.metadata.statistics.totalRules, deserialized.metadata.statistics.totalRules)

        assertEquals(1, deserialized.database.rules.size)
        assertEquals(1, deserialized.database.ruleApplications.size)

        val rule = deserialized.database.rules[0]
        assertEquals("Test Rule", rule.name)
    }

    @Test
    fun backupSerializationWithEmptyLists() {
        val backup = CashiroBackup(
            metadata = BackupMetadata(
                exportId = "empty-test",
                appVersion = "2.15.50",
                databaseVersion = 20,
                device = "Test Device",
                androidVersion = 30,
                statistics = BackupStatistics(
                    totalTransactions = 0,
                    totalCategories = 0,
                    totalCards = 0,
                    totalSubscriptions = 0,
                    totalRules = 0,
                    dateRange = null
                )
            ),
            database = DatabaseSnapshot(
                transactions = emptyList(),
                categories = emptyList(),
                cards = emptyList(),
                accountBalances = emptyList(),
                subscriptions = emptyList(),
                merchantMappings = emptyList(),
                unrecognizedSms = emptyList(),
                chatMessages = emptyList(),
                rules = emptyList(),
                ruleApplications = emptyList()
            ),
            preferences = PreferencesSnapshot(
                theme = ThemePreferences(isDarkThemeEnabled = null, isDynamicColorEnabled = false),
                sms = SmsPreferences(hasSkippedSmsPermission = false, smsScanMonths = 6, lastScanTimestamp = null, lastScanPeriod = null),
                developer = DeveloperPreferences(isDeveloperModeEnabled = false, systemPrompt = null),
                app = AppPreferences(hasShownScanTutorial = false, firstLaunchTime = null, hasShownReviewPrompt = false, lastReviewPromptTime = null)
            )
        )

        val json = gson.toJson(backup)
        val deserialized = gson.fromJson(json, CashiroBackup::class.java)

        assertEquals(0, deserialized.database.rules.size)
    }
}