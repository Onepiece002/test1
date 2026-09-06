import re

with open("app/src/main/java/com/focusbyrj/app/service/BubbleService.kt", "r") as f:
    content = f.read()

# Replace bgColor, strokeColor, headerColor, textColor
old_colors = """        val bgColor = if (isDark) {
            android.graphics.Color.parseColor("#1E293B") // Solid 100% opaque Slate 800
        } else {
            android.graphics.Color.parseColor("#FFFFFF") // Solid 100% opaque crisp white
        }

        val strokeColor = if (isDark) {
            android.graphics.Color.parseColor("#334155") // Slate 700 border
        } else {
            android.graphics.Color.parseColor("#E2E8F0") // Slate 200 border
        }

        val headerColor = if (isDark) {
            android.graphics.Color.parseColor("#38BDF8") // Sky blue accent
        } else {
            android.graphics.Color.parseColor("#0284C7")
        }

        val textColor = if (isDark) {
            android.graphics.Color.parseColor("#F8FAFC") // High contrast text
        } else {
            android.graphics.Color.parseColor("#0F172A")
        }"""

new_colors = """        // Facebook Messenger style solid blue theme
        val bgColor = android.graphics.Color.parseColor("#0084FF")
        val strokeColor = android.graphics.Color.parseColor("#0084FF")
        val headerColor = android.graphics.Color.parseColor("#E0F2FE") // Light blue for title
        val textColor = android.graphics.Color.parseColor("#FFFFFF") // Crisp white body"""

content = content.replace(old_colors, new_colors)

with open("app/src/main/java/com/focusbyrj/app/service/BubbleService.kt", "w") as f:
    f.write(content)
