with open("app/src/main/java/com/focusbyrj/app/ui/components/VocabBriefContent.kt", "r") as f:
    content = f.read()

content = content.replace('Text(if (isLearnMoreSession) "Next" else "Learn More"', 'Text(if (isLearnMoreSession) "Next" else "More words"')

with open("app/src/main/java/com/focusbyrj/app/ui/components/VocabBriefContent.kt", "w") as f:
    f.write(content)
