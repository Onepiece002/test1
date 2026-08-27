package com.focusbyrj.app.ui.screens

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusbyrj.app.data.Task
import com.focusbyrj.app.data.FocusDatabase
import com.focusbyrj.app.ui.theme.FocusByRjTheme
import com.focusbyrj.app.data.AppRestriction
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.withStyle
import android.content.pm.PackageManager
import com.focusbyrj.app.util.SmartDateParser
import com.focusbyrj.app.util.FocusEconomyManager
import com.focusbyrj.app.util.CustomCategoryManager
import com.focusbyrj.app.util.CustomCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ChatMessage(val id: String, val text: String, val isUser: Boolean, val timestamp: Long = System.currentTimeMillis())

class BubbleChatActivity : ComponentActivity() {

    private val closeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.focusbyrj.app.CLOSE_CHAT") {
                sendBroadcast(Intent("com.focusbyrj.app.TRIGGER_CLOSE_ANIM"))
            }
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val filter = IntentFilter("com.focusbyrj.app.CLOSE_CHAT")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(closeReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(closeReceiver, filter)
        }
        
        sendBroadcast(Intent("com.focusbyrj.app.CHAT_OPENED"))
        
        setContent {
            FocusByRjTheme {
                var isVisible by remember { mutableStateOf(false) }
                var hasOpened by remember { mutableStateOf(false) }
                val context = LocalContext.current
                
                DisposableEffect(Unit) {
                    val animReceiver = object : BroadcastReceiver() {
                        override fun onReceive(c: Context?, i: Intent?) {
                            isVisible = false
                        }
                    }
                    val f = IntentFilter("com.focusbyrj.app.TRIGGER_CLOSE_ANIM")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        context.registerReceiver(animReceiver, f, Context.RECEIVER_EXPORTED)
                    } else {
                        context.registerReceiver(animReceiver, f)
                    }
                    onDispose { context.unregisterReceiver(animReceiver) }
                }

                LaunchedEffect(Unit) {
                    isVisible = true
                    hasOpened = true
                }
                
                LaunchedEffect(isVisible) {
                    if (!isVisible && hasOpened) {
                        delay(300)
                        finish()
                        overridePendingTransition(0, 0)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = {
                                isVisible = false
                            })
                        },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = slideInVertically(
                            initialOffsetY = { it },
                            animationSpec = tween(300)
                        ) + fadeIn(),
                        exit = slideOutVertically(
                            targetOffsetY = { it },
                            animationSpec = tween(300)
                        ) + fadeOut()
                    ) {
                        ChatInterface()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(closeReceiver)
        sendBroadcast(Intent("com.focusbyrj.app.CHAT_CLOSED"))
    }
}

@Composable
fun ChatInterface() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val profile by FocusEconomyManager.profileFlow.collectAsState()
    
    val prefs = remember { context.getSharedPreferences("bubble_prefs", android.content.Context.MODE_PRIVATE) }
    
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var showMenu by remember { mutableStateOf(false) }
    
    // Messages start clean; users can type /summary to view tasks on demand
    val focusApp = remember { context.applicationContext as com.focusbyrj.app.FocusApplication }
    val restrictions by focusApp.database.appRestrictionDao().getAllRestrictions().collectAsState(initial = emptyList())
    val lockedPackages = remember(restrictions) {
        restrictions.filter { it.isRestricted }.map { it.packageName }.toSet()
    }
    
    var inputText by remember { mutableStateOf("") }
    var isHighPriority by remember { mutableStateOf(false) }
    var isPersistent by remember { mutableStateOf(false) }

    var installedApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var lastSummaryTasks by remember { mutableStateOf<List<com.focusbyrj.app.data.Task>>(emptyList()) }
    
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val apps = packages.mapNotNull { 
                val name = pm.getApplicationLabel(it).toString()
                if (name.isNotBlank() && it.packageName != context.packageName) {
                    val category = getCategoryForApp(it, it.packageName)
                    AppInfo(name, it.packageName, category)
                } else null
            }.sortedBy { it.name }
            installedApps = apps
        }
    }
    
    val suggestions = remember(inputText, installedApps, lockedPackages) {
        if (!inputText.startsWith("/")) return@remember emptyList<Suggestion>()
        val parts = inputText.split(" ")
        val cmd = parts[0].lowercase()
        
        when {
            parts.size == 1 -> {
                val available = listOf("/summary", "/lock", "/unlock", "/priority", "/postpone all", "/reschedule", "/clear", "/block", "/unblock")
                available.filter { it.startsWith(cmd) }.map { Suggestion(it, "$it ") }
            }
            cmd == "/summary" && parts.size == 2 -> {
                val typed = parts[1].lowercase()
                val modes = listOf("all")
                modes.filter { it.startsWith(typed) }.map { Suggestion("all", "/summary all ") }
            }
            cmd == "/reschedule" && parts.size == 2 -> {
                val typed = parts[1].lowercase()
                lastSummaryTasks.mapIndexed { index, task -> 
                    val num = (index + 1).toString()
                    Suggestion("$num: ${task.title.take(15)}...", "/reschedule $num ")
                }.filter { it.displayText.startsWith(typed) || it.replacementText.contains(" $typed") }
            }
            cmd == "/lock" -> {
                val query = parts.drop(1).joinToString(" ").lowercase().trim()
                val standardFilters = listOf("all", "Social", "Finance", "Shopping", "Games", "Utility", "Others")
                val customFilters = CustomCategoryManager.getCategories(context).map { it.name }
                val allFilterOptions = standardFilters + customFilters
                
                val matchedFilters = allFilterOptions
                    .filter { it.lowercase().contains(query) }
                    .map { Suggestion("📁 $it", "$cmd $it ") }
                
                val matchedApps = installedApps
                    .filter { it.name.lowercase().contains(query) }
                    .take(5)
                    .map { Suggestion("📱 ${it.name}", "$cmd ${it.name} ") }
                
                matchedFilters + matchedApps
            }
            cmd == "/unlock" || cmd == "/unblock" -> {
                val query = parts.drop(1).joinToString(" ").lowercase().trim()
                
                val lockedStandardFilters = mutableListOf<String>()
                if (installedApps.any { it.category == AppCategory.SOCIAL && lockedPackages.contains(it.packageName) }) {
                    lockedStandardFilters.add("Social")
                }
                if (installedApps.any { it.category == AppCategory.PAYMENT && lockedPackages.contains(it.packageName) }) {
                    lockedStandardFilters.add("Finance")
                }
                if (installedApps.any { it.category == AppCategory.SHOPPING && lockedPackages.contains(it.packageName) }) {
                    lockedStandardFilters.add("Shopping")
                }
                if (installedApps.any { it.category == AppCategory.GAMES && lockedPackages.contains(it.packageName) }) {
                    lockedStandardFilters.add("Games")
                }
                if (installedApps.any { it.category == AppCategory.UTILITY && lockedPackages.contains(it.packageName) }) {
                    lockedStandardFilters.add("Utility")
                }
                if (installedApps.any { it.category == AppCategory.OTHERS && lockedPackages.contains(it.packageName) }) {
                    lockedStandardFilters.add("Others")
                }
                
                val customCats = CustomCategoryManager.getCategories(context)
                val lockedCustomFilters = customCats.filter { cat ->
                    cat.packages.any { lockedPackages.contains(it) }
                }.map { it.name }
                
                val lockedFilterOptions = (if (lockedPackages.isNotEmpty()) listOf("all") else emptyList()) + lockedStandardFilters + lockedCustomFilters
                
                val matchedFilters = lockedFilterOptions
                    .filter { it.lowercase().contains(query) }
                    .map { Suggestion("📁 $it", "$cmd $it ") }
                
                val matchedApps = installedApps
                    .filter { lockedPackages.contains(it.packageName) && it.name.lowercase().contains(query) }
                    .take(5)
                    .map { Suggestion("📱 ${it.name}", "$cmd ${it.name} ") }
                
                if (matchedFilters.isEmpty() && matchedApps.isEmpty() && lockedPackages.isEmpty()) {
                    listOf(Suggestion("ℹ️ No apps are currently locked", "$cmd "))
                } else {
                    matchedFilters + matchedApps
                }
            }
            cmd == "/block" && parts.size == 2 -> {
                val typed = parts[1].lowercase()
                val modes = listOf("Hard", "Soft")
                val filterMatches = listOf("all", "Social", "Finance", "Shopping", "Games", "Utility", "Others")
                val customFilters = CustomCategoryManager.getCategories(context).map { it.name }
                
                val modeSuggestions = modes.filter { it.lowercase().startsWith(typed) }.map { Suggestion(it, "/block $it ") }
                val filterSuggestions = (filterMatches + customFilters).filter { it.lowercase().contains(typed) }.map { Suggestion("📁 $it", "/block $it ") }
                val appSuggestions = installedApps.filter { it.name.lowercase().contains(typed) }.take(5).map { Suggestion("📱 ${it.name}", "/block ${it.name} ") }
                modeSuggestions + filterSuggestions + appSuggestions
            }
            (cmd == "/block" && parts.size >= 3) -> {
                val query = parts.drop(2).joinToString(" ").lowercase()
                val appSuggestions = installedApps.filter { it.name.lowercase().contains(query) }.take(5).map { 
                    Suggestion(it.name, "/block ${parts[1]} ${it.name} ") 
                }
                appSuggestions
            }
            else -> emptyList()
        }
    }


    val parsedResult = remember(inputText) {
        if (inputText.isNotBlank()) SmartDateParser.parse(inputText) else null
    }

    fun sendMessage() {
        if (inputText.isNotBlank()) {
            val userMsg = ChatMessage(System.currentTimeMillis().toString(), inputText, true)
            messages = messages + userMsg
            val sentText = inputText.trim()
            val finalTitle = parsedResult?.cleanText?.takeIf { it.isNotBlank() } ?: sentText
            val dueDate = parsedResult?.timestamp ?: System.currentTimeMillis()
            
            val wasPriority = isHighPriority
            val wasPersistent = isPersistent
            inputText = ""
            isHighPriority = false
            isPersistent = false
            
            coroutineScope.launch(Dispatchers.IO) {
                val app = context.applicationContext as com.focusbyrj.app.FocusApplication
                val repo = app.taskRepository
                val db = app.database
                
                if (sentText.startsWith("/")) {
                    val parts = sentText.split(" ")
                    val cmd = parts[0].lowercase()
                    var replyMsg = "Command not recognized."
                    
                    when (cmd) {
                        "/clear" -> {
                            withContext(Dispatchers.Main) {
                                messages = emptyList()
                            }
                            return@launch
                        }
                        "/summary" -> {
                            val isAll = parts.getOrNull(1)?.lowercase() == "all"
                            
                            val now = System.currentTimeMillis()
                            val startOfDay = java.util.Calendar.getInstance().apply {
                                set(java.util.Calendar.HOUR_OF_DAY, 0)
                                set(java.util.Calendar.MINUTE, 0)
                                set(java.util.Calendar.SECOND, 0)
                                set(java.util.Calendar.MILLISECOND, 0)
                            }.timeInMillis
                            val endOfDay = startOfDay + 86400000L - 1
                            
                            // Cleanup old completed tasks
                            repo.deleteCompletedTasksBefore(startOfDay)
                            
                            val allTasks = repo.allTasks.first()
                            val completedToday = allTasks.filter { it.isCompleted && it.completedAt != null && it.completedAt >= startOfDay }
                            val pendingTasks = allTasks.filter { !it.isCompleted }
                            
                            val targetTasks = if (isAll) {
                                pendingTasks
                            } else {
                                pendingTasks.filter { it.dueDate == null || (it.dueDate in startOfDay..endOfDay) || it.dueDate < startOfDay }
                            }
                            
                            val overdueCount = targetTasks.count { it.dueDate != null && it.dueDate < now }
                            val sortedTasks = targetTasks.sortedWith(compareByDescending<com.focusbyrj.app.data.Task> { it.isPriority }.thenBy { it.dueDate ?: Long.MAX_VALUE })
                            
                            withContext(Dispatchers.Main) {
                                lastSummaryTasks = sortedTasks
                            }
                            
                            val builder = StringBuilder()
                            
                            val cal = java.util.Calendar.getInstance()
                            val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
                            val greeting = when {
                                isAll -> "📋 *__OVERALL BRIEFING__*\n_Here's your comprehensive update:_"
                                hour < 12 -> "🌅 *__Good Morning!__*\n_Here is your daily update:_"
                                hour < 17 -> "☀️ *__Good Afternoon!__*\n_Here is your mid-day update:_"
                                else -> "🌙 *__Good Evening!__*\n_Here is your evening wrap-up:_"
                            }
                            builder.append(greeting).append("\n\n")
                            
                            if (completedToday.isNotEmpty() && !isAll) {
                                builder.append("✅ *__Completed Today__* *(${completedToday.size})*:\n")
                                completedToday.forEach { task ->
                                    builder.append("• _${task.title}_\n")
                                }
                                builder.append("\n")
                            }
                            
                            if (sortedTasks.isEmpty()) {
                                builder.append("🎉 *All clear for today!*\n")
                                val nextTask = pendingTasks
                                    .filter { it.dueDate != null && it.dueDate > endOfDay }
                                    .minByOrNull { it.dueDate!! }
                                if (nextTask != null) {
                                    builder.append("\n🗓️ *__Up Next__*: *${nextTask.title}* _(${SmartDateParser.formatDueDate(nextTask.dueDate)})_\n")
                                }
                            } else {
                                val headerTitle = if (isAll) "Pending Tasks" else "Upcoming Today"
                                val icon = if (isAll) "⏳" else "⚡"
                                builder.append("$icon *__${headerTitle}__* *(${sortedTasks.size})*")
                                if (overdueCount > 0) builder.append(" *[⚠️ $overdueCount Overdue]*")
                                builder.append(":\n")
                                
                                sortedTasks.forEachIndexed { index, task ->
                                    val prefix = if (task.isPriority) "🔥 " else ""
                                    val titleFormatted = if (task.isPriority) "*${task.title}*" else task.title
                                    val dueStr = if (task.dueDate != null) " _(Due: ${SmartDateParser.formatDueDate(task.dueDate)})_" else ""
                                    builder.append("${index + 1}. $prefix$titleFormatted$dueStr\n")
                                }
                            }
                            
                            val totalToday = completedToday.size + targetTasks.size
                            val percent = if (totalToday > 0) {
                                (completedToday.size * 100) / totalToday
                            } else {
                                100
                            }
                            val filledBlocks = (percent / 10).coerceIn(0, 10)
                            val emptyBlocks = 10 - filledBlocks
                            val progressBar = "█".repeat(filledBlocks) + "░".repeat(emptyBlocks)
                            
                            builder.append("\n📊 *__Daily Progress__*:\n")
                            builder.append("`[$progressBar]` *$percent%*")
                            
                            replyMsg = builder.toString().trimEnd()
                        }
                        "/reschedule" -> {
                            val numStr = parts.getOrNull(1)
                            val timeStr = parts.drop(2).joinToString(" ")
                            
                            if (numStr == null || timeStr.isEmpty()) {
                                replyMsg = "Usage: /reschedule <number> <time/date>"
                            } else {
                                val num = numStr.toIntOrNull()
                                if (num == null || num < 1 || num > lastSummaryTasks.size) {
                                    replyMsg = "Invalid task reference. Please run /summary first."
                                } else {
                                    val task = lastSummaryTasks[num - 1]
                                    val parsed = SmartDateParser.parse("reschedule to $timeStr")
                                    if (parsed.timestamp != null) {
                                        repo.updateTask(task.copy(dueDate = parsed.timestamp))
                                        replyMsg = "Shifted '${task.title}' to ${SmartDateParser.formatDueDate(parsed.timestamp)}."
                                    } else {
                                        replyMsg = "Unrecognized temporal format: '$timeStr'"
                                    }
                                }
                            }
                        }
                        "/priority" -> {
                            val tasks = repo.allTasks.first()
                            val priority = tasks.filter { it.isPriority && !it.isCompleted }
                            replyMsg = if (priority.isEmpty()) "No priority items in queue." 
                                else "PRIORITY ITEMS:\n" + priority.joinToString("\n") { "• ${it.title}" }
                        }
                        "/postpone" -> {
                            if (parts.getOrNull(1)?.lowercase() == "all") {
                                val tasks = repo.allTasks.first().filter { !it.isCompleted }
                                tasks.forEach { 
                                    repo.updateTask(it.copy(dueDate = System.currentTimeMillis() + 86400000L)) 
                                }
                                replyMsg = "Shifted ${tasks.size} items to tomorrow."
                            }
                        }
                        "/lock", "/block" -> {
                            val isExplicitMode = parts.size >= 3 && (parts[1].equals("Hard", true) || parts[1].equals("Soft", true))
                            val mode = if (isExplicitMode) parts[1].uppercase() else "HARD"
                            val rawArg = if (isExplicitMode) parts.drop(2).joinToString(" ").trim() else parts.drop(1).joinToString(" ").trim()

                            if (rawArg.isEmpty()) {
                                val customCats = CustomCategoryManager.getCategories(context)
                                val customNames = if (customCats.isNotEmpty()) ", " + customCats.joinToString(", ") { it.name } else ""
                                replyMsg = "Usage: /lock <Filter/App>\nAvailable filters: all, Social, Finance, Shopping, Games, Utility, Others$customNames"
                            } else if (rawArg.equals("all", ignoreCase = true)) {
                                val targetApps = installedApps
                                val restrictions = targetApps.map {
                                    AppRestriction(it.packageName, it.name, isRestricted = true, mode = mode, restrictionMode = "SIMPLE")
                                }
                                db.appRestrictionDao().insertRestrictions(restrictions)
                                replyMsg = "🔒 *__Lock Protocol Active__*\n_All applications (${targetApps.size} apps) have been locked._"
                            } else {
                                val matchedStandardCat = when (rawArg.lowercase()) {
                                    "social" -> AppCategory.SOCIAL
                                    "finance", "finances", "payment" -> AppCategory.PAYMENT
                                    "shopping" -> AppCategory.SHOPPING
                                    "games", "game" -> AppCategory.GAMES
                                    "utility", "utilities" -> AppCategory.UTILITY
                                    "others", "other" -> AppCategory.OTHERS
                                    else -> null
                                }

                                val customCats = CustomCategoryManager.getCategories(context)
                                val matchedCustomCat = customCats.find { it.name.equals(rawArg, ignoreCase = true) }
                                val matchedApp = installedApps.find { it.name.equals(rawArg, ignoreCase = true) }

                                if (matchedStandardCat != null) {
                                    val targetApps = installedApps.filter { it.category == matchedStandardCat }
                                    if (targetApps.isEmpty()) {
                                        replyMsg = "⚠️ _No applications found under '${matchedStandardCat.title}' category._"
                                    } else {
                                        val restrictions = targetApps.map {
                                            AppRestriction(it.packageName, it.name, isRestricted = true, mode = mode, restrictionMode = "SIMPLE")
                                        }
                                        db.appRestrictionDao().insertRestrictions(restrictions)
                                        replyMsg = "🔒 *__Lock Protocol Active__*\n_Locked all ${matchedStandardCat.title} apps (${targetApps.size} apps)._"
                                    }
                                } else if (matchedCustomCat != null) {
                                    val targetApps = installedApps.filter { matchedCustomCat.packages.contains(it.packageName) }
                                    if (targetApps.isEmpty()) {
                                        replyMsg = "⚠️ _No apps currently assigned to custom filter '${matchedCustomCat.name}'._"
                                    } else {
                                        val restrictions = targetApps.map {
                                            AppRestriction(it.packageName, it.name, isRestricted = true, mode = mode, restrictionMode = "SIMPLE")
                                        }
                                        db.appRestrictionDao().insertRestrictions(restrictions)
                                        replyMsg = "🔒 *__Lock Protocol Active__*\n_Locked all apps in '${matchedCustomCat.name}' filter (${targetApps.size} apps)._"
                                    }
                                } else if (matchedApp != null) {
                                    db.appRestrictionDao().insertRestriction(
                                        AppRestriction(matchedApp.packageName, matchedApp.name, isRestricted = true, mode = mode, restrictionMode = "SIMPLE")
                                    )
                                    replyMsg = "🔒 *__Lock Protocol Active__*\n_Locked ${matchedApp.name} [$mode]._ "
                                } else {
                                    val customNames = if (customCats.isNotEmpty()) ", " + customCats.joinToString(", ") { it.name } else ""
                                    replyMsg = "⚠️ _System could not find filter or app '$rawArg'._\n_Available filters: all, Social, Finance, Shopping, Games, Utility, Others$customNames._"
                                }
                            }
                        }
                        "/unlock", "/unblock" -> {
                            val rawArg = parts.drop(1).joinToString(" ").trim()

                            if (rawArg.isEmpty()) {
                                val customCats = CustomCategoryManager.getCategories(context)
                                val customNames = if (customCats.isNotEmpty()) ", " + customCats.joinToString(", ") { it.name } else ""
                                replyMsg = "Usage: /unlock <Filter/App>\nAvailable filters: all, Social, Finance, Shopping, Games, Utility, Others$customNames"
                            } else if (rawArg.equals("all", ignoreCase = true)) {
                                db.appRestrictionDao().deleteAllRestrictions()
                                replyMsg = "🔓 *__Lock Protocol Lifted__*\n_All applications have been unlocked._"
                            } else {
                                val matchedStandardCat = when (rawArg.lowercase()) {
                                    "social" -> AppCategory.SOCIAL
                                    "finance", "finances", "payment" -> AppCategory.PAYMENT
                                    "shopping" -> AppCategory.SHOPPING
                                    "games", "game" -> AppCategory.GAMES
                                    "utility", "utilities" -> AppCategory.UTILITY
                                    "others", "other" -> AppCategory.OTHERS
                                    else -> null
                                }

                                val customCats = CustomCategoryManager.getCategories(context)
                                val matchedCustomCat = customCats.find { it.name.equals(rawArg, ignoreCase = true) }
                                val matchedApp = installedApps.find { it.name.equals(rawArg, ignoreCase = true) }

                                if (matchedStandardCat != null) {
                                    val targetApps = installedApps.filter { it.category == matchedStandardCat }
                                    if (targetApps.isEmpty()) {
                                        replyMsg = "⚠️ _No applications found under '${matchedStandardCat.title}' category._"
                                    } else {
                                        db.appRestrictionDao().deleteRestrictions(targetApps.map { it.packageName })
                                        replyMsg = "🔓 *__Lock Protocol Lifted__*\n_Unlocked all ${matchedStandardCat.title} apps (${targetApps.size} apps)._"
                                    }
                                } else if (matchedCustomCat != null) {
                                    val targetPkgs = matchedCustomCat.packages.toList()
                                    if (targetPkgs.isEmpty()) {
                                        replyMsg = "⚠️ _No apps configured in '${matchedCustomCat.name}' filter._"
                                    } else {
                                        db.appRestrictionDao().deleteRestrictions(targetPkgs)
                                        replyMsg = "🔓 *__Lock Protocol Lifted__*\n_Unlocked all apps in '${matchedCustomCat.name}' filter (${targetPkgs.size} apps)._"
                                    }
                                } else if (matchedApp != null) {
                                    db.appRestrictionDao().deleteRestriction(matchedApp.packageName)
                                    replyMsg = "🔓 *__Lock Protocol Lifted__*\n_Unlocked ${matchedApp.name}._"
                                } else {
                                    val customNames = if (customCats.isNotEmpty()) ", " + customCats.joinToString(", ") { it.name } else ""
                                    replyMsg = "⚠️ _System could not find filter or app '$rawArg'._\n_Available filters: all, Social, Finance, Shopping, Games, Utility, Others$customNames._"
                                }
                            }
                        }
                    }
                    withContext(Dispatchers.Main) {
                        messages = messages + ChatMessage(System.currentTimeMillis().toString() + "bot", replyMsg, false)
                    }
                    return@launch
                }
                
                val newTask = Task(
                    title = finalTitle,
                    isPriority = wasPriority,
                    isPersistent = wasPersistent,
                    dueDate = dueDate
                )
                repo.insertTask(newTask)
                
                withContext(Dispatchers.Main) {
                    val attrs = mutableListOf<String>()
                    if (wasPriority) attrs.add("priority")
                    if (wasPersistent) attrs.add("persistent")
                    
                    val attrStr = if (attrs.isNotEmpty()) " ${attrs.joinToString(" and ")}" else ""
                    val dueStr = if (parsedResult?.timestamp != null) " for ${SmartDateParser.formatDueDate(dueDate)}" else ""
                    
                    messages = messages + ChatMessage(
                        System.currentTimeMillis().toString() + "bot", 
                        "Added$attrStr task: \"$finalTitle\"$dueStr.", 
                        false
                    )
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(top = 110.dp) // Space for the floating bubble!
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .pointerInput(Unit) { 
                detectTapGestures(onTap = { /* Prevent clicks from falling through */ })
            }
    ) {
        // Handle drag bar and menu
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Menu", tint = MaterialTheme.colorScheme.onSurface)
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Clear History") },
                        onClick = {
                            messages = emptyList()
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Open Settings") },
                        onClick = {
                            showMenu = false
                            val i = Intent(context, com.focusbyrj.app.MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                putExtra("navigate_to", "bubble_settings")
                            }
                            context.startActivity(i)
                            (context as? android.app.Activity)?.finish()
                        }
                    )
                }
            }
            
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                    .align(Alignment.Center)
            )
        }

        if (messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = parseRichFormattedText("💬 *__Focus Assistant__*\n_Chat history is cleared._\n_Type a task to add it or use `/` for commands._"),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 22.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                reverseLayout = true
            ) {
                items(messages.reversed()) { msg ->
                    ChatBubble(msg)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        // Input Area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            AnimatedVisibility(visible = suggestions.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha=0.5f), RoundedCornerShape(12.dp)),
                    reverseLayout = true
                ) {
                    items(suggestions) { suggestion ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { inputText = suggestion.replacementText }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(suggestion.displayText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha=0.2f))
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Toggles Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Priority Toggle
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isHighPriority) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface)
                            .border(1.dp, if (isHighPriority) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha=0.5f), CircleShape)
                            .clickable { isHighPriority = !isHighPriority },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Whatshot,
                            contentDescription = "Priority",
                            tint = if (isHighPriority) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Persistent Toggle
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isPersistent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                            .border(1.dp, if (isPersistent) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha=0.5f), CircleShape)
                            .clickable { isPersistent = !isPersistent },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsActive,
                            contentDescription = "Persistent",
                            tint = if (isPersistent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                AnimatedVisibility(visible = parsedResult?.timestamp != null) {
                    Text(
                        text = "Setting due: ${parsedResult?.timestamp?.let { SmartDateParser.formatDueDate(it) }}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(if (inputText.startsWith("/")) "Enter command..." else "Add a new task...") },
                    shape = RoundedCornerShape(24.dp),
                    visualTransformation = CommandVisualTransformation(),
                    trailingIcon = {
                        Box(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                .clickable { 
                                    if (!inputText.startsWith("/")) {
                                        inputText = "/$inputText"
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("/", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    ),
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { sendMessage() })
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable(enabled = inputText.isNotBlank()) { sendMessage() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Send,
                        contentDescription = "Send",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            // Navigation Bar padding
            Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val df = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val timeString = df.format(Date(message.timestamp))

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
        ) {
            if (!message.isUser) {
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
                        contentDescription = "Focus Assistant",
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            androidx.compose.material3.Surface(
                modifier = Modifier.widthIn(max = 280.dp),
                shape = RoundedCornerShape(
                    topStart = 24.dp,
                    topEnd = 24.dp,
                    bottomStart = if (message.isUser) 24.dp else 4.dp,
                    bottomEnd = if (message.isUser) 4.dp else 24.dp
                ),
                color = if (message.isUser) androidx.compose.material3.MaterialTheme.colorScheme.primary else androidx.compose.material3.MaterialTheme.colorScheme.surface,
                contentColor = if (message.isUser) androidx.compose.material3.MaterialTheme.colorScheme.onPrimary else androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                border = if (message.isUser) null else androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                shadowElevation = if (message.isUser) 0.dp else 4.dp
            ) {
                Text(
                    text = parseRichFormattedText(message.text),
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 22.sp,
                        letterSpacing = 0.2.sp
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }
        Text(
            text = timeString,
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.padding(
                top = 4.dp, 
                start = if (message.isUser) 0.dp else 40.dp, 
                end = if (message.isUser) 4.dp else 0.dp
            )
        )
    }
}

data class AppInfo(val name: String, val packageName: String, val category: AppCategory = AppCategory.OTHERS)
data class Suggestion(val displayText: String, val replacementText: String)

fun parseRichFormattedText(rawText: String): AnnotatedString {
    val builder = AnnotatedString.Builder()
    var i = 0
    val n = rawText.length

    while (i < n) {
        if (i + 2 < n && rawText[i] == '*' && rawText[i + 1] == '_' && rawText[i + 2] == '_') {
            // Bold + Underline: *__text__*
            val closeIdx = rawText.indexOf("__*", i + 3)
            if (closeIdx != -1) {
                val content = rawText.substring(i + 3, closeIdx)
                builder.withStyle(
                    SpanStyle(
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        textDecoration = TextDecoration.Underline
                    )
                ) {
                    append(content)
                }
                i = closeIdx + 3
                continue
            }
        } else if (i + 1 < n && rawText[i] == '_' && rawText[i + 1] == '_') {
            // Underline: __text__
            val closeIdx = rawText.indexOf("__", i + 2)
            if (closeIdx != -1) {
                val content = rawText.substring(i + 2, closeIdx)
                builder.withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                    append(content)
                }
                i = closeIdx + 2
                continue
            }
        } else if (rawText[i] == '*') {
            // Bold: *text*
            val closeIdx = rawText.indexOf('*', i + 1)
            if (closeIdx != -1 && closeIdx > i + 1) {
                val content = rawText.substring(i + 1, closeIdx)
                builder.withStyle(SpanStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)) {
                    append(content)
                }
                i = closeIdx + 1
                continue
            }
        } else if (rawText[i] == '_') {
            // Italic: _text_
            val closeIdx = rawText.indexOf('_', i + 1)
            if (closeIdx != -1 && closeIdx > i + 1) {
                val content = rawText.substring(i + 1, closeIdx)
                builder.withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    append(content)
                }
                i = closeIdx + 1
                continue
            }
        } else if (rawText[i] == '`') {
            // Code/Monospace: `text`
            val closeIdx = rawText.indexOf('`', i + 1)
            if (closeIdx != -1 && closeIdx > i + 1) {
                val content = rawText.substring(i + 1, closeIdx)
                builder.withStyle(
                    SpanStyle(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                    )
                ) {
                    append(content)
                }
                i = closeIdx + 1
                continue
            }
        }

        builder.append(rawText[i])
        i++
    }

    return builder.toAnnotatedString()
}

class CommandVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val input = text.text
        val builder = AnnotatedString.Builder()
        if (input.startsWith("/")) {
            val parts = input.split(" ", limit = 3)
            val cmd = parts[0]
            builder.withStyle(SpanStyle(color = androidx.compose.ui.graphics.Color(0xFF3B82F6), fontWeight = FontWeight.Bold)) {
                append(cmd)
            }
            if (parts.size > 1) {
                builder.append(" ")
                val arg1 = parts[1]
                if ((cmd == "/block" || cmd == "/lock") && (arg1.equals("Hard", true) || arg1.equals("Soft", true))) {
                    builder.withStyle(SpanStyle(color = androidx.compose.ui.graphics.Color(0xFF10B981), fontWeight = FontWeight.SemiBold)) {
                        builder.append(arg1)
                    }
                } else if (cmd == "/lock" || cmd == "/unlock" || cmd == "/block" || cmd == "/unblock") {
                    builder.withStyle(SpanStyle(color = androidx.compose.ui.graphics.Color(0xFF10B981), fontWeight = FontWeight.SemiBold)) {
                        builder.append(arg1)
                    }
                } else {
                    builder.append(arg1)
                }
                
                if (parts.size > 2) {
                    builder.append(" ")
                    builder.withStyle(SpanStyle(color = androidx.compose.ui.graphics.Color(0xFF8B5CF6))) {
                        append(parts[2])
                    }
                }
            }
        } else {
            builder.append(input)
        }
        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}
