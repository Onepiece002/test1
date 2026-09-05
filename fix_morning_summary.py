import re

with open("app/src/main/java/com/focusbyrj/app/service/DailySummaryReceiver.kt", "r") as f:
    content = f.read()

pattern = re.compile(r'    private suspend fun handleMorningSummary.*?BubbleChatManager\.addMessage\(context, message, incrementBadge = true\)\n    \}', re.DOTALL)

new_method = """    private suspend fun handleMorningSummary(context: Context, app: FocusApplication) {
        val vocabRepo = app.vocabRepository
        val newIdiom = vocabRepo.getNextIdiomToLearn()
        val newOws = vocabRepo.getNextOwsToLearn()
        val revIdiom = vocabRepo.getLastLearnedIdiom()
        val revOws = vocabRepo.getLastLearnedOws()
        
        if (newIdiom != null) vocabRepo.markIdiomLearned(newIdiom)
        if (newOws != null) vocabRepo.markOwsLearned(newOws)
        
        val vocabObj = org.json.JSONObject()
        if (newIdiom != null) {
            vocabObj.put("idiom", org.json.JSONObject().apply {
                put("idiom", newIdiom.idiom)
                put("meaning", newIdiom.meaning)
            })
        }
        if (newOws != null) {
            vocabObj.put("ows", org.json.JSONObject().apply {
                put("term", newOws.term)
                put("definition", newOws.definition)
            })
        }
        if (revIdiom != null) {
            vocabObj.put("rev_idiom", org.json.JSONObject().apply {
                put("idiom", revIdiom.idiom)
                put("meaning", revIdiom.meaning)
            })
        }
        if (revOws != null) {
            vocabObj.put("rev_ows", org.json.JSONObject().apply {
                put("term", revOws.term)
                put("definition", revOws.definition)
            })
        }

        val message = com.focusbyrj.app.util.PersistedChatMessage(
            id = "morning_${System.currentTimeMillis()}",
            text = "☀️ Good morning! Let's build your vocabulary today.",
            isUser = false,
            timestamp = System.currentTimeMillis(),
            isTaskSummary = false,
            isMorningBrief = true,
            isVocabBrief = true,
            vocabJson = vocabObj.toString()
        )
        com.focusbyrj.app.util.BubbleChatManager.addMessage(context, message, incrementBadge = true)
    }"""

content = pattern.sub(new_method, content)

with open("app/src/main/java/com/focusbyrj/app/service/DailySummaryReceiver.kt", "w") as f:
    f.write(content)
