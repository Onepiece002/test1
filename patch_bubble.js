const fs = require('fs');
const path = 'app/src/main/java/com/focusbyrj/app/service/BubbleService.kt';
let code = fs.readFileSync(path, 'utf8');

const oldFilter = `        val filter = IntentFilter().apply {
            addAction("com.focusbyrj.app.CHAT_CLOSED")
            addAction("com.focusbyrj.app.CHAT_OPENED")
            addAction(BubbleChatManager.ACTION_UNREAD_COUNT_CHANGED)
            addAction(ACTION_SETTINGS_CHANGED)
        }`;

const newFilter = `        val filter = IntentFilter().apply {
            addAction("com.focusbyrj.app.CHAT_CLOSED")
            addAction("com.focusbyrj.app.CHAT_OPENED")
            addAction(BubbleChatManager.ACTION_UNREAD_COUNT_CHANGED)
            addAction(ACTION_SETTINGS_CHANGED)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_SCREEN_ON)
        }`;

code = code.replace(oldFilter, newFilter);

const oldReceiver = `                ACTION_SETTINGS_CHANGED -> {
                    applyBubbleStyleSettings()
                }
            }
        }
    }`;

const newReceiver = `                ACTION_SETTINGS_CHANGED -> {
                    applyBubbleStyleSettings()
                }
                Intent.ACTION_USER_PRESENT, Intent.ACTION_SCREEN_ON -> {
                    if (!isChatOpen) {
                        unpeekBubble(animate = false)
                        resetHideTimer()
                    }
                }
            }
        }
    }`;

code = code.replace(oldReceiver, newReceiver);
fs.writeFileSync(path, code);
