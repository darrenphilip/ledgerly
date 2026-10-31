package com.ledgerly.app.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ledgerly.app.data.Categories
import com.ledgerly.app.data.Transaction
import com.ledgerly.app.ui.components.CategoryBadge
import com.ledgerly.app.ui.components.EmptyState
import com.ledgerly.app.ui.components.LedgerCard
import com.ledgerly.app.ui.components.MonthSwitcher
import com.ledgerly.app.ui.components.ProgressBar
import com.ledgerly.app.ui.components.SectionHeader
import com.ledgerly.app.ui.components.StatTile
import com.ledgerly.app.ui.components.TrendBarChart
import com.ledgerly.app.ui.theme.LedgerColors
import com.ledgerly.app.ui.theme.ledgerColors
import com.ledgerly.app.util.BudgetMath
import com.ledgerly.app.util.DateFormats
import com.ledgerly.app.util.Money
import com.ledgerly.app.util.MonthStats
import java.time.YearMonth
import kotlin.math.abs
import kotlin.math.roundToInt

private data class Highlight(
    val emoji: String,
    val title: String,
    val subtitle: String,
    val tint: Color
)

@Composable
fun InsightsScreen(
    transactions: List<Transaction>,
    month: YearMonth,
    stats: MonthStats,
    budgets: Map<String, Long>,
    monthlyBudgetCents: Long,
    currency: String,
    canGoNext: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onAddTransaction: () -> Unit
) {
    val previousMonth = remember(month) { month.minusMonths(1) }
    val previousStats = remember(transactions, previousMonth) {
        BudgetMath.summarize(transactions, previousMonth)
    }
    val trend = remember(transactions, month) { BudgetMath.trend(transactions, month, 6) }
    val extras = MaterialTheme.ledgerColors
    val highlights = buildHighlights(
        stats, previousStats, budgets, monthlyBudgetCents, month, currency, transactions, extras
    )
    val topCategory = remember(stats) {
        stats.byCategory.entries.maxByOrNull { it.value }?.key?.let { Categories.of(it) }
    }
    val breakdown = remember(stats) {
        stats.byCategory.entries.sortedByDescending { it.value }
    }
    val topAmount = breakdown.firstOrNull()?.value ?: 1L

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Insights",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(10.dp))
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            MonthSwitcher(
                month = month,
                canGoNext = canGoNext,
                onPrevious = onPreviousMonth,
                onNext = onNextMonth
            )
        }
        Spacer(Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (stats.isEmpty) {
                item {
                    LedgerCard(contentPadding = PaddingValues(0.dp)) {
                        EmptyState(
                            emoji = "📊",
                            title = "No insights for ${DateFormats.monthLabel(month)}",
                            message = "Add a few transactions and this page will fill up with trends and observations.",
                            actionLabel = "Add transaction",
                            onAction = onAddTransaction
                        )
                    }
                }
            } else {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatTile(
                            label = "Daily average",
                            value = Money.format(BudgetMath.dailyAverage(stats), currency),
                            icon = {
                                Icon(
                                    Icons.Rounded.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(17.dp)
                                )
                            },
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        StatTile(
                            label = "Top category",
                            value = topCategory?.label ?: "—",
                            icon = {
                                Text(
                                    text = topCategory?.emoji ?: "✨",
                                    fontSize = 17.sp
                                )
                            },
                            tint = topCategory?.color ?: MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (highlights.isNotEmpty()) {
                    item {
                        LedgerCard {
                            SectionHeader(title = "Worth knowing")
                            Spacer(Modifier.height(6.dp))
                            highlights.forEachIndexed { index, highlight ->
                                HighlightRow(highlight)
                                if (index < highlights.lastIndex) {
                                    Spacer(Modifier.height(12.dp))
                                }
                            }
                        }
                    }
                }

                item {
                    val total = trend.sumOf { it.spentCents }
                    LedgerCard {
                        Text(
                            text = "Spending trend",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${Money.format(total, currency)} over the last 6 months",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        TrendBarChart(
                            points = trend,
                            currency = currency,
                            highlight = month
                        )
                    }
                }

                item {
                    LedgerCard {
                        SectionHeader(title = "By category")
                        Spacer(Modifier.height(12.dp))
                        breakdown.forEachIndexed { index, entry ->
                            val category = Categories.of(entry.key)
                            val share = if (stats.spentCents > 0) {
                                entry.value.toFloat() / stats.spentCents
                            } else 0f
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CategoryBadge(category = category, size = 34.dp, emojiSize = 16.sp)
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = category.label,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = Money.format(entry.value, currency),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Spacer(Modifier.height(5.dp))
                                    ProgressBar(
                                        progress = entry.value.toFloat() / topAmount,
                                        color = category.color,
                                        height = 5.dp
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = "${(share * 100).roundToInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(32.dp)
                                )
                            }
                            if (index < breakdown.lastIndex) {
                                Spacer(Modifier.height(12.dp))
                            }
                        }
                    }
                }

                item {
                    LedgerCard(contentPadding = PaddingValues(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(extras.success.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("💡", fontSize = 18.sp)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Keep it going",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Log purchases as they happen — small entries add up to a clear picture.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
private fun HighlightRow(highlight: Highlight) {
    Row {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(highlight.tint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = highlight.emoji, fontSize = 17.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = highlight.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = highlight.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun buildHighlights(
    stats: MonthStats,
    previousStats: MonthStats,
    budgets: Map<String, Long>,
    monthlyBudgetCents: Long,
    month: YearMonth,
    currency: String,
    transactions: List<Transaction>,
    extras: LedgerColors
): List<Highlight> {
    val result = mutableListOf<Highlight>()
    val spent = stats.spentCents

    BudgetMath.percentChange(spent, previousStats.spentCents)?.let { change ->
        val percent = (abs(change) * 100).roundToInt()
        if (percent >= 3) {
            result += if (change < 0) {
                Highlight(
                    emoji = "📉",
                    title = "Spending is $percent% lower than last month",
                    subtitle = "You spent ${Money.format(spent, currency)} vs ${
                        Money.format(previousStats.spentCents, currency)
                    } last month.",
                    tint = extras.success
                )
            } else {
                Highlight(
                    emoji = "📈",
                    title = "Spending is $percent% higher than last month",
                    subtitle = "You spent ${Money.format(spent, currency)} vs ${
                        Money.format(previousStats.spentCents, currency)
                    } last month.",
                    tint = extras.warning
                )
            }
        }
    }

    val topEntry = stats.byCategory.entries.maxByOrNull { it.value }
    if (topEntry != null && spent > 0) {
        val category = Categories.of(topEntry.key)
        val share = (topEntry.value.toFloat() / spent * 100).roundToInt()
        result += Highlight(
            emoji = "🏆",
            title = "${category.label} leads your spending",
            subtitle = "$share% of everything you spent this month.",
            tint = category.color
        )
    }

    if (month == YearMonth.now() && monthlyBudgetCents > 0L) {
        val projected = BudgetMath.projectedSpend(stats)
        val difference = monthlyBudgetCents - projected
        if (difference >= 0) {
            result += Highlight(
                emoji = "🎯",
                title = "On pace to finish under budget",
                subtitle = "At this rate you'll spend about ${Money.format(projected, currency)}, keeping ${
                    Money.format(difference, currency)
                }.",
                tint = extras.success
            )
        } else {
            result += Highlight(
                emoji = "⚠️",
                title = "On pace to exceed your budget",
                subtitle = "Projected spend is ${Money.format(projected, currency)}, about ${
                    Money.format(-difference, currency)
                } over.",
                tint = extras.warning
            )
        }
    }

    val subscriptions = BudgetMath.subscriptionsTotal(transactions, month)
    if (subscriptions > 0L) {
        result += Highlight(
            emoji = "🔁",
            title = "Subscriptions add up to ${Money.format(subscriptions, currency)}",
            subtitle = "Recurring services you paid for this month.",
            tint = extras.warning
        )
    }

    val biggest = BudgetMath.biggestExpense(transactions, month)
    if (biggest != null) {
        val category = Categories.of(biggest.categoryKey)
        result += Highlight(
            emoji = "💸",
            title = "Biggest expense: ${biggest.note.ifBlank { category.label }}",
            subtitle = "${Money.format(biggest.amountCents, currency)} · ${
                DateFormats.fullDate(biggest.date)
            }",
            tint = category.color
        )
    }

    val statuses = BudgetMath.budgetStatuses(budgets, stats)
    val over = statuses.count { it.isOver }
    if (over > 0) {
        result += Highlight(
            emoji = "🚨",
            title = if (over == 1) "1 budget is over its limit" else "$over budgets are over their limits",
            subtitle = "Review them on the Budgets tab before the month ends.",
            tint = extras.warning
        )
    }

    return result.take(6)
}
