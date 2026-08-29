const fs = require('fs');
const path = './app/src/main/java/com/focusbyrj/app/util/TalkAction.kt';
let code = fs.readFileSync(path, 'utf8');

const targetStr = `    data class DirectPrefUpdate(`;
const newStr = `    data class AskQuery(
        val query: String,
        val buttonLabel: String,
        val iconEmoji: String = "💬"
    ) : TalkAction(buttonLabel, iconEmoji)

    data class DirectPrefUpdate(`;

code = code.replace(targetStr, newStr);

const executeTarget = `                is DirectPrefUpdate -> {`;
const executeNew = `                is AskQuery -> {
                    val intent = Intent(context, com.focusbyrj.app.ui.screens.BubbleChatActivity::class.java).apply {
                        putExtra("prefill_query", query)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    }
                    context.startActivity(intent)
                    true
                }
                is DirectPrefUpdate -> {`;

code = code.replace(executeTarget, executeNew);

fs.writeFileSync(path, code);
