package com.example.sportpulse.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.sportpulse.domain.MatchInfo
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.math.absoluteValue

class MatchNotificationScheduler(
    private val context: Context,
) {
    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Match reminders",
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun schedule(match: MatchInfo, minutesBefore: Int): Boolean {
        val startAt = match.startTimeMillis() ?: return false
        val triggerAt = startAt - minutesBefore * 60_000L
        if (triggerAt <= System.currentTimeMillis()) return false

        val intent = Intent(context, MatchReminderReceiver::class.java)
            .putExtra(MatchReminderReceiver.EXTRA_TITLE, match.title)
            .putExtra(
                MatchReminderReceiver.EXTRA_BODY,
                if (minutesBefore == 0) {
                    "The match starts now"
                } else {
                    "The match starts in $minutesBefore min."
                },
            )

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            "${match.id}:$minutesBefore".hashCode().absoluteValue,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        return true
    }

    private fun MatchInfo.startTimeMillis(): Long? {
        if (date.isBlank()) return null
        val sourceTime = time.takeIf { it.isNotBlank() } ?: "00:00:00"
        val parser = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return runCatching { parser.parse("$date $sourceTime")?.time }.getOrNull()
    }

    companion object {
        const val CHANNEL_ID = "match_reminders"
    }
}
