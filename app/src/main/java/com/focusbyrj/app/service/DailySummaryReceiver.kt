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

package com.focusbyrj.app.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.focusbyrj.app.FocusApplication
import com.focusbyrj.app.util.BubbleChatManager
import com.focusbyrj.app.util.FocusQuotes
import com.focusbyrj.app.util.PersistedChatMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

class DailySummaryReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_MORNING_SUMMARY = "com.focusbyrj.app.ACTION_MORNING_SUMMARY"
        const val ACTION_EVENING_SUMMARY = "com.focusbyrj.app.ACTION_EVENING_SUMMARY"
        private const val REQUEST_CODE_MORNING = 2001
        private const val REQUEST_CODE_EVENING = 2002

        fun scheduleDailySummaries(context: Context) {
            val prefs = context.getSharedPreferences("bubble_prefs", Context.MODE_PRIVATE)
            val morningTimeStr = prefs.getString("morning_brief_time", "08:00 AM") ?: "08:00 AM"
            val eveningTimeStr = prefs.getString("evening_brief_time", "09:00 PM") ?: "09:00 PM"

            scheduleSingleSummary(context, morningTimeStr, ACTION_MORNING_SUMMARY, REQUEST_CODE_MORNING)
            scheduleSingleSummary(context, eveningTimeStr, ACTION_EVENING_SUMMARY, REQUEST_CODE_EVENING)
        }

        private fun scheduleSingleSummary(
            context: Context,
            timeStr: String,
            action: String,
            requestCode: Int
        ) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val triggerTime = computeNextTriggerTime(timeStr)

            val intent = Intent(context, DailySummaryReceiver::class.java).apply {
                this.action = action
            }

            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags)

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                    } else {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            } catch (_: Exception) {}
        }

        private fun computeNextTriggerTime(timeStr: String): Long {
            val now = Calendar.getInstance()
            val target = Calendar.getInstance()

            var hour = 8
            var minute = 0
            try {
                val parts = timeStr.trim().split(":", " ")
                if (parts.size >= 3) {
                    var h = parts[0].toInt()
                    val m = parts[1].toInt()
                    val amPm = parts[2]
                    if (amPm.equals("PM", true) && h != 12) h += 12
                    if (amPm.equals("AM", true) && h == 12) h = 0
                    hour = h
                    minute = m
                }
            } catch (_: Exception) {}

            target.set(Calendar.HOUR_OF_DAY, hour)
            target.set(Calendar.MINUTE, minute)
            target.set(Calendar.SECOND, 0)
            target.set(Calendar.MILLISECOND, 0)

            if (target.timeInMillis <= now.timeInMillis) {
                target.add(Calendar.DAY_OF_YEAR, 1)
            }
            return target.timeInMillis
        }
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val app = context.applicationContext as? FocusApplication ?: return

        CoroutineScope(Dispatchers.IO).launch {
            when (action) {
                ACTION_MORNING_SUMMARY -> {
                    handleMorningSummary(context, app)
                    // Reschedule next morning
                    scheduleDailySummaries(context)
                }
                ACTION_EVENING_SUMMARY -> {
                    handleEveningSummary(context, app)
                    // Reschedule next evening
                    scheduleDailySummaries(context)
                }
            }
        }
    }

    private suspend fun handleMorningSummary(context: Context, app: FocusApplication) {
        val tasks = app.taskRepository.allTasks.firstOrNull() ?: emptyList()
        val pendingTasks = tasks.filter { !it.isCompleted }
        val priorityTasks = pendingTasks.filter { it.isPriority }

        val quote = com.focusbyrj.app.util.SummaryQuotes.getNextMorningQuote(context)

        val sb = StringBuilder()
        sb.append("☀️ *__Morning Focus Briefing__*\n")
        sb.append("_Good morning! Here's what's up for today:_\n\n")

        if (pendingTasks.isEmpty()) {
            sb.append("📋 *Today's Agenda*: You have no tasks scheduled yet.\n_Type a task into the chat to add it!_\n")
        } else {
            sb.append("📋 *Active Tasks (${pendingTasks.size})*:\n")
            if (priorityTasks.isNotEmpty()) {
                sb.append("⭐ *Priority Focus (${priorityTasks.size})*:\n")
                priorityTasks.take(5).forEachIndexed { index, task ->
                    sb.append("  ${index + 1}. ⚡ ${task.title}\n")
                }
            }
            sb.append("\n📝 *Task List*:\n")
            pendingTasks.take(8).forEachIndexed { index, task ->
                val prio = if (task.isPriority) "[High Priority] " else ""
                sb.append("${index + 1}. $prio${task.title}\n")
            }
            if (pendingTasks.size > 8) {
                sb.append("_...and ${pendingTasks.size - 8} more tasks_\n")
            }
        }

        sb.append("\n💡 _\"$quote\"_")

        val message = PersistedChatMessage(
            id = "morning_${System.currentTimeMillis()}",
            text = sb.toString().trim(),
            isUser = false,
            timestamp = System.currentTimeMillis()
        )

        BubbleChatManager.addMessage(context, message, incrementBadge = true)
    }

    private suspend fun handleEveningSummary(context: Context, app: FocusApplication) {
        val tasks = app.taskRepository.allTasks.firstOrNull() ?: emptyList()
        val completedToday = tasks.filter { it.isCompleted }
        val pendingTasks = tasks.filter { !it.isCompleted }

        val sb = StringBuilder()
        sb.append("🌙 *__Evening Wrap-Up & Reflection__*\n")
        sb.append("_Here is your daily focus breakdown:_\n\n")

        sb.append("✅ *Completed Tasks*: ${completedToday.size}\n")
        sb.append("⏳ *Remaining Tasks*: ${pendingTasks.size}\n\n")

        if (pendingTasks.isNotEmpty()) {
            sb.append("📋 *Carried Forward to Tomorrow*:\n")
            pendingTasks.take(5).forEachIndexed { index, task ->
                sb.append("${index + 1}. ${task.title}\n")
            }
            if (pendingTasks.size > 5) {
                sb.append("_...and ${pendingTasks.size - 5} more_\n")
            }
        } else {
            sb.append("🎉 *Flawless execution! All tasks completed today.*\n")
        }

        val quote = com.focusbyrj.app.util.SummaryQuotes.getNextEveningQuote(context)
        sb.append("\n💡 _\"$quote\"_")

        val message = PersistedChatMessage(
            id = "evening_${System.currentTimeMillis()}",
            text = sb.toString().trim(),
            isUser = false,
            timestamp = System.currentTimeMillis()
        )

        BubbleChatManager.addMessage(context, message, incrementBadge = true)
    }
}
