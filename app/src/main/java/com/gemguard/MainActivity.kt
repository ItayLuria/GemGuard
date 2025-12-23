// קובץ: MainActivity.kt
package com.gemguard

import android.app.AppOpsManager
import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.hardware.*
import android.net.Uri
import android.os.*
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.compose.*
import com.gemguard.pages.*
import com.gemguard.ui.theme.GemGuardTheme
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.Lifecycle

class MainActivity : ComponentActivity(), SensorEventListener {
    private val viewModel: GemViewModel by viewModels()
    private var blockedAppPackage by mutableStateOf<String?>(null)
    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.initData(this)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        blockedAppPackage = intent.getStringExtra("blocked_app")

        setContent {
            val isSetupComplete = viewModel.isSetupCompleteState.value
            val context = LocalContext.current
            val isHebrew = viewModel.language.value == "iw"

            var hasUsagePermission by remember { mutableStateOf(isAccessGranted()) }
            var hasOverlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }

            // שימוש ב-Theme שמושפע מה-ViewModel
            GemGuardTheme(darkTheme = viewModel.isDarkMode.value) {
                val layoutDirection = if (isHebrew) LayoutDirection.Rtl else LayoutDirection.Ltr

                CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                    val navController = rememberNavController()

                    LaunchedEffect(isSetupComplete, hasUsagePermission, hasOverlayPermission) {
                        if (isSetupComplete && hasUsagePermission && hasOverlayPermission) {
                            context.startService(Intent(context, BlockService::class.java))
                        }
                    }

                    if (blockedAppPackage != null) {
                        AlertDialog(
                            onDismissRequest = { blockedAppPackage = null },
                            title = { Text(if (isHebrew) "אפליקציה חסומה" else "App Blocked") },
                            text = { Text(if (isHebrew) "רוצה לקנות זמן ב-Gems?" else "Want to buy time with Gems?") },
                            confirmButton = {
                                Button(onClick = {
                                    blockedAppPackage = null
                                    navController.navigate(Screen.Store.route)
                                }) { Text(if (isHebrew) "חנות" else "Store") }
                            },
                            dismissButton = {
                                TextButton(onClick = {
                                    blockedAppPackage = null
                                    startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME))
                                }) { Text(if (isHebrew) "סגור" else "Close") }
                            }
                        )
                    }

                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background // רקע דינמי
                    ) {
                        Scaffold(
                            bottomBar = {
                                if (isSetupComplete) {
                                    NavigationBar {
                                        val screens = listOf(Screen.Home, Screen.Tasks, Screen.Store, Screen.Settings)
                                        screens.forEach { screen ->
                                            NavigationBarItem(
                                                icon = { Icon(screen.icon, null) },
                                                label = { Text(if (isHebrew) screen.title else screen.titleEn) },
                                                selected = navController.currentBackStackEntryAsState().value?.destination?.route == screen.route,
                                                onClick = {
                                                    navController.navigate(screen.route) {
                                                        popUpTo(navController.graph.startDestinationId)
                                                        launchSingleTop = true
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        ) { padding ->
                            NavHost(
                                navController = navController,
                                startDestination = if (isSetupComplete) Screen.Home.route else "setup",
                                modifier = Modifier.padding(padding)
                            ) {
                                composable("setup") {
                                    SetupScreen(viewModel) {
                                        viewModel.saveSettings(context)
                                        navController.navigate(Screen.Home.route) {
                                            popUpTo("setup") { inclusive = true }
                                        }
                                    }
                                }
                                composable(Screen.Home.route) { Home(viewModel) }
                                composable(Screen.Store.route) { StoreScreen(viewModel) }
                                composable(Screen.Tasks.route) { TasksScreen(viewModel) }
                                composable(Screen.Settings.route) { SettingsScreen(viewModel = viewModel) }
                            }
                        }
                    }
                }
            }

            DisposableEffect(Unit) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        hasUsagePermission = isAccessGranted()
                        hasOverlayPermission = Settings.canDrawOverlays(context)
                    }
                }
                lifecycle.addObserver(observer)
                onDispose { lifecycle.removeObserver(observer) }
            }
        }
    }

    private fun isAccessGranted(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun requestStepPermission(onNext: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACTIVITY_RECOGNITION), 100)
            }
        }
        onNext()
    }

    override fun onResume() {
        super.onResume()
        stepSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        blockedAppPackage = intent.getStringExtra("blocked_app")
    }

    override fun onSensorChanged(e: SensorEvent?) {
        e?.let { viewModel.updateStepsWithContext(it.values[0].toInt(), this) }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {}
}