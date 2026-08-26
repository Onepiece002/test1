import re

with open('app/src/main/java/com/focusbyrj/app/ui/screens/SettingsScreen.kt', 'r') as f:
    content = f.read()

# Replace the text formatting function for valueText
old_text = 'valueText = "${persistentReminderInterval}m",'
new_text = '''valueText = if (persistentReminderInterval >= 60) "${persistentReminderInterval / 60}h" + if (persistentReminderInterval % 60 > 0) " ${persistentReminderInterval % 60}m" else "" else "${persistentReminderInterval}m",'''
content = content.replace(old_text, new_text)

# Replace the intervals logic
old_stepper = """                    SettingsStepperRow(
                        icon = Icons.Filled.NotificationsActive,
                        title = "Persistent Reminder Nag",
                        subtitle = "Interval for recurring task alerts",
                        valueText = if (persistentReminderInterval >= 60) "${persistentReminderInterval / 60}h" + if (persistentReminderInterval % 60 > 0) " ${persistentReminderInterval % 60}m" else "" else "${persistentReminderInterval}m",
                        onDecrement = {
                            val newInterval = when (persistentReminderInterval) {
                                60 -> 30
                                30 -> 15
                                15 -> 10
                                10 -> 5
                                else -> persistentReminderInterval
                            }
                            if (newInterval != persistentReminderInterval) {
                                persistentReminderInterval = newInterval
                                prefs.edit().putInt("persistent_reminder_interval", newInterval).apply()
                            }
                        },
                        onIncrement = {
                            val newInterval = when (persistentReminderInterval) {
                                5 -> 10
                                10 -> 15
                                15 -> 30
                                30 -> 60
                                else -> persistentReminderInterval
                            }
                            if (newInterval != persistentReminderInterval) {
                                persistentReminderInterval = newInterval
                                prefs.edit().putInt("persistent_reminder_interval", newInterval).apply()
                            }
                        }
                    )"""

# I need to properly capture the exact old_stepper string in the actual file.
