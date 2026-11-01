package com.ledgerly.app

import com.ledgerly.app.data.AppSettings
import com.ledgerly.app.data.Budget
import com.ledgerly.app.data.BudgetStyle
import com.ledgerly.app.data.Cadence
import com.ledgerly.app.data.Recurring
import com.ledgerly.app.data.ThemeMode
import com.ledgerly.app.data.Transaction
import com.ledgerly.app.data.TxType
import com.ledgerly.app.util.Backup
import com.ledgerly.app.util.BackupCodec
import com.ledgerly.app.util.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BackupCodecTest {

    private val backup = Backup(
        transactions = listOf(
            Transaction(
                type = TxType.EXPENSE,
                amountCents = 12_550L,
                categoryKey = "food",
                note = "Lunch \"with\" the team,\nand a newline",
                date = LocalDate.of(2026, 3, 5),
                createdAt = 1_700_000_000_000L
            ),
            Transaction(
                type = TxType.INCOME,
                amountCents = 9_000_000L,
                categoryKey = "salary",
                note = "March",
                date = LocalDate.of(2026, 3, 1),
                createdAt = 1_700_000_000_001L
            )
        ),
        budgets = listOf(
            Budget("food", 1_500_000L),
            Budget("transport", 800_000L, month = "2026-03")
        ),
        recurring = listOf(
            Recurring(
                amountCents = 2_500_000L,
                categoryKey = "bills",
                note = "Rent",
                cadence = Cadence.MONTHLY,
                nextDue = LocalDate.of(2026, 4, 1)
            )
        ),
        settings = AppSettings(
            name = "Darren",
            currency = "IDR",
            monthlyBudgetCents = 6_000_000L,
            budgetStyle = BudgetStyle.RULE_50_30_20,
            monthlyIncomeCents = 12_000_000L,
            theme = ThemeMode.DARK,
            onboarded = true,
            appLock = true
        )
    )

    @Test
    fun `round trips every field`() {
        val restored = BackupCodec.decode(BackupCodec.encode(backup))

        assertEquals(backup.transactions.size, restored.transactions.size)
        assertEquals(backup.transactions.first().note, restored.transactions.first().note)
        assertEquals(backup.transactions.first().amountCents, restored.transactions.first().amountCents)
        assertEquals(backup.transactions.first().date, restored.transactions.first().date)
        assertEquals(backup.budgets, restored.budgets.sortedBy { it.categoryKey + it.month })
        assertEquals(backup.recurring.size, restored.recurring.size)
        assertEquals("Rent", restored.recurring.single().note)
        assertEquals(LocalDate.of(2026, 4, 1), restored.recurring.single().nextDue)
        assertEquals("Darren", restored.settings.name)
        assertEquals(BudgetStyle.RULE_50_30_20, restored.settings.budgetStyle)
        assertEquals(ThemeMode.DARK, restored.settings.theme)
        assertEquals(12_000_000L, restored.settings.monthlyIncomeCents)
        assertTrue(restored.settings.onboarded)
    }

    @Test
    fun `the screen lock preference is never taken from a file`() {
        val restored = BackupCodec.decode(BackupCodec.encode(backup))
        assertTrue(!restored.settings.appLock)
    }

    @Test
    fun `rejects files that are not Ledgerly backups`() {
        assertThrows(IllegalArgumentException::class.java) {
            BackupCodec.decode("{\"app\":\"something-else\"}")
        }
        assertThrows(IllegalArgumentException::class.java) {
            BackupCodec.decode("just text")
        }
    }

    @Test
    fun `tolerates a backup with missing sections`() {
        val restored = BackupCodec.decode("{\"app\":\"ledgerly\",\"version\":1}")
        assertTrue(restored.transactions.isEmpty())
        assertTrue(restored.budgets.isEmpty())
        assertTrue(restored.recurring.isEmpty())
        assertEquals("IDR", restored.settings.currency)
    }

    @Test
    fun `skips rows that cannot be used`() {
        val json = """
            {"app":"ledgerly","version":1,"transactions":[
              {"date":"2026-03-05","amountCents":100,"type":"expense","category":"food"},
              {"date":"nope","amountCents":100},
              {"date":"2026-03-06","amountCents":0}
            ]}
        """.trimIndent()
        val restored = BackupCodec.decode(json)
        assertEquals(1, restored.transactions.size)
    }
}

class JsonTest {

    @Test
    fun `round trips nested structures`() {
        val value = mapOf(
            "text" to "quote\" and \\ backslash",
            "int" to 42L,
            "bool" to true,
            "nothing" to null,
            "list" to listOf(1L, 2L, 3L),
            "nested" to mapOf("inner" to "value")
        )
        val parsed = Json.parse(Json.write(value)) as Map<*, *>
        assertEquals("quote\" and \\ backslash", parsed["text"])
        assertEquals(42L, parsed["int"])
        assertEquals(true, parsed["bool"])
        assertEquals(null, parsed["nothing"])
        assertEquals(listOf(1L, 2L, 3L), parsed["list"])
        assertEquals(mapOf("inner" to "value"), parsed["nested"])
    }

    @Test
    fun `escapes control characters`() {
        val text = "line\nbreak\ttab"
        assertEquals(text, Json.parse(Json.write(text)))
    }

    @Test
    fun `reads decimals`() {
        assertEquals(1.5, Json.parse("1.5"))
    }
}