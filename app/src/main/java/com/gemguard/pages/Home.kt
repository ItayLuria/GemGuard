package com.gemguard.pages
import androidx.compose.ui.platform.LocalContext
import android.content.pm.PackageManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gemguard.GemViewModel
import com.gemguard.R
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit
import kotlin.math.min

@Composable
fun Home(viewModel: GemViewModel, onNavigateToStore: () -> Unit) {
    val emeraldColor = Color(0xFF2ECC71)
    val isDark = viewModel.isDarkMode.value
    val isHebrew = viewModel.language.value == "iw"
    val currentSteps = viewModel.currentSteps.value

    // Use remember/derivedStateOf to calculate task logic only when dependencies change
    // This prevents calculation on every recomposition
    val taskState = remember(viewModel.tasks, viewModel.claimedTaskIds, currentSteps) {
        val nextTask = viewModel.tasks.find { !viewModel.claimedTaskIds.contains(it.id) }
        val stepGoal = nextTask?.requiredSteps ?: 10000
        val isComplete = currentSteps >= stepGoal
        val isAllComplete = nextTask == null
        Triple(stepGoal, isComplete, isAllComplete)
    }
    val (stepGoal, isTaskComplete, isAllGoalsComplete) = taskState

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Header Section
        Text(
            text = "GemGuard",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = emeraldColor
        )
        Text(
            text = if (isHebrew) "הצעדים שלך שווים Gems" else "Your steps are worth Gems",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Step Counter Section
        StepCounterSection(
            currentSteps = currentSteps,
            stepGoal = stepGoal,
            isDark = isDark,
            isTaskComplete = isTaskComplete,
            isAllGoalsComplete = isAllGoalsComplete,
            isHebrew = isHebrew,
            emeraldColor = emeraldColor
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Active Apps Section
        ActiveAppsCard(
            unlockedAppsTime = viewModel.unlockedAppsTime,
            isHebrew = isHebrew,
            isDark = isDark,
            emeraldColor = emeraldColor,
            onNavigateToStore = onNavigateToStore
        )

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
fun StepCounterSection(
    currentSteps: Int,
    stepGoal: Int,
    isDark: Boolean,
    isTaskComplete: Boolean,
    isAllGoalsComplete: Boolean,
    isHebrew: Boolean,
    emeraldColor: Color
) {
    val animatedStepCount by animateIntAsState(
        targetValue = currentSteps,
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
        label = "stepCountAnimation"
    )

    Box(
        modifier = Modifier.size(300.dp),
        contentAlignment = Alignment.Center
    ) {
        StepRing(
            currentSteps = currentSteps,
            currentGoal = stepGoal,
            isDark = isDark,
            isAllGoalsComplete = isTaskComplete || isAllGoalsComplete,
            emeraldColor = emeraldColor
        )

        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "$animatedStepCount",
                fontSize = 54.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isDark) Color.White else Color.Black
            )

            Icon(
                painter = painterResource(id = R.drawable.ic_sneaker),
                contentDescription = null,
                tint = if (isDark) Color.White.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.7f),
                modifier = Modifier
                    .size(32.dp)
                    .offset(y = (-55).dp)
            )

            Row(
                modifier = Modifier.offset(y = 50.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = when {
                        isTaskComplete && isHebrew -> "הושלם!"
                        isTaskComplete -> "Completed!"
                        isHebrew -> "מטרה: $stepGoal"
                        else -> "Goal: $stepGoal"
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isTaskComplete) emeraldColor else Color.Gray
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = null,
                    tint = if (isTaskComplete) emeraldColor else Color.Gray,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun ActiveAppsCard(
    unlockedAppsTime: Map<String, Long>, // Assuming Map based on usage .values
    isHebrew: Boolean,
    isDark: Boolean,
    emeraldColor: Color,
    onNavigateToStore: () -> Unit
) {
    // We isolate the periodic time update HERE, so it doesn't redraw the StepRing
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTime = System.currentTimeMillis()
        }
    }

    // Filter apps based on the local ticker
    val activeUnlockedApps = remember(unlockedAppsTime, currentTime) {
        unlockedAppsTime.filter { it.value > currentTime }.toList()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f)
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
            ) {
                Icon(Icons.Default.HourglassEmpty, null, modifier = Modifier.size(18.dp), tint = emeraldColor)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isHebrew) "זמן אפליקציות פעילות" else "Active Apps Time",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }

            if (activeUnlockedApps.isEmpty()) {
                Text(
                    text = if (isHebrew) "אין אפליקציות פעילות כרגע " else "No active apps right now",
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
                Button(
                    onClick = onNavigateToStore,
                    colors = ButtonDefaults.buttonColors(containerColor = emeraldColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.ShoppingCart, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isHebrew) "לקניית זמן בחנות" else "Get more time")
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    activeUnlockedApps.forEach { (packageName, expiryTime) ->
                        // Pass currentTime to row so it doesn't need its own ticker
                        ActiveAppRow(packageName, expiryTime, currentTime, emeraldColor)
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveAppRow(packageName: String, expiryTime: Long, currentTime: Long, activeColor: Color) {
    val context = LocalContext.current

    // אנו משתמשים ב-remember כדי לא להעמיס על המערכת - שולפים את השם פעם אחת בלבד
    val appName = remember(packageName) {
        try {
            val packageManager = context.packageManager
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            // במקרה של שגיאה (למשל אם האפליקציה נמחקה), נשתמש בשיטה הישנה כגיבוי
            packageName.split(".").last().replaceFirstChar { it.uppercase() }
        }
    }

    val timeLeft = expiryTime - currentTime

    if (timeLeft > 0) {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeLeft)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(timeLeft) % 60

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(activeColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // כאן אנחנו משתמשים בשם האמיתי ששלפנו
            Text(
                text = appName,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Text(
                text = String.format("%02d:%02d", minutes, seconds),
                fontWeight = FontWeight.ExtraBold,
                color = activeColor,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun StepRing(currentSteps: Int, currentGoal: Int, isDark: Boolean, isAllGoalsComplete: Boolean, emeraldColor: Color) {
    val railColor = if (isDark) Color.White.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.15f)
    val progress = if (currentGoal > 0) (currentSteps.toFloat() / currentGoal).coerceIn(0f, 1f) else 1f

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1500, easing = FastOutSlowInEasing),
        label = "ringProgress"
    )

    // Only run infinite transition if needed (optional optimization)
    val infiniteTransition = rememberInfiniteTransition(label = "glowEffect")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse),
        label = "glowAlpha"
    )

    Canvas(modifier = Modifier.size(260.dp)) {
        val canvasSize = size.minDimension
        val railThickness = 1.dp.toPx()
        val greenThickness = 20.dp.toPx()
        val outerRailRadius = canvasSize / 2 - 10.dp.toPx()
        val innerRailRadius = outerRailRadius - greenThickness
        val greenRadius = (outerRailRadius + innerRailRadius) / 2
        val strokeStyle = Stroke(width = greenThickness, cap = StrokeCap.Round)

        // 1. Glow Halo (Bottom Layer)
        if (isAllGoalsComplete) {
            drawCircle(
                brush = Brush.radialGradient(
                    0.8f to emeraldColor.copy(alpha = 0f),
                    1.0f to emeraldColor.copy(alpha = 0.4f * glowAlpha),
                    center = center,
                    radius = outerRailRadius + 25.dp.toPx()
                ),
                radius = outerRailRadius + 25.dp.toPx()
            )
        }

        // 2. Background Track
        drawCircle(
            color = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f),
            radius = greenRadius,
            style = Stroke(width = greenThickness)
        )

        // 3. The Green Progress Meter
        if (animatedProgress > 0) {
            // Gradient fill (using sweep gradient)
            drawArc(
                brush = Brush.sweepGradient(
                    0.0f to Color.Transparent,
                    // Adjust gradient stop to prevent hard line at 0/360
                    0.75f to emeraldColor.copy(alpha = 0.5f),
                    center = center
                ),
                startAngle = -90f,
                sweepAngle = 360 * animatedProgress,
                useCenter = false,
                style = strokeStyle,
                topLeft = Offset(center.x - greenRadius, center.y - greenRadius),
                size = Size(greenRadius * 2, greenRadius * 2)
            )

            // Solid overlay for vivid color
            drawArc(
                color = emeraldColor,
                startAngle = -90f,
                sweepAngle = 360 * animatedProgress,
                useCenter = false,
                style = strokeStyle,
                topLeft = Offset(center.x - greenRadius, center.y - greenRadius),
                size = Size(greenRadius * 2, greenRadius * 2)
            )
        }

        // 4. Tick Marks
        val tickCount = 80
        val anglePerTick = 360f / tickCount

        for (i in 0 until tickCount) {
            rotate(degrees = i * anglePerTick) {
                drawLine(
                    color = railColor,
                    start = Offset(x = center.x, y = center.y - outerRailRadius),
                    end = Offset(x = center.x, y = center.y - outerRailRadius + 8.dp.toPx()),
                    strokeWidth = 1.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        // 5. Thin Rails
        drawCircle(color = railColor, radius = outerRailRadius, style = Stroke(width = railThickness))
        drawCircle(color = railColor, radius = innerRailRadius, style = Stroke(width = railThickness))
    }
}