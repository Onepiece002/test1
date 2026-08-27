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

    @Test
    fun testMay202033() {
        val result = SmartDateParser.parse("may 20 2033")
        assertNotNull(result.timestamp)

        val cal = Calendar.getInstance().apply { timeInMillis = result.timestamp!! }
        assertEquals(2033, cal.get(Calendar.YEAR))
        assertEquals(Calendar.MAY, cal.get(Calendar.MONTH))
        assertEquals(20, cal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun testMay202033WithTaskTitleAndTime() {
        val result = SmartDateParser.parse("Renew passport May 20, 2033 at 4pm")
        assertEquals("Renew passport", result.cleanText)
        assertNotNull(result.timestamp)
        assertTrue(result.hasTime)

        val cal = Calendar.getInstance().apply { timeInMillis = result.timestamp!! }
        assertEquals(2033, cal.get(Calendar.YEAR))
        assertEquals(Calendar.MAY, cal.get(Calendar.MONTH))
        assertEquals(20, cal.get(Calendar.DAY_OF_MONTH))
        assertEquals(16, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, cal.get(Calendar.MINUTE))
    }

    @Test
    fun testDayMonthYear2033() {
        val result = SmartDateParser.parse("pay mortgage on 20th of May 2033")
        assertEquals("pay mortgage", result.cleanText)
        assertNotNull(result.timestamp)

        val cal = Calendar.getInstance().apply { timeInMillis = result.timestamp!! }
        assertEquals(2033, cal.get(Calendar.YEAR))
        assertEquals(Calendar.MAY, cal.get(Calendar.MONTH))
        assertEquals(20, cal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun testIsoDateYear() {
        val result = SmartDateParser.parse("Doctor appointment 2033-05-20 at 10:30am")
        assertEquals("Doctor appointment", result.cleanText)
        assertNotNull(result.timestamp)
        assertTrue(result.hasTime)

        val cal = Calendar.getInstance().apply { timeInMillis = result.timestamp!! }
        assertEquals(2033, cal.get(Calendar.YEAR))
        assertEquals(Calendar.MAY, cal.get(Calendar.MONTH))
        assertEquals(20, cal.get(Calendar.DAY_OF_MONTH))
        assertEquals(10, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, cal.get(Calendar.MINUTE))
    }
}
