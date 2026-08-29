const fs = require('fs');
const path = './app/src/main/java/com/focusbyrj/app/ui/screens/BubbleChatActivity.kt';
let code = fs.readFileSync(path, 'utf8');

const targetCall = `                    ChatBubble(
                        message = msg, 
                        fontSizeSp = chatFontSizeSp,
                        isActiveDrill = isActiveDrill,
                        isActiveDrillRunning = activeDrillSession != null,
                        onStartStreakDrill = {`;
const replacementCall = `                    ChatBubble(
                        message = msg, 
                        fontSizeSp = chatFontSizeSp,
                        isActiveDrill = isActiveDrill,
                        isActiveDrillRunning = activeDrillSession != null,
                        onMessageUpdate = { updatedMsg ->
                            val idx = messages.indexOfFirst { it.id == updatedMsg.id }
                            if (idx != -1) {
                                val newList = messages.toMutableList()
                                newList[idx] = updatedMsg
                                messages = newList
                            }
                        },
                        onStartStreakDrill = {`;

code = code.replace(targetCall, replacementCall);
fs.writeFileSync(path, code);
