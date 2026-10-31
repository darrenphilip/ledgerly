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
        "IDR", "USD", "EUR", "GBP", "INR", "CAD", "AUD", "JPY", "SGD",
        "BRL", "ZAR", "NGN", "KES", "AED", "CHF", "SEK", "MXN", "NZD", "PHP"
    )

    /**
     * Symbols we pin ourselves: rupiah is written "Rp" and the platform symbol for
     * IDR is not reliable across Android versions and ICU data sets.
     */
    private val symbolOverrides = mapOf("IDR" to "Rp")

    val all: List<CurrencyOption> = codes.mapNotNull { code ->
        runCatching {
            val currency = Currency.getInstance(code)
            CurrencyOption(
                code = code,
                name = currency.getDisplayName(Locale.ENGLISH),
                symbol = symbolOverrides[code] ?: currency.symbol
            )
        }.getOrNull()
    }

    fun of(code: String): CurrencyOption =
        all.firstOrNull { it.code == code } ?: all.first { it.code == "USD" }

    fun symbolOf(code: String): String = of(code).symbol
}

object Money {

    /**
     * Currencies written in whole major units, with dot grouping and a comma decimal:
     * `Rp 1.234.567`. Amounts are still stored as cents; only display and parsing differ.
     */
    private val wholeUnitDisplay = setOf("IDR")

    /** Largest amount a user can enter, in major units. */
    private const val MAX_MAJOR_DEFAULT = 999_999_999L
    private const val MAX_MAJOR_WHOLE_UNIT = 9_999_999_999L

    fun isWholeUnit(currencyCode: String): Boolean = currencyCode in wholeUnitDisplay

    /**
     * Parses user input into cents. With a currency code the separators are interpreted the
     * way that currency is written: "50.000", "50,000" and "50000" all mean Rp 50.000,
     * while "12.50" stays twelve dollars fifty.
     */
    fun parseToCents(raw: String, currencyCode: String = "USD"): Long? {
        var s = raw.trim().filter { it.isDigit() || it == '.' || it == ',' || it == '-' }
        if (s.isEmpty()) return null

        s = if (isWholeUnit(currencyCode)) normalizeWholeUnit(s) else normalizeDecimal(s)

        val value = s.toBigDecimalOrNull() ?: return null
        if (value <= BigDecimal.ZERO) return null
        val maxMajor = if (isWholeUnit(currencyCode)) MAX_MAJOR_WHOLE_UNIT else MAX_MAJOR_DEFAULT
        if (value > BigDecimal.valueOf(maxMajor)) return null
        return value.setScale(2, RoundingMode.HALF_UP).movePointRight(2).toLong()
    }

    /** `1,234.56` and `12,50` styles: the last separator wins when it looks decimal. */
    private fun normalizeDecimal(s: String): String {
        val lastDot = s.lastIndexOf('.')
        val lastComma = s.lastIndexOf(',')
        return when {
            lastDot >= 0 && lastComma >= 0 ->
                if (lastDot > lastComma) s.replace(",", "")
                else s.replace(".", "").replace(',', '.')
            lastComma >= 0 ->
                if (s.count { it == ',' } == 1 && s.substringAfterLast(',').length <= 2)
                    s.replace(',', '.')
                else s.replace(",", "")
            else -> s
        }
    }

    /**
     * `Rp` style: separators group thousands. A trailing group of one or two digits is read
     * as a decimal fraction so "50,5" still means fifty and a half rupiah.
     */
    private fun normalizeWholeUnit(s: String): String {
        val lastSep = maxOf(s.lastIndexOf('.'), s.lastIndexOf(','))
        if (lastSep < 0) return s
        val tail = s.substring(lastSep + 1)
        val looksDecimal = tail.length in 1..2 && tail.all { it.isDigit() }
        return if (looksDecimal) {
            s.substring(0, lastSep).filter { it.isDigit() } + "." + tail
        } else {
            s.filter { it.isDigit() }
        }
    }

    fun format(cents: Long, currencyCode: String, showPlus: Boolean = false): String {
        val magnitude = kotlin.math.abs(cents)
        val body = if (isWholeUnit(currencyCode)) {
            "${Currencies.symbolOf(currencyCode)} ${groupThousands(majorWholeUnits(magnitude))}"
        } else {
            localeFormat(magnitude, currencyCode)
        }
        return when {
            showPlus && cents > 0 -> "+$body"
            cents < 0 -> "-$body"
            else -> body
        }
    }

    private fun localeFormat(magnitude: Long, currencyCode: String): String {
        val currency = runCatching { Currency.getInstance(currencyCode) }
            .getOrDefault(Currency.getInstance("USD"))
        val formatter = NumberFormat.getCurrencyInstance(Locale.US).apply {
            this.currency = currency
            val digits = currency.defaultFractionDigits.coerceIn(0, 2)
            minimumFractionDigits = digits
            maximumFractionDigits = digits
        }
        return formatter.format(magnitude / 100.0)
    }

    private fun majorWholeUnits(cents: Long): String =
        BigDecimal(cents).movePointLeft(2).setScale(0, RoundingMode.HALF_UP).toBigInteger().toString()

    private fun groupThousands(digits: String): String =
        digits.reversed().chunked(3).joinToString(".").reversed()

    /** Formats a signed amount using the transaction type. */
    fun signed(cents: Long, currencyCode: String, isIncome: Boolean): String =
        if (isIncome) "+${format(cents, currencyCode)}" else "-${format(cents, currencyCode)}"

    /** Short form for chart labels, e.g. "$1.2k" or "Rp 12rb". */
    fun compact(cents: Long, currencyCode: String): String {
        val symbol = Currencies.symbolOf(currencyCode)
        val major = cents / 100.0
        if (isWholeUnit(currencyCode)) {
            return when {
                major >= 1_000_000_000 -> "$symbol ${trimZero(major / 1_000_000_000)}M"
                major >= 1_000_000 -> "$symbol ${trimZero(major / 1_000_000)}jt"
                major >= 1_000 -> "$symbol ${trimZero(major / 1_000)}rb"
                else -> "$symbol ${trimZero(major)}"
            }
        }
        return when {
            major >= 1_000_000 -> String.format(Locale.US, "%s%.1fM", symbol, major / 1_000_000)
            major >= 1_000 -> String.format(Locale.US, "%s%.1fk", symbol, major / 1_000)
            major >= 100 -> String.format(Locale.US, "%s%.0f", symbol, major)
            else -> String.format(Locale.US, "%s%.0f", symbol, major)
        }
    }

    private fun trimZero(value: Double): String {
        val rounded = (value * 10).toLong() / 10.0
        return if (rounded % 1.0 == 0.0) rounded.toLong().toString() else rounded.toString()
    }

    fun plain(cents: Long): String =
        BigDecimal(cents).movePointLeft(2).setScale(2, RoundingMode.HALF_UP).toPlainString()

    /** Prefill form for an amount field: "1234.56" for decimals, "50000" for whole units. */
    fun plain(cents: Long, currencyCode: String): String {
        if (!isWholeUnit(currencyCode)) return plain(cents)
        return BigDecimal(cents).movePointLeft(2).stripTrailingZeros().toPlainString()
    }

    /** Placeholder shown in an empty amount field. */
    fun placeholder(currencyCode: String): String = if (isWholeUnit(currencyCode)) "0" else "0.00"
}
