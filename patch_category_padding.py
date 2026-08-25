import re

with open("app/src/main/java/com/focusbyrj/app/ui/screens/CustomCategoryEditor.kt", "r") as f:
    content = f.read()

# Make sure it doesn't get squished by status bar either
if ".statusBarsPadding()" not in content:
    content = content.replace("modifier = Modifier.fillMaxSize(),", "modifier = Modifier.fillMaxSize().statusBarsPadding(),")

with open("app/src/main/java/com/focusbyrj/app/ui/screens/CustomCategoryEditor.kt", "w") as f:
    f.write(content)
