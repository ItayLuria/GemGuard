package com.gemguard.pages

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gemguard.GemViewModel
import com.gemguard.TimeMission
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TasksScreen(viewModel: GemViewModel) {
    val context = LocalContext.current
    val emeraldColor = Color(0xFF2ECC71)
    val isHebrew = viewModel.language.value == "iw"
    val tasks = viewModel.tasks
    val timeMission by viewModel.timeMission

    LaunchedEffect(Unit) {
        while (true) {
            viewModel.checkTimeMission(context)
            delay(1000)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = if (isHebrew) "משימות" else "Tasks",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = emeraldColor
            )
            Text(
                text = if (isHebrew) "השלם משימות וקבל Gems" else "Complete tasks to earn Gems",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        timeMission?.let {
            item(key = "time_mission_card") {
                Box(modifier = Modifier.animateItem()) {
                    TimeMissionCard(mission = it, viewModel = viewModel)
                }
            }
        }

        item(key = "daily_tasks_title") {
            Text(
                text = if (isHebrew) "משימות יומיות" else "Daily Tasks",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(top = 16.dp, bottom = 8.dp)
                    .animateItem()
            )
        }

        items(tasks, key = { it.id }) { task ->
            val progress = (viewModel.currentSteps.value.toFloat() / task.requiredSteps).coerceIn(0f, 1f)
            val isCompleted = progress >= 1f
            val isClaimed = viewModel.claimedTaskIds.contains(task.id)

            val taskDisplayName = if (isHebrew) task.nameHe else task.nameEn
            val stepsSuffix = if (isHebrew) "צעדים" else "steps"

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .animateItem()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "${String.format("%,d", task.requiredSteps)} $stepsSuffix",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "${task.reward}", fontWeight = FontWeight.Bold, color = emeraldColor, fontSize = 16.sp)
                        Icon(Icons.Default.Diamond, null, tint = emeraldColor, modifier = Modifier.size(18.dp).padding(start = 4.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(10.dp),
                    color = emeraldColor,
                    trackColor = emeraldColor.copy(alpha = 0.2f),
                    strokeCap = StrokeCap.Round
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        taskDisplayName,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f) // נותן לטקסט מקום כדי שלא ידחוף את הכפתור
                    )

                    if (isClaimed) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = emeraldColor, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isHebrew) "נאסף" else "Claimed", color = emeraldColor, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = { viewModel.addDiamonds(task.reward, task.id) },
                            enabled = isCompleted,
                            // הוספת גובה ושינוי צורה למניעת חיתוך האות 'ף'
                            modifier = Modifier
                                .height(40.dp)
                                .widthIn(min = 80.dp),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = emeraldColor,
                                disabledContainerColor = emeraldColor.copy(alpha = 0.3f)
                            )
                        ) {
                            Text(
                                text = if (isHebrew) "איסוף" else "Claim",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(top = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun TimeMissionCard(mission: TimeMission, viewModel: GemViewModel) {
    val emeraldColor = Color(0xFF2ECC71)
    val context = LocalContext.current
    val isHebrew = viewModel.language.value == "iw"
    val cardBackgroundColor = emeraldColor.copy(alpha = 0.12f)

    var isClaimedByMe by remember { mutableStateOf(false) }

    var remainingMillis by remember(mission.endTime) {
        mutableLongStateOf((mission.endTime - System.currentTimeMillis()).coerceAtLeast(0))
    }

    LaunchedEffect(mission.endTime) {
        while (remainingMillis > 0 && !isClaimedByMe) {
            remainingMillis = (mission.endTime - System.currentTimeMillis()).coerceAtLeast(0)
            delay(1000)
        }
        if (!isClaimedByMe) viewModel.checkTimeMission(context)
    }

    LaunchedEffect(isClaimedByMe) {
        if (isClaimedByMe) {
            delay(3000)
            viewModel.claimTimeMissionReward(context)
        }
    }

    val progress = (mission.stepsProgress.toFloat() / mission.stepsGoal).coerceIn(0f, 1f)
    val isCompleted = progress >= 1f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
        border = BorderStroke(1.dp, emeraldColor)
    ) {
        AnimatedContent(
            targetState = isClaimedByMe,
            transitionSpec = {
                fadeIn(animationSpec = tween(500)) with fadeOut(animationSpec = tween(500))
            },
            label = "claim_transition"
        ) { success ->
            if (success) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.CheckCircle, null, tint = emeraldColor, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isHebrew) "משימה הושלמה!" else "Mission Completed!",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = emeraldColor
                    )
                }
            } else {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(emeraldColor.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Timer, null, tint = emeraldColor, modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(if (isHebrew) "משימת זמן!" else "Time Mission!", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                if (remainingMillis > 0) {
                                    val minutes = (remainingMillis / 1000) / 60
                                    val seconds = (remainingMillis / 1000) % 60
                                    Text(
                                        text = String.format(if (isHebrew) "%02d:%02d נותרו" else "%02d:%02d left", minutes, seconds),
                                        color = emeraldColor, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp
                                    )
                                }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "${mission.reward}", fontWeight = FontWeight.Bold, color = emeraldColor, fontSize = 18.sp)
                            Icon(Icons.Default.Diamond, null, tint = emeraldColor, modifier = Modifier.size(20.dp).padding(start = 4.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isHebrew) "יעד: ${mission.stepsGoal} צעדים" else "Goal: ${mission.stepsGoal} steps", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = "${mission.stepsProgress}/${mission.stepsGoal}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = emeraldColor)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                            color = emeraldColor,
                            trackColor = emeraldColor.copy(alpha = 0.1f),
                            strokeCap = StrokeCap.Round
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { isClaimedByMe = true },
                        enabled = isCompleted,
                        modifier = Modifier.fillMaxWidth().height(48.dp), // הגדלתי גם פה ל-48 לנוחות מקסימלית
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = emeraldColor, disabledContainerColor = emeraldColor.copy(alpha = 0.3f))
                    ) {
                        Text(if (isHebrew) "איסוף Gems" else "Claim Gems", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}