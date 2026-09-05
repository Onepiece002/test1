import re

with open("app/src/main/java/com/focusbyrj/app/service/DailySummaryReceiver.kt", "r") as f:
    content = f.read()

target_morning = """        val message = PersistedChatMessage(
            id = "morning_${System.currentTimeMillis()}",
            text = sb.toString().trim(),
            isUser = false,
            timestamp = System.currentTimeMillis(),
            isTaskSummary = false,
            isMorningBrief = true
        )
        BubbleChatManager.addMessage(context, message, incrementBadge = true)"""

replacement_morning = """        val vocabRepo = app.vocabRepository
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

        val message = PersistedChatMessage(
            id = "morning_${System.currentTimeMillis()}",
            text = "☀️ Good morning! Let's build your vocabulary today.",
            isUser = false,
            timestamp = System.currentTimeMillis(),
            isTaskSummary = false,
            isMorningBrief = true,
            isVocabBrief = true,
            vocabJson = vocabObj.toString()
        )
        BubbleChatManager.addMessage(context, message, incrementBadge = true)"""


target_evening = """        val message = PersistedChatMessage(
            id = "evening_${System.currentTimeMillis()}",
            text = sb.toString().trim(),
            isUser = false,
            timestamp = System.currentTimeMillis(),
            isTaskSummary = false,
            isEveningBrief = true
        )
        BubbleChatManager.addMessage(context, message, incrementBadge = true)"""


replacement_evening = """        val vocabRepo = app.vocabRepository
        val newIdiom = vocabRepo.getNextIdiomToLearn()
        val newOws = vocabRepo.getNextOwsToLearn()
        
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

        val message = PersistedChatMessage(
            id = "evening_${System.currentTimeMillis()}",
            text = "🌙 Good evening! Time for your nightly vocab drip.",
            isUser = false,
            timestamp = System.currentTimeMillis(),
            isTaskSummary = false,
            isEveningBrief = true,
            isVocabBrief = true,
            vocabJson = vocabObj.toString()
        )
        BubbleChatManager.addMessage(context, message, incrementBadge = true)"""

content = content.replace(target_morning, replacement_morning)
content = content.replace(target_evening, replacement_evening)

with open("app/src/main/java/com/focusbyrj/app/service/DailySummaryReceiver.kt", "w") as f:
    f.write(content)
