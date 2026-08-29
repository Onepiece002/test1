const fs = require('fs');
const path = 'app/src/main/java/com/focusbyrj/app/service/TaskReminderReceiver.kt';
let code = fs.readFileSync(path, 'utf8');

const targetBlock = `                        val task = app.database.taskDao().getTaskById(taskId)
                        if (task == null || task.isCompleted) {
                            // Task is completed or deleted, stop nagging!
                            TaskReminderHelper.cancelReminderById(appContext, taskId)
                            return@launch
                        }`;

const replacementBlock = `                        val task = app.database.taskDao().getTaskById(taskId)
                        if (task == null || task.isCompleted) {
                            // Task is completed or deleted, stop nagging!
                            TaskReminderHelper.cancelReminderById(appContext, taskId)
                            return@launch
                        }

                        // If the task was rescheduled to a time in the future, stop the current nagging loop.
                        // (60s buffer to account for minor alarm trigger variances)
                        if (task.dueDate != null && task.dueDate > System.currentTimeMillis() + 60000L) {
                            TaskReminderHelper.cancelReminderById(appContext, taskId)
                            return@launch
                        }`;

code = code.replace(targetBlock, replacementBlock);
fs.writeFileSync(path, code);
console.log("Successfully patched TaskReminderReceiver with dueDate check");
