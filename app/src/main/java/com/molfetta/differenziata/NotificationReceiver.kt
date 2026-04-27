package com.molfetta.differenziata

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.time.LocalDate
import java.util.Calendar

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        createNotificationChannel(context)

        // La notifica delle 20:30 riguarda la raccolta del giorno DOPO
        val tomorrow = LocalDate.now().plusDays(1)
        val wasteItems = WasteSchedule.getWasteForDay(tomorrow.dayOfWeek)

        if (wasteItems.isNotEmpty()) {
            val wasteText = wasteItems.joinToString(" + ") { "${it.emoji} ${it.title}" }

            val openIntent = PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("🗑️ È ora di buttare!")
                .setContentText(wasteText)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("$wasteText\n\nEsponi i contenitori entro le 24:00 di stanotte.")
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(openIntent)
                .setAutoCancel(true)
                .build()

            try {
                NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            } catch (_: SecurityException) {
                // POST_NOTIFICATIONS non concesso
            }
        }

        // Si riprogramma autonomamente per il giorno successivo
        scheduleNextAlarm(context)
    }

    companion object {
        const val CHANNEL_ID = "differenziata_reminders"
        private const val NOTIFICATION_ID = 1001

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Promemoria raccolta differenziata",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifica serale alle 20:30 per esporre i rifiuti"
                }
                context.getSystemService(NotificationManager::class.java)
                    .createNotificationChannel(channel)
            }
        }

        fun scheduleNextAlarm(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                return
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context, 0,
                Intent(context, NotificationReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 20)
                set(Calendar.MINUTE, 30)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, target.timeInMillis, pendingIntent
            )
        }
    }
}
