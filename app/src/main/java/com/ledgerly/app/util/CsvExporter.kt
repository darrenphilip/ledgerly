package com.ledgerly.app.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.ledgerly.app.data.Categories
import com.ledgerly.app.data.Transaction
import com.ledgerly.app.data.TxType
import java.io.File
import java.time.LocalDate

object CsvExporter {

    fun build(transactions: List<Transaction>): String {
        val sb = StringBuilder("Date,Type,Category,Note,Amount\n")
        transactions.sortedWith(compareBy({ it.date }, { it.createdAt })).forEach { tx ->
            sb.append(tx.date).append(',')
            sb.append(if (tx.type == TxType.INCOME) "Income" else "Expense").append(',')
            sb.append(escape(Categories.of(tx.categoryKey).label)).append(',')
            sb.append(escape(tx.note)).append(',')
            sb.append(Money.plain(tx.amountCents)).append('\n')
        }
        return sb.toString()
    }

    fun writeToCache(context: Context, csv: String): Uri {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "ledgerly-${LocalDate.now()}.csv")
        file.writeText(csv)
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    private fun escape(value: String): String {
        val needsQuotes = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        val escaped = value.replace("\"", "\"\"")
        return if (needsQuotes) "\"$escaped\"" else escaped
    }
}
