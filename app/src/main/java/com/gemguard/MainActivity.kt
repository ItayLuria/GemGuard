package com.gemguard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gemguard.pages.*
import com.gemguard.ui.theme.GemGuardTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GemGuardTheme {
                // יצירת הבקר שאחראי על הניווט
                val navController = rememberNavController()

                // הגדרת "מפת הדרכים" של האפליקציה
                NavHost(
                    navController = navController,
                    startDestination = Screen.Home.route // המסך שמתחילים ממנו
                ) {
                    composable(Screen.Home.route) {
                        Home(navController) // נשלח את הבקר לדף הבית
                    }

                }
            }
        }
    }
}