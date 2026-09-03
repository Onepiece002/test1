package com.focusbyrj.app.util

import com.focusbyrj.app.data.RecurrencePattern
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class ParseResult(
    val cleanText: String,
    val timestamp: Long?,
    val hasTime: Boolean,
    val recurrence: RecurrencePattern = RecurrencePattern.NONE
)

object SmartDateParser {

    fun formatDueDate(timestamp: Long?): String {
        if (timestamp == null) return ""
        val dueCal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val nowCal = Calendar.getInstance()
        val pattern = if (dueCal.get(Calendar.YEAR) != nowCal.get(Calendar.YEAR)) {
            "MMM dd, yyyy, h:mm a"
        } else {
            "MMM dd, h:mm a"
        }
        return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(timestamp))
    }

    fun formatDueDateFull(timestamp: Long?): String {
        if (timestamp == null) return "No Due Date"
        val dueCal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val nowCal = Calendar.getInstance()
        val pattern = if (dueCal.get(Calendar.YEAR) != nowCal.get(Calendar.YEAR)) {
            "EEE, MMM dd, yyyy • h:mm a"
        } else {
            "EEE, MMM dd • h:mm a"
        }
        return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(timestamp))
    }

    fun parse(input: String): ParseResult {
        var text = input.trim()
        val cal = Calendar.getInstance()
        val nowCal = Calendar.getInstance()
        var hasExplicitDate = false
        var hasExplicitTime = false
        var detectedRecurrence = RecurrencePattern.NONE

        fun removeMatch(match: MatchResult): String {
            return text.removeRange(match.range).trim().replace("\\s+".toRegex(), " ")
        }

        // 0. Clean task command prefixes like "task ,", "task:", "task -", "todo:", "remind me to", "add task", "remember to"
        val prefixRegex = Regex("(?i)^\\s*(?:(?:add|new|create)\\s+(?:task|todo)|task|todo|please\\s+remind\\s+me\\s+to|remind\\s+me\\s+to|remember\\s+to|need\\s+to|don't\\s+forget\\s+to|have\\s+to)\\s*[,:\\-]?\\s*")
        prefixRegex.find(text)?.let { match ->
            text = removeMatch(match)
        }

        // 1. Recurrence pattern detection
        val monthlyRegex = Regex("(?i)\\b(?:of\\s+every\\s+month|every\\s+month|each\\s+month|monthly)\\b")
        monthlyRegex.find(text)?.let { match ->
            detectedRecurrence = RecurrencePattern.MONTHLY
            text = removeMatch(match)
        }

        val dailyRegex = Regex("(?i)\\b(?:every\\s+day|each\\s+day|daily|everyday)\\b")
        dailyRegex.find(text)?.let { match ->
            detectedRecurrence = RecurrencePattern.DAILY
            text = removeMatch(match)
        }

        val weeklyRegex = Regex("(?i)\\b(?:every\\s+week|each\\s+week|weekly)\\b")
        weeklyRegex.find(text)?.let { match ->
            detectedRecurrence = RecurrencePattern.WEEKLY
            text = removeMatch(match)
        }

        val yearlyRegex = Regex("(?i)\\b(?:every\\s+year|each\\s+year|yearly|annually)\\b")
        yearlyRegex.find(text)?.let { match ->
            detectedRecurrence = RecurrencePattern.YEARLY
            text = removeMatch(match)
        }

        val everyDowRegex = Regex("(?i)\\b(?:every)\\s+(mon|tue|wed|thu|fri|sat|sun)(?:day|nes)?(?:day)?\\b")
        everyDowRegex.find(text)?.let { match ->
            detectedRecurrence = RecurrencePattern.WEEKLY
            val dayStr = match.groupValues[1].lowercase()
            val targetDay = when (dayStr) {
                "sun" -> Calendar.SUNDAY
                "mon" -> Calendar.MONDAY
                "tue" -> Calendar.TUESDAY
                "wed" -> Calendar.WEDNESDAY
                "thu" -> Calendar.THURSDAY
                "fri" -> Calendar.FRIDAY
                "sat" -> Calendar.SATURDAY
                else -> Calendar.MONDAY
            }
            val currentDay = cal.get(Calendar.DAY_OF_WEEK)
            var diff = targetDay - currentDay
            if (diff < 0) diff += 7
            cal.add(Calendar.DAY_OF_YEAR, diff)
            hasExplicitDate = true
            text = removeMatch(match)
        }

        // 2. "in X time" (e.g. in 30 mins, in 2 hours, in 3 days, in 2 months, in 5 years)
        val inRegex = Regex("(?i)\\b(in)\\s+(\\d+)\\s+(m|min|mins|minutes?|h|hr|hrs|hours?|d|days?|w|wks|weeks?|mo|mos|months?|y|yr|yrs|years?)\\b")
        inRegex.find(text)?.let { match ->
            val amount = match.groupValues[2].toIntOrNull() ?: 0
            val unit = match.groupValues[3].lowercase()
            if (unit.startsWith("mo") || unit.contains("month")) {
                cal.add(Calendar.MONTH, amount)
                hasExplicitDate = true
            } else if (unit.startsWith("y") || unit.contains("year")) {
                cal.add(Calendar.YEAR, amount)
                hasExplicitDate = true
            } else if (unit.startsWith("m")) {
                cal.add(Calendar.MINUTE, amount)
                hasExplicitTime = true
                hasExplicitDate = true
            } else if (unit.startsWith("h")) {
                cal.add(Calendar.HOUR, amount)
                hasExplicitTime = true
                hasExplicitDate = true
            } else if (unit.startsWith("d")) {
                cal.add(Calendar.DAY_OF_YEAR, amount)
                hasExplicitDate = true
            } else if (unit.startsWith("w")) {
                cal.add(Calendar.WEEK_OF_YEAR, amount)
                hasExplicitDate = true
            }
            text = removeMatch(match)
        }

        // 3. Named relative days: "today", "tomorrow", "tmrw", "tmr", "tonight", "next year"
        if (!hasExplicitDate) {
            val dayRegex = Regex("(?i)\\b(today|tomorrow|tmrw|tmr|tonight|next\\s+year)\\b")
            dayRegex.find(text)?.let { match ->
                val word = match.groupValues[1].lowercase()
                if (word == "tomorrow" || word == "tmrw" || word == "tmr") {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                    hasExplicitDate = true
                } else if (word == "tonight") {
                    cal.set(Calendar.HOUR_OF_DAY, 20)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    hasExplicitTime = true
                    hasExplicitDate = true
                } else if (word.contains("year")) {
                    cal.add(Calendar.YEAR, 1)
                    hasExplicitDate = true
                } else if (word == "today") {
                    hasExplicitDate = true
                }
                text = removeMatch(match)
            }
        }

        // 4. ISO & Numeric dates (e.g. 2026-05-20, 2026/05/20, 20/05/2026, 05/20/2026)
        if (!hasExplicitDate) {
            val isoRegex = Regex("(?i)\\b((?:19|20|21)\\d{2})[-/.](0?[1-9]|1[0-2])[-/.](0?[1-9]|[12]\\d|3[01])\\b")
            isoRegex.find(text)?.let { match ->
                val year = match.groupValues[1].toIntOrNull()
                val month = match.groupValues[2].toIntOrNull()
                val day = match.groupValues[3].toIntOrNull()
                if (year != null && month != null && day != null) {
                    cal.set(Calendar.YEAR, year)
                    cal.set(Calendar.MONTH, month - 1)
                    cal.set(Calendar.DAY_OF_MONTH, day)
                    hasExplicitDate = true
                    text = removeMatch(match)
                }
            } ?: run {
                val dmyRegex = Regex("(?i)\\b(?:on\\s+)?(0?[1-9]|[12]\\d|3[01])[-/.](0?[1-9]|[12]\\d|3[01])[-/.]((?:19|20|21)\\d{2})\\b")
                dmyRegex.find(text)?.let { match ->
                    val num1 = match.groupValues[1].toIntOrNull() ?: 1
                    val num2 = match.groupValues[2].toIntOrNull() ?: 1
                    val year = match.groupValues[3].toIntOrNull()
                    if (year != null) {
                        cal.set(Calendar.YEAR, year)
                        if (num1 > 12) {
                            cal.set(Calendar.MONTH, (num2 - 1).coerceIn(0, 11))
                            cal.set(Calendar.DAY_OF_MONTH, num1)
                        } else if (num2 > 12) {
                            cal.set(Calendar.MONTH, (num1 - 1).coerceIn(0, 11))
                            cal.set(Calendar.DAY_OF_MONTH, num2)
                        } else {
                            cal.set(Calendar.MONTH, (num1 - 1).coerceIn(0, 11))
                            cal.set(Calendar.DAY_OF_MONTH, num2)
                        }
                        hasExplicitDate = true
                        text = removeMatch(match)
                    }
                }
            }
        }

        // 5. Month + Day (e.g., "may 20 2026", "may 20th", "15th of January", "Jan 15")
        if (!hasExplicitDate) {
            val monthNames = listOf("jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec")
            val monthRegex = Regex("(?i)\\b(?:on\\s+)?(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*\\s+(\\d{1,2})(?:\\s*(?:st|nd|rd|th))?(?:[\\s,]+((?:19|20|21)\\d{2}))?\\b")
            monthRegex.find(text)?.let { match ->
                val monthStr = match.groupValues[1].lowercase()
                val day = match.groupValues[2].toIntOrNull() ?: 1
                val yearStr = match.groupValues[3]
                val monthIdx = monthNames.indexOf(monthStr)
                if (monthIdx != -1) {
                    if (yearStr.isNotBlank()) {
                        val year = yearStr.toIntOrNull()
                        if (year != null) {
                            cal.set(Calendar.YEAR, year)
                        }
                    }
                    cal.set(Calendar.MONTH, monthIdx)
                    cal.set(Calendar.DAY_OF_MONTH, day)
                    if (yearStr.isBlank() && cal.timeInMillis < System.currentTimeMillis() - 86400000L) {
                        cal.add(Calendar.YEAR, 1)
                    }
                    hasExplicitDate = true
                    text = removeMatch(match)
                }
            } ?: run {
                val dayMonthRegex = Regex("(?i)\\b(?:on\\s+)?(\\d{1,2})(?:\\s*(?:st|nd|rd|th))?\\s+(?:of\\s+)?(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*(?:[\\s,]+((?:19|20|21)\\d{2}))?\\b")
                dayMonthRegex.find(text)?.let { match ->
                    val day = match.groupValues[1].toIntOrNull() ?: 1
                    val monthStr = match.groupValues[2].lowercase()
                    val yearStr = match.groupValues[3]
                    val monthIdx = monthNames.indexOf(monthStr)
                    if (monthIdx != -1) {
                        if (yearStr.isNotBlank()) {
                            val year = yearStr.toIntOrNull()
                            if (year != null) {
                                cal.set(Calendar.YEAR, year)
                            }
                        }
                        cal.set(Calendar.MONTH, monthIdx)
                        cal.set(Calendar.DAY_OF_MONTH, day)
                        if (yearStr.isBlank() && cal.timeInMillis < System.currentTimeMillis() - 86400000L) {
                            cal.add(Calendar.YEAR, 1)
                        }
                        hasExplicitDate = true
                        text = removeMatch(match)
                    }
                }
            }
        }

        // 6. Day of week (e.g. "on monday", "next friday", "this sunday")
        if (!hasExplicitDate) {
            val dowRegex = Regex("(?i)\\b(?:on\\s+|next\\s+|this\\s+)?(mon|tue|wed|thu|fri|sat|sun)(?:day|nes)?(?:day)?\\b")
            dowRegex.find(text)?.let { match ->
                val dayStr = match.groupValues[1].lowercase()
                val targetDay = when (dayStr) {
                    "sun" -> Calendar.SUNDAY
                    "mon" -> Calendar.MONDAY
                    "tue" -> Calendar.TUESDAY
                    "wed" -> Calendar.WEDNESDAY
                    "thu" -> Calendar.THURSDAY
                    "fri" -> Calendar.FRIDAY
                    "sat" -> Calendar.SATURDAY
                    else -> Calendar.MONDAY
                }
                val currentDay = cal.get(Calendar.DAY_OF_WEEK)
                var diff = targetDay - currentDay
                if (diff <= 0) diff += 7

                val isNext = match.value.lowercase().contains("next")
                if (isNext) diff += 7

                cal.add(Calendar.DAY_OF_YEAR, diff)
                hasExplicitDate = true
                text = removeMatch(match)
            }
        }

        // 7. Ordinal day with mandatory "st/nd/rd/th" OR strict "on [the] <day>"
        // (CRITICAL: Do NOT match lone digits without "on" or ordinal suffix!)
        if (!hasExplicitDate) {
            val ordinalDayRegex = Regex("(?i)\\b(?:on\\s+(?:the\\s+)?|the\\s+)?(\\d{1,2})\\s*(?:st|nd|rd|th)\\b")
            val strictOnDayRegex = Regex("(?i)\\bon\\s+(?:the\\s+)?(\\d{1,2})\\b")

            val match = ordinalDayRegex.find(text) ?: strictOnDayRegex.find(text)
            match?.let { m ->
                val day = m.groupValues[1].toIntOrNull() ?: 1
                if (day in 1..31) {
                    val nowDay = nowCal.get(Calendar.DAY_OF_MONTH)
                    cal.set(Calendar.DAY_OF_MONTH, day)
                    if (day < nowDay) {
                        cal.add(Calendar.MONTH, 1)
                    }
                    hasExplicitDate = true
                    text = removeMatch(m)
                }
            }
        }

        // 8. Time parsing: "at 9.45 pm", "at 9:45 pm", "9:45pm", "9.45pm", "9:45 pm", "9.45 pm", "at 9pm", "9pm", "9 am", "at 14:00", "at 9"
        if (!hasExplicitTime) {
            // Check time with AM/PM (supporting ':' or '.' as separator)
            val timeAmPmRegex = Regex("(?i)\\b(?:at\\s+)?(\\d{1,2})(?:[:.](\\d{2}))?\\s*(am|pm)\\b")
            timeAmPmRegex.find(text)?.let { match ->
                var hour = match.groupValues[1].toIntOrNull() ?: 12
                val minute = match.groupValues[2].toIntOrNull() ?: 0
                val ampm = match.groupValues[3].lowercase()

                if (ampm == "pm" && hour < 12) hour += 12
                if (ampm == "am" && hour == 12) hour = 0

                cal.set(Calendar.HOUR_OF_DAY, hour)
                cal.set(Calendar.MINUTE, minute)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                hasExplicitTime = true
                text = removeMatch(match)
            } ?: run {
                // Check 24-hour time or time with separator (e.g. "at 14:30", "14.30", "at 9:45")
                val timeColonRegex = Regex("(?i)\\b(?:at\\s+)?(\\d{1,2})[:.](\\d{2})\\b")
                timeColonRegex.find(text)?.let { match ->
                    val rawHour = match.groupValues[1].toIntOrNull() ?: 12
                    val minute = match.groupValues[2].toIntOrNull() ?: 0
                    if (rawHour in 0..23 && minute in 0..59) {
                        var hour = rawHour
                        // Heuristic: if user writes "at 9.45" without am/pm, and now is 8pm (20:00), 9.45 is likely 21:45
                        if (hour in 1..11) {
                            val nowHour = nowCal.get(Calendar.HOUR_OF_DAY)
                            if (nowHour in 12..23 && (hour + 12) > nowHour) {
                                hour += 12
                            }
                        }
                        cal.set(Calendar.HOUR_OF_DAY, hour)
                        cal.set(Calendar.MINUTE, minute)
                        cal.set(Calendar.SECOND, 0)
                        cal.set(Calendar.MILLISECOND, 0)
                        hasExplicitTime = true
                        text = removeMatch(match)
                    }
                } ?: run {
                    // Check "at <hour>" (e.g. "at 9", "at 5")
                    val timeAtRegex = Regex("(?i)\\bat\\s+(\\d{1,2})\\b")
                    timeAtRegex.find(text)?.let { match ->
                        var hour = match.groupValues[1].toIntOrNull() ?: 12
                        if (hour in 1..11) {
                            val nowHour = nowCal.get(Calendar.HOUR_OF_DAY)
                            if (hour + 12 > nowHour && hour <= nowHour) {
                                hour += 12
                            } else if (hour in 1..7) {
                                hour += 12
                            }
                        }
                        cal.set(Calendar.HOUR_OF_DAY, hour)
                        cal.set(Calendar.MINUTE, 0)
                        cal.set(Calendar.SECOND, 0)
                        cal.set(Calendar.MILLISECOND, 0)
                        hasExplicitTime = true
                        text = removeMatch(match)
                    } ?: run {
                        // Named time shortcuts: "noon", "midnight"
                        val namedTimeRegex = Regex("(?i)\\b(noon|midnight)\\b")
                        namedTimeRegex.find(text)?.let { match ->
                            val word = match.groupValues[1].lowercase()
                            if (word == "noon") {
                                cal.set(Calendar.HOUR_OF_DAY, 12)
                            } else {
                                cal.set(Calendar.HOUR_OF_DAY, 0)
                            }
                            cal.set(Calendar.MINUTE, 0)
                            cal.set(Calendar.SECOND, 0)
                            cal.set(Calendar.MILLISECOND, 0)
                            hasExplicitTime = true
                            text = removeMatch(match)
                        }
                    }
                }
            }
        }

        // 9. If time was set but NO explicit date was set:
        // If the parsed time is already past today, roll over to tomorrow!
        // Otherwise keep today!
        if (hasExplicitTime && !hasExplicitDate) {
            if (cal.timeInMillis < System.currentTimeMillis()) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            hasExplicitDate = true
        }

        // 10. Clean leftover commas, punctuation, and prepositions
        var clean = text.trim()
            .replace(Regex("^[,:\\- ]+"), "")
            .replace(Regex("[,:\\- ]+$"), "")
            .replace(Regex("(?i)\\b(on|at|every|the|for|by|in)\\s*$"), "")
            .replace(Regex("^\\s*(on|at|every|the|for|by|in)\\b(?i)"), "")
            .replace(Regex("^[,:\\- ]+"), "")
            .replace(Regex("[,:\\- ]+$"), "")
            .trim()
            .replace("\\s+".toRegex(), " ")

        if (clean.isBlank()) {
            clean = input.trim()
        }

        if (hasExplicitDate && !hasExplicitTime) {
            cal.set(Calendar.HOUR_OF_DAY, 8)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
        } else if (!hasExplicitDate && !hasExplicitTime && detectedRecurrence == RecurrencePattern.NONE) {
            return ParseResult(input.trim(), null, false, RecurrencePattern.NONE)
        } else if (!hasExplicitDate && detectedRecurrence != RecurrencePattern.NONE) {
            if (!hasExplicitTime) {
                cal.set(Calendar.HOUR_OF_DAY, 8)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
            }
            if (cal.timeInMillis < System.currentTimeMillis()) {
                when (detectedRecurrence) {
                    RecurrencePattern.DAILY -> cal.add(Calendar.DAY_OF_YEAR, 1)
                    RecurrencePattern.WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 1)
                    RecurrencePattern.MONTHLY -> cal.add(Calendar.MONTH, 1)
                    RecurrencePattern.YEARLY -> cal.add(Calendar.YEAR, 1)
                    else -> {}
                }
            }
            hasExplicitDate = true
        }

        return ParseResult(clean, cal.timeInMillis, hasExplicitTime, detectedRecurrence)
    }
}
