package com.ledgerly.app

import com.ledgerly.app.data.Cadence
import com.ledgerly.app.data.Recurring
import com.ledgerly.app.data.TxType
import com.ledgerly.app.util.RecurringMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RecurringMathTest {

    private fun item(
        due: LocalDate,
        cadence: Cadence = Cadence.MONTHLY,
        active: Boolean = true
    ) = Recurring(
        amountCents = 1_500_000L,
        categoryKey = "bills",
        note = "Rent",
        cadence = cadence,
        nextDue = due,
        active = active
    )

    @Test
    fun `weekly advances seven days`() {
        assertEquals(
            LocalDate.of(2026, 3, 8),
            RecurringMath.advance(LocalDate.of(2026, 3, 1), Cadence.WEEKLY)
        )
    }

    @Test
    fun `monthly clamps to the last day of a short month`() {
        assertEquals(
            LocalDate.of(2026, 2, 28),
            RecurringMath.advance(LocalDate.of(2026, 1, 31), Cadence.MONTHLY)
        )
    }

    @Test
    fun `due list only holds active entries whose date arrived`() {
        val today = LocalDate.of(2026, 3, 10)
        val due = RecurringMath.due(
            listOf(
                item(LocalDate.of(2026, 3, 1)),
                item(LocalDate.of(2026, 3, 10)),
                item(LocalDate.of(2026, 3, 11)),
                item(LocalDate.of(2026, 3, 1), active = false)
            ),
            today
        )
        assertEquals(2, due.size)
        assertTrue(due.all { !it.nextDue.isAfter(today) })
    }

    @Test
    fun `next after skips periods that were missed`() {
        val stale = item(LocalDate.of(2026, 1, 15), Cadence.WEEKLY)
        val today = LocalDate.of(2026, 3, 10)
        val next = RecurringMath.nextAfter(stale, today)
        // First scheduled date strictly after today, not "today + one period".
        assertEquals(LocalDate.of(2026, 3, 12), next)
    }

    @Test
    fun `next after is exactly one period when on time`() {
        val onTime = item(LocalDate.of(2026, 3, 1))
        assertEquals(
            LocalDate.of(2026, 4, 1),
            RecurringMath.nextAfter(onTime, LocalDate.of(2026, 3, 1))
        )
    }

    @Test
    fun `logging uses the due date and entry fields`() {
        val entry = item(LocalDate.of(2026, 3, 1))
        val tx = RecurringMath.transactionFor(entry)
        assertEquals(entry.amountCents, tx.amountCents)
        assertEquals(entry.categoryKey, tx.categoryKey)
        assertEquals(TxType.EXPENSE, tx.type)
        assertEquals(entry.nextDue, tx.date)
    }
}