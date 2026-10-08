package com.ritesh.cashiro

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.ritesh.cashiro.data.database.CashiroDatabase
import com.ritesh.cashiro.data.database.entity.TransactionEntity
import com.ritesh.cashiro.data.database.entity.TransactionType
import com.ritesh.cashiro.data.repository.CategoryRepository
import com.ritesh.cashiro.data.repository.SubcategoryRepository
import com.ritesh.cashiro.data.repository.RuleRepositoryImpl
import com.ritesh.cashiro.domain.model.rule.*
import com.ritesh.cashiro.domain.service.RuleEngine
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime

class RuleLogicalOperatorTest {
    @Test fun storedOrRuleSurvivesBothTypeFiltersAndApplies() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, CashiroDatabase::class.java).build()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            val engine = RuleEngine(SubcategoryRepository(db.subcategoryDao(), db.categoryDao(), context, scope),
                CategoryRepository(db.categoryDao(), context, scope))
            val repository = RuleRepositoryImpl(db.ruleDao(), db.ruleApplicationDao(), engine)
            val rule = TransactionRule(name = "Synthetic OR rule", conditions = listOf(
                RuleCondition(TransactionField.TYPE, ConditionOperator.EQUALS, "INCOME"),
                RuleCondition(TransactionField.MERCHANT, ConditionOperator.EQUALS, "Example Shop", LogicalOperator.OR)
            ), actions = listOf(RuleAction(TransactionField.NARRATION, ActionType.SET, "Synthetic matched note")))
            repository.insertRule(rule)
            val candidates = repository.getActiveRulesByType(TransactionType.EXPENSE)
            assertEquals(1, candidates.size)
            assertEquals(1, repository.getActiveRulesByTypes(listOf(TransactionType.EXPENSE)).size)
            val row = TransactionEntity(amount = BigDecimal("100.00"), merchantName = "Example Shop", category = "Food",
                transactionType = TransactionType.EXPENSE, dateTime = LocalDateTime.of(2026, 1, 1, 12, 0), transactionHash = "synthetic-rule")
            assertEquals("Synthetic matched note", engine.evaluateRulesForType(row, null, candidates, row.transactionType).first.description)
            val blocking = rule.copy(actions = listOf(RuleAction(TransactionField.MERCHANT, ActionType.BLOCK, "")))
            assertNotNull(engine.shouldBlockTransaction(row, null, listOf(blocking)))
            val mixed = blocking.copy(conditions = blocking.conditions + RuleCondition(TransactionField.AMOUNT, ConditionOperator.GREATER_THAN, "200"))
            assertNull(engine.shouldBlockTransaction(row, null, listOf(mixed)))
            val equalAmount = blocking.copy(conditions = listOf(RuleCondition(TransactionField.AMOUNT, ConditionOperator.EQUALS, "100")))
            assertNotNull(engine.shouldBlockTransaction(row, null, listOf(equalAmount)))
            val unequalAmount = equalAmount.copy(conditions = listOf(RuleCondition(TransactionField.AMOUNT, ConditionOperator.NOT_EQUALS, "100")))
            assertNull(engine.shouldBlockTransaction(row, null, listOf(unequalAmount)))
            val invalidAmount = unequalAmount.copy(conditions = listOf(RuleCondition(TransactionField.AMOUNT, ConditionOperator.NOT_EQUALS, "invalid")))
            assertNull(engine.shouldBlockTransaction(row, null, listOf(invalidAmount)))
        } finally { scope.cancel(); db.close() }
    }
}
