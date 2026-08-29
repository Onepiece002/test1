const fs = require('fs');
const path = './app/src/main/java/com/focusbyrj/app/ui/screens/BubbleChatActivity.kt';
let code = fs.readFileSync(path, 'utf8');

// Update ChatBubble signature
const bubbleDecl = `fun ChatBubble(message: ChatMessage, fontSizeSp: Float) {`;
const newBubbleDecl = `fun ChatBubble(message: ChatMessage, fontSizeSp: Float, onMessageUpdate: (ChatMessage) -> Unit = {}) {`;
code = code.replace(bubbleDecl, newBubbleDecl);

// Update PendingActionCard invocation
const pendingInvocation = `                            PendingActionCard(
                                message = message,
                                fontSizeSp = fontSizeSp
                            )`;
const newPendingInvocation = `                            PendingActionCard(
                                message = message,
                                fontSizeSp = fontSizeSp,
                                onMessageUpdate = onMessageUpdate
                            )`;
code = code.replace(pendingInvocation, newPendingInvocation);

// Update PendingActionCard signature
const pendingDecl = `fun PendingActionCard(message: ChatMessage, fontSizeSp: Float) {`;
const newPendingDecl = `fun PendingActionCard(message: ChatMessage, fontSizeSp: Float, onMessageUpdate: (ChatMessage) -> Unit) {`;
code = code.replace(pendingDecl, newPendingDecl);

// Find the Button onClick block and replace BubbleChatManager.updateMessage with onMessageUpdate
code = code.replace(`                    // BubbleChatManager expects PersistedChatMessage
                    com.focusbyrj.app.util.BubbleChatManager.updateMessage(context, com.focusbyrj.app.util.PersistedChatMessage(
                        updatedMessage.id, updatedMessage.text, updatedMessage.isUser, updatedMessage.timestamp,
                        updatedMessage.isArithmetic, updatedMessage.arithmeticJson, updatedMessage.isDrillSummary,
                        updatedMessage.drillSummaryJson, updatedMessage.isAptitudeProfile, updatedMessage.isStreakPrompt,
                        updatedMessage.streakPromptJson, updatedMessage.isTaskSummary, updatedMessage.taskSummaryJson,
                        updatedMessage.isTalkAction, updatedMessage.talkActionJson, updatedMessage.pendingActionJson
                    ))`, `                    onMessageUpdate(updatedMessage)`);

code = code.replace(`                    com.focusbyrj.app.util.BubbleChatManager.updateMessage(context, com.focusbyrj.app.util.PersistedChatMessage(
                        updatedMessage.id, updatedMessage.text, updatedMessage.isUser, updatedMessage.timestamp,
                        updatedMessage.isArithmetic, updatedMessage.arithmeticJson, updatedMessage.isDrillSummary,
                        updatedMessage.drillSummaryJson, updatedMessage.isAptitudeProfile, updatedMessage.isStreakPrompt,
                        updatedMessage.streakPromptJson, updatedMessage.isTaskSummary, updatedMessage.taskSummaryJson,
                        updatedMessage.isTalkAction, updatedMessage.talkActionJson, updatedMessage.pendingActionJson
                    ))`, `                    onMessageUpdate(updatedMessage)`);

// Now fix the LazyColumn to pass the callback
const lazyColumnCall = `                    ChatBubble(message = message, fontSizeSp = fontSizeSp)`;
const newLazyColumnCall = `                    ChatBubble(message = message, fontSizeSp = fontSizeSp, onMessageUpdate = { updatedMsg ->
                        val idx = messages.indexOfFirst { it.id == updatedMsg.id }
                        if (idx != -1) {
                            val newList = messages.toMutableList()
                            newList[idx] = updatedMsg
                            messages = newList
                        }
                    })`;
code = code.replace(lazyColumnCall, newLazyColumnCall);

fs.writeFileSync(path, code);
