package com.ledgerly.app

import com.ledgerly.app.data.Categories
import com.ledgerly.app.data.Transaction
import com.ledgerly.app.data.TxType
import com.ledgerly.app.util.CsvExporter
import com.ledgerly.app.util.CsvImporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CsvImporterTest {

    private val existing = listOf(
        Transaction(
            type = TxType.EXPENSE,
            amountCents = 45_000_00L,
            categoryKey = "groceries",
            note = "Weekly shop",
            date = LocalDate.of(2026, 3, 4)
        )
    )

    private val header = "Date,Type,Category,Note,Amount\n"

    @Test
    fun `round trips an exported file`() {
        val text = CsvExporter.build(existing)
        val plan = CsvImporter.parse(text, "IDR")
        assertEquals(1, plan.transactions.size)
        assertEquals(0, plan.duplicates)
        assertTrue(plan.skippedLines.isEmpty())
        assertEquals(existing.single().amountCents, plan.transactions.single().amountCents)
        assertEquals("groceries", plan.transactions.single().categoryKey)
        assertEquals(TxType.EXPENSE, plan.transactions.single().type)
    }

    @Test
    fun `skips rows already in the ledger`() {
        val text = CsvExporter.build(existing)
        val plan = CsvImporter.parse(text, "IDR", existing)
        assertEquals(0, plan.transactions.size)
        assertEquals(1, plan.duplicates)
    }

    @Test
    fun `reads quoted notes with commas and escaped quotes`() {
        val csv = header + "2026-03-05,Expense,Food & Drink,\"Lunch, with \"\"the team\"\"\",125.50"
        val plan = CsvImporter.parse(csv, "USD")
        val row = plan.transactions.single()
        assertEquals("Lunch, with \"the team\"", row.note)
        assertEquals(12_550L, row.amountCents)
        assertEquals("food", row.categoryKey)
    }

    @Test
    fun `reports unreadable lines instead of dropping them silently`() {
        val csv = header +
            "not-a-date,Expense,Food,broken,10\n" +
            "2026-03-06,Income,Salary,Payday,5000"
        val plan = CsvImporter.parse(csv, "USD")
        assertEquals(1, plan.transactions.size)
        assertEquals(listOf(2), plan.skippedLines)
        assertEquals(TxType.INCOME, plan.transactions.single().type)
        assertEquals("salary", plan.transactions.single().categoryKey)
    }

    @Test
    fun `understands rupiah grouping`() {
        val csv = header + "2026-03-07,Expense,Groceries,Market,\"50.000\""
        val plan = CsvImporter.parse(csv, "IDR")
        assertEquals(5_000_000L, plan.transactions.single().amountCents)
    }

    @Test
    fun `unknown category falls back to the type default`() {
        val csv = header + "2026-03-08,Expense,Teleportation,Beam me up,10"
        val plan = CsvImporter.parse(csv, "USD")
        assertEquals(
            Categories.defaultForType(TxType.EXPENSE).key,
            plan.transactions.single().categoryKey
        )
    }

    @Test
    fun `empty file is an empty plan`() {
        val plan = CsvImporter.parse("", "IDR")
        assertTrue(plan.isEmpty)
    }

    @Test
    fun `duplicates inside one file are counted once`() {
        val csv = header +
            "2026-03-09,Expense,Food,Coffee,5\n" +
            "2026-03-09,Expense,Food,Coffee,5"
        val plan = CsvImporter.parse(csv, "USD")
        assertEquals(1, plan.transactions.size)
        assertEquals(1, plan.duplicates)
    }
}