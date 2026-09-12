package com.focusbyrj.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
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

/**
 * Premium AAA Mystery Box & Chest Reward Experience.
 * Features:
 * - Fluid spring squash-and-stretch physics on every tap
 * - Multi-stage tension building with glowing seams, chest rumble, and explosion burst
 * - Rotating celestial god-ray sunburst with floating stardust motes
 * - 3D shaded vector chest model with ornate gold bevels, rivets, and glowing gemstone core
 * - Dynamic counting-up rewards (Gold, XP) with tactile haptics
 * - Celebratory confetti rain and animated XP potion beaker with bubbling physics
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
    var chancesLeft by remember { mutableStateOf(3) } // 3 -> 2 -> 1 -> 0 (Opening)
    var isUpgrading by remember { mutableStateOf(false) }
    var isRevealed by remember { mutableStateOf(false) }
    var upgradeSuccessNotice by remember { mutableStateOf(false) }
    var claimedReward by remember { mutableStateOf<MysteryReward?>(null) }

    // Physics animations
    val tapScaleX = remember { Animatable(1f) }
    val tapScaleY = remember { Animatable(1f) }
    val chestWobble = remember { Animatable(0f) }
    val chestElevation = remember { Animatable(0f) }
    val chestPitchX = remember { Animatable(0f) }
    val chestYawY = remember { Animatable(0f) }
    val chestRevealOffsetY = remember { Animatable(0f) }
    val chestRevealScale = remember { Animatable(1f) }
    val lidOpenAnim = remember { Animatable(0f) }
    val crackGlowAnim = remember { Animatable(0f) }
    val shockwaveRadius = remember { Animatable(0f) }
    val shockwaveAlpha = remember { Animatable(0f) }
    val secondaryShockwaveRadius = remember { Animatable(0f) }
    val secondaryShockwaveAlpha = remember { Animatable(0f) }
    val screenShakeX = remember { Animatable(0f) }
    val screenShakeY = remember { Animatable(0f) }
    val flashAlpha = remember { Animatable(0f) }
    val hudAlpha = remember { Animatable(1f) }

    // Gravitational Implosion (Tension charge-up)
    val implosionProgress = remember { Animatable(0f) }
    val implosionAlpha = remember { Animatable(0f) }

    // Vault Articulated Mechanical Transformation
    val hullExpandAnim = remember { Animatable(0f) }
    val reactorEjectionAnim = remember { Animatable(0f) }
    val beamIntensityAnim = remember { Animatable(0f) }

    // Burst & Spark Particles
    val tapSparksProgress = remember { Animatable(0f) }
    val tapSparksAlpha = remember { Animatable(0f) }
    val burstParticlesProgress = remember { Animatable(0f) }
    val burstParticlesAlpha = remember { Animatable(0f) }
    val coinBurstProgress = remember { Animatable(0f) }
    val coinBurstAlpha = remember { Animatable(0f) }
    val quantumShardBurstProgress = remember { Animatable(0f) }
    val quantumShardBurstAlpha = remember { Animatable(0f) }

    // Reward screen animations
    val rewardCardOffsetY = remember { Animatable(110f) }
    val rewardCardScale = remember { Animatable(0.4f) }
    val rewardCardAlpha = remember { Animatable(0f) }
    val animatedGoldCount = remember { Animatable(0f) }
    val animatedXpCount = remember { Animatable(0f) }

    // Ambient background infinite loops
    val infiniteTransition = rememberInfiniteTransition(label = "ambient_celestial")
    val sunburstRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(28000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sunburst_spin"
    )
    val ambientPulse by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambient_pulse"
    )
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float_offset"
    )
    val ambientYaw by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambient_yaw"
    )
    val ambientPitch by infiniteTransition.animateFloat(
        initialValue = -1.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambient_pitch"
    )

    // Confetti particles state
    val confettiList = remember {
        List(40) {
            ConfettiParticle(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                size = Random.nextFloat() * 10f + 6f,
                speedY = Random.nextFloat() * 1.5f + 0.8f,
                speedX = (Random.nextFloat() - 0.5f) * 1.2f,
                color = listOf(
                    Color(0xFFFFD700), Color(0xFF38BDF8), Color(0xFFF472B6),
                    Color(0xFF4ADE80), Color(0xFFA78BFA), Color(0xFFFB923C)
                ).random(),
                rotation = Random.nextFloat() * 360f,
                rotationSpeed = (Random.nextFloat() - 0.5f) * 8f
            )
        }
    }

    // Dynamic Tap Sparks
    val tapSparksList = remember {
        List(22) {
            val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
            val speed = Random.nextFloat() * 200f + 80f
            ParticleSparkData(
                vx = cos(angle) * speed,
                vy = sin(angle) * speed - 45f,
                size = Random.nextFloat() * 7f + 3.5f,
                color = listOf(Color(0xFFFFF59D), Color(0xFFFFD54F), Color(0xFFFFB300), Color(0xFF38BDF8), Color.White).random(),
                rotSpeed = (Random.nextFloat() - 0.5f) * 360f
            )
        }
    }

    // Pre-burst Gravitational Implosion Motes (Inward rushing power)
    val implosionMotesList = remember {
        List(32) { idx ->
            val angle = (idx.toFloat() / 32f) * 2f * Math.PI.toFloat() + (Random.nextFloat() - 0.5f) * 0.2f
            ImplosionMoteData(
                angle = angle,
                distance = Random.nextFloat() * 140f + 160f,
                size = Random.nextFloat() * 4.5f + 2.5f,
                color = listOf(Color(0xFF00E5FF), Color(0xFF38BDF8), Color(0xFFE0F2FE), Color.White).random()
            )
        }
    }

    // Grand Burst Fountain Particles
    val burstParticlesList = remember {
        List(56) {
            val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
            val speed = Random.nextFloat() * 380f + 180f
            ParticleSparkData(
                vx = cos(angle) * speed,
                vy = sin(angle) * speed - 240f,
                size = Random.nextFloat() * 12f + 6f,
                color = listOf(
                    Color(0xFFFFD700), Color(0xFFFFEA00), Color(0xFFFFA000),
                    Color(0xFF38BDF8), Color(0xFF00E5FF), Color(0xFFF472B6), Color(0xFFA78BFA),
                    Color(0xFFFFFFFF)
                ).random(),
                rotSpeed = (Random.nextFloat() - 0.5f) * 720f
            )
        }
    }

    // 3D Tumbling Coin Fountain Burst Particles
    val coinBurstList = remember {
        List(28) { idx ->
            val angle = (idx.toFloat() / 28f) * 2f * Math.PI.toFloat() + (Random.nextFloat() - 0.5f) * 0.35f
            val speed = Random.nextFloat() * 320f + 180f
            CoinParticleData(
                vx = cos(angle) * speed,
                vy = sin(angle) * speed - 280f,
                radius = Random.nextFloat() * 5f + 9f,
                rotSpeed = (Random.nextFloat() - 0.5f) * 720f,
                tumbleSpeed = Random.nextFloat() * 8f + 5f,
                color = if (Random.nextBoolean()) Color(0xFFFFD700) else Color(0xFFFFEA00)
            )
        }
    }

    // High-Velocity Faceted Quantum Crystal Shards
    val quantumShardList = remember {
        List(32) {
            val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
            val speed = Random.nextFloat() * 360f + 190f
            CyberShardData(
                vx = cos(angle) * speed,
                vy = sin(angle) * speed - 220f,
                size = Random.nextFloat() * 14f + 7f,
                color = listOf(
                    Color(0xFF00E5FF), Color(0xFF38BDF8), Color(0xFFA78BFA),
                    Color(0xFFF472B6), Color(0xFFFFD700), Color.White
                ).random(),
                rotSpeed = (Random.nextFloat() - 0.5f) * 900f,
                isGem = Random.nextBoolean()
            )
        }
    }

    // Trigger tap action
    fun performTapChance() {
        if (isUpgrading || isRevealed || chancesLeft <= 0) return

        coroutineScope.launch {
            isUpgrading = true
            GamificationHaptics.playLight(context)

            // Trigger ground shockwave ring
            launch {
                shockwaveRadius.snapTo(0.2f)
                shockwaveAlpha.snapTo(0.85f)
                shockwaveRadius.animateTo(1.4f, animationSpec = tween(400, easing = FastOutSlowInEasing))
                shockwaveAlpha.animateTo(0f, animationSpec = tween(200))
            }

            // Burst tap sparks from keyhole
            launch {
                tapSparksProgress.snapTo(0f)
                tapSparksAlpha.snapTo(1f)
                tapSparksProgress.animateTo(1f, animationSpec = tween(380, easing = LinearOutSlowInEasing))
                tapSparksAlpha.animateTo(0f, animationSpec = tween(150))
            }

            // Tactile 3D Physics Squash, Stretch, Pitch and Yaw
            launch {
                // 1. Squash compression & 3D tilt forward
                tapScaleX.animateTo(1.15f, animationSpec = tween(85, easing = FastOutSlowInEasing))
                tapScaleY.animateTo(0.86f, animationSpec = tween(85, easing = FastOutSlowInEasing))
                chestElevation.animateTo(8f, animationSpec = tween(85, easing = FastOutSlowInEasing))
                chestPitchX.animateTo(11f, animationSpec = tween(85, easing = FastOutSlowInEasing))
                chestYawY.animateTo(14f, animationSpec = tween(85, easing = FastOutLinearInEasing))

                // 2. Elastic rebound with 3D overshoot
                tapScaleX.animateTo(0.94f, animationSpec = spring(dampingRatio = 0.52f, stiffness = Spring.StiffnessMediumLow))
                tapScaleY.animateTo(1.07f, animationSpec = spring(dampingRatio = 0.52f, stiffness = Spring.StiffnessMediumLow))
                chestElevation.animateTo(-5f, animationSpec = spring(dampingRatio = 0.52f, stiffness = Spring.StiffnessMediumLow))
                chestPitchX.animateTo(-5f, animationSpec = spring(dampingRatio = 0.52f, stiffness = Spring.StiffnessMediumLow))
                chestYawY.animateTo(-9f, animationSpec = tween(90, easing = LinearOutSlowInEasing))

                // 3. Equilibrium settlement
                tapScaleX.animateTo(1f, animationSpec = spring(dampingRatio = 0.70f, stiffness = Spring.StiffnessLow))
                tapScaleY.animateTo(1f, animationSpec = spring(dampingRatio = 0.70f, stiffness = Spring.StiffnessLow))
                chestElevation.animateTo(0f, animationSpec = spring(dampingRatio = 0.70f, stiffness = Spring.StiffnessLow))
                chestPitchX.animateTo(0f, animationSpec = spring(dampingRatio = 0.70f, stiffness = Spring.StiffnessLow))
                chestYawY.animateTo(0f, animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium))
            }

            // Damped Sinusoidal Wobble recoil
            launch {
                chestWobble.animateTo(-12f, animationSpec = tween(65, easing = FastOutLinearInEasing))
                chestWobble.animateTo(9.5f, animationSpec = tween(75, easing = LinearOutSlowInEasing))
                chestWobble.animateTo(-5f, animationSpec = tween(70))
                chestWobble.animateTo(2f, animationSpec = tween(60))
                chestWobble.animateTo(0f, animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium))
            }

            delay(140)

            // Upgrade probabilities
            val upgradeChance = when (currentRarity) {
                ChestRarity.COMMON -> 0.60f
                ChestRarity.RARE -> 0.35f
                ChestRarity.EPIC -> 0.12f
                ChestRarity.LEGENDARY -> 0.0f
            }

            val didUpgrade = currentRarity != ChestRarity.LEGENDARY && Random.nextFloat() < upgradeChance
            if (didUpgrade) {
                currentRarity = currentRarity.nextTier()
                upgradeSuccessNotice = true
                GamificationHaptics.playSuccess(context)

                // Athletic celebration hop with 3D spin accent
                launch {
                    chestElevation.animateTo(-34f, animationSpec = tween(170, easing = FastOutSlowInEasing))
                    chestElevation.animateTo(0f, animationSpec = spring(dampingRatio = 0.52f, stiffness = Spring.StiffnessMediumLow))
                }
                launch {
                    chestYawY.animateTo(22f, animationSpec = tween(140, easing = FastOutSlowInEasing))
                    chestYawY.animateTo(0f, animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow))
                }

                // Dramatic golden screen flash on tier upgrade
                launch {
                    flashAlpha.snapTo(0.48f)
                    flashAlpha.animateTo(0f, animationSpec = tween(380, easing = FastOutSlowInEasing))
                }

                delay(500)
                upgradeSuccessNotice = false
            }

            val nextChances = chancesLeft - 1
            chancesLeft = nextChances

            if (nextChances == 0) {
                // ==================== MULTI-STAGE SUSPENSE & SUPERLUMINAL BURST OPENING ====================
                GamificationHaptics.playCelebration(context)

                // 1. TENSION CHARGE & LEVITATION LIFT-OFF (HUD fades, anti-gravity rise, vacuum implosion)
                launch {
                    hudAlpha.animateTo(0f, animationSpec = tween(220))
                }
                launch {
                    // Smooth anti-gravity elevation rise with charging energy squash-stretch
                    chestElevation.animateTo(-16f, animationSpec = tween(380, easing = FastOutSlowInEasing))
                    tapScaleX.animateTo(1.08f, animationSpec = tween(190, easing = FastOutSlowInEasing))
                    tapScaleY.animateTo(0.92f, animationSpec = tween(190, easing = FastOutSlowInEasing))
                    tapScaleX.animateTo(0.95f, animationSpec = tween(190, easing = FastOutSlowInEasing))
                    tapScaleY.animateTo(1.08f, animationSpec = tween(190, easing = FastOutSlowInEasing))
                }
                // Gravitational Implosion: Quantum particles sucked inward into the core
                launch {
                    implosionAlpha.snapTo(1f)
                    implosionProgress.animateTo(1f, animationSpec = tween(380, easing = FastOutLinearInEasing))
                    implosionAlpha.animateTo(0f, animationSpec = tween(50))
                }
                // Escalating Harmonic Vibration (frequency rises from 36ms to 16ms per oscillation)
                launch {
                    val tremorPattern = listOf(
                        Triple(4f, 4f, 36),
                        Triple(-6f, -5f, 32),
                        Triple(8f, 6f, 28),
                        Triple(-11f, -8f, 26),
                        Triple(13f, 9f, 24),
                        Triple(-15f, -10f, 22),
                        Triple(17f, 11f, 20),
                        Triple(-18f, -12f, 18),
                        Triple(19f, 13f, 16),
                        Triple(0f, 0f, 20)
                    )
                    tremorPattern.forEach { (wobble, pitch, dur) ->
                        launch { chestYawY.animateTo(wobble * 0.7f, animationSpec = tween(dur)) }
                        launch { chestPitchX.animateTo(pitch, animationSpec = tween(dur)) }
                        chestWobble.animateTo(wobble, animationSpec = tween(dur))
                    }
                    chestWobble.animateTo(0f, animationSpec = spring())
                    chestYawY.animateTo(0f, animationSpec = spring())
                    chestPitchX.animateTo(0f, animationSpec = spring())
                }
                // Strobe laser crack glow across all chassis seams
                launch {
                    crackGlowAnim.animateTo(0.35f, animationSpec = tween(70))
                    crackGlowAnim.animateTo(0.20f, animationSpec = tween(40))
                    crackGlowAnim.animateTo(0.75f, animationSpec = tween(90))
                    crackGlowAnim.animateTo(0.55f, animationSpec = tween(40))
                    crackGlowAnim.animateTo(1.25f, animationSpec = tween(120))
                }

                delay(390)

                // 2. BREATHLESS SUSPENSE PAUSE / FREEZE FRAME (Pure tension)
                delay(40)

                // 3. THE SUPERLUMINAL DETONATION & HYDRAULIC RECOIL
                GamificationHaptics.playCelebration(context)

                // Blinding Screen Flash
                launch {
                    flashAlpha.snapTo(0.92f)
                    flashAlpha.animateTo(0f, animationSpec = tween(520, easing = FastOutSlowInEasing))
                }

                // Camera & Stage Micro-Shake Recoil
                launch {
                    val shakes = listOf(
                        Pair(-12f, 8f), Pair(11f, -9f), Pair(-8f, 6f), Pair(6f, -5f),
                        Pair(-4f, 3f), Pair(2f, -2f), Pair(0f, 0f)
                    )
                    shakes.forEach { (sx, sy) ->
                        launch { screenShakeX.animateTo(sx, animationSpec = tween(26)) }
                        screenShakeY.animateTo(sy, animationSpec = tween(26))
                    }
                }

                // Cascading Dual Shockwaves
                launch {
                    shockwaveRadius.snapTo(0.1f)
                    shockwaveAlpha.snapTo(1f)
                    shockwaveRadius.animateTo(2.3f, animationSpec = tween(550, easing = FastOutSlowInEasing))
                    shockwaveAlpha.animateTo(0f, animationSpec = tween(200))
                }
                launch {
                    delay(75)
                    secondaryShockwaveRadius.snapTo(0.1f)
                    secondaryShockwaveAlpha.snapTo(0.85f)
                    secondaryShockwaveRadius.animateTo(1.85f, animationSpec = tween(500, easing = FastOutSlowInEasing))
                    secondaryShockwaveAlpha.animateTo(0f, animationSpec = tween(180))
                }

                // Upward Vault Recoil Launch
                launch {
                    chestElevation.animateTo(-38f, animationSpec = tween(130, easing = FastOutLinearInEasing))
                    chestElevation.animateTo(0f, animationSpec = spring(dampingRatio = 0.54f, stiffness = Spring.StiffnessMediumLow))
                }
                launch {
                    chestPitchX.animateTo(-15f, animationSpec = tween(160, easing = FastOutSlowInEasing))
                    chestPitchX.animateTo(-2f, animationSpec = spring(dampingRatio = 0.60f, stiffness = Spring.StiffnessMediumLow))
                }
                launch {
                    tapScaleX.animateTo(0.88f, animationSpec = spring(dampingRatio = 0.50f, stiffness = Spring.StiffnessMedium))
                    tapScaleY.animateTo(1.15f, animationSpec = spring(dampingRatio = 0.50f, stiffness = Spring.StiffnessMedium))
                    tapScaleX.animateTo(1f, animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessLow))
                    tapScaleY.animateTo(1f, animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessLow))
                }

                // Mechanical Transformations: Hull expands, Reactor Core ejects, Beam Intensifies
                launch {
                    hullExpandAnim.animateTo(1f, animationSpec = spring(dampingRatio = 0.58f, stiffness = Spring.StiffnessMediumLow))
                }
                launch {
                    reactorEjectionAnim.animateTo(1f, animationSpec = spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow))
                }
                launch {
                    beamIntensityAnim.animateTo(1.25f, animationSpec = tween(220, easing = FastOutSlowInEasing))
                    beamIntensityAnim.animateTo(1.0f, animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessLow))
                }

                // Canopy Flings Open with Dramatic Hydraulic Overshoot & Hinge Recoil
                launch {
                    lidOpenAnim.animateTo(
                        targetValue = 1.28f,
                        animationSpec = tween(240, easing = FastOutSlowInEasing)
                    )
                    lidOpenAnim.animateTo(
                        targetValue = 1.0f,
                        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow)
                    )
                }

                // 4. Multi-Tier Particle Fireworks: Sparks, Coins & Flying Quantum Shards
                launch {
                    burstParticlesProgress.snapTo(0f)
                    burstParticlesAlpha.snapTo(1f)
                    burstParticlesProgress.animateTo(1f, animationSpec = tween(1250, easing = LinearOutSlowInEasing))
                    burstParticlesAlpha.animateTo(0f, animationSpec = tween(350))
                }
                launch {
                    coinBurstProgress.snapTo(0f)
                    coinBurstAlpha.snapTo(1f)
                    coinBurstProgress.animateTo(1f, animationSpec = tween(1150, easing = LinearOutSlowInEasing))
                    coinBurstAlpha.animateTo(0f, animationSpec = tween(300))
                }
                launch {
                    quantumShardBurstProgress.snapTo(0f)
                    quantumShardBurstAlpha.snapTo(1f)
                    quantumShardBurstProgress.animateTo(1f, animationSpec = tween(1350, easing = LinearOutSlowInEasing))
                    quantumShardBurstAlpha.animateTo(0f, animationSpec = tween(380))
                }

                // Reward Calculation
                val currentTotalXp = maxOf(0, AptitudeManager.profileFlow.value.xp)
                var xpEarned = 0
                var goldEarned = 0
                var freezeAwarded = false
                var bonusGold = 0
                val currentFreezes = AptitudeManager.getStreakFreezesCount()

                when (currentRarity) {
                    ChestRarity.COMMON -> {
                        xpEarned = 0
                        goldEarned = 50
                        AptitudeManager.activateXpBoost(durationMinutes = 15, multiplier = 2.0f)
                    }
                    ChestRarity.RARE -> {
                        xpEarned = maxOf(100, (currentTotalXp * 0.02f).toInt())
                        goldEarned = 250
                        if (Random.nextFloat() < 0.15f) {
                            if (currentFreezes < 3) {
                                AptitudeManager.addStreakFreezes(1)
                                freezeAwarded = true
                            } else {
                                bonusGold = 100
                            }
                        }
                    }
                    ChestRarity.EPIC -> {
                        xpEarned = maxOf(250, (currentTotalXp * 0.05f).toInt())
                        goldEarned = 1000
                        if (currentFreezes < 3) {
                            AptitudeManager.addStreakFreezes(1)
                            freezeAwarded = true
                        } else {
                            bonusGold = 250
                        }
                    }
                    ChestRarity.LEGENDARY -> {
                        xpEarned = maxOf(1000, (currentTotalXp * 0.10f).toInt())
                        goldEarned = 5000
                        AptitudeManager.activateXpBoost(durationMinutes = 30, multiplier = 2.0f)
                        if (currentFreezes < 3) {
                            AptitudeManager.addStreakFreezes(1)
                            freezeAwarded = true
                        } else {
                            bonusGold = 500
                        }
                    }
                }

                val finalGold = goldEarned + bonusGold
                FocusEconomyManager.addExactRewards(exactXp = xpEarned, exactGold = finalGold)
                DailyQuestManager.claimMysteryChest()

                val resultReward = MysteryReward(
                    xp = xpEarned,
                    gold = finalGold,
                    streakFreezeAwarded = freezeAwarded,
                    bonusGoldInsteadOfFreeze = bonusGold
                )
                claimedReward = resultReward

                // 5. SEAMLESS REWARD ASCENT: Opened chest slides into lower anchor while reward rises out of chest
                isRevealed = true
                launch {
                    chestRevealOffsetY.animateTo(125f, animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow))
                }
                launch {
                    chestRevealScale.animateTo(0.78f, animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow))
                }

                // Reward Card Emergence
                launch {
                    rewardCardOffsetY.snapTo(100f)
                    rewardCardScale.snapTo(0.35f)
                    rewardCardAlpha.snapTo(0f)
                    rewardCardAlpha.animateTo(1f, animationSpec = tween(320))
                }
                launch {
                    rewardCardOffsetY.animateTo(-24f, animationSpec = spring(dampingRatio = 0.68f, stiffness = Spring.StiffnessMediumLow))
                }
                launch {
                    rewardCardScale.animateTo(1f, animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow))
                }

                // Rolling number counters
                launch {
                    animatedGoldCount.animateTo(finalGold.toFloat(), animationSpec = tween(1100, easing = FastOutSlowInEasing))
                }
                launch {
                    animatedXpCount.animateTo(xpEarned.toFloat(), animationSpec = tween(1100, easing = FastOutSlowInEasing))
                }
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
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF1E293B),
                                Color(0xFF0F172A),
                                Color(0xFF020617)
                            ),
                            center = Offset.Unspecified,
                            radius = 1200f
                        )
                    } else {
                        Brush.radialGradient(
                            colors = listOf(
                                currentRarity.bgTopColor,
                                currentRarity.bgBottomColor,
                                Color(0xFF020617)
                            ),
                            center = Offset.Unspecified,
                            radius = 1100f
                        )
                    }
                )
        ) {
            // 1. QUANTUM ORBITAL WARP FIELD & CYBER PARTICLES
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val centerOffset = Offset(w * 0.5f, h * 0.44f)

                // High-Tech Ambient Radial Depth Glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            currentRarity.glowColor.copy(alpha = if (isRevealed) 0.22f else 0.16f * ambientPulse),
                            currentRarity.primaryColor.copy(alpha = 0.06f),
                            Color.Transparent
                        ),
                        center = centerOffset,
                        radius = maxOf(w, h) * 0.65f
                    ),
                    radius = maxOf(w, h) * 0.65f,
                    center = centerOffset
                )

                // Rotating Orbital Rings & Tech Crosshairs
                rotate(degrees = sunburstRotation * 0.35f, pivot = centerOffset) {
                    val outerOrbitR = w * 0.48f
                    val midOrbitR = w * 0.38f
                    val innerOrbitR = w * 0.26f

                    // Outer Orbit with Cardinal Tech Notches
                    drawCircle(
                        color = currentRarity.primaryColor.copy(alpha = 0.18f),
                        radius = outerOrbitR,
                        center = centerOffset,
                        style = Stroke(width = 1.2.dp.toPx())
                    )
                    // Radial Tech Hash Ticks around outer orbit
                    for (deg in 0 until 360 step 30) {
                        val rad = Math.toRadians(deg.toDouble()).toFloat()
                        val tickLen = if (deg % 90 == 0) 12.dp.toPx() else 6.dp.toPx()
                        val c = cos(rad)
                        val s = sin(rad)
                        drawLine(
                            color = currentRarity.primaryColor.copy(alpha = if (deg % 90 == 0) 0.45f else 0.20f),
                            start = Offset(centerOffset.x + (outerOrbitR - tickLen) * c, centerOffset.y + (outerOrbitR - tickLen) * s),
                            end = Offset(centerOffset.x + (outerOrbitR + tickLen) * c, centerOffset.y + (outerOrbitR + tickLen) * s),
                            strokeWidth = if (deg % 90 == 0) 2.dp.toPx() else 1.dp.toPx()
                        )
                    }

                    // Mid Orbit (Reverse Spin effect)
                    drawCircle(
                        color = currentRarity.glowColor.copy(alpha = 0.14f * ambientPulse),
                        radius = midOrbitR,
                        center = centerOffset,
                        style = Stroke(width = 1.dp.toPx())
                    )

                    // Inner Energy Containment Halo
                    drawCircle(
                        color = currentRarity.primaryColor.copy(alpha = 0.22f * ambientPulse),
                        radius = innerOrbitR,
                        center = centerOffset,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }

                // Cinematic Ethereal Laser Light Fans (Volumetric Cyber Rays)
                rotate(degrees = sunburstRotation * 0.18f, pivot = centerOffset) {
                    val rayCount = 8
                    val angleStep = 360f / rayCount
                    val beamColor = if (isRevealed) {
                        Color(0xFFFFD54F).copy(alpha = 0.08f)
                    } else {
                        currentRarity.glowColor.copy(alpha = 0.07f * ambientPulse)
                    }

                    for (i in 0 until rayCount) {
                        val startAngle = i * angleStep
                        val rayPath = Path().apply {
                            moveTo(centerOffset.x, centerOffset.y)
                            val rad1 = Math.toRadians((startAngle - 6).toDouble())
                            val rad2 = Math.toRadians((startAngle + 6).toDouble())
                            val maxR = maxOf(w, h) * 1.2f
                            lineTo(centerOffset.x + (maxR * cos(rad1)).toFloat(), centerOffset.y + (maxR * sin(rad1)).toFloat())
                            lineTo(centerOffset.x + (maxR * cos(rad2)).toFloat(), centerOffset.y + (maxR * sin(rad2)).toFloat())
                            close()
                        }
                        drawPath(rayPath, color = beamColor)
                    }
                }

                // Ambient Floating Cyber Data Motes & Diamond Glints
                val starColor = Color.White.copy(alpha = 0.40f * ambientPulse)
                drawSparkleStar(Offset(w * 0.12f, h * 0.16f + floatOffset), 15.dp.toPx(), starColor)
                drawSparkleStar(Offset(w * 0.88f, h * 0.20f - floatOffset), 16.dp.toPx(), starColor)
                drawSparkleStar(Offset(w * 0.15f, h * 0.68f - floatOffset * 0.8f), 13.dp.toPx(), starColor)
                drawSparkleStar(Offset(w * 0.85f, h * 0.72f + floatOffset * 0.8f), 14.dp.toPx(), starColor)
                drawSparkleStar(Offset(w * 0.50f, h * 0.10f + floatOffset * 0.5f), 12.dp.toPx(), currentRarity.glowColor.copy(alpha = 0.6f))

                // Pre-burst Gravitational Implosion Motes (Inward rushing cosmic power)
                if (implosionAlpha.value > 0.01f) {
                    val prog = implosionProgress.value
                    val invProg = 1f - prog
                    implosionMotesList.forEach { mote ->
                        val curDist = mote.distance * invProg
                        val mx = centerOffset.x + cos(mote.angle) * curDist
                        val my = centerOffset.y + sin(mote.angle) * curDist
                        val moteAlpha = (implosionAlpha.value * (0.3f + 0.7f * prog)).coerceIn(0f, 1f)
                        drawCircle(
                            color = mote.color.copy(alpha = moteAlpha),
                            radius = mote.size * (0.6f + 0.8f * prog),
                            center = Offset(mx, my)
                        )
                        // Acceleration speed trail pointing toward reactor center
                        val tailDist = curDist + 22.dp.toPx() * prog
                        val tx = centerOffset.x + cos(mote.angle) * tailDist
                        val ty = centerOffset.y + sin(mote.angle) * tailDist
                        drawLine(
                            brush = Brush.linearGradient(
                                listOf(mote.color.copy(alpha = moteAlpha * 0.7f), Color.Transparent),
                                start = Offset(mx, my),
                                end = Offset(tx, ty)
                            ),
                            start = Offset(mx, my),
                            end = Offset(tx, ty),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                }

                // Full-Screen Cascading Dual Shockwaves
                if (shockwaveAlpha.value > 0.01f) {
                    val swR = maxOf(w, h) * 0.55f * shockwaveRadius.value
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(Color.Transparent, Color.White.copy(alpha = shockwaveAlpha.value), currentRarity.glowColor.copy(alpha = shockwaveAlpha.value * 0.7f), Color.Transparent),
                            center = centerOffset,
                            radius = swR
                        ),
                        radius = swR,
                        center = centerOffset
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = shockwaveAlpha.value),
                        radius = swR,
                        center = centerOffset,
                        style = Stroke(width = 3.5.dp.toPx())
                    )
                }
                if (secondaryShockwaveAlpha.value > 0.01f) {
                    val secR = maxOf(w, h) * 0.48f * secondaryShockwaveRadius.value
                    drawCircle(
                        color = currentRarity.primaryColor.copy(alpha = secondaryShockwaveAlpha.value * 0.85f),
                        radius = secR,
                        center = centerOffset,
                        style = Stroke(width = 3.dp.toPx())
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = secondaryShockwaveAlpha.value * 0.6f),
                        radius = secR * 0.95f,
                        center = centerOffset,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }

                // Interactive Tap Sparks Canvas Drawing
                if (tapSparksAlpha.value > 0.01f) {
                    val prog = tapSparksProgress.value
                    tapSparksList.forEach { spark ->
                        val sx = centerOffset.x + spark.vx * prog
                        val sy = centerOffset.y + spark.vy * prog + (160f * prog * prog)
                        rotate(degrees = spark.rotSpeed * prog, pivot = Offset(sx, sy)) {
                            drawSparkleStar(Offset(sx, sy), spark.size.dp.toPx(), spark.color.copy(alpha = tapSparksAlpha.value))
                        }
                    }
                }

                // Grand Burst Fountain Particles Canvas Drawing
                if (burstParticlesAlpha.value > 0.01f) {
                    val prog = burstParticlesProgress.value
                    burstParticlesList.forEach { p ->
                        val px = centerOffset.x + p.vx * prog
                        val py = centerOffset.y + p.vy * prog + (450f * prog * prog) // gravity
                        rotate(degrees = p.rotSpeed * prog, pivot = Offset(px, py)) {
                            drawCircle(
                                color = p.color.copy(alpha = burstParticlesAlpha.value),
                                radius = p.size * (1f - 0.3f * prog),
                                center = Offset(px, py)
                            )
                        }
                    }
                }

                // High-Velocity Faceted Quantum Crystal Shards Canvas Drawing
                if (quantumShardBurstAlpha.value > 0.01f) {
                    val prog = quantumShardBurstProgress.value
                    quantumShardList.forEach { shard ->
                        val sx = centerOffset.x + shard.vx * prog
                        val sy = centerOffset.y + shard.vy * prog + (420f * prog * prog) // parabolic gravity arc
                        rotate(degrees = shard.rotSpeed * prog, pivot = Offset(sx, sy)) {
                            val shardSize = shard.size * (1f - 0.2f * prog)
                            if (shard.isGem) {
                                drawFaceted3DGem(Offset(sx, sy), shardSize, shard.color.copy(alpha = quantumShardBurstAlpha.value))
                            } else {
                                drawSparkleStar(Offset(sx, sy), shardSize * 1.4f, shard.color.copy(alpha = quantumShardBurstAlpha.value))
                            }
                        }
                    }
                }

                // 3D Tumbling Coin Fountain Burst Canvas Drawing
                if (coinBurstAlpha.value > 0.01f) {
                    val prog = coinBurstProgress.value
                    coinBurstList.forEach { coin ->
                        val cx = centerOffset.x + coin.vx * prog
                        val cy = centerOffset.y + coin.vy * prog + (480f * prog * prog) // parabolic gravity arc
                        val tumbleScaleX = cos(prog * coin.tumbleSpeed)
                        rotate(degrees = coin.rotSpeed * prog, pivot = Offset(cx, cy)) {
                            val coinW = (coin.radius * 2f * kotlin.math.abs(tumbleScaleX)).coerceAtLeast(2.5f)
                            val coinH = coin.radius * 2f
                            // 3D coin face with volumetric gradient
                            drawOval(
                                brush = Brush.radialGradient(
                                    listOf(Color(0xFFFFFDE7), coin.color, Color(0xFF92400E)),
                                    center = Offset(cx - coinW * 0.25f, cy - coinH * 0.25f),
                                    radius = coin.radius
                                ),
                                topLeft = Offset(cx - coinW * 0.5f, cy - coinH * 0.5f),
                                size = Size(coinW, coinH),
                                alpha = coinBurstAlpha.value
                            )
                            // 3D Rim stroke
                            drawOval(
                                color = Color(0xFFB45309).copy(alpha = coinBurstAlpha.value),
                                topLeft = Offset(cx - coinW * 0.5f, cy - coinH * 0.5f),
                                size = Size(coinW, coinH),
                                style = Stroke(width = 1.2.dp.toPx())
                            )
                        }
                    }
                }

                // Falling celebratory confetti when revealed
                if (isRevealed) {
                    confettiList.forEach { p ->
                        val px = (p.x * w)
                        val py = ((p.y + (sunburstRotation * 0.003f * p.speedY)) % 1f) * h
                        drawCircle(
                            color = p.color.copy(alpha = 0.75f),
                            radius = p.size * 0.5f,
                            center = Offset(px, py)
                        )
                    }
                }
            }

            // 2. FLASH SCREEN OVERLAY (On tier upgrade & opening)
            if (flashAlpha.value > 0.01f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = flashAlpha.value))
                )
            }

            // ==================== UNIFIED CONTINUOUS STAGE ====================
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Header Row
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Tap phase: Rarity title badge
                    if (!isRevealed) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.alpha(hudAlpha.value)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = Color.Black.copy(alpha = 0.35f),
                                border = androidx.compose.foundation.BorderStroke(2.dp, currentRarity.primaryColor.copy(alpha = 0.8f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(currentRarity.gemColor)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = currentRarity.title,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        letterSpacing = 1.4.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            AnimatedVisibility(
                                visible = upgradeSuccessNotice,
                                enter = fadeIn() + scaleIn(),
                                exit = fadeOut()
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFFFD700).copy(alpha = 0.25f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD700))
                                ) {
                                    Text(
                                        text = "✨ UPGRADED TO ${currentRarity.title}! ✨",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFFFFEA00),
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        // Reveal phase: Gold balance counter at top right
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF1E293B).copy(alpha = 0.85f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
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
                    }
                }

                // Center Stage: 3D Chest & Floating Reward Emerging Out Of Chest Cavity
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    // 3D Vector Chest Model (Seamlessly anchors the entire scene)
                    Box(
                        modifier = Modifier
                            .size(310.dp)
                            .graphicsLayer {
                                cameraDistance = 16f * density
                                val idleYaw = if (!isRevealed && chancesLeft > 0) ambientYaw else 0f
                                val idlePitch = if (!isRevealed && chancesLeft > 0) ambientPitch else 0f
                                rotationX = chestPitchX.value + idlePitch
                                rotationY = chestYawY.value + idleYaw
                                rotationZ = chestWobble.value
                                translationX = screenShakeX.value.dp.toPx()
                                translationY = (chestElevation.value + chestRevealOffsetY.value + screenShakeY.value).dp.toPx()
                                scaleX = tapScaleX.value * chestRevealScale.value
                                scaleY = tapScaleY.value * chestRevealScale.value
                            }
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                enabled = chancesLeft > 0 && !isUpgrading && !isRevealed
                            ) {
                                performTapChance()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Futuristic Holographic Docking Platform & Shockwave Canvas
                        Canvas(modifier = Modifier.size(310.dp)) {
                            val w = size.width
                            val h = size.height
                            val pedestalCenter = Offset(w * 0.5f, h * 0.78f)

                            // Soft Anti-Gravity Ground Occlusion Shadow
                            drawOval(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0x90000000),
                                        Color(0x40000000),
                                        Color.Transparent
                                    ),
                                    center = pedestalCenter,
                                    radius = w * 0.44f
                                ),
                                topLeft = Offset(w * 0.10f, h * 0.68f),
                                size = Size(w * 0.80f, h * 0.20f)
                            )

                            // High-Tech Neon Emitter Bloom
                            drawOval(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        currentRarity.glowColor.copy(alpha = 0.50f * ambientPulse),
                                        currentRarity.primaryColor.copy(alpha = 0.18f * ambientPulse),
                                        Color.Transparent
                                    ),
                                    center = pedestalCenter,
                                    radius = w * 0.42f
                                ),
                                topLeft = Offset(w * 0.12f, h * 0.67f),
                                size = Size(w * 0.76f, h * 0.22f)
                            )

                            // Outer Hologram Ring with Precision Tech Ticks
                            drawOval(
                                color = currentRarity.primaryColor.copy(alpha = 0.75f),
                                topLeft = Offset(w * 0.15f, h * 0.70f),
                                size = Size(w * 0.70f, h * 0.16f),
                                style = Stroke(width = 2.dp.toPx())
                            )

                            // Inner Glowing Concentric Projection Ring
                            drawOval(
                                color = Color.White.copy(alpha = 0.60f * ambientPulse),
                                topLeft = Offset(w * 0.24f, h * 0.73f),
                                size = Size(w * 0.52f, h * 0.10f),
                                style = Stroke(width = 1.5.dp.toPx())
                            )

                            // Vertical Hologram Projector Light Beacons (4 Emitters)
                            val beaconXOffsets = listOf(w * 0.20f, w * 0.38f, w * 0.62f, w * 0.80f)
                            beaconXOffsets.forEach { bx ->
                                drawLine(
                                    brush = Brush.verticalGradient(
                                        listOf(Color.Transparent, currentRarity.primaryColor.copy(alpha = 0.40f * ambientPulse)),
                                        startY = h * 0.62f,
                                        endY = h * 0.76f
                                    ),
                                    start = Offset(bx, h * 0.62f),
                                    end = Offset(bx, h * 0.76f),
                                    strokeWidth = 1.5.dp.toPx()
                                )
                                drawCircle(Color.White, radius = 2.dp.toPx(), center = Offset(bx, h * 0.76f))
                            }

                            // Tap Expanding Cyber Shockwave Wavefront
                            if (shockwaveAlpha.value > 0.01f) {
                                val swR = w * 0.38f * shockwaveRadius.value
                                drawOval(
                                    color = Color.White.copy(alpha = shockwaveAlpha.value),
                                    topLeft = Offset(pedestalCenter.x - swR, pedestalCenter.y - swR * 0.32f),
                                    size = Size(swR * 2f, swR * 0.64f),
                                    style = Stroke(width = 3.5.dp.toPx())
                                )
                                drawOval(
                                    color = currentRarity.glowColor.copy(alpha = shockwaveAlpha.value * 0.6f),
                                    topLeft = Offset(pedestalCenter.x - swR * 0.85f, pedestalCenter.y - swR * 0.85f * 0.32f),
                                    size = Size(swR * 1.7f, swR * 1.7f * 0.32f),
                                    style = Stroke(width = 2.dp.toPx())
                                )
                            }
                        }

                        // Mastercraft 3D Vector Chest Model
                        DuolingoChest3DGraphic(
                            rarity = currentRarity,
                            wobbleDegrees = chestWobble.value,
                            lidOpenRatio = lidOpenAnim.value,
                            crackGlow = crackGlowAnim.value,
                            hullExpandRatio = hullExpandAnim.value,
                            reactorEjectionRatio = reactorEjectionAnim.value,
                            beamIntensityRatio = beamIntensityAnim.value,
                            modifier = Modifier.size(240.dp)
                        )
                    }

                    // Seamless Floating Reward Card (Ascends gracefully out of open chest)
                    if (isRevealed) {
                        val reward = claimedReward ?: MysteryReward(xp = 0, gold = 50, streakFreezeAwarded = false)
                        val isCommonPotion = currentRarity == ChestRarity.COMMON

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .graphicsLayer {
                                    translationY = rewardCardOffsetY.value.dp.toPx()
                                    scaleX = rewardCardScale.value
                                    scaleY = rewardCardScale.value
                                    alpha = rewardCardAlpha.value
                                },
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (isCommonPotion) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF0284C7).copy(alpha = 0.35f),
                                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF38BDF8))
                                ) {
                                    Text(
                                        text = "2X EXP BOOST POTION",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.2.sp,
                                        color = Color(0xFF38BDF8),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Double XP Active!",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "Earn 2X XP for 15 mins on Drills & Blitz!",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFCBD5E1),
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFFFB300).copy(alpha = 0.2f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB300))
                                ) {
                                    Text(
                                        text = "+${animatedGoldCount.value.toInt()} 🪙 Gold",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFFFFD54F),
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                DuolingoXpPotionBeakerGraphic(
                                    modifier = Modifier
                                        .size(150.dp)
                                        .scale(ambientPulse * 0.95f + 0.05f)
                                )
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = currentRarity.primaryColor.copy(alpha = 0.30f),
                                    border = androidx.compose.foundation.BorderStroke(1.5.dp, currentRarity.primaryColor)
                                ) {
                                    Text(
                                        text = "${currentRarity.title} REWARDS",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.2.sp,
                                        color = currentRarity.glowColor,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "TREASURE UNLOCKED!",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = Color(0xFFFFB300).copy(alpha = 0.25f),
                                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFFB300))
                                    ) {
                                        Text(
                                            text = "+${animatedGoldCount.value.toInt()} 🪙 Gold",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFFFFD54F),
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                        )
                                    }

                                    if (reward.xp > 0) {
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = Color(0xFF38BDF8).copy(alpha = 0.25f),
                                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF38BDF8))
                                        ) {
                                            Text(
                                                text = "+${animatedXpCount.value.toInt()} ⚡ XP",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFF38BDF8),
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                            )
                                        }
                                    }

                                    if (reward.streakFreezeAwarded) {
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = Color(0xFF00E5FF).copy(alpha = 0.25f),
                                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF00E5FF))
                                        ) {
                                            Text(
                                                text = "🧊 +1 Freeze",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFF00E5FF),
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Controls: Chances Indicators or 3D Claim Button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    if (!isRevealed) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.alpha(hudAlpha.value)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ChanceDotItem(
                                    state = when {
                                        chancesLeft == 3 -> ChanceDotState.ACTIVE
                                        chancesLeft < 3 -> ChanceDotState.COMPLETED
                                        else -> ChanceDotState.PENDING
                                    },
                                    pulse = if (chancesLeft == 3) ambientPulse else 1f,
                                    activeColor = currentRarity.primaryColor
                                )

                                ChanceDotItem(
                                    state = when {
                                        chancesLeft == 2 -> ChanceDotState.ACTIVE
                                        chancesLeft < 2 -> ChanceDotState.COMPLETED
                                        else -> ChanceDotState.PENDING
                                    },
                                    pulse = if (chancesLeft == 2) ambientPulse else 1f,
                                    activeColor = currentRarity.primaryColor
                                )

                                ChanceDotItem(
                                    state = when {
                                        chancesLeft == 1 -> ChanceDotState.ACTIVE
                                        chancesLeft < 1 -> ChanceDotState.COMPLETED
                                        else -> ChanceDotState.PENDING
                                    },
                                    pulse = if (chancesLeft == 1) ambientPulse else 1f,
                                    activeColor = currentRarity.primaryColor
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Text(
                                text = when (chancesLeft) {
                                    3 -> "Tap the chest to unlock or upgrade!"
                                    2 -> "Tap again! Elevate your prize tier!"
                                    1 -> "Final tap to pop the lock & reveal!"
                                    else -> "Unlocking rewards..."
                                },
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        // 3D Claim Reward Button
                        Duolingo3DButton(
                            text = "CLAIM REWARD 🎁",
                            buttonColor = Color(0xFF1CB0F6),
                            bevelColor = Color(0xFF1899D6),
                            textColor = Color.White,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
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
}

enum class ChanceDotState {
    PENDING,
    ACTIVE,
    COMPLETED
}

@Composable
private fun ChanceDotItem(
    state: ChanceDotState,
    pulse: Float = 1f,
    activeColor: Color = Color(0xFF38BDF8)
) {
    when (state) {
        ChanceDotState.ACTIVE -> {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .scale(pulse)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowUpward,
                    contentDescription = "Upgrade Arrow",
                    tint = activeColor,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        ChanceDotState.COMPLETED -> {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0284C7).copy(alpha = 0.65f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Completed Step",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        ChanceDotState.PENDING -> {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.20f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowUpward,
                    contentDescription = "Next Arrow",
                    tint = Color.White.copy(alpha = 0.40f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

private data class QuantumVaultThemeColors(
    val armorPrimary: Color,
    val armorSecondary: Color,
    val armorHighlight: Color,
    val armorDarkBevel: Color,
    val trimPrimary: Color,
    val trimHighlight: Color,
    val trimShadow: Color,
    val neonPrimary: Color,
    val neonSecondary: Color,
    val neonGlow: Color,
    val coreWhiteHot: Color = Color.White
)

/**
 * Mastercraft 3D Sci-Fi Quantum Core Vault Graphic.
 * Features:
 * - High-tech obsidian and matte titanium chamfered chassis with realistic specular lighting
 * - Multi-layered glowing neon conduit channels and circuit traces
 * - Central Quantum Reactor Core with spinning magnetic containment rings and pulsating plasma bloom
 * - 3D isometric lid articulation that splits open with real depth perspective
 * - Volumetric ethereal hyper-light column erupting upward upon opening
 * - Tactile status indicators and anti-gravity levitation field
 */
@Composable
fun DuolingoChest3DGraphic(
    rarity: ChestRarity,
    wobbleDegrees: Float = 0f,
    lidOpenRatio: Float = 0f,
    crackGlow: Float = 0f,
    hullExpandRatio: Float = 0f,
    reactorEjectionRatio: Float = 0f,
    beamIntensityRatio: Float = 1f,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "quantum_vault_fx")
    val corePulse by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "core_pulse"
    )
    val ringSpin by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_spin"
    )
    val conduitPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "conduit_phase"
    )

    Canvas(
        modifier = modifier.graphicsLayer {
            rotationZ = wobbleDegrees
        }
    ) {
        val w = size.width
        val h = size.height

        // Palette configuration per rarity
        val colors = when (rarity) {
            ChestRarity.COMMON -> QuantumVaultThemeColors(
                armorPrimary = Color(0xFF1E2638),
                armorSecondary = Color(0xFF0F172A),
                armorHighlight = Color(0xFF334155),
                armorDarkBevel = Color(0xFF070B14),
                trimPrimary = Color(0xFF0284C7),
                trimHighlight = Color(0xFFBAE6FD),
                trimShadow = Color(0xFF0369A1),
                neonPrimary = Color(0xFF00F5FF),
                neonSecondary = Color(0xFF0284C7),
                neonGlow = Color(0xFF38BDF8)
            )
            ChestRarity.RARE -> QuantumVaultThemeColors(
                armorPrimary = Color(0xFF161F38),
                armorSecondary = Color(0xFF0A0F24),
                armorHighlight = Color(0xFF2563EB),
                armorDarkBevel = Color(0xFF050814),
                trimPrimary = Color(0xFF3B82F6),
                trimHighlight = Color(0xFFE0E7FF),
                trimShadow = Color(0xFF1D4ED8),
                neonPrimary = Color(0xFF60A5FA),
                neonSecondary = Color(0xFF2563EB),
                neonGlow = Color(0xFF93C5FD)
            )
            ChestRarity.EPIC -> QuantumVaultThemeColors(
                armorPrimary = Color(0xFF221533),
                armorSecondary = Color(0xFF120820),
                armorHighlight = Color(0xFF581C87),
                armorDarkBevel = Color(0xFF0A0214),
                trimPrimary = Color(0xFFA855F7),
                trimHighlight = Color(0xFFF3E8FF),
                trimShadow = Color(0xFF7E22CE),
                neonPrimary = Color(0xFFE879F9),
                neonSecondary = Color(0xFFA855F7),
                neonGlow = Color(0xFFF0ABFC)
            )
            ChestRarity.LEGENDARY -> QuantumVaultThemeColors(
                armorPrimary = Color(0xFF2A1C0E),
                armorSecondary = Color(0xFF180D03),
                armorHighlight = Color(0xFF78350F),
                armorDarkBevel = Color(0xFF0D0601),
                trimPrimary = Color(0xFFF59E0B),
                trimHighlight = Color(0xFFFFFBEB),
                trimShadow = Color(0xFFB45309),
                neonPrimary = Color(0xFFFFD700),
                neonSecondary = Color(0xFFF59E0B),
                neonGlow = Color(0xFFFFEA00)
            )
        }

        val armorPrimary = colors.armorPrimary
        val armorSecondary = colors.armorSecondary
        val armorHighlight = colors.armorHighlight
        val armorDarkBevel = colors.armorDarkBevel
        val trimPrimary = colors.trimPrimary
        val trimHighlight = colors.trimHighlight
        val trimShadow = colors.trimShadow
        val neonPrimary = colors.neonPrimary
        val neonSecondary = colors.neonSecondary
        val neonGlow = colors.neonGlow
        val coreWhiteHot = colors.coreWhiteHot

        val mouthCenterY = h * 0.44f
        val mouthRadiusX = w * 0.36f
        val mouthRadiusY = h * 0.08f

        // -------------------------------------------------------------
        // 1. ANTI-GRAVITY LEVITATION BASE & GROUND OCCLUSION
        // -------------------------------------------------------------
        // Deep soft floor occlusion shadow
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x95000000), Color(0x35000000), Color.Transparent),
                center = Offset(w * 0.50f, h * 0.86f),
                radius = w * 0.45f
            ),
            topLeft = Offset(w * 0.08f, h * 0.77f),
            size = Size(w * 0.84f, h * 0.18f)
        )

        // Concentric Holographic Docking Rings
        drawOval(
            color = neonPrimary.copy(alpha = 0.40f * corePulse),
            topLeft = Offset(w * 0.18f, h * 0.78f),
            size = Size(w * 0.64f, h * 0.14f),
            style = Stroke(width = 1.8.dp.toPx())
        )
        drawOval(
            color = Color.White.copy(alpha = 0.25f * corePulse),
            topLeft = Offset(w * 0.26f, h * 0.80f),
            size = Size(w * 0.48f, h * 0.10f),
            style = Stroke(width = 1.2.dp.toPx())
        )

        // -------------------------------------------------------------
        // 2. INNER QUANTUM REACTOR CAVITY & VOLUMETRIC HYPER-LIGHT BEAM
        // -------------------------------------------------------------
        if (lidOpenRatio > 0.01f) {
            val effLid = lidOpenRatio * beamIntensityRatio
            // Radiant Volumetric Light Shafts
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        coreWhiteHot.copy(alpha = (0.95f * effLid).coerceIn(0f, 1f)),
                        neonGlow.copy(alpha = (0.62f * effLid).coerceIn(0f, 1f)),
                        neonSecondary.copy(alpha = (0.32f * effLid).coerceIn(0f, 1f)),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.50f, mouthCenterY),
                    radius = w * 0.75f * beamIntensityRatio
                ),
                topLeft = Offset(w * 0.0f, mouthCenterY - w * 0.65f * beamIntensityRatio),
                size = Size(w * 1.0f, w * 1.1f * beamIntensityRatio)
            )

            // Volumetric Vertical Laser Columns
            val beamAngles = listOf(-38f, -22f, -8f, 8f, 22f, 38f)
            beamAngles.forEach { deg ->
                val rad = Math.toRadians((deg + sin(ringSpin * 0.03f) * 3f).toDouble()).toFloat()
                val beamLength = h * 1.15f * beamIntensityRatio
                val spread = w * 0.075f * (1f + 0.40f * lidOpenRatio) * beamIntensityRatio
                val cosR = cos(rad)
                val sinR = sin(rad)
                val startCenter = Offset(w * 0.50f + sinR * 8f, mouthCenterY)

                val rayPath = Path().apply {
                    moveTo(startCenter.x - spread * 0.3f, startCenter.y)
                    lineTo(startCenter.x + spread * 0.3f, startCenter.y)
                    lineTo(startCenter.x + sinR * beamLength + spread, startCenter.y - cosR * beamLength)
                    lineTo(startCenter.x + sinR * beamLength - spread, startCenter.y - cosR * beamLength)
                    close()
                }

                drawPath(
                    path = rayPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            neonGlow.copy(alpha = (0.42f * effLid).coerceIn(0f, 1f)),
                            coreWhiteHot.copy(alpha = (0.85f * effLid).coerceIn(0f, 1f))
                        ),
                        startY = 0f,
                        endY = mouthCenterY
                    )
                )
            }

            // Ascending Holographic Energy Ripple Rings
            val rippleY1 = mouthCenterY - (h * 0.32f * lidOpenRatio)
            val rippleY2 = mouthCenterY - (h * 0.52f * lidOpenRatio)
            drawOval(
                color = neonPrimary.copy(alpha = 0.55f * lidOpenRatio),
                topLeft = Offset(w * 0.28f, rippleY1),
                size = Size(w * 0.44f, h * 0.08f),
                style = Stroke(width = 2.dp.toPx())
            )
            if (lidOpenRatio > 0.4f) {
                drawOval(
                    color = Color.White.copy(alpha = 0.35f * lidOpenRatio),
                    topLeft = Offset(w * 0.22f, rippleY2),
                    size = Size(w * 0.56f, h * 0.09f),
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }

            // Dark Quantum Core Interior Cavity
            val cavityDepthPath = Path().apply {
                moveTo(w * 0.18f, mouthCenterY)
                lineTo(w * 0.82f, mouthCenterY)
                lineTo(w * 0.78f, mouthCenterY + h * 0.16f)
                lineTo(w * 0.22f, mouthCenterY + h * 0.16f)
                close()
            }
            drawPath(
                path = cavityDepthPath,
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF02040A), armorDarkBevel, Color(0xFF0B101D)),
                    startY = mouthCenterY,
                    endY = mouthCenterY + h * 0.16f
                )
            )

            // High-Tech Power Cells floating inside cavity
            val cellY = mouthCenterY + 4.dp.toPx()
            fun drawPowerCell(center: Offset, radius: Float, color: Color) {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(coreWhiteHot, color, color.copy(alpha = 0.4f)),
                        center = center,
                        radius = radius
                    ),
                    radius = radius,
                    center = center
                )
                drawSparkleStar(center, radius * 0.6f, coreWhiteHot)
            }

            drawPowerCell(Offset(w * 0.36f, cellY + 3.dp.toPx()), w * 0.045f, neonPrimary)
            drawPowerCell(Offset(w * 0.64f, cellY + 2.dp.toPx()), w * 0.045f, neonGlow)
            drawPowerCell(Offset(w * 0.50f, cellY - 1.dp.toPx()), w * 0.065f, coreWhiteHot)
        }

        // -------------------------------------------------------------
        // 3. STEPPED 3D BASE PEDESTAL & DOCKING COLLAR
        // -------------------------------------------------------------
        // Lower Plinth Step
        val plinthBottomPath = Path().apply {
            moveTo(w * 0.18f, h * 0.77f)
            lineTo(w * 0.82f, h * 0.77f)
            lineTo(w * 0.80f, h * 0.83f)
            lineTo(w * 0.20f, h * 0.83f)
            close()
        }
        drawPath(
            path = plinthBottomPath,
            brush = Brush.verticalGradient(
                listOf(armorHighlight, armorSecondary, armorDarkBevel),
                startY = h * 0.77f,
                endY = h * 0.83f
            )
        )
        // Plinth Under-glow Seam
        drawLine(
            brush = Brush.horizontalGradient(
                listOf(Color.Transparent, neonPrimary.copy(alpha = 0.8f), Color.Transparent),
                startX = w * 0.20f,
                endX = w * 0.80f
            ),
            start = Offset(w * 0.20f, h * 0.83f),
            end = Offset(w * 0.80f, h * 0.83f),
            strokeWidth = 2.dp.toPx()
        )

        // Upper Plinth Step
        val plinthTopPath = Path().apply {
            moveTo(w * 0.20f, h * 0.74f)
            lineTo(w * 0.80f, h * 0.74f)
            lineTo(w * 0.81f, h * 0.77f)
            lineTo(w * 0.19f, h * 0.77f)
            close()
        }
        drawPath(
            path = plinthTopPath,
            brush = Brush.verticalGradient(
                listOf(trimHighlight, trimPrimary, trimShadow),
                startY = h * 0.74f,
                endY = h * 0.77f
            )
        )

        // -------------------------------------------------------------
        // 4. MAIN QUANTUM CHASSIS (HEXAGONAL CHAMFERED 3D MONOLITH)
        // -------------------------------------------------------------
        val hullExpandPx = w * 0.04f * hullExpandRatio

        // Left 3D Chamfer Depth Facet (darker shading on angled side)
        val leftFacetPath = Path().apply {
            moveTo(w * 0.17f - hullExpandPx, mouthCenterY)
            lineTo(w * 0.24f, mouthCenterY)
            lineTo(w * 0.26f, h * 0.74f)
            lineTo(w * 0.20f - hullExpandPx, h * 0.74f)
            close()
        }
        drawPath(
            path = leftFacetPath,
            brush = Brush.horizontalGradient(
                listOf(armorDarkBevel, armorSecondary),
                startX = w * 0.17f - hullExpandPx,
                endX = w * 0.24f
            )
        )

        // Right 3D Chamfer Depth Facet (Machined rim light)
        val rightFacetPath = Path().apply {
            moveTo(w * 0.76f, mouthCenterY)
            lineTo(w * 0.83f + hullExpandPx, mouthCenterY)
            lineTo(w * 0.80f + hullExpandPx, h * 0.74f)
            lineTo(w * 0.74f, h * 0.74f)
            close()
        }
        drawPath(
            path = rightFacetPath,
            brush = Brush.horizontalGradient(
                listOf(armorSecondary, armorHighlight),
                startX = w * 0.76f,
                endX = w * 0.83f + hullExpandPx
            )
        )

        // Front Armor Face (Faceted Titanium Plate)
        val frontFacePath = Path().apply {
            moveTo(w * 0.24f, mouthCenterY)
            lineTo(w * 0.76f, mouthCenterY)
            lineTo(w * 0.74f, h * 0.74f)
            lineTo(w * 0.26f, h * 0.74f)
            close()
        }
        drawPath(
            path = frontFacePath,
            brush = Brush.verticalGradient(
                listOf(armorHighlight, armorPrimary, armorSecondary),
                startY = mouthCenterY,
                endY = h * 0.74f
            )
        )

        // Metallic Chamfer Surface Highlight Lines
        drawLine(
            brush = Brush.horizontalGradient(
                listOf(Color.Transparent, Color.White.copy(alpha = 0.20f), Color.Transparent),
                startX = w * 0.25f,
                endX = w * 0.75f
            ),
            start = Offset(w * 0.25f, mouthCenterY + 4.dp.toPx()),
            end = Offset(w * 0.75f, mouthCenterY + 4.dp.toPx()),
            strokeWidth = 1.5.dp.toPx()
        )

        // -------------------------------------------------------------
        // 5. NEON CONDUIT CIRCUITRY & ENERGY TRACES
        // -------------------------------------------------------------
        val conduitGlowMult = if (crackGlow > 0f) (1f + crackGlow * 1.5f) else 1f
        val activeNeonAlpha = (0.85f * conduitGlowMult).coerceIn(0f, 1f)

        fun drawConduitTrace(path: Path) {
            // Outer Conduit Glow
            drawPath(
                path = path,
                color = neonGlow.copy(alpha = 0.40f * activeNeonAlpha),
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            // Inner Core Laser Line
            drawPath(
                path = path,
                color = neonPrimary.copy(alpha = activeNeonAlpha),
                style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }

        // Left Angled Conduit Line (From core to bottom corner)
        val leftConduit = Path().apply {
            moveTo(w * 0.40f, mouthCenterY + h * 0.11f)
            lineTo(w * 0.32f, mouthCenterY + h * 0.11f)
            lineTo(w * 0.30f, h * 0.66f)
            lineTo(w * 0.34f, h * 0.72f)
        }
        drawConduitTrace(leftConduit)

        // Right Angled Conduit Line (From core to bottom corner)
        val rightConduit = Path().apply {
            moveTo(w * 0.60f, mouthCenterY + h * 0.11f)
            lineTo(w * 0.68f, mouthCenterY + h * 0.11f)
            lineTo(w * 0.70f, h * 0.66f)
            lineTo(w * 0.66f, h * 0.72f)
        }
        drawConduitTrace(rightConduit)

        // Moving Energy Pulse Motes along the conduits
        val leftPulsePos = conduitPhase
        val leftMoteX = w * 0.30f + sin(leftPulsePos * PI.toFloat()) * (w * 0.04f)
        val leftMoteY = (mouthCenterY + h * 0.11f) + (h * 0.16f * leftPulsePos)
        drawCircle(coreWhiteHot, radius = 2.5.dp.toPx(), center = Offset(leftMoteX, leftMoteY))

        val rightMoteX = w * 0.70f - sin(leftPulsePos * PI.toFloat()) * (w * 0.04f)
        drawCircle(coreWhiteHot, radius = 2.5.dp.toPx(), center = Offset(rightMoteX, leftMoteY))

        // High-Tech Hexagonal Corner Clamp Brackets
        val bracketLeft = Path().apply {
            moveTo(w * 0.20f, h * 0.66f)
            lineTo(w * 0.28f, h * 0.66f)
            lineTo(w * 0.27f, h * 0.74f)
            lineTo(w * 0.22f, h * 0.74f)
            close()
        }
        val bracketRight = Path().apply {
            moveTo(w * 0.80f, h * 0.66f)
            lineTo(w * 0.72f, h * 0.66f)
            lineTo(w * 0.73f, h * 0.74f)
            lineTo(w * 0.78f, h * 0.74f)
            close()
        }
        drawPath(bracketLeft, brush = Brush.verticalGradient(listOf(trimHighlight, trimPrimary, trimShadow)))
        drawPath(bracketRight, brush = Brush.verticalGradient(listOf(trimHighlight, trimPrimary, trimShadow)))

        // Glowing Status LEDs on Brackets
        drawCircle(neonPrimary, radius = 3.dp.toPx(), center = Offset(w * 0.24f, h * 0.70f))
        drawCircle(coreWhiteHot, radius = 1.2.dp.toPx(), center = Offset(w * 0.24f, h * 0.70f))

        drawCircle(neonPrimary, radius = 3.dp.toPx(), center = Offset(w * 0.76f, h * 0.70f))
        drawCircle(coreWhiteHot, radius = 1.2.dp.toPx(), center = Offset(w * 0.76f, h * 0.70f))

        // -------------------------------------------------------------
        // 6. TOP RIM COLLAR (THE CHASSIS VISOR FLANGE)
        // -------------------------------------------------------------
        val rimThickness = 5.dp.toPx()
        val rimPath = Path().apply {
            moveTo(w * 0.16f, mouthCenterY)
            cubicTo(
                w * 0.16f, mouthCenterY - mouthRadiusY * 0.8f,
                w * 0.84f, mouthCenterY - mouthRadiusY * 0.8f,
                w * 0.84f, mouthCenterY
            )
            lineTo(w * 0.83f, mouthCenterY + rimThickness)
            cubicTo(
                w * 0.83f, mouthCenterY - mouthRadiusY * 0.8f + rimThickness,
                w * 0.17f, mouthCenterY - mouthRadiusY * 0.8f + rimThickness,
                w * 0.17f, mouthCenterY + rimThickness
            )
            close()
        }
        drawPath(
            path = rimPath,
            brush = Brush.verticalGradient(
                listOf(trimHighlight, trimPrimary, trimShadow),
                startY = mouthCenterY - mouthRadiusY,
                endY = mouthCenterY + rimThickness
            )
        )

        // -------------------------------------------------------------
        // 7. CENTRAL QUANTUM REACTOR CORE (REPLACING THE OLD KEYHOLE)
        // -------------------------------------------------------------
        val coreCenterY = (mouthCenterY + h * 0.11f) - (h * 0.24f * reactorEjectionRatio)
        val coreCenter = Offset(w * 0.50f, coreCenterY)
        val coreR = w * 0.085f * (1f + 0.25f * reactorEjectionRatio)

        // Outer Magnetic Containment Shroud Ring
        drawCircle(
            color = Color(0x70000000),
            radius = coreR * 1.5f,
            center = Offset(coreCenter.x + 1f, coreCenter.y + 2f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                listOf(armorHighlight, armorPrimary, armorDarkBevel),
                center = coreCenter,
                radius = coreR * 1.45f
            ),
            radius = coreR * 1.45f,
            center = coreCenter
        )
        drawCircle(
            brush = Brush.linearGradient(listOf(trimHighlight, trimShadow)),
            radius = coreR * 1.45f,
            center = coreCenter,
            style = Stroke(width = 2.dp.toPx())
        )

        // Rotating Magnetic Stator Notches (Cardinal Magnetic Pole Clamps)
        rotate(degrees = ringSpin, pivot = coreCenter) {
            val notchR = coreR * 1.30f
            for (deg in 0 until 360 step 45) {
                val rad = Math.toRadians(deg.toDouble()).toFloat()
                val nx = coreCenter.x + notchR * cos(rad)
                val ny = coreCenter.y + notchR * sin(rad)
                val isCardinal = deg % 90 == 0
                drawCircle(
                    color = if (isCardinal) neonPrimary else trimHighlight,
                    radius = (if (isCardinal) 3.5f else 2f).dp.toPx(),
                    center = Offset(nx, ny)
                )
            }
        }

        // Intense Reactor Plasma Bloom Field
        drawCircle(
            brush = Brush.radialGradient(
                listOf(
                    coreWhiteHot.copy(alpha = 0.95f),
                    neonPrimary.copy(alpha = 0.70f * corePulse),
                    neonGlow.copy(alpha = 0.35f * corePulse),
                    Color.Transparent
                ),
                center = coreCenter,
                radius = coreR * 1.35f * corePulse
            ),
            radius = coreR * 1.35f * corePulse,
            center = coreCenter
        )

        // 3D Faceted Quantum Crystal at Center
        drawFaceted3DGem(center = coreCenter, radius = coreR * 0.65f, baseColor = neonPrimary)

        // Specular Star Flare in Reactor Center
        drawSparkleStar(coreCenter, coreR * 0.95f * corePulse, coreWhiteHot)

        // -------------------------------------------------------------
        // 8. VISOR SEAM & CRACK GLOW (Laser Light Slicing Across Seam)
        // -------------------------------------------------------------
        if (crackGlow > 0f) {
            val crackY = mouthCenterY
            drawLine(
                brush = Brush.horizontalGradient(
                    listOf(
                        Color.Transparent,
                        neonPrimary.copy(alpha = crackGlow),
                        coreWhiteHot.copy(alpha = (crackGlow * 1.3f).coerceIn(0f, 1f)),
                        neonPrimary.copy(alpha = crackGlow),
                        Color.Transparent
                    )
                ),
                start = Offset(w * 0.12f, crackY),
                end = Offset(w * 0.88f, crackY),
                strokeWidth = (8f * crackGlow).dp.toPx(),
                cap = StrokeCap.Round
            )
            drawSparkleStar(Offset(w * 0.22f, crackY), (18f * crackGlow).dp.toPx(), coreWhiteHot)
            drawSparkleStar(Offset(w * 0.78f, crackY), (18f * crackGlow).dp.toPx(), coreWhiteHot)
            drawSparkleStar(Offset(w * 0.50f, crackY), (24f * crackGlow).dp.toPx(), coreWhiteHot)
        }

        // -------------------------------------------------------------
        // 9. ARTICULATED 3D QUANTUM HOOD / CANOPY LID
        // -------------------------------------------------------------
        val lidPivotY = mouthCenterY - mouthRadiusY * 0.6f
        val baseVaultH = h * 0.22f

        if (lidOpenRatio <= 0.001f) {
            // ================= CLOSED ARMORED CANOPY =================
            val lidTopY = lidPivotY - baseVaultH * 0.75f
            val lidBottomY = mouthCenterY + 2.dp.toPx()

            // Chamfered Aerodynamic Armor Shell
            val closedLidPath = Path().apply {
                moveTo(w * 0.16f, lidBottomY)
                lineTo(w * 0.25f, lidTopY)
                lineTo(w * 0.75f, lidTopY)
                lineTo(w * 0.84f, lidBottomY)
                close()
            }
            drawPath(
                path = closedLidPath,
                brush = Brush.verticalGradient(
                    listOf(armorHighlight, armorPrimary, armorSecondary),
                    startY = lidTopY,
                    endY = lidBottomY
                )
            )

            // Chamfer Bevel Cuts on Canopy Corners
            val leftCanopyFacet = Path().apply {
                moveTo(w * 0.16f, lidBottomY)
                lineTo(w * 0.25f, lidTopY)
                lineTo(w * 0.33f, lidTopY)
                lineTo(w * 0.26f, lidBottomY)
                close()
            }
            drawPath(
                path = leftCanopyFacet,
                brush = Brush.horizontalGradient(
                    listOf(armorDarkBevel, armorSecondary),
                    startX = w * 0.16f,
                    endX = w * 0.33f
                )
            )

            val rightCanopyFacet = Path().apply {
                moveTo(w * 0.84f, lidBottomY)
                lineTo(w * 0.75f, lidTopY)
                lineTo(w * 0.67f, lidTopY)
                lineTo(w * 0.74f, lidBottomY)
                close()
            }
            drawPath(
                path = rightCanopyFacet,
                brush = Brush.horizontalGradient(
                    listOf(armorSecondary, armorHighlight),
                    startX = w * 0.67f,
                    endX = w * 0.84f
                )
            )

            // Top Cyber Heat-Sink Fins / Ventilation Slots
            val ventY = lidTopY + baseVaultH * 0.26f
            val ventWidth = w * 0.36f
            val ventStartX = w * 0.50f - ventWidth * 0.5f
            for (v in 0..2) {
                val vy = ventY + (v * 7.dp.toPx())
                drawLine(
                    color = armorDarkBevel,
                    start = Offset(ventStartX, vy),
                    end = Offset(ventStartX + ventWidth, vy),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = neonPrimary.copy(alpha = 0.55f * corePulse),
                    start = Offset(ventStartX + 4.dp.toPx(), vy),
                    end = Offset(ventStartX + ventWidth - 4.dp.toPx(), vy),
                    strokeWidth = 1.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Specular Reflection Sheen across top plate
            drawLine(
                brush = Brush.horizontalGradient(
                    listOf(Color.Transparent, Color.White.copy(alpha = 0.30f), Color.Transparent),
                    startX = w * 0.25f,
                    endX = w * 0.75f
                ),
                start = Offset(w * 0.25f, lidTopY + 3.dp.toPx()),
                end = Offset(w * 0.75f, lidTopY + 3.dp.toPx()),
                strokeWidth = 2.dp.toPx()
            )

            // Bottom Canopy Trim Lip
            val lidRimPath = Path().apply {
                moveTo(w * 0.16f, lidBottomY - 3.dp.toPx())
                lineTo(w * 0.84f, lidBottomY - 3.dp.toPx())
                lineTo(w * 0.84f, lidBottomY + 3.dp.toPx())
                lineTo(w * 0.16f, lidBottomY + 3.dp.toPx())
                close()
            }
            drawPath(lidRimPath, brush = Brush.verticalGradient(listOf(trimHighlight, trimPrimary, trimShadow)))

            // Central Magnetic Lock Clasp
            val claspW = w * 0.11f
            val claspH = 18.dp.toPx()
            val claspTopY = lidBottomY - 2.dp.toPx()
            val claspPath = Path().apply {
                moveTo(w * 0.50f - claspW * 0.5f, claspTopY)
                lineTo(w * 0.50f + claspW * 0.5f, claspTopY)
                lineTo(w * 0.50f + claspW * 0.4f, claspTopY + claspH)
                lineTo(w * 0.50f - claspW * 0.4f, claspTopY + claspH)
                close()
            }
            drawPath(claspPath, color = Color(0x60000000))
            drawPath(claspPath, brush = Brush.verticalGradient(listOf(trimHighlight, trimPrimary, trimShadow)))

            // Clasp Status Indicator Gem
            drawCircle(neonPrimary, radius = 3.dp.toPx(), center = Offset(w * 0.50f, claspTopY + claspH * 0.55f))
            drawCircle(coreWhiteHot, radius = 1.2.dp.toPx(), center = Offset(w * 0.50f, claspTopY + claspH * 0.55f))
        } else {
            // ================= 3D ARTICULATED OPENING HOOD =================
            val cosAngle = cos(lidOpenRatio * (PI.toFloat() * 0.46f))
            val sinAngle = sin(lidOpenRatio * (PI.toFloat() * 0.46f))

            val lidFrontY = lidPivotY - (baseVaultH * 0.8f * sinAngle)
            val lidTopArchY = lidFrontY - (baseVaultH * 0.70f * maxOf(0.12f, cosAngle))

            // UNDERSIDE OF HOOD (Deep high-tech interior revealed!)
            val interiorCeilingPath = Path().apply {
                moveTo(w * 0.18f, lidPivotY)
                lineTo(w * 0.82f, lidPivotY)
                lineTo(w * 0.82f, lidFrontY)
                lineTo(w * 0.18f, lidFrontY)
                close()
            }
            drawPath(
                path = interiorCeilingPath,
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF030712), armorDarkBevel, Color(0xFF0F172A)),
                    startY = lidFrontY,
                    endY = lidPivotY
                )
            )

            // Inner Trim Molding
            val innerTrimPath = Path().apply {
                moveTo(w * 0.16f, lidFrontY - 2.dp.toPx())
                lineTo(w * 0.84f, lidFrontY - 2.dp.toPx())
                lineTo(w * 0.84f, lidFrontY + 4.dp.toPx())
                lineTo(w * 0.16f, lidFrontY + 4.dp.toPx())
                close()
            }
            drawPath(
                path = innerTrimPath,
                brush = Brush.verticalGradient(
                    listOf(trimHighlight, trimPrimary, trimShadow),
                    startY = lidFrontY - 2.dp.toPx(),
                    endY = lidFrontY + 4.dp.toPx()
                )
            )

            // Outer Armored Canopy Shell (Pivoting in 3D perspective)
            val outerCanopyPath = Path().apply {
                moveTo(w * 0.16f, lidFrontY)
                lineTo(w * 0.25f, lidTopArchY)
                lineTo(w * 0.75f, lidTopArchY)
                lineTo(w * 0.84f, lidFrontY)
                close()
            }
            drawPath(
                path = outerCanopyPath,
                brush = Brush.verticalGradient(
                    listOf(armorHighlight, armorPrimary, armorSecondary),
                    startY = lidTopArchY,
                    endY = lidFrontY
                )
            )

            // Rear Magnetic Hinge Couplings
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(trimHighlight, trimShadow)),
                topLeft = Offset(w * 0.20f, lidPivotY - 6.dp.toPx()),
                size = Size(12.dp.toPx(), 14.dp.toPx()),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(trimHighlight, trimShadow)),
                topLeft = Offset(w * 0.76f, lidPivotY - 6.dp.toPx()),
                size = Size(12.dp.toPx(), 14.dp.toPx()),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )

            // Dangling Magnetic Clasp (Inertia swing)
            val claspH = 16.dp.toPx()
            val claspW = w * 0.09f
            val claspDangleAngle = lidOpenRatio * 55f
            rotate(degrees = claspDangleAngle, pivot = Offset(w * 0.50f, lidFrontY)) {
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(trimHighlight, trimPrimary, trimShadow)),
                    topLeft = Offset(w * 0.50f - claspW * 0.5f, lidFrontY),
                    size = Size(claspW, claspH),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
                drawCircle(neonPrimary, radius = 2.5.dp.toPx(), center = Offset(w * 0.50f, lidFrontY + claspH * 0.6f))
            }

            // Radiant Cyber Sparkles bursting out of the opened reactor
            if (lidOpenRatio > 0.25f) {
                drawSparkleStar(Offset(w * 0.20f, mouthCenterY - h * 0.15f * lidOpenRatio), 15.dp.toPx() * lidOpenRatio, neonPrimary)
                drawSparkleStar(Offset(w * 0.80f, mouthCenterY - h * 0.18f * lidOpenRatio), 16.dp.toPx() * lidOpenRatio, coreWhiteHot)
                drawSparkleStar(Offset(w * 0.50f, mouthCenterY - h * 0.26f * lidOpenRatio), 20.dp.toPx() * lidOpenRatio, neonGlow)
                drawSparkleStar(Offset(w * 0.35f, mouthCenterY - h * 0.22f * lidOpenRatio), 13.dp.toPx() * lidOpenRatio, coreWhiteHot)
            }
        }
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
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bubble_float"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Ground shadow
        drawOval(
            color = Color(0x40000000),
            topLeft = Offset(w * 0.2f, h * 0.85f),
            size = Size(w * 0.6f, h * 0.14f)
        )

        // Translucent Potion Glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF00E5FF).copy(alpha = 0.50f),
                    Color.Transparent
                ),
                center = Offset(w * 0.5f, h * 0.58f),
                radius = w * 0.48f
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

        // Bubbling Liquid Inside
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
                    Color(0xFF38BDF8),
                    Color(0xFF0284C7),
                    Color(0xFF0369A1)
                ),
                startY = liquidTop,
                endY = flaskBottom
            )
        )

        // Animated Rising Bubbles
        val b1Y = liquidTop + (flaskBottom - liquidTop) * (1f - bubbleFloat)
        drawCircle(
            color = Color.White.copy(alpha = 0.85f),
            radius = 6.dp.toPx(),
            center = Offset(w * 0.44f, b1Y)
        )
        val b2Y = liquidTop + (flaskBottom - liquidTop) * (1f - ((bubbleFloat + 0.5f) % 1f))
        drawCircle(
            color = Color.White.copy(alpha = 0.75f),
            radius = 4.5.dp.toPx(),
            center = Offset(w * 0.56f, b2Y)
        )

        // Glass Outline with Specular Glow
        drawPath(
            path = beakerPath,
            color = Color.White.copy(alpha = 0.85f),
            style = Stroke(width = 4.dp.toPx())
        )

        // Glass Highlight Reflection Streak
        drawLine(
            color = Color.White.copy(alpha = 0.65f),
            start = Offset(w * 0.5f - neckHalfWidth + 4, neckBottom + 6),
            end = Offset(w * 0.5f - baseHalfWidth + 14, flaskBottom - 10),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Sparkles
        drawSparkleStar(Offset(w * 0.75f, h * 0.28f), 14.dp.toPx(), Color(0xFFFFD54F))
        drawSparkleStar(Offset(w * 0.22f, h * 0.42f), 12.dp.toPx(), Color(0xFF80D8FF))
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
                Color(0xFFFFF59D),
                Color(0xFFFFB300),
                Color(0xFFC67100)
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
    // Sparkle in center
    drawSparkleStar(center, radius * 0.35f, Color(0xFFFFD54F))
}

/**
 * Draws a multi-faceted 3D crystal gemstone with specular facets.
 */
private fun DrawScope.drawFaceted3DGem(
    center: Offset,
    radius: Float,
    baseColor: Color
) {
    // Outer hexagonal contour
    val facetAngles = listOf(0f, 60f, 120f, 180f, 240f, 300f)
    val outerPoints = facetAngles.map { deg ->
        val rad = Math.toRadians(deg.toDouble()).toFloat()
        Offset(center.x + radius * cos(rad), center.y + radius * sin(rad))
    }

    // Outer Facet Bevels
    for (i in 0 until 6) {
        val nextIdx = (i + 1) % 6
        val p1 = outerPoints[i]
        val p2 = outerPoints[nextIdx]
        val facetPath = Path().apply {
            moveTo(center.x, center.y)
            lineTo(p1.x, p1.y)
            lineTo(p2.x, p2.y)
            close()
        }
        val shade = when (i) {
            4, 5 -> 1.4f // top-facing facets: brilliant highlight
            0, 3 -> 1.0f // mid facets
            else -> 0.65f // bottom facets: dark refraction
        }
        val facetColor = if (shade > 1.0f) {
            Color(
                red = (baseColor.red * 0.7f + 0.3f).coerceIn(0f, 1f),
                green = (baseColor.green * 0.7f + 0.3f).coerceIn(0f, 1f),
                blue = (baseColor.blue * 0.7f + 0.3f).coerceIn(0f, 1f),
                alpha = 1f
            )
        } else {
            Color(
                red = (baseColor.red * shade).coerceIn(0f, 1f),
                green = (baseColor.green * shade).coerceIn(0f, 1f),
                blue = (baseColor.blue * shade).coerceIn(0f, 1f),
                alpha = 1f
            )
        }
        drawPath(facetPath, color = facetColor)
        drawPath(facetPath, color = Color.White.copy(alpha = 0.25f), style = Stroke(width = 0.8.dp.toPx()))
    }

    // Inner Table Facet (Flat top surface)
    val tableR = radius * 0.52f
    val tablePoints = facetAngles.map { deg ->
        val rad = Math.toRadians(deg.toDouble()).toFloat()
        Offset(center.x + tableR * cos(rad), center.y + tableR * sin(rad))
    }
    val tablePath = Path().apply {
        moveTo(tablePoints[0].x, tablePoints[0].y)
        for (i in 1 until 6) lineTo(tablePoints[i].x, tablePoints[i].y)
        close()
    }
    drawPath(
        path = tablePath,
        brush = Brush.radialGradient(
            listOf(Color.White.copy(alpha = 0.95f), baseColor, baseColor.copy(alpha = 0.8f)),
            center = Offset(center.x - tableR * 0.3f, center.y - tableR * 0.3f),
            radius = tableR
        )
    )

    // Specular Star Glint on table
    drawSparkleStar(Offset(center.x - tableR * 0.25f, center.y - tableR * 0.25f), radius * 0.55f, Color.White)
}

/**
 * Draws miniature 3D faceted gems inside the treasure pile.
 */
private fun DrawScope.drawFacetedTreasureGem(
    center: Offset,
    radius: Float,
    color: Color
) {
    drawCircle(Color(0x60000000), radius = radius, center = Offset(center.x + 1.5f, center.y + 1.5f))
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color.White, color, color.copy(alpha = 0.7f)),
            center = Offset(center.x - radius * 0.35f, center.y - radius * 0.35f),
            radius = radius
        ),
        radius = radius,
        center = center
    )
    drawSparkleStar(Offset(center.x - radius * 0.25f, center.y - radius * 0.25f), radius * 0.6f, Color.White)
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

private data class ConfettiParticle(
    val x: Float,
    val y: Float,
    val size: Float,
    val speedY: Float,
    val speedX: Float,
    val color: Color,
    val rotation: Float,
    val rotationSpeed: Float
)

private data class ParticleSparkData(
    val vx: Float,
    val vy: Float,
    val size: Float,
    val color: Color,
    val rotSpeed: Float
)

private data class CoinParticleData(
    val vx: Float,
    val vy: Float,
    val radius: Float,
    val rotSpeed: Float,
    val tumbleSpeed: Float,
    val color: Color
)

private data class CyberShardData(
    val vx: Float,
    val vy: Float,
    val size: Float,
    val color: Color,
    val rotSpeed: Float,
    val isGem: Boolean
)

private data class ImplosionMoteData(
    val angle: Float,
    val distance: Float,
    val size: Float,
    val color: Color
)

