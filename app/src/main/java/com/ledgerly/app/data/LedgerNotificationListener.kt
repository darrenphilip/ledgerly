package com.ledgerly.app.data

import android.app.Notification
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.time.LocalDate

/**
 * Listens for notifications that contain money moving, and remembers the last few so Ledgerly
 * can offer to log them. Never writes a transaction on its own: a wrong auto-entry is worse
 * than a prompt the user can ignore.
 *
 * The listener only runs while the user has granted Notification access in system settings.
 */
class LedgerNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn ?: return
        if (!isCaptureEnabled()) return
        if (sbn.packageName == packageName) return

        val extras = notification.notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val body = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val bigBody = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()
        val text = listOf(title, body, bigBody).filter { it.isNotBlank() }.joinToString(" ")
        if (text.isBlank()) return

        val source = runCatching {
            val info = packageManager.getApplicationInfo(sbn.packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        }.getOrDefault(sbn.packageName)

        val currency = CaptureStore.read(this).currency
        val candidate = CaptureParser.parse(text, source, currency) ?: return

        CaptureStore.remember(
            context = this,
            CaptureStore.Suggestion(
                id = "${sbn.packageName}:${sbn.postTime}",
                amountCents = candidate.amountCents,
                type = candidate.type,
                source = candidate.source,
                summary = candidate.summary,
                date = LocalDate.now(),
                rawText = text.take(160)
            )
        )
        Reminders.showCaptureReady(this, candidate)
    }

    private fun isCaptureEnabled(): Boolean =
        CaptureStore.read(this).enabled
}

/** Where detected payments wait until the user decides. */
object CaptureStore {

    data class Suggestion(
        val id: String,
        val amountCents: Long,
        val type: TxType,
        val source: String,
        val summary: String,
        val date: LocalDate,
        val rawText: String
    )

    data class State(
        val enabled: Boolean = false,
        val currency: String = "IDR",
        val suggestions: List<Suggestion> = emptyList()
    )

    private const val PREFS = "ledgerly_capture"
    private const val MAX = 6

    fun read(context: android.content.Context): State {
        val prefs = context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean("enabled", false)
        val currency = prefs.getString("currency", "IDR") ?: "IDR"
        val raw = prefs.getString("pending", null)
        val suggestions = raw?.let { decode(it) }.orEmpty()
        return State(enabled, currency, suggestions)
    }

    fun setEnabled(context: android.content.Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
            .edit().putBoolean("enabled", enabled).apply()
    }

    fun setCurrency(context: android.content.Context, currency: String) {
        context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
            .edit().putString("currency", currency).apply()
    }

    fun remember(context: android.content.Context, suggestion: Suggestion) {
        val current = read(context).suggestions
        if (current.any { it.id == suggestion.id }) return
        val next = (listOf(suggestion) + current).take(MAX)
        write(context, next)
    }

    fun dismiss(context: android.content.Context, id: String) {
        write(context, read(context).suggestions.filterNot { it.id == id })
    }

    fun clear(context: android.content.Context) = write(context, emptyList())

    private fun write(context: android.content.Context, suggestions: List<Suggestion>) {
        context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
            .edit()
            .putString("pending", encode(suggestions))
            .apply()
    }

    private fun encode(items: List<Suggestion>): String = items.joinToString("\u001F") { item ->
        listOf(
            item.id, item.amountCents.toString(), item.type.raw, item.source,
            item.summary.replace("\u001F", " "), item.date.toString(),
            item.rawText.replace("\u001F", " ")
        ).joinToString("\u001E")
    }

    private fun decode(raw: String): List<Suggestion> = raw.split("\u001F").mapNotNull { record ->
        val parts = record.split("\u001E")
        if (parts.size < 7) return@mapNotNull null
        val amount = parts[1].toLongOrNull() ?: return@mapNotNull null
        val date = runCatching { LocalDate.parse(parts[5]) }.getOrNull() ?: return@mapNotNull null
        Suggestion(
            id = parts[0],
            amountCents = amount,
            type = TxType.from(parts[2]),
            source = parts[3],
            summary = parts[4],
            date = date,
            rawText = parts[6]
        )
    }
}