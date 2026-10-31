package com.ledgerly.app

import com.ledgerly.app.data.Bucket
import com.ledgerly.app.data.Transaction
import com.ledgerly.app.data.TxType
import com.ledgerly.app.util.BudgetMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs

class BudgetMathTest {

    private val month = YearMonth.of(2026, 10)
    private val today = LocalDate.of(2026, 10, 10)

    private val transactions = listOf(
        tx(1, TxType.EXPENSE, 5_000, "food", LocalDate.of(2026, 10, 2)),
        tx(2, TxType.EXPENSE, 3_000, "food", LocalDate.of(2026, 10, 3)),
        tx(3, TxType.EXPENSE, 10_000, "home", LocalDate.of(2026, 10, 4)),
        tx(4, TxType.EXPENSE, 2_000, "transport", LocalDate.of(2026, 10, 5)),
        tx(5, TxType.INCOME, 50_000, "salary", LocalDate.of(2026, 10, 1)),
        tx(6, TxType.EXPENSE, 1_000, "food", LocalDate.of(2026, 9, 30)),
        tx(7, TxType.EXPENSE, 7_000, "shopping", LocalDate.of(2026, 10, 20))
    )

    private fun tx(
        id: Long,
        type: TxType,
        cents: Long,
        category: String,
        date: LocalDate
    ): Transaction = Transaction(
        id = id,
        type = type,
        amountCents = cents,
        categoryKey = category,
        note = "",
        date = date
    )

    @Test
    fun summarize_only_counts_selected_month() {
        val stats = BudgetMath.summarize(transactions, month)
        assertEquals(27_000L, stats.spentCents)
        assertEquals(50_000L, stats.incomeCents)
        assertEquals(23_000L, stats.netCents)
        assertEquals(6, stats.count)
        assertEquals(8_000L, stats.byCategory["food"])
        assertEquals(7_000L, stats.byCategory["shopping"])
    }

    @Test
    fun category_slices_fold_the_tail_into_other() {
        val stats = BudgetMath.summarize(transactions, month)
        val slices = BudgetMath.categorySlices(stats, limit = 2)
        assertEquals(3, slices.size)
        assertEquals("home", slices[0].categoryKey)
        assertEquals("food", slices[1].categoryKey)
        assertEquals("other", slices[2].categoryKey)
        assertEquals(9_000L, slices[2].amountCents) // transport + shopping
        assertEquals(1f, slices.sumOf { it.share.toDouble() }.toFloat(), 0.001f)
    }

    @Test
    fun budget_statuses_detect_over_and_warning() {
        val stats = BudgetMath.summarize(transactions, month)
        val statuses = BudgetMath.budgetStatuses(
            mapOf("food" to 10_000L, "home" to 5_000L, "transport" to 2_500L),
            stats
        )
        val home = statuses.first { it.categoryKey == "home" }
        val food = statuses.first { it.categoryKey == "food" }
        val transport = statuses.first { it.categoryKey == "transport" }
        assertTrue(home.isOver)
        assertTrue(food.isWarning)
        assertEquals(2_000L, food.remainingCents)
        assertEquals(0.8f, transport.rawProgress, 0.0001f)
    }

    @Test
    fun unbudgeted_spend_excludes_budgeted_categories() {
        val stats = BudgetMath.summarize(transactions, month)
        assertEquals(9_000L, BudgetMath.unbudgetedSpend(stats, mapOf("food" to 1L, "home" to 1L)))
    }

    @Test
    fun days_left_includes_today() {
        assertEquals(22, BudgetMath.daysLeftInMonth(month, today))
        assertEquals(0, BudgetMath.daysLeftInMonth(YearMonth.of(2026, 9), today))
    }

    @Test
    fun daily_average_and_safe_to_spend() {
        val stats = BudgetMath.summarize(transactions, month)
        assertEquals(2_700L, BudgetMath.dailyAverage(stats, today))
        assertEquals(1_363L, BudgetMath.safeToSpend(30_000L, month, today))
        assertEquals(0L, BudgetMath.safeToSpend(-500L, month, today))
    }

    @Test
    fun percent_change_handles_missing_baseline() {
        assertNull(BudgetMath.percentChange(100L, 0L))
        assertEquals(-0.5f, BudgetMath.percentChange(50L, 100L)!!, 0.0001f)
        assertEquals(0.25f, BudgetMath.percentChange(125L, 100L)!!, 0.0001f)
    }

    @Test
    fun trend_has_six_buckets_and_totals() {
        val trend = BudgetMath.trend(transactions, month, 6)
        assertEquals(6, trend.size)
        assertEquals(YearMonth.of(2026, 5), trend.first().month)
        assertEquals(month, trend.last().month)
        assertEquals(27_000L, trend.last().spentCents)
        assertEquals(1_000L, trend[4].spentCents)
    }

    @Test
    fun biggest_expense_and_projection() {
        assertEquals(10_000L, BudgetMath.biggestExpense(transactions, month)!!.amountCents)
        val stats = BudgetMath.summarize(transactions, month)
        // 27,000 over 10 elapsed days -> 83,700 projected over 31 days
        assertEquals(83_700L, BudgetMath.projectedSpend(stats, today))
    }

    @Test
    fun subscriptions_total() {
        val subs = listOf(
            tx(20, TxType.EXPENSE, 1_599, "subscriptions", LocalDate.of(2026, 10, 3)),
            tx(21, TxType.EXPENSE, 1_099, "subscriptions", LocalDate.of(2026, 10, 9)),
            tx(22, TxType.EXPENSE, 500, "food", LocalDate.of(2026, 10, 9))
        )
        assertEquals(2_698L, BudgetMath.subscriptionsTotal(subs, month))
    }

    @Test
    fun split_income_is_exactly_fifty_thirty_twenty() {
        val split = BudgetMath.splitIncome(1_000_000L)
        assertEquals(500_000L, split.needsCents)
        assertEquals(300_000L, split.wantsCents)
        assertEquals(200_000L, split.savingsCents)
        assertEquals(1_000_000L, split.totalCents)
    }

    @Test
    fun split_income_rounds_to_whole_cents_that_sum_back() {
        val split = BudgetMath.splitIncome(101L)
        assertEquals(101L, split.totalCents)
        assertTrue(abs(split.needsCents - 50.5) <= 1.0)
        assertTrue(abs(split.wantsCents - 30.3) <= 1.0)
        assertTrue(abs(split.savingsCents - 20.2) <= 1.0)
    }

    @Test
    fun split_income_without_income_is_zero() {
        assertEquals(0L, BudgetMath.splitIncome(0L).totalCents)
        assertEquals(0L, BudgetMath.splitIncome(-500L).totalCents)
    }

    @Test
    fun bucket_spend_groups_categories() {
        val stats = BudgetMath.summarize(transactions, month)
        val spend = BudgetMath.bucketSpend(stats)
        // needs: home 10_000 + transport 2_000, wants: food 8_000 + shopping 7_000
        assertEquals(12_000L, spend[Bucket.NEEDS])
        assertEquals(15_000L, spend[Bucket.WANTS])
        assertEquals(0L, spend[Bucket.SAVINGS])
    }

    @Test
    fun plan_carries_targets_and_spending_per_bucket() {
        val stats = BudgetMath.summarize(transactions, month)
        val plan = BudgetMath.plan(stats, 100_000L)
        assertEquals(3, plan.buckets.size)
        assertEquals(100_000L, plan.totalTargetCents)
        assertEquals(27_000L, plan.totalSpentCents)
        val needs = plan.buckets.first { it.bucket == Bucket.NEEDS }
        assertEquals(50_000L, needs.targetCents)
        assertEquals(12_000L, needs.spentCents)
        assertFalse(needs.isOver)
        assertEquals(0L, plan.overCents)
    }

    @Test
    fun plan_flags_overspent_buckets() {
        val stats = BudgetMath.summarize(transactions, month)
        // 20_000 income -> needs 10_000 (spent 12_000), wants 6_000 (spent 15_000)
        val plan = BudgetMath.plan(stats, 20_000L)
        val wants = plan.buckets.first { it.bucket == Bucket.WANTS }
        assertTrue(wants.isOver)
        assertEquals(9_000L, -wants.remainingCents)
        assertEquals(11_000L, plan.overCents)
    }

    @Test
    fun savings_expense_feeds_the_savings_bucket() {
        val withSavings = transactions +
            tx(30, TxType.EXPENSE, 5_000, "savings", LocalDate.of(2026, 10, 6))
        val stats = BudgetMath.summarize(withSavings, month)
        assertEquals(5_000L, BudgetMath.bucketSpend(stats)[Bucket.SAVINGS])

        val savings = BudgetMath.plan(stats, 25_000L)
            .buckets.first { it.bucket == Bucket.SAVINGS }
        assertEquals(5_000L, savings.targetCents)
        assertEquals(5_000L, savings.spentCents)
        assertEquals(0L, savings.remainingCents)
        assertFalse(savings.isOver)
    }

    @Test
    fun effective_income_prefers_settings_then_logged_income() {
        val stats = BudgetMath.summarize(transactions, month)
        assertEquals(90_000L, BudgetMath.effectiveIncome(90_000L, stats))
        assertEquals(50_000L, BudgetMath.effectiveIncome(0L, stats))
    }

    @Test
    fun verdicts_read_plainly() {
        val stats = BudgetMath.summarize(transactions, month)
        val tight = BudgetMath.plan(stats, 20_000L)
        assertEquals(
            "Rp 90 over",
            BudgetMath.bucketVerdict(tight.buckets.first { it.bucket == Bucket.WANTS }, "IDR")
        )
        assertEquals("Rp 110 over across buckets", BudgetMath.planVerdict(tight, "IDR"))
        assertEquals("On track", BudgetMath.planVerdict(BudgetMath.plan(stats, 200_000L), "IDR"))
        assertEquals(
            "Set your monthly income",
            BudgetMath.planVerdict(BudgetMath.plan(stats, 0L), "IDR")
        )
    }
}
