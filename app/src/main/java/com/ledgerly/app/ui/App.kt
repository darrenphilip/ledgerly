package com.ledgerly.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ledgerly.app.data.AppSettings
import com.ledgerly.app.data.Categories
import com.ledgerly.app.data.Transaction
import com.ledgerly.app.ui.screens.ActivityScreen
import com.ledgerly.app.ui.screens.BudgetsScreen
import com.ledgerly.app.ui.screens.HomeScreen
import com.ledgerly.app.ui.screens.InsightsScreen
import com.ledgerly.app.ui.screens.OnboardingScreen
import com.ledgerly.app.ui.screens.SettingsScreen
import com.ledgerly.app.ui.sheets.BudgetAmountDialog
import com.ledgerly.app.ui.sheets.BudgetSheet
import com.ledgerly.app.ui.sheets.TransactionSheet
import com.ledgerly.app.ui.theme.LedgerlyTheme
import com.ledgerly.app.util.BudgetMath
import kotlinx.coroutines.launch
import java.time.YearMonth

private data class TabItem(
    val label: String,
    val icon: ImageVector
)

private val tabs = listOf(
    TabItem("Home", Icons.Rounded.Home),
    TabItem("Activity", Icons.AutoMirrored.Rounded.ReceiptLong),
    TabItem("Budgets", Icons.Rounded.AccountBalanceWallet),
    TabItem("Insights", Icons.Rounded.Insights)
)

@Composable
fun LedgerlyApp(viewModel: AppViewModel = viewModel()) {
    val transactions by viewModel.transactions.collectAsState()
    val budgets by viewModel.budgets.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val month by viewModel.month.collectAsState()
    val loading by viewModel.loading.collectAsState()

    LedgerlyTheme(themeMode = settings.theme) {
        when {
            loading && !settings.onboarded -> OnboardingScreen(
                defaultCurrency = settings.currency,
                onComplete = viewModel::completeOnboarding
            )

            loading -> LoadingScreen()

            !settings.onboarded -> OnboardingScreen(
                defaultCurrency = settings.currency,
                onComplete = viewModel::completeOnboarding
            )

            else -> MainScaffold(
                viewModel = viewModel,
                transactions = transactions,
                budgets = budgets,
                settings = settings,
                month = month
            )
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun MainScaffold(
    viewModel: AppViewModel,
    transactions: List<Transaction>,
    budgets: Map<String, Long>,
    settings: AppSettings,
    month: YearMonth
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showEditor by remember { mutableStateOf(false) }
    var editorTransaction by remember { mutableStateOf<Transaction?>(null) }
    var showBudgetSheet by remember { mutableStateOf(false) }
    var budgetSheetCategory by remember { mutableStateOf<String?>(null) }
    var showMonthlyBudgetDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val currency = settings.currency
    val stats = remember(transactions, month) { BudgetMath.summarize(transactions, month) }
    val now = remember { YearMonth.now() }
    val isCurrentMonth = month == now
    val canGoNext = month < now

    fun openEditor(transaction: Transaction?) {
        editorTransaction = transaction
        showEditor = true
    }

    fun deleteWithUndo(transaction: Transaction) {
        viewModel.deleteTransaction(transaction)
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "Transaction deleted",
                actionLabel = "Undo",
                withDismissAction = true,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.restoreTransaction(transaction)
            }
        }
    }

    BackHandler(enabled = showSettings) { showSettings = false }
    BackHandler(enabled = !showSettings && selectedTab != 0) { selectedTab = 0 }

    if (showSettings) {
        SettingsScreen(
            settings = settings,
            transactions = transactions,
            onBack = { showSettings = false },
            onNameChange = viewModel::setName,
            onCurrencyChange = viewModel::setCurrency,
            onMonthlyBudgetChange = viewModel::setMonthlyBudget,
            onThemeChange = viewModel::setTheme,
            onLoadSampleData = viewModel::resetWithSampleData,
            onClearAllData = viewModel::clearAllData
        )
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { openEditor(null) },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    Icons.Rounded.Add,
                    contentDescription = "Add transaction",
                    modifier = Modifier.size(26.dp)
                )
            }
        },
        bottomBar = {
            Column {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    tabs.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.label,
                                    modifier = Modifier.size(23.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = tab.label,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        Crossfade(
            targetState = selectedTab,
            animationSpec = tween(220),
            label = "tab",
            modifier = Modifier.fillMaxSize().padding(padding)
        ) { tab ->
            when (tab) {
                0 -> HomeScreen(
                    transactions = transactions,
                    month = month,
                    stats = stats,
                    budgets = budgets,
                    monthlyBudgetCents = settings.monthlyBudgetCents,
                    currency = currency,
                    name = settings.name,
                    isCurrentMonth = isCurrentMonth,
                    canGoNext = canGoNext,
                    onPreviousMonth = { viewModel.selectMonth(month.minusMonths(1)) },
                    onNextMonth = { viewModel.selectMonth(month.plusMonths(1)) },
                    onSettings = { showSettings = true },
                    onSeeAll = { selectedTab = 1 },
                    onAddTransaction = { openEditor(null) },
                    onLoadSample = viewModel::loadSampleDataIfEmpty,
                    onOpenBudgets = { selectedTab = 2 },
                    onOpenInsights = { selectedTab = 3 },
                    onSetBudget = { showMonthlyBudgetDialog = true },
                    onTransactionClick = { openEditor(it) }
                )

                1 -> ActivityScreen(
                    transactions = transactions,
                    month = month,
                    currency = currency,
                    canGoNext = canGoNext,
                    onPreviousMonth = { viewModel.selectMonth(month.minusMonths(1)) },
                    onNextMonth = { viewModel.selectMonth(month.plusMonths(1)) },
                    onAddTransaction = { openEditor(null) },
                    onTransactionClick = { openEditor(it) },
                    onDeleteTransaction = { deleteWithUndo(it) }
                )

                2 -> BudgetsScreen(
                    stats = stats,
                    budgets = budgets,
                    monthlyBudgetCents = settings.monthlyBudgetCents,
                    currency = currency,
                    month = month,
                    canGoNext = canGoNext,
                    onPreviousMonth = { viewModel.selectMonth(month.minusMonths(1)) },
                    onNextMonth = { viewModel.selectMonth(month.plusMonths(1)) },
                    onAddBudget = {
                        budgetSheetCategory = null
                        showBudgetSheet = true
                    },
                    onEditBudget = { key ->
                        budgetSheetCategory = key
                        showBudgetSheet = true
                    },
                    onSetMonthlyBudget = { showMonthlyBudgetDialog = true }
                )

                3 -> InsightsScreen(
                    transactions = transactions,
                    month = month,
                    stats = stats,
                    budgets = budgets,
                    monthlyBudgetCents = settings.monthlyBudgetCents,
                    currency = currency,
                    canGoNext = canGoNext,
                    onPreviousMonth = { viewModel.selectMonth(month.minusMonths(1)) },
                    onNextMonth = { viewModel.selectMonth(month.plusMonths(1)) },
                    onAddTransaction = { openEditor(null) }
                )
            }
        }
    }

    if (showEditor) {
        TransactionSheet(
            editing = editorTransaction,
            currency = currency,
            onDismiss = { showEditor = false },
            onSave = { transaction ->
                if (editorTransaction != null) {
                    viewModel.updateTransaction(transaction)
                } else {
                    viewModel.addTransaction(transaction)
                }
                showEditor = false
            },
            onDelete = { transaction ->
                deleteWithUndo(transaction)
                showEditor = false
            }
        )
    }

    if (showBudgetSheet) {
        val editingKey = budgetSheetCategory
        BudgetSheet(
            existingCategoryKey = editingKey,
            existingLimitCents = editingKey?.let { budgets[it] },
            availableCategories = if (editingKey == null) {
                Categories.expense.filter { it.key !in budgets }
            } else {
                emptyList()
            },
            currency = currency,
            onDismiss = { showBudgetSheet = false },
            onSave = { key, cents ->
                viewModel.setBudget(key, cents)
                showBudgetSheet = false
            },
            onDelete = { key ->
                viewModel.deleteBudget(key)
                showBudgetSheet = false
            }
        )
    }

    if (showMonthlyBudgetDialog) {
        BudgetAmountDialog(
            title = "Monthly budget",
            description = "Set the total you want to spend each month. Ledgerly uses it to calculate what's safe to spend per day.",
            initialCents = settings.monthlyBudgetCents,
            currency = currency,
            onDismiss = { showMonthlyBudgetDialog = false },
            onSave = { cents ->
                viewModel.setMonthlyBudget(cents)
                showMonthlyBudgetDialog = false
            }
        )
    }
}
