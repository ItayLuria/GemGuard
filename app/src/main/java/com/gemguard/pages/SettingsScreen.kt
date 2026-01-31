package com.gemguard.pages

import androidx.compose.ui.text.style.TextAlign
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.biometric.BiometricPrompt
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
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import com.gemguard.BuildConfig
import com.gemguard.GemViewModel
import java.security.MessageDigest

/**
 * פונקציית עזר להפעלת אימות ביומטרי
 */
fun authenticateBiometric(
    context: Context,
    title: String,
    onSuccess: () -> Unit
) {
    val executor = ContextCompat.getMainExecutor(context)
    val biometricPrompt = BiometricPrompt(
        context as FragmentActivity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
            }
        }
    )

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle(title)
        .setNegativeButtonText("ביטול")
        .build()

    biometricPrompt.authenticate(promptInfo)
}

fun String.toSha256(): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(this.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}

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

    var isProtectionEnabled by remember { mutableStateOf(prefs.getBoolean("service_enabled", true)) }
    var isAdminModeActive by remember { mutableStateOf(prefs.getBoolean("is_admin_mode", false)) }
    var isBiometricEnabled by remember { mutableStateOf(prefs.getBoolean("biometric_enabled", false)) }

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

            // --- אבטחה וחסימות ---
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
                        // סטטוס הגנה - עכשיו הכי למעלה
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = {
                                Text(
                                    if (isHebrew) "סטטוס הגנה" else "Protection Status",
                                    fontWeight = FontWeight.Bold,
                                    color = if (isProtectionEnabled) emeraldColor else errorColor
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
                                            prefs.edit().putBoolean("service_enabled", true).apply()
                                        } else {
                                            showDisablePinDialog = true
                                        }
                                    },
                                    colors = SwitchDefaults.colors(checkedTrackColor = emeraldColor, uncheckedTrackColor = errorColor.copy(alpha = 0.5f))
                                )
                            }
                        )

                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

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
                            headlineContent = { Text(if (isHebrew) "זיהוי ביומטרי" else "Biometric Auth") },
                            supportingContent = { Text(if (isHebrew) "אפשר אימות בטביעת אצבע" else "Enable fingerprint auth") },
                            leadingContent = { Icon(Icons.Default.Fingerprint, null, tint = emeraldColor) },
                            trailingContent = {
                                Switch(
                                    checked = isBiometricEnabled,
                                    onCheckedChange = { enabled ->
                                        isBiometricEnabled = enabled
                                        prefs.edit().putBoolean("biometric_enabled", enabled).apply()
                                    },
                                    colors = SwitchDefaults.colors(checkedTrackColor = emeraldColor)
                                )
                            }
                        )
                    }
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
                            ListItem(
                                modifier = Modifier.clickable { viewModel.triggerTimeMissionForTesting(context) },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                headlineContent = { Text(if (isHebrew) "הפעל משימת זמן" else "Trigger Time Mission") },
                                leadingContent = { Icon(Icons.Default.Alarm, null, tint = emeraldColor) }
                            )
                            ListItem(
                                modifier = Modifier.clickable { viewModel.devAddDiamonds(100) },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                headlineContent = { Text(if (isHebrew) "הוסף 100 יהלומים" else "Add 100 Gems") },
                                leadingContent = { Icon(Icons.Default.Diamond, null, tint = emeraldColor) }
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

    // --- DIALOGS (ה-PIN הרחב והרווח המצומצם נשמרים כאן) ---

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            containerColor = dialogContainerColor,
            title = { Text(if (isHebrew) "בחר שפה" else "Select Language", color = emeraldColor) },
            confirmButton = { Button(onClick = { showLanguageDialog = false }) { Text(if (isHebrew) "סגור" else "Close") } },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("iw" to "עברית", "en" to "English").forEach { (code, label) ->
                        val isSelected = viewModel.language.value == code
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable {
                                viewModel.setLanguage(code)
                                viewModel.saveSettings(context)
                                showLanguageDialog = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (isSelected) emeraldColor else Color.LightGray)
                        ) {
                            Text(label, modifier = Modifier.padding(15.dp), color = textColor)
                        }
                    }
                }
            }
        )
    }

    if (showWhitelistPinDialog) {
        AlertDialog(
            onDismissRequest = { showWhitelistPinDialog = false; enteredPin = ""; pinError = false },
            containerColor = dialogContainerColor,
            title = {
                Text(
                    text = if (isHebrew) "הכנס קוד גישה" else "Enter PIN",
                    color = emeraldColor,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = enteredPin, onValueChange = { if (it.length <= 4) enteredPin = it },
                        label = { Text("PIN") }, visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        isError = pinError,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(0.8f)
                    )
                    if (isBiometricEnabled) {
                        Spacer(modifier = Modifier.height(4.dp))
                        TextButton(
                            onClick = {
                                authenticateBiometric(context, if (isHebrew) "גישה ל-Whitelist" else "Whitelist Access") {
                                    showWhitelistPinDialog = false; showWhitelistDialog = true; enteredPin = ""; pinError = false
                                }
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = textColor.copy(alpha = 0.6f))
                        ) {
                            Icon(Icons.Default.Fingerprint, null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isHebrew) "הזדהות ביומטרית" else "Biometric Auth")
                        }
                    }
                }
            },
            confirmButton = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Button(
                        modifier = Modifier.fillMaxWidth(0.8f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = emeraldColor),
                        onClick = {
                            if (enteredPin == viewModel.appPin.value) {
                                showWhitelistPinDialog = false; showWhitelistDialog = true; enteredPin = ""; pinError = false
                            } else pinError = true
                        }) { Text(if (isHebrew) "אשר" else "Confirm", fontWeight = FontWeight.Bold) }
                }
            }
        )
    }

    if (showDisablePinDialog) {
        AlertDialog(
            onDismissRequest = { showDisablePinDialog = false; enteredPin = ""; pinError = false },
            containerColor = dialogContainerColor,
            title = {
                Text(
                    text = if (isHebrew) "קוד לביטול הגנה" else "PIN to Disable",
                    color = errorColor,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = enteredPin, onValueChange = { if (it.length <= 4) enteredPin = it },
                        label = { Text("PIN") }, visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        isError = pinError,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(0.8f)
                    )
                    if (isBiometricEnabled) {
                        Spacer(modifier = Modifier.height(4.dp))
                        TextButton(
                            onClick = {
                                authenticateBiometric(context, if (isHebrew) "ביטול הגנה" else "Disable Protection") {
                                    showDisablePinDialog = false; showDisableConfirmDialog = true; enteredPin = ""; pinError = false
                                }
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = textColor.copy(alpha = 0.6f))
                        ) {
                            Icon(Icons.Default.Fingerprint, null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isHebrew) "הזדהות ביומטרית" else "Biometric Auth")
                        }
                    }
                }
            },
            confirmButton = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Button(
                        modifier = Modifier.fillMaxWidth(0.8f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = errorColor),
                        onClick = {
                            if (enteredPin == viewModel.appPin.value) {
                                showDisablePinDialog = false; showDisableConfirmDialog = true; enteredPin = ""; pinError = false
                            } else pinError = true
                        }) { Text(if (isHebrew) "המשך" else "Continue", fontWeight = FontWeight.Bold) }
                }
            }
        )
    }

    // שאר הדיאלוגים (DisableConfirmDialog, WhitelistDialog, AdminLoginDialog) ללא שינוי מבני
    if (showDisableConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDisableConfirmDialog = false },
            containerColor = dialogContainerColor,
            icon = { Icon(Icons.Default.WarningAmber, null, tint = errorColor, modifier = Modifier.size(36.dp)) },
            title = {
                Text(
                    text = if (isHebrew) "ביטול הגנת האפליקציות" else "Disable App Protection",
                    fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(), color = textColor
                )
            },
            text = {
                Text(
                    text = if (isHebrew) "האם אתה בטוח שברצונך להשבית את החסימה? פעולה זו תאפשר גישה חופשית."
                    else "Are you sure you want to disable protection? This will allow unrestricted access.",
                    textAlign = TextAlign.Center, fontSize = 15.sp, modifier = Modifier.fillMaxWidth(), color = textColor.copy(alpha = 0.8f)
                )
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                ) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, textColor.copy(alpha = 0.2f)),
                        onClick = { showDisableConfirmDialog = false }
                    ) { Text(if (isHebrew) "חזור" else "Go Back", color = textColor) }

                    Button(
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = errorColor),
                        onClick = {
                            isProtectionEnabled = false
                            prefs.edit().putBoolean("service_enabled", false).apply()
                            showDisableConfirmDialog = false
                        }
                    ) { Text(if (isHebrew) "השבת" else "Disable", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }
                }
            }
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
                    Text(if (isHebrew) "אפליקציות מותרות" else "Whitelisted Apps", color = emeraldColor)
                    OutlinedTextField(
                        value = whitelistSearchQuery, onValueChange = { whitelistSearchQuery = it },
                        placeholder = { Text(if (isHebrew) "חפש..." else "Search...") }, modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            text = {
                Box(modifier = Modifier.heightIn(max = 400.dp)) {
                    LazyColumn {
                        items(filteredApps, key = { it.packageName }) { app ->
                            val isChecked = viewModel.whitelistedApps.contains(app.packageName)
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { viewModel.toggleWhitelist(app.packageName) }.padding(8.dp)) {
                                Checkbox(checked = isChecked, onCheckedChange = { viewModel.toggleWhitelist(app.packageName) }, colors = CheckboxDefaults.colors(checkedColor = emeraldColor))
                                Text(app.name, modifier = Modifier.weight(1f), color = textColor)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    onClick = { viewModel.saveSettings(context); showWhitelistDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = emeraldColor)
                ) { Text(if (isHebrew) "שמור" else "Save", fontWeight = FontWeight.Bold) }
            }
        )
    }

    if (showAdminLoginDialog) {
        AlertDialog(
            onDismissRequest = { showAdminLoginDialog = false; adminPasswordEntry = "" },
            containerColor = dialogContainerColor,
            title = { Text("Developer Login", color = emeraldColor, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    OutlinedTextField(
                        value = adminPasswordEntry, onValueChange = { adminPasswordEntry = it },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        label = { Text("Password") },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    onClick = {
                        if (adminPasswordEntry.toSha256() == BuildConfig.ADMIN_PASSWORD) {
                            isAdminModeActive = true
                            prefs.edit().putBoolean("is_admin_mode", true).apply()
                            showAdminLoginDialog = false; adminPasswordEntry = ""
                        }
                    }) { Text("Login", fontWeight = FontWeight.Bold) }
            }
        )
    }
}