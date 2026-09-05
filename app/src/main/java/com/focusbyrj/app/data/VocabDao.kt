package com.focusbyrj.app.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VocabDao {
    @Query("SELECT * FROM idioms WHERE learned_at IS NULL AND is_mastered = 0 ORDER BY repetition_ssc DESC LIMIT :limit")
    suspend fun getUnlearnedIdioms(limit: Int): List<Idiom>

    @Query("SELECT * FROM ows WHERE learned_at IS NULL AND is_mastered = 0 ORDER BY repetition_ssc DESC LIMIT :limit")
    suspend fun getUnlearnedOws(limit: Int): List<Ows>

    @Query("SELECT * FROM idioms WHERE learned_at IS NOT NULL")
    suspend fun getAllLearnedIdioms(): List<Idiom>

    @Query("SELECT * FROM ows WHERE learned_at IS NOT NULL")
    suspend fun getAllLearnedOws(): List<Ows>

    @Update
    suspend fun updateIdiom(idiom: Idiom)

    @Update
    suspend fun updateOws(ows: Ows)

    @Query("SELECT * FROM idioms WHERE learned_at IS NOT NULL ORDER BY learned_at DESC LIMIT 1")
    suspend fun getLastLearnedIdiom(): Idiom?

    @Query("SELECT * FROM ows WHERE learned_at IS NOT NULL ORDER BY learned_at DESC LIMIT 1")
    suspend fun getLastLearnedOws(): Ows?
}
