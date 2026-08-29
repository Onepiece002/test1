const fs = require('fs');
const path = './app/src/main/java/com/focusbyrj/app/ui/screens/BubbleChatActivity.kt';
let code = fs.readFileSync(path, 'utf8');

code = code.replace(/androidx\.compose\.material\.icons\.rounded\.CheckCircle/g, 'androidx.compose.material.icons.Icons.Rounded.CheckCircle');
code = code.replace(/androidx\.compose\.material\.icons\.rounded\.Close/g, 'androidx.compose.material.icons.Icons.Rounded.Close');

fs.writeFileSync(path, code);
