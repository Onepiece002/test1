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
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material3.*
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close

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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.withStyle
import android.content.pm.PackageManager
import com.focusbyrj.app.util.SmartDateParser
import com.focusbyrj.app.util.TaskReminderHelper
import com.focusbyrj.app.widget.TodoWidgetProvider
import com.focusbyrj.app.util.FocusEconomyManager
import com.focusbyrj.app.util.CustomCategoryManager
import com.focusbyrj.app.util.CustomCategory
import com.focusbyrj.app.util.BubbleChatManager
import com.focusbyrj.app.util.PersistedChatMessage
import com.focusbyrj.app.ui.components.ProfessionalSlider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DrillSession(val difficulty: String, val targetQuestions: Int = -1, var correct: Int = 0, var total: Int = 0, var xp: Int = 0, var gold: Int = 0)

data class QuickActionCommand(val label: String, val commandText: String)

data class ChatMessage(
    val id: String, 
    val text: String, 
    val isUser: Boolean, 
    val timestamp: Long = System.currentTimeMillis(),
    val isArithmetic: Boolean = false,
    val arithmeticJson: String? = null,
    val isDrillSummary: Boolean = false,
    val drillSummaryJson: String? = null,
    val isAptitudeProfile: Boolean = false,
    val isStreakPrompt: Boolean = false,
    val streakPromptJson: String? = null,
    val isTaskSummary: Boolean = false,
    val taskSummaryJson: String? = null,
    val isTalkAction: Boolean = false,
    val talkActionJson: String? = null,
    val pendingActionJson: String? = null
)

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

    override fun onResume() {
        super.onResume()
        sendBroadcast(Intent("com.focusbyrj.app.CHAT_OPENED"))
    }

    override fun onPause() {
        super.onPause()
        sendBroadcast(Intent("com.focusbyrj.app.CHAT_CLOSED"))
        if (isFinishing) {
            // Optional: any specific finish logic
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(closeReceiver)

    }
}

@Composable
fun ChatInterface() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val profile by FocusEconomyManager.profileFlow.collectAsState()
    
    val prefs = remember { context.getSharedPreferences("bubble_prefs", android.content.Context.MODE_PRIVATE) }
    
    var messages by remember { 
        val stored = BubbleChatManager.getMessages(context)
        val initialList = if (stored.isEmpty()) {
            val welcome = PersistedChatMessage(
                id = "welcome_${System.currentTimeMillis()}",
                text = com.focusbyrj.app.util.AyvaDialogueEngine.getHelloWelcomeMessage(context),
                isUser = false,
                timestamp = System.currentTimeMillis()
            )
            BubbleChatManager.saveMessages(context, listOf(welcome))
            listOf(welcome)
        } else {
            stored
        }
        mutableStateOf<List<ChatMessage>>(
            initialList.map {
                ChatMessage(it.id, it.text, it.isUser, it.timestamp, it.isArithmetic, it.arithmeticJson, it.isDrillSummary, it.drillSummaryJson, it.isAptitudeProfile, it.isStreakPrompt, it.streakPromptJson, it.isTaskSummary, it.taskSummaryJson, it.isTalkAction, it.talkActionJson, it.pendingActionJson)
            }
        ) 
    }
    var showMenu by remember { mutableStateOf(false) }
    var showFontSizeDialog by remember { mutableStateOf(false) }
    var chatFontSizeSp by remember { mutableStateOf(prefs.getFloat("chat_font_size_sp", 15f)) }
    var activeDrillSession by remember { mutableStateOf<DrillSession?>(null) }

    var inputTextFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    val inputText = inputTextFieldValue.text
    var isHighPriority by remember { mutableStateOf(false) }
    var isPersistent by remember { mutableStateOf(false) }

    val quickActionCommands = remember {
        listOf(
            QuickActionCommand("/talk", "/talk "),
            QuickActionCommand("/tasks", "/tasks "),
            QuickActionCommand("/tasks all", "/tasks all "),
            QuickActionCommand("/summary", "/summary "),
            QuickActionCommand("/drill", "/drill "),
            QuickActionCommand("/clear", "/clear "),
            QuickActionCommand("/priority", "/priority "),
            QuickActionCommand("/reschedule", "/reschedule "),
            QuickActionCommand("/profile", "/profile "),
            QuickActionCommand("/postpone all", "/postpone all ")
        )
    }

    val onFillCommand = { commandText: String ->
        inputTextFieldValue = TextFieldValue(
            text = commandText,
            selection = TextRange(commandText.length)
        )
    }

    val updateFontSize = { newSize: Float ->
        val clamped = newSize.coerceIn(12f, 24f)
        chatFontSizeSp = clamped
        prefs.edit().putFloat("chat_font_size_sp", clamped).apply()
    }
    
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }
    
    LaunchedEffect(messages) {
        BubbleChatManager.saveMessages(context, messages.map {
            PersistedChatMessage(it.id, it.text, it.isUser, it.timestamp, it.isArithmetic, it.arithmeticJson, it.isDrillSummary, it.drillSummaryJson, it.isAptitudeProfile, it.isStreakPrompt, it.streakPromptJson, it.isTaskSummary, it.taskSummaryJson, it.isTalkAction, it.talkActionJson, it.pendingActionJson)
        })
    }
    
    val focusApp = remember { context.applicationContext as com.focusbyrj.app.FocusApplication }
    val allTasksList by focusApp.taskRepository.allTasks.collectAsState(initial = emptyList())
    val pendingTasksList = remember(allTasksList) {
        allTasksList.filter { !it.isCompleted }
            .sortedWith(compareByDescending<com.focusbyrj.app.data.Task> { it.isPriority }.thenBy { it.dueDate ?: Long.MAX_VALUE })
    }

    val restrictions by focusApp.database.appRestrictionDao().getAllRestrictions().collectAsState(initial = emptyList())
    val lockedPackages = remember(restrictions) {
        restrictions.filter { it.isRestricted }.map { it.packageName }.toSet()
    }

    var installedApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var lastSummaryTasks by remember { mutableStateOf<List<com.focusbyrj.app.data.Task>>(emptyList()) }
    
    LaunchedEffect(Unit) {
        BubbleChatManager.clearUnread(context)
        
        // Active inactivity check loop while chat window is open
        while (true) {
            delay(15_000) // check every 15 seconds
            if (BubbleChatManager.isInactiveTimeout(context) && messages.size > 1) {
                BubbleChatManager.clearMessages(context)
                val welcome = PersistedChatMessage(
                    id = "welcome_${System.currentTimeMillis()}",
                    text = com.focusbyrj.app.util.AyvaDialogueEngine.getHelloWelcomeMessage(context),
                    isUser = false,
                    timestamp = System.currentTimeMillis()
                )
                BubbleChatManager.saveMessages(context, listOf(welcome))
                messages = listOf(
                    ChatMessage(welcome.id, welcome.text, welcome.isUser, welcome.timestamp)
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        
        val startDrillFromNotification = (context as? android.app.Activity)?.intent?.getBooleanExtra("EXTRA_START_DRILL", false) == true
        if (startDrillFromNotification && activeDrillSession == null) {
            (context as? android.app.Activity)?.intent?.removeExtra("EXTRA_START_DRILL")
            val diff = com.focusbyrj.app.util.ArithmeticDifficulty.EASY
            val q = com.focusbyrj.app.util.ArithmeticEngine.generateQuestion(diff)
            val json = org.json.JSONObject().apply {
                put("title", q.title)
                put("questionText", q.questionText)
                val arr = org.json.JSONArray()
                q.options.forEach { arr.put(it) }
                put("options", arr)
                put("correctIndex", q.correctIndex)
                put("explanation", q.explanation)
            }.toString()
            
            val response = ChatMessage(
                id = java.util.UUID.randomUUID().toString(),
                text = "Arithmetic Drill",
                isUser = false,
                isArithmetic = true,
                arithmeticJson = json
            )
            activeDrillSession = DrillSession("easy", 10)
            messages = messages + response
        }

        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val apps = packages.filter { (it.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0 && it.packageName != context.packageName }.mapNotNull { 
                val name = pm.getApplicationLabel(it).toString()
                if (name.isNotBlank()) {
                    val category = getCategoryForApp(it, it.packageName)
                    AppInfo(name, it.packageName, category)
                } else null
            }.sortedBy { it.name }
            installedApps = apps
        }
    }
    
    val suggestions = remember(inputText, installedApps, lockedPackages, pendingTasksList, lastSummaryTasks) {
        if (!inputText.startsWith("/")) return@remember emptyList<Suggestion>()
        val parts = inputText.split(" ")
        val cmd = parts[0].lowercase()
        
        when {
            parts.size == 1 -> {
                val available = listOf("/talk", "/tasks", "/tasks all", "/summary", "/summary all", "/drill", "/profile", "/priority", "/postpone all", "/reschedule", "/clear", "/help")
                available.filter { it.startsWith(cmd) }.map { Suggestion(it, "$it ") }
            }
            (cmd == "/summary" || cmd == "/tasks" || cmd == "/task") && parts.size == 2 -> {
                val typed = parts[1].lowercase()
                val modes = listOf("all", "today")
                modes.filter { it.startsWith(typed) }.map { Suggestion("$cmd $it", "$cmd $it ") }
            }
            cmd == "/drill" && parts.size == 2 -> {
                val typed = parts[1].lowercase()
                val modes = listOf("easy", "medium", "hard")
                modes.filter { it.startsWith(typed) }.map { Suggestion(it, "/drill $it ") }
            }
            cmd == "/drill" && parts.size == 3 -> {
                val typed = parts[2].lowercase()
                val limits = listOf("10", "20", "unlimited")
                limits.filter { it.startsWith(typed) }.map { Suggestion(it, "/drill ${parts[1]} $it") }
            }
            cmd == "/reschedule" && parts.size == 2 -> {
                val typed = parts[1].lowercase()
                val targetTasks = if (lastSummaryTasks.isNotEmpty()) lastSummaryTasks else pendingTasksList
                targetTasks.mapIndexed { index, task -> 
                    val num = (index + 1).toString()
                    val dueStr = if (task.dueDate != null) " (${SmartDateParser.formatDueDate(task.dueDate)})" else ""
                    val display = "$num. ${task.title}$dueStr"
                    Suggestion(display, "/reschedule $num ")
                }.filter { 
                    typed.isEmpty() || it.displayText.startsWith(typed) || it.displayText.lowercase().contains(typed) || it.replacementText.contains(" $typed") 
                }
            }
            cmd == "/reschedule" && parts.size >= 3 -> {
                val num = parts[1]
                val timeTyped = parts.drop(2).joinToString(" ").lowercase()
                val timeOptions = listOf("today 5pm", "tomorrow 9am", "tomorrow 3pm", "tomorrow 6pm", "in 2 hours", "next monday 10am")
                timeOptions.filter { it.contains(timeTyped) }.map {
                    Suggestion(it, "/reschedule $num $it")
                }
            }

            else -> emptyList()
        }
    }


    val parsedResult = remember(inputText) {
        if (inputText.isNotBlank()) SmartDateParser.parse(inputText) else null
    }

    fun sendMessage(overrideText: String? = null) {
        val textToSend = (overrideText ?: inputText).trim()
        if (textToSend.isNotBlank()) {
            val userMsg = ChatMessage(System.currentTimeMillis().toString(), textToSend, true)
            messages = messages + userMsg
            var sentText = textToSend
            val finalTitle = parsedResult?.cleanText?.takeIf { it.isNotBlank() && overrideText == null } ?: sentText
            val dueDate = parsedResult?.timestamp ?: System.currentTimeMillis()
            
            val wasPriority = isHighPriority
            val wasPersistent = isPersistent
            if (overrideText == null) {
                inputTextFieldValue = TextFieldValue("")
                isHighPriority = false
                isPersistent = false
            }
            
            coroutineScope.launch(Dispatchers.IO) {
                val app = context.applicationContext as com.focusbyrj.app.FocusApplication
                val repo = app.taskRepository
                val db = app.database
                
                if (sentText.lowercase().startsWith("/talk ") || sentText.lowercase().startsWith("/ask ") || sentText.lowercase().startsWith("/guide ")) {
                    val rawQuery = sentText.substringAfter(" ").trim()
                    val nluResult = com.focusbyrj.app.util.OfflineNluEngine.parse(rawQuery, pendingTasksList)
                    when (nluResult.intent) {
                        com.focusbyrj.app.util.NluIntent.LIST_TASKS -> {
                            sentText = if (nluResult.isAllTasks) "/tasks all" else "/tasks"
                        }
                        com.focusbyrj.app.util.NluIntent.SHOW_PROFILE -> sentText = "/profile"
                        com.focusbyrj.app.util.NluIntent.SHOW_SUMMARY -> sentText = "/summary"
                        com.focusbyrj.app.util.NluIntent.START_DRILL -> sentText = "/drill"
                        com.focusbyrj.app.util.NluIntent.CLEAR_CHAT -> sentText = "/clear"
                        else -> {}
                    }
                }
                
                if (!sentText.startsWith("/")) {
                    val nluResult = com.focusbyrj.app.util.OfflineNluEngine.parse(sentText, pendingTasksList)
                    when (nluResult.intent) {
                        com.focusbyrj.app.util.NluIntent.LIST_TASKS -> {
                            sentText = if (nluResult.isAllTasks) "/tasks all" else "/tasks"
                        }
                        com.focusbyrj.app.util.NluIntent.SHOW_PROFILE -> sentText = "/profile"
                        com.focusbyrj.app.util.NluIntent.SHOW_SUMMARY -> sentText = "/summary"
                        com.focusbyrj.app.util.NluIntent.START_DRILL -> sentText = "/drill"
                        com.focusbyrj.app.util.NluIntent.CLEAR_CHAT -> sentText = "/clear"
                        com.focusbyrj.app.util.NluIntent.UNKNOWN -> {}
                        else -> sentText = "/talk $sentText"
                    }
                }

                if (sentText.startsWith("/")) {
                    val parts = sentText.split(" ")
                    val cmd = parts[0].lowercase()
                    var replyMsg = "Command not recognized."
                    
                    when (cmd) {
                        "/profile" -> {
                            val profMsg = ChatMessage(
                                id = java.util.UUID.randomUUID().toString(),
                                text = "Aptitude Profile",
                                isUser = false,
                                isAptitudeProfile = true
                            )
                            withContext(Dispatchers.Main) {
                                messages = messages + profMsg
                            }
                            return@launch
                        }
                        "/drill" -> {
                            val difficultyStr = parts.getOrNull(1)?.lowercase() ?: "easy"
                            val limitStr = parts.getOrNull(2)?.lowercase() ?: "unlimited"
                            val targetQ = when(limitStr) {
                                "10" -> 10
                                "20" -> 20
                                else -> -1
                            }
                            val diff = when (difficultyStr) {
                                "medium" -> com.focusbyrj.app.util.ArithmeticDifficulty.MEDIUM
                                "hard" -> com.focusbyrj.app.util.ArithmeticDifficulty.HARD
                                else -> com.focusbyrj.app.util.ArithmeticDifficulty.EASY
                            }
                            val q = com.focusbyrj.app.util.ArithmeticEngine.generateQuestion(diff)
                            val json = org.json.JSONObject().apply {
                                put("title", q.title)
                                put("questionText", q.questionText)
                                val arr = org.json.JSONArray()
                                q.options.forEach { arr.put(it) }
                                put("options", arr)
                                put("correctIndex", q.correctIndex)
                                put("explanation", q.explanation)
                            }.toString()
                            
                            val response = ChatMessage(
                                id = java.util.UUID.randomUUID().toString(),
                                text = "Arithmetic Drill",
                                isUser = false,
                                isArithmetic = true,
                                arithmeticJson = json
                            )
                            withContext(Dispatchers.Main) {
                                activeDrillSession = DrillSession(difficultyStr, targetQ)
                                messages = messages + response
                            }
                            return@launch
                        }
                        "/talk", "/guide", "/ask", "/how", "/help", "/faq" -> {
                            val query = if (parts.size > 1) parts.drop(1).joinToString(" ") else ""
                            val talkResp = com.focusbyrj.app.util.AyvaTalkEngine.answerTalkQueryWithActions(query, context)
                            val talkMsg = ChatMessage(
                                id = "talk_${System.currentTimeMillis()}",
                                text = talkResp.formattedText,
                                isUser = false,
                                isTalkAction = talkResp.actions.isNotEmpty() || (talkResp.jsonPayload?.contains("\"status\":\"pending\"") == false && talkResp.jsonPayload != null),
                                talkActionJson = if (talkResp.jsonPayload?.contains("\"status\":\"pending\"") == false) talkResp.jsonPayload else null,
                                pendingActionJson = if (talkResp.jsonPayload?.contains("\"status\":\"pending\"") == true) talkResp.jsonPayload else null
                            )
                            withContext(Dispatchers.Main) {
                                messages = messages + talkMsg
                            }
                            return@launch
                        }
                        "/clear" -> {
                            withContext(Dispatchers.Main) {
                                val welcome = ChatMessage(
                                    id = "welcome_${System.currentTimeMillis()}",
                                    text = com.focusbyrj.app.util.AyvaDialogueEngine.getHelloWelcomeMessage(context),
                                    isUser = false,
                                    timestamp = System.currentTimeMillis()
                                )
                                messages = listOf(welcome)
                                BubbleChatManager.saveMessages(context, listOf(
                                    PersistedChatMessage(welcome.id, welcome.text, welcome.isUser, welcome.timestamp)
                                ))
                                BubbleChatManager.clearUnread(context)
                            }
                            return@launch
                        }
                        "/summary", "/tasks", "/task" -> {
                            val isSummaryCommand = parts[0].lowercase() == "/summary"
                            val isAll = parts.getOrNull(1)?.lowercase() == "all" && parts.size == 2
                            
                            // INTELLIGENCE UPGRADE: If user typed extra words after the command, route to Talk Engine
                            if (parts.size > 2 || (parts.size == 2 && !isAll)) {
                                val query = sentText.removePrefix(parts[0]).trim()
                                val talkResp = com.focusbyrj.app.util.AyvaTalkEngine.answerTalkQueryWithActions(query, context)
                                val talkMsg = ChatMessage(
                                    id = "talk_${System.currentTimeMillis()}",
                                    text = talkResp.formattedText,
                                    isUser = false,
                                    isTalkAction = talkResp.actions.isNotEmpty() || (talkResp.jsonPayload?.contains("\"status\":\"pending\"") == false && talkResp.jsonPayload != null),
                                talkActionJson = if (talkResp.jsonPayload?.contains("\"status\":\"pending\"") == false) talkResp.jsonPayload else null,
                                pendingActionJson = if (talkResp.jsonPayload?.contains("\"status\":\"pending\"") == true) talkResp.jsonPayload else null
                                )
                                withContext(Dispatchers.Main) {
                                    messages = messages + talkMsg
                                }
                                return@launch
                            }
                            
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
                            
                            val taskJsonArray = org.json.JSONArray()
                            sortedTasks.forEach { t ->
                                val obj = org.json.JSONObject().apply {
                                    put("id", t.id)
                                    put("title", t.title)
                                    put("isPriority", t.isPriority)
                                    put("dueDate", t.dueDate ?: 0L)
                                    put("isCompleted", t.isCompleted)
                                    put("isPersistent", t.isPersistent)
                                    put("filterMode", if (isAll) "all" else "today")
                                }
                                taskJsonArray.put(obj)
                            }
                            
                            val builder = StringBuilder()
                            val cal = java.util.Calendar.getInstance()
                            val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
                            val dayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK)
                            val greeting = com.focusbyrj.app.util.AyvaDialogueEngine.getSummaryGreeting(context, isAll, hour, dayOfWeek)
                            builder.append(greeting).append("\n\n")
                            
                            if (completedToday.isNotEmpty() && !isAll) {
                                builder.append("✅ *__Crushed Today__* *(${completedToday.size})*:\n")
                                completedToday.forEach { task ->
                                    builder.append("• _${task.title}_\n")
                                }
                                builder.append("\n")
                            }
                            
                            if (sortedTasks.isEmpty()) {
                                builder.append(com.focusbyrj.app.util.AyvaDialogueEngine.getEmptyDayMessage(context)).append("\n")
                                val nextTask = pendingTasks
                                    .filter { it.dueDate != null && it.dueDate > endOfDay }
                                    .minByOrNull { it.dueDate!! }
                                if (nextTask != null) {
                                    builder.append("\n🗓️ *__Up Next On The Horizon__*: *${nextTask.title}* _(${SmartDateParser.formatDueDate(nextTask.dueDate)})_\n")
                                }
                            } else {
                                val headerTitle = if (isAll) "Pending Tasks" else "On Today's Hit List"
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
                            
                            val quote = if (hour < 15) {
                                com.focusbyrj.app.util.SummaryQuotes.getNextMorningQuote(context)
                            } else {
                                com.focusbyrj.app.util.SummaryQuotes.getNextEveningQuote(context)
                            }
                            builder.append("\n\n💡 _\"$quote\"_")
                            
                            if (sortedTasks.isNotEmpty()) {
                                builder.append("\n\n").append(com.focusbyrj.app.util.AyvaDialogueEngine.getReschedulePrompt(context))
                            }
                            
                            val summaryResponse = ChatMessage(
                                id = "summary_${System.currentTimeMillis()}",
                                text = builder.toString().trimEnd(),
                                isUser = false,
                                isTaskSummary = !isSummaryCommand,
                                taskSummaryJson = if (isSummaryCommand) null else taskJsonArray.toString()
                            )
                            
                            withContext(Dispatchers.Main) {
                                lastSummaryTasks = sortedTasks
                                messages = messages + summaryResponse
                            }
                            return@launch
                        }
                        "/reschedule" -> {
                            val numStr = parts.getOrNull(1)
                            val timeStr = parts.drop(2).joinToString(" ")
                            
                            val allPending = repo.allTasks.first().filter { !it.isCompleted }
                                .sortedWith(compareByDescending<com.focusbyrj.app.data.Task> { it.isPriority }.thenBy { it.dueDate ?: Long.MAX_VALUE })
                            val targetList = if (lastSummaryTasks.isNotEmpty()) lastSummaryTasks else allPending

                            if (numStr == null || timeStr.isEmpty()) {
                                if (targetList.isEmpty()) {
                                    replyMsg = "No active tasks found to reschedule! 🎯"
                                } else {
                                    val builder = StringBuilder()
                                    builder.append("📋 *__Pending Tasks for Rescheduling__*:\n")
                                    targetList.forEachIndexed { index, task ->
                                        val prefix = if (task.isPriority) "🔥 " else ""
                                        val dueStr = if (task.dueDate != null) " _(Due: ${SmartDateParser.formatDueDate(task.dueDate)})_" else ""
                                        builder.append("${index + 1}. $prefix${task.title}$dueStr\n")
                                    }
                                    builder.append("\n_Type `/reschedule <number> <time>` (e.g. `/reschedule 1 tomorrow at 4pm`)_")
                                    replyMsg = builder.toString().trimEnd()
                                }
                            } else {
                                val num = numStr.toIntOrNull()
                                if (num == null || num < 1 || num > targetList.size) {
                                    replyMsg = "Hmm, couldn't match task #$numStr. There are ${targetList.size} pending tasks."
                                } else {
                                    val task = targetList[num - 1]
                                    val parsed = SmartDateParser.parse("reschedule to $timeStr")
                                    if (parsed.timestamp != null) {
                                        val updatedTask = task.copy(dueDate = parsed.timestamp)
                                        repo.updateTask(updatedTask)
                                        TaskReminderHelper.scheduleReminder(context, updatedTask)
                                        TodoWidgetProvider.updateAllWidgets(context)
                                        replyMsg = com.focusbyrj.app.util.AyvaDialogueEngine.getRescheduleSuccessResponse(context, task.title, SmartDateParser.formatDueDate(parsed.timestamp))
                                    } else {
                                        replyMsg = "Couldn't decipher '$timeStr'. Try something like 'tomorrow at 3pm' or '5pm'."
                                    }
                                }
                            }
                        }
                        "/priority" -> {
                            val tasks = repo.allTasks.first()
                            val priority = tasks.filter { it.isPriority && !it.isCompleted }
                            replyMsg = if (priority.isEmpty()) com.focusbyrj.app.util.AyvaDialogueEngine.getPriorityEmptyResponse(context)
                                else "🔥 *__Ayva's Priority Radar__*:\n" + priority.joinToString("\n") { "• ${it.title}" }
                        }
                        "/postpone" -> {
                            if (parts.getOrNull(1)?.lowercase() == "all") {
                                val tasks = repo.allTasks.first().filter { !it.isCompleted }
                                tasks.forEach { 
                                    val updatedTask = it.copy(dueDate = System.currentTimeMillis() + 86400000L)
                                    repo.updateTask(updatedTask)
                                    TaskReminderHelper.scheduleReminder(context, updatedTask)
                                }
                                TodoWidgetProvider.updateAllWidgets(context)
                                replyMsg = com.focusbyrj.app.util.AyvaDialogueEngine.getPostponeAllResponse(context, tasks.size)
                            }
                        }
                    }
                    val isSummaryCmd = parts.firstOrNull()?.equals("/summary", ignoreCase = true) == true
                    withContext(Dispatchers.Main) {
                        messages = messages + ChatMessage(
                            System.currentTimeMillis().toString() + "bot",
                            replyMsg,
                            false,
                            isTaskSummary = isSummaryCmd && lastSummaryTasks.isNotEmpty()
                        )
                    }
                    return@launch
                }
                
                // INTELLIGENCE UPGRADE: Natural language intent interception
                val lowerSent = sentText.lowercase()
                val isLikelyTalkIntent = lowerSent.startsWith("set ") || lowerSent.startsWith("change ") ||
                                         lowerSent.startsWith("why ") || lowerSent.startsWith("how ") || 
                                         lowerSent.startsWith("what is ") || lowerSent.startsWith("disable ") ||
                                         lowerSent.startsWith("enable ") || lowerSent.startsWith("turn on ") || lowerSent.startsWith("turn off ")
                
                if (isLikelyTalkIntent) {
                    val talkResp = com.focusbyrj.app.util.AyvaTalkEngine.answerTalkQueryWithActions(sentText, context)
                    if (!talkResp.formattedText.contains("🤔 I couldn't find") && !talkResp.formattedText.contains("Ask me any question")) {
                        val talkMsg = ChatMessage(
                            id = "talk_${System.currentTimeMillis()}",
                            text = talkResp.formattedText,
                            isUser = false,
                            isTalkAction = talkResp.actions.isNotEmpty() || (talkResp.jsonPayload?.contains("\"status\":\"pending\"") == false && talkResp.jsonPayload != null),
                                talkActionJson = if (talkResp.jsonPayload?.contains("\"status\":\"pending\"") == false) talkResp.jsonPayload else null,
                                pendingActionJson = if (talkResp.jsonPayload?.contains("\"status\":\"pending\"") == true) talkResp.jsonPayload else null
                        )
                        withContext(Dispatchers.Main) {
                            messages = messages + talkMsg
                        }
                        return@launch
                    }
                }
                
                val newTask = Task(
                    title = finalTitle,
                    isPriority = wasPriority,
                    isPersistent = wasPersistent,
                    dueDate = dueDate
                )
                val newId = repo.insertTask(newTask)
                TaskReminderHelper.scheduleReminder(context, newTask.copy(id = newId))
                TodoWidgetProvider.updateAllWidgets(context)
                
                withContext(Dispatchers.Main) {
                    val attrs = mutableListOf<String>()
                    if (wasPriority) attrs.add("priority")
                    if (wasPersistent) attrs.add("persistent")
                    
                    val attrStr = if (attrs.isNotEmpty()) attrs.joinToString(" and ") else null
                    val dueStr = if (parsedResult?.timestamp != null) SmartDateParser.formatDueDate(dueDate) else null
                    
                    val confirmationText = com.focusbyrj.app.util.AyvaDialogueEngine.getTaskAddedResponse(
                        context = context,
                        title = finalTitle,
                        isPriority = wasPriority,
                        hasDueDate = parsedResult?.timestamp != null,
                        dueDateStr = dueStr,
                        attrStr = attrStr
                    )
                    
                    messages = messages + ChatMessage(
                        System.currentTimeMillis().toString() + "bot", 
                        confirmationText, 
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
                        text = { Text("Text Size") },
                        leadingIcon = {
                            Icon(Icons.Filled.FormatSize, contentDescription = null, modifier = Modifier.size(20.dp))
                        },
                        onClick = {
                            showMenu = false
                            showFontSizeDialog = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Clear History") },
                        onClick = {
                            val welcome = ChatMessage(
                                id = "welcome_${System.currentTimeMillis()}",
                                text = com.focusbyrj.app.util.AyvaDialogueEngine.getHelloWelcomeMessage(context),
                                isUser = false,
                                timestamp = System.currentTimeMillis()
                            )
                            messages = listOf(welcome)
                            BubbleChatManager.saveMessages(context, listOf(
                                PersistedChatMessage(welcome.id, welcome.text, welcome.isUser, welcome.timestamp)
                            ))
                            BubbleChatManager.clearUnread(context)
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
            val emptyChatText = remember(messages.isEmpty()) {
                com.focusbyrj.app.util.AyvaDialogueEngine.getClearChatIntro(context)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = parseRichFormattedText(emptyChatText),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = chatFontSizeSp.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = (chatFontSizeSp * 1.45f).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                reverseLayout = true
            ) {
                val reversedMessages = messages.reversed()
                items(
                    count = reversedMessages.size,
                    key = { index -> reversedMessages[index].id }
                ) { index ->
                    val msg = reversedMessages[index]
                    val isLatest = index == 0
                    val isActiveDrill = isLatest && activeDrillSession != null && msg.isArithmetic
                    
                    ChatBubble(
                        message = msg, 
                        fontSizeSp = chatFontSizeSp,
                        isActiveDrill = isActiveDrill,
                        isActiveDrillRunning = activeDrillSession != null,
                        onMessageUpdate = { updatedMsg ->
                            val idx = messages.indexOfFirst { it.id == updatedMsg.id }
                            if (idx != -1) {
                                val newList = messages.toMutableList()
                                newList[idx] = updatedMsg
                                messages = newList
                            }
                        },
                        onStartStreakDrill = {
                            if (activeDrillSession == null) {
                                val aptProfile = com.focusbyrj.app.util.AptitudeManager.profileFlow.value
                                val diffStr = when {
                                    aptProfile.titleTier >= 5 -> "hard"
                                    aptProfile.titleTier >= 3 -> "medium"
                                    else -> "easy"
                                }
                                val diffEnum = when (diffStr) {
                                    "hard" -> com.focusbyrj.app.util.ArithmeticDifficulty.HARD
                                    "medium" -> com.focusbyrj.app.util.ArithmeticDifficulty.MEDIUM
                                    else -> com.focusbyrj.app.util.ArithmeticDifficulty.EASY
                                }
                                val q = com.focusbyrj.app.util.ArithmeticEngine.generateQuestion(diffEnum)
                                val json = org.json.JSONObject().apply {
                                    put("title", q.title)
                                    put("questionText", q.questionText)
                                    val arr = org.json.JSONArray()
                                    q.options.forEach { arr.put(it) }
                                    put("options", arr)
                                    put("correctIndex", q.correctIndex)
                                    put("explanation", q.explanation)
                                }.toString()
                                
                                val nextMsg = ChatMessage(
                                    id = java.util.UUID.randomUUID().toString(),
                                    text = "Arithmetic Drill",
                                    isUser = false,
                                    isArithmetic = true,
                                    arithmeticJson = json
                                )
                                activeDrillSession = DrillSession(diffStr, 10)
                                messages = messages + nextMsg
                            }
                        },
                        onDrillAnswer = { isCorrect ->
                            activeDrillSession?.let { session ->
                                session.total++
                                if (isCorrect) {
                                    session.correct++
                                    session.xp += 40
                                    session.gold += 20
                                }
                                coroutineScope.launch {
                                    delay(400) // Reduced from 1500 for immediate next question
                                    // Check if session hasn't been ended during delay
                                    if (activeDrillSession != null) {
                                        if (session.targetQuestions != -1 && session.total >= session.targetQuestions) {
                                            val summaryMsg = com.focusbyrj.app.util.DrillSummaryHelper.generateSummaryMessage(session)
                                            messages = messages + summaryMsg
                                            activeDrillSession = null
                                        } else {
                                            val diffEnum = when (session.difficulty) {
                                                "medium" -> com.focusbyrj.app.util.ArithmeticDifficulty.MEDIUM
                                                "hard" -> com.focusbyrj.app.util.ArithmeticDifficulty.HARD
                                                else -> com.focusbyrj.app.util.ArithmeticDifficulty.EASY
                                            }
                                            val nextQ = com.focusbyrj.app.util.ArithmeticEngine.generateQuestion(diffEnum)
                                            val json = org.json.JSONObject().apply {
                                                put("title", nextQ.title)
                                                put("questionText", nextQ.questionText)
                                                val arr = org.json.JSONArray()
                                                nextQ.options.forEach { arr.put(it) }
                                                put("options", arr)
                                                put("correctIndex", nextQ.correctIndex)
                                                put("explanation", nextQ.explanation)
                                            }.toString()
                                            
                                            val nextMsg = ChatMessage(
                                                id = java.util.UUID.randomUUID().toString(),
                                                text = "Arithmetic Drill",
                                                isUser = false,
                                                isArithmetic = true,
                                                arithmeticJson = json
                                            )
                                            messages = messages + nextMsg
                                        }
                                    }
                                }
                            }
                        },
                        onDrillEnd = {
                            activeDrillSession?.let { session ->
                                val summaryMsg = com.focusbyrj.app.util.DrillSummaryHelper.generateSummaryMessage(session)
                                messages = messages + summaryMsg
                                activeDrillSession = null
                            }
                        },
                        onRescheduleClick = {
                            val rep = "/reschedule "
                            inputTextFieldValue = TextFieldValue(
                                text = rep,
                                selection = TextRange(rep.length)
                            )
                        },
                        onTaskToggle = { taskId ->
                            coroutineScope.launch(Dispatchers.IO) {
                                val app = context.applicationContext as com.focusbyrj.app.FocusApplication
                                val repo = app.taskRepository
                                val task = repo.getTaskById(taskId)
                                if (task != null) {
                                    val newCompleted = !task.isCompleted
                                    val updatedTask = task.copy(
                                        isCompleted = newCompleted,
                                        completedAt = if (newCompleted) System.currentTimeMillis() else null
                                    )
                                    repo.updateTask(updatedTask)
                                    if (newCompleted) {
                                        TaskReminderHelper.cancelReminderById(context, taskId)
                                    } else if (updatedTask.dueDate != null) {
                                        TaskReminderHelper.scheduleReminder(context, updatedTask)
                                    }
                                    TodoWidgetProvider.updateAllWidgets(context)
                                    
                                    withContext(Dispatchers.Main) {
                                        messages = messages.map { m ->
                                            if (m.taskSummaryJson != null) {
                                                try {
                                                    val arr = org.json.JSONArray(m.taskSummaryJson)
                                                    val newArr = org.json.JSONArray()
                                                    for (i in 0 until arr.length()) {
                                                        val item = arr.getJSONObject(i)
                                                        if (item.optLong("id") == taskId) {
                                                            item.put("isCompleted", newCompleted)
                                                        }
                                                        newArr.put(item)
                                                    }
                                                    m.copy(taskSummaryJson = newArr.toString())
                                                } catch (e: Exception) { m }
                                            } else m
                                        }
                                        if (newCompleted) {
                                            val ackMsg = ChatMessage(
                                                id = "done_${System.currentTimeMillis()}",
                                                text = "Checked off: *${task.title}* 🎉",
                                                isUser = false
                                            )
                                            messages = messages + ackMsg
                                        }
                                    }
                                }
                            }
                        },
                        onFilterChange = { cmd ->
                            sendMessage(cmd)
                        }
                    )
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
            // Horizontal scrollable quick action commands floating above icons
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(quickActionCommands) { action ->
                    androidx.compose.material3.Surface(
                        onClick = { onFillCommand(action.commandText) },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.22f))
                    ) {
                        Text(
                            text = action.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.5.sp,
                                letterSpacing = 0.2.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                        )
                    }
                }
            }

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
                                .clickable {
                                    val rep = suggestion.replacementText
                                    inputTextFieldValue = TextFieldValue(
                                        text = rep,
                                        selection = TextRange(rep.length)
                                    )
                                }
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
                    // Ayva Talk Quick Action
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha=0.5f), CircleShape)
                            .clickable {
                                inputTextFieldValue = TextFieldValue(
                                    text = "/talk ",
                                    selection = TextRange(6)
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = com.focusbyrj.app.ui.components.AyvaIcon,
                            contentDescription = "Talk to Ayva",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    // Drill Quick Action
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha=0.5f), CircleShape)
                            .clickable {
                                inputTextFieldValue = TextFieldValue(
                                    text = "/drill ",
                                    selection = TextRange(7)
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⚡", fontSize = 16.sp)
                    }
                    
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
                    value = inputTextFieldValue,
                    onValueChange = { inputTextFieldValue = it },
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
                                    val cur = inputTextFieldValue.text
                                    if (!cur.startsWith("/")) {
                                        val newText = "/$cur"
                                        inputTextFieldValue = TextFieldValue(
                                            text = newText,
                                            selection = TextRange(newText.length)
                                        )
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

        if (showFontSizeDialog) {
            ChatTextSizeDialog(
                fontSizeSp = chatFontSizeSp,
                onFontSizeChange = { updateFontSize(it) },
                onDismiss = { showFontSizeDialog = false }
            )
        }
    }
}

@Composable
fun ChatTextSizeDialog(
    fontSizeSp: Float,
    onFontSizeChange: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.FormatSize,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Chat Text Size", style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Adjust text size for the assistant chat window only.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Live Preview Bubble
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Preview",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "🌅 Good Morning!\nHere is your daily update:\n🎉 All clear for today!",
                            fontSize = fontSizeSp.sp,
                            lineHeight = (fontSizeSp * 1.45f).sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Stepper Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    FilledTonalIconButton(
                        onClick = { onFontSizeChange(fontSizeSp - 1f) },
                        enabled = fontSizeSp > 12f,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Filled.Remove, contentDescription = "Decrease size")
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${fontSizeSp.toInt()} sp",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = when {
                                fontSizeSp <= 13f -> "Small"
                                fontSizeSp <= 15f -> "Default"
                                fontSizeSp <= 18f -> "Medium"
                                fontSizeSp <= 21f -> "Large"
                                else -> "Extra Large"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    FilledTonalIconButton(
                        onClick = { onFontSizeChange(fontSizeSp + 1f) },
                        enabled = fontSizeSp < 24f,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Increase size")
                    }
                }

                // Classic professional slider
                ProfessionalSlider(
                    value = fontSizeSp,
                    onValueChange = { onFontSizeChange(kotlin.math.round(it)) },
                    valueRange = 12f..24f,
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick preset buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        "Small" to 13f,
                        "Default" to 15f,
                        "Large" to 18f,
                        "Huge" to 21f
                    ).forEach { (label, size) ->
                        val isSelected = (fontSizeSp == size)
                        FilterChip(
                            selected = isSelected,
                            onClick = { onFontSizeChange(size) },
                            label = { Text(label, fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        },
        dismissButton = {
            TextButton(onClick = { onFontSizeChange(15f) }) {
                Text("Reset")
            }
        }
    )
}

@Composable
fun ChatBubble(
    message: ChatMessage, 
    fontSizeSp: Float = 15f,
    isActiveDrill: Boolean = false,
    isActiveDrillRunning: Boolean = false,
    onDrillAnswer: ((Boolean) -> Unit)? = null,
    onDrillEnd: (() -> Unit)? = null,
    onStartStreakDrill: (() -> Unit)? = null,
    onRescheduleClick: (() -> Unit)? = null,
    onTaskToggle: ((Long) -> Unit)? = null,
    onFilterChange: ((String) -> Unit)? = null,
    onNavigateSummary: (() -> Unit)? = null,
    onMessageUpdate: (ChatMessage) -> Unit = {}
) {
    if (message.isStreakPrompt) {
        StreakPromptCard(
            message = message,
            fontSizeSp = fontSizeSp,
            isActiveDrillRunning = isActiveDrillRunning,
            onStartDrill = { onStartStreakDrill?.invoke() }
        )
        return
    }
    if (message.isDrillSummary) {
        DrillSummaryCard(message = message, fontSizeSp = fontSizeSp)
        return
    }
    if (message.isAptitudeProfile) {
        com.focusbyrj.app.ui.screens.AptitudeProfileCard()
        return
    }
    if (message.isTaskSummary && !message.isUser) {
        TaskSummaryCard(
            message = message,
            fontSizeSp = fontSizeSp,
            onTaskToggle = onTaskToggle,
            onRescheduleClick = onRescheduleClick,
            onFilterChange = onFilterChange
        )
        return
    }

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
                        contentDescription = "Ayva",
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            val maxBubbleWidth = (280 + (fontSizeSp - 15f) * 10f).coerceIn(280f, 350f).dp
            androidx.compose.material3.Surface(
                modifier = Modifier.widthIn(max = maxBubbleWidth),
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
                if (message.isArithmetic && message.arithmeticJson != null) {
                    ArithmeticCard(
                        message = message, 
                        fontSizeSp = fontSizeSp,
                        isActiveDrill = isActiveDrill,
                        onAnswered = onDrillAnswer,
                        onEndDrill = onDrillEnd
                    )
                } else {
                    Column(
                        modifier = Modifier.padding(
                            horizontal = (14 + (fontSizeSp - 15f) * 0.5f).coerceIn(12f, 20f).dp,
                            vertical = (10 + (fontSizeSp - 15f) * 0.5f).coerceIn(8f, 16f).dp
                        )
                    ) {
                        Text(
                            text = parseRichFormattedText(message.text),
                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(
                                fontSize = fontSizeSp.sp,
                                lineHeight = (fontSizeSp * 1.45f).sp,
                                letterSpacing = 0.2.sp
                            )
                        )

                        if (message.isTalkAction && !message.talkActionJson.isNullOrBlank()) {
                            com.focusbyrj.app.ui.components.TalkActionChips(
                                talkActionJson = message.talkActionJson,
                                fontSizeSp = fontSizeSp
                            )
                        }
                        
                        if (!message.pendingActionJson.isNullOrBlank()) {
                            PendingActionCard(
                                message = message,
                                fontSizeSp = fontSizeSp,
                                onMessageUpdate = onMessageUpdate
                            )
                        }
                    }
                }
            }
        }
        Text(
            text = timeString,
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(
                fontSize = (fontSizeSp * 0.72f).coerceIn(10f, 14f).sp,
                fontWeight = FontWeight.Medium
            ),
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.padding(
                top = 4.dp, 
                start = if (message.isUser) 0.dp else 40.dp, 
                end = if (message.isUser) 4.dp else 0.dp
            )
        )
    }
}

data class TaskItemData(
    val id: Long,
    val title: String,
    val isPriority: Boolean,
    val dueDate: Long,
    val isCompleted: Boolean,
    val isPersistent: Boolean = false,
    val filterMode: String = "today"
)

@Composable
fun TaskSummaryCard(
    message: ChatMessage,
    fontSizeSp: Float = 15f,
    onTaskToggle: ((Long) -> Unit)? = null,
    onRescheduleClick: (() -> Unit)? = null,
    onFilterChange: ((String) -> Unit)? = null
) {
    val isDark = isSystemInDarkTheme()
    val df = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val timeString = df.format(Date(message.timestamp))
    val maxBubbleWidth = (300 + (fontSizeSp - 15f) * 12f).coerceIn(300f, 380f).dp

    var showFilterDropdown by remember { mutableStateOf(false) }

    val taskItems = remember(message.taskSummaryJson) {
        val list = mutableListOf<TaskItemData>()
        if (!message.taskSummaryJson.isNullOrBlank()) {
            try {
                val arr = org.json.JSONArray(message.taskSummaryJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        TaskItemData(
                            id = obj.optLong("id"),
                            title = obj.optString("title", ""),
                            isPriority = obj.optBoolean("isPriority", false),
                            dueDate = obj.optLong("dueDate", 0L),
                            isCompleted = obj.optBoolean("isCompleted", false),
                            isPersistent = obj.optBoolean("isPersistent", false),
                            filterMode = obj.optString("filterMode", "today")
                        )
                    )
                }
            } catch (_: Exception) {}
        }
        list
    }

    val isAllMode = taskItems.firstOrNull()?.filterMode == "all"

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

            Surface(
                modifier = Modifier.widthIn(max = maxBubbleWidth),
                shape = RoundedCornerShape(
                    topStart = 20.dp,
                    topEnd = 20.dp,
                    bottomStart = 4.dp,
                    bottomEnd = 20.dp
                ),
                color = if (isDark) Color(0xFF131316) else Color(0xFFF8FAFC),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = if (isDark) 0.1f else 0.25f)),
                shadowElevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Header Bar with Filter Dropdown
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isAllMode) "All Tasks" else "Today's Tasks",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = (fontSizeSp * 0.95f).sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (taskItems.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                ) {
                                    val leftCount = taskItems.count { !it.isCompleted }
                                    Text(
                                        text = "$leftCount left",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        // Dropdown Selector Button
                        Box {
                            Surface(
                                onClick = { showFilterDropdown = true },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isDark) Color(0xFF1C1C20) else MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isAllMode) "All" else "Today",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Icon(
                                        imageVector = Icons.Filled.ArrowDropDown,
                                        contentDescription = "Select task view",
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showFilterDropdown,
                                onDismissRequest = { showFilterDropdown = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Today's Tasks") },
                                    onClick = {
                                        showFilterDropdown = false
                                        onFilterChange?.invoke("/tasks")
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Filled.Today, contentDescription = null, modifier = Modifier.size(18.dp))
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("All Tasks") },
                                    onClick = {
                                        showFilterDropdown = false
                                        onFilterChange?.invoke("/tasks all")
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Filled.FormatListBulleted, contentDescription = null, modifier = Modifier.size(18.dp))
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Clean Task Items List or Empty State
                    if (taskItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isAllMode) "No tasks found" else "No tasks scheduled for today",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = (fontSizeSp * 0.95f).sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            )
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            taskItems.forEach { task ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isDark) Color(0xFF131B26) else Color.White,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (task.isPriority && !task.isCompleted) Color(0xFFEF4444).copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline.copy(alpha = if(isDark) 0.1f else 0.18f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onTaskToggle?.invoke(task.id) }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Clickable Circle Checkbox
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (task.isCompleted) MaterialTheme.colorScheme.primary else Color.Transparent
                                                )
                                                .border(
                                                    width = 2.dp,
                                                    color = when {
                                                        task.isCompleted -> MaterialTheme.colorScheme.primary
                                                        task.isPriority -> Color(0xFFEF4444).copy(alpha = 0.7f)
                                                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                                    },
                                                    shape = CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (task.isCompleted) {
                                                Icon(
                                                    imageVector = Icons.Filled.Check,
                                                    contentDescription = "Completed",
                                                    tint = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        // Task details
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = task.title,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontSize = (fontSizeSp * 0.95f).sp,
                                                        fontWeight = if (task.isPriority && !task.isCompleted) FontWeight.Bold else FontWeight.SemiBold,
                                                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                                    ),
                                                    color = if (task.isCompleted) {
                                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                                    } else if (task.isPriority) {
                                                        Color(0xFFEF4444)
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurface
                                                    }
                                                )
                                            }

                                            if (task.dueDate > 0L) {
                                                val now = System.currentTimeMillis()
                                                val isOverdue = task.dueDate < now && !task.isCompleted
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.AccessTime,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(11.dp),
                                                        tint = if (isOverdue) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = if (isOverdue) "Overdue: ${SmartDateParser.formatDueDate(task.dueDate)}" else SmartDateParser.formatDueDate(task.dueDate),
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontSize = 11.sp,
                                                            fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Medium
                                                        ),
                                                        color = if (isOverdue) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Bottom action row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            onClick = { onRescheduleClick?.invoke() },
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = if(isDark) 0.1f else 0.08f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = if(isDark) 0.3f else 0.25f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AccessTime,
                                    contentDescription = "Reschedule",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Reschedule",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = (fontSizeSp * 0.8f).sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Surface(
                            onClick = { onFilterChange?.invoke(if (isAllMode) "/tasks" else "/tasks all") },
                            shape = RoundedCornerShape(10.dp),
                            color = if(isDark) Color(0xFF1C1C20) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = if(isDark) 0.2f else 0.2f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (isAllMode) Icons.Filled.Today else Icons.Filled.FormatListBulleted,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isAllMode) "Show Today" else "Show All",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = (fontSizeSp * 0.8f).sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        Text(
            text = timeString,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = (fontSizeSp * 0.72f).coerceIn(10f, 14f).sp,
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.padding(top = 4.dp, start = 40.dp)
        )
    }
}

@Composable
fun StreakPromptCard(
    message: ChatMessage,
    fontSizeSp: Float = 15f,
    isActiveDrillRunning: Boolean = false,
    onStartDrill: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val json = remember(message.streakPromptJson) {
        try {
            if (message.streakPromptJson != null) org.json.JSONObject(message.streakPromptJson) else null
        } catch (_: Exception) {
            null
        }
    }

    val streak = json?.optInt("streak", 0) ?: 0
    val bonusPercent = json?.optInt("bonusPercent", 0) ?: 0

    val df = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val timeString = df.format(Date(message.timestamp))
    val maxBubbleWidth = (290 + (fontSizeSp - 15f) * 10f).coerceIn(290f, 360f).dp

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

            Surface(
                modifier = Modifier.widthIn(max = maxBubbleWidth),
                shape = RoundedCornerShape(
                    topStart = 20.dp,
                    topEnd = 20.dp,
                    bottomStart = 4.dp,
                    bottomEnd = 20.dp
                ),
                color = if (isDark) Color(0xFF1E140A) else Color(0xFFFFF7ED),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFF97316).copy(alpha = 0.7f)),
                shadowElevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Header Tag
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF97316).copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF97316).copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (streak > 0) "🔥 $streak-DAY STREAK" else "⚡ DAILY STREAK",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEA580C)
                                )
                            }
                        }

                        if (bonusPercent > 0) {
                            Text(
                                text = "+$bonusPercent% XP Bonus",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF16A34A)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = parseRichFormattedText(message.text),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = fontSizeSp.sp,
                            lineHeight = (fontSizeSp * 1.4f).sp,
                            letterSpacing = 0.2.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onStartDrill,
                        enabled = !isActiveDrillRunning,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEA580C),
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFFEA580C).copy(alpha = 0.4f),
                            disabledContentColor = Color.White.copy(alpha = 0.7f)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (isActiveDrillRunning) Icons.Filled.CheckCircle else Icons.Filled.Bolt,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isActiveDrillRunning) "Drill in Progress" else "Start 10-Question Drill",
                                fontWeight = FontWeight.Bold,
                                fontSize = (fontSizeSp * 0.95f).sp
                            )
                        }
                    }
                }
            }
        }

        Text(
            text = timeString,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = (fontSizeSp * 0.72f).coerceIn(10f, 14f).sp,
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.padding(top = 4.dp, start = 40.dp)
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
                builder.append(parts.drop(1).joinToString(" "))
            }
        } else {
            builder.append(input)
        }
        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}

@Composable
fun PendingActionCard(message: ChatMessage, fontSizeSp: Float, onMessageUpdate: (ChatMessage) -> Unit) {
    val context = LocalContext.current
    val json = remember(message.pendingActionJson) { org.json.JSONObject(message.pendingActionJson) }
    val status = json.optString("status", "pending")
    val title = json.optString("title", "")
    val displayVal = json.optString("displayVal", "")
    
    Spacer(modifier = Modifier.height(12.dp))
    
    if (status == "pending") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            androidx.compose.material3.Button(
                onClick = {
                    val prefKey = json.optString("prefKey")
                    val prefType = json.optString("prefType")
                    val value = json.optString("value")
                    
                    val prefs = context.getSharedPreferences("focus_prefs", Context.MODE_PRIVATE).edit()
                    val bubblePrefs = context.getSharedPreferences("bubble_prefs", Context.MODE_PRIVATE).edit()
                    
                    if (prefType == "int") {
                        prefs.putInt(prefKey, value.toIntOrNull() ?: 0)
                        bubblePrefs.putInt(prefKey, value.toIntOrNull() ?: 0)
                    } else if (prefType == "boolean") {
                        prefs.putBoolean(prefKey, value == "true")
                        bubblePrefs.putBoolean(prefKey, value == "true")
                    } else if (prefType == "string") {
                        prefs.putString(prefKey, value)
                        bubblePrefs.putString(prefKey, value)
                    }
                    prefs.apply()
                    bubblePrefs.apply()
                    
                    if (prefKey == "streak_notification_time" || prefKey == "streak_notification_enabled") {
                        com.focusbyrj.app.service.AptitudeReminderReceiver.scheduleDrillReminders(context)
                    } else if (prefKey == "morning_brief_time" || prefKey == "evening_brief_time") {
                        com.focusbyrj.app.service.DailySummaryReceiver.scheduleDailySummaries(context)
                    }
                    
                    json.put("status", "executed")
                    val updatedMessage = message.copy(pendingActionJson = json.toString())
                    
                    onMessageUpdate(updatedMessage)
                },
                modifier = Modifier.weight(1f),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF10B981)),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
            ) {
                Text("Yes, proceed", fontSize = (fontSizeSp * 0.9f).sp, fontWeight = FontWeight.SemiBold)
            }
            
            androidx.compose.material3.OutlinedButton(
                onClick = {
                    json.put("status", "cancelled")
                    val updatedMessage = message.copy(pendingActionJson = json.toString())
                    
                    onMessageUpdate(updatedMessage)
                },
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
            ) {
                Text("Cancel", fontSize = (fontSizeSp * 0.9f).sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    } else if (status == "executed") {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().background(androidx.compose.ui.graphics.Color(0xFF10B981).copy(alpha = 0.1f), RoundedCornerShape(8.dp)).padding(12.dp)) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = "Done", tint = androidx.compose.ui.graphics.Color(0xFF10B981), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Changed successfully.", color = androidx.compose.ui.graphics.Color(0xFF10B981), fontSize = (fontSizeSp * 0.9f).sp, fontWeight = FontWeight.Medium)
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp)).padding(12.dp)) {
            Icon(Icons.Rounded.Close, contentDescription = "Cancelled", tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Action cancelled.", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant, fontSize = (fontSizeSp * 0.9f).sp, fontWeight = FontWeight.Medium)
        }
    }
}
