package com.focusbyrj.app.ui.screens

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.focusbyrj.app.R
import com.focusbyrj.app.service.BubbleService
import com.focusbyrj.app.ui.components.ProfessionalSlider
import java.util.*

data class BubbleAccentColor(val name: String, val hex: String)

val BUBBLE_ACCENT_COLORS = listOf(
    BubbleAccentColor("Emerald", "#4ADE80"),
    BubbleAccentColor("Cyan", "#00E5FF"),
    BubbleAccentColor("Electric Blue", "#3B82F6"),
    BubbleAccentColor("Violet", "#A855F7"),
    BubbleAccentColor("Sunset Orange", "#FF9800"),
    BubbleAccentColor("Rose", "#F43F5E"),
    BubbleAccentColor("Sunlight", "#FACC15"),
    BubbleAccentColor("Pure White", "#FFFFFF"),
    BubbleAccentColor("Slate", "#94A3B8")
)

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

    // Hidden Bubble Customization States
    var isDockingSettingsExpanded by remember { mutableStateOf(false) }
    var hiddenOpacity by remember { mutableStateOf(prefs.getInt("bubble_hidden_opacity", 85)) }
    var hiddenAmount by remember { mutableStateOf(prefs.getInt("bubble_hidden_amount", 60)) }
    var glowIntensity by remember { mutableStateOf(prefs.getInt("bubble_glow_intensity", 65)) }
    var accentColorHex by remember { mutableStateOf(prefs.getString("bubble_accent_color", "#4ADE80") ?: "#4ADE80") }

    fun notifyService() {
        try {
            context.sendBroadcast(Intent(BubbleService.ACTION_SETTINGS_CHANGED))
        } catch (_: Exception) {}
    }

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
                                
                                val intent = Intent(context, BubbleService::class.java)
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
                                    notifyService()
                                } else if (autoHideEnabled) {
                                    autoHideEnabled = false
                                    prefs.edit().putBoolean("auto_hide_enabled", false).apply()
                                    notifyService()
                                }
                            },
                            onIncrement = {
                                if (!autoHideEnabled) {
                                    autoHideEnabled = true
                                    autoHideDuration = 1
                                    prefs.edit().putBoolean("auto_hide_enabled", true)
                                        .putInt("auto_hide_duration_sec", 1).apply()
                                    notifyService()
                                } else if (autoHideDuration < 15) {
                                    autoHideDuration += 1
                                    prefs.edit().putBoolean("auto_hide_enabled", true)
                                        .putInt("auto_hide_duration_sec", autoHideDuration).apply()
                                    notifyService()
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

                Spacer(modifier = Modifier.height(20.dp))

                // --- HIDDEN BUBBLE CUSTOMIZATION (COLLAPSIBLE) ---
                val currentColor = remember(accentColorHex) {
                    BUBBLE_ACCENT_COLORS.find { it.hex.equals(accentColorHex, ignoreCase = true) }
                        ?: BUBBLE_ACCENT_COLORS.first()
                }
                val currentParsedColor = remember(accentColorHex) {
                    try {
                        Color(android.graphics.Color.parseColor(currentColor.hex))
                    } catch (_: Exception) {
                        Color(0xFF4ADE80)
                    }
                }
                val chevronRotation by animateFloatAsState(
                    targetValue = if (isDockingSettingsExpanded) 180f else 0f,
                    label = "chevron_rot"
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { isDockingSettingsExpanded = !isDockingSettingsExpanded },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isDockingSettingsExpanded) 1.5.dp else 1.dp,
                        color = if (isDockingSettingsExpanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Tune,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Hidden Bubble Docking & Glow",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(currentParsedColor)
                                    )
                                    Text(
                                        text = "${currentColor.name} • $hiddenAmount% Tuck • $hiddenOpacity% Opacity",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = if (isDockingSettingsExpanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.KeyboardArrowDown,
                                    contentDescription = if (isDockingSettingsExpanded) "Collapse" else "Expand",
                                    tint = if (isDockingSettingsExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .rotate(chevronRotation)
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = isDockingSettingsExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Spacer(modifier = Modifier.height(12.dp))

                        // Live Preview Card
                        LiveBubblePreviewCard(
                            hiddenOpacity = hiddenOpacity,
                            hiddenAmount = hiddenAmount,
                            glowIntensity = glowIntensity,
                            accentColorHex = accentColorHex
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                AccentColorDropdownRow(
                                    selectedHex = accentColorHex,
                                    onColorSelected = { newHex ->
                                        accentColorHex = newHex
                                        prefs.edit().putString("bubble_accent_color", newHex).apply()
                                        notifyService()
                                    }
                                )

                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 14.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                )

                                // Hidden Opacity Slider Row
                                SettingsSliderRow(
                                    icon = Icons.Filled.Opacity,
                                    title = "Hidden Opacity",
                                    subtitle = "Transparency of the bubble when docked",
                                    value = hiddenOpacity,
                                    valueRange = 20..100,
                                    unit = "%",
                                    onValueChange = { newVal ->
                                        hiddenOpacity = newVal
                                        prefs.edit().putInt("bubble_hidden_opacity", newVal).apply()
                                        notifyService()
                                    }
                                )

                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 14.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                )

                                // Amount Hidden (Tuck Depth) Slider Row
                                SettingsSliderRow(
                                    icon = Icons.Filled.KeyboardDoubleArrowRight,
                                    title = "Amount Hidden (Tuck Depth)",
                                    subtitle = "Percentage of the bubble tucked into the bezel",
                                    value = hiddenAmount,
                                    valueRange = 25..85,
                                    unit = "%",
                                    onValueChange = { newVal ->
                                        hiddenAmount = newVal
                                        prefs.edit().putInt("bubble_hidden_amount", newVal).apply()
                                        notifyService()
                                    }
                                )

                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 14.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                )

                                // Glow Ring Intensity Slider Row
                                SettingsSliderRow(
                                    icon = Icons.Filled.Highlight,
                                    title = "Glow Ring Intensity",
                                    subtitle = "Brightness & stroke visibility of accent border",
                                    value = glowIntensity,
                                    valueRange = 0..100,
                                    unit = "%",
                                    onValueChange = { newVal ->
                                        glowIntensity = newVal
                                        prefs.edit().putInt("bubble_glow_intensity", newVal).apply()
                                        notifyService()
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

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

@Composable
private fun LiveBubblePreviewCard(
    hiddenOpacity: Int,
    hiddenAmount: Int,
    glowIntensity: Int,
    accentColorHex: String
) {
    val accentColor = try {
        Color(android.graphics.Color.parseColor(accentColorHex))
    } catch (_: Exception) {
        Color(0xFF4ADE80)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Live Docking Preview",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "$hiddenAmount% Hidden • $hiddenOpacity% Opacity",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Simulated screen frame
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF121417))
                    .border(1.5.dp, Color(0xFF2C3036), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.CenterEnd
            ) {
                // Subtle desktop wallpaper texture lines
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Column {
                        Text(
                            text = "Phone Display Canvas",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White.copy(alpha = 0.4f)
                        )
                        Text(
                            text = "Glow Intensity: $glowIntensity%",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.25f)
                        )
                    }
                }

                // Bubble container parked at right edge with dynamic offset
                val bubbleDiameter = 54.dp
                val hideFraction = hiddenAmount / 100f
                val offsetValue = bubbleDiameter * hideFraction

                Box(
                    modifier = Modifier
                        .offset(x = offsetValue)
                        .size(bubbleDiameter)
                        .alpha(hiddenOpacity / 100f),
                    contentAlignment = Alignment.Center
                ) {
                    // Base bubble circle with app icon
                    Box(
                        modifier = Modifier
                            .size(bubbleDiameter)
                            .clip(CircleShape)
                            .background(Color.Black)
                            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_app_logo),
                            contentDescription = "Hidden Bubble Preview",
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    // Accent glow ring
                    if (glowIntensity > 0) {
                        val strokeWidthDp = (1.5f + (glowIntensity / 100f * 1.5f)).dp
                        Box(
                            modifier = Modifier
                                .size(bubbleDiameter)
                                .clip(CircleShape)
                                .border(
                                    width = strokeWidthDp,
                                    color = accentColor.copy(alpha = glowIntensity / 100f),
                                    shape = CircleShape
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSliderRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    value: Int,
    valueRange: IntRange,
    unit: String,
    onValueChange: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
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

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Text(
                    text = "$value$unit",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        ProfessionalSlider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt().coerceIn(valueRange.first, valueRange.last)) },
            valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
            modifier = Modifier.fillMaxWidth()
        )
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

@Composable
private fun AccentColorDropdownRow(
    selectedHex: String,
    onColorSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val currentColor = remember(selectedHex) {
        BUBBLE_ACCENT_COLORS.find { it.hex.equals(selectedHex, ignoreCase = true) }
            ?: BUBBLE_ACCENT_COLORS.first()
    }
    val currentParsedColor = remember(selectedHex) {
        try {
            Color(android.graphics.Color.parseColor(currentColor.hex))
        } catch (_: Exception) {
            Color(0xFF4ADE80)
        }
    }

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
                    imageVector = Icons.Filled.Palette,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "Edge Accent Glow Color",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Color of the edge rim when docked",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Box {
            Surface(
                onClick = { expanded = true },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(currentParsedColor)
                            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), CircleShape)
                    )
                    Text(
                        text = currentColor.name,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = "Select Color",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .widthIn(min = 190.dp)
            ) {
                BUBBLE_ACCENT_COLORS.forEach { colorItem ->
                    val isSelected = selectedHex.equals(colorItem.hex, ignoreCase = true)
                    val parsedColor = try {
                        Color(android.graphics.Color.parseColor(colorItem.hex))
                    } catch (_: Exception) {
                        Color(0xFF4ADE80)
                    }

                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(parsedColor)
                                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), CircleShape)
                                )
                                Text(
                                    text = colorItem.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        trailingIcon = {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        onClick = {
                            onColorSelected(colorItem.hex)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}


