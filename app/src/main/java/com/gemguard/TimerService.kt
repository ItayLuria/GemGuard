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
        // אם הטיימר כבר רץ, לא צריך להפעיל אותו מחדש
        if (timerJob?.isActive == true) return

        // שליפת השפה
        val prefs = getSharedPreferences("GemGuardPrefs", Context.MODE_PRIVATE)
        val language = prefs.getString("language", "en") ?: "en"
        val isHebrew = language == "iw"

        timerJob = serviceScope.launch {
            while (isActive) {
                val currentTime = System.currentTimeMillis()

                // 1. הסרת אפליקציות שהזמן שלהן נגמר
                val expiredApps = activeTimers.filter { it.value <= currentTime }.keys
                expiredApps.forEach { key ->
                    activeTimers.remove(key)
                    appNames.remove(key)
                }

                // אם אין יותר טיימרים פעילים - עוצרים את ה-Service ומסירים את ההתראה
                if (activeTimers.isEmpty()) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    break
                }

                // 2. בניית הטקסט להתראה
                // ניצור רשימה של שורות, שורה לכל אפליקציה
                val notificationLines = activeTimers.map { (pkg, expiry) ->
                    val timeLeft = expiry - currentTime
                    val minutes = (timeLeft / 1000) / 60
                    val seconds = (timeLeft / 1000) % 60
                    val name = appNames[pkg] ?: (if (isHebrew) "אפליקציה" else "App")
                    val timeString = String.format("%02d:%02d", minutes, seconds)

                    if (isHebrew) "$name: $timeString" else "$name: $timeString left"
                }

                // נחבר את השורות לטקסט אחד
                val contentText = notificationLines.joinToString("\n")

                // כותרת
                val title = if (isHebrew) "אפליקציות פעילות" else "Active Apps"

                // 3. עדכון ההתראה
                val notification = NotificationCompat.Builder(this@TimerService, "timer_channel")
                    .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                    .setContentTitle(title)
                    .setContentText(if (notificationLines.isNotEmpty()) notificationLines[0] else "") // מציג שורה ראשונה בטקסט הקצר
                    .setStyle(NotificationCompat.BigTextStyle().bigText(contentText)) // מציג את כל השורות בהרחבה
                    .setOngoing(true)
                    .setOnlyAlertOnce(true) // חשוב! מונע צפצוף/רעידה כל שנייה כשהטקסט מתעדכן
                    .setPriority(NotificationCompat.PRIORITY_LOW) // עדיפות נמוכה כדי לא להסתיר הודעות חשובות
                    .build()

                // מזהה 1 משמש לעדכון אותה התראה שוב ושוב
                startForeground(1, notification)

                // ממתינים שנייה לפני העדכון הבא
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