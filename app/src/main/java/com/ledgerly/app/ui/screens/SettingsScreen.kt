package com.ledgerly.app.ui.screens

import android.content.ClipData
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.SettingsBrightness
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
import com.ledgerly.app.data.SampleData
import com.ledgerly.app.data.ThemeMode
import com.ledgerly.app.data.Transaction
import com.ledgerly.app.ui.components.LedgerCard
import com.ledgerly.app.ui.components.RowDivider
import com.ledgerly.app.ui.sheets.BudgetAmountDialog
import com.ledgerly.app.ui.theme.ledgerColors
import com.ledgerly.app.util.CsvExporter
import com.ledgerly.app.util.Currencies
import com.ledgerly.app.util.Money
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    transactions: List<Transaction>,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onMonthlyBudgetChange: (Long) -> Unit,
    onIncomeChange: (Long) -> Unit,
    onBudgetStyleChange: (BudgetStyle) -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    onLoadSampleData: () -> Unit,
    onClearAllData: () -> Unit
) {
    var showNameDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var showIncomeDialog by remember { mutableStateOf(false) }
    var showStyleDialog by remember { mutableStateOf(false) }
    var showSampleConfirm by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val extras = MaterialTheme.ledgerColors
    val currencyOption = Currencies.of(settings.currency)

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
