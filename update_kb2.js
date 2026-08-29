const fs = require('fs');
const path = './app/src/main/assets/ayva_knowledge_base.json';
const data = JSON.parse(fs.readFileSync(path, 'utf8'));

data.push(
  {
    "id": "bubble_enabled",
    "title": "Ayva Bubble Assistant",
    "emoji": "💬",
    "directExplanation": "Enable or disable the floating Ayva assistant bubble.",
    "whereToGo": "Settings -> Bubble Settings -> Enable Ayva Bubble",
    "whatToClick": ["Open Settings", "Toggle Ayva Bubble"],
    "keywords": ["bubble", "floating bubble", "assistant bubble", "ayva bubble", "floating head"],
    "route": "settings",
    "isPackageSettingUri": false,
    "prefKey": "bubble_enabled",
    "prefType": "boolean",
    "diagnosticKey": "bubble_enabled"
  },
  {
    "id": "auto_hide_enabled",
    "title": "Auto Hide Bubble",
    "emoji": "👀",
    "directExplanation": "Whether the bubble should automatically peek to the edge of the screen when inactive.",
    "whereToGo": "Settings -> Bubble Settings -> Auto Hide Bubble",
    "whatToClick": ["Open Settings", "Toggle Auto Hide Bubble"],
    "keywords": ["auto hide", "hide bubble", "peek bubble", "tuck bubble"],
    "route": "settings",
    "isPackageSettingUri": false,
    "prefKey": "auto_hide_enabled",
    "prefType": "boolean",
    "diagnosticKey": "auto_hide_enabled"
  },
  {
    "id": "streak_notification_enabled",
    "title": "Streak & Practice Alerts",
    "emoji": "🔥",
    "directExplanation": "Whether you receive daily alerts to practice and build your streak.",
    "whereToGo": "Settings -> Daily Summaries & Practice -> Streak & Practice Alerts",
    "whatToClick": ["Open Settings", "Toggle Streak & Practice Alerts"],
    "keywords": ["practice alerts", "streak notifications", "drill notifications", "daily practice"],
    "route": "settings",
    "isPackageSettingUri": false,
    "prefKey": "streak_notification_enabled",
    "prefType": "boolean",
    "diagnosticKey": "streak_notification_enabled"
  },
  {
    "id": "smart_regex_enabled",
    "title": "Smart Regex Blocking",
    "emoji": "🧠",
    "directExplanation": "Uses advanced text matching to block specific content in URLs and titles.",
    "whereToGo": "Settings -> Advanced Blocking -> Smart Regex Blocking",
    "whatToClick": ["Open Settings", "Toggle Smart Regex Blocking"],
    "keywords": ["smart regex", "regex blocking", "text blocking", "smart matching"],
    "route": "settings",
    "isPackageSettingUri": false,
    "prefKey": "smart_regex_enabled",
    "prefType": "boolean",
    "diagnosticKey": "smart_regex_enabled"
  }
);

fs.writeFileSync(path, JSON.stringify(data, null, 2));
