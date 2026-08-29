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
    val streakBonusPercent: Int = 0,
    val isVacationMode: Boolean = false
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
    const val KEY_VACATION_MODE = "vacation_mode"
    
    private var prefs: SharedPreferences? = null
    
    private val _profileFlow = MutableStateFlow(calculateProfile(0, 0, 0, 0, 0, 0, "", false))
    val profileFlow: StateFlow<AptitudeProfile> = _profileFlow.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val bubblePrefs = context.getSharedPreferences("bubble_prefs", Context.MODE_PRIVATE)
        // Sync vacation mode if present in bubble_prefs
        if (bubblePrefs.contains(KEY_VACATION_MODE)) {
            val vm = bubblePrefs.getBoolean(KEY_VACATION_MODE, false)
            prefs?.edit()?.putBoolean(KEY_VACATION_MODE, vm)?.apply()
        }
        refreshProfile()
    }

    fun isVacationMode(context: Context? = null): Boolean {
        if (context != null) {
            val bubblePrefs = context.getSharedPreferences("bubble_prefs", Context.MODE_PRIVATE)
            if (bubblePrefs.contains(KEY_VACATION_MODE)) {
                return bubblePrefs.getBoolean(KEY_VACATION_MODE, false)
            }
        }
        return prefs?.getBoolean(KEY_VACATION_MODE, false) ?: false
    }

    fun setVacationMode(context: Context, enabled: Boolean) {
        prefs?.edit()?.putBoolean(KEY_VACATION_MODE, enabled)?.apply()
        context.getSharedPreferences("bubble_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_VACATION_MODE, enabled)
            .apply()
        context.getSharedPreferences("focus_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_VACATION_MODE, enabled)
            .apply()

        // When disabling vacation mode, set last active date to yesterday so today's drill seamlessly continues the streak
        if (!enabled) {
            val current = _profileFlow.value.currentStreak
            if (current > 0) {
                val yesterday = getYesterdayDateString()
                prefs?.edit()?.putString(KEY_LAST_ACTIVE_DATE, yesterday)?.apply()
            }
            // Re-schedule drill reminders if enabled
            com.focusbyrj.app.service.AptitudeReminderReceiver.scheduleDrillReminders(context)
        } else {
            // Cancel drill reminders while in vacation mode
            com.focusbyrj.app.service.AptitudeReminderReceiver.cancelAllReminders(context)
        }

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
        val vacation = p.getBoolean(KEY_VACATION_MODE, false)
        
        _profileFlow.value = calculateProfile(savedXp, tQ, cQ, tD, savedStreak, longestStreak, lastDate, vacation)
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
        val vacation = prefs?.getBoolean(KEY_VACATION_MODE, false) ?: false

        val newStreak = when {
            vacation -> savedStreak + 1
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
        
        _profileFlow.value = calculateProfile(newXp, newTQ, newCQ, newTD, newStreak, newLongest, today, vacation)
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
        lastDate: String,
        isVacationMode: Boolean
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
            isVacationMode -> savedStreak // Frozen streak in vacation mode
            lastDate.isEmpty() -> 0
            lastDate == today || lastDate == yesterday -> savedStreak
            else -> 0 // Broken streak if missed a day and not on vacation
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
            streakBonusPercent = streakBonusPercent,
            isVacationMode = isVacationMode
        )
    }
}
