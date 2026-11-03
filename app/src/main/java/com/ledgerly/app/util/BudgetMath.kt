package com.ledgerly.app.util

import com.ledgerly.app.data.Bucket
import com.ledgerly.app.data.Categories
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

/** A 50/30/20 income split. The three parts always sum back to the income. */
data class IncomeSplit(
    val needsCents: Long,
    val wantsCents: Long,
    val savingsCents: Long
) {
    val totalCents: Long get() = needsCents + wantsCents + savingsCents

    fun forBucket(bucket: Bucket): Long = when (bucket) {
        Bucket.NEEDS -> needsCents
        Bucket.WANTS -> wantsCents
        Bucket.SAVINGS -> savingsCents
    }
}

data class BucketTarget(
    val bucket: Bucket,
    val targetCents: Long,
    val spentCents: Long
) {
    val remainingCents: Long get() = targetCents - spentCents
    val isOver: Boolean get() = spentCents > targetCents
    val rawProgress: Float
        get() = if (targetCents <= 0L) 0f else spentCents.toFloat() / targetCents
    val progress: Float get() = rawProgress.coerceIn(0f, 1f)
}

data class BudgetPlan(
    val incomeCents: Long,
    val buckets: List<BucketTarget>
) {
    val totalTargetCents: Long get() = buckets.sumOf { it.targetCents }
    val totalSpentCents: Long get() = buckets.sumOf { it.spentCents }
    val overCents: Long get() = buckets.filter { it.isOver }.sumOf { -it.remainingCents }
    val hasIncome: Boolean get() = incomeCents > 0L
    val isEmpty: Boolean get() = totalSpentCents == 0L
}

object BudgetMath {

    /**
     * Totals for [month]. [startDay] of 1 means a calendar month; anything else makes the cycle
     * run from that day, matching `Cycles`.
     */
    fun summarize(
        transactions: List<Transaction>,
        month: YearMonth,
        startDay: Int = 1
    ): MonthStats {
        var spent = 0L
        var income = 0L
        var count = 0
        val byCategory = LinkedHashMap<String, Long>()

        for (tx in transactions) {
            if (!Cycles.contains(tx.date, month, startDay)) continue
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

    fun inMonth(
        transactions: List<Transaction>,
        month: YearMonth,
        startDay: Int = 1
    ): List<Transaction> = transactions.filter { Cycles.contains(it.date, month, startDay) }

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

    fun daysLeftInMonth(month: YearMonth, startDay: Int = 1, today: LocalDate = LocalDate.now()): Int =
        Cycles.daysLeft(month, startDay, today)

    fun daysElapsedInMonth(month: YearMonth, startDay: Int = 1, today: LocalDate = LocalDate.now()): Int =
        Cycles.daysElapsed(month, startDay, today).coerceAtLeast(1)

    fun dailyAverage(stats: MonthStats, startDay: Int = 1, today: LocalDate = LocalDate.now()): Long {
        val days = daysElapsedInMonth(stats.month, startDay, today)
        return stats.spentCents / days
    }

    fun safeToSpend(
        remainingCents: Long,
        month: YearMonth,
        startDay: Int = 1,
        today: LocalDate = LocalDate.now()
    ): Long {
        if (remainingCents <= 0) return 0
        val days = daysLeftInMonth(month, startDay, today)
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
        count: Int = 6,
        startDay: Int = 1
    ): List<MonthPoint> {
        val start = endMonth.minusMonths((count - 1).toLong())
        val byMonth = LinkedHashMap<YearMonth, LongArray>()
        for (i in 0 until count) {
            byMonth[start.plusMonths(i.toLong())] = longArrayOf(0L, 0L)
        }
        for (tx in transactions) {
            val m = Cycles.cycleOf(tx.date, startDay)
            val bucket = byMonth[m] ?: continue
            if (tx.type == TxType.EXPENSE) bucket[0] += tx.amountCents else bucket[1] += tx.amountCents
        }
        return byMonth.map { (month, values) -> MonthPoint(month, values[0], values[1]) }
    }

    fun biggestExpense(
        transactions: List<Transaction>,
        month: YearMonth,
        startDay: Int = 1
    ): Transaction? = transactions.asSequence()
        .filter { it.type == TxType.EXPENSE && Cycles.contains(it.date, month, startDay) }
        .maxByOrNull { it.amountCents }

    fun projectedSpend(stats: MonthStats, startDay: Int = 1, today: LocalDate = LocalDate.now()): Long {
        if (!Cycles.contains(today, stats.month, startDay)) return stats.spentCents
        val elapsed = daysElapsedInMonth(stats.month, startDay, today)
        val length = Cycles.length(stats.month, startDay)
        return (stats.spentCents.toDouble() / elapsed * length).toLong()
    }

    fun subscriptionsTotal(
        transactions: List<Transaction>,
        month: YearMonth,
        startDay: Int = 1
    ): Long = transactions.filter {
        it.type == TxType.EXPENSE &&
            it.categoryKey == "subscriptions" &&
            Cycles.contains(it.date, month, startDay)
    }.sumOf { it.amountCents }

    /**
     * Splits monthly income into needs/wants/savings. Each part is the exact share rounded
     * down, and savings takes the remainder, so the parts always add up to the income.
     */
    fun splitIncome(incomeCents: Long): IncomeSplit {
        if (incomeCents <= 0L) return IncomeSplit(0L, 0L, 0L)
        val needs = incomeCents * Bucket.NEEDS.share / 100
        val wants = incomeCents * Bucket.WANTS.share / 100
        return IncomeSplit(needs, wants, incomeCents - needs - wants)
    }

    /** Month spending grouped by 50:30:20 bucket. Every bucket is present, zero included. */
    fun bucketSpend(stats: MonthStats): Map<Bucket, Long> {
        val totals = Bucket.entries.associateWith { 0L }.toMutableMap()
        for ((categoryKey, amount) in stats.byCategory) {
            val bucket = Categories.bucketOf(categoryKey)
            totals[bucket] = (totals[bucket] ?: 0L) + amount
        }
        return totals
    }

    fun plan(stats: MonthStats, incomeCents: Long): BudgetPlan {
        val split = splitIncome(incomeCents)
        val spend = bucketSpend(stats)
        return BudgetPlan(
            incomeCents = incomeCents,
            buckets = Bucket.entries.map { bucket ->
                BucketTarget(
                    bucket = bucket,
                    targetCents = split.forBucket(bucket),
                    spentCents = spend[bucket] ?: 0L
                )
            }
        )
    }

    /** Income typed in settings wins; otherwise the income logged this month is the baseline. */
    fun effectiveIncome(settingsIncomeCents: Long, stats: MonthStats): Long =
        if (settingsIncomeCents > 0L) settingsIncomeCents else stats.incomeCents

    /** One-line verdict for a bucket row, e.g. "Rp 120.000 left". */
    fun bucketVerdict(target: BucketTarget, currency: String): String = when {
        target.targetCents <= 0L -> "No target"
        target.isOver -> "${Money.format(-target.remainingCents, currency)} over"
        target.spentCents == 0L -> "Nothing yet"
        else -> "${Money.format(target.remainingCents, currency)} left"
    }

    fun planVerdict(plan: BudgetPlan, currency: String): String = when {
        !plan.hasIncome -> "Set your monthly income"
        plan.isEmpty -> "Nothing spent yet"
        plan.overCents > 0L -> "${Money.format(plan.overCents, currency)} over across buckets"
        else -> "On track"
    }
}
