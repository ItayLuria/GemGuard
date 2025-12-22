package com.gemguard.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
// שים לב לייבוא המפורש כאן - זה מה שפותר את הבעיה בדרך כלל
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gemguard.GemViewModel

@Composable
fun Home(viewModel: GemViewModel) {
    val emeraldColor = Color(0xFF2ECC71)
    val darkEmerald = Color(0xFF1B5E20)
    val lightGreenBg = Color(0xFFF1F8E9)

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
            color = darkEmerald
        )
        Text(
            text = "הצעדים שלך שווים Gems",
            fontSize = 14.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(30.dp))

        // כרטיס צעדים
        Card(
            modifier = Modifier.fillMaxWidth().height(220.dp),
            shape = RoundedCornerShape(32.dp),
            elevation = CardDefaults.cardElevation(12.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(colors = listOf(emeraldColor, Color(0xFF27AE60)))
                ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.DirectionsRun,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${viewModel.currentSteps.value}",
                        fontSize = 58.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text("צעדים היום", fontSize = 18.sp, color = Color.White.copy(alpha = 0.9f))
                }
            }
        }

        Spacer(modifier = Modifier.height(25.dp))

        // כרטיס Gems (אמרלד)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = lightGreenBg)
        ) {
            Row(
                modifier = Modifier.padding(20.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Diamond,
                        contentDescription = null,
                        tint = emeraldColor,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Gems",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = darkEmerald
                    )
                }
                Text(
                    text = "${viewModel.diamonds.value}",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = emeraldColor
                )
            }
        }
    }
}