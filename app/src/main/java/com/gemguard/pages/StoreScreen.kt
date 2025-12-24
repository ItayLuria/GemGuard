package com.gemguard.pages

import android.app.usage.UsageStatsManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.gemguard.GemViewModel
import kotlinx.coroutines.delay
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreScreen(viewModel: GemViewModel) {
    val context = LocalContext.current
    val pm = context.packageManager
    val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val isHebrew = viewModel.language.value == "iw" // תמיכה בשפה

    var selectedAppForPurchase by remember { mutableStateOf<GemViewModel.AppInfoData?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val emeraldColor = Color(0xFF2ECC71)
    val darkEmerald = if (viewModel.isDarkMode.value) Color(0xFFA9DFBF) else Color(0xFF1B5E20)

    LaunchedEffect(Unit) {
        viewModel.loadInstalledApps(context)
    }

    val statsMap = remember {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
        }.timeInMillis
        usageStatsManager.queryAndAggregateUsageStats(today, System.currentTimeMillis())
    }

    val filteredApps by remember(searchQuery, viewModel.allInstalledApps, viewModel.whitelistedApps) {
        derivedStateOf {
            viewModel.allInstalledApps.filter {
                !viewModel.whitelistedApps.contains(it.packageName) &&
                        it.name.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val now = System.currentTimeMillis()
    val activeApps = filteredApps.filter { (viewModel.unlockedAppsTime[it.packageName] ?: 0L) > now }
    val otherApps = filteredApps.filter { (viewModel.unlockedAppsTime[it.packageName] ?: 0L) <= now }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = if (isHebrew) "חנות Gems" else "Gems Store",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = emeraldColor
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Diamond, null, tint = emeraldColor, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isHebrew) "היתרה שלך: ${viewModel.diamonds.value} Gems" else "Your Balance: ${viewModel.diamonds.value} Gems",
                color = emeraldColor,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(if (isHebrew) "חפש אפליקציה..." else "Search app...") },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = emeraldColor) },
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (activeApps.isNotEmpty()) {
                item {
                    Text(
                        text = if (isHebrew) "אפליקציות פועלות" else "Active Apps",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = emeraldColor
                    )
                }

                items(activeApps, key = { it.packageName }) { app: GemViewModel.AppInfoData ->
                    AppStoreItem(app, viewModel, statsMap, pm, true, emeraldColor) {
                        selectedAppForPurchase = it
                    }
                }
                item { Spacer(modifier = Modifier.height(12.dp)) }
            }

            if (otherApps.isNotEmpty()) {
                item {
                    Text(
                        text = if (isHebrew) "כל האפליקציות" else "All Apps",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                items(otherApps, key = { it.packageName }) { app: GemViewModel.AppInfoData ->
                    AppStoreItem(app, viewModel, statsMap, pm, false, emeraldColor) {
                        selectedAppForPurchase = it
                    }
                }
            }
        }
    }

    selectedAppForPurchase?.let { app ->
        PurchaseDialog(app, viewModel, statsMap, context, emeraldColor, darkEmerald) { selectedAppForPurchase = null }
    }
}

@Composable
fun AppStoreItem(
    app: GemViewModel.AppInfoData,
    viewModel: GemViewModel,
    statsMap: Map<String, android.app.usage.UsageStats>,
    pm: android.content.pm.PackageManager,
    isActive: Boolean,
    emeraldColor: Color,
    onPurchaseClick: (GemViewModel.AppInfoData) -> Unit
) {
    val isHebrew = viewModel.language.value == "iw"
    var remainingMillis by remember { mutableLongStateOf(0L) }

    if (isActive) {
        LaunchedEffect(app.packageName) {
            while (true) {
                val expiry = viewModel.unlockedAppsTime[app.packageName] ?: 0L
                remainingMillis = (expiry - System.currentTimeMillis()).coerceAtLeast(0)
                if (remainingMillis <= 0) break
                delay(1000)
            }
        }
    }

    val appIcon = remember(app.packageName) {
        try {
            val drawable = pm.getApplicationIcon(app.packageName)
            val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap.asImageBitmap()
        } catch (e: Exception) { null }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) emeraldColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = if (isActive) androidx.compose.foundation.BorderStroke(1.dp, emeraldColor) else null
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            appIcon?.let { Image(bitmap = it, contentDescription = null, modifier = Modifier.size(48.dp).clip(CircleShape)) }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(app.name, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                if (isActive && remainingMillis > 0) {
                    val minutes = (remainingMillis / 1000) / 60
                    val seconds = (remainingMillis / 1000) % 60
                    val timeString = if (isHebrew) "%02d:%02d נותרו" else "%02d:%02d left"
                    Text(String.format(timeString, minutes, seconds), color = emeraldColor, fontWeight = FontWeight.ExtraBold)
                } else {
                    val timeUsedMin = (statsMap[app.packageName]?.totalTimeInForeground ?: 0L) / 60000
                    val usageText = if (isHebrew) "שימוש היום: $timeUsedMin דק'" else "Today's use: $timeUsedMin min"
                    Text(usageText, color = Color.Gray, fontSize = 12.sp)
                }
            }
            Button(
                onClick = { onPurchaseClick(app) },
                colors = ButtonDefaults.buttonColors(containerColor = emeraldColor),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text(
                    text = if (isActive) (if (isHebrew) "עוד" else "More") else (if (isHebrew) "קנה" else "Buy"),
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun PurchaseDialog(
    app: GemViewModel.AppInfoData,
    viewModel: GemViewModel,
    statsMap: Map<String, android.app.usage.UsageStats>,
    context: Context,
    emeraldColor: Color,
    darkEmerald: Color,
    onDismiss: () -> Unit
) {
    val isHebrew = viewModel.language.value == "iw"
    val pm = context.packageManager
    val timeUsedMin = (statsMap[app.packageName]?.totalTimeInForeground ?: 0L) / 60000
    val usagePenalty = (timeUsedMin / 30).toInt() * 10
    var showStatusMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }

    val appIcon = remember(app.packageName) {
        try {
            val drawable = pm.getApplicationIcon(app.packageName)
            val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap.asImageBitmap()
        } catch (e: Exception) { null }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (showStatusMessage != null) {
                    Icon(
                        imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (isSuccess) emeraldColor else Color.Red,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        showStatusMessage!!,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        lineHeight = 28.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = emeraldColor),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isHebrew) "הבנתי, תודה" else "Got it, thanks", fontWeight = FontWeight.Bold)
                    }
                } else {
                    appIcon?.let {
                        Image(bitmap = it, contentDescription = null, modifier = Modifier.size(64.dp).clip(CircleShape))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(app.name, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                    Text(
                        text = if (isHebrew) "פתיחה לזמן מוגבל" else "Unlock for limited time",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )

                    if (usagePenalty > 0) {
                        Surface(
                            color = Color.Red.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text(
                                text = if (isHebrew) "אפליקציה בשימוש מוגבר: $usagePenalty Gems יותר" else "Heavy usage: $usagePenalty Gems extra",
                                color = Color.Red,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    val packages = if (isHebrew) {
                        listOf(Triple(5, 30, "5 דקות"), Triple(15, 70, "15 דקות"), Triple(30, 120, "30 דקות"), Triple(60, 200, "שעה אחת"))
                    } else {
                        listOf(Triple(5, 30, "5 Minutes"), Triple(15, 70, "15 Minutes"), Triple(30, 120, "30 Minutes"), Triple(60, 200, "1 Hour"))
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        packages.chunked(2).forEach { rowItems ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                rowItems.forEach { (mins, basePrice, label) ->
                                    val finalPrice = basePrice + usagePenalty
                                    Surface(
                                        onClick = {
                                            if (viewModel.diamonds.value >= finalPrice) {
                                                viewModel.buyTimeForApp(app.packageName, mins, finalPrice, context)
                                                isSuccess = true
                                                showStatusMessage = if (isHebrew) "תהנה!\n$label של ${app.name} פתוחים עכשיו." else "Enjoy!\n$label of ${app.name} are now open."
                                            } else {
                                                isSuccess = false
                                                val needed = finalPrice - viewModel.diamonds.value
                                                showStatusMessage = if (isHebrew) "חסרים לך עוד $needed Gems..." else "You need $needed more Gems..."
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (viewModel.isDarkMode.value) Color(0xFF2C2C2C) else Color(0xFFF5F5F5),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, emeraldColor.copy(alpha = 0.3f))
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("$finalPrice", fontWeight = FontWeight.ExtraBold, color = emeraldColor, fontSize = 18.sp)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(Icons.Default.Diamond, null, tint = emeraldColor, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    TextButton(onClick = onDismiss) {
                        Text(if (isHebrew) "ביטול" else "Cancel", color = Color.Gray)
                    }
                }
            }
        }
    }
}