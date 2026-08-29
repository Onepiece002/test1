package com.focusbyrj.app.util

import android.content.Context
import android.provider.Settings
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.first
import com.focusbyrj.app.data.Task
import com.focusbyrj.app.data.FocusSchedule

/**
 * AyvaTalkEngine: Atomic, context-aware, state-diagnostic, deep-link capable knowledge & action engine.
 * 
 * Features:
 * 1. Live Context & State Diagnostics: Inspects live Android permissions and settings directly.
 * 2. Interactive Direct Deep-Linking & Inline Action Chips: Generates action chips that directly launch screens or change settings.
 * 3. Interactive Follow-Up & Session Context: Resolves pronouns ('it', 'change it', 'why') based on previous query context.
 * 4. Fuzzy Semantic Search & Typo Tolerance: Levenshtein distance matching for misspelled commands and settings.
 * 6. Inline Setting Adjustment Toggles: Allows fast inline updates right from chat (e.g. setting reminder interval or toggle switches).
 */
object AyvaTalkEngine {

    // In-memory conversation context for pronouns / follow-ups
    @Volatile
    var lastQueriedTopicId: String? = null

    // --- CONVERSATION SESSION STATE (SLOT-FILLING) ---
    data class ConversationSession(
        val expectedPrefKey: String,
        val expectedPrefType: String,
        val topicTitle: String
    )
    
    @Volatile
    var activeSession: ConversationSession? = null

    // --- NLP SEMANTIC DICTIONARY & STEMMING ---
    private val synonyms = mapOf(
        "alarm" to listOf("chime", "beep", "ring", "alert", "notification", "remind", "sound"),
        "delete" to listOf("remove", "uninstall", "clear", "trash", "erase"),
        "stop" to listOf("block", "prevent", "quit", "disable", "turn off", "halt"),
        "start" to listOf("enable", "turn on", "activate", "begin", "launch"),
        "time" to listOf("duration", "timer", "interval", "countdown", "length", "minutes", "seconds"),
        "app" to listOf("application", "game"),
        "change" to listOf("modify", "edit", "set", "adjust", "update", "switch")
    )

    private fun stem(word: String): String {
        var w = word.lowercase().trim()
        if (w.endsWith("ing") && w.length > 4) return w.removeSuffix("ing")
        if (w.endsWith("ed") && w.length > 3) return w.removeSuffix("ed")
        if (w.endsWith("s") && w.length > 3 && !w.endsWith("ss")) return w.removeSuffix("s")
        return w
    }

    private fun expandTokens(query: String): List<String> {
        val words = query.lowercase().split(Regex("\\W+")).filter { it.isNotBlank() }
        val expanded = mutableListOf<String>()
        for (w in words) {
            expanded.add(w)
            val stemmed = stem(w)
            expanded.add(stemmed)
            synonyms.forEach { (key, list) ->
                if (key == w || key == stemmed || list.contains(w) || list.contains(stemmed)) {
                    expanded.add(key)
                    expanded.addAll(list)
                }
            }
        }
        return expanded.distinct()
    }

    data class TalkResponse(
        val formattedText: String,
        val actions: List<TalkAction> = emptyList(),
        val topicId: String? = null,
        val jsonPayload: String? = null,
        val cliCommandRedirect: String? = null
    )

    data class SpecificTopic(
        val id: String,
        val title: String,
        val emoji: String,
        val directExplanation: String,
        val whereToGo: String,
        val whatToClick: List<String>,
        val toggleOrRange: String? = null,
        val whyOrTip: String? = null,
        val keywords: List<String>,
        val route: String? = null,
        val systemSettingAction: String? = null,
        val isPackageSettingUri: Boolean = false,
        val prefKey: String? = null,
        val prefType: String? = null,
        val diagnosticKey: String? = null
    ) {
        fun formatResponse(context: Context? = null): String {
            val sb = StringBuilder()
            sb.append("*${title}*\n\n")

            if (context != null && diagnosticKey != null) {
                val diagnostic = resolveDiagnostic(diagnosticKey, context)
                if (!diagnostic.isNullOrBlank()) {
                    sb.append("*Current Live Status:*\n").append(diagnostic).append("\n\n")
                }
            }

            sb.append(directExplanation).append("\n\n")

            if (whereToGo != null) {
                sb.append("*Where to go:*\n")
                sb.append("→ ").append(whereToGo).append("\n\n")
            }

            if (!whatToClick.isNullOrEmpty()) {
                whatToClick.forEach { step ->
                    sb.append("• ").append(step).append("\n")
                }
                sb.append("\n")
            }

            if (!toggleOrRange.isNullOrBlank()) {
                sb.append("*Setting Range:*\n")
                sb.append(toggleOrRange).append("\n\n")
            }

            if (!whyOrTip.isNullOrBlank()) {
                sb.append("*Context:*\n")
                sb.append(whyOrTip).append("\n")
            }

            return sb.toString().trimEnd()
        }

        fun getActions(context: Context?): List<TalkAction> {
            val list = mutableListOf<TalkAction>()

            if (route != null) {
                val routeName = when (route) {
                    "settings" -> "Open Settings"
                    "security" -> "Open Security"
                    "permissions" -> "Open Permissions"
                    "dashboard" -> "Open Focus Screen"
                    "todos" -> "Open To-Do List"
                    "schedules" -> "Open Routines"
                    "time" -> "Open Time Stats"
                    "account" -> "Open Account"
                    "bubble_settings" -> "Open Bubble Settings"
                    else -> "Open Screen"
                }
                list.add(TalkAction.NavigateAppScreen(route, routeName, "🚀"))
            }

            if (systemSettingAction != null) {
                val label = when (systemSettingAction) {
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION -> "Open Overlay Settings"
                    Settings.ACTION_USAGE_ACCESS_SETTINGS -> "Open Usage Access"
                    Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS, Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS -> "Battery Optimization"
                    Settings.ACTION_APP_NOTIFICATION_SETTINGS -> "Notification Settings"
                    else -> "Open Android Settings"
                }
                list.add(TalkAction.OpenSystemSetting(systemSettingAction, label, "⚙️", isPackageSettingUri))
            }

            if (context != null && diagnosticKey != null) {
                list.addAll(resolveCustomActions(diagnosticKey, context))
            }

            return list
        }
    }

    private fun resolveDiagnostic(key: String, ctx: Context): String? {
        val prefs = ctx.getSharedPreferences("focus_prefs", Context.MODE_PRIVATE)
        return when (key) {
            "vacation_mode" -> {
                val isVacation = AptitudeManager.isVacationMode(ctx)
                val currentStreak = AptitudeManager.profileFlow.value.currentStreak
                if (isVacation) {
                    "• Status: *ACTIVE (Streak Frozen ❄️)*\n• Frozen Streak: *$currentStreak days*\n• Practice Alerts: *PAUSED 🔕*"
                } else {
                    "• Status: *DISABLED (Normal Daily Tests ⚡)*\n• Current Streak: *$currentStreak days*\n• Practice Alerts: *ACTIVE 🔔*"
                }
            }
            "persistent_reminders" -> {
                val current = prefs.getInt("persistent_reminder_interval", 15)
                val display = if (current >= 60) "${current / 60}h" else "${current}m"
                "• Your interval is currently set to: *$display*"
            }
            "soft_mode_wait_timer" -> {
                val current = prefs.getInt("soft_lock_duration", 10)
                "• Current wait delay before unblocking: *${current}s*"
            }
            "soft_mode_relief_duration" -> {
                val current = prefs.getInt("soft_unlock_duration", 5)
                "• Current temporary unlock window: *${current} minutes*"
            }
            "routine_notifications" -> {
                val enabled = prefs.getBoolean("routine_notifications", true)
                val stateStr = if (enabled) "ENABLED (ON)" else "DISABLED (OFF)"
                "• Routine alerts are currently: *$stateStr*"
            }
            "task_notification_style" -> {
                val style = prefs.getString("task_notification_style", "Both") ?: "Both"
                "• Current display style: *$style*"
            }
            else -> null
        }
    }

    private fun resolveCustomActions(key: String, ctx: Context): List<TalkAction> {
        return when (key) {
            "vacation_mode" -> {
                val isVacation = AptitudeManager.isVacationMode(ctx)
                if (isVacation) {
                    listOf(
                        TalkAction.DirectPrefUpdate("vacation_mode", "boolean", "false", "Turn Off", "Disable Vacation Mode", "⚡")
                    )
                } else {
                    listOf(
                        TalkAction.DirectPrefUpdate("vacation_mode", "boolean", "true", "Turn On", "Enable Vacation Mode 🏖️", "❄️")
                    )
                }
            }
            "persistent_reminders" -> listOf(
                TalkAction.DirectPrefUpdate("persistent_reminder_interval", "int", "5", "5m", "Set to 5m", "⚡"),
                TalkAction.DirectPrefUpdate("persistent_reminder_interval", "int", "15", "15m", "Set to 15m", "⚡"),
                TalkAction.DirectPrefUpdate("persistent_reminder_interval", "int", "30", "30m", "Set to 30m", "⚡"),
                TalkAction.DirectPrefUpdate("persistent_reminder_interval", "int", "60", "1h", "Set to 1h", "⚡")
            )
            "soft_mode_wait_timer" -> listOf(
                TalkAction.DirectPrefUpdate("soft_lock_duration", "int", "5", "5s", "Set 5s Delay", "⏱️"),
                TalkAction.DirectPrefUpdate("soft_lock_duration", "int", "10", "10s", "Set 10s Delay", "⏱️"),
                TalkAction.DirectPrefUpdate("soft_lock_duration", "int", "20", "20s", "Set 20s Delay", "⏱️")
            )
            "soft_mode_relief_duration" -> listOf(
                TalkAction.DirectPrefUpdate("soft_unlock_duration", "int", "2", "2m", "Set 2 min Relief", "⏱️"),
                TalkAction.DirectPrefUpdate("soft_unlock_duration", "int", "5", "5m", "Set 5 min Relief", "⏱️"),
                TalkAction.DirectPrefUpdate("soft_unlock_duration", "int", "10", "10m", "Set 10 min Relief", "⏱️")
            )
            else -> emptyList()
        }
    }

    // --- LEVENSHTEIN FUZZY DISTANCE HELPER ---
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[s1.length][s2.length]
    }

    private fun isFuzzyMatchWord(word: String, target: String): Boolean {
        if (word == target) return true
        if (word.length < 3 || target.length < 3) return false
        val maxDist = if (target.length > 5) 2 else 1
        return levenshteinDistance(word, target) <= maxDist
    }

    // --- ATOMIC KNOWLEDGE BASE (JSON ASSET-DRIVEN) ---
    private var _topicsDatabase: List<SpecificTopic>? = null

    private fun getTopicsDatabase(context: Context?): List<SpecificTopic> {
        if (_topicsDatabase != null) return _topicsDatabase!!
        if (context == null) return emptyList()

        try {
            val jsonString = context.assets.open("ayva_knowledge_base.json").bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<SpecificTopic>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.optString("id")
                val title = obj.optString("title")
                val emoji = obj.optString("emoji")
                val directExplanation = obj.optString("directExplanation")
                val whereToGo = obj.optString("whereToGo")
                
                val whatToClickArray = obj.optJSONArray("whatToClick")
                val whatToClick = mutableListOf<String>()
                if (whatToClickArray != null) {
                    for (j in 0 until whatToClickArray.length()) {
                        whatToClick.add(whatToClickArray.getString(j))
                    }
                }
                
                val keywordsArray = obj.optJSONArray("keywords")
                val keywords = mutableListOf<String>()
                if (keywordsArray != null) {
                    for (j in 0 until keywordsArray.length()) {
                        keywords.add(keywordsArray.getString(j))
                    }
                }

                val toggleOrRange = if (obj.has("toggleOrRange")) obj.getString("toggleOrRange") else null
                val whyOrTip = if (obj.has("whyOrTip")) obj.getString("whyOrTip") else null
                val route = if (obj.has("route")) obj.getString("route") else null
                val systemSettingAction = if (obj.has("systemSettingAction")) obj.getString("systemSettingAction") else null
                val isPackageSettingUri = obj.optBoolean("isPackageSettingUri", false)
                val prefKey = if (obj.has("prefKey")) obj.getString("prefKey") else null
                val prefType = if (obj.has("prefType")) obj.getString("prefType") else null
                val diagnosticKey = if (obj.has("diagnosticKey")) obj.getString("diagnosticKey") else null

                list.add(
                    SpecificTopic(
                        id = id,
                        title = title,
                        emoji = emoji,
                        directExplanation = directExplanation,
                        whereToGo = whereToGo,
                        whatToClick = whatToClick,
                        toggleOrRange = toggleOrRange,
                        whyOrTip = whyOrTip,
                        keywords = keywords,
                        route = route,
                        systemSettingAction = systemSettingAction,
                        isPackageSettingUri = isPackageSettingUri,
                        prefKey = prefKey,
                        prefType = prefType,
                        diagnosticKey = diagnosticKey
                    )
                )
            }
            _topicsDatabase = list
            return list
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }
    /**
     * Answers a /talk query with Context & State awareness, follow-ups, and interactive actions.
     */
    suspend fun answerTalkQueryWithActions(query: String, context: Context? = null): TalkResponse {
        val trimmed = query.trim()
        val lower = trimmed.lowercase()

        // Clean query prefix if user typed /talk or /ask
        val cleanQuery = when {
            lower.startsWith("/talk") -> lower.removePrefix("/talk").trim()
            lower.startsWith("/ask") -> lower.removePrefix("/ask").trim()
            lower.startsWith("/guide") -> lower.removePrefix("/guide").trim()
            lower.startsWith("/how") -> lower.removePrefix("/how").trim()
            lower.startsWith("/faq") -> lower.removePrefix("/faq").trim()
            lower.startsWith("/help") -> lower.removePrefix("/help").trim()
            else -> lower
        }.replace("?", "").replace("!", "").replace(",", "").trim()

        if (cleanQuery.isEmpty()) {
            return TalkResponse(
                formattedText = "💬 Ask me any question about FocusByRj!\n\nFor example:\n• *\"/talk persistent reminders\"*\n• *\"/talk where do I change the app theme?\"*\n• *\"/talk how does soft mode wait timer work?\"*\n• *\"/talk why are apps not blocking?\"*\n• *\"/talk why can't I uninstall the app?\"*",
                actions = listOf(
                    TalkAction.NavigateAppScreen("settings", "Open Settings", "⚙️"),
                    TalkAction.NavigateAppScreen("permissions", "Check Permissions", "🛡️")
                )
            )
        }

        // --- 0. ACTIVE SESSION (SLOT-FILLING) INTERCEPT ---
        val currentSession = activeSession
        if (currentSession != null) {
            val isCancel = cleanQuery.contains("cancel") || cleanQuery.contains("stop") || cleanQuery.contains("nevermind")
            if (isCancel) {
                activeSession = null
                return TalkResponse("✅ Cancelled updating ${currentSession.topicTitle}.")
            }
            
            if (currentSession.expectedPrefType == "int") {
                val inputNum = cleanQuery.replace(Regex("[^0-9]"), "").toIntOrNull()
                if (inputNum != null && context != null) {
                    context.getSharedPreferences("focus_prefs", Context.MODE_PRIVATE)
                        .edit().putInt(currentSession.expectedPrefKey, inputNum).apply()
                    activeSession = null
                    return TalkResponse("✅ Updated ${currentSession.topicTitle} to $inputNum.")
                }
            } else if (currentSession.expectedPrefType == "boolean") {
                val isTrue = cleanQuery.contains("yes") || cleanQuery.contains("on") || cleanQuery.contains("true")
                val isFalse = cleanQuery.contains("no") || cleanQuery.contains("off") || cleanQuery.contains("false")
                if (isTrue || isFalse) {
                    val finalVal = isTrue
                    context?.getSharedPreferences("focus_prefs", Context.MODE_PRIVATE)
                        ?.edit()?.putBoolean(currentSession.expectedPrefKey, finalVal)?.apply()
                    activeSession = null
                    return TalkResponse("✅ ${currentSession.topicTitle} turned ${if (finalVal) "ON" else "OFF"}.")
                }
            }
            
            return TalkResponse("I'm waiting for a valid value to update ${currentSession.topicTitle}. (Type 'cancel' to stop).")
        }

        // --- 0.2 PERMISSIONS CHECK INTERCEPT ---
        if (context != null && (cleanQuery.contains("permission") || cleanQuery.contains("permissions"))) {
            if (cleanQuery.contains("are") || cleanQuery.contains("check") || cleanQuery.contains("granted") || cleanQuery.contains("all") || cleanQuery.contains("what") || cleanQuery.contains("missing") || cleanQuery.contains("necessary")) {
                val missingPermissions = mutableListOf<String>()
                val actions = mutableListOf<TalkAction>()
                
                if (!PermissionUtils.hasUsageStatsPermission(context)) {
                    missingPermissions.add("Usage Access (App Detection)")
                    actions.add(TalkAction.OpenSystemSetting(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS, "Open Usage Access", "⚙️", false))
                }
                if (!PermissionUtils.hasOverlayPermission(context)) {
                    missingPermissions.add("Display Over Other Apps (Blocker Screen)")
                    actions.add(TalkAction.OpenSystemSetting(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "Open Overlay Settings", "⚙️", true))
                }
                if (!PermissionUtils.isIgnoringBatteryOptimizations(context)) {
                    missingPermissions.add("Battery Optimization (Background Reliability)")
                    actions.add(TalkAction.OpenSystemSetting(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, "Battery Optimization", "⚙️", true))
                }
                if (!PermissionUtils.hasNotificationPermission(context)) {
                    missingPermissions.add("Notifications (Reminders)")
                    actions.add(TalkAction.OpenSystemSetting(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS, "Notification Settings", "⚙️", true))
                }
                
                val text = if (missingPermissions.isEmpty()) {
                    "✅ **All necessary permissions are granted.** FocusByRj is fully optimized and ready!"
                } else {
                    "⚠️ **Missing Permissions Detected:**\nYou need to grant the following for the app to work correctly:\n\n" +
                    missingPermissions.joinToString("\n") { "• $it" } +
                    "\n\nTap the buttons below to fix them."
                }
                
                return TalkResponse(text, actions)
            }
        }

        // --- 0.5 DATA INTROSPECTION (DATABASE INJECTION) ---
        if (context != null) {
            val app = context.applicationContext as? com.focusbyrj.app.FocusApplication
            if (app != null) {
                // Check if asking about tasks
                val nluResult = OfflineNluEngine.parse(cleanQuery, app.taskRepository.allTasks.first().filter { !it.isCompleted })
                if (nluResult.intent != NluIntent.UNKNOWN) {
                    val tasks = app.taskRepository.allTasks.first()
                    val pending = tasks.filter { !it.isCompleted }
                    val overdue = pending.filter { it.dueDate != null && it.dueDate < System.currentTimeMillis() }

                    if (nluResult.intent == NluIntent.RESCHEDULE || nluResult.intent == NluIntent.COMPLETE || nluResult.intent == NluIntent.DELETE) {
                        if (nluResult.isAllTasks) {
                            if (nluResult.intent == NluIntent.RESCHEDULE) {
                                val newDate = nluResult.targetDateMs ?: (System.currentTimeMillis() + 86400000L)
                                pending.forEach { app.taskRepository.updateTask(it.copy(dueDate = newDate)) }
                                return TalkResponse("✅ Rescheduled all ${pending.size} tasks.")
                            } else if (nluResult.intent == NluIntent.COMPLETE) {
                                pending.forEach { app.taskRepository.updateTask(it.copy(isCompleted = true)) }
                                return TalkResponse("✅ Completed all ${pending.size} tasks.")
                            } else if (nluResult.intent == NluIntent.DELETE) {
                                pending.forEach { app.taskRepository.deleteTask(it) }
                                return TalkResponse("✅ Deleted all ${pending.size} tasks.")
                            }
                        } else if (nluResult.targetTask != null) {
                            val targetTask = nluResult.targetTask
                            if (nluResult.intent == NluIntent.COMPLETE) {
                                app.taskRepository.updateTask(targetTask.copy(isCompleted = true))
                                return TalkResponse("✅ Marked '${targetTask.title}' as complete.")
                            } else if (nluResult.intent == NluIntent.DELETE) {
                                app.taskRepository.deleteTask(targetTask)
                                return TalkResponse("✅ Deleted '${targetTask.title}'.")
                            } else if (nluResult.intent == NluIntent.RESCHEDULE) {
                                val newDate = nluResult.targetDateMs ?: (System.currentTimeMillis() + 86400000L)
                                app.taskRepository.updateTask(targetTask.copy(dueDate = newDate))
                                return TalkResponse("✅ Rescheduled '${targetTask.title}'.")
                            }
                        }
                    }
                    
                    if (nluResult.intent == NluIntent.LIST_TASKS || cleanQuery.contains("today")) {
                        return TalkResponse("", emptyList(), cliCommandRedirect = if (nluResult.isAllTasks || cleanQuery.contains("all")) "/tasks all" else "/tasks")
                    }
                    
                    if (nluResult.intent == NluIntent.LIST_ROUTINES) {
                        val scheds = app.repository.allSchedules.first()
                        val enabledScheds = scheds.filter { it.isEnabled }
                        val today = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK)
                        val todayScheds = enabledScheds.filter { it.daysOfWeek.split(",").contains(today.toString()) }
                                            
                        val text = if (todayScheds.isEmpty()) {
                            "🗓️ You have **no active routines scheduled for today**."
                        } else {
                            "🗓️ You have **${todayScheds.size} routine(s) scheduled today**:\n\n" +
                            todayScheds.joinToString("\n\n") {
                                val startStr = String.format("%02d:%02d", it.startHour, it.startMinute)
                                val endStr = String.format("%02d:%02d", it.endHour, it.endMinute)
                                val appsStr = if (it.appsToBlock.isNotBlank()) "Apps: ${it.appsToBlock.split(",").size} restricted" else "No apps restricted"
                                "• **${it.name}** ($startStr - $endStr)\n  └ $appsStr" 
                            }
                        }
                        return TalkResponse(text, listOf(TalkAction.NavigateAppScreen("schedules", "View Schedules", "📅")))
                    }
                    
                    if (nluResult.intent == NluIntent.BLOCK_APP || nluResult.intent == NluIntent.BLOCK_FILTER || nluResult.intent == NluIntent.UNBLOCK) {
                        val isUnblock = (nluResult.intent == NluIntent.UNBLOCK)
                        val target = nluResult.targetFilterOrAppName ?: ""
                        val mode = nluResult.blockMode ?: "HARD"
                        val dao = app.database.appRestrictionDao()
                        
                        val pm = context.packageManager
                        val installedApps = pm.getInstalledApplications(android.content.pm.PackageManager.GET_META_DATA).mapNotNull {
                            val name = pm.getApplicationLabel(it).toString()
                            if (name.isNotBlank() && it.packageName != context.packageName) {
                                val category = com.focusbyrj.app.ui.screens.getCategoryForApp(it, it.packageName)
                                com.focusbyrj.app.ui.screens.AppInfo(name, it.packageName, category)
                            } else null
                        }
                        
                        val matchedApp = installedApps.filter { target.contains(it.name, ignoreCase = true) || OfflineNluEngine.levenshtein(target.lowercase(), it.name.lowercase()) <= 1 }.maxByOrNull { it.name.length }
                        val customCats = CustomCategoryManager.getCategories(context)
                        val matchedCustomCat = customCats.find { target.contains(it.name, ignoreCase = true) || OfflineNluEngine.levenshtein(target.lowercase(), it.name.lowercase()) <= 1 }
                        
                        val standardCatMapping = mapOf(
                            "social" to com.focusbyrj.app.ui.screens.AppCategory.SOCIAL,
                            "finance" to com.focusbyrj.app.ui.screens.AppCategory.PAYMENT,
                            "fianance" to com.focusbyrj.app.ui.screens.AppCategory.PAYMENT,
                            "payment" to com.focusbyrj.app.ui.screens.AppCategory.PAYMENT,
                            "shopping" to com.focusbyrj.app.ui.screens.AppCategory.SHOPPING,
                            "game" to com.focusbyrj.app.ui.screens.AppCategory.GAMES,
                            "games" to com.focusbyrj.app.ui.screens.AppCategory.GAMES,
                            "utility" to com.focusbyrj.app.ui.screens.AppCategory.UTILITY,
                            "utilities" to com.focusbyrj.app.ui.screens.AppCategory.UTILITY,
                            "other" to com.focusbyrj.app.ui.screens.AppCategory.OTHERS,
                            "others" to com.focusbyrj.app.ui.screens.AppCategory.OTHERS
                        )
                        val matchedStandardCatWord = standardCatMapping.keys.find { target.contains(it, ignoreCase = true) }
                        val matchedStandardCat = matchedStandardCatWord?.let { standardCatMapping[it] }
                        
                        if (isUnblock) {
                            if ((cleanQuery.contains("all") || target.isEmpty()) && !cleanQuery.contains("filter")) {
                                dao.deleteAllRestrictions()
                                return TalkResponse("🔓 **Lock Protocol Lifted**\n_All applications have been unlocked._")
                            }
                            
                            if (matchedStandardCat != null) {
                                val targetApps = installedApps.filter { it.category == matchedStandardCat }
                                dao.deleteRestrictions(targetApps.map { it.packageName })
                                return TalkResponse("🔓 **Lock Protocol Lifted**\n_Unlocked all ${matchedStandardCat.title} apps._")
                            } else if (matchedCustomCat != null) {
                                val targetApps = installedApps.filter { matchedCustomCat.packages.contains(it.packageName) }
                                dao.deleteRestrictions(targetApps.map { it.packageName })
                                return TalkResponse("🔓 **Lock Protocol Lifted**\n_Unlocked all apps in '${matchedCustomCat.name}' filter._")
                            } else if (matchedApp != null) {
                                dao.deleteRestriction(matchedApp.packageName)
                                return TalkResponse("🔓 **Lock Protocol Lifted**\n_Unlocked ${matchedApp.name}._")
                            } else {
                                return TalkResponse("⚠️ _Could not identify filter or app '$target' to unlock._")
                            }
                        } else {
                            if ((cleanQuery.contains("all") || target.isEmpty()) && !cleanQuery.contains("filter")) {
                                val targetApps = pm.getInstalledApplications(android.content.pm.PackageManager.GET_META_DATA).filter {
                                    (it.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0 && it.packageName != context.packageName
                                }.map {
                                    val name = pm.getApplicationLabel(it).toString()
                                    com.focusbyrj.app.data.AppRestriction(it.packageName, name, isRestricted = true, mode = mode, restrictionMode = "SIMPLE")
                                }
                                dao.insertRestrictions(targetApps)
                                return TalkResponse("🔒 **Lock Protocol Active**\n_Locked all non-system apps [$mode]._")
                            } else if (matchedStandardCat != null) {
                                val targetApps = installedApps.filter { it.category == matchedStandardCat }
                                val restrictions = targetApps.map {
                                    com.focusbyrj.app.data.AppRestriction(it.packageName, it.name, isRestricted = true, mode = mode, restrictionMode = "SIMPLE")
                                }
                                dao.insertRestrictions(restrictions)
                                return TalkResponse("🔒 **Lock Protocol Active**\n_Locked all ${matchedStandardCat.title} apps [$mode]._")
                            } else if (matchedCustomCat != null) {
                                val targetApps = installedApps.filter { matchedCustomCat.packages.contains(it.packageName) }
                                val restrictions = targetApps.map {
                                    com.focusbyrj.app.data.AppRestriction(it.packageName, it.name, isRestricted = true, mode = mode, restrictionMode = "SIMPLE")
                                }
                                dao.insertRestrictions(restrictions)
                                return TalkResponse("🔒 **Lock Protocol Active**\n_Locked all apps in '${matchedCustomCat.name}' filter [$mode]._")
                            } else if (matchedApp != null) {
                                dao.insertRestriction(
                                    com.focusbyrj.app.data.AppRestriction(matchedApp.packageName, matchedApp.name, isRestricted = true, mode = mode, restrictionMode = "SIMPLE")
                                )
                                return TalkResponse("🔒 **Lock Protocol Active**\n_Locked ${matchedApp.name} [$mode]._")
                            } else {
                                return TalkResponse("⚠️ _System could not find filter or app '$target'._")
                            }
                        }
                    }
                }
            }
        }

        // --- 1. Follow-up & Pronoun Resolution ('it', 'how to change it', 'where is it', 'why') ---
        val resolvedQuery = if (lastQueriedTopicId != null && isFollowUpQuery(cleanQuery)) {
            val lastTopic = getTopicsDatabase(context).find { it.id == lastQueriedTopicId }
            if (lastTopic != null) {
                lastTopic.id
            } else {
                cleanQuery
            }
        } else {
            cleanQuery
        }

        // --- 2. Direct ID match ---
        val idMatch = getTopicsDatabase(context).find { it.id.equals(resolvedQuery, ignoreCase = true) }
        if (idMatch != null) {
            lastQueriedTopicId = idMatch.id
            val actions = idMatch.getActions(context)
            val json = serializeActionsJson(idMatch.id, actions)
            return TalkResponse(idMatch.formatResponse(context), actions, idMatch.id, json)
        }

        // --- 3. Semantic & Fuzzy Scoring ---
        val scoredTopics = mutableListOf<Pair<SpecificTopic, Int>>()
        val queryWords = expandTokens(cleanQuery)

        for (topic in getTopicsDatabase(context)) {
            var score = 0
            val titleLower = topic.title.lowercase()
            val idLower = topic.id.lowercase()

            if (cleanQuery.contains(titleLower) || titleLower.contains(cleanQuery)) {
                score += 80
            }

            for (kw in topic.keywords) {
                if (cleanQuery.contains(kw)) {
                    score += 45
                } else if (kw.contains(cleanQuery) && cleanQuery.length >= 4) {
                    score += 30
                } else {
                    for (qWord in queryWords) {
                        for (kwWord in kw.split(" ")) {
                            if (isFuzzyMatchWord(qWord, kwWord)) {
                                score += 20
                            }
                        }
                    }
                }
            }

            for (word in queryWords) {
                if (titleLower.contains(word)) score += 15
                if (idLower.contains(word)) score += 10
                if (isFuzzyMatchWord(word, idLower)) score += 8
            }

            if (score > 0) {
                scoredTopics.add(Pair(topic, score))
            }
        }
        
        scoredTopics.sortByDescending { it.second }
        var bestTopic = scoredTopics.firstOrNull()?.first
        var highestScore = scoredTopics.firstOrNull()?.second ?: 0
        
        // --- 3.5 Disambiguation ---
        // If the score is weak or there are multiple close candidates
        if (bestTopic != null && highestScore < 50 && scoredTopics.size > 1) {
            val secondScore = scoredTopics[1].second
            if (highestScore - secondScore < 20) {
                val suggestions = scoredTopics.take(3).map { it.first }
                val actions = suggestions.map { TalkAction.AskQuery(it.title, it.title) }
                return TalkResponse(
                    formattedText = "🤔 I found a few settings that sound similar. Which one did you mean?",
                    actions = actions
                )
            }
        }

        // --- 4. INTENT EXTRACTION (e.g. "set wait timer to 15", "freeze streak", "turn on vacation mode") ---
        val hasIntentPrefix = cleanQuery.startsWith("set ") || cleanQuery.startsWith("change ") || 
            cleanQuery.startsWith("turn ") || cleanQuery.startsWith("toggle ") || 
            cleanQuery.startsWith("enable ") || cleanQuery.startsWith("disable ") || 
            cleanQuery.startsWith("freeze ") || cleanQuery.startsWith("unfreeze ") || 
            cleanQuery.startsWith("activate ") || cleanQuery.startsWith("deactivate ") ||
            cleanQuery.contains("set ") || cleanQuery.contains("turn ") || 
            cleanQuery.contains("change ") || cleanQuery.contains("toggle ") ||
            cleanQuery.contains("freeze") || cleanQuery.contains("unfreeze") ||
            cleanQuery.contains("vacation")

        if (bestTopic != null && hasIntentPrefix && context != null) {
            val isBooleanTurnOn = cleanQuery.contains(" on") || cleanQuery.contains("enable") || cleanQuery.contains("true") || cleanQuery.contains("activate") || cleanQuery.contains("freeze") || cleanQuery.startsWith("freeze")
            val isBooleanTurnOff = cleanQuery.contains(" off") || cleanQuery.contains("disable") || cleanQuery.contains("false") || cleanQuery.contains("deactivate") || cleanQuery.contains("unfreeze") || cleanQuery.startsWith("unfreeze")
            
            var num = cleanQuery.replace(Regex("[^0-9]"), "").toIntOrNull()
            var stringVal: String? = null
            
            // Extract potential string values based on typical settings
            if (bestTopic.prefType == "string") {
                val timeRegex = Regex("([0-9]{1,2})[:.]([0-9]{2})\\s*(am|pm)?", RegexOption.IGNORE_CASE)
                val timeMatch = timeRegex.find(cleanQuery)
                if (timeMatch != null) {
                    val hr = timeMatch.groupValues[1].toIntOrNull() ?: 12
                    val min = timeMatch.groupValues[2]
                    val ampm = (timeMatch.groupValues[3].uppercase().takeIf { it.isNotBlank() } ?: "AM")
                    stringVal = String.format("%02d:%s %s", hr, min, ampm)
                } else if (cleanQuery.contains("dark")) stringVal = "dark"
                else if (cleanQuery.contains("light")) stringVal = "light"
                else if (cleanQuery.contains("system")) stringVal = "system"
                else if (cleanQuery.contains("banner") || cleanQuery.contains("pop")) stringVal = "Banner"
                else if (cleanQuery.contains("silent")) stringVal = "Silent"
                else if (cleanQuery.contains("both")) stringVal = "Both"
                else if (cleanQuery.contains("dashboard")) stringVal = "dashboard"
                else if (cleanQuery.contains("time") || cleanQuery.contains("analytics")) stringVal = "time"
                else if (cleanQuery.contains("account") || cleanQuery.contains("profile")) stringVal = "account"
                else if (cleanQuery.contains("schedule")) stringVal = "schedules"
            }
            
            if (num != null && bestTopic.prefKey != null && bestTopic.prefType == "int") {
                if ((cleanQuery.contains("hour") || cleanQuery.contains("hr")) && num < 24) {
                    num *= 60
                }
                val displayVal = if (num >= 60 && num % 60 == 0) "${num / 60} hour(s)" else if (bestTopic.id.contains("minute")) "$num minute(s)" else if (bestTopic.id.contains("hour")) "$num hour(s)" else "$num"
                
                val pendingJson = JSONObject().apply {
                    put("topicId", bestTopic!!.id)
                    put("prefKey", bestTopic!!.prefKey)
                    put("prefType", "int")
                    put("value", num.toString())
                    put("title", bestTopic!!.title)
                    put("displayVal", displayVal)
                    put("status", "pending")
                }.toString()
                
                return TalkResponse(
                    formattedText = "Do you want to change *${bestTopic.title}* to *$displayVal*?",
                    jsonPayload = pendingJson
                )
            } else if ((isBooleanTurnOn || isBooleanTurnOff) && bestTopic.prefKey != null && bestTopic.prefType == "boolean") {
                val finalBool = isBooleanTurnOn
                val actionStr = if (finalBool) "Enable" else "Disable"
                
                val pendingJson = JSONObject().apply {
                    put("topicId", bestTopic!!.id)
                    put("prefKey", bestTopic!!.prefKey)
                    put("prefType", "boolean")
                    put("value", finalBool.toString())
                    put("title", bestTopic!!.title)
                    put("displayVal", if(finalBool) "Enabled" else "Disabled")
                    put("status", "pending")
                }.toString()
                
                return TalkResponse(
                    formattedText = "Do you want to *$actionStr* *${bestTopic.title}*?",
                    jsonPayload = pendingJson
                )
            } else if (stringVal != null && bestTopic.prefKey != null && bestTopic.prefType == "string") {
                val pendingJson = JSONObject().apply {
                    put("topicId", bestTopic!!.id)
                    put("prefKey", bestTopic!!.prefKey)
                    put("prefType", "string")
                    put("value", stringVal)
                    put("title", bestTopic!!.title)
                    put("displayVal", stringVal)
                    put("status", "pending")
                }.toString()
                
                return TalkResponse(
                    formattedText = "Do you want to change *${bestTopic.title}* to *$stringVal*?",
                    jsonPayload = pendingJson
                )
            }
        }

        if (bestTopic != null && highestScore >= 12) {
            lastQueriedTopicId = bestTopic.id
            val actions = bestTopic.getActions(context)
            val json = serializeActionsJson(bestTopic.id, actions)
            return TalkResponse(bestTopic.formatResponse(context), actions, bestTopic.id, json)
        }

        // Fallback
        return TalkResponse(
            formattedText = "🤔 I couldn't find a specific setting for *\"$trimmed\"*.\n\nYou can ask about any specific toggle or feature, like:\n• *Persistent Reminders*\n• *Soft Mode Wait Timer*\n• *Relief Duration*\n• *Uninstall Protection*\n• *App Theme*\n• *Routines or Subtasks*\n• *Why are apps not blocking?*",
            actions = listOf(
                TalkAction.NavigateAppScreen("settings", "Open Settings", "⚙️")
            )
        )
    }

    /**
     * Backward-compatible simple string response
     */
    suspend fun answerTalkQuery(query: String, context: Context? = null): String {
        return answerTalkQueryWithActions(query, context).formattedText
    }

    private fun isFollowUpQuery(query: String): Boolean {
        val lower = query.lowercase().trim()
        return lower == "it" ||
                lower == "why" ||
                lower == "where" ||
                lower == "how" ||
                lower.contains("change it") ||
                lower.contains("how to change it") ||
                lower.contains("where is it") ||
                lower.contains("what about it") ||
                lower.contains("how do i change it") ||
                lower.contains("how to adjust it")
    }

    private fun serializeActionsJson(topicId: String, actions: List<TalkAction>): String? {
        if (actions.isEmpty()) return null
        return try {
            val obj = JSONObject()
            obj.put("topicId", topicId)
            val arr = JSONArray()
            actions.forEach { act ->
                val actObj = JSONObject()
                actObj.put("label", act.label)
                actObj.put("emoji", act.emoji)
                when (act) {
                    is TalkAction.NavigateAppScreen -> {
                        actObj.put("type", "navigate")
                        actObj.put("route", act.route)
                    }
                    is TalkAction.AskQuery -> {
                        actObj.put("type", "ask_query")
                        actObj.put("query", act.query)
                    }
                    is TalkAction.OpenSystemSetting -> {
                        actObj.put("type", "system_setting")
                        actObj.put("action", act.intentAction)
                        actObj.put("packageUri", act.packageUri)
                    }
                    is TalkAction.DirectPrefUpdate -> {
                        actObj.put("type", "pref_update")
                        actObj.put("prefKey", act.prefKey)
                        actObj.put("prefType", act.prefType)
                        actObj.put("targetValue", act.targetValue)
                        actObj.put("displayValue", act.displayValue)
                    }
                }
                arr.put(actObj)
            }
            obj.put("actions", arr)
            obj.toString()
        } catch (e: Exception) {
            null
        }
    }
}
