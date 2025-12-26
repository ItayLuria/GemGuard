package com.gemguard

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class TimerService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var timerJob: Job? = null

    companion object {
        // רשימת הטיימרים הפעילים: Package Name -> Expiry Timestamp
        var activeTimers = mutableMapOf<String, Long>()
        var appNames = mutableMapOf<String, String>()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val packageName = intent?.getStringExtra("package_name")
        val expiryTime = intent?.getLongExtra("expiry_time", 0L) ?: 0L
        val appName = intent?.getStringExtra("app_name")

        // אם קיבלנו מידע תקין על טיימר חדש, נוסיף אותו לרשימה
        if (packageName != null && appName != null && expiryTime > System.currentTimeMillis()) {
            activeTimers[packageName] = expiryTime
            appNames[packageName] = appName
        }

        createNotificationChannel()
        startGlobalTimer()

        return START_STICKY
    }

    private fun startGlobalTimer() {
        if (timerJob?.isActive == true) return

        val prefs = getSharedPreferences("GemGuardPrefs", Context.MODE_PRIVATE)
        val language = prefs.getString("language", "en") ?: "en"
        val isHebrew = language == "iw"

        timerJob = serviceScope.launch {
            while (isActive) {
                val currentTime = System.currentTimeMillis()

                // 1. ניקוי אפליקציות שפג תוקפן
                val expiredApps = activeTimers.filter { it.value <= currentTime }.keys
                expiredApps.forEach { key ->
                    activeTimers.remove(key)
                    appNames.remove(key)
                }

                // 2. קביעת תוכן ההתראה לפי מצב הרשימה
                val isListEmpty = activeTimers.isEmpty()
                val title: String
                val contentText: String

                if (isListEmpty) {
                    title = "GemGuard"
                    contentText = if (isHebrew) "אין אפליקציות פעילות כרגע" else "No active apps right now"
                } else {
                    title = if (isHebrew) "אפליקציות פעילות" else "Active Apps"
                    val notificationLines = activeTimers.map { (pkg, expiry) ->
                        val timeLeft = (expiry - currentTime).coerceAtLeast(0)
                        val minutes = (timeLeft / 1000) / 60
                        val seconds = (timeLeft / 1000) % 60
                        val name = appNames[pkg] ?: (if (isHebrew) "אפליקציה" else "App")
                        val timeString = String.format("%02d:%02d", minutes, seconds)
                        if (isHebrew) "$name: $timeString" else "$name: $timeString left"
                    }
                    contentText = notificationLines.joinToString("\n")
                }

                // 3. בניית ועדכון ההתראה
                val notificationBuilder = NotificationCompat.Builder(this@TimerService, "timer_channel")
                    .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                    .setContentTitle(title)
                    .setContentText(if (isListEmpty) contentText else contentText.lines().first())
                    .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
                    .setOnlyAlertOnce(true)
                    .setPriority(NotificationCompat.PRIORITY_LOW)

                if (isListEmpty) {
                    // כשהרשימה ריקה: מאפשרים למשתמש להחליק את ההתראה (מבטלים ongoing)
                    notificationBuilder.setOngoing(false)
                    val finalNotification = notificationBuilder.build()

                    // עדכון אחרון לפני סגירה
                    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    manager.notify(1, finalNotification)

                    // עצירת השירות והסרת מצב Foreground (מבלי למחוק את ההתראה האחרונה)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        stopForeground(STOP_FOREGROUND_DETACH)
                    } else {
                        stopForeground(false)
                    }
                    stopSelf()
                    break // יוצאים מהלולאה
                } else {
                    // כשיש אפליקציות: ההתראה דביקה
                    notificationBuilder.setOngoing(true)
                    startForeground(1, notificationBuilder.build())
                }

                delay(1000)
            }
        }
    }
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val prefs = getSharedPreferences("GemGuardPrefs", Context.MODE_PRIVATE)
            val isHebrew = prefs.getString("language", "en") == "iw"

            val channelName = if (isHebrew) "טיימרים פעילים" else "Active Timers"

            val channel = NotificationChannel(
                "timer_channel",
                channelName,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = if (isHebrew) "מציג את הזמן הנותר לאפליקציות" else "Shows remaining time for apps"
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        timerJob?.cancel()
        super.onDestroy()
    }
}