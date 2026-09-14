package com.focusbyrj.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusbyrj.app.util.DailyQuestManager
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
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val gradientRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gradient_rotation"
    )

    Card(
        modifier = modifier
            .fillMaxWidth(0.9f)
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0F172A)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .let {
                    if (isAvailable) {
                        it.drawWithContent {
                            drawContent()
                            val center = Offset(size.width / 2, size.height / 2)
                            val radius = size.width
                            val rad = Math.toRadians(gradientRotation.toDouble())
                            val endX = center.x + radius * cos(rad).toFloat()
                            val endY = center.y + radius * sin(rad).toFloat()
                            
                            drawRoundRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF38BDF8).copy(alpha = 0.8f),
                                        Color(0xFF818CF8).copy(alpha = 0.8f),
                                        Color(0xFF38BDF8).copy(alpha = 0.8f)
                                    ),
                                    start = center,
                                    end = Offset(endX, endY)
                                ),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(20.dp.toPx()),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx()),
                                blendMode = BlendMode.Screen
                            )
                        }
                    } else {
                        it.border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                    }
                }
                .clickable(enabled = isAvailable, onClick = onOpenBox)
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Sleek Geometric Core Icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF1E293B), Color(0xFF020617))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(28.dp)) {
                        val w = size.width
                        val h = size.height
                        val cX = w / 2
                        val cY = h / 2
                        
                        val topColor = if (isAvailable) Color(0xFF38BDF8) else Color.DarkGray
                        val leftColor = if (isAvailable) Color(0xFF0284C7) else Color.Gray
                        val rightColor = if (isAvailable) Color(0xFF818CF8) else Color(0xFF696969)

                        val topPath = Path().apply {
                            moveTo(cX, cY - h * 0.45f)
                            lineTo(cX + w * 0.45f, cY - h * 0.15f)
                            lineTo(cX, cY + h * 0.15f)
                            lineTo(cX - w * 0.45f, cY - h * 0.15f)
                            close()
                        }
                        
                        val leftPath = Path().apply {
                            moveTo(cX - w * 0.45f, cY - h * 0.15f)
                            lineTo(cX, cY + h * 0.15f)
                            lineTo(cX, cY + h * 0.45f)
                            lineTo(cX - w * 0.45f, cY + h * 0.15f)
                            close()
                        }
                        
                        val rightPath = Path().apply {
                            moveTo(cX, cY + h * 0.15f)
                            lineTo(cX + w * 0.45f, cY - h * 0.15f)
                            lineTo(cX + w * 0.45f, cY + h * 0.15f)
                            lineTo(cX, cY + h * 0.45f)
                            close()
                        }
                        
                        if (isAvailable) {
                            drawCircle(
                                color = topColor.copy(alpha = 0.3f * pulse),
                                radius = w * 0.8f,
                                center = Offset(cX, cY)
                            )
                        }

                        drawPath(topPath, topColor)
                        drawPath(leftPath, leftColor)
                        drawPath(rightPath, rightColor)
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isAvailable) "REWARD CORE" else "CORE CLAIMED",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp
                        ),
                        color = if (isAvailable) Color(0xFF38BDF8) else Color.Gray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isAvailable) "Encrypted Mystery Box" else "Next drop tomorrow",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White
                    )
                }
                
                if (isAvailable) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF38BDF8).copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "OPEN",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF38BDF8),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
