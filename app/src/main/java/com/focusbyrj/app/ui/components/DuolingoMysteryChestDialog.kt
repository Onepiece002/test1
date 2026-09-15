package com.focusbyrj.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
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
import kotlin.random.Random

enum class ChestRarity(
    val title: String,
    val oddsPercentage: String,
    val badgeLabel: String,
    val iconSymbol: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val bgTopColor: Color,
    val bgBottomColor: Color,
    val glowColor: Color,
    val gemColor: Color
) {
    COMMON(
        title = "QUANTUM",
        oddsPercentage = "55%",
        badgeLabel = "COMMON • 55% DROP",
        iconSymbol = "💎",
        primaryColor = Color(0xFF00F0FF),
        secondaryColor = Color(0xFF0284C7),
        bgTopColor = Color(0xFF0C2A4D),
        bgBottomColor = Color(0xFF06101F),
        glowColor = Color(0xFF00F0FF),
        gemColor = Color(0xFF38BDF8)
    ),
    RARE(
        title = "AURORA",
        oddsPercentage = "28%",
        badgeLabel = "RARE • 28% DROP",
        iconSymbol = "🔮",
        primaryColor = Color(0xFF818CF8),
        secondaryColor = Color(0xFF4F46E5),
        bgTopColor = Color(0xFF1E1B4B),
        bgBottomColor = Color(0xFF0A0826),
        glowColor = Color(0xFF818CF8),
        gemColor = Color(0xFFA5B4FC)
    ),
    EPIC(
        title = "CELESTIAL",
        oddsPercentage = "13%",
        badgeLabel = "EPIC • 13% DROP",
        iconSymbol = "✨",
        primaryColor = Color(0xFFC084FC),
        secondaryColor = Color(0xFF7E22CE),
        bgTopColor = Color(0xFF3B0764),
        bgBottomColor = Color(0xFF150226),
        glowColor = Color(0xFFE879F9),
        gemColor = Color(0xFFF472B6)
    ),
    LEGENDARY(
        title = "SOLARIS",
        oddsPercentage = "4%",
        badgeLabel = "LEGENDARY • 4% JACKPOT",
        iconSymbol = "👑",
        primaryColor = Color(0xFFFBBF24),
        secondaryColor = Color(0xFFD97706),
        bgTopColor = Color(0xFF451A03),
        bgBottomColor = Color(0xFF1C0A02),
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

    companion object {
        /**
         * Dynamic weighted RNG roll to determine rarity.
         * Higher prize tiers have lower odds, lower prize tiers have higher odds.
         */
        fun rollRarity(baseTier: ChestRarity? = null): ChestRarity {
            val roll = Random.nextInt(1, 101) // 1..100
            return when (baseTier) {
                RARE -> {
                    // Night Owl boosted odds
                    when {
                        roll <= 50 -> RARE       // 50%
                        roll <= 85 -> EPIC       // 35%
                        else -> LEGENDARY       // 15%
                    }
                }
                EPIC -> {
                    when {
                        roll <= 70 -> EPIC       // 70%
                        else -> LEGENDARY       // 30%
                    }
                }
                LEGENDARY -> LEGENDARY
                else -> {
                    // Standard Mystery Drop: 55% Common, 28% Rare, 13% Epic, 4% Legendary
                    when {
                        roll <= 55 -> COMMON
                        roll <= 83 -> RARE
                        roll <= 96 -> EPIC
                        else -> LEGENDARY
                    }
                }
            }
        }
    }
}

/**
 * Modern 3D Graphic used across card previews and badges
 */
@Composable
fun DuolingoChest3DGraphic(
    rarity: ChestRarity,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF161F33), Color(0xFF0A0F1A))
                )
            )
            .border(
                1.dp,
                Brush.linearGradient(listOf(rarity.primaryColor.copy(alpha = 0.6f), rarity.secondaryColor.copy(alpha = 0.2f))),
                RoundedCornerShape(16.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize(0.72f)) {
            val w = size.width
            val h = size.height
            val cx = w / 2
            val cy = h / 2

            // Background glow
            drawCircle(
                color = rarity.primaryColor.copy(alpha = 0.25f),
                radius = w * 0.45f,
                center = Offset(cx, cy)
            )

            // 3D Isometric cube facets
            val topPath = Path().apply {
                moveTo(cx, cy - h * 0.38f)
                lineTo(cx + w * 0.36f, cy - h * 0.14f)
                lineTo(cx, cy + h * 0.10f)
                lineTo(cx - w * 0.36f, cy - h * 0.14f)
                close()
            }

            val leftPath = Path().apply {
                moveTo(cx - w * 0.36f, cy - h * 0.14f)
                lineTo(cx, cy + h * 0.10f)
                lineTo(cx, cy + h * 0.42f)
                lineTo(cx - w * 0.36f, cy + h * 0.18f)
                close()
            }

            val rightPath = Path().apply {
                moveTo(cx, cy + h * 0.10f)
                lineTo(cx + w * 0.36f, cy - h * 0.14f)
                lineTo(cx + w * 0.36f, cy + h * 0.18f)
                lineTo(cx, cy + h * 0.42f)
                close()
            }

            drawPath(topPath, rarity.primaryColor)
            drawPath(leftPath, rarity.secondaryColor)
            drawPath(rightPath, Color(0xFF0F172A))

            // Metallic/Holographic Edges
            drawPath(topPath, Color.White.copy(alpha = 0.6f), style = Stroke(width = 1.2.dp.toPx()))
            drawPath(leftPath, Color.White.copy(alpha = 0.3f), style = Stroke(width = 1.2.dp.toPx()))
            drawPath(rightPath, Color.White.copy(alpha = 0.3f), style = Stroke(width = 1.2.dp.toPx()))

            // Glowing Core Orb
            drawCircle(
                color = Color.White,
                radius = 2.5.dp.toPx(),
                center = Offset(cx, cy + h * 0.10f)
            )
            drawCircle(
                color = rarity.primaryColor,
                radius = 5.5.dp.toPx(),
                center = Offset(cx, cy + h * 0.10f),
                style = Stroke(width = 1.2.dp.toPx())
            )
        }
    }
}

private data class AmbientParticle(
    val x: Float,
    val y: Float,
    val size: Float,
    val alpha: Float,
    val speed: Float
)

private data class SparkBurstParticle(
    val startX: Float,
    val startY: Float,
    val angle: Double,
    val distance: Float,
    val color: Color,
    val size: Float
)

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
    
    // Dynamic Rarity State - rolls and cycles through tiers during decryption
    var wonRarity by remember { mutableStateOf(initialRarity) }
    var displayedRarity by remember { mutableStateOf(initialRarity) }

    // Ambient floating particles
    val ambientParticles = remember {
        List(32) {
            AmbientParticle(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                size = Random.nextFloat() * 3.5f + 1.5f,
                alpha = Random.nextFloat() * 0.6f + 0.2f,
                speed = Random.nextFloat() * 0.0003f + 0.0001f
            )
        }
    }

    // Dynamic Spark burst particles generated on detonation
    val sparkParticles = remember(wonRarity) {
        val particleCount = when (wonRarity) {
            ChestRarity.LEGENDARY -> 80
            ChestRarity.EPIC -> 60
            ChestRarity.RARE -> 45
            ChestRarity.COMMON -> 32
        }
        List(particleCount) {
            val angle = Random.nextDouble(0.0, Math.PI * 2)
            val dist = Random.nextFloat() * 260f + 70f
            val col = when (wonRarity) {
                ChestRarity.LEGENDARY -> listOf(Color(0xFFFFD700), Color(0xFFFBBF24), Color(0xFFFFF59D), Color.White).random()
                ChestRarity.EPIC -> listOf(Color(0xFFE879F9), Color(0xFFC084FC), Color(0xFFF472B6), Color.White).random()
                ChestRarity.RARE -> listOf(Color(0xFF818CF8), Color(0xFF4F46E5), Color(0xFFA5B4FC), Color.White).random()
                ChestRarity.COMMON -> listOf(Color(0xFF00F0FF), Color(0xFF0284C7), Color(0xFF38BDF8), Color.White).random()
            }
            SparkBurstParticle(
                startX = 0.5f,
                startY = 0.45f,
                angle = angle,
                distance = dist,
                color = col,
                size = Random.nextFloat() * 4.5f + 2f
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "vault_ambient")
    val hoverOffset by infiniteTransition.animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hover"
    )

    val gyroRotX by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (wonRarity == ChestRarity.LEGENDARY) 4000 else 7000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gyroX"
    )

    val gyroRotY by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (wonRarity == ChestRarity.LEGENDARY) 5000 else 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gyroY"
    )

    // Sunburst / Celestial Ray rotation for Epic & Legendary
    val rayRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ray_rot"
    )

    val corePulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Unboxing physics
    val boxScale = remember { Animatable(1f) }
    val boxShake = remember { Animatable(0f) }
    val lidElevation = remember { Animatable(0f) }
    val flashAlpha = remember { Animatable(0f) }
    val shockwaveProgress = remember { Animatable(0f) }
    val shockwaveAlpha = remember { Animatable(0f) }
    val burstProgress = remember { Animatable(0f) }
    val cardScale = remember { Animatable(0.7f) }
    val cardOffsetY = remember { Animatable(120f) }

    fun openVault() {
        if (isOpening || isRevealed) return
        isOpening = true

        coroutineScope.launch {
            GamificationHaptics.playLight(context)

            // Determine Rarity with True Dynamic Probability
            val finalRarity = ChestRarity.rollRarity(initialRarity)
            wonRarity = finalRarity

            // 1. Charge Up, Rapid Roulette Decryption & Tremor
            launch {
                boxScale.animateTo(0.85f, animationSpec = tween(650, easing = FastOutSlowInEasing))
            }
            launch {
                // High frequency vibration tremor + Roulette cycling
                val allTiers = listOf(ChestRarity.COMMON, ChestRarity.RARE, ChestRarity.EPIC, ChestRarity.LEGENDARY)
                var elapsed = 0
                var cycleIdx = 0
                while (elapsed < 650) {
                    boxShake.snapTo((Random.nextFloat() - 0.5f) * 14f)
                    if (elapsed % 70 == 0) {
                        displayedRarity = allTiers[cycleIdx % allTiers.size]
                        cycleIdx++
                        GamificationHaptics.playLight(context)
                    }
                    delay(25)
                    elapsed += 25
                }
                displayedRarity = finalRarity
                boxShake.snapTo(0f)
            }
            
            delay(650)

            // 2. Detonation & Vault Fracture
            if (finalRarity == ChestRarity.LEGENDARY) {
                GamificationHaptics.playCelebration(context)
            } else {
                GamificationHaptics.playSuccess(context)
            }

            launch {
                lidElevation.animateTo(1f, animationSpec = tween(300, easing = FastOutSlowInEasing))
                boxScale.animateTo(0f, animationSpec = tween(250, easing = FastOutLinearInEasing))
            }

            launch {
                flashAlpha.snapTo(1f)
                flashAlpha.animateTo(0f, animationSpec = tween(800, easing = FastOutSlowInEasing))
            }

            launch {
                shockwaveProgress.snapTo(0f)
                shockwaveAlpha.snapTo(1f)
                shockwaveProgress.animateTo(1f, animationSpec = tween(700, easing = FastOutSlowInEasing))
                shockwaveAlpha.animateTo(0f, animationSpec = tween(400, easing = LinearEasing))
            }

            launch {
                burstProgress.snapTo(0f)
                burstProgress.animateTo(1f, animationSpec = tween(900, easing = FastOutSlowInEasing))
            }

            // 3. Calculate Tier-Scaled Rewards with Weighted Chances
            val currentFreezes = AptitudeManager.getStreakFreezesCount()
            val (baseXp, baseGold, freezeChance, maxFreezes) = when (finalRarity) {
                ChestRarity.COMMON -> {
                    val xp = Random.nextInt(120, 260)
                    val gold = Random.nextInt(150, 320)
                    Tuple4(xp, gold, 25, 1) // 25% freeze chance
                }
                ChestRarity.RARE -> {
                    val xp = Random.nextInt(320, 650)
                    val gold = Random.nextInt(400, 800)
                    Tuple4(xp, gold, 60, 1) // 60% freeze chance
                }
                ChestRarity.EPIC -> {
                    val xp = Random.nextInt(800, 1600)
                    val gold = Random.nextInt(1000, 2200)
                    Tuple4(xp, gold, 100, 1) // 100% Guaranteed Freeze
                }
                ChestRarity.LEGENDARY -> {
                    val xp = Random.nextInt(2500, 5000)
                    val gold = Random.nextInt(3000, 6000)
                    Tuple4(xp, gold, 100, 2) // 100% Guaranteed Jackpot (+2 Freezes)
                }
            }

            var freezesAwarded = 0
            var bonusGold = 0
            val wonFreeze = Random.nextInt(1, 101) <= freezeChance

            if (wonFreeze) {
                val availableSlots = (3 - currentFreezes).coerceAtLeast(0)
                if (availableSlots >= maxFreezes) {
                    freezesAwarded = maxFreezes
                    AptitudeManager.addStreakFreezes(freezesAwarded)
                } else if (availableSlots > 0) {
                    freezesAwarded = availableSlots
                    AptitudeManager.addStreakFreezes(freezesAwarded)
                    val overflow = maxFreezes - availableSlots
                    bonusGold = overflow * when (finalRarity) {
                        ChestRarity.LEGENDARY -> 300
                        ChestRarity.EPIC -> 250
                        ChestRarity.RARE -> 175
                        ChestRarity.COMMON -> 100
                    }
                } else {
                    // Freeze inventory already full (3/3)
                    bonusGold = maxFreezes * when (finalRarity) {
                        ChestRarity.LEGENDARY -> 300
                        ChestRarity.EPIC -> 250
                        ChestRarity.RARE -> 175
                        ChestRarity.COMMON -> 100
                    }
                }
            }

            val totalGold = baseGold + bonusGold
            FocusEconomyManager.addExactRewards(exactXp = baseXp, exactGold = totalGold)
            DailyQuestManager.claimMysteryChest()

            val reward = MysteryReward(
                xp = baseXp,
                gold = totalGold,
                streakFreezeAwarded = freezesAwarded > 0,
                bonusGoldInsteadOfFreeze = bonusGold,
                rarityTitle = finalRarity.title,
                freezesCount = if (freezesAwarded > 0) freezesAwarded else 1
            )
            claimedReward = reward

            isRevealed = true

            // 4. Smooth Card Entrance with Spring Physics
            launch {
                cardScale.animateTo(1f, animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow))
            }
            launch {
                cardOffsetY.animateTo(0f, animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow))
            }
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
        val activeRarity = if (isOpening || isRevealed) wonRarity else displayedRarity

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            activeRarity.bgTopColor,
                            activeRarity.bgBottomColor,
                            Color(0xFF030712)
                        ),
                        center = Offset(200f, 300f),
                        radius = 1200f
                    )
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        if (!isOpening && !isRevealed) {
                            openVault()
                        }
                    }
                )
        ) {
            // -----------------------------------------------------------------
            // 0. TIER-SPECIFIC BACKGROUND PHENOMENA (God Rays / Aurora / Nebulas)
            // -----------------------------------------------------------------
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val cx = w / 2
                val cy = h * 0.45f

                // Ambient drifting background stars
                ambientParticles.forEach { p ->
                    val curY = ((p.y + p.speed * System.currentTimeMillis()) % 1f) * h
                    val curX = p.x * w
                    drawCircle(
                        color = Color.White.copy(alpha = p.alpha),
                        radius = p.size.dp.toPx(),
                        center = Offset(curX, curY)
                    )
                }

                // Tier-Specific Luxury Backdrops
                when (activeRarity) {
                    ChestRarity.LEGENDARY -> {
                        // 12 Rotating Solar God Rays
                        rotate(degrees = rayRotation, pivot = Offset(cx, cy)) {
                            val rayCount = 12
                            for (i in 0 until rayCount) {
                                val rayAngle = (i * (360f / rayCount)) * (Math.PI / 180.0)
                                val rayPath = Path().apply {
                                    moveTo(cx, cy)
                                    val r1 = rayAngle - 0.12
                                    val r2 = rayAngle + 0.12
                                    val rayDist = w * 1.5f
                                    lineTo((cx + cos(r1) * rayDist).toFloat(), (cy + sin(r1) * rayDist).toFloat())
                                    lineTo((cx + cos(r2) * rayDist).toFloat(), (cy + sin(r2) * rayDist).toFloat())
                                    close()
                                }
                                drawPath(
                                    path = rayPath,
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFFFFD700).copy(alpha = 0.22f),
                                            Color(0xFFF59E0B).copy(alpha = 0.08f),
                                            Color.Transparent
                                        ),
                                        center = Offset(cx, cy),
                                        radius = w * 0.9f
                                    )
                                )
                            }
                        }
                    }
                    ChestRarity.EPIC -> {
                        // Pulsing Celestial Ring Nebulas
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFE879F9).copy(alpha = 0.28f * corePulse),
                                    Color(0xFF7E22CE).copy(alpha = 0.15f),
                                    Color.Transparent
                                ),
                                center = Offset(cx, cy),
                                radius = w * 0.8f
                            ),
                            radius = w * 0.8f,
                            center = Offset(cx, cy)
                        )
                    }
                    ChestRarity.RARE -> {
                        // Aurora Light Waves
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0xFF818CF8).copy(alpha = 0.18f),
                                    Color.Transparent
                                )
                            ),
                            topLeft = Offset(0f, cy - h * 0.2f),
                            size = Size(w, h * 0.4f)
                        )
                    }
                    ChestRarity.COMMON -> {
                        // Quantum Cyber Grid Glow
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF00F0FF).copy(alpha = 0.16f),
                                    Color.Transparent
                                ),
                                center = Offset(cx, cy),
                                radius = w * 0.65f
                            ),
                            radius = w * 0.65f,
                            center = Offset(cx, cy)
                        )
                    }
                }

                // Shockwave on detonation
                if (shockwaveAlpha.value > 0f) {
                    val maxRadius = w * 0.95f
                    val currentRadius = maxRadius * shockwaveProgress.value
                    drawCircle(
                        color = activeRarity.glowColor.copy(alpha = shockwaveAlpha.value * 0.8f),
                        radius = currentRadius,
                        center = Offset(cx, cy),
                        style = Stroke(width = 8.dp.toPx() * (1f - shockwaveProgress.value))
                    )
                }

                // Particle explosion burst on fracture
                if (burstProgress.value > 0f) {
                    sparkParticles.forEach { p ->
                        val currDist = p.distance * burstProgress.value
                        val px = (cx + cos(p.angle) * currDist).toFloat()
                        val py = (cy + sin(p.angle) * currDist).toFloat()
                        val pAlpha = (1f - burstProgress.value).coerceIn(0f, 1f)
                        drawCircle(
                            color = p.color.copy(alpha = pAlpha),
                            radius = p.size * (1f - burstProgress.value * 0.5f),
                            center = Offset(px, py)
                        )
                    }
                }
            }

            // Whiteout Flash on detonation
            if (flashAlpha.value > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(flashAlpha.value)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color.White,
                                    activeRarity.primaryColor.copy(alpha = 0.6f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            // -----------------------------------------------------------------
            // STAGE 1: 3D ISOMETRIC QUANTUM VAULT (Interactive Chest)
            // -----------------------------------------------------------------
            if (!isRevealed) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Header Subtitle
                    Text(
                        text = if (isOpening) "⚡ DECRYPTING VAULT..." else "MYSTERY QUANTUM VAULT",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.8.sp,
                            fontSize = 12.sp
                        ),
                        color = activeRarity.primaryColor
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (isOpening) "Rolling Odds & Tier Buffs" else "Tap To Unbox Core Rewards",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp,
                            fontSize = 24.sp
                        ),
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Dynamic Rarity & Odds Badge
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = activeRarity.primaryColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.2.dp, activeRarity.primaryColor.copy(alpha = 0.6f)),
                        modifier = Modifier.padding(bottom = 32.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
                        ) {
                            Text(text = activeRarity.iconSymbol, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = activeRarity.badgeLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.5.sp,
                                    fontSize = 11.sp
                                ),
                                color = activeRarity.primaryColor
                            )
                        }
                    }

                    // 3D Vault Box Container with Hover & Shake
                    Box(
                        modifier = Modifier
                            .offset(
                                x = boxShake.value.dp,
                                y = (hoverOffset + if (isOpening) 15f else 0f).dp
                            )
                            .scale(boxScale.value)
                            .size(240.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val cx = w / 2
                            val cy = h / 2

                            // 1. Dynamic Ground Pedestal Shadow
                            val shadowScale = (1f - (hoverOffset / 24f)).coerceIn(0.7f, 1.3f)
                            drawOval(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        activeRarity.primaryColor.copy(alpha = 0.35f * shadowScale),
                                        Color.Transparent
                                    ),
                                    center = Offset(cx, cy + h * 0.48f),
                                    radius = w * 0.45f * shadowScale
                                ),
                                topLeft = Offset(cx - w * 0.38f * shadowScale, cy + h * 0.42f),
                                size = Size(w * 0.76f * shadowScale, h * 0.18f * shadowScale)
                            )

                            // 2. Outer Kinetic Gyro Rings
                            rotate(degrees = gyroRotX, pivot = Offset(cx, cy)) {
                                drawOval(
                                    brush = Brush.sweepGradient(
                                        colors = listOf(
                                            activeRarity.primaryColor.copy(alpha = 0.85f),
                                            Color.Transparent,
                                            activeRarity.secondaryColor.copy(alpha = 0.6f),
                                            activeRarity.primaryColor.copy(alpha = 0.85f)
                                        )
                                    ),
                                    topLeft = Offset(cx - w * 0.48f, cy - h * 0.26f),
                                    size = Size(w * 0.96f, h * 0.52f),
                                    style = Stroke(width = 2.dp.toPx())
                                )
                            }

                            rotate(degrees = gyroRotY, pivot = Offset(cx, cy)) {
                                drawOval(
                                    brush = Brush.sweepGradient(
                                        colors = listOf(
                                            activeRarity.secondaryColor.copy(alpha = 0.8f),
                                            Color.Transparent,
                                            activeRarity.primaryColor.copy(alpha = 0.5f),
                                            activeRarity.secondaryColor.copy(alpha = 0.8f)
                                        )
                                    ),
                                    topLeft = Offset(cx - w * 0.28f, cy - h * 0.46f),
                                    size = Size(w * 0.56f, h * 0.92f),
                                    style = Stroke(width = 2.dp.toPx())
                                )
                            }

                            // 3. Central Ambient Flare
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        activeRarity.primaryColor.copy(alpha = 0.45f * corePulse),
                                        Color.Transparent
                                    ),
                                    center = Offset(cx, cy),
                                    radius = w * 0.48f
                                ),
                                radius = w * 0.48f,
                                center = Offset(cx, cy)
                            )

                            // 4. 3D Isometric Obsidian Vault Body
                            val lidOffsetPx = lidElevation.value * 40.dp.toPx()

                            val topPath = Path().apply {
                                moveTo(cx, cy - h * 0.38f - lidOffsetPx)
                                lineTo(cx + w * 0.38f, cy - h * 0.14f - lidOffsetPx)
                                lineTo(cx, cy + h * 0.10f - lidOffsetPx)
                                lineTo(cx - w * 0.38f, cy - h * 0.14f - lidOffsetPx)
                                close()
                            }

                            val leftPath = Path().apply {
                                moveTo(cx - w * 0.38f, cy - h * 0.14f)
                                lineTo(cx, cy + h * 0.10f)
                                lineTo(cx, cy + h * 0.44f)
                                lineTo(cx - w * 0.38f, cy + h * 0.20f)
                                close()
                            }

                            val rightPath = Path().apply {
                                moveTo(cx, cy + h * 0.10f)
                                lineTo(cx + w * 0.38f, cy - h * 0.14f)
                                lineTo(cx + w * 0.38f, cy + h * 0.20f)
                                lineTo(cx, cy + h * 0.44f)
                                close()
                            }

                            // Facet Gradient Fills
                            drawPath(
                                topPath,
                                brush = Brush.linearGradient(
                                    colors = listOf(activeRarity.primaryColor, activeRarity.secondaryColor)
                                )
                            )
                            drawPath(
                                leftPath,
                                brush = Brush.linearGradient(
                                    colors = listOf(activeRarity.secondaryColor, Color(0xFF0B132B))
                                )
                            )
                            drawPath(
                                rightPath,
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFF0F172A), Color(0xFF020617))
                                )
                            )

                            // High-Tech Cybernetic Wireframe Highlights
                            drawPath(topPath, Color.White.copy(alpha = 0.75f), style = Stroke(width = 2.dp.toPx()))
                            drawPath(leftPath, Color.White.copy(alpha = 0.4f), style = Stroke(width = 1.5.dp.toPx()))
                            drawPath(rightPath, Color.White.copy(alpha = 0.4f), style = Stroke(width = 1.5.dp.toPx()))

                            // Laser Seam Lines
                            drawLine(
                                color = activeRarity.primaryColor,
                                start = Offset(cx, cy - h * 0.14f - lidOffsetPx),
                                end = Offset(cx, cy + h * 0.10f - lidOffsetPx),
                                strokeWidth = 2.dp.toPx()
                            )

                            // Glowing Reactor Node at center
                            drawCircle(
                                color = Color.White,
                                radius = 4.dp.toPx(),
                                center = Offset(cx, cy + h * 0.10f - lidOffsetPx)
                            )
                            drawCircle(
                                color = activeRarity.primaryColor,
                                radius = 9.dp.toPx() * corePulse,
                                center = Offset(cx, cy + h * 0.10f - lidOffsetPx),
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Bottom Call-to-action
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        Text(
                            text = if (isOpening) "DECRYPTING QUANTUM CORE..." else "TAP ANYWHERE TO UNBOX",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.5.sp,
                                fontSize = 14.sp
                            ),
                            color = if (isOpening) activeRarity.primaryColor else Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Weighted Odds: Common 55% • Rare 28% • Epic 13% • Legendary 4%",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.5.sp,
                                letterSpacing = 0.4.sp
                            ),
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }

            // -----------------------------------------------------------------
            // STAGE 2: VAULT REVEALED - LUXURY GLASS REWARD PEDESTAL
            // -----------------------------------------------------------------
            AnimatedVisibility(
                visible = isRevealed,
                enter = fadeIn(animationSpec = tween(400)),
                exit = fadeOut(animationSpec = tween(200)),
                modifier = Modifier.align(Alignment.Center)
            ) {
                claimedReward?.let { reward ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth(0.88f)
                            .offset(y = cardOffsetY.value.dp)
                            .scale(cardScale.value),
                        shape = RoundedCornerShape(28.dp),
                        color = Color(0xFF0C1322),
                        border = BorderStroke(
                            1.5.dp,
                            Brush.sweepGradient(
                                colors = listOf(
                                    wonRarity.primaryColor.copy(alpha = 0.9f),
                                    wonRarity.secondaryColor.copy(alpha = 0.5f),
                                    Color.White.copy(alpha = 0.8f),
                                    wonRarity.primaryColor.copy(alpha = 0.9f)
                                )
                            )
                        ),
                        shadowElevation = 24.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFF131D36),
                                            Color(0xFF090E1A)
                                        )
                                    )
                                )
                                .padding(28.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Decrypted Rarity Badge
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = wonRarity.primaryColor.copy(alpha = 0.15f),
                                    border = BorderStroke(1.2.dp, wonRarity.primaryColor.copy(alpha = 0.6f))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Text(text = wonRarity.iconSymbol, fontSize = 13.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = wonRarity.badgeLabel,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Black,
                                                letterSpacing = 1.5.sp,
                                                fontSize = 11.sp
                                            ),
                                            color = wonRarity.primaryColor
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = if (wonRarity == ChestRarity.LEGENDARY) "Legendary Jackpot!" else "Rewards Claimed!",
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 24.sp,
                                        letterSpacing = (-0.5).sp
                                    ),
                                    color = if (wonRarity == ChestRarity.LEGENDARY) Color(0xFFFFD700) else Color.White
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                // Dual Stat Cards (XP & Gold)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    ModernRewardCard(
                                        modifier = Modifier.weight(1f),
                                        label = "XP GAINED",
                                        value = "+${reward.xp}",
                                        accentColor = Color(0xFF818CF8),
                                        iconEmoji = "⚡"
                                    )

                                    ModernRewardCard(
                                        modifier = Modifier.weight(1f),
                                        label = "GOLD COINS",
                                        value = "+${reward.gold}",
                                        accentColor = Color(0xFFFBBF24),
                                        iconEmoji = "🪙"
                                    )
                                }

                                // Streak Freeze Drop
                                if (reward.streakFreezeAwarded) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color(0xFF10B981).copy(alpha = 0.12f),
                                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                        ) {
                                            Text(
                                                text = if (reward.freezesCount > 1) "👑" else "🛡️",
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = if (reward.freezesCount > 1) "+${reward.freezesCount} STREAK FREEZES (JACKPOT!)" else "+1 STREAK FREEZE",
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        fontWeight = FontWeight.Black,
                                                        letterSpacing = 1.sp
                                                    ),
                                                    color = Color(0xFF34D399)
                                                )
                                                Text(
                                                    text = "Protects your streak from missed days",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                                                    color = Color(0xFF94A3B8)
                                                )
                                            }
                                        }
                                    }
                                } else if (reward.bonusGoldInsteadOfFreeze > 0) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color(0xFFF59E0B).copy(alpha = 0.12f),
                                        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.35f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                        ) {
                                            Text(text = "💎", style = MaterialTheme.typography.titleMedium)
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = "+${reward.bonusGoldInsteadOfFreeze} MAX VAULT BONUS",
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        fontWeight = FontWeight.Black,
                                                        letterSpacing = 1.sp
                                                    ),
                                                    color = Color(0xFFFBBF24)
                                                )
                                                Text(
                                                    text = "Streak freezes full (3/3) - Converted to gold",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                                                    color = Color(0xFF94A3B8)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(28.dp))

                                // Claim & Continue Action Button
                                Button(
                                    onClick = {
                                        GamificationHaptics.playLight(context)
                                        claimedReward?.let { onClaimed?.invoke(it) }
                                        onDismiss()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Transparent
                                    ),
                                    contentPadding = PaddingValues()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.horizontalGradient(
                                                    colors = listOf(
                                                        wonRarity.primaryColor,
                                                        wonRarity.secondaryColor
                                                    )
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "COLLECT ALL REWARDS",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Black,
                                                letterSpacing = 1.2.sp,
                                                fontSize = 13.sp
                                            ),
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class Tuple4<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun ModernRewardCard(
    label: String,
    value: String,
    accentColor: Color,
    iconEmoji: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF0F182A),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = iconEmoji, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp,
                    fontSize = 20.sp
                ),
                color = accentColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    fontSize = 10.sp
                ),
                color = Color(0xFF94A3B8)
            )
        }
    }
}
