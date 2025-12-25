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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun SetupScreen(viewModel: GemViewModel, onComplete: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isHebrew = viewModel.language.value == "iw"
    val emeraldColor = Color(0xFF2ECC71)
    var pin by remember { mutableStateOf("") }
    val currentStep by viewModel.setupStep

    // --- פונקציות עזר לבדיקת הרשאות ---
    fun hasStepPermission(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
    } else true

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

    fun hasNotificationPermission(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    } else true

    fun isAccessibilityEnabled(): Boolean {
        val expectedComponentName = ComponentName(context, RecentsBlockerService::class.java)
        val enabledServicesSetting = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        return enabledServicesSetting.contains(expectedComponentName.flattenToString())
    }

    // מעבר אוטומטי בעת חזרה לאפליקציה (OnResume)
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

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        // מחוון צעדים (Step Indicator)
        Row(modifier = Modifier.padding(top = 10.dp, bottom = 30.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(7) { index ->
                Box(modifier = Modifier
                    .size(if (currentStep == index) 12.dp else 8.dp)
                    .clip(CircleShape)
                    .background(if (currentStep == index) emeraldColor else Color.LightGray)
                )
            }
        }

        AnimatedContent(
            targetState = currentStep,
            label = "SetupTransition",
            transitionSpec = { fadeIn() togetherWith fadeOut() }
        ) { step ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
                when (step) {
                    0 -> { // בחירת שפה
                        Icon(Icons.Default.Language, null, modifier = Modifier.size(70.dp), tint = emeraldColor)
                        Text(if(isHebrew) "ברוכים הבאים" else "Welcome", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Text(if(isHebrew) "בחר את שפת הממשק" else "Choose interface language", color = Color.Gray)
                        Spacer(modifier = Modifier.height(24.dp))
                        listOf("iw" to "עברית", "en" to "English").forEach { (code, label) ->
                            val selected = viewModel.language.value == code
                            OutlinedButton(
                                onClick = { viewModel.setLanguage(code) },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                border = BorderStroke(2.dp, if(selected) emeraldColor else Color.LightGray),
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = if(selected) emeraldColor.copy(alpha = 0.05f) else Color.Transparent)
                            ) { Text(label, color = if(selected) emeraldColor else Color.Gray) }
                        }
                        InfoCard(if(isHebrew) "השפה שתבחר תחול על כל התפריטים וההתראות באפליקציה." else "The language will affect all menus and notifications.")
                        Spacer(modifier = Modifier.weight(1f))
                        Button(onClick = { viewModel.setupStep.intValue = 1 }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = emeraldColor)) {
                            Text(if(isHebrew) "המשך" else "Continue")
                        }
                    }

                    1 -> { // הגדרת PIN
                        Icon(Icons.Default.Lock, null, modifier = Modifier.size(70.dp), tint = emeraldColor)
                        Text(if(isHebrew) "הגדר קוד נעילה" else "Set locking PIN", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = pin,
                            onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pin = it },
                            label = { Text(if(isHebrew) "קוד PIN (4 ספרות)" else "4-Digit PIN") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        InfoCard(if(isHebrew) "הקוד נדרש כדי לשנות הגדרות או לבטל את החסימה. ללא קוד, לא ניתן יהיה לעקוף את חסימת האפליקציות." else "The PIN is required to change settings or disable app locking. Without it, the lock cannot be bypassed.")
                        Spacer(modifier = Modifier.weight(1f))
                        Button(
                            onClick = { viewModel.appPin.value = pin; viewModel.setupStep.intValue = 2 },
                            enabled = pin.length == 4,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = emeraldColor)
                        ) { Text(if(isHebrew) "המשך" else "Next") }
                    }

                    2 -> PermissionStep(
                        icon = Icons.Default.DirectionsWalk,
                        title = if(isHebrew) "סנכרון פעילות" else "Activity Sync",
                        desc = if(isHebrew) "גישה למד הצעדים של המכשיר" else "Access to device step counter",
                        info = if(isHebrew) "הצעדים שלך הם הלב של האפליקציה. אנחנו צריכים את ההרשאה הזו כדי להמיר את ההליכה שלך לזמן מסך באמצעות Gems." else "Your steps are the heart of our app. We need this permission to convert your physical movement into screen time through gems.",
                        btnText = if(isHebrew) "אשר גישה" else "Grant Access",
                        onClick = { (context as? MainActivity)?.requestStepPermission {} }
                    )

                    3 -> PermissionStep(
                        icon = Icons.Default.NotificationsActive,
                        title = if(isHebrew) "התראות" else "Notifications",
                        desc = if(isHebrew) "הצגת זמן זמני האפליקציות ועדכונים" else "Show remaining app time and updates",
                        info = if(isHebrew) "אנחנו שולחים התראות כשזמן האפליקציה עומד להיגמר וכדי להציג את הטיימר בזמן אמת." else "We notify you when the app's time is running out.",
                        btnText = if(isHebrew) "אשר התראות" else "Allow Notifications",
                        onClick = { (context as? MainActivity)?.requestNotificationPermission() }
                    )

                    4 -> PermissionStep(
                        icon = Icons.Default.QueryStats,
                        title = if(isHebrew) "זיהוי אפליקציות" else "App Detection",
                        desc = if(isHebrew) "צפייה באפליקציות הפועלות בטלפון" else "Monitor active application",
                        info = if(isHebrew) "כדי לדעת אם פתחת אפליקציה חסומה, המערכת צריכה לדעת איזו אפליקציה נמצאת בשימוש כרגע." else "To know if you've opened a restricted app, the system needs to see which app is currently in the background.",
                        btnText = if(isHebrew) "פתח הגדרות" else "Open Settings",
                        onClick = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
                    )

                    5 -> PermissionStep(
                        icon = Icons.Default.Layers,
                        title = if(isHebrew) "מסך חסימה" else "Blocking Screen",
                        desc = if(isHebrew) "תצוגה מעל אפליקציות" else "Display over other apps",
                        info = if(isHebrew) "זו ההרשאה שמאפשרת לנו להציג את חסימת האפליקציות ולמנוע את השימוש בהן." else "This allows us to block restricted apps, physically preventing their use.",
                        btnText = if(isHebrew) "אישור ההרשאה" else "Allow Permission",
                        onClick = { context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))) }
                    )

                    6 -> PermissionStep(
                        icon = Icons.Default.Security,
                        title = if(isHebrew) "הגנה הרמטית" else "Ironclad Protection",
                        desc = if(isHebrew) "מניעת עקיפת החסימה" else "Prevent bypass techniques",
                        info = if(isHebrew) "שירות הנגישות מונע ממך להשתמש בכפתור ה'יישומים אחרונים' כדי לעקוף את החסימה." else "The Accessibility service prevents using the 'Recent Apps' button to bypass the lock.",
                        btnText = if(isHebrew) "הפעל הגנה" else "Enable Protection",
                        onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                    )
                }
            }
        }
    }
}

@Composable
fun ColumnScope.PermissionStep(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String,
    info: String,
    btnText: String,
    onClick: () -> Unit
) {
    Icon(icon, null, modifier = Modifier.size(80.dp), tint = Color(0xFF2ECC71))
    Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    Text(desc, color = Color.Gray, textAlign = TextAlign.Center)
    Spacer(modifier = Modifier.height(20.dp))
    InfoCard(info)
    Spacer(modifier = Modifier.weight(1f))
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2ECC71))
    ) {
        Text(btnText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun InfoCard(text: String) {
    val isHebrew = text.any { it in '\u0590'..'\u05FF' }
    val emeraldColor = Color(0xFF2ECC71)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        shape = RoundedCornerShape(12.dp),
        // שימוש ב-surfaceVariant עם שקיפות יוצר גוון ניטרלי (אפרפר/כהה) ללא סגול
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        // מסגרת עדינה מאוד שמתאימה את עצמה לצבע הטקסט
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = emeraldColor, // שימוש בירוק של האפליקציה במקום צבע מערכת
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = if (isHebrew) TextAlign.Right else TextAlign.Left,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}