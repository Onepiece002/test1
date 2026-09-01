package com.focusbyrj.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.focusbyrj.app.ui.navigation.Screen

@Composable
fun FocusBottomBar(
    items: List<Screen>,
    currentDestination: NavDestination?,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val containerBg = if (isDark) {
        Color(0xF2111317)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
    }
    val containerBorder = if (isDark) {
        Color.White.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .shadow(
                    elevation = 16.dp,
                    shape = CircleShape,
                    ambientColor = Color.Black.copy(alpha = 0.40f),
                    spotColor = Color.Black.copy(alpha = 0.50f)
                ),
            shape = CircleShape,
            color = containerBg,
            border = BorderStroke(0.85.dp, containerBorder)
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .height(52.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()

                    val targetPillBg = when {
                        selected && isDark -> Color.White.copy(alpha = 0.16f)
                        selected && !isDark -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        isPressed && isDark -> Color.White.copy(alpha = 0.08f)
                        isPressed && !isDark -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                        else -> Color.Transparent
                    }
                    val targetPillBorder = when {
                        selected && isDark -> Color.White.copy(alpha = 0.22f)
                        selected && !isDark -> MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                        else -> Color.Transparent
                    }

                    val animatedPillBg by animateColorAsState(
                        targetValue = targetPillBg,
                        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                        label = "pillBgAnim"
                    )
                    val animatedPillBorder by animateColorAsState(
                        targetValue = targetPillBorder,
                        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                        label = "pillBorderAnim"
                    )

                    val targetIconTint = when {
                        selected && isDark -> Color.White
                        selected && !isDark -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                    }
                    val animatedIconTint by animateColorAsState(
                        targetValue = targetIconTint,
                        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                        label = "iconTintAnim"
                    )

                    val pressScale by animateFloatAsState(
                        targetValue = if (isPressed) 0.92f else if (selected) 1.0f else 0.98f,
                        animationSpec = spring(
                            stiffness = Spring.StiffnessMediumLow,
                            dampingRatio = Spring.DampingRatioLowBouncy
                        ),
                        label = "pressScaleAnim"
                    )

                    val iconScale by animateFloatAsState(
                        targetValue = if (selected) 1.08f else 1.0f,
                        animationSpec = spring(
                            stiffness = Spring.StiffnessMediumLow,
                            dampingRatio = Spring.DampingRatioMediumBouncy
                        ),
                        label = "iconScaleAnim"
                    )

                    Box(
                        modifier = Modifier
                            .scale(pressScale)
                            .clip(CircleShape)
                            .background(animatedPillBg, CircleShape)
                            .border(0.75.dp, animatedPillBorder, CircleShape)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) {
                                if (!selected) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    kotlin.runCatching {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            }
                            .animateContentSize(
                                animationSpec = spring(
                                    stiffness = Spring.StiffnessMediumLow,
                                    dampingRatio = Spring.DampingRatioLowBouncy
                                )
                            )
                            .padding(
                                horizontal = if (selected) 16.dp else 12.dp,
                                vertical = 8.dp
                            )
                            .testTag("tab_${screen.route}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title,
                                tint = animatedIconTint,
                                modifier = Modifier
                                    .size(20.dp)
                                    .scale(iconScale)
                            )

                            AnimatedVisibility(
                                visible = selected,
                                enter = fadeIn(
                                    animationSpec = tween(durationMillis = 180, delayMillis = 40)
                                ) + expandHorizontally(
                                    animationSpec = spring(
                                        stiffness = Spring.StiffnessMediumLow,
                                        dampingRatio = Spring.DampingRatioLowBouncy
                                    ),
                                    expandFrom = Alignment.Start
                                ),
                                exit = fadeOut(
                                    animationSpec = tween(durationMillis = 120)
                                ) + shrinkHorizontally(
                                    animationSpec = spring(
                                        stiffness = Spring.StiffnessMedium,
                                        dampingRatio = Spring.DampingRatioNoBouncy
                                    ),
                                    shrinkTowards = Alignment.Start
                                )
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = screen.title,
                                        maxLines = 1,
                                        softWrap = false,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            letterSpacing = 0.15.sp
                                        ),
                                        color = if (isDark) Color.White else MaterialTheme.colorScheme.primary
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


