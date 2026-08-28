package com.focusbyrj.app.util

import com.focusbyrj.app.ui.screens.ChatMessage
import com.focusbyrj.app.ui.screens.DrillSession
import org.json.JSONObject
import java.util.UUID
import kotlin.math.roundToInt

object DrillSummaryHelper {
    fun generateSummaryMessage(session: DrillSession): ChatMessage {
        val baseMultiplier = when (session.difficulty) {
            "medium" -> 25
            "hard" -> 30
            else -> 20
        }
        
        var xpEarned = session.correct * baseMultiplier
        val isPerfect = session.targetQuestions >= 10 && session.correct == session.targetQuestions
        if (isPerfect) {
            xpEarned *= 2
        }

        val streakBonusPercent = AptitudeManager.getStreakBonusPercent()
        val streakBonusXp = if (streakBonusPercent > 0) {
            ((xpEarned * streakBonusPercent) / 100.0).roundToInt()
        } else {
            0
        }
        xpEarned += streakBonusXp

        val profileBefore = AptitudeManager.profileFlow.value
        AptitudeManager.recordDrillResult(xpEarned, session.total, session.correct)
        val profileNow = AptitudeManager.profileFlow.value

        val json = JSONObject().apply {
            put("title", profileNow.title)
            put("tier", profileNow.titleTier)
            put("level", profileNow.level)
            put("xpEarned", xpEarned)
            put("goldEarned", session.gold)
            put("total", session.total)
            put("correct", session.correct)
            put("xpBefore", profileBefore.xp)
            put("xpNow", profileNow.xp)
            put("xpCurrentLevelStart", profileNow.xpForCurrentLevel)
            put("xpNextLevelStart", profileNow.xpForNextLevel)
            put("isPerfect", isPerfect)
            put("currentStreak", profileNow.currentStreak)
            put("longestStreak", profileNow.longestStreak)
            put("streakBonusPercent", profileNow.streakBonusPercent)
            put("streakBonusXp", streakBonusXp)
        }.toString()

        return ChatMessage(
            id = UUID.randomUUID().toString(),
            text = "Drill Summary",
            isUser = false,
            isDrillSummary = true,
            drillSummaryJson = json
        )
    }
}
