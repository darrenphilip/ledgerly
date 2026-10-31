package com.ledgerly.app

import com.ledgerly.app.util.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
}
