const fs = require('fs');
const path = './app/src/main/java/com/focusbyrj/app/ui/screens/BubbleChatActivity.kt';
let code = fs.readFileSync(path, 'utf8');

code = code.replace(
`Icon(Icons.Rounded.CheckCircle, contentDescription = "Done"`,
`androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Rounded.CheckCircle, contentDescription = "Done"`
);

code = code.replace(
`Icon(Icons.Rounded.Close, contentDescription = "Cancelled"`,
`androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Rounded.Close, contentDescription = "Cancelled"`
);

fs.writeFileSync(path, code);
