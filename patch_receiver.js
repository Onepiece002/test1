const fs = require('fs');
const path = 'app/src/main/java/com/focusbyrj/app/service/TaskReminderReceiver.kt';
let code = fs.readFileSync(path, 'utf8');

// Add imports for Coroutines and FocusApplication
const newImports = `
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.focusbyrj.app.FocusApplication
`;

code = code.replace('import com.focusbyrj.app.util.TaskReminderHelper', 'import com.focusbyrj.app.util.TaskReminderHelper\n' + newImports);

// Replace onReceive method body
const newBody = `    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra("taskId", -1L)
        if (taskId == -1L) return

        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Acquire a temporary WakeLock (5 seconds) to ensure CPU does not sleep during notification dispatch
                val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
                val wakeLock = powerManager?.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "FocusByRJ:TaskReminderWakeLock"
                )
                try {
                    wakeLock?.acquire(5000L)
                } catch (_: Exception) {}

                try {
                    val app = appContext as? FocusApplication
                    var dbTitle = intent.getStringExtra("taskTitle") ?: "Task Reminder"
                    var dbDetails = intent.getStringExtra("taskDetails") ?: ""
                    var dbDueDate = intent.getLongExtra("taskDueDate", System.currentTimeMillis())
                    var dbTypeStr = intent.getStringExtra("taskType") ?: "TASK"
                    var dbRecurrenceStr = intent.getStringExtra("taskRecurrence") ?: "NONE"
                    var dbIsPersistent = intent.getBooleanExtra("isPersistent", false)
                    var dbIsPriority = intent.getBooleanExtra("isPriority", false)

                    if (app != null) {
                        val task = app.database.taskDao().getTaskById(taskId)
                        if (task == null || task.isCompleted) {
                            // Task is completed or deleted, stop nagging!
                            TaskReminderHelper.cancelReminderById(appContext, taskId)
                            return@launch
                        }
                        
                        dbTitle = task.title
                        dbDetails = task.details
                        dbDueDate = task.dueDate ?: dbDueDate
                        dbTypeStr = task.type.name
                        dbRecurrenceStr = task.recurrence.name
                        dbIsPersistent = task.isPersistent
                        dbIsPriority = task.isPriority
                    }

                    val title = dbTitle
                    val details = dbDetails
                    val dueDate = dbDueDate
                    val typeStr = dbTypeStr
                    val recurrenceStr = dbRecurrenceStr
                    val isPersistent = dbIsPersistent
                    val isPriority = dbIsPriority

                    val prefs = appContext.getSharedPreferences("focus_prefs", Context.MODE_PRIVATE)
                    val notificationStyle = prefs.getString("task_notification_style", "Both") ?: "Both"
                    val showNotification = true
                    val showFloating = isPriority || notificationStyle == "Both" || notificationStyle == "Floating Bar"

                    val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    
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
                    val popupActionIntent = Intent(appContext, TaskActionReceiver::class.java).apply {
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
                        appContext,
                        taskId.toInt(),
                        popupActionIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    // Reschedule Action Intent (Direct background receiver opening reschedule popup on home screen)
                    val rescheduleActionIntent = Intent(appContext, TaskActionReceiver::class.java).apply {
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
                        appContext,
                        (taskId + 100000).toInt(),
                        rescheduleActionIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    // Complete Action Intent (Direct background receiver)
                    val completeActionIntent = Intent(appContext, TaskActionReceiver::class.java).apply {
                        action = TaskActionReceiver.ACTION_COMPLETE_TASK
                        putExtra(TaskActionReceiver.EXTRA_TASK_ID, taskId)
                    }
                    val completePendingIntent = PendingIntent.getBroadcast(
                        appContext,
                        (taskId + 200000).toInt(),
                        completeActionIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    // Ignore Action Intent (Direct background receiver)
                    val ignoreActionIntent = Intent(appContext, TaskActionReceiver::class.java).apply {
                        action = TaskActionReceiver.ACTION_IGNORE_TASK
                        putExtra(TaskActionReceiver.EXTRA_TASK_ID, taskId)
                    }
                    val ignorePendingIntent = PendingIntent.getBroadcast(
                        appContext,
                        (taskId + 300000).toInt(),
                        ignoreActionIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    val contentSubtitle = when {
                        details.isNotBlank() -> details
                        isPersistent -> "Persistent Reminder • Tap to manage"
                        else -> "It's time to focus on this task."
                    }

                    val builder = NotificationCompat.Builder(appContext, channelId)
                        .setSmallIcon(R.mipmap.ic_launcher_round)
                        .setContentTitle(title)
                        .setContentText(contentSubtitle)
                        .setStyle(NotificationCompat.BigTextStyle().bigText(if (details.isNotBlank()) "$details\\nTap to view or manage task." else "Scheduled task reminder is due."))
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
                            context = appContext,
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
                        TaskReminderHelper.scheduleNaggingReminder(appContext, taskId, title, intervalMins)
                    }

                } finally {
                    try {
                        if (wakeLock != null && wakeLock.isHeld) {
                            wakeLock.release()
                        }
                    } catch (_: Exception) {}
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
`;

const startIndex = code.indexOf('    override fun onReceive(context: Context, intent: Intent) {');
const endIndex = code.lastIndexOf('}') + 1; // get the last closing brace of the class
if (startIndex !== -1) {
    // find the matching closing brace for the class
    // Wait, the file ends with the class body closing brace. 
    // We can just replace from startIndex to the end of file with newBody + '}'
    code = code.substring(0, startIndex) + newBody + '}\n';
    fs.writeFileSync(path, code);
    console.log("Successfully patched TaskReminderReceiver.kt");
} else {
    console.log("Could not find onReceive method");
}
