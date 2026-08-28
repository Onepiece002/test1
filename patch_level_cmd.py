import re

with open('app/src/main/java/com/focusbyrj/app/ui/screens/BubbleChatActivity.kt', 'r') as f:
    content = f.read()

level_cmd = """                    when (cmd) {
                        "/level", "/profile" -> {
                            val profMsg = ChatMessage(
                                id = java.util.UUID.randomUUID().toString(),
                                text = "Aptitude Profile",
                                isUser = false,
                                isAptitudeProfile = true
                            )
                            withContext(Dispatchers.Main) {
                                messages = messages + profMsg
                            }
                            return@launch
                        }"""

content = content.replace("                    when (cmd) {", level_cmd)

with open('app/src/main/java/com/focusbyrj/app/ui/screens/BubbleChatActivity.kt', 'w') as f:
    f.write(content)
