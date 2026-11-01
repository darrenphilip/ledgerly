package com.ledgerly.app.data

import android.database.sqlite.SQLiteDatabase

/**
 * Forward-only schema migrations. Every upgrade step runs inside the transaction
 * `SQLiteOpenHelper` opens, and no step may drop user data.
 */
object Migrations {

    const val VERSION = 2

    fun create(db: SQLiteDatabase) {
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
        createBudgets(db)
        createRecurring(db)
    }

    fun upgrade(db: SQLiteDatabase, from: Int) {
        var version = from
        while (version < VERSION) {
            when (version) {
                1 -> upgradeTo2(db)
            }
            version++
        }
    }

    private fun createBudgets(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS budgets (
                month TEXT NOT NULL DEFAULT '',
                category TEXT NOT NULL,
                limit_cents INTEGER NOT NULL,
                PRIMARY KEY(month, category)
            )
            """.trimIndent()
        )
    }

    private fun createRecurring(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS recurring (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                type TEXT NOT NULL,
                amount_cents INTEGER NOT NULL,
                category TEXT NOT NULL,
                note TEXT NOT NULL DEFAULT '',
                cadence TEXT NOT NULL,
                next_due_epoch_day INTEGER NOT NULL,
                active INTEGER NOT NULL DEFAULT 1
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_recurring_due ON recurring(next_due_epoch_day)")
    }

    /** v1 stored one limit per category; v2 adds an optional per-month override. */
    private fun upgradeTo2(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE budgets_v2 (
                month TEXT NOT NULL DEFAULT '',
                category TEXT NOT NULL,
                limit_cents INTEGER NOT NULL,
                PRIMARY KEY(month, category)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO budgets_v2 (month, category, limit_cents)
            SELECT '', category, limit_cents FROM budgets
            """.trimIndent()
        )
        db.execSQL("DROP TABLE budgets")
        db.execSQL("ALTER TABLE budgets_v2 RENAME TO budgets")
        createRecurring(db)
    }
}