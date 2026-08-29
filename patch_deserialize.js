const fs = require('fs');
const path = './app/src/main/java/com/focusbyrj/app/util/BubbleChatManager.kt';
let code = fs.readFileSync(path, 'utf8');

code = code.replace(`talkActionJson = if (obj.has("talkActionJson") && !obj.isNull("talkActionJson")) obj.optString("talkActionJson", null) else null
                    )`, `talkActionJson = if (obj.has("talkActionJson") && !obj.isNull("talkActionJson")) obj.optString("talkActionJson", null) else null,
                        pendingActionJson = if (obj.has("pendingActionJson") && !obj.isNull("pendingActionJson")) obj.optString("pendingActionJson", null) else null
                    )`);

fs.writeFileSync(path, code);
