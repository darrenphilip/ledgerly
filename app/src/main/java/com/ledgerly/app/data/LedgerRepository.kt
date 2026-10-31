package com.ledgerly.app.data

import android.content.Context
import java.time.LocalDate

class LedgerRepository(context: Context) {

    private val appContext = context.applicationContext
    private val db = LedgerDatabase(appContext)
    private val prefs = appContext.getSharedPreferences("ledgerly_prefs", Context.MODE_PRIVATE)

    fun loadTransactions(): List<Transaction> = db.readTransactions()

    fun loadBudgets(): List<Budget> = db.readBudgets()

    fun addTransaction(tx: Transaction): Long = db.insertTransaction(tx)

    fun updateTransaction(tx: Transaction) = db.updateTransaction(tx)

    fun deleteTransaction(id: Long) = db.deleteTransaction(id)

    fun setBudget(categoryKey: String, limitCents: Long) {
        if (limitCents <= 0L) db.deleteBudget(categoryKey)
        else db.upsertBudget(Budget(categoryKey, limitCents))
    }

    fun deleteBudget(categoryKey: String) = db.deleteBudget(categoryKey)

    fun loadSettings(): AppSettings {
        val theme = runCatching {
            ThemeMode.valueOf(prefs.getString("theme", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)
        }.getOrDefault(ThemeMode.SYSTEM)
        return AppSettings(
            name = prefs.getString("name", "") ?: "",
            currency = prefs.getString("currency", DEFAULT_CURRENCY) ?: DEFAULT_CURRENCY,
            monthlyBudgetCents = prefs.getLong("monthly_budget_cents", 0L),
            budgetStyle = BudgetStyle.from(prefs.getString("budget_style", null)),
            monthlyIncomeCents = prefs.getLong("monthly_income_cents", 0L),
            theme = theme,
            onboarded = prefs.getBoolean("onboarded", false)
        )
    }

    fun saveSettings(settings: AppSettings) {
        prefs.edit()
            .putString("name", settings.name)
            .putString("currency", settings.currency)
            .putLong("monthly_budget_cents", settings.monthlyBudgetCents)
            .putString("budget_style", settings.budgetStyle.name)
            .putLong("monthly_income_cents", settings.monthlyIncomeCents)
            .putString("theme", settings.theme.name)
            .putBoolean("onboarded", settings.onboarded)
            .apply()
    }

    fun seedSampleData(force: Boolean = false, today: LocalDate = LocalDate.now()) {
        if (!SampleData.available) return
        if (!force && db.readTransactions().isNotEmpty()) return
        if (force) {
            db.clearTransactions()
            db.clearBudgets()
        }
        val currency = loadSettings().currency
        val scale = SampleData.scaleFor(currency)
        db.insertTransactions(SampleData.transactions(today, scale))
        db.insertBudgets(SampleData.budgets(scale))
        val settings = loadSettings()
        if (settings.monthlyBudgetCents <= 0L) {
            saveSettings(
                settings.copy(monthlyBudgetCents = SampleData.defaultMonthlyBudgetCents(currency))
            )
        }
    }

    fun clearAllData() {
        db.clearTransactions()
        db.clearBudgets()
    }

    private companion object {
        /** New installs start in rupiah; existing installs keep whatever is stored. */
        const val DEFAULT_CURRENCY = "IDR"
    }
}
