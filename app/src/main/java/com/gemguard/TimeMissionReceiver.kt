package com.gemguard

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import com.gemguard.R
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class TimeMissionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("GemGuardPrefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("time_mission_active", false)) {
            return
        }
        createTimeMission(context)
        scheduleNextMission(context)
    }

    private fun createTimeMission(context: Context) {
        val prefs = context.getSharedPreferences("GemGuardPrefs", Context.MODE_PRIVATE)

        val stepsGoal = Random.nextInt(500, 3001)
        val timeLimitMinutes = Random.nextInt(30, 91)
        val reward = (stepsGoal / 100) + (timeLimitMinutes / 5)

        val endTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(timeLimitMinutes.toLong())
        
        // This value must be updated by the ViewModel whenever the step count changes.
        val lastKnownTotalSteps = prefs.getInt("last_known_total_steps", 0)

        prefs.edit(commit = true) {
            putBoolean("time_mission_active", true)
            putInt("time_mission_steps_goal", stepsGoal)
            putLong("time_mission_end_time", endTime)
            putInt("time_mission_reward", reward)
            putInt("time_mission_start_steps", lastKnownTotalSteps)
        }

        sendNotification(context, stepsGoal, timeLimitMinutes)
    }

    private fun sendNotification(context: Context, steps: Int, minutes: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "time_mission_channel"

        // Channel creation is idempotent and safe to call multiple times.
        val channel = NotificationChannel(channelId, "Time Missions", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Notifications for surprise time missions"
        }
        notificationManager.createNotificationChannel(channel)
        
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Use a reliable fallback icon
            .setContentTitle(context.getString(R.string.time_mission_title))
            .setContentText(context.getString(R.string.time_mission_notification_text, steps, minutes))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(soundUri)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(3, notification)
    }

    companion object {
        fun scheduleNextMission(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, TimeMissionReceiver::class.java)
            // Using FLAG_IMMUTABLE is a requirement for newer Android versions.
            val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                // If it's already past 8 PM, schedule for tomorrow.
                if (get(Calendar.HOUR_OF_DAY) >= 20) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
                // Set a random time between 12:00 (noon) and 20:00 (8 PM).
                set(Calendar.HOUR_OF_DAY, Random.nextInt(12, 20))
                set(Calendar.MINUTE, Random.nextInt(0, 60))
                set(Calendar.SECOND, 0)
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // For Android 12+, we need to check if we can schedule exact alarms.
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                    } else {
                        // Optional: Fallback to a less exact alarm or log an error.
                        Log.w("TimeMissionReceiver", "Cannot schedule exact alarms. Time mission might be delayed.")
                    }
                } else {
                    // For older versions, set the alarm directly.
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                }
            } catch (e: SecurityException) {
                // This might happen if the permission is revoked while the app is running.
                Log.e("TimeMissionReceiver", "SecurityException: Could not schedule exact alarm.", e)
            }
        }
    }
}
