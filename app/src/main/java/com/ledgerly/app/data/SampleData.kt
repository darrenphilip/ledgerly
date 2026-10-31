package com.ledgerly.app.data

import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlin.random.Random

/**
 * Deterministic, realistic demo data. Dates are generated relative to today so
 * the app always has something interesting to show on first launch.
 */
object SampleData {

    const val MONTHLY_BUDGET_CENTS: Long = 320_000L

    val budgets: List<Budget> = listOf(
        Budget("home", 120_000L),
        Budget("food", 50_000L),
        Budget("groceries", 40_000L),
        Budget("bills", 25_000L),
        Budget("shopping", 20_000L),
        Budget("travel", 15_000L),
        Budget("transport", 12_000L),
        Budget("entertainment", 10_000L),
        Budget("health", 8_000L),
        Budget("subscriptions", 5_000L),
        Budget("education", 4_000L),
        Budget("other", 4_000L)
    )

    private val foodNotes = listOf(
        "Lunch at Chipotle", "Coffee at Blue Bottle", "Dinner with friends",
        "Sushi night", "Pizza takeout", "Sunday brunch", "Tacos al pastor",
        "Noodle bar", "Bakery run", "Burger and fries"
    )
    private val groceryNotes = listOf(
        "Whole Foods", "Trader Joe's", "Weekly groceries", "Farmers market",
        "Corner store", "Costco run"
    )
    private val transportNotes = listOf(
        "Uber ride", "Metro card", "Gas station", "Parking garage", "Lyft home"
    )
    private val shoppingNotes = listOf(
        "Nike sneakers", "Amazon order", "Target run", "New headphones",
        "Bookstore", "Home decor"
    )
    private val entertainmentNotes = listOf(
        "Movie tickets", "Concert tickets", "Museum entry", "Video game", "Bowling night"
    )

    fun transactions(today: LocalDate = LocalDate.now()): List<Transaction> {
        val random = Random(73)
        val currentMonth = YearMonth.from(today)
        val result = mutableListOf<Transaction>()

        for (offset in 3 downTo 0) {
            val month = currentMonth.minusMonths(offset.toLong())
            val daysInMonth = month.lengthOfMonth()

            fun add(day: Int, type: TxType, category: String, cents: Long, note: String) {
                val date = month.atDay(day.coerceIn(1, daysInMonth))
                if (date.isAfter(today)) return
                val hour = 8 + random.nextInt(13)
                result += Transaction(
                    type = type,
                    amountCents = cents,
                    categoryKey = category,
                    note = note,
                    date = date,
                    createdAt = date.atTime(hour, random.nextInt(60))
                        .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                )
            }

            add(1, TxType.INCOME, "salary", 420_000L, "Monthly salary")
            add(2, TxType.EXPENSE, "home", 115_000L, "Rent")
            add(5, TxType.EXPENSE, "bills", 8_500L + random.nextInt(4_000), "Electricity bill")
            add(6, TxType.EXPENSE, "bills", 5_999L, "Internet")
            add(8, TxType.EXPENSE, "bills", 3_500L, "Phone plan")
            add(3, TxType.EXPENSE, "subscriptions", 1_599L, "Netflix")
            add(9, TxType.EXPENSE, "subscriptions", 1_099L, "Spotify")
            add(12, TxType.EXPENSE, "subscriptions", 299L, "iCloud+")

            if (offset % 2 == 0) {
                add(18, TxType.EXPENSE, "health", 4_500L + random.nextInt(2_000), "Pharmacy")
            }
            if (offset == 2 || offset == 0) {
                add(20, TxType.INCOME, "freelance", 35_000L + random.nextInt(20_000), "Freelance project")
            }

            var day = 3
            while (day <= daysInMonth) {
                add(day, TxType.EXPENSE, "groceries", 4_200L + random.nextInt(6_500), groceryNotes.random(random))
                day += 6 + random.nextInt(3)
            }

            repeat(9) {
                add(
                    1 + random.nextInt(daysInMonth), TxType.EXPENSE, "food",
                    850L + random.nextInt(3_400), foodNotes.random(random)
                )
            }
            repeat(6) {
                add(
                    1 + random.nextInt(daysInMonth), TxType.EXPENSE, "transport",
                    275L + random.nextInt(3_000), transportNotes.random(random)
                )
            }
            repeat(2) {
                add(
                    1 + random.nextInt(daysInMonth), TxType.EXPENSE, "shopping",
                    2_400L + random.nextInt(16_000), shoppingNotes.random(random)
                )
            }
            repeat(2) {
                add(
                    1 + random.nextInt(daysInMonth), TxType.EXPENSE, "entertainment",
                    1_200L + random.nextInt(4_000), entertainmentNotes.random(random)
                )
            }
            if (offset == 1) add(15, TxType.EXPENSE, "travel", 28_000L, "Weekend trip")
            if (offset == 3) add(22, TxType.EXPENSE, "education", 4_900L, "Online course")
        }

        return result.sortedBy { it.date }
    }

}
