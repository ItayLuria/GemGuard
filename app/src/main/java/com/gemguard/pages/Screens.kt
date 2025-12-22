package com.gemguard

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

sealed class Screen(
    val route: String,
    val title: String,    // כותרת בעברית
    val titleEn: String,  // כותרת באנגלית (פותר את השגיאה)
    val icon: ImageVector
) {
    object Home : Screen("home", "בית", "Home", Icons.Default.Home)
    object Tasks : Screen("tasks", "משימות", "Tasks", Icons.Default.List)
    object Store : Screen("store", "חנות", "Store", Icons.Default.ShoppingCart)
    object Stats : Screen("stats", "סטטיסטיקה", "Stats", Icons.Default.BarChart) // החלפתי ל-BarChart שמתאים יותר
    object Settings : Screen("settings", "הגדרות", "Settings", Icons.Default.Settings)
}