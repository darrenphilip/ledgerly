package com.ledgerly.app

import com.ledgerly.app.data.CaptureParser
import com.ledgerly.app.data.TxType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CaptureParserTest {

    private fun parse(text: String, currency: String = "IDR") =
        CaptureParser.parse(text, "BCA mobile", currency)

    @Test
    fun `reads a rupiah payment`() {
        val result = parse("Pembayaran Rp 50.000 berhasil. Terima kasih.")!!
        assertEquals(5_000_000L, result.amountCents)
        assertEquals(TxType.EXPENSE, result.type)
        assertEquals("BCA mobile", result.source)
    }

    @Test
    fun `reads dotted amounts without the Rp prefix`() {
        val result = parse("Transaksi QRIS 1.250.000 berhasil")!!
        assertEquals(125_000_000L, result.amountCents)
        assertEquals(TxType.EXPENSE, result.type)
    }

    @Test
    fun `recognises incoming money`() {
        val result = parse("Dana masuk Rp 2.000.000 dari PT Contoh")!!
        assertEquals(TxType.INCOME, result.type)
        assertEquals(200_000_000L, result.amountCents)
    }

    @Test
    fun `salary wording counts as income`() {
        val result = parse("Gaji November Rp 10.000.000 telah ditransfer")!!
        assertEquals(TxType.INCOME, result.type)
    }

    @Test
    fun `ignores notifications without an amount`() {
        assertNull(parse("Anda berhasil login di aplikasi BCA"))
    }

    @Test
    fun `ignores notifications without a payment word`() {
        assertNull(parse("Promo Rp 50.000 untuk kamu, cek sekarang"))
    }

    @Test
    fun `ignores one time passwords`() {
        assertNull(parse("Kode OTP Anda 123456. Jangan bagikan."))
    }

    @Test
    fun `rejects dates and plain numbers`() {
        assertNull(parse("Rapat dijadwalkan 12 03 2026"))
    }

    @Test
    fun `handles two decimal currencies`() {
        val result = CaptureParser.parse(
            "You paid $12.90 at Blue Bottle",
            "Wallet",
            "USD"
        )!!
        assertEquals(1_290L, result.amountCents)
        assertEquals(TxType.EXPENSE, result.type)
    }

    @Test
    fun `handles euro amounts`() {
        val result = CaptureParser.parse("Payment €5,25 confirmed", "Bank", "EUR")!!
        assertEquals(TxType.EXPENSE, result.type)
        assertEquals(525L, result.amountCents)
    }

    @Test
    fun `refund wording is income`() {
        val result = parse("Refund Rp 75.000 telah diterima")!!
        assertEquals(TxType.INCOME, result.type)
    }

    @Test
    fun `falls back to a generic source name`() {
        val result = CaptureParser.parse("Pembayaran Rp 10.000 berhasil", "  ")!!
        assertEquals("Notification", result.source)
    }

    @Test
    fun `summary is human readable`() {
        val result = parse("Pembayaran Rp 50.000 berhasil")!!
        assertEquals("Rp 50.000 · payment", result.summary)
    }

    @Test
    fun `mixed wording prefers expense when both appear`() {
        // "transfer" appears on both sides; "pembayaran" makes it a spend.
        val result = parse("Pembayaran transfer Rp 100.000 berhasil")!!
        assertEquals(TxType.EXPENSE, result.type)
    }
}