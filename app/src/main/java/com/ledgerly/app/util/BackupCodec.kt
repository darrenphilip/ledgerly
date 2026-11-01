package com.ledgerly.app.util

import com.ledgerly.app.data.AppSettings
import com.ledgerly.app.data.Budget
import com.ledgerly.app.data.BudgetStyle
import com.ledgerly.app.data.Cadence
import com.ledgerly.app.data.Recurring
import com.ledgerly.app.data.ThemeMode
import com.ledgerly.app.data.Transaction
import com.ledgerly.app.data.TxType
import java.time.LocalDate

/** Everything needed to restore a ledger, minus the screen-lock preference. */
data class Backup(
    val transactions: List<Transaction>,
    val budgets: List<Budget>,
    val recurring: List<Recurring>,
    val settings: AppSettings,
    val createdAt: Long = System.currentTimeMillis()
)

object BackupCodec {

    private const val VERSION = 1

    fun encode(backup: Backup): String = Json.write(
        mapOf(
            "app" to "ledgerly",
            "version" to VERSION,
            "createdAt" to backup.createdAt,
            "settings" to encodeSettings(backup.settings),
            "transactions" to backup.transactions.map {
                mapOf(
                    "type" to it.type.raw,
                    "amountCents" to it.amountCents,
                    "category" to it.categoryKey,
                    "note" to it.note,
                    "date" to it.date.toString(),
                    "createdAt" to it.createdAt
                )
            },
            "budgets" to backup.budgets.map {
                mapOf(
                    "category" to it.categoryKey,
                    "limitCents" to it.limitCents,
                    "month" to it.month
                )
            },
            "recurring" to backup.recurring.map {
                mapOf(
                    "type" to it.type.raw,
                    "amountCents" to it.amountCents,
                    "category" to it.categoryKey,
                    "note" to it.note,
                    "cadence" to it.cadence.name,
                    "nextDue" to it.nextDue.toString(),
                    "active" to it.active
                )
            }
        )
    )

    private fun encodeSettings(settings: AppSettings): Map<String, Any?> = mapOf(
        "name" to settings.name,
        "currency" to settings.currency,
        "monthlyBudgetCents" to settings.monthlyBudgetCents,
        "budgetStyle" to settings.budgetStyle.name,
        "monthlyIncomeCents" to settings.monthlyIncomeCents,
        "theme" to settings.theme.name
    )

    /** Throws [IllegalArgumentException] when the file isn't a Ledgerly backup. */
    fun decode(text: String): Backup {
        val parsed = runCatching { Json.parse(text) }.getOrNull()
        val root = parsed as? Map<*, *>
            ?: throw IllegalArgumentException("Not a Ledgerly backup file")
        require(root["app"] == "ledgerly") { "Not a Ledgerly backup file" }
        require((root["version"] as? Number)?.toInt() ?: 0 <= VERSION) {
            "This backup was written by a newer version of Ledgerly"
        }

        val settingsMap = root["settings"] as? Map<*, *> ?: emptyMap<Any, Any>()
        val settings = AppSettings(
            name = settingsMap["name"] as? String ?: "",
            currency = settingsMap["currency"] as? String ?: "IDR",
            monthlyBudgetCents = settingsMap.long("monthlyBudgetCents"),
            budgetStyle = BudgetStyle.from(settingsMap["budgetStyle"] as? String),
            monthlyIncomeCents = settingsMap.long("monthlyIncomeCents"),
            theme = runCatching { ThemeMode.valueOf(settingsMap["theme"] as? String ?: "") }
                .getOrDefault(ThemeMode.SYSTEM),
            onboarded = true,
            appLock = false
        )

        val transactions = (root["transactions"] as? List<*>).orEmpty().mapNotNull { entry ->
            val map = entry as? Map<*, *> ?: return@mapNotNull null
            val date = map.date("date") ?: return@mapNotNull null
            val amount = map.long("amountCents")
            if (amount <= 0L) return@mapNotNull null
            Transaction(
                type = TxType.from(map["type"] as? String),
                amountCents = amount,
                categoryKey = map["category"] as? String ?: "other",
                note = map["note"] as? String ?: "",
                date = date,
                createdAt = map.long("createdAt").takeIf { it > 0L } ?: System.currentTimeMillis()
            )
        }

        val budgets = (root["budgets"] as? List<*>).orEmpty().mapNotNull { entry ->
            val map = entry as? Map<*, *> ?: return@mapNotNull null
            val category = map["category"] as? String ?: return@mapNotNull null
            val limit = map.long("limitCents")
            if (limit <= 0L) return@mapNotNull null
            Budget(category, limit, map["month"] as? String ?: "")
        }

        val recurring = (root["recurring"] as? List<*>).orEmpty().mapNotNull { entry ->
            val map = entry as? Map<*, *> ?: return@mapNotNull null
            val due = map.date("nextDue") ?: return@mapNotNull null
            val amount = map.long("amountCents")
            if (amount <= 0L) return@mapNotNull null
            Recurring(
                type = TxType.from(map["type"] as? String),
                amountCents = amount,
                categoryKey = map["category"] as? String ?: "other",
                note = map["note"] as? String ?: "",
                cadence = Cadence.from(map["cadence"] as? String),
                nextDue = due,
                active = map["active"] as? Boolean ?: true
            )
        }

        return Backup(transactions, budgets, recurring, settings)
    }

    private fun Map<*, *>.long(key: String): Long = (this[key] as? Number)?.toLong() ?: 0L

    private fun Map<*, *>.date(key: String): LocalDate? =
        (this[key] as? String)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
}