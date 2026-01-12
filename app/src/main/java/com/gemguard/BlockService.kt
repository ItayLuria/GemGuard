package com.gemguard

import android.app.*
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.*
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class BlockService : Service(), SensorEventListener {
    private val notifiedApps = mutableSetOf<String>()
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)

    private var currentForegroundApp: String? = null
    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null

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
        
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        stepSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, "block_service")
            .setContentTitle("GemGuard Protection Active")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

        startForeground(1, notification)

        serviceScope.launch {
            while (isActive) {
                checkTopApp()
                delay(250)
            }
        }

        return START_STICKY
    }

    private fun checkTopApp() {
        val prefs = getSharedPreferences("GemGuardPrefs", Context.MODE_PRIVATE)
        if (prefs.getString("user_role", null) == "parent") {
            stopSelf()
            return
        }

        val isServiceEnabled = prefs.getBoolean("service_enabled", true)
        if (!isServiceEnabled || !prefs.getBoolean("setup_complete", false)) return

        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 1000

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

        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(homeIntent)

        Handler(Looper.getMainLooper()).postDelayed({
            val lockIntent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra("blocked_app", packageName)
            }
            startActivity(lockIntent)
        }, 150)
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
            .setContentTitle("Time is almost up!")
            .setContentText("Less than a minute left for $appName")
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

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            val totalSteps = event.values[0].toInt()
            val viewModel = GemViewModel(application)
            viewModel.updateStepsOptimized(totalSteps)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        serviceJob.cancel()
        sensorManager.unregisterListener(this)
        super.onDestroy()
    }

    override fun onBind(p0: Intent?): IBinder? = null
}