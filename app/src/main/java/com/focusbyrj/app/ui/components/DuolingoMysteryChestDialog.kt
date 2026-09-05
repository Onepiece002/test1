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
    val bgBottomColor: Color,
    val gemReward: Int,
    val xpReward: Int,
    val chanceForFreeze: Float
) {
    COMMON(
        title = "COMMON",
        primaryColor = Color(0xFF64B5F6),
        secondaryColor = Color(0xFF1E88E5),
        bgTopColor = Color(0xFF42A5F5),
        bgBottomColor = Color(0xFF1976D2),
        gemReward = 8,
        xpReward = 60,
        chanceForFreeze = 0.2f
    ),
    RARE(
        title = "RARE",
        primaryColor = Color(0xFF29B6F6),
        secondaryColor = Color(0xFF0288D1),
        bgTopColor = Color(0xFF38BDF8),
        bgBottomColor = Color(0xFF0284C7),
        gemReward = 15,
        xpReward = 120,
        chanceForFreeze = 0.5f
    ),
    EPIC(
        title = "EPIC",
        primaryColor = Color(0xFFAB47BC),
        secondaryColor = Color(0xFF7B1FA2),
        bgTopColor = Color(0xFFA855F7),
        bgBottomColor = Color(0xFF7E22CE),
        gemReward = 30,
        xpReward = 200,
        chanceForFreeze = 0.85f
    ),
    LEGENDARY(
        title = "LEGENDARY",
        primaryColor = Color(0xFFFFB300),
        secondaryColor = Color(0xFFFF8F00),
        bgTopColor = Color(0xFFFBBF24),
        bgBottomColor = Color(0xFFD97706),
        gemReward = 60,
        xpReward = 350,
        chanceForFreeze = 1.0f
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
 * rarity upgrading (RARE -> EPIC -> LEGENDARY), dynamic bouncy animations,
 * tactile haptics, and celebration gem reveal screen.
 */
@Composable
fun DuolingoMysteryChestDialog(
    initialRarity: ChestRarity = ChestRarity.RARE,
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

            // Calculate upgrade roll: 55% chance to upgrade rarity on each tap!
            val didUpgrade = currentRarity != ChestRarity.LEGENDARY && Random.nextFloat() < 0.60f
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

                // Grant rewards based on final achieved rarity
                val gemsEarned = currentRarity.gemReward
                val xpEarned = currentRarity.xpReward
                var freezeAwarded = false
                var bonusGems = 0

                val currentFreezes = AptitudeManager.getStreakFreezesCount()
                if (currentFreezes < 3 && Random.nextFloat() < currentRarity.chanceForFreeze) {
                    AptitudeManager.addStreakFreezes(1)
                    freezeAwarded = true
                } else if (currentFreezes >= 3) {
                    bonusGems = (gemsEarned * 0.4f).toInt()
                }

                val finalGems = gemsEarned + bonusGems

                // Save to economy
                AptitudeManager.addAptitudeXp(xpEarned)
                FocusEconomyManager.addRewards(baseXp = xpEarned, baseGold = finalGems)

                // Complete in quest manager if daily quest state
                val questReward = DailyQuestManager.claimMysteryChest()

                val resultReward = MysteryReward(
                    xp = xpEarned,
                    gold = finalGems,
                    streakFreezeAwarded = freezeAwarded,
                    bonusGoldInsteadOfFreeze = bonusGems
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
                                Color(0xFF0F1E25),
                                Color(0xFF091115)
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
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (!isRevealed) {
                        performTapChance()
                    }
                }
        ) {
            if (!isRevealed) {
                // ==================== UPGRADE PHASE ====================
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top: Rarity Header
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 28.dp)
                    ) {
                        Text(
                            text = currentRarity.title,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        AnimatedVisibility(
                            visible = upgradeSuccessNotice,
                            enter = fadeIn() + scaleIn(),
                            exit = fadeOut()
                        ) {
                            Text(
                                text = "✨ UPGRADED! ✨",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFFFEB3B),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    // Center: 3D Chest Graphic + Sparkling Stars
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(300.dp)
                            .offset(y = floatOffset.dp)
                            .scale(tapScale.value)
                    ) {
                        // Ambient Twinkle Stars
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawSparkleStar(
                                center = Offset(size.width * 0.18f, size.height * 0.35f),
                                radius = 10f * ambientPulse,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                            drawSparkleStar(
                                center = Offset(size.width * 0.84f, size.height * 0.28f),
                                radius = 12f * ambientPulse,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                            drawSparkleStar(
                                center = Offset(size.width * 0.88f, size.height * 0.58f),
                                radius = 8f * ambientPulse,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                            drawSparkleStar(
                                center = Offset(size.width * 0.12f, size.height * 0.72f),
                                radius = 9f * ambientPulse,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            drawSparkleStar(
                                center = Offset(size.width * 0.5f, size.height * 0.12f),
                                radius = 11f * ambientPulse,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }

                        // Duolingo 3D Vector Chest
                        DuolingoChest3DGraphic(
                            rarity = currentRarity,
                            wobbleDegrees = chestWobble.value,
                            modifier = Modifier
                                .size(240.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    performTapChance()
                                }
                        )
                    }

                    // Bottom: 3 Dots and Dynamic Prompt Text
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(bottom = 36.dp)
                    ) {
                        // 3 Chance Indicator Dots
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Dot 1 (corresponds to chance 3 -> 2)
                            ChanceDotItem(
                                state = when {
                                    chancesLeft >= 3 -> ChanceDotState.ACTIVE
                                    else -> ChanceDotState.COMPLETED
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
                                3 -> "Tap for a chance to upgrade!"
                                2 -> "Tap! Tap!"
                                1 -> "1 chance left!"
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
                val reward = claimedReward ?: MysteryReward(xp = 120, gold = 15, streakFreezeAwarded = false)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top Right: Gems Counter
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, end = 8.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF1B2E37),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2B4653))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🔷", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${economyProfile.gold}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                    color = Color(0xFF38BDF8)
                                )
                            }
                        }
                    }

                    // Center Content: +X gems and 3D Gems Pile
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Title: +X gems
                        Text(
                            text = "+${reward.gold} gems",
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF38BDF8),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Bonus info if applicable
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFFB300).copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB300))
                            ) {
                                Text(
                                    text = "+${reward.xp} XP",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFD54F),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF80D8FF),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(36.dp))

                        // 3D Gem Pile Graphic
                        DuolingoGemsPileGraphic(
                            modifier = Modifier
                                .size(220.dp)
                                .scale(ambientPulse * 0.95f + 0.05f)
                        )
                    }

                    // Bottom: Duolingo 3D "CONTINUE" Button
                    Duolingo3DButton(
                        text = "CONTINUE",
                        onClick = {
                            GamificationHaptics.playLight(context)
                            claimedReward?.let { onClaimed?.invoke(it) }
                            onDismiss()
                        },
                        containerColor = Color(0xFF1CB0F6),
                        shadowColor = Color(0xFF1899D6),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp)
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
 * 3D Tactile Duolingo Button with bottom bevel depth that squashes on press.
 */
@Composable
fun Duolingo3DButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = Color(0xFF1CB0F6),
    shadowColor: Color = Color(0xFF1899D6),
    textColor: Color = Color.White
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressOffsetY by animateFloatAsState(
        targetValue = if (isPressed) 4f else 0f,
        animationSpec = tween(durationMillis = 60),
        label = "button_press"
    )

    Box(
        modifier = modifier
            .height(54.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        // Bottom Shadow / Bevel Lip (4dp depth)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .offset(y = 4.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(shadowColor)
        )

        // Top Face of Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .offset(y = pressOffsetY.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(containerColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                ),
                color = textColor,
                textAlign = TextAlign.Center
            )
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

        // Colors based on Rarity
        val bodyPrimary = when (rarity) {
            ChestRarity.COMMON -> Color(0xFF29B6F6)
            ChestRarity.RARE -> Color(0xFF0288D1)
            ChestRarity.EPIC -> Color(0xFF8E24AA)
            ChestRarity.LEGENDARY -> Color(0xFFE65100)
        }
        val bodySecondary = when (rarity) {
            ChestRarity.COMMON -> Color(0xFF0288D1)
            ChestRarity.RARE -> Color(0xFF01579B)
            ChestRarity.EPIC -> Color(0xFF4A148C)
            ChestRarity.LEGENDARY -> Color(0xFFBF360C)
        }
        val goldHighlight = Color(0xFFFFE082)
        val goldMain = Color(0xFFFFB300)
        val goldShadow = Color(0xFFFF8F00)
        val goldDeepShadow = Color(0xFFC67100)

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

        // 4. Chunky Gold Lid Border & Straps (Duolingo Style)
        // Left gold strap
        val leftStrap = Path().apply {
            moveTo(w * 0.16f, h * 0.46f)
            cubicTo(w * 0.18f, h * 0.26f, w * 0.32f, h * 0.24f, w * 0.34f, h * 0.24f)
            lineTo(w * 0.40f, h * 0.24f)
            cubicTo(w * 0.38f, h * 0.26f, w * 0.26f, h * 0.28f, w * 0.24f, h * 0.46f)
            close()
        }
        drawPath(path = leftStrap, color = goldMain)

        // Right gold strap
        val rightStrap = Path().apply {
            moveTo(w * 0.84f, h * 0.46f)
            cubicTo(w * 0.82f, h * 0.26f, w * 0.68f, h * 0.24f, w * 0.66f, h * 0.24f)
            lineTo(w * 0.60f, h * 0.24f)
            cubicTo(w * 0.62f, h * 0.26f, w * 0.74f, h * 0.28f, w * 0.76f, h * 0.46f)
            close()
        }
        drawPath(path = rightStrap, color = goldMain)

        // Left gold corner pillar
        drawRoundRect(
            color = goldMain,
            topLeft = Offset(w * 0.18f, h * 0.44f),
            size = Size(w * 0.12f, h * 0.36f),
            cornerRadius = CornerRadius(10f, 10f)
        )

        // Right gold corner pillar
        drawRoundRect(
            color = goldMain,
            topLeft = Offset(w * 0.70f, h * 0.44f),
            size = Size(w * 0.12f, h * 0.36f),
            cornerRadius = CornerRadius(10f, 10f)
        )

        // Center Gold Horizontal Bezel / Rim
        drawRoundRect(
            color = goldShadow,
            topLeft = Offset(w * 0.13f, h * 0.43f),
            size = Size(w * 0.74f, h * 0.13f),
            cornerRadius = CornerRadius(18f, 18f)
        )
        drawRoundRect(
            color = goldMain,
            topLeft = Offset(w * 0.13f, h * 0.41f),
            size = Size(w * 0.74f, h * 0.12f),
            cornerRadius = CornerRadius(18f, 18f)
        )
        // Bezel top highlight
        drawRoundRect(
            color = goldHighlight,
            topLeft = Offset(w * 0.16f, h * 0.415f),
            size = Size(w * 0.68f, h * 0.035f),
            cornerRadius = CornerRadius(8f, 8f)
        )

        // 5. Center Golden Heart / Shield Crest Lock
        // Outer gold crest holder
        val crestHolder = Path().apply {
            moveTo(w * 0.40f, h * 0.42f)
            lineTo(w * 0.60f, h * 0.42f)
            cubicTo(w * 0.62f, h * 0.54f, w * 0.55f, h * 0.60f, w * 0.50f, h * 0.63f)
            cubicTo(w * 0.45f, h * 0.60f, w * 0.38f, h * 0.54f, w * 0.40f, h * 0.42f)
            close()
        }
        drawPath(path = crestHolder, color = goldDeepShadow)

        // Inner White Shield / Heart Lock
        val innerShield = Path().apply {
            moveTo(w * 0.43f, h * 0.44f)
            lineTo(w * 0.57f, h * 0.44f)
            cubicTo(w * 0.59f, h * 0.53f, w * 0.54f, h * 0.58f, w * 0.50f, h * 0.60f)
            cubicTo(w * 0.46f, h * 0.58f, w * 0.41f, h * 0.53f, w * 0.43f, h * 0.44f)
            close()
        }
        drawPath(path = innerShield, color = Color.White)
        drawPath(path = innerShield, color = Color(0xFFE2E8F0), style = Stroke(width = 3f))
    }
}

/**
 * 3D Gems Pile Graphic for the Reward Reveal Screen.
 */
@Composable
private fun DuolingoGemsPileGraphic(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Ground shadow
        drawOval(
            color = Color(0x66000000),
            topLeft = Offset(w * 0.08f, h * 0.72f),
            size = Size(w * 0.84f, h * 0.22f)
        )

        // Draw multiple glowing 3D Gems in a pyramid cluster
        // Bottom layer left
        drawGemHexagon(
            center = Offset(w * 0.30f, h * 0.65f),
            radius = w * 0.16f,
            primaryColor = Color(0xFF00B0FF),
            highlightColor = Color(0xFF80D8FF),
            shadowColor = Color(0xFF0091EA)
        )

        // Bottom layer right
        drawGemHexagon(
            center = Offset(w * 0.70f, h * 0.65f),
            radius = w * 0.16f,
            primaryColor = Color(0xFF00B0FF),
            highlightColor = Color(0xFF80D8FF),
            shadowColor = Color(0xFF0091EA)
        )

        // Bottom layer center front
        drawGemHexagon(
            center = Offset(w * 0.50f, h * 0.70f),
            radius = w * 0.18f,
            primaryColor = Color(0xFF29B6F6),
            highlightColor = Color(0xFFE1F5FE),
            shadowColor = Color(0xFF0288D1)
        )

        // Mid layer left
        drawGemHexagon(
            center = Offset(w * 0.38f, h * 0.45f),
            radius = w * 0.15f,
            primaryColor = Color(0xFF40C4FF),
            highlightColor = Color(0xFFE1F5FE),
            shadowColor = Color(0xFF0091EA)
        )

        // Mid layer right
        drawGemHexagon(
            center = Offset(w * 0.62f, h * 0.45f),
            radius = w * 0.15f,
            primaryColor = Color(0xFF00B0FF),
            highlightColor = Color(0xFF80D8FF),
            shadowColor = Color(0xFF0091EA)
        )

        // Top apex gem
        drawGemHexagon(
            center = Offset(w * 0.50f, h * 0.28f),
            radius = w * 0.17f,
            primaryColor = Color(0xFF40C4FF),
            highlightColor = Color.White,
            shadowColor = Color(0xFF0288D1)
        )
    }
}

/**
 * Draws a faceted 3D Hexagonal Gemstone.
 */
private fun DrawScope.drawGemHexagon(
    center: Offset,
    radius: Float,
    primaryColor: Color,
    highlightColor: Color,
    shadowColor: Color
) {
    val points = mutableListOf<Offset>()
    for (i in 0 until 6) {
        val angle = (i * 60f - 30f) * (PI / 180f).toFloat()
        points.add(
            Offset(
                center.x + radius * cos(angle),
                center.y + radius * sin(angle)
            )
        )
    }

    // Outer Hexagon Path
    val outerHex = Path().apply {
        moveTo(points[0].x, points[0].y)
        for (i in 1 until 6) {
            lineTo(points[i].x, points[i].y)
        }
        close()
    }
    drawPath(path = outerHex, color = shadowColor)

    // Inner smaller face
    val innerRadius = radius * 0.55f
    val innerPoints = mutableListOf<Offset>()
    for (i in 0 until 6) {
        val angle = (i * 60f - 30f) * (PI / 180f).toFloat()
        innerPoints.add(
            Offset(
                center.x + innerRadius * cos(angle),
                center.y + innerRadius * sin(angle)
            )
        )
    }

    // Top Facets (Highlights)
    val topFacet = Path().apply {
        moveTo(points[0].x, points[0].y)
        lineTo(points[1].x, points[1].y)
        lineTo(innerPoints[1].x, innerPoints[1].y)
        lineTo(innerPoints[0].x, innerPoints[0].y)
        close()
    }
    drawPath(path = topFacet, color = highlightColor)

    // Side Facets
    val leftFacet = Path().apply {
        moveTo(points[1].x, points[1].y)
        lineTo(points[2].x, points[2].y)
        lineTo(innerPoints[2].x, innerPoints[2].y)
        lineTo(innerPoints[1].x, innerPoints[1].y)
        close()
    }
    drawPath(path = leftFacet, color = primaryColor)

    // Center Hexagon Flat Table
    val centerTable = Path().apply {
        moveTo(innerPoints[0].x, innerPoints[0].y)
        for (i in 1 until 6) {
            lineTo(innerPoints[i].x, innerPoints[i].y)
        }
        close()
    }
    drawPath(path = centerTable, color = primaryColor)

    // Glint highlight inside table
    drawCircle(
        color = Color.White.copy(alpha = 0.9f),
        radius = innerRadius * 0.25f,
        center = Offset(center.x - innerRadius * 0.2f, center.y - innerRadius * 0.2f)
    )
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
