package com.ledgerly.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.time.LocalDate

class LedgerDatabase(context: Context) :
    SQLiteOpenHelper(context.applicationContext, NAME, null, Migrations.VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        Migrations.create(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Migrations.upgrade(db, oldVersion)
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
                    date = LocalDate.ofEpochDay(cursor.getLong(dateIdx)),
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

    /** Every budget row, including per-month overrides. */
    fun readBudgetRows(): List<Budget> {
        val list = mutableListOf<Budget>()
        readableDatabase.query("budgets", null, null, null, null, null, "month DESC, limit_cents DESC")
            .use { cursor ->
                val monthIdx = cursor.getColumnIndexOrThrow("month")
                val categoryIdx = cursor.getColumnIndexOrThrow("category")
                val limitIdx = cursor.getColumnIndexOrThrow("limit_cents")
                while (cursor.moveToNext()) {
                    list += Budget(
                        categoryKey = cursor.getString(categoryIdx),
                        limitCents = cursor.getLong(limitIdx),
                        month = cursor.getString(monthIdx) ?: ""
                    )
                }
            }
        return list
    }

    fun upsertBudget(budget: Budget) {
        val values = ContentValues().apply {
            put("month", budget.month)
            put("category", budget.categoryKey)
            put("limit_cents", budget.limitCents)
        }
        writableDatabase.insertWithOnConflict(
            "budgets", null, values, SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun deleteBudget(categoryKey: String, month: String = "") {
        writableDatabase.delete(
            "budgets", "category = ? AND month = ?", arrayOf(categoryKey, month)
        )
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

    fun readRecurring(): List<Recurring> {
        val list = mutableListOf<Recurring>()
        readableDatabase.query("recurring", null, null, null, null, null, "next_due_epoch_day ASC")
            .use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow("id")
                val typeIdx = cursor.getColumnIndexOrThrow("type")
                val amountIdx = cursor.getColumnIndexOrThrow("amount_cents")
                val categoryIdx = cursor.getColumnIndexOrThrow("category")
                val noteIdx = cursor.getColumnIndexOrThrow("note")
                val cadenceIdx = cursor.getColumnIndexOrThrow("cadence")
                val dueIdx = cursor.getColumnIndexOrThrow("next_due_epoch_day")
                val activeIdx = cursor.getColumnIndexOrThrow("active")
                while (cursor.moveToNext()) {
                    list += Recurring(
                        id = cursor.getLong(idIdx),
                        type = TxType.from(cursor.getString(typeIdx)),
                        amountCents = cursor.getLong(amountIdx),
                        categoryKey = cursor.getString(categoryIdx),
                        note = cursor.getString(noteIdx) ?: "",
                        cadence = Cadence.from(cursor.getString(cadenceIdx)),
                        nextDue = LocalDate.ofEpochDay(cursor.getLong(dueIdx)),
                        active = cursor.getInt(activeIdx) != 0
                    )
                }
            }
        return list
    }

    fun insertRecurring(item: Recurring): Long {
        val values = ContentValues().apply {
            put("type", item.type.raw)
            put("amount_cents", item.amountCents)
            put("category", item.categoryKey)
            put("note", item.note)
            put("cadence", item.cadence.name)
            put("next_due_epoch_day", item.nextDue.toEpochDay())
            put("active", if (item.active) 1 else 0)
        }
        return writableDatabase.insert("recurring", null, values)
    }

    fun updateRecurring(item: Recurring) {
        val values = ContentValues().apply {
            put("type", item.type.raw)
            put("amount_cents", item.amountCents)
            put("category", item.categoryKey)
            put("note", item.note)
            put("cadence", item.cadence.name)
            put("next_due_epoch_day", item.nextDue.toEpochDay())
            put("active", if (item.active) 1 else 0)
        }
        writableDatabase.update("recurring", values, "id = ?", arrayOf(item.id.toString()))
    }

    fun deleteRecurring(id: Long) {
        writableDatabase.delete("recurring", "id = ?", arrayOf(id.toString()))
    }

    fun clearRecurring() {
        writableDatabase.delete("recurring", null, null)
    }

    fun clearTransactions() {
        writableDatabase.delete("transactions", null, null)
    }

    fun clearBudgets() {
        writableDatabase.delete("budgets", null, null)
    }

    companion object {
        private const val NAME = "ledgerly.db"
    }
}