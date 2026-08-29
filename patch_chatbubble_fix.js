const fs = require('fs');
const path = './app/src/main/java/com/focusbyrj/app/ui/screens/BubbleChatActivity.kt';
let code = fs.readFileSync(path, 'utf8');

// 1. Update ChatBubble definition
code = code.replace(
`fun ChatBubble(
    message: ChatMessage, 
    fontSizeSp: Float = 15f,
    isActiveDrill: Boolean = false,
    isActiveDrillRunning: Boolean = false,
    onDrillAnswer: ((Boolean) -> Unit)? = null,
    onStartStreakDrill: (() -> Unit)? = null,
    onNavigateSummary: (() -> Unit)? = null
) {`,
`fun ChatBubble(
    message: ChatMessage, 
    fontSizeSp: Float = 15f,
    isActiveDrill: Boolean = false,
    isActiveDrillRunning: Boolean = false,
    onDrillAnswer: ((Boolean) -> Unit)? = null,
    onStartStreakDrill: (() -> Unit)? = null,
    onNavigateSummary: (() -> Unit)? = null,
    onMessageUpdate: (ChatMessage) -> Unit = {}
) {`
);

// 2. Update ChatBubble call
code = code.replace(
`                    ChatBubble(
                        message = msg, 
                        fontSizeSp = chatFontSizeSp,
                        isActiveDrill = isActiveDrill,
                        isActiveDrillRunning = activeDrillSession != null,
                        onStartStreakDrill = {
                            if (activeDrillSession == null) {
                                val isArithmetic = msg.streakPromptJson?.contains("Arithmetic") == true
                                activeDrillSession = DrillSession(isArithmetic = isArithmetic)
                            }
                        },
                        onNavigateSummary = {
                            val intent = Intent(context, com.focusbyrj.app.MainActivity::class.java).apply {
                                putExtra("navigate_to", "analytics")
                            }
                            context.startActivity(intent)
                        },
                        onDrillAnswer = { isCorrect ->
                            // Answer logic
                            if (isCorrect) {
                                val intent = Intent(context, com.focusbyrj.app.MainActivity::class.java).apply {
                                    putExtra("log_drill_score", 100)
                                }
                                context.startActivity(intent)
                            }
                            activeDrillSession = null
                            
                            val reportMsg = ChatMessage(
                                id = "report_\${System.currentTimeMillis()}",
                                text = "Drill Completed! Your streak has been updated.",
                                isUser = false,
                                isDrillSummary = true,
                                drillSummaryJson = "{\\"score\\": \${if(isCorrect) 100 else 0}, \\"isCorrect\\": $isCorrect}"
                            )
                            messages = messages + reportMsg
                        }
                    )`,
`                    ChatBubble(
                        message = msg, 
                        fontSizeSp = chatFontSizeSp,
                        isActiveDrill = isActiveDrill,
                        isActiveDrillRunning = activeDrillSession != null,
                        onStartStreakDrill = {
                            if (activeDrillSession == null) {
                                val isArithmetic = msg.streakPromptJson?.contains("Arithmetic") == true
                                activeDrillSession = DrillSession(isArithmetic = isArithmetic)
                            }
                        },
                        onNavigateSummary = {
                            val intent = Intent(context, com.focusbyrj.app.MainActivity::class.java).apply {
                                putExtra("navigate_to", "analytics")
                            }
                            context.startActivity(intent)
                        },
                        onDrillAnswer = { isCorrect ->
                            if (isCorrect) {
                                val intent = Intent(context, com.focusbyrj.app.MainActivity::class.java).apply {
                                    putExtra("log_drill_score", 100)
                                }
                                context.startActivity(intent)
                            }
                            activeDrillSession = null
                            
                            val reportMsg = ChatMessage(
                                id = "report_\${System.currentTimeMillis()}",
                                text = "Drill Completed! Your streak has been updated.",
                                isUser = false,
                                isDrillSummary = true,
                                drillSummaryJson = "{\\"score\\": \${if(isCorrect) 100 else 0}, \\"isCorrect\\": $isCorrect}"
                            )
                            messages = messages + reportMsg
                        },
                        onMessageUpdate = { updatedMsg ->
                            val idx = messages.indexOfFirst { it.id == updatedMsg.id }
                            if (idx != -1) {
                                val newList = messages.toMutableList()
                                newList[idx] = updatedMsg
                                messages = newList
                            }
                        }
                    )`);

fs.writeFileSync(path, code);
