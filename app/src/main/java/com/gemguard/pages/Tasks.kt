package com.gemguard.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext // ייבוא חובה
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gemguard.GemViewModel

data class Task(val id: Int, val title: String, val description: String, val requiredSteps: Int, val reward: Int)

@Composable
fun TasksScreen(viewModel: GemViewModel) {
    val context = LocalContext.current // קבלת ה-Context
    val currentSteps = viewModel.currentSteps.value
    val claimedTasks = viewModel.claimedTaskIds

    val emeraldColor = Color(0xFF2ECC71)
    val darkEmerald = Color(0xFF1B5E20)

    val tasks = listOf(
        Task(1, "הליכת בוקר", "עשה 1,000 צעדים", 1000, 100),
        Task(2, "מתקדמים", "עשה 5,000 צעדים", 5000, 500),
        Task(3, "אלוף היום", "עשה 10,000 צעדים", 10000, 1200)
    )

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text(
            text = "משימות יומיות",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = darkEmerald
        )
        Text(
            text = "השלם משימות כדי להרוויח Gems",
            fontSize = 14.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(tasks) { task ->
                val isCompleted = currentSteps >= task.requiredSteps
                val isClaimed = claimedTasks.contains(task.id)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isClaimed) Color(0xFFF5F5F5) else Color.White
                    ),
                    border = if (isCompleted && !isClaimed)
                        androidx.compose.foundation.BorderStroke(2.dp, emeraldColor)
                    else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    if (isCompleted) emeraldColor.copy(alpha = 0.1f) else Color(0xFFF5F5F5),
                                    RoundedCornerShape(10.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isClaimed) Icons.Default.CheckCircle else Icons.Default.DirectionsWalk,
                                contentDescription = null,
                                tint = if (isCompleted) emeraldColor else Color.Gray
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = task.title,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isClaimed) Color.Gray else darkEmerald
                            )
                            if (!isClaimed) {
                                val progress = (currentSteps.toFloat() / task.requiredSteps).coerceIn(0f, 1f)
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth(0.6f).height(4.dp),
                                    color = emeraldColor,
                                    trackColor = emeraldColor.copy(alpha = 0.1f)
                                )
                                Text("$currentSteps / ${task.requiredSteps} צעדים", fontSize = 11.sp, color = Color.Gray)
                            } else {
                                Text("המשימה הושלמה!", fontSize = 12.sp, color = emeraldColor)
                            }
                        }

                        // התיקון כאן: העברת context ל-addDiamonds
                        Button(
                            enabled = isCompleted && !isClaimed,
                            onClick = { viewModel.addDiamonds(task.reward, task.id, context) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = emeraldColor,
                                disabledContainerColor = if (isClaimed) Color(0xFFE0E0E0) else emeraldColor.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (!isClaimed) {
                                    Icon(Icons.Default.Diamond, null, modifier = Modifier.size(16.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = if (isClaimed) "נאסף" else "${task.reward}",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}