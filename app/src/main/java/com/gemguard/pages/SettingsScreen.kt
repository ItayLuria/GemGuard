package com.gemguard.pages

import android.content.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gemguard.GemViewModel

@Composable
fun SettingsScreen(viewModel: GemViewModel) {
    val context = LocalContext.current
    val isHebrew = viewModel.language.value == "iw"
    val emeraldColor = Color(0xFF2ECC71)

    var showPinDialog by remember { mutableStateOf(false) }
    var showWhitelistDialog by remember { mutableStateOf(false) }
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text(
            text = if (isHebrew) "הגדרות" else "Settings",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = emeraldColor // כותרת בצבע ירוק אמרלד
        )

        Spacer(modifier = Modifier.height(20.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // --- נראות ושפה ---
            item { Text(if (isHebrew) "נראות ושפה" else "Appearance & Language", fontSize = 14.sp, color = Color.Gray) }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(15.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent), // הסרת רקע אפור
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant) // מסגרת עדינה
                ) {
                    Column {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = { Text(if (isHebrew) "מצב כהה" else "Dark Mode") },
                            trailingContent = {
                                Switch(
                                    checked = viewModel.isDarkMode.value,
                                    onCheckedChange = {
                                        viewModel.toggleDarkMode()
                                        viewModel.saveSettings(context)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,        // עיגול לבן
                                        checkedTrackColor = emeraldColor,      // רקע ירוק
                                        uncheckedThumbColor = Color.Gray,
                                        uncheckedTrackColor = Color.LightGray.copy(alpha = 0.5f)
                                    )
                                )
                            }
                        )
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = { Text(if (isHebrew) "שפה / Language" else "Language / שפה") },
                            trailingContent = {
                                TextButton(onClick = {
                                    viewModel.setLanguage(if (isHebrew) "en" else "iw")
                                    viewModel.saveSettings(context)
                                }) { Text(if (isHebrew) "English" else "עברית", color = emeraldColor) }
                            }
                        )
                    }
                }
            }

            // --- אבטחה ---
            item { Spacer(modifier = Modifier.height(10.dp)) }
            item { Text(if (isHebrew) "אבטחה וחסימות" else "Security & Blocking", fontSize = 14.sp, color = Color.Gray) }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(15.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                    onClick = { showPinDialog = true }
                ) {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = { Text(if (isHebrew) "ניהול Whitelist" else "Manage Whitelist") },
                        supportingContent = { Text(if (isHebrew) "דרוש קוד גישה" else "Requires PIN") },
                        leadingContent = { Icon(Icons.Default.Lock, null, tint = emeraldColor) }
                    )
                }
            }

            // --- אפשרויות מפתחים ---
            item { Spacer(modifier = Modifier.height(20.dp)) }
            item { Text(if (isHebrew) "אפשרויות מפתחים" else "Developer Options", fontSize = 14.sp, color = Color.Gray) }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(15.dp),
                    colors = CardDefaults.cardColors(containerColor = emeraldColor.copy(alpha = 0.05f)),
                    border = BorderStroke(0.5.dp, emeraldColor.copy(alpha = 0.2f))
                ) {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = { Text(if (isHebrew) "הוסף 1,000 Gems" else "Add 1,000 Gems") },
                        supportingContent = { Text(if (isHebrew) "לצורכי בדיקה בלבד" else "For testing purposes only") },
                        leadingContent = { Icon(Icons.Default.Diamond, null, tint = emeraldColor) },
                        modifier = Modifier.clickable {
                            viewModel.addDiamonds(1000, -1, context)
                        }
                    )
                }
            }

            // --- מגזר תחזוקה ---
            item { Spacer(modifier = Modifier.height(10.dp)) }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(15.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.05f)),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
                ) {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = {
                            Text(
                                if (isHebrew) "איפוס סטאפ מחדש" else "Reset Setup",
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        supportingContent = { Text(if (isHebrew) "מחזיר למסך ההגדרות הראשוניות" else "Returns to onboarding screens") },
                        leadingContent = { Icon(Icons.Default.Refresh, null, tint = MaterialTheme.colorScheme.error) },
                        modifier = Modifier.clickable { viewModel.resetSetup(context) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(50.dp)) }
        }
    }

    // דיאלוג PIN
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false; enteredPin = "" },
            title = { Text(if (isHebrew) "הכנס קוד גישה" else "Enter PIN") },
            text = {
                OutlinedTextField(
                    value = enteredPin,
                    onValueChange = { if (it.length <= 4) enteredPin = it },
                    isError = pinError,
                    placeholder = { Text("****") },
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (enteredPin == viewModel.appPin.value) {
                            showPinDialog = false
                            showWhitelistDialog = true
                            enteredPin = ""
                            pinError = false
                        } else { pinError = true }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = emeraldColor)
                ) { Text(if (isHebrew) "אשר" else "Confirm") }
            }
        )
    }

    // דיאלוג Whitelist
    if (showWhitelistDialog) {
        AlertDialog(
            onDismissRequest = { showWhitelistDialog = false },
            title = { Text(if (isHebrew) "אפליקציות מותרות" else "Whitelisted Apps") },
            text = {
                Box(modifier = Modifier.height(400.dp)) {
                    LazyColumn {
                        items(viewModel.allInstalledApps) { app ->
                            ListItem(
                                headlineContent = { Text(app.name) },
                                leadingContent = {
                                    Checkbox(
                                        checked = viewModel.whitelistedApps.contains(app.packageName),
                                        onCheckedChange = { viewModel.toggleWhitelist(app.packageName) },
                                        colors = CheckboxDefaults.colors(checkedColor = emeraldColor)
                                    )
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveSettings(context)
                        showWhitelistDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = emeraldColor)
                ) { Text(if (isHebrew) "שמור" else "Save") }
            }
        )
    }
}