const fs = require('fs');
const path = './app/src/main/java/com/focusbyrj/app/ui/screens/BubbleChatActivity.kt';
let code = fs.readFileSync(path, 'utf8');

const oldBlock = `                if (sentText.lowercase().startsWith("/talk ") || sentText.lowercase().startsWith("/ask ") || sentText.lowercase().startsWith("/guide ")) {
                    val rawQuery = sentText.substringAfter(" ").trim()
                    val nluResult = com.focusbyrj.app.util.OfflineNluEngine.parse(rawQuery, pendingTasksList)
                    if (nluResult.intent == com.focusbyrj.app.util.NluIntent.LIST_TASKS) {
                        sentText = if (nluResult.isAllTasks) "/tasks all" else "/tasks"
                    }
                }`;

const newBlock = `                if (sentText.lowercase().startsWith("/talk ") || sentText.lowercase().startsWith("/ask ") || sentText.lowercase().startsWith("/guide ")) {
                    val rawQuery = sentText.substringAfter(" ").trim()
                    val nluResult = com.focusbyrj.app.util.OfflineNluEngine.parse(rawQuery, pendingTasksList)
                    when (nluResult.intent) {
                        com.focusbyrj.app.util.NluIntent.LIST_TASKS -> {
                            sentText = if (nluResult.isAllTasks) "/tasks all" else "/tasks"
                        }
                        com.focusbyrj.app.util.NluIntent.SHOW_PROFILE -> sentText = "/profile"
                        com.focusbyrj.app.util.NluIntent.SHOW_SUMMARY -> sentText = "/summary"
                        com.focusbyrj.app.util.NluIntent.START_DRILL -> sentText = "/drill"
                        com.focusbyrj.app.util.NluIntent.CLEAR_CHAT -> sentText = "/clear"
                        else -> {}
                    }
                }`;

code = code.replace(oldBlock, newBlock);
fs.writeFileSync(path, code);
