package com.gemguard.pages

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gemguard.GemViewModel
import com.gemguard.R

@Composable
fun Home(viewModel: GemViewModel) {
    val emeraldColor = Color(0xFF2ECC71)
    val isDark = viewModel.isDarkMode.value
    val currentSteps = viewModel.currentSteps.value

    // מציאת המשימה הבאה שטרם הושלמה (Claimed)
    val nextTask = viewModel.tasks.find { !it.isCompleted(viewModel.claimedTaskIds) }
    val stepGoal = nextTask?.requiredSteps ?: 10000
    val isAllGoalsComplete = nextTask == null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "GemGuard",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = emeraldColor
        )
        Text(
            text = "הצעדים שלך שווים Gems",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(40.dp))

        // מד הצעדים (הטבעת)
        Box(
            modifier = Modifier.size(300.dp),
            contentAlignment = Alignment.Center
        ) {
            StepRing(
                currentSteps = currentSteps,
                currentGoal = stepGoal,
                isDark = isDark,
                isAllGoalsComplete = isAllGoalsComplete,
                emeraldColor = emeraldColor
            )

            // סידור פנימי של המספר, האייקון והמטרה
            Box(contentAlignment = Alignment.Center) {
                // המספר הגדול - ממוקם בדיוק במרכז הגיאומטרי
                Text(
                    text = "$currentSteps",
                    fontSize = 64.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDark) Color.White else Color.Black
                )

                // אייקון הנעל - ממוקם מעל המספר
                Icon(
                    painter = painterResource(id = R.drawable.ic_sneaker),
                    contentDescription = null,
                    tint = if (isDark) Color.White.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.7f),
                    modifier = Modifier
                        .size(32.dp)
                        .offset(y = (-55).dp)
                )

                // שורת מטרה - ממוקמת מתחת למספר, הדגל משמאל
                Row(
                    modifier = Modifier.offset(y = 50.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (isAllGoalsComplete) "כל המטרות הושגו!" else "מטרה: $stepGoal",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isAllGoalsComplete) emeraldColor else Color.Gray
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = null,
                        tint = if (isAllGoalsComplete) emeraldColor else Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun StepRing(
    currentSteps: Int,
    currentGoal: Int,
    isDark: Boolean,
    isAllGoalsComplete: Boolean,
    emeraldColor: Color
) {
    val railColor = if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.1f)

    // חישוב התקדמות יחסית למטרה הנוכחית
    val progress = if (currentGoal > 0) (currentSteps.toFloat() / currentGoal).coerceIn(0f, 1f) else 1f

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1500, easing = FastOutSlowInEasing),
        label = "ringProgress"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "glow")
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

        for (i in 0 until 80) {
            rotate(degrees = i * (360f / 80)) {
                drawLine(
                    color = railColor,
                    start = Offset(x = center.x, y = center.y - outerRailRadius),
                    end = Offset(x = center.x, y = center.y - outerRailRadius + 6.dp.toPx()),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }

        drawCircle(
            color = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f),
            radius = greenRadius,
            style = Stroke(width = greenThickness)
        )

        if (animatedProgress > 0) {
            drawArc(
                brush = Brush.sweepGradient(
                    0f to Color.Transparent,
                    animatedProgress to emeraldColor.copy(alpha = 0.3f),
                    center = center
                ),
                startAngle = -90f,
                sweepAngle = 360 * animatedProgress,
                useCenter = false,
                style = Stroke(width = greenThickness),
                topLeft = center.copy(x = center.x - greenRadius, y = center.y - greenRadius),
                size = Size(greenRadius * 2, greenRadius * 2)
            )

            drawArc(
                color = emeraldColor,
                startAngle = -90f,
                sweepAngle = 360 * animatedProgress,
                useCenter = false,
                style = Stroke(width = greenThickness, cap = StrokeCap.Round),
                topLeft = center.copy(x = center.x - greenRadius, y = center.y - greenRadius),
                size = Size(greenRadius * 2, greenRadius * 2)
            )
        }

        drawCircle(color = railColor, radius = outerRailRadius, style = Stroke(width = railThickness))
        drawCircle(color = railColor, radius = innerRailRadius, style = Stroke(width = railThickness))
    }
}