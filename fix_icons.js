const fs = require('fs');
const path = './app/src/main/java/com/focusbyrj/app/ui/screens/BubbleChatActivity.kt';
let code = fs.readFileSync(path, 'utf8');

// revert the inline stuff
code = code.replace(/androidx\.compose\.material3\.Icon\(androidx\.compose\.material\.icons\.Icons\.Rounded\.CheckCircle/g, 'Icon(androidx.compose.material.icons.rounded.CheckCircle');
code = code.replace(/androidx\.compose\.material3\.Icon\(androidx\.compose\.material\.icons\.Icons\.Rounded\.Close/g, 'Icon(androidx.compose.material.icons.rounded.Close');

fs.writeFileSync(path, code);
