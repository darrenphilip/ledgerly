package com.ledgerly.app.util

import com.ledgerly.app.data.Categories
import com.ledgerly.app.data.Transaction
import com.ledgerly.app.data.TxType
import java.time.LocalDate

/**
 * Reads the CSV written by [CsvExporter] back in. Rows that don't parse are reported instead of
 * silently dropped, and rows already in the ledger are counted as duplicates.
 */
object CsvImporter {

    data class Plan(
        val transactions: List<Transaction>,
        val duplicates: Int,
        val skippedLines: List<Int>
    ) {
        val isEmpty: Boolean get() = transactions.isEmpty() && duplicates == 0 && skippedLines.isEmpty()
    }

    fun parse(
        text: String,
        currency: String,
        existing: List<Transaction> = emptyList(),
        today: LocalDate = LocalDate.now()
    ): Plan {
        val rows = mutableListOf<Transaction>()
        val skipped = mutableListOf<Int>()
        var duplicates = 0
        val seen = existing.map { it.fingerprint() }.toMutableSet()

        text.lineSequence().forEachIndexed { index, rawLine ->
            val line = rawLine.trimEnd('\r')
            if (line.isBlank()) return@forEachIndexed
            val fields = splitFields(line)
            if (index == 0 && looksLikeHeader(fields)) return@forEachIndexed
            if (fields.size < 5) {
                skipped += index + 1
                return@forEachIndexed
            }

            val date = runCatching { LocalDate.parse(fields[0].trim()) }.getOrNull()
            val amount = Money.parseToCents(fields[4].trim(), currency)
            if (date == null || amount == null || amount <= 0L) {
                skipped += index + 1
                return@forEachIndexed
            }

            val type = if (fields[1].trim().equals("income", ignoreCase = true)) {
                TxType.INCOME
            } else {
                TxType.EXPENSE
            }
            val category = Categories.byLabel(fields[2], type) ?: Categories.defaultForType(type)
            val note = fields[3].trim()
            val tx = Transaction(
                type = type,
                amountCents = amount,
                categoryKey = category.key,
                note = note,
                date = date,
                createdAt = date.toEpochDay() * 86_400_000L + index
            )
            if (!seen.add(tx.fingerprint())) {
                duplicates++
                return@forEachIndexed
            }
            rows += tx
        }

        return Plan(rows.sortedBy { it.date }, duplicates, skipped)
    }

    private fun looksLikeHeader(fields: List<String>): Boolean {
        val first = fields.firstOrNull()?.trim()?.lowercase() ?: return false
        return first == "date" || first == "tanggal"
    }

    private fun Transaction.fingerprint(): String =
        "$date|${type.raw}|$amountCents|$categoryKey|${note.trim().lowercase()}"

    /** Handles quoted fields, escaped quotes and commas inside a field. */
    private fun splitFields(line: String): List<String> {
        val fields = mutableListOf<String>()
        val sb = StringBuilder()
        var quoted = false
        var index = 0
        while (index < line.length) {
            val char = line[index]
            when {
                quoted && char == '"' && index + 1 < line.length && line[index + 1] == '"' -> {
                    sb.append('"')
                    index++
                }
                char == '"' -> quoted = !quoted
                char == ',' && !quoted -> {
                    fields += sb.toString()
                    sb.clear()
                }
                else -> sb.append(char)
            }
            index++
        }
        fields += sb.toString()
        return fields
    }
}