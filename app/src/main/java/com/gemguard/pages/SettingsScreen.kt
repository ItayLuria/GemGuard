package com.gemguard.pages

import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    val errorColor = Color(0xFFE74C3C)

    val prefs = remember { context.getSharedPreferences("GemGuardPrefs", Context.MODE_PRIVATE) }
    var isAdminModeActive by remember {
        mutableStateOf(prefs.getBoolean("is_admin_mode", false))
    }

    // משתני עזר לדיאלוגים
    var showPinDialog by remember { mutableStateOf(false) }
    var showWhitelistDialog by remember { mutableStateOf(false) }
    var showAdminLoginDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    var adminPasswordEntry by remember { mutableStateOf("") }
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text(
            text = if (isHebrew) "הגדרות" else "Settings",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = emeraldColor
        )

        Spacer(modifier = Modifier.height(20.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // --- נראות ושפה ---
            item { Text(if (isHebrew) "נראות ושפה" else "Appearance & Language", fontSize = 14.sp, color = Color.Gray) }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(15.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column {
                        // מצב כהה
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
                                    colors = SwitchDefaults.colors(checkedTrackColor = emeraldColor)
                                )
                            }
                        )
                        // בחירת שפה
                        ListItem(
                            modifier = Modifier.clickable { showLanguageDialog = true },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = { Text(if (isHebrew) "שפת האפליקציה" else "App Language") },
                            supportingContent = {
                                Text(if (isHebrew) "עברית (IL)" else "English (US)")
                            },
                            trailingContent = {
                                Icon(Icons.Default.Language, contentDescription = null, tint = emeraldColor)
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
                    modifier = Modifier.fillMaxWidth().clickable { showPinDialog = true },
                    shape = RoundedCornerShape(15.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
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
            item { Spacer(modifier = Modifier.height(10.dp)) }
            item { Text(if (isHebrew) "אפשרויות מפתחים" else "Developer Options", fontSize = 14.sp, color = Color.Gray) }

            if (!isAdminModeActive) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { showAdminLoginDialog = true },
                        shape = RoundedCornerShape(15.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = { Text(if (isHebrew) "הפעל מצב מפתח" else "Enable Dev Mode") },
                            leadingContent = { Icon(Icons.Default.Code, null, tint = emeraldColor) }
                        )
                    }
                }
            } else {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(15.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        border = BorderStroke(0.5.dp, emeraldColor.copy(alpha = 0.5f))
                    ) {
                        Column {
                            // הוספת 100 יהלומים
                            ListItem(
                                modifier = Modifier.clickable {
                                    try {
                                        val field = viewModel.javaClass.getDeclaredField("_diamonds")
                                        field.isAccessible = true
                                        val state = field.get(viewModel) as MutableState<Int>
                                        state.value += 100
                                        prefs.edit().putInt("diamonds", state.value).apply()
                                    } catch (e: Exception) { viewModel.initData(context) }
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                headlineContent = { Text(if (isHebrew) "הוסף 100 יהלומים" else "Add 100 Gems") },
                                leadingContent = { Icon(Icons.Default.Diamond, null, tint = emeraldColor) }
                            )

                            // הוספת 100 צעדים
                            ListItem(
                                modifier = Modifier.clickable {
                                    try {
                                        val field = viewModel.javaClass.getDeclaredField("_currentSteps")
                                        field.isAccessible = true
                                        val state = field.get(viewModel) as MutableState<Int>
                                        state.value += 100
                                        val initialSteps = prefs.getInt("initial_steps", 0)
                                        prefs.edit().putInt("initial_steps", initialSteps - 100).apply()
                                    } catch (e: Exception) { viewModel.initData(context) }
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                headlineContent = { Text(if (isHebrew) "הוסף 100 צעדים" else "Add 100 Steps") },
                                leadingContent = { Icon(Icons.Default.DirectionsWalk, null, tint = emeraldColor) }
                            )

                            // מחיקת כל הנתונים
                            ListItem(
                                modifier = Modifier.clickable {
                                    prefs.edit().clear().apply()
                                    try {
                                        val diagField = viewModel.javaClass.getDeclaredField("_diamonds")
                                        diagField.isAccessible = true
                                        (diagField.get(viewModel) as MutableState<Int>).value = 0
                                        val stepField = viewModel.javaClass.getDeclaredField("_currentSteps")
                                        stepField.isAccessible = true
                                        (stepField.get(viewModel) as MutableState<Int>).value = 0
                                        val claimedField = viewModel.javaClass.getDeclaredField("_claimedTaskIds")
                                        claimedField.isAccessible = true
                                        (claimedField.get(viewModel) as MutableList<Int>).clear()
                                    } catch (e: Exception) {}
                                    viewModel.initData(context)
                                    isAdminModeActive = false
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                headlineContent = { Text(if (isHebrew) "מחיקת כל הנתונים" else "Wipe All Data", color = errorColor, fontWeight = FontWeight.Bold) },
                                leadingContent = { Icon(Icons.Default.DeleteForever, null, tint = errorColor) }
                            )
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(50.dp)) }
        }
    }

    // --- דיאלוג בחירת שפה מעוצב (הגרסה היפה) ---
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            shape = RoundedCornerShape(28.dp),
            title = {
                Text(
                    text = if (isHebrew) "בחר שפה" else "Select Language",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = emeraldColor
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val languages = listOf("iw" to "עברית", "en" to "English")
                    languages.forEach { (code, label) ->
                        val isSelected = viewModel.language.value == code
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setLanguage(code)
                                    viewModel.saveSettings(context)
                                    showLanguageDialog = false
                                },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) emeraldColor.copy(alpha = 0.1f) else Color.Transparent,
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) emeraldColor else MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 18.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) emeraldColor else MaterialTheme.colorScheme.onSurface
                                )
                                if (isSelected) {
                                    Icon(Icons.Default.CheckCircle, null, tint = emeraldColor)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(if (isHebrew) "סגור" else "Close", color = emeraldColor)
                }
            }
        )
    }

    // --- דיאלוג כניסת מפתח ---
    if (showAdminLoginDialog) {
        AlertDialog(
            onDismissRequest = { showAdminLoginDialog = false; adminPasswordEntry = "" },
            title = { Text(if (isHebrew) "כניסת מפתח" else "Developer Login") },
            text = {
                OutlinedTextField(
                    value = adminPasswordEntry,
                    onValueChange = { adminPasswordEntry = it },
                    label = { Text("Password") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (adminPasswordEntry == "Admin") {
                            isAdminModeActive = true
                            prefs.edit().putBoolean("is_admin_mode", true).apply()
                            showAdminLoginDialog = false
                            adminPasswordEntry = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = emeraldColor)
                ) { Text("Login") }
            }
        )
    }

    // --- דיאלוג PIN ---
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

    // --- דיאלוג Whitelist ---
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
                Button(onClick = { viewModel.saveSettings(context); showWhitelistDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = emeraldColor)) {
                    Text(if (isHebrew) "שמור" else "Save")
                }
            }
        )
    }
}