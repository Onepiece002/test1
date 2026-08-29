const fs = require('fs');
const path = './app/src/main/java/com/focusbyrj/app/util/AyvaTalkEngine.kt';
let code = fs.readFileSync(path, 'utf8');

code = code.replace(
`                    is TalkAction.NavigateAppScreen -> {
                        actObj.put("type", "navigate")
                        actObj.put("route", act.route)
                    }`,
`                    is TalkAction.NavigateAppScreen -> {
                        actObj.put("type", "navigate")
                        actObj.put("route", act.route)
                    }
                    is TalkAction.AskQuery -> {
                        actObj.put("type", "ask_query")
                        actObj.put("query", act.query)
                    }`);

fs.writeFileSync(path, code);
