package com.gemguard.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gemguard.GemViewModel
import com.gemguard.R
import com.gemguard.TimeMission
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

@Composable
fun TasksScreen(viewModel: GemViewModel) {
    val context = LocalContext.current
    val emeraldColor = Color(0xFF2ECC71)
    val isHebrew = viewModel.language.value == "iw"
    val tasks = viewModel.tasks
    val timeMission by viewModel.timeMission

    // Re-check mission status periodically
    LaunchedEffect(Unit) {
        while (true) {
            viewModel.checkTimeMission(context)
            delay(1000) // Check every second
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Screen Title ---
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

        // --- Time Mission ---
        timeMission?.let {
            item {
                TimeMissionCard(mission = it, viewModel = viewModel)
            }
        }

        // --- Daily Tasks Title ---
        item {
            Text(
                text = if (isHebrew) "משימות יומיות" else "Daily Tasks",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
        }

        // --- Daily Tasks List ---
        items(tasks) { task ->
            val progress = (viewModel.currentSteps.value.toFloat() / task.requiredSteps).coerceIn(0f, 1f)
            val isCompleted = progress >= 1f
            val isClaimed = viewModel.claimedTaskIds.contains(task.id)

            val taskDisplayName = if (isHebrew) task.nameHe else task.nameEn
            val stepsSuffix = if (isHebrew) "צעדים" else "steps"

            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
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
                        Text(
                            text = "${task.reward}",
                            fontWeight = FontWeight.Bold,
                            color = emeraldColor,
                            fontSize = 16.sp
                        )
                        Icon(
                            imageVector = Icons.Default.Diamond,
                            contentDescription = null,
                            tint = emeraldColor,
                            modifier = Modifier.size(18.dp).padding(start = 4.dp)
                        )
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
                        text = taskDisplayName,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (isClaimed) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = emeraldColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isHebrew) "נאסף" else "Claimed",
                                color = emeraldColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Button(
                            onClick = { viewModel.addDiamonds(task.reward, task.id) },
                            enabled = isCompleted,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                            modifier = Modifier.height(34.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = emeraldColor,
                                disabledContainerColor = emeraldColor.copy(alpha = 0.3f)
                            )
                        ) {
                            Text(
                                text = if (isHebrew) "איסוף" else "Claim",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.padding(top = 16.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
fun TimeMissionCard(mission: TimeMission, viewModel: GemViewModel) {
    val emeraldColor = Color(0xFF2ECC71)
    val context = LocalContext.current

    var remainingTime by remember { mutableStateOf("") }

    LaunchedEffect(mission.endTime) {
        while (System.currentTimeMillis() < mission.endTime) {
            val remaining = mission.endTime - System.currentTimeMillis()
            if (remaining > 0) {
                val minutes = TimeUnit.MILLISECONDS.toMinutes(remaining)
                val seconds = TimeUnit.MILLISECONDS.toSeconds(remaining) % 60
                remainingTime = String.format("%02d:%02d", minutes, seconds)
            }
            delay(1000)
        }
        remainingTime = "00:00"
        viewModel.checkTimeMission(context)
    }

    val progress = (mission.stepsProgress.toFloat() / mission.stepsGoal).coerceIn(0f, 1f)
    val isCompleted = progress >= 1f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.time_mission_category_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = emeraldColor
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.background(emeraldColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Timer, contentDescription = null, tint = emeraldColor, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = remainingTime, fontWeight = FontWeight.Bold, color = emeraldColor, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress Bar and Text
            Box(contentAlignment = Alignment.Center) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(25.dp).clip(RoundedCornerShape(12.dp)),
                    color = emeraldColor,
                    trackColor = MaterialTheme.colorScheme.surface,
                    strokeCap = StrokeCap.Round
                )
                Text(
                    text = stringResource(id = R.string.time_mission_steps_format, mission.stepsProgress, mission.stepsGoal),
                    color = if (viewModel.isDarkMode.value) Color.Black else Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "+",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Light,
                        color = emeraldColor
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = mission.reward.toString(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = emeraldColor
                    )
                    Icon(
                        imageVector = Icons.Default.Diamond,
                        contentDescription = null,
                        tint = emeraldColor,
                        modifier = Modifier.size(22.dp).padding(start = 4.dp)
                    )
                }

                Button(
                    onClick = { viewModel.claimTimeMissionReward(context) },
                    enabled = isCompleted,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = emeraldColor, disabledContainerColor = emeraldColor.copy(alpha = 0.4f))
                ) {
                    Text(stringResource(id = R.string.time_mission_claim_button))
                }
            }
        }
    }
}