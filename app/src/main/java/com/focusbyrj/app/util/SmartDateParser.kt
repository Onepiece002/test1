package com.focusbyrj.app.util

import com.focusbyrj.app.data.RecurrencePattern
import java.util.Calendar

data class ParseResult(
    val cleanText: String,
    val timestamp: Long?,
    val hasTime: Boolean,
    val recurrence: RecurrencePattern = RecurrencePattern.NONE
)

object SmartDateParser {
    fun parse(input: String): ParseResult {
        var text = input.trim()
        val cal = Calendar.getInstance()
        val nowCal = Calendar.getInstance()
        var hasDate = false
        var hasTime = false
        var detectedRecurrence = RecurrencePattern.NONE

        fun removeMatch(match: MatchResult): String {
            return text.removeRange(match.range).trim().replace("\\s+".toRegex(), " ")
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
            hasDate = true
            text = removeMatch(match)
        }

        // 2. "in X time" (e.g. in 30 mins, in 2 hours, in 3 days)
        val inRegex = Regex("(?i)\\b(in)\\s+(\\d+)\\s+(m|min|mins|minutes?|h|hr|hrs|hours?|d|days?|w|wks|weeks?)\\b")
        inRegex.find(text)?.let { match ->
            val amount = match.groupValues[2].toIntOrNull() ?: 0
            val unit = match.groupValues[3].lowercase()
            if (unit.startsWith("m")) {
                cal.add(Calendar.MINUTE, amount)
                hasTime = true
                hasDate = true
            } else if (unit.startsWith("h")) {
                cal.add(Calendar.HOUR, amount)
                hasTime = true
                hasDate = true
            } else if (unit.startsWith("d")) {
                cal.add(Calendar.DAY_OF_YEAR, amount)
                hasDate = true
            } else if (unit.startsWith("w")) {
                cal.add(Calendar.WEEK_OF_YEAR, amount)
                hasDate = true
            }
            text = removeMatch(match)
        }

        // 3. "today", "tomorrow", "tonight"
        if (!hasDate) {
            val dayRegex = Regex("(?i)\\b(today|tomorrow|tonight)\\b")
            dayRegex.find(text)?.let { match ->
                val word = match.groupValues[1].lowercase()
                if (word == "tomorrow") {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                } else if (word == "tonight") {
                    cal.set(Calendar.HOUR_OF_DAY, 20)
                    cal.set(Calendar.MINUTE, 0)
                    hasTime = true
                }
                hasDate = true
                text = removeMatch(match)
            }
        }

        // 4. Specific Month + Day (e.g., "Jan 15th", "15th of January", "on April 19 th")
        if (!hasDate) {
            val monthNames = listOf("jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec")
            val monthRegex = Regex("(?i)\\b(?:on\\s+)?(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*\\s+(\\d{1,2})(?:\\s*(?:st|nd|rd|th))?\\b")
            monthRegex.find(text)?.let { match ->
                val monthStr = match.groupValues[1].lowercase()
                val day = match.groupValues[2].toIntOrNull() ?: 1
                val monthIdx = monthNames.indexOf(monthStr)
                if (monthIdx != -1) {
                    cal.set(Calendar.MONTH, monthIdx)
                    cal.set(Calendar.DAY_OF_MONTH, day)
                    if (cal.timeInMillis < System.currentTimeMillis() - 86400000L) {
                        cal.add(Calendar.YEAR, 1)
                    }
                    hasDate = true
                    text = removeMatch(match)
                }
            } ?: run {
                val dayMonthRegex = Regex("(?i)\\b(?:on\\s+)?(\\d{1,2})(?:\\s*(?:st|nd|rd|th))?\\s+(?:of\\s+)?(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*\\b")
                dayMonthRegex.find(text)?.let { match ->
                    val day = match.groupValues[1].toIntOrNull() ?: 1
                    val monthStr = match.groupValues[2].lowercase()
                    val monthIdx = monthNames.indexOf(monthStr)
                    if (monthIdx != -1) {
                        cal.set(Calendar.MONTH, monthIdx)
                        cal.set(Calendar.DAY_OF_MONTH, day)
                        if (cal.timeInMillis < System.currentTimeMillis() - 86400000L) {
                            cal.add(Calendar.YEAR, 1)
                        }
                        hasDate = true
                        text = removeMatch(match)
                    }
                }
            }
        }

        // 5. Day-of-month ordinal with NO month specified (e.g. "on 19 th", "19th", "on the 19th", "on 19", "every 19th")
        if (!hasDate) {
            val ordinalDayRegex = Regex("(?i)\\b(?:on\\s+(?:the\\s+)?|every\\s+|the\\s+)?(\\d{1,2})\\s*(?:st|nd|rd|th)\\b")
            val onDayRegex = Regex("(?i)\\b(?:on\\s+(?:the\\s+)?)(\\d{1,2})\\b")

            val match = ordinalDayRegex.find(text) ?: onDayRegex.find(text)
            match?.let { m ->
                val day = m.groupValues[1].toIntOrNull() ?: 1
                if (day in 1..31) {
                    val nowDay = nowCal.get(Calendar.DAY_OF_MONTH)
                    cal.set(Calendar.DAY_OF_MONTH, day)
                    // If the day in the current month has already passed, default to next month!
                    if (day < nowDay) {
                        cal.add(Calendar.MONTH, 1)
                    }
                    hasDate = true
                    text = removeMatch(m)
                }
            }
        }

        // 6. Day of week (e.g. "on monday", "next friday", "this sunday")
        if (!hasDate) {
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
                hasDate = true
                text = removeMatch(match)
            }
        }

        // 7. Time parsing: "at 5pm", "5:30 am", "5pm", "10am", "at 14:00", "at 5"
        if (!hasTime) {
            val timeAmPmRegex = Regex("(?i)\\b(?:at\\s+)?(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)\\b")
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
                hasTime = true
                if (!hasDate && cal.timeInMillis < System.currentTimeMillis()) {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                    hasDate = true
                }
                text = removeMatch(match)
            } ?: run {
                val time24Regex = Regex("(?i)\\b(?:at\\s+)?(\\d{1,2}):(\\d{2})\\b")
                time24Regex.find(text)?.let { match ->
                    val hour = match.groupValues[1].toIntOrNull() ?: 12
                    val minute = match.groupValues[2].toIntOrNull() ?: 0
                    if (hour in 0..23 && minute in 0..59) {
                        cal.set(Calendar.HOUR_OF_DAY, hour)
                        cal.set(Calendar.MINUTE, minute)
                        cal.set(Calendar.SECOND, 0)
                        cal.set(Calendar.MILLISECOND, 0)
                        hasTime = true
                        if (!hasDate && cal.timeInMillis < System.currentTimeMillis()) {
                            cal.add(Calendar.DAY_OF_YEAR, 1)
                            hasDate = true
                        }
                        text = removeMatch(match)
                    }
                } ?: run {
                    val timeAtRegex = Regex("(?i)\\bat\\s+(\\d{1,2})\\b")
                    timeAtRegex.find(text)?.let { match ->
                        var hour = match.groupValues[1].toIntOrNull() ?: 12
                        if (hour in 1..7) hour += 12 // heuristics: "at 5" -> 5pm
                        cal.set(Calendar.HOUR_OF_DAY, hour)
                        cal.set(Calendar.MINUTE, 0)
                        cal.set(Calendar.SECOND, 0)
                        cal.set(Calendar.MILLISECOND, 0)
                        hasTime = true
                        if (!hasDate && cal.timeInMillis < System.currentTimeMillis()) {
                            cal.add(Calendar.DAY_OF_YEAR, 1)
                            hasDate = true
                        }
                        text = removeMatch(match)
                    }
                }
            }
        }

        // Clean trailing leftover words like "on", "at", "every", "the"
        var clean = text.trim()
            .replace(Regex("(?i)\\b(on|at|every|the|for|by|in)\\s*$"), "")
            .replace(Regex("^\\s*(on|at|every|the|for|by|in)\\b(?i)"), "")
            .trim()
            .replace("\\s+".toRegex(), " ")

        if (clean.isBlank()) {
            clean = input.trim()
        }

        if (hasDate && !hasTime) {
            cal.set(Calendar.HOUR_OF_DAY, 8)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
        } else if (!hasDate && !hasTime && detectedRecurrence == RecurrencePattern.NONE) {
            return ParseResult(input.trim(), null, false, RecurrencePattern.NONE)
        } else if (!hasDate && detectedRecurrence != RecurrencePattern.NONE) {
            // When recurrence is detected without explicit date (e.g. "workout every day"), set date to today/now
            if (!hasTime) {
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
            hasDate = true
        }

        return ParseResult(clean, cal.timeInMillis, hasTime, detectedRecurrence)
    }
}

