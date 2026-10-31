package com.ledgerly.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ledgerly.app.data.AppSettings
import com.ledgerly.app.data.Budget
import com.ledgerly.app.data.LedgerRepository
import com.ledgerly.app.data.ThemeMode
import com.ledgerly.app.data.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.YearMonth

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LedgerRepository(application)

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    private val _budgets = MutableStateFlow<Map<String, Long>>(emptyMap())
    val budgets: StateFlow<Map<String, Long>> = _budgets.asStateFlow()

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
        val (txs, budgetList, storedSettings) = withContext(Dispatchers.IO) {
            Triple(
                repository.loadTransactions(),
                repository.loadBudgets(),
                repository.loadSettings()
            )
        }
        _transactions.value = txs
        _budgets.value = budgetList.associate { it.categoryKey to it.limitCents }
        _settings.value = storedSettings
        _loading.value = false
    }

    fun selectMonth(month: YearMonth) {
        val clamped = if (month.isAfter(YearMonth.now())) YearMonth.now() else month
        _month.value = clamped
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

    fun setBudget(categoryKey: String, limitCents: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.setBudget(categoryKey, limitCents) }
            reload()
        }
    }

    fun deleteBudget(categoryKey: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.deleteBudget(categoryKey) }
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

    fun setName(name: String) = saveSettings(_settings.value.copy(name = name.trim()))

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

    fun allBudgets(): List<Budget> = _budgets.value.map { Budget(it.key, it.value) }
}
