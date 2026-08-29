const fs = require('fs');
const path = './app/src/main/java/com/focusbyrj/app/ui/screens/BubbleChatActivity.kt';
let code = fs.readFileSync(path, 'utf8');

const classDecl = `    val isTaskSummary: Boolean = false,
    val taskSummaryJson: String? = null,
    val isTalkAction: Boolean = false,
    val talkActionJson: String? = null
)`;
const newClassDecl = `    val isTaskSummary: Boolean = false,
    val taskSummaryJson: String? = null,
    val isTalkAction: Boolean = false,
    val talkActionJson: String? = null,
    val pendingActionJson: String? = null
)`;
code = code.replace(classDecl, newClassDecl);

const initMap = `ChatMessage(it.id, it.text, it.isUser, it.timestamp, it.isArithmetic, it.arithmeticJson, it.isDrillSummary, it.drillSummaryJson, it.isAptitudeProfile, it.isStreakPrompt, it.streakPromptJson, it.isTaskSummary, it.taskSummaryJson, it.isTalkAction, it.talkActionJson)`;
const newInitMap = `ChatMessage(it.id, it.text, it.isUser, it.timestamp, it.isArithmetic, it.arithmeticJson, it.isDrillSummary, it.drillSummaryJson, it.isAptitudeProfile, it.isStreakPrompt, it.streakPromptJson, it.isTaskSummary, it.taskSummaryJson, it.isTalkAction, it.talkActionJson, it.pendingActionJson)`;
code = code.replace(initMap, newInitMap);

const saveMap = `PersistedChatMessage(it.id, it.text, it.isUser, it.timestamp, it.isArithmetic, it.arithmeticJson, it.isDrillSummary, it.drillSummaryJson, it.isAptitudeProfile, it.isStreakPrompt, it.streakPromptJson, it.isTaskSummary, it.taskSummaryJson, it.isTalkAction, it.talkActionJson)`;
const newSaveMap = `PersistedChatMessage(it.id, it.text, it.isUser, it.timestamp, it.isArithmetic, it.arithmeticJson, it.isDrillSummary, it.drillSummaryJson, it.isAptitudeProfile, it.isStreakPrompt, it.streakPromptJson, it.isTaskSummary, it.taskSummaryJson, it.isTalkAction, it.talkActionJson, it.pendingActionJson)`;
code = code.replace(saveMap, newSaveMap);

fs.writeFileSync(path, code);
