const fs = require('fs');
const path = './app/src/main/java/com/focusbyrj/app/util/OfflineNluEngine.kt';
let code = fs.readFileSync(path, 'utf8');

code = code.replace(
    'enum class NluIntent {\n    RESCHEDULE, COMPLETE, DELETE, LIST_TASKS, BLOCK_APP, BLOCK_FILTER, UNBLOCK, LIST_ROUTINES, UNKNOWN\n}',
    'enum class NluIntent {\n    RESCHEDULE, COMPLETE, DELETE, LIST_TASKS, BLOCK_APP, BLOCK_FILTER, UNBLOCK, LIST_ROUTINES, START_DRILL, SHOW_PROFILE, SHOW_SUMMARY, CLEAR_CHAT, UNKNOWN\n}'
);

const newWhen = `        val isStartDrill = matchesAnyFuzzy(tokens, listOf("drill", "math", "arithmetic", "calculate", "test", "quiz"))
        val isShowProfile = matchesAnyFuzzy(tokens, listOf("profile", "level", "aptitude", "streak", "stats", "statistics", "points", "xp"))
        val isShowSummary = matchesAnyFuzzy(tokens, listOf("summary", "briefing", "report", "overview", "recap", "dashboard"))
        val isClearChat = matchesAnyFuzzy(tokens, listOf("clear", "clean", "reset", "wipe")) && matchesAnyFuzzy(tokens, listOf("chat", "messages", "screen", "history", "all"))

        return when {
            isClearChat -> NluIntent.CLEAR_CHAT
            isStartDrill -> NluIntent.START_DRILL
            isShowProfile -> NluIntent.SHOW_PROFILE
            isShowSummary -> NluIntent.SHOW_SUMMARY
            isListRoutines -> NluIntent.LIST_ROUTINES`;

code = code.replace('        return when {\n            isListRoutines -> NluIntent.LIST_ROUTINES', newWhen);

// fallback for list tasks
const newFallback = `        // Fallback for generic intents
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
        }`;

code = code.replace(`        // Fallback for list tasks
        if (intent == NluIntent.UNKNOWN && query.lowercase().contains("task")) {
            intent = NluIntent.LIST_TASKS
        }`, newFallback);

fs.writeFileSync(path, code);
