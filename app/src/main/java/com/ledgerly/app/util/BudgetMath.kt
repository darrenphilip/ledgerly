package com.ledgerly.app.util

import com.ledgerly.app.data.Transaction
import com.ledgerly.app.data.TxType
import java.time.LocalDate
import java.time.YearMonth

data class MonthStats(
    val month: YearMonth,
    val spentCents: Long,
    val incomeCents: Long,
    val byCategory: Map<String, Long>,
    val count: Int
) {
    val netCents: Long get() = incomeCents - spentCents
    val isEmpty: Boolean get() = count == 0
}

data class BudgetStatus(
    val categoryKey: String,
    val spentCents: Long,
    val limitCents: Long
) {
    val remainingCents: Long get() = limitCents - spentCents
    val progress: Float
        get() = if (limitCents <= 0) 0f else (spentCents.toFloat() / limitCents).coerceIn(0f, 1f)
    val rawProgress: Float
        get() = if (limitCents <= 0) 0f else spentCents.toFloat() / limitCents
    val isOver: Boolean get() = spentCents > limitCents
    val isWarning: Boolean get() = !isOver && rawProgress >= 0.8f
}

data class MonthPoint(
    val month: YearMonth,
    val spentCents: Long,
    val incomeCents: Long
)

data class CategorySlice(
    val categoryKey: String,
    val amountCents: Long,
    val share: Float
)

object BudgetMath {

    fun summarize(transactions: List<Transaction>, month: YearMonth): MonthStats {
        var spent = 0L
        var income = 0L
        var count = 0
        val byCategory = LinkedHashMap<String, Long>()

        for (tx in transactions) {
            if (YearMonth.from(tx.date) != month) continue
            count++
            if (tx.type == TxType.EXPENSE) {
                spent += tx.amountCents
                byCategory[tx.categoryKey] = (byCategory[tx.categoryKey] ?: 0L) + tx.amountCents
            } else {
                income += tx.amountCents
            }
        }

        return MonthStats(
            month = month,
            spentCents = spent,
            incomeCents = income,
            byCategory = byCategory.entries.sortedByDescending { it.value }.associate { it.key to it.value },
            count = count
        )
    }

    fun inMonth(transactions: List<Transaction>, month: YearMonth): List<Transaction> =
        transactions.filter { YearMonth.from(it.date) == month }

    fun categorySlices(stats: MonthStats, limit: Int = 5): List<CategorySlice> {
        if (stats.spentCents <= 0L) return emptyList()
        val sorted = stats.byCategory.entries.sortedByDescending { it.value }
        val top = sorted.take(limit)
        val rest = sorted.drop(limit)
        val slices = top.map {
            CategorySlice(it.key, it.value, it.value.toFloat() / stats.spentCents)
        }.toMutableList()
        if (rest.isNotEmpty()) {
            val otherTotal = rest.sumOf { it.value }
            slices += CategorySlice("other", otherTotal, otherTotal.toFloat() / stats.spentCents)
        }
        return slices
    }

    fun budgetStatuses(
        budgets: Map<String, Long>,
        stats: MonthStats
    ): List<BudgetStatus> = budgets.map { (key, limit) ->
        BudgetStatus(key, stats.byCategory[key] ?: 0L, limit)
    }.sortedWith(compareByDescending<BudgetStatus> { it.rawProgress }.thenByDescending { it.spentCents })

    fun unbudgetedSpend(stats: MonthStats, budgets: Map<String, Long>): Long =
        stats.byCategory.filterKeys { it !in budgets }.values.sum()

    fun daysLeftInMonth(month: YearMonth, today: LocalDate = LocalDate.now()): Int {
        if (month != YearMonth.from(today)) {
            return if (month.isAfter(YearMonth.from(today))) month.lengthOfMonth() else 0
        }
        return (month.lengthOfMonth() - today.dayOfMonth + 1).coerceAtLeast(0)
    }

    fun daysElapsedInMonth(month: YearMonth, today: LocalDate = LocalDate.now()): Int {
        return when {
            month == YearMonth.from(today) -> today.dayOfMonth
            month.isBefore(YearMonth.from(today)) -> month.lengthOfMonth()
            else -> 1
        }.coerceAtLeast(1)
    }

    fun dailyAverage(stats: MonthStats, today: LocalDate = LocalDate.now()): Long {
        val days = daysElapsedInMonth(stats.month, today)
        return stats.spentCents / days
    }

    fun safeToSpend(remainingCents: Long, month: YearMonth, today: LocalDate = LocalDate.now()): Long {
        if (remainingCents <= 0) return 0
        val days = daysLeftInMonth(month, today)
        if (days <= 0) return 0
        return remainingCents / days
    }

    /** Positive = spending increased, negative = spending decreased. Null when no baseline. */
    fun percentChange(current: Long, previous: Long): Float? {
        if (previous <= 0L) return null
        return (current - previous).toFloat() / previous
    }

    fun trend(
        transactions: List<Transaction>,
        endMonth: YearMonth,
        count: Int = 6
    ): List<MonthPoint> {
        val start = endMonth.minusMonths((count - 1).toLong())
        val byMonth = LinkedHashMap<YearMonth, LongArray>()
        for (i in 0 until count) {
            byMonth[start.plusMonths(i.toLong())] = longArrayOf(0L, 0L)
        }
        for (tx in transactions) {
            val m = YearMonth.from(tx.date)
            val bucket = byMonth[m] ?: continue
            if (tx.type == TxType.EXPENSE) bucket[0] += tx.amountCents else bucket[1] += tx.amountCents
        }
        return byMonth.map { (month, values) -> MonthPoint(month, values[0], values[1]) }
    }

    fun biggestExpense(transactions: List<Transaction>, month: YearMonth): Transaction? =
        transactions.asSequence()
            .filter { it.type == TxType.EXPENSE && YearMonth.from(it.date) == month }
            .maxByOrNull { it.amountCents }

    fun projectedSpend(stats: MonthStats, today: LocalDate = LocalDate.now()): Long {
        if (stats.month != YearMonth.from(today)) return stats.spentCents
        val elapsed = daysElapsedInMonth(stats.month, today)
        return (stats.spentCents.toDouble() / elapsed * stats.month.lengthOfMonth()).toLong()
    }

    fun subscriptionsTotal(transactions: List<Transaction>, month: YearMonth): Long =
        transactions.filter {
            it.type == TxType.EXPENSE &&
                it.categoryKey == "subscriptions" &&
                YearMonth.from(it.date) == month
        }.sumOf { it.amountCents }
}
