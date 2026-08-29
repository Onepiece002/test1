const fs = require('fs');
const path = './app/src/main/java/com/focusbyrj/app/ui/screens/BubbleChatActivity.kt';
let code = fs.readFileSync(path, 'utf8');

const oldBlock = `                if (!sentText.startsWith("/")) {
                    val nluResult = com.focusbyrj.app.util.OfflineNluEngine.parse(sentText, pendingTasksList)
                    if (nluResult.intent == com.focusbyrj.app.util.NluIntent.LIST_TASKS) {
                        sentText = if (nluResult.isAllTasks) "/tasks all" else "/tasks"
                    } else if (nluResult.intent == com.focusbyrj.app.util.NluIntent.BLOCK_APP || nluResult.intent == com.focusbyrj.app.util.NluIntent.BLOCK_FILTER || nluResult.intent == com.focusbyrj.app.util.NluIntent.UNBLOCK) {
                        sentText = "/talk $sentText"
                    } else if (nluResult.intent != com.focusbyrj.app.util.NluIntent.UNKNOWN) {
                        sentText = "/talk $sentText"
                    }
                }`;

const newBlock = `                if (!sentText.startsWith("/")) {
                    val nluResult = com.focusbyrj.app.util.OfflineNluEngine.parse(sentText, pendingTasksList)
                    when (nluResult.intent) {
                        com.focusbyrj.app.util.NluIntent.LIST_TASKS -> {
                            sentText = if (nluResult.isAllTasks) "/tasks all" else "/tasks"
                        }
                        com.focusbyrj.app.util.NluIntent.SHOW_PROFILE -> sentText = "/profile"
                        com.focusbyrj.app.util.NluIntent.SHOW_SUMMARY -> sentText = "/summary"
                        com.focusbyrj.app.util.NluIntent.START_DRILL -> sentText = "/drill"
                        com.focusbyrj.app.util.NluIntent.CLEAR_CHAT -> sentText = "/clear"
                        com.focusbyrj.app.util.NluIntent.UNKNOWN -> {}
                        else -> sentText = "/talk $sentText"
                    }
                }`;

code = code.replace(oldBlock, newBlock);
fs.writeFileSync(path, code);
