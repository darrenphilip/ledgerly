package com.ledgerly.app.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ledgerly.app.data.SampleData
import com.ledgerly.app.ui.components.LedgerCard
import com.ledgerly.app.ui.theme.ledgerColors
import com.ledgerly.app.util.Currencies
import com.ledgerly.app.util.Money

@Composable
fun OnboardingScreen(
    defaultCurrency: String,
    onComplete: (name: String, currency: String, monthlyBudgetCents: Long, withSampleData: Boolean) -> Unit
) {
    var step by rememberSaveable { mutableStateOf(0) }
    var name by rememberSaveable { mutableStateOf("") }
    var currency by rememberSaveable { mutableStateOf(defaultCurrency) }
    var budgetText by rememberSaveable { mutableStateOf(defaultBudgetText(defaultCurrency)) }
    var useSample by rememberSaveable { mutableStateOf(SampleData.available) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .size(280.dp)
                .align(Alignment.TopEnd)
                .offset(x = 110.dp, y = (-130).dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.07f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(190.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-80).dp, y = 70.dp)
                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.06f), CircleShape)
        )

        Crossfade(
            targetState = step,
            animationSpec = tween(280),
            label = "onboarding"
        ) { currentStep ->
            if (currentStep == 0) {
                WelcomeStep(onNext = { step = 1 })
            } else {
                SetupStep(
                    name = name,
                    onNameChange = { if (it.length <= 30) name = it },
                    currency = currency,
                    onCurrencyChange = { next ->
                        // Keep the suggested budget sensible when the currency changes.
                        val untouched = budgetText == defaultBudgetText(currency)
                        currency = next
                        if (untouched) budgetText = defaultBudgetText(next)
                    },
                    budgetText = budgetText,
                    onBudgetChange = { input ->
                        if (input.length <= 14 && input.all { c -> c.isDigit() || c == '.' || c == ',' }) {
                            budgetText = input
                        }
                    },
                    useSample = useSample,
                    onUseSampleChange = { useSample = it },
                    onBack = { step = 0 },
                    onFinish = {
                        onComplete(
                            name,
                            currency,
                            Money.parseToCents(budgetText, currency) ?: 0L,
                            useSample && SampleData.available
                        )
                    }
                )
            }
        }
    }
}

/** Suggested monthly budget for a currency, as typed text. Empty in release builds. */
private fun defaultBudgetText(currency: String): String {
    val cents = SampleData.defaultMonthlyBudgetCents(currency)
    return if (cents > 0L) Money.plain(cents, currency) else ""
}

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    val extras = MaterialTheme.ledgerColors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 26.dp, vertical = 24.dp)
    ) {
        Spacer(Modifier.height(26.dp))
        Box(
            modifier = Modifier
                .size(86.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(Brush.linearGradient(listOf(extras.heroStart, extras.heroEnd))),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.AccountBalanceWallet,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(44.dp)
            )
        }
        Spacer(Modifier.height(22.dp))
        Text(
            text = "Ledgerly",
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Know where your money goes — without the spreadsheet.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(34.dp))
        FeatureRow(
            icon = { Icon(Icons.AutoMirrored.Rounded.ReceiptLong, null, tint = MaterialTheme.colorScheme.primary) },
            title = "Log spending in seconds",
            description = "A big amount pad, smart categories, and zero friction."
        )
        Spacer(Modifier.height(20.dp))
        FeatureRow(
            icon = { Icon(Icons.Rounded.Savings, null, tint = MaterialTheme.colorScheme.secondary) },
            title = "Budgets that keep you honest",
            description = "See what's left and what's safe to spend each day."
        )
        Spacer(Modifier.height(20.dp))
        FeatureRow(
            icon = { Icon(Icons.Rounded.Insights, null, tint = extras.warning) },
            title = "Insights that actually help",
            description = "Trends, category breakdowns, and gentle nudges."
        )

        Spacer(Modifier.height(38.dp))
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text("Get started", style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = "No account. No cloud. Your data stays on this device.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun FeatureRow(
    icon: @Composable () -> Unit,
    title: String,
    description: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SetupStep(
    name: String,
    onNameChange: (String) -> Unit,
    currency: String,
    onCurrencyChange: (String) -> Unit,
    budgetText: String,
    onBudgetChange: (String) -> Unit,
    useSample: Boolean,
    onUseSampleChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onFinish: () -> Unit
) {
    var currencyMenuOpen by remember { mutableStateOf(false) }
    val selectedCurrency = Currencies.of(currency)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 26.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "Step 2 of 2",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(
                    Modifier.size(7.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
                Box(
                    Modifier.size(7.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        Text(
            text = "Make it yours",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "A few details so Ledgerly fits how you spend.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("What should we call you?") },
            placeholder = { Text("Your name") },
            leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(Modifier.height(14.dp))
        Text(
            text = "Currency",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        Box {
            OutlinedButton(
                onClick = { currencyMenuOpen = true },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "${selectedCurrency.symbol}  ${selectedCurrency.code} · ${selectedCurrency.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Start,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(
                expanded = currencyMenuOpen,
                onDismissRequest = { currencyMenuOpen = false },
                modifier = Modifier.heightIn(max = 340.dp)
            ) {
                Currencies.all.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "${option.symbol}  ${option.code} · ${option.name}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        onClick = {
                            onCurrencyChange(option.code)
                            currencyMenuOpen = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        OutlinedTextField(
            value = budgetText,
            onValueChange = onBudgetChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Monthly budget (optional)") },
            prefix = {
                Text(
                    text = selectedCurrency.symbol,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            placeholder = { Text(Money.placeholder(currency)) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            supportingText = { Text("You can change this any time.") }
        )

        if (SampleData.available) {
            Spacer(Modifier.height(18.dp))
            LedgerCard(contentPadding = PaddingValues(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✨", fontSize = 18.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Start with sample data",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Four months of example spending so you can explore right away.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Switch(checked = useSample, onCheckedChange = onUseSampleChange)
                }
            }
        }

        Spacer(Modifier.height(26.dp))
        Button(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text("Start budgeting", style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.height(8.dp))
        TextButton(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Skip setup",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}
