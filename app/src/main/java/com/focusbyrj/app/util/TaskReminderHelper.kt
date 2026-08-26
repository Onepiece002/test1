package com.focusbyrj.app.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.focusbyrj.app.data.RecurrencePattern
import com.focusbyrj.app.data.Task
import com.focusbyrj.app.service.TaskReminderReceiver
import java.util.Calendar

object TaskReminderHelper {

    fun scheduleReminder(context: Context, task: Task) {
        if (task.dueDate == null || task.isCompleted) return

        kotlin.runCatching {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, TaskReminderReceiver::class.java).apply {
                putExtra("taskId", task.id)
                putExtra("taskTitle", task.title)
                putExtra("isPersistent", task.isPersistent)
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
                    putExtra("isPersistent", task.isPersistent)
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
        kotlin.runCatching {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, TaskReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                task.id.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
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
