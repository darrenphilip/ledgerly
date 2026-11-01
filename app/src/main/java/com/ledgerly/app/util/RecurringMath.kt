package com.ledgerly.app.util

import com.ledgerly.app.data.Cadence
import com.ledgerly.app.data.Recurring
import com.ledgerly.app.data.Transaction
import java.time.LocalDate

/** Date handling for recurring entries. Pure so the rollover rules are unit tested. */
object RecurringMath {

    fun advance(from: LocalDate, cadence: Cadence): LocalDate = when (cadence) {
        Cadence.WEEKLY -> from.plusWeeks(1)
        Cadence.MONTHLY -> from.plusMonths(1)
    }

    /** Entries whose due date has arrived. Oldest first, so the list reads like a to-do. */
    fun due(items: List<Recurring>, today: LocalDate = LocalDate.now()): List<Recurring> =
        items.filter { it.active && !it.nextDue.isAfter(today) }
            .sortedBy { it.nextDue }

    /** The transaction an entry logs, dated when it was due. */
    fun transactionFor(item: Recurring, date: LocalDate = item.nextDue): Transaction = Transaction(
        type = item.type,
        amountCents = item.amountCents,
        categoryKey = item.categoryKey,
        note = item.note,
        date = date
    )

    /**
     * Next due date after logging the entry on [loggedOn]: skips whole periods so a weekly entry
     * that's been ignored for a month doesn't come back due immediately.
     */
    fun nextAfter(item: Recurring, loggedOn: LocalDate): LocalDate {
        var next = advance(item.nextDue, item.cadence)
        var guard = 0
        while (!next.isAfter(loggedOn) && guard < 500) {
            next = advance(next, item.cadence)
            guard++
        }
        return next
    }

    fun label(item: Recurring, currency: String): String =
        item.note.ifBlank { com.ledgerly.app.data.Categories.of(item.categoryKey).label }
            .let { "$it · ${Money.format(item.amountCents, currency)}" }
}