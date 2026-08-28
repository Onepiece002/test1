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

        // 2. "in X time" (e.g. in 30 mins, in 2 hours, in 3 days, in 2 months, in 5 years)
        val inRegex = Regex("(?i)\\b(in)\\s+(\\d+)\\s+(m|min|mins|minutes?|h|hr|hrs|hours?|d|days?|w|wks|weeks?|mo|mos|months?|y|yr|yrs|years?)\\b")
        inRegex.find(text)?.let { match ->
            val amount = match.groupValues[2].toIntOrNull() ?: 0
            val unit = match.groupValues[3].lowercase()
            if (unit.startsWith("mo") || unit.contains("month")) {
                cal.add(Calendar.MONTH, amount)
                hasDate = true
            } else if (unit.startsWith("y") || unit.contains("year")) {
                cal.add(Calendar.YEAR, amount)
                hasDate = true
            } else if (unit.startsWith("m")) {
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

        // 3. "today", "tomorrow", "tonight", "next year"
        if (!hasDate) {
            val dayRegex = Regex("(?i)\\b(today|tomorrow|tonight|next\\s+year)\\b")
            dayRegex.find(text)?.let { match ->
                val word = match.groupValues[1].lowercase()
                if (word == "tomorrow") {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                } else if (word == "tonight") {
                    cal.set(Calendar.HOUR_OF_DAY, 20)
                    cal.set(Calendar.MINUTE, 0)
                    hasTime = true
                } else if (word.contains("year")) {
                    cal.add(Calendar.YEAR, 1)
                }
                hasDate = true
                text = removeMatch(match)
            }
        }

        // 4. Numeric standard dates (e.g. 2033-05-20, 2033/05/20, 20/05/2033, 05/20/2033)
        if (!hasDate) {
            val isoRegex = Regex("(?i)\\b((?:19|20|21)\\d{2})[-/.](0?[1-9]|1[0-2])[-/.](0?[1-9]|[12]\\d|3[01])\\b")
            isoRegex.find(text)?.let { match ->
                val year = match.groupValues[1].toIntOrNull()
                val month = match.groupValues[2].toIntOrNull()
                val day = match.groupValues[3].toIntOrNull()
                if (year != null && month != null && day != null) {
                    cal.set(Calendar.YEAR, year)
                    cal.set(Calendar.MONTH, month - 1)
                    cal.set(Calendar.DAY_OF_MONTH, day)
                    hasDate = true
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
                            // Default to month/day/year
                            cal.set(Calendar.MONTH, (num1 - 1).coerceIn(0, 11))
                            cal.set(Calendar.DAY_OF_MONTH, num2)
                        }
                        hasDate = true
                        text = removeMatch(match)
                    }
                }
            }
        }

        // 5. Specific Month + Day with optional Year (e.g., "may 20 2033", "may 20, 2033", "Jan 15th 2027", "15th of January 2030", "on April 19 th 2033")
        if (!hasDate) {
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
                    hasDate = true
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
                        hasDate = true
                        text = removeMatch(match)
                    }
                } ?: run {
                    // Month + Year with no day (e.g. "in May 2033", "May 2033")
                    val monthYearRegex = Regex("(?i)\\b(?:in\\s+|on\\s+)?(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*[\\s,]+((?:19|20|21)\\d{2})\\b")
                    monthYearRegex.find(text)?.let { match ->
                        val monthStr = match.groupValues[1].lowercase()
                        val year = match.groupValues[2].toIntOrNull()
                        val monthIdx = monthNames.indexOf(monthStr)
                        if (monthIdx != -1 && year != null) {
                            cal.set(Calendar.YEAR, year)
                            cal.set(Calendar.MONTH, monthIdx)
                            cal.set(Calendar.DAY_OF_MONTH, 1)
                            hasDate = true
                            text = removeMatch(match)
                        }
                    }
                }
            }
        }

        // 6. Day-of-month ordinal with NO month specified (e.g. "on 19 th", "19th", "on the 19th", "on 19", "every 19th")
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

        // 7. Day of week (e.g. "on monday", "next friday", "this sunday")
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

        // 8. Time parsing: "at 5pm", "5:30 am", "5.45 pm", "5pm", "10am", "at 14:00", "at 5"
        if (!hasTime) {
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
                hasTime = true
                
                if (!hasDate && cal.timeInMillis < System.currentTimeMillis()) {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                    hasDate = true
                }
                text = removeMatch(match)
            } ?: run {
                val time24Regex = Regex("(?i)\\b(?:at\\s+)?(\\d{1,2})[:.](\\d{2})\\b")
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
                        
                        // Smart heuristic for "at 9" when it's currently 8 PM
                        // If adding 12 puts it in the near future today, assume PM.
                        if (hour in 1..11) {
                            val nowHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                            if (hour + 12 > nowHour && hour <= nowHour) {
                                hour += 12
                            } else if (hour in 1..7) {
                                hour += 12 // General heuristic: 1-7 is usually PM (e.g. 5pm)
                            }
                        }
                        
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


