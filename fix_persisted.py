import re

with open("app/src/main/java/com/focusbyrj/app/util/BubbleChatManager.kt", "r") as f:
    content = f.read()

target = """data class PersistedChatMessage(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isArithmetic: Boolean = false,
    val isAptitudeProfile: Boolean = false,
    val isDailyQuests: Boolean = false,
    val isTaskSummary: Boolean = false,
    val taskSummaryJson: String? = null,
    val isMorningBrief: Boolean = false,
    val isEveningBrief: Boolean = false,
    val isStreakFreezeSkipped: Boolean = false,
    val isVocabBrief: Boolean = false,
    val vocabJson: String? = null,
    val isTalkAction: Boolean = false,
    val talkActionJson: String? = null,
    val pendingActionJson: String? = null
)"""

replacement = """data class PersistedChatMessage(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isArithmetic: Boolean = false,
    val isAptitudeProfile: Boolean = false,
    val isDailyQuests: Boolean = false,
    val isTaskSummary: Boolean = false,
    val taskSummaryJson: String? = null,
    val isMorningBrief: Boolean = false,
    val isEveningBrief: Boolean = false,
    val isStreakFreezeSkipped: Boolean = false,
    val isVocabBrief: Boolean = false,
    val vocabJson: String? = null,
    val isTalkAction: Boolean = false,
    val talkActionJson: String? = null,
    val pendingActionJson: String? = null,
    val streakPromptJson: String? = null
)"""

if "val streakPromptJson" not in content:
    content = content.replace(target, replacement)
    
    parse_target = """                        isTalkAction = obj.optBoolean("isTalkAction", false),
                        talkActionJson = if (obj.has("talkActionJson")) obj.getString("talkActionJson") else null,
                        pendingActionJson = if (obj.has("pendingActionJson")) obj.getString("pendingActionJson") else null"""
    
    parse_replacement = """                        isTalkAction = obj.optBoolean("isTalkAction", false),
                        talkActionJson = if (obj.has("talkActionJson")) obj.getString("talkActionJson") else null,
                        pendingActionJson = if (obj.has("pendingActionJson")) obj.getString("pendingActionJson") else null,
                        streakPromptJson = if (obj.has("streakPromptJson")) obj.getString("streakPromptJson") else null"""
                        
    content = content.replace(parse_target, parse_replacement)
    
    save_target = """                if (msg.talkActionJson != null) obj.put("talkActionJson", msg.talkActionJson)
                if (msg.pendingActionJson != null) obj.put("pendingActionJson", msg.pendingActionJson)"""
                
    save_replacement = """                if (msg.talkActionJson != null) obj.put("talkActionJson", msg.talkActionJson)
                if (msg.pendingActionJson != null) obj.put("pendingActionJson", msg.pendingActionJson)
                if (msg.streakPromptJson != null) obj.put("streakPromptJson", msg.streakPromptJson)"""
                
    content = content.replace(save_target, save_replacement)

with open("app/src/main/java/com/focusbyrj/app/util/BubbleChatManager.kt", "w") as f:
    f.write(content)
