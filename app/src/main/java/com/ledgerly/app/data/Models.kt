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

enum class BudgetStyle(val label: String) {
    SIMPLE("Simple"),
    RULE_50_30_20("50:30:20");

    companion object {
        fun from(raw: String?): BudgetStyle =
            entries.firstOrNull { it.name == raw } ?: SIMPLE
    }
}

/** The three buckets of the 50:30:20 rule. Shares are percentages of monthly income. */
enum class Bucket(val label: String, val share: Int) {
    NEEDS("Needs", 50),
    WANTS("Wants", 30),
    SAVINGS("Savings", 20)
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
    val limitCents: Long,
    /** ISO month (`2026-10`), or empty for the default that applies to every month. */
    val month: String = ""
) {
    val isOverride: Boolean get() = month.isNotEmpty()
}

data class Recurring(
    val id: Long = 0L,
    val type: TxType = TxType.EXPENSE,
    val amountCents: Long,
    val categoryKey: String,
    val note: String,
    val cadence: Cadence = Cadence.MONTHLY,
    val nextDue: LocalDate,
    val active: Boolean = true
)

enum class Cadence(val label: String) {
    WEEKLY("Every week"),
    MONTHLY("Every month");

    companion object {
        fun from(raw: String?): Cadence = entries.firstOrNull { it.name == raw } ?: MONTHLY
    }
}

data class AppSettings(
    val name: String = "",
    val currency: String = "IDR",
    val monthlyBudgetCents: Long = 0L,
    val budgetStyle: BudgetStyle = BudgetStyle.SIMPLE,
    val monthlyIncomeCents: Long = 0L,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val onboarded: Boolean = false,
    /** Require the device screen lock before showing the ledger. */
    val appLock: Boolean = false,
    /** Ask GitHub for a newer release when the app starts. */
    val checkUpdates: Boolean = true
)

data class Category(
    val key: String,
    val label: String,
    val emoji: String,
    val color: Color,
    /** Which 50:30:20 bucket this category counts toward. Null for income categories. */
    val bucket: Bucket? = null
)

object Categories {
    val expense: List<Category> = listOf(
        Category("food", "Food & Drink", "🍔", Color(0xFFF97316), Bucket.WANTS),
        Category("groceries", "Groceries", "🛒", Color(0xFF22C55E), Bucket.NEEDS),
        Category("transport", "Transport", "🚌", Color(0xFF3B82F6), Bucket.NEEDS),
        Category("shopping", "Shopping", "🛍️", Color(0xFFA855F7), Bucket.WANTS),
        Category("bills", "Bills & Utilities", "🧾", Color(0xFFEAB308), Bucket.NEEDS),
        Category("entertainment", "Entertainment", "🎬", Color(0xFFEC4899), Bucket.WANTS),
        Category("health", "Health", "💊", Color(0xFF14B8A6), Bucket.NEEDS),
        Category("travel", "Travel", "✈️", Color(0xFF06B6D4), Bucket.WANTS),
        Category("home", "Home", "🏠", Color(0xFF64748B), Bucket.NEEDS),
        Category("education", "Education", "📚", Color(0xFF6366F1), Bucket.NEEDS),
        Category("subscriptions", "Subscriptions", "🔁", Color(0xFF8B5CF6), Bucket.WANTS),
        Category("savings", "Savings", "🏦", Color(0xFF0EA5E9), Bucket.SAVINGS),
        Category("other", "Other", "✨", Color(0xFF94A3B8), Bucket.WANTS)
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

    /** Case-insensitive label lookup, used when reading CSV exports back in. */
    fun byLabel(label: String, type: TxType): Category? {
        val wanted = label.trim().lowercase()
        return forType(type).firstOrNull { it.label.lowercase() == wanted }
            ?: all.firstOrNull { it.label.lowercase() == wanted }
    }

    fun forType(type: TxType): List<Category> = if (type == TxType.INCOME) income else expense

    fun defaultForType(type: TxType): Category = forType(type).first()

    /** Bucket for a spending category. Unknown keys fall back to wants, like `other`. */
    fun bucketOf(key: String): Bucket = of(key).bucket ?: Bucket.WANTS
}
