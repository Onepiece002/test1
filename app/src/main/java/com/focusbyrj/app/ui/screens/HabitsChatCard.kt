/*
 * Copyright (C) 2024-2026 Focus by Rj
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.focusbyrj.app.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusbyrj.app.FocusApplication
import com.focusbyrj.app.MainActivity
import com.focusbyrj.app.data.Habit
import com.focusbyrj.app.data.HabitType
import com.focusbyrj.app.data.HabitWithProgress
import com.focusbyrj.app.util.FocusEconomyManager
import com.focusbyrj.app.util.GamificationHaptics
import com.focusbyrj.app.util.HabitAlarmScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HabitsChatCard(
    message: ChatMessage,
    fontSizeSp: Float = 15f,
    onHabitLog: ((Long) -> Unit)? = null,
    onHabitCreated: ((Habit) -> Unit)? = null,
    onOpenFullTracker: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()
    val app = remember { context.applicationContext as FocusApplication }
    val habitRepo = remember { app.habitRepository }

    // Live observing habits from repository
    val activeHabitsWithProgress by habitRepo.activeHabitsWithProgress.collectAsState(initial = emptyList())

    // Card View Mode: false = List of habits, true = Enter a habit form
    var isCreatingHabit by remember { mutableStateOf(false) }

    val df = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val timeString = df.format(Date(message.timestamp))
    val maxBubbleWidth = (320 + (fontSizeSp - 15f) * 12f).coerceIn(320f, 400f).dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Start
        ) {
            // Ayva Avatar
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.Black)
                    .border(1.dp, Color(0x33FFFFFF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.focusbyrj.app.R.drawable.ic_app_logo),
                    contentDescription = "Ayva",
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))

            // Main Card Container
            Surface(
                modifier = Modifier.widthIn(max = maxBubbleWidth),
                shape = RoundedCornerShape(
                    topStart = 20.dp,
                    topEnd = 20.dp,
                    bottomStart = 4.dp,
                    bottomEnd = 20.dp
                ),
                color = if (isDark) Color(0xFF121824) else Color(0xFFF8FAFC),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = if (isDark) 0.15f else 0.25f)
                ),
                shadowElevation = 4.dp
            ) {
                AnimatedContent(
                    targetState = isCreatingHabit,
                    transitionSpec = {
                        (slideInVertically { height -> height / 2 } + fadeIn()) togetherWith
                                (slideOutVertically { height -> -height / 2 } + fadeOut())
                    },
                    label = "HabitCardModeTransition"
                ) { creating ->
                    if (!creating) {
                        // -------------------------------------------------------------
                        // 1. HABITS LIST VIEW
                        // -------------------------------------------------------------
                        HabitsListView(
                            habits = activeHabitsWithProgress,
                            fontSizeSp = fontSizeSp,
                            isDark = isDark,
                            onPlusClicked = { isCreatingHabit = true },
                            onLogHabit = { habitId ->
                                coroutineScope.launch(Dispatchers.IO) {
                                    try {
                                        habitRepo.incrementHabitProgress(habitId)
                                        withContext(Dispatchers.Main) {
                                            GamificationHaptics.playSuccess(context)
                                            FocusEconomyManager.addRewards(baseXp = 15, baseGold = 5)
                                            onHabitLog?.invoke(habitId)
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Error logging habit: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            onOpenFullTracker = {
                                if (onOpenFullTracker != null) {
                                    onOpenFullTracker()
                                } else {
                                    val intent = Intent(context, MainActivity::class.java).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                        putExtra("NAV_DESTINATION", "habits")
                                    }
                                    context.startActivity(intent)
                                }
                            }
                        )
                    } else {
                        // -------------------------------------------------------------
                        // 2. ENTER HABIT (OPTIONS & CREATION) VIEW
                        // -------------------------------------------------------------
                        EnterHabitView(
                            isDark = isDark,
                            fontSizeSp = fontSizeSp,
                            onBack = { isCreatingHabit = false },
                            onCreateHabit = { newHabit ->
                                coroutineScope.launch(Dispatchers.IO) {
                                    try {
                                        val createdId = habitRepo.insertHabit(newHabit)
                                        val scheduledHabit = newHabit.copy(id = createdId)
                                        HabitAlarmScheduler.scheduleHabitReminder(context, scheduledHabit)
                                        withContext(Dispatchers.Main) {
                                            GamificationHaptics.playCelebration(context)
                                            FocusEconomyManager.addRewards(baseXp = 25, baseGold = 10)
                                            Toast.makeText(context, "🎯 Habit '${newHabit.title}' added! (+25 XP)", Toast.LENGTH_SHORT).show()
                                            onHabitCreated?.invoke(scheduledHabit)
                                            isCreatingHabit = false
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Failed to create habit: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// SUB-VIEW 1: HABITS LIST VIEW
// =============================================================================
@Composable
private fun HabitsListView(
    habits: List<HabitWithProgress>,
    fontSizeSp: Float,
    isDark: Boolean,
    onPlusClicked: () -> Unit,
    onLogHabit: (Long) -> Unit,
    onOpenFullTracker: () -> Unit
) {
    Column(modifier = Modifier.padding(14.dp)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.TrackChanges,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Habit Radar",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = (fontSizeSp * 1.0f).sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                if (habits.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        val completedCount = habits.count { it.isCompletedToday }
                        Text(
                            text = "$completedCount/${habits.size} done",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // The PLUS Button to flip into Enter Habit View
            Surface(
                onClick = onPlusClicked,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shadowElevation = 2.dp,
                modifier = Modifier.size(30.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add Habit",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Empty state
        if (habits.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isDark) Color(0xFF1A2232) else Color.White,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "🎯", fontSize = 28.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "No habits tracked yet",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Start small! Hydration, posture, or reading daily routines.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onPlusClicked,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create First Habit", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        } else {
            // Habit Cards List
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                habits.forEach { hp ->
                    val habit = hp.habit
                    val habitColor = parseHexColor(habit.colorHex)
                    val isDone = hp.isCompletedToday
                    val progressFraction = hp.progressFraction

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isDark) Color(0xFF161E2E) else Color.White,
                        border = BorderStroke(
                            1.dp,
                            if (isDone) habitColor.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = if (isDark) 0.12f else 0.18f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Emoji Icon Box
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(habitColor.copy(alpha = if (isDark) 0.2f else 0.12f))
                                        .border(1.dp, habitColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = habit.iconEmoji, fontSize = 18.sp)
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                // Title, Schedule, and Streak
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = habit.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = (fontSizeSp * 0.95f).sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )

                                        if (hp.currentStreak > 0) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFFF97316).copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = "🔥 ${hp.currentStreak}d",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = Color(0xFFF97316),
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    // Schedule subtext
                                    val scheduleDesc = if (habit.type == HabitType.INTERVAL_WINDOW) {
                                        "Every ${habit.formattedInterval} • ${habit.formattedWindow}"
                                    } else {
                                        "Daily at ${formatTime(habit.fixedReminderHour, habit.fixedReminderMinute)}"
                                    }

                                    Text(
                                        text = scheduleDesc,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Log Action Button (+1 or Done)
                                Surface(
                                    onClick = { onLogHabit(habit.id) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isDone) habitColor else habitColor.copy(alpha = if (isDark) 0.2f else 0.12f),
                                    contentColor = if (isDone) Color.White else habitColor,
                                    border = BorderStroke(1.dp, habitColor.copy(alpha = if (isDone) 1f else 0.4f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (isDone) {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = "Completed",
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "Done",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp
                                                )
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Filled.Add,
                                                contentDescription = "Log progress",
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = "1",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Progress Bar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(progressFraction)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(habitColor)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${hp.completedToday}/${hp.targetToday}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.5.sp
                                    ),
                                    color = if (isDone) habitColor else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Footer Actions Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onPlusClicked,
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Habit", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold))
            }

            TextButton(
                onClick = onOpenFullTracker,
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text("Open Full Tracker", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold))
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(12.dp))
            }
        }
    }
}

// =============================================================================
// SUB-VIEW 2: ENTER HABIT (OPTIONS & FORM) VIEW
// =============================================================================
@Composable
private fun EnterHabitView(
    isDark: Boolean,
    fontSizeSp: Float,
    onBack: () -> Unit,
    onCreateHabit: (Habit) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var iconEmoji by remember { mutableStateOf("🥤") }
    var colorHex by remember { mutableStateOf("#38BDF8") }
    var habitType by remember { mutableStateOf(HabitType.INTERVAL_WINDOW) }
    var targetPerDay by remember { mutableIntStateOf(6) }
    var intervalHours by remember { mutableIntStateOf(2) }
    var intervalMinutes by remember { mutableIntStateOf(0) }
    var fixedHour by remember { mutableIntStateOf(9) }
    var fixedMinute by remember { mutableIntStateOf(0) }
    var windowStartHour by remember { mutableIntStateOf(8) }
    var windowStartMinute by remember { mutableIntStateOf(0) }
    var windowEndHour by remember { mutableIntStateOf(20) }
    var windowEndMinute by remember { mutableIntStateOf(0) }

    val activeColor = parseHexColor(colorHex)

    val activeWindowMinutes by remember {
        derivedStateOf {
            val startMins = windowStartHour * 60 + windowStartMinute
            val endMins = windowEndHour * 60 + windowEndMinute
            if (endMins > startMins) endMins - startMins else (24 * 60 - startMins + endMins).coerceAtLeast(15)
        }
    }

    fun autoSplit(target: Int) {
        val step = (activeWindowMinutes / target.coerceAtLeast(1)).coerceAtLeast(5)
        intervalHours = step / 60
        intervalMinutes = step % 60
    }

    // Preset Habit Templates for 1-tap fill
    val presets = remember {
        listOf(
            PresetChoice("Drink Water", "🥤", "#38BDF8", HabitType.INTERVAL_WINDOW, 8, 1, 30, 8, 0, 20, 0, 9, 0),
            PresetChoice("Medicine", "💊", "#F43F5E", HabitType.ONCE_DAILY, 1, 2, 0, 8, 0, 20, 0, 9, 0),
            PresetChoice("Read 20m", "📖", "#A855F7", HabitType.ONCE_DAILY, 1, 2, 0, 8, 0, 20, 0, 21, 0),
            PresetChoice("Posture", "🧍", "#10B981", HabitType.INTERVAL_WINDOW, 4, 2, 15, 10, 0, 19, 0, 9, 0),
            PresetChoice("Meditate", "🧘", "#F59E0B", HabitType.ONCE_DAILY, 1, 2, 0, 8, 0, 20, 0, 7, 30),
            PresetChoice("Workout", "🏃", "#3B82F6", HabitType.ONCE_DAILY, 1, 2, 0, 8, 0, 20, 0, 7, 0)
        )
    }

    val emojiChoices = listOf("🥤", "💊", "📖", "🧍", "🧘", "🏃", "🎯", "🍎", "💪", "💧", "✨", "💤")
    val colorChoices = listOf("#38BDF8", "#F43F5E", "#A855F7", "#10B981", "#F59E0B", "#3B82F6", "#EC4899")

    Column(modifier = Modifier.padding(14.dp)) {
        // Header with Back Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    onClick = onBack,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "New Habit",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = (fontSizeSp * 1.0f).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            IconButton(
                onClick = onBack,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 1. Quick Presets Bar
        Text(
            text = "Quick Presets",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 10.sp
            )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            presets.forEach { preset ->
                val isSelected = title == preset.title
                Surface(
                    onClick = {
                        title = preset.title
                        iconEmoji = preset.emoji
                        colorHex = preset.colorHex
                        habitType = preset.type
                        targetPerDay = preset.target
                        intervalHours = preset.intervalH
                        intervalMinutes = preset.intervalM
                        windowStartHour = preset.wStartH
                        windowStartMinute = preset.wStartM
                        windowEndHour = preset.wEndH
                        windowEndMinute = preset.wEndM
                        fixedHour = preset.fixedH
                        fixedMinute = preset.fixedM
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) activeColor else (if (isDark) Color(0xFF1E2838) else Color(0xFFE2E8F0)),
                    contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                    border = BorderStroke(1.dp, if (isSelected) activeColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(preset.emoji, fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            preset.title,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 10.5.sp
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Habit Title Input
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            placeholder = {
                Text(
                    "Habit title (e.g. Read 10 pages)",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)
                )
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = activeColor,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                focusedContainerColor = if (isDark) Color(0xFF161E2E) else Color.White,
                unfocusedContainerColor = if (isDark) Color(0xFF161E2E) else Color.White
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 3. Emoji & Color Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Emoji Picker Chips
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                emojiChoices.forEach { emoji ->
                    val selected = iconEmoji == emoji
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (selected) activeColor.copy(alpha = 0.25f) else Color.Transparent)
                            .border(
                                width = if (selected) 1.5.dp else 0.5.dp,
                                color = if (selected) activeColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                shape = CircleShape
                            )
                            .clickable { iconEmoji = emoji },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(emoji, fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Color Picker Dots
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                colorChoices.take(4).forEach { hex ->
                    val color = parseHexColor(hex)
                    val selected = colorHex == hex
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (selected) 2.dp else 0.dp,
                                color = Color.White,
                                shape = CircleShape
                            )
                            .clickable { colorHex = hex }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 4. Frequency Type Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Once Daily Option
            Surface(
                onClick = {
                    habitType = HabitType.ONCE_DAILY
                    targetPerDay = 1
                },
                shape = RoundedCornerShape(10.dp),
                color = if (habitType == HabitType.ONCE_DAILY) activeColor.copy(alpha = 0.18f) else (if (isDark) Color(0xFF161E2E) else Color.White),
                border = BorderStroke(1.dp, if (habitType == HabitType.ONCE_DAILY) activeColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = if (habitType == HabitType.ONCE_DAILY) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Once Daily",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (habitType == HabitType.ONCE_DAILY) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 10.sp
                        ),
                        color = if (habitType == HabitType.ONCE_DAILY) activeColor else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Interval Window Option
            Surface(
                onClick = {
                    habitType = HabitType.INTERVAL_WINDOW
                    if (targetPerDay == 1) targetPerDay = 4
                },
                shape = RoundedCornerShape(10.dp),
                color = if (habitType == HabitType.INTERVAL_WINDOW) activeColor.copy(alpha = 0.18f) else (if (isDark) Color(0xFF161E2E) else Color.White),
                border = BorderStroke(1.dp, if (habitType == HabitType.INTERVAL_WINDOW) activeColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Repeat,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = if (habitType == HabitType.INTERVAL_WINDOW) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Periodic Window",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (habitType == HabitType.INTERVAL_WINDOW) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 10.sp
                        ),
                        color = if (habitType == HabitType.INTERVAL_WINDOW) activeColor else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 5. Config Details for Selected Habit Type
        if (habitType == HabitType.ONCE_DAILY) {
            // Time presets
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    Pair("8 AM", 8),
                    Pair("12 PM", 12),
                    Pair("6 PM", 18),
                    Pair("9 PM", 21)
                ).forEach { (label, h) ->
                    val selected = fixedHour == h
                    Surface(
                        onClick = {
                            fixedHour = h
                            fixedMinute = 0
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = if (selected) activeColor else (if (isDark) Color(0xFF161E2E) else Color.White),
                        border = BorderStroke(1.dp, if (selected) activeColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 9.5.sp
                                ),
                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        } else {
            val currentIntervalText = when {
                intervalHours > 0 && intervalMinutes > 0 -> "${intervalHours}h ${intervalMinutes}m"
                intervalHours > 0 -> "${intervalHours}h"
                else -> "${intervalMinutes}m"
            }
            val winHours = activeWindowMinutes / 60
            val winRemMins = activeWindowMinutes % 60
            val winText = if (winRemMins > 0) "${winHours}h ${winRemMins}m" else "${winHours}h"

            // Target per day chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daily Target:",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(2, 4, 6, 8).forEach { count ->
                        val selected = targetPerDay == count
                        Surface(
                            onClick = {
                                targetPerDay = count
                                autoSplit(count)
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) activeColor else (if (isDark) Color(0xFF161E2E) else Color.White),
                            border = BorderStroke(1.dp, if (selected) activeColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            Text(
                                text = "${count}x",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 9.5.sp
                                ),
                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Auto-division pill
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = activeColor.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, activeColor.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚡ $winText window cut into $targetPerDay parts (~$currentIntervalText each)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Frequency Interval options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Interval:",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val intervalOptions = listOf(
                        Triple(1, 0, "1h"),
                        Triple(1, 30, "1.5h"),
                        Triple(2, 0, "2h"),
                        Triple(3, 0, "3h")
                    )
                    intervalOptions.forEach { (h, m, label) ->
                        val selected = intervalHours == h && intervalMinutes == m
                        Surface(
                            onClick = {
                                intervalHours = h
                                intervalMinutes = m
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) activeColor else (if (isDark) Color(0xFF161E2E) else Color.White),
                            border = BorderStroke(1.dp, if (selected) activeColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            Text(
                                text = "Every $label",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 9.5.sp
                                ),
                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 6. Action Buttons: Cancel and Create Habit
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                Text("Cancel", style = MaterialTheme.typography.labelMedium)
            }

            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val newHabit = Habit(
                            title = title.trim(),
                            description = description.trim(),
                            iconEmoji = iconEmoji,
                            colorHex = colorHex,
                            type = habitType,
                            targetPerDay = targetPerDay,
                            intervalHours = intervalHours,
                            intervalMinutes = intervalMinutes,
                            fixedReminderHour = fixedHour,
                            fixedReminderMinute = fixedMinute,
                            windowStartHour = windowStartHour,
                            windowStartMinute = windowStartMinute,
                            windowEndHour = windowEndHour,
                            windowEndMinute = windowEndMinute
                        )
                        onCreateHabit(newHabit)
                    }
                },
                enabled = title.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = activeColor,
                    contentColor = Color.White,
                    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                ),
                modifier = Modifier.weight(1.5f),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Create Habit",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

// Data class helper for quick preset selection
private data class PresetChoice(
    val title: String,
    val emoji: String,
    val colorHex: String,
    val type: HabitType,
    val target: Int,
    val intervalH: Int,
    val intervalM: Int = 0,
    val wStartH: Int = 8,
    val wStartM: Int = 0,
    val wEndH: Int = 20,
    val wEndM: Int = 0,
    val fixedH: Int = 9,
    val fixedM: Int = 0
)

private fun parseHexColor(hex: String, defaultColor: Color = Color(0xFF38BDF8)): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        defaultColor
    }
}

private fun formatTime(hour: Int, minute: Int): String {
    val h = if (hour == 0 || hour == 12) 12 else hour % 12
    val amPm = if (hour < 12) "AM" else "PM"
    return String.format(Locale.getDefault(), "%d:%02d %s", h, minute, amPm)
}
