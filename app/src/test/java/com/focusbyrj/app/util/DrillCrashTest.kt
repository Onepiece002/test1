package com.focusbyrj.app.util

import org.junit.Test
import org.junit.Assert.*
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Robolectric
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import com.focusbyrj.app.ui.screens.*
import com.focusbyrj.app.util.*

@RunWith(RobolectricTestRunner::class)
class DrillCrashTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testDrillCreation() {
        val session = createDrillSessionWithQuestions("easy", 10)
        assertNotNull(session)
        assertEquals(10, session.preGeneratedQuestions.size)
        
        val medSession = createDrillSessionWithQuestions("medium", 10)
        assertNotNull(medSession)
        
        val hardSession = createDrillSessionWithQuestions("hard", 10)
        assertNotNull(hardSession)
    }

    @Test
    fun testBubbleChatActivityDrillIntent() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = Intent(context, BubbleChatActivity::class.java).apply {
            putExtra("EXTRA_START_DRILL", true)
        }
        val controller = Robolectric.buildActivity(BubbleChatActivity::class.java, intent).setup()
        assertNotNull(controller.get())
    }

    @Test
    fun testDrillSummaryHelperGeneration() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        FocusEconomyManager.init(context)
        AptitudeManager.init(context)
        DailyQuestManager.init(context)

        val session = createDrillSessionWithQuestions("easy", 10)
        session.correct = 8
        session.total = 10
        session.maxCombo = 5
        session.gold = 50
        
        val summaryMsg = DrillSummaryHelper.generateSummaryMessage(session)
        assertNotNull(summaryMsg)
        assertTrue(summaryMsg.isDrillSummary)
        assertNotNull(summaryMsg.drillSummaryJson)
    }

    @Test
    fun testFullscreenDrillViewInteractive() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        FocusEconomyManager.init(context)
        AptitudeManager.init(context)
        DailyQuestManager.init(context)

        val session = createDrillSessionWithQuestions("easy", 10)
        val currentJson = session.preGeneratedQuestions.firstOrNull()
        val latestQuestionMessage = ChatMessage(
            id = "drill_active_test",
            text = "Arithmetic Drill",
            isUser = false,
            isArithmetic = true,
            arithmeticJson = currentJson
        )

        composeTestRule.setContent {
            FullscreenDrillView(
                activeSession = session,
                latestQuestionMessage = latestQuestionMessage,
                allQuestions = emptyList(),
                onNextQuestion = {},
                onAnswerSubmitted = { _, _ -> },
                onEndSession = {}
            )
        }
        composeTestRule.waitForIdle()

        // Try clicking option containing 1.
        composeTestRule.onAllNodes(hasText("1.", substring = true))[1].performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun testChatInterfaceDrillFlow() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        FocusEconomyManager.init(context)
        AptitudeManager.init(context)
        DailyQuestManager.init(context)

        composeTestRule.setContent {
            ChatInterface()
        }
        composeTestRule.waitForIdle()

        // Find the quick action button for drill
        composeTestRule.onNodeWithText("⚡ /drill", substring = true).performClick()
        composeTestRule.waitForIdle()

        // Click Send button
        composeTestRule.onNodeWithContentDescription("Send").performClick()
        
        // Wait for IO coroutine to finish and post to Main
        Thread.sleep(1500)
        composeTestRule.waitForIdle()

        // Check if FullscreenDrillView or Drill option appears
        composeTestRule.onAllNodes(hasText("1.", substring = true))[1].performClick()
        composeTestRule.waitForIdle()
    }
}
