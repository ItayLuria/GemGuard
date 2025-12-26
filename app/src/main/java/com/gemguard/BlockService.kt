package com.gemguard

import android.app.*
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class BlockService : Service() {
    private val notifiedApps = mutableSetOf<String>()
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)

    private var currentForegroundApp: String? = null

    private val systemSafePackages = listOf(
        "com.android.settings",
        "com.google.android.permissioncontroller",
        "com.android.packageinstaller",
        "com.google.android.settings.intelligence",
        "com.android.systemui",
        "android",
        "com.gemguard"
    )

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, "block_service")
            .setContentTitle("GemGuard פעיל")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

        startForeground(1, notification)

        serviceScope.launch {
            while (isActive) {
                checkTopApp()
                delay(300) // 300ms זה איזון מצוין לחסימה מהירה אך יציבה
            }
        }

        return START_STICKY
    }

    private fun checkTopApp() {
        val prefs = getSharedPreferences("GemGuardPrefs", Context.MODE_PRIVATE)

        if (!prefs.getBoolean("service_enabled", true)) return
        if (!prefs.getBoolean("setup_complete", false)) return

        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 1500

        val events = usageStatsManager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()

        var lastApp: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                lastApp = event.packageName
            }
        }

        if (lastApp != null) {
            // אם אנחנו כבר בתוך GemGuard, אל תבצע חסימה (מונע לולאה והבהובים)
            if (lastApp == packageName) return
            currentForegroundApp = lastApp
        }

        val appToCheck = currentForegroundApp ?: return

        if (shouldBlock(appToCheck, prefs)) {
            lockApp(appToCheck)
        }
    }

    private fun shouldBlock(topApp: String, prefs: SharedPreferences): Boolean {
        if (topApp == packageName || isLauncherApp(topApp) || systemSafePackages.contains(topApp)) {
            return false
        }

        val whitelist = prefs.getString("whitelist", "")?.split(",") ?: listOf()
        if (whitelist.contains(topApp)) return false

        val expiryTime = prefs.getLong("unlock_$topApp", 0L)
        if (expiryTime == 0L) return true

        val currentTime = System.currentTimeMillis()
        val remaining = expiryTime - currentTime

        if (remaining in 1..60000 && !notifiedApps.contains(topApp)) {
            sendWarning(topApp)
            notifiedApps.add(topApp)
        }

        return remaining <= 0
    }

    private fun lockApp(packageName: String) {
        notifiedApps.remove(packageName)

        // פתיחה ישירה של הדיאלוג מעל האפליקציה החסומה
        val lockIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT) // מביא אותנו לקדמת המסך בצורה חלקה
            putExtra("blocked_app", packageName)
        }

        try {
            startActivity(lockIntent)
        } catch (e: Exception) {
            // גיבוי למקרה שמשהו נכשל - שליחה למסך הבית
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(homeIntent)
        }
    }

    private fun isLauncherApp(packageName: String): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.resolveActivity(intent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            packageManager.resolveActivity(intent, 0)
        }
        return resolveInfo?.activityInfo?.packageName == packageName
    }

    private fun sendWarning(appName: String) {
        val n = NotificationCompat.Builder(this, "block_service")
            .setContentTitle("זמן עומד להיגמר!")
            .setContentText("נשאר פחות מדקה לשימוש ב-$appName")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(2, n)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "block_service",
                "GemGuard Protection Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        serviceJob.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}