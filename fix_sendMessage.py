with open("app/src/main/java/com/focusbyrj/app/ui/screens/BubbleChatActivity.kt", "r") as f:
    lines = f.readlines()

for i, line in enumerate(lines):
    if "fun sendMessage(overrideText: String? = null) {" in line:
        start_idx = i
        break

# Find the end of this block we want to rewrite
# The block is from `fun sendMessage` down to `val finalTitle = ...`
end_idx = start_idx
for i in range(start_idx, len(lines)):
    if "val finalTitle =" in lines[i]:
        end_idx = i
        break

new_lines = [
    "    fun sendMessage(overrideText: String? = null) {\n",
    "        lastInteractionTimestamp = System.currentTimeMillis()\n",
    "        showCatForWelcome = false\n",
    "        showCatForInactivity = false\n",
    "        isCatActionPlaying = false\n",
    "\n",
    "        val textToSendOriginal = (overrideText ?: inputText).trim()\n",
    "        var textToSend = textToSendOriginal\n",
    "        if (textToSend.isNotBlank()) {\n",
    "            val lowerCheck = textToSend.lowercase()\n",
    "            if (lowerCheck == \"morning brief\" || lowerCheck == \"morning briefing\" || lowerCheck == \"good morning\") {\n",
    "                textToSend = \"/summary morning\"\n",
    "            } else if (lowerCheck == \"evening brief\" || lowerCheck == \"evening briefing\" || lowerCheck == \"good evening\" || lowerCheck == \"night brief\") {\n",
    "                textToSend = \"/summary evening\"\n",
    "            }\n",
    "            val userMsg = ChatMessage(System.currentTimeMillis().toString(), textToSendOriginal, true)\n",
    "            messages = messages + userMsg\n",
    "            var sentText = textToSend\n"
]

lines[start_idx:end_idx] = new_lines

with open("app/src/main/java/com/focusbyrj/app/ui/screens/BubbleChatActivity.kt", "w") as f:
    f.writelines(lines)
