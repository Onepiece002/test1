package com.focusbyrj.app.util

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class AptitudeProfile(
    val xp: Int,
    val level: Int,
    val title: String,
    val titleTier: Int, // 1 to 6 for UI styling
    val xpForNextLevel: Int,
    val xpForCurrentLevel: Int,
    val totalQuestions: Int = 0,
    val correctQuestions: Int = 0,
    val totalDrills: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val streakBonusPercent: Int = 0
) {
    val accuracy: Float
        get() = if (totalQuestions > 0) (correctQuestions.toFloat() / totalQuestions) * 100f else 0f
}

object AptitudeManager {
    private const val PREFS_NAME = "aptitude_economy_prefs"
    private const val KEY_APTITUDE_XP = "aptitude_xp"
    private const val KEY_TOTAL_QUESTIONS = "total_questions"
    private const val KEY_CORRECT_QUESTIONS = "correct_questions"
    private const val KEY_TOTAL_DRILLS = "total_drills"
    private const val KEY_CURRENT_STREAK = "current_streak"
    private const val KEY_LONGEST_STREAK = "longest_streak"
    private const val KEY_LAST_ACTIVE_DATE = "last_active_date"
    
    private var prefs: SharedPreferences? = null
    
    private val _profileFlow = MutableStateFlow(calculateProfile(0, 0, 0, 0, 0, 0, ""))
    val profileFlow: StateFlow<AptitudeProfile> = _profileFlow.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        refreshProfile()
    }

    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    private fun getYesterdayDateString(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }

    fun refreshProfile() {
        val p = prefs ?: return
        val savedXp = p.getInt(KEY_APTITUDE_XP, 0)
        val tQ = p.getInt(KEY_TOTAL_QUESTIONS, 0)
        val cQ = p.getInt(KEY_CORRECT_QUESTIONS, 0)
        val tD = p.getInt(KEY_TOTAL_DRILLS, 0)
        val savedStreak = p.getInt(KEY_CURRENT_STREAK, 0)
        val longestStreak = p.getInt(KEY_LONGEST_STREAK, 0)
        val lastDate = p.getString(KEY_LAST_ACTIVE_DATE, "") ?: ""
        
        _profileFlow.value = calculateProfile(savedXp, tQ, cQ, tD, savedStreak, longestStreak, lastDate)
    }

    fun addAptitudeXp(amount: Int) {
        val currentXp = _profileFlow.value.xp
        val newXp = currentXp + amount
        prefs?.edit()?.putInt(KEY_APTITUDE_XP, newXp)?.apply()
        refreshProfile()
    }

    fun getStreakBonusPercent(): Int {
        val p = _profileFlow.value
        val effectiveStreak = p.currentStreak
        // 1 to 7 days: 5%, 10%, 15%, 20%, 25%, 30%, 35% (max constant at 7 days)
        val bonusDays = effectiveStreak.coerceIn(0, 7)
        return bonusDays * 5
    }
    
    fun recordDrillResult(xpEarned: Int, questions: Int, correct: Int) {
        val p = _profileFlow.value
        val today = getTodayDateString()
        val yesterday = getYesterdayDateString()
        val lastDate = prefs?.getString(KEY_LAST_ACTIVE_DATE, "") ?: ""
        val savedStreak = prefs?.getInt(KEY_CURRENT_STREAK, 0) ?: 0
        val savedLongest = prefs?.getInt(KEY_LONGEST_STREAK, 0) ?: 0

        val newStreak = when {
            lastDate == today -> if (savedStreak <= 0) 1 else savedStreak
            lastDate == yesterday -> savedStreak + 1
            else -> 1
        }
        val newLongest = maxOf(savedLongest, newStreak)

        val newXp = p.xp + xpEarned
        val newTQ = p.totalQuestions + questions
        val newCQ = p.correctQuestions + correct
        val newTD = p.totalDrills + 1
        
        prefs?.edit()?.apply {
            putInt(KEY_APTITUDE_XP, newXp)
            putInt(KEY_TOTAL_QUESTIONS, newTQ)
            putInt(KEY_CORRECT_QUESTIONS, newCQ)
            putInt(KEY_TOTAL_DRILLS, newTD)
            putInt(KEY_CURRENT_STREAK, newStreak)
            putInt(KEY_LONGEST_STREAK, newLongest)
            putString(KEY_LAST_ACTIVE_DATE, today)
        }?.apply()
        
        _profileFlow.value = calculateProfile(newXp, newTQ, newCQ, newTD, newStreak, newLongest, today)
    }

    // Level formula: xp = (level-1)^2 * 100
    // => level = sqrt(xp/100) + 1
    private fun getLevelForXp(xp: Int): Int {
        return (sqrt(xp / 100.0)).toInt() + 1
    }
    
    private fun getXpForLevel(level: Int): Int {
        return (level - 1) * (level - 1) * 100
    }

    private fun calculateProfile(
        xp: Int, 
        tQ: Int, 
        cQ: Int, 
        tD: Int, 
        savedStreak: Int, 
        longestStreak: Int, 
        lastDate: String
    ): AptitudeProfile {
        val level = getLevelForXp(xp)
        val currentLevelXp = getXpForLevel(level)
        val nextLevelXp = getXpForLevel(level + 1)
        
        val (title, tier) = when {
            level >= 76 -> "Aptitude Grandmaster" to 6
            level >= 51 -> "Human Calculator" to 5
            level >= 31 -> "Quant Mastermind" to 4
            level >= 16 -> "Speed Math Specialist" to 3
            level >= 6 -> "Arithmetic Scholar" to 2
            else -> "Novice Number-Cruncher" to 1
        }

        val today = getTodayDateString()
        val yesterday = getYesterdayDateString()
        val currentStreak = when {
            lastDate.isEmpty() -> 0
            lastDate == today || lastDate == yesterday -> savedStreak
            else -> 0 // Broken streak if missed a day
        }

        // Streak bonus starts from 1 day up to 7 days (+5% per day up to +35% max)
        val streakBonusPercent = minOf(currentStreak, 7) * 5

        return AptitudeProfile(
            xp = xp,
            level = level,
            title = title,
            titleTier = tier,
            xpForNextLevel = nextLevelXp,
            xpForCurrentLevel = currentLevelXp,
            totalQuestions = tQ,
            correctQuestions = cQ,
            totalDrills = tD,
            currentStreak = currentStreak,
            longestStreak = maxOf(longestStreak, currentStreak),
            streakBonusPercent = streakBonusPercent
        )
    }
}
