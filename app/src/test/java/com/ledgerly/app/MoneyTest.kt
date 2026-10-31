package com.ledgerly.app

import com.ledgerly.app.util.Currencies
import com.ledgerly.app.util.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyTest {

    @Test
    fun parses_plain_decimal() {
        assertEquals(1234L, Money.parseToCents("12.34"))
    }

    @Test
    fun parses_grouped_amounts() {
        assertEquals(123456L, Money.parseToCents("1,234.56"))
        assertEquals(123456L, Money.parseToCents("$1,234.56"))
    }

    @Test
    fun parses_comma_decimal() {
        assertEquals(1250L, Money.parseToCents("12,50"))
    }

    @Test
    fun parses_whole_numbers() {
        assertEquals(4500L, Money.parseToCents("45"))
        assertEquals(4500L, Money.parseToCents(" 45 "))
    }

    @Test
    fun rejects_invalid_or_non_positive_input() {
        assertNull(Money.parseToCents(""))
        assertNull(Money.parseToCents("abc"))
        assertNull(Money.parseToCents("0"))
        assertNull(Money.parseToCents("-5"))
        assertNull(Money.parseToCents("1.2.3"))
    }

    @Test
    fun formats_usd() {
        assertEquals("$1,234.56", Money.format(123456L, "USD"))
    }

    @Test
    fun formats_negative_and_plus() {
        assertEquals("-$12.50", Money.format(-1250L, "USD"))
        assertEquals("+$12.50", Money.format(1250L, "USD", showPlus = true))
    }

    @Test
    fun formats_zero_decimal_currency() {
        assertEquals("¥1,500", Money.format(150000L, "JPY"))
    }

    @Test
    fun signed_amounts_follow_type() {
        assertEquals("-$9.99", Money.signed(999L, "USD", isIncome = false))
        assertEquals("+$9.99", Money.signed(999L, "USD", isIncome = true))
    }

    @Test
    fun plain_output_has_two_decimals() {
        assertEquals("1234.56", Money.plain(123456L))
        assertEquals("0.05", Money.plain(5L))
    }

    @Test
    fun compact_output_for_charts() {
        assertEquals("$12", Money.compact(1200L, "USD"))
        assertEquals("$1.2k", Money.compact(120000L, "USD"))
    }

    @Test
    fun idr_is_offered_with_the_rupiah_symbol() {
        assertTrue(Currencies.all.any { it.code == "IDR" })
        assertEquals("Rp", Currencies.symbolOf("IDR"))
        assertTrue(Money.isWholeUnit("IDR"))
    }

    @Test
    fun formats_idr_with_dot_grouping() {
        assertEquals("Rp 50.000", Money.format(5_000_000L, "IDR"))
        assertEquals("Rp 1.234.567", Money.format(123_456_700L, "IDR"))
        assertEquals("-Rp 50.000", Money.format(-5_000_000L, "IDR"))
        assertEquals("+Rp 50.000", Money.format(5_000_000L, "IDR", showPlus = true))
    }

    @Test
    fun parses_idr_amounts_however_they_are_written() {
        assertEquals(5_000_000L, Money.parseToCents("50000", "IDR"))
        assertEquals(5_000_000L, Money.parseToCents("50.000", "IDR"))
        assertEquals(5_000_000L, Money.parseToCents("50,000", "IDR"))
        assertEquals(123_456_700L, Money.parseToCents("1.234.567", "IDR"))
    }

    @Test
    fun parses_idr_fractions_and_rejects_junk() {
        assertEquals(150L, Money.parseToCents("1,5", "IDR"))
        assertEquals(5_000L, Money.parseToCents("50.00", "IDR"))
        assertNull(Money.parseToCents("0", "IDR"))
        assertNull(Money.parseToCents("abc", "IDR"))
        assertNull(Money.parseToCents("99999999999", "IDR"))
    }

    @Test
    fun idr_prefill_and_placeholder_hide_minor_units() {
        assertEquals("50000", Money.plain(5_000_000L, "IDR"))
        assertEquals("0", Money.placeholder("IDR"))
        assertEquals("1234.56", Money.plain(123_456L, "USD"))
        assertEquals("0.00", Money.placeholder("USD"))
    }

    @Test
    fun idr_compact_labels_use_rb_and_jt() {
        assertEquals("Rp 50", Money.compact(5_000L, "IDR"))
        assertEquals("Rp 12rb", Money.compact(1_200_000L, "IDR"))
        assertEquals("Rp 10jt", Money.compact(1_000_000_000L, "IDR"))
    }
}
