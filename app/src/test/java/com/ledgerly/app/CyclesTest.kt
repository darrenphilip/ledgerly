package com.ledgerly.app

import com.ledgerly.app.util.Cycles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class CyclesTest {

    private val oct = YearMonth.of(2026, 10)

    @Test
    fun `calendar months keep the plain month label`() {
        assertEquals(oct, Cycles.cycleOf(LocalDate.of(2026, 10, 15), 1))
        assertEquals(LocalDate.of(2026, 10, 1), Cycles.start(oct, 1))
        assertEquals(LocalDate.of(2026, 10, 31), Cycles.end(oct, 1))
        assertEquals(31, Cycles.length(oct, 1))
    }

    @Test
    fun `a payday cycle runs from that day to the day before`() {
        assertEquals(LocalDate.of(2026, 9, 25), Cycles.start(oct, 25))
        assertEquals(LocalDate.of(2026, 10, 24), Cycles.end(oct, 25))
        assertEquals(30, Cycles.length(oct, 25))
    }

    @Test
    fun `dates before the start day belong to the same labelled cycle`() {
        // October's payday cycle is 25 Sep – 24 Oct, so the 25th starts the NEXT cycle.
        assertEquals(oct, Cycles.cycleOf(LocalDate.of(2026, 9, 25), 25))
        assertEquals(oct, Cycles.cycleOf(LocalDate.of(2026, 10, 24), 25))
        assertEquals(YearMonth.of(2026, 11), Cycles.cycleOf(LocalDate.of(2026, 10, 25), 25))
        assertTrue(Cycles.contains(LocalDate.of(2026, 9, 25), oct, 25))
        assertFalse(Cycles.contains(LocalDate.of(2026, 10, 25), oct, 25))
    }

    @Test
    fun `days left counts today as remaining`() {
        val today = LocalDate.of(2026, 10, 20)
        assertEquals(5, Cycles.daysLeft(oct, 25, today))
        assertEquals(12, Cycles.daysLeft(oct, 1, today))
    }

    @Test
    fun `a finished cycle has no days left`() {
        assertEquals(0, Cycles.daysLeft(oct, 1, LocalDate.of(2026, 11, 5)))
    }

    @Test
    fun `days elapsed is clamped to the cycle`() {
        assertEquals(0, Cycles.daysElapsed(oct, 25, LocalDate.of(2026, 9, 1)))
        assertEquals(30, Cycles.daysElapsed(oct, 25, LocalDate.of(2026, 11, 20)))
        assertEquals(1, Cycles.daysElapsed(oct, 25, LocalDate.of(2026, 9, 25)))
    }

    @Test
    fun `february is shorter in a payday cycle`() {
        val feb = YearMonth.of(2026, 2)
        assertEquals(28, Cycles.length(feb, 1))
        // 25 Jan – 24 Feb
        assertEquals(31, Cycles.length(feb, 25))
        // 25 Feb – 24 Mar
        assertEquals(28, Cycles.length(YearMonth.of(2026, 3), 25))
    }

    @Test
    fun `labels describe the range`() {
        assertEquals("25 Sep – 24 Oct", Cycles.label(oct, 25))
        assertTrue(Cycles.label(oct, 1).contains("October"))
    }

    @Test
    fun `current cycle follows the start day`() {
        assertEquals(oct, Cycles.current(1, LocalDate.of(2026, 10, 3)))
        // 3 Oct falls in the cycle labelled October, which began 25 Sep.
        assertEquals(oct, Cycles.current(25, LocalDate.of(2026, 10, 3)))
        assertEquals(YearMonth.of(2026, 11), Cycles.current(25, LocalDate.of(2026, 10, 26)))
    }
}