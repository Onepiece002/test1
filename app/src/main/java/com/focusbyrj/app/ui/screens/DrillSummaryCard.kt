package com.focusbyrj.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject
import kotlin.math.roundToInt

@Composable
fun DrillSummaryCard(message: ChatMessage, fontSizeSp: Float) {
    val json = message.drillSummaryJson ?: return
    
    var title by remember { mutableStateOf("") }
    var tier by remember { mutableStateOf(1) }
    var level by remember { mutableStateOf(1) }
    var xpEarned by remember { mutableStateOf(0) }
    var goldEarned by remember { mutableStateOf(0) }
    var totalQuestions by remember { mutableStateOf(0) }
    var correctQuestions by remember { mutableStateOf(0) }
    var xpBefore by remember { mutableStateOf(0) }
    var xpNow by remember { mutableStateOf(0) }
    var xpCurrentLevelStart by remember { mutableStateOf(0) }
    var xpNextLevelStart by remember { mutableStateOf(0) }
    var isPerfect by remember { mutableStateOf(false) }
    var currentStreak by remember { mutableStateOf(0) }
    var longestStreak by remember { mutableStateOf(0) }
    var streakBonusPercent by remember { mutableStateOf(0) }
    var streakBonusXp by remember { mutableStateOf(0) }

    LaunchedEffect(json) {
        try {
            val obj = JSONObject(json)
            title = obj.getString("title")
            tier = obj.getInt("tier")
            level = obj.getInt("level")
            xpEarned = obj.getInt("xpEarned")
            goldEarned = obj.optInt("goldEarned", 0)
            totalQuestions = obj.getInt("total")
            correctQuestions = obj.getInt("correct")
            xpBefore = obj.getInt("xpBefore")
            xpNow = obj.getInt("xpNow")
            xpCurrentLevelStart = obj.getInt("xpCurrentLevelStart")
            xpNextLevelStart = obj.getInt("xpNextLevelStart")
            isPerfect = obj.optBoolean("isPerfect", false)
            currentStreak = obj.optInt("currentStreak", 0)
            longestStreak = obj.optInt("longestStreak", 0)
            streakBonusPercent = obj.optInt("streakBonusPercent", 0)
            streakBonusXp = obj.optInt("streakBonusXp", 0)
        } catch (_: Exception) {}
    }

    val isDark = isSystemInDarkTheme()
    
    // Style based on tier
    val (cardBg, borderColor, titleColor, icon) = when (tier) {
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

    // Animation for progress bar
    var progressStarted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(300)
        progressStarted = true
    }

    val range = (xpNextLevelStart - xpCurrentLevelStart).toFloat().takeIf { it > 0f } ?: 1f
    val initialProgress = ((xpBefore - xpCurrentLevelStart).toFloat() / range).coerceIn(0f, 1f)
    val finalProgress = ((xpNow - xpCurrentLevelStart).toFloat() / range).coerceIn(0f, 1f)

    val animatedProgress by animateFloatAsState(
        targetValue = if (progressStarted) finalProgress else initialProgress,
        animationSpec = tween(durationMillis = 1500),
        label = "xp_progress"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardBg as Color,
        border = BorderStroke(if (tier >= 4) 2.dp else 1.dp, borderColor as Color),
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(icon as String, fontSize = 24.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = titleColor as Color
                    )
                    Text(
                        text = "Level $level",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = (titleColor as Color).copy(alpha = 0.8f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = (borderColor as Color).copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(16.dp))
            
            // Stats Grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatBox(label = "Questions", value = totalQuestions.toString(), color = MaterialTheme.colorScheme.onSurface)
                StatBox(
                    label = "Accuracy", 
                    value = if (totalQuestions > 0) "${((correctQuestions.toFloat() / totalQuestions) * 100).roundToInt()}%" else "0%",
                    color = if (isPerfect) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface
                )
                StatBox(
                    label = "Gold", 
                    value = "+$goldEarned", 
                    color = Color(0xFFFFB300)
                )
                StatBox(
                    label = "XP", 
                    value = "+$xpEarned", 
                    color = titleColor as Color
                )
            }
            
            if (isPerfect && totalQuestions >= 10) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFD54F).copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🏅 Perfect Run! 2x Base XP Applied", color = Color(0xFFF57F17), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            if (currentStreak > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFF7043).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🔥 Streak Day $currentStreak! +$streakBonusPercent% XP Bonus (+$streakBonusXp XP)",
                        color = Color(0xFFE64A19),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Progress Bar
            val xpNeeded = xpNextLevelStart - xpNow
            Text(
                text = if (xpNeeded > 0) "$xpNeeded XP to next level" else "Level Up!",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End
            )
            Spacer(modifier = Modifier.height(4.dp))
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
                        .fillMaxWidth(animatedProgress.coerceAtLeast(0.001f))
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (tier >= 5) Brush.horizontalGradient(listOf(Color(0xFF8E24AA), Color(0xFFD81B60)))
                            else Brush.horizontalGradient(listOf((titleColor as Color).copy(alpha = 0.5f), titleColor as Color))
                        )
                )
            }
        }
    }
}

@Composable
private fun StatBox(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = color)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

