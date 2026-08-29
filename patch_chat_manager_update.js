const fs = require('fs');
const path = './app/src/main/java/com/focusbyrj/app/util/BubbleChatManager.kt';
let code = fs.readFileSync(path, 'utf8');

const targetStr = `    fun addMessage(context: Context, message: PersistedChatMessage, incrementBadge: Boolean = false) {
        val current = getMessages(context).toMutableList()
        current.add(message)
        saveMessages(context, current)
        if (incrementBadge) {
            incrementUnread(context)
        }
    }`;

const newStr = targetStr + `

    fun updateMessage(context: Context, updatedMessage: PersistedChatMessage) {
        val current = getMessages(context).toMutableList()
        val index = current.indexOfFirst { it.id == updatedMessage.id }
        if (index != -1) {
            current[index] = updatedMessage
            saveMessages(context, current)
        }
    }`;

code = code.replace(targetStr, newStr);
fs.writeFileSync(path, code);
