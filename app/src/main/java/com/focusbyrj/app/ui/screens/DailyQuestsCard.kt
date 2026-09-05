package com.focusbyrj.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.focusbyrj.app.ui.components.ChestRarity
import com.focusbyrj.app.ui.components.DuolingoMysteryChestDialog
import com.focusbyrj.app.util.DailyQuest
import com.focusbyrj.app.util.DailyQuestManager
import com.focusbyrj.app.util.MysteryReward

@Composable
fun DailyQuestsCard(
    onDismiss: (() -> Unit)? = null
) {
    val questState by DailyQuestManager.stateFlow.collectAsState()
    val isDark = isSystemInDarkTheme()
    var justClaimedReward by remember { mutableStateOf<MysteryReward?>(null) }
    var showChestDialog by remember { mutableStateOf(false) }

    val bgCard = if (isDark) Color(0xFF131F24) else Color(0xFFF8FAFC)
    val borderCol = if (questState.allCompleted) Color(0xFF1CB0F6) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)

    if (showChestDialog) {
        DuolingoMysteryChestDialog(
            initialRarity = ChestRarity.RARE,
            onDismiss = { showChestDialog = false },
            onClaimed = { reward ->
                justClaimedReward = reward
            }
        )
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = bgCard,
        border = BorderStroke(if (questState.allCompleted) 1.5.dp else 1.dp, borderCol),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            val completedCount = questState.quests.count { it.isCompleted }
            val totalQuests = questState.quests.size.coerceAtLeast(1)
            val progressFraction = (completedCount.toFloat() / totalQuests.toFloat()).coerceIn(0f, 1f)

            Text(
                text = "Daily Quest",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = if (isDark) Color.White else Color(0xFF0F172A)
            )
            
            Spacer(modifier = Modifier.height(10.dp))

            // Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(if (isDark) Color(0xFF20343D) else Color(0xFFE2E8F0)),
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progressFraction.coerceAtLeast(0.06f))
                        .clip(RoundedCornerShape(17.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF1CB0F6),
                                    Color(0xFF38BDF8)
                                )
                            )
                        )
                )
                Text(
                    text = "$completedCount / $totalQuests",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 13.5.sp
                    ),
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(12.dp))

            // Quest Rows
            questState.quests.forEach { quest ->
                DailyQuestItem(quest = quest, isDark = isDark)
                Spacer(modifier = Modifier.height(10.dp))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Mystery Boxes",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = if (isDark) Color.White else Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Morning Box
            MysteryBoxRow(
                title = "Morning Box",
                subtitle = "Available 6 AM - 6 PM",
                isAvailable = questState.isMorningAvailable,
                isClaimed = questState.morningChestClaimed,
                isDark = isDark,
                onClick = {
                    DailyQuestManager.markMorningChestClaimed()
                    showChestDialog = true
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Evening Box
            MysteryBoxRow(
                title = "Evening Box",
                subtitle = "Available 6 PM - 6 AM",
                isAvailable = questState.isEveningAvailable,
                isClaimed = questState.eveningChestClaimed,
                isDark = isDark,
                onClick = {
                    DailyQuestManager.markEveningChestClaimed()
                    showChestDialog = true
                }
            )

            AnimatedVisibility(visible = justClaimedReward != null) {
                justClaimedReward?.let { rew ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF4CAF50).copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, Color(0xFF4CAF50)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "🎉 Loot Unlocked: +${rew.xp} XP • +${rew.gold} Gems" +
                                    if (rew.streakFreezeAwarded) " • +1 🛡️ Streak Freeze!" else "!",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isDark) Color(0xFFA5D6A7) else Color(0xFF1B5E20),
                            modifier = Modifier.padding(8.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MysteryBoxRow(
    title: String,
    subtitle: String,
    isAvailable: Boolean,
    isClaimed: Boolean,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val bgColor = when {
        isClaimed -> if (isDark) Color(0xFF142416) else Color(0xFFE8F5E9)
        isAvailable -> if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
        else -> if (isDark) Color(0xFF131F24) else Color(0xFFF8FAFC)
    }
    
    val borderColor = when {
        isClaimed -> Color(0xFF4CAF50)
        isAvailable -> Color(0xFF1CB0F6)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    }
    
    val textColor = when {
        isClaimed -> if (isDark) Color(0xFFA5D6A7) else Color(0xFF2E7D32)
        else -> MaterialTheme.colorScheme.onSurface
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (isClaimed) "🎁" else if (isAvailable) "📦" else "🔒", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = textColor
                    )
                    Text(
                        text = if (isClaimed) "Claimed today" else subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (isAvailable && !isClaimed) {
                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1CB0F6)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("OPEN", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            } else if (isClaimed) {
                Text("✓", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DailyQuestItem(quest: DailyQuest, isDark: Boolean) {
    val animatedProgress by animateFloatAsState(
        targetValue = quest.progressFraction,
        animationSpec = tween(durationMillis = 600),
        label = "quest_progress"
    )

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isDark) Color(0x22FFFFFF) else Color(0x11000000),
        border = BorderStroke(1.dp, if (quest.isCompleted) Color(0xFF4CAF50).copy(alpha = 0.4f) else Color.Transparent),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (quest.isCompleted) Color(0xFF4CAF50).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(quest.icon, fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = quest.title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${quest.currentProgress}/${quest.target}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (quest.isCompleted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = quest.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedProgress.coerceAtLeast(0.001f))
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                brush = if (quest.isCompleted) Brush.linearGradient(listOf(Color(0xFF4CAF50), Color(0xFF4CAF50)))
                                else Brush.horizontalGradient(listOf(Color(0xFFFFB300), Color(0xFFFF7043)))
                            )
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFFFB300).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "+${quest.rewardXp} XP",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFFFB300),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                if (quest.isCompleted) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("✓", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}
