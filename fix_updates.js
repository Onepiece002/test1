const fs = require('fs');
const path = './app/src/main/java/com/focusbyrj/app/ui/screens/BubbleChatActivity.kt';
let code = fs.readFileSync(path, 'utf8');

const targetStr = `                    prefs.apply()
                    bubblePrefs.apply()
                    
                    json.put("status", "executed")`;

const replacementStr = `                    prefs.apply()
                    bubblePrefs.apply()
                    
                    if (prefKey == "streak_notification_time" || prefKey == "streak_notification_enabled") {
                        com.focusbyrj.app.service.AptitudeReminderReceiver.scheduleDrillReminders(context)
                    } else if (prefKey == "morning_brief_time" || prefKey == "evening_brief_time") {
                        com.focusbyrj.app.service.DailySummaryReceiver.scheduleDailySummaries(context)
                    }
                    
                    json.put("status", "executed")`;

code = code.replace(targetStr, replacementStr);
fs.writeFileSync(path, code);
