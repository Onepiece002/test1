const fs = require('fs');
const path = 'app/src/main/java/com/focusbyrj/app/util/TaskReminderHelper.kt';
let code = fs.readFileSync(path, 'utf8');

const targetStr = `val intent = Intent(context, TaskReminderReceiver::class.java).apply {
                putExtra("taskId", taskId)
                putExtra("taskTitle", title)
                putExtra("isPersistent", true)
            }`;

const replacementStr = `val intent = Intent(context, TaskReminderReceiver::class.java).apply {
                data = android.net.Uri.parse("focusreminder://task/\${taskId}")
                putExtra("taskId", taskId)
                putExtra("taskTitle", title)
                putExtra("isPersistent", true)
            }`;

// Need to replace all occurrences of this intent creation in scheduleNaggingReminder
// There are two occurrences due to the .onFailure block
code = code.split(targetStr).join(replacementStr);
fs.writeFileSync(path, code);
