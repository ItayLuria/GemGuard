package com.gemguard.pages

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.gemguard.GemViewModel

@Composable
fun SettingsScreen(navController: NavController, viewModel: GemViewModel) {
    val context = LocalContext.current
    val isHebrew = viewModel.language.value == "iw"
    val isDark = viewModel.isDarkMode.value

    val emeraldColor = Color(0xFF2ECC71)
    val errorColor = Color(0xFFE74C3C)

    val dialogContainerColor = if (isDark) Color(0xFF1E1E1E) else Color(0xFFFDFDFD)
    val textColor = if (isDark) Color.White else Color.Black

    val prefs = remember { context.getSharedPreferences("GemGuardPrefs", Context.MODE_PRIVATE) }

    // מצב הגנה
    var isProtectionEnabled by remember {
        mutableStateOf(prefs.getBoolean("service_enabled", true))
    }

    var isAdminModeActive by remember {
        mutableStateOf(prefs.getBoolean("is_admin_mode", false))
    }

    // ניהול דיאלוגים
    var showWhitelistPinDialog by remember { mutableStateOf(false) }
    var showDisablePinDialog by remember { mutableStateOf(false) }
    var showDisableConfirmDialog by remember { mutableStateOf(false) }
    var showWhitelistDialog by remember { mutableStateOf(false) }
    var showAdminLoginDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    var adminPasswordEntry by remember { mutableStateOf("") }
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }
    var whitelistSearchQuery by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text(
            text = if (isHebrew) "הגדרות" else "Settings",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = emeraldColor
        )

        Spacer(modifier = Modifier.height(20.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Text(if (isHebrew) "נראות ושפה" else "Appearance & Language", fontSize = 14.sp, color = Color.Gray) }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(15.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
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
                                    colors = SwitchDefaults.colors(checkedTrackColor = emeraldColor)
                                )
                            }
                        )
                        ListItem(
                            modifier = Modifier.clickable { showLanguageDialog = true },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = { Text(if (isHebrew) "שפת האפליקציה" else "App Language") },
                            supportingContent = { Text(if (isHebrew) "עברית (IL)" else "English (US)") },
                            trailingContent = { Icon(Icons.Default.Language, null, tint = emeraldColor) }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(10.dp)) }
            item { Text(if (isHebrew) "אבטחה וחסימות" else "Security & Blocking", fontSize = 14.sp, color = Color.Gray) }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(15.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column {
                        ListItem(
                            modifier = Modifier.clickable { showWhitelistPinDialog = true },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = { Text(if (isHebrew) "ניהול Whitelist" else "Manage Whitelist") },
                            supportingContent = { Text(if (isHebrew) "דרוש קוד גישה" else "Requires PIN") },
                            leadingContent = { Icon(Icons.Default.Lock, null, tint = emeraldColor) }
                        )

                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = {
                                Text(
                                    if (isHebrew) "סטטוס הגנה" else "Protection Status",
                                    fontWeight = FontWeight.Bold,
                                    color = if (isProtectionEnabled) emeraldColor else errorColor
                                )
                            },
                            supportingContent = {
                                Text(
                                    if (isProtectionEnabled)
                                        (if (isHebrew) "פעיל - חסימות מופעלות" else "Active - Blocking enabled")
                                    else
                                        (if (isHebrew) "מושבת - אין חסימות" else "Disabled - No blocking")
                                )
                            },
                            leadingContent = {
                                Icon(
                                    if (isProtectionEnabled) Icons.Default.Security else Icons.Default.NoEncryption,
                                    null,
                                    tint = if (isProtectionEnabled) emeraldColor else errorColor
                                )
                            },
                            trailingContent = {
                                Switch(
                                    checked = isProtectionEnabled,
                                    onCheckedChange = { shouldEnable ->
                                        if (shouldEnable) {
                                            isProtectionEnabled = true
                                            // שימוש ב-commit() לכתיבה מיידית לדיסק
                                            prefs.edit().putBoolean("service_enabled", true).commit()
                                            Toast.makeText(context, if (isHebrew) "ההגנה הופעלה" else "Protection Enabled", Toast.LENGTH_SHORT).show()
                                        } else {
                                            showDisablePinDialog = true
                                        }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedTrackColor = emeraldColor,
                                        uncheckedTrackColor = errorColor.copy(alpha = 0.5f)
                                    )
                                )
                            }
                        )
                    }
                }
            }

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
                            ListItem(
                                modifier = Modifier.clickable {
                                    try {
                                        val field = viewModel.javaClass.getDeclaredField("_diamonds")
                                        field.isAccessible = true
                                        val state = field.get(viewModel) as MutableState<Int>
                                        state.value += 100
                                        prefs.edit().putInt("diamonds", state.value).apply()
                                    } catch (e: Exception) { viewModel.initData() }
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                headlineContent = { Text(if (isHebrew) "הוסף 100 יהלומים" else "Add 100 Gems") },
                                leadingContent = { Icon(Icons.Default.Diamond, null, tint = emeraldColor) }
                            )
                            ListItem(
                                modifier = Modifier.clickable {
                                    try {
                                        val field = viewModel.javaClass.getDeclaredField("_currentSteps")
                                        field.isAccessible = true
                                        val state = field.get(viewModel) as MutableState<Int>
                                        state.value += 100
                                        val initialSteps = prefs.getInt("initial_steps", 0)
                                        prefs.edit().putInt("initial_steps", initialSteps - 100).apply()
                                    } catch (e: Exception) { viewModel.initData() }
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                headlineContent = { Text(if (isHebrew) "הוסף 100 צעדים" else "Add 100 Steps") },
                                leadingContent = { Icon(Icons.Default.DirectionsWalk, null, tint = emeraldColor) }
                            )
                            ListItem(
                                modifier = Modifier.clickable {
                                    prefs.edit().clear().commit()
                                    viewModel.initData()
                                    isAdminModeActive = false
                                    navController.navigate("setup") { popUpTo(0) { inclusive = true } }
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                headlineContent = { Text(if (isHebrew) "מחיקת כל הנתונים" else "Wipe All Data", color = errorColor) },
                                leadingContent = { Icon(Icons.Default.DeleteForever, null, tint = errorColor) }
                            )
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(50.dp)) }
        }
    }

    // --- DIALOGS ---

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            containerColor = dialogContainerColor,
            titleContentColor = textColor,
            title = { Text(if (isHebrew) "בחר שפה" else "Select Language", fontWeight = FontWeight.Bold, color = emeraldColor) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf("iw" to "עברית", "en" to "English").forEach { (code, label) ->
                        val isSelected = viewModel.language.value == code
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable {
                                viewModel.setLanguage(code)
                                viewModel.saveSettings(context)
                                showLanguageDialog = false
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) emeraldColor.copy(alpha = 0.1f) else Color.Transparent,
                            border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) emeraldColor else MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(label, color = if (isSelected) emeraldColor else textColor)
                                if (isSelected) Icon(Icons.Default.CheckCircle, null, tint = emeraldColor)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showLanguageDialog = false }) { Text(if (isHebrew) "סגור" else "Close", color = emeraldColor) } }
        )
    }

    if (showAdminLoginDialog) {
        AlertDialog(
            onDismissRequest = { showAdminLoginDialog = false; adminPasswordEntry = "" },
            containerColor = dialogContainerColor,
            title = { Text(if (isHebrew) "כניסת מפתח" else "Developer Login") },
            text = {
                OutlinedTextField(
                    value = adminPasswordEntry, onValueChange = { adminPasswordEntry = it },
                    label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (adminPasswordEntry == "Admin") {
                        isAdminModeActive = true
                        prefs.edit().putBoolean("is_admin_mode", true).commit()
                        showAdminLoginDialog = false
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = emeraldColor)) { Text("Login") }
            }
        )
    }

    if (showWhitelistPinDialog) {
        AlertDialog(
            onDismissRequest = { showWhitelistPinDialog = false; enteredPin = "" },
            containerColor = dialogContainerColor,
            title = { Text(if (isHebrew) "הכנס קוד גישה" else "Enter PIN") },
            text = {
                Column {
                    OutlinedTextField(
                        value = enteredPin, onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) enteredPin = it },
                        isError = pinError, label = { Text("PIN") }, visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                    )
                    if (pinError) Text(if (isHebrew) "קוד שגוי" else "Incorrect PIN", color = errorColor, fontSize = 12.sp)
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (enteredPin == viewModel.appPin.value) {
                        showWhitelistPinDialog = false; showWhitelistDialog = true; enteredPin = ""; pinError = false
                    } else pinError = true
                }, colors = ButtonDefaults.buttonColors(containerColor = emeraldColor)) { Text(if (isHebrew) "אשר" else "Confirm") }
            }
        )
    }

    if (showDisablePinDialog) {
        AlertDialog(
            onDismissRequest = { showDisablePinDialog = false; enteredPin = "" },
            containerColor = dialogContainerColor,
            title = { Text(if (isHebrew) "הכנס קוד לביטול הגנה" else "PIN to Disable") },
            text = {
                Column {
                    OutlinedTextField(
                        value = enteredPin, onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) enteredPin = it },
                        isError = pinError, label = { Text("PIN") }, visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                    )
                    if (pinError) Text(if (isHebrew) "קוד שגוי" else "Incorrect PIN", color = errorColor, fontSize = 12.sp)
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (enteredPin == viewModel.appPin.value) {
                        showDisablePinDialog = false; showDisableConfirmDialog = true; enteredPin = ""; pinError = false
                    } else pinError = true
                }, colors = ButtonDefaults.buttonColors(containerColor = errorColor)) { Text(if (isHebrew) "המשך" else "Continue") }
            }
        )
    }

    if (showDisableConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDisableConfirmDialog = false },
            containerColor = dialogContainerColor,
            icon = { Icon(Icons.Default.Warning, null, tint = errorColor) },
            title = { Text(if (isHebrew) "האם אתה בטוח?" else "Are you sure?") },
            text = { Text(if (isHebrew) "פעולה זו תשבית את ההגנה." else "This will disable protection.") },
            confirmButton = {
                Button(onClick = {
                    isProtectionEnabled = false
                    prefs.edit().putBoolean("service_enabled", false).commit() // שימוש ב-commit
                    Toast.makeText(context, if (isHebrew) "ההגנה הושבתה" else "Protection Disabled", Toast.LENGTH_SHORT).show()
                    showDisableConfirmDialog = false
                }, colors = ButtonDefaults.buttonColors(containerColor = errorColor)) { Text(if (isHebrew) "השבת" else "Disable") }
            },
            dismissButton = { TextButton(onClick = { showDisableConfirmDialog = false }) { Text(if (isHebrew) "ביטול" else "Cancel", color = textColor) } }
        )
    }

    if (showWhitelistDialog) {
        val filteredApps = remember(whitelistSearchQuery, viewModel.allInstalledApps, viewModel.whitelistedApps) {
            viewModel.allInstalledApps.filter { it.name.contains(whitelistSearchQuery, ignoreCase = true) }
                .sortedWith(compareByDescending<GemViewModel.AppInfoData> { viewModel.whitelistedApps.contains(it.packageName) }.thenBy { it.name })
        }
        AlertDialog(
            onDismissRequest = { showWhitelistDialog = false },
            containerColor = dialogContainerColor,
            title = {
                Column {
                    Text(if (isHebrew) "אפליקציות מותרות" else "Whitelisted Apps", fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = whitelistSearchQuery, onValueChange = { whitelistSearchQuery = it },
                        placeholder = { Text(if (isHebrew) "חפש..." else "Search...") },
                        modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.Search, null) }
                    )
                }
            },
            text = {
                Box(modifier = Modifier.heightIn(max = 400.dp)) {
                    LazyColumn {
                        items(filteredApps, key = { it.packageName }) { app ->
                            val isChecked = viewModel.whitelistedApps.contains(app.packageName)
                            Row(modifier = Modifier.fillMaxWidth().clickable { viewModel.toggleWhitelist(app.packageName) }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = isChecked, onCheckedChange = { viewModel.toggleWhitelist(app.packageName) }, colors = CheckboxDefaults.colors(checkedColor = emeraldColor))
                                Text(app.name, color = if (isChecked) emeraldColor else textColor, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.saveSettings(context); showWhitelistDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = emeraldColor)) { Text(if (isHebrew) "שמור" else "Save") }
            }
        )
    }
}