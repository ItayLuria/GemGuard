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

        // --- 1. הגרלת יעד צעדים (בין 1000 ל-3000 בקפיצות של 50) ---
        val stepsSteps = (3000 - 1000) / 50
        val stepsGoal = 1000 + (Random.nextInt(0, stepsSteps + 1) * 50)

        // --- 2. חישוב פרס לפי שלוש רמות (אופציות) של קושי ---
        val reward = when {
            // אופציה 1: משימה קלה (1000 עד 1950 צעדים)
            stepsGoal < 2000 -> {
                // פרס: בין 50 ל-100 (בקפיצות של 10)
                val rewardChoices = (100 - 50) / 10
                50 + (Random.nextInt(0, rewardChoices + 1) * 10)
            }

            // אופציה 2: משימה בינונית (2000 עד 2500 צעדים)
            stepsGoal <= 2500 -> {
                // פרס: בין 100 ל-150 (בקפיצות של 10)
                val rewardChoices = (150 - 100) / 10
                100 + (Random.nextInt(0, rewardChoices + 1) * 10)
            }

            // אופציה 3: משימה קשה / מאתגרת (2550 עד 3000 צעדים)
            else -> {
                // פרס: בין 150 ל-200 (בקפיצות של 10)
                val rewardChoices = (200 - 150) / 10
                150 + (Random.nextInt(0, rewardChoices + 1) * 10)
            }
        }

        // --- 3. זמן: שעה עגולה (ללא שינוי) ---
        val timeLimitMinutes = 60
        val endTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(timeLimitMinutes.toLong())

        val lastKnownTotalSteps = prefs.getInt("last_known_total_steps", 0)

        prefs.edit(commit = true) {
            putBoolean("time_mission_active", true)
            putInt("time_mission_steps_goal", stepsGoal)
            putLong("time_mission_end_time", endTime)
            putInt("time_mission_reward", reward)
            putInt("time_mission_start_steps", lastKnownTotalSteps)
        }

        sendNotification(context, stepsGoal, timeLimitMinutes, reward)
    }

    private fun sendNotification(context: Context, steps: Int, minutes: Int, reward: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "time_mission_channel"

        // שליפת השפה מהגדרות האפליקציה
        val prefs = context.getSharedPreferences("GemGuardPrefs", Context.MODE_PRIVATE)
        val lang = prefs.getString("language", "iw") ?: "iw"
        val isHebrew = lang == "iw"

        val title = if (isHebrew) {
            "משימת בונוס! | $reward Gems"
        } else {
            "Bonus Mission! | $reward Gems"
        }

        val content = if (isHebrew) {
            "תלך $steps צעדים בתוך שעה והיהלומים שלך."
        } else {
            "Walk $steps steps within an hour and the gems are yours."
        }

        val channel = NotificationChannel(channelId, "Time Missions", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Notifications for surprise time missions"
        }
        notificationManager.createNotificationChannel(channel)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content)) // מאפשר לראות את כל הטקסט גם אם הוא ארוך
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
