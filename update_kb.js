const fs = require('fs');
const path = './app/src/main/assets/ayva_knowledge_base.json';
const data = JSON.parse(fs.readFileSync(path, 'utf8'));

data.push(
  {
    "id": "practice_alert_time",
    "title": "Practice Alert Time",
    "emoji": "⏰",
    "directExplanation": "The specific time when you want to receive your daily practice alert.",
    "whereToGo": "Settings -> Daily Summaries & Practice -> Practice Alert Time",
    "whatToClick": ["Open Settings", "Scroll down to Daily Summaries & Practice", "Set Practice Alert Time"],
    "keywords": ["practice time", "practice alert", "streak time", "streak alert", "drill time", "when to practice"],
    "route": "settings",
    "isPackageSettingUri": false,
    "prefKey": "streak_notification_time",
    "prefType": "string",
    "diagnosticKey": "practice_alert_time"
  },
  {
    "id": "morning_brief_time",
    "title": "Morning Brief Time",
    "emoji": "🌅",
    "directExplanation": "The time you receive your daily morning summary.",
    "whereToGo": "Settings -> Daily Summaries & Practice -> Morning Brief Time",
    "whatToClick": ["Open Settings", "Scroll down to Daily Summaries & Practice", "Set Morning Brief Time"],
    "keywords": ["morning brief", "morning time", "morning summary time", "morning alert"],
    "route": "settings",
    "isPackageSettingUri": false,
    "prefKey": "morning_brief_time",
    "prefType": "string",
    "diagnosticKey": "morning_brief_time"
  },
  {
    "id": "evening_brief_time",
    "title": "Evening Brief Time",
    "emoji": "🌙",
    "directExplanation": "The time you receive your daily evening summary.",
    "whereToGo": "Settings -> Daily Summaries & Practice -> Evening Brief Time",
    "whatToClick": ["Open Settings", "Scroll down to Daily Summaries & Practice", "Set Evening Brief Time"],
    "keywords": ["evening brief", "evening time", "evening summary time", "evening alert"],
    "route": "settings",
    "isPackageSettingUri": false,
    "prefKey": "evening_brief_time",
    "prefType": "string",
    "diagnosticKey": "evening_brief_time"
  },
  {
    "id": "auto_hide_duration",
    "title": "Auto Hide Duration",
    "emoji": "⏱️",
    "directExplanation": "How many seconds before the Ayva Bubble automatically hides to the edge.",
    "whereToGo": "Settings -> Bubble Settings -> Auto Hide Bubble",
    "whatToClick": ["Open Settings", "Enable Auto Hide", "Set Duration"],
    "keywords": ["auto hide time", "bubble hide time", "bubble hide duration", "hide delay"],
    "route": "settings",
    "isPackageSettingUri": false,
    "prefKey": "auto_hide_duration_sec",
    "prefType": "int",
    "diagnosticKey": "auto_hide_duration"
  }
);

fs.writeFileSync(path, JSON.stringify(data, null, 2));
