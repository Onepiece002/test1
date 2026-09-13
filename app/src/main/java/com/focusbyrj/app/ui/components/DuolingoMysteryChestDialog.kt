package com.focusbyrj.app.ui.components
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.focusbyrj.app.util.AptitudeManager
import com.focusbyrj.app.util.DailyQuestManager
import com.focusbyrj.app.util.FocusEconomyManager
import com.focusbyrj.app.util.GamificationHaptics
import com.focusbyrj.app.util.MysteryReward
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext

@Composable
fun DuolingoMysteryChestDialog(
    initialRarity: ChestRarity = ChestRarity.COMMON,
    onDismiss: () -> Unit,
    onClaimed: ((MysteryReward) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var isRevealed by remember { mutableStateOf(false) }
    var isOpening by remember { mutableStateOf(false) }
    var claimedReward by remember { mutableStateOf<MysteryReward?>(null) }
    
    val boxScale = remember { Animatable(1f) }
    val glowAlpha = remember { Animatable(0.5f) }
    
    LaunchedEffect(Unit) {
        glowAlpha.animateTo(
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
    }

    fun openBox() {
        if (isOpening || isRevealed) return
        isOpening = true
        coroutineScope.launch {
            GamificationHaptics.playLight(context)
            
            // Anticipation shrink
            boxScale.animateTo(0.85f, animationSpec = tween(200, easing = FastOutLinearInEasing))
            
            // Burst open
            boxScale.animateTo(1.1f, animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium))
            GamificationHaptics.playCelebration(context)
            
            // Calculate rewards
            val currentTotalXp = maxOf(0, AptitudeManager.profileFlow.value.xp)
            val xpEarned = maxOf(100, (currentTotalXp * 0.05f).toInt())
            val goldEarned = 250
            var freezeAwarded = false
            var bonusGold = 0
            
            if (AptitudeManager.getStreakFreezesCount() < 3) {
                AptitudeManager.addStreakFreezes(1)
                freezeAwarded = true
            } else {
                bonusGold = 100
            }
            
            val finalGold = goldEarned + bonusGold
            FocusEconomyManager.addExactRewards(exactXp = xpEarned, exactGold = finalGold)
            DailyQuestManager.claimMysteryChest()
            
            claimedReward = MysteryReward(
                xp = xpEarned,
                gold = finalGold,
                streakFreezeAwarded = freezeAwarded,
                bonusGoldInsteadOfFreeze = bonusGold
            )
            
            isRevealed = true
            boxScale.animateTo(1f, animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow))
        }
    }

    Dialog(
        onDismissRequest = {
            if (isRevealed) {
                claimedReward?.let { onClaimed?.invoke(it) }
                onDismiss()
            } else if (!isOpening) {
                onDismiss()
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (isRevealed) {
                        claimedReward?.let { onClaimed?.invoke(it) }
                        onDismiss()
                    } else if (!isOpening) {
                        openBox()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Background Glow
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .scale(boxScale.value)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF38BDF8).copy(alpha = glowAlpha.value * 0.4f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(32.dp)
            ) {
                AnimatedVisibility(
                    visible = !isRevealed,
                    exit = fadeOut(animationSpec = tween(200))
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .scale(boxScale.value)
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                                )
                            )
                            .border(
                                width = 1.dp,
                                color = Color(0xFF38BDF8).copy(alpha = 0.5f),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .clickable { openBox() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Mystery Box",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
                
                AnimatedVisibility(
                    visible = isRevealed,
                    enter = fadeIn(animationSpec = tween(400)) + scaleIn(initialScale = 0.8f, animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium))
                ) {
                    claimedReward?.let { reward ->
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF1E293B)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp,
                                    color = Color(0xFF38BDF8).copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(24.dp)
                                )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stars,
                                    contentDescription = "Reward",
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Rewards Claimed!",
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    RewardItem("XP", reward.xp.toString(), Color(0xFF38BDF8))
                                    RewardItem("Gold", reward.gold.toString(), Color(0xFFFFD700))
                                }
                                
                                if (reward.streakFreezeAwarded) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "+1 Streak Freeze",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF4ADE80)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(32.dp))
                                Text(
                                    text = "Tap anywhere to continue",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
                
                if (!isRevealed) {
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = "Tap to open",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun RewardItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = Color.LightGray
        )
    }
}

enum class ChestRarity(
    val title: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val bgTopColor: Color,
    val bgBottomColor: Color,
    val glowColor: Color,
    val gemColor: Color
) {
    COMMON(
        title = "COMMON",
        primaryColor = Color(0xFF38BDF8),
        secondaryColor = Color(0xFF0284C7),
        bgTopColor = Color(0xFF0C4A6E),
        bgBottomColor = Color(0xFF031E2F),
        glowColor = Color(0xFF38BDF8),
        gemColor = Color(0xFF00E5FF)
    ),
    RARE(
        title = "RARE",
        primaryColor = Color(0xFF60A5FA),
        secondaryColor = Color(0xFF2563EB),
        bgTopColor = Color(0xFF1E3A8A),
        bgBottomColor = Color(0xFF0F172A),
        glowColor = Color(0xFF60A5FA),
        gemColor = Color(0xFF80D8FF)
    ),
    EPIC(
        title = "EPIC",
        primaryColor = Color(0xFFC084FC),
        secondaryColor = Color(0xFF7E22CE),
        bgTopColor = Color(0xFF581C87),
        bgBottomColor = Color(0xFF1E1035),
        glowColor = Color(0xFFE879F9),
        gemColor = Color(0xFFF472B6)
    ),
    LEGENDARY(
        title = "LEGENDARY",
        primaryColor = Color(0xFFFBBF24),
        secondaryColor = Color(0xFFB45309),
        bgTopColor = Color(0xFF78350F),
        bgBottomColor = Color(0xFF241005),
        glowColor = Color(0xFFFFD700),
        gemColor = Color(0xFFFFEA00)
    );

    fun nextTier(): ChestRarity {
        return when (this) {
            COMMON -> RARE
            RARE -> EPIC
            EPIC -> LEGENDARY
            LEGENDARY -> LEGENDARY
        }
    }
}

@Composable
fun DuolingoChest3DGraphic(
    rarity: ChestRarity,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(rarity.primaryColor, rarity.secondaryColor)
                ),
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}
