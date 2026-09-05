import re

with open("app/src/main/java/com/focusbyrj/app/ui/screens/BubbleChatActivity.kt", "r") as f:
    content = f.read()

# Intercept morning brief and evening brief
target = """                    val rawText = sentText.trim()
                    if (!rawText.startsWith("/")) {"""

replacement = """                    var rawText = sentText.trim()
                    if (!rawText.startsWith("/")) {
                        val lower = rawText.lowercase()
                        if (lower == "morning brief" || lower == "morning briefing" || lower == "good morning") {
                            sentText = "/summary morning"
                            rawText = sentText
                        } else if (lower == "evening brief" || lower == "evening briefing" || lower == "good evening" || lower == "night brief") {
                            sentText = "/summary evening"
                            rawText = sentText
                        }"""

content = content.replace(target, replacement)
with open("app/src/main/java/com/focusbyrj/app/ui/screens/BubbleChatActivity.kt", "w") as f:
    f.write(content)
