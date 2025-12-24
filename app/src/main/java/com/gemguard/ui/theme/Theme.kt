package com.gemguard.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// הגדרת צבעי האמרלד החדשים
val EmeraldGreen = Color(0xFF2ECC71)
val DarkEmerald = Color(0xFF27AE60)
val LightEmerald = Color(0xFFE8F5E9)

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldGreen,
    onPrimary = Color.White,   // זה יבטיח שהטקסט על הכפתורים הירוקים יהיה לבן ולא סגול/שחור
    secondary = DarkEmerald,
    onSecondary = Color.White,
    tertiary = Color(0xFF81C784),
    background = Color(0xFF181818),
    surface = Color(0xFF1E1E1E),
    onSurface = Color.White    // טקסט כללי על משטחים כהים
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldGreen,
    secondary = DarkEmerald,
    tertiary = Color(0xFFC8E6C9),
    background = Color(0xFFFFFBFE),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF413E46)
)

@Composable
fun GemGuardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // ביטלתי את dynamicColor כברירת מחדל כדי שהצבע הירוק שבחרנו ישלוט ולא צבע הטפט של המכשיר
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}