package com.ledgerly.app

import com.ledgerly.app.data.Bucket
import com.ledgerly.app.data.Categories
import com.ledgerly.app.data.Category
import com.ledgerly.app.data.TxType
import com.ledgerly.app.data.Transaction
import com.ledgerly.app.util.BudgetMath
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class CategoriesTest {

    @After
    fun tearDown() {
        // Categories is a global catalogue; leave no custom rows behind for other tests.
        Categories.load(emptyList())
    }

    @Test
    fun `built-in categories carry a bucket`() {
        assertEquals(Bucket.NEEDS, Categories.of("groceries").bucket)
        assertEquals(Bucket.WANTS, Categories.of("food").bucket)
        assertEquals(Bucket.SAVINGS, Categories.of("savings").bucket)
    }

    @Test
    fun `income categories have no bucket`() {
        assertNull(Categories.of("salary").bucket)
        assertEquals(TxType.INCOME, Categories.of("salary").type)
    }

    @Test
    fun `a custom category keeps the bucket it was given`() {
        Categories.load(
            listOf(
                Category(
                    key = "gym",
                    label = "Gym",
                    emoji = "🏋️",
                    color = androidx.compose.ui.graphics.Color(0xFF22C55E),
                    bucket = Bucket.NEEDS,
                    custom = true
                )
            )
        )
        assertEquals("Gym", Categories.of("gym").label)
        assertEquals(Bucket.NEEDS, Categories.bucketOf("gym"))
        assertTrue(Categories.expense.any { it.key == "gym" })
    }

    @Test
    fun `custom investment categories count toward savings`() {
        Categories.load(
            listOf(
                Category(
                    key = "invest",
                    label = "Investing",
                    emoji = "📈",
                    color = androidx.compose.ui.graphics.Color(0xFF0EA5E9),
                    bucket = Bucket.SAVINGS,
                    custom = true
                )
            )
        )
        assertEquals(Bucket.SAVINGS, Categories.bucketOf("invest"))
    }

    @Test
    fun `hidden categories disappear from pickers but keep their label`() {
        Categories.load(
            listOf(
                Category(
                    key = "food",
                    label = "Food & Drink",
                    emoji = "🍔",
                    color = androidx.compose.ui.graphics.Color(0xFFF97316),
                    bucket = Bucket.WANTS,
                    custom = true,
                    hidden = true
                )
            )
        )
        assertFalse(Categories.expense.any { it.key == "food" })
        assertEquals("Food & Drink", Categories.of("food").label)
    }

    @Test
    fun `unknown keys fall back instead of crashing`() {
        val category = Categories.of("does_not_exist")
        assertNotNull(category)
        assertTrue(category.key.isNotEmpty())
    }

    @Test
    fun `bucketOf defaults to wants for unknown keys`() {
        assertEquals(Bucket.WANTS, Categories.bucketOf("mystery"))
    }

    @Test
    fun `label lookup is case insensitive and type aware`() {
        assertEquals("groceries", Categories.byLabel("groceries", TxType.EXPENSE)?.key)
        assertEquals("salary", Categories.byLabel("SALARY", TxType.INCOME)?.key)
        assertNull(Categories.byLabel("nonsense", TxType.EXPENSE))
    }

    @Test
    fun `built-in detection is exact`() {
        assertTrue(Categories.isBuiltIn("food"))
        assertFalse(Categories.isBuiltIn("gym"))
    }

    @Test
    fun `a custom savings category lands in the savings bucket of the plan`() {
        Categories.load(
            listOf(
                Category(
                    key = "stocks",
                    label = "Stocks",
                    emoji = "📈",
                    color = androidx.compose.ui.graphics.Color(0xFF0EA5E9),
                    bucket = Bucket.SAVINGS,
                    custom = true
                )
            )
        )
        val month = YearMonth.of(2026, 10)
        val transactions = listOf(
            Transaction(
                type = TxType.EXPENSE,
                amountCents = 2_000_000L,
                categoryKey = "stocks",
                note = "Index fund",
                date = LocalDate.of(2026, 10, 5)
            )
        )
        val stats = BudgetMath.summarize(transactions, month)
        val spend = BudgetMath.bucketSpend(stats)
        assertEquals(2_000_000L, spend[Bucket.SAVINGS])
        assertEquals(0L, spend[Bucket.WANTS])
    }
}