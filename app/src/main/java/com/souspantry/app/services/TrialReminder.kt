package com.souspantry.app.services

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
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.souspantry.app.MainActivity
import com.souspantry.app.R
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * The local notification promised on the funnel's "We'll remind you" screen
 * (mirrors iOS `TrialReminder.scheduleFromPurchase`). Schedule it ONLY after a
 * successful trial purchase: the trial starts then and ends [TRIAL_DAYS] later,
 * so the reminder fires `daysBefore` ahead of that, at ~10am local.
 */
object TrialReminder {
    const val TRIAL_DAYS = 7
    private const val WORK_NAME   = "sp_trialEndingReminder"
    private const val KEY_DAYS    = "daysBefore"
    private const val CHANNEL_ID  = "trial_reminder"

    /** @param reminderChoice the funnel value, "1_day" or "2_days". */
    fun scheduleFromPurchase(context: Context, reminderChoice: String) {
        val daysBefore = if (reminderChoice == "2_days") 2 else 1
        val request = OneTimeWorkRequestBuilder<TrialReminderWorker>()
            .setInitialDelay(fireDelayMillis(ZonedDateTime.now(), daysBefore), TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(KEY_DAYS to daysBefore))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    /** Millis from [now] until 10:00 local on the day `TRIAL_DAYS − daysBefore` days ahead. */
    internal fun fireDelayMillis(now: ZonedDateTime, daysBefore: Int): Long {
        val fireInDays = (TRIAL_DAYS - daysBefore).coerceAtLeast(0).toLong()
        val fireAt = now.toLocalDate().plusDays(fireInDays).atTime(LocalTime.of(10, 0)).atZone(now.zone)
        return Duration.between(now, fireAt).toMillis().coerceAtLeast(0)
    }

    class TrialReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
        override fun doWork(): Result {
            val ctx = applicationContext
            val daysBefore = inputData.getInt(KEY_DAYS, 1)
            val allowed = Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            if (!allowed || !NotificationManagerCompat.from(ctx).areNotificationsEnabled()) return Result.success()

            ctx.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Trial reminders", NotificationManager.IMPORTANCE_DEFAULT)
            )
            val open = PendingIntent.getActivity(
                ctx, 0, Intent(ctx, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val body = if (daysBefore <= 1)
                "Your free trial ends tomorrow. Keep your plans, recipes and pantry — or cancel anytime."
            else
                "Your free trial ends in $daysBefore days. Keep your plans, recipes and pantry — or cancel anytime."
            val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_fg)
                .setContentTitle("Your Sous Pantry trial ends soon")
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setContentIntent(open)
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(ctx).notify(WORK_NAME.hashCode(), notification)
            return Result.success()
        }
    }
}
