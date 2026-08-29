const fs = require('fs');

const kb = [
  // ---------------------------------
  // APP THEME & APPEARANCE
  // ---------------------------------
  {
    "id": "app_background_theme",
    "title": "App Background Theme Mode",
    "emoji": "🌓",
    "directExplanation": "Controls the light/dark mode for the entire application.",
    "whereToGo": "Settings -> Appearance -> App Theme",
    "whatToClick": ["Open Settings", "Select Light, Dark, or System"],
    "keywords": ["dark mode", "light mode", "theme", "system mode", "background color", "app appearance", "mode"],
    "route": "settings",
    "isPackageSettingUri": false,
    "prefKey": "app_theme_mode",
    "prefType": "string",
    "diagnosticKey": "app_background_theme"
  },
  {
    "id": "accent_theme",
    "title": "App Accent Color",
    "emoji": "🎨",
    "directExplanation": "Sets the primary highlight color used for buttons, switches, and active elements across the app.",
    "whereToGo": "Settings -> Appearance -> Accent Color",
    "whatToClick": ["Open Settings", "Choose an accent color like Emerald, Violet, etc."],
    "keywords": ["accent color", "primary color", "highlight color", "app color", "change color", "emerald", "violet"],
    "route": "settings",
    "isPackageSettingUri": false,
    "prefKey": "app_theme_color",
    "prefType": "string",
    "diagnosticKey": "accent_theme"
  },
  {
    "id": "locked_screen_overlay_theme",
    "title": "Lock Screen Overlay Theme Mode",
    "emoji": "🛡️",
    "directExplanation": "Sets the theme strictly for the full-screen app blocker overlay.",
    "whereToGo": "Settings -> Appearance -> Overlay Theme",
    "whatToClick": ["Open Settings", "Select Light, Dark, or System for the overlay"],
    "keywords": ["overlay theme", "blocker theme", "lock screen color", "block mode color"],
    "route": "settings",
    "isPackageSettingUri": false,
    "prefKey": "overlay_theme_mode",
    "prefType": "string",
    "diagnosticKey": "locked_screen_overlay_theme"
  },

  // ---------------------------------
  // FOCUS BLOCKING (SOFT/HARD)
  // ---------------------------------
  {
    "id": "soft_mode_wait_timer",
    "title": "Soft Mode Wait Timer",
    "emoji": "⏱️",
    "directExplanation": "The number of seconds you must wait on the block screen before you can bypass it in Soft Mode.",
    "whereToGo": "Settings -> Soft Mode Delay",
    "whatToClick": ["Open Settings", "Change the Soft Mode Delay value."],
    "keywords": ["wait timer", "delay", "soft mode timer", "soft block delay", "wait time", "block timer", "soft lock duration"],
    "route": "settings",
    "isPackageSettingUri": false,
    "prefKey": "soft_lock_duration",
    "prefType": "int",
    "diagnosticKey": "soft_mode_wait_timer"
  },
  {
    "id": "soft_mode_relief_duration",
    "title": "Soft Mode Relief Duration",
    "emoji": "🕊️",
    "directExplanation": "How many minutes an app stays unlocked after you wait out the Soft Mode timer.",
    "whereToGo": "Settings -> Soft Unlock Relief",
    "whatToClick": ["Open Settings", "Change the Soft Unlock Relief value."],
    "keywords": ["relief duration", "unlock time", "soft mode relief", "how long unlocked", "unblock time", "soft unlock duration"],
    "route": "settings",
    "isPackageSettingUri": false,
    "prefKey": "soft_unlock_duration",
    "prefType": "int",
    "diagnosticKey": "soft_mode_relief_duration"
  },
  {
    "id": "strict_mode",
    "title": "Strict Mode (Hard Block)",
    "emoji": "🧱",
    "directExplanation": "Strict Mode blocks apps completely without a wait timer option. You cannot bypass it until the focus session ends.",
    "whereToGo": "Dashboard -> Set category to Strict",
    "whatToClick": ["Go to Dashboard", "Toggle the mode switch on a blocked category to Strict/Hard"],
    "keywords": ["strict mode", "hard block", "no bypass", "total lock", "completely blocked"],
    "route": "dashboard",
    "isPackageSettingUri": false,
    "diagnosticKey": "strict_mode"
  },
  {
    "id": "emergency_relief",
    "title": "Emergency Relief",
    "emoji": "🆘",
    "directExplanation": "If you are in a hard block, you can use an Emergency Relief charge to get 5 minutes of access.",
    "whereToGo": "Block Screen -> Tap Emergency Relief",
    "whatToClick": ["When blocked in strict mode, tap the Emergency Relief button at the bottom."],
    "keywords": ["emergency", "relief", "stuck", "need access", "help bypass", "emergency unlock"],
    "isPackageSettingUri": false,
    "diagnosticKey": "emergency_relief"
  },

  // ---------------------------------
  // NOTIFICATIONS & REMINDERS
  // ---------------------------------
  {
    "id": "persistent_reminders",
    "title": "Persistent Task Reminders",
    "emoji": "🔔",
    "directExplanation": "Sets how often FocusByRj reminds you about overdue or pending priority tasks.",
    "whereToGo": "Settings -> Task Reminders",
    "whatToClick": ["Open Settings", "Change the 'Persistent Reminder Interval'."],
    "keywords": ["persistent reminders", "remind me often", "nag me", "task reminders", "reminder interval"],
    "route": "settings",
    "isPackageSettingUri": false,
    "prefKey": "persistent_reminder_interval",
    "prefType": "int",
    "diagnosticKey": "persistent_reminders"
  },
  {
    "id": "routine_notifications",
    "title": "Routine Start/End Notifications",
    "emoji": "📅",
    "directExplanation": "Alerts you when a scheduled routine begins or finishes.",
    "whereToGo": "Settings -> Routine Notifications",
    "whatToClick": ["Open Settings", "Toggle Routine Notifications on or off."],
    "keywords": ["routine notifications", "schedule alerts", "routine alerts", "start notification"],
    "route": "settings",
    "isPackageSettingUri": false,
    "prefKey": "routine_notifications",
    "prefType": "boolean",
    "diagnosticKey": "routine_notifications"
  },
  {
    "id": "task_notification_style",
    "title": "Task Notification Style",
    "emoji": "📱",
    "directExplanation": "Choose whether task reminders show as a top banner, play a sound (silent), or both.",
    "whereToGo": "Settings -> Task Notification Style",
    "whatToClick": ["Open Settings", "Select Banner, Silent, or Both."],
    "keywords": ["notification style", "banner", "silent notification", "how to notify", "sound or banner"],
    "route": "settings",
    "isPackageSettingUri": false,
    "prefKey": "task_notification_style",
    "prefType": "string",
    "diagnosticKey": "task_notification_style"
  },
  {
    "id": "streak_notification_enabled",
    "title": "Streak Notifications",
    "emoji": "🔥",
    "directExplanation": "Receive a daily reminder to maintain your focus streak.",
    "whereToGo": "Settings -> Streak Notifications",
    "whatToClick": ["Open Settings", "Toggle Streak Notifications."],
    "keywords": ["streak notification", "streak reminder", "remind me to focus"],
    "route": "settings",
    "isPackageSettingUri": false,
    "prefKey": "streak_notification_enabled",
    "prefType": "boolean",
    "diagnosticKey": "streak_notification_enabled"
  },

  // ---------------------------------
  // BUBBLE & OVERLAY
  // ---------------------------------
  {
    "id": "bubble_enable_disable",
    "title": "Floating Chat Bubble",
    "emoji": "💬",
    "directExplanation": "A floating AI assistant bubble that hovers over other apps.",
    "whereToGo": "Bubble Settings",
    "whatToClick": ["Open Bubble Settings", "Toggle 'Enable Floating Bubble'."],
    "keywords": ["enable bubble", "disable bubble", "turn off bubble", "floating bubble", "hide bubble"],
    "route": "bubble_settings",
    "isPackageSettingUri": false,
    "prefKey": "bubble_enabled",
    "prefType": "boolean",
    "diagnosticKey": "bubble_enable_disable"
  },
  {
    "id": "bubble_auto_hide_duration",
    "title": "Bubble Auto-Hide Wait Timer",
    "emoji": "⏱️",
    "directExplanation": "Seconds before the bubble auto-hides into the screen edge.",
    "whereToGo": "Bubble Settings -> Auto Hide Timer",
    "whatToClick": ["Open Bubble Settings", "Change the Auto-Hide wait timer."],
    "keywords": ["hide bubble timer", "auto hide duration", "auto hide timer", "bubble hide time"],
    "route": "bubble_settings",
    "isPackageSettingUri": false,
    "prefKey": "auto_hide_duration_sec",
    "prefType": "int",
    "diagnosticKey": "bubble_auto_hide_duration"
  },
  {
    "id": "bubble_auto_hide_enabled",
    "title": "Enable Bubble Auto-Hide",
    "emoji": "🙈",
    "directExplanation": "Automatically hide the bubble into the edge of the screen after inactivity.",
    "whereToGo": "Bubble Settings -> Enable Auto-Hide",
    "whatToClick": ["Open Bubble Settings", "Toggle Enable Auto-Hide."],
    "keywords": ["auto hide bubble", "auto hide enabled", "enable auto hide", "hide on edge"],
    "route": "bubble_settings",
    "isPackageSettingUri": false,
    "prefKey": "auto_hide_enabled",
    "prefType": "boolean",
    "diagnosticKey": "bubble_auto_hide_enabled"
  },
  {
    "id": "bubble_hide_in_landscape",
    "title": "Hide Bubble in Landscape",
    "emoji": "📱",
    "directExplanation": "Hides the bubble when playing full-screen games or videos.",
    "whereToGo": "Bubble Settings -> Hide in Landscape",
    "whatToClick": ["Open Bubble Settings", "Toggle 'Hide in Landscape'."],
    "keywords": ["hide in landscape", "landscape mode", "gaming bubble", "fullscreen video"],
    "route": "bubble_settings",
    "isPackageSettingUri": false,
    "prefKey": "hide_in_landscape",
    "prefType": "boolean",
    "diagnosticKey": "bubble_hide_in_landscape"
  },

  // ---------------------------------
  // PRIVACY & SECURITY
  // ---------------------------------
  {
    "id": "secure_recents_blur",
    "title": "Secure Recents Privacy Blur",
    "emoji": "🛡️",
    "directExplanation": "Blurs the app contents when shown in the Android recent apps overview.",
    "whereToGo": "Security -> Secure Recents",
    "whatToClick": ["Open Security", "Toggle Secure Recents Privacy Blur."],
    "keywords": ["secure recents", "privacy blur", "blur recents", "recent apps blur", "hide recents"],
    "route": "security",
    "isPackageSettingUri": false,
    "prefKey": "secure_recents",
    "prefType": "boolean",
    "diagnosticKey": "secure_recents_blur"
  },
  {
    "id": "uninstall_protection",
    "title": "Uninstall Protection",
    "emoji": "🔒",
    "directExplanation": "Prevents you from deleting the app during a moment of weakness using Device Administrator.",
    "whereToGo": "Security -> Uninstall Protection",
    "whatToClick": ["Open Security", "Toggle Uninstall Protection."],
    "keywords": ["uninstall protection", "prevent delete", "cant uninstall", "device administrator"],
    "route": "security",
    "isPackageSettingUri": false,
    "diagnosticKey": "uninstall_protection"
  },

  // ---------------------------------
  // OTHER CONFIGS
  // ---------------------------------
  {
    "id": "default_launch_tab",
    "title": "Default Start Tab",
    "emoji": "🏠",
    "directExplanation": "Sets which screen opens when you first launch the app.",
    "whereToGo": "Settings -> Default Start Tab",
    "whatToClick": ["Open Settings", "Select Dashboard, Time, Schedules, or Account."],
    "keywords": ["start tab", "default tab", "launch screen", "startup screen", "first screen"],
    "route": "settings",
    "isPackageSettingUri": false,
    "prefKey": "default_start_tab",
    "prefType": "string",
    "diagnosticKey": "default_launch_tab"
  },
  {
    "id": "smart_regex_enabled",
    "title": "Smart Regex NLP",
    "emoji": "🧠",
    "directExplanation": "Enables smart detection of tasks from plain text (e.g. 'Meeting at 5pm').",
    "whereToGo": "Settings -> Smart Text Parsing",
    "whatToClick": ["Open Settings", "Toggle Smart Text Parsing."],
    "keywords": ["smart regex", "smart text", "nlp", "auto parse task"],
    "route": "settings",
    "isPackageSettingUri": false,
    "prefKey": "smart_regex_enabled",
    "prefType": "boolean",
    "diagnosticKey": "smart_regex_enabled"
  }
];

fs.writeFileSync('./app/src/main/assets/ayva_knowledge_base.json', JSON.stringify(kb, null, 2));
