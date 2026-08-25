package com.focusbyrj.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.focusbyrj.app.ui.theme.SurfaceDark

import com.focusbyrj.app.util.HeatmapTheme
import com.focusbyrj.app.util.UserProfile
import java.util.Calendar

@Composable
fun HeatmapAndStreaksWidget(
    dailyUsage: Map<Int, Long>,
    theme: HeatmapTheme,
    profile: UserProfile
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceDark)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // LEFT HALF: Heatmap
            Column(modifier = Modifier.weight(1f)) {
                Text("30 Days", style = MaterialTheme.typography.titleMedium, color = Color(0xFFCBD5E1))
                Spacer(modifier = Modifier.height(12.dp))
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    for (row in 0..4) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            for (col in 0..5) {
                                val daysAgo = 29 - (row * 6 + col)
                                val usage = dailyUsage[Calendar.getInstance().get(Calendar.DAY_OF_YEAR) - daysAgo] ?: 0L
                                
                                val color = when {
                                    usage == 0L -> Color(0xFF1A1B26)
                                    usage < 30 * 60 * 1000L -> theme.colors[0]
                                    usage < 60 * 60 * 1000L -> theme.colors[1]
                                    usage < 120 * 60 * 1000L -> theme.colors[2]
                                    else -> theme.colors[3]
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(color)
                                )
                            }
                        }
                    }
                }
            }
            
            // RIGHT HALF: Streaks
            Column(modifier = Modifier.weight(1f)) {
                Text("Streaks", style = MaterialTheme.typography.titleMedium, color = Color(0xFFCBD5E1))
                Spacer(modifier = Modifier.height(12.dp))
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1A1B26))
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${profile.currentStreak}", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                            Text("Current Streak", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                    }
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1A1B26))
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${profile.longestStreak}", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF6366F1))
                            Text("Longest Streak", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}
