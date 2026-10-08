package com.ritesh.cashiro

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ritesh.cashiro.data.database.CashiroDatabase
import com.ritesh.cashiro.data.database.entity.*
import com.ritesh.cashiro.data.repository.CategoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class CategoryRenameTest {
    private lateinit var db: CashiroDatabase
    private val time = LocalDateTime.of(2026, 1, 1, 12, 0)

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext, CashiroDatabase::class.java
        ).build()
    }
    @After fun tearDown() { db.close() }

    private suspend fun seed(): CategoryEntity {
        val category = CategoryEntity(name = "Example old", color = "#123456")
        val id = db.categoryDao().insertCategory(category)
        db.subcategoryDao().insertSubcategory(SubcategoryEntity(id = 7, categoryId = id, name = "Example child"))
        db.transactionDao().insertTransaction(TransactionEntity(
            id = 11, amount = BigDecimal("1.23"), merchantName = "Example merchant", category = category.name,
            subcategory = "Example child", transactionType = TransactionType.EXPENSE, dateTime = time,
            transactionHash = "example-rename", isDeleted = true
        ))
        db.merchantMappingDao().insertMapping(MerchantMappingEntity("Example merchant", category.name))
        db.subscriptionDao().insertSubscription(SubscriptionEntity(
            merchantName = "Example subscription", amount = BigDecimal.ONE, nextPaymentDate = null,
            category = category.name, subcategory = "Example child"
        ))
        val person = db.lendBorrowDao().insertPerson(LendBorrowPersonEntity(name = "Example ledger", category = category.name))
        db.lendBorrowDao().insertTransaction(LendBorrowTransactionEntity(
            personId = person, type = LendBorrowType.LENT, amount = BigDecimal.ONE,
            title = "Example loan", category = category.name
        ))
        val budget = db.budgetDao().insertBudget(BudgetEntity(name = "Example budget", amount = BigDecimal.TEN, year = 2026, month = 1))
        db.budgetDao().insertCategoryLimit(BudgetCategoryLimitEntity(budgetId = budget, categoryName = category.name, limitAmount = BigDecimal("0.10")))
        db.budgetDao().insertCategoryLimit(BudgetCategoryLimitEntity(budgetId = budget, categoryName = "Example new", limitAmount = BigDecimal("0.20")))
        db.ruleDao().insertRule(RuleEntity(
            id = "example-rule", name = "Example rule", description = null, priority = 1,
            conditions = """[{"field":"CATEGORY","operator":"IN","value":"Example old,Other","future":{"enabled":true}}]""",
            actions = """[{"field":"CATEGORY","actionType":"SET","value":"Example old","future":7}]""",
            isActive = false, createdAt = time, updatedAt = time
        ))
        return category.copy(id = id)
    }

    private fun text(sql: String): String = db.openHelper.readableDatabase.query(sql).use {
        assertTrue(it.moveToFirst())
        it.getString(0)
    }

    private fun assertReferences(name: String) {
        for ((table, column) in listOf(
            "transactions" to "category", "merchant_mappings" to "category",
            "subscriptions" to "category", "lend_borrow_persons" to "category",
            "lend_borrow_transactions" to "category_name"
        )) assertEquals(table, name, text("SELECT $column FROM $table"))
    }

    @Test fun renamePreservesSubcategoriesAndUpdatesEveryReferenceIncludingInactiveRules() = runBlocking {
        val old = seed()
        db.transactionDao().insertTransaction(db.transactionDao().getTransactionById(11)!!.copy(
            id = 12, category = "EXAMPLE OLD", transactionHash = "example-unrelated-case"
        ))
        val otherBudget = db.budgetDao().insertBudget(BudgetEntity(
            name = "Other budget", amount = BigDecimal.TEN, year = 2026, month = 2
        ))
        db.budgetDao().insertCategoryLimit(BudgetCategoryLimitEntity(
            budgetId = otherBudget, categoryName = "Example new", limitAmount = BigDecimal("0.40")
        ))
        db.categoryDao().updateCategoryWithReferences(old.copy(name = "Example new"))
        assertReferences("Example new")
        assertEquals("EXAMPLE OLD", db.transactionDao().getTransactionById(12)!!.category)
        assertEquals(0, BigDecimal("0.40").compareTo(
            db.budgetDao().getCategoryLimitsForBudgetSync(otherBudget).single().limitAmount
        ))
        assertEquals("Example new", db.categoryDao().getCategoryById(old.id)!!.name)
        assertEquals(old.id, db.subcategoryDao().getSubcategoryById(7)!!.categoryId)
        assertEquals("Example child", text("SELECT subcategory FROM transactions"))
        assertEquals("Example child", text("SELECT subcategory FROM subscriptions"))
        val limits = db.categoryDao().limitsForRename(old.name, "Example new").filter { it.budgetId != otherBudget }
        assertEquals(1, limits.size)
        assertEquals(0, BigDecimal("0.30").compareTo(limits.single().limitAmount))
        val rule = db.ruleDao().getRuleById("example-rule")!!
        assertTrue(rule.conditions.contains("Example new,Other"))
        assertTrue(rule.conditions.contains("\"future\":{\"enabled\":true}"))
        assertTrue(rule.actions.contains("\"future\":7"))
        assertTrue(rule.actions.contains("Example new"))
    }

    @Test fun malformedRuleRollsBackAlreadyUpdatedReferencesAndBudgetMerge() = runBlocking {
        val old = seed()
        val rule = db.ruleDao().getRuleById("example-rule")!!
        db.ruleDao().updateRule(rule.copy(actions = "invalid JSON"))
        try {
            db.categoryDao().updateCategoryWithReferences(old.copy(name = "Example new"))
            fail("Malformed rules must abort the whole rename")
        } catch (_: IllegalArgumentException) { }
        assertReferences(old.name)
        assertEquals(old.name, db.categoryDao().getCategoryById(old.id)!!.name)
        assertEquals(2, db.categoryDao().limitsForRename(old.name, "Example new").size)
        assertEquals(rule.conditions, db.ruleDao().getRuleById(rule.id)!!.conditions)
    }

    @Test fun duplicateNameIsRejectedButCaseOnlyRenameIsAllowed() = runBlocking {
        val old = seed()
        db.categoryDao().insertCategory(CategoryEntity(name = "Example taken", color = "#123456"))
        try {
            db.categoryDao().updateCategoryWithReferences(old.copy(name = "EXAMPLE TAKEN"))
            fail("Case-insensitive duplicate must be rejected")
        } catch (_: IllegalArgumentException) { }
        assertReferences(old.name)
        db.categoryDao().updateCategoryWithReferences(old.copy(name = "EXAMPLE OLD"))
        assertReferences("EXAMPLE OLD")
        assertEquals(old.id, db.subcategoryDao().getSubcategoryById(7)!!.categoryId)
    }

    @Test fun resettingSystemCategoryRenamesReferencesThroughTheSameTransaction() = runBlocking {
        val old = seed().copy(isSystem = true, defaultName = "Example default")
        db.categoryDao().updateCategory(old)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            val repository = CategoryRepository(
                db.categoryDao(), InstrumentationRegistry.getInstrumentation().targetContext, scope
            )
            repository.resetCategoryToDefault(old.id)
            assertReferences("Example default")
            assertEquals("Example default", db.categoryDao().getCategoryById(old.id)!!.name)
            assertEquals(old.id, db.subcategoryDao().getSubcategoryById(7)!!.categoryId)
        } finally { scope.cancel() }
    }

}
