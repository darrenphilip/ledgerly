package com.ledgerly.app.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.time.LocalDate

/**
 * All user-facing notifications: the daily "log today" nudge, the weekly review, and the
 * "we spotted a payment" prompt from notification capture.
 *
 * Nothing here fires without the user turning it on, and every one is a normal notification the
 * user can dismiss — no full-screen intents, no alarms.
 */
object Reminders {

    const val CHANNEL_REMINDERS = "reminders"
    const val CHANNEL_CAPTURE = "capture"

    private const val ID_DAILY = 1001
    private const val ID_WEEKLY = 1002
    private const val ID_CAPTURE = 1003

    const val ACTION_ADD_EXPENSE = "com.ledgerly.app.action.ADD_EXPENSE"
    const val ACTION_REVIEW = "com.ledgerly.app.action.REVIEW"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_REMINDERS,
                "Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "A nudge to log your spending, and a weekly review."
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_CAPTURE,
                "Payments detected",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "When a bank or wallet notification looks like a payment."
            }
        )
    }

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

    fun showDailyNudge(context: Context, spentTodayCents: Long, currency: String, name: String) {
        val body = when {
            spentTodayCents > 0L ->
                "You've logged ${com.ledgerly.app.util.Money.format(spentTodayCents, currency)} today. Anything else?"
            name.isNotBlank() -> "Nothing logged today, $name. Takes ten seconds."
            else -> "Nothing logged today. Takes ten seconds."
        }
        notify(
            context = context,
            channel = CHANNEL_REMINDERS,
            id = ID_DAILY,
            title = "Log today's spending",
            body = body,
            actionLabel = "Add expense",
            action = ACTION_ADD_EXPENSE
        )
    }

    fun showWeeklyReview(context: Context, spentCents: Long, currency: String, days: Int) {
        notify(
            context = context,
            channel = CHANNEL_REMINDERS,
            id = ID_WEEKLY,
            title = "Your week in money",
            body = "${com.ledgerly.app.util.Money.format(spentCents, currency)} over $days days. " +
                "Open Ledgerly to see where it went.",
            actionLabel = "Open",
            action = ACTION_REVIEW
        )
    }

    fun showCaptureReady(context: Context, candidate: CaptureParser.Candidate) {
        notify(
            context = context,
            channel = CHANNEL_CAPTURE,
            id = ID_CAPTURE,
            title = "${candidate.source}: ${candidate.summary}",
            body = "Tap to add it to Ledgerly. Nothing is saved until you confirm.",
            actionLabel = "Review",
            action = ACTION_REVIEW
        )
    }

    fun cancelAll(context: Context) {
        NotificationManagerCompat.from(context).apply {
            cancel(ID_DAILY)
            cancel(ID_WEEKLY)
            cancel(ID_CAPTURE)
        }
    }

    private fun notify(
        context: Context,
        channel: String,
        id: Int,
        title: String,
        body: String,
        actionLabel: String,
        action: String
    ) {
        if (!canNotify(context)) return

        val intent = Intent(context, com.ledgerly.app.MainActivity::class.java).apply {
            this.action = action
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pending = PendingIntent.getActivity(context, id, intent, flags)

        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(com.ledgerly.app.R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pending)
            .addAction(0, actionLabel, pending)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .build()

        runCatching { NotificationManagerCompat.from(context).notify(id, notification) }
    }

    /** Once a day, only if the user asked for reminders. */
    fun maybeDailyNudge(
        context: Context,
        enabled: Boolean,
        spentTodayCents: Long,
        currency: String,
        name: String
    ) {
        if (!enabled) return
        val prefs = context.getSharedPreferences("ledgerly_reminders", Context.MODE_PRIVATE)
        val today = LocalDate.now().toString()
        if (prefs.getString("last_daily", null) == today) return
        prefs.edit().putString("last_daily", today).apply()
        showDailyNudge(context, spentTodayCents, currency, name)
    }

    fun maybeWeeklyReview(context: Context, enabled: Boolean, spentCents: Long, currency: String) {
        if (!enabled) return
        val prefs = context.getSharedPreferences("ledgerly_reminders", Context.MODE_PRIVATE)
        val last = prefs.getLong("last_weekly", 0L)
        val week = 7L * 24 * 60 * 60 * 1000
        if (System.currentTimeMillis() - last < week) return
        prefs.edit().putLong("last_weekly", System.currentTimeMillis()).apply()
        showWeeklyReview(context, spentCents, currency, 7)
    }
}