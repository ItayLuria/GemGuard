package com.gemguard.pages

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.DirectionsRun
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gemguard.GemViewModel

@Composable
fun Home(viewModel: GemViewModel) {
    val emeraldColor = Color(0xFF2ECC71)
    val isDark = viewModel.isDarkMode.value

    // הגדרת יעד הצעדים (ניתן לשנות לערך דינמי מה-ViewModel)
    val stepGoal = 10000
    val isAllGoalsComplete = viewModel.currentSteps.value >= stepGoal

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

        // מד הצעדים החדש (הטבעת)
        Box(contentAlignment = Alignment.Center) {
            StepRing(
                currentSteps = viewModel.currentSteps.value,
                currentGoal = stepGoal,
                isDark = isDark,
                isAllGoalsComplete = isAllGoalsComplete,
                emeraldColor = emeraldColor
            )

            // טקסט בתוך הטבעת
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.DirectionsRun,
                    null,
                    tint = if (isDark) Color.White else Color.Black,
                    modifier = Modifier.size(40.dp)
                )
                Text(
                    text = "${viewModel.currentSteps.value}",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDark) Color.White else Color.Black
                )
                Text(
                    text = "מתוך $stepGoal",
                    fontSize = 16.sp,
                    color = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // כרטיס Gems נקי (ללא רקע אפור)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 40.dp, start = 10.dp, end = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Diamond, null, tint = emeraldColor, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "היתרה שלך",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                "${viewModel.diamonds.value}",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = emeraldColor
            )
        }
    }
}

@Composable
fun StepRing(
    currentSteps: Int,
    currentGoal: Int,
    isDark: Boolean,
    isAllGoalsComplete: Boolean,
    emeraldColor: Color = Color(0xFF2ECC71)
) {
    val railColor = if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.1f)

    val progress = (currentSteps.toFloat() / currentGoal).coerceIn(0f, 1f)
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

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(300.dp)) {
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
}