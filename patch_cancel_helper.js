const fs = require('fs');
const path = 'app/src/main/java/com/focusbyrj/app/util/TaskReminderHelper.kt';
let code = fs.readFileSync(path, 'utf8');

const oldCancelStr = `    fun cancelReminderById(context: Context, taskId: Long) {
        kotlin.runCatching {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, TaskReminderReceiver::class.java).apply {
                data = android.net.Uri.parse("focusreminder://task/\${taskId}")
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                taskId.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }`;

const newCancelStr = `    fun cancelReminderById(context: Context, taskId: Long) {
        kotlin.runCatching {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            
            // 1. Cancel with Data URI (new standard)
            val intentWithData = Intent(context, TaskReminderReceiver::class.java).apply {
                data = android.net.Uri.parse("focusreminder://task/\${taskId}")
            }
            val pendingWithData = PendingIntent.getBroadcast(
                context,
                taskId.toInt(),
                intentWithData,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingWithData)
            
            // 2. Cancel without Data URI (legacy fallback for any older lingering alarms)
            val intentLegacy = Intent(context, TaskReminderReceiver::class.java).apply {
                putExtra("taskId", taskId)
                putExtra("isPersistent", true)
            }
            val pendingLegacy = PendingIntent.getBroadcast(
                context,
                taskId.toInt(),
                intentLegacy,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingLegacy)
        }
    }`;

code = code.replace(oldCancelStr, newCancelStr);
fs.writeFileSync(path, code);
console.log("Successfully patched cancelReminderById");
