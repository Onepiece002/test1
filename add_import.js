const fs = require('fs');
const path = './app/src/main/java/com/focusbyrj/app/ui/screens/BubbleChatActivity.kt';
let code = fs.readFileSync(path, 'utf8');

code = code.replace(/androidx\.compose\.material\.icons\.Icons\.Rounded\.CheckCircle/g, 'Icons.Rounded.CheckCircle');
code = code.replace(/androidx\.compose\.material\.icons\.Icons\.Rounded\.Close/g, 'Icons.Rounded.Close');

const import1 = 'import androidx.compose.material.icons.rounded.CheckCircle\n';
const import2 = 'import androidx.compose.material.icons.rounded.Close\n';
const import3 = 'import androidx.compose.material.icons.Icons\n';

if (!code.includes('import androidx.compose.material.icons.rounded.CheckCircle')) {
    code = code.replace('import androidx.compose.material3.Text', import1 + import2 + import3 + 'import androidx.compose.material3.Text');
}

fs.writeFileSync(path, code);
