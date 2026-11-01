package com.ledgerly.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ledgerly.app.data.AppSettings
import com.ledgerly.app.data.Budget
import com.ledgerly.app.data.BudgetStyle
import com.ledgerly.app.data.LedgerRepository
import com.ledgerly.app.data.Recurring
import com.ledgerly.app.data.ThemeMode
import com.ledgerly.app.data.Transaction
import com.ledgerly.app.util.Backup
import com.ledgerly.app.util.BackupCodec
import com.ledgerly.app.util.CsvImporter
import com.ledgerly.app.util.RecurringMath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LedgerRepository(application)

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    private var budgetRows: List<Budget> = emptyList()

    private val _budgets = MutableStateFlow<Map<String, Long>>(emptyMap())
    val budgets: StateFlow<Map<String, Long>> = _budgets.asStateFlow()

    /** Categories whose limit is overridden for the selected month. */
    private val _monthOverrides = MutableStateFlow<Set<String>>(emptySet())
    val monthOverrides: StateFlow<Set<String>> = _monthOverrides.asStateFlow()

    private val _recurring = MutableStateFlow<List<Recurring>>(emptyList())
    val recurring: StateFlow<List<Recurring>> = _recurring.asStateFlow()

    private val _dueRecurring = MutableStateFlow<List<Recurring>>(emptyList())
    val dueRecurring: StateFlow<List<Recurring>> = _dueRecurring.asStateFlow()

    private val _settings = MutableStateFlow(repository.loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _month = MutableStateFlow(YearMonth.now())
    val month: StateFlow<YearMonth> = _month.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    init {
        viewModelScope.launch { reload() }
    }

    private suspend fun reload() {
        val loaded = withContext(Dispatchers.IO) {
            Loaded(
                transactions = repository.loadTransactions(),
                budgetRows = repository.loadBudgetRows(),
                recurring = repository.loadRecurring(),
                settings = repository.loadSettings()
            )
        }
        _transactions.value = loaded.transactions
        budgetRows = loaded.budgetRows
        _recurring.value = loaded.recurring
        _dueRecurring.value = RecurringMath.due(loaded.recurring)
        _settings.value = loaded.settings
        applyBudgetsForSelectedMonth()
        _loading.value = false
    }

    private data class Loaded(
        val transactions: List<Transaction>,
        val budgetRows: List<Budget>,
        val recurring: List<Recurring>,
        val settings: AppSettings
    )

    private fun applyBudgetsForSelectedMonth() {
        val key = _month.value.monthKey()
        val defaults = budgetRows.filter { !it.isOverride }
        val overrides = budgetRows.filter { it.month == key }
        val effective = defaults.associate { it.categoryKey to it.limitCents }
            .toMutableMap()
        overrides.forEach { effective[it.categoryKey] = it.limitCents }
        _budgets.value = effective
        _monthOverrides.value = overrides.map { it.categoryKey }.toSet()
    }

    fun selectMonth(month: YearMonth) {
        val clamped = if (month.isAfter(YearMonth.now())) YearMonth.now() else month
        _month.value = clamped
        applyBudgetsForSelectedMonth()
    }

    fun previousMonth() = selectMonth(_month.value.minusMonths(1))

    fun nextMonth() = selectMonth(_month.value.plusMonths(1))

    fun addTransaction(tx: Transaction) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.addTransaction(tx) }
            reload()
        }
    }

    fun updateTransaction(tx: Transaction) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.updateTransaction(tx) }
            reload()
        }
    }

    fun deleteTransaction(tx: Transaction) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.deleteTransaction(tx.id) }
            reload()
        }
    }

    fun restoreTransaction(tx: Transaction) {
        addTransaction(tx.copy(id = 0L))
    }

    /** `forSelectedMonth = true` writes an override for the visible month instead of the default. */
    fun setBudget(categoryKey: String, limitCents: Long, forSelectedMonth: Boolean = false) {
        val month = if (forSelectedMonth) _month.value.monthKey() else ""
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.setBudget(categoryKey, limitCents, month) }
            reload()
        }
    }

    fun deleteBudget(categoryKey: String, forSelectedMonth: Boolean = false) {
        val month = if (forSelectedMonth) _month.value.monthKey() else ""
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.deleteBudget(categoryKey, month) }
            reload()
        }
    }

    fun saveSettings(settings: AppSettings) {
        _settings.value = settings
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.saveSettings(settings) }
        }
    }

    fun setTheme(mode: ThemeMode) = saveSettings(_settings.value.copy(theme = mode))

    fun setMonthlyBudget(cents: Long) =
        saveSettings(_settings.value.copy(monthlyBudgetCents = cents))

    fun setCurrency(code: String) = saveSettings(_settings.value.copy(currency = code))

    fun setBudgetStyle(style: BudgetStyle) =
        saveSettings(_settings.value.copy(budgetStyle = style))

    fun setMonthlyIncome(cents: Long) =
        saveSettings(_settings.value.copy(monthlyIncomeCents = cents))

    fun setName(name: String) = saveSettings(_settings.value.copy(name = name.trim()))

    fun setAppLock(enabled: Boolean) = saveSettings(_settings.value.copy(appLock = enabled))

    fun addRecurring(item: Recurring) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.addRecurring(item) }
            reload()
        }
    }

    fun updateRecurring(item: Recurring) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.updateRecurring(item) }
            reload()
        }
    }

    fun deleteRecurring(item: Recurring) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.deleteRecurring(item.id) }
            reload()
        }
    }

    /** Logs the entry as a transaction today and moves its next due date forward. */
    fun logRecurring(item: Recurring, today: LocalDate = LocalDate.now()) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.addTransaction(RecurringMath.transactionFor(item, today))
                repository.updateRecurring(
                    item.copy(nextDue = RecurringMath.nextAfter(item, today))
                )
            }
            reload()
        }
    }

    fun snoozeRecurring(item: Recurring, today: LocalDate = LocalDate.now()) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.updateRecurring(
                    item.copy(nextDue = RecurringMath.nextAfter(item, today))
                )
            }
            reload()
        }
    }

    suspend fun buildBackupJson(): String = withContext(Dispatchers.IO) {
        BackupCodec.encode(repository.backup())
    }

    /** Returns how much was restored, or throws when the file isn't a valid backup. */
    suspend fun restoreBackup(json: String, replace: Boolean): Backup = withContext(Dispatchers.IO) {
        val backup = BackupCodec.decode(json)
        repository.restore(backup, replace)
        reload()
        backup
    }

    suspend fun importTransactions(transactions: List<Transaction>): Int =
        withContext(Dispatchers.IO) {
            val count = repository.importTransactions(transactions)
            reload()
            count
        }

    /** Parses a CSV export, adds the new rows and returns a one-line report. */
    suspend fun importCsv(text: String): String {
        val plan = CsvImporter.parse(text, _settings.value.currency, _transactions.value)
        if (plan.isEmpty) return "Nothing to import \u2014 no recognisable rows found"
        if (plan.transactions.isNotEmpty()) repository.importTransactions(plan.transactions)
        reload()
        return buildString {
            val count = plan.transactions.size
            append("Imported $count transaction${if (count == 1) "" else "s"}")
            if (plan.duplicates > 0) {
                append(", skipped ${plan.duplicates} duplicate${if (plan.duplicates == 1) "" else "s"}")
            }
            if (plan.skippedLines.isNotEmpty()) {
                append(", ignored ${plan.skippedLines.size} unreadable line${if (plan.skippedLines.size == 1) "" else "s"}")
            }
        }
    }

    /** Restores a backup file and returns a one-line report. */
    suspend fun restoreBackupReport(json: String, replace: Boolean): String {
        val backup = restoreBackup(json, replace)
        return buildString {
            append(if (replace) "Restored " else "Merged ")
            append("${backup.transactions.size} transaction${if (backup.transactions.size == 1) "" else "s"}")
            if (backup.budgets.isNotEmpty()) {
                append(", ${backup.budgets.size} budget${if (backup.budgets.size == 1) "" else "s"}")
            }
            if (backup.recurring.isNotEmpty()) {
                append(", ${backup.recurring.size} recurring entr${if (backup.recurring.size == 1) "y" else "ies"}")
            }
        }
    }

    fun completeOnboarding(
        name: String,
        currency: String,
        monthlyBudgetCents: Long,
        withSampleData: Boolean
    ) {
        val updated = _settings.value.copy(
            name = name.trim(),
            currency = currency,
            monthlyBudgetCents = monthlyBudgetCents,
            onboarded = true
        )
        _loading.value = true
        _settings.value = updated
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.saveSettings(updated)
                if (withSampleData) repository.seedSampleData()
            }
            reload()
        }
    }

    /** Loads demo data only when the ledger is empty. */
    fun loadSampleDataIfEmpty() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.seedSampleData(force = false) }
            reload()
        }
    }

    /** Wipes the ledger and fills it with a fresh demo dataset. */
    fun resetWithSampleData() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.seedSampleData(force = true) }
            reload()
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.clearAllData() }
            reload()
        }
    }
}

/** `2026-10`, the format budgets store their month override under. */
fun YearMonth.monthKey(): String = "%04d-%02d".format(year, monthValue)