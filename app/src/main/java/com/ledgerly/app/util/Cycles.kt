package com.ledgerly.app.util

import java.time.LocalDate
import java.time.YearMonth

/**
 * Budget cycles. By default a cycle is the calendar month; when the user sets a start day
 * (payday), cycles run from that day to the day before the next one and are labelled by the
 * month they end in, so "October" means "the money you had for October".
 */
object Cycles {

    /**
     * The cycle a [date] belongs to, labelled by the month it mostly covers. With a start day of
     * 25, the cycle named October runs 25 Sep – 24 Oct, so most days show the month you're in.
     */
    fun cycleOf(date: LocalDate, startDay: Int): YearMonth {
        val day = startDay.coerceIn(1, 28)
        if (day == 1) return YearMonth.from(date)
        return if (date.dayOfMonth >= day) {
            YearMonth.from(date.plusMonths(1))
        } else {
            YearMonth.from(date)
        }
    }

    fun current(startDay: Int, today: LocalDate = LocalDate.now()): YearMonth =
        cycleOf(today, startDay)

    /** First day of [cycle], e.g. 25 Sep for an October cycle starting on the 25th. */
    fun start(cycle: YearMonth, startDay: Int): LocalDate {
        val day = startDay.coerceIn(1, 28)
        val thisMonth = cycle.atDay(day)
        return if (day == 1) thisMonth else thisMonth.minusMonths(1)
    }

    /** Last day of [cycle], inclusive. */
    fun end(cycle: YearMonth, startDay: Int): LocalDate =
        start(cycle, startDay).plusMonths(1).minusDays(1)

    /** True when [date] belongs to [cycle] under the given start day. */
    fun contains(date: LocalDate, cycle: YearMonth, startDay: Int): Boolean =
        cycleOf(date, startDay) == cycle

    /** Number of days in the cycle, 28-31. */
    fun length(cycle: YearMonth, startDay: Int): Int =
        (end(cycle, startDay).toEpochDay() - start(cycle, startDay).toEpochDay() + 1).toInt()

    fun daysLeft(cycle: YearMonth, startDay: Int, today: LocalDate = LocalDate.now()): Int {
        val end = end(cycle, startDay)
        if (today.isAfter(end)) return 0
        val start = start(cycle, startDay)
        val from = if (today.isBefore(start)) start else today
        return (end.toEpochDay() - from.toEpochDay() + 1).toInt().coerceAtLeast(0)
    }

    fun daysElapsed(cycle: YearMonth, startDay: Int, today: LocalDate = LocalDate.now()): Int {
        val start = start(cycle, startDay)
        if (today.isBefore(start)) return 0
        val end = end(cycle, startDay)
        val to = if (today.isAfter(end)) end else today
        return (to.toEpochDay() - start.toEpochDay() + 1).toInt()
    }

    /** "1 – 30 Nov" is not used; this is "25 Sep – 24 Oct" or the plain month name. */
    fun label(cycle: YearMonth, startDay: Int): String {
        if (startDay.coerceIn(1, 28) == 1) return DateFormats.monthLabel(cycle)
        val start = start(cycle, startDay)
        val end = end(cycle, startDay)
        return "${start.dayOfMonth} ${DateFormats.monthShort(YearMonth.from(start))} – " +
            "${end.dayOfMonth} ${DateFormats.monthShort(YearMonth.from(end))}"
    }
}