package com.gemguard.pages

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Tasks : Screen("tasks")
    object Stats : Screen("stats")
}