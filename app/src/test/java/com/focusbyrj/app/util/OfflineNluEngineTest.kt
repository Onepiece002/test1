package com.focusbyrj.app.util

import com.focusbyrj.app.data.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
class OfflineNluEngineTest {

    @Test
    fun testRescheduleIntent() {
        val result = OfflineNluEngine.parse("reschedule task 1 to tomorrow", emptyList())
        assertEquals(NluIntent.RESCHEDULE, result.intent)
    }

    @Test
    fun testCompleteIntent() {
        val result = OfflineNluEngine.parse("mark 2nd task as done", emptyList())
        assertEquals(NluIntent.COMPLETE, result.intent)
    }

    @Test
    fun testTargetExtractionByTitle() {
        val tasks = listOf(Task(id = 1, title = "Buy groceries", type = com.focusbyrj.app.data.TaskType.TASK), Task(id = 2, title = "Finish report", type = com.focusbyrj.app.data.TaskType.TASK))
        val result = OfflineNluEngine.parse("finish report", tasks)
        assertNotNull(result.targetTask)
        assertEquals("Finish report", result.targetTask?.title)
    }
    
    @Test
    fun testTargetExtractionByOrdinal() {
        val tasks = listOf(Task(id = 1, title = "Buy groceries", type = com.focusbyrj.app.data.TaskType.TASK), Task(id = 2, title = "Finish report", type = com.focusbyrj.app.data.TaskType.TASK))
        val result1 = OfflineNluEngine.parse("complete 1st task", tasks)
        assertEquals("Buy groceries", result1.targetTask?.title)
        
        val result2 = OfflineNluEngine.parse("cancel task 2", tasks)
        assertEquals("Finish report", result2.targetTask?.title)
    }
    
    @Test
    fun testBlockIntent() {
         val result1 = OfflineNluEngine.parse("block instagram", emptyList())
         assertEquals(NluIntent.BLOCK_APP, result1.intent)
         assertEquals("instagram", result1.targetFilterOrAppName)
         assertEquals("HARD", result1.blockMode)
         
         val result2 = OfflineNluEngine.parse("soft block Social filter", emptyList())
         assertEquals(NluIntent.BLOCK_FILTER, result2.intent)
         assertEquals("social", result2.targetFilterOrAppName)
         assertEquals("SOFT", result2.blockMode)
    }
    
    @Test
    fun testRoutinesIntent() {
        val result = OfflineNluEngine.parse("list my routines", emptyList())
        assertEquals(NluIntent.LIST_ROUTINES, result.intent)
    }
}
