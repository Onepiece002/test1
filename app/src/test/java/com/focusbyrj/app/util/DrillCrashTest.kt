package com.focusbyrj.app.util

import org.junit.Test
import org.junit.Assert.*
import com.focusbyrj.app.ui.screens.createDrillSessionWithQuestions

class DrillCrashTest {
    @Test
    fun testDrillCreation() {
        try {
            val session = createDrillSessionWithQuestions("easy", 10)
            assertNotNull(session)
        } catch (e: Exception) {
            println("CRASH CAUSE:")
            println(e.stackTraceToString())
            fail(e.message)
        }
    }
}
