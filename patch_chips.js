const fs = require('fs');
const path = './app/src/main/java/com/focusbyrj/app/ui/components/TalkActionChips.kt';
let code = fs.readFileSync(path, 'utf8');

code = code.replace(
`                    "navigate" -> {
                        val route = obj.optString("route")
                        list.add(TalkAction.NavigateAppScreen(route, label, emoji))
                    }`,
`                    "navigate" -> {
                        val route = obj.optString("route")
                        list.add(TalkAction.NavigateAppScreen(route, label, emoji))
                    }
                    "ask_query" -> {
                        val query = obj.optString("query")
                        list.add(TalkAction.AskQuery(query, label, emoji))
                    }`);

fs.writeFileSync(path, code);
