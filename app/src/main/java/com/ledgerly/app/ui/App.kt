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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.os.Build
import com.ledgerly.app.data.Reminders
import com.ledgerly.app.ui.components.NotificationAccess
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ledgerly.app.data.AppSettings
import com.ledgerly.app.data.BudgetStyle
import com.ledgerly.app.data.CaptureStore
import com.ledgerly.app.data.Categories
import com.ledgerly.app.data.Category
import com.ledgerly.app.data.Recurring
import com.ledgerly.app.data.SampleData
import com.ledgerly.app.data.Transaction
import com.ledgerly.app.ui.components.ApkInstaller
import com.ledgerly.app.ui.screens.ActivityScreen
import com.ledgerly.app.ui.screens.BudgetsScreen
import com.ledgerly.app.ui.screens.HomeScreen
import com.ledgerly.app.ui.screens.InsightsScreen
import com.ledgerly.app.ui.screens.OnboardingScreen
import com.ledgerly.app.ui.screens.SettingsScreen
import com.ledgerly.app.ui.sheets.BudgetAmountDialog
import com.ledgerly.app.ui.sheets.BudgetSheet
import com.ledgerly.app.ui.sheets.CategorySheet
import com.ledgerly.app.ui.sheets.RecurringSheet
import com.ledgerly.app.ui.sheets.TransactionSheet
import com.ledgerly.app.ui.theme.LedgerlyTheme
import com.ledgerly.app.util.BudgetMath
import com.ledgerly.app.util.Cycles
import com.ledgerly.app.util.DateFormats
import com.ledgerly.app.util.ReleaseInfo
import com.ledgerly.app.util.UpdateCheck
import kotlinx.coroutines.launch
import java.time.YearMonth
import android.app.KeyguardManager

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
fun LedgerlyApp(
    viewModel: AppViewModel = viewModel(),
    locked: Boolean = false,
    onUnlock: () -> Unit = {},
    pendingAdd: Boolean = false,
    onPendingAddHandled: () -> Unit = {},
    pendingReview: Boolean = false,
    onPendingReviewHandled: () -> Unit = {}
) {
    val transactions by viewModel.transactions.collectAsState()
    val budgets by viewModel.budgets.collectAsState()
    val monthOverrides by viewModel.monthOverrides.collectAsState()
    val recurring by viewModel.recurring.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val dueRecurring by viewModel.dueRecurring.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val month by viewModel.month.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val availableUpdate by viewModel.availableUpdate.collectAsState()
    val updateStatus by viewModel.updateStatus.collectAsState()
    val capture by viewModel.capture.collectAsState()

    // One check per launch, opt-out in Settings. Failure is silent.
    LaunchedEffect(Unit) {
        if (viewModel.settings.value.checkUpdates) viewModel.checkForUpdates()
    }

    LedgerlyTheme(themeMode = settings.theme) {
        when {
            locked && settings.appLock -> LockScreen(onUnlock = onUnlock)

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
                monthOverrides = monthOverrides,
                recurring = recurring,
                categories = categories,
                dueRecurring = dueRecurring,
                settings = settings,
                month = month,
                pendingAdd = pendingAdd,
                onPendingAddHandled = onPendingAddHandled,
                availableUpdate = availableUpdate,
                updateStatus = updateStatus,
                capture = capture,
                pendingReview = pendingReview,
                onPendingReviewHandled = onPendingReviewHandled
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
    monthOverrides: Set<String>,
    recurring: List<Recurring>,
    categories: List<Category>,
    dueRecurring: List<Recurring>,
    settings: AppSettings,
    month: YearMonth,
    pendingAdd: Boolean,
    onPendingAddHandled: () -> Unit,
    availableUpdate: ReleaseInfo?,
    updateStatus: UpdateCheck.Status,
    capture: CaptureStore.State,
    pendingReview: Boolean,
    onPendingReviewHandled: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showEditor by remember { mutableStateOf(false) }
    var editorTransaction by remember { mutableStateOf<Transaction?>(null) }
    var showBudgetSheet by remember { mutableStateOf(false) }
    var budgetSheetCategory by remember { mutableStateOf<String?>(null) }
    var showMonthlyBudgetDialog by remember { mutableStateOf(false) }
    var showIncomeDialog by remember { mutableStateOf(false) }
    var showRecurringSheet by remember { mutableStateOf(false) }
    var editingRecurring by remember { mutableStateOf<Recurring?>(null) }
    var showCategorySheet by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var notificationPermissionCallback by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }

    fun onNotificationPermission(granted: Boolean) {
        notificationPermissionCallback?.invoke(granted)
        notificationPermissionCallback = null
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var updating by remember { mutableStateOf(false) }

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        onNotificationPermission(granted)
    }
    val requestNotifications: ((Boolean) -> Unit) -> Unit = { onResult ->
        if (Reminders.canNotify(context)) {
            onResult(true)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionCallback = onResult
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onResult(true)
        }
    }

    val currency = settings.currency
    val cycleLabel = remember(month, settings.monthStartDay) {
        Cycles.label(month, settings.monthStartDay)
    }
    val ruleMode = settings.budgetStyle == BudgetStyle.RULE_50_30_20
    val stats = remember(transactions, month, settings.monthStartDay) {
        BudgetMath.summarize(transactions, month, settings.monthStartDay)
    }
    val now = remember { YearMonth.now() }
    val isCurrentMonth = month == now
    val canGoNext = month < now

    fun openEditor(transaction: Transaction?) {
        editorTransaction = transaction
        showEditor = true
    }

    fun installUpdate() {
        val info = availableUpdate ?: return
        if (updating) return
        updating = true
        scope.launch {
            val file = viewModel.downloadUpdate(info)
            updating = false
            val message = when {
                file == null -> "Couldn't download the update. Try again later."
                !ApkInstaller.install(context, file) ->
                    "Couldn't open the installer. Check that Ledgerly may install apps."
                else -> null
            }
            message?.let { snackbarHostState.showSnackbar(it) }
        }
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

    LaunchedEffect(pendingReview) {
        if (pendingReview) {
            selectedTab = 0
            onPendingReviewHandled()
        }
    }

    LaunchedEffect(pendingAdd) {
        if (pendingAdd) {
            openEditor(null)
            onPendingAddHandled()
        }
    }

    // Rendered before the settings branch: the sheet is opened from Settings, and it draws in
    // its own window, so composing it here keeps it reachable from both screens.
    if (showCategorySheet) {
        CategorySheet(
            editing = editingCategory,
            usageCount = editingCategory?.let { viewModel.usageOf(it.key) } ?: 0,
            onDismiss = { showCategorySheet = false },
            onSave = { category ->
                viewModel.saveCategory(category)
                showCategorySheet = false
            },
            onDelete = { category ->
                if (Categories.isBuiltIn(category.key)) viewModel.hideCategory(category)
                else viewModel.deleteCategory(category.key)
                showCategorySheet = false
            }
        )
    }

    if (showRecurringSheet) {
        RecurringSheet(
            editing = editingRecurring,
            currency = currency,
            onDismiss = { showRecurringSheet = false },
            onSave = { item ->
                if (editingRecurring == null) viewModel.addRecurring(item)
                else viewModel.updateRecurring(item)
                showRecurringSheet = false
            },
            onDelete = { item ->
                viewModel.deleteRecurring(item)
                showRecurringSheet = false
            }
        )
    }

    if (showSettings) {
        SettingsScreen(
            settings = settings,
            transactions = transactions,
            recurring = recurring,
            categories = categories,
            canUseAppLock = LocalContext.current.let { context ->
                context.getSystemService(KeyguardManager::class.java)?.isDeviceSecure == true
            },
            availableUpdate = availableUpdate,
            updateStatus = updateStatus,
            updating = updating,
            onBack = { showSettings = false },
            onNameChange = viewModel::setName,
            onCurrencyChange = viewModel::setCurrency,
            onMonthlyBudgetChange = viewModel::setMonthlyBudget,
            onIncomeChange = viewModel::setMonthlyIncome,
            onBudgetStyleChange = viewModel::setBudgetStyle,
            onThemeChange = viewModel::setTheme,
            onMonthStartChange = viewModel::setMonthStartDay,
            onAppLockChange = viewModel::setAppLock,
            onCheckUpdatesChange = viewModel::setCheckUpdates,
            onCheckForUpdates = { viewModel.checkForUpdates() },
            onInstallUpdate = { installUpdate() },
            onLoadSampleData = viewModel::resetWithSampleData,
            onClearAllData = viewModel::clearAllData,
            onBackupRequested = { viewModel.buildBackupJson() },
            onRestoreRequested = { json, replace ->
                viewModel.restoreBackupReport(json, replace)
            },
            onImportCsvRequested = { text -> viewModel.importCsv(text) },
            onAddRecurring = {
                editingRecurring = null
                showRecurringSheet = true
            },
            onEditRecurring = { item ->
                editingRecurring = item
                showRecurringSheet = true
            },
            onAddCategory = {
                editingCategory = null
                showCategorySheet = true
            },
            onEditCategory = { category ->
                editingCategory = category
                showCategorySheet = true
            },
            onRemindChange = { enabled ->
                if (enabled) {
                    // Ask for the notification permission before promising a reminder.
                    requestNotifications { granted ->
                        viewModel.setRemindToLog(granted)
                        if (granted) viewModel.previewReminder()
                    }
                } else {
                    viewModel.setRemindToLog(false)
                }
            },
            onWeeklyReviewChange = { enabled ->
                if (enabled) {
                    requestNotifications { granted -> viewModel.setWeeklyReview(granted) }
                } else {
                    viewModel.setWeeklyReview(false)
                }
            },
            onPreviewReminder = { viewModel.previewReminder() },
            onCaptureChange = { viewModel.setCapturePayments(it) },
            onOpenNotificationAccess = {
                NotificationAccess.request(context)
            },
            canReadNotifications = NotificationAccess.isGranted(context)
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
                    monthlyIncomeCents = settings.monthlyIncomeCents,
                    budgetStyle = settings.budgetStyle,
                    currency = currency,
                    name = settings.name,
                    isCurrentMonth = isCurrentMonth,
                    canGoNext = canGoNext,
                    onPreviousMonth = { viewModel.selectMonth(month.minusMonths(1)) },
                    onNextMonth = { viewModel.selectMonth(month.plusMonths(1)) },
                    cycleLabel = cycleLabel,
                    onSettings = { showSettings = true },
                    onSeeAll = { selectedTab = 1 },
                    onAddTransaction = { openEditor(null) },
                    onLoadSample = if (SampleData.available) {
                        viewModel::loadSampleDataIfEmpty
                    } else {
                        null
                    },
                    onOpenBudgets = { selectedTab = 2 },
                    onOpenInsights = { selectedTab = 3 },
                    onSetBudget = {
                        if (ruleMode) showIncomeDialog = true else showMonthlyBudgetDialog = true
                    },
                    onTransactionClick = { openEditor(it) },
                    dueRecurring = dueRecurring,
                    onLogRecurring = { viewModel.logRecurring(it) },
                    onSnoozeRecurring = { viewModel.snoozeRecurring(it) },
                    onManageRecurring = { showSettings = true },
                    availableUpdate = availableUpdate,
                    updating = updating,
                    onUpdate = { installUpdate() },
                    pendingPayments = capture.suggestions,
                    onAcceptPayment = { suggestion, categoryKey ->
                        viewModel.acceptCapture(suggestion, categoryKey)
                    },
                    onDismissPayment = { id -> viewModel.dismissCapture(id) }
                )

                1 -> ActivityScreen(
                    transactions = transactions,
                    month = month,
                    currency = currency,
                    canGoNext = canGoNext,
                    onPreviousMonth = { viewModel.selectMonth(month.minusMonths(1)) },
                    onNextMonth = { viewModel.selectMonth(month.plusMonths(1)) },
                    cycleLabel = cycleLabel,
                    onAddTransaction = { openEditor(null) },
                    onTransactionClick = { openEditor(it) },
                    onDeleteTransaction = { deleteWithUndo(it) }
                )

                2 -> BudgetsScreen(
                    stats = stats,
                    budgets = budgets,
                    monthlyBudgetCents = settings.monthlyBudgetCents,
                    monthlyIncomeCents = settings.monthlyIncomeCents,
                    budgetStyle = settings.budgetStyle,
                    currency = currency,
                    month = month,
                    monthOverrides = monthOverrides,
                    canGoNext = canGoNext,
                    onPreviousMonth = { viewModel.selectMonth(month.minusMonths(1)) },
                    onNextMonth = { viewModel.selectMonth(month.plusMonths(1)) },
                    cycleLabel = cycleLabel,
                    onAddBudget = {
                        budgetSheetCategory = null
                        showBudgetSheet = true
                    },
                    onEditBudget = { key ->
                        budgetSheetCategory = key
                        showBudgetSheet = true
                    },
                    onSetMonthlyBudget = { showMonthlyBudgetDialog = true },
                    onSetIncome = { showIncomeDialog = true }
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
                    cycleLabel = cycleLabel,
                    onAddTransaction = { openEditor(null) }
                )
            }
        }
    }

    if (showEditor) {
        TransactionSheet(
            editing = editorTransaction,
            currency = currency,
            categories = categories,
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
            isMonthOverride = editingKey != null && editingKey in monthOverrides,
            monthLabel = DateFormats.monthShort(month),
            availableCategories = if (editingKey == null) {
                Categories.expense.filter { it.key !in budgets }
            } else {
                emptyList()
            },
            currency = currency,
            onDismiss = { showBudgetSheet = false },
            onSave = { key, cents, thisMonthOnly ->
                viewModel.setBudget(key, cents, thisMonthOnly)
                showBudgetSheet = false
            },
            onDelete = { key, thisMonthOnly ->
                viewModel.deleteBudget(key, thisMonthOnly)
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

    if (showIncomeDialog) {
        BudgetAmountDialog(
            title = "Monthly income",
            description = "Your take-home pay each month. The 50:30:20 rule splits it into 50% needs, 30% wants and 20% savings.",
            initialCents = settings.monthlyIncomeCents,
            currency = currency,
            removeLabel = "Remove monthly income",
            onDismiss = { showIncomeDialog = false },
            onSave = { cents ->
                viewModel.setMonthlyIncome(cents)
                showIncomeDialog = false
            }
        )
    }
}
