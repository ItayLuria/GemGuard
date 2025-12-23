package com.gemguard

import android.app.*
import android.app.usage.UsageStatsManager
import android.content.*
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import android.content.pm.PackageManager
class BlockService : Service() {
    private val notifiedApps = mutableSetOf<String>()

    // רשימת אפליקציות מערכת שלעולם לא נחסום
    private val systemSafePackages = listOf(
        "com.android.settings",
        "com.google.android.permissioncontroller",
        "com.android.packageinstaller",
        "com.google.android.settings.intelligence"
    )

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, "block_service")
            .setContentTitle("GemGuard פעיל")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .build()
        startForeground(1, notification)

        Thread {
            while (true) {
                checkTopApp()
                Thread.sleep(1000)
            }
        }.start()
        return START_STICKY
    }

    private fun checkTopApp() {
        val prefs = getSharedPreferences("GemGuardPrefs", Context.MODE_PRIVATE)

        // תיקון 1: אם ה-Setup לא הושלם, אל תחסום כלום!
        val isSetupComplete = prefs.getBoolean("setup_complete", false)
        if (!isSetupComplete) return

        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val time = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 10000, time)

        if (!stats.isNullOrEmpty()) {
            val topApp = stats.maxByOrNull { it.lastTimeUsed }?.packageName ?: return

            // תיקון 2: אל תחסום את GemGuard, את ה-Launcher או את הגדרות המערכת
            if (topApp == packageName || isLauncherApp(topApp) || systemSafePackages.contains(topApp)) {
                return
            }

            val whitelist = prefs.getString("whitelist", "")?.split(",") ?: listOf()
            val expiryTime = prefs.getLong("unlock_$topApp", 0L)
            val remaining = expiryTime - System.currentTimeMillis()

            if (remaining in 1..60000 && !notifiedApps.contains(topApp)) {
                sendWarning()
                notifiedApps.add(topApp)
            }

            if (!whitelist.contains(topApp) && remaining <= 0) {
                notifiedApps.remove(topApp)
                val lockIntent = Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    putExtra("blocked_app", topApp)
                }
                startActivity(lockIntent)
            }
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

    private fun sendWarning() {
        val n = NotificationCompat.Builder(this, "block_service")
            .setContentTitle("זמן עומד להיגמר!")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(2, n)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("block_service", "GemGuard", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onBind(p0: Intent?): IBinder? = null
}