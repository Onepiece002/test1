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

import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove

import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.navigation.NavController
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusbyrj.app.ui.theme.*
import com.focusbyrj.app.util.AppThemeColor
import com.focusbyrj.app.util.AppThemeManager
import com.focusbyrj.app.util.FocusStatsManager
import com.focusbyrj.app.util.HeatmapTheme

@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("focus_prefs", Context.MODE_PRIVATE) }
    
    var softLockDuration by remember { mutableStateOf(prefs.getInt("soft_lock_duration", 10)) }
    var softUnlockDuration by remember { mutableStateOf(prefs.getInt("soft_unlock_duration", 5)) }
    var routineNotifications by remember { mutableStateOf(prefs.getBoolean("routine_notifications", true)) }
    
    val currentAppTheme by AppThemeManager.themeFlow.collectAsState()
    var isAppThemeDropdownExpanded by remember { mutableStateOf(false) }
    val appThemeArrowRotation by animateFloatAsState(
        targetValue = if (isAppThemeDropdownExpanded) 180f else 0f,
        label = "app_theme_dropdown_rotation"
    )
    val appThemes = remember { AppThemeColor.entries }

    val currentHeatmapTheme by FocusStatsManager.themeFlow.collectAsState()
    var isHeatmapDropdownExpanded by remember { mutableStateOf(false) }
    val dropdownArrowRotation by animateFloatAsState(
        targetValue = if (isHeatmapDropdownExpanded) 180f else 0f,
        label = "heatmap_dropdown_rotation"
    )

    val heatmapThemes = HeatmapTheme.entries

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                "Settings",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Personalize your focus experience & visuals",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            // --- APP THEME COLOUR OPTIONS ---
            Text(
                text = "App Colour Scheme",
                style = MaterialTheme.typography.titleMedium.copy(color = currentAppTheme.primary, letterSpacing = 1.sp),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceVariantDark)
                    .border(1.dp, BorderGlass, RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(SurfaceDark)
                                .border(
                                    1.dp,
                                    if (isAppThemeDropdownExpanded) currentAppTheme.primary else BorderGlass,
                                    RoundedCornerShape(18.dp)
                                )
                                .clickable { isAppThemeDropdownExpanded = !isAppThemeDropdownExpanded }
                                .padding(horizontal = 16.dp, vertical = 14.dp)
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
                                            .background(currentAppTheme.primary.copy(alpha = 0.2f))
                                            .border(1.dp, currentAppTheme.primary.copy(alpha = 0.5f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Filled.Palette,
                                            contentDescription = null,
                                            tint = currentAppTheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = currentAppTheme.displayName,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = Color.White
                                        )
                                        Text(
                                            text = currentAppTheme.description,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                        currentAppTheme.swatch.forEach { color ->
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .background(color)
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Filled.KeyboardArrowDown,
                                        contentDescription = "Select App Color",
                                        tint = if (isAppThemeDropdownExpanded) currentAppTheme.primary else Color.Gray,
                                        modifier = Modifier
                                            .size(22.dp)
                                            .rotate(appThemeArrowRotation)
                                    )
                                }
                            }
                        }

                        DropdownMenu(
                            expanded = isAppThemeDropdownExpanded,
                            onDismissRequest = { isAppThemeDropdownExpanded = false },
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .clip(RoundedCornerShape(18.dp))
                                .background(SurfaceDark)
                                .border(1.dp, BorderGlass, RoundedCornerShape(18.dp))
                        ) {
                            appThemes.forEach { theme ->
                                val isSelected = currentAppTheme == theme
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                if (isSelected) {
                                                    Icon(
                                                        Icons.Filled.Check,
                                                        contentDescription = "Selected",
                                                        tint = theme.primary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                } else {
                                                    Spacer(modifier = Modifier.width(26.dp))
                                                }
                                                Column {
                                                    Text(
                                                        text = theme.displayName,
                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                        ),
                                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = theme.description,
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                        color = if (isSelected) theme.primary.copy(alpha = 0.85f) else Color.Gray,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                theme.swatch.forEach { color ->
                                                    Box(
                                                        modifier = Modifier
                                                            .size(14.dp)
                                                            .clip(RoundedCornerShape(3.dp))
                                                            .background(color)
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    onClick = {
                                        AppThemeManager.setTheme(context, theme)
                                        isAppThemeDropdownExpanded = false
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (isSelected) theme.primary.copy(alpha = 0.12f) else Color.Transparent
                                        )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0F172A).copy(alpha = 0.5f))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "ACCENT PREVIEW",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp, fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(currentAppTheme.primary)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "ACTIVE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MidnightBlack
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .border(1.dp, currentAppTheme.secondary, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "SHIELD",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = currentAppTheme.secondary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- HEATMAP COLOUR OPTIONS ---
            Text(
                text = "Heatmap Colour Options",
                style = MaterialTheme.typography.titleMedium.copy(color = AccentCyan, letterSpacing = 1.sp),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceVariantDark)
                    .border(1.dp, BorderGlass, RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(SurfaceDark)
                                .border(
                                    1.dp,
                                    if (isHeatmapDropdownExpanded) currentHeatmapTheme.colors.last() else BorderGlass,
                                    RoundedCornerShape(18.dp)
                                )
                                .clickable { isHeatmapDropdownExpanded = !isHeatmapDropdownExpanded }
                                .padding(horizontal = 16.dp, vertical = 14.dp)
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
                                            .background(currentHeatmapTheme.colors.last().copy(alpha = 0.2f))
                                            .border(1.dp, currentHeatmapTheme.colors.last().copy(alpha = 0.5f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Filled.Palette,
                                            contentDescription = null,
                                            tint = currentHeatmapTheme.colors.last(),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = currentHeatmapTheme.displayName,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Active gradient palette",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                        currentHeatmapTheme.colors.forEach { color ->
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .background(color)
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Filled.KeyboardArrowDown,
                                        contentDescription = "Select Palette",
                                        tint = if (isHeatmapDropdownExpanded) currentHeatmapTheme.colors.last() else Color.Gray,
                                        modifier = Modifier
                                            .size(22.dp)
                                            .rotate(dropdownArrowRotation)
                                    )
                                }
                            }
                        }

                        DropdownMenu(
                            expanded = isHeatmapDropdownExpanded,
                            onDismissRequest = { isHeatmapDropdownExpanded = false },
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .clip(RoundedCornerShape(18.dp))
                                .background(SurfaceDark)
                                .border(1.dp, BorderGlass, RoundedCornerShape(18.dp))
                        ) {
                            heatmapThemes.forEach { theme ->
                                val isSelected = currentHeatmapTheme == theme
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                if (isSelected) {
                                                    Icon(
                                                        Icons.Filled.Check,
                                                        contentDescription = "Selected",
                                                        tint = theme.colors.last(),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                } else {
                                                    Spacer(modifier = Modifier.width(26.dp))
                                                }
                                                Text(
                                                    text = theme.displayName,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                    ),
                                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                theme.colors.forEach { color ->
                                                    Box(
                                                        modifier = Modifier
                                                            .size(14.dp)
                                                            .clip(RoundedCornerShape(3.dp))
                                                            .background(color)
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    onClick = {
                                        FocusStatsManager.setHeatmapTheme(context, theme)
                                        isHeatmapDropdownExpanded = false
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (isSelected) theme.colors.last().copy(alpha = 0.12f) else Color.Transparent
                                        )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0F172A).copy(alpha = 0.5f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "INTENSITY PREVIEW",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp, fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            currentHeatmapTheme.colors.forEachIndexed { index, color ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(color)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = when (index) {
                                            0 -> "0m"
                                            1 -> "15m"
                                            2 -> "30m"
                                            3 -> "1h"
                                            else -> "2h+"
                                        },
                                        fontSize = 9.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }


            Spacer(modifier = Modifier.height(28.dp))
            
            Text(
                text = "Restrictions",
                style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.secondary, letterSpacing = 1.sp),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceVariantDark)
                    .border(1.dp, BorderGlass, RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Soft Lock Timer",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Wait duration before unlocking soft-shielded apps.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Row(
                            modifier = Modifier
                                .background(Color(0xFF1E1E2E), RoundedCornerShape(12.dp))
                                .padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { 
                                    if (softLockDuration > 5) {
                                        softLockDuration -= 5
                                        prefs.edit().putInt("soft_lock_duration", softLockDuration).apply()
                                    }
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(0xFF2A2A3A), RoundedCornerShape(8.dp))
                            ) {
                                Icon(androidx.compose.material.icons.Icons.Filled.Remove, contentDescription = "Decrease", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                            
                            Text(
                                text = "${softLockDuration}s",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFCBD5E1),
                                modifier = Modifier.padding(horizontal = 12.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )

                            IconButton(
                                onClick = { 
                                    if (softLockDuration < 60) {
                                        softLockDuration += 5
                                        prefs.edit().putInt("soft_lock_duration", softLockDuration).apply()
                                    }
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(0xFF2A2A3A), RoundedCornerShape(8.dp))
                            ) {
                                Icon(androidx.compose.material.icons.Icons.Filled.Add, contentDescription = "Increase", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Soft Unlock Duration",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "How many minutes to unlock an app in Soft Mode.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Row(
                            modifier = Modifier
                                .background(Color(0xFF1E1E2E), RoundedCornerShape(12.dp))
                                .padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { 
                                    if (softUnlockDuration > 1) {
                                        softUnlockDuration -= 1
                                        prefs.edit().putInt("soft_unlock_duration", softUnlockDuration).apply()
                                    }
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(0xFF2A2A3A), RoundedCornerShape(8.dp))
                            ) {
                                Icon(androidx.compose.material.icons.Icons.Filled.Remove, contentDescription = "Decrease", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                            
                            Text(
                                text = "${softUnlockDuration}m",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFCBD5E1),
                                modifier = Modifier.padding(horizontal = 12.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )

                            IconButton(
                                onClick = { 
                                    if (softUnlockDuration < 60) {
                                        softUnlockDuration += 1
                                        prefs.edit().putInt("soft_unlock_duration", softUnlockDuration).apply()
                                    }
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(0xFF2A2A3A), RoundedCornerShape(8.dp))
                            ) {
                                Icon(androidx.compose.material.icons.Icons.Filled.Add, contentDescription = "Increase", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Routine Notifications",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Notify when a scheduled routine starts or ends.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = routineNotifications,
                            onCheckedChange = { 
                                routineNotifications = it
                                prefs.edit().putBoolean("routine_notifications", it).apply()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.secondary,
                                checkedTrackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}
