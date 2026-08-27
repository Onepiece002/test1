package com.focusbyrj.app.util

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.focusbyrj.app.FocusApplication
import com.focusbyrj.app.data.RecurrencePattern
import com.focusbyrj.app.data.Task
import com.focusbyrj.app.service.TaskReminderReceiver
import com.focusbyrj.app.widget.TodoWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

object TaskReminderHelper {

    fun scheduleReminder(context: Context, task: Task) {
        if (task.dueDate == null || task.isCompleted) return

        kotlin.runCatching {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, TaskReminderReceiver::class.java).apply {
                putExtra("taskId", task.id)
                putExtra("taskTitle", task.title)
                putExtra("taskDetails", task.details)
                putExtra("taskDueDate", task.dueDate)
                putExtra("taskType", task.type.name)
                putExtra("taskRecurrence", task.recurrence.name)
                putExtra("isPersistent", task.isPersistent)
                putExtra("isPriority", task.isPriority)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                task.id.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, task.dueDate, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, task.dueDate, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, task.dueDate, pendingIntent)
            }
        }.onFailure {
            // Safe fallback
            kotlin.runCatching {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
                val intent = Intent(context, TaskReminderReceiver::class.java).apply {
                    putExtra("taskId", task.id)
                    putExtra("taskTitle", task.title)
                    putExtra("taskDetails", task.details)
                    putExtra("taskDueDate", task.dueDate)
                    putExtra("taskType", task.type.name)
                    putExtra("taskRecurrence", task.recurrence.name)
                    putExtra("isPersistent", task.isPersistent)
                    putExtra("isPriority", task.isPriority)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    task.id.toInt(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.set(AlarmManager.RTC_WAKEUP, task.dueDate, pendingIntent)
            }
        }
    }

    fun cancelReminder(context: Context, task: Task) {
        cancelReminderById(context, task.id)
    }

    fun cancelReminderById(context: Context, taskId: Long) {
        kotlin.runCatching {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, TaskReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                taskId.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }

    fun completeTask(context: Context, taskId: Long, onDone: (() -> Unit)? = null) {
        val app = context.applicationContext as? FocusApplication ?: return
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(taskId.toInt())
        cancelReminderById(context, taskId)
        com.focusbyrj.app.service.TaskReminderOverlayManager.hideOverlay()

        CoroutineScope(Dispatchers.IO).launch {
            kotlin.runCatching {
                val taskDao = app.database.taskDao()
                val existing = taskDao.getTaskById(taskId)
                if (existing != null && !existing.isCompleted) {
                    val updated = existing.copy(isCompleted = true, completedAt = System.currentTimeMillis())
                    taskDao.updateTask(updated)
                    if (updated.recurrence != RecurrencePattern.NONE) {
                        val nextTask = generateNextRecurringTask(updated)
                        val newId = taskDao.insertTask(nextTask)
                        scheduleReminder(context, nextTask.copy(id = newId))
                    }
                    TodoWidgetProvider.updateAllWidgets(context)
                }
            }
            if (onDone != null) {
                withContext(Dispatchers.Main) {
                    kotlin.runCatching { onDone.invoke() }
                }
            }
        }
    }

    fun rescheduleTask(context: Context, taskId: Long, newDueDate: Long, onDone: (() -> Unit)? = null) {
        val app = context.applicationContext as? FocusApplication ?: return
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(taskId.toInt())
        cancelReminderById(context, taskId)
        com.focusbyrj.app.service.TaskReminderOverlayManager.hideOverlay()

        CoroutineScope(Dispatchers.IO).launch {
            kotlin.runCatching {
                val taskDao = app.database.taskDao()
                val existing = taskDao.getTaskById(taskId)
                if (existing != null) {
                    val updated = existing.copy(dueDate = newDueDate, isCompleted = false, completedAt = null)
                    taskDao.updateTask(updated)
                    scheduleReminder(context, updated)
                    TodoWidgetProvider.updateAllWidgets(context)
                }
            }
            if (onDone != null) {
                withContext(Dispatchers.Main) {
                    kotlin.runCatching { onDone.invoke() }
                }
            }
        }
    }

    fun ignoreTask(context: Context, taskId: Long) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(taskId.toInt())
        cancelReminderById(context, taskId)
        com.focusbyrj.app.service.TaskReminderOverlayManager.hideOverlay()
    }

    fun parseSmartDateTime(input: String): Long? {
        val lowerInput = input.lowercase(java.util.Locale.getDefault()).trim()
        val cal = Calendar.getInstance()
        
        // Pattern 1: Relative time ("in 2 hours", "15 mins")
        val relativePattern = java.util.regex.Pattern.compile("(?:in\\s+)?(\\d+)\\s*(m|min|mins|minute|minutes|h|hr|hrs|hour|hours|d|day|days)")
        val relMatcher = relativePattern.matcher(lowerInput)
        if (relMatcher.find()) {
            val amount = relMatcher.group(1)?.toIntOrNull() ?: return null
            val unitStr = relMatcher.group(2) ?: ""
            when {
                unitStr.startsWith("m") -> cal.add(Calendar.MINUTE, amount)
                unitStr.startsWith("h") -> cal.add(Calendar.HOUR, amount)
                unitStr.startsWith("d") -> cal.add(Calendar.DAY_OF_YEAR, amount)
            }
            return cal.timeInMillis
        }

        // Pattern 2: Tomorrow absolute time ("tomorrow 5pm", "tomorrow at 10")
        val tomorrowPattern = java.util.regex.Pattern.compile("tomorrow(?:\\s+(?:at\\s+)?(\\d+)(?::(\\d+))?\\s*(am|pm)?)?")
        val tomMatcher = tomorrowPattern.matcher(lowerInput)
        if (tomMatcher.find()) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
            val hourStr = tomMatcher.group(1)
            if (hourStr != null) {
                var hour = hourStr.toIntOrNull() ?: 9
                val min = tomMatcher.group(2)?.toIntOrNull() ?: 0
                val ampm = tomMatcher.group(3)
                if (ampm == "pm" && hour < 12) hour += 12
                if (ampm == "am" && hour == 12) hour = 0
                cal.set(Calendar.HOUR_OF_DAY, hour)
                cal.set(Calendar.MINUTE, min)
                cal.set(Calendar.SECOND, 0)
            } else {
                cal.set(Calendar.HOUR_OF_DAY, 9)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
            }
            return cal.timeInMillis
        }
        
        // Pattern 3: Absolute time today/tomorrow ("5pm", "at 10:30", "15:00")
        val timePattern = java.util.regex.Pattern.compile("(?:at\\s+)?(\\d+)(?::(\\d+))?\\s*(am|pm)?")
        val timeMatcher = timePattern.matcher(lowerInput)
        if (timeMatcher.find()) {
            var hour = timeMatcher.group(1)?.toIntOrNull() ?: return null
            val min = timeMatcher.group(2)?.toIntOrNull() ?: 0
            val ampm = timeMatcher.group(3)
            
            if (ampm == "pm" && hour < 12) hour += 12
            if (ampm == "am" && hour == 12) hour = 0
            
            // Smart default for am/pm if omitted
            if (ampm == null && hour < 12) {
                val currentHour24 = cal.get(Calendar.HOUR_OF_DAY)
                if (currentHour24 >= 12 && hour + 12 > currentHour24) {
                    hour += 12 // Assumed PM
                }
            }
            
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, min)
            cal.set(Calendar.SECOND, 0)
            
            // if time has already passed today, schedule for tomorrow
            if (cal.timeInMillis <= System.currentTimeMillis()) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            return cal.timeInMillis
        }
        
        return null
    }

    fun generateNextRecurringTask(completedTask: Task): Task {
        if (completedTask.dueDate == null) return completedTask
        
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = completedTask.dueDate

        when (completedTask.recurrence) {
            RecurrencePattern.DAILY -> calendar.add(Calendar.DAY_OF_YEAR, 1)
            RecurrencePattern.WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            RecurrencePattern.MONTHLY -> calendar.add(Calendar.MONTH, 1)
            RecurrencePattern.YEARLY -> calendar.add(Calendar.YEAR, 1)
            RecurrencePattern.NONE -> return completedTask
        }

        return completedTask.copy(
            id = 0, // new task
            isCompleted = false,
            dueDate = calendar.timeInMillis
        )
    }

    fun scheduleNaggingReminder(context: Context, taskId: Long, title: String, intervalMins: Int) {
        val triggerTime = System.currentTimeMillis() + (intervalMins * 60 * 1000L)
        kotlin.runCatching {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, TaskReminderReceiver::class.java).apply {
                putExtra("taskId", taskId)
                putExtra("taskTitle", title)
                putExtra("isPersistent", true)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                taskId.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        }.onFailure {
            kotlin.runCatching {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
                val intent = Intent(context, TaskReminderReceiver::class.java).apply {
                    putExtra("taskId", taskId)
                    putExtra("taskTitle", title)
                    putExtra("isPersistent", true)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    taskId.toInt(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        }
    }
}
