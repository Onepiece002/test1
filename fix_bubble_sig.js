const fs = require('fs');
const path = './app/src/main/java/com/focusbyrj/app/ui/screens/BubbleChatActivity.kt';
let code = fs.readFileSync(path, 'utf8');

code = code.replace(
`fun ChatBubble(
    message: ChatMessage, 
    fontSizeSp: Float = 15f,
    isActiveDrill: Boolean = false,
    isActiveDrillRunning: Boolean = false,
    onDrillAnswer: ((Boolean) -> Unit)? = null,
    onDrillEnd: (() -> Unit)? = null,
    onStartStreakDrill: (() -> Unit)? = null,
    onRescheduleClick: (() -> Unit)? = null,
    onTaskToggle: ((Long) -> Unit)? = null,
    onFilterChange: ((String) -> Unit)? = null
) {`,
`fun ChatBubble(
    message: ChatMessage, 
    fontSizeSp: Float = 15f,
    isActiveDrill: Boolean = false,
    isActiveDrillRunning: Boolean = false,
    onDrillAnswer: ((Boolean) -> Unit)? = null,
    onDrillEnd: (() -> Unit)? = null,
    onStartStreakDrill: (() -> Unit)? = null,
    onRescheduleClick: (() -> Unit)? = null,
    onTaskToggle: ((Long) -> Unit)? = null,
    onFilterChange: ((String) -> Unit)? = null,
    onNavigateSummary: (() -> Unit)? = null,
    onMessageUpdate: (ChatMessage) -> Unit = {}
) {`);

fs.writeFileSync(path, code);
