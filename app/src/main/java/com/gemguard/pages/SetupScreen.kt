package com.gemguard.pages

import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.gemguard.GemViewModel
import com.gemguard.MainActivity
import com.gemguard.RecentsBlockerService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(viewModel: GemViewModel, onComplete: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isHebrew = viewModel.language.value == "iw"
    val emeraldColor = Color(0xFF2ECC71) // הצבע הירוק של האפליקציה
    var pin by remember { mutableStateOf("") }
    val currentStep by viewModel.setupStep

    // --- פונקציות עזר לבדיקת הרשאות ---

    fun hasStepPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun hasUsagePermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun hasOverlayPermission(): Boolean = Settings.canDrawOverlays(context)

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    fun isAccessibilityEnabled(): Boolean {
        val expectedComponentName = ComponentName(context, RecentsBlockerService::class.java)
        val enabledServicesSetting = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServicesSetting)

        while (colonSplitter.hasNext()) {
            val componentNameString = colonSplitter.next()
            val enabledComponent = ComponentName.unflattenFromString(componentNameString)
            if (enabledComponent != null && enabledComponent == expectedComponentName) {
                return true
            }
        }
        return false
    }

    // --- ניהול מעבר אוטומטי בין שלבים ---
    DisposableEffect(lifecycleOwner, currentStep) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                when (currentStep) {
                    2 -> if (hasStepPermission()) viewModel.setupStep.intValue = 3
                    3 -> if (hasNotificationPermission()) viewModel.setupStep.intValue = 4
                    4 -> if (hasUsagePermission()) viewModel.setupStep.intValue = 5
                    5 -> if (hasOverlayPermission()) viewModel.setupStep.intValue = 6
                    6 -> if (isAccessibilityEnabled()) {
                        viewModel.saveSettings(context)
                        onComplete()
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(30.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // מחוון צעדים
        Row(
            modifier = Modifier.padding(top = 20.dp, bottom = 40.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(7) { index ->
                Box(
                    modifier = Modifier
                        .size(if (currentStep == index) 12.dp else 8.dp)
                        .clip(CircleShape)
                        .background(if (currentStep == index) emeraldColor else Color.LightGray)
                )
            }
        }

        AnimatedContent(targetState = currentStep, label = "SetupTransition") { step ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                when (step) {
                    0 -> { // בחירת שפה
                        Icon(Icons.Default.Language, null, modifier = Modifier.size(80.dp), tint = emeraldColor)
                        Text(if(isHebrew) "בחר שפה" else "Choose Language", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(30.dp))

                        listOf("iw" to "עברית", "en" to "English").forEach { (code, label) ->
                            val selected = viewModel.language.value == code
                            OutlinedButton(
                                onClick = { viewModel.setLanguage(code) },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                                border = BorderStroke(2.dp, if(selected) emeraldColor else Color.LightGray),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if(selected) emeraldColor.copy(alpha = 0.1f) else Color.Transparent
                                )
                            ) {
                                Text(label, color = if(selected) emeraldColor else Color.Gray, fontSize = 18.sp)
                                if(selected) {
                                    Spacer(Modifier.width(10.dp))
                                    Icon(Icons.Default.Check, null, tint = emeraldColor)
                                }
                            }
                        }

                        Button(
                            onClick = { viewModel.setupStep.intValue = 1 },
                            modifier = Modifier.padding(top = 30.dp).fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = emeraldColor)
                        ) { Text(if(isHebrew) "המשך" else "Continue") }
                    }

                    1 -> { // הגדרת PIN
                        Icon(Icons.Default.Lock, null, modifier = Modifier.size(80.dp), tint = emeraldColor)
                        Text(if(isHebrew) "אבטחת האפליקציה" else "Secure App", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(20.dp))
                        OutlinedTextField(
                            value = pin,
                            onValueChange = { if (it.length <= 4) pin = it },
                            label = { Text(if(isHebrew) "קוד גישה (4 ספרות)" else "PIN Code") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        Button(
                            onClick = { if (pin.length == 4) { viewModel.appPin.value = pin; viewModel.setupStep.intValue = 2 } },
                            modifier = Modifier.padding(top = 20.dp).fillMaxWidth(),
                            enabled = pin.length == 4,
                            colors = ButtonDefaults.buttonColors(containerColor = emeraldColor)
                        ) { Text(if(isHebrew) "המשך" else "Next") }
                    }

                    2 -> { // צעדים
                        Icon(Icons.Default.DirectionsRun, null, modifier = Modifier.size(100.dp), tint = emeraldColor)
                        Text(if(isHebrew) "מעקב צעדים" else "Step Tracking", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text(if(isHebrew) "כדי שנוכל להמיר צעדים ליהלומים." else "To convert steps to gems.", textAlign = TextAlign.Center)
                        Button(
                            onClick = { (context as? MainActivity)?.requestStepPermission {} },
                            modifier = Modifier.padding(top = 20.dp).fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = emeraldColor)
                        ) { Text(if(isHebrew) "אשר הרשאה" else "Grant Permission") }
                    }

                    3 -> { // התראות
                        Icon(Icons.Default.NotificationsActive, null, modifier = Modifier.size(100.dp), tint = emeraldColor)
                        Text(if(isHebrew) "התראות טיימר" else "Timer Notifications", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text(if(isHebrew) "אשר התראות כדי לראות את הטיימר." else "Allow notifications to see the timer.", textAlign = TextAlign.Center)
                        Button(
                            onClick = { (context as? MainActivity)?.requestNotificationPermission() },
                            modifier = Modifier.padding(top = 20.dp).fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = emeraldColor)
                        ) { Text(if(isHebrew) "אשר התראות" else "Allow Notifications") }
                    }

                    4 -> { // גישה לנתונים
                        Icon(Icons.Default.QueryStats, null, modifier = Modifier.size(100.dp), tint = emeraldColor)
                        Text(if(isHebrew) "גישה לנתוני שימוש" else "Usage Access", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text(if(isHebrew) "חובה כדי לדעת מתי לחסום אפליקציות." else "Required to know when to block apps.", textAlign = TextAlign.Center)
                        Button(
                            onClick = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
                            modifier = Modifier.padding(top = 20.dp).fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = emeraldColor)
                        ) { Text(if(isHebrew) "פתח הגדרות" else "Open Settings") }
                    }

                    5 -> { // תצוגה מעל אפליקציות
                        // תוקן: שונה מירוק (emeraldColor) במקום אדום (Color.Red)
                        Icon(Icons.Default.Layers, null, modifier = Modifier.size(100.dp), tint = emeraldColor)
                        Text(if(isHebrew) "תצוגה מעל אפליקציות" else "Display Over Apps", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text(if(isHebrew) "אשר תצוגה מעל כדי להציג את מסך החסימה." else "Allow overlay to show the block screen.", textAlign = TextAlign.Center)
                        Button(
                            onClick = {
                                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.padding(top = 20.dp).fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = emeraldColor)
                        ) { Text(if(isHebrew) "אשר תצוגה מעל" else "Allow Overlay") }
                    }

                    6 -> { // שירות נגישות
                        Icon(Icons.Default.AccessibilityNew, null, modifier = Modifier.size(100.dp), tint = emeraldColor)
                        Text(if(isHebrew) "חסימה מתקדמת" else "Advanced Blocking", fontSize = 24.sp, fontWeight = FontWeight.Bold)

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            if(isHebrew) "כדי למנוע עקיפה, יש להפעיל את GemGuard בתפריט הנגישות."
                            else "To prevent bypassing, enable GemGuard in Accessibility settings.",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        )

                        Spacer(modifier = Modifier.height(15.dp))

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if(isHebrew) "הוראות הפעלה:" else "Instructions:",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = if(isHebrew)
                                        "1. לחץ על הכפתור למטה\n\n2. חפש 'יישומים מותקנים'\n(Installed Apps)\n\n3. בחר ב-GemGuard והפעל"
                                    else
                                        "1. Tap the button below\n\n2. Find 'Installed Apps'\n\n3. Select GemGuard and turn ON",
                                    fontSize = 14.sp,
                                    lineHeight = 22.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Button(
                            onClick = {
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                context.startActivity(intent)
                            },
                            modifier = Modifier.padding(top = 20.dp).fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = emeraldColor)
                        ) { Text(if(isHebrew) "פתח הגדרות נגישות" else "Open Accessibility") }
                    }
                }
            }
        }
    }
}