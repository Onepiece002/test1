const fs = require('fs');
const path = './app/src/main/java/com/focusbyrj/app/util/BubbleChatManager.kt';
let code = fs.readFileSync(path, 'utf8');

code = code.replace(`put("talkActionJson", msg.talkActionJson)`, `put("talkActionJson", msg.talkActionJson)
                    put("pendingActionJson", msg.pendingActionJson)`);

code = code.replace(`talkActionJson = if (obj.has("talkActionJson") && !obj.isNull("talkActionJson")) obj.getString("talkActionJson") else null`, `talkActionJson = if (obj.has("talkActionJson") && !obj.isNull("talkActionJson")) obj.getString("talkActionJson") else null,
                pendingActionJson = if (obj.has("pendingActionJson") && !obj.isNull("pendingActionJson")) obj.getString("pendingActionJson") else null`);

fs.writeFileSync(path, code);
