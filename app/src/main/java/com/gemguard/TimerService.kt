package com.gemguard

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class TimerService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var timerJob: Job? = null

    companion object {
        var activeTimers = mutableMapOf<String, Long>()
        var appNames = mutableMapOf<String, String>()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val packageName = intent?.getStringExtra("package_name")
        val expiryTime = intent?.getLongExtra("expiry_time", 0L) ?: 0L
        val appName = intent?.getStringExtra("app_name")

        if (packageName != null && appName != null) {
            activeTimers[packageName] = expiryTime
            appNames[packageName] = appName
        }

        createNotificationChannel()
        startGlobalTimer()

        return START_STICKY
    }

    private fun startGlobalTimer() {
        if (timerJob?.isActive == true) return

        // שליפת השפה השמורה כדי להציג התראה נכונה
        val prefs = getSharedPreferences("gemguard_prefs", Context.MODE_PRIVATE)
        val language = prefs.getString("language", "en") ?: "en"
        val isHebrew = language == "iw"

        timerJob = serviceScope.launch {
            while (isActive) {
                val currentTime = System.currentTimeMillis()
                val currentActive = activeTimers.filter { it.value > currentTime }

                if (currentActive.isEmpty()) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    break
                }

                // בניית טקסט ההתראה לפי שפה
                val notificationText = currentActive.map { (pkg, expiry) ->
                    val timeLeft = expiry - currentTime
                    val minutes = (timeLeft / 1000) / 60
                    val seconds = (timeLeft / 1000) % 60
                    val name = appNames[pkg] ?: (if (isHebrew) "אפליקציה" else "App")
                    val timeString = String.format("%02d:%02d", minutes, seconds)

                    if (isHebrew) "$name: נותרו $timeString" else "$name: $timeString left"
                }.joinToString("\n")

                val title = if (isHebrew) "אפליקציות פתוחות" else "Unlocked Apps"

                val notification = NotificationCompat.Builder(this@TimerService, "timer_channel")
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(title)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(notificationText))
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .build()

                startForeground(1, notification)
                delay(1000)
            }
        }
    }

    private fun createNotificationChannel() {
        val prefs = getSharedPreferences("gemguard_prefs", Context.MODE_PRIVATE)
        val isHebrew = prefs.getString("language", "en") == "iw"

        val channelName = if (isHebrew) "טיימרים של GemGuard" else "GemGuard Timers"

        val channel = NotificationChannel(
            "timer_channel",
            channelName,
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        timerJob?.cancel()
        super.onDestroy()
    }
}