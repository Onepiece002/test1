package com.focusbyrj.app.util

import com.focusbyrj.app.data.Task
import java.util.Calendar

enum class NluIntent {
    RESCHEDULE, COMPLETE, DELETE, LIST_TASKS, BLOCK_APP, BLOCK_FILTER, UNBLOCK, LIST_ROUTINES, START_DRILL, SHOW_PROFILE, SHOW_SUMMARY, CLEAR_CHAT, UNKNOWN
}

data class NluParsedResult(
    val intent: NluIntent,
    val targetTask: Task? = null,
    val isAllTasks: Boolean = false,
    val targetDateMs: Long? = null,
    val blockMode: String? = null,
    val targetFilterOrAppName: String? = null
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

        val isReschedule = matchesAnyFuzzy(tokens, listOf("reschedule", "postpone", "delay", "move", "bump")) || (lower.contains("change") && lower.contains("date"))
        val isComplete = matchesAnyFuzzy(tokens, listOf("complete", "finish", "done", "check"))
        val isDelete = matchesAnyFuzzy(tokens, listOf("delete", "remove", "trash", "cancel"))
        val isListTasks = matchesAnyFuzzy(tokens, listOf("list", "show", "what", "pending", "overdue", "today")) && (lower.contains("task") || lower.contains("to do") || lower.contains("todo"))
        
        val isBlock = matchesAnyFuzzy(tokens, listOf("block", "lock", "restrict"))
        val isUnblock = matchesAnyFuzzy(tokens, listOf("unblock", "unlock", "allow"))
        
        val isListRoutines = matchesAnyFuzzy(tokens, listOf("list", "show", "what", "display")) && matchesAnyFuzzy(tokens, listOf("routine", "routines", "schedule", "schedules", "timings"))

        val isStartDrill = matchesAnyFuzzy(tokens, listOf("drill", "math", "arithmetic", "calculate", "test", "quiz"))
        val isShowProfile = matchesAnyFuzzy(tokens, listOf("profile", "level", "aptitude", "streak", "stats", "statistics", "points", "xp"))
        val isShowSummary = matchesAnyFuzzy(tokens, listOf("summary", "briefing", "report", "overview", "recap", "dashboard"))
        val isClearChat = matchesAnyFuzzy(tokens, listOf("clear", "clean", "reset", "wipe")) && matchesAnyFuzzy(tokens, listOf("chat", "messages", "screen", "history", "all"))

        return when {
            isClearChat -> NluIntent.CLEAR_CHAT
            isStartDrill -> NluIntent.START_DRILL
            isShowProfile -> NluIntent.SHOW_PROFILE
            isShowSummary -> NluIntent.SHOW_SUMMARY
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
        val lower = query.lowercase()
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        
        val startOfDay = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val noon = startOfDay + (12 * 3600000L)
        
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
            lower.contains("tonight") -> startOfDay // Time handled separately or defaults to 8pm
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
        val lower = query.lowercase()
        val isAll = lower.contains("all") || lower.contains("everything")
        if (isAll) return Pair(null, true)

        if (pendingTasks.isEmpty()) return Pair(null, false)
        if (pendingTasks.size == 1 && (lower.contains("it") || lower.contains("that") || lower.contains("the task"))) {
            return Pair(pendingTasks.first(), false)
        }
        
        // Ordinal Check (1st, 2nd, 3rd... or "task 1", "task 2")
        val ordinalRegex = Regex("(?:(\\d+)(?:st|nd|rd|th)?\\s+task|task\\s+(\\d+)|(first|second|third|fourth|fifth)\\s+task)")
        val match = ordinalRegex.find(lower)
        if (match != null) {
            val numStr1 = match.groupValues[1]
            val numStr2 = match.groupValues[2]
            val wordStr = match.groupValues[3]
            
            var index = -1
            if (numStr1.isNotEmpty()) index = numStr1.toInt() - 1
            else if (numStr2.isNotEmpty()) index = numStr2.toInt() - 1
            else if (wordStr.isNotEmpty()) {
                index = when(wordStr) {
                    "first" -> 0
                    "second" -> 1
                    "third" -> 2
                    "fourth" -> 3
                    "fifth" -> 4
                    else -> -1
                }
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
        
        return Pair(targetName, mode)
    }
    
    // Master Parser
    fun parse(query: String, pendingTasks: List<Task>): NluParsedResult {
        var intent = classifyIntent(query)
        val (targetTask, isAll) = extractTargetTask(query, pendingTasks)
        val dateMs = if (intent == NluIntent.RESCHEDULE) extractTimeEntity(query) else null
        
        var targetFilterOrAppName: String? = null
        var blockMode: String? = null
        
        if (intent == NluIntent.BLOCK_APP || intent == NluIntent.BLOCK_FILTER || intent == NluIntent.UNBLOCK) {
            val (name, mode) = extractBlockTarget(query)
            targetFilterOrAppName = name
            blockMode = mode
            if (name.isNullOrEmpty() && !query.lowercase().contains("all")) {
                intent = NluIntent.UNKNOWN
            }
        }
        
        // Fallback for generic intents
        if (intent == NluIntent.UNKNOWN) {
            if (query.lowercase().contains("task") || query.lowercase().contains("todo")) {
                intent = NluIntent.LIST_TASKS
            } else if (query.lowercase().contains("profile") || query.lowercase().contains("stats")) {
                intent = NluIntent.SHOW_PROFILE
            } else if (query.lowercase().contains("summary")) {
                intent = NluIntent.SHOW_SUMMARY
            } else if (query.lowercase().contains("math") || query.lowercase().contains("drill")) {
                intent = NluIntent.START_DRILL
            } else if (query.lowercase().contains("clear chat")) {
                intent = NluIntent.CLEAR_CHAT
            }
        }
        
        return NluParsedResult(intent, targetTask, isAll, dateMs, blockMode, targetFilterOrAppName)
    }
}
