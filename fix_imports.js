const fs = require('fs');
const path = './app/src/main/java/com/focusbyrj/app/ui/screens/BubbleChatActivity.kt';
let code = fs.readFileSync(path, 'utf8');

const import1 = '\nimport androidx.compose.material.icons.rounded.CheckCircle\nimport androidx.compose.material.icons.rounded.Close\n';

if (!code.includes('import androidx.compose.material.icons.rounded.CheckCircle')) {
    code = code.replace('import androidx.compose.material3.*', 'import androidx.compose.material3.*' + import1);
}

fs.writeFileSync(path, code);
