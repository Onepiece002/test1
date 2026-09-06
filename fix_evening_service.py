with open("app/src/main/java/com/focusbyrj/app/service/DailySummaryReceiver.kt", "r") as f:
    content = f.read()

import re

old_evening_block = """    private suspend fun handleEveningSummary(context: Context, app: FocusApplication) {
        val vocabRepo = app.vocabRepository
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
        }"""

new_evening_block = """    private suspend fun handleEveningSummary(context: Context, app: FocusApplication) {
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
        }"""

content = content.replace(old_evening_block, new_evening_block)

with open("app/src/main/java/com/focusbyrj/app/service/DailySummaryReceiver.kt", "w") as f:
    f.write(content)
