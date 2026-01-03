package com.gemguard.pages

import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
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

    var pinFirstEntry by remember { mutableStateOf("") }
    var pinConfirmEntry by remember { mutableStateOf("") }
    var showConfirmDialog by remember { mutableStateOf(false) }

    val currentStep by viewModel.setupStep

    fun hasStepPermission(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
    } else true

    fun hasUsagePermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            @Suppress("DEPRECATION")
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

    DisposableEffect(lifecycleOwner, currentStep) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                when (currentStep) {
                    3 -> if (hasStepPermission()) viewModel.setupStep.intValue = 4
                    4 -> if (hasNotificationPermission()) viewModel.setupStep.intValue = 5
                    5 -> if (hasUsagePermission()) viewModel.setupStep.intValue = 6
                    6 -> if (hasOverlayPermission()) viewModel.setupStep.intValue = 7
                    7 -> if (isAccessibilityEnabled()) {
                        viewModel.saveSettings(context)
                        onComplete()
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(modifier = Modifier.fillMaxSize().imePadding()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.padding(top = 10.dp, bottom = 30.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(8) { index ->
                    Box(
                        modifier = Modifier
                            .size(if (currentStep == index) 12.dp else 8.dp)
                            .clip(CircleShape)
                            .background(if (currentStep == index) emeraldColor else Color.LightGray)
                    )
                }
            }

            AnimatedContent(
                targetState = currentStep,
                label = "SetupTransition",
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                modifier = Modifier.weight(1f)
            ) { step ->
                val stepScrollState = rememberScrollState()
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize().verticalScroll(stepScrollState)
                ) {
                    when (step) {
                        0 -> {
                            Icon(Icons.Default.Language, null, modifier = Modifier.size(70.dp), tint = emeraldColor)
                            Text(if(isHebrew) "ברוכים הבאים" else "Welcome", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                            Text(if(isHebrew) "בחר את שפת הממשק" else "Choose language", color = Color.Gray)
                            Spacer(modifier = Modifier.height(24.dp))
                            listOf("iw" to "עברית", "en" to "English").forEach { (code, label) ->
                                val selected = viewModel.language.value == code
                                OutlinedButton(
                                    onClick = { viewModel.setLanguage(code) },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(12.dp), // נוסף לעיצוב מרובע יותר
                                    border = BorderStroke(2.dp, if(selected) emeraldColor else Color.LightGray),
                                    colors = ButtonDefaults.outlinedButtonColors(containerColor = if(selected) emeraldColor.copy(alpha = 0.05f) else Color.Transparent)
                                ) { Text(label, color = if(selected) emeraldColor else Color.Gray) }
                            }
                            InfoCard(if(isHebrew) "השפה שתבחר תחול על כל התפריטים וההתראות." else "Language affects all menus and notifications.")
                            Spacer(modifier = Modifier.weight(1f))
                            Button(
                                onClick = { viewModel.setupStep.intValue = 1 }, 
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = emeraldColor)
                            ) {
                                Text(if(isHebrew) "המשך" else "Continue", fontWeight = FontWeight.Bold)
                            }
                        }

                        1 -> {
                            Icon(if(viewModel.isDarkMode.value) Icons.Default.DarkMode else Icons.Default.LightMode, null, modifier = Modifier.size(70.dp), tint = emeraldColor)
                            Text(if(isHebrew) "מראה האפליקציה" else "App Appearance", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Text(if(isHebrew) "בחר סגנון מועדף" else "Choose your style", color = Color.Gray)
                            Spacer(modifier = Modifier.height(32.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                AppearanceOption(
                                    label = if(isHebrew) "בהיר" else "Light",
                                    icon = Icons.Default.LightMode,
                                    isSelected = !viewModel.isDarkMode.value,
                                    modifier = Modifier.weight(1f),
                                    emeraldColor = emeraldColor,
                                    onClick = { viewModel.isDarkMode.value = false }
                                )
                                AppearanceOption(
                                    label = if(isHebrew) "כהה" else "Dark",
                                    icon = Icons.Default.DarkMode,
                                    isSelected = viewModel.isDarkMode.value,
                                    modifier = Modifier.weight(1f),
                                    emeraldColor = emeraldColor,
                                    onClick = { viewModel.isDarkMode.value = true }
                                )
                            }

                            InfoCard(if(isHebrew) "ניתן לשנות זאת בכל עת בהגדרות." else "Can be changed anytime in settings.")
                            Spacer(modifier = Modifier.weight(1f))
                            Button(
                                onClick = { viewModel.setupStep.intValue = 2 }, 
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = emeraldColor)
                            ) {
                                Text(if(isHebrew) "המשך" else "Continue", fontWeight = FontWeight.Bold)
                            }
                        }

                        2 -> {
                            Icon(Icons.Default.Lock, null, modifier = Modifier.size(70.dp), tint = emeraldColor)
                            Text(if(isHebrew) "הגדר קוד נעילה" else "Set locking PIN", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = pinFirstEntry,
                                onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pinFirstEntry = it },
                                placeholder = { Text(if(isHebrew) "הכנס PIN" else "Enter PIN") },
                                label = { Text(if(isHebrew) "קוד PIN (4 ספרות)" else "4-Digit PIN") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                visualTransformation = PasswordVisualTransformation(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            InfoCard(if(isHebrew) "הקוד נדרש כדי לבטל את החסימה." else "The PIN is required to bypass the lock.")
                            Spacer(modifier = Modifier.weight(1f))
                            Button(
                                onClick = { showConfirmDialog = true },
                                enabled = pinFirstEntry.length == 4,
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = emeraldColor),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text(if(isHebrew) "המשך" else "Next", fontWeight = FontWeight.Bold) }

                            if (showConfirmDialog) {
                                AlertDialog(
                                    onDismissRequest = { showConfirmDialog = false; pinConfirmEntry = "" },
                                    shape = RoundedCornerShape(24.dp),
                                    title = {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                            Icon(Icons.Default.VerifiedUser, null, tint = emeraldColor, modifier = Modifier.size(40.dp))
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(if(isHebrew) "אישור קוד PIN" else "Confirm PIN", fontWeight = FontWeight.Bold)
                                        }
                                    },
                                    text = {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                if(isHebrew) "הקש שוב את הקוד לאישור:" else "Re-enter your PIN to confirm:",
                                                textAlign = TextAlign.Center, color = Color.Gray
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                            OutlinedTextField(
                                                value = pinConfirmEntry,
                                                onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pinConfirmEntry = it },
                                                modifier = Modifier.fillMaxWidth(),
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                visualTransformation = PasswordVisualTransformation(),
                                                placeholder = { Text(if(isHebrew) "הכנס PIN" else "Re-enter PIN") },
                                                shape = RoundedCornerShape(12.dp),
                                                isError = pinConfirmEntry.length == 4 && pinConfirmEntry != pinFirstEntry
                                            )
                                            if (pinConfirmEntry.length == 4 && pinConfirmEntry != pinFirstEntry) {
                                                Text(if(isHebrew) "הקודים אינם תואמים" else "PINs don't match", color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                                            }
                                        }
                                    },
                                    confirmButton = {
                                        Button(
                                            onClick = {
                                                if (pinConfirmEntry == pinFirstEntry) {
                                                    viewModel.appPin.value = pinConfirmEntry
                                                    showConfirmDialog = false
                                                    viewModel.setupStep.intValue = 3
                                                }
                                            },
                                            enabled = pinConfirmEntry == pinFirstEntry,
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = emeraldColor)
                                        ) {
                                            Text(if(isHebrew) "אישור" else "Confirm")
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showConfirmDialog = false; pinConfirmEntry = "" }) {
                                            Text(if(isHebrew) "ביטול" else "Cancel", color = Color.Gray)
                                        }
                                    }
                                )
                            }
                        }

                        3 -> PermissionStep(
                            icon = Icons.Default.DirectionsWalk,
                            title = if(isHebrew) "סנכרון פעילות" else "Activity Sync",
                            desc = if(isHebrew) "גישה למד הצעדים" else "Access to steps",
                            info = if(isHebrew) "הצעדים שלך הם הבסיס לאפליקציה. אנחנו צריכים גישה כדי להמיר הליכה לזמן מסך." else "We need this to convert movement into screen time.",
                            btnText = if(isHebrew) "אשר גישה" else "Grant Access",
                            onClick = { (context as? MainActivity)?.requestStepPermission {} }
                        )

                        4 -> PermissionStep(
                            icon = Icons.Default.NotificationsActive,
                            title = if(isHebrew) "התראות" else "Notifications",
                            desc = if(isHebrew) "הצגת זמני האפליקציות" else "Show remaining time",
                            info = if(isHebrew) "נשלח התראות כשהזמן עומד להיגמר וכדי להציג טיימר פעיל." else "We will notify you when app time is running out.",
                            btnText = if(isHebrew) "אשר התראות" else "Allow",
                            onClick = { (context as? MainActivity)?.requestNotificationPermission() }
                        )

                        5 -> PermissionStep(
                            icon = Icons.Default.QueryStats,
                            title = if(isHebrew) "זיהוי אפליקציות" else "App Detection",
                            desc = if(isHebrew) "צפייה באפליקציות פועלות" else "Monitor active apps",
                            info = if(isHebrew) "כדי לדעת אם פתחת אפליקציה חסומה, עלינו לזהות מה פתוח כרגע." else "Required to detect when a blocked app is opened.",
                            btnText = if(isHebrew) "פתח הגדרות" else "Settings",
                            onClick = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
                        )

                        6 -> PermissionStep(
                            icon = Icons.Default.Layers,
                            title = if(isHebrew) "מסך חסימה" else "Blocking Screen",
                            desc = if(isHebrew) "תצוגה מעל אפליקציות" else "Display over apps",
                            info = if(isHebrew) "זו ההרשאה שמאפשרת לנו להציג את מסך הנעילה מעל האפליקציה החסומה." else "Allows the lock screen to appear over restricted apps.",
                            btnText = if(isHebrew) "אישור" else "Allow",
                            onClick = { context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))) }
                        )

                        7 -> PermissionStep(
                            icon = Icons.Default.SettingsAccessibility, 
                            title = if(isHebrew) "שירותי נגישות" else "Accessibility Service",
                            desc = if(isHebrew) "לחסימה מאובטחת יותר" else "For secure blocking",
                            info = if(isHebrew) "שימוש בשירות הנגישות מבטיח שהחסימה לא תעקף על ידי שימוש בכפתור 'יישומים אחרונים' או פיצול מסך."
                            else "Accessibility ensures the lock cannot be bypassed using the recent apps button or split screen.",
                            btnText = if(isHebrew) "אשר בהגדרות" else "Allow in Settings",
                            onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                        )

                    }
                }
            }
        }
    }
}

@Composable
fun AppearanceOption(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    modifier: Modifier,
    emeraldColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(110.dp),
        shape = RoundedCornerShape(12.dp), // שונה מ-16dp ל-12dp
        border = BorderStroke(if(isSelected) 2.dp else 1.dp, if(isSelected) emeraldColor else Color.LightGray.copy(alpha = 0.5f)),
        color = if(isSelected) emeraldColor.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = if(isSelected) emeraldColor else Color.Gray, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, fontWeight = FontWeight.Bold, color = if(isSelected) emeraldColor else Color.Gray)
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
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Default.Info, null, tint = emeraldColor, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = if (isHebrew) TextAlign.Right else TextAlign.Left,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}