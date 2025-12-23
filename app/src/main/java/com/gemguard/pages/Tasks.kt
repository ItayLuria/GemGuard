package com.gemguard.pages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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

data class Task(val id: Int, val title: String, val target: Int, val reward: Int)

@Composable
fun TasksScreen(viewModel: GemViewModel) {
    val context = LocalContext.current
    val emeraldColor = Color(0xFF2ECC71)

    val tasks = listOf(
        Task(1, "הליכת בוקר (1,000 צעדים)", 1000, 50),
        Task(2, "מתקדמים (5,000 צעדים)", 5000, 150),
        Task(3, "אלוף היום (10,000 צעדים)", 10000, 400)
    )

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("משימות", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = emeraldColor)
        Text("השלם משימות וקבל Gems", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(modifier = Modifier.height(20.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(tasks) { task ->
                val progress = (viewModel.currentSteps.value.toFloat() / task.target).coerceIn(0f, 1f)
                val isCompleted = progress >= 1f
                val isClaimed = viewModel.claimedTaskIds.contains(task.id)

                // הסרת הרקע והמסגרת לחלוטין - מראה נקי על גבי הרקע
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(task.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${task.reward}", fontWeight = FontWeight.Bold, color = emeraldColor)
                            Icon(Icons.Default.Diamond, null, tint = emeraldColor, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = emeraldColor,
                        trackColor = emeraldColor.copy(alpha = 0.2f),
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (isClaimed) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = emeraldColor, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("הושלם", color = emeraldColor, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = { viewModel.addDiamonds(task.reward, task.id, context) },
                            enabled = isCompleted,
                            modifier = Modifier.align(Alignment.End),
                            colors = ButtonDefaults.buttonColors(containerColor = emeraldColor)
                        ) {
                            Text("אסוף Gems")
                        }
                    }

                    // קו מפריד דק בין המשימות למראה מסודר יותר
                    HorizontalDivider(
                        modifier = Modifier.padding(top = 16.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}