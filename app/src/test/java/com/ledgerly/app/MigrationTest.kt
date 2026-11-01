package com.ledgerly.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.test.core.app.ApplicationProvider
import com.ledgerly.app.data.LedgerDatabase
import com.ledgerly.app.data.LedgerRepository
import com.ledgerly.app.data.Recurring
import com.ledgerly.app.data.Cadence
import com.ledgerly.app.data.TxType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * Opens a real v1 database and upgrades it with the v2 helper. This is the test that stands
 * between a schema change and a user's ledger.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigrationTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private fun createV1Database() {
        val helper = object : SQLiteOpenHelper(context, DB_NAME, null, 1) {
            override fun onCreate(db: SQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE transactions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        type TEXT NOT NULL,
                        amount_cents INTEGER NOT NULL,
                        category TEXT NOT NULL,
                        note TEXT NOT NULL DEFAULT '',
                        date_epoch_day INTEGER NOT NULL,
                        created_at INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX idx_transactions_date ON transactions(date_epoch_day)")
                db.execSQL(
                    """
                    CREATE TABLE budgets (
                        category TEXT PRIMARY KEY,
                        limit_cents INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }

            override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        }

        val db = helper.writableDatabase
        db.insert("transactions", null, ContentValues().apply {
            put("type", "expense")
            put("amount_cents", 45_000L)
            put("category", "groceries")
            put("note", "Weekly shop")
            put("date_epoch_day", LocalDate.of(2026, 3, 4).toEpochDay())
            put("created_at", 1_700_000_000_000L)
        })
        db.insert("budgets", null, ContentValues().apply {
            put("category", "groceries")
            put("limit_cents", 2_000_000L)
        })
        helper.close()
    }

    private fun <T> withDatabase(block: (LedgerDatabase) -> T): T {
        val db = LedgerDatabase(context)
        try {
            return block(db)
        } finally {
            db.close()
        }
    }

    @Test
    fun `upgrading keeps transactions and turns budgets into defaults`() {
        createV1Database()

        withDatabase { db ->
            val transactions = db.readTransactions()
            assertEquals(1, transactions.size)
            assertEquals(45_000L, transactions.single().amountCents)
            assertEquals("Weekly shop", transactions.single().note)

            val budgets = db.readBudgetRows()
            assertEquals(1, budgets.size)
            assertEquals("groceries", budgets.single().categoryKey)
            assertEquals(2_000_000L, budgets.single().limitCents)
            assertTrue(budgets.single().month.isEmpty())
        }
    }

    @Test
    fun `upgrading adds the recurring table`() {
        createV1Database()

        withDatabase { db ->
            val id = db.insertRecurring(
                Recurring(
                    amountCents = 1_500_000L,
                    categoryKey = "bills",
                    note = "Rent",
                    cadence = Cadence.MONTHLY,
                    nextDue = LocalDate.of(2026, 4, 1)
                )
            )
            assertTrue(id > 0L)
            val stored = db.readRecurring().single()
            assertEquals("Rent", stored.note)
            assertEquals(LocalDate.of(2026, 4, 1), stored.nextDue)
            assertEquals(Cadence.MONTHLY, stored.cadence)
            assertEquals(TxType.EXPENSE, stored.type)
        }
    }

    @Test
    fun `budget overrides win for their month only`() {
        createV1Database()
        val repository = LedgerRepository(context)

        repository.setBudget("groceries", 2_500_000L, "2026-03")

        val march = repository.loadBudgets("2026-03")
        assertEquals(2_500_000L, march.single { it.categoryKey == "groceries" }.limitCents)

        val april = repository.loadBudgets("2026-04")
        assertEquals(2_000_000L, april.single { it.categoryKey == "groceries" }.limitCents)
    }

    @Test
    fun `removing an override falls back to the default`() {
        createV1Database()
        val repository = LedgerRepository(context)
        repository.setBudget("groceries", 2_500_000L, "2026-03")

        repository.deleteBudget("groceries", "2026-03")

        val march = repository.loadBudgets("2026-03")
        assertEquals(2_000_000L, march.single { it.categoryKey == "groceries" }.limitCents)
        assertNotNull(repository.loadBudgetRows().firstOrNull { !it.isOverride })
    }

    @Test
    fun `restoring a backup replaces the ledger`() {
        createV1Database()
        val repository = LedgerRepository(context)
        val original = repository.backup()
        assertEquals(1, original.transactions.size)

        repository.clearAllData()
        assertEquals(0, repository.loadTransactions().size)

        repository.restore(original, replace = true)
        assertEquals(1, repository.loadTransactions().size)
        assertEquals(2_000_000L, repository.loadBudgets("2026-03").single().limitCents)
    }

    private companion object {
        const val DB_NAME = "ledgerly.db"
    }
}