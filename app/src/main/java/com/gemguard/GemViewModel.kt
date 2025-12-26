package com.gemguard

import android.app.Application
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.*
import androidx.core.content.edit
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

// --- Data Classes ---

data class Task(
    val id: Int,
    val nameHe: String,
    val nameEn: String,
    val requiredSteps: Int,
    val reward: Int
) {
    fun isCompleted(claimedIds: List<Int>) = claimedIds.contains(id)
}

data class TimeMission(
    val isActive: Boolean,
    val stepsGoal: Int,
    val stepsProgress: Int,
    val endTime: Long,
    val reward: Int
)

// --- ViewModel ---

class GemViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs: SharedPreferences by lazy {
        getApplication<Application>().getSharedPreferences("GemGuardPrefs", Context.MODE_PRIVATE)
    }

    // --- State Variables ---
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

    private val _timeMission = mutableStateOf<TimeMission?>(null)
    val timeMission: State<TimeMission?> = _timeMission

    // --- המשתנה החדש לניהול רכישה ישירה מהחסימה ---
    var appToPurchasePackage = mutableStateOf<String?>(null)

    var appPin = mutableStateOf("")
    var isDarkMode = mutableStateOf(false)
    var language = mutableStateOf("iw")
    var isSetupCompleteState = mutableStateOf(false)

    private var cachedInitialSteps = -1
    private var cachedLastDate = ""

    data class AppInfoData(val name: String, val packageName: String, val usageTime: Long = 0)

    // --- Core Logic ---

    fun initData() {
        initData(getApplication<Application>().applicationContext)
    }

    fun initData(context: Context) {
        val todayDate = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val lastSavedDate = prefs.getString("last_task_date", "")

        val isFirstTimeEver = !prefs.contains("setup_complete")
        if (isFirstTimeEver) {
            prefs.edit().putInt("diamonds", 0).apply()
        }

        _diamonds.value = prefs.getInt("diamonds", 0)
        appPin.value = prefs.getString("app_pin", "") ?: ""
        isDarkMode.value = prefs.getBoolean("dark_mode", false)
        language.value = prefs.getString("language", "iw") ?: "iw"
        isSetupCompleteState.value = prefs.getBoolean("setup_complete", false)

        cachedInitialSteps = prefs.getInt("initial_steps", -1)
        cachedLastDate = prefs.getString("last_date", "") ?: ""

        if (todayDate != lastSavedDate) {
            val lastClaimed = prefs.getString("yesterday_claimed", "") ?: ""
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

        generateSmartTasks()
        loadUnlockedApps()
        loadWhitelist()
        loadInstalledApps()
        checkTimeMission(context)
    }

    private fun generateSmartTasks() {
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

        val safeIndex = (dayOfWeek - 1) % nameHePool.size
        val selectedHeNames = nameHePool[safeIndex]
        val selectedEnNames = nameEnPool[safeIndex]

        val baseSteps = listOf(1000, 2500, 5000, 7500, 10000)
        val baseRewards = listOf(20, 60, 150, 250, 500)

        val yesterdayClaimed = prefs.getString("yesterday_claimed", "") ?: ""
        val yesterdayIds = yesterdayClaimed.split(",").mapNotNull { it.toIntOrNull() }

        _dailyTasks.clear()
        val editor = prefs.edit()

        baseSteps.forEachIndexed { index, steps ->
            val taskId = index + 1
            val taskNameHe = selectedHeNames.getOrElse(index) { "משימה $taskId" }
            val taskNameEn = selectedEnNames.getOrElse(index) { "Task $taskId" }
            val wasCompletedYesterday = yesterdayIds.contains(taskId)

            var modifier = prefs.getFloat("task_modifier_$taskId", 1.0f)
            if (wasCompletedYesterday) modifier *= 0.95f else modifier *= 1.05f
            modifier = modifier.coerceIn(0.5f, 2.0f)
            editor.putFloat("task_modifier_$taskId", modifier)

            val calculatedReward = (baseRewards[index] * modifier).toInt()
            val roundedReward = (calculatedReward / 10) * 10
            val finalReward = roundedReward.coerceAtLeast(10)

            _dailyTasks.add(Task(taskId, taskNameHe, taskNameEn, steps, finalReward))
        }
        editor.apply()
    }

    // --- Time Mission Logic ---

    fun checkTimeMission(context: Context) {
        val isActive = prefs.getBoolean("time_mission_active", false)
        if (!isActive) {
            _timeMission.value = null
            return
        }

        val endTime = prefs.getLong("time_mission_end_time", 0L)

        if (System.currentTimeMillis() > endTime) {
            clearTimeMission(false, context, 0)
            return
        }

        if (_timeMission.value == null) {
            val stepsGoal = prefs.getInt("time_mission_steps_goal", 0)
            val startSteps = prefs.getInt("time_mission_start_steps", 0)
            val reward = prefs.getInt("time_mission_reward", 0)
            val lastKnownTotal = prefs.getInt("last_known_total_steps", startSteps)
            val progress = (lastKnownTotal - startSteps).coerceAtLeast(0)

            _timeMission.value = TimeMission(true, stepsGoal, progress, endTime, reward)
        }
    }

    fun claimTimeMissionReward(context: Context) {
        val mission = _timeMission.value
        if (mission != null && mission.isActive && mission.stepsProgress >= mission.stepsGoal) {
            clearTimeMission(true, context, mission.reward)
        }
    }

    fun clearTimeMission(isSuccess: Boolean, context: Context, reward: Int) {
        if (isSuccess) {
            _diamonds.value += reward
            prefs.edit { putInt("diamonds", _diamonds.value) }
        }

        prefs.edit {
            remove("time_mission_active")
            remove("time_mission_steps_goal")
            remove("time_mission_end_time")
            remove("time_mission_reward")
            remove("time_mission_start_steps")
        }

        _timeMission.value = null
    }

    fun triggerTimeMissionForTesting(context: Context) {
        prefs.edit { remove("time_mission_active") }
        val receiver = TimeMissionReceiver()
        receiver.onReceive(context, Intent())
    }

    // --- App Logic ---

    fun setLanguage(langCode: String) {
        language.value = langCode
        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(langCode)
        AppCompatDelegate.setApplicationLocales(appLocale)
        prefs.edit().putString("language", langCode).apply()
    }

    fun addDiamonds(amount: Int, taskId: Int) {
        if (!_claimedTaskIds.contains(taskId)) {
            _diamonds.value += amount
            _claimedTaskIds.add(taskId)
            prefs.edit().apply {
                putInt("diamonds", _diamonds.value)
                putString("claimed_tasks", _claimedTaskIds.joinToString(","))
                apply()
            }
        }
    }

    fun updateStepsOptimized(totalStepsFromSensor: Int) {
        val todayDate = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())

        prefs.edit { putInt("last_known_total_steps", totalStepsFromSensor) }

        if (cachedInitialSteps == -1 || todayDate != cachedLastDate) {
            cachedInitialSteps = totalStepsFromSensor
            cachedLastDate = todayDate
            prefs.edit {
                putString("last_date", todayDate)
                putInt("initial_steps", cachedInitialSteps)
            }
        } else if (totalStepsFromSensor < cachedInitialSteps) {
            cachedInitialSteps = totalStepsFromSensor
            prefs.edit { putInt("initial_steps", cachedInitialSteps) }
        }

        _currentSteps.value = (totalStepsFromSensor - cachedInitialSteps).coerceAtLeast(0)

        if (prefs.getBoolean("time_mission_active", false)) {
            val startTimeSteps = prefs.getInt("time_mission_start_steps", totalStepsFromSensor)
            val endTime = prefs.getLong("time_mission_end_time", 0L)

            if (System.currentTimeMillis() <= endTime) {
                val progress = (totalStepsFromSensor - startTimeSteps).coerceAtLeast(0)
                _timeMission.value = _timeMission.value?.copy(stepsProgress = progress)
            }
        }

        checkTimeMission(getApplication<Application>().applicationContext)
    }

    // --- Settings & Utils ---

    fun saveSettings(context: Context) {
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

    fun toggleDarkMode() {
        isDarkMode.value = !isDarkMode.value
        prefs.edit().putBoolean("dark_mode", isDarkMode.value).apply()
    }

    fun toggleWhitelist(packageName: String) {
        if (packageName == "com.android.settings") return
        if (_whitelistedApps.contains(packageName)) {
            _whitelistedApps.remove(packageName)
        } else {
            _whitelistedApps.add(packageName)
        }
        prefs.edit().putString("whitelist", _whitelistedApps.joinToString(",")).apply()
    }

    fun buyTimeForApp(packageName: String, minutes: Int, cost: Int, context: Context): Boolean {
        if (_diamonds.value >= cost) {
            _diamonds.value -= cost
            val currentTime = System.currentTimeMillis()
            val currentExpiry = unlockedAppsTime[packageName] ?: currentTime
            val newExpiry = (if (currentExpiry > currentTime) currentExpiry else currentTime) + (minutes.toLong() * 60 * 1000)

            unlockedAppsTime[packageName] = newExpiry
            prefs.edit().putLong("unlock_$packageName", newExpiry).putInt("diamonds", _diamonds.value).apply()

            val appData = allInstalledApps.find { it.packageName == packageName }
            val intent = Intent(context, TimerService::class.java).apply {
                putExtra("package_name", packageName)
                putExtra("expiry_time", newExpiry)
                putExtra("app_name", appData?.name ?: "App")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
            else context.startService(intent)
            return true
        }
        return false
    }

    fun loadInstalledApps() {
        if (allInstalledApps.isNotEmpty()) return
        val context = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            val pm = context.packageManager
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
            }
            val usageStats = usageStatsManager.queryAndAggregateUsageStats(calendar.timeInMillis, System.currentTimeMillis())
            val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val tempList = apps.filter { pm.getLaunchIntentForPackage(it.packageName) != null }
                .map { AppInfoData(it.loadLabel(pm).toString(), it.packageName, usageStats[it.packageName]?.totalTimeInForeground ?: 0L) }
                .filter { !it.packageName.startsWith("com.android.systemui") && it.packageName != "android" }
                .sortedByDescending { it.usageTime }

            withContext(Dispatchers.Main) {
                allInstalledApps.clear()
                allInstalledApps.addAll(tempList)
            }
        }
    }

    private fun loadWhitelist() {
        val savedWhitelist = prefs.getString("whitelist", "") ?: ""
        val myPackageName = getApplication<Application>().packageName
        _whitelistedApps.clear()
        if (savedWhitelist.isNotEmpty()) {
            _whitelistedApps.addAll(savedWhitelist.split(","))
        }
        if (!_whitelistedApps.contains("com.android.settings")) _whitelistedApps.add("com.android.settings")
        if (!_whitelistedApps.contains(myPackageName)) {
            _whitelistedApps.add(myPackageName)
            prefs.edit { putString("whitelist", _whitelistedApps.joinToString(",")) }
        }
    }

    private fun loadUnlockedApps() {
        val allPrefs = prefs.all
        unlockedAppsTime.clear()
        for ((key, value) in allPrefs) {
            if (key.startsWith("unlock_") && value is Long && value > System.currentTimeMillis()) {
                unlockedAppsTime[key.replace("unlock_", "")] = value
            }
        }
    }
}