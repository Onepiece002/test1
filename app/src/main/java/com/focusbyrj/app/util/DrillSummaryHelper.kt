package com.focusbyrj.app.util

import com.focusbyrj.app.ui.screens.ChatMessage
import com.focusbyrj.app.ui.screens.DrillSession
import com.focusbyrj.app.ui.screens.QuestionRecord
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

        // Combo bonus XP
        val comboBonusXp = when {
            session.maxCombo >= 8 -> (session.correct * 15)
            session.maxCombo >= 5 -> (session.correct * 10)
            session.maxCombo >= 3 -> (session.correct * 5)
            else -> 0
        }
        xpEarned += comboBonusXp

        val streakBonusPercent = AptitudeManager.getStreakBonusPercent()
        val streakBonusXp = if (streakBonusPercent > 0) {
            ((xpEarned * streakBonusPercent) / 100.0).roundToInt()
        } else {
            0
        }
        xpEarned += streakBonusXp

        // Sync gold to user wallet
        if (session.gold > 0) {
            FocusEconomyManager.addRewards(baseXp = 0, baseGold = session.gold)
        }

        val profileBefore = AptitudeManager.profileFlow.value
        AptitudeManager.recordDrillResult(xpEarned, session.total, session.correct)
        val profileNow = AptitudeManager.profileFlow.value

        // Check if daily quest was progressed by combo
        if (session.maxCombo >= 4) {
            DailyQuestManager.recordCombo(session.maxCombo)
        }
        
        DailyQuestManager.recordDrillCompleted()

        val elapsedSeconds = if (session.isBlitz) {
            (60 - session.blitzSecondsRemaining).coerceIn(1, 60)
        } else {
            ((System.currentTimeMillis() - session.startTime) / 1000).toInt().coerceAtLeast(1)
        }
        val mins = elapsedSeconds / 60
        val secs = elapsedSeconds % 60
        val timeFormatted = "$mins:${secs.toString().padStart(2, '0')}"

        val questionsArray = org.json.JSONArray()
        session.questionRecords.forEach { qRec ->
            val qObj = JSONObject().apply {
                put("qNum", qRec.questionNumber)
                put("title", qRec.title)
                put("questionText", qRec.questionText)
                val optsArr = org.json.JSONArray()
                qRec.options.forEach { optsArr.put(it) }
                put("options", optsArr)
                put("correctIndex", qRec.correctIndex)
                put("userSelectedIndex", qRec.userSelectedIndex ?: -1)
                put("status", qRec.status)
                put("explanation", qRec.explanation)
            }
            questionsArray.put(qObj)
        }

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
            put("maxCombo", session.maxCombo)
            put("comboBonusXp", comboBonusXp)
            put("isBlitz", session.isBlitz)
            put("elapsedSeconds", elapsedSeconds)
            put("timeFormatted", timeFormatted)
            put("divisionTitle", profileNow.divisionTitle)
            put("divisionIcon", profileNow.divisionIcon)
            put("weeklyXp", profileNow.weeklyXp)
            put("freezeNotice", profileNow.freezeUsedNotice ?: "")
            put("questions", questionsArray)
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
