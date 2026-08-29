const fs = require('fs');

const kbPath = './app/src/main/assets/ayva_knowledge_base.json';
let kb = JSON.parse(fs.readFileSync(kbPath, 'utf8'));

const troubleshootNodes = [
  {
    "id": "troubleshoot_why_not_blocking",
    "title": "Troubleshooting: Why are apps NOT being blocked?",
    "emoji": "🔍",
    "directExplanation": "Apps opening without the blocker screen is usually caused by a missing permission or battery sleep.",
    "whereToGo": "Settings -> Permissions",
    "whatToClick": [
      "1. Check Usage Access: Ensures app foreground launches are detected.",
      "2. Check Display Over Other Apps: Ensures direct overlay can be drawn.",
      "3. Check Battery Optimization: Set to Unrestricted so Android does not kill the service."
    ],
    "whyOrTip": "FocusByRj uses direct window manager overlays (`TYPE_APPLICATION_OVERLAY`) to reliably block apps on Android 14+.",
    "keywords": [
      "why not blocking",
      "apps not blocking",
      "blocker not working",
      "overlay not showing",
      "not locked",
      "apps still open",
      "not blocking"
    ],
    "route": "permissions",
    "isPackageSettingUri": false,
    "diagnosticKey": "troubleshoot_why_not_blocking"
  },
  {
    "id": "troubleshoot_why_no_notifications",
    "title": "Troubleshooting: Why am I not getting task reminders/sounds?",
    "emoji": "🔍",
    "directExplanation": "Missing or delayed task alarms are usually caused by disabled notification permissions, battery sleep, or system Do Not Disturb.",
    "whereToGo": "Android Settings -> Apps -> FocusByRj -> Notifications",
    "whatToClick": [
      "1. Verify 'Post Notifications' is allowed in Android App Settings.",
      "2. Set Battery Optimization to Unrestricted.",
      "3. Check if your phone is in Do Not Disturb (DND) mode.",
      "4. In Settings -> Persistent Reminder Interval, ensure an interval (e.g. 15m) is selected."
    ],
    "keywords": [
      "why no notification",
      "reminder not working",
      "sound not playing",
      "alarm not ringing",
      "missing notifications",
      "delayed notification",
      "no sound"
    ],
    "route": "settings",
    "systemSettingAction": "android.settings.APP_NOTIFICATION_SETTINGS",
    "isPackageSettingUri": true,
    "diagnosticKey": "troubleshoot_why_no_notifications"
  },
  {
    "id": "troubleshoot_why_cant_uninstall",
    "title": "Troubleshooting: Why can't I uninstall FocusByRj?",
    "emoji": "🔍",
    "directExplanation": "You cannot uninstall FocusByRj because **Uninstall Protection (Device Administrator)** is currently active.",
    "whereToGo": "Settings -> Security -> Uninstall Protection",
    "whatToClick": [
      "Open Settings in FocusByRj.",
      "Tap 'Security'.",
      "Toggle 'Uninstall Protection' to OFF.",
      "Follow the system prompt to deactivate Device Administrator.",
      "You can now uninstall the app normally from Android."
    ],
    "keywords": [
      "why cant i uninstall",
      "cant uninstall",
      "cant delete app",
      "uninstall disabled",
      "delete app",
      "cannot uninstall",
      "uninstal"
    ],
    "route": "security",
    "isPackageSettingUri": false,
    "diagnosticKey": "troubleshoot_why_cant_uninstall"
  },
  {
    "id": "troubleshoot_why_streak_reset",
    "title": "Troubleshooting: Why did my focus streak reset?",
    "emoji": "🔍",
    "directExplanation": "A streak resets to 0 when a full calendar day (midnight to 11:59 PM) elapses without completing at least one 10-minute focus session or checking off a due task.",
    "whereToGo": "Time tab / Account tab",
    "whatToClick": [
      "Complete at least 1 focus session (10m minimum) OR check off 1 due task every day before 11:59 PM.",
      "Your streak will increment automatically upon claiming rewards."
    ],
    "keywords": [
      "why streak reset",
      "streak reset",
      "lost streak",
      "streak broken",
      "streak zero",
      "streak broke"
    ],
    "route": "time",
    "isPackageSettingUri": false,
    "diagnosticKey": "troubleshoot_why_streak_reset"
  },
  {
    "id": "troubleshoot_why_bubble_missing",
    "title": "Troubleshooting: Why did the floating bubble disappear?",
    "emoji": "🔍",
    "directExplanation": "The floating bubble may disappear if your device is in landscape mode or if Android put the background service to sleep.",
    "whereToGo": "Bubble Settings",
    "whatToClick": [
      "1. Check if your phone is rotated: 'Hide in Landscape' automatically hides the bubble during videos or games.",
      "2. Check 'Enable Floating Bubble' toggle in Bubble Settings.",
      "3. Ensure Battery optimization is set to Unrestricted for FocusByRj."
    ],
    "keywords": [
      "why bubble disappeared",
      "bubble gone",
      "bubble missing",
      "bubble disappeared",
      "bubble not showing"
    ],
    "route": "bubble_settings",
    "isPackageSettingUri": false,
    "diagnosticKey": "troubleshoot_why_bubble_missing"
  },
  {
    "id": "homescreen_widget",
    "title": "Interactive Home Screen Widget",
    "emoji": "📱",
    "directExplanation": "Add an interactive task checklist to your home screen with direct tap check-off and custom glass styling.",
    "whereToGo": "Phone Home Screen -> Long Press -> Widgets -> FocusByRj -> Todo List",
    "whatToClick": [
      "Long press an empty space on your phone's home screen.",
      "Tap Widgets -> FocusByRj -> drag Todo List to home screen.",
      "Configure widget background (OLED Black, Glass, Frost), opacity, and tap Save."
    ],
    "keywords": [
      "widget",
      "home screen widget",
      "todo widget",
      "desktop widget",
      "add widget",
      "interactive widget"
    ],
    "isPackageSettingUri": false,
    "diagnosticKey": "homescreen_widget"
  },
  {
    "id": "arithmetic_drills",
    "title": "Arithmetic Drills & Neuro-Agility",
    "emoji": "🧠",
    "directExplanation": "Engage your prefrontal cortex with quick math drills to overcome brain fog and initiate flow state.",
    "whereToGo": "Account -> Arithmetic Drills (or type /drill in chat)",
    "whatToClick": [
      "Open Chat and type /drill.",
      "Or go to Account Tab and tap 'Start Drill Session'."
    ],
    "keywords": [
      "math drills",
      "arithmetic",
      "brain fog",
      "math",
      "test",
      "quiz",
      "calculate"
    ],
    "isPackageSettingUri": false,
    "diagnosticKey": "arithmetic_drills"
  }
];

kb = [...kb, ...troubleshootNodes];

fs.writeFileSync(kbPath, JSON.stringify(kb, null, 2));
