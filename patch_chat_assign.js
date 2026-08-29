const fs = require('fs');
const path = './app/src/main/java/com/focusbyrj/app/ui/screens/BubbleChatActivity.kt';
let code = fs.readFileSync(path, 'utf8');

const regex = /isTalkAction\s*=\s*talkResp\.actions\.isNotEmpty\(\),\s*talkActionJson\s*=\s*talkResp\.jsonPayload/g;
code = code.replace(regex, `isTalkAction = talkResp.actions.isNotEmpty() || (talkResp.jsonPayload?.contains("\\"status\\":\\"pending\\"") == false && talkResp.jsonPayload != null),
                                talkActionJson = if (talkResp.jsonPayload?.contains("\\"status\\":\\"pending\\"") == false) talkResp.jsonPayload else null,
                                pendingActionJson = if (talkResp.jsonPayload?.contains("\\"status\\":\\"pending\\"") == true) talkResp.jsonPayload else null`);

fs.writeFileSync(path, code);
