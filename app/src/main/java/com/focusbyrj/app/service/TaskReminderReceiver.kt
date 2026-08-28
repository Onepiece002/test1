package com.focusbyrj.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.focusbyrj.app.R
import com.focusbyrj.app.ui.screens.TaskReminderPopupActivity
import com.focusbyrj.app.util.TaskReminderHelper

class TaskReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra("taskId", -1L)
        if (taskId == -1L) return

        // Acquire a temporary WakeLock (5 seconds) to ensure CPU does not sleep during notification dispatch
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "FocusByRJ:TaskReminderWakeLock"
        )
        try {
            wakeLock?.acquire(5000L)
        } catch (_: Exception) {}

        try {
            val title = intent.getStringExtra("taskTitle") ?: "Task Reminder"
        val details = intent.getStringExtra("taskDetails") ?: ""
        val dueDate = intent.getLongExtra("taskDueDate", System.currentTimeMillis())
        val typeStr = intent.getStringExtra("taskType") ?: "TASK"
        val recurrenceStr = intent.getStringExtra("taskRecurrence") ?: "NONE"
        val isPersistent = intent.getBooleanExtra("isPersistent", false)
        val isPriority = intent.getBooleanExtra("isPriority", false)

        val prefs = context.getSharedPreferences("focus_prefs", Context.MODE_PRIVATE)
        val notificationStyle = prefs.getString("task_notification_style", "Both") ?: "Both"

        val showNotification = true
        val showFloating = isPriority || notificationStyle == "Both" || notificationStyle == "Floating Bar"

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val channelId = "task_reminders"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Task Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Pop up notifications and reminders for your tasks"
                enableVibration(true)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Tap Action Intent (Direct background receiver opening floating modal in the middle of the screen)
        val popupActionIntent = Intent(context, TaskActionReceiver::class.java).apply {
            action = TaskActionReceiver.ACTION_SHOW_POPUP
            putExtra(TaskActionReceiver.EXTRA_TASK_ID, taskId)
            putExtra("taskTitle", title)
            putExtra("taskDetails", details)
            putExtra("taskDueDate", dueDate)
            putExtra("taskType", typeStr)
            putExtra("taskRecurrence", recurrenceStr)
            putExtra("isPersistent", isPersistent)
        }
        val popupPendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            popupActionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Reschedule Action Intent (Direct background receiver opening reschedule popup on home screen)
        val rescheduleActionIntent = Intent(context, TaskActionReceiver::class.java).apply {
            action = TaskActionReceiver.ACTION_OPEN_RESCHEDULE
            putExtra(TaskActionReceiver.EXTRA_TASK_ID, taskId)
            putExtra("taskTitle", title)
            putExtra("taskDetails", details)
            putExtra("taskDueDate", dueDate)
            putExtra("taskType", typeStr)
            putExtra("taskRecurrence", recurrenceStr)
            putExtra("isPersistent", isPersistent)
        }
        val reschedulePendingIntent = PendingIntent.getBroadcast(
            context,
            (taskId + 100000).toInt(),
            rescheduleActionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Complete Action Intent (Direct background receiver)
        val completeActionIntent = Intent(context, TaskActionReceiver::class.java).apply {
            action = TaskActionReceiver.ACTION_COMPLETE_TASK
            putExtra(TaskActionReceiver.EXTRA_TASK_ID, taskId)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            (taskId + 200000).toInt(),
            completeActionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Ignore Action Intent (Direct background receiver)
        val ignoreActionIntent = Intent(context, TaskActionReceiver::class.java).apply {
            action = TaskActionReceiver.ACTION_IGNORE_TASK
            putExtra(TaskActionReceiver.EXTRA_TASK_ID, taskId)
        }
        val ignorePendingIntent = PendingIntent.getBroadcast(
            context,
            (taskId + 300000).toInt(),
            ignoreActionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentSubtitle = when {
            details.isNotBlank() -> details
            isPersistent -> "Persistent Reminder • Tap to manage"
            else -> "It's time to focus on this task."
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(contentSubtitle)
            .setStyle(NotificationCompat.BigTextStyle().bigText(if (details.isNotBlank()) "$details\nTap to view or manage task." else "Scheduled task reminder is due."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(popupPendingIntent)
            .setFullScreenIntent(popupPendingIntent, true)
            .setAutoCancel(!isPersistent)
            .addAction(0, "Ignore", ignorePendingIntent)
            .addAction(0, "Reschedule", reschedulePendingIntent)
            .addAction(0, "Complete", completePendingIntent)

        if (isPersistent) {
            builder.setOngoing(true)
        }

        if (showNotification) {
            notificationManager.notify(taskId.toInt(), builder.build())
        }

        if (showFloating) {
            // Display floating modal pop-up in the middle of the screen via direct overlay
            TaskReminderOverlayManager.showReminderOverlay(
                context = context,
                taskId = taskId,
                taskTitle = title,
                taskDetails = details,
                taskDueDate = dueDate,
                taskTypeStr = typeStr,
                taskRecurrenceStr = recurrenceStr,
                isPersistent = isPersistent,
                openRescheduleInitially = false
            )
        }

        if (isPersistent) {
            val intervalMins = prefs.getInt("persistent_reminder_interval", 15)
            TaskReminderHelper.scheduleNaggingReminder(context, taskId, title, intervalMins)
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

