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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.ledgerly.app.data.Bucket
import com.ledgerly.app.data.BudgetStyle
import com.ledgerly.app.data.Categories
import com.ledgerly.app.ui.components.CategoryBadge
import com.ledgerly.app.ui.components.EmptyState
import com.ledgerly.app.ui.components.LedgerCard
import com.ledgerly.app.ui.components.MonthSwitcher
import com.ledgerly.app.ui.components.ProgressBar
import com.ledgerly.app.ui.components.SectionHeader
import com.ledgerly.app.ui.theme.ledgerColors
import com.ledgerly.app.util.BudgetMath
import com.ledgerly.app.util.BudgetPlan
import com.ledgerly.app.util.BudgetStatus
import com.ledgerly.app.util.Money
import com.ledgerly.app.util.MonthStats
import java.time.YearMonth
import kotlin.math.roundToInt

@Composable
fun BudgetsScreen(
    stats: MonthStats,
    budgets: Map<String, Long>,
    monthlyBudgetCents: Long,
    monthlyIncomeCents: Long,
    budgetStyle: BudgetStyle,
    currency: String,
    month: YearMonth,
    canGoNext: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onAddBudget: () -> Unit,
    onEditBudget: (String) -> Unit,
    onSetMonthlyBudget: () -> Unit,
    onSetIncome: () -> Unit
) {
    val statuses = remember(budgets, stats) { BudgetMath.budgetStatuses(budgets, stats) }
    val unbudgeted = remember(budgets, stats) { BudgetMath.unbudgetedSpend(stats, budgets) }
    val availableCategories = remember(budgets) {
        Categories.expense.filter { it.key !in budgets }
    }
    val income = remember(monthlyIncomeCents, stats) {
        BudgetMath.effectiveIncome(monthlyIncomeCents, stats)
    }
    val plan = remember(stats, income) { BudgetMath.plan(stats, income) }
    val ruleMode = budgetStyle == BudgetStyle.RULE_50_30_20

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Budgets",
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
            if (ruleMode) {
                item {
                    PlanCard(
                        plan = plan,
                        currency = currency,
                        onSetIncome = onSetIncome
                    )
                }
            } else {
                item {
                    MonthlyBudgetCard(
                        monthlyBudgetCents = monthlyBudgetCents,
                        spentCents = stats.spentCents,
                        currency = currency,
                        onSetMonthlyBudget = onSetMonthlyBudget
                    )
                }
            }

            item {
                SectionHeader(
                    title = "Category budgets",
                    actionLabel = if (availableCategories.isNotEmpty()) "Add" else null,
                    onAction = if (availableCategories.isNotEmpty()) onAddBudget else null
                )
            }

            if (statuses.isEmpty()) {
                item {
                    LedgerCard(contentPadding = PaddingValues(0.dp)) {
                        EmptyState(
                            emoji = "🎯",
                            title = "Give every dollar a job",
                            message = "Set limits for the categories you care about. You can change them any time.",
                            actionLabel = "Create a budget",
                            onAction = onAddBudget
                        )
                    }
                }
            } else {
                items(statuses.size, key = { statuses[it].categoryKey }) { index ->
                    BudgetCard(
                        status = statuses[index],
                        currency = currency,
                        onClick = { onEditBudget(statuses[index].categoryKey) }
                    )
                }
            }

            if (unbudgeted > 0L) {
                item {
                    LedgerCard(contentPadding = PaddingValues(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.Category,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Outside category budgets",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${Money.format(unbudgeted, currency)} spent in categories without a limit.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            if (statuses.isNotEmpty() && availableCategories.isNotEmpty()) {
                item {
                    OutlinedButton(
                        onClick = onAddBudget,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        contentPadding = PaddingValues(vertical = 14.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Add category budget", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            if (statuses.isNotEmpty() && availableCategories.isEmpty()) {
                item {
                    Text(
                        text = "Every category has a budget. Nice work. 🎉",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthlyBudgetCard(
    monthlyBudgetCents: Long,
    spentCents: Long,
    currency: String,
    onSetMonthlyBudget: () -> Unit
) {
    val hasOverall = monthlyBudgetCents > 0L
    val remaining = monthlyBudgetCents - spentCents
    val ratio = if (hasOverall) spentCents.toFloat() / monthlyBudgetCents else 0f
    val overallTint = when {
        !hasOverall -> MaterialTheme.colorScheme.primary
        remaining < 0 -> MaterialTheme.colorScheme.error
        ratio >= 0.85f -> MaterialTheme.ledgerColors.warning
        else -> MaterialTheme.colorScheme.primary
    }

    LedgerCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Monthly budget",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (hasOverall) Money.format(monthlyBudgetCents, currency)
                    else "Not set",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            TextButton(onClick = onSetMonthlyBudget) {
                Text(
                    text = if (hasOverall) "Edit" else "Set",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
        if (hasOverall) {
            Spacer(Modifier.height(12.dp))
            ProgressBar(progress = ratio, color = overallTint, height = 10.dp)
            Spacer(Modifier.height(9.dp))
            Row {
                Text(
                    text = "${Money.format(spentCents, currency)} spent",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (remaining >= 0) {
                        "${Money.format(remaining, currency)} left"
                    } else {
                        "${Money.format(-remaining, currency)} over"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = overallTint
                )
            }
        } else {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Set a total for the month and Ledgerly will track what's safe to spend each day.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PlanCard(
    plan: BudgetPlan,
    currency: String,
    onSetIncome: () -> Unit
) {
    LedgerCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "50:30:20 plan",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (plan.hasIncome) {
                        Money.format(plan.incomeCents, currency)
                    } else {
                        "No income yet"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            TextButton(onClick = onSetIncome) {
                Text(
                    text = if (plan.hasIncome) "Edit" else "Set",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        if (!plan.hasIncome) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Enter your monthly income and Ledgerly splits it 50% needs, 30% wants, 20% savings.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onSetIncome,
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text("Set monthly income", style = MaterialTheme.typography.labelLarge)
            }
            return@LedgerCard
        }

        Spacer(Modifier.height(4.dp))
        Text(
            text = BudgetMath.planVerdict(plan, currency),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(14.dp))

        plan.buckets.forEachIndexed { index, target ->
            if (index > 0) Spacer(Modifier.height(14.dp))
            BucketRow(
                label = "${target.bucket.share}% ${target.bucket.label}",
                tint = bucketTint(target.bucket),
                progress = target.progress,
                spentCents = target.spentCents,
                targetCents = target.targetCents,
                verdict = BudgetMath.bucketVerdict(target, currency),
                currency = currency
            )
        }

        Spacer(Modifier.height(14.dp))
        Text(
            text = "Savings counts when you log a transfer as a Savings expense.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BucketRow(
    label: String,
    tint: Color,
    progress: Float,
    spentCents: Long,
    targetCents: Long,
    verdict: String,
    currency: String
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = verdict,
                style = MaterialTheme.typography.labelMedium,
                color = tint
            )
        }
        Spacer(Modifier.height(7.dp))
        ProgressBar(progress = progress, color = tint, height = 7.dp)
        Spacer(Modifier.height(6.dp))
        Text(
            text = "${Money.format(spentCents, currency)} of ${Money.format(targetCents, currency)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun bucketTint(bucket: Bucket): Color = when (bucket) {
    Bucket.NEEDS -> MaterialTheme.colorScheme.primary
    Bucket.WANTS -> MaterialTheme.ledgerColors.warning
    Bucket.SAVINGS -> MaterialTheme.ledgerColors.success
}

@Composable
private fun BudgetCard(
    status: BudgetStatus,
    currency: String,
    onClick: () -> Unit
) {
    val category = Categories.of(status.categoryKey)
    val tint = when {
        status.isOver -> MaterialTheme.colorScheme.error
        status.isWarning -> MaterialTheme.ledgerColors.warning
        else -> MaterialTheme.ledgerColors.success
    }
    val percent = (status.rawProgress * 100).roundToInt()

    LedgerCard(onClick = onClick, contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryBadge(category = category, size = 42.dp, emojiSize = 19.sp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = category.label,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "$percent%",
                        style = MaterialTheme.typography.labelMedium,
                        color = tint
                    )
                }
                Spacer(Modifier.height(8.dp))
                ProgressBar(progress = status.progress, color = tint, height = 7.dp)
                Spacer(Modifier.height(7.dp))
                Row {
                    Text(
                        text = "${Money.format(status.spentCents, currency)} of ${
                            Money.format(status.limitCents, currency)
                        }",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = if (status.isOver) {
                            "Over by ${Money.format(-status.remainingCents, currency)}"
                        } else {
                            "${Money.format(status.remainingCents, currency)} left"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = tint
                    )
                }
            }
        }
    }
}
