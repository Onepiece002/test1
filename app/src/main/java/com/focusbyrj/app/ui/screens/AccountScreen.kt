package com.focusbyrj.app.ui.screens
import androidx.compose.material.icons.filled.Favorite





import androidx.compose.foundation.lazy.items

import androidx.compose.foundation.lazy.grid.items


import androidx.compose.animation.core.animateFloatAsState

import androidx.compose.foundation.ExperimentalFoundationApi

import androidx.compose.foundation.background

import androidx.compose.foundation.border

import androidx.compose.foundation.clickable

import androidx.compose.foundation.layout.*

import androidx.compose.foundation.lazy.grid.GridCells

import androidx.compose.foundation.lazy.grid.LazyVerticalGrid

import androidx.compose.foundation.lazy.grid.items

import androidx.compose.foundation.pager.HorizontalPager

import androidx.compose.foundation.pager.rememberPagerState

import androidx.compose.foundation.rememberScrollState

import androidx.compose.foundation.shape.CircleShape

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.foundation.text.BasicTextField

import androidx.compose.foundation.verticalScroll

import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.filled.Check

import androidx.compose.material.icons.filled.Edit

import androidx.compose.material.icons.filled.Lock

import androidx.compose.material.icons.filled.Person

import androidx.compose.material.icons.filled.Shield

import androidx.compose.material.icons.filled.MonetizationOn

import androidx.compose.material.icons.filled.Star


import androidx.compose.material.icons.filled.DirectionsWalk

import androidx.compose.material.icons.filled.Adjust

import androidx.compose.material.icons.filled.AutoAwesome

import androidx.compose.material.icons.filled.MilitaryTech

import androidx.compose.material.icons.filled.HourglassEmpty

import androidx.compose.material.icons.filled.Whatshot

import androidx.compose.material.icons.filled.Shield

import androidx.compose.material.icons.filled.Toll

import androidx.compose.material.icons.filled.EmojiEvents

import androidx.compose.material.icons.filled.MenuBook

import androidx.compose.material.icons.filled.Security

import androidx.compose.material.icons.filled.AccountBalance


import androidx.compose.material.icons.filled.CheckCircle

import androidx.compose.material.icons.filled.Face

import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning

import androidx.compose.material3.*

import androidx.compose.material3.HorizontalDivider

import androidx.compose.runtime.*

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.graphics.SolidColor

import androidx.compose.ui.graphics.vector.ImageVector

import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.platform.LocalSoftwareKeyboardController

import androidx.compose.ui.text.TextStyle

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.text.style.TextAlign

import androidx.compose.ui.unit.dp

import androidx.compose.ui.unit.sp

import com.focusbyrj.app.ui.theme.MidnightBlack

import com.focusbyrj.app.ui.theme.AccentCyan

import com.focusbyrj.app.ui.theme.BorderGlass

import com.focusbyrj.app.ui.theme.SurfaceDark

import com.focusbyrj.app.util.FocusEconomyManager

import com.focusbyrj.app.util.FocusStatsManager


import androidx.compose.foundation.lazy.items

import androidx.compose.foundation.lazy.grid.items

import com.focusbyrj.app.util.UserProfile

import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AccountScreen() {
    val context = LocalContext.current
    val profile by FocusEconomyManager.profileFlow.collectAsState()
    val stats by FocusStatsManager.statsFlow.collectAsState()
    val heatmapTheme by FocusStatsManager.themeFlow.collectAsState()
    val isPro by com.focusbyrj.app.util.LicenseManager.isProFlow.collectAsState()
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val achievementsSheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()
    
    var showAvatarSheet by remember { mutableStateOf(false) }
    var showAchievementsSheet by remember { mutableStateOf(false) }
    var showRulesDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        FocusStatsManager.refreshStats(context)
    }

    if (profile.pendingXp > 0 || profile.pendingGold > 0) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { /* Force claim */ },
            containerColor = Color(0xFF1B1D28),
            titleContentColor = Color.White,
            textContentColor = Color.LightGray,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Focus Rewards!")
                }
            },
            text = {
                Column {
                    Text("Excellent work during your focus session. Here are your rewards:")
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF2A2C3A)).padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("+${profile.pendingXp}", color = AccentCyan, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Text("XP Gained", fontSize = 12.sp, color = Color.Gray)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("+${profile.pendingGold}", color = Color(0xFFFFD700), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Text("Gold Found", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = { FocusEconomyManager.claimPendingRewards() },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = MidnightBlack)
                ) {
                    Text("Collect Rewards", color = Color(0xFFCBD5E1), fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ProfileCard(
                profile = profile, 
                onAvatarClick = { showAvatarSheet = true },
                onNameChange = { FocusEconomyManager.updateName(it) },
                onInfoClick = { showRulesDialog = true }
            )
        }

        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            androidx.compose.material3.TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {}
            ) {
                androidx.compose.material3.Tab(
                    selected = pagerState.currentPage == 0,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(0) } },
                    text = { Text("Overview", style = MaterialTheme.typography.titleSmall) }
                )
                androidx.compose.material3.Tab(
                    selected = pagerState.currentPage == 1,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(1) } },
                    text = { Text("Achievements", style = MaterialTheme.typography.titleSmall) }
                )
            }
            
            androidx.compose.foundation.pager.HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> OverviewTab(stats, heatmapTheme, profile)
                    1 -> {
                        val isPro by com.focusbyrj.app.util.LicenseManager.isProFlow.collectAsState()
                        AchievementsPreviewTab(profile, stats, isPro, onViewAllClick = { showAchievementsSheet = true })
                    }
                }
            }
        }
    }

    if (showAchievementsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAchievementsSheet = false },
            sheetState = achievementsSheetState,
            containerColor = SurfaceDark
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "All Achievements",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFFCBD5E1),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
                AchievementsTab(profile, stats, isPro)
            }
        }
    }

    if (showAvatarSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAvatarSheet = false },
            sheetState = sheetState,
            containerColor = SurfaceDark
        ) {
            AvatarSelectionSheet(profile = profile)
        }
    }

    if (showRulesDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showRulesDialog = false },
            containerColor = Color(0xFF1B1D28),
            titleContentColor = Color.White,
            textContentColor = Color.LightGray,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Info, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Economy & Rules", style = MaterialTheme.typography.titleLarge)
                }
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    RuleSection(
                        title = "Experience (EXP)", 
                        desc = "Earned by completing focus sessions. Every minute of deep focus brings you closer to the next level.",
                        icon = Icons.Filled.Star,
                        color = AccentCyan
                    )
                    Spacer(Modifier.height(16.dp))
                    RuleSection(
                        title = "Gold Coins", 
                        desc = "Earned alongside EXP. Use Gold to unlock exclusive avatars and tiers in the store. Your current Level acts as a multiplier—higher levels yield more Gold!",
                        icon = Icons.Filled.MonetizationOn,
                        color = Color(0xFFFFD700)
                    )
                    Spacer(Modifier.height(16.dp))
                    RuleSection(
                        title = "The Penalty", 
                        desc = "Discipline is key. Exiting a session prematurely or breaking a lock will incur a steep EXP penalty, setting back your progress.",
                        icon = Icons.Filled.Warning,
                        color = Color(0xFFF43F5E)
                    )
                }
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = { showRulesDialog = false },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = MidnightBlack)
                ) {
                    Text("Understood", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun RuleSection(title: String, desc: String, icon: ImageVector, color: Color) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp).padding(top = 2.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
            Spacer(Modifier.height(4.dp))
            Text(desc, color = Color.LightGray, fontSize = 14.sp, lineHeight = 20.sp)
        }
    }
}

@Composable
fun ProfileCard(
    profile: UserProfile,
    onAvatarClick: () -> Unit,
    onNameChange: (String) -> Unit,
    onInfoClick: () -> Unit
) {
    var isEditingName by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf(profile.name) }
    val keyboardController = LocalSoftwareKeyboardController.current
    
    val currentLevelXp = FocusEconomyManager.requiredXpForLevel(profile.level)
    val nextLevelXp = FocusEconomyManager.requiredXpForLevel(profile.level + 1)
    val targetXpProgress = if (profile.level >= 100) 1f else (profile.xp - currentLevelXp).toFloat() / (nextLevelXp - currentLevelXp).toFloat()
    
    val xpProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = targetXpProgress,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 1500, easing = androidx.compose.animation.core.FastOutSlowInEasing)
    )
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(androidx.compose.ui.graphics.Brush.linearGradient(
                colors = listOf(Color(0xFF1E2030), Color(0xFF14151F))
            ))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
            .padding(24.dp)
    ) {
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF12121A))
                    .border(3.dp, getAvatarColor(profile.selectedAvatar, profile.avatarTier), CircleShape)
                    .clickable { onAvatarClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getAvatarIcon(profile.selectedAvatar, profile.avatarTier),
                    contentDescription = "Avatar",
                    tint = getAvatarColor(profile.selectedAvatar, profile.avatarTier),
                    modifier = Modifier.size(40.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(20.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                if (isEditingName) {
                    BasicTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        textStyle = MaterialTheme.typography.titleLarge.copy(color = Color(0xFFCBD5E1)),
                        cursorBrush = SolidColor(Color.White),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF2A2C3A), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            imeAction = androidx.compose.ui.text.input.ImeAction.Done
                        ),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onDone = {
                                isEditingName = false
                                if (tempName.isNotBlank()) onNameChange(tempName)
                                keyboardController?.hide()
                            }
                        )
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { isEditingName = true }) {
                        Text(
                            text = profile.name,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 22.sp),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = Color.Gray, modifier = Modifier.size(14.dp))
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = getAvatarTitle(profile.selectedAvatar, profile.avatarTier).uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = getAvatarColor(profile.selectedAvatar, profile.avatarTier)
                )
            }
            
            IconButton(onClick = onInfoClick) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = "Economy Rules",
                    tint = Color.Gray.copy(alpha = 0.5f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Level ${profile.level}", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
            Text("${profile.xp} / $nextLevelXp XP", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { xpProgress },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = AccentCyan,
            trackColor = Color(0xFF12121A)
        )
        
        Spacer(modifier = Modifier.height(28.dp))
        
        
        val unlockedCount = getAchievements(profile, com.focusbyrj.app.util.FocusStats(0,0, emptyMap())).count { it.isUnlocked }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF12121A).copy(alpha = 0.6f))
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                .padding(vertical = 16.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProfileStatItem(
                icon = androidx.compose.material.icons.Icons.Filled.MonetizationOn, 
                value = "${profile.gold}", 
                label = "Coins", 
                color = Color(0xFFFFD700),
                modifier = Modifier.weight(1f)
            )
            Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.White.copy(alpha = 0.1f)))
            ProfileStatItem(
                icon = androidx.compose.material.icons.Icons.Filled.LocalFireDepartment, 
                value = "${FocusEconomyManager.getGoldMultiplier(profile.level)}x", 
                label = "Multiplier", 
                color = Color(0xFF00E5FF),
                modifier = Modifier.weight(1f)
            )
            Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.White.copy(alpha = 0.1f)))
            ProfileStatItem(
                icon = androidx.compose.material.icons.Icons.Filled.EmojiEvents, 
                value = "$unlockedCount", 
                label = "Awards", 
                color = Color(0xFFF43F5E),
                modifier = Modifier.weight(1f)
            )
        }
        
        if (profile.level < profile.maxLevel) {
            Spacer(modifier = Modifier.height(20.dp))
            androidx.compose.material3.Button(
                onClick = { FocusEconomyManager.recoverXp(500, 500) },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = AccentCyan.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(androidx.compose.material.icons.Icons.Filled.CheckCircle, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Recover 500 XP", color = AccentCyan, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("500", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(androidx.compose.material.icons.Icons.Filled.MonetizationOn, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

@Composable
fun ProfileStatItem(icon: ImageVector, value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(value, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = Color.White, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = Color.Gray)
    }
}

@Composable
fun OverviewTab(stats: com.focusbyrj.app.util.FocusStats, heatmapTheme: com.focusbyrj.app.util.HeatmapTheme, profile: UserProfile) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        com.focusbyrj.app.ui.components.HeatmapAndStreaksWidget(dailyUsage = stats.dailyFocusMinutes, theme = heatmapTheme, profile = profile)
        
        Spacer(modifier = Modifier.height(32.dp))
        StatsGrid(profile = profile)
        
        Spacer(modifier = Modifier.height(64.dp))
    }
}


@Composable
fun StatsGrid(profile: UserProfile) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Lifetime Stats", style = MaterialTheme.typography.titleMedium, color = Color(0xFFCBD5E1))
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatCard(
                title = "Total Focus",
                value = formatMinutesToHours(profile.lifetimeFocusMins),
                icon = androidx.compose.material.icons.Icons.Filled.CheckCircle,
                color = AccentCyan,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Apps Resisted",
                value = profile.lifetimeResists.toString(),
                icon = androidx.compose.material.icons.Icons.Filled.Shield,
                color = Color(0xFF4CAF50),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun StatCard(title: String, value: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderGlass, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = Color.White)
        }
    }
}

fun formatMinutesToHours(minutes: Int): String {
    if (minutes < 60) return "${minutes}m"
    val hours = minutes / 60
    val mins = minutes % 60
    return if (mins == 0) "${hours}h" else "${hours}h ${mins}m"
}


@Composable
fun AchievementsPreviewTab(profile: UserProfile, stats: com.focusbyrj.app.util.FocusStats, isPro: Boolean, onViewAllClick: () -> Unit) {
    val achievements = getAchievements(profile, stats, isPro)
    val unlockedCount = achievements.count { it.isUnlocked }
    val totalCount = achievements.size
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceDark)
                .border(1.dp, BorderGlass, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Achievements", style = MaterialTheme.typography.titleMedium, color = Color(0xFFCBD5E1))
                        Text("$unlockedCount / $totalCount Unlocked", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                    androidx.compose.material3.TextButton(onClick = onViewAllClick) {
                        Text("View All", color = AccentCyan, fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val previewList = achievements.sortedByDescending { it.isUnlocked }.take(4)
                    previewList.forEach { ach ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            MedievalMedal(iconRes = ach.iconRes, color = ach.color, isUnlocked = ach.isUnlocked, modifier = Modifier.size(56.dp))
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(64.dp))
    }
}

enum class BadgeType { LEVEL, STREAK, GOLD, TIER, FOCUS, RESIST }

data class Achievement(val title: String, val description: String, val isUnlocked: Boolean, val type: BadgeType, val color: Color, val iconRes: Int)

fun getAchievements(profile: UserProfile, stats: com.focusbyrj.app.util.FocusStats, isPro: Boolean = false): List<Achievement> {
    return listOf(
        Achievement("First Steps", "Reach Level 2", isPro || profile.level >= 2, BadgeType.LEVEL, Color(0xFF00E5FF), com.focusbyrj.app.R.drawable.ic_achievement_first_steps),
        Achievement("Apprentice", "Reach Level 5", isPro || profile.level >= 5, BadgeType.LEVEL, Color(0xFF00E5FF), com.focusbyrj.app.R.drawable.ic_achievement_apprentice),
        Achievement("Adept", "Reach Level 10", isPro || profile.level >= 10, BadgeType.LEVEL, Color(0xFF00B0FF), com.focusbyrj.app.R.drawable.ic_achievement_adept),
        Achievement("Master", "Reach Level 25", isPro || profile.level >= 25, BadgeType.LEVEL, Color(0xFF651FFF), com.focusbyrj.app.R.drawable.ic_achievement_master),
        Achievement("Grandmaster", "Reach Level 50", isPro || profile.level >= 50, BadgeType.LEVEL, Color(0xFFD500F9), com.focusbyrj.app.R.drawable.ic_achievement_grandmaster),
        Achievement("Hero", "Reach Level 75", isPro || profile.level >= 75, BadgeType.LEVEL, Color(0xFFD500F9), com.focusbyrj.app.R.drawable.ic_achievement_hero),
        Achievement("Legend", "Reach Level 100", isPro || profile.level >= 100, BadgeType.LEVEL, Color(0xFFFF1744), com.focusbyrj.app.R.drawable.ic_achievement_legend),
        Achievement("Mythic", "Reach Level 200", isPro || profile.level >= 200, BadgeType.LEVEL, Color(0xFFFF1744), com.focusbyrj.app.R.drawable.ic_achievement_mythic),
        
        Achievement("Consistency", "3-Day Streak", isPro || profile.longestStreak >= 3, BadgeType.STREAK, Color(0xFFFF9800), com.focusbyrj.app.R.drawable.ic_achievement_consistency),
        Achievement("Dedication", "7-Day Streak", isPro || profile.longestStreak >= 7, BadgeType.STREAK, Color(0xFFFF5722), com.focusbyrj.app.R.drawable.ic_achievement_dedication),
        Achievement("Fortnight Focus", "14-Day Streak", isPro || profile.longestStreak >= 14, BadgeType.STREAK, Color(0xFFFF5722), com.focusbyrj.app.R.drawable.ic_achievement_fortnight_focus),
        Achievement("Unbreakable", "30-Day Streak", isPro || profile.longestStreak >= 30, BadgeType.STREAK, Color(0xFFF44336), com.focusbyrj.app.R.drawable.ic_achievement_unbreakable),
        Achievement("Habit Builder", "60-Day Streak", isPro || profile.longestStreak >= 60, BadgeType.STREAK, Color(0xFFF44336), com.focusbyrj.app.R.drawable.ic_achievement_habit_builder),
        Achievement("Century Club", "100-Day Streak", isPro || profile.longestStreak >= 100, BadgeType.STREAK, Color(0xFFF44336), com.focusbyrj.app.R.drawable.ic_achievement_century_club),
        Achievement("Year of Focus", "365-Day Streak", isPro || profile.longestStreak >= 365, BadgeType.STREAK, Color(0xFFF44336), com.focusbyrj.app.R.drawable.ic_achievement_year_of_focus),
        
        Achievement("Piggy Bank", "100 Gold", isPro || profile.gold >= 100, BadgeType.GOLD, Color(0xFFFFC107), com.focusbyrj.app.R.drawable.ic_achievement_piggy_bank),
        Achievement("Savings", "1,000 Gold", isPro || profile.gold >= 1000, BadgeType.GOLD, Color(0xFFFFC107), com.focusbyrj.app.R.drawable.ic_achievement_savings),
        Achievement("Wealthy", "5,000 Gold", isPro || profile.gold >= 5000, BadgeType.GOLD, Color(0xFFFFB300), com.focusbyrj.app.R.drawable.ic_achievement_wealthy),
        Achievement("Hoarder", "10,000 Gold", isPro || profile.gold >= 10000, BadgeType.GOLD, Color(0xFFFFA000), com.focusbyrj.app.R.drawable.ic_achievement_hoarder),
        Achievement("Dragon's Hoard", "25,000 Gold", isPro || profile.gold >= 25000, BadgeType.GOLD, Color(0xFFFFB300), com.focusbyrj.app.R.drawable.ic_achievement_dragons_hoard),
        Achievement("Midas Touch", "50,000 Gold", isPro || profile.gold >= 50000, BadgeType.GOLD, Color(0xFFFFA000), com.focusbyrj.app.R.drawable.ic_achievement_midas_touch),
        Achievement("Treasury", "100,000 Gold", isPro || profile.gold >= 100000, BadgeType.GOLD, Color(0xFFFFA000), com.focusbyrj.app.R.drawable.ic_achievement_treasury),
        
        Achievement("Getting Started", "1 Hour Focused", isPro || profile.lifetimeFocusMins >= 60, BadgeType.FOCUS, Color(0xFF00B0FF), com.focusbyrj.app.R.drawable.ic_achievement_getting_started),
        Achievement("Flow State", "10 Hours Focused", isPro || profile.lifetimeFocusMins >= 600, BadgeType.FOCUS, Color(0xFF00B0FF), com.focusbyrj.app.R.drawable.ic_achievement_flow_state),
        Achievement("Zone In", "50 Hours Focused", isPro || profile.lifetimeFocusMins >= 3000, BadgeType.FOCUS, Color(0xFF651FFF), com.focusbyrj.app.R.drawable.ic_achievement_zone_in),
        Achievement("Deep Work Sentinel", "100 Hours Focused", isPro || profile.lifetimeFocusMins >= 6000, BadgeType.FOCUS, Color(0xFF651FFF), com.focusbyrj.app.R.drawable.ic_achievement_deep_work_sentinel),
        Achievement("Monk Mode", "500 Hours Focused", isPro || profile.lifetimeFocusMins >= 30000, BadgeType.FOCUS, Color(0xFFD500F9), com.focusbyrj.app.R.drawable.ic_achievement_monk_mode),
        Achievement("Time Lord", "1000 Hours Focused", isPro || profile.lifetimeFocusMins >= 60000, BadgeType.FOCUS, Color(0xFFFF1744), com.focusbyrj.app.R.drawable.ic_achievement_time_lord),
        Achievement("Master of Time", "2,000 Hours Focused", isPro || profile.lifetimeFocusMins >= 120000, BadgeType.FOCUS, Color(0xFFFF1744), com.focusbyrj.app.R.drawable.ic_achievement_master_of_time),

        Achievement("First Temptation", "Resist 1 App", isPro || profile.lifetimeResists >= 1, BadgeType.RESIST, Color(0xFF4CAF50), com.focusbyrj.app.R.drawable.ic_achievement_first_temptation),
        Achievement("Iron Will", "Resist 10 Apps", isPro || profile.lifetimeResists >= 10, BadgeType.RESIST, Color(0xFF4CAF50), com.focusbyrj.app.R.drawable.ic_achievement_iron_will),
        Achievement("Willpower", "Resist 50 Apps", isPro || profile.lifetimeResists >= 50, BadgeType.RESIST, Color(0xFF009688), com.focusbyrj.app.R.drawable.ic_achievement_willpower),
        Achievement("Dopamine Detox", "Resist 100 Apps", isPro || profile.lifetimeResists >= 100, BadgeType.RESIST, Color(0xFF009688), com.focusbyrj.app.R.drawable.ic_achievement_dopamine_detox),
        Achievement("Unshatterable", "Resist 500 Apps", isPro || profile.lifetimeResists >= 500, BadgeType.RESIST, Color(0xFF00BCD4), com.focusbyrj.app.R.drawable.ic_achievement_unshatterable),
        Achievement("Zen Mind", "Resist 1,000 Apps", isPro || profile.lifetimeResists >= 1000, BadgeType.RESIST, Color(0xFF00BCD4), com.focusbyrj.app.R.drawable.ic_achievement_zen_mind),
        
        Achievement("Scholar", "Unlock Scholar", isPro || profile.avatarTier >= 1, BadgeType.TIER, Color(0xFF4CAF50), com.focusbyrj.app.R.drawable.ic_achievement_scholar),
        Achievement("Knight", "Unlock Knight", isPro || profile.avatarTier >= 2, BadgeType.TIER, Color(0xFF9C27B0), com.focusbyrj.app.R.drawable.ic_achievement_knight),
        Achievement("Noble", "Unlock Noble", isPro || profile.avatarTier >= 3, BadgeType.TIER, Color(0xFF2196F3), com.focusbyrj.app.R.drawable.ic_achievement_noble),
        Achievement("Emperor", "Unlock Emperor", isPro || profile.avatarTier >= 4, BadgeType.TIER, Color(0xFFFFD700), com.focusbyrj.app.R.drawable.ic_achievement_emperor)
    )
}


@Composable
fun MedievalMedal(iconRes: Int, color: Color, isUnlocked: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Vibrant glowing aura for unlocked achievements
        if (isUnlocked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.radialGradient(
                            colors = listOf(
                                color.copy(alpha = 0.3f),
                                color.copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
        
        val imageModifier = Modifier.fillMaxSize(1.0f)
        
        if (isUnlocked) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = iconRes),
                contentDescription = null,
                modifier = imageModifier
            )
        } else {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = iconRes),
                contentDescription = null,
                modifier = imageModifier.alpha(0.3f),
                colorFilter = androidx.compose.ui.graphics.ColorFilter.colorMatrix(androidx.compose.ui.graphics.ColorMatrix().apply { setToSaturation(0f) })
            )
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = "Locked",
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun AchievementsTab(profile: UserProfile, stats: com.focusbyrj.app.util.FocusStats, isPro: Boolean) {
    val achievements = getAchievements(profile, stats, isPro)
    val context = LocalContext.current
    
    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
        columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(4), 
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        items(achievements) { achievement ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { 
                        
                        android.widget.Toast.makeText(context, "${achievement.title}: ${achievement.description}", android.widget.Toast.LENGTH_SHORT).show()
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MedievalMedal(iconRes = achievement.iconRes, color = achievement.color, isUnlocked = achievement.isUnlocked, modifier = Modifier.size(64.dp))
                
                Spacer(modifier = Modifier.height(8.dp))
                
                
                Text(
                    text = achievement.title,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                    color = if (achievement.isUnlocked) Color.White else Color.Gray,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun AvatarSelectionSheet(profile: UserProfile) {
    val context = LocalContext.current
    var avatarToPurchase by remember { mutableStateOf<StoreAvatar?>(null) }
    
    val tiers = listOf(
        Pair(1, 0),
        Pair(2, 500),
        Pair(3, 2000),
        Pair(4, 10000),
        Pair(5, 50000)
    )
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 24.dp, bottom = 48.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("VIP Tiers", style = MaterialTheme.typography.titleLarge, color = Color(0xFFCBD5E1))
        Text("Unlocked by reaching higher Gold milestones.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.heightIn(max = 300.dp)
        ) {
            items(tiers) { (tier, cost) ->
                val isUnlocked = tier <= profile.avatarTier || profile.avatarTier == 5
                val isCurrent = profile.selectedAvatar == "tier_$tier" || (profile.selectedAvatar == "tier_1" && profile.selectedAvatar == "tier_$tier")
                
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isUnlocked) Color(0xFF2A2A3A) else Color(0xFF1A1A2A))
                        .border(
                            2.dp, 
                            if (isCurrent) getTierColor(tier) else Color.Transparent, 
                            RoundedCornerShape(16.dp)
                        )
                        .clickable(enabled = isUnlocked) {
                            FocusEconomyManager.equipAvatar("tier_$tier")
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = getTierIcon(tier),
                            contentDescription = "Tier $tier",
                            tint = if (isUnlocked) getTierColor(tier) else Color.DarkGray,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (!isUnlocked) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Lock, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(cost.toString(), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                        } else if (isCurrent) {
                            Text("Equipped", style = MaterialTheme.typography.labelSmall, color = getTierColor(tier))
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text("Store Avatars", style = MaterialTheme.typography.titleLarge, color = Color(0xFFCBD5E1))
        Text("Purchase with your earned Gold.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.heightIn(max = 300.dp)
        ) {
            items(storeAvatars) { storeAvatar ->
                val isPurchased = profile.purchasedAvatars.contains(storeAvatar.id)
                val isCurrent = profile.selectedAvatar == storeAvatar.id
                val canAfford = profile.gold >= storeAvatar.cost
                
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isPurchased) Color(0xFF2A2A3A) else Color(0xFF1A1A2A))
                        .border(
                            2.dp, 
                            if (isCurrent) storeAvatar.color else Color.Transparent, 
                            RoundedCornerShape(16.dp)
                        )
                        .clickable {
                            if (isPurchased) {
                                FocusEconomyManager.equipAvatar(storeAvatar.id)
                            } else if (canAfford) {
                                avatarToPurchase = storeAvatar
                            } else {
                                android.widget.Toast.makeText(context, "Not enough Gold", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = storeAvatar.icon,
                            contentDescription = storeAvatar.title,
                            tint = if (isPurchased || canAfford) storeAvatar.color else Color.DarkGray,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (!isPurchased) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.MonetizationOn, contentDescription = null, tint = if (canAfford) Color(0xFFFFD700) else Color.Gray, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(storeAvatar.cost.toString(), style = MaterialTheme.typography.labelSmall, color = if (canAfford) Color.White else Color.Gray)
                            }
                        } else if (isCurrent) {
                            Text("Equipped", style = MaterialTheme.typography.labelSmall, color = storeAvatar.color)
                        } else {
                            Text("Equip", style = MaterialTheme.typography.labelSmall, color = storeAvatar.color.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }

    avatarToPurchase?.let { avatar ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { avatarToPurchase = null },
            containerColor = Color(0xFF1B1D28),
            titleContentColor = Color.White,
            textContentColor = Color.LightGray,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(avatar.icon, contentDescription = null, tint = avatar.color, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Unlock ${avatar.title}?")
                }
            },
            text = {
                Text(
                    text = "Are you sure you want to purchase this avatar? This will cost ${avatar.cost} Gold.",
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        FocusEconomyManager.purchaseAvatar(avatar.id, avatar.cost)
                        android.widget.Toast.makeText(context, "Purchased ${avatar.title}!", android.widget.Toast.LENGTH_SHORT).show()
                        avatarToPurchase = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = MidnightBlack)
                ) {
                    Text("Purchase", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { avatarToPurchase = null }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}

data class StoreAvatar(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val cost: Int
)

val storeAvatars = listOf(
    StoreAvatar("store_companion", "Companion", Icons.Filled.Favorite, Color(0xFFE91E63), 1000),
    StoreAvatar("store_inferno", "Inferno", Icons.Filled.LocalFireDepartment, Color(0xFFFF5722), 2500),
    StoreAvatar("store_champion", "Champion", Icons.Filled.EmojiEvents, Color(0xFFFFD700), 5000),
    StoreAvatar("store_prestige", "Prestige", Icons.Filled.MilitaryTech, Color(0xFFE040FB), 10000),
    StoreAvatar("store_crown", "Crown", Icons.Filled.Star, Color(0xFFFFD700), 25000)
)

fun getTierIcon(tier: Int): ImageVector {
    return when (tier) {
        5 -> Icons.Filled.Star 
        4 -> Icons.Filled.MonetizationOn 
        3 -> Icons.Filled.Shield 
        2 -> Icons.Filled.Person 
        else -> Icons.Filled.Person 
    }
}

fun getTierColor(tier: Int): Color {
    return when (tier) {
        5 -> Color(0xFFFFD700) 
        4 -> Color(0xFF00E5FF) 
        3 -> Color(0xFFC0C0C0) 
        2 -> Color(0xFF4CAF50) 
        else -> Color(0xFF9E9E9E) 
    }
}

fun getTierTitle(tier: Int): String {
    return when (tier) {
        5 -> "The Emperor"
        4 -> "The Noble"
        3 -> "The Knight"
        2 -> "The Scholar"
        else -> "The Wanderer"
    }
}

fun getAvatarIcon(selectedAvatar: String, tier: Int): ImageVector {
    if (selectedAvatar.startsWith("tier_")) {
        val t = selectedAvatar.removePrefix("tier_").toIntOrNull() ?: tier
        return getTierIcon(t)
    }
    return storeAvatars.find { it.id == selectedAvatar }?.icon ?: getTierIcon(tier)
}

fun getAvatarColor(selectedAvatar: String, tier: Int): Color {
    if (selectedAvatar.startsWith("tier_")) {
        val t = selectedAvatar.removePrefix("tier_").toIntOrNull() ?: tier
        return getTierColor(t)
    }
    return storeAvatars.find { it.id == selectedAvatar }?.color ?: getTierColor(tier)
}

fun getAvatarTitle(selectedAvatar: String, tier: Int): String {
    if (selectedAvatar.startsWith("tier_")) {
        val t = selectedAvatar.removePrefix("tier_").toIntOrNull() ?: tier
        return getTierTitle(t)
    }
    return storeAvatars.find { it.id == selectedAvatar }?.title ?: getTierTitle(tier)
}
