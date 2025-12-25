package com.gemguard

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.util.Log

class RecentsBlockerService : AccessibilityService() {

    private val SYSTEM_PACKAGES = setOf(
        "com.android.systemui",           // אנדרואיד נקי / סמסונג ועוד
        "com.google.android.apps.nexuslauncher", // פיקסל
        "com.sec.android.app.launcher",   // סמסונג (מסך הבית)
        "com.miui.home",                  // שיאומי
        "com.huawei.android.launcher",    // וואווי
        "com.oneplus.launcher",           // וואן פלוס
        "com.oppo.launcher",              // אופו
        "com.teslacoilsw.launcher"        // Nova Launcher (למשתמשים מתקדמים)
    )

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: ""

        // לוג בסיסי - אם אתה לא רואה את זה ב-Logcat, השירות פשוט לא רץ
        Log.d("GemGuard", "Package detected: $packageName")

        // התנאי הכי רחב שעובד ב-99% מהמקרים:
        // אם שם החבילה הוא SystemUI או ה-Launcher של המכשיר, וזה שינוי חלון
        if (packageName.contains("com.android.systemui") ||
            packageName.contains("launcher", ignoreCase = true)) {

            // כאן אנחנו בודקים אם מדובר ב-Recents
            val className = event.className?.toString() ?: ""

            if (className.contains("Recents") ||
                className.contains("Overview") ||
                className.contains("TaskView")) {

                Log.e("GemGuard", "!!! BLOCKING RECENTS !!!")
                performGlobalAction(GLOBAL_ACTION_HOME)
            }
        }
    }
    private fun isRecentsScreen(packageName: String, className: String): Boolean {
        // 1. בדיקה גסה: אם זו אחת מאפליקציות המערכת
        if (SYSTEM_PACKAGES.contains(packageName)) {
            // 2. סינון עדין יותר: האם זה באמת מסך ה-Recents?
            // רוב המכשירים משתמשים במילים האלו ב-Class Name של ה-Recents
            return className.contains("Recents", ignoreCase = true) ||
                    className.contains("Overview", ignoreCase = true) ||
                    className.contains("TaskStack", ignoreCase = true) ||
                    // בסמסונג לפעמים זה פשוט ה-Launcher הראשי כשלוחצים על הכפתור
                    (packageName == "com.sec.android.app.launcher" && className.contains("QuickStep"))
        }
        return false
    }

    override fun onInterrupt() {
        // לא בשימוש כרגע
    }
}