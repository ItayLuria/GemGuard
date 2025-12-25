package com.gemguard

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class RecentsBlockerService : AccessibilityService() {

    private var isProtectionEnabled = true
    private lateinit var prefs: SharedPreferences

    // מאזין לשינויים ב-SharedPreferences - מתעדכן ברגע שלוחצים על ה-Switch ב-Settings
    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPrefs, key ->
        if (key == "service_enabled") {
            isProtectionEnabled = sharedPrefs.getBoolean("service_enabled", true)
            Log.d("GemGuard", "Service: Protection status changed to $isProtectionEnabled")
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = getSharedPreferences("GemGuardPrefs", Context.MODE_PRIVATE)

        // קריאה ראשונית של המצב
        isProtectionEnabled = prefs.getBoolean("service_enabled", true)

        // רישום המאזין
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)
        Log.d("GemGuard", "Service connected. Protection enabled: $isProtectionEnabled")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // אם המשתמש כיבה את ההגנה ב-Settings, פשוט לא עושים כלום
        if (!isProtectionEnabled) return

        val packageName = event.packageName?.toString() ?: ""

        // בדיקה אם מדובר במערכת או בלאנצ'ר (שם בד"כ נמצא ה-Recents)
        if (packageName.contains("com.android.systemui") ||
            packageName.contains("launcher", ignoreCase = true)) {

            val className = event.className?.toString() ?: ""

            // זיהוי מסך היישומים האחרונים
            if (className.contains("Recents") ||
                className.contains("Overview") ||
                className.contains("TaskView")) {

                Log.e("GemGuard", "!!! BLOCKING RECENTS !!!")
                // זריקה למסך הבית
                performGlobalAction(GLOBAL_ACTION_HOME)
            }
        }
    }

    override fun onInterrupt() {
        // פונקציה חובה לממש - נשארת ריקה
    }

    override fun onDestroy() {
        super.onDestroy()
        // ניקוי המאזין למניעת דליפות זיכרון
        if (::prefs.isInitialized) {
            prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
        }
    }
}