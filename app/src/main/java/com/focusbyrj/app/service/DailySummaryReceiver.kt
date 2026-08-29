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

            // Intent to open app when alarm triggers
            val showIntent = Intent(context, com.focusbyrj.app.MainActivity::class.java).apply {
                this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val showPendingIntent = PendingIntent.getActivity(context, requestCode + 100, showIntent, flags)

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, showPendingIntent)
                    alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
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
        
        // Acquire WakeLock to prevent CPU from sleeping while generating summary
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
        val wakeLock = powerManager?.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "FocusByRJ:SummaryWakeLock")
        try {
            wakeLock?.acquire(10000L) // 10 seconds to generate and post summary
        } catch (_: Exception) {}

        CoroutineScope(Dispatchers.IO).launch {
            try {
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
            } finally {
                try {
                    if (wakeLock != null && wakeLock.isHeld) {
                        wakeLock.release()
                    }
                } catch (_: Exception) {}
            }
        }
    }

    private suspend fun handleMorningSummary(context: Context, app: FocusApplication) {
        val tasks = app.taskRepository.allTasks.firstOrNull() ?: emptyList()
        val pendingTasks = tasks.filter { !it.isCompleted }

        val quote = com.focusbyrj.app.util.SummaryQuotes.getNextMorningQuote(context)

        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)

        val sb = StringBuilder()
        sb.append(com.focusbyrj.app.util.AyvaDialogueEngine.getSummaryGreeting(context, false, hour, dayOfWeek)).append("\n\n")

        if (pendingTasks.isEmpty()) {
            sb.append("📋 *Today's Agenda*: ${com.focusbyrj.app.util.AyvaDialogueEngine.getEmptyDayMessage(context)}\n_Type a task in chat if you want to put something on my radar!_\n")
        } else {
            val sortedPending = pendingTasks.sortedWith(compareByDescending<com.focusbyrj.app.data.Task> { it.isPriority }.thenBy { it.dueDate ?: Long.MAX_VALUE })
            sb.append("📋 *All Active Tasks (${sortedPending.size})*:\n")
            sortedPending.forEachIndexed { index, task ->
                val prio = if (task.isPriority) "🔥 " else ""
                val dueStr = if (task.dueDate != null) " _(Due: ${com.focusbyrj.app.util.SmartDateParser.formatDueDate(task.dueDate)})_" else ""
                sb.append("${index + 1}. $prio${task.title}$dueStr\n")
            }
        }

        sb.append("\n💡 _\"$quote\"_")

        if (pendingTasks.isNotEmpty()) {
            sb.append("\n\n").append(com.focusbyrj.app.util.AyvaDialogueEngine.getReschedulePrompt(context))
        }

        val message = PersistedChatMessage(
            id = "morning_${System.currentTimeMillis()}",
            text = sb.toString().trim(),
            isUser = false,
            timestamp = System.currentTimeMillis(),
            isTaskSummary = pendingTasks.isNotEmpty()
        )

        BubbleChatManager.addMessage(context, message, incrementBadge = true)
    }

    private suspend fun handleEveningSummary(context: Context, app: FocusApplication) {
        val tasks = app.taskRepository.allTasks.firstOrNull() ?: emptyList()
        val completedToday = tasks.filter { it.isCompleted }
        val pendingTasks = tasks.filter { !it.isCompleted }

        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)

        val sb = StringBuilder()
        sb.append(com.focusbyrj.app.util.AyvaDialogueEngine.getSummaryGreeting(context, false, hour, dayOfWeek)).append("\n\n")

        sb.append("✅ *Completed Tasks*: ${completedToday.size}\n")
        sb.append("⏳ *Remaining Tasks*: ${pendingTasks.size}\n\n")

        if (pendingTasks.isNotEmpty()) {
            val sortedPending = pendingTasks.sortedWith(compareByDescending<com.focusbyrj.app.data.Task> { it.isPriority }.thenBy { it.dueDate ?: Long.MAX_VALUE })
            sb.append("📋 *Carried Forward to Tomorrow (${sortedPending.size})*:\n")
            sortedPending.forEachIndexed { index, task ->
                val prio = if (task.isPriority) "🔥 " else ""
                val dueStr = if (task.dueDate != null) " _(Due: ${com.focusbyrj.app.util.SmartDateParser.formatDueDate(task.dueDate)})_" else ""
                sb.append("${index + 1}. $prio${task.title}$dueStr\n")
            }
        } else {
            sb.append("🎉 *Flawless execution! You crushed every single task today.*\n")
        }

        val quote = com.focusbyrj.app.util.SummaryQuotes.getNextEveningQuote(context)
        sb.append("\n💡 _\"$quote\"_")

        if (pendingTasks.isNotEmpty()) {
            sb.append("\n\n").append(com.focusbyrj.app.util.AyvaDialogueEngine.getReschedulePrompt(context))
        }

        val message = PersistedChatMessage(
            id = "evening_${System.currentTimeMillis()}",
            text = sb.toString().trim(),
            isUser = false,
            timestamp = System.currentTimeMillis(),
            isTaskSummary = pendingTasks.isNotEmpty()
        )

        BubbleChatManager.addMessage(context, message, incrementBadge = true)
    }
}
