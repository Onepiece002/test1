/*
 * Copyright (C) 2024-2026 Focus by Rj
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.focusbyrj.app.ui.screens

import com.focusbyrj.app.ui.theme.MidnightBlack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.draw.scale
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.focusbyrj.app.MainActivity
import com.focusbyrj.app.data.AppRestriction
import com.focusbyrj.app.ui.theme.*
import com.focusbyrj.app.util.DeviceStatsHelper
import com.focusbyrj.app.util.DndHelper
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    restrictions: List<AppRestriction> = emptyList(),
    onToggle: (AppRestriction) -> Unit = {},
    onDelete: (AppRestriction) -> Unit = {},
    onUpdate: (AppRestriction) -> Unit = {},
    isSessionActive: Boolean = false,
    timeRemaining: Long = 25 * 60L,
    initialTime: Long = 25 * 60L,
    onToggleSession: () -> Unit = {},
    onSetTime: (Int) -> Unit = {}
) {
    if (isSessionActive) {
        ActiveSessionScreen(timeRemaining = timeRemaining, initialTime = initialTime, onToggleSession = onToggleSession)
    } else {
        NormalDashboard(
            restrictions = restrictions,
            onToggle = onToggle,
            onDelete = onDelete,
            onUpdate = onUpdate,
            timeRemaining = timeRemaining,
            onToggleSession = onToggleSession,
            onSetTime = onSetTime
        )
    }
}

@Composable
fun NormalDashboard(
    restrictions: List<AppRestriction>,
    onToggle: (AppRestriction) -> Unit,
    onDelete: (AppRestriction) -> Unit = {},
    onUpdate: (AppRestriction) -> Unit = {},
    timeRemaining: Long,
    onToggleSession: () -> Unit,
    onSetTime: (Int) -> Unit
) {
    var editingApp by remember { mutableStateOf<AppRestriction?>(null) }

    if (editingApp != null) {
        EditRestrictionBottomSheet(
            app = editingApp!!,
            onDismiss = { editingApp = null },
            onSave = { updated ->
                onUpdate(updated)
                editingApp = null
            },
            onDelete = { toDelete ->
                onDelete(toDelete)
                editingApp = null
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))

            val context = LocalContext.current
            val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
            var hasUsageStats by remember { mutableStateOf(com.focusbyrj.app.util.PermissionUtils.hasUsageStatsPermission(context)) }
            var hasOverlay by remember { mutableStateOf(com.focusbyrj.app.util.PermissionUtils.hasOverlayPermission(context)) }
            var isBatteryUnrestricted by remember { mutableStateOf(com.focusbyrj.app.util.PermissionUtils.isIgnoringBatteryOptimizations(context)) }
            
            DisposableEffect(lifecycleOwner) {
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                        hasUsageStats = com.focusbyrj.app.util.PermissionUtils.hasUsageStatsPermission(context)
                        hasOverlay = com.focusbyrj.app.util.PermissionUtils.hasOverlayPermission(context)
                        isBatteryUnrestricted = com.focusbyrj.app.util.PermissionUtils.isIgnoringBatteryOptimizations(context)
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            if (!hasUsageStats || !hasOverlay || !isBatteryUnrestricted) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .clickable {
                            if (!hasUsageStats) {
                                com.focusbyrj.app.util.PermissionUtils.requestUsageStatsPermission(context)
                            } else if (!hasOverlay) {
                                com.focusbyrj.app.util.PermissionUtils.requestOverlayPermission(context)
                            } else if (!isBatteryUnrestricted) {
                                com.focusbyrj.app.util.PermissionUtils.requestIgnoreBatteryOptimizations(context)
                            }
                        }
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Warning, contentDescription = "Warning", tint = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            val title = when {
                                !hasUsageStats -> "Usage Access Required"
                                !hasOverlay -> "Display Over Apps Required"
                                else -> "Battery: Set to 'No Restrictions'"
                            }
                            val subtitle = when {
                                !hasUsageStats -> "Tap to grant Usage Access to detect running apps."
                                !hasOverlay -> "Tap to allow displaying block overlay over apps."
                                else -> "Tap to set Unrestricted battery so Android doesn't kill focus locks."
                            }
                            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
                            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            DeepWorkCard(timeRemaining = timeRemaining, onToggleSession = onToggleSession, onSetTime = onSetTime)
            Spacer(modifier = Modifier.height(12.dp))
            StreakAndShieldedSection(restrictions = restrictions)
            Spacer(modifier = Modifier.height(12.dp))
            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your boundaries",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Swipe to edit or delete",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (restrictions.isEmpty()) {
            item { EmptyStateView() }
        } else {
            items(restrictions, key = { it.packageName }) { app ->
                SwipeableAppRestrictionCard(
                    app = app,
                    onToggle = { onToggle(app) },
                    onEdit = { editingApp = app },
                    onDelete = { onDelete(app) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun DeepWorkCard(
    timeRemaining: Long,
    onToggleSession: () -> Unit,
    onSetTime: (Int) -> Unit
) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    var sliderValue by remember { mutableStateOf((timeRemaining / 60).toFloat()) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Set Focus Duration") },
            text = {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1E1E2E), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = { if (sliderValue > 5f) sliderValue -= 5f },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF2A2A3A), RoundedCornerShape(8.dp))
                        ) {
                            Icon(androidx.compose.material.icons.Icons.Filled.Remove, contentDescription = "Decrease", tint = Color.White)
                        }
                        
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "${sliderValue.toInt()}",
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "min",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.Gray,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        IconButton(
                            onClick = { if (sliderValue < 120f) sliderValue += 5f },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF2A2A3A), RoundedCornerShape(8.dp))
                        ) {
                            Icon(androidx.compose.material.icons.Icons.Filled.Add, contentDescription = "Increase", tint = Color.White)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onSetTime(sliderValue.toInt())
                    showDialog = false
                }) { Text("Save", color = AccentCyan) }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel", color = Color.Gray) }
            },
            containerColor = SurfaceVariantDark,
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 140.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceVariantDark)
            .border(1.dp, BorderGlass, RoundedCornerShape(24.dp))
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column {
                    Text(
                        text = "DEEP FOCUS",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${timeRemaining / 60} min",
                        style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFCBD5E1),
                        modifier = Modifier.clickable { showDialog = true }
                    )
                }
                
                Button(
                    onClick = {
                        if (!DndHelper.hasDndPermission(context)) {
                            DndHelper.requestDndPermission(context)
                        } else {
                            DndHelper.setDndMode(context, true)
                            onToggleSession()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = MidnightBlack
                    ),
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text("Start", fontWeight = FontWeight.Bold)
                }
            }
            Text("Tap time to edit duration", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        }
    }
}


@Composable
fun ActiveSessionScreen(timeRemaining: Long, initialTime: Long, onToggleSession: () -> Unit) {
    val progress = if (initialTime > 0) (timeRemaining.toFloat() / initialTime.toFloat()) else 0f
    val minutes = timeRemaining / 60
    val seconds = timeRemaining % 60
    val timeString = String.format("%02d:%02d", minutes, seconds)
    val context = LocalContext.current
    
    
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val glowOpacity by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowOpacity"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightBlack)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        
        Box(
            modifier = Modifier
                .size(400.dp)
                .scale(scale)
                .background(
                    androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(AccentCyan.copy(alpha = glowOpacity), Color.Transparent)
                    ),
                    shape = CircleShape
                )
        )
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "DEEP WORK",
                style = MaterialTheme.typography.labelMedium,
                color = AccentCyan,
                letterSpacing = 6.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(48.dp))
            
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(280.dp)
                    .scale(scale)
            ) {
                
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = Color(0xFF1E2030),
                        style = Stroke(width = 12.dp.toPx())
                    )
                    
                    
                    drawArc(
                        color = AccentCyan,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = timeString,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 68.sp
                        ),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (scale > 1f) "Breathe In..." else "Breathe Out...",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFCBD5E1).copy(alpha = 0.5f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(80.dp))
            
            
            Button(
                onClick = {
                    DndHelper.setDndMode(context, false)
                    onToggleSession()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF43F5E).copy(alpha = 0.1f), contentColor = Color(0xFFF43F5E)),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF43F5E).copy(alpha = 0.3f)),
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp)
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("End Early", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}


@Composable
fun EmptyStateView() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(SurfaceVariantDark)
            .border(1.dp, BorderGlass, RoundedCornerShape(28.dp))
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.LockOpen,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No apps locked",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tap + to add a distraction",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


@Composable
fun StreakAndShieldedSection(restrictions: List<AppRestriction>) {
    val context = LocalContext.current
    val stats by com.focusbyrj.app.util.FocusStatsManager.statsFlow.collectAsState()
    val heatmapTheme by com.focusbyrj.app.util.FocusStatsManager.themeFlow.collectAsState()
    
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                com.focusbyrj.app.util.FocusStatsManager.refreshStats(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Row(
        modifier = Modifier.fillMaxWidth().height(120.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceDark)
                .border(1.dp, BorderGlass, RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Column {
                Text("STREAK", style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp, fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.weight(1f))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("${stats.currentStreak}", style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp), color = heatmapTheme.colors.last())
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("days", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    heatmapTheme.colors.forEach { color ->
                        Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(color))
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceDark)
                .border(1.dp, BorderGlass, RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Column {
                Text("SHIELDED", style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp, fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.weight(1f))
                
                Row(modifier = Modifier.padding(vertical = 4.dp)) {
                    val activeRestrictions = restrictions.filter { it.isRestricted }.take(4)
                    
                    if (activeRestrictions.isEmpty()) {
                        Text("No active boundaries", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    } else {
                        activeRestrictions.forEachIndexed { index, app ->
                            val pm = context.packageManager
                            val icon = remember(app.packageName) {
                                com.focusbyrj.app.util.ImageUtils.getAppIcon(pm, app.packageName)
                            }
                            
                            Box(
                                modifier = Modifier
                                    .offset(x = (-4 * index).dp)
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SurfaceVariantDark)
                                    .border(2.dp, MidnightBlack, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (icon != null) {
                                    androidx.compose.foundation.Image(bitmap = icon, contentDescription = null, modifier = Modifier.fillMaxSize().padding(4.dp))
                                } else {
                                    val text = app.appName.take(2).uppercase()
                                    Text(text, color = Color(0xFFCBD5E1), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
                Text("${restrictions.count { it.isRestricted }} shielded apps", style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
@Composable
fun SwipeableAppRestrictionCard(
    app: AppRestriction,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val maxActionWidthDp = 84.dp
    val maxActionWidthPx = with(density) { maxActionWidthDp.toPx() }

    val currentOffset = offsetX.value
    val isSwipingRight = currentOffset > 1f
    val isSwipingLeft = currentOffset < -1f

    val editProgress = (currentOffset / maxActionWidthPx).coerceIn(0f, 1f)
    val deleteProgress = (-currentOffset / maxActionWidthPx).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(18.dp))
    ) {
        if (isSwipingRight) {
            Box(
                modifier = Modifier
                    .width(maxActionWidthDp)
                    .fillMaxHeight()
                    .align(Alignment.CenterStart)
                    .clip(RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp))
                    .background(Color(0xFF282D42))
                    .clickable {
                        coroutineScope.launch {
                            offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
                        }
                        onEdit()
                    }
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.graphicsLayer {
                        alpha = editProgress
                        scaleX = 0.75f + 0.25f * editProgress
                        scaleY = 0.75f + 0.25f * editProgress
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit Restriction",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Edit",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFCBD5E1),
                        fontSize = 11.sp
                    )
                }
            }
        }

        if (isSwipingLeft) {
            Box(
                modifier = Modifier
                    .width(maxActionWidthDp)
                    .fillMaxHeight()
                    .align(Alignment.CenterEnd)
                    .clip(RoundedCornerShape(topEnd = 18.dp, bottomEnd = 18.dp))
                    .background(
                        AccentRose
                    )
                    .clickable {
                        coroutineScope.launch {
                            offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
                        }
                        onDelete()
                    }
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.graphicsLayer {
                        alpha = deleteProgress
                        scaleX = 0.75f + 0.25f * deleteProgress
                        scaleY = 0.75f + 0.25f * deleteProgress
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete Restriction",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Delete",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFCBD5E1),
                        fontSize = 11.sp
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            coroutineScope.launch {
                                val current = offsetX.value
                                val target = when {
                                    current > maxActionWidthPx * 0.4f -> maxActionWidthPx
                                    current < -maxActionWidthPx * 0.4f -> -maxActionWidthPx
                                    else -> 0f
                                }
                                offsetX.animateTo(
                                    targetValue = target,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )
                            }
                        },
                        onDragCancel = {
                            coroutineScope.launch {
                                offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            coroutineScope.launch {
                                val newOffset = (offsetX.value + dragAmount).coerceIn(
                                    -maxActionWidthPx * 1.15f,
                                    maxActionWidthPx * 1.15f
                                )
                                offsetX.snapTo(newOffset)
                            }
                        }
                    )
                }
        ) {
            AppRestrictionCard(
                app = app,
                onToggle = {
                    if (kotlin.math.abs(offsetX.value) > 10f) {
                        coroutineScope.launch {
                            offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
                        }
                    } else {
                        onToggle()
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRestrictionBottomSheet(
    app: AppRestriction,
    onDismiss: () -> Unit,
    onSave: (AppRestriction) -> Unit,
    onDelete: (AppRestriction) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    var selectedMode by remember { mutableStateOf(app.mode) }
    var customQuote by remember { mutableStateOf(app.customQuote) }
    var isShieldActive by remember { mutableStateOf(app.isRestricted) }
    val context = LocalContext.current

    val sheetBackground = Color(0xFF12141F)
    val cardBackground = Color(0xFF191C2B)
    val cardBorder = Color(0xFF282D42)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetBackground,
        scrimColor = Color.Black.copy(alpha = 0.65f),
        tonalElevation = 0.dp,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = Color(0xFF3B4158),
                height = 4.dp,
                width = 44.dp
            )
        },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(sheetBackground)
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .navigationBarsPadding()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                val pm = context.packageManager
                val icon = remember(app.packageName) {
                    com.focusbyrj.app.util.ImageUtils.getAppIcon(pm, app.packageName)
                }
                if (icon != null) {
                    androidx.compose.foundation.Image(
                        bitmap = icon,
                        contentDescription = null,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(cardBackground)
                            .border(1.dp, cardBorder, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = app.appName.take(2).uppercase(),
                            color = Color(0xFFCBD5E1),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "EDIT RESTRICTION",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = AccentCyan
                    )
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFCBD5E1),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF261922))
                        .border(1.dp, Color(0x4DEF4444), CircleShape)
                        .clickable {
                            coroutineScope.launch {
                                sheetState.hide()
                                onDismiss()
                                onDelete(app)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete Restriction",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(cardBackground)
                    .border(1.dp, cardBorder, RoundedCornerShape(18.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Shield Active",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isShieldActive) "Blocking enabled for this app" else "Blocking is currently paused",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isShieldActive) NeonGreen else Color(0xFF94A3B8)
                        )
                    }
                    Switch(
                        checked = isShieldActive,
                        onCheckedChange = { isShieldActive = it },
                        modifier = Modifier.scale(0.85f),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = Color(0xFF94A3B8),
                            uncheckedTrackColor = Color(0xFF282D42)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "RESTRICTION MODE",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = AccentCyan,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ModeSelector(
                    title = "HARD MODE",
                    description = "No bypass allowed",
                    isSelected = selectedMode == "HARD",
                    onClick = { selectedMode = "HARD" },
                    modifier = Modifier.weight(1f)
                )
                ModeSelector(
                    title = "SOFT MODE",
                    description = "10 sec wait bypass",
                    isSelected = selectedMode == "SOFT",
                    onClick = { selectedMode = "SOFT" },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "CUSTOM QUOTE (OPTIONAL)",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = AccentCyan,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = customQuote,
                onValueChange = { customQuote = it },
                placeholder = { Text("Why are you blocking this app?", color = Color(0xFF64748B)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = cardBackground,
                    unfocusedContainerColor = cardBackground,
                    focusedBorderColor = AccentViolet,
                    unfocusedBorderColor = cardBorder,
                    focusedTextColor = Color(0xFFCBD5E1),
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    coroutineScope.launch {
                        val updated = app.copy(
                            mode = selectedMode,
                            customQuote = customQuote.trim(),
                            isRestricted = isShieldActive
                        )
                        sheetState.hide()
                        onDismiss()
                        onSave(updated)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = MidnightBlack
                ),
                shape = RoundedCornerShape(27.dp)
            ) {
                Text(
                    "Save Changes",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun AppRestrictionCard(app: AppRestriction, onToggle: () -> Unit) {
    val isLocked = app.isRestricted
    val cardColor = if (isLocked) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
    val borderColor = if (isLocked) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else BorderGlass
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.background)
            .background(cardColor)
            .border(1.dp, borderColor, RoundedCornerShape(18.dp))
            .clickable { onToggle() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                val pm = LocalContext.current.packageManager
                val icon = remember(app.packageName) { com.focusbyrj.app.util.ImageUtils.getAppIcon(pm, app.packageName) }
                if (icon != null) {
                    androidx.compose.foundation.Image(
                        bitmap = icon,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.background),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(app.appName.take(2).uppercase(), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isLocked) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = app.mode.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Switch(
                checked = isLocked,
                onCheckedChange = { onToggle() },
                modifier = Modifier.scale(0.85f),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.background,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    uncheckedTrackColor = MaterialTheme.colorScheme.background
                )
            )
        }
    }
}
