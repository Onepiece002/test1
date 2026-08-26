package com.focusbyrj.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.focusbyrj.app.MainActivity
import com.focusbyrj.app.R

class TaskReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra("taskId", -1)
        val title = intent.getStringExtra("taskTitle") ?: "Task Reminder"
        val isPersistent = intent.getBooleanExtra("isPersistent", false)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val channelId = "task_reminders"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Task Reminders",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val activityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            taskId.toInt(),
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_app_logo) // replace with actual icon if needed
            .setContentTitle(title)
            .setContentText("It's time to focus on this task.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(!isPersistent)
            
        if (isPersistent) {
            builder.setOngoing(true)
        }

        notificationManager.notify(taskId.toInt(), builder.build())
        
        if (isPersistent) {
            val prefs = context.getSharedPreferences("focus_prefs", Context.MODE_PRIVATE)
            val intervalMins = prefs.getInt("persistent_reminder_interval", 15)
            com.focusbyrj.app.util.TaskReminderHelper.scheduleNaggingReminder(context, taskId, title, intervalMins)
        }
    }
}
