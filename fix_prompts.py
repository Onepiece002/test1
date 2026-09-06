with open("app/src/main/java/com/focusbyrj/app/ui/screens/BubbleChatActivity.kt", "r") as f:
    content = f.read()

import re

# Update prompt for evening
prompt_block = """                                    val rawPrompt = if (isMorningBriefQuery) {
                                        "You are Ayva, an AI assistant. The user just asked for their Morning Brief.\\n" +
                                        "1. Greet them with a productive morning message.\\n" +
                                        "2. Summarize their tasks smoothly (do not use harsh markdown lists, blend them in).\\n" +
                                        "3. You must include a section called 'Today's Lessons' using the provided new idiom and one word substitution.\\n" +
                                        (if (revIdiom != null || revOws != null) "4. You must include a section called 'Yesterday's Revision' to remind them of the previous words.\\n" else "") +
                                        "JSON DATA: " + contextData.toString()
                                    } else {
                                        "You are Ayva, an AI assistant. The user just asked for their Evening Brief.\\n" +
                                        "1. Greet them with a relaxing evening message.\\n" +
                                        "2. Summarize their remaining tasks for the day smoothly.\\n" +
                                        "3. You must include a section called 'Tonight's Lessons' using the provided new idiom and one word substitution.\\n" +
                                        (if (revIdiom != null || revOws != null) "4. You must include a section called 'Morning Revision' to remind them of the previous words.\\n" else "") +
                                        "JSON DATA: " + contextData.toString()
                                    }"""

old_prompt_regex = r'val rawPrompt = if \(isMorningBriefQuery\) \{.*?\} else \{.*?\}'
content = re.sub(old_prompt_regex, prompt_block, content, flags=re.DOTALL)

with open("app/src/main/java/com/focusbyrj/app/ui/screens/BubbleChatActivity.kt", "w") as f:
    f.write(content)
