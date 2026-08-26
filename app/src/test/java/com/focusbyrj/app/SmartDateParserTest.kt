package com.focusbyrj.app

import com.focusbyrj.app.data.RecurrencePattern
import com.focusbyrj.app.util.SmartDateParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class SmartDateParserTest {

    @Test
    fun testEveryMonthOn19th() {
        val result = SmartDateParser.parse("pay rent on 19 th every month")
        assertEquals("pay rent", result.cleanText)
        assertEquals(RecurrencePattern.MONTHLY, result.recurrence)
        assertNotNull(result.timestamp)

        val cal = Calendar.getInstance().apply { timeInMillis = result.timestamp!! }
        assertEquals(19, cal.get(Calendar.DAY_OF_MONTH))
        
        val nowCal = Calendar.getInstance()
        if (nowCal.get(Calendar.DAY_OF_MONTH) > 19) {
            val expectedMonth = (nowCal.get(Calendar.MONTH) + 1) % 12
            assertEquals(expectedMonth, cal.get(Calendar.MONTH))
        }
    }

    @Test
    fun testEveryDayAt8pm() {
        val result = SmartDateParser.parse("call mom every day at 8pm")
        assertEquals("call mom", result.cleanText)
        assertEquals(RecurrencePattern.DAILY, result.recurrence)
        assertTrue(result.hasTime)
        assertNotNull(result.timestamp)

        val cal = Calendar.getInstance().apply { timeInMillis = result.timestamp!! }
        assertEquals(20, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, cal.get(Calendar.MINUTE))
    }

    @Test
    fun testEveryMondayAt10am() {
        val result = SmartDateParser.parse("team standup every monday at 10am")
        assertEquals("team standup", result.cleanText)
        assertEquals(RecurrencePattern.WEEKLY, result.recurrence)
        assertTrue(result.hasTime)
        assertNotNull(result.timestamp)

        val cal = Calendar.getInstance().apply { timeInMillis = result.timestamp!! }
        assertEquals(Calendar.MONDAY, cal.get(Calendar.DAY_OF_WEEK))
        assertEquals(10, cal.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun testEveryYearOnApril15th() {
        val result = SmartDateParser.parse("taxes on apr 15th every year")
        assertEquals("taxes", result.cleanText)
        assertEquals(RecurrencePattern.YEARLY, result.recurrence)
        assertNotNull(result.timestamp)

        val cal = Calendar.getInstance().apply { timeInMillis = result.timestamp!! }
        assertEquals(Calendar.APRIL, cal.get(Calendar.MONTH))
        assertEquals(15, cal.get(Calendar.DAY_OF_MONTH))
    }
}
