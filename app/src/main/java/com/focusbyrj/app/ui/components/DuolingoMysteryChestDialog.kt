package com.focusbyrj.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.focusbyrj.app.util.AptitudeManager
import com.focusbyrj.app.util.DailyQuestManager
import com.focusbyrj.app.util.FocusEconomyManager
import com.focusbyrj.app.util.GamificationHaptics
import com.focusbyrj.app.util.MysteryReward
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

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
        // Simple shape instead of relying on Material icons that might be missing
        Canvas(modifier = Modifier.size(24.dp)) {
            drawCircle(color = Color.White.copy(alpha = 0.5f), radius = size.width / 2)
            drawCircle(color = Color.White, radius = size.width / 4)
        }
    }
}

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
    
    val infiniteTransition = rememberInfiniteTransition(label = "core_idle")
    val hoverOffset by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hover"
    )
    val idleRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "idle_rot"
    )

    // Physics
    val boxScale = remember { Animatable(1f) }
    val spinRotation = remember { Animatable(0f) }
    val flashAlpha = remember { Animatable(0f) }
    val ringRadius = remember { Animatable(0f) }
    val ringAlpha = remember { Animatable(0f) }
    
    val cardOffsetY = remember { Animatable(200f) }

    fun openCore() {
        if (isOpening || isRevealed) return
        isOpening = true
        coroutineScope.launch {
            GamificationHaptics.playLight(context)
            
            // Anticipation charge up
            launch {
                boxScale.animateTo(0.7f, animationSpec = tween(400, easing = FastOutSlowInEasing))
            }
            launch {
                spinRotation.animateTo(720f, animationSpec = tween(400, easing = FastOutLinearInEasing))
            }
            delay(450)
            
            // Detonation
            GamificationHaptics.playCelebration(context)
            
            launch {
                flashAlpha.snapTo(1f)
                flashAlpha.animateTo(0f, animationSpec = tween(800, easing = FastOutSlowInEasing))
            }
            
            launch {
                ringRadius.snapTo(0.1f)
                ringAlpha.snapTo(1f)
                ringRadius.animateTo(3f, animationSpec = tween(600, easing = FastOutSlowInEasing))
                ringAlpha.animateTo(0f, animationSpec = tween(300))
            }
            
            launch {
                boxScale.animateTo(0f, animationSpec = tween(200))
            }
            
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
            
            // Card rise
            cardOffsetY.animateTo(0f, animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow))
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
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF0F172A), Color.Black),
                        center = Offset.Unspecified,
                        radius = 1200f
                    )
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (isRevealed) {
                        claimedReward?.let { onClaimed?.invoke(it) }
                        onDismiss()
                    } else if (!isOpening) {
                        openCore()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            
            // Particle & Shockwave Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2
                val cy = size.height / 2
                
                if (ringAlpha.value > 0f) {
                    val currentRadius = size.width * 0.4f * ringRadius.value
                    drawCircle(
                        color = Color(0xFF38BDF8).copy(alpha = ringAlpha.value),
                        radius = currentRadius,
                        center = Offset(cx, cy),
                        style = Stroke(width = 4.dp.toPx())
                    )
                    drawCircle(
                        color = Color(0xFF818CF8).copy(alpha = ringAlpha.value * 0.5f),
                        radius = currentRadius * 0.8f,
                        center = Offset(cx, cy),
                        style = Stroke(width = 8.dp.toPx())
                    )
                }
                
                if (flashAlpha.value > 0f) {
                    drawRect(color = Color.White.copy(alpha = flashAlpha.value))
                }
            }
            
            // Idle State: The Mystery Core
            if (!isRevealed) {
                Box(
                    modifier = Modifier
                        .offset(y = hoverOffset.dp)
                        .scale(boxScale.value)
                        .size(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val cx = w / 2
                        val cy = h / 2
                        
                        val totalRot = idleRotation + spinRotation.value
                        
                        rotate(degrees = totalRot * 0.5f, pivot = Offset(cx, cy)) {
                            drawCircle(
                                color = Color(0xFF38BDF8).copy(alpha = 0.15f),
                                radius = w * 0.7f,
                                center = Offset(cx, cy)
                            )
                        }
                        
                        rotate(degrees = totalRot, pivot = Offset(cx, cy)) {
                            // Draw sleek isometric hexagon/cube
                            val topColor = Color(0xFF38BDF8)
                            val leftColor = Color(0xFF0284C7)
                            val rightColor = Color(0xFF818CF8)

                            val topPath = Path().apply {
                                moveTo(cx, cy - h * 0.45f)
                                lineTo(cx + w * 0.45f, cy - h * 0.15f)
                                lineTo(cx, cy + h * 0.15f)
                                lineTo(cx - w * 0.45f, cy - h * 0.15f)
                                close()
                            }
                            
                            val leftPath = Path().apply {
                                moveTo(cx - w * 0.45f, cy - h * 0.15f)
                                lineTo(cx, cy + h * 0.15f)
                                lineTo(cx, cy + h * 0.45f)
                                lineTo(cx - w * 0.45f, cy + h * 0.15f)
                                close()
                            }
                            
                            val rightPath = Path().apply {
                                moveTo(cx, cy + h * 0.15f)
                                lineTo(cx + w * 0.45f, cy - h * 0.15f)
                                lineTo(cx + w * 0.45f, cy + h * 0.15f)
                                lineTo(cx, cy + h * 0.45f)
                                close()
                            }
                            
                            drawPath(topPath, topColor)
                            drawPath(leftPath, leftColor)
                            drawPath(rightPath, rightColor)
                            
                            // High-tech wireframe outlines
                            drawPath(topPath, Color.White.copy(alpha = 0.5f), style = Stroke(width = 2.dp.toPx()))
                            drawPath(leftPath, Color.White.copy(alpha = 0.3f), style = Stroke(width = 2.dp.toPx()))
                            drawPath(rightPath, Color.White.copy(alpha = 0.3f), style = Stroke(width = 2.dp.toPx()))
                        }
                    }
                }
                
                Text(
                    text = "TAP TO DECRYPT CORE",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    ),
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 64.dp)
                )
            }
            
            // Revealed State: The Reward Card
            AnimatedVisibility(
                visible = isRevealed,
                enter = fadeIn(animationSpec = tween(400)),
                exit = fadeOut(animationSpec = tween(200)),
                modifier = Modifier.align(Alignment.Center)
            ) {
                claimedReward?.let { reward ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .offset(y = cardOffsetY.value.dp),
                        shape = RoundedCornerShape(24.dp),
                        color = Color.White.copy(alpha = 0.05f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "CORE DECRYPTED",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 2.sp
                                ),
                                color = Color(0xFF38BDF8)
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                ModernRewardStat("XP YIELD", "+${reward.xp}", Color(0xFF818CF8))
                                ModernRewardStat("CREDITS", "+${reward.gold}", Color(0xFFFCD34D))
                            }
                            
                            if (reward.streakFreezeAwarded) {
                                Spacer(modifier = Modifier.height(24.dp))
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF34D399).copy(alpha = 0.15f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF34D399).copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        text = "+1 STREAK FREEZE",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF34D399),
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(48.dp))
                            Text(
                                text = "TAP TO CONTINUE",
                                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                                color = Color.White.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModernRewardStat(label: String, value: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            ),
            color = valueColor
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            ),
            color = Color.White.copy(alpha = 0.6f)
        )
    }
}
