const fs = require('fs');
const path = './app/src/main/java/com/focusbyrj/app/ui/screens/BubbleChatActivity.kt';
let code = fs.readFileSync(path, 'utf8');

code = code.replace(
`    onNavigateSummary: (() -> Unit)? = null
) {`,
`    onNavigateSummary: (() -> Unit)? = null,
    onMessageUpdate: (ChatMessage) -> Unit = {}
) {`);

fs.writeFileSync(path, code);
