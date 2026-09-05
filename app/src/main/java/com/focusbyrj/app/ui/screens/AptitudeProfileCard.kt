package com.focusbyrj.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusbyrj.app.util.AptitudeManager
import com.focusbyrj.app.util.GamificationHaptics
import com.focusbyrj.app.ui.components.DuolingoBoltIcon
import kotlin.math.roundToInt

@Composable
fun AptitudeProfileCard() {
    val profile by AptitudeManager.profileFlow.collectAsState()
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current
    
    // Duolingo Authentic Colors
    val cardBg = if (isDark) Color(0xFF131F24) else Color(0xFFFFFFFF)
    val borderColor = if (isDark) Color(0xFF28414D) else Color(0xFFE5E5E5)
    val textColor = if (isDark) Color.White else Color(0xFF4B4B4B)
    val mutedTextColor = if (isDark) Color(0xFF8699A6) else Color(0xFFAFAFAF)
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Stats Row
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), 
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ProfileStatItem("Total Drills", profile.totalDrills.toString(), textColor, mutedTextColor)
            ProfileStatItem("Questions", profile.totalQuestions.toString(), textColor, mutedTextColor)
            ProfileStatItem("Accuracy", "${profile.accuracy.roundToInt()}%", textColor, mutedTextColor)
        }

        // League Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = cardBg,
            border = BorderStroke(2.dp, borderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(profile.divisionIcon, fontSize = 28.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = profile.divisionTitle,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontSize = 18.sp),
                                color = textColor
                            )
                            Text(
                                text = "Weekly Aspirant League",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = mutedTextColor
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFFC800).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "${profile.weeklyXp} Weekly XP",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                            color = Color(0xFFFFC800),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                val divProgress = if (profile.divisionNextTierXp > 0) {
                    (profile.weeklyXp.toFloat() / profile.divisionNextTierXp.toFloat()).coerceIn(0f, 1f)
                } else 1f
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(if (isDark) Color(0xFF28414D) else Color(0xFFE5E5E5))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(divProgress.coerceAtLeast(0.001f))
                            .clip(RoundedCornerShape(7.dp))
                            .background(Color(0xFFFFC800))
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (profile.weeklyXp < profile.divisionNextTierXp)
                        "${profile.divisionNextTierXp - profile.weeklyXp} XP until promotion"
                    else "Promotion zone secured! 🏆",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = mutedTextColor
                )
            }
        }

        // Streak Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = cardBg,
            border = BorderStroke(2.dp, borderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp, horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Row: Current vs Longest
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Current Streak
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "🔥",
                                fontSize = 24.sp,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Text(
                                text = "${profile.currentStreak.coerceAtLeast(1)} Days",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 20.sp
                                ),
                                color = Color(0xFFFF9600)
                            )
                        }
                        Text(
                            text = "Current Streak",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                            color = mutedTextColor,
                            modifier = Modifier.padding(start = 28.dp)
                        )
                    }
                    
                    // Longest Streak
                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            DuolingoBoltIcon(
                                modifier = Modifier.size(20.dp).padding(end = 4.dp),
                                color = Color(0xFFFFC800)
                            )
                            Text(
                                text = "${Math.max(profile.currentStreak, profile.longestStreak)} Days",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 20.sp
                                ),
                                color = Color(0xFFFFC800)
                            )
                        }
                        Text(
                            text = "Longest Streak",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                            color = mutedTextColor,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                }

                // Middle Row: Daily Streak Bonus
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Daily Streak Bonus",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp
                        ),
                        color = textColor
                    )
                    
                    val currentCycleDay = if (profile.currentStreak == 0) 1 else ((profile.currentStreak - 1) % 7) + 1
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFF9600).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "+${profile.streakBonusPercent.coerceAtLeast(5)}% XP (Day $currentCycleDay/7)",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                            color = Color(0xFFFF9600),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                
                // Bottom Row: 7 circles
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val currentCycleDay = if (profile.currentStreak == 0) 1 else ((profile.currentStreak - 1) % 7) + 1
                    for (i in 1..7) {
                        val isActive = i <= currentCycleDay
                        val circleColor = if (isActive) Color(0xFFFF9600) else borderColor
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, circleColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isActive) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFF9600)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = "Done",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else {
                                    Text(
                                        text = i.toString(),
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                        color = mutedTextColor
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "D$i",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                color = if (isActive) Color(0xFFFF9600) else mutedTextColor
                            )
                        }
                    }
                }
            }
        }

        // Shop Items (Streak Freeze and Wager)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = cardBg,
            border = BorderStroke(2.dp, borderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Streak Freeze Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Text("🛡️", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Streak Freeze (${profile.streakFreezesCount}/3)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, fontSize = 15.sp),
                                color = textColor
                            )
                            Text(
                                text = "Auto-protects streak if missed",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = mutedTextColor
                            )
                        }
                    }
                    if (profile.streakFreezesCount < 3) {
                        Button(
                            onClick = {
                                val success = AptitudeManager.buyStreakFreeze(1000)
                                if (success) {
                                    GamificationHaptics.playCelebration(context)
                                    Toast.makeText(context, "🛡️ Streak Freeze Equipped!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Need 1,000 Gold to equip shield", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1CB0F6))
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Equip", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White)
                                Text("(1000 🪙)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.9f))
                            }
                        }
                    } else {
                        Text("Equipped", fontSize = 12.sp, color = Color(0xFF58CC02), fontWeight = FontWeight.Black, modifier = Modifier.padding(end = 8.dp))
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = borderColor, thickness = 2.dp)
                Spacer(modifier = Modifier.height(16.dp))
                
                // 7-Day Wager Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Text("💰", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (profile.isWagerActive) "Wager Active (${profile.wagerDaysCompleted}/7)" else "7-Day Streak Wager",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, fontSize = 15.sp),
                                color = textColor
                            )
                            Text(
                                text = if (profile.isWagerActive) "Maintain your streak to win 100 🪙" else "Stake 50 🪙 to double it in 7 days",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = mutedTextColor
                            )
                        }
                    }
                    if (!profile.isWagerActive) {
                        Button(
                            onClick = {
                                val success = AptitudeManager.startWager(50)
                                if (success) {
                                    GamificationHaptics.playCelebration(context)
                                    Toast.makeText(context, "💰 Wager started! Maintain a 7-day streak to double it!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Need 50 Gold to start wager", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC800))
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Wager", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFF2B1D00))
                                Text("50 🪙", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2B1D00).copy(alpha = 0.9f))
                            }
                        }
                    } else {
                        Text("Active", fontSize = 12.sp, color = Color(0xFFFFC800), fontWeight = FontWeight.Black, modifier = Modifier.padding(end = 8.dp))
                    }
                }
            }
        }

        // Bottom XP Bar
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            val xpNeeded = profile.xpForNextLevel - profile.xp
            val range = (profile.xpForNextLevel - profile.xpForCurrentLevel).toFloat().takeIf { it > 0f } ?: 1f
            val progress = ((profile.xp - profile.xpForCurrentLevel).toFloat() / range).coerceIn(0f, 1f)
            
            Row(
                modifier = Modifier.fillMaxWidth(), 
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${profile.xp} XP",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                    color = textColor
                )
                Text(
                    text = if (xpNeeded > 0) "$xpNeeded XP to next level" else "Level Up Ready!",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                    color = textColor
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(borderColor)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress.coerceAtLeast(0.001f))
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF58CC02))
                )
                // Highlight line for 3D effect
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp, start = 8.dp, end = 8.dp)
                        .fillMaxWidth(progress.coerceAtLeast(0.001f))
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.3f))
                )
            }
        }
    }
}

@Composable
fun ProfileStatItem(label: String, value: String, textColor: Color, mutedTextColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, fontSize = 24.sp), color = textColor)
        Text(text = label, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = mutedTextColor)
    }
}
