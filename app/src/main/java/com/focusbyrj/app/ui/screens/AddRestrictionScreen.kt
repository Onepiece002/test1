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

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.focusbyrj.app.data.AppRestriction
import com.focusbyrj.app.ui.components.CustomRestrictionSection
import com.focusbyrj.app.ui.theme.*
import com.focusbyrj.app.ui.viewmodels.FocusViewModel
import com.focusbyrj.app.util.CustomCategory
import com.focusbyrj.app.util.CustomCategoryManager
import com.focusbyrj.app.util.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class AppCategory(val title: String) {
    ALL("All"),
    SOCIAL("Social"),
    PAYMENT("Finance"),
    SHOPPING("Shopping"),
    GAMES("Games"),
    UTILITY("Utility"),
    OTHERS("Others")
}

fun getCategoryForApp(appInfo: ApplicationInfo, packageName: String): AppCategory {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        when (appInfo.category) {
            ApplicationInfo.CATEGORY_SOCIAL -> return AppCategory.SOCIAL
            ApplicationInfo.CATEGORY_GAME -> return AppCategory.GAMES
            ApplicationInfo.CATEGORY_PRODUCTIVITY -> return AppCategory.UTILITY
        }
    }
    
    val lowerPkg = packageName.lowercase()
    if (lowerPkg.contains("whatsapp") || lowerPkg.contains("instagram") || 
        lowerPkg.contains("facebook") || lowerPkg.contains("twitter") || 
        lowerPkg.contains("tiktok") || lowerPkg.contains("snapchat") ||
        lowerPkg.contains("reddit") || lowerPkg.contains("telegram") ||
        lowerPkg.contains("discord")) {
        return AppCategory.SOCIAL
    }
    
    if (lowerPkg.contains("pay") || lowerPkg.contains("bank") || 
        lowerPkg.contains("cash") || lowerPkg.contains("wallet") ||
        lowerPkg.contains("paypal") || lowerPkg.contains("venmo") ||
        lowerPkg.contains("stripe") || lowerPkg.contains("finance")) {
        return AppCategory.PAYMENT
    }
    
    if (lowerPkg.contains("shop") || lowerPkg.contains("store") || 
        lowerPkg.contains("amazon") || lowerPkg.contains("flipkart") ||
        lowerPkg.contains("ebay") || lowerPkg.contains("cart") ||
        lowerPkg.contains("myntra") || lowerPkg.contains("alibaba") ||
        lowerPkg.contains("aliexpress") || lowerPkg.contains("buy") || lowerPkg.contains("retail") || lowerPkg.contains("commerce")) {
        return AppCategory.SHOPPING
    }
    
    if (lowerPkg.contains("game") || lowerPkg.contains("unity") || 
        lowerPkg.contains("unreal") || lowerPkg.contains("roblox") ||
        lowerPkg.contains("mojang") || lowerPkg.contains("ea") ||
        lowerPkg.contains("supercell") || lowerPkg.contains("king") ||
        lowerPkg.contains("epic")) {
        return AppCategory.GAMES
    }
    
    if (lowerPkg.contains("tool") || lowerPkg.contains("util") || 
        lowerPkg.contains("calculator") || lowerPkg.contains("calendar") ||
        lowerPkg.contains("clock") || lowerPkg.contains("camera") ||
        lowerPkg.contains("weather") || lowerPkg.contains("notes") ||
        lowerPkg.contains("file") || lowerPkg.contains("settings") ||
        lowerPkg.contains("drive") || lowerPkg.contains("docs") ||
        lowerPkg.contains("sheet") || lowerPkg.contains("slide") ||
        lowerPkg.contains("gmail") || lowerPkg.contains("mail")) {
        return AppCategory.UTILITY
    }
    
    return AppCategory.OTHERS
}

data class InstalledApp(
    val packageName: String,
    val appName: String,
    val category: AppCategory
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRestrictionScreen(
    navController: NavController,
    viewModel: FocusViewModel
) {
    val context = LocalContext.current
    var installedApps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf<Any>(AppCategory.ALL) }
    var selectedApps by remember { mutableStateOf<Set<InstalledApp>>(emptySet()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf("HARD") }
    var restrictionMode by remember { mutableStateOf("SIMPLE") }
    var timeLimitMinutes by remember { mutableIntStateOf(15) }
    var clickLimitCount by remember { mutableIntStateOf(5) }
    var customQuote by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var currentStep by remember { mutableIntStateOf(1) } 
    var showCustomCategoryEditor by remember { mutableStateOf(false) }
    var editingCustomCategory by remember { mutableStateOf<CustomCategory?>(null) }
    var customCategories by remember { mutableStateOf<List<CustomCategory>>(CustomCategoryManager.getCategories(context)) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val apps = packages
                .filter { pm.getLaunchIntentForPackage(it.packageName) != null && it.packageName != context.packageName }
                .map { InstalledApp(it.packageName, it.loadLabel(pm).toString(), getCategoryForApp(it, it.packageName)) }
                .sortedBy { it.appName }
            installedApps = apps
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (currentStep == 1) "Shield New Apps" else "Configure Shield", 
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { 
                        if (currentStep == 2) currentStep = 1 
                        else navController.popBackStack() 
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MidnightBlack, 
                    titleContentColor = Color.White, 
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = MidnightBlack,
        bottomBar = {
            if (currentStep == 1) {
                AnimatedVisibility(
                    visible = selectedApps.isNotEmpty(),
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MidnightBlack)
                            .padding(16.dp)
                    ) {
                        Button(
                            onClick = { currentStep = 2 },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentViolet, contentColor = MidnightBlack),
                            shape = RoundedCornerShape(28.dp)
                        ) {
                            Text("Configure Shield (${selectedApps.size} Apps)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MidnightBlack)
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = {
                            val newRestrictions = selectedApps.map { app ->
                                AppRestriction(
                                    packageName = app.packageName,
                                    appName = app.appName,
                                    mode = selectedMode,
                                    restrictionMode = restrictionMode,
                                    timeLimitMinutes = if (restrictionMode == "TIME_LIMIT") timeLimitMinutes else 0,
                                    clickLimitCount = if (restrictionMode == "CLICK_LIMIT") clickLimitCount else 0,
                                    customQuote = customQuote.trim(),
                                    isRestricted = true
                                )
                            }
                            viewModel.addRestrictions(newRestrictions)
                            navController.popBackStack()
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = MidnightBlack),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Text("Enable Shield for ${selectedApps.size} Apps", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { width -> width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> -width } + fadeOut()
                    } else {
                        slideInHorizontally { width -> -width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> width } + fadeOut()
                    }
                },
                label = "stepAnimation"
            ) { step ->
                if (step == 1) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search applications...", color = Color.Gray) },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search", tint = Color.Gray) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SurfaceDark,
                                unfocusedContainerColor = SurfaceDark,
                                focusedBorderColor = AccentViolet,
                                unfocusedBorderColor = BorderGlass,
                                focusedTextColor = Color(0xFFCBD5E1),
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true
                        )

                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(AppCategory.entries.toTypedArray()) { category ->
                                val isSelected = selectedCategory == category
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (isSelected) AccentViolet else SurfaceDark)
                                        .border(1.dp, if (isSelected) AccentViolet else BorderGlass, RoundedCornerShape(20.dp))
                                        .clickable { selectedCategory = category }
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = category.title,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }

                            items(customCategories, key = { it.id }) { cat ->
                                val isSelected = selectedCategory == cat
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (isSelected) AccentCyan else SurfaceDark)
                                        .border(1.dp, if (isSelected) AccentCyan else BorderGlass, RoundedCornerShape(20.dp))
                                        .clickable { selectedCategory = cat }
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = cat.name,
                                            color = if (isSelected) MidnightBlack else MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            Icons.Filled.Edit,
                                            contentDescription = "Edit Category",
                                            tint = if (isSelected) MidnightBlack else Color.Gray,
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clickable {
                                                    editingCustomCategory = cat
                                                    showCustomCategoryEditor = true
                                                }
                                        )
                                    }
                                }
                            }

                            item {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(SurfaceDark)
                                        .border(1.dp, BorderGlass, RoundedCornerShape(20.dp))
                                        .clickable {
                                            editingCustomCategory = null
                                            showCustomCategoryEditor = true
                                        }
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Add, contentDescription = "Add Filter", tint = AccentCyan, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "New Filter",
                                            color = AccentCyan,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }
                            }
                        }

                        val filteredApps = remember(installedApps, selectedCategory, searchQuery) {
                            var list = when (val cat = selectedCategory) {
                                is AppCategory -> {
                                    if (cat == AppCategory.ALL) installedApps
                                    else installedApps.filter { it.category == cat }
                                }
                                is CustomCategory -> {
                                    installedApps.filter { cat.packages.contains(it.packageName) }
                                }
                                else -> installedApps
                            }
                            if (searchQuery.isNotBlank()) {
                                list = list.filter { it.appName.contains(searchQuery, ignoreCase = true) }
                            }
                            list
                        }

                        if (isLoading) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = AccentCyan)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${filteredApps.size} apps available",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray
                                        )

                                        val allSelected = filteredApps.isNotEmpty() && selectedApps.containsAll(filteredApps)
                                        if (allSelected) {
                                            TextButton(
                                                onClick = { selectedApps = selectedApps - filteredApps.toSet() }
                                            ) {
                                                Text("Deselect All", color = Color(0xFFF44336))
                                            }
                                        } else {
                                            TextButton(
                                                onClick = { selectedApps = selectedApps + filteredApps }
                                            ) {
                                                Text("Select All", color = AccentCyan)
                                            }
                                        }
                                    }
                                }

                                items(filteredApps, key = { it.packageName }) { app ->
                                    val isSelected = selectedApps.contains(app)
                                    val pm = context.packageManager
                                    val icon = remember(app.packageName) { ImageUtils.getAppIcon(pm, app.packageName) }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(if (isSelected) AccentViolet.copy(alpha = 0.15f) else SurfaceDark)
                                            .border(
                                                1.dp,
                                                if (isSelected) AccentViolet else BorderGlass,
                                                RoundedCornerShape(16.dp)
                                            )
                                            .clickable {
                                                selectedApps = if (isSelected) {
                                                    selectedApps - app
                                                } else {
                                                    selectedApps + app
                                                }
                                            }
                                            .padding(horizontal = 16.dp, vertical = 12.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            if (icon != null) {
                                                Image(
                                                    bitmap = icon,
                                                    contentDescription = null,
                                                    modifier = Modifier
                                                        .size(48.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                )
                                                Spacer(modifier = Modifier.width(16.dp))
                                            }

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = app.appName,
                                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                                    color = Color.White
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = app.category.title,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = Color.Gray
                                                )
                                            }

                                            if (isSelected) {
                                                Icon(
                                                    Icons.Filled.CheckCircle,
                                                    contentDescription = "Selected",
                                                    tint = AccentCyan,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .border(2.dp, Color.Gray.copy(alpha = 0.5f), CircleShape)
                                                )
                                            }
                                        }
                                    }
                                }
                                item { Spacer(modifier = Modifier.height(80.dp)) } 
                            }
                        }
                    }
                } else {
                    // STEP 2: Configure Shield
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "SELECTED APPS (${selectedApps.size})",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            ),
                            color = AccentCyan,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
                        ) {
                            items(selectedApps.toList()) { app ->
                                val pm = context.packageManager
                                val icon = remember(app.packageName) { ImageUtils.getAppIcon(pm, app.packageName) }
                                
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(72.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(SurfaceVariantDark)
                                            .border(1.dp, BorderGlass, RoundedCornerShape(16.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (icon != null) {
                                            Image(
                                                bitmap = icon,
                                                contentDescription = null,
                                                modifier = Modifier.size(42.dp).clip(RoundedCornerShape(10.dp))
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = app.appName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFCBD5E1),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                        
                        // CUSTOM RESTRICTIONS (3 boxes: Simple, Time Limit, Click Limit)
                        CustomRestrictionSection(
                            restrictionMode = restrictionMode,
                            onRestrictionModeChange = { restrictionMode = it },
                            timeLimitMinutes = timeLimitMinutes,
                            onTimeLimitChange = { timeLimitMinutes = it },
                            clickLimitCount = clickLimitCount,
                            onClickLimitChange = { clickLimitCount = it },
                            modifier = Modifier.padding(bottom = 24.dp)
                        )

                        Text(
                            text = "RESTRICTION MODE",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            ),
                            color = AccentCyan,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "CUSTOM QUOTE (OPTIONAL)",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            ),
                            color = AccentCyan,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        OutlinedTextField(
                            value = customQuote,
                            onValueChange = { customQuote = it },
                            placeholder = { Text(text = "Is this urgent, or are you chasing cheap dopamine?", color = Color.DarkGray) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SurfaceDark,
                                unfocusedContainerColor = SurfaceDark,
                                focusedBorderColor = AccentViolet,
                                unfocusedBorderColor = BorderGlass,
                                focusedTextColor = Color(0xFFCBD5E1),
                                unfocusedTextColor = Color.White
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            }
        }
        
        if (showCustomCategoryEditor) {
            CustomCategoryEditor(
                context = context,
                installedApps = installedApps,
                editingCategory = editingCustomCategory,
                onSave = { name, packages ->
                    CustomCategoryManager.saveCategory(
                        context, 
                        editingCustomCategory?.id, 
                        name.trim(), 
                        packages
                    )
                    customCategories = CustomCategoryManager.getCategories(context)
                    showCustomCategoryEditor = false
                },
                onDismiss = { showCustomCategoryEditor = false }
            )
        }
    }
}

@Composable
fun ModeSelector(
    title: String, 
    description: String, 
    isSelected: Boolean, 
    onClick: () -> Unit, 
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) Color(0xFF231D38) else Color(0xFF191C2B)
    val borderColor = if (isSelected) AccentViolet else Color(0xFF282D42)
    val titleColor = if (isSelected) Color.White else Color(0xFFCBD5E1)
    val subtitleColor = if (isSelected) Color(0xFFCBD5E1) else Color(0xFF94A3B8)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(backgroundColor)
            .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = titleColor
                )
                if (isSelected) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = AccentCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = subtitleColor
            )
        }
    }
}

@Composable
fun CustomCategoryEditor(
    context: Context,
    installedApps: List<InstalledApp>,
    editingCategory: CustomCategory?,
    onSave: (String, Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(editingCategory?.name ?: "") }
    var packages by remember { mutableStateOf(editingCategory?.packages ?: emptySet()) }
    var showPicker by remember { mutableStateOf(false) }

    if (showPicker) {
        AppPicker(
            context = context,
            installedApps = installedApps,
            initialSelection = packages,
            onConfirm = { 
                packages = it
                showPicker = false
            },
            onDismiss = { showPicker = false }
        )
    } else {
        androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color(0xFF161824),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
                    Text(
                        text = if (editingCategory == null) "Create Filter" else "Edit Filter", 
                        color = Color.White, 
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Group specific apps together to block them quickly.",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(24.dp))
                    
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Category Name") },
                        placeholder = { Text("e.g., Social Media") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = AccentCyan,
                            unfocusedBorderColor = BorderGlass,
                            focusedLabelColor = AccentCyan,
                            unfocusedLabelColor = Color.Gray,
                            focusedContainerColor = SurfaceDark,
                            unfocusedContainerColor = SurfaceDark
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(24.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically, 
                        horizontalArrangement = Arrangement.SpaceBetween, 
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Selected Apps", color = Color.White, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                        Text("${packages.size} selected", color = AccentCyan, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(AccentCyan.copy(alpha = 0.15f))
                                        .border(1.dp, AccentCyan, CircleShape)
                                        .clickable { showPicker = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = "Add App", tint = AccentCyan, modifier = Modifier.size(28.dp))
                                }
                                Spacer(Modifier.height(8.dp))
                                Text("Add App", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        
                        items(packages.toList()) { pkgName ->
                            val app = installedApps.find { it.packageName == pkgName }
                            if (app != null) {
                                val pm = context.packageManager
                                val icon = remember(app.packageName) { ImageUtils.getAppIcon(pm, app.packageName) }
                                
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(64.dp)) {
                                    Box {
                                        if (icon != null) {
                                            Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(60.dp).clip(CircleShape))
                                        } else {
                                            Box(modifier = Modifier.size(60.dp).background(SurfaceDark, CircleShape))
                                        }
                                        
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFEF4444))
                                                .border(2.dp, Color(0xFF161824), CircleShape)
                                                .clickable { packages = packages - pkgName },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Filled.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        app.appName, 
                                        maxLines = 1, 
                                        overflow = TextOverflow.Ellipsis, 
                                        color = Color(0xFFCBD5E1), 
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(32.dp))
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = onDismiss, modifier = Modifier.height(48.dp)) { 
                            Text("Cancel", color = Color.Gray)
                        }
                        Spacer(Modifier.width(12.dp))
                        Button(
                            onClick = { if (name.isNotBlank()) onSave(name, packages) },
                            modifier = Modifier.height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = MidnightBlack)
                        ) {
                            Text("Save Filter", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppPicker(
    context: Context,
    installedApps: List<InstalledApp>,
    initialSelection: Set<String>,
    onConfirm: (Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var selection by remember { mutableStateOf(initialSelection) }
    var selectedCategory by remember { mutableStateOf(AppCategory.ALL) }
    
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss, 
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(), 
            color = Color(0xFF0F111A)
        ) {
            Column(modifier = Modifier.systemBarsPadding()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp), 
                    horizontalArrangement = Arrangement.SpaceBetween, 
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Select Apps", color = Color.White, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Add apps to your custom filter", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.background(SurfaceDark, CircleShape)
                    ) { 
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White) 
                    }
                }
                
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(AppCategory.entries.toTypedArray()) { category ->
                        val isSelected = selectedCategory == category
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) AccentViolet else SurfaceDark)
                                .clickable { selectedCategory = category }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = category.title,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
                
                val filteredApps = remember(installedApps, selectedCategory) {
                    if (selectedCategory == AppCategory.ALL) installedApps
                    else installedApps.filter { it.category == selectedCategory }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${filteredApps.size} apps",
                        color = Color.Gray,
                        style = MaterialTheme.typography.labelMedium
                    )
                    Row {
                        val filteredPackages = filteredApps.map { it.packageName }.toSet()
                        val allSelected = filteredPackages.isNotEmpty() && selection.containsAll(filteredPackages)
                        if (allSelected) {
                            TextButton(onClick = { selection = selection - filteredPackages }) {
                                Text("Deselect All", color = Color(0xFFF44336))
                            }
                        } else {
                            TextButton(onClick = { selection = selection + filteredPackages }) {
                                Text("Select All", color = AccentCyan)
                            }
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredApps, key = { it.packageName }) { app ->
                        val isSelected = selection.contains(app.packageName)
                        val pm = context.packageManager
                        val icon = remember(app.packageName) { ImageUtils.getAppIcon(pm, app.packageName) }
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) AccentViolet.copy(alpha = 0.15f) else SurfaceDark)
                                .border(1.dp, if (isSelected) AccentViolet else BorderGlass, RoundedCornerShape(16.dp))
                                .clickable { selection = if (isSelected) selection - app.packageName else selection + app.packageName }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (icon != null) {
                                    Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)))
                                    Spacer(modifier = Modifier.width(16.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(app.appName, color = Color.White, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                                    Text(app.category.title, color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                                }
                                if (isSelected) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = "Selected", tint = AccentCyan, modifier = Modifier.size(28.dp))
                                } else {
                                    Box(modifier = Modifier.size(28.dp).border(2.dp, Color.Gray.copy(alpha = 0.5f), CircleShape))
                                }
                            }
                        }
                    }
                }
                
                Surface(
                    color = Color(0xFF0F111A).copy(alpha = 0.95f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { onConfirm(selection) },
                        modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = MidnightBlack)
                    ) {
                        Text("Confirm ${selection.size} Apps", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}
