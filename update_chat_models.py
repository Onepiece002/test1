import re

def update_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    # Update PersistedChatMessage / ChatMessage definition
    if 'data class PersistedChatMessage' in content:
        content = re.sub(
            r'val drillSummaryJson: String\? = null\n\)',
            'val drillSummaryJson: String? = null,\n    val isAptitudeProfile: Boolean = false\n)',
            content
        )
    elif 'data class ChatMessage' in content:
        content = re.sub(
            r'val drillSummaryJson: String\? = null\n\)',
            'val drillSummaryJson: String? = null,\n    val isAptitudeProfile: Boolean = false\n)',
            content
        )

    # Update usages in BubbleChatManager.kt
    if 'object BubbleChatManager' in content:
        content = re.sub(
            r'isDrillSummary = obj.optBoolean\("isDrillSummary", false\),\n\s*drillSummaryJson = (.*?)\n\s*\)',
            r'isDrillSummary = obj.optBoolean("isDrillSummary", false),\n                        drillSummaryJson = \1,\n                        isAptitudeProfile = obj.optBoolean("isAptitudeProfile", false)\n                    )',
            content
        )
        content = re.sub(
            r'put\("drillSummaryJson", msg\.drillSummaryJson\)\n\s*\}',
            r'put("drillSummaryJson", msg.drillSummaryJson)\n                    put("isAptitudeProfile", msg.isAptitudeProfile)\n                }',
            content
        )

    with open(filepath, 'w') as f:
        f.write(content)

update_file('app/src/main/java/com/focusbyrj/app/util/BubbleChatManager.kt')
update_file('app/src/main/java/com/focusbyrj/app/ui/screens/BubbleChatActivity.kt')
