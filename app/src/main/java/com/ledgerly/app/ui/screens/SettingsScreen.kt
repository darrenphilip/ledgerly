package com.ledgerly.app.ui.screens

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.SettingsBrightness
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ledgerly.app.BuildConfig
import com.ledgerly.app.data.AppSettings
import com.ledgerly.app.data.BudgetStyle
import com.ledgerly.app.data.Categories
import com.ledgerly.app.data.Recurring
import com.ledgerly.app.data.SampleData
import com.ledgerly.app.data.ThemeMode
import com.ledgerly.app.data.Transaction
import com.ledgerly.app.ui.components.CategoryBadge
import com.ledgerly.app.ui.components.LedgerCard
import com.ledgerly.app.ui.components.RowDivider
import com.ledgerly.app.ui.sheets.BudgetAmountDialog
import com.ledgerly.app.ui.theme.ledgerColors
import com.ledgerly.app.util.CsvExporter
import com.ledgerly.app.util.Currencies
import com.ledgerly.app.util.DateFormats
import com.ledgerly.app.util.Money
import com.ledgerly.app.util.ReleaseInfo
import com.ledgerly.app.util.UpdateCheck
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    transactions: List<Transaction>,
    recurring: List<Recurring>,
    canUseAppLock: Boolean,
    availableUpdate: ReleaseInfo?,
    updateStatus: UpdateCheck.Status,
    updating: Boolean,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onMonthlyBudgetChange: (Long) -> Unit,
    onIncomeChange: (Long) -> Unit,
    onBudgetStyleChange: (BudgetStyle) -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    onAppLockChange: (Boolean) -> Unit,
    onCheckUpdatesChange: (Boolean) -> Unit,
    onCheckForUpdates: () -> Unit,
    onInstallUpdate: () -> Unit,
    onLoadSampleData: () -> Unit,
    onClearAllData: () -> Unit,
    onBackupRequested: suspend () -> String,
    onRestoreRequested: suspend (String, Boolean) -> String,
    onImportCsvRequested: suspend (String) -> String,
    onAddRecurring: () -> Unit,
    onEditRecurring: (Recurring) -> Unit
) {
    var showNameDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var showIncomeDialog by remember { mutableStateOf(false) }
    var showStyleDialog by remember { mutableStateOf(false) }
    var showSampleConfirm by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var pendingRestoreJson by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val extras = MaterialTheme.ledgerColors
    val currencyOption = Currencies.of(settings.currency)

    fun readText(uri: Uri): String? = runCatching {
        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
    }.getOrNull()

    fun toast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    val createBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val json = onBackupRequested()
            val written = runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {
                        it.write(json)
                    } ?: error("Could not open the chosen file")
                }
            }
            toast(if (written.isSuccess) "Backup saved" else "Couldn't write the backup file")
        }
    }

    val openBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val text = readText(uri)
        if (text == null) {
            toast("Couldn't read that file")
        } else {
            pendingRestoreJson = text
        }
    }

    val openCsv = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val text = readText(uri)
        if (text == null) {
            toast("Couldn't read that file")
        } else {
            scope.launch {
                val summary = runCatching { onImportCsvRequested(text) }
                    .getOrElse { "That file isn't a Ledgerly CSV export" }
                toast(summary)
            }
        }
    }

    val exportCsv: () -> Unit = {
        scope.launch {
            runCatching {
                val csv = CsvExporter.build(transactions)
                val uri = withContext(Dispatchers.IO) { CsvExporter.writeToCache(context, csv) }
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Ledgerly export")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    clipData = ClipData.newRawUri("Ledgerly export", uri)
                }
                context.startActivity(Intent.createChooser(intent, "Export transactions"))
            }.onFailure {
                Toast.makeText(context, "Couldn't export right now", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LedgerCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(extras.heroStart, extras.heroEnd))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        val initial = settings.name.trim().firstOrNull()?.uppercaseChar()?.toString()
                        Text(
                            text = initial ?: "👋",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = settings.name.ifBlank { "Your profile" },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Local-first budgeting",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = { showNameDialog = true }) {
                        Text("Edit", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            SectionLabel("Money")
            LedgerCard(contentPadding = PaddingValues(0.dp)) {
                SettingsRow(
                    icon = Icons.Rounded.Payments,
                    title = "Currency",
                    subtitle = "${currencyOption.symbol} ${currencyOption.code} · ${currencyOption.name}",
                    onClick = { showCurrencyDialog = true }
                )
                RowDivider(Modifier.padding(horizontal = 16.dp))
                SettingsRow(
                    icon = Icons.Rounded.AccountBalanceWallet,
                    title = "Monthly budget",
                    subtitle = if (settings.monthlyBudgetCents > 0L) {
                        Money.format(settings.monthlyBudgetCents, settings.currency)
                    } else {
                        "Not set"
                    },
                    onClick = { showBudgetDialog = true }
                )
                RowDivider(Modifier.padding(horizontal = 16.dp))
                SettingsRow(
                    icon = Icons.Rounded.PieChart,
                    title = "Budget style",
                    subtitle = if (settings.budgetStyle == BudgetStyle.RULE_50_30_20) {
                        "50:30:20 \u00b7 needs, wants, savings"
                    } else {
                        "Simple \u00b7 one limit per category"
                    },
                    onClick = { showStyleDialog = true }
                )
                if (settings.budgetStyle == BudgetStyle.RULE_50_30_20) {
                    RowDivider(Modifier.padding(horizontal = 16.dp))
                    SettingsRow(
                        icon = Icons.Rounded.Payments,
                        title = "Monthly income",
                        subtitle = if (settings.monthlyIncomeCents > 0L) {
                            Money.format(settings.monthlyIncomeCents, settings.currency)
                        } else {
                            "Not set \u00b7 using income logged this month"
                        },
                        onClick = { showIncomeDialog = true }
                    )
                }
            }

            SectionLabel("Appearance")
            LedgerCard(contentPadding = PaddingValues(8.dp)) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val modes = ThemeMode.entries
                    modes.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = settings.theme == mode,
                            onClick = { onThemeChange(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                            icon = {
                                Icon(
                                    imageVector = when (mode) {
                                        ThemeMode.SYSTEM -> Icons.Rounded.SettingsBrightness
                                        ThemeMode.LIGHT -> Icons.Rounded.LightMode
                                        ThemeMode.DARK -> Icons.Rounded.DarkMode
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = {
                                Text(mode.label, style = MaterialTheme.typography.labelMedium)
                            }
                        )
                    }
                }
            }

            SectionLabel("Data")
            LedgerCard(contentPadding = PaddingValues(0.dp)) {
                SettingsRow(
                    icon = Icons.Rounded.FileDownload,
                    title = "Export CSV",
                    subtitle = if (transactions.isEmpty()) "No transactions to export yet"
                    else "Share a spreadsheet of every transaction",
                    onClick = exportCsv,
                    enabled = transactions.isNotEmpty()
                )
                RowDivider(Modifier.padding(horizontal = 16.dp))
                SettingsRow(
                    icon = Icons.Rounded.Backup,
                    title = "Back up to a file",
                    subtitle = "Saves transactions, budgets, recurring entries and settings as a restore file",
                    onClick = { createBackup.launch("ledgerly-backup.json") }
                )
                RowDivider(Modifier.padding(horizontal = 16.dp))
                SettingsRow(
                    icon = Icons.Rounded.Restore,
                    title = "Restore from a backup",
                    subtitle = "Replace or merge with a previously saved backup file",
                    onClick = { openBackup.launch(arrayOf("application/json", "application/octet-stream", "text/plain")) }
                )
                RowDivider(Modifier.padding(horizontal = 16.dp))
                SettingsRow(
                    icon = Icons.Rounded.FileUpload,
                    title = "Import CSV",
                    subtitle = "Reads a previously exported CSV back into the ledger",
                    onClick = {
                        openCsv.launch(arrayOf("text/*", "application/vnd.ms-excel"))
                    }
                )
                if (SampleData.available) {
                    RowDivider(Modifier.padding(horizontal = 16.dp))
                    SettingsRow(
                        icon = Icons.Rounded.RestartAlt,
                        title = "Reset with sample data",
                        subtitle = "Replace everything with four months of demo data",
                        onClick = { showSampleConfirm = true }
                    )
                }
                RowDivider(Modifier.padding(horizontal = 16.dp))
                SettingsRow(
                    icon = Icons.Rounded.Delete,
                    title = "Clear all data",
                    subtitle = "Remove every transaction and budget",
                    onClick = { showClearConfirm = true },
                    danger = true
                )
            }

            SectionLabel("Automation")
            LedgerCard(contentPadding = PaddingValues(0.dp)) {
                SettingsRow(
                    icon = Icons.Rounded.Repeat,
                    title = "Add recurring entry",
                    subtitle = if (recurring.isEmpty()) "Rent, salary, subscriptions — log once per cycle"
                    else "Schedule entries that repeat on their own",
                    onClick = onAddRecurring
                )
                recurring.forEach { item ->
                    RowDivider(Modifier.padding(horizontal = 16.dp))
                    RecurringRow(item = item, currency = settings.currency, onClick = { onEditRecurring(item) })
                }
            }

            SectionLabel("Updates")
            LedgerCard(contentPadding = PaddingValues(0.dp)) {
                SettingsRow(
                    icon = Icons.Rounded.SystemUpdate,
                    title = when {
                        availableUpdate != null -> "Update to ${availableUpdate.version}"
                        updateStatus.checking -> "Checking for updates…"
                        updateStatus.failed -> "Check for updates"
                        updateStatus.lastChecked > 0L -> "You're up to date"
                        else -> "Check for updates"
                    },
                    subtitle = when {
                        updating -> "Downloading…"
                        availableUpdate != null -> "Tap to download and install ${availableUpdate.apkName}"
                        updateStatus.checking -> "Asking the GitHub release feed"
                        updateStatus.failed -> "Couldn't reach GitHub · tap to retry"
                        updateStatus.lastChecked > 0L ->
                            "Version ${BuildConfig.VERSION_NAME} · checked just now"
                        else -> "Version ${BuildConfig.VERSION_NAME} · GitHub releases"
                    },
                    onClick = {
                        if (availableUpdate != null) onInstallUpdate() else onCheckForUpdates()
                    },
                    enabled = !updating && !updateStatus.checking
                )
                RowDivider(Modifier.padding(horizontal = 16.dp))
                SettingsRow(
                    icon = Icons.Rounded.Autorenew,
                    title = "Check automatically",
                    subtitle = if (settings.checkUpdates) {
                        "Looks for a new release when the app opens"
                    } else {
                        "Off · only checks when you ask"
                    },
                    onClick = { onCheckUpdatesChange(!settings.checkUpdates) }
                )
            }

            SectionLabel("Privacy")
            LedgerCard(contentPadding = PaddingValues(0.dp)) {
                SettingsRow(
                    icon = Icons.Rounded.Lock,
                    title = "App lock",
                    subtitle = when {
                        !canUseAppLock -> "Your device has no screen lock yet"
                        settings.appLock -> "On \u00b7 asks for your device screen lock when reopened"
                        else -> "Off \u00b7 opens straight to the ledger"
                    },
                    onClick = { if (canUseAppLock) onAppLockChange(!settings.appLock) },
                    enabled = canUseAppLock
                )
            }

            SectionLabel("About")
            LedgerCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("💜", fontSize = 19.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Ledgerly",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Version ${BuildConfig.VERSION_NAME} · Jetpack Compose",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Your data never leaves this device. No account, no cloud, no tracking.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showNameDialog) {
        var draft by remember { mutableStateOf(settings.name) }
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Your name") },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { if (it.length <= 30) draft = it },
                    singleLine = true,
                    label = { Text("Name") },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onNameChange(draft)
                        showNameDialog = false
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showCurrencyDialog) {
        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            title = { Text("Currency") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(Currencies.all, key = { it.code }) { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onCurrencyChange(option.code)
                                    showCurrencyDialog = false
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.currency == option.code,
                                onClick = {
                                    onCurrencyChange(option.code)
                                    showCurrencyDialog = false
                                }
                            )
                            Spacer(Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${option.symbol}  ${option.code}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = option.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCurrencyDialog = false }) { Text("Done") }
            }
        )
    }

    if (showBudgetDialog) {
        BudgetAmountDialog(
            title = "Monthly budget",
            description = "Set the total you want to spend each month. Ledgerly uses it to calculate what's safe to spend per day.",
            initialCents = settings.monthlyBudgetCents,
            currency = settings.currency,
            onDismiss = { showBudgetDialog = false },
            onSave = { cents ->
                onMonthlyBudgetChange(cents)
                showBudgetDialog = false
            }
        )
    }

    if (showIncomeDialog) {
        BudgetAmountDialog(
            title = "Monthly income",
            description = "Your take-home pay each month. The 50:30:20 rule splits it into 50% needs, 30% wants and 20% savings.",
            initialCents = settings.monthlyIncomeCents,
            currency = settings.currency,
            removeLabel = "Remove monthly income",
            onDismiss = { showIncomeDialog = false },
            onSave = { cents ->
                onIncomeChange(cents)
                showIncomeDialog = false
            }
        )
    }

    if (showStyleDialog) {
        AlertDialog(
            onDismissRequest = { showStyleDialog = false },
            title = { Text("Budget style") },
            text = {
                Column {
                    BudgetStyleOption(
                        title = "Simple",
                        description = "One limit per category, plus an optional monthly total.",
                        selected = settings.budgetStyle == BudgetStyle.SIMPLE,
                        onClick = {
                            onBudgetStyleChange(BudgetStyle.SIMPLE)
                            showStyleDialog = false
                        }
                    )
                    Spacer(Modifier.height(10.dp))
                    BudgetStyleOption(
                        title = "50:30:20",
                        description = "Split income into 50% needs, 30% wants and 20% savings.",
                        selected = settings.budgetStyle == BudgetStyle.RULE_50_30_20,
                        onClick = {
                            onBudgetStyleChange(BudgetStyle.RULE_50_30_20)
                            showStyleDialog = false
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showStyleDialog = false }) { Text("Done") }
            }
        )
    }

    if (showSampleConfirm) {
        AlertDialog(
            onDismissRequest = { showSampleConfirm = false },
            title = { Text("Reset with sample data?") },
            text = { Text("This replaces all of your current transactions and budgets with four months of demo data.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onLoadSampleData()
                        showSampleConfirm = false
                    }
                ) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = { showSampleConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear all data?") },
            text = { Text("Every transaction and budget will be removed. Your name, currency and theme stay as they are.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearAllData()
                        showClearConfirm = false
                    }
                ) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
            }
        )
    }

    pendingRestoreJson?.let { json ->
        AlertDialog(
            onDismissRequest = { pendingRestoreJson = null },
            title = { Text("Restore this backup?") },
            text = {
                Column {
                    Text(
                        "Replace removes what's on the device and loads the backup. Merge keeps " +
                            "everything and only adds transactions that aren't there yet."
                    )
                    Spacer(Modifier.height(10.dp))
                    TextButton(
                        onClick = {
                            pendingRestoreJson = null
                            scope.launch {
                                val summary = runCatching { onRestoreRequested(json, false) }
                                    .getOrElse { "That file isn't a Ledgerly backup" }
                                toast(summary)
                            }
                        }
                    ) { Text("Merge instead") }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingRestoreJson = null
                        scope.launch {
                            val summary = runCatching { onRestoreRequested(json, true) }
                                .getOrElse { "That file isn't a Ledgerly backup" }
                            toast(summary)
                        }
                    }
                ) { Text("Replace") }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestoreJson = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun RecurringRow(item: Recurring, currency: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryBadge(category = Categories.of(item.categoryKey), size = 34.dp, emojiSize = 16.sp)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.note.ifBlank { Categories.of(item.categoryKey).label },
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${Money.format(item.amountCents, currency)} \u00b7 ${item.cadence.label.lowercase()} \u00b7 next ${DateFormats.fullDate(item.nextDue)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun BudgetStyleOption(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 6.dp)
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    danger: Boolean = false
) {
    val tint = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.45f)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(19.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = if (danger) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(20.dp)
        )
    }
}
