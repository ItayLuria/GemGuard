package com.gemguard

import android.app.*
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.*
import android.os.Build
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import android.content.pm.PackageManager
import kotlinx.coroutines.*

class BlockService : Service() {
    private val notifiedApps = mutableSetOf<String>()
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)

    private val systemSafePackages = listOf(
        "com.android.settings",
        "com.google.android.permissioncontroller",
        "com.android.packageinstaller",
        "com.google.android.settings.intelligence",
        "com.android.systemui",
        "android"
    )

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, "block_service")
            .setContentTitle("GemGuard פעיל")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setOngoing(true)
            .build()
        startForeground(1, notification)

        serviceScope.launch {
            while (isActive) {
                checkTopApp()
                // דגימה מהירה מאוד (250ms) כדי לסגור כל ניסיון פתיחה
                delay(250)
            }
        }

        return START_STICKY
    }

    private fun checkTopApp() {
        val prefs = getSharedPreferences("GemGuardPrefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("setup_complete", false)) return

        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val time = System.currentTimeMillis()

        // שיטה משולבת: בודקים את האפליקציה האחרונה שדווחה כפעילה ב-5 השניות האחרונות
        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 5000, time)
        val topApp = stats?.maxByOrNull { it.lastTimeUsed }?.packageName ?: return

        if (shouldBlock(topApp, prefs)) {
            lockApp(topApp)
        }
    }

    private fun shouldBlock(topApp: String, prefs: SharedPreferences): Boolean {
        if (topApp == packageName || isLauncherApp(topApp) || systemSafePackages.contains(topApp)) {
            return false
        }

        val whitelist = prefs.getString("whitelist", "")?.split(",") ?: listOf()
        if (whitelist.contains(topApp)) return false

        val expiryTime = prefs.getLong("unlock_$topApp", 0L)
        val remaining = expiryTime - System.currentTimeMillis()

        if (remaining in 1..60000 && !notifiedApps.contains(topApp)) {
            sendWarning(topApp)
            notifiedApps.add(topApp)
        }

        return remaining <= 0
    }

    private fun lockApp(packageName: String) {
        notifiedApps.remove(packageName)

        // 1. קוד ה"פטיש": שליחת המשתמש למסך הבית.
        // זה מבטל Split Screen באופן מיידי ברוב הגרסאות של אנדרואיד.
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(homeIntent)

        // 2. פתיחת GemGuard עם דיאלוג החסימה בהשהייה קצרה מאוד
        Handler(Looper.getMainLooper()).postDelayed({
            val lockIntent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                putExtra("blocked_app", packageName)
            }
            startActivity(lockIntent)
        }, 150) // השהייה של 150ms כדי לוודא שמסך הבית תפס את הפוקוס
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
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(2, n)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("block_service", "GemGuard Service", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    override fun onBind(p0: Intent?): IBinder? = null
}