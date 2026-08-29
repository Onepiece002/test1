const fs = require('fs');
const path = './app/src/main/java/com/focusbyrj/app/util/AyvaTalkEngine.kt';
let code = fs.readFileSync(path, 'utf8');

code = code.replace(
    `val actions = suggestions.map { TalkAction.AskQuery(it.title) }`,
    `val actions = suggestions.map { TalkAction.AskQuery(it.title, it.title) }`
);

fs.writeFileSync(path, code);
