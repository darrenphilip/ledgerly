package com.ledgerly.app.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ledgerly.app.data.Categories
import com.ledgerly.app.data.Transaction
import com.ledgerly.app.ui.components.CategoryBadge
import com.ledgerly.app.ui.components.DonutChart
import com.ledgerly.app.ui.components.DonutSegment
import com.ledgerly.app.ui.components.EmptyState
import com.ledgerly.app.ui.components.LedgerCard
import com.ledgerly.app.ui.components.MonthSwitcher
import com.ledgerly.app.ui.components.ProgressBar
import com.ledgerly.app.ui.components.SectionHeader
import com.ledgerly.app.ui.components.StatTile
import com.ledgerly.app.ui.components.TransactionRow
import com.ledgerly.app.ui.theme.ledgerColors
import com.ledgerly.app.util.BudgetMath
import com.ledgerly.app.util.DateFormats
import com.ledgerly.app.util.Money
import com.ledgerly.app.util.MonthStats
import java.time.LocalTime
import java.time.YearMonth
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    transactions: List<Transaction>,
    month: YearMonth,
    stats: MonthStats,
    budgets: Map<String, Long>,
    monthlyBudgetCents: Long,
    currency: String,
    name: String,
    isCurrentMonth: Boolean,
    canGoNext: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSettings: () -> Unit,
    onSeeAll: () -> Unit,
    onAddTransaction: () -> Unit,
    onLoadSample: () -> Unit,
    onOpenBudgets: () -> Unit,
    onOpenInsights: () -> Unit,
    onSetBudget: () -> Unit,
    onTransactionClick: (Transaction) -> Unit
) {
    val statuses = remember(budgets, stats) { BudgetMath.budgetStatuses(budgets, stats) }
    val alerts = remember(statuses) { statuses.filter { it.isOver || it.isWarning } }
    val slices = remember(stats) { BudgetMath.categorySlices(stats) }
    val recent = remember(transactions, month) {
        transactions.asSequence()
            .filter { YearMonth.from(it.date) == month }
            .sortedWith(compareByDescending<Transaction> { it.date }.thenByDescending { it.createdAt })
            .take(5)
            .toList()
    }
    val greeting = remember {
        when (LocalTime.now().hour) {
            in 5..11 -> "Good morning"
            in 12..17 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (name.isBlank()) "Welcome back" else "Hi, $name",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    IconButton(onClick = onSettings) {
                        Icon(
                            Icons.Rounded.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                MonthSwitcher(
                    month = month,
                    canGoNext = canGoNext,
                    onPrevious = onPreviousMonth,
                    onNext = onNextMonth
                )
            }
        }

        item {
            HeroCard(
                stats = stats,
                month = month,
                monthlyBudgetCents = monthlyBudgetCents,
                currency = currency,
                isCurrentMonth = isCurrentMonth,
                onSetBudget = onSetBudget
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(
                    label = "Income",
                    value = Money.format(stats.incomeCents, currency),
                    icon = {
                        Icon(
                            Icons.Rounded.ArrowDownward,
                            contentDescription = null,
                            tint = MaterialTheme.ledgerColors.income,
                            modifier = Modifier.size(17.dp)
                        )
                    },
                    tint = MaterialTheme.ledgerColors.income,
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = "Spent",
                    value = Money.format(stats.spentCents, currency),
                    icon = {
                        Icon(
                            Icons.Rounded.ArrowUpward,
                            contentDescription = null,
                            tint = MaterialTheme.ledgerColors.warning,
                            modifier = Modifier.size(17.dp)
                        )
                    },
                    tint = MaterialTheme.ledgerColors.warning,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (alerts.isNotEmpty()) {
            item {
                BudgetAlertCard(
                    alerts = alerts,
                    currency = currency,
                    onClick = onOpenBudgets
                )
            }
        }

        if (transactions.isEmpty()) {
            item {
                LedgerCard(contentPadding = PaddingValues(0.dp)) {
                    EmptyState(
                        emoji = "👋",
                        title = "Your money, minus the mystery",
                        message = "Add your first transaction, or load a few months of sample data to explore Ledgerly.",
                        actionLabel = "Add transaction",
                        onAction = onAddTransaction,
                        secondaryLabel = "Load sample data",
                        onSecondary = onLoadSample
                    )
                }
            }
        } else {
            item {
                BreakdownCard(
                    stats = stats,
                    slices = slices,
                    currency = currency,
                    month = month,
                    onOpenInsights = onOpenInsights
                )
            }

            item {
                Column {
                    SectionHeader(title = "Recent", actionLabel = "See all", onAction = onSeeAll)
                    Spacer(Modifier.height(8.dp))
                    if (recent.isEmpty()) {
                        LedgerCard(contentPadding = PaddingValues(0.dp)) {
                            EmptyState(
                                emoji = "🗓️",
                                title = "Nothing this month",
                                message = "No transactions recorded in ${DateFormats.monthLabel(month)}.",
                                actionLabel = "Add transaction",
                                onAction = onAddTransaction
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            recent.forEach { tx ->
                                TransactionRow(
                                    transaction = tx,
                                    currency = currency,
                                    onClick = { onTransactionClick(tx) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroCard(
    stats: MonthStats,
    month: YearMonth,
    monthlyBudgetCents: Long,
    currency: String,
    isCurrentMonth: Boolean,
    onSetBudget: () -> Unit
) {
    val hasBudget = monthlyBudgetCents > 0L
    val remaining = monthlyBudgetCents - stats.spentCents
    val isOver = hasBudget && remaining < 0L
    val ratio = if (hasBudget) stats.spentCents.toFloat() / monthlyBudgetCents else 0f
    val daysLeft = BudgetMath.daysLeftInMonth(month)
    val safe = BudgetMath.safeToSpend(remaining, month)
    val extras = MaterialTheme.ledgerColors
    val shape = RoundedCornerShape(28.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(listOf(extras.heroStart, extras.heroEnd)))
    ) {
        Box(
            modifier = Modifier
                .size(190.dp)
                .align(Alignment.TopEnd)
                .offset(x = 66.dp, y = -76.dp)
                .background(Color.White.copy(alpha = 0.07f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(120.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 46.dp, y = 52.dp)
                .background(Color.White.copy(alpha = 0.05f), CircleShape)
        )

        Column(modifier = Modifier.padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = when {
                        !hasBudget -> "Spent this month"
                        isOver -> "Over budget"
                        else -> "Left to spend"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.78f),
                    modifier = Modifier.weight(1f)
                )
                if (isCurrentMonth && daysLeft > 0) {
                    Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.16f)) {
                        Text(
                            text = "$daysLeft day${if (daysLeft == 1) "" else "s"} left",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = Money.format(
                    if (hasBudget) abs(remaining) else stats.spentCents,
                    currency
                ),
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(14.dp))

            if (hasBudget) {
                ProgressBar(
                    progress = ratio,
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.22f),
                    height = 8.dp
                )
                Spacer(Modifier.height(10.dp))
                Row {
                    Text(
                        text = "${Money.format(stats.spentCents, currency)} of ${
                            Money.format(monthlyBudgetCents, currency)
                        }",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.82f),
                        modifier = Modifier.weight(1f)
                    )
                    if (isCurrentMonth && remaining > 0) {
                        Text(
                            text = "${Money.format(safe, currency)}/day safe",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.82f)
                        )
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Set a monthly budget to see what's safe to spend.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(12.dp))
                    Button(
                        onClick = onSetBudget,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = extras.heroStart
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("Set", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun BudgetAlertCard(
    alerts: List<com.ledgerly.app.util.BudgetStatus>,
    currency: String,
    onClick: () -> Unit
) {
    val worst = alerts.maxByOrNull { it.rawProgress } ?: return
    val category = Categories.of(worst.categoryKey)
    val extras = MaterialTheme.ledgerColors
    val tint = if (worst.isOver) MaterialTheme.colorScheme.error else extras.warning
    val title = if (worst.isOver) "Budget exceeded" else "Budget alert"
    val detail = if (worst.isOver) {
        "${category.label} is over by ${Money.format(-worst.remainingCents, currency)}"
    } else {
        "${category.label} is at ${(worst.rawProgress * 100).roundToInt()}% of its budget"
    }
    val extraCount = alerts.size - 1

    LedgerCard(onClick = onClick, contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.WarningAmber,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(21.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = detail + if (extraCount > 0) " · +$extraCount more" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun BreakdownCard(
    stats: MonthStats,
    slices: List<com.ledgerly.app.util.CategorySlice>,
    currency: String,
    month: YearMonth,
    onOpenInsights: () -> Unit
) {
    LedgerCard {
        SectionHeader(
            title = "Where it went",
            actionLabel = "Insights",
            onAction = onOpenInsights
        )
        if (stats.spentCents <= 0L) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "No spending recorded for ${DateFormats.monthLabel(month)}.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                DonutChart(
                    segments = slices.map {
                        DonutSegment(Categories.of(it.categoryKey).color, it.amountCents)
                    },
                    size = 136.dp,
                    strokeWidth = 16.dp
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Spent",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = Money.compact(stats.spentCents, currency),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Spacer(Modifier.width(18.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    slices.take(4).forEach { slice ->
                        val category = Categories.of(slice.categoryKey)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(category.color)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = category.label,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = Money.compact(slice.amountCents, currency),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    if (slices.size > 4) {
                        Text(
                            text = "+${slices.size - 4} more",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
