package com.ledgerly.app.data

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * One daily background check that decides whether a nudge is due. WorkManager owns the timing,
 * so the app needs no exact-alarm permission and nothing runs while the phone is idle in Doze.
 */
class ReminderWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val settings = LedgerRepository(context).loadSettings()
        if (!settings.remindToLog) return Result.success()

        Reminders.ensureChannels(context)
        val today = LocalDate.now()
        val transactions = LedgerRepository(context).loadTransactions()
        val spentToday = transactions
            .filter { it.type == TxType.EXPENSE && it.date == today }
            .sumOf { it.amountCents }

        Reminders.maybeDailyNudge(
            context = context,
            enabled = settings.remindToLog,
            spentTodayCents = spentToday,
            currency = settings.currency,
            name = settings.name
        )

        val weekAgo = today.minusDays(6)
        val spentWeek = transactions
            .filter { it.type == TxType.EXPENSE && it.date in weekAgo..today }
            .sumOf { it.amountCents }
        Reminders.maybeWeeklyReview(
            context = context,
            enabled = settings.weeklyReview,
            spentCents = spentWeek,
            currency = settings.currency
        )
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "ledgerly-daily-reminder"

        /** Called whenever the reminder setting changes. */
        fun sync(context: Context, enabled: Boolean) {
            val manager = WorkManager.getInstance(context)
            if (!enabled) {
                manager.cancelUniqueWork(WORK_NAME)
                Reminders.cancelAll(context)
                return
            }
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
                .setConstraints(Constraints.NONE)
                .setInitialDelay(hoursUntilEvening(), TimeUnit.MILLISECONDS)
                .build()
            manager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        /** Aims the first run at ~20:00 local time, when people are likely to log the day. */
        private fun hoursUntilEvening(): Long {
            val now = java.time.LocalTime.now()
            val target = java.time.LocalTime.of(20, 0)
            val millis = java.time.Duration.between(now, target).toMillis()
            return if (millis > 0) millis else millis + TimeUnit.DAYS.toMillis(1)
        }
    }
}