package com.focusbyrj.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusbyrj.app.util.AptitudeManager
import com.focusbyrj.app.util.GamificationHaptics
import kotlin.math.roundToInt

@Composable
fun AptitudeProfileCard() {
    val profile by AptitudeManager.profileFlow.collectAsState()
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current
    
    val (cardBg, borderColor, titleColor, icon) = when (profile.titleTier) {
        6 -> listOf(
            if (isDark) Color(0xFF261B07) else Color(0xFFFFF8E1),
            Color(0xFFFFD54F),
            Color(0xFFFFB300),
            "👑"
        )
        5 -> listOf(
            if (isDark) Color(0xFF1E112A) else Color(0xFFF3E5F5),
            Color(0xFFBA68C8),
            Color(0xFF8E24AA),
            "🔮"
        )
        4 -> listOf(
            if (isDark) Color(0xFF0F1B2A) else Color(0xFFE3F2FD),
            Color(0xFF64B5F6),
            Color(0xFF1976D2),
            "⚡"
        )
        3 -> listOf(
            if (isDark) Color(0xFF1A1F24) else Color(0xFFECEFF1),
            Color(0xFF90A4AE),
            Color(0xFF546E7A),
            "⚔️"
        )
        2 -> listOf(
            if (isDark) Color(0xFF162319) else Color(0xFFE8F5E9),
            Color(0xFF81C784),
            Color(0xFF388E3C),
            "📜"
        )
        else -> listOf(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
            MaterialTheme.colorScheme.primary,
            "🔰"
        )
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = cardBg as Color,
        border = BorderStroke(if (profile.titleTier >= 4) 2.dp else 1.dp, borderColor as Color),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp), 
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            // Tier Badge Icon
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background((borderColor as Color).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = icon as String, fontSize = 40.sp)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = profile.title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = titleColor as Color,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "Level ${profile.level}",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = (titleColor as Color).copy(alpha = 0.8f)
            )
            
            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(color = (borderColor as Color).copy(alpha = 0.25f))
            Spacer(modifier = Modifier.height(16.dp))
            
            // Overall Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(), 
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ProfileStatBox("Total Drills", profile.totalDrills.toString(), MaterialTheme.colorScheme.onSurface)
                ProfileStatBox("Questions", profile.totalQuestions.toString(), MaterialTheme.colorScheme.onSurface)
                ProfileStatBox(
                    "Accuracy", 
                    "${profile.accuracy.roundToInt()}%", 
                    if (profile.accuracy >= 90f) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Weekly Aspirant League Section
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isDark) Color(0x33000000) else Color(0x33FFFFFF),
                border = BorderStroke(1.dp, (borderColor as Color).copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(profile.divisionIcon, fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = profile.divisionTitle,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Weekly Aspirant League",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFFFB300).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${profile.weeklyXp} Weekly XP",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFFFB300),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val divProgress = if (profile.divisionNextTierXp > 0) {
                        (profile.weeklyXp.toFloat() / profile.divisionNextTierXp.toFloat()).coerceIn(0f, 1f)
                    } else 1f

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(divProgress.coerceAtLeast(0.001f))
                                .clip(RoundedCornerShape(4.dp))
                                .background(Brush.horizontalGradient(listOf(Color(0xFFFFD54F), Color(0xFFFF9800))))
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (profile.weeklyXp < profile.divisionNextTierXp)
                            "${profile.divisionNextTierXp - profile.weeklyXp} XP until promotion"
                        else "Promotion zone secured! 🏆",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Streak & Daily Streak Bonus Section
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isDark) Color(0x33000000) else Color(0x33FFFFFF),
                border = BorderStroke(1.dp, (borderColor as Color).copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Current Streak
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🔥", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "${profile.currentStreak} Days",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (profile.currentStreak > 0) Color(0xFFFF7043) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Current Streak",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Longest Streak
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⚡", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${profile.longestStreak} Days",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFFFFB300)
                                )
                                Text(
                                    text = "Longest Streak",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 7-Day Streak Bonus Tracker
                    val streakDaysCapped = minOf(profile.currentStreak, 7)
                    val bonusPercent = profile.streakBonusPercent
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Daily Streak Bonus",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (bonusPercent > 0) Color(0xFFFF7043).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, if (bonusPercent > 0) Color(0xFFFF7043).copy(alpha = 0.5f) else Color.Transparent)
                        ) {
                            Text(
                                text = if (bonusPercent > 0) "+$bonusPercent% XP (Day $streakDaysCapped/7)" else "Day 0/7 (No Bonus)",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (bonusPercent > 0) Color(0xFFFF7043) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 7 Days Visual Progress Indicators
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (day in 1..7) {
                            val isAchieved = profile.currentStreak >= day
                            val isCurrentDay = profile.currentStreak == day || (day == 7 && profile.currentStreak >= 7)
                            val dayColor = if (isAchieved) Color(0xFFFF7043) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                            val textColor = if (isAchieved) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isAchieved) {
                                                Brush.radialGradient(listOf(Color(0xFFFF8A65), Color(0xFFE64A19)))
                                            } else {
                                                Brush.linearGradient(listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surfaceVariant))
                                            }
                                        )
                                        .border(
                                            width = if (isCurrentDay && isAchieved) 1.5.dp else 1.dp,
                                            color = if (isCurrentDay && isAchieved) Color(0xFFFFD54F) else dayColor,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isAchieved) "✓" else "$day",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "D$day",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isAchieved) Color(0xFFFF7043) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Streak Freeze Shield & Wager Section
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isDark) Color(0x33000000) else Color(0x33FFFFFF),
                border = BorderStroke(1.dp, (borderColor as Color).copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Streak Freeze Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🛡️", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "Streak Freeze (${profile.streakFreezesCount}/3)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Auto-protects streak if a day is missed",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (profile.streakFreezesCount < 3) {
                            Button(
                                onClick = {
                                    val success = AptitudeManager.buyStreakFreeze(1000)
                                    if (success) {
                                        GamificationHaptics.playCelebration(context)
                                        Toast.makeText(context, "🛡️ Streak Freeze Shield Equipped!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Need 1,000 Gold to equip shield", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1))
                            ) {
                                Text("Equip (1000 🪙)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Text("Max Capacity", fontSize = 11.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = (borderColor as Color).copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // 7-Day Wager Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("💰", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = if (profile.isWagerActive) "7-Day Wager (Day ${profile.wagerDaysCompleted}/7)" else "7-Day Streak Wager",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (profile.isWagerActive) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (profile.isWagerActive) "Win 100 Gold + 100 XP upon completion" else "Stake 50 🪙 to win 100 🪙 on 7-day streak",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (!profile.isWagerActive) {
                            Button(
                                onClick = {
                                    val success = AptitudeManager.startWager(50)
                                    if (success) {
                                        GamificationHaptics.playCelebration(context)
                                        Toast.makeText(context, "💰 7-Day Wager started! Practice 7 days in a row to win 100 Gold!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Need 50 Gold to start wager", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300), contentColor = Color.Black)
                            ) {
                                Text("Wager 50 🪙", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFFB300).copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "Active (${profile.wagerDaysCompleted}/7)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFB300),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(18.dp))
            
            // Progress Bar to Next Level
            val xpNeeded = profile.xpForNextLevel - profile.xp
            val range = (profile.xpForNextLevel - profile.xpForCurrentLevel).toFloat().takeIf { it > 0f } ?: 1f
            val progress = ((profile.xp - profile.xpForCurrentLevel).toFloat() / range).coerceIn(0f, 1f)
            
            Row(
                modifier = Modifier.fillMaxWidth(), 
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${profile.xp} XP",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (xpNeeded > 0) "$xpNeeded XP to next level" else "Level Up!",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress.coerceAtLeast(0.001f))
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (profile.titleTier >= 5) Brush.horizontalGradient(listOf(Color(0xFF8E24AA), Color(0xFFD81B60)))
                            else Brush.horizontalGradient(listOf((titleColor as Color).copy(alpha = 0.5f), titleColor as Color))
                        )
                )
            }
        }
    }
}

@Composable
fun ProfileStatBox(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = color)
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
