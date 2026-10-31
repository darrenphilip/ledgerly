package com.ledgerly.app.data

import androidx.compose.ui.graphics.Color
import java.time.LocalDate

enum class TxType {
    EXPENSE,
    INCOME;

    val raw: String
        get() = if (this == INCOME) "income" else "expense"

    companion object {
        fun from(raw: String?): TxType = if (raw == "income") INCOME else EXPENSE
    }
}

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark")
}

data class Transaction(
    val id: Long = 0L,
    val type: TxType,
    val amountCents: Long,
    val categoryKey: String,
    val note: String,
    val date: LocalDate,
    val createdAt: Long = System.currentTimeMillis()
)

data class Budget(
    val categoryKey: String,
    val limitCents: Long
)

data class AppSettings(
    val name: String = "",
    val currency: String = "USD",
    val monthlyBudgetCents: Long = 0L,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val onboarded: Boolean = false
)

data class Category(
    val key: String,
    val label: String,
    val emoji: String,
    val color: Color
)

object Categories {
    val expense: List<Category> = listOf(
        Category("food", "Food & Drink", "🍔", Color(0xFFF97316)),
        Category("groceries", "Groceries", "🛒", Color(0xFF22C55E)),
        Category("transport", "Transport", "🚌", Color(0xFF3B82F6)),
        Category("shopping", "Shopping", "🛍️", Color(0xFFA855F7)),
        Category("bills", "Bills & Utilities", "🧾", Color(0xFFEAB308)),
        Category("entertainment", "Entertainment", "🎬", Color(0xFFEC4899)),
        Category("health", "Health", "💊", Color(0xFF14B8A6)),
        Category("travel", "Travel", "✈️", Color(0xFF06B6D4)),
        Category("home", "Home", "🏠", Color(0xFF64748B)),
        Category("education", "Education", "📚", Color(0xFF6366F1)),
        Category("subscriptions", "Subscriptions", "🔁", Color(0xFF8B5CF6)),
        Category("other", "Other", "✨", Color(0xFF94A3B8))
    )

    val income: List<Category> = listOf(
        Category("salary", "Salary", "💼", Color(0xFF16A34A)),
        Category("freelance", "Freelance", "🧑‍💻", Color(0xFF0D9488)),
        Category("gifts", "Gifts", "🎁", Color(0xFFF59E0B)),
        Category("other_income", "Other income", "💰", Color(0xFF10B981))
    )

    val all: List<Category> = expense + income

    private val byKey: Map<String, Category> = all.associateBy { it.key }

    val fallbackExpense: Category = expense.last()
    val fallbackIncome: Category = income.last()

    fun of(key: String): Category = byKey[key] ?: fallbackExpense

    fun forType(type: TxType): List<Category> = if (type == TxType.INCOME) income else expense

    fun defaultForType(type: TxType): Category = forType(type).first()
}
