package com.gemguard

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.app.AppOpsManager
import android.content.*
import android.hardware.*
import android.net.Uri
import android.os.*
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
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
            val isDark = viewModel.isDarkMode.value

            var hasUsagePermission by remember { mutableStateOf(isAccessGranted()) }
            var hasOverlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }

            GemGuardTheme(darkTheme = isDark) {
                // קביעת כיוון הממשק (RTL לעברית, LTR לאנגלית)
                val layoutDirection = if (isHebrew) LayoutDirection.Rtl else LayoutDirection.Ltr

                CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                    val navController = rememberNavController()

                    LaunchedEffect(isSetupComplete, hasUsagePermission, hasOverlayPermission) {
                        if (isSetupComplete && hasUsagePermission && hasOverlayPermission) {
                            context.startService(Intent(context, BlockService::class.java))
                        }
                    }

                    // הודעת חסימת אפליקציה מתורגמת
                    if (blockedAppPackage != null) {
                        AlertDialog(
                            onDismissRequest = { blockedAppPackage = null },
                            title = { Text(if (isHebrew) "אפליקציה חסומה" else "App Blocked") },
                            text = { Text(if (isHebrew) "רוצה לקנות זמן ב-Gems?" else "Want to buy time with Gems?") },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        blockedAppPackage = null
                                        navController.navigate(Screen.Store.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2ECC71), contentColor = Color.White)
                                ) { Text(if (isHebrew) "חנות" else "Store") }
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
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Scaffold(
                            topBar = {
                                if (isSetupComplete) {
                                    GlobalTopBar(viewModel, navController)
                                }
                            },
                            bottomBar = {
                                if (isSetupComplete) {
                                    MyBottomNavigationBar(navController, viewModel, isHebrew)
                                }
                            }
                        ) { padding ->
                            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                                NavHost(
                                    navController = navController,
                                    startDestination = if (isSetupComplete) Screen.Home.route else "setup",
                                    enterTransition = { fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.95f, animationSpec = tween(300)) },
                                    exitTransition = { fadeOut(animationSpec = tween(300)) }
                                ) {
                                    composable("setup") {
                                        SetupScreen(viewModel) {
                                            viewModel.saveSettings(context)
                                            navController.navigate(Screen.Home.route) {
                                                popUpTo("setup") { inclusive = true }
                                            }
                                        }
                                    }
                                    composable(Screen.Home.route) {
                                        Home(viewModel, onNavigateToStore = { navController.navigate(Screen.Store.route) })
                                    }
                                    composable(Screen.Store.route) { StoreScreen(viewModel) }
                                    composable(Screen.Tasks.route) { TasksScreen(viewModel) }
                                    composable(Screen.Settings.route) { SettingsScreen(viewModel) }
                                }
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

    fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        stepSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    override fun onSensorChanged(e: SensorEvent?) {
        e?.let { viewModel.updateStepsWithContext(it.values[0].toInt(), this) }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {}
}

@Composable
fun GlobalTopBar(viewModel: GemViewModel, navController: NavHostController) {
    val isDark = viewModel.isDarkMode.value
    val emeraldColor = Color(0xFF2ECC71)
    val barColor = if (isDark) Color(0xFF1A1A1A) else Color(0xFFFDFDFD)
    val borderColor = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f)

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(barColor)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "GemGuard",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 19.sp,
                color = if (isDark) Color.White else Color.Black
            )

            Surface(
                color = if (isDark) Color(0xFF2C2C2C) else Color(0xFFF0F0F0),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            navController.navigate(Screen.Tasks.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Add, "Add", tint = emeraldColor, modifier = Modifier.size(18.dp))
                    }

                    Box(modifier = Modifier.width(1.dp).height(18.dp).background(borderColor))

                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${viewModel.diamonds.value}",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color.Black
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Default.Diamond, null, tint = emeraldColor, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
        HorizontalDivider(color = borderColor, thickness = 1.dp)
    }
}

@Composable
fun MyBottomNavigationBar(navController: NavHostController, viewModel: GemViewModel, isHebrew: Boolean) {
    val emeraldColor = Color(0xFF2ECC71)
    val isDark = viewModel.isDarkMode.value
    val navBackgroundColor = if (isDark) Color(0xFF1A1A1A) else Color.White
    val unselectedColor = if (isDark) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.4f)
    val selectedContentColor = if (isDark) Color.White else Color.Black

    NavigationBar(containerColor = navBackgroundColor, tonalElevation = 0.dp) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        val screens = listOf(Screen.Home, Screen.Tasks, Screen.Store, Screen.Settings)

        screens.forEach { screen ->
            val isSelected = currentRoute == screen.route
            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (currentRoute != screen.route) {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(screen.icon, null, tint = if (isSelected) emeraldColor else unselectedColor) },
                label = {
                    Text(
                        text = if (isHebrew) screen.title else screen.titleEn,
                        color = if (isSelected) selectedContentColor else unselectedColor,
                        fontSize = 11.sp
                    )
                },
                colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
            )
        }
    }
}