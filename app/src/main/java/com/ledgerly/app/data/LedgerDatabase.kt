package com.ledgerly.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class LedgerDatabase(context: Context) :
    SQLiteOpenHelper(context.applicationContext, NAME, null, VERSION) {

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

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS transactions")
        db.execSQL("DROP TABLE IF EXISTS budgets")
        onCreate(db)
    }

    fun readTransactions(): List<Transaction> {
        val list = mutableListOf<Transaction>()
        readableDatabase.query(
            "transactions", null, null, null, null, null,
            "date_epoch_day DESC, created_at DESC"
        ).use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow("id")
            val typeIdx = cursor.getColumnIndexOrThrow("type")
            val amountIdx = cursor.getColumnIndexOrThrow("amount_cents")
            val categoryIdx = cursor.getColumnIndexOrThrow("category")
            val noteIdx = cursor.getColumnIndexOrThrow("note")
            val dateIdx = cursor.getColumnIndexOrThrow("date_epoch_day")
            val createdIdx = cursor.getColumnIndexOrThrow("created_at")
            while (cursor.moveToNext()) {
                list += Transaction(
                    id = cursor.getLong(idIdx),
                    type = TxType.from(cursor.getString(typeIdx)),
                    amountCents = cursor.getLong(amountIdx),
                    categoryKey = cursor.getString(categoryIdx),
                    note = cursor.getString(noteIdx) ?: "",
                    date = java.time.LocalDate.ofEpochDay(cursor.getLong(dateIdx)),
                    createdAt = cursor.getLong(createdIdx)
                )
            }
        }
        return list
    }

    fun insertTransaction(tx: Transaction): Long {
        val values = ContentValues().apply {
            put("type", tx.type.raw)
            put("amount_cents", tx.amountCents)
            put("category", tx.categoryKey)
            put("note", tx.note)
            put("date_epoch_day", tx.date.toEpochDay())
            put("created_at", tx.createdAt)
        }
        return writableDatabase.insert("transactions", null, values)
    }

    fun insertTransactions(txs: List<Transaction>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            txs.forEach { insertTransaction(it) }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun updateTransaction(tx: Transaction) {
        val values = ContentValues().apply {
            put("type", tx.type.raw)
            put("amount_cents", tx.amountCents)
            put("category", tx.categoryKey)
            put("note", tx.note)
            put("date_epoch_day", tx.date.toEpochDay())
            put("created_at", tx.createdAt)
        }
        writableDatabase.update("transactions", values, "id = ?", arrayOf(tx.id.toString()))
    }

    fun deleteTransaction(id: Long) {
        writableDatabase.delete("transactions", "id = ?", arrayOf(id.toString()))
    }

    fun readBudgets(): List<Budget> {
        val list = mutableListOf<Budget>()
        readableDatabase.query("budgets", null, null, null, null, null, "limit_cents DESC")
            .use { cursor ->
                val categoryIdx = cursor.getColumnIndexOrThrow("category")
                val limitIdx = cursor.getColumnIndexOrThrow("limit_cents")
                while (cursor.moveToNext()) {
                    list += Budget(cursor.getString(categoryIdx), cursor.getLong(limitIdx))
                }
            }
        return list
    }

    fun upsertBudget(budget: Budget) {
        val values = ContentValues().apply {
            put("category", budget.categoryKey)
            put("limit_cents", budget.limitCents)
        }
        writableDatabase.insertWithOnConflict(
            "budgets", null, values, SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun deleteBudget(categoryKey: String) {
        writableDatabase.delete("budgets", "category = ?", arrayOf(categoryKey))
    }

    fun insertBudgets(budgets: List<Budget>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            budgets.forEach { upsertBudget(it) }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun clearTransactions() {
        writableDatabase.delete("transactions", null, null)
    }

    fun clearBudgets() {
        writableDatabase.delete("budgets", null, null)
    }

    companion object {
        private const val NAME = "ledgerly.db"
        private const val VERSION = 1
    }
}
