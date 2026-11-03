package com.ledgerly.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import com.ledgerly.app.data.Bucket
import com.ledgerly.app.data.BudgetStyle
import com.ledgerly.app.data.SampleData
import com.ledgerly.app.ui.components.LedgerCard
import com.ledgerly.app.ui.theme.ledgerColors
import com.ledgerly.app.util.Currencies
import com.ledgerly.app.util.Money

/** Everything the first run collects. */
data class OnboardingResult(
    val name: String,
    val currency: String,
    val monthlyBudgetCents: Long,
    val monthlyIncomeCents: Long,
    val budgetStyle: BudgetStyle,
    val monthStartDay: Int,
    val withSampleData: Boolean
)

/**
 * First run, in four short steps: who you are, how you budget, what your month costs, and a
 * confirmation that tells you exactly what was set up.
 */
@Composable
fun OnboardingScreen(
    defaultCurrency: String,
    onComplete: (OnboardingResult) -> Unit
) {
    var step by rememberSaveable { mutableStateOf(0) }
    var name by rememberSaveable { mutableStateOf("") }
    var currency by rememberSaveable { mutableStateOf(defaultCurrency) }
    var budgetText by rememberSaveable { mutableStateOf("") }
    var incomeText by rememberSaveable { mutableStateOf("") }
    var style by rememberSaveable { mutableStateOf(BudgetStyle.SIMPLE) }
    var startDay by rememberSaveable { mutableStateOf(1) }
    var useSample by rememberSaveable { mutableStateOf(SampleData.available) }

    val steps = 4
    val budgetCents = Money.parseToCents(budgetText, currency) ?: 0L
    val incomeCents = Money.parseToCents(incomeText, currency) ?: 0L

    BackHandler(enabled = step > 0) { step-- }

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

        Column(modifier = Modifier.fillMaxSize()) {
            if (step > 0) {
                StepHeader(
                    step = step,
                    total = steps,
                    onBack = { step-- }
                )
            }

            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    val forward = targetState > initialState
                    val enter = slideInHorizontally(
                        animationSpec = tween(280),
                        initialOffsetX = { if (forward) it / 6 else -it / 6 }
                    ) + fadeIn(tween(280))
                    val exit = slideOutHorizontally(
                        animationSpec = tween(280),
                        targetOffsetX = { if (forward) -it / 6 else it / 6 }
                    ) + fadeOut(tween(180))
                    enter togetherWith exit
                },
                label = "onboarding"
            ) { current ->
                when (current) {
                    0 -> WelcomeStep(onNext = { step = 1 })

                    1 -> IdentityStep(
                        name = name,
                        onNameChange = { if (it.length <= 30) name = it },
                        currency = currency,
                        onCurrencyChange = { next ->
                            val untouched = budgetText == defaultBudgetText(currency)
                            currency = next
                            if (untouched) budgetText = defaultBudgetText(next)
                        },
                        onNext = { step = 2 }
                    )

                    2 -> StyleStep(
                        style = style,
                        onStyleChange = { style = it },
                        incomeText = incomeText,
                        onIncomeChange = { input ->
                            if (input.length <= 14 && input.all { it.isDigit() || it == '.' || it == ',' }) {
                                incomeText = input
                            }
                        },
                        currency = currency,
                        onNext = { step = 3 }
                    )

                    else -> FinishStep(
                        name = name,
                        currency = currency,
                        budgetText = budgetText,
                        onBudgetChange = { input ->
                            if (input.length <= 14 && input.all { it.isDigit() || it == '.' || it == ',' }) {
                                budgetText = input
                            }
                        },
                        startDay = startDay,
                        onStartDayChange = { startDay = it.coerceIn(1, 28) },
                        useSample = useSample && SampleData.available,
                        onUseSampleChange = { useSample = it },
                        style = style,
                        budgetCents = budgetCents,
                        incomeCents = incomeCents,
                        onFinish = {
                            onComplete(
                                OnboardingResult(
                                    name = name.trim(),
                                    currency = currency,
                                    monthlyBudgetCents = budgetCents,
                                    monthlyIncomeCents = incomeCents,
                                    budgetStyle = style,
                                    monthStartDay = startDay,
                                    withSampleData = useSample && SampleData.available
                                )
                            )
                        }
                    )
                }
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
private fun StepHeader(step: Int, total: Int, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(top = 6.dp)
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
                text = "${step + 1} of $total",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "Skip",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onBack() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { (step.toFloat() / (total - 1)).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
private fun StepScaffold(
    title: String,
    subtitle: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    primaryEnabled: Boolean = true,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 26.dp, vertical = 12.dp)
    ) {
        Spacer(Modifier.height(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(22.dp))
        content()
        Spacer(Modifier.height(26.dp))
        Button(
            onClick = onPrimary,
            enabled = primaryEnabled,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text(primaryLabel, style = MaterialTheme.typography.labelLarge)
        }
        if (secondaryLabel != null && onSecondary != null) {
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onSecondary, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = secondaryLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(16.dp))
    }
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
                Icons.Rounded.Savings,
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

        Spacer(Modifier.height(30.dp))
        FeatureRow(
            icon = { Icon(Icons.AutoMirrored.Rounded.ReceiptLong, null, tint = MaterialTheme.colorScheme.primary) },
            title = "Log spending in seconds",
            description = "Amount pad, categories, done. Works offline."
        )
        Spacer(Modifier.height(18.dp))
        FeatureRow(
            icon = { Icon(Icons.Rounded.Savings, null, tint = MaterialTheme.colorScheme.secondary) },
            title = "Every expense has a place",
            description = "Needs, wants and savings — counted automatically."
        )
        Spacer(Modifier.height(18.dp))
        FeatureRow(
            icon = { Icon(Icons.Rounded.Insights, null, tint = extras.warning) },
            title = "Insights that actually help",
            description = "What's safe to spend, and where it went."
        )

        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text("Set up Ledgerly", style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = "Takes about a minute · no account, no cloud",
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
private fun IdentityStep(
    name: String,
    onNameChange: (String) -> Unit,
    currency: String,
    onCurrencyChange: (String) -> Unit,
    onNext: () -> Unit
) {
    var currencyMenuOpen by remember { mutableStateOf(false) }
    val selectedCurrency = Currencies.of(currency)

    StepScaffold(
        title = "First, the basics",
        subtitle = "Two things, and you can change both later.",
        primaryLabel = "Continue",
        onPrimary = onNext,
        secondaryLabel = if (name.isBlank()) "Leave it blank" else null,
        onSecondary = if (name.isBlank()) onNext else null
    ) {
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

        Spacer(Modifier.height(16.dp))
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
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Used for every amount you enter. Rupiah is the default for new installs.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StyleStep(
    style: BudgetStyle,
    onStyleChange: (BudgetStyle) -> Unit,
    incomeText: String,
    onIncomeChange: (String) -> Unit,
    currency: String,
    onNext: () -> Unit
) {
    StepScaffold(
        title = "How do you want to budget?",
        subtitle = "Pick one now — switching later takes two taps.",
        primaryLabel = "Continue",
        onPrimary = onNext
    ) {
        StyleCard(
            title = "Simple",
            description = "One limit per category, plus an optional monthly total.",
            selected = style == BudgetStyle.SIMPLE,
            onClick = { onStyleChange(BudgetStyle.SIMPLE) }
        )
        Spacer(Modifier.height(10.dp))
        StyleCard(
            title = "50:30:20",
            description = "Split your income into needs, wants and savings automatically.",
            selected = style == BudgetStyle.RULE_50_30_20,
            onClick = { onStyleChange(BudgetStyle.RULE_50_30_20) }
        )

        if (style == BudgetStyle.RULE_50_30_20) {
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = incomeText,
                onValueChange = onIncomeChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Monthly income") },
                prefix = {
                    Text(
                        text = Currencies.symbolOf(currency),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                placeholder = { Text(Money.placeholder(currency)) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                supportingText = { Text("Take-home pay. You can add this later instead.") }
            )
            Spacer(Modifier.height(10.dp))
            SplitPreview(Money.parseToCents(incomeText, currency) ?: 0L, currency)
        }
    }
}

@Composable
private fun SplitPreview(incomeCents: Long, currency: String) {
    if (incomeCents <= 0L) return
    val needs = incomeCents * 50 / 100
    val wants = incomeCents * 30 / 100
    val savings = incomeCents - needs - wants
    LedgerCard(contentPadding = PaddingValues(14.dp)) {
        Text(
            text = "Your split",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SplitCell("🏠", "Needs", Money.format(needs, currency), Color(0xFF3B82F6), Modifier.weight(1f))
            SplitCell("🎬", "Wants", Money.format(wants, currency), Color(0xFFEC4899), Modifier.weight(1f))
            SplitCell("🏦", "Save", Money.format(savings, currency), Color(0xFF22C55E), Modifier.weight(1f))
        }
    }
}

@Composable
private fun SplitCell(
    emoji: String,
    label: String,
    amount: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(tint.copy(alpha = 0.12f))
            .padding(vertical = 9.dp, horizontal = 8.dp)
    ) {
        Text(text = emoji, fontSize = 15.sp)
        Spacer(Modifier.height(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = amount,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StyleCard(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.surface
            )
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FinishStep(
    name: String,
    currency: String,
    budgetText: String,
    onBudgetChange: (String) -> Unit,
    startDay: Int,
    onStartDayChange: (Int) -> Unit,
    useSample: Boolean,
    onUseSampleChange: (Boolean) -> Unit,
    style: BudgetStyle,
    budgetCents: Long,
    incomeCents: Long,
    onFinish: () -> Unit
) {
    val dayChips = listOf(1, 5, 15, 20, 25, 28)

    StepScaffold(
        title = if (name.isBlank()) "One last thing" else "Almost there, $name",
        subtitle = "Set the frame for your month, then you're in.",
        primaryLabel = "Start budgeting",
        onPrimary = onFinish,
        secondaryLabel = "Skip for now",
        onSecondary = onFinish
    ) {
        if (style == BudgetStyle.SIMPLE) {
            OutlinedTextField(
                value = budgetText,
                onValueChange = onBudgetChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Monthly budget (optional)") },
                prefix = {
                    Text(
                        text = Currencies.symbolOf(currency),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                placeholder = { Text(Money.placeholder(currency)) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                supportingText = {
                    Text(
                        text = if (budgetCents > 0L) {
                            "That's ${Money.format(budgetCents, currency)} a month to spend."
                        } else {
                            "Skip it and set it once you know your numbers."
                        }
                    )
                }
            )
            Spacer(Modifier.height(18.dp))
        }

        Text(
            text = "Your month starts on",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = if (startDay == 1) {
                "Calendar month. Change this if your salary lands mid-month."
            } else {
                "Cycle runs from the ${ordinal(startDay)} to the day before the next one."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            dayChips.forEach { day ->
                val selected = startDay == day
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        )
                        .clickable { onStartDayChange(day) }
                        .padding(horizontal = 16.dp, vertical = 9.dp)
                ) {
                    Text(
                        text = if (day == 1) "1st" else ordinal(day),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

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
                            text = "Four months of example spending to explore with.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Switch(checked = useSample, onCheckedChange = onUseSampleChange)
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        SummaryCard(
            style = style,
            budgetCents = budgetCents,
            incomeCents = incomeCents,
            currency = currency,
            startDay = startDay
        )
    }
}

/** Restates the choices so nothing is a surprise on the first screen. */
@Composable
private fun SummaryCard(
    style: BudgetStyle,
    budgetCents: Long,
    incomeCents: Long,
    currency: String,
    startDay: Int
) {
    val rows = buildList {
        add("Budget style" to if (style == BudgetStyle.RULE_50_30_20) "50:30:20" else "Simple")
        if (style == BudgetStyle.RULE_50_30_20 && incomeCents > 0L) {
            add("Monthly income" to Money.format(incomeCents, currency))
        }
        if (budgetCents > 0L) add("Monthly budget" to Money.format(budgetCents, currency))
        add("Month starts" to if (startDay == 1) "1st (calendar month)" else "${ordinal(startDay)} of each month")
    }
    LedgerCard(contentPadding = PaddingValues(14.dp)) {
        Text(
            text = "Your setup",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        rows.forEach { (label, value) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Your ledger stays on this device. You can change all of this in Settings.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun ordinal(day: Int): String = when {
    day % 100 in 11..13 -> "${day}th"
    day % 10 == 1 -> "${day}st"
    day % 10 == 2 -> "${day}nd"
    day % 10 == 3 -> "${day}rd"
    else -> "${day}th"
}