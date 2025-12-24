package com.gemguard

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

data class Task(val id: Int, val name: String, val requiredSteps: Int, val reward: Int) {
    fun isCompleted(claimedIds: List<Int>) = claimedIds.contains(id)
}

class GemViewModel : ViewModel() {
    private val _diamonds = mutableStateOf(0)
    val diamonds: State<Int> = _diamonds

    private val _currentSteps = mutableStateOf(0)
    val currentSteps: State<Int> = _currentSteps

    private val _whitelistedApps = mutableStateListOf<String>()
    val whitelistedApps: List<String> = _whitelistedApps

    val allInstalledApps = mutableStateListOf<AppInfoData>()
    val unlockedAppsTime = mutableStateMapOf<String, Long>()

    var setupStep = mutableIntStateOf(1)
    private val _claimedTaskIds = mutableStateListOf<Int>()
    val claimedTaskIds: List<Int> = _claimedTaskIds

    // רשימת משימות דינמית
    private val _dailyTasks = mutableStateListOf<Task>()
    val tasks: List<Task> = _dailyTasks

    var appPin = mutableStateOf("")
    var isDarkMode = mutableStateOf(false)
    var language = mutableStateOf("iw")
    var isSetupCompleteState = mutableStateOf(false)

    data class AppInfoData(val name: String, val packageName: String, val usageTime: Long = 0)

    fun initData(context: Context) {
        val prefs = context.getSharedPreferences("GemGuardPrefs", Context.MODE_PRIVATE)
        val todayDate = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val lastSavedDate = prefs.getString("last_task_date", "")

        // טעינת הגדרות בסיסיות
        _diamonds.value = prefs.getInt("diamonds", 0)
        appPin.value = prefs.getString("app_pin", "") ?: ""
        isDarkMode.value = prefs.getBoolean("dark_mode", false)
        language.value = prefs.getString("language", "iw") ?: "iw"
        isSetupCompleteState.value = prefs.getBoolean("setup_complete", false)

        // בדיקה אם עבר יום - אם כן, מאפסים משימות שבוצעו
        if (todayDate != lastSavedDate) {
            _claimedTaskIds.clear()
            prefs.edit()
                .putString("claimed_tasks", "")
                .putString("last_task_date", todayDate)
                .apply()
        } else {
            val savedTasks = prefs.getString("claimed_tasks", "") ?: ""
            if (savedTasks.isNotEmpty()) {
                _claimedTaskIds.clear()
                _claimedTaskIds.addAll(savedTasks.split(",").mapNotNull { it.toIntOrNull() })
            }
        }

        generateDailyTasks(todayDate)

        // טעינת Whitelist
        val savedWhitelist = prefs.getString("whitelist", "") ?: ""
        _whitelistedApps.clear()
        if (savedWhitelist.isNotEmpty()) _whitelistedApps.addAll(savedWhitelist.split(","))
        if (!_whitelistedApps.contains("com.android.settings")) _whitelistedApps.add("com.android.settings")

        // טעינת זמנים פתוחים
        val allPrefs = prefs.all
        unlockedAppsTime.clear()
        for ((key, value) in allPrefs) {
            if (key.startsWith("unlock_") && value is Long && value > System.currentTimeMillis()) {
                unlockedAppsTime[key.replace("unlock_", "")] = value
            }
        }
    }

    private fun generateDailyTasks(dateSeed: String) {
        val random = Random(dateSeed.hashCode().toLong())
        val baseSteps = listOf(1000, 2500, 5000, 7500, 10000)
        val baseRewards = listOf(20, 60, 150, 250, 500)

        _dailyTasks.clear()
        baseSteps.forEachIndexed { index, steps ->
            val factor = 0.9 + (random.nextDouble() * 0.2)
            val finalSteps = (steps * factor).toInt() / 50 * 50
            val finalReward = (baseRewards[index] * factor).toInt()
            _dailyTasks.add(Task(index + 1, "הליכה יומית", finalSteps, finalReward))
        }
    }

    // פונקציית הקנייה שהייתה חסרה וגרמה לשגיאה ב-StoreScreen
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
        isSetupCompleteState.value = true
    }

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
                if (!app.packageName.startsWith("com.android.systemui") && app.packageName != "android") {
                    tempList.add(AppInfoData(label, app.packageName, totalTime))
                }
            }
        }
        tempList.sortByDescending { it.usageTime }
        allInstalledApps.clear()
        allInstalledApps.addAll(tempList)
    }

    fun resetSetup(context: Context) {
        val prefs = context.getSharedPreferences("GemGuardPrefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("setup_complete", false).apply()
        setupStep.intValue = 1
        isSetupCompleteState.value = false
    }

    fun toggleWhitelist(packageName: String) {
        if (packageName == "com.android.settings") return
        if (_whitelistedApps.contains(packageName)) _whitelistedApps.remove(packageName)
        else _whitelistedApps.add(packageName)
    }

    fun updateStepsWithContext(totalStepsFromSensor: Int, context: Context) {
        val prefs = context.getSharedPreferences("GemGuardPrefs", Context.MODE_PRIVATE)
        val todayDate = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        var initialSteps = prefs.getInt("initial_steps", -1)
        val lastDate = prefs.getString("last_date", "")

        if (todayDate != lastDate || initialSteps == -1) {
            initialSteps = totalStepsFromSensor
            prefs.edit().putString("last_date", todayDate).putInt("initial_steps", initialSteps).apply()
        }
        _currentSteps.value = (totalStepsFromSensor - initialSteps).coerceAtLeast(0)
    }
}