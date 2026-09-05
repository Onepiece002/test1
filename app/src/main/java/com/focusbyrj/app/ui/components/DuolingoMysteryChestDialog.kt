package com.focusbyrj.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class ChestRarity(
    val title: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val bgTopColor: Color,
    val bgBottomColor: Color
) {
    COMMON(
        title = "COMMON",
        primaryColor = Color(0xFF42A5F5),
        secondaryColor = Color(0xFF1E88E5),
        bgTopColor = Color(0xFF38BDF8),
        bgBottomColor = Color(0xFF0284C7)
    ),
    RARE(
        title = "RARE",
        primaryColor = Color(0xFF29B6F6),
        secondaryColor = Color(0xFF0288D1),
        bgTopColor = Color(0xFF0284C7),
        bgBottomColor = Color(0xFF075985)
    ),
    EPIC(
        title = "EPIC",
        primaryColor = Color(0xFFAB47BC),
        secondaryColor = Color(0xFF7B1FA2),
        bgTopColor = Color(0xFFA855F7),
        bgBottomColor = Color(0xFF6B21A8)
    ),
    LEGENDARY(
        title = "LEGENDARY",
        primaryColor = Color(0xFFFFB300),
        secondaryColor = Color(0xFFFF8F00),
        bgTopColor = Color(0xFFFBBF24),
        bgBottomColor = Color(0xFFB45309)
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

/**
 * Duolingo-style Mystery Chest Upgrade and Reward Experience.
 * Exactly matches Duolingo's interactive chest upgrade flow with 3 chances,
 * rarity upgrading (COMMON -> RARE -> EPIC -> LEGENDARY), dynamic bouncy animations,
 * tactile haptics, and celebration reward reveal screen.
 */
@Composable
fun DuolingoMysteryChestDialog(
    initialRarity: ChestRarity = ChestRarity.COMMON,
    onDismiss: () -> Unit,
    onClaimed: ((MysteryReward) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val economyProfile by FocusEconomyManager.profileFlow.collectAsState()

    var currentRarity by remember { mutableStateOf(initialRarity) }
    var chancesLeft by remember { mutableIntStateOf(3) } // 3 -> 2 -> 1 -> 0 (Opening)
    var isUpgrading by remember { mutableStateOf(false) }
    var isRevealed by remember { mutableStateOf(false) }
    var upgradeSuccessNotice by remember { mutableStateOf(false) }
    var claimedReward by remember { mutableStateOf<MysteryReward?>(null) }

    // Tap bounce physics
    val tapScale = remember { Animatable(1f) }
    val chestWobble = remember { Animatable(0f) }

    // Floating stars animation
    val infiniteTransition = rememberInfiniteTransition(label = "stars_ambient")
    val ambientPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambient_pulse"
    )
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float_offset"
    )

    // Trigger tap action
    fun performTapChance() {
        if (isUpgrading || isRevealed || chancesLeft <= 0) return

        coroutineScope.launch {
            isUpgrading = true
            GamificationHaptics.playLight(context)

            // Tactile squash and stretch
            launch {
                tapScale.animateTo(0.88f, animationSpec = tween(70))
                tapScale.animateTo(1.12f, animationSpec = tween(110))
                tapScale.animateTo(1f, animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium))
            }
            launch {
                chestWobble.animateTo(-8f, animationSpec = tween(50))
                chestWobble.animateTo(8f, animationSpec = tween(60))
                chestWobble.animateTo(-4f, animationSpec = tween(60))
                chestWobble.animateTo(0f, animationSpec = spring())
            }

            delay(120)

            // Calculate upgrade odds per tap:
            // Common -> Rare: 60% chance
            // Rare -> Epic: 35% chance
            // Epic -> Legendary: 10% chance
            val upgradeChance = when (currentRarity) {
                ChestRarity.COMMON -> 0.60f
                ChestRarity.RARE -> 0.35f
                ChestRarity.EPIC -> 0.10f
                ChestRarity.LEGENDARY -> 0.0f
            }

            val didUpgrade = currentRarity != ChestRarity.LEGENDARY && Random.nextFloat() < upgradeChance
            if (didUpgrade) {
                currentRarity = currentRarity.nextTier()
                upgradeSuccessNotice = true
                GamificationHaptics.playSuccess(context)
                delay(350)
                upgradeSuccessNotice = false
            }

            val nextChances = chancesLeft - 1
            chancesLeft = nextChances

            if (nextChances == 0) {
                // Chest opens!
                delay(300)
                GamificationHaptics.playCelebration(context)

                val currentTotalXp = maxOf(0, AptitudeManager.profileFlow.value.xp)
                var xpEarned = 0
                var goldEarned = 0
                var freezeAwarded = false
                var bonusGold = 0
                val currentFreezes = AptitudeManager.getStreakFreezesCount()

                when (currentRarity) {
                    ChestRarity.COMMON -> {
                        // Common: 2X EXP potion for 15 mins + 1000 gold
                        xpEarned = 0
                        goldEarned = 1000
                        AptitudeManager.activateXpBoost(durationMinutes = 15, multiplier = 2.0f)
                    }
                    ChestRarity.RARE -> {
                        // Rare: 1000 xp or 5% current xp (whichever higher) and 10,000 gold
                        xpEarned = maxOf(1000, (currentTotalXp * 0.05f).toInt())
                        goldEarned = 10000
                        // 10% chance for streak freeze
                        if (Random.nextFloat() < 0.10f) {
                            if (currentFreezes < 3) {
                                AptitudeManager.addStreakFreezes(1)
                                freezeAwarded = true
                            } else {
                                bonusGold = 1000
                            }
                        }
                    }
                    ChestRarity.EPIC -> {
                        // Epic: 10% xp or 2000 xp (whichever higher) and 50,000 gold + streak freeze
                        xpEarned = maxOf(2000, (currentTotalXp * 0.10f).toInt())
                        goldEarned = 50000
                        if (currentFreezes < 3) {
                            AptitudeManager.addStreakFreezes(1)
                            freezeAwarded = true
                        } else {
                            bonusGold = 2000
                        }
                    }
                    ChestRarity.LEGENDARY -> {
                        // Legendary jackpot: 20% xp or 5000 xp + 100,000 gold + 2X boost + freeze
                        xpEarned = maxOf(5000, (currentTotalXp * 0.20f).toInt())
                        goldEarned = 100000
                        AptitudeManager.activateXpBoost(durationMinutes = 30, multiplier = 2.0f)
                        if (currentFreezes < 3) {
                            AptitudeManager.addStreakFreezes(1)
                            freezeAwarded = true
                        } else {
                            bonusGold = 5000
                        }
                    }
                }

                val finalGold = goldEarned + bonusGold

                // Save to economy
                if (xpEarned > 0) {
                    AptitudeManager.addAptitudeXp(xpEarned)
                }
                FocusEconomyManager.addRewards(baseXp = xpEarned, baseGold = finalGold)

                // Complete in quest manager
                DailyQuestManager.claimMysteryChest()

                val resultReward = MysteryReward(
                    xp = xpEarned,
                    gold = finalGold,
                    streakFreezeAwarded = freezeAwarded,
                    bonusGoldInsteadOfFreeze = bonusGold
                )
                claimedReward = resultReward
                isRevealed = true
            }

            isUpgrading = false
        }
    }

    Dialog(
        onDismissRequest = {
            if (isRevealed) {
                claimedReward?.let { onClaimed?.invoke(it) }
                onDismiss()
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (isRevealed) {
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF0F172A),
                                Color(0xFF020617)
                            )
                        )
                    } else {
                        Brush.verticalGradient(
                            listOf(
                                currentRarity.bgTopColor,
                                currentRarity.bgBottomColor
                            )
                        )
                    }
                )
        ) {
            // Ambient Floating Sparkles Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val starColor = Color.White.copy(alpha = 0.25f)

                drawSparkleStar(Offset(w * 0.15f, h * 0.18f + floatOffset), 14.dp.toPx(), starColor)
                drawSparkleStar(Offset(w * 0.85f, h * 0.22f - floatOffset), 18.dp.toPx(), starColor)
                drawSparkleStar(Offset(w * 0.20f, h * 0.65f - floatOffset), 12.dp.toPx(), starColor)
                drawSparkleStar(Offset(w * 0.80f, h * 0.72f + floatOffset), 16.dp.toPx(), starColor)
                drawSparkleStar(Offset(w * 0.50f, h * 0.12f + floatOffset * 0.5f), 10.dp.toPx(), starColor)
            }

            if (!isRevealed) {
                // ==================== TAPPING / UPGRADING PHASE ====================
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top: Rarity Pill Badge
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.Black.copy(alpha = 0.25f),
                            border = androidx.compose.foundation.BorderStroke(2.dp, Color.White.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = currentRarity.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    letterSpacing = 1.2.sp
                                )
                            }
                        }

                        // Upgrade notification banner
                        AnimatedVisibility(
                            visible = upgradeSuccessNotice,
                            enter = fadeIn() + scaleIn(),
                            exit = fadeOut()
                        ) {
                            Text(
                                text = "✨ UPGRADED TO ${currentRarity.title}! ✨",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFFFD700),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }

                    // Center: 3D Animated Chest
                    Box(
                        modifier = Modifier
                            .size(280.dp)
                            .scale(tapScale.value)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                enabled = chancesLeft > 0 && !isUpgrading
                            ) {
                                performTapChance()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Radial Ambient Halo
                        Canvas(modifier = Modifier.size(280.dp)) {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.35f),
                                        Color.Transparent
                                    ),
                                    center = center,
                                    radius = size.width * 0.45f * ambientPulse
                                )
                            )
                        }

                        // 3D Chest Model
                        DuolingoChest3DGraphic(
                            rarity = currentRarity,
                            wobbleDegrees = chestWobble.value,
                            modifier = Modifier.size(220.dp)
                        )
                    }

                    // Bottom: 3 Chance Dots & Instruction
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(bottom = 24.dp)
                    ) {
                        // 3 Chances Indicator (Duolingo Style: Arrow circles)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Dot 1 (corresponds to chance 3 -> 2)
                            ChanceDotItem(
                                state = when {
                                    chancesLeft == 3 -> ChanceDotState.ACTIVE
                                    chancesLeft < 3 -> ChanceDotState.COMPLETED
                                    else -> ChanceDotState.PENDING
                                },
                                pulse = if (chancesLeft >= 3) ambientPulse else 1f
                            )

                            // Dot 2 (corresponds to chance 2 -> 1)
                            ChanceDotItem(
                                state = when {
                                    chancesLeft == 2 -> ChanceDotState.ACTIVE
                                    chancesLeft < 2 -> ChanceDotState.COMPLETED
                                    else -> ChanceDotState.PENDING
                                },
                                pulse = if (chancesLeft == 2) ambientPulse else 1f
                            )

                            // Dot 3 (corresponds to chance 1 -> 0)
                            ChanceDotItem(
                                state = when {
                                    chancesLeft == 1 -> ChanceDotState.ACTIVE
                                    chancesLeft < 1 -> ChanceDotState.COMPLETED
                                    else -> ChanceDotState.PENDING
                                },
                                pulse = if (chancesLeft == 1) ambientPulse else 1f
                            )
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // Prompt Text
                        Text(
                            text = when (chancesLeft) {
                                3 -> "Tap the chest to unlock or upgrade!"
                                2 -> "Tap again! Upgrade your prize!"
                                1 -> "Final tap to reveal rewards!"
                                else -> "Opening chest..."
                            },
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // ==================== REWARD REVEAL PHASE ====================
                val reward = claimedReward ?: MysteryReward(xp = 1000, gold = 10000, streakFreezeAwarded = false)
                val isCommonPotion = currentRarity == ChestRarity.COMMON

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top Right: Gold Counter
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, end = 8.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF1E293B),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🪙", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${economyProfile.gold}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                    color = Color(0xFFFFD54F)
                                )
                            }
                        }
                    }

                    // Center Content
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (isCommonPotion) {
                            // Common Reward: 2X EXP Potion Beaker
                            Text(
                                text = "2X EXP BOOST",
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF38BDF8),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Double XP for next 15 mins on Drills & Blitz!",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFE2E8F0),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFFB300).copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB300))
                            ) {
                                Text(
                                    text = "+${reward.gold} 🪙 Gold",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFD54F),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(28.dp))

                            // Animated XP Potion Beaker Graphic
                            DuolingoXpPotionBeakerGraphic(
                                modifier = Modifier
                                    .size(200.dp)
                                    .scale(ambientPulse * 0.95f + 0.05f)
                            )
                        } else {
                            // Rare / Epic / Legendary Rewards: Gold + XP Pile
                            Text(
                                text = "+${reward.gold} Gold",
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFFFD54F),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF38BDF8).copy(alpha = 0.2f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8))
                                ) {
                                    Text(
                                        text = "+${reward.xp} XP",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF7DD3FC),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }

                                if (reward.streakFreezeAwarded) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF00E5FF).copy(alpha = 0.2f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF))
                                    ) {
                                        Text(
                                            text = "+1 🛡️ Streak Freeze",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF80D8FF),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            // 3D Gold Coins Pile Graphic
                            DuolingoGoldCoinsPileGraphic(
                                modifier = Modifier
                                    .size(220.dp)
                                    .scale(ambientPulse * 0.95f + 0.05f)
                            )
                        }
                    }

                    // Bottom: Duolingo 3D "CONTINUE" Button
                    Duolingo3DButton(
                        text = "CONTINUE",
                        buttonColor = Color(0xFF1CB0F6),
                        bevelColor = Color(0xFF1899D6),
                        textColor = Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp),
                        onClick = {
                            GamificationHaptics.playLight(context)
                            claimedReward?.let { onClaimed?.invoke(it) }
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

enum class ChanceDotState {
    PENDING,
    ACTIVE,
    COMPLETED
}

@Composable
private fun ChanceDotItem(
    state: ChanceDotState,
    pulse: Float = 1f
) {
    when (state) {
        ChanceDotState.ACTIVE -> {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .scale(pulse)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowUpward,
                    contentDescription = "Upgrade Arrow",
                    tint = Color(0xFF0284C7),
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        ChanceDotState.COMPLETED -> {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0369A1).copy(alpha = 0.65f))
            )
        }
        ChanceDotState.PENDING -> {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0284C7).copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowUpward,
                    contentDescription = "Next Arrow",
                    tint = Color.White.copy(alpha = 0.35f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Custom 3D Vector Chest Graphic matching Duolingo's aesthetic.
 */
@Composable
private fun DuolingoChest3DGraphic(
    rarity: ChestRarity,
    wobbleDegrees: Float = 0f,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.graphicsLayer {
            rotationZ = wobbleDegrees
        }
    ) {
        val w = size.width
        val h = size.height

        val bodyPrimary = when (rarity) {
            ChestRarity.COMMON -> Color(0xFF42A5F5)
            ChestRarity.RARE -> Color(0xFF0288D1)
            ChestRarity.EPIC -> Color(0xFF8E24AA)
            ChestRarity.LEGENDARY -> Color(0xFFE65100)
        }
        val bodySecondary = when (rarity) {
            ChestRarity.COMMON -> Color(0xFF1E88E5)
            ChestRarity.RARE -> Color(0xFF01579B)
            ChestRarity.EPIC -> Color(0xFF4A148C)
            ChestRarity.LEGENDARY -> Color(0xFFBF360C)
        }
        val goldMain = Color(0xFFFFB300)

        // 1. Base Shadow on ground
        drawOval(
            color = Color(0x33000000),
            topLeft = Offset(w * 0.12f, h * 0.78f),
            size = Size(w * 0.76f, h * 0.18f)
        )

        // 2. Chest Lower Body
        val bodyRect = Path().apply {
            moveTo(w * 0.22f, h * 0.44f)
            lineTo(w * 0.78f, h * 0.44f)
            lineTo(w * 0.74f, h * 0.80f)
            lineTo(w * 0.26f, h * 0.80f)
            close()
        }
        drawPath(
            path = bodyRect,
            brush = Brush.verticalGradient(
                listOf(bodyPrimary, bodySecondary),
                startY = h * 0.44f,
                endY = h * 0.80f
            )
        )

        // Lower body bottom trim (Gold)
        val bottomTrim = Path().apply {
            moveTo(w * 0.24f, h * 0.74f)
            lineTo(w * 0.76f, h * 0.74f)
            lineTo(w * 0.73f, h * 0.82f)
            lineTo(w * 0.27f, h * 0.82f)
            close()
        }
        drawPath(path = bottomTrim, color = goldMain)

        // 3. Chest Upper Lid
        val lidPath = Path().apply {
            moveTo(w * 0.15f, h * 0.44f)
            cubicTo(
                w * 0.16f, h * 0.24f,
                w * 0.84f, h * 0.24f,
                w * 0.85f, h * 0.44f
            )
            lineTo(w * 0.85f, h * 0.48f)
            lineTo(w * 0.15f, h * 0.48f)
            close()
        }
        drawPath(
            path = lidPath,
            brush = Brush.verticalGradient(
                listOf(bodyPrimary, bodySecondary),
                startY = h * 0.24f,
                endY = h * 0.48f
            )
        )

        // Center Gold Lock Emblem
        val lockSize = w * 0.18f
        val lockCenter = Offset(w * 0.50f, h * 0.50f)
        drawCircle(
            color = Color(0xFFFFB300),
            radius = lockSize * 0.5f,
            center = lockCenter
        )
        drawCircle(
            color = Color(0xFFFFD54F),
            radius = lockSize * 0.35f,
            center = Offset(lockCenter.x - 2, lockCenter.y - 2)
        )
        drawCircle(
            color = Color(0xFF3E2723),
            radius = lockSize * 0.18f,
            center = lockCenter
        )
    }
}

/**
 * Animated Duolingo-style XP Potion Beaker Flask Graphic.
 */
@Composable
private fun DuolingoXpPotionBeakerGraphic(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "beaker_anim")
    val bubbleFloat by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bubble_float"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Ground shadow
        drawOval(
            color = Color(0x33000000),
            topLeft = Offset(w * 0.2f, h * 0.85f),
            size = Size(w * 0.6f, h * 0.12f)
        )

        // Ambient potion glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF00E5FF).copy(alpha = 0.45f),
                    Color.Transparent
                ),
                center = Offset(w * 0.5f, h * 0.58f),
                radius = w * 0.45f
            )
        )

        // Beaker Flask Body (Erlenmeyer shape)
        val neckTop = h * 0.20f
        val neckBottom = h * 0.42f
        val flaskBottom = h * 0.82f
        val neckHalfWidth = w * 0.12f
        val baseHalfWidth = w * 0.36f

        val beakerPath = Path().apply {
            moveTo(w * 0.5f - neckHalfWidth, neckTop)
            lineTo(w * 0.5f + neckHalfWidth, neckTop)
            lineTo(w * 0.5f + neckHalfWidth, neckBottom)
            lineTo(w * 0.5f + baseHalfWidth, flaskBottom)
            cubicTo(
                w * 0.5f + baseHalfWidth, flaskBottom + h * 0.04f,
                w * 0.5f - baseHalfWidth, flaskBottom + h * 0.04f,
                w * 0.5f - baseHalfWidth, flaskBottom
            )
            lineTo(w * 0.5f - neckHalfWidth, neckBottom)
            close()
        }

        // Liquid inside beaker
        val liquidTop = h * 0.48f
        val liquidPath = Path().apply {
            val liquidHalfWidth = w * 0.22f
            moveTo(w * 0.5f - liquidHalfWidth, liquidTop)
            lineTo(w * 0.5f + liquidHalfWidth, liquidTop)
            lineTo(w * 0.5f + baseHalfWidth - 4, flaskBottom - 4)
            cubicTo(
                w * 0.5f + baseHalfWidth - 4, flaskBottom + h * 0.03f,
                w * 0.5f - baseHalfWidth + 4, flaskBottom + h * 0.03f,
                w * 0.5f - baseHalfWidth + 4, flaskBottom - 4
            )
            close()
        }

        drawPath(
            path = liquidPath,
            brush = Brush.verticalGradient(
                listOf(
                    Color(0xFF00E5FF),
                    Color(0xFF0288D1)
                ),
                startY = liquidTop,
                endY = flaskBottom
            )
        )

        // Animated Rising Bubbles
        val b1Y = liquidTop + (flaskBottom - liquidTop) * (1f - bubbleFloat)
        drawCircle(
            color = Color.White.copy(alpha = 0.8f),
            radius = 6.dp.toPx(),
            center = Offset(w * 0.45f, b1Y)
        )
        val b2Y = liquidTop + (flaskBottom - liquidTop) * (1f - ((bubbleFloat + 0.5f) % 1f))
        drawCircle(
            color = Color.White.copy(alpha = 0.7f),
            radius = 4.dp.toPx(),
            center = Offset(w * 0.56f, b2Y)
        )

        // Glass outline
        drawPath(
            path = beakerPath,
            color = Color.White.copy(alpha = 0.85f),
            style = Stroke(width = 4.dp.toPx())
        )

        // Beaker Rim
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(w * 0.5f - neckHalfWidth - 4.dp.toPx(), neckTop - 3.dp.toPx()),
            size = Size((neckHalfWidth * 2) + 8.dp.toPx(), 8.dp.toPx()),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )

        // Glass highlight reflection streak
        drawLine(
            color = Color.White.copy(alpha = 0.6f),
            start = Offset(w * 0.5f - neckHalfWidth + 4, neckBottom + 6),
            end = Offset(w * 0.5f - baseHalfWidth + 14, flaskBottom - 10),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )

        // 2X Badge on Beaker
        drawSparkleStar(Offset(w * 0.75f, h * 0.28f), 14.dp.toPx(), Color(0xFFFFD54F))
        drawSparkleStar(Offset(w * 0.22f, h * 0.42f), 10.dp.toPx(), Color(0xFF80D8FF))
    }
}

/**
 * 3D Pile of Gold Coins Graphic.
 */
@Composable
private fun DuolingoGoldCoinsPileGraphic(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Ground shadow
        drawOval(
            color = Color(0x33000000),
            topLeft = Offset(w * 0.15f, h * 0.78f),
            size = Size(w * 0.70f, h * 0.18f)
        )

        // Bottom layer coins
        draw3DGoldCoin(Offset(w * 0.30f, h * 0.68f), radius = w * 0.14f)
        draw3DGoldCoin(Offset(w * 0.70f, h * 0.68f), radius = w * 0.14f)
        draw3DGoldCoin(Offset(w * 0.50f, h * 0.72f), radius = w * 0.16f)

        // Mid layer coins
        draw3DGoldCoin(Offset(w * 0.38f, h * 0.50f), radius = w * 0.13f)
        draw3DGoldCoin(Offset(w * 0.62f, h * 0.50f), radius = w * 0.13f)

        // Top apex coin
        draw3DGoldCoin(Offset(w * 0.50f, h * 0.34f), radius = w * 0.15f)

        // Sparkles
        drawSparkleStar(Offset(w * 0.25f, h * 0.35f), 12.dp.toPx(), Color(0xFFFFD54F))
        drawSparkleStar(Offset(w * 0.78f, h * 0.42f), 16.dp.toPx(), Color.White)
        drawSparkleStar(Offset(w * 0.50f, h * 0.18f), 14.dp.toPx(), Color(0xFFFFE082))
    }
}

private fun DrawScope.draw3DGoldCoin(
    center: Offset,
    radius: Float
) {
    // 3D Rim Shadow
    drawCircle(
        color = Color(0xFFC67100),
        radius = radius,
        center = Offset(center.x, center.y + 4.dp.toPx())
    )
    // Gold Face
    drawCircle(
        brush = Brush.radialGradient(
            listOf(
                Color(0xFFFFE082),
                Color(0xFFFFB300)
            ),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
    // Inner Ring
    drawCircle(
        color = Color(0xFFFF8F00),
        radius = radius * 0.78f,
        center = center,
        style = Stroke(width = 2.dp.toPx())
    )
    // Star or Symbol in center
    drawSparkleStar(center, radius * 0.35f, Color(0xFFFFD54F))
}

/**
 * Draws a 4-point sparkling star.
 */
private fun DrawScope.drawSparkleStar(
    center: Offset,
    radius: Float,
    color: Color
) {
    val starPath = Path().apply {
        moveTo(center.x, center.y - radius)
        quadraticBezierTo(center.x, center.y, center.x + radius, center.y)
        quadraticBezierTo(center.x, center.y, center.x, center.y + radius)
        quadraticBezierTo(center.x, center.y, center.x - radius, center.y)
        quadraticBezierTo(center.x, center.y, center.x, center.y - radius)
        close()
    }
    drawPath(path = starPath, color = color)
}
