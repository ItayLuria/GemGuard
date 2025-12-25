package com.gemguard.pages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gemguard.GemViewModel

@Composable
fun TasksScreen(viewModel: GemViewModel) {
    val context = LocalContext.current
    val emeraldColor = Color(0xFF2ECC71)
    val isHebrew = viewModel.language.value == "iw"
    val tasks = viewModel.tasks

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        // כותרת המסך
        Text(
            text = if (isHebrew) "משימות יומיות" else "Daily Tasks",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = emeraldColor
        )
        Text(
            text = if (isHebrew) "השלם משימות וקבל Gems" else "Complete tasks to earn Gems",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(tasks) { task ->
                val progress = (viewModel.currentSteps.value.toFloat() / task.requiredSteps).coerceIn(0f, 1f)
                val isCompleted = progress >= 1f
                val isClaimed = viewModel.claimedTaskIds.contains(task.id)

                val taskDisplayName = if (isHebrew) task.nameHe else task.nameEn
                val stepsSuffix = if (isHebrew) "צעדים" else "steps"

                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {

                    // --- שורה מעל ה-Progress Bar: מספר צעדים ויהלומים ---
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

                    // --- מד התקדמות (Progress Bar) ---
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(10.dp),
                        color = emeraldColor,
                        trackColor = emeraldColor.copy(alpha = 0.2f),
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // --- שורה מתחת ל-Progress Bar: שם המשימה וכפתור איסוף ---
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
}