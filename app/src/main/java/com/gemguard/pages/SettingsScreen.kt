package com.gemguard.pages

import android.content.*
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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

    var showPinDialog by remember { mutableStateOf(false) }
    var showWhitelistDialog by remember { mutableStateOf(false) }
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text(
            text = if (isHebrew) "הגדרות" else "Settings",
            fontSize = 32.sp, fontWeight = FontWeight.ExtraBold
        )

        Spacer(modifier = Modifier.height(20.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // תצוגה ושפה
            item { Text(if (isHebrew) "נראות ושפה" else "Appearance & Language", fontSize = 14.sp, color = Color.Gray) }
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp)) {
                    Column {
                        ListItem(
                            headlineContent = { Text(if (isHebrew) "מצב כהה" else "Dark Mode") },
                            trailingContent = {
                                Switch(checked = viewModel.isDarkMode.value, onCheckedChange = {
                                    viewModel.toggleDarkMode()
                                    viewModel.saveSettings(context)
                                })
                            }
                        )
                        ListItem(
                            headlineContent = { Text(if (isHebrew) "שפה / Language" else "Language / שפה") },
                            trailingContent = {
                                TextButton(onClick = {
                                    viewModel.setLanguage(if (isHebrew) "en" else "iw")
                                    viewModel.saveSettings(context)
                                }) { Text(if (isHebrew) "English" else "עברית") }
                            }
                        )
                    }
                }
            }

            // אבטחה
            item { Spacer(modifier = Modifier.height(10.dp)) }
            item { Text(if (isHebrew) "אבטחה וחסימות" else "Security & Blocking", fontSize = 14.sp, color = Color.Gray) }
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp), onClick = { showPinDialog = true }) {
                    ListItem(
                        headlineContent = { Text(if (isHebrew) "ניהול Whitelist" else "Manage Whitelist") },
                        supportingContent = { Text(if (isHebrew) "דרוש קוד גישה" else "Requires PIN") },
                        leadingContent = { Icon(Icons.Default.Lock, null) }
                    )
                }
            }
        }
    }

    // דיאלוג PIN
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false; enteredPin = "" },
            title = { Text(if (isHebrew) "הכנס קוד גישה" else "Enter PIN") },
            text = {
                TextField(
                    value = enteredPin,
                    onValueChange = { if (it.length <= 4) enteredPin = it },
                    isError = pinError,
                    placeholder = { Text("****") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (enteredPin == viewModel.appPin.value) {
                        showPinDialog = false
                        showWhitelistDialog = true
                        enteredPin = ""
                        pinError = false
                    } else { pinError = true }
                }) { Text(if (isHebrew) "אשר" else "Confirm") }
            }
        )
    }

    // דיאלוג Whitelist (כמו קודם, עם תרגום)
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
                                        onCheckedChange = { viewModel.toggleWhitelist(app.packageName) }
                                    )
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.saveSettings(context)
                    showWhitelistDialog = false
                }) { Text(if (isHebrew) "שמור" else "Save") }
            }
        )
    }
}