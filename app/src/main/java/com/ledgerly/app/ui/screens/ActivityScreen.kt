package com.ledgerly.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ledgerly.app.data.Categories
import com.ledgerly.app.data.Transaction
import com.ledgerly.app.data.TxType
import com.ledgerly.app.ui.components.EmptyState
import com.ledgerly.app.ui.components.MonthSwitcher
import com.ledgerly.app.ui.components.SwipeableTransactionRow
import com.ledgerly.app.ui.theme.ledgerColors
import com.ledgerly.app.util.BudgetMath
import com.ledgerly.app.util.DateFormats
import com.ledgerly.app.util.Money
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun ActivityScreen(
    transactions: List<Transaction>,
    month: YearMonth,
    currency: String,
    canGoNext: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onAddTransaction: () -> Unit,
    onTransactionClick: (Transaction) -> Unit,
    onDeleteTransaction: (Transaction) -> Unit
) {
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var typeFilter by rememberSaveable { mutableStateOf("all") }
    var categoryFilter by rememberSaveable { mutableStateOf<String?>(null) }

    val monthTransactions = remember(transactions, month) {
        BudgetMath.inMonth(transactions, month)
    }
    val availableCategories = remember(monthTransactions, typeFilter) {
        monthTransactions
            .filter { typeFilter == "all" || it.type.raw == typeFilter }
            .map { it.categoryKey }
            .distinct()
            .map { Categories.of(it) }
            .sortedBy { it.label }
    }
    LaunchedEffect(typeFilter, availableCategories) {
        if (categoryFilter != null && availableCategories.none { it.key == categoryFilter }) {
            categoryFilter = null
        }
    }

    val filtered = remember(monthTransactions, typeFilter, categoryFilter, query) {
        val q = query.trim()
        monthTransactions.filter { tx ->
            (typeFilter == "all" || tx.type.raw == typeFilter) &&
                (categoryFilter == null || tx.categoryKey == categoryFilter) &&
                (q.isBlank() ||
                    tx.note.contains(q, ignoreCase = true) ||
                    Categories.of(tx.categoryKey).label.contains(q, ignoreCase = true))
        }.sortedWith(compareByDescending<Transaction> { it.date }.thenByDescending { it.createdAt })
    }
    val groups = remember(filtered) {
        filtered.groupBy { it.date }
            .toSortedMap(compareByDescending<LocalDate> { it })
    }
    val spentTotal = remember(filtered) {
        filtered.filter { it.type == TxType.EXPENSE }.sumOf { it.amountCents }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Activity",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = {
                    searchOpen = !searchOpen
                    if (!searchOpen) query = ""
                }
            ) {
                Icon(
                    imageVector = if (searchOpen) Icons.Rounded.Close else Icons.Rounded.Search,
                    contentDescription = if (searchOpen) "Close search" else "Search transactions",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            MonthSwitcher(
                month = month,
                canGoNext = canGoNext,
                onPrevious = onPreviousMonth,
                onNext = onNextMonth
            )
        }

        AnimatedVisibility(
            visible = searchOpen,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                placeholder = { Text("Search notes or categories") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TypeChip("All", typeFilter == "all") { typeFilter = "all" }
            TypeChip("Expenses", typeFilter == "expense") { typeFilter = "expense" }
            TypeChip("Income", typeFilter == "income") { typeFilter = "income" }
        }

        if (availableCategories.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(availableCategories, key = { it.key }) { category ->
                    val selected = categoryFilter == category.key
                    FilterChip(
                        selected = selected,
                        onClick = { categoryFilter = if (selected) null else category.key },
                        label = {
                            Text(
                                text = "${category.emoji} ${category.label}",
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedContainerColor = category.color.copy(alpha = 0.2f),
                            selectedLabelColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        Text(
            text = buildString {
                append(filtered.size)
                append(if (filtered.size == 1) " transaction" else " transactions")
                if (spentTotal > 0L) {
                    append(" · ")
                    append(Money.format(spentTotal, currency))
                    append(" spent")
                }
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(6.dp))

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (monthTransactions.isEmpty()) {
                    EmptyState(
                        emoji = "🧾",
                        title = "Nothing in ${DateFormats.monthLabel(month)}",
                        message = "Track an expense or income and it will show up here.",
                        actionLabel = "Add transaction",
                        onAction = onAddTransaction
                    )
                } else {
                    EmptyState(
                        emoji = "🔍",
                        title = "No matches",
                        message = "Try a different search, type, or category filter.",
                        actionLabel = "Clear filters",
                        onAction = {
                            query = ""
                            typeFilter = "all"
                            categoryFilter = null
                        }
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                groups.forEach { (date, dayTransactions) ->
                    item(key = "header-$date") {
                        DayHeader(date = date, transactions = dayTransactions, currency = currency)
                    }
                    items(dayTransactions, key = { it.id }) { tx ->
                        SwipeableTransactionRow(
                            transaction = tx,
                            currency = currency,
                            onClick = { onTransactionClick(tx) },
                            onDelete = { onDeleteTransaction(tx) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TypeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        shape = CircleShape,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surface,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

@Composable
private fun DayHeader(
    date: LocalDate,
    transactions: List<Transaction>,
    currency: String
) {
    val net = transactions.sumOf {
        if (it.type == TxType.INCOME) it.amountCents else -it.amountCents
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = DateFormats.relativeDay(date),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        if (net != 0L) {
            Text(
                text = Money.format(net, currency, showPlus = net > 0),
                style = MaterialTheme.typography.labelSmall,
                color = if (net > 0) MaterialTheme.ledgerColors.income
                else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
