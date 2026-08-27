package com.focusbyrj.app.ui.screens

import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BubbleSettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("bubble_prefs", Context.MODE_PRIVATE) }
    
    var isBubbleEnabled by remember { mutableStateOf(prefs.getBoolean("bubble_enabled", false)) }
    var morningBriefTime by remember { mutableStateOf(prefs.getString("morning_brief_time", "08:00 AM") ?: "08:00 AM") }
    var eveningBriefTime by remember { mutableStateOf(prefs.getString("evening_brief_time", "08:00 PM") ?: "08:00 PM") }
    var smartRegexEnabled by remember { mutableStateOf(prefs.getBoolean("smart_regex_enabled", true)) }
    var autoHideEnabled by remember { mutableStateOf(prefs.getBoolean("auto_hide_enabled", false)) }
    var autoHideDuration by remember { mutableStateOf(prefs.getInt("auto_hide_duration_sec", 3)) }
    var hideInLandscape by remember { mutableStateOf(prefs.getBoolean("hide_in_landscape", true)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Bubble Settings",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.testTag("bubble_settings_back_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                
                // --- GENERAL / APPEARANCE ---
                SettingsSectionHeader(title = "GENERAL & APPEARANCE")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SettingsSwitchRow(
                            icon = Icons.Filled.Chat,
                            title = "Enable Chat Bubble",
                            subtitle = "Show floating assistant icon across apps",
                            checked = isBubbleEnabled,
                            onCheckedChange = { 
                                isBubbleEnabled = it 
                                prefs.edit().putBoolean("bubble_enabled", it).apply()
                                
                                val intent = android.content.Intent(context, com.focusbyrj.app.service.BubbleService::class.java)
                                if (it) {
                                    context.startService(intent)
                                } else {
                                    context.stopService(intent)
                                }
                            }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )
                        SettingsStepperRow(
                            icon = Icons.Filled.VisibilityOff,
                            title = "Auto-hide Bubble",
                            subtitle = "Shrink bubble to edge after inactivity",
                            valueText = if (autoHideEnabled) "${autoHideDuration}s" else "Off",
                            onDecrement = {
                                if (autoHideDuration > 1 && autoHideEnabled) {
                                    autoHideDuration -= 1
                                    prefs.edit().putBoolean("auto_hide_enabled", true)
                                        .putInt("auto_hide_duration_sec", autoHideDuration).apply()
                                } else if (autoHideEnabled) {
                                    autoHideEnabled = false
                                    prefs.edit().putBoolean("auto_hide_enabled", false).apply()
                                }
                            },
                            onIncrement = {
                                if (!autoHideEnabled) {
                                    autoHideEnabled = true
                                    autoHideDuration = 1
                                    prefs.edit().putBoolean("auto_hide_enabled", true)
                                        .putInt("auto_hide_duration_sec", 1).apply()
                                } else if (autoHideDuration < 15) {
                                    autoHideDuration += 1
                                    prefs.edit().putBoolean("auto_hide_enabled", true)
                                        .putInt("auto_hide_duration_sec", autoHideDuration).apply()
                                }
                            }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )
                        SettingsSwitchRow(
                            icon = Icons.Filled.AutoFixHigh,
                            title = "Smart Regex Parsing",
                            subtitle = "Automatically parse priority and tags in chat",
                            checked = smartRegexEnabled,
                            onCheckedChange = { 
                                smartRegexEnabled = it 
                                prefs.edit().putBoolean("smart_regex_enabled", it).apply()
                            }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )
                        SettingsSwitchRow(
                            icon = Icons.Filled.ScreenRotation,
                            title = "Hide in Landscape / Video Mode",
                            subtitle = "Instantly hide bubble during video playback and landscape orientation",
                            checked = hideInLandscape,
                            onCheckedChange = { 
                                hideInLandscape = it 
                                prefs.edit().putBoolean("hide_in_landscape", it).apply()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- DAILY SUMMARIES ---
                SettingsSectionHeader(title = "DAILY SUMMARIES")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SettingsTimePickerRow(
                            icon = Icons.Filled.WbSunny,
                            title = "Morning Briefing",
                            subtitle = "When to send daily task summary",
                            timeText = morningBriefTime,
                            onClick = {
                                showTimePicker(context, morningBriefTime) { newTime ->
                                    morningBriefTime = newTime
                                    prefs.edit().putString("morning_brief_time", newTime).apply()
                                    com.focusbyrj.app.service.DailySummaryReceiver.scheduleDailySummaries(context)
                                }
                            }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )
                        SettingsTimePickerRow(
                            icon = Icons.Filled.NightsStay,
                            title = "Evening Wrap-up",
                            subtitle = "When to send daily reflection",
                            timeText = eveningBriefTime,
                            onClick = {
                                showTimePicker(context, eveningBriefTime) { newTime ->
                                    eveningBriefTime = newTime
                                    prefs.edit().putString("evening_brief_time", newTime).apply()
                                    com.focusbyrj.app.service.DailySummaryReceiver.scheduleDailySummaries(context)
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

private fun showTimePicker(context: Context, currentTime: String, onTimeSelected: (String) -> Unit) {
    val calendar = Calendar.getInstance()
    var hour = calendar.get(Calendar.HOUR_OF_DAY)
    var minute = calendar.get(Calendar.MINUTE)
    
    try {
        val parts = currentTime.split(":", " ")
        if(parts.size == 3) {
            var h = parts[0].toInt()
            val m = parts[1].toInt()
            val amPm = parts[2]
            if (amPm == "PM" && h != 12) h += 12
            if (amPm == "AM" && h == 12) h = 0
            hour = h
            minute = m
        }
    } catch (e: Exception) {}

    TimePickerDialog(
        context,
        { _, selectedHour, selectedMinute ->
            val amPm = if (selectedHour < 12) "AM" else "PM"
            val formatHour = if (selectedHour == 0) 12 else if (selectedHour > 12) selectedHour - 12 else selectedHour
            val timeStr = String.format(Locale.US, "%02d:%02d %s", formatHour, selectedMinute, amPm)
            onTimeSelected(timeStr)
        },
        hour,
        minute,
        false
    ).show()
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        ),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
private fun SettingsTimePickerRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    timeText: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                .clip(RoundedCornerShape(10.dp))
                .clickable { onClick() }
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = timeText,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun SettingsStepperRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    valueText: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                .padding(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onDecrement,
                modifier = Modifier
                    .size(28.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            ) {
                Icon(
                    imageVector = Icons.Filled.Remove,
                    contentDescription = "Decrease",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(14.dp)
                )
            }

            Text(
                text = valueText,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 10.dp)
            )

            IconButton(
                onClick = onIncrement,
                modifier = Modifier
                    .size(28.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Increase",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

