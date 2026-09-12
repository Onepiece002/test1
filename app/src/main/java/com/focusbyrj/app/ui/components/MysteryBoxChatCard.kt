package com.focusbyrj.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusbyrj.app.util.DailyQuestManager
import java.util.Calendar

@Composable
fun MysteryBoxChatCard(
    onOpenBox: () -> Unit
) {
    val questState by DailyQuestManager.stateFlow.collectAsState()
    val isAvailable = questState.isEarlyBirdAvailable || questState.isNightOwlAvailable
    val isDaytime = remember {
        val cal = Calendar.getInstance()
        cal.get(Calendar.HOUR_OF_DAY) in 6..17
    }

    val cardInteractionSource = remember { MutableInteractionSource() }
    val isPressed by cardInteractionSource.collectIsPressedAsState()

    val cardScale by animateFloatAsState(
        targetValue = if (isPressed && isAvailable) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow),
        label = "card_press_scale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "mystery_card_anim")

    // Smooth sinusoidal floating levitation for chest
    val hoverOffset by infiniteTransition.animateFloat(
        initialValue = -3.5f,
        targetValue = 3.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hover_offset"
    )

    // Gentle breathing scale for chest
    val chestBreathScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "chest_breath"
    )

    // Glowing aura expansion
    val auraRadiusScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aura_radius"
    )

    // Shimmering border progression
    val borderShimmerProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(3800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "border_shimmer"
    )

    // Dynamic gradient border brush
    val activeBorderBrush = remember(borderShimmerProgress) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFFFB300),
                Color(0xFFFFF176),
                Color(0xFFFF8F00),
                Color(0xFFFFD54F),
                Color(0xFFFFB300)
            ),
            start = Offset(borderShimmerProgress, 0f),
            end = Offset(borderShimmerProgress + 300f, 300f)
        )
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFF0F1B22),
        border = if (isAvailable) {
            BorderStroke(2.dp, activeBorderBrush)
        } else {
            BorderStroke(1.5.dp, Color(0xFF1E313C))
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .scale(cardScale)
            .clickable(
                interactionSource = cardInteractionSource,
                indication = null,
                enabled = isAvailable
            ) { onOpenBox() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Chest Hero Box with Levitating Physics and Radial Magic Aura
                Box(
                    modifier = Modifier.size(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isAvailable) {
                        // Ambient Radial Floor Glow & Cast Shadow Canvas
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height

                            // Soft radial backdrop glow bloom
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFFFFB300).copy(alpha = 0.35f * (2f - chestBreathScale)),
                                        Color(0xFFFF8F00).copy(alpha = 0.15f),
                                        Color.Transparent
                                    ),
                                    radius = w * 0.52f * auraRadiusScale
                                )
                            )

                            // Dynamic floor shadow that scales inversely with hover height
                            val shadowScale = (1f - (hoverOffset / 20f)).coerceIn(0.7f, 1.2f)
                            drawOval(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0x80000000), Color.Transparent),
                                    radius = w * 0.35f * shadowScale
                                ),
                                topLeft = Offset(w * 0.15f, h * 0.76f),
                                size = Size(w * 0.70f * shadowScale, h * 0.20f)
                            )
                        }
                    }

                    // Floating Chest Graphic with breathing scale and hover
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .graphicsLayer {
                                translationY = if (isAvailable) hoverOffset else 0f
                                scaleX = if (isAvailable) chestBreathScale else 1f
                                scaleY = if (isAvailable) chestBreathScale else 1f
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isDaytime) {
                            EarlyBirdChestGraphic(modifier = Modifier.size(74.dp))
                        } else {
                            NightOwlChestGraphic(modifier = Modifier.size(74.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isAvailable) Color(0xFFFFB300).copy(alpha = 0.22f) else Color(0xFF58CC02).copy(alpha = 0.2f),
                        border = if (isAvailable) BorderStroke(1.dp, Color(0xFFFFB300).copy(alpha = 0.5f)) else null
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            if (isAvailable) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .scale(auraRadiusScale)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFFD54F))
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                            }
                            Text(
                                text = if (isAvailable) "DAILY REWARD READY 🎁" else "CLAIMED TODAY ✓",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.6.sp
                                ),
                                color = if (isAvailable) Color(0xFFFFD54F) else Color(0xFF58CC02)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(5.dp))

                    Text(
                        text = if (isAvailable) "Mystery Box Unlocked!" else "Mystery Box Claimed",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp
                        ),
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = if (isAvailable) {
                            "1 drill completed! Tap to crack open your chest & upgrade your rewards."
                        } else {
                            "You unlocked and claimed today's mystery reward! Next chest available tomorrow."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            if (isAvailable) {
                Duolingo3DButton(
                    text = "OPEN MYSTERY BOX 🎁",
                    buttonColor = Color(0xFFFFB300),
                    bevelColor = Color(0xFFFF8F00),
                    textColor = Color(0xFF3E2723),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onOpenBox
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF58CC02),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Rewards Added To Wallet & XP",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF58CC02)
                    )
                }
            }
        }
    }
}
