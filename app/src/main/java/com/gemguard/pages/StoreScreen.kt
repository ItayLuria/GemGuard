package com.gemguard.pages

import android.app.usage.UsageStatsManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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

    var selectedAppForPurchase by remember { mutableStateOf<GemViewModel.AppInfoData?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    // צבעי המותג החדשים
    val emeraldColor = Color(0xFF2ECC71)
    val darkEmerald = Color(0xFF1B5E20)

    LaunchedEffect(Unit) { viewModel.loadInstalledApps(context) }

    // טיימר לרענון זמן נותר
    var tick by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            tick++
        }
    }

    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
    }.timeInMillis
    val statsMap = usageStatsManager.queryAndAggregateUsageStats(today, System.currentTimeMillis())

    val filteredApps = viewModel.allInstalledApps.filter {
        !viewModel.whitelistedApps.contains(it.packageName) &&
                it.name.contains(searchQuery, ignoreCase = true)
    }

    val activeApps = filteredApps.filter { (viewModel.unlockedAppsTime[it.packageName] ?: 0L) > System.currentTimeMillis() }
    val otherApps = filteredApps.filter { (viewModel.unlockedAppsTime[it.packageName] ?: 0L) <= System.currentTimeMillis() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("חנות Gems", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = darkEmerald)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Diamond, null, tint = emeraldColor, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("היתרה שלך: ${viewModel.diamonds.value} Gems", color = emeraldColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("חפש אפליקציה...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = emeraldColor) },
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = emeraldColor,
                unfocusedBorderColor = Color.LightGray,
                focusedContainerColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (activeApps.isNotEmpty()) {
                item {
                    Text("אפליקציות פועלות", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = emeraldColor)
                }
                items(activeApps) { app ->
                    AppStoreItem(app, viewModel, statsMap, pm, true, emeraldColor, darkEmerald) { selectedAppForPurchase = it }
                }
                item { Spacer(modifier = Modifier.height(12.dp)) }
            }

            if (otherApps.isNotEmpty()) {
                item {
                    Text("כל האפליקציות", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = darkEmerald)
                }
                items(otherApps) { app ->
                    AppStoreItem(app, viewModel, statsMap, pm, false, emeraldColor, darkEmerald) { selectedAppForPurchase = it }
                }
            } else if (filteredApps.isEmpty() && searchQuery.isNotEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                        Text("לא נמצאו תוצאות ל- \"$searchQuery\"", color = Color.Gray)
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
    darkEmerald: Color,
    onPurchaseClick: (GemViewModel.AppInfoData) -> Unit
) {
    val expiryTime = viewModel.unlockedAppsTime[app.packageName] ?: 0L
    val remainingMillis = (expiryTime - System.currentTimeMillis()).coerceAtLeast(0)

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
        elevation = CardDefaults.cardElevation(if (isActive) 6.dp else 2.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isActive) Color(0xFFE8F5E9) else Color.White),
        border = if (isActive) androidx.compose.foundation.BorderStroke(1.dp, emeraldColor) else null
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            appIcon?.let {
                Image(bitmap = it, contentDescription = null, modifier = Modifier.size(48.dp).clip(CircleShape))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(app.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = darkEmerald)
                if (isActive) {
                    val remainingText = String.format("%02d:%02d", remainingMillis / 60000, (remainingMillis % 60000) / 1000)
                    Text("נותר: $remainingText", fontSize = 13.sp, color = emeraldColor, fontWeight = FontWeight.ExtraBold)
                } else {
                    val timeUsedMin = (statsMap[app.packageName]?.totalTimeInForeground ?: 0L) / 60000
                    Text("שימוש היום: $timeUsedMin דק'", fontSize = 12.sp, color = Color.Gray)
                }
            }

            Button(
                onClick = { onPurchaseClick(app) },
                colors = ButtonDefaults.buttonColors(containerColor = emeraldColor),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(if (isActive) "הוסף" else "קנה", fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
    val pm = context.packageManager
    val timeUsedMin = (statsMap[app.packageName]?.totalTimeInForeground ?: 0L) / 60000
    val usagePenalty = (timeUsedMin / 30).toInt() * 10

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
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                appIcon?.let {
                    Image(bitmap = it, contentDescription = null, modifier = Modifier.size(72.dp).clip(CircleShape))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(app.name, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = darkEmerald)
                Text("בחר חבילת זמן", color = Color.Gray, fontSize = 14.sp)

                Spacer(modifier = Modifier.height(24.dp))

                val packages = listOf(
                    Triple(5, 30, "5 דקות"),
                    Triple(15, 70, "15 דקות"),
                    Triple(30, 120, "חצי שעה"),
                    Triple(60, 200, "שעה אחת")
                )

                packages.forEach { (mins, basePrice, label) ->
                    val finalPrice = basePrice + usagePenalty
                    Button(
                        onClick = {
                            viewModel.buyTimeForApp(app.packageName, mins, finalPrice, context)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F8E9), contentColor = darkEmerald),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, emeraldColor.copy(alpha = 0.2f))
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(label, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("$finalPrice", fontWeight = FontWeight.ExtraBold, color = emeraldColor)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.Diamond, null, tint = emeraldColor, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onDismiss) { Text("ביטול", color = Color.Gray) }
            }
        }
    }
}