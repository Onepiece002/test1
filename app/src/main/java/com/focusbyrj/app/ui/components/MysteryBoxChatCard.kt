package com.focusbyrj.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MysteryBoxChatCard(
    onOpenBox: () -> Unit,
    isAvailable: Boolean = true,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mystery_card_anim")
    
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val orbitRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_rot"
    )

    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    val hoverOffset by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "card_box_hover"
    )

    val primaryCyan = Color(0xFF00F0FF)
    val royalIndigo = Color(0xFF6366F1)
    val deepPurple = Color(0xFFA855F7)
    val goldAccent = Color(0xFFF59E0B)

    Card(
        modifier = modifier
            .fillMaxWidth(0.94f)
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0B101B)
        ),
        border = BorderStroke(
            1.dp,
            if (isAvailable) {
                Brush.sweepGradient(
                    colors = listOf(
                        primaryCyan.copy(alpha = 0.6f),
                        royalIndigo.copy(alpha = 0.3f),
                        deepPurple.copy(alpha = 0.7f),
                        goldAccent.copy(alpha = 0.4f),
                        primaryCyan.copy(alpha = 0.6f)
                    )
                )
            } else {
                Brush.linearGradient(
                    colors = listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.04f))
                )
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isAvailable) 10.dp else 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            if (isAvailable) primaryCyan.copy(alpha = 0.08f) else Color.Transparent,
                            Color(0xFF0B101B)
                        ),
                        center = Offset(80f, 80f),
                        radius = 400f
                    )
                )
                .clickable(enabled = isAvailable, onClick = onOpenBox)
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // -------------------------------------------------------------
                // 1. 3D QUANTUM VAULT ICON WITH ORBIT RINGS
                // -------------------------------------------------------------
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF131B2E), Color(0xFF080C14))
                            )
                        )
                        .border(
                            1.dp,
                            if (isAvailable) primaryCyan.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.08f),
                            RoundedCornerShape(18.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier
                            .size(54.dp)
                            .offset(y = if (isAvailable) hoverOffset.dp else 0.dp)
                    ) {
                        val w = size.width
                        val h = size.height
                        val cx = w / 2
                        val cy = h / 2

                        // Orbiting kinetic ring
                        if (isAvailable) {
                            rotate(degrees = orbitRotation, pivot = Offset(cx, cy)) {
                                drawOval(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            primaryCyan.copy(alpha = 0.85f),
                                            royalIndigo.copy(alpha = 0.1f),
                                            deepPurple.copy(alpha = 0.75f)
                                        )
                                    ),
                                    topLeft = Offset(cx - w * 0.42f, cy - h * 0.22f),
                                    size = androidx.compose.ui.geometry.Size(w * 0.84f, h * 0.44f),
                                    style = Stroke(width = 1.6.dp.toPx())
                                )
                            }
                        }

                        // Core ambient glow
                        if (isAvailable) {
                            drawCircle(
                                color = primaryCyan.copy(alpha = 0.25f * pulse),
                                radius = w * 0.35f,
                                center = Offset(cx, cy)
                            )
                        }

                        // 3D Isometric Facets
                        val topColor = if (isAvailable) Color(0xFF38BDF8) else Color(0xFF475569)
                        val leftColor = if (isAvailable) Color(0xFF0284C7) else Color(0xFF334155)
                        val rightColor = if (isAvailable) Color(0xFF6366F1) else Color(0xFF1E293B)
                        val seamColor = if (isAvailable) Color(0xFFE0F2FE) else Color(0xFF64748B)

                        val topPath = Path().apply {
                            moveTo(cx, cy - h * 0.36f)
                            lineTo(cx + w * 0.32f, cy - h * 0.14f)
                            lineTo(cx, cy + h * 0.08f)
                            lineTo(cx - w * 0.32f, cy - h * 0.14f)
                            close()
                        }

                        val leftPath = Path().apply {
                            moveTo(cx - w * 0.32f, cy - h * 0.14f)
                            lineTo(cx, cy + h * 0.08f)
                            lineTo(cx, cy + h * 0.38f)
                            lineTo(cx - w * 0.32f, cy + h * 0.16f)
                            close()
                        }

                        val rightPath = Path().apply {
                            moveTo(cx, cy + h * 0.08f)
                            lineTo(cx + w * 0.32f, cy - h * 0.14f)
                            lineTo(cx + w * 0.32f, cy + h * 0.16f)
                            lineTo(cx, cy + h * 0.38f)
                            close()
                        }

                        drawPath(topPath, topColor)
                        drawPath(leftPath, leftColor)
                        drawPath(rightPath, rightColor)

                        // Glowing Bevel Highlights
                        drawPath(topPath, seamColor.copy(alpha = 0.5f), style = Stroke(width = 1.2.dp.toPx()))
                        drawPath(leftPath, seamColor.copy(alpha = 0.3f), style = Stroke(width = 1.2.dp.toPx()))
                        drawPath(rightPath, seamColor.copy(alpha = 0.3f), style = Stroke(width = 1.2.dp.toPx()))

                        // Glowing Center Reactor Node
                        if (isAvailable) {
                            drawCircle(
                                color = Color.White,
                                radius = 2.5.dp.toPx(),
                                center = Offset(cx, cy + h * 0.08f)
                            )
                            drawCircle(
                                color = primaryCyan,
                                radius = 5.dp.toPx() * pulse,
                                center = Offset(cx, cy + h * 0.08f),
                                style = Stroke(width = 1.2.dp.toPx())
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // -------------------------------------------------------------
                // 2. VAULT DETAILS & REWARDS TAGS
                // -------------------------------------------------------------
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isAvailable) primaryCyan.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.08f)
                        ) {
                            Text(
                                text = if (isAvailable) "⚡ READY TO UNLOCK" else "✓ CLAIMED",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 9.sp,
                                    letterSpacing = 1.sp
                                ),
                                color = if (isAvailable) primaryCyan else Color(0xFF94A3B8),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (isAvailable) "Quantum Mystery Vault" else "Daily Vault Claimed",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            letterSpacing = (-0.2).sp
                        ),
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = if (isAvailable) "Tap to unbox XP, Gold & Streak Freezes" else "New drop available tomorrow",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.5.sp,
                            color = if (isAvailable) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // -------------------------------------------------------------
                // 3. UNLOCK ACTION BUTTON
                // -------------------------------------------------------------
                if (isAvailable) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Transparent,
                        border = BorderStroke(
                            1.2.dp,
                            Brush.linearGradient(
                                listOf(primaryCyan, royalIndigo)
                            )
                        ),
                        modifier = Modifier.clip(RoundedCornerShape(12.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            primaryCyan.copy(alpha = 0.22f),
                                            royalIndigo.copy(alpha = 0.28f)
                                        )
                                    )
                                )
                                .padding(horizontal = 14.dp, vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "UNLOCK",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp,
                                    fontSize = 11.sp
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

