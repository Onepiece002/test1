import re

with open("app/src/main/java/com/focusbyrj/app/ui/screens/BubbleChatActivity.kt", "r") as f:
    content = f.read()

target = """    fun sendMessage(overrideText: String? = null) {
        lastInteractionTimestamp = System.currentTimeMillis()
        showCatForWelcome = false
        showCatForInactivity = false
        isCatActionPlaying = false

        val textToSend = (overrideText ?: inputText).trim()
        if (textToSend.isNotBlank()) {
            val userMsg = ChatMessage(System.currentTimeMillis().toString(), textToSend, true)
            messages = messages + userMsg
            var sentText = textToSend"""

replacement = """    fun sendMessage(overrideText: String? = null) {
        lastInteractionTimestamp = System.currentTimeMillis()
        showCatForWelcome = false
        showCatForInactivity = false
        isCatActionPlaying = false

        var textToSend = (overrideText ?: inputText).trim()
        if (textToSend.isNotBlank()) {
            val lowerCheck = textToSend.lowercase()
            if (lowerCheck == "morning brief" || lowerCheck == "morning briefing" || lowerCheck == "good morning") {
                textToSend = "/summary morning"
            } else if (lowerCheck == "evening brief" || lowerCheck == "evening briefing" || lowerCheck == "good evening" || lowerCheck == "night brief") {
                textToSend = "/summary evening"
            }

            val userMsg = ChatMessage(System.currentTimeMillis().toString(), (overrideText ?: inputText).trim(), true)
            messages = messages + userMsg
            var sentText = textToSend"""

content = content.replace(target, replacement)
with open("app/src/main/java/com/focusbyrj/app/ui/screens/BubbleChatActivity.kt", "w") as f:
    f.write(content)
