const fs = require('fs');
const path = './app/src/main/java/com/focusbyrj/app/ui/screens/BubbleChatActivity.kt';
let code = fs.readFileSync(path, 'utf8');

const targetStr = `                        if (message.isTalkAction && !message.talkActionJson.isNullOrBlank()) {
                            com.focusbyrj.app.ui.components.TalkActionChips(
                                talkActionJson = message.talkActionJson,
                                fontSizeSp = fontSizeSp
                            )
                        }
                    }`;

const newStr = `                        if (message.isTalkAction && !message.talkActionJson.isNullOrBlank()) {
                            com.focusbyrj.app.ui.components.TalkActionChips(
                                talkActionJson = message.talkActionJson,
                                fontSizeSp = fontSizeSp
                            )
                        }
                        
                        if (!message.pendingActionJson.isNullOrBlank()) {
                            PendingActionCard(
                                message = message,
                                fontSizeSp = fontSizeSp
                            )
                        }
                    }`;

code = code.replace(targetStr, newStr);

const componentToAdd = `
@Composable
fun PendingActionCard(message: ChatMessage, fontSizeSp: Float) {
    val context = LocalContext.current
    val json = remember(message.pendingActionJson) { org.json.JSONObject(message.pendingActionJson) }
    val status = json.optString("status", "pending")
    val title = json.optString("title", "")
    val displayVal = json.optString("displayVal", "")
    
    Spacer(modifier = Modifier.height(12.dp))
    
    if (status == "pending") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            androidx.compose.material3.Button(
                onClick = {
                    val prefKey = json.optString("prefKey")
                    val prefType = json.optString("prefType")
                    val value = json.optString("value")
                    
                    val prefs = context.getSharedPreferences("focus_prefs", Context.MODE_PRIVATE).edit()
                    val bubblePrefs = context.getSharedPreferences("bubble_prefs", Context.MODE_PRIVATE).edit()
                    
                    if (prefType == "int") {
                        prefs.putInt(prefKey, value.toIntOrNull() ?: 0)
                        bubblePrefs.putInt(prefKey, value.toIntOrNull() ?: 0)
                    } else if (prefType == "boolean") {
                        prefs.putBoolean(prefKey, value == "true")
                        bubblePrefs.putBoolean(prefKey, value == "true")
                    } else if (prefType == "string") {
                        prefs.putString(prefKey, value)
                        bubblePrefs.putString(prefKey, value)
                    }
                    prefs.apply()
                    bubblePrefs.apply()
                    
                    json.put("status", "executed")
                    val updatedMessage = message.copy(pendingActionJson = json.toString())
                    
                    // BubbleChatManager expects PersistedChatMessage
                    com.focusbyrj.app.util.BubbleChatManager.updateMessage(context, com.focusbyrj.app.util.PersistedChatMessage(
                        updatedMessage.id, updatedMessage.text, updatedMessage.isUser, updatedMessage.timestamp,
                        updatedMessage.isArithmetic, updatedMessage.arithmeticJson, updatedMessage.isDrillSummary,
                        updatedMessage.drillSummaryJson, updatedMessage.isAptitudeProfile, updatedMessage.isStreakPrompt,
                        updatedMessage.streakPromptJson, updatedMessage.isTaskSummary, updatedMessage.taskSummaryJson,
                        updatedMessage.isTalkAction, updatedMessage.talkActionJson, updatedMessage.pendingActionJson
                    ))
                },
                modifier = Modifier.weight(1f),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF10B981)),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
            ) {
                Text("Yes, proceed", fontSize = (fontSizeSp * 0.9f).sp, fontWeight = FontWeight.SemiBold)
            }
            
            androidx.compose.material3.OutlinedButton(
                onClick = {
                    json.put("status", "cancelled")
                    val updatedMessage = message.copy(pendingActionJson = json.toString())
                    
                    com.focusbyrj.app.util.BubbleChatManager.updateMessage(context, com.focusbyrj.app.util.PersistedChatMessage(
                        updatedMessage.id, updatedMessage.text, updatedMessage.isUser, updatedMessage.timestamp,
                        updatedMessage.isArithmetic, updatedMessage.arithmeticJson, updatedMessage.isDrillSummary,
                        updatedMessage.drillSummaryJson, updatedMessage.isAptitudeProfile, updatedMessage.isStreakPrompt,
                        updatedMessage.streakPromptJson, updatedMessage.isTaskSummary, updatedMessage.taskSummaryJson,
                        updatedMessage.isTalkAction, updatedMessage.talkActionJson, updatedMessage.pendingActionJson
                    ))
                },
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
            ) {
                Text("Cancel", fontSize = (fontSizeSp * 0.9f).sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    } else if (status == "executed") {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().background(androidx.compose.ui.graphics.Color(0xFF10B981).copy(alpha = 0.1f), RoundedCornerShape(8.dp)).padding(12.dp)) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = "Done", tint = androidx.compose.ui.graphics.Color(0xFF10B981), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Changed successfully.", color = androidx.compose.ui.graphics.Color(0xFF10B981), fontSize = (fontSizeSp * 0.9f).sp, fontWeight = FontWeight.Medium)
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp)).padding(12.dp)) {
            Icon(Icons.Rounded.Close, contentDescription = "Cancelled", tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Action cancelled.", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant, fontSize = (fontSizeSp * 0.9f).sp, fontWeight = FontWeight.Medium)
        }
    }
}
`;

code = code + componentToAdd;
fs.writeFileSync(path, code);
