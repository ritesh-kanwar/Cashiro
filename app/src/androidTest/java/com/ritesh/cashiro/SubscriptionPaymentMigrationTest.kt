package com.ritesh.cashiro

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ritesh.cashiro.data.database.CashiroDatabase
import com.ritesh.cashiro.data.database.entity.SubscriptionPaymentEntity
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class SubscriptionPaymentMigrationTest {
    @Test fun version63KeepsSubscriptionsAndBalancesAndAllowsIndependentCurrencyAtSameTimestamp() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "synthetic-subscription-migration.db"
        context.deleteDatabase(name)
        val schema = InstrumentationRegistry.getInstrumentation().context.assets
            .open("com.ritesh.cashiro.data.database.CashiroDatabase/63.json").bufferedReader()
            .use { JSONObject(it.readText()).getJSONObject("database") }
        context.openOrCreateDatabase(name, 0, null).use { sql ->
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                val table = entity.getString("tableName")
                sql.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = entity.optJSONArray("indices") ?: JSONArray()
                for (j in 0 until indices.length()) sql.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
            }
            val setup = schema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) sql.execSQL(setup.getString(i))
            sql.execSQL("""INSERT INTO subscriptions(id, merchant_name, amount, next_payment_date, state, bank_name, created_at, updated_at)
                VALUES(1, 'Example service', '100', '2026-01-01', 'ACTIVE', 'Example Bank', '2026-01-01T00:00', '2026-01-01T00:00')""")
            sql.execSQL("""INSERT INTO account_balances(bank_name, account_last4, balance, timestamp, created_at, currency, source_type)
                VALUES('Example Bank', '1000', '1000', '2026-01-01T00:00', '2026-01-01T00:00', 'INR', 'MANUAL')""")
            sql.version = 63
        }
        val db = Room.databaseBuilder(context, CashiroDatabase::class.java, name)
            .addMigrations(*CashiroDatabase.ALL_MIGRATIONS).build()
        try {
            val subscription = db.subscriptionDao().getSubscriptionById(1)!!
            assertEquals("Example service", subscription.merchantName)
            assertNull(subscription.accountLast4)
            val balance = db.accountBalanceDao().getLatestBalanceForCurrency("Example Bank", "1000", "INR")!!
            db.accountBalanceDao().insertBalance(balance.copy(id = 0, currency = "USD"))
            assertNotNull(db.accountBalanceDao().getLatestBalanceForCurrency("Example Bank", "1000", "INR"))
            assertNotNull(db.accountBalanceDao().getLatestBalanceForCurrency("Example Bank", "1000", "USD"))
            val date = LocalDate.of(2026, 1, 1)
            db.subscriptionPaymentDao().insert(SubscriptionPaymentEntity(1, date, date, 99))
            assertEquals(99L, db.subscriptionPaymentDao().forCycle(1, date)!!.transactionId)
            db.subscriptionDao().deleteSubscriptionById(1)
            assertNull(db.subscriptionPaymentDao().forCycle(1, date))
            assertEquals(64, db.openHelper.readableDatabase.version)
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
