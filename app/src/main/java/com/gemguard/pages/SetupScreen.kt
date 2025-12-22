package com.gemguard.pages

import androidx.compose.ui.text.style.TextAlign
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.gemguard.GemViewModel
import com.gemguard.MainActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(viewModel: GemViewModel, onComplete: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isHebrew = viewModel.language.value == "iw"
    var pin by remember { mutableStateOf("") }
    val currentStep by viewModel.setupStep // שימוש ב-by מקל על הקריאה

    // פונקציות בדיקה
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

    // תיקון קריטי: הוספת currentStep כ-Key כדי שהאובזרבר יתעדכן בשלב הנכון
    DisposableEffect(lifecycleOwner, currentStep) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (currentStep == 3 && hasUsagePermission()) {
                    viewModel.setupStep.intValue = 4
                } else if (currentStep == 4 && hasOverlayPermission()) {
                    viewModel.saveSettings(context)
                    onComplete()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(30.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.padding(top = 20.dp, bottom = 40.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(4) { index ->
                Box(
                    modifier = Modifier
                        .size(if (currentStep == index + 1) 12.dp else 8.dp)
                        .clip(CircleShape)
                        .background(if (currentStep == index + 1) MaterialTheme.colorScheme.primary else Color.LightGray)
                )
            }
        }

        AnimatedContent(targetState = currentStep, label = "SetupTransition") { step ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                when (step) {
                    1 -> {
                        Icon(Icons.Default.Language, null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(if(isHebrew) "ברוכים הבאים!" else "Welcome!", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(20.dp))
                        OutlinedTextField(
                            value = pin,
                            onValueChange = { if (it.length <= 4) pin = it },
                            label = { Text(if(isHebrew) "קוד גישה (4 ספרות)" else "PIN Code") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = { if (pin.length == 4) { viewModel.appPin.value = pin; viewModel.setupStep.intValue = 2 } },
                            modifier = Modifier.padding(top = 20.dp).fillMaxWidth(),
                            enabled = pin.length == 4
                        ) { Text(if(isHebrew) "המשך" else "Next") }
                    }

                    2 -> {
                        Icon(Icons.Default.DirectionsRun, null, modifier = Modifier.size(100.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(if(isHebrew) "מעקב צעדים" else "Step Tracking", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Button(
                            onClick = { (context as? MainActivity)?.requestStepPermission { viewModel.setupStep.intValue = 3 } },
                            modifier = Modifier.padding(top = 20.dp).fillMaxWidth()
                        ) { Text(if(isHebrew) "אשר הרשאת צעדים" else "Grant Permission") }
                    }

                    3 -> {
                        Icon(Icons.Default.QueryStats, null, modifier = Modifier.size(100.dp), tint = MaterialTheme.colorScheme.secondary)
                        Text(if(isHebrew) "גישה לנתוני שימוש" else "Usage Access", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = if(isHebrew) "עליך לאשר את GemGuard ברשימה שתיפתח." else "Enable GemGuard in the list.",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(10.dp)
                        )
                        Button(
                            onClick = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
                            modifier = Modifier.padding(top = 20.dp).fillMaxWidth()
                        ) {
                            Text(if(isHebrew) "פתח הגדרות" else "Open Settings")
                        }
                    }

                    4 -> {
                        Icon(Icons.Default.Layers, null, modifier = Modifier.size(100.dp), tint = Color.Red)
                        Text(if(isHebrew) "תצוגה מעל אפליקציות" else "Display Over Apps", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = if(isHebrew) "אשר תצוגה מעל אפליקציות כדי לחסום הסחות דעת." else "Allow overlay to block distractions.",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(10.dp)
                        )
                        Button(
                            onClick = {
                                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.padding(top = 20.dp).fillMaxWidth()
                        ) { Text(if(isHebrew) "אשר תצוגה מעל" else "Allow Overlay") }
                    }
                }
            }
        }
    }
}