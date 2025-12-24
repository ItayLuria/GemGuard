package com.gemguard

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.*
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

// מודל המשימה עם תמיכה בשתי שפות ופונקציית העזר שחוסכת שגיאות ב-Home.kt
data class Task(
    val id: Int,
    val nameHe: String,
    val nameEn: String,
    val requiredSteps: Int,
    val reward: Int
) {
    // הפונקציה הזו מוודאת ש-Home.kt יזהה את הקריאה .isCompleted()
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

    var setupStep = mutableIntStateOf(0)
    private val _claimedTaskIds = mutableStateListOf<Int>()
    val claimedTaskIds: List<Int> = _claimedTaskIds

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

        _diamonds.value = prefs.getInt("diamonds", 0)
        appPin.value = prefs.getString("app_pin", "") ?: ""
        isDarkMode.value = prefs.getBoolean("dark_mode", false)
        language.value = prefs.getString("language", "iw") ?: "iw"
        isSetupCompleteState.value = prefs.getBoolean("setup_complete", false)

        if (todayDate != lastSavedDate) {
            val lastClaimed = prefs.getString("claimed_tasks", "") ?: ""
            prefs.edit().putString("yesterday_claimed", lastClaimed).apply()
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

        generateSmartTasks(context)
        loadWhitelist(prefs)
        loadUnlockedApps(prefs)
    }

    private fun generateSmartTasks(context: Context) {
        val prefs = context.getSharedPreferences("GemGuardPrefs", Context.MODE_PRIVATE)
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        val nameHePool = listOf(
            listOf("לפתוח את הבוקר", "סיבוב קצר", "חצי דרך", "תותח צעדים", "מקצוען"),
            listOf("צעידה קלילה", "התחלנו לזוז", "עברנו חצי", "הליכה רצינית", "אין עליך"),
            listOf("צעד קטן", "קצת אוויר", "עוד קצת", "הליכה ארוכה", "שיחקת אותה"),
            listOf("בשביל ההרגשה", "סיבוב בשכונה", "בדרך ליעד", "מאמץ רציני", "הישג יומי"),
            listOf("כמה צעדים", "יוצאים החוצה", "קצת אירובי", "השקעה להיום", "סחתין עליך"),
            listOf("סיבוב קליל", "זזים קצת", "חצי בכיס", "קילומטרז'", "היעד נכבש"),
            listOf("לקום מהמיטה", "מטיילים מעט", "כמעט שם", "הליכה מאסיבית", "ניצחת את היום")
        )

        val nameEnPool = listOf(
            listOf("Morning Opener", "Short Circuit", "Halfway Mark", "Step Cannon", "Pro Walker"),
            listOf("Light Stroll", "Moving On", "Half Point", "Serious Hike", "Unstoppable"),
            listOf("Small Step", "Fresh Air", "A Bit More", "Long Walk", "Daily Winner"),
            listOf("Good Vibes", "Neighborhood Round", "On My Way", "Hard Effort", "Daily Peak"),
            listOf("Few Steps", "Heading Out", "Mild Cardio", "Today's Work", "Well Done"),
            listOf("Easy Round", "Getting Active", "In The Bag", "Milestone", "Goal Crushed"),
            listOf("Out of Bed", "Small Trip", "Almost There", "Massive Walk", "Day Conqueror")
        )

        val selectedHeNames = nameHePool[(dayOfWeek - 1) % nameHePool.size]
        val selectedEnNames = nameEnPool[(dayOfWeek - 1) % nameEnPool.size]

        val baseSteps = listOf(1000, 2500, 5000, 7500, 10000)
        val baseRewards = listOf(20, 60, 150, 250, 500)

        val yesterdayClaimed = prefs.getString("yesterday_claimed", "") ?: ""
        val yesterdayIds = yesterdayClaimed.split(",").mapNotNull { it.toIntOrNull() }

        _dailyTasks.clear()
        val editor = prefs.edit()

        baseSteps.forEachIndexed { index, steps ->
            val taskId = index + 1
            val taskNameHe = selectedHeNames[index]
            val taskNameEn = selectedEnNames[index]
            val wasCompletedYesterday = yesterdayIds.contains(taskId)

            var modifier = prefs.getFloat("task_modifier_$taskId", 1.0f)

            if (wasCompletedYesterday) modifier *= 0.95f else modifier *= 1.05f
            modifier = modifier.coerceIn(0.5f, 2.0f)
            editor.putFloat("task_modifier_$taskId", modifier)

            val finalReward = (baseRewards[index] * modifier).toInt().coerceAtLeast(5)
            _dailyTasks.add(Task(taskId, taskNameHe, taskNameEn, steps, finalReward))
        }
        editor.apply()
    }

    fun setLanguage(langCode: String) {
        language.value = langCode
        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(langCode)
        AppCompatDelegate.setApplicationLocales(appLocale)
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

    fun buyTimeForApp(packageName: String, minutes: Int, cost: Int, context: Context): Boolean {
        if (_diamonds.value >= cost) {
            _diamonds.value -= cost
            val currentTime = System.currentTimeMillis()
            val currentExpiry = unlockedAppsTime[packageName] ?: currentTime
            val newExpiry = (if (currentExpiry > currentTime) currentExpiry else currentTime) + (minutes.toLong() * 60 * 1000)

            unlockedAppsTime[packageName] = newExpiry

            val prefs = context.getSharedPreferences("GemGuardPrefs", Context.MODE_PRIVATE)
            prefs.edit()
                .putLong("unlock_$packageName", newExpiry)
                .putInt("diamonds", _diamonds.value)
                .apply()

            val appData = allInstalledApps.find { it.packageName == packageName }
            val intent = Intent(context, TimerService::class.java).apply {
                putExtra("package_name", packageName)
                putExtra("expiry_time", newExpiry)
                putExtra("app_name", appData?.name ?: "App")
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            return true
        }
        return false
    }

    private fun loadWhitelist(prefs: android.content.SharedPreferences) {
        val savedWhitelist = prefs.getString("whitelist", "") ?: ""
        _whitelistedApps.clear()
        if (savedWhitelist.isNotEmpty()) _whitelistedApps.addAll(savedWhitelist.split(","))
        if (!_whitelistedApps.contains("com.android.settings")) _whitelistedApps.add("com.android.settings")
    }

    private fun loadUnlockedApps(prefs: android.content.SharedPreferences) {
        val allPrefs = prefs.all
        unlockedAppsTime.clear()
        for ((key, value) in allPrefs) {
            if (key.startsWith("unlock_") && value is Long && value > System.currentTimeMillis()) {
                unlockedAppsTime[key.replace("unlock_", "")] = value
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

    fun loadInstalledApps(context: Context) {
        if (allInstalledApps.isNotEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val pm = context.packageManager
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
            }
            val startTime = calendar.timeInMillis
            val endTime = System.currentTimeMillis()

            val usageStats = usageStatsManager.queryAndAggregateUsageStats(startTime, endTime)
            val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val tempList = mutableListOf<AppInfoData>()

            for (app in apps) {
                if (pm.getLaunchIntentForPackage(app.packageName) != null) {
                    val label = app.loadLabel(pm).toString()
                    val totalTime = usageStats[app.packageName]?.totalTimeInForeground ?: 0L
                    if (!app.packageName.startsWith("com.android.systemui") && app.packageName != "android") {
                        tempList.add(AppInfoData(label, app.packageName, totalTime))
                    }
                }
            }
            tempList.sortByDescending { it.usageTime }
            withContext(Dispatchers.Main) {
                allInstalledApps.clear()
                allInstalledApps.addAll(tempList)
            }
        }
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