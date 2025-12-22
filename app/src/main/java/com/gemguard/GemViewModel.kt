package com.gemguard

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import java.text.SimpleDateFormat
import java.util.*

class GemViewModel : ViewModel() {
    private val _diamonds = mutableStateOf(0)
    val diamonds: State<Int> = _diamonds

    private val _currentSteps = mutableStateOf(0)
    val currentSteps: State<Int> = _currentSteps

    private val _whitelistedApps = mutableStateListOf<String>()
    val whitelistedApps: List<String> = _whitelistedApps

    val allInstalledApps = mutableStateListOf<AppInfoData>()
    val unlockedAppsTime = mutableStateMapOf<String, Long>()
    // בתוך GemViewModel.kt
    var setupStep = mutableIntStateOf(1) // 1: שפה/PIN, 2: צעדים, 3: חסימות
    private val _claimedTaskIds = mutableStateListOf<Int>()
    val claimedTaskIds: List<Int> = _claimedTaskIds

    // --- הגדרות משתמש ומצב אפליקציה ---
    var appPin = mutableStateOf("")
    var isDarkMode = mutableStateOf(false)
    var language = mutableStateOf("iw") // "iw" לעברית, "en" לאנגלית

    // המשתנה שפותר את בעיית ה-Navbar:
    var isSetupCompleteState = mutableStateOf(false)

    data class AppInfoData(val name: String, val packageName: String, val usageTime: Long = 0)

    fun initData(context: Context) {
        val prefs = context.getSharedPreferences("GemGuardPrefs", Context.MODE_PRIVATE)

        // טעינת הגדרות בסיסיות
        _diamonds.value = prefs.getInt("diamonds", 0)
        appPin.value = prefs.getString("app_pin", "") ?: ""
        isDarkMode.value = prefs.getBoolean("dark_mode", false)
        language.value = prefs.getString("language", "iw") ?: "iw"

        // עדכון מצב ה-setup ב-state
        isSetupCompleteState.value = prefs.getBoolean("setup_complete", false)

        // טעינת Whitelist וודוא שהגדרות המכשיר תמיד בפנים
        val savedWhitelist = prefs.getString("whitelist", "") ?: ""
        _whitelistedApps.clear()
        if (savedWhitelist.isNotEmpty()) {
            _whitelistedApps.addAll(savedWhitelist.split(","))
        }
        if (!_whitelistedApps.contains("com.android.settings")) {
            _whitelistedApps.add("com.android.settings")
        }

        val savedTasks = prefs.getString("claimed_tasks", "") ?: ""
        if (savedTasks.isNotEmpty()) {
            _claimedTaskIds.clear()
            _claimedTaskIds.addAll(savedTasks.split(",").mapNotNull { it.toIntOrNull() })
        }

        // טעינת זמנים פתוחים
        val allPrefs = prefs.all
        unlockedAppsTime.clear()
        for ((key, value) in allPrefs) {
            if (key.startsWith("unlock_") && value is Long) {
                if (value > System.currentTimeMillis()) {
                    unlockedAppsTime[key.replace("unlock_", "")] = value
                }
            }
        }
    }

    // שמירת כל ההגדרות (PIN, שפה, Dark Mode, Whitelist)
    fun saveSettings(context: Context) {
        val prefs = context.getSharedPreferences("GemGuardPrefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("app_pin", appPin.value)
            putBoolean("dark_mode", isDarkMode.value)
            putString("language", language.value)
            putString("whitelist", _whitelistedApps.joinToString(","))
            putBoolean("setup_complete", true)
            apply()
        }
        // עדכון ה-State כדי שה-Navbar יופיע מיד
        isSetupCompleteState.value = true
    }

    // פונקציות עזר לשינוי הגדרות
    fun toggleDarkMode() { isDarkMode.value = !isDarkMode.value }
    fun setLanguage(lang: String) { language.value = lang }

    fun loadInstalledApps(context: Context) {
        val pm = context.packageManager
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startTime = calendar.timeInMillis

        val usageStats = usageStatsManager.queryAndAggregateUsageStats(startTime, endTime)
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val tempList = mutableListOf<AppInfoData>()

        for (app in apps) {
            val launchIntent = pm.getLaunchIntentForPackage(app.packageName)
            if (launchIntent != null) {
                val label = app.loadLabel(pm).toString()
                val totalTime = usageStats[app.packageName]?.totalTimeInForeground ?: 0L
                val isIrrelevant = app.packageName.startsWith("com.android.systemui") || app.packageName == "android"

                if (!isIrrelevant) {
                    tempList.add(AppInfoData(label, app.packageName, totalTime))
                }
            }
        }
        tempList.sortByDescending { it.usageTime }
        allInstalledApps.clear()
        allInstalledApps.addAll(tempList)
    }

    fun buyTimeForApp(packageName: String, minutes: Int, cost: Int, context: Context): Boolean {
        if (_diamonds.value >= cost) {
            _diamonds.value -= cost
            val currentTime = System.currentTimeMillis()
            val currentExpiry = unlockedAppsTime[packageName] ?: currentTime
            val newExpiry = (if (currentExpiry > currentTime) currentExpiry else currentTime) + (minutes.toLong() * 60 * 1000)

            unlockedAppsTime[packageName] = newExpiry

            val prefs = context.getSharedPreferences("GemGuardPrefs", Context.MODE_PRIVATE)
            prefs.edit().apply {
                putLong("unlock_$packageName", newExpiry)
                putInt("diamonds", _diamonds.value)
                apply()
            }
            return true
        }
        return false
    }

    fun toggleWhitelist(packageName: String) {
        if (packageName == "com.android.settings") return
        if (_whitelistedApps.contains(packageName)) {
            _whitelistedApps.remove(packageName)
        } else {
            _whitelistedApps.add(packageName)
        }
    }

    fun addDiamonds(amount: Int, taskId: Int, context: Context) {
        if (!_claimedTaskIds.contains(taskId)) {
            _diamonds.value += amount
            _claimedTaskIds.add(taskId)

            val prefs = context.getSharedPreferences("GemGuardPrefs", Context.MODE_PRIVATE)
            prefs.edit().apply {
                putInt("diamonds", _diamonds.value)
                putString("claimed_tasks", _claimedTaskIds.joinToString(","))
                apply()
            }
        }
    }

    fun updateStepsWithContext(totalStepsFromSensor: Int, context: Context) {
        val prefs = context.getSharedPreferences("GemGuardPrefs", Context.MODE_PRIVATE)
        val todayDate = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())

        var initialSteps = prefs.getInt("initial_steps", -1)
        val lastDate = prefs.getString("last_date", "")

        if (todayDate != lastDate || initialSteps == -1) {
            initialSteps = totalStepsFromSensor
            prefs.edit()
                .putString("last_date", todayDate)
                .putInt("initial_steps", initialSteps)
                .apply()
        }

        _currentSteps.value = (totalStepsFromSensor - initialSteps).coerceAtLeast(0)
    }
}