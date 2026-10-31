package com.ledgerly.app.util

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateFormats {
    private val monthYear = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
    private val monthShort = DateTimeFormatter.ofPattern("MMM", Locale.getDefault())
    private val dayHeader = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())
    private val full = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())

    fun monthLabel(month: YearMonth): String = month.atDay(1).format(monthYear)
    fun monthShort(month: YearMonth): String = month.atDay(1).format(monthShort)
    fun dayHeader(date: LocalDate): String = date.format(dayHeader)
    fun fullDate(date: LocalDate): String = date.format(full)

    fun relativeDay(date: LocalDate, today: LocalDate = LocalDate.now()): String = when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> dayHeader(date)
    }
}
