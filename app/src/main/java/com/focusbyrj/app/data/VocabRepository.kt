package com.focusbyrj.app.data

class VocabRepository(val vocabDao: VocabDao) {
    suspend fun getNextIdiomToLearn(): Idiom? {
        return vocabDao.getUnlearnedIdioms(1).firstOrNull()
    }

    suspend fun getNextOwsToLearn(): Ows? {
        return vocabDao.getUnlearnedOws(1).firstOrNull()
    }

    suspend fun markIdiomLearned(idiom: Idiom) {
        vocabDao.updateIdiom(idiom.copy(learnedAt = System.currentTimeMillis()))
    }

    suspend fun markOwsLearned(ows: Ows) {
        vocabDao.updateOws(ows.copy(learnedAt = System.currentTimeMillis()))
    }

    suspend fun getAllLearnedIdioms(): List<Idiom> {
        return vocabDao.getAllLearnedIdioms()
    }

    suspend fun getAllLearnedOws(): List<Ows> {
        return vocabDao.getAllLearnedOws()
    }

    suspend fun getLastLearnedIdiom(): Idiom? {
        return vocabDao.getLastLearnedIdiom()
    }

    suspend fun getLastLearnedOws(): Ows? {
        return vocabDao.getLastLearnedOws()
    }
}
