package com.focusbyrj.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "idioms")
data class Idiom(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sn: Int? = null,
    val idiom: String,
    val meaning: String,
    @ColumnInfo(name = "repetition_ssc") val repetitionSsc: Int = 0,
    @ColumnInfo(name = "repetition_other") val repetitionOther: Int = 0,
    @ColumnInfo(name = "is_top_200") val isTop200: Int = 0,
    @ColumnInfo(name = "is_mastered") val isMastered: Int = 0,
    @ColumnInfo(name = "is_bookmarked") val isBookmarked: Int = 0,
    @ColumnInfo(name = "learned_at") val learnedAt: Long? = null
)

@Entity(tableName = "ows")
data class Ows(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sn: Int? = null,
    val term: String,
    @ColumnInfo(name = "part_of_speech") val partOfSpeech: String? = null,
    val definition: String,
    @ColumnInfo(name = "repetition_ssc") val repetitionSsc: Int = 0,
    @ColumnInfo(name = "repetition_other") val repetitionOther: Int = 0,
    @ColumnInfo(name = "is_top_200") val isTop200: Int = 0,
    @ColumnInfo(name = "is_spelling_pyq") val isSpellingPyq: Int = 0,
    @ColumnInfo(name = "has_syno_anto") val hasSynoAnto: Int = 0,
    @ColumnInfo(name = "is_mastered") val isMastered: Int = 0,
    @ColumnInfo(name = "is_bookmarked") val isBookmarked: Int = 0,
    @ColumnInfo(name = "learned_at") val learnedAt: Long? = null
)
