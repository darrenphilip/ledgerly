package com.ledgerly.app.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

data class CurrencyOption(
    val code: String,
    val name: String,
    val symbol: String
)

object Currencies {
    private val codes = listOf(
        "USD", "EUR", "GBP", "INR", "CAD", "AUD", "JPY", "SGD",
        "BRL", "ZAR", "NGN", "KES", "AED", "CHF", "SEK", "MXN", "NZD", "PHP"
    )

    val all: List<CurrencyOption> = codes.mapNotNull { code ->
        runCatching {
            val currency = Currency.getInstance(code)
            CurrencyOption(
                code = code,
                name = currency.getDisplayName(Locale.ENGLISH),
                symbol = currency.symbol
            )
        }.getOrNull()
    }

    fun of(code: String): CurrencyOption =
        all.firstOrNull { it.code == code } ?: all.first { it.code == "USD" }

    fun symbolOf(code: String): String = of(code).symbol
}

object Money {

    /** Parses user input such as "12.50", "1,200", "12,50" into cents. */
    fun parseToCents(raw: String): Long? {
        var s = raw.trim().filter { it.isDigit() || it == '.' || it == ',' || it == '-' }
        if (s.isEmpty()) return null

        val lastDot = s.lastIndexOf('.')
        val lastComma = s.lastIndexOf(',')
        s = when {
            lastDot >= 0 && lastComma >= 0 ->
                if (lastDot > lastComma) s.replace(",", "")
                else s.replace(".", "").replace(',', '.')
            lastComma >= 0 ->
                if (s.count { it == ',' } == 1 && s.substringAfterLast(',').length <= 2)
                    s.replace(',', '.')
                else s.replace(",", "")
            else -> s
        }

        val value = s.toBigDecimalOrNull() ?: return null
        if (value <= BigDecimal.ZERO) return null
        if (value > BigDecimal("999999999")) return null
        return value.setScale(2, RoundingMode.HALF_UP).movePointRight(2).toLong()
    }

    fun format(cents: Long, currencyCode: String, showPlus: Boolean = false): String {
        val currency = runCatching { Currency.getInstance(currencyCode) }
            .getOrDefault(Currency.getInstance("USD"))
        val formatter = NumberFormat.getCurrencyInstance(Locale.US).apply {
            this.currency = currency
            val digits = currency.defaultFractionDigits.coerceIn(0, 2)
            minimumFractionDigits = digits
            maximumFractionDigits = digits
        }
        val major = cents / 100.0
        val body = formatter.format(kotlin.math.abs(major))
        return when {
            showPlus && cents > 0 -> "+$body"
            cents < 0 -> "-$body"
            else -> body
        }
    }

    /** Formats a signed amount using the transaction type. */
    fun signed(cents: Long, currencyCode: String, isIncome: Boolean): String =
        if (isIncome) "+${format(cents, currencyCode)}" else "-${format(cents, currencyCode)}"

    /** Short form for chart labels, e.g. "$1.2k". */
    fun compact(cents: Long, currencyCode: String): String {
        val symbol = Currencies.symbolOf(currencyCode)
        val major = cents / 100.0
        return when {
            major >= 1_000_000 -> String.format(Locale.US, "%s%.1fM", symbol, major / 1_000_000)
            major >= 1_000 -> String.format(Locale.US, "%s%.1fk", symbol, major / 1_000)
            major >= 100 -> String.format(Locale.US, "%s%.0f", symbol, major)
            else -> String.format(Locale.US, "%s%.0f", symbol, major)
        }
    }

    fun plain(cents: Long): String =
        BigDecimal(cents).movePointLeft(2).setScale(2, RoundingMode.HALF_UP).toPlainString()
}
