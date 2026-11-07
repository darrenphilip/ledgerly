package com.ledgerly.app.data

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Reads a transaction out of another app's notification, e.g. "Pembayaran Rp 50.000 berhasil"
 * from a bank app. Pure and conservative: when the text is ambiguous it returns null rather
 * than guessing, because a wrong amount in a ledger is worse than a missing one.
 */
object CaptureParser {

    data class Candidate(
        val amountCents: Long,
        val type: TxType,
        /** App that posted the notification, e.g. "BCA mobile". */
        val source: String,
        /** Short description for the user, e.g. "Rp 50.000 · payment". */
        val summary: String
    )

    private val expenseWords = listOf(
        "pembayaran", "payment", "bayar", "paid", "purchase", "pembelian", "debet", "debit",
        "keluar", "withdraw", "penarikan", "transfer ke", "trf ke", "spent", "charged",
        "tagihan", "top up", "topup", "tarik tunai", "qris"
    )

    private val incomeWords = listOf(
        "masuk", "diterima", "received", "kredit", "credit", "gaji", "salary", "refund",
        "cashback", "transfer dari", "trf dari", "incoming", "deposit", "deposito", "bunga"
    )

    /** Currency shapes the parser understands. Kept narrow on purpose. */
    private val amountRegex = Regex(
        """(?i)(?:rp|idr|\$|usd|€|eur)\s*\.?\s*(\d{1,3}(?:[.,]\d{3})+|\d+)(?:[.,](\d{2}))?"""
    )

    private val bareAmountRegex = Regex("""(?:^|\s)(\d{1,3}(?:\.\d{3})+)(?:[.,](\d{2}))?(?:\s|$)""")

    /**
     * @param text the notification's title + body, joined.
     * @param source the posting app's name, only used for the label.
     */
    fun parse(text: String, source: String, currency: String = "IDR"): Candidate? {
        val cleaned = text.replace('\n', ' ').trim()
        if (cleaned.length < 4) return null

        val match = amountRegex.find(cleaned) ?: bareAmountRegex.find(cleaned) ?: return null
        val whole = match.groupValues[1]
        val fraction = match.groupValues.getOrNull(2).orEmpty()
        val cents = toCents(whole, fraction, currency) ?: return null
        if (cents <= 0L) return null
        // Ignore noise like "OTP 123456" or dates: real cash amounts are rarely tiny.
        if (cents < MIN_CENTS && !cleaned.contains("rp", ignoreCase = true)) return null

        val lower = cleaned.lowercase()
        val type = when {
            incomeWords.any { lower.contains(it) } && expenseWords.none { lower.contains(it) } ->
                TxType.INCOME
            expenseWords.any { lower.contains(it) } -> TxType.EXPENSE
            else -> return null
        }

        val label = when (type) {
            TxType.EXPENSE -> "payment"
            TxType.INCOME -> "money in"
        }
        return Candidate(
            amountCents = cents,
            type = type,
            source = source.trim().ifBlank { "Notification" },
            summary = "${com.ledgerly.app.util.Money.format(cents, currency)} · $label"
        )
    }

    /**
     * Interprets `50.000`, `50,000`, `50000` and `50.000,00` for whole-unit currencies, and
     * `1,234.56` / `1.234,56` for two-decimal ones.
     */
    private fun toCents(whole: String, fraction: String, currency: String): Long? = runCatching {
        val wholeUnit = currency in WHOLE_UNIT
        val digits = if (wholeUnit) {
            whole.filter { it.isDigit() }
        } else {
            // Keep the last group as decimals when the separator looks like one.
            whole.filter { it.isDigit() }
        }
        val major = BigDecimal(digits)
        val minor = if (fraction.isNotBlank() && !wholeUnit) {
            BigDecimal("0.$fraction")
        } else {
            BigDecimal.ZERO
        }
        val value = if (wholeUnit) major else major.add(minor)
        value.multiply(BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).toLong()
    }.getOrNull()

    /** Roughly Rp 1 — smaller than this is almost certainly not a transaction. */
    private const val MIN_CENTS = 100L

    private val WHOLE_UNIT = setOf("IDR", "JPY", "KRW", "VND", "CLP")
}