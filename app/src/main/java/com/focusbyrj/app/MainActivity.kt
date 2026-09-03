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

package com.focusbyrj.app

import androidx.compose.material3.MaterialTheme
import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.focusbyrj.app.service.FocusBlockerService
import com.focusbyrj.app.ui.components.FocusBottomBar
import com.focusbyrj.app.ui.components.SetupPermissionsDialog
import com.focusbyrj.app.ui.navigation.Screen
import com.focusbyrj.app.ui.screens.AccountScreen
import com.focusbyrj.app.ui.screens.AddRestrictionScreen
import com.focusbyrj.app.ui.screens.DashboardScreen
import com.focusbyrj.app.ui.screens.SchedulesScreen
import com.focusbyrj.app.ui.screens.SecurityScreen
import com.focusbyrj.app.ui.screens.SettingsScreen
import com.focusbyrj.app.ui.screens.SubscriptionScreen
import com.focusbyrj.app.ui.screens.TimeScreen
import com.focusbyrj.app.ui.theme.BorderGlass
import com.focusbyrj.app.ui.theme.FocusByRjTheme
import com.focusbyrj.app.ui.viewmodels.FocusViewModel
import com.focusbyrj.app.ui.viewmodels.FocusViewModelFactory
import com.focusbyrj.app.ui.viewmodels.TaskViewModel
import com.focusbyrj.app.ui.viewmodels.TaskViewModelFactory
import com.focusbyrj.app.util.FocusEconomyManager
import com.focusbyrj.app.util.FocusStatsManager
import com.focusbyrj.app.util.PermissionUtils
import com.focusbyrj.app.util.ProfileAvatarManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.delay
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import com.focusbyrj.app.util.EconomyEvent

class MainActivity : FragmentActivity() {

    lateinit var viewModel: FocusViewModel
    lateinit var taskViewModel: com.focusbyrj.app.ui.viewmodels.TaskViewModel

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val navigateTo = intent.getStringExtra("navigate_to")
        val openAddDialog = intent.getBooleanExtra("open_add_dialog", false)
        if (navigateTo != null) {
            viewModel.triggerNavigation(navigateTo)
        }
        if (openAddDialog) {
            viewModel.triggerOpenAddDialog()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as FocusApplication
        val vm: FocusViewModel by viewModels {
            FocusViewModelFactory(app.repository, app)
        }
        viewModel = vm

        val tvm: com.focusbyrj.app.ui.viewmodels.TaskViewModel by viewModels {
            com.focusbyrj.app.ui.viewmodels.TaskViewModelFactory(app.taskRepository, app)
        }
        taskViewModel = tvm

        kotlin.runCatching { FocusBlockerService.startService(this) }
        kotlin.runCatching { com.focusbyrj.app.service.BubbleService.startIfEnabled(this) }
        kotlin.runCatching { com.focusbyrj.app.service.DailySummaryReceiver.scheduleDailySummaries(this) }
        kotlin.runCatching { com.focusbyrj.app.service.AptitudeReminderReceiver.scheduleRandomDrillReminders(this) }
        kotlin.runCatching { com.focusbyrj.app.util.TaskReminderHelper.scheduleAllPendingReminders(this) }

        val navigateTo = intent?.getStringExtra("navigate_to")
        val openAddDialog = intent?.getBooleanExtra("open_add_dialog", false) ?: false

        setContent {
            FocusByRjTheme {
                MainAppScreen(
                    viewModel = viewModel, 
                    taskViewModel = taskViewModel,
                    initialNavigateTo = navigateTo,
                    initialOpenAdd = openAddDialog
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    viewModel: FocusViewModel, 
    taskViewModel: com.focusbyrj.app.ui.viewmodels.TaskViewModel,
    initialNavigateTo: String? = null,
    initialOpenAdd: Boolean = false
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val pendingRoute by viewModel.pendingNavigationRoute.collectAsStateWithLifecycle()
    val pendingOpenAdd by viewModel.pendingOpenAddDialog.collectAsStateWithLifecycle()

    LaunchedEffect(initialNavigateTo) {
        if (initialNavigateTo != null) {
            navController.navigate(initialNavigateTo) {
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(pendingRoute) {
        pendingRoute?.let { route ->
            navController.navigate(route) {
                launchSingleTop = true
            }
            viewModel.clearNavigation()
        }
    }

    val items = listOf(
        Screen.Dashboard,
        Screen.Todos,
        Screen.Schedules,
        Screen.Time,
        Screen.Account
    )

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val isSessionActive by viewModel.isSessionActive.collectAsStateWithLifecycle()
    val economyProfile by FocusEconomyManager.profileFlow.collectAsStateWithLifecycle()
    val focusStats by FocusStatsManager.statsFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val prefs = remember { context.getSharedPreferences("focus_app_prefs", Context.MODE_PRIVATE) }
    val hasSeenOnboarding = remember { prefs.getBoolean("has_seen_permission_onboarding", false) }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    
    var hasUsageStats by remember { mutableStateOf(PermissionUtils.hasUsageStatsPermission(context)) }
    var hasOverlay by remember { mutableStateOf(PermissionUtils.hasOverlayPermission(context)) }
    var isBatteryUnrestricted by remember { mutableStateOf(PermissionUtils.isIgnoringBatteryOptimizations(context)) }
    var hasNotifications by remember { mutableStateOf(PermissionUtils.hasNotificationPermission(context)) }

    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME || event == androidx.lifecycle.Lifecycle.Event.ON_START) {
                hasUsageStats = PermissionUtils.hasUsageStatsPermission(context)
                hasOverlay = PermissionUtils.hasOverlayPermission(context)
                isBatteryUnrestricted = PermissionUtils.isIgnoringBatteryOptimizations(context)
                hasNotifications = PermissionUtils.hasNotificationPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val allConfigured = hasUsageStats && hasOverlay && isBatteryUnrestricted && hasNotifications

    var showSetupDialog by remember { mutableStateOf(!hasSeenOnboarding && !allConfigured) }

    var currentEconomyEvent by remember { mutableStateOf<EconomyEvent?>(null) }
    
    androidx.compose.runtime.LaunchedEffect(Unit) {
        FocusEconomyManager.economyEvents.collect { event ->
            currentEconomyEvent = event
            delay(4000)
            currentEconomyEvent = null
            delay(500)
        }
    }


    androidx.compose.runtime.LaunchedEffect(showSetupDialog) {
        if (showSetupDialog) {
            while (true) {
                hasUsageStats = PermissionUtils.hasUsageStatsPermission(context)
                hasOverlay = PermissionUtils.hasOverlayPermission(context)
                isBatteryUnrestricted = PermissionUtils.isIgnoringBatteryOptimizations(context)
                hasNotifications = PermissionUtils.hasNotificationPermission(context)
                kotlinx.coroutines.delay(1000)
            }
        }
    }

    if (showSetupDialog) {
        SetupPermissionsDialog(
            hasUsageStats = hasUsageStats,
            hasOverlay = hasOverlay,
            isBatteryUnrestricted = isBatteryUnrestricted,
            hasNotifications = hasNotifications,
            onDismiss = {
                prefs.edit().putBoolean("has_seen_permission_onboarding", true).apply()
                showSetupDialog = false
            }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(320.dp),
                drawerContainerColor = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                ) {
                    Spacer(modifier = Modifier.height(14.dp))

                    // Executive Profile Card inside Drawer
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .clickable {
                                scope.launch { drawerState.close() }
                                kotlin.runCatching {
                                    navController.navigate(Screen.Account.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val avatarRes = ProfileAvatarManager.getAvatarImageRes(economyProfile.selectedAvatar, economyProfile.avatarTier)
                                val avatarBorder = ProfileAvatarManager.getAvatarBorderColor(economyProfile.selectedAvatar, economyProfile.avatarTier)
                                val rankTitle = ProfileAvatarManager.getAvatarTitle(economyProfile.selectedAvatar, economyProfile.avatarTier)
                                
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surface)
                                        .border(2.dp, avatarBorder, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = avatarRes),
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp).clip(CircleShape)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = economyProfile.name.ifBlank { "Focus Master" },
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "$rankTitle • Lvl ${economyProfile.level}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Streak Pill
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFEA580C).copy(alpha = 0.12f),
                                    border = BorderStroke(0.8.dp, Color(0xFFF97316).copy(alpha = 0.35f)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text("🔥", fontSize = 12.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${focusStats.currentStreak}d Streak",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFFF97316)
                                        )
                                    }
                                }

                                // Gold Pill
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF59E0B).copy(alpha = 0.12f),
                                    border = BorderStroke(0.8.dp, Color(0xFFF59E0B).copy(alpha = 0.35f)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text("💎", fontSize = 12.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${economyProfile.gold}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFFF59E0B)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Section: PREFERENCES
                    Text(
                        text = "PREFERENCES",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.3.sp,
                            fontSize = 11.sp
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
                    )

                    // Preferences Card Container
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    ) {
                        Column {
                            // Security & Permissions
                            DrawerMenuItem(
                                icon = Icons.Filled.Security,
                                title = "Security & Permissions",
                                subtitle = "Device shield & lock access",
                                trailingBadge = {
                                    if (allConfigured) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(11.dp))
                                                Text("Active", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    } else {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(11.dp))
                                                Text("Setup", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                },
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    kotlin.runCatching {
                                        navController.navigate(Screen.Security.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            )

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            // App Settings
                            DrawerMenuItem(
                                icon = Icons.Filled.Palette,
                                title = "App Settings",
                                subtitle = "Theme, sounds & preferences",
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    kotlin.runCatching {
                                        navController.navigate(Screen.Settings.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            )

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            // Bubble Settings
                            DrawerMenuItem(
                                icon = Icons.Filled.Chat,
                                title = "Bubble Settings",
                                subtitle = "Floating timer & quick dock",
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    kotlin.runCatching {
                                        navController.navigate(Screen.BubbleSettings.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            )

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            // Subscription
                            DrawerMenuItem(
                                icon = Icons.Filled.Star,
                                title = "Subscription",
                                subtitle = "Unlock Pro & cloud sync",
                                trailingBadge = {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                        border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                                    ) {
                                        Text(
                                            text = "PRO",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.ExtraBold),
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                },
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    kotlin.runCatching {
                                        navController.navigate(Screen.Subscription.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Section: ASSISTANCE
                    Text(
                        text = "ASSISTANCE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.3.sp,
                            fontSize = 11.sp
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
                    )

                    // Assistance Card Container
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    ) {
                        // Setup Guide
                        DrawerMenuItem(
                            icon = Icons.Filled.Info,
                            title = "Setup Guide",
                            subtitle = "Tour & permissions guide",
                            onClick = {
                                scope.launch { drawerState.close() }
                                showSetupDialog = true
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(26.dp))

                    // Footer Branding
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Focus by Rj • v${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Stay present. Guard your mind.",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                if (!isSessionActive) {
                    TopAppBar(
                        title = {
                            AnimatedContent(
                                targetState = currentDestination?.route,
                                transitionSpec = {
                                    fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) togetherWith 
                                    fadeOut(animationSpec = tween(160, easing = FastOutLinearInEasing))
                                },
                                label = "TopBarTitleTransition"
                            ) { route ->
                                when (route) {
                                    Screen.Dashboard.route -> {
                                        Text(
                                            text = "Focus by Rj",
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                letterSpacing = 1.2.sp,
                                                fontSize = 20.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                    }
                                    Screen.Todos.route -> {
                                        Text(
                                            text = "Todos",
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 20.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                    }
                                    Screen.Schedules.route -> {
                                        Text(
                                            text = "Routines",
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 20.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                    }
                                    Screen.Time.route -> {
                                        Text(
                                            text = "Screen Time",
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 20.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                    }
                                    Screen.Account.route -> {
                                        Text(
                                            text = "Profile & Stats",
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 20.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                    }
                                    else -> {
                                        Text(
                                            text = "Focus by Rj",
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                letterSpacing = 1.2.sp,
                                                fontSize = 20.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                    }
                                }
                            }
                        },
                        navigationIcon = {
                            Box(
                                modifier = Modifier
                                    .padding(start = 12.dp, end = 4.dp)
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                    .clickable { scope.launch { drawerState.open() } },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.Menu,
                                    contentDescription = "Menu",
                                    tint = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        actions = {
                            // Live Streak Flame Pill
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        navController.navigate(Screen.Account.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("🔥", fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "${focusStats.currentStreak}d",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            // Interactive Profile Avatar Ring
                            val avatarRes = ProfileAvatarManager.getAvatarImageRes(economyProfile.selectedAvatar, economyProfile.avatarTier)
                            val avatarBorder = ProfileAvatarManager.getAvatarBorderColor(economyProfile.selectedAvatar, economyProfile.avatarTier)

                            Box(
                                modifier = Modifier
                                    .padding(start = 8.dp, end = 12.dp)
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(1.8.dp, avatarBorder, CircleShape)
                                    .clickable {
                                        navController.navigate(Screen.Account.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = avatarRes),
                                    contentDescription = "Profile",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(2.dp)
                                        .clip(CircleShape)
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                    )
                }
            },
            bottomBar = {
                val hideBottomBarRoutes = listOf(
                    Screen.AddRestriction.route,
                    Screen.Security.route,
                    Screen.Settings.route,
                    Screen.BubbleSettings.route,
                    Screen.Subscription.route
                )
                if (currentDestination?.route !in hideBottomBarRoutes && !isSessionActive) {
                    FocusBottomBar(
                        items = items,
                        currentDestination = currentDestination,
                        navController = navController
                    )
                }
            },
            floatingActionButton = {
                if (currentDestination?.route == Screen.Dashboard.route) {
                    FloatingActionButton(
                        onClick = { navController.navigate(Screen.AddRestriction.route) },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(18.dp),
                        elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(
                            defaultElevation = 3.dp,
                            pressedElevation = 6.dp
                        )
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add Restriction")
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                val defaultStartTab = remember {
                    val saved = prefs.getString("default_start_tab", null)
                        ?: context.getSharedPreferences("focus_prefs", Context.MODE_PRIVATE).getString("default_start_tab", Screen.Dashboard.route)
                        ?: Screen.Dashboard.route
                    if (items.any { it.route == saved }) saved else Screen.Dashboard.route
                }
                val startDest = initialNavigateTo ?: defaultStartTab

                NavHost(
                    navController = navController,
                    startDestination = startDest,
                    enterTransition = { androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(0)) },
                    exitTransition = { androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(0)) },
                    popEnterTransition = { androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(0)) },
                    popExitTransition = { androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(0)) }
                ) {
                composable(Screen.Dashboard.route) {
                    val restrictions by viewModel.combinedRestrictions.collectAsStateWithLifecycle()
                    val timeRemaining by viewModel.timeRemaining.collectAsStateWithLifecycle()
                    val initialTime by viewModel.initialTime.collectAsStateWithLifecycle()

                    DashboardScreen(
                        restrictions = restrictions,
                        onToggle = { app -> viewModel.toggleRestriction(app) },
                        onDelete = { app -> viewModel.deleteRestriction(app) },
                        onUpdate = { app -> viewModel.updateRestriction(app) },
                        isSessionActive = isSessionActive,
                        timeRemaining = timeRemaining,
                        initialTime = initialTime,
                        onToggleSession = { viewModel.toggleFocusSession() },
                        onSetTime = { time -> viewModel.setTimeRemaining(time) }
                    )
                }
                composable(Screen.Schedules.route) {
                    SchedulesScreen(viewModel)
                }
                composable(Screen.Account.route) {
                    AccountScreen()
                }
                composable(Screen.Time.route) {
                    TimeScreen()
                }
                composable(Screen.AddRestriction.route) {
                    AddRestrictionScreen(navController, viewModel)
                }
                composable(Screen.Security.route) {
                    SecurityScreen(navController)
                }
                composable(Screen.Settings.route) {
                    SettingsScreen(navController)
                }
                composable(Screen.BubbleSettings.route) {
                    com.focusbyrj.app.ui.screens.BubbleSettingsScreen(navController)
                }
                composable(Screen.Subscription.route) {
                    SubscriptionScreen(navController)
                }
                composable(Screen.Todos.route) {
                    com.focusbyrj.app.ui.screens.TodosScreen(
                        taskViewModel, 
                        initialOpenAdd = initialOpenAdd || pendingOpenAdd,
                        onOpenAddHandled = { viewModel.clearOpenAddDialog() }
                    )
                }
            }
            
            // Economy Popup - Limited to Profile / Account tab only
            AnimatedVisibility(
                visible = currentEconomyEvent != null && currentDestination?.route == Screen.Account.route,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp).padding(horizontal = 16.dp)
            ) {
                currentEconomyEvent?.let { event ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val iconRes = when (event) {
                                is EconomyEvent.AchievementUnlocked -> R.drawable.ic_achievement_century_club // generic for now if none provided
                                is EconomyEvent.RewardsEarned -> R.drawable.ic_achievement_wealthy
                            }
                            
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha=0.2f))
                                    .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = iconRes),
                                    contentDescription = null,
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column {
                                when (event) {
                                    is EconomyEvent.AchievementUnlocked -> {
                                        Text("Achievement Unlocked!", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                        Text(event.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                    }
                                    is EconomyEvent.RewardsEarned -> {
                                        Text(event.source, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                        Text("+${event.xp} XP  •  +${event.gold} Gold", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun DrawerMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailingBadge: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    maxLines = 1
                )
            }
        }
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (trailingBadge != null) {
                trailingBadge()
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

