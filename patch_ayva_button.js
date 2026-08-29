const fs = require('fs');
const path = './app/src/main/java/com/focusbyrj/app/ui/screens/BubbleChatActivity.kt';
let code = fs.readFileSync(path, 'utf8');

const targetStr = `                    // Drill Quick Action`;

const replacementStr = `                    // Ayva Talk Quick Action
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha=0.5f), CircleShape)
                            .clickable {
                                inputTextFieldValue = TextFieldValue(
                                    text = "/talk ",
                                    selection = TextRange(6)
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = com.focusbyrj.app.ui.components.AyvaIcon,
                            contentDescription = "Talk to Ayva",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    // Drill Quick Action`;

code = code.replace(targetStr, replacementStr);
fs.writeFileSync(path, code);
