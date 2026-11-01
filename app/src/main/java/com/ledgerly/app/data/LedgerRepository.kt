package com.ledgerly.app.data

import android.content.Context
import com.ledgerly.app.util.Backup
import java.time.LocalDate

class LedgerRepository(context: Context) {

    private val appContext = context.applicationContext
    private val db = LedgerDatabase(appContext)
    private val prefs = appContext.getSharedPreferences("ledgerly_prefs", Context.MODE_PRIVATE)

    fun loadTransactions(): List<Transaction> = db.readTransactions()

    /** Every budget row, including per-month overrides. Used by backups. */
    fun loadBudgetRows(): List<Budget> = db.readBudgetRows()

    /**
     * Limits that apply to [month] (`2026-10`): a month-specific row wins over the default row.
     */
    fun loadBudgets(month: String): List<Budget> {
        val rows = db.readBudgetRows()
        val defaults = rows.filter { !it.isOverride }.associateBy { it.categoryKey }
        val overrides = rows.filter { it.month == month }.associateBy { it.categoryKey }
        val merged = defaults.toMutableMap()
        merged.putAll(overrides)
        return merged.values.sortedByDescending { it.limitCents }
    }

    fun addTransaction(tx: Transaction): Long = db.insertTransaction(tx)

    fun updateTransaction(tx: Transaction) = db.updateTransaction(tx)

    fun deleteTransaction(id: Long) = db.deleteTransaction(id)

    /** `month` empty means the limit applies to every month. */
    fun setBudget(categoryKey: String, limitCents: Long, month: String = "") {
        if (limitCents <= 0L) db.deleteBudget(categoryKey, month)
        else db.upsertBudget(Budget(categoryKey, limitCents, month))
    }

    fun deleteBudget(categoryKey: String, month: String = "") = db.deleteBudget(categoryKey, month)

    fun loadRecurring(): List<Recurring> = db.readRecurring()

    fun addRecurring(item: Recurring): Long = db.insertRecurring(item)

    fun updateRecurring(item: Recurring) = db.updateRecurring(item)

    fun deleteRecurring(id: Long) = db.deleteRecurring(id)

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
            onboarded = prefs.getBoolean("onboarded", false),
            appLock = prefs.getBoolean("app_lock", false),
            checkUpdates = prefs.getBoolean("check_updates", true)
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
            .putBoolean("app_lock", settings.appLock)
            .putBoolean("check_updates", settings.checkUpdates)
            .apply()
    }

    fun backup(): Backup = Backup(
        transactions = db.readTransactions(),
        budgets = db.readBudgetRows(),
        recurring = db.readRecurring(),
        settings = loadSettings()
    )

    /** `replace = false` keeps what's on the device and only adds what's missing. */
    fun restore(backup: Backup, replace: Boolean = true) {
        if (replace) {
            db.clearTransactions()
            db.clearBudgets()
            db.clearRecurring()
        }
        db.insertTransactions(backup.transactions)
        db.insertBudgets(backup.budgets)
        backup.recurring.forEach { db.insertRecurring(it) }
        saveSettings(backup.settings.copy(onboarded = true))
    }

    fun importTransactions(transactions: List<Transaction>): Int {
        db.insertTransactions(transactions)
        return transactions.size
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
        db.clearRecurring()
    }

    private companion object {
        /** New installs start in rupiah; existing installs keep whatever is stored. */
        const val DEFAULT_CURRENCY = "IDR"
    }
}