package com.focusbyrj.app.util

import com.focusbyrj.app.data.Task
import java.util.Calendar

enum class NluIntent {
    RESCHEDULE, COMPLETE, DELETE, LIST_TASKS, BLOCK_APP, BLOCK_FILTER, UNBLOCK, LIST_ROUTINES, START_ROUTINE, STOP_ROUTINE, START_DRILL, SHOW_PROFILE, SHOW_SUMMARY, CLEAR_CHAT, UNKNOWN
}

data class NluParsedResult(
    val intent: NluIntent,
    val targetTask: Task? = null,
    val isAllTasks: Boolean = false,
    val targetDateMs: Long? = null,
    val blockMode: String? = null,
    val targetFilterOrAppName: String? = null,
    val targetRoutineName: String? = null
)

object OfflineNluEngine {

    // 1. Levenshtein Distance for fuzzy typo matching
    fun levenshtein(lhs: CharSequence, rhs: CharSequence): Int {
        val lhsLength = lhs.length
        val rhsLength = rhs.length
        var cost = IntArray(lhsLength + 1) { it }
        var newCost = IntArray(lhsLength + 1)
        for (i in 1..rhsLength) {
            newCost[0] = i
            for (j in 1..lhsLength) {
                val match = if (lhs[j - 1] == rhs[i - 1]) 0 else 1
                val costReplace = cost[j - 1] + match
                val costInsert = cost[j] + 1
                val costDelete = newCost[j - 1] + 1
                newCost[j] = minOf(costInsert, costDelete, costReplace)
            }
            val swap = cost
            cost = newCost
            newCost = swap
        }
        return cost[lhsLength]
    }

    // 2. Fuzzy Token Matcher
    private fun matchesAnyFuzzy(tokens: List<String>, targets: List<String>, maxDist: Int = 1): Boolean {
        for (token in tokens) {
            for (target in targets) {
                if (token.length > 3 && target.length > 3) {
                    if (levenshtein(token, target) <= maxDist) return true
                } else {
                    if (token == target) return true
                }
            }
        }
        return false
    }

    // 3. Intent Classification
    fun classifyIntent(query: String): NluIntent {
        val lower = query.lowercase().trim()
        val tokens = lower.split(Regex("\\s+"))

        val isReschedule = matchesAnyFuzzy(tokens, listOf("reschedule", "postpone", "delay", "move", "bump")) || 
                           (lower.contains("change") && (lower.contains("date") || lower.contains("time") || lower.contains("due")))
        val isComplete = matchesAnyFuzzy(tokens, listOf("complete", "finish", "done", "check")) || 
                         (lower.contains("mark") && (lower.contains("done") || lower.contains("complete"))) ||
                         lower.startsWith("done with") || lower.startsWith("completed")
        val isDelete = matchesAnyFuzzy(tokens, listOf("delete", "remove", "trash", "cancel"))
        val isListTasks = ((matchesAnyFuzzy(tokens, listOf("list", "show", "what", "pending", "overdue", "today")) || lower.startsWith("what are")) && 
                           (lower.contains("task") || lower.contains("to do") || lower.contains("todo") || lower.contains("agenda"))) || 
                           lower == "tasks" || lower == "my tasks" || lower == "todo list" || lower == "todo" || lower == "todos"
        
        val isBlock = matchesAnyFuzzy(tokens, listOf("block", "lock", "restrict"))
        val isUnblock = matchesAnyFuzzy(tokens, listOf("unblock", "unlock", "allow"))
        
        val isRoutineKeyword = matchesAnyFuzzy(tokens, listOf("routine", "routines", "schedule", "schedules"))
        val isStartRoutine = (matchesAnyFuzzy(tokens, listOf("start", "enable", "activate", "resume", "begin", "launch")) || (lower.contains("turn") && lower.contains("on"))) && isRoutineKeyword
        val isStopRoutine = (matchesAnyFuzzy(tokens, listOf("stop", "disable", "deactivate", "pause", "halt", "end", "quit")) || (lower.contains("turn") && lower.contains("off"))) && isRoutineKeyword
        val isListRoutines = (matchesAnyFuzzy(tokens, listOf("list", "show", "what", "display", "check", "status")) && matchesAnyFuzzy(tokens, listOf("routine", "routines", "schedule", "schedules", "timings"))) || lower == "routines" || lower == "schedules" || lower == "my routines"

        val isStartDrill = matchesAnyFuzzy(tokens, listOf("drill", "math", "arithmetic", "calculate", "quiz")) || lower.contains("practice math")
        val isShowProfile = matchesAnyFuzzy(tokens, listOf("profile", "level", "aptitude", "streak", "stats", "statistics", "points", "xp"))
        val isShowSummary = matchesAnyFuzzy(tokens, listOf("summary", "briefing", "report", "overview", "recap", "dashboard")) || lower.contains("my day") || lower == "today"
        val isClearChat = matchesAnyFuzzy(tokens, listOf("clear", "clean", "reset", "wipe")) && matchesAnyFuzzy(tokens, listOf("chat", "messages", "screen", "history", "all"))

        return when {
            isClearChat -> NluIntent.CLEAR_CHAT
            isStartDrill -> NluIntent.START_DRILL
            isShowProfile -> NluIntent.SHOW_PROFILE
            isShowSummary -> NluIntent.SHOW_SUMMARY
            isStartRoutine -> NluIntent.START_ROUTINE
            isStopRoutine -> NluIntent.STOP_ROUTINE
            isListRoutines -> NluIntent.LIST_ROUTINES
            isBlock -> if (lower.contains("filter") || lower.contains("category")) NluIntent.BLOCK_FILTER else NluIntent.BLOCK_APP
            isUnblock -> NluIntent.UNBLOCK
            isReschedule -> NluIntent.RESCHEDULE
            isComplete -> NluIntent.COMPLETE
            isDelete -> NluIntent.DELETE
            isListTasks -> NluIntent.LIST_TASKS
            else -> NluIntent.UNKNOWN
        }
    }

    // 4. Time Entity Extraction (NER)
    fun extractTimeEntity(query: String): Long? {
        // First try high-precision SmartDateParser
        val parsed = SmartDateParser.parse(query)
        if (parsed.timestamp != null) {
            return parsed.timestamp
        }

        val lower = query.lowercase()
        val now = System.currentTimeMillis()
        
        val startOfDay = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        // Time parsing like "9pm"
        var customTimeMs: Long? = null
        val timeRegex = Regex("([1-9]|1[0-2])\\s*(am|pm)")
        val match = timeRegex.find(lower)
        if (match != null) {
            var hour = match.groupValues[1].toInt()
            val ampm = match.groupValues[2]
            if (ampm == "pm" && hour < 12) hour += 12
            if (ampm == "am" && hour == 12) hour = 0
            
            customTimeMs = startOfDay + (hour * 3600000L)
        }

        val baseDay = when {
            lower.contains("tomorrow") -> startOfDay + 86400000L
            lower.contains("next week") -> startOfDay + (7 * 86400000L)
            lower.contains("tonight") -> startOfDay
            lower.contains("monday") -> getNextDayOfWeek(Calendar.MONDAY, startOfDay)
            lower.contains("tuesday") -> getNextDayOfWeek(Calendar.TUESDAY, startOfDay)
            lower.contains("wednesday") -> getNextDayOfWeek(Calendar.WEDNESDAY, startOfDay)
            lower.contains("thursday") -> getNextDayOfWeek(Calendar.THURSDAY, startOfDay)
            lower.contains("friday") -> getNextDayOfWeek(Calendar.FRIDAY, startOfDay)
            lower.contains("saturday") -> getNextDayOfWeek(Calendar.SATURDAY, startOfDay)
            lower.contains("sunday") -> getNextDayOfWeek(Calendar.SUNDAY, startOfDay)
            else -> null
        }
        
        if (baseDay != null && customTimeMs != null) {
             val diff = customTimeMs - startOfDay
             return baseDay + diff
        } else if (baseDay != null) {
             return if (lower.contains("tonight")) baseDay + (20 * 3600000L) else baseDay + (12 * 3600000L)
        } else if (customTimeMs != null) {
             return customTimeMs
        }

        return null
    }
    
    private fun getNextDayOfWeek(targetDayOfWeek: Int, startOfDayMs: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = startOfDayMs }
        var addDays = targetDayOfWeek - cal.get(Calendar.DAY_OF_WEEK)
        if (addDays <= 0) addDays += 7
        return startOfDayMs + (addDays * 86400000L)
    }

    // 5. Target Entity Extraction (Which task?)
    fun extractTargetTask(query: String, pendingTasks: List<Task>): Pair<Task?, Boolean> {
        val lower = query.lowercase().trim()
        val isAll = lower.contains("all") || lower.contains("everything")
        if (isAll) return Pair(null, true)

        if (pendingTasks.isEmpty()) return Pair(null, false)
        if (pendingTasks.size == 1 && (lower.contains("it") || lower.contains("that") || lower.contains("the task"))) {
            return Pair(pendingTasks.first(), false)
        }

        // Semantic relative task descriptions
        val now = System.currentTimeMillis()
        if (lower.contains("overdue")) {
            val overdue = pendingTasks.find { it.dueDate != null && it.dueDate < now }
            if (overdue != null) return Pair(overdue, false)
        }
        if (lower.contains("priority") || lower.contains("important") || lower.contains("urgent") || lower.contains("top task")) {
            val priorityTask = pendingTasks.find { it.isPriority }
            if (priorityTask != null) return Pair(priorityTask, false)
        }
        if (lower.contains("last one") || lower.contains("last task")) {
            return Pair(pendingTasks.lastOrNull(), false)
        }
        if (lower == "it" || lower == "that" || lower.endsWith(" it") || lower.endsWith(" that")) {
            return Pair(pendingTasks.firstOrNull(), false)
        }
        
        // Ordinal & Digit Check (e.g. "task 1", "task #1", "#1", "1st task", "complete 1", "delete 2", "reschedule 1")
        val numRegex = Regex("(?:task\\s*#?\\s*(\\d+)|#\\s*(\\d+)|\\b(?:complete|finish|done|delete|remove|reschedule|postpone|move|bump|check)\\s+(?:task\\s+|#)?(\\d+)\\b|\\b(\\d+)(?:st|nd|rd|th)?\\s+(?:task|one)\\b)")
        val numMatch = numRegex.find(lower)
        if (numMatch != null) {
            val numStr = (numMatch.groupValues[1].ifEmpty { null }
                ?: numMatch.groupValues[2].ifEmpty { null }
                ?: numMatch.groupValues[3].ifEmpty { null }
                ?: numMatch.groupValues[4].ifEmpty { null })
            val num = numStr?.toIntOrNull()
            if (num != null) {
                val index = num - 1
                if (index in pendingTasks.indices) {
                    return Pair(pendingTasks[index], false)
                }
            }
        }

        // Word ordinals
        val wordRegex = Regex("\\b(first|second|third|fourth|fifth|1st|2nd|3rd|4th|5th)\\b(?:\\s+(?:task|one))?")
        val wordMatch = wordRegex.find(lower)
        if (wordMatch != null) {
            val wordStr = wordMatch.groupValues[1]
            val index = when (wordStr) {
                "first", "1st" -> 0
                "second", "2nd" -> 1
                "third", "3rd" -> 2
                "fourth", "4th" -> 3
                "fifth", "5th" -> 4
                else -> -1
            }
            if (index in pendingTasks.indices) {
                return Pair(pendingTasks[index], false)
            }
        }

        // Fuzzy match against task titles
        var bestMatch: Task? = null
        var lowestDistance = Int.MAX_VALUE

        for (task in pendingTasks) {
            val titleLower = task.title.lowercase()
            // Direct substring match is strongest
            if (lower.contains(titleLower)) return Pair(task, false)
            
            // Token intersection match
            val titleTokens = titleLower.split(Regex("\\s+")).filter { it.length > 2 }
            val queryTokens = lower.split(Regex("\\s+")).filter { it.length > 2 }
            
            for (tToken in titleTokens) {
                for (qToken in queryTokens) {
                    val dist = levenshtein(tToken, qToken)
                    // If a significant word matches with typo allowance
                    if (dist <= 1 && tToken.length > 3) {
                         if (dist < lowestDistance) {
                             lowestDistance = dist
                             bestMatch = task
                         }
                    }
                }
            }
        }
        return Pair(bestMatch, false)
    }
    
    // Extractor for Block Intent
    fun extractBlockTarget(query: String): Pair<String?, String?> {
        val lower = query.lowercase()
        val mode = if (lower.contains("soft block") || lower.contains("soft mode") || lower.contains("soft lock") || lower.contains("soft")) "SOFT" else "HARD"
        
        val match = Regex("\\b(block|lock|restrict|unblock|unlock|allow)\\b\\s+(.*)").find(lower)
        var targetName = match?.groupValues?.get(2) ?: lower
        targetName = targetName.replace(Regex("\\b(soft|hard|in|mode|filter|category|apps|app|the|a|an)\\b"), " ")
        targetName = targetName.replace(Regex("\\s+"), " ").trim()
        
        return Pair(targetName.ifBlank { null }, mode)
    }
    
    // Extractor for Routine Intent
    fun extractRoutineTarget(query: String): Pair<String?, Boolean> {
        val lower = query.lowercase().trim()
        val isAll = lower.contains("all")
        
        val match = Regex("\\b(start|begin|launch|enable|activate|resume|stop|disable|deactivate|pause|halt|end|turn\\s+on|turn\\s+off)\\b\\s+(.*)").find(lower)
        var targetName = match?.groupValues?.get(2) ?: lower
        targetName = targetName.replace(Regex("\\b(routine|routines|schedule|schedules|the|my|a|an|all)\\b"), " ")
        targetName = targetName.replace(Regex("\\s+"), " ").trim()
        
        return Pair(targetName.ifEmpty { null }, isAll)
    }

    // Master Parser
    fun parse(query: String, pendingTasks: List<Task>): NluParsedResult {
        var intent = classifyIntent(query)
        val (targetTask, isAll) = extractTargetTask(query, pendingTasks)
        val dateMs = if (intent == NluIntent.RESCHEDULE) extractTimeEntity(query) else null
        
        var targetFilterOrAppName: String? = null
        var blockMode: String? = null
        var targetRoutineName: String? = null
        var isAllRoutines = false
        
        if (intent == NluIntent.BLOCK_APP || intent == NluIntent.BLOCK_FILTER || intent == NluIntent.UNBLOCK) {
            val (name, mode) = extractBlockTarget(query)
            targetFilterOrAppName = name
            blockMode = mode
            if (name.isNullOrEmpty() && !query.lowercase().contains("all")) {
                intent = NluIntent.UNKNOWN
            }
        } else if (intent == NluIntent.START_ROUTINE || intent == NluIntent.STOP_ROUTINE) {
            val (routineName, allRoutines) = extractRoutineTarget(query)
            targetRoutineName = routineName
            isAllRoutines = allRoutines
        }
        
        // Fallback for strict single-word or direct command phrases only
        if (intent == NluIntent.UNKNOWN) {
            val trimmed = query.lowercase().trim()
            if (trimmed == "tasks" || trimmed == "todos" || trimmed == "my tasks" || trimmed == "pending tasks" || trimmed == "task list") {
                intent = NluIntent.LIST_TASKS
            } else if (trimmed == "profile" || trimmed == "my profile" || trimmed == "stats" || trimmed == "my stats" || trimmed == "xp") {
                intent = NluIntent.SHOW_PROFILE
            } else if (trimmed == "summary" || trimmed == "my summary" || trimmed == "daily summary" || trimmed == "briefing") {
                intent = NluIntent.SHOW_SUMMARY
            } else if (trimmed == "math" || trimmed == "drill" || trimmed == "math drill" || trimmed == "quiz") {
                intent = NluIntent.START_DRILL
            } else if (trimmed == "clear chat" || trimmed == "wipe chat" || trimmed == "clean chat") {
                intent = NluIntent.CLEAR_CHAT
            } else if (trimmed == "routines" || trimmed == "schedules" || trimmed == "my routines") {
                intent = NluIntent.LIST_ROUTINES
            }
        }
        
        return NluParsedResult(intent, targetTask, if (isAllRoutines) true else isAll, dateMs, blockMode, targetFilterOrAppName, targetRoutineName)
    }
}
