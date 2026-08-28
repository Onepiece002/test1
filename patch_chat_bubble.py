with open('app/src/main/java/com/focusbyrj/app/ui/screens/BubbleChatActivity.kt', 'r') as f:
    content = f.read()

replacement = """    if (message.isDrillSummary) {
        DrillSummaryCard(message = message, fontSizeSp = fontSizeSp)
        return
    }
    if (message.isAptitudeProfile) {
        com.focusbyrj.app.ui.screens.AptitudeProfileCard()
        return
    }"""

content = content.replace("    if (message.isDrillSummary) {\n        DrillSummaryCard(message = message, fontSizeSp = fontSizeSp)\n        return\n    }", replacement)

with open('app/src/main/java/com/focusbyrj/app/ui/screens/BubbleChatActivity.kt', 'w') as f:
    f.write(content)
