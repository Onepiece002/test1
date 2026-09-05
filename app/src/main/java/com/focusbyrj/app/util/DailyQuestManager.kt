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

data class DailyQuest(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val currentProgress: Int,
    val target: Int,
    val isCompleted: Boolean,
    val rewardXp: Int,
    val rewardGold: Int
) {
    val progressFraction: Float
        get() = (currentProgress.toFloat() / target.toFloat()).coerceIn(0f, 1f)
}

data class MysteryReward(
    val xp: Int,
    val gold: Int,
    val streakFreezeAwarded: Boolean,
    val bonusGoldInsteadOfFreeze: Int = 0
)

data class DailyQuestState(
    val dateKey: String,
    val quests: List<DailyQuest>,
    val allCompleted: Boolean,
    val morningChestClaimed: Boolean,
    val eveningChestClaimed: Boolean,
    val isMorningAvailable: Boolean,
    val isEveningAvailable: Boolean,
    val lastReward: MysteryReward? = null
)

object DailyQuestManager {
    private const val PREFS_NAME = "daily_learning_quests_prefs"
    private const val KEY_DATE = "quest_date"
    private const val KEY_DRILLS_COMPLETED = "quest_drills_completed"
    private const val KEY_MORNING_CHEST_CLAIMED = "quest_morning_chest_claimed"
    private const val KEY_EVENING_CHEST_CLAIMED = "quest_evening_chest_claimed"

    private const val TARGET_DRILLS = 1

    private var prefs: SharedPreferences? = null
    private val _stateFlow = MutableStateFlow(
        DailyQuestState(
            dateKey = "",
            quests = emptyList(),
            allCompleted = false,
            morningChestClaimed = false,
            eveningChestClaimed = false,
            isMorningAvailable = false,
            isEveningAvailable = false
        )
    )
    val stateFlow: StateFlow<DailyQuestState> = _stateFlow.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        refreshState()
    }

    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    fun refreshState() {
        val p = prefs ?: return
        val today = getTodayDateString()
        val savedDate = p.getString(KEY_DATE, "") ?: ""

        if (savedDate != today) {
            p.edit()
                .putString(KEY_DATE, today)
                .putInt(KEY_DRILLS_COMPLETED, 0)
                .putBoolean(KEY_MORNING_CHEST_CLAIMED, false)
                .putBoolean(KEY_EVENING_CHEST_CLAIMED, false)
                .remove("quest_correct_count")
                .remove("quest_combo_count")
                .remove("quest_xp_count")
                .remove("quest_chest_claimed")
                .apply()
        }

        val drills = p.getInt(KEY_DRILLS_COMPLETED, 0)
        val morningClaimed = p.getBoolean(KEY_MORNING_CHEST_CLAIMED, false)
        val eveningClaimed = p.getBoolean(KEY_EVENING_CHEST_CLAIMED, false)

        val quest = DailyQuest(
            id = "quest_daily_drill",
            title = "Focus Continuation",
            description = "Attempt a test daily and continue your streak",
            icon = "🎯",
            currentProgress = minOf(drills, TARGET_DRILLS),
            target = TARGET_DRILLS,
            isCompleted = drills >= TARGET_DRILLS,
            rewardXp = 100,
            rewardGold = 50
        )

        val quests = listOf(quest)
        val allDone = quests.all { it.isCompleted }

        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val isMorning = hour in 6..17
        val isEvening = hour < 6 || hour >= 18

        _stateFlow.value = DailyQuestState(
            dateKey = today,
            quests = quests,
            allCompleted = allDone,
            morningChestClaimed = morningClaimed,
            eveningChestClaimed = eveningClaimed,
            isMorningAvailable = isMorning,
            isEveningAvailable = isEvening
        )
    }

    fun recordDrillCompleted() {
        val p = prefs ?: return
        refreshState()
        val current = p.getInt(KEY_DRILLS_COMPLETED, 0)
        p.edit().putInt(KEY_DRILLS_COMPLETED, current + 1).apply()
        refreshState()
    }
    
    fun recordCorrectAnswer() {}
    fun recordCombo(combo: Int) {}
    fun recordXpEarned(xp: Int) {}

    fun markMorningChestClaimed() {
        prefs?.edit()?.putBoolean(KEY_MORNING_CHEST_CLAIMED, true)?.apply()
        refreshState()
    }

    fun markEveningChestClaimed() {
        prefs?.edit()?.putBoolean(KEY_EVENING_CHEST_CLAIMED, true)?.apply()
        refreshState()
    }
    
    // Kept for backward compat in the dialog
    fun claimMysteryChest(): MysteryReward? {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val isMorning = hour in 6..17
        if (isMorning) markMorningChestClaimed() else markEveningChestClaimed()
        return null
    }
}
